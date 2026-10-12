import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import group_policy as gp  # noqa: E402


def refusal(code, reason="REPAIR_L1_LEDGER_INVALID", measured=True):
    return {"valid": False, "measuredRefusal": measured, "reasonCode": "P6_L1_PREDECESSOR_FAILED:" + reason,
            "stops": ["P6_L1_PREDECESSOR_FAILED:" + reason, "phase=L1_RECONCILE", code]}


class ClassifyTest(unittest.TestCase):
    def test_valid_fixture(self):
        self.assertEqual((gp.VALID, "", ""), gp.classify({"valid": True, "stops": []}, True))

    def test_typed_refusal_is_a_measurement_with_its_code(self):
        outcome, code, why = gp.classify(refusal("L1_RAW_QUOTE_AMBIGUOUS:findings.1.rawQuote"), True)
        self.assertEqual((gp.REFUSED, "L1_RAW_QUOTE_AMBIGUOUS:findings.1.rawQuote"), (outcome, code))
        self.assertEqual("REPAIR_L1_LEDGER_INVALID", why)

    def test_refusal_without_a_detail_code_still_counts_by_its_engine_reason(self):
        structural = {"valid": False, "measuredRefusal": True, "stops": ["P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID"]}
        self.assertEqual((gp.REFUSED, "REPAIR_L1_LEDGER_INVALID"), gp.classify(structural, True)[:2])

    def test_refusal_not_recorded_as_measurement_is_infrastructure(self):
        self.assertEqual(gp.INFRASTRUCTURE, gp.classify(refusal("L1_X_Y:root", measured=False), True)[0])

    def test_provider_and_budget_failures_are_infrastructure(self):
        for reason in ("RETRY_L1_PROVIDER_CALL_FAILED", "RETRY_L1_OUTPUT_TRUNCATED", "STOP_L1_EXTERNAL_CALL_STATE_UNRESOLVED",
                       "L1_TOKEN_OR_COST_BUDGET_EXCEEDED"):
            self.assertEqual(gp.INFRASTRUCTURE, gp.classify(refusal("L1_X_Y:root", reason), True)[0], reason)

    def test_missing_output_harness_crash_and_unknown_cost_are_infrastructure(self):
        self.assertEqual(gp.INFRASTRUCTURE, gp.classify(None, False)[0])
        self.assertEqual("UNKNOWN_COST", gp.classify({"valid": True}, True, unknown_cost_calls=1)[2])
        self.assertEqual(gp.INFRASTRUCTURE, gp.classify({"valid": False, "reasonCode": "P6_FIXTURE_RAW_EMPTY",
                                                         "stops": ["P6_FIXTURE_RAW_EMPTY"]}, False)[0])
        self.assertEqual(gp.INFRASTRUCTURE, gp.classify({"valid": True, "stops": []}, False)[0])


class DecideTest(unittest.TestCase):
    def entry(self, code, outcome=gp.REFUSED):
        return {"outcome": outcome, "code": code}

    def test_a_refusal_does_not_stop_the_group(self):
        self.assertEqual(("CONTINUE", ""), gp.decide([], gp.REFUSED, "L1_A_B:x.0"))
        self.assertEqual(("CONTINUE", ""), gp.decide([self.entry("L1_A_B:x.0")], gp.REFUSED, "L1_C_D:y"))

    def test_infrastructure_unknown_cost_and_cap_stop(self):
        self.assertEqual(("STOP", "INFRASTRUCTURE"), gp.decide([], gp.INFRASTRUCTURE, ""))
        self.assertEqual(("STOP", "GROUP_CAP"), gp.decide([], gp.VALID, "", remaining_ok=False))

    def test_three_consecutive_refusals_with_the_same_code_stop(self):
        history = [self.entry("L1_A_B:findings.0.x"), self.entry("L1_A_B:findings.2.y")]
        action, reason = gp.decide(history, gp.REFUSED, "L1_A_B:findings.1.z")
        self.assertEqual("STOP", action)
        self.assertTrue(reason.endswith("L1_A_B"), reason)

    def test_two_of_a_kind_or_an_interruption_does_not_stop(self):
        self.assertEqual("CONTINUE", gp.decide([self.entry("L1_A_B:x")], gp.REFUSED, "L1_A_B:y")[0])
        history = [self.entry("L1_A_B:x"), self.entry("", gp.VALID)]
        self.assertEqual("CONTINUE", gp.decide(history, gp.REFUSED, "L1_A_B:y")[0])
        history = [self.entry("L1_A_B:x"), self.entry("L1_C_D:x")]
        self.assertEqual("CONTINUE", gp.decide(history, gp.REFUSED, "L1_A_B:y")[0])

    def test_only_the_last_three_count(self):
        history = [self.entry("L1_A_B:x"), self.entry("L1_A_B:x"), self.entry("", gp.VALID), self.entry("L1_A_B:x")]
        self.assertEqual("CONTINUE", gp.decide(history, gp.REFUSED, "L1_A_B:x")[0])


class CommandTest(unittest.TestCase):
    def run_main(self, directory, structural, exit_code, fixture, remaining="yes", metadata=None):
        paths = {}
        if structural is not None:
            paths["structural"] = os.path.join(directory, fixture + "-structural.json")
            with open(paths["structural"], "w", encoding="utf-8") as handle:
                json.dump(structural, handle)
        args = ["--instrumentation-exit", str(exit_code), "--history", os.path.join(directory, "history.jsonl"),
                "--fixture", fixture, "--remaining-ok", remaining]
        if "structural" in paths:
            args += ["--structural", paths["structural"]]
        if metadata is not None:
            paths["metadata"] = os.path.join(directory, fixture + "-meta.json")
            with open(paths["metadata"], "w", encoding="utf-8") as handle:
                json.dump(metadata, handle)
            args += ["--metadata", paths["metadata"]]
        self.assertEqual(0, gp.main(args))

    def test_history_is_kept_and_three_same_codes_stop_the_third_fixture(self):
        with tempfile.TemporaryDirectory() as directory:
            for index in (1, 2, 3):
                self.run_main(directory, refusal("L1_A_B:findings.%d.x" % index), 1, "fx-%d" % index)
            with open(os.path.join(directory, "history.jsonl"), encoding="utf-8") as handle:
                rows = [json.loads(line) for line in handle]
            self.assertEqual(["CONTINUE", "CONTINUE", "STOP"], [row["action"] for row in rows])
            self.assertEqual(gp.REFUSED, rows[2]["outcome"])

    def test_unknown_cost_in_metadata_stops_even_a_valid_fixture(self):
        with tempfile.TemporaryDirectory() as directory:
            self.run_main(directory, {"valid": True, "stops": []}, 0, "fx-1", metadata={"unknownCostCalls": 1})
            with open(os.path.join(directory, "history.jsonl"), encoding="utf-8") as handle:
                self.assertEqual("STOP", json.loads(handle.readline())["action"])

    def test_missing_structural_output_stops(self):
        with tempfile.TemporaryDirectory() as directory:
            self.run_main(directory, None, 1, "fx-1")
            with open(os.path.join(directory, "history.jsonl"), encoding="utf-8") as handle:
                row = json.loads(handle.readline())
            self.assertEqual(("STOP", gp.INFRASTRUCTURE), (row["action"], row["outcome"]))


class WiringTest(unittest.TestCase):
    """The PowerShell group runner and the instrumented runner must carry the rule (no host emulator needed)."""

    def read(self, relative):
        with open(os.path.join(HERE, "..", "..", *relative.split("/")), encoding="utf-8") as handle:
            return handle.read()

    def test_group_runner_uses_the_policy_and_the_flag(self):
        script = self.read("scripts/p6/run_group.ps1")
        for needle in ("group_policy.py", "p6_measure_refusals", "StopOnFirstRefusal", "--measure-refusals", "group-decisions.jsonl"):
            self.assertIn(needle, script)
        # the old unconditional stop must only remain on the opt-out path
        self.assertIn("elseif ($Failed.Contains($FixtureId))", script)

    def test_instrumented_runner_records_a_typed_refusal_instead_of_failing(self):
        source = self.read("app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP6FixtureRunnerInstrumentedTest.java")
        for needle in ("p6_measure_refusals", "measuredRefusal", "refusedByEngine"):
            self.assertIn(needle, source)


if __name__ == "__main__":
    unittest.main()
