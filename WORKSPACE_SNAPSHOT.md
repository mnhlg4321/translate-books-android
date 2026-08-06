# Workspace Snapshot

## G2-C1B1-D0 promotion audit review stop

- Snapshot updated: `2026-08-06` (D0 eligibility audit blocked; dependent work stopped).
- Current version: released product remains `4.15`/code62; latest development archive remains `4.16-dev.48`/code110, APK SHA-256 `B7E07C945602CF65572702DDF19579961CBA0070024DB9C5FFF89585C825F2C3`; SQLite v17.
- Current branch: `feature/v4.16-g2-c1b1d0-d1-c1b2a`; protected `.idea/compiler.xml`, `.idea/gradle.xml`, and `.idea/misc.xml` remain user-owned, unstaged and uncommitted.
- Current commit: `862408a69cdc84debef6a27bad807a061a5bcc7c` is the verified implementation baseline immediately before the D0 documentation/snapshot commit.
- Completed: D0 audit and permitted production-lifecycle dependency plan. Core/persistence/orchestration evidence passes; fixed fixture manifest hash is retained; production positive closure-event/context source is absent, so promotion is blocked. Read-only JVM regression is 269/269 with 0 failures/errors/skips; JDK preflight and diff-check pass.
- Pending: review stop for `EDITORIAL_PACK_PLATFORM_G2_C1B1D0_LINEAGE_PROMOTION_AUDIT.md` and `EDITORIAL_PACK_PLATFORM_G2_C1B1C2B2A_PRODUCTION_LIFECYCLE_DEPENDENCY_PLAN.md`. D1 and C1B2-A are not started.
- Known bugs/blockers: no production `RUN_CONTEXT_CLOSED` owner/event, no production positive context, current profile non-executable and only declares `pack.integrity.sha256.v1`; `executionEnabled()` remains false. No migration/caller/profile/catalog/runtime change was made.
- Regression status: D0 read-only checks PASS; promotion eligibility BLOCKED; R2 connected evidence remains PASS and B1 remains PASS within its isolated boundary.
- Next step: user review of the D0 blocker and lifecycle dependency plan; no lifecycle/schema v18/caller implementation, D1 promotion, C1B2-A, certification, activation or execution.

- Snapshot updated: `2026-08-06` (G2-R2 global regression and B1 requalification review stop)
- Current version: released product remains `4.15`/code62; latest development archive is `4.16-dev.48`/code110, APK SHA-256 `B7E07C945602CF65572702DDF19579961CBA0070024DB9C5FFF89585C825F2C3`; SQLite v17.
- Current branch: `feature/v4.16-g2-c1b1c2b1-regression`; protected `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` remain user-owned, unstaged and uncommitted.
- Current commit: `8e0893438e51da2abe28dde447ae6450bb891901` is the implementation baseline immediately before this R2 review-stop documentation commit.
- Completed: R0 root-cause audit PASS; R1 importer-only fix PASS; R2 archive-first code110 build and two complete UTP connected runs PASS. XML: `82` tests, `0` failures, `0` errors, `1` approved skip. Post-commit JVM `269/269`, Android-test compilation, lint and diff-check pass; final focused B1 `7/7` PASS.
- Regression status: `G2-C1B1C2B1_COORDINATOR: PASS`. The B1 coordinator boundary remains isolated: no production closure event/caller, schema/profile/catalog, production lineage/binding row, capability promotion, certification, activation or execution. `executionEnabled()` remains false.
- Pending: review/approval only. The next implementation phase is not started; `REAL_DATA_CONTINUITY: NOT_CLAIMED`.

- Snapshot updated: `2026-08-06` (G2-R1 minimal importer regression fix; R1 PASS, before post-commit R2 archive/global gate)
- Current version: released product remains `4.15`/code62; latest R1 archive is `4.16-dev.47`/code109, APK SHA-256 `A16F1D4B9D2DAFCF16E9FC038878BD4C58D8D3F46A2666BAF4C5091E4C0D0567`; SQLite v17.
- Current branch: `feature/v4.16-g2-c1b1c2b1-regression`; protected `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` remain user-owned, unstaged and uncommitted.
- Current commit: `93ac1f46f0fd637239287968fb821f091929c598` is the implementation baseline immediately before the R1 implementation/evidence commit.
- Completed: proven ZIP traversal/truncation importer defects fixed with early path classification and bounded ZIP structure metadata; focused code109 device import `13/13`, management `3/3`, B1 `7/7`; engine `105/105`, app JVM `164/164`, Android-test compilation, lint and diff-check pass; artifact/backup parity pass.
- Regression status: R1 PASS; global connected status is not yet requalified. No B1 coordinator, production caller, schema/profile/catalog, production row, capability, certification, activation or execution change.
- Pending: commit R1 group, then fresh archive-first R2 build and full connected suite. `REAL_DATA_CONTINUITY: NOT_CLAIMED`; execution remains disabled.

- Snapshot updated: `2026-08-06` (G2-R0 connected-suite root-cause audit; R0 PASS, before importer-only R1 fix)
- Current version: released product remains `4.15`/code62; latest development archive remains `4.16-dev.45`/code107 with APK SHA-256 `F0A8971AA9E8A4D08CF08A63857BC37F17BD6DEFE264CE4F2813DAAF8A1211A9`; SQLite source version v17.
- Current branch: `feature/v4.16-g2-c1b1c2b1-regression`; `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` are user-owned, unchanged by this work, unstaged and uncommitted.
- Current commit: `0834ec8f1b5ce8557ea0adfe6a1f45005d13e591` is the implementation baseline immediately before the R0 documentation commit.
- Completed: R0 reproduced the two ZIP mismatches and separated the historical management empty-body report. Traversal and truncation are importer defects; management abort is `RUNNER_DEVICE_ENVIRONMENT`. Management method/class/group and two full direct runs completed without the historical abort; each full run completed all 80 tests with only the two ZIP failures and one approved real-API skip.
- Regression status: B1 focused device evidence remains 7/7 PASS; global suite remains blocked only by the two proven ZIP classification defects until R1/R2. No code or test expectation was changed in R0.
- Pending: commit R0 evidence; then R1 importer-only fix and focused matrix. No B1 caller wiring, schema/profile/lifecycle/capability/execution work is authorized.

- Snapshot updated: `2026-08-05` (G2-C1B1-C1 pure-JVM lineage runtime context/service seam review stop; no caller wiring, persistence, migration, promotion, certification, activation, binding or execution)
- Current version: `4.15`/code62 remains the released product; latest development archive is `4.16-dev.39`/code101. SQLite source version is v16 and Editorial execution remains intentionally blocked.
- Current branch: `feature/v4.16-g2-c1b1c15a`; `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` remain user-owned and unstaged. The branch was created from the verified C1 HEAD `63d77ac267671f88ee2c9ebc31ebc8e575ec74cd` after baseline verification.
- Current commit: `f8d0662` (C1.5-A handoff/checklist documentation commit immediately before this snapshot correction; intentionally not self-referential)
- Current build: `artifacts/builds/v4.16-dev.39/build-20260805-124714/TranslateBooks-v4.16-dev.39-code101.apk`, SHA-256 `BF5C7EB41A26242C171B04977182A0A1378BF1F8269B34E77A98D0CC13E61F30`; matching five-file payload is under `backup/builds/v4.16-dev.39/build-20260805-124714/`. Source ZIP SHA-256 is `7EE2512AA71E39E4C12BD17697C981B6B11A12F7C4B34EDF8C95C47C1C762E8C`. Final verification passed engine `74/74`, app JVM `163/163`, Android-test Java compilation, lint `0` errors/`53` warnings, `git diff --check`, and direct device instrumentation `20/20`; the released v4.15/code62 archive remains unchanged.

## G2-C1B1-A exact-parent lineage handoff

- Completed: pure-JVM immutable lineage model, canonical JSON projections, domain-separated SHA-256 fingerprints, exact-parent validator, stable machine failure codes, fixed `lineage/root-child-v1` fixture manifest and 14 focused tests for `lineage.exact-parent.v1`.
- Implementation: branch `feature/v4.16-g2-c1b1b`; C1A correction commit `3fcf039be0185b22567e75f34a970bf75ba5ea6a`; B1B implementation commits `c0539b9`, `e65b723`, `1d6fbae`. Handoff document is `EDITORIAL_PACK_PLATFORM_G2_C1B1B_LINEAGE_SQLITE_V16.md`; phase checklist is `release_checklists/v4.16-g2-c1b1b.md`.
- Actual test results: `:editorial-engine:test` `74/74`, `:app:testDebugUnitTest` `163/163`, Android-test Java compilation PASS, `:app:lintDebug` PASS with `0` errors/`53` warnings, `git diff --check` PASS. Direct device runner passed lineage `11/11` and compatibility migration `9/9`; isolated migration/append QA passed. No real production database existed on the device at final inspection, so real-data continuity is not claimed.
- Protected identity: canonical profile hash `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`, machine fingerprint `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`; catalog still confirms only `pack.integrity.sha256.v1`; `EditorialSafe4Pack.executionEnabled()` remains false.
- Unchanged boundaries: no production evidence-catalog promotion, profile update/version, importer/runtime call site, compatibility-history reevaluation, certification, Golden Replay, activation, project binding or execution. SQLite v16 lineage persistence exists only as an unused append/read boundary.
- Completed: G2-C1B1-B persistence review stop is accepted as the v16 append/read boundary; the current C0 review explicitly keeps importer/runtime wiring separate.

## G2-C1B1-C0 lineage creation boundary handoff

- Completed: read-only audit of pack ZIP import, project/chapter preparation, run/evaluation creation, v15 compatibility evidence, v16 lineage storage and current repository identity sources. The authoritative lineage event is future `RUN_CONTEXT_CLOSED` with a frozen manifest and stable run identity; ZIP import and project/chapter preparation do not create lineage.
- Handoff: `EDITORIAL_PACK_PLATFORM_G2_C1B1C0_LINEAGE_CREATION_BOUNDARY.md`; checklist: `release_checklists/v4.16-g2-c1b1c0.md`. Exact identity gaps are stable project identity, input-scope identity, production run creator and explicit parent/checkpoint selection; no placeholder, pack ID, import ID, SQLite row ID or random UUID was introduced.
- Verification: read-only engine `74/74`, app JVM `163/163`, combined `237/237`, failures/errors/skips `0/0/0`; `git diff --check` PASS. No APK was built or installed; latest artifact remains `4.16-dev.39`/code101.
- Status: `G2-C1B1C0_BOUNDARY_PLAN: PASS`; `LINEAGE_RUNTIME_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`. The protected profile/catalog, SQLite v16, pack/evaluation history and QA blocked boundary are unchanged.
- Completed: C1 was separately approved and implemented as a pure-JVM preparation seam; importer/runtime caller wiring and persistence remain intentionally absent.

## G2-C1B1-C1 explicit lineage runtime service seam handoff

- Completed: immutable caller selection/request/assertion contract, authoritative context/resolver port, compatibility/run-state values, canonical boundary result mapping and `EditorialLineageRuntimeService.validateAndPrepare`. The service enforces `RUN_CONTEXT_CLOSED`, prepares explicit ROOT/CHILD records and delegates record checks to the existing validator without persistence.
- Implementation: `1c5ba96` on `feature/v4.16-g2-c1b1c1`; handoff `EDITORIAL_PACK_PLATFORM_G2_C1B1C1_LINEAGE_SERVICE_SEAM.md`; checklist `release_checklists/v4.16-g2-c1b1c1.md`.
- Verification: focused pure-JVM C1 suite `21/21`; full engine `95/95`; app JVM `163/163`; combined `258/258`; failures/errors/skips `0/0/0`; Android-test Java compilation PASS; lint `0` errors/`53` warnings; `git diff --check` PASS. No APK was built or installed; latest artifact remains code101.
- Decision: `G2-C1B1C1_SERVICE_SEAM: PASS`; `AUTHORITATIVE_IDENTITY_SCHEMA: REQUIRED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Blocker: current project/chapter/run AUTOINCREMENT IDs and mutable/run-incomplete schema are not authoritative semantic identities, and no run-to-lineage binding exists. The exact next step is `G2-C1B1-C1.5` additive identity schema review/implementation; do not start C2 caller integration automatically.

## G2-C1B1-C1.5-A authoritative identity schema plan handoff

- Completed: surveyed the v10→v16 migration chain and current project/chapter/asset/reference/run/compatibility/lineage APIs. The plan defines immutable project revision, complete input-scope snapshot, closed run context and append-only run-lineage binding tables for a future additive v17 migration.
- Implementation: `EDITORIAL_PACK_PLATFORM_G2_C1B1C15A_IDENTITY_SCHEMA_PLAN.md`, committed in `f8d0662270436a0a39b7f2c9a48e0ef4655090c7`; checklist `release_checklists/v4.16-g2-c1b1c15a.md`. No source/database/runtime/profile/catalog code changed.
- Decision: `G2-C1B1C15A_SCHEMA_PLAN: PASS`; `AUTHORITATIVE_IDENTITY_SCHEMA: REQUIRED`; `SQLITE_V17_IMPLEMENTATION: NOT_STARTED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Verification: required read-only Wrapper regression passed with engine `95/95` and app JVM `163/163`; failures/errors/skips `0/0/0`; combined `258/258`; `git diff --check` passed. No APK was built or installed; latest artifact remains `4.16-dev.39`/code101.
- Boundary: AUTOINCREMENT IDs, pack/import IDs and v15 compatibility evaluation IDs are not substituted for semantic project/scope/run identity. Existing rows are `IDENTITY_UNATTESTED`; no backfill is allowed. `RUN_CONTEXT_CLOSED` remains the only future lineage creation event.
- Next action: exact next step is `G2-C1B1-C1.5-B` schema/DAO implementation review after the listed project-key, manifest, run-attempt, retention and shared-transaction decisions are approved. Do not start C2 caller integration, importer wiring, promotion, certification, activation, binding or execution.

## G2-C1A executable-contract plan handoff

- Completed: startup/read-only audit, baseline verification, canonical SAFE4 byte/hash verification, exact nine-capability inventory, machine-level executable eligibility contract, state machine, dependency graph, profile immutability/versioning policy, resolver ambiguity policy, new-pack compatibility matrix and separate G2-C1B1–B9 implementation plan in `EDITORIAL_PACK_PLATFORM_G2_C1A_EXECUTABLE_CONTRACT_PLAN.md`.
- Baseline: source `feature/v4.16`/`1876439517057df0dc2cf4744de74dc0cd4d14fa`; review branch `feature/v4.16-g2-c1a`; SQLite v15; latest accepted build remains `4.16-dev.36`/code98. No production source, profile, database, SAFE4 byte, APK or device state changed.
- Trust anchors remain canonical profile hash `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6` and machine fingerprint `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`; only `pack.integrity.sha256.v1` has production evidence; `EditorialSafe4Pack.executionEnabled()` remains false.
- Regression boundary: required Wrapper verification passed `:editorial-engine:test` 60/60 and `:app:testDebugUnitTest` 162/162 with 0 failures/errors/skips; `git diff --check` exit 0. No APK build/install, device QA, certification, Golden Replay, activation, binding, reevaluation, push, merge or tag is permitted.
- Next step: user review of G2-C1A and explicit approval before starting exactly one capability phase, `G2-C1B1` for `lineage.exact-parent.v1`.

## G2-C0B bundled trusted-profile handoff

- Completed in implementation commits `fb2bd262dc996d20fbef051414727525410e9e1a`, `6c6fdb2c3ab15e972716d005b1588cee8b51fdec` and `285819c329c922d1256eb27bc13ee5ba75ec5fd1`: one production profile resource, independent compile-time trust catalog, immutable read-only registry, 17/17 focused tests, source-of-truth-only generated APK asset copy/verification and reviewed evidence fingerprint. Resource path is `editorial/engine-profile/v1/profile.json`; profile ID/version is `com.ml.tblandroidtxt.editorial.engine.bootstrap`/`1.0.0`.
- Trust values: raw resource SHA-256 `deb0e89a4084a88c137c71ba2aa7a7170f84d9979395ef58866c73529ce601eb`, canonical profile hash `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`, machine-contract fingerprint `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`.
- Profile truth: only `pack.integrity.sha256.v1` implemented; nine SAFE4 capabilities remain explicit missing; contract/phase/context/gate/release/adapter fields remain empty and no executable contract is declared.
- Regression after C0B: engine 50/50, app JVM 161/161, instrumentation source compilation PASS, lint 0 errors/53 warnings, `git diff --check` PASS. No APK was built/installed and code92 remains the latest accepted artifact.
- Pending: C0C-B is blocked until an additive immutable evidence schema is separately approved and the production profile receives reviewed executable contract semantics. Runtime/importer wiring remains absent and compatibility remains fail-closed.

## G2-C0C runtime profile resolution handoff

- C0C-A implementation: pure-JVM `EditorialEngineProfileAdapter`, deterministic `EditorialEngineProfileResolver`, immutable `EditorialCompatibilityEvaluationResult` and stable `EditorialCompatibilityReasonCode`; no Android, SQLite, importer, UI or runtime call site was changed.
- C0C-A focused tests: 6/6 pass, 0 failures/errors/skips. Coverage includes adapter evaluator-fact mapping without profile metadata, production no-executable-contract fail-closed selection, profile hash mismatch, machine fingerprint mismatch, duplicate trusted identity and immutable/deterministic result collections.
- C0C-B blocker: live SQLite v14 stores pack hash, engine version, machine fingerprint, outcome/required class, reason and timestamp, but not trusted profile ID/version/hash, adapter-set hash, capability fingerprint or evaluation-context identity. The approved plan forbids using `blocked_reason` as a hidden channel and requires an additive immutable migration before durable profile evidence; no migration was made.
- C0C-B semantic blocker: the bundled production profile has null contract bounds, empty schema/phase/context descriptors and therefore no approved executable contract. Creating an integrity-only `DATA_COMPATIBLE` fixture result would require inventing semantic profile values, which is forbidden.
- Regression: Wrapper Gradle 9.3.0/JBR 21; engine 56/56, app JVM 161/161, instrumentation Java compilation PASS, lint 0 errors/53 warnings, `git diff --check` PASS; JDK preflight 4/4. No APK build/install; code92 remains the latest accepted artifact.

## G2-C0C-B1 SQLite v15 persistence review stop

- Completed: additive SQLite v14→v15 migration plus immutable `editorial_pack_compatibility_evaluations` history table, pure semantic context fingerprint, and narrow append/read DAO. No C0C-B2 resolver/importer wiring, production DATA_COMPATIBLE result, profile change, importer/SAF change, certification, activation, binding or execution path was added.
- Schema contract: v14 `editorial_packs`, `editorial_pack_files`, `editorial_pack_imports` and `editorial_pack_compatibility_results` remain unchanged. v15 adds only the new history table, two indexes and update/delete rejection triggers; legacy rows are not backfilled and map as `LEGACY_UNATTESTED` with absent trusted-profile/context fields.
- Evidence: engine context tests 3/3; full engine 59/59; app JVM 162/162; instrumentation Java compilation PASS; scoped device migration/persistence tests 9/9; lint 0 errors/53 warnings; `git diff --check` PASS; preflight 4/4. Archive-first code93 artifact/backup parity and BUILD_INFO provenance pass. The three protected `.idea/*` files remain untouched and unstaged.
- Final boundary: C0C-B2 runtime wiring and reevaluation remain pending; next proposed step is C0C-B2 after review. No production DATA_COMPATIBLE result was created by B1.

## G2-C0C-B2 runtime wiring review stop

- Decision: `G2-C0C-B2_IMPLEMENTATION: PASS`; `SAFE4_EXECUTION_READINESS: BLOCKED`. Connect the bundled G2-C0B resolver to new-pack import after integrity validation, persist trusted-profile compatibility evidence through SQLite v15, and keep the current production profile fail-closed because it has no executable contract descriptor.
- Source boundary: production composition loads `BundledEditorialEngineContractProfileRegistry` and injects `EditorialEngineProfileResolver`; the old facts constructor remains only as a test seam. No profile is read from pack content, SQLite, SAF, external filesystem or network.
- Eligibility gate: the bundled profile still declares only `pack.integrity.sha256.v1`, so the resolver returns `PROFILE_NON_EXECUTABLE` / `ENGINE_UPGRADE_REQUIRED`, storage remains `STORED_BLOCKED`, and no production `DATA_COMPATIBLE` or ready state is permitted. `EditorialSafe4Pack.executionEnabled()` remains false.
- Persistence boundary: v14 pack/compatibility rows and the v15 trusted evidence row are written in one transaction; unresolved registry failures use the v14 blocker row without fabricated profile provenance; v15 failures roll back the transaction and retain immutable storage for recovery. Duplicate checks precede resolver evaluation.
- Regression/build/device: Wrapper/JBR21 `:editorial-engine:test` 60/60, `:app:testDebugUnitTest` 162/162, instrumentation Java compilation PASS, lint 0 errors/53 warnings, `git diff --check` PASS and JDK preflight 4/4. Final code98 connected QA passed 5/5 on OnePlus CPH2691 / Android 15: production non-executable provenance, duplicate no-re-evaluation, v15 rollback, restart readback and test-only unsupported contract. Archive `4.16-dev.36`/code98, event `build-20260805-095511`, APK SHA `9E147135DEA5D37EDFF5220D5EC8C44CE2686438DE3831BC5EC564E7BC752EC8`, source ZIP SHA `B8B518270092D03ECC4D479DC93AB2C55EFF027A395600582EECAA5797799F22`; artifact/backup parity PASS and BUILD_INFO provenance inspected.
- Protected files: `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` remain user-owned and unstaged.
- Next step: review stop; propose a separately approved executable-contract/capability phase. Do not proceed directly to certification or activation.

## G2-B1 handoff

- Completed: additive SQLite v14 pack registry, immutable private storage, TOCTOU-safe headless import service, recovery, fail-closed read-only registry, migration/security/recovery tests, checklist and archive evidence.
- Pending: user review of G2-C0 plan only. G2-B2A and G2-B2B-ZIP are complete. Certification, Golden Replay, project binding and execution remain blocked.
- Known limitations: the code91 walkthrough used a disposable synthetic ZIP and is not full release QA or certification evidence; one blocked QA registry row remains on the device because no manual DB mutation/cleanup bypass was used. The APK has no trusted profile and compatibility remains fail-closed.
- Canonical/candidate boundary: code86 SAFE4 hashes are unchanged; `DBE214...` and `3B2FCC...` remain outside the registry and cannot activate or replace SAFE4.

## G2-B2A handoff

- Completed: read-only Editorial Packs section, grouped deterministic list, detail dialog, exact fail-closed wording, persisted metadata/integrity display, presenter/mapper tests and no-row-mutation instrumentation source.
- Pending: user review only. G2-B2B-ZIP is complete. Certification, activation, binding and execution remain blocked.
- Connected/visual UI QA remains pending because `adb` is unavailable.

## G2-B2B-ZIP handoff

- Completed: headless `importZip(InputStream)` boundary, progress/result mapper, one-shot SAF bridge, ZIP import coordinator and Editorial-tab import action. No certification, activation, project binding, execution or folder import is present.
- Evidence: commits `e124b5a`, `8901c8a`, `22c080b`, `fd4f807`, `3efb620`, `f62ea5b`, `ced89c5`, `a831e94`; engine 19/19; app JVM 161/161; instrumentation source compilation pass; lint 0 errors/54 warnings; archive-first `4.16-dev.29`/code91 event `build-20260804-073059` APK SHA `959A25630941550E3D59CB2FBE75C1E3C6D33DCA2A109553C248CDA653E4DE90`; source ZIP SHA `A55B5AEDDD945F5247FC1A6177C80654C615DF664B34485F106B92C83B948BAD`; artifact/backup parity pass. Code91 device QA passed SAF ZIP selection, immutable storage, fail-closed blocking and restart persistence for a synthetic non-SAFE4 fixture; no certification/execution evidence.
- Runtime profile is deliberately empty until a trusted contract registry exists; imports cannot be guessed compatible and therefore remain fail-closed/blocked in the current app. Canonical SAFE4 and external candidate boundary is unchanged.
- Handoff: `EDITORIAL_ACCOUNT_TRANSFER_HANDOFF.md` is the self-contained transfer document; `EDITORIAL_PACK_PLATFORM_G2_C0_PLAN.md` is the approved review-stop plan; `EDITORIAL_PACK_PLATFORM_G2_C0A_CHECKLIST.md` records the C0A evidence. C0A is complete and stopped before C0B/C0C.

## Build toolchain audit handoff

- Audit: `BUILD_TOOLCHAIN_REPRODUCIBILITY_AUDIT_G2_C0A.md` remains the historical `FAIL / BLOCKED` finding; `BUILD_TOOLCHAIN_BASELINE_RECOVERY_G2_T0A.md` records the approved recovery as `PASS / REVIEW STOP`.
- The uncommitted AGP `9.2.1`/Gradle `9.4.1` drift was proven out-of-band and restored. Baseline is wrapper Gradle `9.3.0`, AGP `8.7.3`, Java source/target `17`, compile/target SDK `35`.
- Wrapper verification used explicit Android Studio JBR `21.0.10` and completed successfully. No direct cached Gradle executable was used; no wrapper checksum or toolchain upgrade was added.
- The pre-recovery diff is retained in `BUILD_TOOLCHAIN_DRIFT_G2_T0A.diff` with SHA-256 `0F6F4BE4D8B41051EEA5C9F6F4ECA15C399667F7B58801E7EF7822F53BC67F33`. No source, database, APK or SAFE4 asset changed.

## G2-T0B toolchain hardening handoff

- Completed: wrapper Gradle `9.3.0` is pinned with official sidecar SHA-256 `0d585f69da091fc5b2beced877feab55a3064d43b8a1d46aeb07996b0915e0e0`; AGP remains `8.7.3`; Java source/target remains `17`; compile/target SDK remains `35`.
- Completed: JDK preflight requires build runtime major `21`, accepts explicit `-JavaHome` or `JAVA_HOME`, rejects Java 8, missing JAVA_HOME and invalid paths without exposing personal paths; test matrix is 4/4.
- Completed: required Wrapper regression passed 33/33 engine tests, 161/161 app JVM tests, instrumentation source compilation, lint 0 errors/53 warnings and `git diff --check`.
- Completed: archive-first `4.16-dev.30`/code92 event `build-20260804-182006` passed; APK SHA-256 `07BC98B22019832AFD37D0307E691957FBDC47D1C0E929949535D69AFB472801`; source ZIP SHA-256 `BD6BCDC832DC5C3D9AFFA3E1C597A12D090F560F6B8D6319D0D65F7FB22C1126`; artifact/backup five-file parity passed; code92 was not installed.
- Completed: `BUILD_INFO.json` and README now record Gradle/URL/checksum, AGP, JDK/vendor/runtime/policy, Java source/target, SDK, Git/event and APK/source hashes. Existing metadata keys remain for backward compatibility.
- Pending: T0B review stop is complete; C0B is now complete at its own review stop. G2-C0C is the proposed next step and has not been implemented.

## Completed tasks

- Initialized the project as a Git repository on `main` and captured the v4.7 source baseline.
- Created and retained annotated tag `v4.7`.
- Added `BUILD_STATE.md`, `RELEASE_NOTES.md`, and normalized `CHANGELOG.md`.
- Established the `main -> feature/vX.Y -> small commits -> regression -> APK -> tag -> merge --no-ff` workflow.
- Enabled repository-local commit template and hooks that block direct commits to `main`, empty commits, invalid branch names, and unclear commit messages.
- Added the resumable `WORKSPACE_SNAPSHOT.md` handoff format and root `AGENTS.md` startup instructions.
- Enforced snapshot freshness before commits: update is required after approximately 30 minutes or before the 10th commit unless the snapshot is already staged.
- Regression-tested that a fresh snapshot is accepted by the commit guard.
- Integrated the completed Workspace Snapshot bootstrap into `main` with a no-ff merge; no application release was created.
- Added an immutable release archiver that requires APK, checksums, QA, release documents, Perfetto, Macrobenchmark evidence/status, screenshots, video, and an exact-ref source ZIP.
- Archived tag `v4.7` in both durable locations without using `build/` as storage.
- Created `project_source_v4.7.zip` from tag `v4.7` at commit `d63005793cf464064d08ed9625d36fb0b9b8c9d8`.
- Verified all 16 archived files, opened the 299-entry source ZIP, matched artifact/backup checksum manifests, and confirmed overwrite protection.
- Recorded the honest v4.7 Macrobenchmark status as `NOT RUN`; Perfetto/frame/PSS evidence remains available.
- Defined the fixed 14-step Development Workflow required for every v4.8+ session.
- Added a reusable release checklist that requires concrete Evidence for every completed step.
- Added executable `PreTag`, `PreBackup`, and `Complete` gates; v4.8+ tag/archive operations require a matching checklist version.
- Added a guarded annotated-tag wrapper and integrated the PreBackup gate into the immutable archiver.
- Regression-tested successful gates plus rejection of incomplete checklists, failed Regression, and version mismatch.
- Created pre-cleanup ZIP backup `backup/pre-git/project_source_v4.7_pre-git_20260717-192534.zip` with SHA-256 `2544FF1363F78D7B1A9960C3C234898D35676664716161E2D3BD6F544D16A94C`.
- Hardened `.gitignore`, removed tracked IDE cache/device state, and sanitized the sample API key to `REPLACE_ME`.
- Confirmed no tracked build/cache/local.properties files, no credential-pattern matches, no remote, and preserved immutable tag `v4.7`.
- Audited version history: v4.7/code46 is the last product release; feature/v4.8 through feature/v4.12 were infrastructure-only branches with no app-code changes.
- Started the focused v4.8/code47 import-navigation cycle on `fix/v4.8-import-navigation`.
- Normalized both glossary import paths so imported terms are stored as a library profile, activated immediately, reflected in Settings/prompt state, and returned to the glossary list.
- Added focused glossary parsing/merge/prompt unit coverage; `GlossaryImportV48Test` passes (2 tests).
- Made pronoun profile persistence and active selection a single synchronous transaction, and return to the profile list immediately after import/replacement.
- Added deterministic Back navigation: editors return to lists, Library subsections return to Files, Library root returns to Translate, and Settings walks through General before Translate.
- Confirmed by static audit that the four primary pages use ordinary `ScrollView` containers and have no swipe-refresh/touch gesture interceptor.
- Added instrumentation for glossary reload, pronoun cold-store reload, Library/Settings Back, and four-tab vertical gestures with preference snapshot/restore.
- Installed the current v4.8 debug APK on OnePlus CPH2691 / Android 15 through the connected instrumentation run.
- Preserved the development APK, SHA-256, QA report, release documents, and test reports under `artifacts/releases/v4.8/dev-e4b9dfd-20260717-201229/` and a non-overwriting backup mirror.
- Created local annotated tag `v4.8` and immutable release archive/backup, including source ZIP and required evidence.
- Added an opt-in two-request real API harness that asserts both glossary and pronoun locks are present in each provider prompt without logging credentials.
- Ran the opt-in harness directly against the retained OnePlus app data: exactly 2 real API requests completed, and both prompts contained the expected glossary and pronoun locks.
- Added `QA_REPORT_v4_8.md`, v4.8 release notes, and the v4.8 changelog entry; checklist steps 1-9 now have evidence.
- Created annotated local tag `v4.8` after the PreTag gate passed.
- Verified remote ancestry with no divergence (`0` remote-only / `12` local-only commits), then pushed `main` and annotated tag `v4.8` without force.
- Verified GitHub refs: `main` at `c1e807b3bad1b64f079d0a802c3b0a2e7621c0c2`; tag object `e5e6bb8aec6c713edf87354b8b76d21e004ea726`; tag target `21fc148c561dac395e85d19cd48fef6c378da3ab`.
- Clean-cloned tag `v4.8`, confirmed versionName `4.8`, versionCode `47`, all release/snapshot documents, and no sensitive path or credential-pattern matches across 31 reachable commits.
- Added mandatory `scripts/build-and-save.ps1`; direct debug APK assembly is blocked unless invoked through the archive-first workflow.
- Added automatic build numbering based on prior archives, the source default, and the connected device versionCode; the first accepted build is `4.14-dev.1`/code50.
- Created and verified immutable matching payloads for event `build-20260725-084757` in both `artifacts/builds/` and `backup/builds/`, including APK, per-build README, BUILD_INFO, SHA-256 manifest, and exact source ZIP.
- Added list-level `Import glossary` parity with Pronoun, exact single-file filename adoption, deterministic multi-file naming, custom-name preservation, and staged parsing before mutating the glossary.
- Added four naming-policy tests; focused `GlossaryImportV48Test` passes 6 tests with 0 failures/skips.
- Committed the Glossary filename-parity implementation as `b9e17cc328e594f317e99a1aae406bd9cef0171f`.
- Built and archived `4.14-dev.2`/code51 under immutable event `build-20260725-090328`; both durable copies and every manifest hash match.
- Passed the full 103-test JVM suite and Android Lint with 54 warnings and 0 errors.
- Installed `4.14-dev.2`/code51 on the connected OnePlus 13R.
- Verified on device that editor import adopts the exact CSV filename, activates the one-term glossary, and preserves the name/selection after a cold restart; restored the user's prior active glossary and removed all QA data.
- Restored the Translate dashboard's scrollable last accepted translation preview with a separate completed-chunk identity.
- Added current chunk plus exact Glossary/Pronoun rule-use metrics sourced from the actual translation/refinement `PromptPlan`, including zero-versus-unknown presentation and persisted runtime restoration.
- Added focused prompt-count, runtime-state, dashboard-label, and Android fixture coverage; the focused JVM group passed.
- Built and archived `4.14-dev.3`/code52 under event `build-20260725-093258`; 106 JVM tests passed, lint reported 54 warnings/0 errors, and every artifact/backup payload hash matched.
- Installed and visually verified the current chunk, exact rule counts, exact-prompt association, and accepted-chunk preview on OnePlus CPH2691 / Android 15 without a provider call; restored the original runtime state byte-for-byte.
- Promoted the approved bright cool logo to a permanent Android resource, launcher/round icon, and in-app top badge; retained both design concepts under `artwork/logo-concepts/`.
- Added a `preBuild` guard that requires the approved logo SHA-256 and all manifest/header references; the guard and 106 JVM tests passed.
- Replaced the obsolete v2.6.x root README diary with a current project landing page covering product capabilities, configuration formats, mandatory archive-first builds, verification, privacy, repository structure, and development workflow.
- Built and archived `4.14-dev.4`/code53 from exact source commit `ca51d7e` under immutable event `build-20260725-095315`; all artifact/backup payload hashes match.
- Installed `4.14-dev.4` and verified the approved launcher icon and in-app header logo on OnePlus CPH2691 / Android 15; the user runtime-state hash remained byte-for-byte unchanged.
- Added exact-release build mode so `v4.14` can produce `versionName 4.14` with the next unused versionCode while retaining the mandatory archive-first workflow.
- Exact-release builds now require the matching clean feature branch and reject conflicting series arguments, mismatched branches, existing tags, or duplicate archived release versions.
- Added a dedicated AndroidX Macrobenchmark 1.4.1 test module targeting a non-debuggable, profileable benchmark app variant with ProfileInstaller 1.4.1.
- Added a five-iteration physical-device cold-start benchmark that produces real StartupTiming metrics, AndroidX Benchmark JSON, and Perfetto traces.
- Prepared the v4.14 changelog/release notes and corrected the README release-candidate identity without inventing final hashes or unrun QA results.
- With explicit user approval, deleted the invalid lightweight `v4.14` tag from GitHub and local; verified that neither ref remains before resuming the release build.
- Built and archived exact `4.14`/code54 from commit `0569036` under event `build-20260725-101846`; all artifact/backup payload hashes match.
- Connected Android instrumentation passed 12 tests with 1 opt-in real-API test skipped on OnePlus CPH2691 / Android 15.
- Identified the Macrobenchmark install failure as an unsigned benchmark test APK and added the missing debug signing configuration.
- Built and archived exact `4.14`/code55 from commit `b53d209` under event `build-20260725-102329`; all artifact/backup payload hashes match.
- Signed Macrobenchmark installed successfully and captured two real startup Perfetto traces before the run was stopped after the OnePlus launcher delayed each setup Home action for nearly 10 minutes.
- Removed the redundant Home setup action; `StartupMode.COLD` already force-stops the target before each measured launch.
- Built and archived exact `4.14`/code56 from commit `59b20f1` under event `build-20260725-103832`; all artifact/backup payload hashes match.
- Diagnosed OnePlus Hans freezing the background Macrobenchmark runner even while it held a partial wake lock; standard device-idle whitelisting did not bypass the OEM freezer and was fully reverted.
- Added a benchmark-only foreground host activity, retained a bounded wake lock, and returned the host to the foreground immediately after each measured launch.
- Suppressed the first-run notification request only in the non-product `benchmark` build type so StartupTiming measures `MainActivity`, not the Android permission controller.
- Corrected diagnostic Macrobenchmark passed 5/5 physical-device cold starts with 0 failures/skips and produced AndroidX JSON plus five Perfetto traces. Time to initial display was 293.57 ms minimum, 332.01 ms median, and 441.19 ms maximum.
- Committed the OEM-safe benchmark host and benchmark-only notification-dialog suppression as `82d5e4a`.
- Built, archived, verified, and installed exact `4.14`/code57 from `82d5e4a` under event `build-20260725-111133`; all five build payloads match the backup.
- Passed 106 JVM tests, logo guard, lint (0 errors/54 warnings), and connected instrumentation (11 passed/1 opt-in paid case skipped).
- Accepted code57 Macrobenchmark rerun passed 5/5 physical-device cold starts at 307.78/322.81/337.52 ms min/median/max and retained five real Perfetto traces plus AndroidX JSON in both durable stores.
- Passed controlled output-permission-loss QA with a valid prepared local TXT, exact missing-write-grant blocker, `Choose output folder again`, and Start disabled; no provider request occurred and QA-created app state was removed.
- Retained final Translate/Library screenshots and a 24-frame MJPEG AVI from timed actual device captures; all visual files match their backup copies.
- Passed the PreTag workflow gate and created annotated tag `v4.14` at release-document commit `8a3b281`.
- Passed the PreBackup gate and created immutable event `tag-v4.14` from the annotated tag.
- Verified both 20-file release payloads by relative path, length, and SHA-256; all 19 manifest entries pass.
- Regenerated `project_source_v4.14.zip` directly from tag `v4.14` and confirmed exact SHA-256 equality with the retained source ZIP.
- Merged `feature/v4.14` into `main` with `--no-ff`, pushed `main` and annotated `v4.14`, and verified remote release merge `41cc6e9`, tag object `246de34`, and tag target `8a3b281`.
- Started the v4.15 cycle from clean `main` at `9199733`, created `feature/v4.15`, and restored the user's unrelated `.idea/gradle.xml` change after branch creation.
- Added a Settings hydration guard so programmatic `setText`/`setChecked` calls cannot schedule delayed persistence while stored values are being loaded.
- Hydrated phone Settings sections immediately after lazy construction and stopped section expand/collapse state changes from writing the entire Settings model.
- Added an `onStop` persistence boundary for real user edits while retaining the hydration guard.
- Added `V415SettingsPersistenceInstrumentedTest`, which covers cold launch, lazy Provider/Prompt/Performance expansion, exact custom-value hydration, refresh, and storage preservation.
- Created and installed archive-first `4.15-dev.1`/code59 from Settings P0 commit `e1b7a17` under event `build-20260729-062725`; APK SHA-256 is `18C36F6A...BCC665`.
- Passed the focused Settings P0 instrumentation 1/1 on OnePlus CPH2691 / Android 15.
- Preserved the current vertical position when a user selects a different Glossary or Pronoun from its list, while retaining top-of-page behavior for editor transitions.
- Added `V415LibraryScrollInstrumentedTest`, which builds long fixture libraries, scrolls both lists, selects a different profile, verifies exact `scrollY` and selected IDs, and restores Glossary, Pronoun, and Settings preferences exactly.
- Created and installed archive-first `4.15-dev.2`/code60 from scroll implementation commit `9e6730d` under event `build-20260729-063857`; APK SHA-256 is `828B4535...DCE7`.
- Passed the focused scroll instrumentation 1/1 on OnePlus CPH2691 / Android 15.
- Enabled multi-select on list-level Glossary and Pronoun pickers; every valid file is parsed and persisted as an independent profile, invalid siblings are reported without discarding valid imports, and the first valid profile becomes active.
- Kept Pronoun replacement single-select and cleared stale replacement intent after cancellation/failure. Renamed the Glossary editor action to `Merge files` so its intentional merge behavior is distinct from list import.
- Added four pure planner tests and `V415MultiProfileImportInstrumentedTest`, which verifies picker flags, URI de-duplication, two independent Glossaries, two independent Pronouns, activation, and exact restoration of user preferences/runtime log.
- Created and installed archive-first `4.15-dev.3`/code61 from multi-import commit `cda8eff` under event `build-20260729-065132`; APK SHA-256 is `5D412996...A1EF7`.
- Passed focused multi-profile import instrumentation 2/2 on OnePlus CPH2691 / Android 15.
- Passed the full v4.15 automated regression: approved-logo guard, 110/110 JVM tests, lint with 0 errors/53 warnings, and connected instrumentation with 15 passed plus 1 explicitly opt-in paid case skipped and 0 failures/errors.
- Added `QA_REPORT_v4_15.md` with exact build identity, archive hashes, suite counts, focused v4.15 evidence, and remaining release work.
- Built exact `4.15`/code62 from commit `5253f00` with the mandatory archive-first workflow under event `build-20260729-070042`; repeated 110 passing JVM tests and lint with 0 errors/53 warnings.
- Verified all five exact-build files match their backup mirrors, all four manifest hashes pass, installed the APK, and confirmed device package metadata reports versionName `4.15`/versionCode `62`.
- Passed full connected instrumentation against exact `4.15`/code62: 16 total, 15 passed, 1 explicitly opt-in paid real-API case skipped, and 0 failures/errors.
- Passed exact-code62 physical-device Macrobenchmark with 5/5 cold starts at 313.89/330.23/335.04 ms minimum/median/maximum and retained AndroidX JSON plus five real Perfetto traces.
- Retained five exact-device screenshots, five UI hierarchy dumps, and a clearly documented 25-frame MJPEG AVI covering Settings, Provider/Prompt/Performance, Glossaries, and Pronouns.
- Mirrored the 21-file event `qa-code62-20260729-072300` under both durable QA stores with matching relative paths, lengths, and SHA-256 hashes.
- Reinstalled the exact archived APK after benchmark cleanup and reconfirmed versionName `4.15`, versionCode `62`, minSdk `26`, targetSdk `35`, and `.MainActivity`.
- Passed the PreTag gate and created annotated tag `v4.15`; tag object `7e99cc69eb948a41659021c1114468e775e13b34` targets approved commit `d6d844d97f51ce8637048e1fbbd0f7a9940cee47`.
- Stopped before PreBackup/archive after proving that local tag `v4.15` still contains v4.14 release notes and no v4.15 changelog section; confirmed the tag is absent from `origin` and both `tag-v4.15` archive destinations are absent.
- With explicit approval, deleted only rejected unpublished tag object `7e99cc69eb948a41659021c1114468e775e13b34`; no remote ref or archive payload was removed.
- Prepared correct v4.15 release notes and a v4.15 changelog entry covering all three fixes plus exact build/QA evidence.
- Committed corrected release metadata as `292b24e`, reran PreTag successfully, and created fresh annotated tag object `cb474d2dd22763f67e24b4c0f57a27b7ae4689e7` targeting that commit.
- Verified directly from tag `v4.15` that release notes identify 4.15 and the changelog begins with a 4.15 section.
- Passed PreBackup and created immutable `tag-v4.15` artifact/backup payloads from corrected tag target `292b24e`.
- Verified both 22-file payloads match by relative path, length, and SHA-256; all 21 manifest entries pass.
- Verified archived APK SHA-256 `7FDF60C9...3969` and 357-entry source ZIP SHA-256 `0F14121D...F433`; a fresh `git archive` of `v4.15` matches the retained ZIP exactly.
- Updated `BUILD_STATE.md` to make v4.15/code62 the current accepted local release and record the exact build, corrected tag, installed-device state, regression, archive hashes, mirror parity, and known limitations.
- Passed final export verification on a clean tracked tree: tag ancestry/metadata, 22-file parity, all 21 manifest entries, APK hash, fresh 357-entry tag ZIP equality, current release documents, and installed v4.15/code62 identity all passed.
- Removed the temporary fresh-tag verification ZIP and restored the user's `.idea/gradle.xml` change.
- Recorded the first Complete-gate rejection: all 14 steps were checked, but the gate requires literal workflow status `COMPLETE`; corrected that transitional status for the retry.
- Passed `verify-release-workflow.ps1 -Gate Complete -ExpectedVersion 4.15` after the required status correction; all 14 checklist steps are complete.
- Merged `feature/v4.15` into `main` with `--no-ff` as release merge `81b8344`; corrected tag target `292b24e` is an ancestor of merged `main`.
- Pushed `main` and corrected annotated tag `v4.15` to the private GitHub repository without force.
- Verified remote `main` at `81b8344`, annotated tag object `cb474d2d...89e7`, and peeled tag target `292b24e...432c`.
- Created `feature/v4.16` from clean `main` after preserving only the user-owned IDE change in a named stash.
- Added `EDITORIAL_WORKFLOW_V5_PLAN.md`: reviewed V5 context/state/gate requirements, UI inheritance, single and batch import UX, filename metadata suggestions, project-level glossary/pronoun defaults, asset snapshots, data model, context allow-list, risks, and phased plan.
- Created `release_checklists/v4.16.md`; P0.1 implementation/build evidence is recorded while UI and persistence work remain pending.
- Added P0.1 editorial workflow contracts: strict V5 chapter state transitions, L1/L2/L3 context allow-lists, release-gate predicate, versioned L1/L2/L3 evidence schemas, and structural L1 report validation.
- Added six focused offline tests in `EditorialWorkflowV5Test`; passed 6/6. Archive-first `4.16-dev.1`/code63 build passed all 116 JVM tests and lint with 53 warnings/0 errors; artifact/backup files and hashes match.
- Completed P0.2: L2/L3 output validators, Canonical RAW Map, asset snapshot ambiguity/staleness checks, L2/L3 JSON fixtures, and long-RAW/voice/truncation tests. Focused suite passed 9/9; archive-first `4.16-dev.3`/code65 passed 119 JVM tests and lint with 53 warnings/0 errors; immutable artifact/backup parity passed.
- Completed P1 persistence: additive SQLite v10→v11 migration and repository records for Editorial Project, Chapter, asset snapshot, run, scene, gate and evidence. All 120 JVM tests and the physical-device repository test pass; archive-first `4.16-dev.4`/code66 passed lint with 53 warnings/0 errors and artifact/backup parity.
- Completed P1 import UI: isolated Biên tập tab, project creation, one-picker multi-TXT selection, deterministic RAW–DRAFT mapping preview, optional shared glossary/pronoun and active-profile defaults, and confirmed immutable asset snapshots. Archive-first `4.16-dev.5`/code67 passed 123 JVM tests and lint with 53 warnings/0 errors; artifact/backup parity passed.
- Completed P2 L1: isolated context builder and on-demand audit runner with RAW Map-before-DRAFT order, output validator, scene/gate/evidence persistence and REPORT_L1 rendering. Archive-first `4.16-dev.6`/code68 passed full JVM tests and lint; long chapters are deliberately blocked until scene segmentation is implemented.
- Completed segmented L1 for structurally mapped long chapters: deterministic RAW segmentation, evidence-based RAW–DRAFT marker mapping, per-scene contract/call/checkpoint, aggregate validator and REPORT_L1. Build `4.16-dev.7`/code69 passed 133 JVM tests and lint with 53 warnings/0 errors; artifact parity passed. Ambiguous/misaligned mappings remain deliberately blocked.
- Completed REPORT_L1 UX and checkpoint recovery: closed chapters can view/export TXT; failed segmented runs show failure evidence and retry only non-closed scenes on the same run. Build `4.16-dev.8`/code70 passed 133 JVM tests and lint with 53 warnings/0 errors; device execution of the new retry instrumentation remains pending.
- Completed model-assisted mapping fallback for long L1 chapters without matching markers. The model may select only deterministic anchors; validator rejects skipped, overlapping, reordered, or incomplete DRAFT coverage and retains mapping/token evidence.
- Completed initial L2 RAW-first runner and UI. The first isolated call sees only RAW/Glossary/Pronoun and must close `L2_RAW_LEDGER`; only then does a fresh edit context open DRAFT and validated REPORT_L1. VI_L2, Global Change Register, five gates, context manifest, hashes and per-phase usage are persisted. Build `4.16-dev.10`/code72 passed 138 JVM tests, lint 53 warnings/0 errors and artifact/backup parity; focused device instrumentation passed 4/4 on code71 before the final context-size guard.
- Completed segmented L2, per-RAW/per-edit scene checkpoints, aggregate VI_L2 validation, failed-scene-only retry and line-oriented DRAFT→VI_L2 diff/export UI. Device regression exposed and fixed marker-only scene segmentation. Accepted `4.16-dev.12`/code74 passed 142 JVM tests, lint 0 errors/53 warnings, focused device Editorial tests 5/5 and artifact/backup parity.

- Completed isolated L3 core: fresh RAW–VI_L2 context without REPORT_L1, persisted/validated independent ledger checkpoint, delayed report-review context, FINAL_QA/Change Set/five gates, cross-scene voice audit, final read-through and RELEASE_READY UI. Accepted `4.16-dev.13`/code75 passed 144 JVM tests, lint 0 errors/53 warnings, focused device Editorial tests 6/6 and artifact/backup parity.

- Completed segmented L3: inherits accepted L2 scene checkpoints, validates matching anchors, checkpoints independent comparison per scene, closes a separate chapter-level voice audit before REPORT_L1 unlock, checkpoints final review per scene, aggregates FINAL_QA/Change Set/gates, and retries only invalid checkpoints. Accepted code77 passed 145 JVM tests, lint 0 errors/53 warnings, focused device Editorial tests 7/7 and artifact/backup parity.
- Completed Editorial release: revalidates the latest closed L3, FINAL_QA contract and persisted gates; exports a ZIP with final text, redacted hash/length/status evidence and checksums; records manifest/bundle hash and changes state atomically only after successful SAF write. Accepted code78 passed 145 JVM tests, lint 0 errors/53 warnings, and the complete Editorial device suite 9/9.
- Completed the code78 manual Release walkthrough and desktop ZIP inspection; retained eight matching evidence files in artifact/backup, removed all disposable device data, and defined the full v4.16 RC matrix in `QA_SCOPE_v4_16.md`.
- Implemented responsive two-line five-tab navigation and per-project default Release folders with persisted SAF permission, direct ZIP creation, unique `.zip` naming and an explicit fallback-picker destination/cancel warning; code79 build/device verification is pending.
- Accepted code79 after 147/147 JVM tests, lint 0 errors/53 warnings, artifact/backup parity and 10/10 focused Editorial device tests; exact code79 APK was reinstalled after the test runner cleanup.
- Added project-owned Editorial Glossary/Pronoun persistence and UI, removed the Translation Settings fallback from Editorial batch import, and added editable Series/Volume identity with collision validation. The expanded JVM suite passes; archive-first build/device migration regression is pending.
- Replaced the ambiguous all-in-one Editorial batch picker with independent RAW, DRAFT, Glossary, and Pronoun controls. RAW/DRAFT roles are explicit and accept filenames without role tokens; Glossary/Pronoun reuse Translate's wildcard picker and content validation. Code81 archive-first build passed 149 JVM tests and lint, both immutable payloads were created, and exact code81 was installed; hands-on picker UX verification remains pending.
- Corrected explicit RAW/DRAFT pairing to prefer the leading chapter number, so `005_RAW_...txt` pairs with a selected DRAFT named `005_RAW_... (Vietnamese).txt`. Selected RAW/DRAFT filenames are now visible on the project card. Added additive database v12→v13 project-reference profiles: multi-file Glossary/Pronoun import, per-project ACTIVE selection, profile listing/deletion, and migration of each existing project reference to an active profile. Code82 archive-first build passed 150 JVM tests/lint, installed over code81 and cold-launched successfully; hands-on profile/picker UX verification is pending.

- Editorial improvement steps 2-3: chapter cards now show RAW, DRAFT, Glossary and Pronoun immutable snapshot metadata (name, size and hash); the import preview is a scrollable card layout with READY/BLOCKED state, four input rows, active-reference validation and warnings. The preview remains read-only.
- Editorial improvement step 1 contract: `EditorialImportPlanner.planBundle(...)` groups by canonical numeric chapter key, detects RAW/DRAFT/Glossary/Pronoun from filename plus supported format, accepts CSV references, exposes `READY`/`NEEDS_REVIEW`, and keeps unclear files in `unassigned` so they cannot be saved automatically. Commit `36f42e7`.
- Editorial improvement step 2: RAW/DRAFT plans explicitly bind project ACTIVE references as `Inherited project default`; four-file bundles bind Glossary/Pronoun as `Chapter override`; chapter-numbered reference imports remain pending per chapter without changing ACTIVE; chapter-less reference imports require confirmation before project-default persistence. Preview renders source/profile/file ownership before save. Commit `4aca556`.
- Built and archived `4.16-dev.21`/code83 from commit `5264501` under event `build-20260801-133230`; both five-file payloads match, APK/source ZIP hashes are recorded in `BUILD_STATE.md`, 153 JVM tests passed and lint has 53 warnings/0 errors.
- Built and installed `4.16-dev.22`/code84 from commit `b6936fb` under event `build-20260801-133953`; both five-file payloads match, APK SHA-256 is `CDE2085E...892A51`, source ZIP SHA-256 is `4FECD825...CF835E2`, and the device reports exact package identity with a successful cold MainActivity launch.
- Added `EDITORIAL_HANDOFF_V4_16.md` and linked it from `README.md`; the handoff preserves the investigation, four-role/default-vs-override contract, revision-safe editing/stale-run roadmap, transfer checklist, build commands, artifact paths, and safety constraints. Commit `6d82c41`.
- Retired the incompatible V5 Editorial execution core: removed its schema/validator, three runners, context/model contracts, unsafe evidence mutation/latest lookup APIs, release bundle/folder flow, run/retry/release UI and obsolete behavior tests. No old execution entry point remains in current source.
- Bundled the exact V5-SAFE.4 three-file source pack with immutable per-file hashes and a mandatory `preBuild` integrity guard. Added a version/pack capability gate that remains disabled until lineage, exhaustive ledgers, evidence-derived gates, conditional Pronoun/Pair handling, SAFE4 release artifacts and Golden Replay G1–G10 are evidenced.
- Converted Editorial storage/UI to fail closed: new projects require the exact SAFE4 identity, new chapters are `SAFE4_BLOCKED`, legacy projects/chapters are read-only, and RAW/DRAFT/Glossary plus optional `PRONOUN_STATUS=NONE` can be prepared without enabling model execution.
- Added `EDITORIAL_SAFE4_MIGRATION.md` as the current source of truth and marked the older V5 plan/handoff historical. SAFE4 foundation regression passed 144/144 JVM tests and Android instrumentation-source compilation; the three bundled assets match the external source byte-for-byte.
- Built and retained `4.16-dev.23`/code85, then rejected it during post-build verification because its source ZIP normalized the three SAFE4 text files to CRLF. The APK pack guard passed, but the archived source was not byte-reproducible. Added a binary `.gitattributes` rule; code85 remains immutable audit evidence and cannot be accepted.
- Built and accepted `4.16-dev.24`/code86 from `ab8e78a` under event `build-20260803-165452`. Artifact/backup parity and manifests pass; 144 JVM tests and lint 0 errors/53 warnings pass; source ZIP reproduces the external SAFE4 sizes/hashes exactly. Installation was not requested.
- Completed Phase 1 source audit and saved `EDITORIAL_PACK_PLATFORM_PLAN.md` as the proposed contract-first, multi-pack, fail-closed architecture. G2-A now adds only the isolated JVM manifest/integrity/compatibility/read-only registry slice; no retired V5 execution component was restored.
- Rechecked the external SAFE4 source folder during Editorial Pack research. Project Instruction and Workflow still match code86, but the external Prompt is now 5,040 bytes/SHA-256 `DBE214D842D98FD76AFD2E700747FCC2CF3D5B6038134B38EA8F220FED3BF273`, while code86/source ZIP retains 5,008 bytes/SHA-256 `0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81`. The added L2 `PRONOUN_STATUS: LEGACY_REJECTED` line may change phase policy, so the external bytes are BLOCKED pending a distinct version, canonical manifest and contract review; no old build evidence applies to them.
- Final G2-A recheck found the external Prompt path changed again to 5,072 bytes/SHA-256 `3B2FCC0A8B684B851F8F6180D41DBE8C75E00CDB5BED217C3FB7A50E94437B04`. The earlier `DBE214...` candidate identity is not assumed to describe these current bytes; both remain UNIDENTIFIED/BLOCKED and outside the official registry.

## Pending tasks

- Resolve, in a later approved pack-review phase, whether the external `DBE214...` Prompt intentionally changes L2 Pronoun policy. Preserve code86's `0B4C...` Prompt as a separate immutable historical identity; do not reuse the same pack version/hash.
- G2-A implementation is complete in commits `83a9428`, `616dc46`, `eb15481` and `9d401b2`; its legacy `EditorialEngineProfile` remains an evaluator-facts object and is not a second trusted-profile source of truth. G2-C0A adds only the immutable trusted-profile data contract, strict parser/canonicalizer, fingerprint/hash calculations and fail-closed validator in `:editorial-engine`. Focused C0A tests pass 14/14 and full engine/app regression remains green. No app connection, SQLite change, model call, runtime wiring, profile bundle, L1/L2/L3 or APK build was made.
- G2-C0B implementation is complete in `fb2bd262dc996d20fbef051414727525410e9e1a` + `6c6fdb2c3ab15e972716d005b1588cee8b51fdec` + `285819c329c922d1256eb27bc13ee5ba75ec5fd1`: the source-bundled bootstrap profile, independent trust anchors, immutable read-only registry, fail-closed negative matrix, build-only APK asset copy and reviewed evidence fingerprint are complete. No app source/runtime/importer call site, SQLite state or APK build changed. C0B is stopped for review; G2-C0C remains pending.
- G2-B2B-ZIP is complete for this approval scope. Await user review before any folder import, certification, activation, project binding or execution work.
- Build-toolchain reproducibility audit is blocked: the actual worktree contains six unstaged changes instead of only `.idea/gradle.xml`; committed HEAD is AGP 8.7.3/wrapper 9.3.0, while the dirty tree is AGP 9.2.1/wrapper 9.4.1. Do not attribute cached 9.4.1 C0A results to clean HEAD.
- Implement typed exhaustive ledgers and machine-derived gates; do not accept model-provided PASS/CLOSED as evidence.
- Implement Pronoun `AVAILABLE/NONE/LEGACY_REJECTED`, scoped Pair Context, exact per-phase context allow-lists and checkpoint lineage.
- Implement versioned L1/L2/L3 SAFE4 runners, SAFE4 receipt/release artifacts, and retained Golden Replay G1–G10 evidence before enabling execution.
- Run device QA only on a new archive-first SAFE4 build. Keep the full v4.16 QA/tag/backup/export gates open.
- Review the completed `EDITORIAL_PACK_PLATFORM_G2_C0B` implementation and approve any later G2-C0C work separately. Do not start runtime selection/importer wiring, certification, activation, binding or execution in this handoff.

## Known bugs

- The current external SAFE4 folder no longer matches the exact code86/source-ZIP Prompt bytes, changed during this session, and has no canonical Editorial Pack manifest. Its observed current hash `3B2FCC...` and earlier `DBE214...` identity must not be classified as DATA_COMPATIBLE or certified under the old SAFE4 identity until separately versioned and reviewed.
- SAFE4 model execution and Editorial release remain intentionally blocked; G2-A is not connected to the Android execution path and does not certify any pack. This is a safety blocker, not a completed feature.
- The installed/archived code84 APK still contains the retired V5 engine. It must not be used to create or certify SAFE4 output.
- Code91 device QA proved only that a synthetic ZIP can be stored immutably and remains fail-closed; the device retains one blocked QA registry row and has no trusted engine profile.
- G2-C0B does not wire the bundled registry into compatibility/importer runtime; compatibility remains fail-closed and packs remain `STORED_BLOCKED`. The source is newer than the accepted code92 APK because no APK build was authorized for this registry-only slice.
- Toolchain reproducibility is unresolved: no wrapper checksum is declared, JDK selection is not pinned, default Java 8 is unsuitable for AGP, and the wrapper currently depends on cache/network permissions in this environment.
- Code85 is rejected even though its APK pack guard passed: the retained source ZIP changed SAFE4 line endings and therefore failed exact-source reproducibility. Both payload copies are intentionally retained, not overwritten.
- Legacy V5 database rows are preserved but do not yet have SAFE4 manifest/parent lineage; they remain read-only and cannot be promoted into a SAFE4 chain.

- Code78 bottom-navigation clipping has an implemented responsive two-line fix in code79; visual verification is pending because the physical device is secured at the lock screen.
- Code80 device acceptance is blocked only by the Android device being disconnected; build, 148 JVM tests, lint and artifact parity passed.
- The OnePlus last-used-folder ambiguity has a code79 per-project Release folder/direct-write path plus clearer fallback warning; physical SAF selection/write/permission-loss verification is pending.

- Rejected code54 candidate: Android removed the target package during cleanup after rejecting the unsigned Macrobenchmark APK. No benchmark measurement was produced.
- Rejected code55 candidate: the signed test captured two traces, but the OEM launcher stability wait made the five-iteration run impractical; the redundant Home step is removed for the next candidate.
- Rejected code56 candidate: its archived source removed the Home step but OnePlus Hans still froze the background test runner after trace capture. The correction was committed and rebuilt as accepted code57 release evidence.
- First code57 Macrobenchmark evidence set: completed but rejected because one OEM-affected first iteration measured 109,774 ms; retained for audit. The accepted rerun has all five iterations between 307.78 and 337.52 ms.
- P0 root causes fixed: the picker stripped `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`; external small config files were used at runtime; stale prepared-plan callbacks could keep Start disabled; and the output tree URI was decoded twice between service preparation and materialization, corrupting the persisted-grant identity.
- Controlled output-permission-loss UX passed on the physical device with a prepared local TXT, an invalid persisted output grant, the exact recovery message, and Start disabled before dispatch.
- Warm tab-switch P50 is `20 ms`, above the `16 ms` target.
- Android Lint has 50 warnings and 0 errors in the recorded v4.7 build.
- The full 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.
- APK and source ZIP archives are deliberately ignored by Git, so they are not present in a clean clone; durable copies remain in the local `artifacts/releases/v4.8/` and `backup/v4.8/` stores.
- Android Lint reports 54 warnings and 0 errors for the accepted v4.14 development build.
- v4.15 Settings P0 root cause was lazy phone-section construction followed by persistence of default-valued controls before hydration. The correction passed compile/JVM checks and the strengthened physical-device instrumentation, including exact preference snapshot equality after QA restoration.
- An accidental but valid immutable `4.14-dev.5`/code58 build is retained under event `build-20260729-062629` because the first build invocation omitted the explicit v4.15 series. It was superseded, not overwritten, by `4.15-dev.1`/code59.
- v4.15 library scroll root cause was same-tab cache invalidation rebuilding the `ScrollView` at position zero after selection. The correction restores the bounded prior position only for list-level Use actions; editor navigation behavior is unchanged.
- v4.15 multi-file import root cause was that list-level Glossary/Pronoun pickers did not set `EXTRA_ALLOW_MULTIPLE`, and their result handlers assumed one URI. The corrected handlers enumerate unique selected URIs and persist one profile per valid file.
- OnePlus Android 15 denies shell `screenrecord`; exact-code62 visual evidence therefore uses an explicitly documented MJPEG sequence of actual-device screenshots, not continuous MediaProjection capture.
- The first local tag was rejected for backup because it contained stale v4.14 release metadata; it was absent from `origin` and was deleted with approval before any immutable archive existed.
- Project default/chapter override flow is connected to the current picker/preview/save path; revision-safe editing of already-created snapshots is still not implemented.
- The portable handoff is tracked, but it does not replace the required source implementation, device QA, or v4.16 release gates.

## Regression status

- Editorial Pack Platform G2-A: PASS / REVIEW STOP. The isolated `:editorial-engine` module passes 19/19 JVM tests from clean state; the existing app passes 144/144 JVM tests. No SQLite, UI, model, execution, APK or release evidence was produced for G2-A.
- V5-SAFE.4 retirement/foundation regression: PASS for source safety and compilation. `:app:testDebugUnitTest` passed 144/144 with 0 failures/errors/skips; the mandatory pack guard verified all three external-source hashes; `:app:compileDebugAndroidTestJavaWithJavac` succeeded; `git diff --check` passed. SAFE4 execution, release, device QA and Golden Replay are deliberately not claimed.
- V5-SAFE.4 archive-first build verification: PASS on accepted code86. Both five-file stores and both checksum manifests match; APK/source ZIP hashes are `6C3EEF73...F37875`/`9CCE4C99...A0D9F1`; the ZIP's three SAFE4 files match external byte sizes and SHA-256 values. Installation/manual device QA remain open.

- v4.16 Editorial project-default/override regression: PASS. `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest` passed 160/160 JVM tests with 0 failures/errors/skips; targeted planner tests passed 14/14; `git diff --check` passed. Tests cover inherited defaults, chapter overrides, missing defaults and the 005/006/007 chapter-local bundle. Source/test evidence is committed through `5589af0`.
- v4.16 Editorial improvement steps 1-3 regression/build: PASS. `scripts/build-and-save.ps1 -Series 4.16-dev` archived code83 with 153 JVM tests passed, 0 failures/errors/skips, lint 0 errors/53 warnings, and matching five-file artifact/backup payloads. Device/manual UX QA remains pending.
- v4.16-dev.22 archive/install verification: PASS. `scripts/build-and-save.ps1 -Series 4.16-dev -Install` produced matching five-file artifact/backup payloads; manifest hashes pass; OnePlus CPH2691 / Android 15 reports versionName `4.16-dev.22`, versionCode `84`, minSdk `26`, targetSdk `35`; cold MainActivity launch returned `Status: ok`.
- v4.16 segmented L3 regression: PASS on accepted code77. 145 JVM tests passed; lint reports 0 errors/53 warnings; focused device Editorial suite passed 7/7. Controlled final scene-002 failure proved retry made exactly one additional model call while reusing independent scenes, chapter voice audit and closed final scene-001. Artifact/backup parity passed.
- v4.16 Editorial release regression: PASS on accepted code78. 145 JVM tests passed; lint reports 0 errors/53 warnings; complete physical-device Editorial suite passed 9/9 on OnePlus CPH2691 / Android 15. Tests prove sensitive source/config strings are absent from redacted evidence, checksums match, an open gate blocks release, and successful recording reaches `RELEASED`. Artifact/backup parity passed.
- v4.16 code78 manual Release QA: PARTIAL PASS. Confirmation, picker abort/restart safety, successful save, `RELEASED` UI, actual ZIP opening, exact three-entry contract, UTF-8 output, checksum verification and redaction inspection passed. Disposable device data was removed and evidence mirrored. Full RC scope remains pending; bottom-nav clipping and picker destination/cancel UX are recorded.
- v4.16 code79 navigation/destination regression: AUTOMATED PASS / VISUAL PENDING. 147 JVM tests and 10/10 Editorial device tests passed; lint and artifact parity passed. Visual navigation and real SAF folder walkthrough could not proceed while the phone was secured; no lock bypass was attempted.
- Editorial reference isolation/edit-identity JVM regression: PASS. `testDebugUnitTest` completed successfully after the additive v11→v12 migration and UI/data-flow changes; device migration/instrumentation remains pending.
- v4.16 transfer handoff documentation: PASS. `EDITORIAL_HANDOFF_V4_16.md` records the problem analysis, implemented contract, remaining revision-safe/stale-run design, exact resource map, transfer checklist, and archive-first continuation commands; `git diff --check` passed before commit `6d82c41`.
- G2-C0 audit/device evidence: PASS WITH LIMITATIONS. Source audit found only `pack.integrity.sha256.v1` implemented and all nine SAFE4 execution capabilities missing. Archived code91 installed successfully; synthetic SAF ZIP import stored the exact fixture immutably, returned `UNSUPPORTED_CONTRACT_SCHEMA`/`STORED_BLOCKED`, and persisted after process restart. No profile was bundled, no database/schema was changed, and no certification or execution was performed.
- G2-C0A trusted profile model/validation regression: PASS / REVIEW STOP. Focused C0A suite passed 14/14; full `:editorial-engine:test` passed 33/33; `:app:testDebugUnitTest` passed 161/161; Android instrumentation source compilation passed; `git diff --check` passed. Cached Gradle 9.4.1 was invoked directly because the unchanged repository wrapper is 9.3.0 while the installed Android Gradle Plugin requires 9.4.1; no APK was built. C0A validates declared capability evidence against the production catalog, which confirms only `pack.integrity.sha256.v1`; the nine SAFE4 capabilities remain missing.
- G2-C0B bundled registry regression: PASS / REVIEW STOP. Focused suite passed 17/17; full `:editorial-engine:test` passed 50/50; `:app:testDebugUnitTest` passed 161/161; Android instrumentation source compilation passed; lint has 0 errors/53 warnings; `git diff --check` passed. All Gradle commands used Wrapper Gradle 9.3.0 with JBR 21. No APK build/install occurred; latest accepted artifact remains code92. The registry is not wired into runtime/importer, so the historical fail-closed compatibility state is unchanged.
- G2-C0A build-toolchain reproducibility audit: FAIL / BLOCKED. The C0A tests are functionally green but environment-bound because they used direct cached Gradle 9.4.1; clean HEAD uses wrapper 9.3.0 plus AGP 8.7.3, and the actual worktree has uncommitted AGP 9.2.1/wrapper 9.4.1 drift. Default Java 8, absent wrapper checksum and cache/network permission failures prevent a clean-clone PASS.
- G2-T0A toolchain baseline recovery: PASS / REVIEW STOP. The three toolchain files were restored narrowly to wrapper Gradle 9.3.0 and AGP 8.7.3 after their drift was preserved and hashed. With explicit Android Studio JBR 21.0.10, `gradlew.bat --version`, the combined C0A regression, lint and `git diff --check` all passed. Final worktree changes are only the three user-owned `.idea/*` files; no APK was built.

- v4.16 isolated L3 regression: PASS on code75. 144 JVM tests passed; lint reports 0 errors/53 warnings; focused device Editorial suite passed 6/6. The L3 test proves the first model prompt contains RAW and VI_L2 but no REPORT_L1, then proves report visibility only after independent evidence validates CLOSED. Artifact/backup parity passed.

- v4.16 segmented L2 regression: PASS on accepted code74. 142 JVM tests passed; lint reports 0 errors/53 warnings; focused device Editorial suite passed 5/5, including a controlled scene-002 failure where retry made exactly one additional model call and reused every closed RAW/edit checkpoint. Artifact/backup parity passed.
- v4.16 model-mapping/L2 RAW-first regression: PASS. Archive-first `4.16-dev.10`/code72 ran 138 JVM tests with 0 failures/errors; lint reported 53 warnings/0 errors; focused physical-device Editorial instrumentation passed 4/4 on code71; final code72 added only the safe-context guard and passed the complete JVM/lint build; artifact/backup file hashes match.

- v4.16 planning documentation: PASS. `git diff --check` passed; no product code, APK build, or device regression has been run because implementation has not started.
- v4.16 P0.1 regression: PASS. `EditorialWorkflowV5Test` 6/6; full archive-first build recorded 116 JVM tests, 0 failures/errors, Android Lint 53 warnings/0 errors, and verified five-file artifact/backup parity.
- v4.16 P0 regression: PASS. `EditorialWorkflowV5Test` 9/9; full archive-first build recorded 119 JVM tests, 0 failures/errors, Android Lint 53 warnings/0 errors, and verified five-file artifact/backup parity.
- v4.16 P1 regression: PASS. All 120 JVM tests recorded 0 failures/errors; physical-device `EditorialRepositoryInstrumentedTest` passed 1/1; archive-first build recorded lint 53 warnings/0 errors and verified five-file artifact/backup parity.
- v4.16 P1 import UI regression: PASS. All 123 JVM tests recorded 0 failures/errors; `EditorialImportPlannerTest` exercises complete, missing, and ambiguous filename cases; archive-first build recorded lint 53 warnings/0 errors and verified five-file artifact/backup parity.

- v4.7 recorded regression: `PASS WITH KNOWN LIMITATIONS`.
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Workspace Snapshot enforcement: implemented; fresh-snapshot guard regression passed.
- Immutable archive regression: passed checksum, ZIP integrity, artifact/backup parity, and no-overwrite checks.
- v4.8+ workflow gate regression: passed; negative cases were rejected as designed.
- Repository hygiene regression: passed tracked-file, secret-pattern, and ignore-rule checks.
- Glossary import unit regression: 2 passed, 0 failed; device reload instrumentation passed.
- Pronoun/glossary focused JVM regression: 4 passed, 0 failed; cold-store instrumentation passed.
- Back/Settings navigation policy regression: 7 passed, 0 failed; device Back/gesture instrumentation passed.
- Full v4.8 JVM regression: 99 passed, 0 failed, 0 skipped.
- Full Android instrumentation on OnePlus CPH2691 / Android 15: 11 passed, 0 failed, 0 skipped, including all 4 new import/navigation/scroll cases.
- With the opt-in API harness included, normal instrumentation reports 11 passed, 0 failed, 1 intentionally skipped.
- Explicit real API run: PASS; 2 requests completed and both provider prompts were verified to contain glossary and pronoun locks.
- v4.13 P0 device regression: PASS for fresh SAF output selection (persisted read/write plus write/delete probe), YAML/glossary/pronoun valid state, real Start/preflight/service/request/response/partial output/completed sequence, and cold restart followed by a second real output. JVM unit tests: 99 passed. Connected instrumentation on OnePlus 13R: 12 passed, 0 failed, 1 intentionally skipped real-API test.
- v4.14 archive workflow: PASS. PowerShell parser and `git diff --check` passed; direct `assembleDebug` was blocked; 99 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is 4.14-dev.1/code50; artifact/backup parity, checksum manifest, per-build README/JSON, and 323-entry source ZIP passed.
- Glossary filename-parity focused regression: 6 passed, 0 failed, 0 skipped.
- v4.14-dev.2 full regression/build/archive/install: PASS. 103 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is 4.14-dev.2/code51; artifact/backup parity and all manifest hashes passed.
- Glossary filename-parity device QA: PASS. Exact imported filename, one-term activation, and cold-restart persistence verified; prior user state restored after cleanup.
- Runtime preview/rule-usage focused regression: PASS. Prompt counts are taken from the exact injected plan; snapshot round-trip distinguishes unknown from zero; compact dashboard and preview labels passed.
- v4.14-dev.3 full regression/build/archive/install: PASS. 106 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is 4.14-dev.3/code52; artifact/backup parity and all manifest hashes passed.
- Runtime preview/rule-usage device QA: PASS. Exact fixture values and accepted preview rendered without navigation overlap; original user runtime state was restored byte-for-byte after QA.
- Approved-logo focused regression: PASS. `verifyApprovedLogo` ran through `preBuild`, exact resource/source hashes matched, manifest/header references passed, and 106 JVM tests passed.
- README static verification: PASS. `git diff --check` passed, all six relative documentation links resolve, the obsolete v2.6.7 heading and direct Android Studio APK instructions are absent, and only `README.md` was included in commit `f8649b5`.
- v4.14-dev.4 full regression/build/archive/install: PASS. Approved-logo guard and 106 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is v4.14-dev.4/code53; all artifact/backup hashes match.
- Permanent-logo device QA: PASS. The installed launcher and in-app header display the approved logo, packaged resource and manifest references match, and the original user runtime state was preserved byte-for-byte.
- Exact-release tooling regression: PASS. PowerShell parser returned 0 errors; mutual exclusion, branch matching, and clean-tree guards each rejected the controlled invalid invocation with the expected reason.
- Macrobenchmark build regression: PASS. `compileBenchmarkJavaWithJavac`, `:app:assembleBenchmark`, and `:macrobenchmark:assembleBenchmark` completed successfully; the approved-logo guard also ran for the target variant.
- v4.14/code54 regression: PARTIAL PASS. Approved-logo guard, 106 JVM tests, lint (54 warnings/0 errors), artifact parity, and connected instrumentation (12 passed/1 opt-in skipped) passed; Macrobenchmark failed before measurement with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.
- v4.14/code55 regression: PARTIAL PASS. Approved-logo guard, 106 JVM tests, lint (54 warnings/0 errors), artifact parity, connected instrumentation (12 passed/1 opt-in skipped), signed benchmark installation, and two real traces passed; the five-iteration Macrobenchmark was stopped after the OEM launcher added nearly 10 minutes per setup.
- v4.14/code56 regression: PARTIAL PASS. Approved-logo guard, 106 JVM tests, lint (54 warnings/0 errors), artifact parity, and connected instrumentation (12 passed/1 opt-in skipped) passed. The archived source remained vulnerable to the OnePlus background freezer, so code56 is rejected.
- Corrected working-tree Macrobenchmark diagnostic: PASS. OnePlus CPH2691 / Android 15 completed 5/5 cold starts of `com.ml.tblandroidtxt.MainActivity`, 0 failed/skipped; median time to initial display was 332.01 ms and all five real Perfetto traces plus AndroidX JSON were retained in ignored durable artifact/backup diagnostic stores.
- v4.14/code57 final pre-tag regression: PASS. Approved-logo guard, 106 JVM tests, lint (0 errors/54 warnings), five-file build parity, connected instrumentation (11 passed/1 opt-in skip), controlled output-permission-loss QA, final visuals, and the accepted 5/5 Macrobenchmark rerun all passed.
- v4.15 Settings P0 focused compile/JVM regression: PASS. `:app:compileDebugAndroidTestJavaWithJavac testDebugUnitTest` completed with the approved-logo guard; 106 JVM tests passed with 0 failures/errors/skips.
- v4.15 Settings P0 strengthened device regression: PASS. `V415SettingsPersistenceInstrumentedTest` passed 1/1 on OnePlus CPH2691 / Android 15 with the exact `4.15-dev.1`/code59 build identity and exact preference snapshot equality after restore.
- v4.15-dev.1 archive verification: PASS. Both durable stores contain the same five files; all mirror SHA-256 values match, all four manifest entries verify, lint reports 0 errors/54 warnings, and the APK/source ZIP hashes are `18C36F6A...BCC665`/`D047836E...356`.
- v4.15 Glossary/Pronoun scroll regression: PASS. `V415LibraryScrollInstrumentedTest` passed 1/1 on OnePlus CPH2691 / Android 15 with exact `4.15-dev.2`/code60 identity; exact scroll positions and selected IDs passed for both lists, and all preference snapshots matched after restoration.
- v4.15-dev.2 archive verification: PASS. Both durable stores contain the same five files; every mirror hash matches, all four manifest entries verify, 106 JVM tests pass, lint reports 0 errors/54 warnings, and the APK/source ZIP hashes are `828B4535...DCE7`/`96838283...9C40`.
- v4.15 multi-profile import regression: PASS. Four focused planner tests pass, and `V415MultiProfileImportInstrumentedTest` passes 2/2 with exact `4.15-dev.3`/code61 identity on OnePlus CPH2691 / Android 15.
- v4.15-dev.3 archive verification: PASS. Both durable stores contain the same five files; every mirror hash matches, all four manifest entries verify, 110 JVM tests pass, lint reports 0 errors/53 warnings, and the APK/source ZIP hashes are `5D412996...A1EF7`/`9C7D7978...8EF`.
- v4.15 full automated regression: PASS. Logo guard and 110 JVM tests passed; lint reports 0 errors/53 warnings; full connected instrumentation on OnePlus CPH2691 / Android 15 records 16 total, 15 passed, 1 opt-in paid case skipped, and 0 failures/errors.
- v4.15/code62 exact build verification: PASS. Archive-first build repeated 110 JVM tests and lint, both five-file durable payloads match, all four manifest entries verify, installation succeeded, and device metadata matches `4.15`/code62.
- v4.15/code62 exact release QA: PASS. Connected instrumentation records 16 total/15 passed/1 opt-in paid skip/0 failures or errors; Macrobenchmark passed 5/5 cold starts at 313.89/330.23/335.04 ms min/median/max with five real traces; the 21-file visual, UI, instrumentation, and performance evidence event matches its backup mirror exactly.
- v4.15 PreTag/tag verification: PASS. The PreTag gate accepted completed checklist steps 1-9; `v4.15` is an annotated tag object targeting `d6d844d`.
- v4.15 tag repair: PASS. Corrected release metadata is committed and the fresh annotated tag targets it.
- v4.15 immutable tag archive: PASS. Both 22-file stores match, all 21 manifest entries pass, and the retained source ZIP exactly matches a fresh archive of corrected tag `v4.15`.
- v4.15 BUILD_STATE verification: PASS. Current release identity, code62 APK, corrected tag, QA counts, performance metrics, archive hashes, device state, and known limitations are recorded.
- v4.15 final export verification: PASS. Corrected tag, artifact/backup parity, manifest, APK, source ZIP, release documents, and installed device identity all passed.
- v4.15 merge/publication verification: PASS. Initial release merge `main` and corrected annotated tag/peeled target match the verified GitHub refs.

## G2-C1B1-C1.5-B SQLite v17 implementation snapshot (2026-08-05)

- Current version: `4.16-dev.39` / Android version code `101`; no new APK has been built in this session yet.
- Current branch: `feature/v4.16-g2-c1b1c15b`.
- Current commit: `40ae6b8` (`feat(editorial): add v17 canonical identity models`) is the implementation baseline immediately before this snapshot commit; the v17 migration/DAO changes are staged but not yet committed.
- Current build: archived baseline `4.16-dev.39`/code101, event `build-20260805-124714`, artifact/backup parity previously verified; this session's build is pending.
- Completed tasks: Gate A canonical project revision, scope snapshot, closed-run and lineage-binding models/canonicalizers; focused Gate A `10/10` PASS; SQLite v16→v17 additive migration with exactly five new tables, indexes and immutability triggers; append/read-only DAOs; transaction-scoped v16 lineage append seam and atomic lineage+binding service; JVM regression `:editorial-engine:test` `105/105` and `:app:testDebugUnitTest` `164/164`; Android-test Java compilation PASS; lint `0` errors/`53` warnings; `git diff --check` PASS.
- Pending tasks: commit Gate B, run archive-first `scripts/build-and-save.ps1 -Series 4.16-dev`, verify artifact/backup parity, install exact artifact without clearing data, run isolated v17 instrumentation/restart/immutability/rollback QA, update handoff/checklist/BUILD_STATE, and stop at review.
- Known bugs/limitations: production profile remains non-executable; no production closed-run creator/caller, importer wiring, capability promotion, certification, activation, project binding or execution; `executionEnabled()` remains `false`; device QA for this v17 change has not run yet; real-data continuity is not claimed until an existing production DB is observed.
- Regression status: Gate A PASS; Gate B JVM/lint/compile PASS; device/build/archive evidence pending. Protected `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` remain user-owned, untouched by this work, unstaged and uncommitted.
- Next step: commit Gate B, then archive-first build and isolated device v17 QA; do not start C2 or production caller wiring.

## G2-C1B1-C1.5-B finalization snapshot (2026-08-05)

- Current version: `4.16-dev.41`/code103, event `build-20260805-183201`.
- Current branch: `feature/v4.16-g2-c1b1c15b`.
- Current commit: `4cd50ba34b1a64749290c51c90fe3a6fc65e91ed` is the implementation baseline immediately before this snapshot update; final documentation/state is being committed separately, and `.idea/*` remains protected/unstaged.
- Current build: final APK `artifacts/builds/v4.16-dev.41/build-20260805-183201/TranslateBooks-v4.16-dev.41-code103.apk`; immutable mirror `backup/builds/v4.16-dev.41/build-20260805-183201/`; APK SHA-256 `F9935E2CC4BE5B863A14DA194ED7073E76853A186E1877F5FAC6EE548285252`; source ZIP SHA-256 `183E6B4DF79EA8F03CCA9536868EDDF92623088D55EE40CD53C4535E55C0D11B`; five-file artifact/backup parity PASS.
- Completed tasks: v17 schema/DAO/transaction implementation; engine `105/105`, app `164/164`, Android-test compile, lint `0/53`; device v17 `8/8`, lineage `11/11`, v15 evaluation `9/9`; total device `28/28`.
- Pending tasks: commit final documentation/state, verify final HEAD/status/diff-check, and stop at review. No implementation work remains in this approved scope.
- Known bugs/limitations: production profile remains non-executable; no production identity creator/caller, importer wiring, capability promotion, certification, activation, binding or execution; production `databases/` was empty at inspection, so real-data continuity remains not claimed; `executionEnabled()` remains false.
- Regression status: PASS for JVM/lint/archive/device isolated scope. `.idea/*` is untouched, unstaged and uncommitted.
- Next step: review this G2-C1B1-C1.5-B handoff and separately approve any future C2; do not start C2 automatically.

## G2-C1B1-C2-A creator/resolver finalization snapshot (2026-08-05)

- Current version: `4.16-dev.42`/code104, event `build-20260805-190244`.
- Current branch: `feature/v4.16-g2-c1b1c2a`.
- Current commit: `183f47455093046fe991a9abd95fb2bbed57d951` is the implementation/build baseline immediately before this snapshot update; final documentation/state is committed separately by policy.
- Current build: `artifacts/builds/v4.16-dev.42/build-20260805-190244/TranslateBooks-v4.16-dev.42-code104.apk`; backup mirror `backup/builds/v4.16-dev.42/build-20260805-190244/`; APK SHA-256 `961D4DDF531EF3703EDFDFB3DF55BCBF26CFC8BF512076B25D63DB55AAC01A7B`; source ZIP SHA-256 `BE80D9D197AAA7493AAFC4DE9C5D554CEF8DF7C37EC8BF4A632268FFE36E9266`; five-file parity PASS.
- Completed tasks: explicit immutable creator requests; v17 project/scope append through canonical models; closed-run closure with DAO-owned ordinal and re-read pack/evaluation/trusted facts; default bundled resolver blocks non-executable production profile; SQLite C1 context resolver, exact parent lookup and read-only retention preflight. Engine `105/105`, app `164/164`, lint `0/53`, isolated device C2-A `5/5` PASS.
- Pending tasks: no implementation work remains in C2-A. Commit final documentation/state, verify final status, then stop at review.
- Known bugs/limitations: production profile remains non-executable; no production closed-run has been created; no importer/startup/UI caller, lineage append, run-lineage binding, certification, activation, project binding, capability promotion or execution exists. Retention preflight is read-only; deletion UX wiring belongs to separately approved C2-B or later. `executionEnabled()` remains false. Device production database was empty, so real-data continuity is not claimed.
- Regression status: PASS. Protected `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` remain untouched by this phase, unstaged and uncommitted.
- Next step: separately review and approve G2-C1B1-C2-B for production caller wiring at RUN_CONTEXT_CLOSED; do not start it automatically.

## G2-C1B1-C2-B0 production caller boundary plan (2026-08-06)

- Current version: `4.16-dev.42` / code104; no APK was built in B0.
- Current branch: `feature/v4.16-g2-c1b1c2b0`.
- Current commit: `ddec1acebec448f7f206142cd011dfa633a91816` is the implementation baseline immediately before this B0 documentation/state commit; no production code or database implementation changed. The protected `.idea/compiler.xml`, `.idea/gradle.xml`, and `.idea/misc.xml` remain user-owned, untouched, unstaged and uncommitted.
- Current build: accepted `4.16-dev.42`/code104, event `build-20260805-190244`; APK SHA-256 `961D4DDF531EF3703EDFDFB3DF55BCBF26CFC8BF512076B25D63DB55AAC01A7B`; source ZIP SHA-256 `BE80D9D197AAA7493AAFC4DE9C5D554CEF8DF7C37EC8BF4A632268FFE36E9266`; artifact/backup parity remains PASS.
- Completed tasks: source audit of project/chapter/input/run/import/startup/retry/delete lifecycle; evidence that production `RUN_CONTEXT_CLOSED` is absent and no production `editorial_runs` INSERT/creator exists; boundary plan with one future caller proposal, state machine, Option-B atomicity/recovery matrix, exact ROOT/CHILD selection, retention contract, compatibility blocker, and C2-B1/B2 split.
- Pending tasks: review/approve C2-B1 coordinator plan; identify/build a real production run lifecycle and explicit `RUN_CONTEXT_CLOSED` event before any caller wiring. No C2-B1/B2, capability promotion, certification, activation, project binding, or execution is authorized by B0.
- Known bugs/limitations: current production profile remains non-executable and only declares `pack.integrity.sha256.v1`; `EditorialSafe4Pack.executionEnabled()` remains false; no production closed-run/lineage/binding may be created; retention preflight is read-only and delete UX wiring is deferred; real-data continuity is not claimed from this plan-only phase.
- Regression status: Wrapper/JBR read-only `:editorial-engine:test` `105/105`, `:app:testDebugUnitTest` `164/164`, failures/errors/skips `0/0/0`; `git diff --check` PASS. No APK, instrumentation, lint, migration, database row, or D: candidate activity.
- Next step: review `EDITORIAL_PACK_PLATFORM_G2_C1B1C2B0_PRODUCTION_CALLER_BOUNDARY_PLAN.md`; exactly one proposed next implementation step is `G2-C1B1-C2-B1` after approval, and it must stop if the real lifecycle event is still absent.

## G2-C1B1-C2-B1 lineage coordinator and recovery (2026-08-06)

- Current version: `4.16-dev.45` / code107, event `build-20260806-090226`.
- Current branch: `feature/v4.16-g2-c1b1c2b1`.
- Current commit: `616a0184a2df2462c3adca316f59cbd70e7d88b9` is the implementation baseline immediately before the B1 source group commit; final source/documentation commits are separate. The three protected `.idea/*` files remain user-owned, unstaged and uncommitted.
- Current build: `artifacts/builds/v4.16-dev.45/build-20260806-090226/TranslateBooks-v4.16-dev.45-code107.apk`; backup mirror is `backup/builds/v4.16-dev.45/build-20260806-090226/`; APK SHA-256 `F0A8971AA9E8A4D08CF08A63857BC37F17BD6DEFE264CE4F2813DAAF8A1211A9`; archive-first provenance and parity PASS.
- Completed tasks: immutable closure-event/command/result/status contracts; production resolver fail-closed; Option B coordinator; exact ROOT/CHILD parent lock; injected clock; replay/recovery; shared transaction invocation; exact readback; isolated focused device B1 `7/7` PASS.
- Pending tasks: commit final handoff/state/checklist, verify final HEAD/status and stop at review. No production caller or C2-B2 work is pending within this approved phase.
- Known bugs/limitations: full connected suite has three unrelated failures described in BUILD_STATE; production `RUN_CONTEXT_CLOSED` remains absent; production profile only has `pack.integrity.sha256.v1`; no production closed-run/lineage/binding; `executionEnabled()` remains false; `REAL_DATA_CONTINUITY: NOT_CLAIMED`.
- Regression status: JVM `269/269` pass, 0 failures/errors/skips; Android-test Java compilation PASS; lint 0 errors/53 warnings; diff-check PASS; focused exact APK/device B1 `7/7` PASS; global connected suite not clean due unrelated failures.
- Next step: review `EDITORIAL_PACK_PLATFORM_G2_C1B1C2B1_LINEAGE_COORDINATOR.md`; resolve/approve the unrelated connected-suite blocker before separately considering C2-B2. Do not start C2-B2 automatically.

## Next step

`G2-C0C-B2`, `G2-C1B1-A`, `G2-C1B1-B`, `G2-C1B1-C0`, `G2-C1B1-C1`, `G2-C1B1-C1.5-A`, `G2-C1B1-C1.5-B`, `G2-C1B1-C2-A` and `G2-C1B1-C2-B0` remain PASS / REVIEW STOP. G2-C1B1-C2-B1 is IMPLEMENTED but BLOCKED for global PASS by unrelated connected-suite failures. The exact next action is review this B1 handoff and resolve that blocker; do not start C2-B2 automatically.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
