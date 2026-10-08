#!/usr/bin/env python3
"""Offline source-pack preflight for Q2 V5; emits names, hashes and schema status only."""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import sys
from pathlib import Path

REQUIRED = ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv")
GLOSSARY_HEADER = ["source", "target", "category", "note", "priority"]
PRONOUN_HEADER = ["from", "speaker", "target", "self", "call", "scope", "note"]
LEGACY_HEADER = ["from", "target", "note"]


class PreflightError(ValueError):
    def __init__(self, code: str, chapter: str, filename: str = "") -> None:
        super().__init__(code)
        self.code = code
        self.chapter = chapter
        self.filename = filename


def _rows(text: str) -> list[list[str]]:
    return list(csv.reader(io.StringIO(text, newline="")))


def check_chapter(root: Path, chapter: str) -> dict[str, object]:
    folder = root / chapter
    if not folder.is_dir():
        raise PreflightError("V5_SOURCE_CHAPTER_MISSING", chapter)
    records: list[dict[str, object]] = []
    texts: dict[str, str] = {}
    for name in REQUIRED:
        path = folder / name
        try:
            data = path.read_bytes()
        except OSError as error:
            raise PreflightError("V5_SOURCE_FILE_MISSING", chapter, name) from error
        if not data:
            raise PreflightError("V5_SOURCE_EMPTY", chapter, name)
        try:
            text = data.decode("utf-8", errors="strict")
        except UnicodeDecodeError as error:
            raise PreflightError("V5_SOURCE_UTF8_INVALID", chapter, name) from error
        if not text.strip():
            raise PreflightError("V5_SOURCE_EMPTY", chapter, name)
        texts[name] = text
        records.append({"name": name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})

    glossary_rows = _rows(texts["GLOSSARY.csv"])
    if not glossary_rows or [cell.lstrip("\ufeff").strip() for cell in glossary_rows[0]] != GLOSSARY_HEADER:
        raise PreflightError("V5_GLOSSARY_SCHEMA_INVALID", chapter, "GLOSSARY.csv")
    if any(row and not row[0].strip().startswith("#") and len(row) != 5 for row in glossary_rows[1:]):
        raise PreflightError("V5_GLOSSARY_SCHEMA_INVALID", chapter, "GLOSSARY.csv")
    pronoun_rows = [row for row in _rows(texts["PRONOUN.csv"]) if row and not row[0].strip().startswith("#")]
    if not pronoun_rows:
        raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, "PRONOUN.csv")
    header = [cell.lstrip("\ufeff").strip().lower() for cell in pronoun_rows[0]]
    if header == PRONOUN_HEADER:
        if any(len(row) != 7 for row in pronoun_rows[1:]):
            raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, "PRONOUN.csv")
    elif header == LEGACY_HEADER:
        if any(len(row) != 3 for row in pronoun_rows[1:]):
            raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, "PRONOUN.csv")
    elif header[0] in {"from", "source", "nguồn"}:
        raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, "PRONOUN.csv")
    elif any(len(row) != 3 for row in pronoun_rows):
        raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, "PRONOUN.csv")

    return {"chapter": chapter, "status": "PASS", "files": records,
            "glossaryColumns": 5, "pronounColumns": len(pronoun_rows[0])}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--chapters", required=True, nargs="+")
    args = parser.parse_args(argv)
    results: list[dict[str, object]] = []
    try:
        for chapter in args.chapters:
            results.append(check_chapter(args.root, chapter))
    except PreflightError as error:
        print(json.dumps({"status": "STOP", "chapter": error.chapter, "code": error.code,
                          "file": error.filename}, ensure_ascii=False, sort_keys=True))
        return 2
    print(json.dumps({"status": "PASS", "chapters": results}, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
