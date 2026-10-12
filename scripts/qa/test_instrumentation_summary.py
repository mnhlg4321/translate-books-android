"""Synthetic tests for instrumentation_summary: no device and no real log is needed."""
import os
import sys
import unittest
import contextlib
import io
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import instrumentation_summary as s  # noqa: E402

CLS = "com.example.FooInstrumentedTest"


def status(test, code, numtests, extra=""):
    return ("INSTRUMENTATION_STATUS: class=%s\nINSTRUMENTATION_STATUS: current=1\nINSTRUMENTATION_STATUS: numtests=%d\n"
            "%sINSTRUMENTATION_STATUS: test=%s\nINSTRUMENTATION_STATUS_CODE: %d\n") % (CLS, numtests, extra, test, test and code)


def method(test, final_code, numtests, stack=""):
    extra = ("INSTRUMENTATION_STATUS: stack=%s\n\tat x.Y(Y.java:1)\n" % stack) if stack else ""
    return status(test, 1, numtests) + status(test, final_code, numtests, extra)


class SummaryTest(unittest.TestCase):
    def test_pass_skip_fail_are_separated(self):
        log = method("a", 0, 4) + method("b", -4, 4, "org.junit.AssumptionViolatedException: x") + method("c", -2, 4, "AssertionError") \
            + method("d", -3, 4) + "\nFAILURES!!!\nTests run: 4,  Failures: 1\n"
        r = s.summarize(log)
        self.assertEqual((r["discovered"], r["pass"], r["skip"], r["fail"]), (4, 1, 2, 1))
        self.assertEqual(r["overall"], s.FAIL)

    def test_ok_line_does_not_turn_skips_into_passes(self):
        log = method("a", 0, 3) + method("b", -4, 3) + method("c", -4, 3) + "\nOK (3 tests)\n"
        r = s.summarize(log)
        self.assertEqual((r["pass"], r["skip"], r["fail"], r["ok_line_tests"]), (1, 2, 0, 3))
        self.assertEqual(r["overall"], s.PASS)

    def test_all_skipped_is_skip_not_pass(self):
        r = s.summarize(method("a", -4, 1) + "\nOK (1 test)\n")
        self.assertEqual((r["pass"], r["skip"], r["overall"]), (0, 1, s.SKIP))

    def test_started_without_result_is_incomplete(self):
        r = s.summarize(status("a", 1, 2) + method("b", 0, 2))
        self.assertEqual((r["incomplete"], r["overall"]), (1, s.INCOMPLETE))

    def test_empty_log_and_missing_methods_are_incomplete(self):
        self.assertEqual(s.summarize("")["overall"], s.INCOMPLETE)
        r = s.summarize(method("a", 0, 3))
        self.assertEqual(r["overall"], s.INCOMPLETE)
        self.assertTrue(r["notes"])

    def test_error_code_is_fail_and_unknown_code_is_never_pass(self):
        self.assertEqual(s.summarize(method("a", -1, 1))["fail"], 1)
        self.assertEqual(s.summarize(method("a", 7, 1))["fail"], 1)

    def test_bom_and_crlf_logs(self):
        log = "﻿" + (method("a", 0, 2) + method("b", -4, 2)).replace("\n", "\r\n")
        r = s.summarize(log)
        self.assertEqual((r["pass"], r["skip"]), (1, 1))

    def test_a_restarted_method_cannot_mask_its_failure(self):
        r = s.summarize(method("a", -2, 1) + status("a", 1, 1) + status("a", 0, 1))
        self.assertEqual(r["discovered"], 1)
        self.assertEqual(r["overall"], s.INCOMPLETE)
        self.assertEqual(r["fail"], 1)

    def test_failure_banner_cannot_be_overruled_by_pass_status(self):
        self.assertEqual(s.summarize(method("a", 0, 1) + "\nFAILURES!!!\n")["overall"], s.FAIL)

    def test_ok_count_mismatch_is_incomplete(self):
        self.assertEqual(s.summarize(method("a", 0, 1) + "\nOK (2 tests)\n")["overall"], s.INCOMPLETE)

    def test_duplicate_result_is_not_a_clean_run(self):
        self.assertEqual(s.summarize(method("a", -2, 1) + status("a", 0, 1))["overall"], s.INCOMPLETE)

    def test_cli_fails_for_run_errors_even_without_failed_method_counts(self):
        cases = ["", method("a", 0, 2), method("a", 0, 1) + "\nFAILURES!!!\n",
                 method("a", 0, 1) + "\nOK (2 tests)\n",
                 method("a", -2, 1) + method("a", 0, 1)]
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "run.txt")
            for log in cases:
                with self.subTest(log=log):
                    with open(path, "w", encoding="utf-8") as stream:
                        stream.write(log)
                    with contextlib.redirect_stdout(io.StringIO()):
                        self.assertEqual(s.main([path]), 1)

    def test_cli_pass_and_intentional_skip_keep_zero_exit(self):
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "run.txt")
            for code in (0, -4):
                with open(path, "w", encoding="utf-8") as stream:
                    stream.write(method("a", code, 1))
                with contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(s.main([path]), 0)


if __name__ == "__main__":
    unittest.main()
