#!/usr/bin/env python3
"""Verify private emulator outputs, exact prompt captures, spend integrity, and C3 leak probes."""
import argparse
import decimal
import hashlib
import json
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import score_run  # noqa: E402

L1_PROMPT_PHASES = ("L1_RAW_DISCOVERY", "L1_RECONCILE")
L2_PROMPT_PHASES = ("L2_RAW_DISCOVERY", "L2_EDIT", "L2_FINAL_READ")
L3_PROMPT_PHASES = ("L3_RAW_FIRST_REAUDIT", "L3_RECONCILE", "L3_FINAL_READ")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)


def fixture_output(root, fixture_id):
    matches = []
    for current, dirs, files in os.walk(root):
        if os.path.basename(current) == fixture_id and "final.txt" in files:
            matches.append(current)
    if not matches:
        raise ValueError("expected one output directory for " + fixture_id)
    canonical = os.path.abspath(os.path.join(root, fixture_id))
    selected = next((path for path in matches if os.path.abspath(path) == canonical), matches[0])
    if len(matches) > 1:
        expected = tree_sha256s(selected)
        for candidate in matches:
            if tree_sha256s(candidate) != expected:
                raise ValueError("duplicate output directories differ for " + fixture_id)
        print("byte-identical duplicate output copies:", fixture_id, len(matches))
    return selected


def tree_sha256s(root):
    result = {}
    for current, dirs, files in os.walk(root):
        for name in files:
            path = os.path.join(current, name)
            relative = os.path.relpath(path, root).replace(os.sep, "/")
            with open(path, "rb") as handle:
                result[relative] = sha(handle.read())
    return result


def check_ledger(path):
    prior = "0" * 64
    calls = {}
    cap = None
    entries = 0
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                raise ValueError("empty spend-ledger row")
            row = json.loads(line, parse_float=decimal.Decimal)
            digest = row.pop("entryHash", None)
            body = canonical(row).encode("utf-8")
            if digest != sha(body) or row.get("previousHash") != prior:
                raise ValueError("spend-ledger hash chain mismatch")
            entries += 1
            if row.get("sequence") != entries:
                raise ValueError("spend-ledger sequence mismatch")
            if cap is None:
                cap = decimal.Decimal(row["groupMaximumUsd"])
            elif cap != decimal.Decimal(row["groupMaximumUsd"]):
                raise ValueError("spend-ledger group cap changed")
            call_id = row["callId"]
            amount = decimal.Decimal(row["amountUsd"])
            if row["kind"] == "RESERVE":
                if call_id in calls:
                    raise ValueError("duplicate spend reservation")
                calls[call_id] = {"phase": row["phase"], "reserved": amount, "settled": None}
            elif row["kind"] == "SETTLE":
                call = calls.get(call_id)
                if call is None or call["settled"] is not None or call["phase"] != row["phase"]:
                    raise ValueError("invalid spend settlement")
                if amount > call["reserved"]:
                    raise ValueError("settled spend exceeded reservation")
                call["settled"] = amount
            else:
                raise ValueError("unknown spend-ledger event")
            prior = digest
    if entries == 0 or cap is None:
        raise ValueError("spend ledger is empty")
    pending = [value for value in calls.values() if value["settled"] is None]
    exposure = sum((value["reserved"] if value["settled"] is None else value["settled"]
                    for value in calls.values()), decimal.Decimal(0))
    if pending or exposure > cap:
        raise ValueError("spend ledger has pending or over-cap exposure")
    return {"entries": entries, "calls": len(calls), "pending": len(pending), "exposureUsd": str(exposure), "capUsd": str(cap)}


def expected_prompt_phases(mode, reused_l1=False):
    if mode == "L1_ONLY":
        return list(L1_PROMPT_PHASES)
    if mode == "L1_THEN_L2" and reused_l1:
        return list(L2_PROMPT_PHASES)
    if mode in ("L2_ONLY", "L1_THEN_L2"):
        return list(L1_PROMPT_PHASES + L2_PROMPT_PHASES)
    if mode in ("L3_ONLY", "CHAIN"):
        return list(L1_PROMPT_PHASES + L2_PROMPT_PHASES + L3_PROMPT_PHASES)
    raise ValueError("unsupported mode for prompt capture verification")


def expected_live_call_counts(mode):
    actual_calls = {"L1_ONLY": 2, "L3_ONLY": 3, "L1_THEN_L2": 3, "CHAIN": 8}
    pipeline_calls = {"L1_ONLY": 2, "L3_ONLY": 8, "L1_THEN_L2": 3, "CHAIN": 8}
    if mode not in actual_calls:
        raise ValueError("unsupported live fixture mode")
    return actual_calls[mode], pipeline_calls[mode]


def check_prompt_captures(output, mode, reused_l1=False, invalid_l1=False, partial=False):
    prompt_root = os.path.join(output, "prompts")
    if not os.path.isdir(prompt_root):
        raise ValueError("prompt capture directory is missing")
    files = sorted(os.listdir(prompt_root))
    if any(not os.path.isfile(os.path.join(prompt_root, name)) or not name.endswith(".txt")
           for name in files):
        raise ValueError("prompt capture directory contains an unexpected entry")
    expected = (["L1_RAW_DISCOVERY"] if invalid_l1 else expected_prompt_phases(mode, reused_l1))
    captured = []
    for sequence, (name, phase) in enumerate(zip(files, expected), start=1):
        match = re.fullmatch(r"([0-9]{3})-([A-Z0-9_]{1,48})\.txt", name)
        if not match or int(match.group(1)) != sequence or match.group(2) != phase:
            raise ValueError("prompt capture phase set or order differs from the selected mode")
        with open(os.path.join(prompt_root, name), "rb") as handle:
            prompt_bytes = handle.read()
        if not prompt_bytes:
            raise ValueError("captured prompt is empty")
        captured.append(prompt_bytes)
    if len(files) != len(expected) and not (partial and 0 < len(files) < len(expected)):
        raise ValueError("prompt capture phase count differs from the selected mode")
    legacy_path = os.path.join(output, "l2-edit-prompt.txt")
    if "L2_EDIT" in expected:
        with open(legacy_path, "rb") as handle:
            if handle.read() != captured[expected.index("L2_EDIT")]:
                raise ValueError("legacy L2 prompt copy differs from the captured dispatch bytes")
    elif os.path.exists(legacy_path):
        raise ValueError("unexpected L2 edit prompt exists for a mode that did not run L2")
    return captured


def check_oracle_probe(fixture, output, fixtures_root, mode, reused_l1=False):
    names = ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv")
    source_parts = []
    for name in names:
        with open(os.path.join(fixtures_root, fixture["id"], name), "rb") as handle:
            source_parts.append(handle.read())
    source = b"\n".join(source_parts)
    with open(os.path.join(output, "report-l1.json"), "rb") as handle:
        model_inputs = [handle.read()]
    model_inputs.extend(check_prompt_captures(output, mode, reused_l1))
    combined = b"\n".join(model_inputs)
    if b"mustContain" in combined or b"mustNotContain" in combined:
        raise ValueError(fixture["id"] + ": label field name reached a captured model input")
    labels = score_run.load_json(os.path.join(fixtures_root, fixture["labels"]["path"]))
    for target in labels.get("targets", []):
        if target.get("id", "").encode("utf-8") in combined:
            raise ValueError(fixture["id"] + ": target id reached a captured model input")
        terms = []
        if target.get("old"):
            terms.append(target["old"])
        terms.extend(target.get("mustContain", []))
        for term in terms:
            encoded = term.encode("utf-8") if term else b""
            if encoded and encoded not in source and encoded in combined:
                raise ValueError(fixture["id"] + ": corrected label text reached a captured model input")


def select_fixtures(manifest, fixture_ids):
    fixtures = manifest.get("fixtures", [])
    by_id = {fixture.get("id"): fixture for fixture in fixtures}
    selected_ids = fixture_ids or list(by_id)
    if (not selected_ids or len(selected_ids) != len(set(selected_ids))
            or any(fixture_id not in by_id for fixture_id in selected_ids)):
        raise ValueError("fixture selection must be unique and belong to the frozen fixture set")
    return [by_id[fixture_id] for fixture_id in selected_ids]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixtures-root", required=True)
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--run-dir", required=True)
    parser.add_argument("--mode", choices=("L1_ONLY", "L2_ONLY", "L3_ONLY", "L1_THEN_L2", "CHAIN"), required=True)
    parser.add_argument("--fixture-ids", nargs="+")
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--measure-refusals", action="store_true",
                        help="Z3: a typed refusal of the production engine is a recorded measurement, not a failure")
    parser.add_argument("--expect-invalid-l1", action="store_true",
                        help="accept the explicit fake-only typed L1 stop used by W2")
    args = parser.parse_args()
    fixtures_root, run_dir = os.path.abspath(args.fixtures_root), os.path.abspath(args.run_dir)
    if "6.FINAL" in fixtures_root or "6.FINAL" in run_dir:
        raise ValueError("refusing a path through 6.FINAL")
    manifest = score_run.load_json(args.manifest)
    selected = select_fixtures(manifest, args.fixture_ids)
    if args.live and args.mode == "L2_ONLY":
        raise ValueError("live L2 requires reused G1 L1 state; use L1_THEN_L2")
    if args.expect_invalid_l1 and (args.live or args.mode != "L1_ONLY" or len(selected) != 1):
        raise ValueError("--expect-invalid-l1 is restricted to one offline L1_ONLY fixture")
    reports = []
    for fixture in selected:
        output = fixture_output(os.path.join(run_dir, "results"), fixture["id"])
        direct_output = os.path.join(run_dir, "results", fixture["id"])
        if os.path.abspath(output) != os.path.abspath(direct_output):
            if os.path.exists(direct_output):
                raise ValueError(fixture["id"] + ": duplicate pulled output directory")
            shutil.copytree(output, direct_output)
            output = direct_output
        with open(os.path.join(output, "structural.json"), encoding="utf-8") as handle:
            structural = json.load(handle)
        with open(os.path.join(output, "run-metadata.json"), encoding="utf-8") as handle:
            metadata = json.load(handle)
        if metadata.get("mode") != args.mode:
            raise ValueError(fixture["id"] + ": metadata mode does not match the requested mode")
        if metadata.get("l1ThenL2SameDatabase") != (args.mode == "L1_THEN_L2"):
            raise ValueError(fixture["id"] + ": L1_THEN_L2 database contract marker is invalid")
        expected_stage = {"L1_ONLY": "L1", "L2_ONLY": "L2", "L3_ONLY": "L3",
                          "L1_THEN_L2": "L2", "CHAIN": "CHAIN"}[args.mode]
        if args.expect_invalid_l1:
            if (metadata.get("testOnlyInvalidL1") is not True
                    or metadata.get("providerKind") != "FAKE_OFFLINE"
                    or metadata.get("actualProviderCalls") != 0
                    or metadata.get("fakeProviderCalls") != 1
                    or structural.get("valid") is not False
                    or structural.get("stage") != "L1"
                    or structural.get("providerCalls") != 1
                    or "L1_COVERAGE_GAP:coverage" not in structural.get("stops", [])):
                raise ValueError(fixture["id"] + ": explicit fake invalid-L1 contract failed")
            check_prompt_captures(output, args.mode, invalid_l1=True)
            final = open(os.path.join(output, "final.txt"), "rb").read()
            if sha(final) != metadata.get("finalSha256"):
                raise ValueError(fixture["id"] + ": final hash mismatch")
            if os.path.exists(os.path.join(output, "report-l1.json")):
                raise ValueError(fixture["id"] + ": invalid L1 unexpectedly produced REPORT_L1")
            ledger = check_ledger(os.path.join(output, "spend-ledger.jsonl"))
            reports.append({"fixture": fixture["id"], "providerCalls": structural.get("providerCalls"), "ledger": ledger})
            continue
        if args.measure_refusals and args.live and structural.get("valid") is False and structural.get("measuredRefusal") is True:
            # a live refusal: the engine stopped with a typed reason; the evidence is the prompts sent, the ledger and the stops
            if metadata.get("providerKind") != "LIVE" or metadata.get("actualProviderCalls", 0) <= 0:
                raise ValueError(fixture["id"] + ": a refusal can only be measured on a live provider call")
            if not structural.get("stops"):
                raise ValueError(fixture["id"] + ": a measured refusal must carry its stops")
            final = open(os.path.join(output, "final.txt"), "rb").read()
            if sha(final) != metadata.get("finalSha256"):
                raise ValueError(fixture["id"] + ": final hash mismatch")
            check_prompt_captures(output, args.mode, metadata.get("l1ReusedFromPriorGroup") is True, partial=True)
            ledger = check_ledger(os.path.join(output, "spend-ledger.jsonl"))
            reports.append({"fixture": fixture["id"], "providerCalls": structural.get("providerCalls"), "ledger": ledger,
                            "refusal": structural.get("stops")})
            continue
        if not structural.get("valid") or structural.get("stage") != expected_stage:
            raise ValueError(fixture["id"] + ": production structural contract failed")
        if args.live:
            if metadata.get("providerKind") != "LIVE" or metadata.get("actualProviderCalls", 0) <= 0:
                raise ValueError(fixture["id"] + ": expected a live provider run")
            expected_live_calls, expected_pipeline_calls = expected_live_call_counts(args.mode)
            if (metadata.get("actualProviderCalls") != expected_live_calls
                    or structural.get("providerCalls") != expected_pipeline_calls):
                raise ValueError(fixture["id"] + ": live provider call count differs from the approved mode")
            if args.mode == "L1_THEN_L2" and metadata.get("l1ReusedFromPriorGroup") is not True:
                raise ValueError(fixture["id"] + ": live L1_THEN_L2 did not reuse G1 L1 state")
        elif metadata.get("providerKind") != "FAKE_OFFLINE" or metadata.get("actualProviderCalls") != 0:
            raise ValueError(fixture["id"] + ": a non-fake provider ran")
        else:
            expected_fake_calls = (3 if args.mode == "L1_THEN_L2"
                                   and metadata.get("l1ReusedFromPriorGroup") is True
                                   else {"L1_ONLY": 2, "L2_ONLY": 5, "L3_ONLY": 8,
                                         "L1_THEN_L2": 5, "CHAIN": 8}[args.mode])
            if (metadata.get("fakeProviderCalls") != expected_fake_calls
                    or structural.get("providerCalls") != expected_fake_calls):
                raise ValueError(fixture["id"] + ": fake provider call count differs from the expected mode")
        final = open(os.path.join(output, "final.txt"), "rb").read()
        if sha(final) != metadata.get("finalSha256"):
            raise ValueError(fixture["id"] + ": final hash mismatch")
        with open(os.path.join(output, "report-l1.json"), encoding="utf-8") as handle:
            report = json.load(handle)
        if report.get("artifactType") != "REPORT_L1" or report.get("phase") != "L1_RECONCILE":
            raise ValueError(fixture["id"] + ": REPORT_L1 evidence is invalid")
        check_oracle_probe(fixture, output, fixtures_root, args.mode,
                           metadata.get("l1ReusedFromPriorGroup") is True)
        ledger = check_ledger(os.path.join(output, "spend-ledger.jsonl"))
        reports.append({"fixture": fixture["id"], "providerCalls": structural.get("providerCalls"), "ledger": ledger})

    report_path = os.path.join(run_dir, "score-report.json")
    selected_manifest = os.path.join(run_dir, "selected-fixture-manifest.json")
    with open(selected_manifest, "w", encoding="utf-8", newline="\n") as handle:
        json.dump({"fixtures": selected}, handle, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        handle.write("\n")
    result_code = score_run.main(["--fixtures-root", fixtures_root, "--run-dir", os.path.join(run_dir, "results"),
                                  "--manifest", selected_manifest, "--out", report_path])
    if result_code != 0:
        raise ValueError("frozen scorer refused the run")
    score = score_run.load_json(report_path)
    summary = score["summary"]
    if summary["fixtures"] != len(selected):
        raise ValueError("scorer did not read every fixture output")
    expected_structural = "0/1" if args.expect_invalid_l1 else f"{len(selected)}/{len(selected)}"
    if args.measure_refusals:
        refused = sum(1 for item in reports if item.get("refusal"))
        expected_structural = f"{len(selected) - refused}/{len(selected)}"
    if summary["STRUCTURAL_VALID"] != expected_structural:
        raise ValueError("scorer found an unexpected structural result")
    print("fixtures:", summary["fixtures"])
    print("STRUCTURAL_VALID:", summary["STRUCTURAL_VALID"])
    print("SEMANTIC_EVAL:", json.dumps(summary["SEMANTIC_EVAL"], sort_keys=True))
    print("actual provider calls:", "reported in run metadata" if args.live else "0; provider responses were generated by local fakes")
    print("spend-ledger entries:", sum(item["ledger"]["entries"] for item in reports),
          "pending UNKNOWN reservations: 0")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError) as error:
        print("fixture verification failed:", str(error), file=sys.stderr)
        sys.exit(1)
