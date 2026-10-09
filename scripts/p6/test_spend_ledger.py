#!/usr/bin/env python3
"""Group spend checks stop before a fixture dispatch when cap or UNKNOWN state blocks it."""
import hashlib
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spend_ledger  # noqa: E402


def append(path, previous, sequence, kind, call_id, amount, phase="L1_RAW_DISCOVERY"):
    row = {"amountUsd": amount, "callId": call_id, "groupId": "G1-test",
           "groupMaximumUsd": "1", "kind": kind, "phase": phase,
           "previousHash": previous, "sequence": sequence}
    digest = hashlib.sha256(spend_ledger.canonical(row).encode("utf-8")).hexdigest()
    row["entryHash"] = digest
    with open(path, "a", encoding="utf-8", newline="\n") as handle:
        handle.write(spend_ledger.canonical(row) + "\n")
    return digest


class GroupSpendGuardTest(unittest.TestCase):
    def test_fixture_that_would_exceed_group_cap_is_refused_before_dispatch(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = os.path.join(temporary, "group.jsonl")
            prior = append(path, spend_ledger.GENESIS, 1, "RESERVE", "c1", "0.94")
            append(path, prior, 2, "SETTLE", "c1", "0.94")
            snapshot = spend_ledger.inspect(path, "G1-test", "1")
            with self.assertRaisesRegex(ValueError, "P6_SPEND_GROUP_CAP_WOULD_BE_EXCEEDED"):
                spend_ledger.preflight(snapshot, "0.08")

    def test_unknown_cost_stops_the_group_before_the_next_fixture(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = os.path.join(temporary, "group.jsonl")
            append(path, spend_ledger.GENESIS, 1, "RESERVE", "c1", "0.08")
            snapshot = spend_ledger.inspect(path, "G1-test", "1")
            self.assertEqual(1, snapshot["pending"])
            with self.assertRaisesRegex(ValueError, "P6_SPEND_UNKNOWN_PENDING"):
                spend_ledger.preflight(snapshot, "0.08")

    def test_cumulative_spend_survives_a_host_restart_snapshot(self):
        with tempfile.TemporaryDirectory() as temporary:
            device_copy = os.path.join(temporary, "device-ledger.jsonl")
            host_snapshot = os.path.join(temporary, "after-fixture.jsonl")
            prior = append(device_copy, spend_ledger.GENESIS, 1, "RESERVE", "c1", "0.08")
            append(device_copy, prior, 2, "SETTLE", "c1", "0.07")
            with open(device_copy, "rb") as source, open(host_snapshot, "wb") as target:
                target.write(source.read())
            snapshot = spend_ledger.inspect(host_snapshot, "G1-test", "1")
            self.assertEqual("0.07", snapshot["exposureUsd"])
            self.assertEqual("0.93", spend_ledger.preflight(snapshot, "0.08")["remainingUsd"])


def reserve_row(path, call_id="c6", amount="0.002066", cap="0.03", group="C6-test"):
    row = {"amountUsd": amount, "callId": call_id, "groupId": group, "groupMaximumUsd": cap, "kind": "RESERVE",
           "phase": "EDIT", "previousHash": spend_ledger.GENESIS, "sequence": 1}
    row["entryHash"] = hashlib.sha256(spend_ledger.canonical({k: v for k, v in row.items() if k != "entryHash"}).encode("utf-8")).hexdigest()
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(spend_ledger.canonical(row) + "\n")
    return row["entryHash"]


def raw_overrun_row(prior, call_id="c6", amount="0.0024366", provider="0.0024366", reserved="0.002066", reason=spend_ledger.OVERRUN_REASON,
                    sequence=2, group="C6-test", cap="0.03"):
    row = {"amountUsd": amount, "callId": call_id, "evidence": "test", "groupId": group, "groupMaximumUsd": cap,
           "kind": spend_ledger.OVERRUN_KIND, "phase": "EDIT", "previousHash": prior, "providerCostUsd": provider,
           "reason": reason, "reservedUsd": reserved, "sequence": sequence}
    row["entryHash"] = hashlib.sha256(spend_ledger.canonical({k: v for k, v in row.items() if k != "entryHash"}).encode("utf-8")).hexdigest()
    return row


class OverrunCloseTest(unittest.TestCase):
    def setUp(self):
        self._dir = tempfile.TemporaryDirectory()
        self.path = os.path.join(self._dir.name, "C6-test.jsonl")
        self.prior = reserve_row(self.path)

    def tearDown(self):
        self._dir.cleanup()

    def lines(self):
        with open(self.path, encoding="utf-8") as handle:
            return handle.read()

    def test_provider_cost_above_the_reservation_closes_it_and_leaves_nothing_pending(self):
        before = spend_ledger.inspect(self.path, "C6-test", "0.03")
        self.assertEqual(1, before["pending"])
        first_line = self.lines()
        after = spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.0024366", "C6.5 run 887c; cost-overrun.txt")
        self.assertEqual(0, after["pending"])
        self.assertEqual("0.0024366", after["settledUsd"])
        self.assertEqual("0.0024366", after["exposureUsd"])
        self.assertEqual(1, after["overrunClosures"])
        self.assertEqual(2, after["entries"])
        # the existing line is untouched: the record is appended, not edited
        self.assertTrue(self.lines().startswith(first_line))

    def test_the_closing_record_carries_the_provider_cost_and_the_reservation_it_closes(self):
        spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.0024366", "evidence")
        row = json.loads(self.lines().split("\n")[1])
        self.assertEqual("SETTLE_OVERRUN", row["kind"])
        self.assertEqual("0.0024366", row["providerCostUsd"])
        self.assertEqual("0.0024366", row["amountUsd"])
        self.assertEqual("0.002066", row["reservedUsd"])
        self.assertEqual(spend_ledger.OVERRUN_REASON, row["reason"])
        self.assertEqual(self.prior, row["previousHash"])

    def test_a_cost_that_is_not_above_the_reservation_is_refused(self):
        with self.assertRaisesRegex(ValueError, "P6_SPEND_OVERRUN_NOT_ABOVE_RESERVATION"):
            spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.002066")
        self.assertEqual(1, spend_ledger.inspect(self.path, "C6-test", "0.03")["pending"])

    def test_an_unknown_or_already_settled_call_is_refused(self):
        with self.assertRaisesRegex(ValueError, "P6_SPEND_OVERRUN_CALL_UNKNOWN"):
            spend_ledger.close_overrun(self.path, "C6-test", "0.03", "no-such-call", "0.0024366")
        spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.0024366")
        with self.assertRaisesRegex(ValueError, "P6_SPEND_OVERRUN_ALREADY_SETTLED"):
            spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.003")

    def test_a_closing_record_whose_amount_differs_from_the_provider_cost_is_refused_by_the_verifier(self):
        row = raw_overrun_row(self.prior, amount="0.0025", provider="0.0024366")
        with open(self.path, "a", encoding="utf-8", newline="\n") as handle:
            handle.write(spend_ledger.canonical(row) + "\n")
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID"):
            spend_ledger.inspect(self.path, "C6-test", "0.03")

    def test_a_closing_record_for_a_reservation_it_does_not_name_is_refused(self):
        row = raw_overrun_row(self.prior, reserved="0.001")
        with open(self.path, "a", encoding="utf-8", newline="\n") as handle:
            handle.write(spend_ledger.canonical(row) + "\n")
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID"):
            spend_ledger.inspect(self.path, "C6-test", "0.03")

    def test_a_closing_record_with_another_reason_is_refused(self):
        row = raw_overrun_row(self.prior, reason="OTHER")
        with open(self.path, "a", encoding="utf-8", newline="\n") as handle:
            handle.write(spend_ledger.canonical(row) + "\n")
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID"):
            spend_ledger.inspect(self.path, "C6-test", "0.03")

    def test_a_closing_record_that_is_not_an_overrun_is_refused_by_the_verifier(self):
        row = raw_overrun_row(self.prior, amount="0.002", provider="0.002", reserved="0.002066")
        with open(self.path, "a", encoding="utf-8", newline="\n") as handle:
            handle.write(spend_ledger.canonical(row) + "\n")
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_OVERRUN_CLOSE_NOT_OVERRUN"):
            spend_ledger.inspect(self.path, "C6-test", "0.03")

    def test_editing_a_closed_line_breaks_the_hash_chain(self):
        spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.0024366")
        edited = self.lines().replace('"amountUsd":"0.0024366"', '"amountUsd":"0.0020000"', 1)
        with open(self.path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(edited)
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_HASH_CHAIN_INVALID"):
            spend_ledger.inspect(self.path, "C6-test", "0.03")

    def test_a_closure_that_would_exceed_the_group_cap_is_refused_and_writes_nothing(self):
        capped = os.path.join(self._dir.name, "capped.jsonl")
        reserve_row(capped, amount="0.002", cap="0.003", group="C6-test")
        with open(capped, "rb") as handle:
            before = handle.read()
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_GROUP_CAP_OVERRUN"):
            spend_ledger.close_overrun(capped, "C6-test", "0.003", "c6", "0.0035")
        with open(capped, "rb") as handle:
            self.assertEqual(before, handle.read())

    def test_the_c6_20261010_shape_closes_at_the_reported_cost(self):
        # same amounts as the group that stopped at its first call: reservation 0.002066, provider cost 0.0024366, cap 0.03
        real = os.path.join(self._dir.name, "C6-20261010.jsonl")
        reserve_row(real, call_id="90a2de67", amount="0.002066", cap="0.03", group="C6-20261010")
        after = spend_ledger.close_overrun(real, "C6-20261010", "0.03", "90a2de67", "0.0024366",
                                           "C6.5 run 887c0553; cost-overrun.txt 90a2de67")
        self.assertEqual({"pending": 0, "settledUsd": "0.0024366", "exposureUsd": "0.0024366", "overrunClosures": 1},
                         {key: after[key] for key in ("pending", "settledUsd", "exposureUsd", "overrunClosures")})

    def test_the_group_check_refuses_a_second_dispatch_until_the_closure_exists(self):
        snapshot = spend_ledger.inspect(self.path, "C6-test", "0.03")
        with self.assertRaisesRegex(ValueError, "P6_SPEND_UNKNOWN_PENDING"):
            spend_ledger.preflight(snapshot, "0.001")
        closed = spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.0024366")
        self.assertEqual("0.0275634", spend_ledger.preflight(closed, "0.001")["remainingUsd"])

    def test_a_ledger_without_a_final_newline_is_not_appended_to(self):
        with open(self.path, "rb") as handle:
            data = handle.read().rstrip(b"\n")
        with open(self.path, "wb") as handle:
            handle.write(data)
        with self.assertRaisesRegex(ValueError, "P6_SPEND_LEDGER_NOT_LINE_TERMINATED"):
            spend_ledger.close_overrun(self.path, "C6-test", "0.03", "c6", "0.0024366")


if __name__ == "__main__":
    unittest.main()
