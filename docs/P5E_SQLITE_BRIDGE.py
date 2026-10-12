"""Bounded, read-only host SQLite bridge for P5E database snapshots."""

from __future__ import annotations

import argparse
import re
import sqlite3
import sys
import time
from urllib.parse import quote
from pathlib import Path


def split_statements(sql: str) -> list[str]:
    statements: list[str] = []
    current: list[str] = []
    quote: str | None = None
    index = 0
    while index < len(sql):
        char = sql[index]
        current.append(char)
        if char == "'":
            if quote == "'" and index + 1 < len(sql) and sql[index + 1] == "'":
                current.append(sql[index + 1])
                index += 1
            elif quote == "'":
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
        index += 1
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


class BridgeError(RuntimeError):
    pass


def sqlite_uri(database: Path, immutable: bool) -> str:
    # Keep the drive-colon and path separators structural, but encode spaces,
    # '#', '%', and every other URI-significant character in the Windows path.
    encoded = quote(database.as_posix(), safe="/:")
    suffix = "mode=ro&immutable=1" if immutable else "mode=ro"
    return f"file:{encoded}?{suffix}"


def validate_read_only_statement(statement: str) -> None:
    normalized = statement.strip()
    upper = normalized.upper()
    if not normalized:
        return
    forbidden = (
        "BEGIN", "COMMIT", "END", "ROLLBACK", "SAVEPOINT", "RELEASE",
        "VACUUM", "CREATE", "ALTER", "DROP", "INSERT", "UPDATE",
        "DELETE", "REPLACE", "REINDEX", "ATTACH", "DETACH",
    )
    if upper.startswith(forbidden):
        raise BridgeError("P5E_SQLITE_BRIDGE_WRITE_STATEMENT_REJECTED")
    if upper.startswith("WITH ") and re.search(r"\b(?:INSERT|UPDATE|DELETE|REPLACE|CREATE|ALTER|DROP|VACUUM)\b", upper):
        raise BridgeError("P5E_SQLITE_BRIDGE_WRITE_STATEMENT_REJECTED")
    if not (upper.startswith("SELECT") or upper.startswith("WITH ") or upper.startswith("PRAGMA ")):
        raise BridgeError("P5E_SQLITE_BRIDGE_STATEMENT_REJECTED")
    if upper.startswith("PRAGMA "):
        pragma = upper[7:].strip()
        pragma_without_spacing = re.sub(r"\s+", "", pragma)
        if pragma_without_spacing.startswith("QUERY_ONLY=") and pragma_without_spacing != "QUERY_ONLY=ON":
            raise BridgeError("P5E_SQLITE_BRIDGE_WRITE_PRAGMA_REJECTED")
        if pragma_without_spacing.startswith("FOREIGN_KEYS=") and pragma_without_spacing != "FOREIGN_KEYS=ON":
            raise BridgeError("P5E_SQLITE_BRIDGE_WRITE_PRAGMA_REJECTED")
        if "=" in pragma and not pragma_without_spacing.startswith("FOREIGN_KEYS=") and not pragma_without_spacing.startswith("QUERY_ONLY="):
            raise BridgeError("P5E_SQLITE_BRIDGE_WRITE_PRAGMA_REJECTED")


class BoundedOutput:
    def __init__(self, maximum_bytes: int) -> None:
        self.maximum_bytes = maximum_bytes
        self.written_bytes = 0

    def write_line(self, value: str) -> None:
        payload = (value + "\n").encode("utf-8")
        if self.written_bytes + len(payload) > self.maximum_bytes:
            raise BridgeError("P5E_SQLITE_BRIDGE_OUTPUT_LIMIT")
        sys.stdout.buffer.write(payload)
        self.written_bytes += len(payload)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", required=True)
    parser.add_argument("--query", required=True)
    parser.add_argument("--immutable", action="store_true")
    parser.add_argument("--timeout-ms", type=int, default=60000)
    parser.add_argument("--max-output-bytes", type=int, default=4194304)
    args = parser.parse_args()
    if args.timeout_ms <= 0 or args.max_output_bytes <= 0:
        raise BridgeError("P5E_SQLITE_BRIDGE_LIMIT_INVALID")

    database = Path(args.database).resolve(strict=True)
    query_path = Path(args.query).resolve(strict=True)
    if not database.is_file() or not query_path.is_file():
        raise BridgeError("P5E_SQLITE_BRIDGE_INPUT_MISSING")
    query = query_path.read_text(encoding="utf-8")
    statements = split_statements(query)
    if not statements:
        raise BridgeError("P5E_SQLITE_BRIDGE_QUERY_EMPTY")
    for statement in statements:
        validate_read_only_statement(statement)

    deadline = time.monotonic() + (args.timeout_ms / 1000.0)
    connection = sqlite3.connect(sqlite_uri(database, args.immutable), uri=True)
    output = BoundedOutput(args.max_output_bytes)
    connection.set_progress_handler(
        lambda: 1 if time.monotonic() >= deadline else 0,
        1000,
    )
    try:
        connection.execute("PRAGMA query_only=ON")
        connection.execute("PRAGMA foreign_keys=ON")
        connection.execute("BEGIN")
        for statement in statements:
            cursor = connection.execute(statement)
            if cursor.description is None:
                continue
            for row in cursor:
                output.write_line("\t".join(render(value) for value in row))
        connection.rollback()
    finally:
        connection.set_progress_handler(None, 0)
        connection.close()
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BridgeError as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(2)
    except Exception:
        print("P5E_SQLITE_BRIDGE_FAILED", file=sys.stderr)
        raise SystemExit(3)
