import tempfile
import unittest
from pathlib import Path

from v5_pack_preflight import PreflightError, check_chapter


class V5PackPreflightTests(unittest.TestCase):
    def write_chapter(self, root: Path, chapter: str = "007", *, glossary: str | None = None,
                      pronoun: str | None = None, bad_utf8: bool = False) -> Path:
        folder = root / chapter
        folder.mkdir(parents=True)
        (folder / "RAW.txt").write_text("raw\r\n", encoding="utf-8", newline="")
        (folder / "DRAFT.txt").write_text("draft\n", encoding="utf-8", newline="")
        if bad_utf8:
            (folder / "RAW.txt").write_bytes(b"\xff")
        (folder / "GLOSSARY.csv").write_text(
            glossary if glossary is not None else "source,target,category,note,priority\na,b,term,,1\n",
            encoding="utf-8", newline="")
        (folder / "PRONOUN.csv").write_text(
            pronoun if pronoun is not None else "\ufefffrom,speaker,target,self,call,scope,note\na,b,c,d,e,f,g\n",
            encoding="utf-8", newline="")
        return folder

    def test_accepts_four_nonempty_strict_utf8_files_and_pack_schemas(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write_chapter(root)
            result = check_chapter(root, "007")
            self.assertEqual("PASS", result["status"])
            self.assertEqual(["RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv"],
                             [item["name"] for item in result["files"]])

    def test_accepts_headerless_legacy_three_column_pronouns(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write_chapter(root, pronoun="花子,em,legacy\n")
            result = check_chapter(root, "007")
            self.assertEqual(3, result["pronounColumns"])

    def test_rejects_missing_empty_bad_utf8_and_wrong_headers(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            folder = self.write_chapter(root)
            (folder / "PRONOUN.csv").unlink()
            with self.assertRaisesRegex(PreflightError, "V5_SOURCE_FILE_MISSING"):
                check_chapter(root, "007")
        for kwargs, code in [
            ({"glossary": "source,target,category,note\na,b,c,d\n"}, "V5_GLOSSARY_SCHEMA_INVALID"),
            ({"glossary": "source,target,category,note,priority\na,b,c,d,1\na,b,c\n"}, "V5_GLOSSARY_SCHEMA_INVALID"),
            ({"pronoun": "from,speaker,target,self,call,note\na,b,c,d,e,f\n"}, "V5_PRONOUN_SCHEMA_INVALID"),
            ({"pronoun": "from,speaker,target,self,call,scope,note\na,b,c,d,e,f,g\na,b,c\n"}, "V5_PRONOUN_SCHEMA_INVALID"),
            ({"bad_utf8": True}, "V5_SOURCE_UTF8_INVALID"),
        ]:
            with tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                self.write_chapter(root, **kwargs)
                with self.assertRaisesRegex(PreflightError, code):
                    check_chapter(root, "007")


if __name__ == "__main__":
    unittest.main()
