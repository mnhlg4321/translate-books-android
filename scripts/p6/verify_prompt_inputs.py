#!/usr/bin/env python3
"""Fail closed if a selected fixture source or prompt template contains answer labels."""
import argparse
import hashlib
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SOURCE_NAMES = ("RAW.txt", "DRAFT.txt", "GLOSSARY.csv", "PRONOUN.csv")
TEMPLATE_FILES = (
    "app/src/main/java/com/ml/tblandroidtxt/OpenRouterEditorialP5PilotProvider.java",
    "app/src/main/java/com/ml/tblandroidtxt/OpenRouterEditorialL2Provider.java",
    "app/src/main/java/com/ml/tblandroidtxt/OpenRouterEditorialL3Provider.java",
    "app/src/main/assets/editorial/v5-safe4/PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4.txt",
    "app/src/main/assets/editorial/v5-safe4/PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4.txt",
    "app/src/main/assets/editorial/v5-safe4/WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4.txt",
)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def check_model_surface(fixture_id, labels, source_bytes, template_bytes):
    """Return safe error codes only; never include source or label values in diagnostics."""
    combined = b"\n".join([source_bytes, *template_bytes])
    errors = []
    for field in (b"mustContain", b"mustNotContain"):
        if field in combined:
            errors.append("LABEL_FIELD_NAME_PRESENT")
    targets = labels.get("targets", [])
    for target in targets:
        target_id = target.get("id", "").encode("utf-8")
        if target_id and target_id in combined:
            errors.append("TARGET_ID_PRESENT")
        answer_terms = []
        if target.get("old"):
            answer_terms.append(target["old"])
        answer_terms.extend(target.get("mustContain", []))
        for term in answer_terms:
            encoded = term.encode("utf-8") if term else b""
            if encoded and encoded not in source_bytes and encoded in combined:
                errors.append("CORRECTED_LABEL_TEXT_PRESENT")
                break
    return sorted(set(errors))


def load_template_bytes(repo_root):
    content = []
    for relative in TEMPLATE_FILES:
        path = os.path.join(repo_root, relative.replace("/", os.sep))
        with open(path, "rb") as handle:
            content.append(handle.read())
    return content


def check_transfer_fixture(fixture, fixtures_root, transfer_root, template_bytes):
    source_parts = []
    for name in SOURCE_NAMES:
        spec = fixture["files"].get(name)
        if not spec:
            return ["SOURCE_MANIFEST_INCOMPLETE"]
        path = os.path.join(transfer_root, fixture["id"], name)
        with open(path, "rb") as handle:
            data = handle.read()
        if len(data) != spec["bytes"] or sha(data) != spec["sha256"]:
            return ["TRANSFER_SOURCE_HASH_MISMATCH"]
        source_parts.append(data)
    label_path = os.path.join(fixtures_root, fixture["labels"]["path"].replace("/", os.sep))
    with open(label_path, "rb") as handle:
        label_bytes = handle.read()
    if sha(label_bytes) != fixture["labels"]["sha256"]:
        return ["LABEL_MANIFEST_HASH_MISMATCH"]
    labels = json.loads(label_bytes.decode("utf-8-sig"))
    return check_model_surface(fixture["id"], labels, b"\n".join(source_parts), template_bytes)


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixtures-root", required=True)
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--transfer-root", required=True)
    parser.add_argument("--fixture-ids", nargs="+", required=True)
    parser.add_argument("--repo-root", default=REPO_ROOT)
    args = parser.parse_args(argv)
    if len(args.fixture_ids) != len(set(args.fixture_ids)):
        raise ValueError("fixture selection must not contain duplicates")
    manifest = json.load(open(args.manifest, encoding="utf-8"))
    by_id = {fixture.get("id"): fixture for fixture in manifest.get("fixtures", [])}
    if any(fixture_id not in by_id for fixture_id in args.fixture_ids):
        raise ValueError("fixture selection must belong to the frozen fixture set")
    repo_root = os.path.abspath(args.repo_root)
    fixtures_root = os.path.abspath(args.fixtures_root)
    transfer_root = os.path.abspath(args.transfer_root)
    templates = load_template_bytes(repo_root)
    for fixture_id in args.fixture_ids:
        errors = check_transfer_fixture(by_id[fixture_id], fixtures_root, transfer_root, templates)
        if errors:
            raise ValueError(fixture_id + ": prompt-input guard refused: " + ",".join(errors))
    print("prompt-input guard: PASS (templates and selected transfer sources checked)")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError) as error:
        print("prompt-input guard failed:", str(error), file=sys.stderr)
        sys.exit(1)
