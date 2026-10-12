# P5E signature-layout adapter review — 2026-09-24

Status: `LOCAL_SIGNATURE_LAYOUT_ADAPTER_PASS / OWNER_EVENT_DECISION_PENDING`.

This review covers only the offline parser and orchestration change after the
closed `PRODUCTION_PACKAGE / UNSUPPORTED_LAYOUT` event. It does not convert the
closed event into a retry and does not claim that the synthetic grammar is the
actual device layout.

## Findings and disposition

| Review question | Evidence | Disposition |
| --- | --- | --- |
| Is the current/past distinction source-grounded? | AOSP `PackageSignatures.toString` at immutable ref `8551ec363dcd7c2d7c82c45e89db4922156766ab`; provenance in `P5E_LAYOUT_SIGNATURE_ADAPTER_PROVENANCE_20260924.json`. | PASS |
| Can wrapper identity or scheme become a signer accidentally? | Adapter records wrapper/scheme counts separately and only accepts one current list candidate. | PASS |
| Can a past signer substitute for a missing current signer? | `aosp_current_missing_past_present` stops with `SIGNATURE_MISSING`. | PASS |
| Does the positive fixture prove a real device layout? | No. `source_derived_positive_red_to_green` is explicitly synthetic and only proves old-parser RED/new-parser GREEN. | PASS with scope limit |
| Are malformed, duplicate and conflicting forms fail-closed? | `aosp-current-multiple`, `aosp-duplicate-wrapper`, `aosp-conflicting-format`, `aosp-truncated`, `aosp-nonhex` and `aosp-past-malformed` all stop in the 189-assertion suite. | PASS |
| Are full package identity gates still present? | The command requires the v3 preflight receipt, both layout families, short-token pins, pulled APK SHA-256 and certificate pins. | PASS |
| Can metadata leak into receipts? | Positive receipt scan rejects raw AOSP fields; receipts retain enum/bool/count only. | PASS |
| Does orchestration use the new parser path? | Command QA reads the nested v3 preflight receipt and requires `AOSP_PACKAGE_SIGNATURES_WRAPPER` for both synthetic packages before the one fake runner. | PASS |
| Was live work performed? | Both latest QA records state `syntheticOnly=true`, `adbInvocations=0`; no device/provider/DB/RAW/build/install action occurred. | PASS |

## Open boundaries

- The device layout remains `UNKNOWN_NO_RAW_CAPTURE`; no ADB diagnostic was
  added to resolve it.
- `MATCH` would remain only a current route/digest equality result. It would
  not prove provider account validity or open A4.3, RAW, P5 or P6.
- The old event directory remains closed. A future event requires a separate
  owner decision and a new, explicit evidence directory.

The implementation is closed locally because the source-derived positive
fixture is RED on the baseline parser and GREEN on the new parser, the negative
matrix is covered, and both affected synthetic chains pass. No independent
runtime/device compatibility claim is made.
