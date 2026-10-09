#!/usr/bin/env python3
"""Synthetic RAW-adjudicated controls for the offline chunk quality scorer."""
from __future__ import annotations

import hashlib
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from adjudicated_quality import AnnotationError, SCHEMA, evaluate  # noqa: E402


def raw_anchor(case_id: str, line: str | None = None, span: str | None = None) -> dict:
    synthetic = line or f"SYNTHETIC RAW evidence for {case_id}: explicitly resolved speaker, target, and content unit."
    return {
        "source_role": "RAW",
        "range": span or "L1-L1",
        "sha256": hashlib.sha256(synthetic.encode("utf-8")).hexdigest(),
        "synthetic_raw": synthetic,
    }


def address(case_id: str, draft: str, candidate: str | None, acceptable: list[str] | None = None,
            adjudication: str = "ADJUDICATED", speaker: str = "S1", target: str = "T1",
            defect_type: str = "CALL", scope: str = "MAIN") -> dict:
    synthetic = (f"SYNTHETIC RAW: dialogue cue identifies speaker {speaker} and target {target}; "
                 f"scope={scope}; occurrence={case_id}.")
    row = {
        "id": case_id,
        "raw_anchor": raw_anchor(case_id, line=synthetic),
        "adjudication": adjudication,
        "defect_type": defect_type,
        "scope": scope,
        "draft_form": draft,
        "candidate_form": candidate,
        "adjudication_note": (
            "Synthetic RAW leaves speaker/target ambiguous or conflicting; preserve the original DRAFT form."
            if adjudication == "PRESERVE" else
            f"Synthetic RAW dialogue cue adjudicates speaker, target, scope, and form for {case_id}."
        ),
    }
    if adjudication == "ADJUDICATED":
        row.update({"speaker_id": speaker, "target_id": target, "acceptable_forms": acceptable or ["FORM_GOOD"]})
    return row


def unit(case_id: str, draft: bool, candidate: bool, *, required: bool = True,
         scope: str = "MAIN", adjudication: str = "ADJUDICATED") -> dict:
    return {
        "id": case_id,
        "raw_anchor": raw_anchor(case_id),
        "adjudication": adjudication,
        "required_by_raw": required,
        "draft_present": draft,
        "candidate_present": candidate,
        "scope": scope,
        "adjudication_note": f"Synthetic RAW fixture adjudicates required-main membership for {case_id}.",
    }


def synthetic_controls() -> dict:
    return {
        "schema": SCHEMA,
        "address_occurrences": [
            address("wrong-self-fixed", "SELF_BAD", "SELF_GOOD", ["SELF_GOOD"], defect_type="SELF"),
            # Two explicit occurrences in one speaker-to-target turn.
            address("repeat-call-1", "CALL_BAD", "TARGET_NAME", ["TARGET_NAME"]),
            address("repeat-call-2", "CALL_BAD", "TARGET_NAME", ["TARGET_NAME"]),
            # Reversed pair is adjudicated from the opposite RAW speaker/target.
            address("reversed-pair", "CALL_BAD", "TARGET_NAME", ["TARGET_NAME"], speaker="S2", target="T1"),
            address("wrong-call-missed", "CALL_BAD", "CALL_BAD", ["TARGET_NAME"]),
            address("wrong-call-wrong-fix", "CALL_BAD", "OTHER_BAD", ["TARGET_NAME"]),
            # Speaker switched; the correct form for the new speaker stays intact.
            address("speaker-switch-clean", "S2_SELF", "S2_SELF", ["S2_SELF"], speaker="S2", target="T1"),
            # Pronoun row scope ended at the chunk boundary; keep the local RAW form.
            address("outside-scope-clean", "LOCAL_FORM", "LOCAL_FORM", ["LOCAL_FORM"], scope="OUTSIDE_PRONOUN_ROW"),
            # Ambiguity/conflict are preserve-only and do not enter the defect denominator.
            address("ambiguous-preserve", "AMBIGUOUS_FORM", "AMBIGUOUS_FORM", adjudication="PRESERVE"),
            address("ambiguous-guessed", "AMBIGUOUS_FORM", "GUESSED_FORM", adjudication="PRESERVE"),
            address("conflict-guessed", "CONFLICT_FORM", "OTHER_FORM", adjudication="PRESERVE"),
            # A clean control is deliberately changed to a disallowed address form.
            address("clean-negative-control", "CLEAN_FORM", "NEW_BAD", ["CLEAN_FORM"]),
            # Deleting the address occurrence is not a successful correction.
            address("deleted-address", "CALL_BAD", None, ["TARGET_NAME"]),
            address("unadjudicated", "UNKNOWN_FORM", "OTHER_FORM", adjudication="UNADJUDICATED"),
        ],
        "content_units": [
            unit("omission-restored", False, True),
            unit("omission-remaining", False, False),
            unit("deleted-error-sentence", True, False),
            unit("newly-dropped-content", True, False),
            unit("clean-content-retained", True, True),
            unit("unknown-content", False, False, adjudication="UNADJUDICATED"),
            # Neighbouring chunk text is reference-only and must not enter main output.
            unit("boundary-reference-leak", False, True, required=False, scope="REFERENCE"),
            unit("unsupported-addition", False, True, required=False),
        ],
        "other_edits": [
            {
                "id": "kana-residue-fixed",
                "raw_anchor": raw_anchor("kana-residue-fixed"),
                "adjudication": "ADJUDICATED",
                "adjudication_note": "Synthetic RAW and target-language mapping confirm the kana residue is an error.",
                "defect_type": "KANA_RESIDUE",
                "draft_has_defect": True,
                "candidate_has_defect": False,
                "candidate_changed": True,
                "candidate_supported_by_raw": True,
            },
            {
                "id": "damaged-character-missed",
                "raw_anchor": raw_anchor("damaged-character-missed"),
                "adjudication": "ADJUDICATED",
                "adjudication_note": "Synthetic RAW determines the intended character; candidate leaves it damaged.",
                "defect_type": "DAMAGED_CHARACTER",
                "draft_has_defect": True,
                "candidate_has_defect": True,
                "candidate_changed": False,
                "candidate_supported_by_raw": True,
            },
            {
                "id": "damaged-character-wrong-fix",
                "raw_anchor": raw_anchor("damaged-character-wrong-fix"),
                "adjudication": "ADJUDICATED",
                "adjudication_note": "Synthetic RAW does not support the candidate replacement.",
                "defect_type": "DAMAGED_CHARACTER",
                "draft_has_defect": True,
                "candidate_has_defect": True,
                "candidate_changed": True,
                "candidate_supported_by_raw": False,
            },
            {
                "id": "clean-style-polish-control",
                "raw_anchor": raw_anchor("clean-style-polish-control"),
                "adjudication": "ADJUDICATED",
                "adjudication_note": "Synthetic RAW supports the existing wording; the stylistic change is unsupported.",
                "defect_type": "CLEAN_CONTROL",
                "draft_has_defect": False,
                "candidate_has_defect": False,
                "candidate_changed": True,
                "candidate_supported_by_raw": False,
            },
            {
                "id": "new-corruption",
                "raw_anchor": raw_anchor("new-corruption"),
                "adjudication": "ADJUDICATED",
                "adjudication_note": "Synthetic RAW supports the clean DRAFT and rejects the introduced corruption.",
                "defect_type": "DAMAGED_CHARACTER",
                "draft_has_defect": False,
                "candidate_has_defect": True,
                "candidate_changed": True,
                "candidate_supported_by_raw": False,
            },
            {
                "id": "clean-content-noop",
                "raw_anchor": raw_anchor("clean-content-noop"),
                "adjudication": "ADJUDICATED",
                "adjudication_note": "Synthetic RAW confirms the unchanged content is correct.",
                "defect_type": "CLEAN_CONTROL",
                "draft_has_defect": False,
                "candidate_has_defect": False,
                "candidate_changed": False,
                "candidate_supported_by_raw": True,
            },
        ],
        "non_occurrence_controls": [
            {
                "id": "narration-name-is-not-address",
                "raw_anchor": raw_anchor(
                    "narration-name-is-not-address",
                    line="SYNTHETIC RAW: narrator mentions target by name; no dialogue address occurrence exists.",
                ),
                "decision": "NOT_AN_ADDRESS_OCCURRENCE",
                "candidate_changed": False,
            },
        ],
    }


class AdjudicatedQualityTest(unittest.TestCase):
    def test_address_error_buckets_and_controls_are_distinct(self):
        result = evaluate(synthetic_controls())
        self.assertEqual(result["address"]["denominator"], 7)
        self.assertEqual(result["address"]["correct_fixes"], 4)
        self.assertEqual(result["address"]["missed_errors"], 2)
        self.assertEqual(result["address"]["wrong_fixes"], 1)
        self.assertEqual(result["address"]["new_errors"], 3)
        self.assertEqual(result["address"]["clean_unchanged"], 3)
        self.assertEqual(result["address"]["unadjudicated_excluded"], 1)
        self.assertEqual(result["controls"]["non_address_controls_passed"], 1)

    def test_completeness_counts_restored_remaining_and_new_omissions(self):
        result = evaluate(synthetic_controls())
        self.assertEqual(result["completeness"]["denominator"], 5)
        self.assertEqual(result["completeness"]["omissions_restored"], 1)
        self.assertEqual(result["completeness"]["omissions_remaining"], 1)
        self.assertEqual(result["completeness"]["new_omissions"], 2)
        self.assertEqual(result["completeness"]["required_units_retained"], 1)
        self.assertEqual(result["completeness"]["boundary_leaks"], 1)
        self.assertEqual(result["completeness"]["unsupported_additions"], 1)
        self.assertEqual(result["completeness"]["unadjudicated_excluded"], 1)

    def test_non_address_edits_track_kana_damage_and_collateral_changes_separately(self):
        result = evaluate(synthetic_controls())
        self.assertEqual(result["other_edits"]["denominator"], 3)
        self.assertEqual(result["other_edits"]["correct_fixes"], 1)
        self.assertEqual(result["other_edits"]["missed_errors"], 1)
        self.assertEqual(result["other_edits"]["wrong_fixes"], 1)
        self.assertEqual(result["other_edits"]["new_errors"], 1)
        self.assertEqual(result["other_edits"]["unsupported_changes"], 1)
        self.assertEqual(result["other_edits"]["clean_unchanged"], 1)

    def test_deleting_an_error_sentence_is_missed_and_new_omission(self):
        result = evaluate(synthetic_controls())
        # The bad call was deleted, so it is not a correct pronoun fix; its RAW-required
        # semantic unit is independently recorded as a new omission.
        self.assertEqual(result["address"]["missed_errors"], 2)
        self.assertEqual(result["completeness"]["new_omissions"], 2)
        self.assertEqual(result["quality_outcome"], "ISSUES_PRESENT")

    def test_bad_anchor_hash_is_rejected(self):
        data = synthetic_controls()
        data["address_occurrences"][0]["raw_anchor"]["sha256"] = "0" * 64
        with self.assertRaisesRegex(AnnotationError, "hash mismatch"):
            evaluate(data)

    def test_unadjudicated_items_do_not_enter_denominators(self):
        result = evaluate(synthetic_controls())
        self.assertEqual(result["address"]["denominator"], 7)
        self.assertEqual(result["completeness"]["denominator"], 5)

    def test_final_similarity_cannot_change_address_or_completeness_scores(self):
        data = synthetic_controls()
        baseline = evaluate(data)
        data["final_similarity"] = 0.0
        with_final_metric = evaluate(data)
        self.assertEqual(with_final_metric, baseline)
        self.assertNotIn("similarity", str(with_final_metric).lower())

    def test_missed_only_case_is_not_reported_as_issue_free(self):
        result = evaluate({
            "schema": SCHEMA,
            "address_occurrences": [address("only-missed", "BAD", "BAD", ["GOOD"])],
            "content_units": [],
            "other_edits": [],
            "non_occurrence_controls": [],
        })
        self.assertEqual(result["address"]["missed_errors"], 1)
        self.assertEqual(result["quality_outcome"], "ISSUES_PRESENT")

    def test_missing_raw_role_or_acceptable_resolution_fails_closed(self):
        data = synthetic_controls()
        del data["address_occurrences"][0]["raw_anchor"]["source_role"]
        with self.assertRaisesRegex(AnnotationError, "source_role must be RAW"):
            evaluate(data)

        data = synthetic_controls()
        data["address_occurrences"][0]["acceptable_forms"] = []
        with self.assertRaisesRegex(AnnotationError, "acceptable_forms"):
            evaluate(data)


if __name__ == "__main__":
    unittest.main(verbosity=2)
