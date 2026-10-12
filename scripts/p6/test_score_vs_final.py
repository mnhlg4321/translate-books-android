import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from score_vs_final import aggregate, score


class ScoreVsFinalTest(unittest.TestCase):
    def test_improvement_and_regression_are_line_based(self):
        raw = "一。\n二。\n三。"
        draft = "One.\nTwo old.\nThree."
        app = "One.\nTwo fixed.\nThree worse 漢"
        final = "One.\nTwo fixed.\nThree."
        result = score(raw, draft, app, final, "001")
        self.assertEqual(result["fixRecall"]["ownerChanged"], 1)
        self.assertEqual(result["fixRecall"]["improved"], 1)
        self.assertGreaterEqual(result["regression"]["farther"], 1)
        self.assertIn(3, result["hard"]["addedKanaHanLines"])

    def test_an_untouched_draft_fixes_nothing_even_when_owner_edits_are_tiny(self):
        raw = "一。\n二。\n三。"
        draft = "Một câu khá dài ở đây.\nHai câu khá dài ở đây…\nBa."
        final = "Một câu khá dài ở đây!\nHai câu khá dài ở đây……\nBa."
        result = score(raw, draft, draft, final, "001")
        self.assertEqual(result["fixRecall"]["ownerChanged"], 2)
        self.assertEqual(result["fixRecall"]["improved"], 0)
        exact = score(raw, draft, final, final, "001")
        self.assertEqual(exact["fixRecall"]["improved"], 2)

    def test_hard_symbols_ruby_and_fullwidth(self):
        raw = "『一』：……"
        draft = "Opening"
        app = "Opening 《かな》 Ａ"
        final = "Opening"
        result = score(raw, draft, app, final, "010")
        self.assertEqual(result["hard"]["rubyLines"], [1])
        self.assertEqual(result["hard"]["fullwidthLatinLines"], [1])
        self.assertEqual(result["hard"]["remainingKanaHanLines"], [1])

    def test_raw_symbol_mismatch_uses_each_draft_lines_raw_pair(self):
        raw = "plain\n『two』"
        draft = "plain\nTwo"
        result = score(raw, draft, draft, draft, "011")
        self.assertEqual(result["hard"]["rawSymbolMismatchLines"], [2])

    def test_alignment_diagnostics_report_inserted_and_deleted_nonblank_lines(self):
        final = "One.\nTwo.\nThree."
        inserted = score("raw", final, "One.\nExtra.\nTwo.\nThree.", final, "012")
        self.assertEqual(inserted["alignment"]["unmatchedAppToFinalCount"], 1)
        self.assertEqual(inserted["alignment"]["unmatchedAppToFinalLines"], [2])
        self.assertEqual(inserted["alignment"]["unmatchedFinalFromAppCount"], 0)
        self.assertEqual(inserted["alignment"]["unmatchedFinalFromAppLines"], [])

        deleted = score("raw", final, "One.\nThree.", final, "013")
        self.assertEqual(deleted["alignment"]["unmatchedAppToFinalCount"], 0)
        self.assertEqual(deleted["alignment"]["unmatchedFinalFromAppCount"], 1)
        self.assertEqual(deleted["alignment"]["unmatchedFinalFromAppLines"], [2])

        deleted_draft = score("raw", "One.\nThree.", final, final, "014")
        self.assertEqual(deleted_draft["alignment"]["unmatchedDraftToFinalCount"], 0)
        self.assertEqual(deleted_draft["alignment"]["unmatchedFinalFromDraftCount"], 1)
        self.assertEqual(deleted_draft["alignment"]["unmatchedFinalFromDraftLines"], [2])

    def test_alignment_diagnostics_are_empty_when_app_and_draft_match_final(self):
        result = score("raw", "One.\nTwo.", "One.\nTwo.", "One.\nTwo.", "015")
        for key, value in result["alignment"].items():
            self.assertEqual(value, [] if key.endswith("Lines") else 0)
        self.assertEqual(result["draftAppDiff"], {"changedOpcodeCount": 0, "changedOpcodes": []})

    def test_draft_app_diff_diagnostic_reports_replacement_and_insertion_lines(self):
        result = score("raw", "One.\nTwo.\nThree.",
                       "One.\nTwo revised.\nThree.\nExtra.",
                       "One.\nTwo.\nThree.", "016")
        self.assertEqual(result["draftAppDiff"], {
            "changedOpcodeCount": 2,
            "changedOpcodes": [
                {"tag": "replace", "draftLines": [2], "appLines": [2]},
                {"tag": "insert", "draftLines": [], "appLines": [4]},
            ],
        })

    def test_aggregate_is_counts_only(self):
        one = score("一", "One", "One", "One", "001")
        two = score("二", "Two", "Deux", "Two", "002")
        result = aggregate([one, two])
        self.assertEqual(result["count"], 2)
        self.assertNotIn("One", repr(result))
        self.assertEqual(result["fixRecall"]["ownerChanged"], 0)


if __name__ == "__main__":
    unittest.main()
