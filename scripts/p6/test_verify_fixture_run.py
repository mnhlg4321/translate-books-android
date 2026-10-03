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


if __name__ == "__main__":
    unittest.main()
