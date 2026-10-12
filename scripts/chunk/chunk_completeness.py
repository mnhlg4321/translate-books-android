#!/usr/bin/env python3
"""Whole-chunk completeness control for a single-chunk experiment (docs/EDITORIAL_CHUNK_PLAN.md, 6.2).

The RAW-required units of the chunk are the paragraphs of its "RAW (this part)" block, read from the request the app sent (or
would send). Each unit is matched to one DRAFT line. A candidate line identical to its DRAFT line cannot change what the unit
says, so it is adjudicated by identity. Every changed, ambiguous or added line needs a person's label; without one it is
reported as UNADJUDICATED and the control is only PARTIAL, never a pass. A deleted line is a mechanical omission.

The output of this module carries ids, hashes and counts only: no book text. The label files and the chunk text stay outside Git.
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))
from adjudicated_quality import SCHEMA, evaluate  # noqa: E402

LABEL_RE = re.compile(r"^⟦(P\d{3})⟧\s*")
RAW_HEADER = "# RAW (this part)"


class ControlError(ValueError):
    """The chunk cannot be controlled safely (units do not map one to one, a label is malformed)."""


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def lines(text: str) -> list[str]:
    return [ln for ln in text.replace("\r\n", "\n").replace("\r", "\n").split("\n") if ln.strip()]


def main_units(prompt_text: str) -> list[tuple[str, str]]:
    """(label, RAW text) of every paragraph of the main range, in order. Labels are P061-style when the prompt carries them."""
    start = prompt_text.find(RAW_HEADER)
    if start < 0:
        raise ControlError("no RAW (this part) block in the prompt")
    body = prompt_text[start + len(RAW_HEADER):]
    end = re.search(r"^# ", body, re.MULTILINE)
    block = body[:end.start()] if end else body
    out: list[tuple[str, str]] = []
    for n, ln in enumerate(lines(block), 1):
        m = LABEL_RE.match(ln)
        out.append((m.group(1) if m else f"L{n:03d}", LABEL_RE.sub("", ln, count=1)))
    if not out:
        raise ControlError("the RAW main block is empty")
    return out


def align(draft: list[str], candidate: list[str]) -> tuple[list[tuple[str, int | None]], int]:
    """Per DRAFT line: (status, candidate index). Status SAME, CHANGED, DELETED or AMBIGUOUS. Also the count of added lines."""
    result: list[tuple[str, int | None]] = [("AMBIGUOUS", None)] * len(draft)
    added = 0
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, draft, candidate, autojunk=False).get_opcodes():
        if tag == "equal":
            for k in range(i2 - i1):
                result[i1 + k] = ("SAME", j1 + k)
        elif tag == "delete":
            for i in range(i1, i2):
                result[i] = ("DELETED", None)
        elif tag == "insert":
            added += j2 - j1
        elif i2 - i1 == j2 - j1:
            for k in range(i2 - i1):
                result[i1 + k] = ("CHANGED", j1 + k)
        else:
            for i in range(i1, i2):
                result[i] = ("AMBIGUOUS", None)
            added += max(0, (j2 - j1) - (i2 - i1))
    return result, added


def _bool(labels: dict[str, Any], key: str, what: str) -> bool | None:
    if key not in labels:
        return None
    value = labels[key]
    if isinstance(value, dict):
        value = value.get("present")
    if not isinstance(value, bool):
        raise ControlError(f"{what} label for {key} must be a boolean")
    return value


def build(prompt_text: str, draft_text: str, candidate_text: str, draft_labels: dict[str, Any],
          candidate_labels: dict[str, Any]) -> dict[str, Any]:
    """The adjudicated-quality annotation for the chunk plus coverage counts; see the module doc for the rules."""
    units = main_units(prompt_text)
    draft = lines(draft_text)
    candidate = lines(candidate_text)
    if len(units) != len(draft):
        raise ControlError(f"RAW units and DRAFT lines do not map one to one ({len(units)} vs {len(draft)})")
    alignment, added = align(draft, candidate)
    content: list[dict[str, Any]] = []
    stats = {"units": len(units), "same": 0, "changed": 0, "deleted": 0, "ambiguous": 0, "changed_unadjudicated": 0,
             "draft_unadjudicated": 0, "added_lines": added, "added_unadjudicated": 0}
    for (label, raw), (status, _), draft_line in zip(units, alignment, draft):
        present_in_draft = _bool(draft_labels, label, "DRAFT")
        item: dict[str, Any] = {
            "id": f"unit-{label}",
            "raw_anchor": {"source_role": "RAW", "range": label, "sha256": sha(raw)},
            "required_by_raw": True,
            "scope": "MAIN",
            "draft_present": bool(present_in_draft),
            "candidate_present": False,
            "adjudication": "UNADJUDICATED",
            "adjudication_note": "",
            "mechanical_status": status,
        }
        if present_in_draft is None:
            stats["draft_unadjudicated"] += 1
        else:
            if status == "SAME":
                stats["same"] += 1
                item.update(candidate_present=present_in_draft, adjudication="ADJUDICATED",
                            adjudication_note="Candidate line identical to the DRAFT line; presence is unchanged by identity.")
            elif status == "DELETED":
                stats["deleted"] += 1
                item.update(candidate_present=False, adjudication="ADJUDICATED",
                            adjudication_note="The DRAFT line has no counterpart in the candidate (mechanical omission).")
            else:
                stats["changed" if status == "CHANGED" else "ambiguous"] += 1
                label_value = _bool(candidate_labels, label, "candidate")
                if label_value is None:
                    stats["changed_unadjudicated"] += 1
                else:
                    note = candidate_labels[label].get("note", "") if isinstance(candidate_labels[label], dict) else ""
                    item.update(candidate_present=label_value, adjudication="ADJUDICATED",
                                adjudication_note=note or "Adjudicated against RAW by the reviewer.")
        content.append(item)
    for n in range(added):
        key = f"+{n + 1}"
        value = _bool(candidate_labels, key, "candidate")
        if value is None:
            stats["added_unadjudicated"] += 1
        else:
            note = candidate_labels[key].get("note", "") if isinstance(candidate_labels[key], dict) else ""
            content.append({
                "id": f"added-{n + 1}", "raw_anchor": {"source_role": "RAW", "range": key, "sha256": sha(f"added-{n + 1}")},
                "required_by_raw": False, "scope": "MAIN", "draft_present": False, "candidate_present": True,
                "adjudication": "ADJUDICATED", "adjudication_note": note or "Added candidate line judged by the reviewer.",
            })
    for item in content:
        item.pop("mechanical_status", None)
    return {"annotation": {"schema": SCHEMA, "content_units": content}, "coverage": stats}


def control(prompt_text: str, draft_text: str, candidate_text: str, draft_labels: dict[str, Any],
            candidate_labels: dict[str, Any]) -> dict[str, Any]:
    built = build(prompt_text, draft_text, candidate_text, draft_labels, candidate_labels)
    cov = built["coverage"]
    open_items = cov["changed_unadjudicated"] + cov["draft_unadjudicated"] + cov["added_unadjudicated"]
    scored = evaluate(built["annotation"])
    comp = scored["completeness"]
    status = "NOT_MEASURED" if comp["status"] == "NOT_MEASURED" else ("PARTIAL" if open_items else "MEASURED")
    return {"status": status, "coverage": cov, "completeness": comp, "open_items": open_items,
            "semantics": "Completeness control only; unadjudicated changes are listed, never counted as preserved"}


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--prompt", required=True, help="request text of the chunk (SYSTEM/USER file written by the runner)")
    ap.add_argument("--draft", required=True, help="DRAFT text of the chunk")
    ap.add_argument("--candidate", required=True, help="candidate text of the chunk (the DRAFT itself gives the no-op control)")
    ap.add_argument("--draft-labels", required=True, help="JSON {Pnnn: bool|{present}} for the DRAFT, adjudicated against RAW")
    ap.add_argument("--candidate-labels", default="", help="JSON for changed/ambiguous/added candidate lines")
    ap.add_argument("--out", default="", help="write the result here; stdout otherwise")
    a = ap.parse_args(argv)
    read = lambda p: Path(p).read_text(encoding="utf-8-sig")  # noqa: E731
    cand_labels = json.loads(read(a.candidate_labels)) if a.candidate_labels else {}
    result = control(read(a.prompt), read(a.draft), read(a.candidate), json.loads(read(a.draft_labels)), cand_labels)
    text = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True)
    if a.out:
        Path(a.out).write_text(text + "\n", encoding="utf-8")
    else:
        print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
