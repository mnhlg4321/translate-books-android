#!/usr/bin/env python3
"""Unit tests for pre-dispatch fixture and template leak checks."""
import hashlib
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import verify_prompt_inputs  # noqa: E402


class PromptInputGuardTest(unittest.TestCase):
    def setUp(self):
        self.labels = {"targets": [{
            "id": "T-001",
            "old": "wrong source phrase",
            "mustContain": ["correct answer phrase"],
        }]}

    def test_clean_source_and_template_pass(self):
        errors = verify_prompt_inputs.check_model_surface(
            "fx-a01", self.labels, b"wrong source phrase", [b"generic prompt rules"])
        self.assertEqual([], errors)

    def test_template_cannot_contain_answer_missing_from_source(self):
        errors = verify_prompt_inputs.check_model_surface(
            "fx-a01", self.labels, b"wrong source phrase", [b"correct answer phrase"])
        self.assertIn("CORRECTED_LABEL_TEXT_PRESENT", errors)

    def test_target_id_and_label_field_names_are_rejected(self):
        for leak in (b"T-001", b"mustContain", b"mustNotContain"):
            with self.subTest(leak=leak):
                errors = verify_prompt_inputs.check_model_surface(
                    "fx-a01", self.labels, b"source only", [leak])
                self.assertTrue(errors)

    def test_source_text_marked_as_old_is_allowed_when_it_is_visible(self):
        errors = verify_prompt_inputs.check_model_surface(
            "fx-a01", self.labels, b"wrong source phrase", [b"wrong source phrase"])
        self.assertEqual([], errors)

    def test_guard_checks_exact_transferred_source_bytes_against_manifest(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixtures_root = os.path.join(temporary, "fixtures")
            transfer_root = os.path.join(temporary, "transfer")
            os.makedirs(os.path.join(fixtures_root, "_labels"))
            os.makedirs(os.path.join(transfer_root, "fx-a01"))
            files = {}
            for name in verify_prompt_inputs.SOURCE_NAMES:
                data = b"wrong source phrase\n"
                with open(os.path.join(transfer_root, "fx-a01", name), "wb") as handle:
                    handle.write(data)
                files[name] = {"bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
            labels_bytes = json.dumps(self.labels).encode("utf-8")
            with open(os.path.join(fixtures_root, "_labels", "fx-a01.json"), "wb") as handle:
                handle.write(labels_bytes)
            fixture = {
                "id": "fx-a01", "files": files,
                "labels": {"path": "_labels/fx-a01.json", "sha256": hashlib.sha256(labels_bytes).hexdigest()},
            }
            self.assertEqual([], verify_prompt_inputs.check_transfer_fixture(
                fixture, fixtures_root, transfer_root, [b"generic template"]))
            with open(os.path.join(transfer_root, "fx-a01", "DRAFT.txt"), "ab") as handle:
                handle.write(b"tampered")
            self.assertEqual(["TRANSFER_SOURCE_HASH_MISMATCH"], verify_prompt_inputs.check_transfer_fixture(
                fixture, fixtures_root, transfer_root, [b"generic template"]))


if __name__ == "__main__":
    unittest.main()
