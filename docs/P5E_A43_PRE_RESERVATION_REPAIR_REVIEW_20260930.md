# P5E A4.3 pre-reservation repair — independent offline review

Ngày: `2026-09-30`
Branch: `feature/v4.18-p5e-runner-repair-20260917`
Review boundary: tracked parent template + Windows PowerShell 5.1 behavioral fixture; no candidate/live execution.

## Kết luận

`OFFLINE_ACCEPTANCE_PASS / BLOCKER 0 / HIGH 0 / MEDIUM 0 / LOW 0`.

Gói này đạt acceptance offline của parent/pre-reservation boundary. Đây không
phải A4.3 PASS, không phải P5 exit và không mở P6. Historical first stop của
attempt ngày 2026-09-30 vẫn `UNRESOLVED`; không có phép suy diễn ngược từ QA
synthetic sang nguyên nhân lịch sử.

## Source và provenance

- Parent template: `scripts/p5e-a43-pre-reservation-launcher.ps1`, SHA-256
  `9183619AD9C3BA5E5C0AF2701968F1F2121894472A8FD136C0DC6B54898F9A8D`.
- Behavioral QA: `scripts/test-p5e-a43-pre-reservation-launcher.ps1`, SHA-256
  `2F320FBB617249A8E7202B71D68C366C23ECF9C15289C7AAEDCD6D2749668131`.
- Final report: `evidence/p5e-a43-pre-reservation-repair-20260930/P5E_A43_PRE_RESERVATION_REPAIR_QA_20260930_02.json`, SHA-256
  `DBBBCAB4E545E8B1779A2E01A2D7A562F7FB6F27DA1B8014E8F7E455525A2565`.
- Resolver remained unchanged: `scripts/p5e-raw-toolchain.ps1`, SHA-256
  `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`.

## Exact source checks

1. The test dot-sources the same tracked parent function; it does not copy the
   path/exception algorithm into a separate test implementation.
2. `sdkPath` is checked with `Container`; `adbPath`, `javaPath` and
   `apksignerJarPath` use separate `Leaf` checks. Canonical, no-reparse and
   ancestor checks remain enabled.
3. Typed errors are selected by exact allowlist membership. Unknown exceptions
   retain only safe exception class, stage and `UNKNOWN_STOP`; raw message,
   stack, argv and secret are not placed in the result or diagnostic.
4. The reservation path is derived from the contract-provided reservation root
   and launcher hash. No historical 20260929 root is embedded in the source.
5. The parent writes a bounded diagnostic artifact before any owner root is
   required. Diagnostic/cleanup failures are secondary and cannot replace the
   original typed stop; absent diagnostic evidence is `EVIDENCE_INCOMPLETE`.

## Behavioral evidence

The final PS5.1 report is `26/26 PASS`, including:

- SDK directory/file kind, executable-directory, missing, target-reparse,
  ancestor-reparse and path-with-spaces fixtures;
- exact approval, wrong, empty, EOF and cancel inputs, with no key prompt or
  reservation on rejected input;
- hash mismatch, CreateNew collision, owner-root, receipt, expiry, key and
  unknown-exception stops;
- cleanup masking, diagnostic-write failure and missing-diagnostic handling;
- synthetic parent success, child-start failure and child nonzero exit;
- an actual synthetic PowerShell process exiting nonzero, rather than a string
  presence assertion.

The independent child contract regression was rerun at `27/27 PASS`; the
existing static launcher QA remained `16/16 PASS`. PS5.1 parse and
`git diff --check` both pass. All live boundaries are `NOT_EVALUATED`; no ADB,
provider, credential, database or RAW action occurred.

## Disposition

The new tracked-template hash may be shown to the owner for a separate future
decision. The old candidate hash, marker, receipt and event remain immutable
and non-reusable. No owner approval or key entry is requested in this package.

**Next action duy nhất:** owner review exact tracked-template hash if a future
live decision is desired; otherwise keep A4.3/P5/P6 closed.
