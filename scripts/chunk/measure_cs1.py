#!/usr/bin/env python3
"""Measure CS-1 (the Java engine's chunk planner) on the owner's private library and compare with the acceptance numbers.

It runs the opt-in JVM test Cs1CorpusMeasurementTest with CS1_CORPUS pointing at the folder that holds
'JAKUAKU MONSTER' and 'JAKUAKU MONSTER WN', reads the JSON the test writes, and prints counts only (no book text).
Acceptance (docs/EDITORIAL_CHUNK_PLAN.md, 3.4): 133/133 cuts correct on 18 chapters with a FINAL-derived answer; 55/55
wrong-chapter pairs BLOCK with and without glossary; 0/113 correct pairs BLOCK; other-edition pairs: at most 1 of 11 OK.
"""
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CORPUS = os.environ.get("CS1_CORPUS", r"D:\Ebooks")
OUT = ROOT / "editorial-engine" / "build" / "cs1-measure.json"
PERF_OUT = ROOT / "app" / "build" / "cs1-performance.json"


def run_engine() -> None:
    env = dict(os.environ)
    env["CS1_CORPUS"] = CORPUS
    env["CS1_OUT"] = str(OUT)
    env.setdefault("JAVA_HOME", r"C:\Program Files\Android\Android Studio\jbr")
    OUT.unlink(missing_ok=True)
    cmd = ["cmd", "/c", r".\gradlew.bat :editorial-engine:cleanTest :editorial-engine:test --tests "
           "com.ml.tblandroidtxt.editorial.api.chunk.Cs1CorpusMeasurementTest --console=plain"]
    done = subprocess.run(cmd, cwd=ROOT, env=env, capture_output=True, text=True)
    if done.returncode != 0 or not OUT.exists():
        print((done.stdout + done.stderr)[-1500:])
        raise SystemExit("engine measurement failed")


def run_performance() -> None:
    """Chunk counts with the app's default Performance settings and the app's own Chunker (an opt-in app unit test)."""
    env = dict(os.environ)
    env["CS1_CORPUS"] = CORPUS
    env["CS1_PERF_OUT"] = str(PERF_OUT)
    env.setdefault("JAVA_HOME", r"C:\Program Files\Android\Android Studio\jbr")
    PERF_OUT.unlink(missing_ok=True)
    cmd = ["cmd", "/c", r".\gradlew.bat :app:cleanTestDebugUnitTest :app:testDebugUnitTest --tests "
           "com.ml.tblandroidtxt.Cs1PerformanceCountTest --console=plain"]
    done = subprocess.run(cmd, cwd=ROOT, env=env, capture_output=True, text=True)
    if done.returncode != 0 or not PERF_OUT.exists():
        print((done.stdout + done.stderr)[-1500:])
        raise SystemExit("performance measurement failed")


def performance() -> int:
    if "--reuse" not in sys.argv:
        run_performance()
    data = json.loads(PERF_OUT.read_text(encoding="utf-8"))
    print("settings used:", data["settings"])
    rows = data["chapters"]
    for name in ("LN007", "LN011", "WN059"):
        print(" ", name, rows[name])
    counts = [r["chunks"] for r in rows.values()]
    print(f"  chapters {len(rows)}, chunks min {min(counts)} median {sorted(counts)[len(counts) // 2]} max {max(counts)}, "
          f"verdicts {sorted({r['verdict'] for r in rows.values()})}")
    print("  slowest plan on the JVM:", data["slowestChapter"], data["slowestMs"], "ms")
    return 0


def main() -> int:
    if "--performance" in sys.argv:
        return performance()
    if "--reuse" not in sys.argv:
        run_engine()
    data = json.loads(OUT.read_text(encoding="utf-8"))
    cuts = data["cuts"]
    verdicts = data["verdicts"]

    def n(cls: str, level: str) -> int:
        return verdicts.get(cls, {}).get(level, 0)

    def total(cls: str) -> int:
        return sum(verdicts.get(cls, {}).values())

    checks = []
    checks.append(("cuts correct", f"{cuts['cuts'] - cuts['wrong']}/{cuts['cuts']} on {cuts['chapters']} chapters",
                   cuts["cuts"] == 133 and cuts["wrong"] == 0 and cuts["chapters"] == 18))
    for suffix in ("+gl", " no-gl"):
        cls = "wrong_chapter" + suffix
        checks.append((f"wrong chapter{suffix} BLOCK", f"{n(cls, 'BLOCK')}/{total(cls)}", total(cls) == 55 and n(cls, "BLOCK") == 55))
    for suffix in ("+gl", " no-gl"):
        cls = "correct" + suffix
        checks.append((f"correct{suffix} BLOCK", f"{n(cls, 'BLOCK')}/{total(cls)}", total(cls) == 113 and n(cls, "BLOCK") == 0))
    for suffix in ("+gl", " no-gl"):
        cls = "other_edition" + suffix
        checks.append((f"other edition{suffix} OK", f"{n(cls, 'OK')}/{total(cls)}", total(cls) == 11 and n(cls, "OK") <= 1))
    ok = True
    for name, value, passed in checks:
        print(f"{'PASS' if passed else 'FAIL'}  {name}: {value}")
        ok = ok and passed
    for cls in sorted(verdicts):
        print("  verdicts", cls, verdicts[cls])
    print("  wrong cuts in:", cuts["chaptersWithAWrongCut"])
    print("  slowest verdict on the JVM:", data["timing"])
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
