#!/usr/bin/env python3
"""Score a private RAW/DRAFT/app/FINAL quartet without printing book text.

The FINAL is an evaluation reference only.  This tool deliberately reports line
numbers and counts, never source or translated text.  It uses difflib for the
same non-blank line alignment used by the coordinator's earlier measurements.
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import json
import re
import unicodedata
from pathlib import Path
from typing import Iterable


KANJI = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
KANA = re.compile(r"[\u3040-\u30ff\u31f0-\u31ff]")
RUBY = re.compile(r"《[\u3040-\u30ff]+》")
FULLWIDTH_LATIN = re.compile(r"[Ａ-Ｚａ-ｚ０-９]")
STRUCTURAL = "《》〝〟『』：／……"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig").replace("\r\n", "\n").replace("\r", "\n")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def nonblank(text: str) -> list[tuple[int, str]]:
    return [(i + 1, line) for i, line in enumerate(text.split("\n")) if line.strip()]


def similarity(a: str, b: str) -> float:
    # Whole chapters contain repeated punctuation and whitespace; disabling
    # autojunk makes SequenceMatcher quadratic on those runs.
    return difflib.SequenceMatcher(None, a, b, autojunk=True).ratio()


def align(left: list[tuple[int, str]], right: list[tuple[int, str]]) -> dict[int, int]:
    """Map each left index to one right index using stable difflib opcodes."""
    result: dict[int, int] = {}
    # ``autojunk`` avoids quadratic behaviour on long chapters with repeated
    # punctuation/scene lines while retaining difflib's stable opcode mapping.
    sm = difflib.SequenceMatcher(None, [x[1] for x in left], [x[1] for x in right], autojunk=True)
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            for i, j in zip(range(i1, i2), range(j1, j2)):
                result[i] = j
        else:
            width = min(i2 - i1, j2 - j1)
            for offset in range(width):
                result[i1 + offset] = j1 + offset
            # An insertion/deletion has no exact counterpart.  Do not invent one.
    return result


def has_japanese_or_han(value: str) -> bool:
    return bool(KANA.search(value) or KANJI.search(value))


def symbol_counts(value: str) -> dict[str, int]:
    return {symbol: value.count(symbol) for symbol in STRUCTURAL}


def score(raw: str, draft: str, app: str, final: str, chapter: str = "") -> dict:
    raw_lines = nonblank(raw)
    draft_lines = nonblank(draft)
    app_lines = nonblank(app)
    final_lines = nonblank(final)
    draft_final = align(draft_lines, final_lines)
    app_final = align(app_lines, final_lines)
    draft_raw = align(draft_lines, raw_lines)
    final_to_app = {final_index: app_index for app_index, final_index in app_final.items()}

    owner_changed = 0
    improved = 0
    near_exact = 0
    unchanged = 0
    app_changed_unchanged = 0
    farther = 0
    hard_symbol = []
    app_line_metrics: list[dict] = []
    for i, (draft_no, draft_line) in enumerate(draft_lines):
        fj = draft_final.get(i)
        if fj is None:
            continue
        final_no, final_line = final_lines[fj]
        draft_score = similarity(draft_line, final_line)
        changed_by_owner = draft_line != final_line
        if changed_by_owner:
            owner_changed += 1
        else:
            unchanged += 1
        # Pair the app by its position in the corresponding FINAL alignment.
        app_index = final_to_app.get(fj)
        app_pair = (app_index, app_lines[app_index][0], app_lines[app_index][1]) if app_index is not None else None
        app_line = app_pair[2] if app_pair else ""
        app_score = similarity(app_line, final_line) if app_pair else 0.0
        if changed_by_owner:
            # Credit only a move towards FINAL: a DRAFT line that was already >= 0.97 similar and left
            # untouched is not a fix (the no-op DRAFT scored 0.38 "recall" before this rule).
            if app_line == final_line or app_score >= draft_score + 0.02:
                improved += 1
            if app_score >= 0.97 and app_score > draft_score:
                near_exact += 1
        elif app_pair and app_line != draft_line:
            app_changed_unchanged += 1
        if app_pair and app_score + 1e-12 < draft_score:
            farther += 1
        raw_no = draft_raw.get(i)
        if app_pair and raw_no is not None:
            raw_line = raw_lines[raw_no][1]
            if symbol_counts(app_line) != symbol_counts(raw_line):
                hard_symbol.append(app_pair[1])
        app_line_metrics.append({
            "draftLine": draft_no,
            "finalLine": final_no,
            "appLine": app_pair[1] if app_pair else None,
            "ownerChanged": changed_by_owner,
            "draftSimilarity": round(draft_score, 8),
            "appSimilarity": round(app_score, 8),
        })

    hard_added = []
    hard_remaining = []
    hard_ruby = []
    hard_fullwidth = []
    for metric in app_line_metrics:
        ai = metric["appLine"]
        if ai is None:
            continue
        app_line = app_lines[next(j for j, item in enumerate(app_lines) if item[0] == ai)][1]
        draft_no = metric["draftLine"]
        draft_line = next(line for no, line in draft_lines if no == draft_no)
        final_line = next(line for no, line in final_lines if no == metric["finalLine"])
        if has_japanese_or_han(app_line) and not has_japanese_or_han(draft_line):
            hard_added.append(ai)
        if not has_japanese_or_han(final_line) and has_japanese_or_han(app_line):
            hard_remaining.append(ai)
        if RUBY.search(app_line):
            hard_ruby.append(ai)
        if FULLWIDTH_LATIN.search(app_line):
            hard_fullwidth.append(ai)

    return {
        "chapter": chapter,
        "inputs": {
            "rawSha256": hashlib.sha256(raw.encode("utf-8")).hexdigest(),
            "draftSha256": hashlib.sha256(draft.encode("utf-8")).hexdigest(),
            "appSha256": hashlib.sha256(app.encode("utf-8")).hexdigest(),
            "finalSha256": hashlib.sha256(final.encode("utf-8")).hexdigest(),
        },
        "lines": {"raw": len(raw_lines), "draft": len(draft_lines), "app": len(app_lines), "final": len(final_lines)},
        "fixRecall": {
            "ownerChanged": owner_changed,
            "improved": improved,
            "nearExact": near_exact,
            "rate": round(improved / owner_changed, 8) if owner_changed else 1.0,
        },
        "regression": {"unchangedOwner": unchanged, "appChangedUnchanged": app_changed_unchanged, "farther": farther},
        "hard": {
            "addedKanaHanLines": hard_added,
            "remainingKanaHanLines": hard_remaining,
            "rubyLines": hard_ruby,
            "fullwidthLatinLines": hard_fullwidth,
            "rawSymbolMismatchLines": hard_symbol,
            "lineDeltaFromRaw": len(app_lines) - len(raw_lines),
        },
        "similarity": {
            "draftVsFinal": round(similarity(draft, final), 8),
            "appVsFinal": round(similarity(app, final), 8),
            "delta": round(similarity(app, final) - similarity(draft, final), 8),
        },
        "lineMetrics": app_line_metrics,
    }


def aggregate(rows: Iterable[dict]) -> dict:
    rows = list(rows)
    total_owner = sum(r["fixRecall"]["ownerChanged"] for r in rows)
    total_improved = sum(r["fixRecall"]["improved"] for r in rows)
    return {
        "chapters": [r["chapter"] for r in rows],
        "count": len(rows),
        "fixRecall": {"ownerChanged": total_owner, "improved": total_improved,
                      "rate": round(total_improved / total_owner, 8) if total_owner else 1.0},
        "regression": {k: sum(r["regression"][k] for r in rows) for k in ("unchangedOwner", "appChangedUnchanged", "farther")},
        "hard": {k: sum(len(r["hard"][k]) if isinstance(r["hard"][k], list) else abs(r["hard"][k]) for r in rows)
                 for k in ("addedKanaHanLines", "remainingKanaHanLines", "rubyLines", "fullwidthLatinLines", "rawSymbolMismatchLines", "lineDeltaFromRaw")},
        "similarity": {"draftVsFinal": round(sum(r["similarity"]["draftVsFinal"] for r in rows) / len(rows), 8) if rows else 0,
                       "appVsFinal": round(sum(r["similarity"]["appVsFinal"] for r in rows) / len(rows), 8) if rows else 0,
                       "delta": round(sum(r["similarity"]["delta"] for r in rows) / len(rows), 8) if rows else 0},
    }


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--raw", type=Path, required=True)
    p.add_argument("--draft", type=Path, required=True)
    p.add_argument("--app", type=Path, required=True)
    p.add_argument("--final", type=Path, required=True)
    p.add_argument("--chapter", default="")
    p.add_argument("--output", type=Path)
    args = p.parse_args()
    result = score(read(args.raw), read(args.draft), read(args.app), read(args.final), args.chapter)
    result["fileHashes"] = {k: sha256(path) for k, path in (("raw", args.raw), ("draft", args.draft), ("app", args.app), ("final", args.final))}
    data = json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(data, encoding="utf-8")
    else:
        print(data, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
