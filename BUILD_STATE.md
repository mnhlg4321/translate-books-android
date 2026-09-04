# Build State

- Status: P5C_DOCUMENTATION_BASELINE_CONSISTENT / P5_DRY_RUN_ONLY / LIVE_AUTHORIZATION_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Active authority: V5-SAFE.4.1.3-FULL; canonical plan `EDITORIAL_RECOVERY_V4_18.md`.
- Workspace: D:\App Translate Books\App Translate Books-translation-profile.
- Branch: feature/v4.18.
- Current commit baseline: 44e5a659f23da1314cd6294a87d1cc44d1a58686, the exact P5 dry-run implementation/docs baseline immediately before this P5C.0 documentation update; not self-referential.
- Current device build: 4.17-dev.1 / Android versionCode 169 on device 15e84958. P5 dry-run validation build 4.17-dev.9 / code177 was archived and used only for validation, then the target package was restored to code169.
- Current phase: P5C.0 documentation baseline is consistent; exact-binding fake E2E is the next task. No live authorization was supplied. Editorial execution, provider/API calls, real chapter pilot and certification remain disabled.

## Source and authority identity

- P0/P1 checkpoint: aef7da1.
- P2/P3A incoming checkpoint: b5589bf5f2b3b60942841950c5877e6f8281f7ff.
- P4 starting checkpoint: 270759e5589b2e9101c3e1a5a6b84cff12ec2fd3.
- Canonical ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip`; 23,638 bytes; SHA-256 `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`.
- Java control ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip`; 23,418 bytes; SHA-256 `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`.
- Project authority: 9,485 bytes; SHA-256 `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD`.
- Prompt authority: 8,852 bytes; SHA-256 `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F`.
- Workflow authority: 34,917 bytes; SHA-256 `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730`.
- Canonical pack hash: `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d`.

## Trusted profile

- Profile: `com.ml.tblandroidtxt.editorial.engine.safe4.full / 2.0.0`.
- Contract: `safe4.full.three-pass.v1`.
- Receipt schema: `safe4.full.receipt.v1`.
- Profile resource SHA-256: `1b2db011d59f3e2ef4349aeb0daa9c54a19b7efd1e2ca6886bc29b56e4690d62`.
- Canonical profile hash: `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21`.
- Machine contract fingerprint: `a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3`.
- Implemented P3B capabilities: exact 11/11 with owner, positive/negative test, fingerprint and source commit.
- Explicitly missing P3B capabilities: none.
- Profile sourceCommit: `3156835d1cc6b723d7932709224bf626dc7a1747`.
- executionEnabled: false; automatic replacement and project rebind: false.

## Completed evidence

- P2A/P2B and P3A GAP-012 complete: canonical/control import, immutable byte readback, idempotent re-import, 4.1.4 side-by-side and security negatives pass.
- P3B complete: P01-P09 `9/9`, G1-G24 `24/24`, typed stop/recovery, PRESERVE_DRAFT, receipt/ledger/diff/QA/release validators and trusted profile acceptance.
- P5 dry-run boundary: `EditorialP5PilotExecutionBoundaryTest` `15/15`; bounded authorization, exact binding checks, preflight-before-provider, phase projection, typed truncation/repair/recovery, local ledger/diff/receipt validation, token budget, idempotency and atomic-store failure are covered with an injected fake provider. No real provider or chapter was used.
- P4 characterization documented the legacy hard-coded project owner and the v18 tuple/source identity boundary. Additive v19 P4 tables were introduced only after that failing persistence evidence.
- P4 selection/binding: explicit exact pack selection, immutable tuple, app-computed source identities, atomic project/revision/scope/declaration/binding transaction, idempotent retry and collision rejection.
- P4 resume: side-by-side canonical 4.1.3/synthetic 4.1.4, exact DB close/reopen readback, activity recreation UI proof, two-invocation host process-stop proof and stale-chain fail-closed checks.
- P4 post-closure correction: `EditorialP4BindingTransactionService` and the P4 UI now write only the contract vocabulary `NORMAL_FOUR_SOURCE`/`ALTERNATE_EXPLICIT`; the prior `NORMAL`/`ALTERNATE` mismatch was caught by a new canonical-mode assertion and fixed in commit `364faa4`.
- P4 focused device evidence on 15e84958: importer `13/13`, P1 `7/7`, P2 `3/3`, P3B trusted profile `1/1`, runtime wiring `5/5`, P4 binding `4/4`, process-stop preparation/resume `1/1 + 1/1`, UI recreation `3/3`.
- Full device instrumentation: `112 total`, `111 PASS`, `1 approved real-API skip`, `0` failures. P3B baseline was 104; P4 adds eight tests (two characterization, four binding/atomicity, two process-death). The activity test correction changes behavior only, not count.
- Host engine suite: `178/178 PASS` (baseline `163`; P5 added `15` tests).
- App unit suite: `210/210 PASS`.
- P5 validation APK: `artifacts/builds/v4.17-dev.9/build-20260904-193721/TranslateBooks-v4.17-dev.9-code177.apk`, SHA-256 `8B4D4714287114013A43C181C232921ABC6D6BA43CFB34F5E87D6CABF34001FA`; matching backup archive; device suite `112/112` with one approved real-API assumption skip and `0` failures.
- External qualification `TESTS/test_full_release.ps1`: `306 PASS / 0 FAIL`.
- Profile and canonical pack verification: PASS. Provider/API calls: `0`; real API skipped by explicit opt-in. Device restored to code169 after validation APK uninstall/reinstall because `pm clear` was rejected.
- `git diff --check`: PASS at closure.

## Validation artifact

- Latest validation APK: `artifacts/builds/v4.17-dev.9/build-20260904-193721/TranslateBooks-v4.17-dev.9-code177.apk`.
- Backup mirror: `backup/builds/v4.17-dev.9/build-20260904-193721/TranslateBooks-v4.17-dev.9-code177.apk`.
- APK SHA-256: `8B4D4714287114013A43C181C232921ABC6D6BA43CFB34F5E87D6CABF34001FA`.
- Build event source snapshot: `d0de39cf2ea22117303f299b522048a371372679`; it contains the P5 dry-run implementation/test baseline.
- This is a validation APK, not a V4.18 release build.
- Historical P4 correction validation remains immutable and is recorded in
  `docs/P4_VALIDATION_REPORT.md` as code176; it is not the current validation
  artifact.

## Allowed-change guard

P4 production changes are limited to the P4 binding/selection/service/DAO
owners, the additive v19 migration, read-only project projection/setup UI and
the engine P4 value types. P5 production changes are limited to engine-local
pilot contract/result/request/authorization boundary types and the injected
fake-test seam; no app provider adapter or persistence wiring was added. Test
changes are AndroidTest/engine-test only.
There was no provider/API wiring, execution protocol, certification state,
authority byte, canonical ZIP, trusted profile resource, build metadata or
legacy workflow rewrite. The original workspace `D:\App Translate Books` was
not modified.

## Known limitations

- P4 creates setup metadata only. It does not run L1/L2/L3, call a provider,
  create a model request, certify a pack or open Run/Start.
- The setup UI collects explicit initial source text for the metadata fixture;
  this is not source certification and is not an execution UI.
- `DATA_COMPATIBLE`, selectable and `PILOT_SETUP_READY` do not mean runnable.
- Bootstrap profile v1 remains loadable and non-executable.
- P5 live authorization is not present; no real chapter/provider call is permitted. Dry-run/fake-provider contract work may proceed only with external execution disabled.
- P5 dry-run does not prove live provider behavior, app database attempt persistence, cancellation/process-death handling during a real call, data-egress approval, or a real `REPORT_L1`/receipt commit. The supplied authorization block is still required before any provider access.

## Next step

After the P5C.0 documentation gate, select one exact persisted P4 binding and
add the failing exact-binding fake E2E contract before any provider work.
Obtain the complete `P5 PILOT AUTHORIZATION` block before any live call. Keep
`EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE`; do not open L2/L3,
auto-activate, auto-rebind or certify from this dry-run evidence.

This file is current-only; Git history preserves prior state.
