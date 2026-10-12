"""Method-level PASS / SKIP / FAIL summary of `adb shell am instrument -r` output.

The runner's closing line `OK (N tests)` counts every method that was started, including those that stopped on a JUnit
assumption (opt-in tests that were not selected). Reporting that N as "N passed" overstates what ran, so this reads the raw
per-method status codes instead:

    0  OK                   -> PASS
    -3 ignored               -> SKIP
    -4 assumption failure    -> SKIP  (an opt-in or phase test that was not selected)
    -1 error / -2 failure    -> FAIL
    1  started with no later status -> INCOMPLETE (counted as FAIL: the method never reported a result)

A log with no method statuses at all, or whose `numtests` does not match the methods seen, is reported as INCOMPLETE
rather than silently passing.

Usage: python instrumentation_summary.py LOG [LOG ...] [--json OUT]
"""
import argparse
import json
import re
import sys

PASS, SKIP, FAIL, INCOMPLETE = "PASS", "SKIP", "FAIL", "INCOMPLETE"
CODE_TO_RESULT = {0: PASS, -3: SKIP, -4: SKIP, -1: FAIL, -2: FAIL}

_STATUS = re.compile(r"^INSTRUMENTATION_STATUS: (\w+)=(.*)$")
_CODE = re.compile(r"^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$")


def parse(text):
    """Return {"methods": [(class, test, result)], "numtests": int|None, "ok_line": int|None}."""
    text = text.lstrip("﻿").replace("\r\n", "\n").replace("\r", "\n")
    block = {}
    last_key = None
    started = {}  # (class, test) -> True while no final status was seen
    finished = {}  # (class, test) -> result of the last final status
    order = []
    numtests = None
    protocol_errors = []
    for line in text.split("\n"):
        m = _STATUS.match(line)
        if m:
            last_key = m.group(1)
            block[last_key] = m.group(2)
            continue
        c = _CODE.match(line)
        if c:
            code = int(c.group(1))
            cls, test = block.get("class", ""), block.get("test", "")
            if block.get("numtests", "").isdigit():
                numtests = int(block["numtests"])
            if test:
                key = (cls, test)
                if key not in started and key not in finished:
                    order.append(key)
                if code == 1:
                    if key in started or key in finished:
                        protocol_errors.append("repeated start for %s#%s; separate runs into separate logs" % key)
                    if key not in finished:
                        started[key] = True
                elif code in CODE_TO_RESULT:
                    if key in finished:
                        protocol_errors.append("duplicate result for %s#%s" % key)
                    result = CODE_TO_RESULT[code]
                    finished[key] = FAIL if finished.get(key) == FAIL else result
                    started.pop(key, None)
                else:
                    finished[key] = FAIL  # an unknown status code is never a pass
                    started.pop(key, None)
            block, last_key = {}, None
            continue
    methods = []
    for key in order:
        result = finished.get(key, INCOMPLETE)  # started, never reported
        methods.append((key[0], key[1], result))
    ok = re.search(r"^OK \((\d+) tests?\)", text, re.M)
    return {"methods": methods, "numtests": numtests, "ok_line": int(ok.group(1)) if ok else None,
            "failures_line": bool(re.search(r"^FAILURES!!!", text, re.M)), "protocol_errors": protocol_errors}


def summarize(text):
    parsed = parse(text)
    counts = {PASS: 0, SKIP: 0, FAIL: 0, INCOMPLETE: 0}
    for _, _, result in parsed["methods"]:
        counts[result] += 1
    discovered = len(parsed["methods"])
    notes = list(parsed["protocol_errors"])
    if discovered == 0:
        notes.append("no method status found")
    if parsed["numtests"] is not None and parsed["numtests"] != discovered:
        notes.append("numtests=%d but %d methods reported" % (parsed["numtests"], discovered))
    if parsed["ok_line"] is not None and parsed["ok_line"] != discovered:
        notes.append("OK line says %d but %d methods reported" % (parsed["ok_line"], discovered))
    if parsed["failures_line"] and counts[FAIL] == 0:
        notes.append("FAILURES!!! line without a failed method status")
    count_mismatch = parsed["numtests"] is not None and parsed["numtests"] != discovered
    ok_mismatch = parsed["ok_line"] is not None and parsed["ok_line"] != discovered
    if discovered == 0 or counts[INCOMPLETE] or count_mismatch or ok_mismatch or parsed["protocol_errors"]:
        overall = INCOMPLETE
    elif counts[FAIL] or parsed["failures_line"]:
        overall = FAIL
    elif counts[PASS] == 0:
        overall = SKIP
    else:
        overall = PASS
    return {"overall": overall, "discovered": discovered, "pass": counts[PASS], "skip": counts[SKIP], "fail": counts[FAIL],
            "incomplete": counts[INCOMPLETE], "ok_line_tests": parsed["ok_line"], "notes": notes,
            "methods": [{"class": c, "test": t, "result": r} for c, t, r in parsed["methods"]]}


def read_log(path):
    with open(path, "rb") as stream:
        raw = stream.read()
    for enc in ("utf-8-sig", "utf-16"):
        try:
            return raw.decode(enc)
        except UnicodeError:
            continue
    return raw.decode("utf-8", "replace")


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("logs", nargs="+")
    ap.add_argument("--json")
    args = ap.parse_args(argv)
    out = {}
    total = {"discovered": 0, "pass": 0, "skip": 0, "fail": 0, "incomplete": 0}
    run_failed = False
    for path in args.logs:
        s = summarize(read_log(path))
        out[path] = s
        run_failed = run_failed or s["overall"] in (FAIL, INCOMPLETE)
        for k in total:
            total[k] += s[k]
        print("%-12s discovered=%d pass=%d skip=%d fail=%d incomplete=%d  %s" % (
            s["overall"], s["discovered"], s["pass"], s["skip"], s["fail"], s["incomplete"], path))
        for n in s["notes"]:
            print("             note: " + n)
    print("TOTAL discovered=%(discovered)d pass=%(pass)d skip=%(skip)d fail=%(fail)d incomplete=%(incomplete)d" % total)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump({"logs": out, "total": total}, fh, indent=1, ensure_ascii=False)
    return 1 if run_failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
