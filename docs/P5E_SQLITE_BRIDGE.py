"""Read-only SQLite bridge for the P5E offline behavioral fixture.

The production collector sends the same SQL text to sqlite3 on Android.  This
small bridge exists only for local tests where no adb/sqlite3 device process is
allowed.  It opens the fixture in SQLite read-only mode, executes the exact
query statements, and emits the sqlite3-compatible tab-separated stream with
an explicit NULL sentinel.
"""

from __future__ import annotations

import argparse
import sqlite3
from pathlib import Path


def split_statements(sql: str) -> list[str]:
    statements: list[str] = []
    current: list[str] = []
    quote: str | None = None
    for char in sql:
        current.append(char)
        if char == "'":
            if quote == "'":
                quote = None
            elif quote is None:
                quote = "'"
        elif char == '"':
            if quote == '"':
                quote = None
            elif quote is None:
                quote = '"'
        elif char == ";" and quote is None:
            statement = "".join(current[:-1]).strip()
            if statement:
                statements.append(statement)
            current = []
    tail = "".join(current).strip()
    if tail:
        statements.append(tail)
    return statements


def render(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bytes):
        return value.hex().upper()
    return str(value)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", required=True)
    parser.add_argument("--query", required=True)
    args = parser.parse_args()

    database = Path(args.database).resolve()
    query = Path(args.query).read_text(encoding="utf-8")
    uri = f"file:{database.as_posix()}?mode=ro"
    connection = sqlite3.connect(uri, uri=True)
    try:
        connection.execute("PRAGMA foreign_keys=ON")
        for statement in split_statements(query):
            cursor = connection.execute(statement)
            if cursor.description is None:
                continue
            for row in cursor.fetchall():
                print("\t".join(render(value) for value in row))
    finally:
        connection.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
