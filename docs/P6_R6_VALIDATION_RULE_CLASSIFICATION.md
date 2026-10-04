# P6 R6 — validation rule classification (Z1, 2026-10-05)

Source of truth: `editorial-engine/src/test/resources/rejection-classification.csv`; the test `EditorialRejectionClassificationTest` fails when a code literal of a model-facing parser, receipt or validator is not in the table, when a table row is `BOOKKEEPING` yet its literal still exists in the sources, or when a label is unknown. This page is generated from that file.

Rule: **if dropping or fixing a value changes no claim the app verified against the source, it is BOOKKEEPING** (normalize, record `kind:path` in the artifact `normalizations`/`wireWarnings`, never invent content). Everything the app can disprove from the source or the applied text stays **SEMANTIC** (refused or measured). **PROTOCOL** = envelope, echo, JSON typing (constrained by the strict schema at generation time). **APP** = input, chain, store, provider, budget, receipt and stop-reason labels the model cannot cause. **MIXED** = some paths are normalized (see action), the rest stay rejected. **NOT_A_CODE** = enum values, roles and phase names that matched the literal scan.

Totals: APP 169, BOOKKEEPING 11, MIXED 20, NOT_A_CODE 61, PROTOCOL 30, SEMANTIC 132 (total 423).

## BOOKKEEPING (11)

| Code | Phase | Action / reason |
|---|---|---|
| `CHANGE_BEFORE_SUBSTRING_MISMATCH` | L2/L3 | already a warning: the app derives `before` from the line (wireWarnings) |
| `FINAL_READ_DEFECT_LIMIT_EXCEEDED` | final-read | removed: the defect list is cut at the cap, note listTruncated:defects |
| `L1_DRAFT_ANCHOR_OUT_OF_RANGE` | L1 | removed: start/end hints and MISSING.after are clamped, note anchorClamped:<path> |
| `L1_DRAFT_ANCHOR_UNUSED_FIELD` | L1 | removed: `after` of LINES and `start`/`end` of MISSING are ignored, note unusedAnchorFieldIgnored:<path> |
| `L1_EVIDENCE_REF_INVALID` | L1 | removed: unusable evidenceRefs items are dropped, note listItemDropped:<path> |
| `L1_PROTECTED_ID_DUPLICATE` | L1 | removed: duplicate/invalid span id is renumbered, a range is swapped/clamped to the draft or the span dropped; notes protectedSpanIdAssigned/protectedSpanRangeAdjusted/protectedSpanDropped |
| `L1_PROTECTED_RANGE_INVALID` | L1 | removed: duplicate/invalid span id is renumbered, a range is swapped/clamped to the draft or the span dropped; notes protectedSpanIdAssigned/protectedSpanRangeAdjusted/protectedSpanDropped |
| `L1_SPEAKER_LIMIT_EXCEEDED` | L1 | removed: records beyond the cap are dropped, counted in speakerRecordsDropped |
| `L1_UNKNOWN_KEY` | L1/L2/L3 | removed: unknown keys are ignored, note unknownKeyIgnored:<path> |
| `L2_WIRE_UNKNOWN_KEY` | L1/L2/L3 | removed: unknown keys are ignored, note unknownKeyIgnored:<path> |
| `L3_PROBE_UNIT_UNKNOWN` | L3 | removed: unusable probe rawUnits references are dropped and the unit carrying rawQuote is the anchor (rawAnchorDerivedFromQuote) |

## MIXED (20)

| Code | Phase | Action / reason |
|---|---|---|
| `FINAL_READ_KEYS_INVALID` | L1/L2/L3 | MAY keys and MAY arrays absent => empty value, note missingOptionalKeyDefaulted:<path>; a required (MUST) key still rejects |
| `FINAL_READ_TEXT_CONTROL_CHARACTER` | final-read | defect notes are sanitized (control characters, length); a blank quote/tail/verdict stays rejected |
| `FINAL_READ_TEXT_REQUIRED` | final-read | defect notes are sanitized (control characters, length); a blank quote/tail/verdict stays rejected |
| `L1_DISPOSITION_REASON_INVALID` | L1/L2/L3 | CONTINUE/PRESERVE_DRAFT: reason label dropped when unsafe and stop class forced to NONE (dispositionReasonDropped/dispositionStopClassIgnored); a STOP disposition stays strict |
| `L1_DISPOSITION_STOP_CLASS_INVALID` | L1/L2/L3 | CONTINUE/PRESERVE_DRAFT: reason label dropped when unsafe and stop class forced to NONE (dispositionReasonDropped/dispositionStopClassIgnored); a STOP disposition stays strict |
| `L1_LIST_INVALID` | L1 | evidenceRefs/occurrenceUnits: unusable item dropped, extra items cut (listItemDropped/unitReferenceDropped/listTruncated); candidateIds, rawUnits and other reference lists stay rejected |
| `L1_LIST_ITEM_INVALID` | L1 | evidenceRefs/occurrenceUnits: unusable item dropped, extra items cut (listItemDropped/unitReferenceDropped/listTruncated); candidateIds, rawUnits and other reference lists stay rejected |
| `L1_LIST_TOO_LONG` | L1 | evidenceRefs/occurrenceUnits: unusable item dropped, extra items cut (listItemDropped/unitReferenceDropped/listTruncated); candidateIds, rawUnits and other reference lists stay rejected |
| `L1_MISSING_KEY` | L1/L2/L3 | MAY keys and MAY arrays absent => empty value, note missingOptionalKeyDefaulted:<path>; a required (MUST) key still rejects |
| `L1_TEXT_CONTROL_CHARACTER` | L1/L2/L3 | free-text notes (observation, expectedMeaning, evidenceLimit, note, reason, scope, contrast, labels): control characters replaced and overlong text cut, notes textControlCharactersReplaced/textTruncated; quotes and identifiers stay rejected |
| `L1_TEXT_REQUIRED` | L1/L2/L3 | blank content fields stay rejected; blank speaker/basis drops the speaker record; blank protected-span reason is stated as "reason not stated" |
| `L1_TEXT_TOO_LONG` | L1/L2/L3 | free-text notes (observation, expectedMeaning, evidenceLimit, note, reason, scope, contrast, labels): control characters replaced and overlong text cut, notes textControlCharactersReplaced/textTruncated; quotes and identifiers stay rejected |
| `L1_UNIT_LINE_NOT_A_UNIT` | L1/L2/L3 | anchor units (rawUnits, coverage, candidates) stay SEMANTIC; speakerRecords and extra occurrenceUnits with such a reference are dropped (speakerRecordsDropped / unitReferenceDropped) |
| `L1_UNIT_REF_INVALID` | L1/L2/L3 | anchor units (rawUnits, coverage, candidates) stay SEMANTIC; speakerRecords and extra occurrenceUnits with such a reference are dropped (speakerRecordsDropped / unitReferenceDropped) |
| `L1_UNIT_UNKNOWN` | L1/L2/L3 | anchor units (rawUnits, coverage, candidates) stay SEMANTIC; speakerRecords and extra occurrenceUnits with such a reference are dropped (speakerRecordsDropped / unitReferenceDropped) |
| `L2_WIRE_MISSING_KEY` | L1/L2/L3 | MAY keys and MAY arrays absent => empty value, note missingOptionalKeyDefaulted:<path>; a required (MUST) key still rejects |
| `L2_WIRE_REASON_INVALID` | L1/L2/L3 | CONTINUE/PRESERVE_DRAFT: reason label dropped when unsafe and stop class forced to NONE (dispositionReasonDropped/dispositionStopClassIgnored); a STOP disposition stays strict |
| `L2_WIRE_STOP_CLASS_INVALID` | L1/L2/L3 | CONTINUE/PRESERVE_DRAFT: reason label dropped when unsafe and stop class forced to NONE (dispositionReasonDropped/dispositionStopClassIgnored); a STOP disposition stays strict |
| `L2_WIRE_TEXT_REQUIRED` | L2/L3 | blank content fields stay rejected; note fields are sanitized, not rejected |
| `L3_PROBE_VI_ANCHOR_OUT_OF_RANGE` | L3 | viStart/viEnd are hints (clamped; the line carrying viQuote decides the anchor); the code remains only for a VI text with no lines at all |

## SEMANTIC (132)

| Code | Phase | Action / reason |
|---|---|---|
| `CHANGE_AFTER_INVALID` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `CHANGE_AFTER_REQUIRED` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `CHANGE_ANCHOR_MISMATCH` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `CHANGE_DELETE_AFTER_NOT_EMPTY` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `CHANGE_ERROR_ID_INVALID` | L2/L3 | row identity or required row fields |
| `CHANGE_ID_DUPLICATE` | L2/L3 | row identity or required row fields |
| `CHANGE_ID_INVALID` | L2/L3 | row identity or required row fields |
| `CHANGE_LINE_CONFLICT` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `CHANGE_LINE_OUT_OF_RANGE` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `CHANGE_NO_OP` | L2/L3 | a change that changes nothing is reported by the reconstructor; not a formatting slip |
| `CHANGE_REASON_MISSING` | L2/L3 | row identity or required row fields |
| `CHANGE_ROW_MISSING` | L2/L3 | row identity or required row fields |
| `CHANGE_STATUS_MISSING` | L2/L3 | row identity or required row fields |
| `DECLARED_REVERTED` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `DIFF_RECONSTRUCTION_MISMATCH` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `FINAL_READ_DEFECT_LINE_OUT_OF_RANGE` | final-read | a defect report that contradicts the text or the verdict |
| `FINAL_READ_DEFECT_QUOTE_NOT_IN_LINE` | final-read | a defect report that contradicts the text or the verdict |
| `FINAL_READ_DEFECT_TYPE_INVALID` | final-read | a defect report that contradicts the text or the verdict |
| `FINAL_READ_HASH_ECHO_MISMATCH` | final-read | proof that the model read the exact bytes: the hash and probe tails are the evidence, so a mismatch stays a refusal |
| `FINAL_READ_PROBE_INVALID` | final-read | proof that the model read the exact bytes: the hash and probe tails are the evidence, so a mismatch stays a refusal |
| `FINAL_READ_PROBE_SET_MISMATCH` | final-read | proof that the model read the exact bytes: the hash and probe tails are the evidence, so a mismatch stays a refusal |
| `FINAL_READ_PROBE_TAIL_MISMATCH` | final-read | proof that the model read the exact bytes: the hash and probe tails are the evidence, so a mismatch stays a refusal |
| `FINAL_READ_VERDICT_DEFECT_MISMATCH` | final-read | a defect report that contradicts the text or the verdict |
| `FINAL_READ_VERDICT_INVALID` | final-read | a defect report that contradicts the text or the verdict |
| `L1_CANDIDATE_ID_DUPLICATE` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_CANDIDATE_LIMIT_EXCEEDED` | L1 | volume beyond the contract would silently drop claims; rejected |
| `L1_CANDIDATE_REF_UNKNOWN` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_CANDIDATE_UNRESOLVED` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_COVERAGE_EMPTY` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_COVERAGE_GAP` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_COVERAGE_OVERLAP` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_COVERAGE_RANGE_LIMIT_EXCEEDED` | L1 | volume beyond the contract would silently drop claims; rejected |
| `L1_COVERAGE_REVERSED` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_COVERAGE_UNKNOWN_ID` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_DISPOSITION_INVALID` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_DRAFT_ANCHOR_KIND_INVALID` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_DRAFT_QUOTE_AMBIGUOUS` | L1 | a quote that is not in the source (or is ambiguous) is a claim the app can disprove |
| `L1_DRAFT_QUOTE_FORBIDDEN_FOR_MISSING` | L1 | a quote that is not in the source (or is ambiguous) is a claim the app can disprove |
| `L1_DRAFT_QUOTE_NOT_IN_ANCHOR` | L1 | a quote that is not in the source (or is ambiguous) is a claim the app can disprove |
| `L1_DRAFT_QUOTE_REQUIRED` | L1 | a quote that is not in the source (or is ambiguous) is a claim the app can disprove |
| `L1_ENUM_INVALID` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_ERROR_ID_DUPLICATE` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_FINDING_LIMIT_EXCEEDED` | L1 | volume beyond the contract would silently drop claims; rejected |
| `L1_FINDING_RAW_ANCHOR_REQUIRED` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_FINDING_REF_UNKNOWN` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_ID_INVALID` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_PRESERVED_NEEDS_EVIDENCE_LIMIT` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_PROTECTED_LIMIT_EXCEEDED` | L1 | volume beyond the contract would silently drop claims; rejected |
| `L1_PROTECTED_OVERLAPS_OPEN_FINDING` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_RAW_QUOTE_AMBIGUOUS` | L1/L3 | the RAW quote occurs in several units and no declared unit or nearby hint decides |
| `L1_RAW_QUOTE_NOT_IN_ANCHOR` | L1 | a quote that is not in the source (or is ambiguous) is a claim the app can disprove |
| `L1_REPORT_NOT_LEDGER_V2` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L1_RESOLUTION_ID_INVALID` | L1 | identity, reference, coverage or disposition claim that contradicts the inventory or itself |
| `L2_CHANGE_ERROR_ID_UNKNOWN` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_FINDING_CHANGE_ERROR_MISMATCH` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_CHANGE_NOT_CLOSED` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_CHANGE_NOT_LISTED` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_CHANGE_UNKNOWN` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_FIXED_WITHOUT_CHANGE` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_FIX_OFF_ANCHOR` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_NOT_RESOLVED` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_OCCURRENCE_MISSING` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_OCCURRENCE_UNBACKED` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_OCCURRENCE_UNKNOWN` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_PRESERVED_BUT_CHANGED` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_PRESERVED_WITHOUT_ROW` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_PRESERVE_OFF_ANCHOR` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_PRESERVE_UNKNOWN` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_REJECTED_BUT_CHANGED` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_REJECTED_WITHOUT_RAW_EVIDENCE` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_REJECTED_WITHOUT_REASON` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_FINDING_UNRESOLVED_WITHOUT_REASON` | L2 | what became of an L1 finding must be backed by an applied change, a preserve row or raw evidence |
| `L2_WIRE_CANDIDATE_ID_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_DISPOSITION_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_FINDING_ID_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_FINDING_STATUS_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_ID_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_LEDGER_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_LINE_OUT_OF_RANGE` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_OP_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_RAW_UNITS_MISSING` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_RESOLUTION_ID_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_ROW_LIMIT_EXCEEDED` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L2_WIRE_STATUS_INVALID` | L2 | identity, reference, ledger, status or line claim that contradicts the inventory or the text |
| `L3_CANDIDATE_ID_DUPLICATE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CANDIDATE_LIMIT_EXCEEDED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_CHANGE_UNKNOWN` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_FIXED_WITHOUT_CHANGE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_FIX_OFF_ANCHOR` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_INDEX_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_NOT_RESOLVED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_PRESERVED_BUT_CHANGED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_PRESERVED_WITHOUT_ROW` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_PRESERVE_OFF_ANCHOR` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_PRESERVE_UNKNOWN` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_REJECTED_BUT_CHANGED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_REJECTED_WITHOUT_EVIDENCE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_CARRIED_UNRESOLVED_WITHOUT_REASON` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBES_BREADTH_LOW` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBES_COVERAGE_TOO_FEW` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBES_REGRESSION_TOO_FEW` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_ACTION_CHANGE_UNKNOWN` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_ACTION_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_ACTION_OFF_ANCHOR` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_ACTION_PRESERVE_UNKNOWN` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_ANCHOR_DUPLICATE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_ID_DUPLICATE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_LIMIT_EXCEEDED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_RAW_ANCHOR_REQUIRED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_RAW_QUOTE_AMBIGUOUS` | L1/L3 | the RAW quote occurs in several units and no declared unit or nearby hint decides |
| `L3_PROBE_RAW_QUOTE_NOT_IN_ANCHOR` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_VI_QUOTE_AMBIGUOUS` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_PROBE_VI_QUOTE_NOT_IN_ANCHOR` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_RESOLUTION_ID_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_VI_LINE_OUT_OF_RANGE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_CANDIDATE_ID_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_LEDGER_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_LINE_OUT_OF_RANGE` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_PROBES_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_PROBE_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_RAW_UNITS_MISSING` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_RESOLUTION_ID_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_ROW_LIMIT_EXCEEDED` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `L3_WIRE_STATUS_INVALID` | L3 | probe, carried-defect and re-audit claims that contradict the texts, the applied changes or the minimum evidence |
| `PRESERVE_ANCHOR_MISMATCH` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `PRESERVE_EVIDENCE_LIMIT_MISSING` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `PRESERVE_ID_DUPLICATE` | L2/L3 | row identity or required row fields |
| `PRESERVE_ID_INVALID` | L2/L3 | row identity or required row fields |
| `PRESERVE_LINE_CHANGED` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `PRESERVE_LINE_OUT_OF_RANGE` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `PROTECTED_SPAN_TOUCHED` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |
| `SPEAKER_PROOF_MISSING` | L2/L3 | a change that does not match the base text, a protected line or the dialogue rules: handled by the existing revert/repair mechanism |

## PROTOCOL (30)

| Code | Phase | Action / reason |
|---|---|---|
| `FINAL_READ_ATTEMPT_ECHO_MISMATCH` | final-read | envelope, echo and JSON types |
| `FINAL_READ_INT_INVALID` | final-read | envelope, echo and JSON types |
| `FINAL_READ_OBJECT_EXPECTED` | final-read | envelope, echo and JSON types |
| `FINAL_READ_TARGET_NOT_UTF8` | final-read | envelope, echo and JSON types |
| `FINAL_READ_TEXT_INVALID` | final-read | envelope, echo and JSON types |
| `FINAL_READ_WIRE_BYTE_LIMIT_EXCEEDED` | final-read | envelope, echo and JSON types |
| `FINAL_READ_WIRE_SCHEMA_INVALID` | final-read | envelope, echo and JSON types |
| `L1_INT_INVALID` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_OBJECT_EXPECTED` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_TEXT_INVALID` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_TEXT_PATTERN_INVALID` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_WIRE_ATTEMPT_ECHO_MISMATCH` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_WIRE_BYTE_LIMIT_EXCEEDED` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_WIRE_PARSE_FAILED` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L1_WIRE_SCHEMA_INVALID` | L1 | envelope, echo, JSON type and pattern: the strict response schema constrains them at generation time; kept as a refusal |
| `L2_WIRE_ARRAY_INVALID` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_ATTEMPT_ECHO_MISMATCH` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_BASE_TEXT_INVALID` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_BASE_TEXT_MISSING` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_BOOLEAN_INVALID` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_BYTE_LIMIT_EXCEEDED` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_LINE_INVALID` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_OBJECT_EXPECTED` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_PARSE_FAILED` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_SCHEMA_INVALID` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L2_WIRE_TEXT_INVALID` | L2 | envelope, echo and JSON types; kept as a refusal |
| `L3_ID_INVALID` | L3 | envelope, echo and identifier syntax; kept as a refusal |
| `L3_WIRE_ATTEMPT_ECHO_MISMATCH` | L3 | envelope, echo and identifier syntax; kept as a refusal |
| `L3_WIRE_BYTE_LIMIT_EXCEEDED` | L3 | envelope, echo and identifier syntax; kept as a refusal |
| `L3_WIRE_SCHEMA_INVALID` | L3 | envelope, echo and identifier syntax; kept as a refusal |

## APP (169)

| Code | Phase | Action / reason |
|---|---|---|
| `BASE_TEXT_MISSING` | L2/L3 | app-supplied base text and predecessor identity |
| `BASE_TEXT_NOT_UTF8` | L2/L3 | app-supplied base text and predecessor identity |
| `CHUNK_PARAMETERS_INVALID` | L1 | input or chain state supplied by the app, not by the model |
| `CONTENT_L2_CANDIDATE_CONFLICT` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `CONTENT_L2_FINDING_UNRESOLVED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `CONTENT_L3_CARRIED_DEFECT_UNRESOLVED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `CONTENT_L3_FINAL_READ_DEFECTS` | L3 | stop reasons, input, chain, store, provider and budget states |
| `CONTENT_L3_PROVEN_CONFLICT_UNRESOLVED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `DIFF_CHANGED_SPAN_MISMATCH` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `DIFF_DECLARED_COUNT_MISMATCH` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `DIFF_ERROR_MAPPING_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `DIFF_INPUT_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `EDITORIAL_FIELD_SPEC_MISSING` | L1 | internal specification lookup; cannot be caused by the model |
| `INPUT_CHANGE_MAP_L2_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_CHANGE_MAP_L2_MISMATCH` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_DRAFT_MISSING` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_DRAFT_REQUIRED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_L2_BASE_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_L2_PROJECTION_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_L2_REQUEST_MISSING` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_L3_LEDGER_CHAIN_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_L3_PROJECTION_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_L3_REQUEST_MISSING` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_RAW_HAS_NO_UNITS` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_RAW_LEDGER_PHASE_INVALID` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_RAW_LEDGER_REPORT_INVALID` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_RAW_LEDGER_REPORT_MISSING` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_RAW_LEDGER_STALE` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_RAW_MISSING` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_RAW_NOT_UTF8` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_REPORT_L1_CONTRACT_MISMATCH` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_DISPOSITION_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_IDENTITY_MISMATCH` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_LEDGER_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_LEDGER_STALE` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_LEGACY_CONTRACT` | L1 | input or chain state supplied by the app, not by the model |
| `INPUT_REPORT_L1_PHASE_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_REQUIRED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_REPORT_L1_SOURCE_DRIFT` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `INPUT_VI_L2_CHAIN_MISMATCH` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_VI_L2_INTEGRITY_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `INPUT_VI_L2_REQUIRED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `L1_UNIT_LIMIT_EXCEEDED` | L1 | input or chain state supplied by the app, not by the model |
| `L2_BUDGET_REQUIRED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `L2_FINAL_READ_BUDGET_REQUIRED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `L2_INPUT_BUDGET_EXCEEDED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `L2_PROVIDER_OR_STORE_NOT_CONFIGURED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `L2_TOKEN_OR_COST_BUDGET_EXCEEDED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `L3_BUDGET_REQUIRED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `L3_FINAL_READ_BUDGET_REQUIRED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `L3_INPUT_BUDGET_EXCEEDED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `L3_PROVIDER_OR_STORE_NOT_CONFIGURED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `L3_TOKEN_OR_COST_BUDGET_EXCEEDED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `LEDGER_DISPOSITION_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_DUPLICATE_ITEM` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_ENTRY_BLANK` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_EVIDENCE_REF_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_MODEL_PASS_NOT_A_DECISION` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_POPULATION_DUPLICATE_OR_BLANK` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_PROCESSED_EVIDENCE_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_REQUEST_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_UNACCOUNTED_ITEM` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `LEDGER_UNKNOWN_ITEM` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `PREDECESSOR_IDENTITY_INVALID` | L2/L3 | app-supplied base text and predecessor identity |
| `QA_EVIDENCE_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `QA_INPUT_IDENTITY_MISMATCH` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `QA_INPUT_IDENTITY_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `QA_PASS_ID_DUPLICATE` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `QA_PASS_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `QA_REQUIRES_TWO_ADVERSARIAL_PASSES` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_DECLARED_CHANGES_FORBIDDEN` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_ATTEMPT_IDENTITY_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_BLOCKING_GATE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_BYTE_LIMIT_EXCEEDED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DISPOSITION_EVIDENCE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DISPOSITION_EVIDENCE_LIMIT_EXCEEDED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DISPOSITION_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DISPOSITION_PHASE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DISPOSITION_TEXT_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DUPLICATE_EVIDENCE_REF` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DUPLICATE_FINDING_ID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_DUPLICATE_PRESERVED_ITEM` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_ENTRY_EVIDENCE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_ENTRY_EVIDENCE_LIMIT_EXCEEDED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_EVIDENCE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_EVIDENCE_LIMIT_EXCEEDED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_EVIDENCE_REF_NOT_DECLARED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_FINDINGS_LIMIT_EXCEEDED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_FINDING_DISPOSITION_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_GATES_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_ITEM_ID_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_LEDGER_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_PRESERVED_ITEM_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_PRESERVED_LIMIT_EXCEEDED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_REQUEST_HASH_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_RESPONSE_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RAW_WIRE_SCHEMA_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_ARTIFACT_TYPE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_BUNDLE_IDENTITY_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_CARRIED_DEFECTS_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_CARRIED_DEFECT_UNRESOLVED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_EVIDENCE_REF_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_HASH_MISMATCH` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_LENGTH_MISMATCH` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_READ_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_READ_NOT_CLEAN` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_READ_PHASE_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_READ_PROBES_UNVERIFIED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_READ_TARGET_MISMATCH` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_FINAL_READ_WITHOUT_OPERATION` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_GATE_DEFINITION_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_GATE_STATUS_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_MALFORMED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_OPERATIONS_INCOMPLETE` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_OPERATION_ORDER_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PREDECESSOR_IDENTITY_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PRESERVED_INVENTORY_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PRESERVE_STOP_OVERLAP` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PROBES_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PROBES_TOO_FEW` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PROBE_ACTION_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PROBE_INCOMPLETE` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_PROBE_UNANCHORED` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_RELEASE_COUNTER_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_RELEASE_NUMBERS_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_RELEASE_NUMBER_NOT_ZERO` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_REVISION_UNKNOWN` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_SCHEMA_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_STABLE_ANCHOR_INVALID` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_STATIC_FINAL_READ_MARKER` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_STOP_PRESENT` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_TYPED_STOP_MISSING` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `RECEIPT_UNPARSABLE` | receipt/legacy | app-built receipt, ledger, diff or legacy P5 wire validation; not produced by the live L1-L3 wire |
| `REPAIR_L2_CANDIDATES_UNPROCESSED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L2_CHANGE_MAP_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L2_DISCOVERY_SCHEMA_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L2_FINAL_READ_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L2_FINDING_RESOLUTION_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L2_OUTPUT_SCHEMA_INVALID` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L2_RESOLUTIONS_INCOMPLETE` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `REPAIR_L3_CARRIED_RESOLUTION_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `REPAIR_L3_FINAL_READ_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `REPAIR_L3_PROBES_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `REPAIR_L3_QA_CHANGE_MAP_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `REPAIR_L3_REAUDIT_SCHEMA_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `REPAIR_L3_RECONCILE_SCHEMA_INVALID` | L3 | stop reasons, input, chain, store, provider and budget states |
| `REPAIR_L3_RELEASE_NUMBERS_NOT_ZERO` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L2_ATOMIC_COMMIT_FAILED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_CALL_STATE_UNKNOWN` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_CLAIM_FAILED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_COMMITTED_RESULT_UNAVAILABLE` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_OUTPUT_TRUNCATED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_PROVIDER_CALL_FAILED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_PROVIDER_COST_UNAVAILABLE` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_PROVIDER_EMPTY_RESPONSE` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L2_READBACK_MISMATCH` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `RETRY_L3_ATOMIC_COMMIT_FAILED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_CALL_STATE_UNKNOWN` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_CLAIM_FAILED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_COMMITTED_RESULT_UNAVAILABLE` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_OUTPUT_TRUNCATED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_PROVIDER_CALL_FAILED` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_PROVIDER_COST_UNAVAILABLE` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_PROVIDER_EMPTY_RESPONSE` | L3 | stop reasons, input, chain, store, provider and budget states |
| `RETRY_L3_READBACK_MISMATCH` | L3 | stop reasons, input, chain, store, provider and budget states |
| `STOP_L2_EXTERNAL_CALL_STATE_UNRESOLVED` | L2 | stop reasons, input, chain, store, provider and budget states: outcomes of the app or infrastructure, not model formatting |
| `STOP_L3_EXTERNAL_CALL_STATE_UNRESOLVED` | L3 | stop reasons, input, chain, store, provider and budget states |

## NOT_A_CODE (61)

| Code | Phase | Action / reason |
|---|---|---|
| `ADDRESS_PROFILE` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `ADVERSARIAL_COVERAGE` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `ADVERSARIAL_REGRESSION` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `APP_RELEASE_NUMBERS` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `ARTIFACT_IDENTITY` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `CHANGE_MAP_L2` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `CONTENT_BLOCKED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `CONTINUITY_STRUCTURE_TECHNICAL` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `COVERAGE_EMPTY` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `COVERAGE_GAP` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `COVERAGE_OVERLAP` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `COVERAGE_REVERSED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `COVERAGE_UNKNOWN` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `COVERAGE_UNKNOWN_ID` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `DEFECT_FOUND` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `FINAL_QA` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `GLOSSARY_ROW` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `IMAGE_MARKER` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `INPUT_REQUIRED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `INSERT_AFTER` | - | change operation enum value |
| `L1_PROOF` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L1_RAW_CANDIDATES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L1_RAW_DISCOVERY` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L1_RAW_LEDGER` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L1_RECONCILE` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_ALREADY_COMMITTED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_COMMITTED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_CONFLICT_CANDIDATES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_EDIT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_FINAL_READ` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_RAW_CANDIDATES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_RAW_DISCOVERY` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_UNPROCESSED_CANDIDATES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_UNRESOLVED_CANDIDATES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L2_UNRESOLVED_FINDING` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_ALREADY_COMMITTED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_CARRIED_DEFECTS` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_FINAL_COMMITTED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_FINAL_READ` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_FINAL_READ_DEFECT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_RAW_FIRST_REAUDIT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_REAUDIT_CANDIDATES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_RECONCILE` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `L3_UNRESOLVED_CARRIED_DEFECT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `MERGE_WITH_NEXT` | - | change operation enum value |
| `NOT_APPLICABLE` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `NOT_EVALUATED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `NO_DEFECT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `PRESERVE_DRAFT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `PRONOUN_ROW` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `QA_CHANGE_MAP` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `QA_RECEIPT` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `RAW_LINE_INVENTORY_V1` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `READ_PROBE_LINES` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `READ_TARGET` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `REPAIR_REQUIRED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `REPORT_L1` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `RETRY_REQUIRED` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `SPEAKER_LISTENER` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `SPEAKER_PROOF` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |
| `VI_L2` | - | enum value, role, phase name, artifact type or internal issue label; not a rejection by itself |

