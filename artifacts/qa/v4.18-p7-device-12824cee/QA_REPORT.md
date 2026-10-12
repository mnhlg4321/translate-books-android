# v4.18 P7 development QA — reviewed source `12824cee`

Date: 2026-10-12 (+07:00)

## Scope and disposition

- Owner disposition for checklist section C is confirmed: historical FAIL remains FAIL, with no retroactive PASS and no waiver. The original code196 pilot-data preservation failure is retained as historical evidence.
- Current-scope evidence is the Editorial chunk path only. It is separate from the frozen V5/pilot gates and does not certify those gates.
- No provider call, pilot operation, tag, merge, release archive, or push was performed.

## Exact source and artifacts

- Reviewed implementation source: `12824cee6c6f4e9f9482efaf6fe398799d5732c0`.
- Isolated build worktree: `D:\P7-builds\wt-p7-20261012` on temporary local branch `codex/p7-build-20261012`; worktree is clean after the build.
- Production wrapper: `scripts/build-and-save.ps1 -Series 4.18-p7 -MinimumVersionCode 264 -Install -DeviceSerial emulator-5554`.
- Production: `4.18-p7.1`, Android versionCode `264`, APK SHA-256 `64A9439D42E3979BA4404E847610F25CEBEE74B34279542D318B6CD77CEC661D`.
- Production source ZIP SHA-256: `F0D553C6481DFE90DE95C7359168E2D6688FBC5600D3A7D5526799250179EB84`.
- Production payloads are byte-verified in both `artifacts/builds/v4.18-p7.1/build-20261012-143618/` and `backup/builds/v4.18-p7.1/build-20261012-143618/`.
- AndroidTest wrapper event `p7-test-20261012-143800`; test APK SHA-256 `286A1B2F2DECA676C57B3242D412A19D9EB1CDF7E97B14CA226BAAD36699AFD1`.
- AndroidTest source ZIP SHA-256 is the same `F0D553C6481DFE90DE95C7359168E2D6688FBC5600D3A7D5526799250179EB84`; test payloads are byte-verified in both `artifacts/test-builds/v4.18-p7.1/p7-test-20261012-143800/` and `backup/test-builds/v4.18-p7.1/p7-test-20261012-143800/`.
- Production and test certificates are `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.

The production wrapper ran `testDebugUnitTest`, `lintDebug`, and `assembleDebug` successfully. The test-only wrapper ran `:app:assembleDebugAndroidTest` successfully. Both archives were completed before installation; the AndroidTest APK was then installed explicitly with `adb -s emulator-5554 install -r`.

## Device and current chunk QA

Device preflight/readback: only `emulator-5554` was ready; production readback is `4.18-p7.1`/code `264`; both package signature tokens are `abebea4b`; no package clear/reset was used. Raw per-method logs are private at `D:\P7-builds\p7-qa-20261012\` and contain synthetic QA data only.

Current Editorial chunk scope passed:

| Group | Result |
|---|---:|
| `EditorialPairStoreInstrumentedTest` | 6/6 |
| `EditorialPairAddressKeepSqliteInstrumentedTest` | 1/1 |
| `EditorialApiStoreInstrumentedTest` | 6/6 |
| `EditorialApiBienTapFlowInstrumentedTest` | 3/3 |
| `EditorialChunkPlanPerformanceInstrumentedTest` | 1/1 |
| Pair UI normal methods | 3/3 |
| BienTap UI normal methods | 2/2 |
| Pair UI restart: seed → force-stop → verify → cleanup | 1/1 each phase |
| BienTap UI restart: seed → force-stop → verify → cleanup | 1/1 each phase |
| Product/Translation smoke classes | 15/15 |

The UI flow exported TXT through the same export callback used by the file-picker result, then read the UTF-8 file back and compared its hash to the stored final text. Stored results reopened after the forced process stop without a provider call. The fake/offline providers in these tests are test doubles; this is plumbing/readback evidence, not semantic model-quality evidence.

## Frozen V5/pack remeasurement

The frozen allowlist executed `106/117` tests successfully. The remaining `11` failures are all the same historical schema signature and are not chunk failures:

| Class | Failing methods | Signature |
|---|---:|---|
| `EditorialLineagePersistenceInstrumentedTest` | 2 | `expected:<27> but was:<29>` |
| `EditorialP4BindingCharacterizationInstrumentedTest` | 1 | `expected:<27> but was:<29>` |
| `EditorialPackCompatibilityEvaluationInstrumentedTest` | 3 | `expected:<27> but was:<29>` |
| `EditorialV17IdentityStoreInstrumentedTest` | 2 | `expected:<27> but was:<29>` |
| `EditorialV18RunDeclarationEventInstrumentedTest` | 3 | `expected:<27> but was:<29>` |

The reviewed production source has `TranslationRepository.VER = 29`, while these frozen tests assert schema version 27. This is retained as a historical frozen-track FAIL with method/message evidence; no schema or frozen V5 code was changed to make the run green. Provider/live classes were excluded by the P7 allowlist.

## Result

`P7_CURRENT_CHUNK_SCOPE=PASS`

`P7_FROZEN_HISTORICAL_FAILURES=RETAINED_FAIL_11_SCHEMA_27_VS_29`

`P7_RELEASE_CANDIDATE=NOT_CLAIMED`

Performance evidence and `archive-release.ps1` release payload remain outside this development QA package and are required only before a release candidate/tag. Checklist boxes remain unchanged; no tag, merge, release, or push was performed.
