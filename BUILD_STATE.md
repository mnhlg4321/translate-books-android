# Build State

- Status: P3B_COMPLETE / PACK_READY_FOR_CERTIFICATION / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE.
- Active authority: EDITORIAL_RECOVERY_V4_18.md; editorial authority V5-SAFE.4.1.3-FULL.
- Workspace: D:\App Translate Books\App Translate Books-translation-profile.
- Branch: feature/v4.18.
- Current commit baseline: a58090f6ece78f1aeb224c1bd249449a23c49ed5, the exact implementation/test baseline immediately before this documentation closure; not self-referential.
- Current device build: 4.17-dev.1 / Android versionCode 169 on device 15e84958. The P3B validation build 4.17-dev.3 / code171 was archived, used only for device validation, and removed from the device.
- Current phase: P3B trusted runtime contract/profile is complete as deterministic contract and compatibility evidence. Editorial execution, real chapter pilot and certification remain disabled.

## Source and authority identity

- P0/P1 checkpoint: aef7da1.
- P2/P3A incoming checkpoint: b5589bf5f2b3b60942841950c5877e6f8281f7ff.
- Canonical ZIP: app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip; 23,638 bytes; SHA-256 B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987.
- Java control ZIP: app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip; 23,418 bytes; SHA-256 44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5.
- Project authority: 9,485 bytes; SHA-256 1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD.
- Prompt authority: 8,852 bytes; SHA-256 D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F.
- Workflow authority: 34,917 bytes; SHA-256 5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730.
- Canonical pack hash: 497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d.

## P3B profile

- Profile: com.ml.tblandroidtxt.editorial.engine.safe4.full / 2.0.0.
- Contract: safe4.full.three-pass.v1.
- Receipt schema: safe4.full.receipt.v1.
- Profile resource SHA-256: 1b2db011d59f3e2ef4349aeb0daa9c54a19b7efd1e2ca6886bc29b56e4690d62.
- Canonical profile hash: beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21.
- Machine contract fingerprint: a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3.
- Implemented capabilities: exact 11/11 required P3B IDs, each with owner, positive/negative test, fingerprint and source commit.
- Explicitly missing capabilities: none.
- sourceCommit in profile: 3156835d1cc6b723d7932709224bf626dc7a1747.
- executionEnabled: false; automatic replacement and project rebind: false.

## Completed evidence

- P2A/P2B and P3A GAP-012 remain complete: canonical/control import, immutable byte readback, idempotent re-import, synthetic 4.1.4 side-by-side, and security negatives pass.
- P3B source preflight P01-P09: 9/9 PASS.
- P3B deterministic replay G1-G24: 24/24 PASS.
- Typed stop/recovery, PRESERVE_DRAFT, bundle/visibility, source-status, receipt, ledger, diff, QA and release validators: PASS.
- Host engine suite: 161/161 PASS.
- App unit suite: 210/210 PASS.
- Profile asset verification: PASS.
- External qualification TESTS/test_full_release.ps1: 306 PASS / 0 FAIL.
- Device focused tests on 15e84958: importer 13/13, P1 7/7, P2 3/3, P3B trusted profile 1/1, runtime wiring 5/5.
- Full device instrumentation: 104 total, 103 PASS, 1 approved real-API skip, 0 failures. Suite increase from 103 to 104 is the new P3B trusted-profile test.
- Provider/API calls: 0. Real API test: skipped by explicit opt-in.
- git diff --check: PASS at closure.

Validation APK archive:
artifacts/builds/v4.17-dev.3/build-20260903-193024/TranslateBooks-v4.17-dev.3-code171.apk
SHA-256 034F33485352AC4C019C3363B4666C880CD036AF6CB1550DDADCB5ABF0923A33.

## Allowed-change guard

P3B production changes are restricted to deterministic Safe4 contract,
validator, evidence-catalog, profile-factory, resolver and registry owners in
editorial-engine/src/main, plus app/build.gradle profile asset
verification/bundling. Test setup changes are AndroidTest-only. No
app/src/main, database schema, UI, provider/API wiring, project/run binding,
authority bytes, canonical ZIP, GAP-012 importer or build metadata changed.
The original workspace D:\App Translate Books was not modified.

## Known limitations

- No provider/API execution, real chapter L1/L2/L3, benchmark, certification,
  project/run binding or persistence was performed.
- DATA_COMPATIBLE means trusted contract compatibility only.
- PACK_READY_FOR_CERTIFICATION does not mean CERTIFIED or RUNNABLE.
- Bootstrap profile v1 remains loadable and non-executable.

## Next step

Do not open Editorial execution from this state. The next separately approved
phase is P4 binding/resume contract work or a controlled L1 pilot, with new
failing tests for that scope. Keep NOT_CERTIFIED and NOT_RUNNABLE until real
certification evidence exists.

This file is current-only; Git history preserves prior state.
