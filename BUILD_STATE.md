# Build State

- Status: `V4.18_P1_CHARACTERIZATION_COMPLETE_WITH_RUNTIME_GAPS / P2_REFERENCE_PACK_PENDING`.
- Active authority: `EDITORIAL_RECOVERY_V4_18.md`.
- Current phase: `P0_DOCS` and `P1_CHARACTERIZATION` evidence complete, including Android runtime; P1 is closed with GAP-012 and documented baseline schema-test failures.
- Current version/build: latest verified artifact remains `4.17-dev.1` / Android versionCode `169`; no v4.18 APK has been built, installed or claimed.
- Active branch/worktree: `feature/v4.18` / `D:\App Translate Books\App Translate Books-translation-profile`.
- Source baseline: device-verified v4.17 HEAD `921af9256e1b1fe4ab9ac113affa98eec7a1e339`.
- Current commit baseline: `921af9256e1b1fe4ab9ac113affa98eec7a1e339`, the exact clean implementation/device-QA baseline before the P0 documentation changes; not self-referential.
- Baseline build: `TranslateBooks-v4.17-dev.1-code169.apk`, 2,784,446 bytes, SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`; event `build-20260829-185818`; artifact/backup/source ZIP parity remains preserved.
- Editorial authority selected: `V5-SAFE.4.1.3-FULL` from `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE`.
- Authority hashes: Project `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD`; Prompt `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F`; Workflow `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730`.
- P0 completed: moved v4.17 worktree repaired through Git and verified clean; owner-selected continuation branch created; external release inventoried; static release qualification rerun `306 PASS / 0 FAIL`; existing manifest/import/storage/compatibility platform audited; reuse-first Pack Manifest v1 contract, implementation map, release checklist and README prepared.
- P1 evidence completed: test-only four-root-entry 4.1.3 fixture generated with exact authority bytes; manifest and compatibility characterization added; import/integrity/storage/future-pack AndroidTest source added; false-block JVM characterization added; engine tests `123/123`, app unit tests `210/210`, AndroidTest Java compile PASS; Android P1 class `5/7` PASS with GAP-012 recorded; full instrumented suite `91/100` PASS with seven pre-existing v17/v18 schema-test failures. No production source was changed.
- P1 test action: after explicit permission, immutable baseline APK code169 and separate AndroidTest APK were installed on device `15e84958`/API 35; no v4.18 release APK or build metadata was created.
- Pending: P2 canonical reference pack; P3 minimal proven runtime changes/trusted profile and GAP-012 importer hardening; P4 binding/resume UX; P5 real L1 pilot; P6 L2/L3; P7 regression/build/device QA/release gates.
- Known limitations: external 4.1.3 evidence is structural only; real chapter pilot/model benchmark/provider execution are not run. CODE169 engine profile trusts only pack integrity, old bundled V5-SAFE.4 remains historical, and Editorial execution remains disabled. GAP-012 shows the current importer rejects the host-validated authority ZIP as `TRUNCATED_STREAM`; future no-rebuild support applies only to packs compatible with installed contract/capabilities.
- Regression status: preserved v4.17 evidence is full JVM `205/205`, Lint `0 errors/53 warnings`, artifact parity and device QA PASS. Current v4.18 engine unit `123/123` and app unit `210/210` PASS; P1 Android class `5/7` PASS and full instrumented suite `91/100` PASS on `CPH2691`/API 35. Seven full-suite failures are existing schema assertions expecting v17 while `TranslationRepository.VER=18`; two P1 failures are GAP-012. External 4.1.3 `TESTS/test_full_release.ps1` rerun passed `306/306`.
- Protected state: original workspace `D:\App Translate Books` remains on `feature/v4.16-d1-code113-baseline` with user-owned `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` changes; it was not reset, staged or modified.
- Exact next action: begin P2 with a canonical reference-pack decision for GAP-012; keep production changes deferred to P3 and only behind the failing Android test. P1 evidence is closed, but 4.1.3 remains uncertified/unrunnable.

This file is current-only; Git history preserves prior state.
