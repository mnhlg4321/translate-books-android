#!/usr/bin/env python3
"""Automatic part of the "minimum quality of V5-SAFE.4.1.3" gate (Q2.5 request, section 7).

A chapter passes the automatic filter when all of these hold; the fifth condition (the owner reads and accepts the text)
is not computed here and is the deciding one.

1. Structure: the run is valid and not truncated; the app text is neither much shorter nor much longer than the DRAFT
   (integer fences 0.80 and 1.25 on non-blank characters); the number of non-blank lines does not differ from RAW by more
   than the DRAFT's own difference.
2. No new technical error: no line gained Japanese kana or Han; the number of lines still holding kana or Han is not above
   the owner FINAL's; the number of lines whose structural symbols differ from RAW is not above the DRAFT's.
3. Nothing broken: among the lines the owner changed, the lines the app moved farther from FINAL are at most 10 % of the
   owner-changed lines; the whole-chapter similarity to FINAL does not fall below the DRAFT's.
4. Real fixes: improved lines are at least 25 % of the owner-changed lines.

The FINAL is an evaluation reference only. This module prints counts and line numbers, never book text.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import score_vs_final as sv

FENCE_LOW = (4, 5)   # app chars * 5 >= draft chars * 4   (0.80)
FENCE_HIGH = (5, 4)  # app chars * 4 <= draft chars * 5   (1.25)


def chars(text: str) -> int:
    return sum(len(line) for _, line in sv.nonblank(text))


def japanese_lines(text: str) -> int:
    return sum(1 for _, line in sv.nonblank(text) if sv.has_japanese_or_han(line))


def farther_on_owner_changed(metrics: list[dict]) -> int:
    return sum(1 for m in metrics if m["ownerChanged"] and m["appLine"] is not None
               and m["appSimilarity"] + 1e-12 < m["draftSimilarity"])


def gate(raw: str, draft: str, app: str, final: str, *, valid: bool = True, truncated: bool = False, chapter: str = "") -> dict:
    scored = sv.score(raw, draft, app, final, chapter)
    baseline = sv.score(raw, draft, draft, final, chapter)  # the DRAFT scored as if it were the app
    raw_lines = len(sv.nonblank(raw))
    draft_lines = len(sv.nonblank(draft))
    app_lines = len(sv.nonblank(app))
    app_chars = chars(app)
    draft_chars = chars(draft)
    owner_changed = scored["fixRecall"]["ownerChanged"]
    improved = scored["fixRecall"]["improved"]
    farther = farther_on_owner_changed(scored["lineMetrics"])
    checks = {
        "c1_valid_not_truncated": bool(valid) and not truncated,
        "c1_length_fence": app_chars * FENCE_LOW[1] >= draft_chars * FENCE_LOW[0] and app_chars * FENCE_HIGH[1] <= draft_chars * FENCE_HIGH[0],
        "c1_line_count_vs_raw": abs(app_lines - raw_lines) <= abs(draft_lines - raw_lines),
        "c2_no_added_kana_han": len(scored["hard"]["addedKanaHanLines"]) == 0,
        "c2_japanese_lines_not_above_final": japanese_lines(app) <= japanese_lines(final),
        "c2_symbol_mismatch_not_above_draft": len(scored["hard"]["rawSymbolMismatchLines"]) <= len(baseline["hard"]["rawSymbolMismatchLines"]),
        "c3_farther_on_owner_changed_at_most_10pct": 10 * farther <= owner_changed,
        "c3_similarity_not_below_draft": scored["similarity"]["appVsFinal"] + 1e-12 >= scored["similarity"]["draftVsFinal"],
        "c4_improved_at_least_25pct": 4 * improved >= owner_changed and owner_changed > 0,
    }
    return {
        "chapter": chapter,
        "pass": all(checks.values()),
        "checks": checks,
        "numbers": {
            "ownerChanged": owner_changed,
            "improved": improved,
            "nearExact": scored["fixRecall"]["nearExact"],
            "fartherOnOwnerChanged": farther,
            "fartherAll": scored["regression"]["farther"],
            "appChangedOwnerUnchanged": scored["regression"]["appChangedUnchanged"],
            "addedKanaHanLines": len(scored["hard"]["addedKanaHanLines"]),
            "japaneseLinesApp": japanese_lines(app),
            "japaneseLinesFinal": japanese_lines(final),
            "symbolMismatchApp": len(scored["hard"]["rawSymbolMismatchLines"]),
            "symbolMismatchDraft": len(baseline["hard"]["rawSymbolMismatchLines"]),
            "linesRaw": raw_lines, "linesDraft": draft_lines, "linesApp": app_lines,
            "charsApp": app_chars, "charsDraft": draft_chars,
            "similarityDraftVsFinal": scored["similarity"]["draftVsFinal"],
            "similarityAppVsFinal": scored["similarity"]["appVsFinal"],
            "similarityDelta": scored["similarity"]["delta"],
        },
    }


def main(argv: list[str] | None = None) -> int:
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument("--raw", type=Path, required=True)
    p.add_argument("--draft", type=Path, required=True)
    p.add_argument("--app", type=Path, required=True)
    p.add_argument("--final", type=Path, required=True)
    p.add_argument("--chapter", default="")
    p.add_argument("--invalid", action="store_true", help="the run did not produce a valid candidate")
    p.add_argument("--truncated", action="store_true")
    args = p.parse_args(argv)
    result = gate(sv.read(args.raw), sv.read(args.draft), sv.read(args.app), sv.read(args.final),
                  valid=not args.invalid, truncated=args.truncated, chapter=args.chapter)
    print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
