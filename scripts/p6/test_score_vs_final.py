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

    def test_hard_symbols_ruby_and_fullwidth(self):
        raw = "『一』：……"
        draft = "Opening"
        app = "Opening 《かな》 Ａ"
        final = "Opening"
        result = score(raw, draft, app, final, "010")
        self.assertEqual(result["hard"]["rubyLines"], [1])
        self.assertEqual(result["hard"]["fullwidthLatinLines"], [1])
        self.assertEqual(result["hard"]["remainingKanaHanLines"], [1])

    def test_aggregate_is_counts_only(self):
        one = score("一", "One", "One", "One", "001")
        two = score("二", "Two", "Deux", "Two", "002")
        result = aggregate([one, two])
        self.assertEqual(result["count"], 2)
        self.assertNotIn("One", repr(result))
        self.assertEqual(result["fixRecall"]["ownerChanged"], 0)


if __name__ == "__main__":
    unittest.main()
