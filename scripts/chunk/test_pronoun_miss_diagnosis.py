#!/usr/bin/env python3
"""Synthetic tests for the pronoun-miss diagnosis tool, the known-error suite and its separate clean control set. No book text."""
from __future__ import annotations

import base64
import contextlib
import io
import json
import re
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import pronoun_miss_diagnosis as P  # noqa: E402

REPO = Path(__file__).resolve().parents[2]
JAVA = REPO / "editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/api/AddressChecklist.java"


def fact(**kw):
    base = {"ch": "900", "d": 0, "dialogue": True, "lex": ["tớ"], "flagged": [], "conflicted": False, "rows": [], "text": "x", "rawText": "y"}
    base.update(kw)
    return base


def row(**kw):
    base = {"from": "A", "speaker": "A", "target": "B", "self": "tôi", "call": "em", "complete": True, "projected": True,
            "coversDialogue": True, "unexpected": []}
    base.update(kw)
    return base


class LexiconTest(unittest.TestCase):
    def test_python_lexicon_equals_the_java_lexicon(self):
        source = JAVA.read_text(encoding="utf-8")
        body = re.search(r"WORDS = Set\.of\((.*?)\);", source, re.S).group(1)
        self.assertEqual(frozenset(re.findall(r'"([^"]+)"', body)), P.WORDS)


class ClassifyLineTest(unittest.TestCase):
    def test_every_reason_of_the_finder_chain(self):
        cases = [
            ("FLAGGED", fact(flagged=["ADDRESS"])),
            ("S1_NO_DIALOGUE", fact(dialogue=False, rows=[row(unexpected=["tớ"])])),
            ("S1_NO_ROW_COVERS", fact(rows=[])),
            ("S1_ROW_NOT_PROJECTED", fact(rows=[row(projected=False, unexpected=["tớ"])])),
            ("S1_ROW_INCOMPLETE", fact(rows=[row(call="", complete=False)])),
            ("S1_ROW_SCOPE_NO_QUOTE", fact(rows=[row(coversDialogue=False)])),
            ("S1_CONFLICT", fact(conflicted=True, rows=[row(unexpected=["tớ"])])),
            ("S1_LEXICON_GAP", fact(lex=[], rows=[row()])),
            ("S1_ALLOWED_WORD_ONLY", fact(lex=["tôi"], rows=[row()])),
        ]
        for expected, f in cases:
            with self.subTest(expected):
                self.assertEqual(P.classify_line(f), expected)

    def test_a_kana_only_flag_does_not_count_as_an_address_flag(self):
        self.assertEqual(P.classify_line(fact(flagged=["KANA"], rows=[row()])), "S1_ALLOWED_WORD_ONLY")


class DeltaAndAlignmentTest(unittest.TestCase):
    def test_tokens_are_nfc_lowercase_letter_runs(self):
        self.assertEqual(P.tokens("「Cậu, đã đến!」"), ["cậu", "đã", "đến"])
        self.assertEqual(P.tokens("Cậu"), P.tokens("Cậu"))  # NFD input equals NFC input

    def test_address_delta_uses_the_lexicon_and_the_row_words(self):
        removed, added = P.address_delta("Cậu đi nhé", "Rea đi nhé", {"rea"})
        self.assertEqual((removed, added), (["cậu"], ["rea"]))
        self.assertEqual(P.address_delta("Mình đi", "Tôi đi", set()), ([], ["tôi"]))  # "mình" is outside the lexicon
        self.assertEqual(P.address_delta("Mình đi", "Tôi đi", {"mình"}), (["mình"], ["tôi"]))
        self.assertEqual(P.address_delta("Đi nhé", "Đi nhé!", set()), ([], []))

    def test_alignment_states(self):
        pairs = P.align_final(["a b", "c d", "g h"], ["a b", "c dd", "g h"])
        self.assertEqual(pairs, [("SAME", "a b"), ("CHANGED", "c dd"), ("SAME", "g h")])
        # a line the owner removed outright is DELETED; inside an uneven replaced block the unmatched line is UNPAIRED
        self.assertEqual(P.align_final(["a b", "c d", "g h"], ["a b", "g h"])[1], ("DELETED", None))
        self.assertEqual(P.align_final(["a b", "c d", "e f", "g h"], ["a b", "c dd", "g h"])[2][0], "UNPAIRED")

    def test_uneven_replace_pairs_only_similar_lines(self):
        pairs = P.align_final(["Cậu đã đến rồi", "xyz abc"], ["Rea đã đến rồi", "hoàn toàn khác", "dòng thêm"])
        self.assertEqual(pairs[0], ("CHANGED", "Rea đã đến rồi"))
        self.assertEqual(pairs[1][0], "UNPAIRED")


class LayerTest(unittest.TestCase):
    def test_layers_and_groups(self):
        L = P.layer_and_group
        self.assertEqual(L("SÓT", "bỏ sót", "KEPT", "MODEL_KEEP"), ("KNOWN", "S2_MODEL_KEPT"))
        self.assertEqual(L("SÓT", "bỏ sót (C6B model sửa đúng, lần này giữ)", "KEPT", "MODEL_KEEP"), ("KNOWN", "S2_MODEL_KEPT"))
        self.assertEqual(L("SÓT", "sửa đúng bị chặn nhầm", "KEPT", "ADDRESS_X"), ("KNOWN", "S3_GUARD_FALSE_BLOCK"))
        self.assertEqual(L("SÓT", "sửa sai bị chặn đúng", "KEPT", "ADDRESS_X"), ("KNOWN", "S2B_WRONG_PROPOSAL_BLOCKED_DRAFT_STILL_WRONG"))
        self.assertEqual(L("SÓT", "sửa khác", "FIXED", "REPLACED"), ("KNOWN", "S2P_PARTIAL_FIX"))
        self.assertEqual(L("SAI", "sửa sai lọt qua", "FIXED", "REPLACED"), ("KNOWN", "S4_WRONG_PASSED"))
        self.assertEqual(L("CHƯA CHẮC", "giữ", "KEPT", "MODEL_KEEP"), ("UNSURE", "S5_UNSURE"))
        self.assertEqual(L("ĐÚNG", "sửa đúng", "FIXED", "REPLACED"), ("CLEAN", "CLEAN_FIXED_OK"))
        self.assertEqual(L("ĐÚNG", "sửa sai bị chặn đúng", "KEPT", "ADDRESS_X"), ("CLEAN", "CLEAN_WRONG_PROPOSAL_BLOCKED"))
        self.assertEqual(L("ĐÚNG", "giữ đúng", "KEPT", "MODEL_KEEP"), ("CLEAN", "CLEAN_KEPT"))
        with self.assertRaises(ValueError):
            L("?", "", "KEPT", "")

    def test_label_006_follows_the_scorer(self):
        sets = {"NEW_CORRECT": {"5:1"}, "NEW_WRONG_PASSED": {"14:5"}, "WRONGLY_BLOCKED": {"7:3"}, "WRONG_BLOCKED": {"2:2"}}
        def p(key, label, status, reason, old="KEPT"):
            return {"key": key, "label": label, "status": status, "reason": reason, "old_status": old}
        self.assertEqual(P.label_006(p("14:5", "ĐÚNG", "FIXED", "REPLACED"), sets), ("SAI", "sửa sai lọt qua"))
        self.assertEqual(P.label_006(p("5:1", "SÓT", "FIXED", "REPLACED"), sets), ("ĐÚNG", "sửa đúng"))
        self.assertEqual(P.label_006(p("7:3", "SÓT", "KEPT", "ADDRESS_SCOPE_EXCEEDED"), sets), ("SÓT", "sửa đúng bị chặn nhầm"))
        self.assertEqual(P.label_006(p("2:2", "SAI", "KEPT", "ADDRESS_X"), sets), ("ĐÚNG", "sửa sai bị chặn đúng"))
        self.assertEqual(P.label_006(p("9:9", "SÓT", "KEPT", "MODEL_KEEP"), sets), ("SÓT", "bỏ sót"))
        self.assertEqual(P.label_006(p("9:8", "ĐÚNG", "KEPT", "MODEL_KEEP", old="FIXED"), sets)[0], "SÓT")
        self.assertEqual(P.label_006(p("9:7", "ĐÚNG", "KEPT", "MODEL_KEEP"), sets), ("ĐÚNG", "giữ đúng"))


def run_main(args):
    """P.main with its report printed to nowhere, so the test log stays readable."""
    with contextlib.redirect_stdout(io.StringIO()):
        return P.main(args)


def tsv_line(key, types, status, reason, before, after):
    b = lambda s: base64.b64encode(s.encode("utf-8")).decode("ascii")
    return "\t".join([key, types, status, reason, "1", b(before), b(after)])


SENTINEL_RAW = "SENTINEL-RAW-TEXT"
SENTINEL_DRAFT = "SENTINEL-DRAFT-TEXT"


def make_guard_dir(root: Path) -> None:
    def point(key, status, reason, label=None):
        chunk, ident = key.split(":")
        p = {"key": key, "chunk": chunk, "id": ident, "types": ["ADDRESS"], "status": status, "reason": reason, "raw": f"{SENTINEL_RAW} {key}",
             "before": f"{SENTINEL_DRAFT} {key}", "after": f"{SENTINEL_DRAFT} {key} fixed", "old_status": "KEPT"}
        if label:
            p["label"] = label
        return p
    pts006 = [point("1:1", "KEPT", "MODEL_KEEP", "ĐÚNG"), point("1:2", "KEPT", "MODEL_KEEP", "SÓT"), point("1:3", "FIXED", "REPLACED", "ĐÚNG")]
    pts008 = [point("1:1", "KEPT", "MODEL_KEEP"), point("1:2", "FIXED", "REPLACED")]
    pts011 = [point("1:1", "KEPT", "ADDRESS_X"), point("1:2", "KEPT", "MODEL_KEEP"), point("1:3", "FIXED", "REPLACED")]
    for name, pts in (("c6e-points.json", pts006), ("c7a-points.json", pts008), ("c7d-points.json", pts011)):
        (root / name).write_text(json.dumps(pts, ensure_ascii=False), encoding="utf-8")
    (root / "score_c6e.py").write_text("NEW_CORRECT = set()\nNEW_WRONG_PASSED = set()\nWRONGLY_BLOCKED = set()\nWRONG_BLOCKED = set()\n"
                                       "raise SystemExit('never executed')\n", encoding="utf-8")
    (root / "score_c7a.py").write_text("KEY = {'1:2': ('SAI', 'sửa sai lọt qua', 'n')}\nraise SystemExit('never executed')\n", encoding="utf-8")
    (root / "score_c7d.py").write_text("KEY = {'1:1': ('ĐÚNG', 'sửa sai bị chặn đúng', 'n'), '1:2': ('SÓT', 'sửa đúng bị chặn nhầm', 'n')}\n", encoding="utf-8")
    (root / "v15-replay-006-c6e.tsv").write_text("\n".join([
        tsv_line("1:1", "ADDRESS", "KEPT", "MODEL_KEEP", "d", "d"), tsv_line("1:2", "ADDRESS", "KEPT", "MODEL_KEEP", "d", "d"),
        tsv_line("1:3", "ADDRESS", "FIXED", "REPLACED", "d", "d2")]) + "\n", encoding="utf-8")
    (root / "v15-replay-008.tsv").write_text(tsv_line("1:1", "ADDRESS", "KEPT", "MODEL_KEEP", "d", "d") + "\n", encoding="utf-8")  # 1:2 vanished
    (root / "v15-replay-011-c7d.tsv").write_text("\n".join([
        tsv_line("1:1", "ADDRESS", "KEPT", "ADDRESS_X", "d", "d"), tsv_line("1:2", "ADDRESS", "FIXED", "REPLACED", "d", "d3"),
        tsv_line("1:3", "ADDRESS", "FIXED", "REPLACED", "d", "d4")]) + "\n", encoding="utf-8")


class SuiteTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        make_guard_dir(self.root)
        self.suite = P.build_suite(self.root, None)

    def tearDown(self):
        self.tmp.cleanup()

    def layers(self):
        return {r["id"]: r["layer"] for r in self.suite["records"]}

    def test_records_get_layers_from_label_event_and_live_outcome(self):
        got = self.layers()
        self.assertEqual(got["006-C6E:1:1"], "CLEAN_KEPT")
        self.assertEqual(got["006-C6E:1:2"], "S2_MODEL_KEPT")
        self.assertEqual(got["006-C6E:1:3"], "CLEAN_FIXED_OK")
        self.assertEqual(got["008-C7A:1:2"], "S4_WRONG_PASSED")
        self.assertEqual(got["011-C7D:1:1"], "CLEAN_WRONG_PROPOSAL_BLOCKED")
        self.assertEqual(got["011-C7D:1:2"], "S3_GUARD_FALSE_BLOCK")

    def test_the_point_that_vanished_from_the_replay_is_recorded_as_no_point(self):
        rec = next(r for r in self.suite["records"] if r["id"] == "008-C7A:1:2")
        self.assertEqual(rec["current"]["status"], "NO_POINT")
        self.assertEqual(expectation(rec), "STAY_BLOCKED")

    def test_known_and_clean_parts_are_disjoint_and_pass_the_separation_check(self):
        known, clean = P.split_suite(self.suite)
        self.assertEqual(P.check_separation(known, clean), [])
        self.assertTrue(all(r["group"] in ("KNOWN", "UNSURE") for r in known["records"]))
        self.assertTrue(all(r["group"] == "CLEAN" and r["label"] == "ĐÚNG" for r in clean["records"]))
        self.assertEqual(len(known["records"]) + len(clean["records"]), len(self.suite["records"]))

    def test_separation_check_catches_a_clean_record_in_the_known_part_and_the_reverse(self):
        known, clean = P.split_suite(self.suite)
        known["records"].append(clean["records"][0])
        self.assertTrue(P.check_separation(known, clean))
        known, clean = P.split_suite(self.suite)
        clean["records"].append(dict(known["records"][0]))
        self.assertTrue(P.check_separation(known, clean))

    def test_public_index_has_hashes_and_no_book_text(self):
        known, clean = P.split_suite(self.suite)
        for part in (known, clean):
            text = json.dumps(P.public_index(part), ensure_ascii=False)
            self.assertNotIn(SENTINEL_RAW, text)
            self.assertNotIn(SENTINEL_DRAFT, text)
            self.assertIn(P.sha256(f"{SENTINEL_RAW} 1:1"), json.dumps(P.public_index(clean)) + json.dumps(P.public_index(known)))
            self.assertEqual(P.verify_index(P.public_index(part), part), [])

    def test_verify_index_detects_edited_text_and_missing_records(self):
        known, _ = P.split_suite(self.suite)
        index = P.public_index(known)
        known["records"][0]["text"]["draft"] += " edited"
        self.assertTrue(any("DRAFT text does not match" in p for p in P.verify_index(index, known)))
        index["records"].pop()
        self.assertTrue(any("not in the index" in p for p in P.verify_index(index, known)))

    def test_cli_builds_both_parts_without_leaking_text_into_the_public_files(self):
        out_private, out_public = self.root / "private", self.root / "public"
        self.assertEqual(run_main(["suite", "--guard-dir", str(self.root), "--out-private-dir", str(out_private), "--out-public-dir", str(out_public)]), 0)
        for name in ("known-misses.json", "clean-controls.json"):
            self.assertIn(SENTINEL_RAW, (out_private / name).read_text(encoding="utf-8"))
        for name in ("V419_PRONOUN_KNOWN_MISS_INDEX.json", "V419_PRONOUN_CLEAN_CONTROL_INDEX.json"):
            self.assertNotIn("SENTINEL", (out_public / name).read_text(encoding="utf-8"))


def expectation(rec):
    return P.expectation(rec)


class VerifyReplayTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        make_guard_dir(self.root)
        self.records = P.build_suite(self.root, None)["records"]
        self.replays = {"006-C6E": P.read_replay(self.root / "v15-replay-006-c6e.tsv"),
                        "008-C7A": P.read_replay(self.root / "v15-replay-008.tsv"),
                        "011-C7D": P.read_replay(self.root / "v15-replay-011-c7d.tsv")}

    def tearDown(self):
        self.tmp.cleanup()

    def test_unchanged_replay_reports_nothing(self):
        result = P.verify_replay(self.records, self.replays)
        self.assertEqual(result["regressions"], [])
        self.assertEqual(result["improvements"], [])
        self.assertEqual(result["unchanged"], len(self.records))

    def test_a_changed_clean_control_is_a_regression(self):
        self.replays["006-C6E"]["1:1"]["status"] = "FIXED"
        self.replays["006-C6E"]["1:1"]["after"] = "changed"
        result = P.verify_replay(self.records, self.replays)
        self.assertTrue(any("006-C6E:1:1" in r for r in result["regressions"]))

    def test_a_clean_fixed_point_whose_text_changes_is_a_regression(self):
        self.replays["006-C6E"]["1:3"]["after"] = "other text"
        self.assertTrue(P.verify_replay(self.records, self.replays)["regressions"])

    def test_a_wrong_fix_that_passes_again_is_a_regression(self):
        self.replays["011-C7D"]["1:2"]  # S3 record exists
        rec_ids = {r["id"] for r in self.records if r["layer"] == "S4_WRONG_PASSED"}
        self.assertEqual(rec_ids, {"008-C7A:1:2"})
        self.replays["008-C7A"]["1:2"] = {"types": "ADDRESS", "status": "FIXED", "reason": "REPLACED", "before": "d", "after": "bad"}
        self.assertTrue(any("008-C7A:1:2" in r for r in P.verify_replay(self.records, self.replays)["regressions"]))

    def test_a_guard_false_block_that_becomes_fixed_is_an_improvement(self):
        rec = next(r for r in self.records if r["layer"] == "S3_GUARD_FALSE_BLOCK")
        rec["current"] = {"status": "KEPT", "reason": "ADDRESS_X", "afterSha256": P.sha256("d")}
        self.replays["011-C7D"]["1:2"] = {"types": "ADDRESS", "status": "FIXED", "reason": "REPLACED", "before": "d", "after": "fixed"}
        result = P.verify_replay(self.records, self.replays)
        self.assertEqual(result["regressions"], [])
        self.assertTrue(any("guard false block now fixed" in i for i in result["improvements"]))

    def test_cli_exit_code_follows_the_regressions(self):
        out_private, out_public = self.root / "private", self.root / "public"
        run_main(["suite", "--guard-dir", str(self.root), "--out-private-dir", str(out_private), "--out-public-dir", str(out_public)])
        args = ["verify-replay", "--known", str(out_private / "known-misses.json"), "--clean", str(out_private / "clean-controls.json"),
                "--replay", f"006-C6E={self.root / 'v15-replay-006-c6e.tsv'}", "--replay", f"008-C7A={self.root / 'v15-replay-008.tsv'}",
                "--replay", f"011-C7D={self.root / 'v15-replay-011-c7d.tsv'}"]
        self.assertEqual(run_main(args), 0)
        bad = self.root / "bad.tsv"
        bad.write_text(tsv_line("1:1", "ADDRESS", "FIXED", "REPLACED", "d", "zzz") + "\n", encoding="utf-8")
        args[args.index(f"006-C6E={self.root / 'v15-replay-006-c6e.tsv'}")] = f"006-C6E={bad}"
        self.assertEqual(run_main(args), 1)


class AnalyzeTest(unittest.TestCase):
    def test_analyze_counts_owner_changed_address_lines_by_reason(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            facts = [
                {"meta": True, "ch": "900", "draftLines": 3, "rawLines": 3, "rows": 1, "rowsIncomplete": 0, "chunks": 1, "conflicts": 0},
                fact(d=0, text="「Cậu đi nhé」", flagged=["ADDRESS"], rows=[row()]),
                fact(d=1, text="Mình ngồi đó", dialogue=False, lex=[], rows=[row()]),
                fact(d=2, text="「Cũng được」", lex=[], rows=[row()]),
            ]
            facts_path = root / "facts.jsonl"
            facts_path.write_text("\n".join(json.dumps(f, ensure_ascii=False) for f in facts) + "\n", encoding="utf-8")
            final_root = root / "final"
            final_root.mkdir()
            (final_root / "900_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt").write_text("「Em đi nhé」\nTôi ngồi đó\n「Cũng được」\n", encoding="utf-8")
            result = P.analyze(facts_path, final_root)
            ch = result["chapters"]["900"]
            self.assertEqual(ch["ownerChangedLines"], 2)
            self.assertEqual(ch["ownerChangedWithAddressDelta"], 2)
            self.assertEqual(ch["addressDeltaCovered"], 1)
            self.assertEqual(ch["addressDeltaUncoveredByReason"], {"S1_NO_DIALOGUE": 1})
            public = P.public_summary(result)
            self.assertNotIn("Cậu", json.dumps(public, ensure_ascii=False))
            self.assertNotIn("candidates", public["chapters"]["900"])

    def test_facts_that_do_not_cover_every_draft_line_are_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            facts_path = Path(tmp) / "facts.jsonl"
            facts_path.write_text(json.dumps({"meta": True, "ch": "900", "draftLines": 5, "rawLines": 5, "rows": 0, "rowsIncomplete": 0,
                                              "chunks": 1, "conflicts": 0}) + "\n" + json.dumps(fact()) + "\n", encoding="utf-8")
            with self.assertRaises(SystemExit):
                P.analyze(facts_path, None)


if __name__ == "__main__":
    unittest.main()
