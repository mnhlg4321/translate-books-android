# Editorial V5-SAFE.4 migration

Status: `FOUNDATION_COMPLETE / EXECUTION_BLOCKED`

This document replaces the executable design in `EDITORIAL_WORKFLOW_V5_PLAN.md` and the continuation plan in `EDITORIAL_HANDOFF_V4_16.md`. Those two files remain only as historical records.

## Immutable source pack

The app bundles the exact three-file source pack from:

`D:\Ebooks\1. Prompt cac the loai\BIÊN TẬP\BO_QUY_TRINH_BIEN_TAP_V5_SAFE_4`

| File | SHA-256 |
|---|---|
| `PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4.txt` | `C57100C45F16FC5A27E56AE17ABE919BE89DA55082A66D060DB504746ED8B717` |
| `PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4.txt` | `0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81` |
| `WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4.txt` | `7A434ADE77239DB33AF5A677456D0310AC11DC8391565FC4888236536FF96B5E` |

`verifyEditorialSafe4Pack` runs before every app build and fails if a file is absent or one byte differs. New projects persist `V5-SAFE.4` plus the derived pack hash; a different identity is rejected.

## Removed completely

The following old implementation was unsafe to reuse under SAFE4 and has been deleted:

- `EditorialWorkflowV5` and `editorial-v5-evidence-1` schema/validator;
- all old L1/L2/L3 runners;
- all old L1/L2/L3 context builders and model-output contracts;
- the code paths that converted model text such as `CLOSED` directly into persisted closed gates;
- old run/evidence mutation and latest-evidence lookup APIs that could combine artifacts from different chains;
- the old release ZIP builder, release-folder UI, release recording path and release activity results;
- Run/Retry/Report/VI_L2/FINAL/Release buttons wired to the old engine;
- unit/instrumentation fixtures whose expected behavior proved the retired contracts.

The old APKs remain immutable historical artifacts. They are not SAFE4 QA evidence and must not be used to release SAFE4 output.

## Preserved and adapted

The migration deliberately preserves useful non-executable infrastructure:

- SQLite tables and existing project/chapter/run/evidence rows, so user history is not deleted;
- legacy projects and chapters as read-only history;
- project identity, input selection, chapter-number mapping and immutable input snapshots;
- project-owned Glossary/Pronoun profiles and chapter overrides;
- RAW/DRAFT pairing, content hashing, manifest comparison and project deletion cleanup;
- generic translation functionality outside Editorial.

SAFE4 input rules now require RAW, DRAFT and Glossary. Pronoun is conditional: absent input is stored/displayed as `PRONOUN_STATUS=NONE`; a present file is only a candidate for `AVAILABLE` until the future SAFE4 validator accepts it. `PAIR_CONTEXT` is reserved as a separate optional scoped input.

## Fail-closed boundary

`EditorialSafe4Pack` lists every capability required to execute. Only byte-level pack integrity is currently implemented. `executionEnabled()` therefore returns `false`; every workflow transition and release check remains closed.

New chapters are saved as `SAFE4_BLOCKED`. Legacy projects cannot receive chapters or reference mutations and are rendered read-only. No UI action can call a model, retry old checkpoints, export old reports, or create an Editorial release bundle.

The remaining capabilities are:

1. exact manifest/run/parent lineage;
2. exhaustive ledger schema with machine-countable populations and anchors;
3. gates derived from stored evidence rather than model labels;
4. Pronoun `AVAILABLE/NONE/LEGACY_REJECTED` validation and scoped Pair Context;
5. L1 RAW-first discovery/reconciliation barrier;
6. L2 change coverage and protected-span regression proof;
7. L3 blind QA plus separate adversarial coverage and regression passes;
8. SAFE4 `FINAL`, `QA_RECEIPT` and optional `PAIR_DELTA_QA` release contract;
9. Golden Replay G1–G10 with retained evidence.

No capability may be marked implemented without a test/evidence artifact. The engine can open only when all capabilities are present.

## Future three-file workflow versions

A later three-file Editorial pack does **not** require rebuilding the whole app from zero. The reusable boundary is now explicit:

- add the new immutable pack and hashes;
- add a versioned contract/adapter and additive database migration;
- compare its required inputs, ledgers, gates, context barriers and release artifacts with SAFE4;
- reuse unchanged project/import/snapshot/storage/UI components;
- rerun the new version's Golden Replay suite before enabling execution.

If only wording changes while the machine contract is identical, this is primarily a new pack adapter and replay certification. If the schema, phase visibility, gate derivation or release contract changes, those versioned engine modules must change too. Historical projects remain bound to their original pack and are never silently upgraded.

## Next implementation order

1. Add additive lineage/schema migration without modifying legacy rows.
2. Implement typed exhaustive ledgers and evidence-derived gate calculators.
3. Implement conditional Pronoun/Pair validation and phase allow-lists.
4. Implement checkpointed L1, L2 and L3 SAFE4 runners against exact manifests.
5. Implement SAFE4 receipt/release artifacts.
6. Run and retain Golden Replay G1–G10.
7. Only then enable model execution, archive-first build, device QA and release gates.
