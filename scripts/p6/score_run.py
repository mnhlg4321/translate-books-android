#!/usr/bin/env python3
"""P6 R5 scorer for fixture runs.

Two verdicts are always reported separately and never merged:

* STRUCTURAL_VALID - the engine's own validators accepted the committed artifacts of the run
  (read from <fixture>/structural.json, written by the harness that ran the production engine).
* SEMANTIC_EVAL - machine-checkable invariants against the frozen labels (this file), plus the human rubric
  when one is required. A known defect that is still in the final text is a semantic failure even when every
  artifact is structurally valid.

Nothing here calls a provider. Fixture text and labels live outside Git (D:\\P5E-private\\p6-fixtures); the
run directory is private as well. Usage:

  score_run.py --fixtures-root D:\\P5E-private\\p6-fixtures --run-dir D:\\P5E-private\\p6-runs\\<id> [--out report.json]

Run directory layout, one folder per fixture id:
  final.txt            the text the run ends with (FINAL for chains, VI_L2 for L2-only runs, DRAFT-equivalent for L1-only)
  structural.json      {"valid": bool, "reasonCode": str, "stage": "L1|L2|L3|CHAIN", "providerCalls": int, "stops": [str]}
  human_scores.json    optional: {"items": {"<targetId>": 0|1|2, ...}, "sampleOk": 0|1|2, "voiceOk": 0|1|2}
"""
import argparse
import difflib
import hashlib
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
THRESHOLDS = os.path.join(HERE, "thresholds.json")
STATES_FIXED = ("RESTORED_EXACT", "FIXED_MATCH")


def read_lines(path):
    with open(path, encoding="utf-8", newline="") as handle:
        text = handle.read()
    if text.startswith("\ufeff"):
        text = text[1:]
    return [line[:-1] if line.endswith("\r") else line for line in text.split("\n")]


def sha256_file(path):
    with open(path, "rb") as handle:
        return hashlib.sha256(handle.read()).hexdigest()


def load_json(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def target_lines(target, config):
    """DRAFT line numbers a target is about."""
    if "line" in target:
        return [int(target["line"])]
    rule_lines = config.get("ruleLines", {}).get(target.get("id"))
    if rule_lines:
        return [int(n) for n in rule_lines]
    match = re.search(r"DRAFT line (\d+)", target.get("scope", "") or "")
    return [int(match.group(1))] if match else []


def region_for(line_no, opcodes, final):
    """Final text that stands where DRAFT line `line_no` was, with the opcode indexes it used."""
    used = []
    texts = []
    state = "UNCHANGED"
    for index, (tag, i1, i2, j1, j2) in enumerate(opcodes):
        if tag != "equal" and i2 > i1 and i1 <= line_no - 1 < i2:
            used.append(index)
            texts.extend(final[j1:j2])
            state = "CHANGED"
        elif tag == "insert" and i1 in (line_no - 1, line_no):
            # content added next to the line (a restored sentence may come back as its own line)
            used.append(index)
            texts.extend(final[j1:j2])
            state = "CHANGED"
    return state, texts, used


def judge_target(target, config, draft, opcodes, final, final_text):
    lines = target_lines(target, config)
    result = {"id": target.get("id"), "class": target.get("class"), "severity": target.get("severity"), "lines": lines}
    used_all = []
    must = target.get("mustContain", [])
    must_not = target.get("mustNotContain", [])
    if target.get("rule") == "tohha-must-differ-from-kouryaku":
        changed = 0
        for line in lines:
            state, _texts, used = region_for(line, opcodes, final)
            used_all.extend(used)
            changed += state == "CHANGED"
        result["changedLines"] = changed
        result["expectedLines"] = len(lines)
        result["state"] = "FIXED_MATCH" if changed == len(lines) else ("PARTIAL" if changed else "MISSED")
        result["needsHuman"] = True
        result["claimed"] = used_all
        return result
    regions = []
    for line in lines:
        state, texts, used = region_for(line, opcodes, final)
        used_all.extend(used)
        regions.append((state, texts))
    result["claimed"] = used_all
    if not lines:
        # a label without a line: judge on the whole text
        ok = all(m in final_text for m in must) and not any(m in final_text for m in must_not)
        result["state"] = "FIXED_MATCH" if ok else "STILL_DEFECTIVE"
        result["needsHuman"] = False
        return result
    if all(state == "UNCHANGED" for state, _t in regions):
        # the known defect is still there: a structural PASS cannot rescue this
        result["state"] = "MISSED"
        result["needsHuman"] = False
        return result
    region_text = "\n".join(t for _s, texts in regions for t in texts)
    has_all = all(m in region_text for m in must)
    has_bad = any(m in region_text for m in must_not)
    old = target.get("old")
    if has_all and not has_bad:
        result["state"] = "RESTORED_EXACT" if old is not None and old in region_text.split("\n") else "FIXED_MATCH"
        # a label with mustContain / mustNotContain is fully machine-checkable; only guesses and rules go to a person
        result["needsHuman"] = False
    elif has_bad:
        result["state"] = "STILL_DEFECTIVE"
        result["needsHuman"] = False
    else:
        result["state"] = "CHANGED_UNVERIFIED"
        result["needsHuman"] = True
    return result


def api_v1_block(run):
    """EDITORIAL_API_V1 extras read from run-metadata.json (present only for API_V1 runs).

    New errors introduced by the edit are NOT_MEASURED here: they are adjudicated by a person afterwards, exactly like the
    P6 G1 adjudication, and are never guessed by the scorer.
    """
    meta_path = os.path.join(run, "run-metadata.json")
    if not os.path.exists(meta_path):
        return None
    meta = load_json(meta_path)
    api = meta.get("apiV1")
    if not isinstance(api, dict):
        return None
    return {"newErrors": "NOT_MEASURED", "rewriteRatio": api.get("rewriteRatio"), "guardFlags": list(api.get("guardFlags", [])),
            "calls": api.get("calls"), "mode": api.get("mode"), "state": api.get("state"),
            "inputTokens": meta.get("inputTokens"), "outputTokens": meta.get("outputTokens"), "usd": meta.get("usd")}


def score_fixture(fixture, root, run_dir, thresholds):
    fid = fixture["id"]
    kind = fixture["kind"]
    fx_dir = os.path.join(root, fid)
    labels = load_json(os.path.join(root, fixture["labels"]["path"]))
    config = thresholds.get("fixtureConfig", {}).get(fid, {})
    run = os.path.join(run_dir, fid)
    report = {"fixture": fid, "kind": kind}

    structural_path = os.path.join(run, "structural.json")
    if os.path.exists(structural_path):
        structural = load_json(structural_path)
        report["STRUCTURAL_VALID"] = {"valid": bool(structural.get("valid")), "reasonCode": structural.get("reasonCode", ""),
                                      "stage": structural.get("stage", ""), "providerCalls": structural.get("providerCalls"),
                                      "stops": structural.get("stops", [])}
    else:
        report["STRUCTURAL_VALID"] = {"valid": False, "reasonCode": "STRUCTURAL_RESULT_MISSING"}

    api = api_v1_block(run)
    if api is not None:
        report["API_V1"] = api

    final_path = os.path.join(run, "final.txt")
    if not os.path.exists(final_path):
        report["SEMANTIC_EVAL"] = {"machine": "FAIL", "reasons": ["FINAL_TEXT_MISSING"], "human": "NOT_REQUIRED", "verdict": "FAIL"}
        return report
    draft = read_lines(os.path.join(fx_dir, "DRAFT.txt"))
    final = read_lines(final_path)
    final_text = "\n".join(final)
    opcodes = difflib.SequenceMatcher(None, draft, final, autojunk=False).get_opcodes()

    targets = [judge_target(t, config, draft, opcodes, final, final_text) for t in labels.get("targets", [])]
    claimed = set()
    for t in targets:
        claimed.update(t.pop("claimed", []))

    allowed = set(config.get("allowedExtraDraftLines", [])) | set(labels.get("paraphraseLines", []))
    collateral = 0
    ambiguous = 0
    for index, (tag, i1, i2, j1, j2) in enumerate(opcodes):
        if tag == "equal" or index in claimed:
            continue
        span = list(range(i1 + 1, i2 + 1)) or [i1 + 1]
        size = max(i2 - i1, j2 - j1)
        if all(n in allowed for n in span) and (i2 > i1):
            ambiguous += size
        else:
            collateral += size

    fixed = sum(1 for t in targets if t["state"] in STATES_FIXED)
    missed = [t["id"] for t in targets if t["state"] in ("MISSED", "STILL_DEFECTIVE")]
    reasons = []
    limits = thresholds["limits"]
    if kind in ("clean", "ambiguous"):
        if collateral > limits["cleanControlSemanticEditsMax"]:
            reasons.append("CLEAN_CONTROL_EDITED:%d" % collateral)
    elif kind == "seeded":
        if len(targets) == 1:
            if targets[0]["state"] not in STATES_FIXED and targets[0]["state"] != "CHANGED_UNVERIFIED":
                reasons.append("KNOWN_DEFECT_NOT_FIXED:%s" % targets[0]["id"])
        else:
            recall = fixed / len(targets) if targets else 1.0
            if recall < limits["multiDefectRecallMin"]:
                reasons.append("MULTI_DEFECT_RECALL_LOW:%.2f" % recall)
        if collateral > limits["collateralMax"]["seeded"]:
            reasons.append("COLLATERAL_TOO_HIGH:%d" % collateral)
    elif kind == "holdout":
        recall = fixed / len(targets) if targets else 1.0
        if recall < limits["holdoutRecallMin"]:
            reasons.append("HOLDOUT_RECALL_LOW:%.2f" % recall)
        if collateral > limits["collateralMax"]["holdout"]:
            reasons.append("COLLATERAL_TOO_HIGH:%d" % collateral)
    elif kind == "real":
        for t in targets:
            if t["id"] == "I-002":
                if t["state"] != "FIXED_MATCH":
                    reasons.append("KNOWN_DEFECT_NOT_FIXED:I-002")
            elif t["state"] not in STATES_FIXED:
                reasons.append("KNOWN_DEFECT_NOT_FIXED:%s" % t["id"])
        if collateral > limits["collateralMax"]["real"]:
            reasons.append("COLLATERAL_TOO_HIGH:%d" % collateral)

    # a person also reads every edit the labels do not account for, and the whole real chapter
    needs_human = (any(t.get("needsHuman") for t in targets) or kind == "real" or collateral > 0 or ambiguous > 0)
    human = "NOT_REQUIRED"
    human_path = os.path.join(run, "human_scores.json")
    if needs_human:
        human = "PENDING"
        if os.path.exists(human_path):
            scores = load_json(human_path)
            items = scores.get("items", {})
            required = [t["id"] for t in targets if t.get("needsHuman")]
            values = [items.get(i) for i in required] + [scores.get("sampleOk"), scores.get("voiceOk")]
            if any(v is None for v in values):
                human = "PENDING"
            else:
                rubric = thresholds["humanRubric"]
                ok = all(v >= rubric["itemMin"] for v in values) and (sum(values) / len(values)) >= rubric["meanMin"]
                human = "PASS" if ok else "FAIL"

    machine = "PASS" if not reasons else "FAIL"
    verdict = "FAIL" if machine == "FAIL" or human == "FAIL" else ("PENDING_HUMAN" if human == "PENDING" else "PASS")
    report["SEMANTIC_EVAL"] = {
        "machine": machine, "reasons": reasons, "human": human, "verdict": verdict,
        "targets": targets, "targetsFixed": fixed, "targetsTotal": len(targets), "missedOrStillDefective": missed,
        "collateralChangedLines": collateral, "ambiguousEditedLines": ambiguous,
        "finalSha256": sha256_file(final_path),
    }
    return report


def check_leaks(root, manifest):
    """Oracle-leak guard on the inputs a model would see: no path through 6.FINAL and no corrected line in them."""
    problems = []
    for fixture in manifest["fixtures"]:
        fx_dir = os.path.join(root, fixture["id"])
        for name in ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv"):
            path = os.path.join(fx_dir, name)
            if "6.FINAL" in os.path.abspath(path):
                problems.append("%s: input path goes through 6.FINAL" % fixture["id"])
        labels = load_json(os.path.join(root, fixture["labels"]["path"]))
        visible = ""
        for name in ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv"):
            with open(os.path.join(fx_dir, name), encoding="utf-8") as handle:
                visible += handle.read() + "\n"
        for target in labels.get("targets", []):
            old = target.get("old")
            # only a whole corrected sentence is an oracle; a short fragment can legitimately come from the glossary
            # an additive mutation (the corrected line is a prefix or part of the mutated one) cannot be an oracle
            if (fixture["kind"] in ("seeded", "holdout") and old and len(old.strip()) >= 25 and old in visible
                    and old not in (target.get("new") or "")):
                problems.append("%s: corrected text of %s is visible to the model" % (fixture["id"], target["id"]))
            for key in ("humanRubric", "rule"):
                value = target.get(key)
                if value and value in visible:
                    problems.append("%s: label text %s appears in a model input" % (fixture["id"], key))
    return problems


def summarize(reports):
    structural = sum(1 for r in reports if r["STRUCTURAL_VALID"]["valid"])
    semantic = {"PASS": 0, "FAIL": 0, "PENDING_HUMAN": 0}
    for r in reports:
        semantic[r["SEMANTIC_EVAL"]["verdict"]] += 1
    return {"fixtures": len(reports), "STRUCTURAL_VALID": "%d/%d" % (structural, len(reports)),
            "SEMANTIC_EVAL": semantic,
            "note": "STRUCTURAL_VALID and SEMANTIC_EVAL are separate measurements and are never combined"}


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixtures-root", required=True)
    parser.add_argument("--run-dir", required=True)
    parser.add_argument("--manifest", default=os.path.join(HERE, "..", "..", "docs", "P6_R0_FIXTURE_MANIFEST.json"))
    parser.add_argument("--only", nargs="*", default=None)
    parser.add_argument("--out", default=None)
    args = parser.parse_args(argv)
    if "6.FINAL" in os.path.abspath(args.fixtures_root) or "6.FINAL" in os.path.abspath(args.run_dir):
        print("refusing a path through 6.FINAL", file=sys.stderr)
        return 2
    thresholds = load_json(THRESHOLDS)
    manifest = load_json(args.manifest)
    leaks = check_leaks(args.fixtures_root, manifest)
    if leaks:
        print(json.dumps({"leaks": leaks}, indent=1))
        return 3
    reports = []
    for fixture in manifest["fixtures"]:
        if args.only and fixture["id"] not in args.only:
            continue
        if not os.path.isdir(os.path.join(args.run_dir, fixture["id"])):
            continue
        reports.append(score_fixture(fixture, args.fixtures_root, args.run_dir, thresholds))
    output = {"schema": "p6.r5.score-report.v1", "thresholdsSha256": sha256_file(THRESHOLDS),
              "summary": summarize(reports), "reports": reports}
    text = json.dumps(output, indent=1, ensure_ascii=False)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(text)
    else:
        print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
