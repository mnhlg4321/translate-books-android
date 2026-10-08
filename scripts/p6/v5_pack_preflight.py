#!/usr/bin/env python3
"""Offline source-pack preflight for Q2 V5; emits roles, original names, counts, hashes and schema status only.

A chapter folder holds the four original attachments under the owner's real file names. A roles file maps each role
(RAW, DRAFT, GLOSSARY, PRONOUN) to its file name; files are judged by role, and the chapter ID and series are derived from
the names exactly as the app does (V5SourceIdentity). The per-file counts and SHA-256 values are the ones the app puts into
the HOST SOURCE MANIFEST, so this script is an independent cross-check of that block. No book text is printed.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
import sys
from pathlib import Path

ROLES = ("RAW", "DRAFT", "GLOSSARY", "PRONOUN")
GENERIC_NAMES = ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv")
GLOSSARY_HEADER = ["source", "target", "category", "note", "priority"]
PRONOUN_HEADER = ["from", "speaker", "target", "self", "call", "scope", "note"]
LEGACY_HEADER = ["from", "target", "note"]
VERSION = "V5-SAFE.4.1.3-FULL"
ROLE_WORDS = {"RAW": {"RAW"}, "DRAFT": {"RAW", "DRAFT", "TRANSLATED"}, "GLOSSARY": {"CHAPTER", "GLOSSARY"}, "PRONOUN": {"PRONOUN"}}
NAME_PATTERN = re.compile(r"^([0-9]{3,6})_(.+)$")
ID_PATTERN = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._-]{0,31}$")
SERIES_PATTERN = re.compile(r"^[0-9A-Za-z][0-9A-Za-z._-]{0,119}$")


class PreflightError(ValueError):
    def __init__(self, code: str, chapter: str, filename: str = "") -> None:
        super().__init__(code)
        self.code = code
        self.chapter = chapter
        self.filename = filename


def _rows(text: str) -> list[list[str]]:
    return list(csv.reader(io.StringIO(text, newline="")))


def original_name(name: str) -> bool:
    if not name or name != name.strip() or name in GENERIC_NAMES:
        return False
    return not any(ord(c) < 0x20 or ord(c) == 0x7F or c in "/\\" for c in name)


def identity_of(role: str, name: str) -> tuple[str, str]:
    stem = name.rsplit(".", 1)[0] if "." in name[1:] else name
    match = NAME_PATTERN.match(stem)
    if not match:
        return "", ""
    words = [w for w in match.group(2).split("_") if w and w.upper() not in ROLE_WORDS.get(role, set())]
    return match.group(1), "_".join(words)


def derive_identity(chapter: str, names: dict[str, str]) -> tuple[str, str]:
    found: set[tuple[str, str]] = set()
    for role in ROLES:
        one = identity_of(role, names[role])
        if not (one[0] and one[1]):
            raise PreflightError("V5_IDENTITY_MISSING", chapter, names[role])
        found.add(one)
    if len(found) != 1:
        raise PreflightError("V5_IDENTITY_MISMATCH", chapter)
    chain_id, series = next(iter(found))
    if not ID_PATTERN.match(chain_id) or not SERIES_PATTERN.match(series):
        raise PreflightError("V5_IDENTITY_INVALID", chapter)
    return chain_id, series


def line_counts(text: str) -> tuple[int, int]:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    parts = normalized.split("\n")
    count = len(parts)
    if count and parts[-1] == "":
        count -= 1
    if normalized == "":
        count = 0
    nonblank = sum(1 for line in parts[:count] if line.strip())
    return count, nonblank


def check_chapter(root: Path, chapter: str, roles: dict[str, dict[str, str]] | None = None) -> dict[str, object]:
    folder = root / chapter
    if not folder.is_dir():
        raise PreflightError("V5_SOURCE_CHAPTER_MISSING", chapter)
    if roles is None:
        roles = json.loads((root / "roles.json").read_text(encoding="utf-8"))
    names = roles.get(chapter)
    if not isinstance(names, dict) or set(names) != set(ROLES):
        raise PreflightError("V5_SOURCE_FILE_SET_INVALID", chapter)
    if len(set(names.values())) != len(ROLES):
        raise PreflightError("V5_SOURCE_FILE_SET_INVALID", chapter)
    for role in ROLES:
        if not original_name(names[role]):
            raise PreflightError("V5_SOURCE_NAME_INVALID", chapter, names[role])
    records: list[dict[str, object]] = []
    texts: dict[str, str] = {}
    for role in ROLES:
        name = names[role]
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
        texts[role] = text
        lines, nonblank = line_counts(text)
        records.append({"role": role, "name": name, "bytes": len(data), "chars": len(text), "lines": lines,
                        "nonblankLines": nonblank, "sha256": hashlib.sha256(data).hexdigest()})

    glossary_rows = _rows(texts["GLOSSARY"])
    if not glossary_rows or [cell.lstrip("﻿").strip() for cell in glossary_rows[0]] != GLOSSARY_HEADER:
        raise PreflightError("V5_GLOSSARY_SCHEMA_INVALID", chapter, names["GLOSSARY"])
    if any(row and not row[0].strip().startswith("#") and len(row) != 5 for row in glossary_rows[1:]):
        raise PreflightError("V5_GLOSSARY_SCHEMA_INVALID", chapter, names["GLOSSARY"])
    pronoun_rows = [row for row in _rows(texts["PRONOUN"]) if row and not row[0].strip().startswith("#")]
    if not pronoun_rows:
        raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, names["PRONOUN"])
    header = [cell.lstrip("﻿").strip().lower() for cell in pronoun_rows[0]]
    if header == PRONOUN_HEADER:
        if any(len(row) != 7 for row in pronoun_rows[1:]):
            raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, names["PRONOUN"])
    elif header == LEGACY_HEADER:
        if any(len(row) != 3 for row in pronoun_rows[1:]):
            raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, names["PRONOUN"])
    elif header[0] in {"from", "source", "nguồn"}:
        raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, names["PRONOUN"])
    elif any(len(row) != 3 for row in pronoun_rows):
        raise PreflightError("V5_PRONOUN_SCHEMA_INVALID", chapter, names["PRONOUN"])

    chain_id, series = derive_identity(chapter, names)
    return {"chapter": chapter, "status": "PASS", "id": chain_id, "series": series, "version": VERSION, "files": records,
            "glossaryColumns": 5, "pronounColumns": len(pronoun_rows[0])}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--chapters", required=True, nargs="+")
    parser.add_argument("--roles", type=Path, help="roles.json (default: <root>/roles.json)")
    args = parser.parse_args(argv)
    roles = json.loads((args.roles or args.root / "roles.json").read_text(encoding="utf-8"))
    results: list[dict[str, object]] = []
    try:
        for chapter in args.chapters:
            results.append(check_chapter(args.root, chapter, roles))
    except PreflightError as error:
        print(json.dumps({"status": "STOP", "chapter": error.chapter, "code": error.code,
                          "file": error.filename}, ensure_ascii=False, sort_keys=True))
        return 2
    print(json.dumps({"status": "PASS", "chapters": results}, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
