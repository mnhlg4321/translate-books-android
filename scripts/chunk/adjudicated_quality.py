#!/usr/bin/env python3
"""Score manually adjudicated chunk-edit annotations without consulting FINAL.

This is an offline QA tool. It does not parse or alter provider responses and it
is never part of the runtime prompt or app call path. Input annotations contain
only occurrence forms, source anchors/hashes, and human decisions.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

SCHEMA = "chunk-adjudicated-quality-v1"
HASH_RE = re.compile(r"^[0-9a-fA-F]{64}$")
ADDRESS_STATES = {"ADJUDICATED", "PRESERVE", "UNADJUDICATED"}
UNIT_STATES = {"ADJUDICATED", "UNADJUDICATED"}


class AnnotationError(ValueError):
    """The annotation is incomplete or cannot be safely interpreted."""


def _anchor(item: dict[str, Any], path: str) -> None:
    anchor = item.get("raw_anchor")
    if not isinstance(anchor, dict) or anchor.get("source_role") != "RAW":
        raise AnnotationError(f"{path}: raw_anchor.source_role must be RAW")
    if not isinstance(anchor.get("range"), str) or not anchor["range"].strip():
        raise AnnotationError(f"{path}: raw_anchor.range is required")
    digest = anchor.get("sha256")
    if not isinstance(digest, str) or not HASH_RE.fullmatch(digest):
        raise AnnotationError(f"{path}: raw_anchor.sha256 must be 64 hex characters")
    excerpt = anchor.get("synthetic_raw")
    if excerpt is not None:
        _require_text(excerpt, f"{path}.raw_anchor.synthetic_raw")
        observed = hashlib.sha256(excerpt.encode("utf-8")).hexdigest()
        if observed.lower() != digest.lower():
            raise AnnotationError(f"{path}: synthetic RAW anchor hash mismatch")


def _require_text(value: Any, path: str) -> None:
    if not isinstance(value, str) or not value:
        raise AnnotationError(f"{path}: required non-empty text")


def evaluate(data: dict[str, Any]) -> dict[str, Any]:
    """Score the supplied adjudicated occurrences and content units.

    Address categories are occurrence-based. A defective DRAFT occurrence that
    is unchanged or deleted is a miss; a changed unacceptable form is a wrong
    fix. A previously acceptable form changed or deleted is a new error.
    Ambiguous/conflicting occurrences have exactly one acceptable output: the
    original DRAFT form. Unadjudicated items are excluded from denominators.

    Completeness units are RAW-required semantic atoms in the main chunk. A
    reference-only atom must not appear in the edited main chunk; its appearance
    is reported separately as a boundary leak. No text similarity is computed.
    """
    if not isinstance(data, dict) or data.get("schema") != SCHEMA:
        raise AnnotationError(f"input.schema must be {SCHEMA}")

    address = Counter()
    completeness = Counter()
    controls = Counter()
    other_edits = Counter()
    seen_ids: set[str] = set()

    addresses = data.get("address_occurrences", [])
    units = data.get("content_units", [])
    non_occurrences = data.get("non_occurrence_controls", [])
    edits = data.get("other_edits", [])
    for key, rows in (("address_occurrences", addresses), ("content_units", units),
                      ("other_edits", edits),
                      ("non_occurrence_controls", non_occurrences)):
        if not isinstance(rows, list):
            raise AnnotationError(f"{key} must be a list")

    for index, item in enumerate(addresses):
        path = f"address_occurrences[{index}]"
        if not isinstance(item, dict):
            raise AnnotationError(f"{path} must be an object")
        case_id = item.get("id")
        _require_text(case_id, f"{path}.id")
        if case_id in seen_ids:
            raise AnnotationError(f"{path}.id is duplicated")
        seen_ids.add(case_id)
        _anchor(item, path)
        state = item.get("adjudication")
        if not isinstance(state, str) or state not in ADDRESS_STATES:
            raise AnnotationError(f"{path}.adjudication is invalid")
        _require_text(item.get("scope"), f"{path}.scope")
        _require_text(item.get("defect_type"), f"{path}.defect_type")
        draft = item.get("draft_form")
        candidate = item.get("candidate_form")
        if not isinstance(draft, str) or not draft:
            raise AnnotationError(f"{path}.draft_form is required")
        if candidate is not None and (not isinstance(candidate, str) or not candidate):
            raise AnnotationError(f"{path}.candidate_form must be text or null")
        if state == "UNADJUDICATED":
            address["unadjudicated_excluded"] += 1
            continue

        if state == "PRESERVE":
            acceptable = {draft}
        else:
            acceptable_values = item.get("acceptable_forms")
            if (not isinstance(acceptable_values, list) or not acceptable_values
                    or any(not isinstance(value, str) or not value for value in acceptable_values)):
                raise AnnotationError(f"{path}.acceptable_forms must be a non-empty list")
            acceptable = set(acceptable_values)

        speaker = item.get("speaker_id")
        target = item.get("target_id")
        if state == "ADJUDICATED":
            _require_text(speaker, f"{path}.speaker_id")
            _require_text(target, f"{path}.target_id")
        if state != "UNADJUDICATED":
            _require_text(item.get("adjudication_note"), f"{path}.adjudication_note")

        defective = draft not in acceptable
        address["adjudicated_count"] += 1
        if defective:
            address["denominator"] += 1
            if candidate is None or candidate == draft:
                address["missed_errors"] += 1
            elif candidate in acceptable:
                address["correct_fixes"] += 1
            else:
                address["wrong_fixes"] += 1
        else:
            address["clean_denominator"] += 1
            if candidate == draft:
                address["clean_unchanged"] += 1
            elif candidate in acceptable:
                address["redundant_acceptable_changes"] += 1
            else:
                address["new_errors"] += 1

    for index, item in enumerate(units):
        path = f"content_units[{index}]"
        if not isinstance(item, dict):
            raise AnnotationError(f"{path} must be an object")
        case_id = item.get("id")
        _require_text(case_id, f"{path}.id")
        if case_id in seen_ids:
            raise AnnotationError(f"{path}.id is duplicated")
        seen_ids.add(case_id)
        _anchor(item, path)
        state = item.get("adjudication")
        if not isinstance(state, str) or state not in UNIT_STATES:
            raise AnnotationError(f"{path}.adjudication is invalid")
        required = item.get("required_by_raw")
        draft_present = item.get("draft_present")
        candidate_present = item.get("candidate_present")
        if not all(isinstance(value, bool) for value in (required, draft_present, candidate_present)):
            raise AnnotationError(f"{path}: required_by_raw/draft_present/candidate_present must be booleans")
        scope = item.get("scope", "MAIN")
        if not isinstance(scope, str) or scope not in {"MAIN", "REFERENCE"}:
            raise AnnotationError(f"{path}.scope must be MAIN or REFERENCE")
        if state == "UNADJUDICATED":
            completeness["unadjudicated_excluded"] += 1
            continue
        _require_text(item.get("adjudication_note"), f"{path}.adjudication_note")
        completeness["adjudicated_count"] += 1
        if scope == "REFERENCE":
            if candidate_present:
                completeness["boundary_leaks"] += 1
            else:
                completeness["reference_only_absent"] += 1
            continue
        if not required:
            if candidate_present and not draft_present:
                completeness["unsupported_additions"] += 1
            continue
        completeness["denominator"] += 1
        if not draft_present and candidate_present:
            completeness["omissions_restored"] += 1
        elif not draft_present and not candidate_present:
            completeness["omissions_remaining"] += 1
        elif draft_present and not candidate_present:
            completeness["new_omissions"] += 1
        else:
            completeness["required_units_retained"] += 1

    for index, item in enumerate(edits):
        path = f"other_edits[{index}]"
        if not isinstance(item, dict):
            raise AnnotationError(f"{path} must be an object")
        case_id = item.get("id")
        _require_text(case_id, f"{path}.id")
        if case_id in seen_ids:
            raise AnnotationError(f"{path}.id is duplicated")
        seen_ids.add(case_id)
        _anchor(item, path)
        state = item.get("adjudication")
        if not isinstance(state, str) or state not in UNIT_STATES:
            raise AnnotationError(f"{path}.adjudication is invalid")
        _require_text(item.get("defect_type"), f"{path}.defect_type")
        if state == "UNADJUDICATED":
            other_edits["unadjudicated_excluded"] += 1
            continue
        _require_text(item.get("adjudication_note"), f"{path}.adjudication_note")
        draft_defect = item.get("draft_has_defect")
        candidate_defect = item.get("candidate_has_defect")
        candidate_changed = item.get("candidate_changed")
        supported = item.get("candidate_supported_by_raw")
        if not all(isinstance(value, bool) for value in (draft_defect, candidate_defect,
                                                         candidate_changed, supported)):
            raise AnnotationError(f"{path}: defect/change/support fields must be booleans")
        if not candidate_changed and draft_defect != candidate_defect:
            raise AnnotationError(f"{path}: unchanged candidate cannot change defect status")
        other_edits["adjudicated_count"] += 1
        if draft_defect:
            other_edits["denominator"] += 1
            if candidate_defect:
                other_edits["missed_errors" if not candidate_changed else "wrong_fixes"] += 1
            elif supported:
                other_edits["correct_fixes"] += 1
            else:
                other_edits["wrong_fixes"] += 1
        else:
            other_edits["clean_denominator"] += 1
            if candidate_defect:
                other_edits["new_errors"] += 1
            elif candidate_changed and not supported:
                other_edits["unsupported_changes"] += 1
            else:
                other_edits["clean_unchanged"] += 1

    for index, item in enumerate(non_occurrences):
        path = f"non_occurrence_controls[{index}]"
        if not isinstance(item, dict):
            raise AnnotationError(f"{path} must be an object")
        _require_text(item.get("id"), f"{path}.id")
        if item["id"] in seen_ids:
            raise AnnotationError(f"{path}.id is duplicated")
        seen_ids.add(item["id"])
        _anchor(item, path)
        if item.get("decision") != "NOT_AN_ADDRESS_OCCURRENCE":
            raise AnnotationError(f"{path}.decision is invalid")
        if item.get("candidate_changed") is not False:
            raise AnnotationError(f"{path}.candidate_changed must be false for a non-occurrence control")
        controls["non_address_controls_passed"] += 1

    has_issues = bool(
        address["missed_errors"] or address["wrong_fixes"] or address["new_errors"]
        or other_edits["missed_errors"] or other_edits["wrong_fixes"]
        or other_edits["new_errors"] or other_edits["unsupported_changes"]
        or completeness["omissions_remaining"] or completeness["new_omissions"]
        or completeness["unsupported_additions"] or completeness["boundary_leaks"]
    )
    def metric(counter: Counter) -> dict[str, int | str]:
        result: dict[str, int | str] = dict(sorted(counter.items()))
        # A clean-only control still measures collateral errors. Its zero defect
        # denominator means repair recall is unmeasured, not that the control is absent.
        result["status"] = "MEASURED" if counter["adjudicated_count"] else "NOT_MEASURED"
        return result

    measured = any(c["adjudicated_count"] for c in (address, completeness, other_edits)) or bool(controls)
    return {
        "schema": SCHEMA,
        "address": metric(address),
        "completeness": metric(completeness),
        "other_edits": metric(other_edits),
        "controls": dict(sorted(controls.items())),
        "measurement_status": "SCORED" if measured else "NOT_MEASURED",
        "quality_outcome": ("ISSUES_PRESENT" if has_issues else "NO_ADJUDICATED_ISSUES") if measured else "NOT_MEASURED",
        "semantics": "Annotation measurement only; not app acceptance or model-quality certification",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path, help="offline annotation JSON")
    args = parser.parse_args(argv)
    try:
        data = json.loads(args.input.read_text(encoding="utf-8-sig"))
        result = evaluate(data)
    except FileNotFoundError:
        print(json.dumps({"error": "INPUT_NOT_FOUND", "path": str(args.input)}, ensure_ascii=False))
        return 2
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        print(json.dumps({"error": "INPUT_INVALID_JSON", "detail": str(exc)}, ensure_ascii=False))
        return 2
    except AnnotationError as exc:
        print(json.dumps({"error": "ANNOTATION_INVALID", "detail": str(exc)}, ensure_ascii=False))
        return 2
    print(json.dumps(result, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
