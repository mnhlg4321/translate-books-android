# Proposal — exact offline-repaired A4.3 parent template

## Readiness

`OFFLINE_PRE_RESERVATION_REPAIR_PASS / HISTORICAL_FIRST_STOP_UNRESOLVED / A4_3_NOT_RUN / P6_NOT_READY`.

The proposal is for review only. It creates no owner approval, receipt,
reservation, event or live authority and does not ask the owner to run a
launcher in this work package.

## Exact candidate source

The future candidate must be derived from the tracked parent template below;
the template itself was not executed as a launcher:

```text
path=scripts/p5e-a43-pre-reservation-launcher.ps1
sha256=9183619AD9C3BA5E5C0AF2701968F1F2121894472A8FD136C0DC6B54898F9A8D
executed=false
```

The old private candidate `569F8437A0630D52E8F26D5631AF2D9CBC089947BFD337E7FEC510B831D98A41`
was not changed, renamed, rerun or reused.

## Evidence

- Final behavioral report: `evidence/p5e-a43-pre-reservation-repair-20260930/P5E_A43_PRE_RESERVATION_REPAIR_QA_20260930_02.json`, SHA-256 `DBBBCAB4E545E8B1779A2E01A2D7A562F7FB6F27DA1B8014E8F7E455525A2565`, `26/26 PASS` on Windows PowerShell 5.1.
- Parent source/test/provenance: `docs/P5E_A43_PRE_RESERVATION_REPAIR_PROVENANCE_20260930.json`.
- Independent review: `docs/P5E_A43_PRE_RESERVATION_REPAIR_REVIEW_20260930.md`, `BLOCKER 0 / HIGH 0 / MEDIUM 0 / LOW 0`.
- Child regression: `27/27 PASS`; inherited static launcher QA rerun `16/16 PASS`; PS5.1 parse and diff check `PASS`.

## Constraints that remain active

No A4.3 PASS, P5 exit, RAW acceptance, After/readback/verifier or P6
readiness is claimed. Historical first cause remains unresolved. Any future
live event requires a new owner decision against the exact candidate hash and
must be evaluated independently from pre-dispatch through After/readback and
verifier. The consumed old receipt/event cannot be reused.

**Next action duy nhất:** owner may review this exact hash for a separate
decision; no live command, key or approval is requested now.
