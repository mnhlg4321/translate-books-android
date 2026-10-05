#!/usr/bin/env python3
"""Z3 run rule for a live fixture group.

A typed refusal of the production engine is a *measurement*: the fixture is recorded as STRUCTURAL_VALID=false with its
CODE:path and the group goes on. The group stops only for

* UNKNOWN provider cost (a reservation that was not settled),
* the group cap (not enough budget left for the next fixture),
* an infrastructure failure (transport, route, fingerprint, budget of one call, harness crash, missing output),
* three consecutive refusals with the same code (a systematic defect, not noise).

0 automatic retry and 0 repair call are enforced elsewhere; this module only decides whether to dispatch the next fixture.
"""
import argparse
import json
import os
import re
import sys

REFUSAL_REASON_PREFIXES = ("REPAIR_", "CONTENT_", "INPUT_")
INFRA_REASON_PREFIXES = ("RETRY_", "STOP_")
CODE_PATH = re.compile(r"^[A-Z][A-Z0-9_]{1,95}:(?:[A-Za-z][A-Za-z0-9]*|[0-9]+)(?:\.(?:[A-Za-z][A-Za-z0-9]*|[0-9]+))*$")
STAGE_FAILED = re.compile(r"^P6_L[123]_PREDECESSOR_FAILED:(.+)$")
CONSECUTIVE_SAME_CODE_LIMIT = 3

VALID = "VALID"
REFUSED = "REFUSED"
INFRASTRUCTURE = "INFRASTRUCTURE"


def refusal_detail(structural):
    """(engine reason, CODE:path or '') of a typed stage stop, or (None, '') when it is not one."""
    stops = structural.get("stops") or []
    for stop in stops:
        match = STAGE_FAILED.match(stop)
        if match:
            reason = match.group(1)
            code = ""
            for item in stops:
                if item is stop or item.startswith("P6_") or item.startswith("phase="):
                    continue
                if CODE_PATH.match(item):
                    code = item
                    break
            return reason, code
    return None, ""


def classify(structural, instrumentation_ok, unknown_cost_calls=0):
    """VALID, REFUSED (a measurement) or INFRASTRUCTURE, plus the CODE:path and a short reason."""
    if unknown_cost_calls:
        return INFRASTRUCTURE, "", "UNKNOWN_COST"
    if structural is None:
        return INFRASTRUCTURE, "", "NO_STRUCTURAL_OUTPUT"
    if structural.get("valid") is True:
        if not instrumentation_ok:
            return INFRASTRUCTURE, "", "HARNESS_FAILED_AFTER_VALID_OUTPUT"
        return VALID, "", ""
    if structural.get("stage") == "API_V1":
        # EDITORIAL_API_V1 has no refusal codes: a wrong pair is a finding about the input, everything else
        # (technical failure after the one retry, cost cap, cancel) stops the group
        if structural.get("reasonCode") == "WRONG_PAIR" and instrumentation_ok:
            return REFUSED, "API_V1_WRONG_PAIR", "WRONG_PAIR"
        return INFRASTRUCTURE, "", "API_V1_" + str(structural.get("reasonCode", ""))
    reason, code = refusal_detail(structural)
    if reason is None:
        return INFRASTRUCTURE, "", "NOT_A_TYPED_STAGE_STOP:" + str(structural.get("reasonCode", ""))
    if reason.startswith(INFRA_REASON_PREFIXES) or "BUDGET" in reason or "TOKEN_OR_COST" in reason:
        return INFRASTRUCTURE, code, reason
    if reason.startswith(REFUSAL_REASON_PREFIXES):
        if structural.get("measuredRefusal") is not True:
            return INFRASTRUCTURE, code, "REFUSAL_NOT_RECORDED_AS_MEASUREMENT"
        return REFUSED, code or reason, reason
    return INFRASTRUCTURE, code, reason


def code_of(entry_code):
    """The CODE part of CODE:path (systematic defects share the code, not the path)."""
    return entry_code.split(":", 1)[0] if entry_code else ""


def decide(history, outcome, code, remaining_ok=True):
    """history: previous entries [{outcome, code}] in execution order; returns (action, reason)."""
    entries = list(history) + [{"outcome": outcome, "code": code}]
    if outcome == INFRASTRUCTURE:
        return "STOP", "INFRASTRUCTURE"
    if not remaining_ok:
        return "STOP", "GROUP_CAP"
    tail = entries[-CONSECUTIVE_SAME_CODE_LIMIT:]
    if (len(tail) == CONSECUTIVE_SAME_CODE_LIMIT and all(item["outcome"] == REFUSED for item in tail)
            and len({code_of(item["code"]) for item in tail}) == 1 and code_of(tail[0]["code"])):
        return "STOP", "THREE_CONSECUTIVE_REFUSALS_WITH_THE_SAME_CODE:" + code_of(tail[0]["code"])
    return "CONTINUE", ""


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--structural", help="structural.json of the fixture just run (omit when it was not written)")
    parser.add_argument("--metadata", help="run-metadata.json of the fixture")
    parser.add_argument("--instrumentation-exit", type=int, required=True)
    parser.add_argument("--history", required=True, help="JSON lines file of earlier decisions")
    parser.add_argument("--fixture", required=True)
    parser.add_argument("--remaining-ok", choices=("yes", "no"), default="yes")
    args = parser.parse_args(argv)
    structural = None
    if args.structural and os.path.exists(args.structural):
        with open(args.structural, encoding="utf-8") as handle:
            structural = json.load(handle)
    unknown = 0
    if args.metadata and os.path.exists(args.metadata):
        with open(args.metadata, encoding="utf-8") as handle:
            unknown = int(json.load(handle).get("unknownCostCalls", 0))
    history = []
    if os.path.exists(args.history):
        with open(args.history, encoding="utf-8") as handle:
            history = [json.loads(line) for line in handle if line.strip()]
    outcome, code, why = classify(structural, args.instrumentation_exit == 0 or
                                  (structural is not None and structural.get("measuredRefusal") is True), unknown)
    action, stop_reason = decide(history, outcome, code, args.remaining_ok == "yes")
    entry = {"fixture": args.fixture, "outcome": outcome, "code": code, "why": why, "action": action, "stopReason": stop_reason}
    with open(args.history, "a", encoding="utf-8", newline="\n") as handle:
        handle.write(json.dumps(entry, sort_keys=True, ensure_ascii=False) + "\n")
    print(json.dumps(entry, sort_keys=True, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
