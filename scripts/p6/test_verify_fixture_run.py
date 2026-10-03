#!/usr/bin/env python3
"""Tests for private fixture output layout normalization."""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import verify_fixture_run  # noqa: E402


class FixtureOutputLayoutTest(unittest.TestCase):
    def test_identical_nested_and_direct_pulls_resolve_to_canonical_output(self):
        with tempfile.TemporaryDirectory() as temporary:
            results = os.path.join(temporary, "results")
            direct = os.path.join(results, "fx-a01")
            nested = os.path.join(results, "run-id", "fx-a01")
            for path in (direct, nested):
                os.makedirs(path)
                with open(os.path.join(path, "final.txt"), "wb") as handle:
                    handle.write(b"offline fixture output\n")
                with open(os.path.join(path, "structural.json"), "wb") as handle:
                    handle.write(b"{\"valid\":true}\n")

            self.assertEqual(direct, verify_fixture_run.fixture_output(results, "fx-a01"))

    def test_different_duplicate_pulls_are_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            results = os.path.join(temporary, "results")
            direct = os.path.join(results, "fx-a01")
            nested = os.path.join(results, "run-id", "fx-a01")
            for path, content in ((direct, b"first\n"), (nested, b"different\n")):
                os.makedirs(path)
                with open(os.path.join(path, "final.txt"), "wb") as handle:
                    handle.write(content)

            with self.assertRaisesRegex(ValueError, "duplicate output directories differ"):
                verify_fixture_run.fixture_output(results, "fx-a01")

    def test_fixture_selection_preserves_the_requested_order(self):
        selected = verify_fixture_run.select_fixtures({"fixtures": [
            {"id": "fx-a01"}, {"id": "fx-a02"}, {"id": "fx-a03"},
        ]}, ["fx-a03", "fx-a01"])
        self.assertEqual(["fx-a03", "fx-a01"], [fixture["id"] for fixture in selected])

    def test_fixture_selection_rejects_duplicates_and_unknown_ids(self):
        manifest = {"fixtures": [{"id": "fx-a01"}, {"id": "fx-a02"}]}
        for ids in (["fx-a01", "fx-a01"], ["fx-a99"]):
            with self.subTest(ids=ids):
                with self.assertRaisesRegex(ValueError, "fixture selection"):
                    verify_fixture_run.select_fixtures(manifest, ids)


class PromptCaptureVerificationTest(unittest.TestCase):
    @staticmethod
    def write_captures(output, phases, leak_phase=None, leak=b""):
        prompt_root = os.path.join(output, "prompts")
        os.makedirs(prompt_root)
        captured = {}
        for index, phase in enumerate(phases, start=1):
            data = leak if phase == leak_phase else ("prompt " + phase).encode("utf-8")
            name = f"{index:03d}-{phase}.txt"
            with open(os.path.join(prompt_root, name), "wb") as handle:
                handle.write(data)
            captured[phase] = data
        if "L2_EDIT" in captured:
            with open(os.path.join(output, "l2-edit-prompt.txt"), "wb") as handle:
                handle.write(captured["L2_EDIT"])

    def test_l1_only_requires_only_its_two_captured_prompts(self):
        with tempfile.TemporaryDirectory() as temporary:
            phases = verify_fixture_run.expected_prompt_phases("L1_ONLY")
            self.write_captures(temporary, phases)
            self.assertEqual(2, len(verify_fixture_run.check_prompt_captures(temporary, "L1_ONLY")))

    def test_reused_l1_then_l2_requires_only_the_three_l2_prompts(self):
        with tempfile.TemporaryDirectory() as temporary:
            phases = verify_fixture_run.expected_prompt_phases("L1_THEN_L2", reused_l1=True)
            self.write_captures(temporary, phases)
            self.assertEqual(3, len(verify_fixture_run.check_prompt_captures(
                temporary, "L1_THEN_L2", reused_l1=True)))

    def test_missing_phase_prompt_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            phases = verify_fixture_run.expected_prompt_phases("CHAIN")[:-1]
            self.write_captures(temporary, phases)
            with self.assertRaisesRegex(ValueError, "phase count"):
                verify_fixture_run.check_prompt_captures(temporary, "CHAIN")

    def test_live_l3_records_three_billable_calls_and_eight_pipeline_calls(self):
        self.assertEqual((3, 8), verify_fixture_run.expected_live_call_counts("L3_ONLY"))
        self.assertEqual((3, 3), verify_fixture_run.expected_live_call_counts("L1_THEN_L2"))

    def test_post_run_oracle_guard_checks_every_captured_prompt(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixture_root = os.path.join(temporary, "fixtures")
            output = os.path.join(temporary, "output")
            os.makedirs(os.path.join(fixture_root, "fx-a01"))
            os.makedirs(os.path.join(fixture_root, "_labels"))
            os.makedirs(output)
            for name in ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv"):
                with open(os.path.join(fixture_root, "fx-a01", name), "wb") as handle:
                    handle.write(b"visible source\n")
            with open(os.path.join(fixture_root, "_labels", "fx-a01.json"), "w", encoding="utf-8") as handle:
                import json
                json.dump({"targets": [{"id": "T-001", "mustContain": ["correct answer phrase"]}]}, handle)
            with open(os.path.join(output, "report-l1.json"), "wb") as handle:
                handle.write(b"{}")
            phases = verify_fixture_run.expected_prompt_phases("L1_ONLY")
            self.write_captures(output, phases, leak_phase="L1_RECONCILE", leak=b"correct answer phrase")
            fixture = {"id": "fx-a01", "labels": {"path": "_labels/fx-a01.json"}}
            with self.assertRaisesRegex(ValueError, "corrected label text"):
                verify_fixture_run.check_oracle_probe(fixture, output, fixture_root, "L1_ONLY")


if __name__ == "__main__":
    unittest.main()
