import unittest

import min_gate_413 as g

# Synthetic lines only; no book text.
RAW = "\n".join(["一行目です。", "二行目です。", "三行目です。", "四行目です。", "五行目です。"]) + "\n"
DRAFT = "\n".join(["Dòng một có lỗi nhỏ.", "Dòng hai có lỗi nhỏ.", "Dòng ba ổn.", "Dòng bốn có lỗi nhỏ.", "Dòng năm ổn."]) + "\n"
FINAL = "\n".join(["Dòng một đã sửa xong.", "Dòng hai đã sửa xong.", "Dòng ba ổn.", "Dòng bốn đã sửa xong.", "Dòng năm ổn."]) + "\n"


class MinGateTests(unittest.TestCase):
    def test_an_app_text_equal_to_final_passes_every_automatic_check(self):
        result = g.gate(RAW, DRAFT, FINAL, FINAL, chapter="t")
        self.assertTrue(result["pass"], result["checks"])
        self.assertEqual(3, result["numbers"]["ownerChanged"])
        self.assertEqual(3, result["numbers"]["improved"])

    def test_an_untouched_draft_has_no_real_fix(self):
        result = g.gate(RAW, DRAFT, DRAFT, FINAL)
        self.assertFalse(result["pass"])
        self.assertFalse(result["checks"]["c4_improved_at_least_25pct"])
        self.assertTrue(result["checks"]["c1_valid_not_truncated"])

    def test_an_invalid_or_truncated_run_fails_structure(self):
        self.assertFalse(g.gate(RAW, DRAFT, FINAL, FINAL, valid=False)["checks"]["c1_valid_not_truncated"])
        self.assertFalse(g.gate(RAW, DRAFT, FINAL, FINAL, truncated=True)["checks"]["c1_valid_not_truncated"])

    def test_a_shortened_text_fails_the_length_fence_and_the_line_count(self):
        short = "Dòng một đã sửa xong.\nDòng hai đã sửa xong.\n"
        result = g.gate(RAW, DRAFT, short, FINAL)
        self.assertFalse(result["checks"]["c1_length_fence"])
        self.assertFalse(result["checks"]["c1_line_count_vs_raw"])
        self.assertFalse(result["pass"])

    def test_a_line_that_gains_japanese_fails_the_technical_check(self):
        app = FINAL.replace("Dòng ba ổn.", "Dòng ba 勇者 ổn.")
        result = g.gate(RAW, DRAFT, app, FINAL)
        self.assertFalse(result["checks"]["c2_no_added_kana_han"])
        self.assertEqual(1, result["numbers"]["addedKanaHanLines"])

    def test_moving_owner_changed_lines_away_from_final_is_limited_to_ten_percent(self):
        wrong = "\n".join(["Hoàn toàn khác một.", "Dòng hai đã sửa xong.", "Dòng ba ổn.", "Dòng bốn đã sửa xong.", "Dòng năm ổn."]) + "\n"
        result = g.gate(RAW, DRAFT, wrong, FINAL)
        self.assertEqual(1, result["numbers"]["fartherOnOwnerChanged"])
        self.assertFalse(result["checks"]["c3_farther_on_owner_changed_at_most_10pct"])  # 10 * 1 > 3

    def test_the_fences_use_integer_comparisons(self):
        # exactly 0.80 of the draft is allowed, one character less is not
        draft = "a" * 100 + "\n"
        self.assertTrue(g.gate("x\n", draft, "b" * 80 + "\n", "b" * 80 + "\n")["checks"]["c1_length_fence"])
        self.assertFalse(g.gate("x\n", draft, "b" * 79 + "\n", "b" * 79 + "\n")["checks"]["c1_length_fence"])
        self.assertTrue(g.gate("x\n", draft, "b" * 125 + "\n", "b" * 125 + "\n")["checks"]["c1_length_fence"])
        self.assertFalse(g.gate("x\n", draft, "b" * 126 + "\n", "b" * 126 + "\n")["checks"]["c1_length_fence"])


if __name__ == "__main__":
    unittest.main()
