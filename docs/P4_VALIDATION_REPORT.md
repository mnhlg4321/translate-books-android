# P4 — Binding and Resume Validation Report

Ngày chốt: `2026-09-04` (+07:00)

## Kết luận

P4 đã hoàn tất lớp setup/binding metadata cho pack Editorial theo project.
Canonical 4.1.3 và synthetic compatible 4.1.4 được chọn riêng, lưu side-by-
side và resume bằng exact persisted identity. Stale chain, collision và lỗi
transaction đều fail-closed; không có provider/API call, execution hoặc
certification.

Trạng thái chốt:

    P4_COMPLETE
    BINDING_RESUME_VERIFIED
    PILOT_SETUP_READY
    EXECUTION_DISABLED
    NOT_CERTIFIED
    NOT_RUNNABLE

`DATA_COMPATIBLE` chỉ cho phép chuẩn bị pilot setup. Nó không có nghĩa pack
đã runnable hoặc certified.

## Baseline và entry gate

| Hạng mục | Bằng chứng |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| P4 starting commit | `270759e5589b2e9101c3e1a5a6b84cff12ec2fd3` |
| P4 implementation/test head before original docs closure | `76348b38174cdc6e25ce4ce19000b75984d44f75` |
| Original P4 documentation closure | `8d676c336d8011ab1534d1d5528cb52171c11bbf` |
| Post-closure P4 producer correction | `364faa42e7ed6bb08b75dda7fdc7335b8f931df7` |
| P4 correction documentation closure | `16073c6285c5b31b13f25929ad7a774b5044d009` |
| App baseline/device restore | `4.17-dev.1 / code169`, device `15e84958` |
| Incoming profile | `com.ml.tblandroidtxt.editorial.engine.safe4.full / 2.0.0` |
| Schema decision | additive v18→v19 after failing P4 persistence characterization; no legacy backfill |
| Entry worktree | clean; branch and HEAD verified before implementation |

Canonical and profile identity were re-hashed during validation:

| Artifact | SHA-256 |
|---|---|
| Canonical 4.1.3 ZIP | `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java control ZIP | `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Profile v2 resource | `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` |
| Canonical pack hash | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Machine contract fingerprint | `a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3` |

Authority bytes were not changed. The three authority SHA-256 values remain
project `1727AE...A26AD`, prompt `D25757...CD754F` and workflow
`5DB6B4...05730`, with lengths `9,485`, `8,852` and `34,917` bytes.

## Implemented P4 owners

| Owner | P4 responsibility | Evidence |
|---|---|---|
| `EditorialP4Binding` / `EditorialP4SourceIdentity` | Immutable canonical tuple and app-computed source facts | Engine P4 characterization; exact readback assertions |
| `EditorialPackSelectionPolicy` / `EditorialPackSelectionCandidate` | Exact explicit selection; filters storage, integrity, trusted profile, machine and capabilities | P4 side-by-side and blocked-pack tests |
| `EditorialP4BindingTransactionService` | Project revision + scope snapshot + run declaration + binding atomic setup; exact resume/stale checks | P4 binding, drift, collision and process-death tests |
| `EditorialP4BindingDao` | Append/read-only binding and child source identities; immutable readback and idempotent collision classification | P4 acceptance and rollback tests |
| `EditorialMigrationSpec.from18To19` / `TranslationRepository` | Additive v19 tables, restrictive foreign keys, immutable triggers | P4 schema characterization and full Android suite |
| `EditorialRepository` / `EditorialPageFactory` / `MainActivity` | Read-only P4 project projection, explicit project-scoped setup UI and execution lock | UI recreation test, repository chapter rejection, device acceptance |

The legacy `EditorialRepository.createProject()` remains hard-coded to the
legacy V5-SAFE.4 marker. Imported P4 packs go through the new explicit service;
the legacy path is not silently repurposed.

## Binding and resume acceptance

- Canonical 4.1.3 and synthetic 4.1.4 are imported and resolved as two
  selectable candidates with different canonical hashes.
- Project A pins 4.1.3 and project B pins 4.1.4. Re-import does not rewrite
  either binding, and retrying the same selector/facts returns `ALREADY_EXISTS`.
- Binding readback retains pack/profile/evaluation/contract/source identities,
  source mode/status/provenance, phase `L1_SOURCE_PREFLIGHT`, ordinal and the
  execution/certification locks.
- Database close/reopen restores exact bindings. The two-invocation device
  proof executes preparation, host process stop and exact selector resume.
- `ActivityScenario.recreate()` leaves the management surface read-only and
  exposes no global Activate/Replace/Run control.
- Source byte drift and stored pack byte drift return `STALE_CHAIN`; neither
  updates the project nor falls back to a latest pack.
- A blocked unknown-capability pack is visible to management but not selectable;
  it returns `ENGINE_UPGRADE_REQUIRED` at import and `PACK_NOT_SELECTABLE` at
  P4 setup.
- A forced final append failure returns `PERSISTENCE_FAILURE` and leaves zero
  project, revision, scope, declaration, binding or binding-input rows.
- Every P4 binding has `executionAllowed=false`, `NOT_CERTIFIED`; no chapter or
  provider request can be opened by the P4 path.

## Test commands and results

Host engine suite:

```text
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :editorial-engine:test --no-daemon
163/163 PASS
```

App unit suite:

```text
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\ADMIN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest --no-daemon
210/210 PASS
```

External static qualification:

```text
& 'D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\TESTS\test_full_release.ps1'
PASS=306 FAIL=0 OLD_WORDS=5308 NEW_WORDS=7050 EXACT_RETAINED=223/311
```

Focused device commands used the connected allowed device `15e84958` (CPH2691,
API 35) and runner
`com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner`:

| Scope | Result |
|---|---:|
| `EditorialPackImportServiceInstrumentedTest` | `13/13 PASS` |
| `EditorialP1PackImportCharacterizationInstrumentedTest` | `7/7 PASS` |
| `EditorialP2ReferencePackImportInstrumentedTest` | `3/3 PASS` |
| `EditorialP3BTrustedProfileInstrumentedTest` | `1/1 PASS` |
| `EditorialPackRuntimeWiringInstrumentedTest` | `5/5 PASS` |
| `EditorialP4BindingInstrumentedTest` | `4/4 PASS` |
| P4 process-death preparation | `1/1 PASS` |
| P4 process-death exact resume after host `am force-stop` | `1/1 PASS` |
| `EditorialPackManagementPageInstrumentedTest` with real `recreate()` | `3/3 PASS` |
| Full Android instrumentation | `112 total = 111 PASS + 1 approved real-API skip; 0 failures` |

Full-suite count changed from the P3B baseline `104` to `112` (`+8`): two P4
characterization tests, four P4 binding/atomicity tests and two process-death
tests. The activity test was corrected to use `recreate()` without changing
test count. The real API test remains skipped by explicit opt-in; provider/API
calls were `0`.

Process-death proof commands:

```text
adb -s 15e84958 shell am instrument -w -r -e class "com.ml.tblandroidtxt.EditorialP4ProcessDeathInstrumentedTest#a_prepareExactBindingForHostProcessStop" com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner
adb -s 15e84958 shell am force-stop com.ml.tblandroidtxt
adb -s 15e84958 shell am instrument -w -r -e class "com.ml.tblandroidtxt.EditorialP4ProcessDeathInstrumentedTest#b_resumeExactBindingAfterHostProcessStop" com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner
```

Validation APK was made only through the archival build script:

```text
.\scripts\build-and-save.ps1 -Series '4.17-dev' -MinimumVersionCode 174 -JavaHome 'C:\Program Files\Android\Android Studio\jbr' -Notes 'P4 atomic binding rollback validation'
```

Artifact:

```text
artifacts/builds/v4.17-dev.6/build-20260904-184343/TranslateBooks-v4.17-dev.6-code174.apk
backup/builds/v4.17-dev.6/build-20260904-184343/TranslateBooks-v4.17-dev.6-code174.apk
SHA-256: 97DDFD5901AD7957C3B64780C9037A1BE4B6EB3824B2B4C548B0367523163834
```

The code176 artifact above is retained as historical evidence for the P4
post-closure correction. The current cross-phase validation artifact is P5
dry-run APK `4.17-dev.9`/code177, archived in
`artifacts/builds/v4.17-dev.9/build-20260904-193721` and
`backup/builds/v4.17-dev.9/build-20260904-193721`, with SHA-256
`8B4D4714287114013A43C181C232921ABC6D6BA43CFB34F5E87D6CABF34001FA`.
It is not a P4 result or a V4.18 release build.

The device was then restored by uninstalling the test target package and
installing the retained baseline APK, because the device rejected `pm clear`
and would otherwise retain a v19 database that code169 must reject on
downgrade. Final device verification: `versionName=4.17-dev.1`,
`versionCode=169`.

## Production-change guard

The diff from the P4 starting commit contains only the justified P4 owners:
engine P4 value types, app P4 selection/binding/DAO/service, the additive v19
repository migration, read-only project projection/setup UI, direct P4 tests,
and documentation/state/checklist. No provider/API wiring, database schema
outside P4, build metadata, canonical pack, authority bytes, trusted profile
resource, certification state or execution protocol was changed. The original
workspace `D:\App Translate Books` was not modified.

Final checks:

- `git diff --check`: PASS.
- Provider/API calls: `0`.
- Real API: skipped by explicit opt-in.
- Canonical ZIP, Java-control ZIP, authority hashes and profile v2 hash:
  unchanged.
- Device: restored to code169.
- Execution/certification: disabled/not certified.

## Handoff

## Post-closure P4 correction

The P5 entry review found a concrete producer/contract vocabulary mismatch:
P4 test/UI setup used `NORMAL` while the SAFE4 contract names the mode
`NORMAL_FOUR_SOURCE` (and `ALTERNATE_EXPLICIT` for the explicit alternate
mode). A canonical-mode assertion reproduced the failure before the fix:
the setup returned `INVALID_INPUT` with the old validation message. Commit
`364faa42e7ed6bb08b75dda7fdc7335b8f931df7` changed only the P4 service/UI
producer and direct P4 tests/docs. Device validation on APK code176 passed
the P4 binding class `4/4`, including the canonical-mode assertion. The
device was then restored to code169 by uninstall/reinstall because `pm clear`
was rejected. Existing persisted legacy-mode rows are not rewritten or
silently reinterpreted; they remain fail-closed/read-only.

P4 hands off only to a separately authorized `P5 — CONTROLLED L1 PILOT`.
P5 must separately authorize certification/pilot policy, provider execution,
real chapter selection, cost/token limits and stop/recovery measurement. No P4
result is evidence that Editorial 4.1.3 is runnable or certified.
