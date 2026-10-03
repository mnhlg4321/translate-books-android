#!/usr/bin/env python3
"""Create a label-free, source-only emulator payload for one private fixture dry-run."""
import argparse
import hashlib
import json
import os
import re
import sys
import uuid

SOURCE_NAMES = ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixtures-root", required=True)
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--run-dir", required=True)
    parser.add_argument("--run-id", required=True)
    args = parser.parse_args()
    try:
        run_id = str(uuid.UUID(args.run_id))
    except ValueError:
        parser.error("run id must be a UUID")
    if run_id != args.run_id.lower():
        parser.error("run id must use canonical UUID spelling")
    root = os.path.abspath(args.fixtures_root)
    run_dir = os.path.abspath(args.run_dir)
    if "6.FINAL" in root or "6.FINAL" in run_dir:
        parser.error("refusing a path through 6.FINAL")
    if os.path.exists(run_dir):
        parser.error("run directory already exists; use a new run id")
    manifest = json.load(open(args.manifest, encoding="utf-8"))
    os.makedirs(run_dir)
    transfer = os.path.join(run_dir, "to-device")
    os.makedirs(transfer)
    ids = []
    for fixture in manifest["fixtures"]:
        fixture_id = fixture["id"]
        if not re.fullmatch(r"fx-[ah][0-9]{2}", fixture_id):
            raise ValueError("fixture id violates the frozen format")
        if set(fixture["files"]) != set(SOURCE_NAMES):
            raise ValueError("fixture does not have the four frozen runtime files")
        destination = os.path.join(transfer, fixture_id)
        os.makedirs(destination)
        metadata = {"fixtureId": fixture_id, "chapter": fixture["chapter"], "files": {}}
        for name in SOURCE_NAMES:
            spec = fixture["files"][name]
            source = os.path.abspath(os.path.join(root, spec["path"].replace("/", os.sep)))
            if not source.startswith(root + os.sep) or "6.FINAL" in source:
                raise ValueError("fixture source path refused")
            with open(source, "rb") as handle:
                data = handle.read()
            if len(data) != spec["bytes"] or sha(data) != spec["sha256"]:
                raise ValueError("fixture source hash mismatch")
            target = os.path.join(destination, name)
            with open(target, "wb") as handle:
                handle.write(data)
            metadata["files"][name] = {"bytes": len(data), "sha256": spec["sha256"]}
        with open(os.path.join(transfer, fixture_id + ".runtime.json"), "w", encoding="utf-8", newline="\n") as handle:
            json.dump(metadata, handle, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            handle.write("\n")
        ids.append(fixture_id)
    with open(os.path.join(run_dir, "fixture-ids.json"), "w", encoding="utf-8", newline="\n") as handle:
        json.dump(ids, handle, separators=(",", ":"))
        handle.write("\n")
    os.makedirs(os.path.join(run_dir, "logs"))
    print("prepared fixture payloads:", len(ids), "(four source files each; no labels)")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError) as error:
        print("fixture preparation failed:", str(error), file=sys.stderr)
        sys.exit(1)
