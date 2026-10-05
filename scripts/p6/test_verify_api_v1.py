#!/usr/bin/env python3
"""EDITORIAL_API_V1 outputs: layout of the captures, the run shape per mode, the label-leak probe and the group policy."""
import json
import os
import sys
import tempfile
import types
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import group_policy  # noqa: E402
import verify_fixture_run  # noqa: E402

FIXTURE = {"id": "fx-a01", "labels": {"path": "labels.json"}}


def ledger_rows(calls):
    prior, rows, sequence = "0" * 64, [], 0
    for index in range(calls):
        for kind, amount in (("RESERVE", "0.01"), ("SETTLE", "0.001")):
            sequence += 1
            row = {"sequence": sequence, "previousHash": prior, "groupMaximumUsd": "0.50", "callId": "c%d" % index,
                   "kind": kind, "phase": "EDIT" if index == 0 else "CHECK", "amountUsd": amount}
            digest = verify_fixture_run.sha(verify_fixture_run.canonical(row).encode("utf-8"))
            row["entryHash"] = digest
            prior = digest
            rows.append(json.dumps(row, sort_keys=True))
    return "\n".join(rows) + "\n"


def build(temporary, steps=None, state="FINAL_OK", mode="QUICK", leak=b"", live=False, final=b"final text"):
    if steps is None:
        steps = ("EDIT",) if mode == "QUICK" else ("EDIT", "CHECK")
    fixtures = os.path.join(temporary, "fixtures")
    output = os.path.join(temporary, "out")
    os.makedirs(os.path.join(fixtures, "fx-a01"))
    os.makedirs(os.path.join(output, "prompts"))
    os.makedirs(os.path.join(output, "responses"))
    for name in ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv"):
        with open(os.path.join(fixtures, "fx-a01", name), "wb") as handle:
            handle.write(b"ordinary source")
    with open(os.path.join(fixtures, "labels.json"), "w") as handle:
        json.dump({"targets": [{"id": "hidden-target", "mustContain": ["gold_answer"]}]}, handle)
    for index, step in enumerate(steps, start=1):
        with open(os.path.join(output, "prompts", "%03d-%s.txt" % (index, step)), "wb") as handle:
            handle.write(b"prompt " + step.encode() + leak)
        with open(os.path.join(output, "responses", "%03d-%s.json" % (index, step)), "wb") as handle:
            handle.write(b"{}")
    with open(os.path.join(output, "final.txt"), "wb") as handle:
        handle.write(final)
    with open(os.path.join(output, "spend-ledger.jsonl"), "w") as handle:
        handle.write(ledger_rows(len(steps)))
    calls = len(steps)
    finished = state in ("FINAL_OK", "FINAL_NOTES")
    structural = {"valid": finished, "reasonCode": state, "stage": "API_V1", "providerCalls": calls,
                  "stops": [] if finished else ["P6_API_V1_" + state]}
    metadata = {"mode": "API_V1_" + mode, "providerKind": "LIVE" if live else "FAKE_OFFLINE",
                "actualProviderCalls": calls if live else 0, "fakeProviderCalls": 0 if live else calls,
                "finalSha256": verify_fixture_run.sha(final), "model": "m", "route": "r",
                "contractRevision": "EDITORIAL_API_V1.1", "qualityCoreSha256": "a" * 64, "sourceCommit": "abc",
                "apkVersionName": "1", "apiV1": {"mode": mode, "state": state}}
    return fixtures, output, structural, metadata


def check(fixtures, output, structural, metadata, mode="QUICK", live=False, measure=False):
    args = types.SimpleNamespace(mode="API_V1_" + mode, live=live, measure_refusals=measure)
    return verify_fixture_run.check_api_fixture(FIXTURE, output, fixtures, args, structural, metadata)


class ApiV1VerificationTest(unittest.TestCase):
    def test_a_clean_quick_run_is_accepted(self):
        with tempfile.TemporaryDirectory() as temporary:
            entry = check(*build(temporary))
            self.assertEqual(1, entry["providerCalls"])
            self.assertEqual(0, entry["ledger"]["pending"])

    def test_thorough_may_check_and_recheck_but_quick_is_the_edit_alone(self):
        with tempfile.TemporaryDirectory() as temporary:
            self.assertEqual(2, check(*build(temporary, mode="THOROUGH"), mode="THOROUGH")["providerCalls"])
        with tempfile.TemporaryDirectory() as temporary:
            steps = ("EDIT", "CHECK", "RECHECK")
            self.assertEqual(3, check(*build(temporary, mode="THOROUGH", steps=steps), mode="THOROUGH")["providerCalls"])
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "QUICK must not check"):
                check(*build(temporary, mode="QUICK", steps=("EDIT", "CHECK")))
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "only one technical retry"):
                check(*build(temporary, mode="QUICK", steps=("EDIT", "EDIT", "EDIT")))
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "at least an edit and a check"):
                check(*build(temporary, mode="THOROUGH", steps=("EDIT",)), mode="THOROUGH")

    def test_a_run_that_does_not_begin_with_the_edit_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "start with the edit"):
                check(*build(temporary, steps=("CHECK", "CHECK")))

    def test_a_label_phrase_in_a_prompt_before_any_answer_produced_it_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "corrected label text"):
                check(*build(temporary, leak=b" gold_answer"))

    def test_a_label_field_name_in_a_prompt_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "label field name"):
                check(*build(temporary, leak=b" mustContain"))

    def test_an_unfinished_run_is_reported_only_when_refusals_are_measured(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(ValueError, "did not finish"):
                check(*build(temporary, state="WRONG_PAIR"))
        with tempfile.TemporaryDirectory() as temporary:
            entry = check(*build(temporary, state="WRONG_PAIR"), measure=True)
            self.assertEqual(["P6_API_V1_WRONG_PAIR"], entry["refusal"])

    def test_a_fake_run_that_claims_an_actual_call_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixtures, output, structural, metadata = build(temporary)
            metadata["actualProviderCalls"] = 2
            with self.assertRaisesRegex(ValueError, "non-fake provider"):
                check(fixtures, output, structural, metadata)

    def test_a_live_run_needs_one_actual_call_per_request(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixtures, output, structural, metadata = build(temporary, live=True)
            self.assertEqual(1, check(fixtures, output, structural, metadata, live=True)["providerCalls"])
            metadata["actualProviderCalls"] = 0
            with self.assertRaisesRegex(ValueError, "live provider run"):
                check(fixtures, output, structural, metadata, live=True)

    def test_a_changed_final_text_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixtures, output, structural, metadata = build(temporary)
            with open(os.path.join(output, "final.txt"), "wb") as handle:
                handle.write(b"tampered")
            with self.assertRaisesRegex(ValueError, "final hash mismatch"):
                check(fixtures, output, structural, metadata)

    def test_a_missing_quality_core_hash_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixtures, output, structural, metadata = build(temporary)
            metadata["qualityCoreSha256"] = "none"
            with self.assertRaisesRegex(ValueError, "Quality Core"):
                check(fixtures, output, structural, metadata)

    def test_a_missing_capture_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            fixtures, output, structural, metadata = build(temporary, mode="THOROUGH")
            os.remove(os.path.join(output, "responses", "002-CHECK.json"))
            with self.assertRaisesRegex(ValueError, "capture count"):
                check(fixtures, output, structural, metadata, mode="THOROUGH")


class ApiV1GroupPolicyTest(unittest.TestCase):
    def test_a_finished_run_is_valid(self):
        structural = {"valid": True, "stage": "API_V1", "reasonCode": "FINAL_NOTES", "stops": []}
        self.assertEqual((group_policy.VALID, "", ""), group_policy.classify(structural, True))

    def test_a_wrong_pair_is_a_finding_not_an_infrastructure_stop(self):
        structural = {"valid": False, "stage": "API_V1", "reasonCode": "WRONG_PAIR", "stops": ["P6_API_V1_WRONG_PAIR"]}
        self.assertEqual(group_policy.REFUSED, group_policy.classify(structural, True)[0])

    def test_a_retry_required_run_stops_the_group(self):
        structural = {"valid": False, "stage": "API_V1", "reasonCode": "RETRY_REQUIRED", "stops": ["P6_API_V1_RETRY_REQUIRED"]}
        outcome, _, why = group_policy.classify(structural, True)
        self.assertEqual(group_policy.INFRASTRUCTURE, outcome)
        self.assertEqual("API_V1_RETRY_REQUIRED", why)


if __name__ == "__main__":
    unittest.main()
