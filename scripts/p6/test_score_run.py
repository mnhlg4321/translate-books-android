"""Offline tests of the R5 scorer. They use the private fixtures when present (skipped otherwise) and fabricate
run outputs by editing the fixture DRAFT; no model and no provider is involved. Run:

  py scripts/p6/test_score_run.py
"""
import json
import os
import shutil
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import score_run  # noqa: E402

ROOT = os.environ.get("P6_FIXTURES_ROOT", r"D:\P5E-private\p6-fixtures")
MANIFEST = os.path.join(HERE, "..", "..", "docs", "P6_R0_FIXTURE_MANIFEST.json")
HAVE = os.path.isdir(ROOT) and os.path.exists(MANIFEST)


def read(path):
    with open(path, encoding="utf-8", newline="") as handle:
        return handle.read()


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as handle:
        handle.write(text)


@unittest.skipUnless(HAVE, "private fixtures are not available on this machine")
class ScorerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = score_run.load_json(MANIFEST)
        cls.fixtures = {f["id"]: f for f in cls.manifest["fixtures"]}
        cls.thresholds = score_run.load_json(score_run.THRESHOLDS)

    def setUp(self):
        self.run = tempfile.mkdtemp(prefix="p6-score-")

    def tearDown(self):
        shutil.rmtree(self.run, ignore_errors=True)

    def labels(self, fid):
        return score_run.load_json(os.path.join(ROOT, self.fixtures[fid]["labels"]["path"]))

    def draft(self, fid):
        return read(os.path.join(ROOT, fid, "DRAFT.txt"))

    def clean_of(self, fid):
        """What a perfect run would end with: the seeded line put back."""
        if fid in ("fx-a03", "fx-a04", "fx-a05", "fx-a06", "fx-a07", "fx-a08", "fx-a09", "fx-a10", "fx-a11"):
            return self.draft("fx-a02")
        lines = self.draft(fid).split("\n")
        for target in self.labels(fid)["targets"]:
            n = target["line"] - 1
            if target["new"]:
                lines[n] = lines[n].replace(target["new"], target["old"])
            else:
                lines[n] = lines[n] + target["old"]
        return "\n".join(lines)

    def place(self, fid, final, structural=True, human=None):
        write(os.path.join(self.run, fid, "final.txt"), final)
        write(os.path.join(self.run, fid, "structural.json"), json.dumps(
            {"valid": structural, "reasonCode": "OK" if structural else "REPAIR_X", "stage": "CHAIN", "providerCalls": 8, "stops": []}))
        if human is not None:
            write(os.path.join(self.run, fid, "human_scores.json"), json.dumps(human))

    def score(self, fid):
        return score_run.score_fixture(self.fixtures[fid], ROOT, self.run, self.thresholds)

    # ---- seeded fixtures ----

    def test_a_perfect_run_passes_every_single_seeded_fixture(self):
        for fid in ("fx-a03", "fx-a04", "fx-a05", "fx-a06", "fx-a07", "fx-a08", "fx-a09", "fx-a10"):
            self.place(fid, self.clean_of(fid))
            report = self.score(fid)
            sem = report["SEMANTIC_EVAL"]
            self.assertEqual("PASS", sem["verdict"], (fid, sem["reasons"], sem["targets"]))
            self.assertIn(sem["targets"][0]["state"], score_run.STATES_FIXED, fid)
            self.assertEqual(0, sem["collateralChangedLines"], fid)

    def test_a_run_that_leaves_the_known_defect_fails_semantically_even_when_structurally_valid(self):
        for fid in ("fx-a03", "fx-a04", "fx-a05", "fx-a08"):
            self.place(fid, self.draft(fid), structural=True)
            report = self.score(fid)
            self.assertTrue(report["STRUCTURAL_VALID"]["valid"])
            self.assertEqual("FAIL", report["SEMANTIC_EVAL"]["verdict"], fid)
            self.assertEqual("MISSED", report["SEMANTIC_EVAL"]["targets"][0]["state"], fid)
            self.assertTrue(any(r.startswith("KNOWN_DEFECT_NOT_FIXED") for r in report["SEMANTIC_EVAL"]["reasons"]), fid)

    def test_structural_and_semantic_verdicts_are_independent(self):
        self.place("fx-a04", self.clean_of("fx-a04"), structural=False)
        report = self.score("fx-a04")
        self.assertFalse(report["STRUCTURAL_VALID"]["valid"])
        self.assertEqual("PASS", report["SEMANTIC_EVAL"]["verdict"])
        summary = score_run.summarize([report])
        self.assertEqual("0/1", summary["STRUCTURAL_VALID"])
        self.assertEqual(1, summary["SEMANTIC_EVAL"]["PASS"])

    def test_collateral_edits_are_counted_and_bounded(self):
        lines = self.clean_of("fx-a07").split("\n")
        for n in (10, 20, 30):
            lines[n] = lines[n] + " (đổi)"
        self.place("fx-a07", "\n".join(lines))
        report = self.score("fx-a07")
        self.assertEqual(3, report["SEMANTIC_EVAL"]["collateralChangedLines"])
        self.assertEqual("FAIL", report["SEMANTIC_EVAL"]["verdict"])
        lines = self.clean_of("fx-a07").split("\n")
        lines[10] = lines[10] + " (đổi)"
        self.place("fx-a07", "\n".join(lines))
        sem = self.score("fx-a07")["SEMANTIC_EVAL"]
        # one stray edit is within the bound, but a person must read it before the run counts
        self.assertEqual("PASS", sem["machine"])
        self.assertEqual("PENDING_HUMAN", sem["verdict"])
        self.place("fx-a07", "\n".join(lines), human={"items": {}, "sampleOk": 2, "voiceOk": 2})
        self.assertEqual("PASS", self.score("fx-a07")["SEMANTIC_EVAL"]["verdict"])

    def test_a_fix_that_keeps_the_defect_text_is_still_defective(self):
        target = self.labels("fx-a04")["targets"][0]
        lines = self.draft("fx-a04").split("\n")
        lines[target["line"] - 1] = lines[target["line"] - 1] + " thêm"
        self.place("fx-a04", "\n".join(lines))
        sem = self.score("fx-a04")["SEMANTIC_EVAL"]
        self.assertEqual("STILL_DEFECTIVE", sem["targets"][0]["state"])
        self.assertEqual("FAIL", sem["verdict"])

    def test_a_change_that_is_neither_the_defect_nor_the_fix_needs_a_human(self):
        target = self.labels("fx-a04")["targets"][0]
        lines = self.draft("fx-a04").split("\n")
        lines[target["line"] - 1] = "Một câu khác hẳn."
        self.place("fx-a04", "\n".join(lines))
        sem = self.score("fx-a04")["SEMANTIC_EVAL"]
        self.assertEqual("CHANGED_UNVERIFIED", sem["targets"][0]["state"])
        self.assertEqual("PENDING_HUMAN", sem["verdict"])

    def test_inserted_and_removed_lines_do_not_misalign_the_targets(self):
        # a missing sentence restored as its own new line next to the damaged one still counts as a fix
        target = self.labels("fx-a05")["targets"][0]
        lines = self.draft("fx-a05").split("\n")
        lines.insert(target["line"], target["old"].strip())
        # and an unrelated line is added at the top: the alignment must not move the verdict
        lines.insert(0, "")
        self.place("fx-a05", "\n".join(lines))
        sem = self.score("fx-a05")["SEMANTIC_EVAL"]
        self.assertIn(sem["targets"][0]["state"], score_run.STATES_FIXED, sem)

    def test_multiple_defects_need_enough_of_them(self):
        clean = self.clean_of("fx-a11")
        self.place("fx-a11", clean)
        self.assertEqual("PASS", self.score("fx-a11")["SEMANTIC_EVAL"]["verdict"])
        # fix only three of six
        lines = self.draft("fx-a11").split("\n")
        good = clean.split("\n")
        for target in self.labels("fx-a11")["targets"][:3]:
            lines[target["line"] - 1] = good[target["line"] - 1]
        self.place("fx-a11", "\n".join(lines))
        sem = self.score("fx-a11")["SEMANTIC_EVAL"]
        self.assertEqual(3, sem["targetsFixed"])
        self.assertEqual("FAIL", sem["verdict"])
        self.assertTrue(any(r.startswith("MULTI_DEFECT_RECALL_LOW") for r in sem["reasons"]))

    # ---- controls ----

    def test_clean_control_must_stay_untouched_and_ambiguous_paraphrases_are_reported_not_graded(self):
        self.place("fx-a02", self.draft("fx-a02"))
        self.assertEqual("PASS", self.score("fx-a02")["SEMANTIC_EVAL"]["verdict"])
        lines = self.draft("fx-a02").split("\n")
        lines[40] = lines[40] + " thêm"
        self.place("fx-a02", "\n".join(lines))
        sem = self.score("fx-a02")["SEMANTIC_EVAL"]
        self.assertEqual("FAIL", sem["verdict"])
        self.assertTrue(sem["reasons"][0].startswith("CLEAN_CONTROL_EDITED"))

        paraphrase = self.labels("fx-a12")["paraphraseLines"]
        lines = self.draft("fx-a12").split("\n")
        for n in paraphrase:
            lines[n - 1] = lines[n - 1] + " (diễn đạt khác)"
        self.place("fx-a12", "\n".join(lines))
        sem = self.score("fx-a12")["SEMANTIC_EVAL"]
        self.assertEqual("PASS", sem["machine"])
        self.assertEqual("PENDING_HUMAN", sem["verdict"])
        self.assertEqual(len(paraphrase), sem["ambiguousEditedLines"])
        self.assertEqual(0, sem["collateralChangedLines"])

    # ---- the real chapter ----

    def real_final(self, change_tohha=4):
        lines = self.draft("fx-a01").split("\n")
        lines[236] = lines[236].replace("今回", "Lần này")
        lines[320] = lines[320] + " cô bé"
        for n in (107, 148, 176, 216)[:change_tohha]:
            lines[n - 1] = lines[n - 1] + " (vượt tới tầng sâu nhất)"
        return "\n".join(lines)

    def test_real_chapter_needs_all_three_known_defects_and_a_human_for_the_sense_contrast(self):
        self.place("fx-a01", self.real_final())
        sem = self.score("fx-a01")["SEMANTIC_EVAL"]
        self.assertEqual("PENDING_HUMAN", sem["verdict"], sem["reasons"])
        states = {t["id"]: t["state"] for t in sem["targets"]}
        self.assertIn(states["I-001"], score_run.STATES_FIXED)
        self.assertIn(states["I-003"], score_run.STATES_FIXED)
        self.assertEqual("FIXED_MATCH", states["I-002"])
        items = {"I-002": 2, "I-001": 2, "I-003": 2}
        self.place("fx-a01", self.real_final(), human={"items": items, "sampleOk": 2, "voiceOk": 2})
        self.assertEqual("PASS", self.score("fx-a01")["SEMANTIC_EVAL"]["verdict"])
        self.place("fx-a01", self.real_final(), human={"items": {"I-002": 0, "I-001": 2, "I-003": 2}, "sampleOk": 2, "voiceOk": 2})
        self.assertEqual("FAIL", self.score("fx-a01")["SEMANTIC_EVAL"]["verdict"])

    def test_fixing_one_of_four_occurrences_is_the_pilot_outcome_and_fails(self):
        self.place("fx-a01", self.real_final(change_tohha=1))
        sem = self.score("fx-a01")["SEMANTIC_EVAL"]
        self.assertEqual("FAIL", sem["verdict"])
        self.assertIn("KNOWN_DEFECT_NOT_FIXED:I-002", sem["reasons"])
        by_id = {t["id"]: t for t in sem["targets"]}
        self.assertEqual("PARTIAL", by_id["I-002"]["state"])
        self.assertEqual(1, by_id["I-002"]["changedLines"])

    def test_the_uncertain_lines_of_the_real_chapter_are_not_collateral(self):
        lines = self.real_final().split("\n")
        for n in (125, 87, 89):
            lines[n - 1] = lines[n - 1] + " (tuỳ chọn)"
        self.place("fx-a01", "\n".join(lines))
        sem = self.score("fx-a01")["SEMANTIC_EVAL"]
        self.assertEqual(0, sem["collateralChangedLines"])
        self.assertEqual(3, sem["ambiguousEditedLines"])

    # ---- holdout ----

    def test_holdout_perfect_and_null_runs(self):
        for fid in ("fx-h01", "fx-h02"):
            self.place(fid, self.clean_of(fid))
            sem = self.score(fid)["SEMANTIC_EVAL"]
            self.assertEqual("PASS", sem["verdict"], (fid, sem["reasons"], sem["targets"]))
            self.place(fid, self.draft(fid))
            self.assertEqual("FAIL", self.score(fid)["SEMANTIC_EVAL"]["verdict"], fid)

    # ---- guards ----

    def test_missing_outputs_are_failures_not_silent_passes(self):
        report = self.score_missing()
        self.assertFalse(report["STRUCTURAL_VALID"]["valid"])
        self.assertEqual("FAIL", report["SEMANTIC_EVAL"]["verdict"])

    def score_missing(self):
        os.makedirs(os.path.join(self.run, "fx-a04"), exist_ok=True)
        return self.score("fx-a04")

    def test_the_real_fixtures_pass_the_oracle_leak_check_and_a_leak_is_detected(self):
        self.assertEqual([], score_run.check_leaks(ROOT, self.manifest))
        # fabricate a leak: a seeded fixture whose DRAFT contains the corrected sentence
        tmp = tempfile.mkdtemp(prefix="p6-leak-")
        try:
            fid = "fx-a04"
            shutil.copytree(os.path.join(ROOT, fid), os.path.join(tmp, fid))
            os.makedirs(os.path.join(tmp, "_labels"))
            shutil.copy(os.path.join(ROOT, "_labels", fid + ".json"), os.path.join(tmp, "_labels", fid + ".json"))
            target = self.labels(fid)["targets"][0]
            with open(os.path.join(tmp, fid, "DRAFT.txt"), "a", encoding="utf-8") as handle:
                handle.write("\n" + target["old"])
            problems = score_run.check_leaks(tmp, {"fixtures": [self.fixtures[fid]]})
            self.assertEqual(1, len(problems), problems)
        finally:
            shutil.rmtree(tmp, ignore_errors=True)

    def test_cli_refuses_a_6_final_path_and_reports_thresholds_hash(self):
        self.assertEqual(2, score_run.main(["--fixtures-root", os.path.join("x", "6.FINAL"), "--run-dir", self.run]))
        out = os.path.join(self.run, "report.json")
        self.place("fx-a04", self.clean_of("fx-a04"))
        self.assertEqual(0, score_run.main(["--fixtures-root", ROOT, "--run-dir", self.run, "--out", out]))
        report = score_run.load_json(out)
        self.assertEqual(score_run.sha256_file(score_run.THRESHOLDS), report["thresholdsSha256"])
        self.assertIn("STRUCTURAL_VALID", report["summary"])
        self.assertIn("SEMANTIC_EVAL", report["summary"])


if __name__ == "__main__":
    unittest.main()
