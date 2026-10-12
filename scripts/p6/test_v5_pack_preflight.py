import hashlib
import tempfile
import unittest
from pathlib import Path

from v5_pack_preflight import PreflightError, check_chapter, identity_of, line_counts

NAMES = {
    "RAW": "007_RAW_SAMPLE_SERIES_VOL1.txt",
    "DRAFT": "007_SAMPLE_SERIES_VOL1_DRAFT.txt",
    "GLOSSARY": "007_SAMPLE_SERIES_VOL1_chapter_glossary.csv",
    "PRONOUN": "007_PRONOUN_SAMPLE_SERIES_VOL1.csv",
}


class V5PackPreflightTests(unittest.TestCase):
    def write_chapter(self, root: Path, chapter: str = "007", *, glossary: str | None = None,
                      pronoun: str | None = None, bad_utf8: bool = False, names: dict[str, str] | None = None) -> dict:
        names = dict(names or NAMES)
        folder = root / chapter
        folder.mkdir(parents=True)
        (folder / names["RAW"]).write_text("raw\r\n", encoding="utf-8", newline="")
        (folder / names["DRAFT"]).write_text("draft\n", encoding="utf-8", newline="")
        if bad_utf8:
            (folder / names["RAW"]).write_bytes(b"\xff")
        (folder / names["GLOSSARY"]).write_text(
            glossary if glossary is not None else "source,target,category,note,priority\na,b,term,,1\n",
            encoding="utf-8", newline="")
        (folder / names["PRONOUN"]).write_text(
            pronoun if pronoun is not None else "\ufefffrom,speaker,target,self,call,scope,note\na,b,c,d,e,f,g\n",
            encoding="utf-8", newline="")
        return {chapter: names}

    def test_accepts_four_roles_with_original_names_identity_and_the_counts_the_app_will_print(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            roles = self.write_chapter(root)
            result = check_chapter(root, "007", roles)
            self.assertEqual("PASS", result["status"])
            self.assertEqual(["RAW", "DRAFT", "GLOSSARY", "PRONOUN"], [item["role"] for item in result["files"]])
            self.assertEqual(list(NAMES.values()), [item["name"] for item in result["files"]])
            self.assertEqual(("007", "SAMPLE_SERIES_VOL1"), (result["id"], result["series"]))
            self.assertEqual("V5-SAFE.4.1.3-FULL", result["version"])
            raw = result["files"][0]
            self.assertEqual(5, raw["bytes"])
            self.assertEqual(1, raw["lines"])
            self.assertEqual(hashlib.sha256(b"raw\r\n").hexdigest(), raw["sha256"])

    def test_accepts_headerless_legacy_three_column_pronouns(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            roles = self.write_chapter(root, pronoun="花子,em,legacy\n")
            self.assertEqual(3, check_chapter(root, "007", roles)["pronounColumns"])

    def test_identity_follows_the_owners_name_patterns_and_stops_when_missing_or_disagreeing(self):
        self.assertEqual(("017", "SAMPLE_SERIES_VOL1"), identity_of("DRAFT", "017_SAMPLE_SERIES_VOL1_translated.txt"))
        self.assertEqual(("002", "SAMPLE_SERIES_VOL1"), identity_of("DRAFT", "002_RAW_SAMPLE_SERIES_VOL1_DRAFT.txt"))
        self.assertEqual(("", ""), identity_of("RAW", "RAW.txt"))
        for names, code in [
            ({**NAMES, "DRAFT": "008_SAMPLE_SERIES_VOL1_DRAFT.txt"}, "V5_IDENTITY_MISMATCH"),
            ({**NAMES, "PRONOUN": "007_PRONOUN_OTHER.csv"}, "V5_IDENTITY_MISMATCH"),
            ({**NAMES, "RAW": "chapter-seven.txt"}, "V5_IDENTITY_MISSING"),
        ]:
            with tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                roles = self.write_chapter(root, names=names)
                with self.assertRaisesRegex(PreflightError, code):
                    check_chapter(root, "007", roles)

    def test_refuses_placeholder_names_and_wrong_role_sets(self):
        generic = {"RAW": "RAW.txt", "DRAFT": "DRAFT.txt", "GLOSSARY": "GLOSSARY.csv", "PRONOUN": "PRONOUN.csv"}
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            roles = self.write_chapter(root, names=generic)
            with self.assertRaisesRegex(PreflightError, "V5_SOURCE_NAME_INVALID"):
                check_chapter(root, "007", roles)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            roles = self.write_chapter(root)
            del roles["007"]["PRONOUN"]
            with self.assertRaisesRegex(PreflightError, "V5_SOURCE_FILE_SET_INVALID"):
                check_chapter(root, "007", roles)

    def test_line_counts_match_the_app_rules(self):
        self.assertEqual((3, 2), line_counts("\ufeffa\r\n\r\nb\r\n"))
        self.assertEqual((3, 3), line_counts("a\nx\nb"))
        self.assertEqual((2, 2), line_counts("a\rb\r"))
        self.assertEqual((0, 0), line_counts(""))
        self.assertEqual((1, 0), line_counts("\n"))

    def test_rejects_missing_empty_bad_utf8_and_wrong_headers(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            roles = self.write_chapter(root)
            (root / "007" / NAMES["PRONOUN"]).unlink()
            with self.assertRaisesRegex(PreflightError, "V5_SOURCE_FILE_MISSING"):
                check_chapter(root, "007", roles)
        for kwargs, code in [
            ({"glossary": "source,target,category,note\na,b,c,d\n"}, "V5_GLOSSARY_SCHEMA_INVALID"),
            ({"glossary": "source,target,category,note,priority\na,b,c,d,1\na,b,c\n"}, "V5_GLOSSARY_SCHEMA_INVALID"),
            ({"pronoun": "from,speaker,target,self,call,note\na,b,c,d,e,f\n"}, "V5_PRONOUN_SCHEMA_INVALID"),
            ({"pronoun": "from,speaker,target,self,call,scope,note\na,b,c,d,e,f,g\na,b,c\n"}, "V5_PRONOUN_SCHEMA_INVALID"),
            ({"bad_utf8": True}, "V5_SOURCE_UTF8_INVALID"),
        ]:
            with tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                roles = self.write_chapter(root, **kwargs)
                with self.assertRaisesRegex(PreflightError, code):
                    check_chapter(root, "007", roles)


if __name__ == "__main__":
    unittest.main()
