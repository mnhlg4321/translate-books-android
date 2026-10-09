#!/usr/bin/env python3
"""Synthetic tests for the whole-chunk completeness control. No book text: every line is made up."""
from __future__ import annotations

import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from chunk_completeness import ControlError, build, control, main_units  # noqa: E402

RAW_LINES = [f"RAW unit {n} says something distinct" for n in range(61, 67)]
PROMPT = (
    "SYSTEM\ncore text\nUSER\n# RAW CONTEXT BEFORE (REFERENCE ONLY - do not return, do not edit)\nbefore context\n\n"
    "# RAW (this part)\n" + "\n\n".join(f"⟦P{n:03d}⟧ {t}" for n, t in zip(range(61, 67), RAW_LINES)) +
    "\n\n# RAW CONTEXT AFTER (REFERENCE ONLY - do not return, do not edit)\nafter context\n\n# DRAFT (this part)\n")
DRAFT = [f"draft line {n}" for n in range(1, 7)]
DRAFT_LABELS = {f"P{n:03d}": True for n in range(61, 67)}


def text(lines: list[str]) -> str:
    return "\n\n".join(lines) + "\n"


class ChunkCompletenessTest(unittest.TestCase):
    def test_units_are_the_main_paragraphs_only(self):
        units = main_units(PROMPT)
        self.assertEqual([u[0] for u in units], [f"P{n:03d}" for n in range(61, 67)])
        self.assertEqual(units[0][1], RAW_LINES[0])

    def test_no_op_candidate_is_fully_measured_and_retained(self):
        r = control(PROMPT, text(DRAFT), text(DRAFT), DRAFT_LABELS, {})
        self.assertEqual(r["status"], "MEASURED")
        self.assertEqual(r["completeness"].get("required_units_retained", 0), 6)
        self.assertEqual(r["completeness"].get("new_omissions", 0), 0)
        self.assertEqual(r["open_items"], 0)

    def test_a_changed_line_without_a_label_is_never_counted_as_preserved(self):
        cand = list(DRAFT)
        cand[2] = "reworded line three"
        r = control(PROMPT, text(DRAFT), text(cand), DRAFT_LABELS, {})
        self.assertEqual(r["status"], "PARTIAL")
        self.assertEqual(r["coverage"]["changed_unadjudicated"], 1)
        self.assertEqual(r["completeness"].get("required_units_retained", 0), 5)
        self.assertEqual(r["completeness"].get("denominator", 0), 5)

    def test_changed_lines_with_labels_split_into_retained_and_lost(self):
        cand = list(DRAFT)
        cand[1] = "reworded two"
        cand[4] = "reworded five that lost the point"
        labels = {"P062": True, "P065": {"present": False, "note": "the event of RAW unit 65 is gone"}}
        r = control(PROMPT, text(DRAFT), text(cand), DRAFT_LABELS, labels)
        self.assertEqual(r["status"], "MEASURED")
        self.assertEqual(r["completeness"].get("required_units_retained", 0), 5)
        self.assertEqual(r["completeness"].get("new_omissions", 0), 1)

    def test_a_dropped_paragraph_is_a_mechanical_omission(self):
        cand = DRAFT[:3] + DRAFT[4:]
        r = control(PROMPT, text(DRAFT), text(cand), DRAFT_LABELS, {})
        self.assertEqual(r["completeness"].get("new_omissions", 0), 1)
        self.assertEqual(r["coverage"]["deleted"], 1)
        self.assertEqual(r["status"], "MEASURED")

    def test_an_added_line_needs_a_label_and_a_supported_one_is_not_an_addition_error(self):
        cand = DRAFT + ["an extra line copied from the neighbour"]
        r = control(PROMPT, text(DRAFT), text(cand), DRAFT_LABELS, {})
        self.assertEqual(r["status"], "PARTIAL")
        self.assertEqual(r["coverage"]["added_unadjudicated"], 1)
        r = control(PROMPT, text(DRAFT), text(cand), DRAFT_LABELS, {"+1": {"present": True, "note": "context copied into main"}})
        self.assertEqual(r["completeness"].get("unsupported_additions", 0), 1)
        self.assertEqual(r["status"], "MEASURED")

    def test_a_merge_of_two_lines_is_ambiguous_until_labelled(self):
        cand = DRAFT[:2] + ["draft line 3 and draft line 4 merged"] + DRAFT[4:]
        r = control(PROMPT, text(DRAFT), text(cand), DRAFT_LABELS, {})
        self.assertEqual(r["status"], "PARTIAL")
        self.assertEqual(r["coverage"]["ambiguous"], 2)

    def test_units_that_do_not_map_one_to_one_are_refused(self):
        with self.assertRaises(ControlError):
            build(PROMPT, text(DRAFT[:5]), text(DRAFT[:5]), DRAFT_LABELS, {})
        with self.assertRaises(ControlError):
            build("no raw block here", text(DRAFT), text(DRAFT), DRAFT_LABELS, {})

    def test_missing_draft_labels_leave_the_control_unmeasured(self):
        r = control(PROMPT, text(DRAFT), text(DRAFT), {}, {})
        self.assertEqual(r["status"], "NOT_MEASURED")
        self.assertEqual(r["coverage"]["draft_unadjudicated"], 6)

    def test_a_draft_omission_restored_by_the_candidate_is_counted(self):
        labels = dict(DRAFT_LABELS)
        labels["P064"] = {"present": False, "note": "RAW unit 64 is missing from the DRAFT"}
        cand = list(DRAFT)
        cand[3] = "the restored fourth line"
        r = control(PROMPT, text(DRAFT), text(cand), labels, {"P064": True})
        self.assertEqual(r["completeness"].get("omissions_restored", 0), 1)
        r = control(PROMPT, text(DRAFT), text(DRAFT), labels, {})
        self.assertEqual(r["completeness"].get("omissions_remaining", 0), 1)

    def test_output_has_no_book_text(self):
        built = build(PROMPT, text(DRAFT), text(DRAFT), DRAFT_LABELS, {})
        blob = str(built)
        for t in RAW_LINES + DRAFT:
            self.assertNotIn(t, blob)


if __name__ == "__main__":
    unittest.main()
