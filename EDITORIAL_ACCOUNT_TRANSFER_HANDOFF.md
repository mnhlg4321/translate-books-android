# Editorial Account Transfer Handoff

Status: `REVIEW STOP — HANDOFF ONLY`

This handoff was prepared on 2026-08-04. No feature work, database change,
application-logic change or APK build was performed for this handoff. The
workspace is ready for another Codex account to review and continue only after
the user explicitly chooses the next scope.

## A. Repository identity

- Workspace: `C:\Users\ADMIN\Documents\App Translate Books`
- Branch: `feature/v4.16`
- Verified pre-handoff implementation `HEAD`: `a831e946520a194fa70687df2048aa0ab3a463ef`
- Remote: `origin https://github.com/manhluongvd/translate-books-android.git`
- Upstream tracking branch: none
- Immutable release tag: `v4.15`
  - annotated tag object: `cb474d2dd22763f67e24b4c0f57a27b7ae4689e7`
  - peeled commit target: `292b24e2ac7dec7b9635b8d0e72f76745ddf432c`
- Worktree: only the user-owned modification `M .idea/gradle.xml` is present.
  It is intentionally not staged or committed. There are no other source or
  untracked changes.
- No remote push was performed during this handoff.

The verified pre-handoff source/state `HEAD` is
`a831e946520a194fa70687df2048aa0ab3a463ef`. This document and the aligned
state/checklist updates are the documentation-only handoff commit that follows
that baseline. The current development APK was built from source commit
`f62ea5b`, because state/evidence documents were updated after the archive
build as required by the snapshot policy.

## B. Product and build identity

Released product:

- `v4.15` / code62 remains the current release and its tag is unchanged.

Latest accepted development build:

- Version: `4.16-dev.29`
- Version code: `91`
- Event: `build-20260804-073059`
- Build source commit: `f62ea5b35a183071374498417b7ed4eef5637f33`
- Artifact APK: `C:\Users\ADMIN\Documents\App Translate Books\artifacts\builds\v4.16-dev.29\build-20260804-073059\TranslateBooks-v4.16-dev.29-code91.apk`
- Backup APK: `C:\Users\ADMIN\Documents\App Translate Books\backup\builds\v4.16-dev.29\build-20260804-073059\TranslateBooks-v4.16-dev.29-code91.apk`
- APK SHA-256: `959A25630941550E3D59CB2FBE75C1E3C6D33DCA2A109553C248CDA653E4DE90`
- Artifact source ZIP: `C:\Users\ADMIN\Documents\App Translate Books\artifacts\builds\v4.16-dev.29\build-20260804-073059\project_source_build-20260804-073059.zip`
- Backup source ZIP: `C:\Users\ADMIN\Documents\App Translate Books\backup\builds\v4.16-dev.29\build-20260804-073059\project_source_build-20260804-073059.zip`
- Source ZIP SHA-256: `A55B5AEDDD945F5247FC1A6177C80654C615DF664B34485F106B92C83B948BAD`
- `BUILD_INFO.json`, `README.md` and `SHA256SUMS.txt` are present in both
  locations. All four checksum entries pass, and artifact/backup files are
  byte-identical.
- APK state: archived, not installed. No connected device or visual QA is
  claimed because `adb` is unavailable.

The source ZIP contains 468 entries and preserves the canonical SAFE4 assets:

| Asset | Bytes | SHA-256 |
|---|---:|---|
| `PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4.txt` | 9063 | `C57100C45F16FC5A27E56AE17ABE919BE89DA55082A66D060DB504746ED8B717` |
| `PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4.txt` | 5008 | `0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81` |
| `WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4.txt` | 18337 | `7A434ADE77239DB33AF5A677456D0310AC11DC8391565FC4888236536FF96B5E` |

## C. Current architecture

### `editorial-engine` / G2-A — pure JVM

The `:editorial-engine` module has no Android dependency. It owns the canonical
manifest representation, canonical JSON/hash rules, strict integrity
validation, trusted compatibility evaluation, compatibility classes and the
read-only registry contract. It does not know `Context`, `Uri`, SQLite or SAF.

### G2-B1 — persistent storage and headless import boundary

Android persistence remains in `:app`:

- `TranslationRepository` database version: **14**.
- Additive v13→v14 tables include `editorial_packs`,
  `editorial_pack_files`, `editorial_pack_imports` and
  `editorial_pack_compatibility_results`.
- `EditorialPackImportService` owns all write authority, snapshotting,
  integrity/compatibility calls, immutable move and recovery.
- `EditorialPackStorageLayout` uses private storage:
  `filesDir/editorial-packs/staging/<importId>` and
  `filesDir/editorial-packs/immutable/<canonicalPackHash>`.
- Registry queries are read-only; import/certification/activation mutation APIs
  are not exposed on the public registry.

`EditorialPackImportService.importZip(InputStream)` is the headless ZIP entry
point. It consumes the selected stream once, applies bounded ZIP entry/path,
size and compression checks, then delegates to the existing private staging,
integrity, compatibility, atomic storage and SQLite transaction pipeline.

### G2-B2A — read-only management UI

`EditorialPackManagementPageFactory`, `EditorialPackListPresenter`,
`EditorialPackUiModel` and their mapper expose persisted registry data only.
They do not read SQLite or immutable files directly from UI code and provide no
delete, replace, update, certify, activate or bind actions.

### G2-B2B-ZIP — runtime ZIP import UI

- `EditorialPackImportPageFactory`: instructions, progress and terminal result
  presentation.
- `EditorialPackSafBridge`: thin `ACTION_OPEN_DOCUMENT` bridge; ZIP MIME is
  preferred, octet-stream is the provider fallback, selection is single and
  read-only.
- `EditorialPackImportCoordinator`: one-shot stream handoff, worker executor,
  cancel/double-click handling and typed UI state mapping.
- `EditorialPackImportUiState` and `EditorialPackImportResultMapper`:
  fail-closed wording for snapshot/integrity/compatibility/storage/blocked and
  invalid outcomes.
- `MainActivity` changes are limited to a coordinator reference and thin
  request/result dispatch. It does not parse ZIP, hash, query SQLite, decide
  compatibility or own the pack state machine.

Android-dependent code is in `:app`; the engine contracts and all G2-A tests
are pure JVM. Coordinator logic has injected stream/importer/executor seams for
JVM tests.

## D. Completed work

- Canonical manifest model and deterministic representation.
- Strict integrity validation: manifest, role count, encoding/BOM, duplicate and
  extra-file, length/hash and canonical-pack identity checks.
- Compatibility classes: `DATA_COMPATIBLE`, `ADAPTER_REQUIRED`,
  `ENGINE_UPGRADE_REQUIRED`, `INVALID`, `BLOCKED`.
- Additive SQLite v14 persistent registry and private immutable content-addressed
  storage.
- TOCTOU-safe headless import and orphan recovery.
- Read-only persisted-pack list/detail UI.
- Runtime ZIP picker via SAF without write permission or persistent external URI.
- Progress/result UI that never claims certification or runnable readiness.
- Same canonical hash is idempotent and is shown as `Pack đã tồn tại`.
- Same packId/version with another hash remains an identity collision/block.
- No dynamic Java/Dex/native code loading from packs.

Automated evidence retained in the repository:

- `:editorial-engine:test`: 19/19 pass.
- `:app:testDebugUnitTest`: 161/161 pass.
- `:app:compileDebugAndroidTestJavaWithJavac`: pass.
- `:app:lintDebug`: 0 errors, 54 warnings.
- `git diff --check`: pass.
- Connected/visual/device QA: not run; no ADB device.

## E. Not completed / deliberately blocked

- Trusted production engine contract profile/registry.
- Certification and `CERTIFIED` state.
- Golden Replay runner/certification.
- Activation or execution.
- Project-to-pack binding.
- Folder/tree import (G2-B2C is not started).
- L1/L2/L3 model execution and release flow.
- Connected instrumentation execution and visual/device QA.
- Any release/tag operation for v4.16.

## F. Safety invariants

- Canonical SAFE4 code86 remains the same three files and three hashes listed
  above. Code91 is only the newest APK; it does not change the canonical pack
  identity.
- Candidate identities `DBE214...` and `3B2FCC...` are not read from `D:`, not
  synthesized into a manifest, not imported automatically and not activated.
- `EditorialSafe4Pack.executionEnabled()` remains false; no SAFE4 capability was
  marked implemented.
- The current runtime profile is intentionally empty. The app must not infer
  `DATA_COMPATIBLE` from filename, display name, version or a similar contract.
- Model-provided `PASS`/`CLOSED` text is never evidence; gates must remain
  derived from stored ledger/evidence.
- Existing projects are never silently upgraded or rebound to a newer pack.
- Packs cannot load Java, Dex or native libraries.
- Release tag `v4.15` and its target are not to be changed.
- `.idea/gradle.xml` is user-owned and must remain outside commits.

## G. Current blocker

The current APK has no trusted runtime engine contract profile. Therefore a
selected pack must fail-closed and may be persisted as blocked; it must not be
guessed as `DATA_COMPATIBLE`, certified or executed. A future profile registry
must provide explicit contract/schema/fingerprint/capability facts before any
pack can be considered compatible.

## H. Exact next-step choices

No next step was executed in this handoff. The first new account should ask the
user to choose one of these scopes:

1. **If a device is available:** install the archived code91 APK and run the
   connected/visual ZIP-import QA matrix. Do not use the old installed code84
   as SAFE4 evidence.
2. **If no device is available:** design **G2-C0 trusted engine contract
   profile/profile registry** only; do not certify, activate or execute.
3. **Optional later:** G2-B2C folder import. It is not a prerequisite for G2-C0
   and must not be started without explicit approval.

## I. Resume commands

Run these verification commands first in the new account. Do not use
`assembleDebug` directly and do not push remote changes.

```powershell
Set-Location 'C:\Users\ADMIN\Documents\App Translate Books'
git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' status --short --branch
git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' rev-parse HEAD
git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' log -10 --oneline --decorate
git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' show-ref --tags v4.15
```

Focused regression and static verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :editorial-engine:test :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:compileDebugAndroidTestJavaWithJavac --no-daemon
.\gradlew.bat :app:lintDebug --no-daemon
git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' diff --check
```

For an existing archive, verify the four entries in
`artifacts/builds/v4.16-dev.29/build-20260804-073059/SHA256SUMS.txt` with
`Get-FileHash`, then compare every file by name, length and SHA-256 against the
matching `backup/builds/v4.16-dev.29/build-20260804-073059` directory. Do not
rebuild merely to reproduce this handoff.

## J. Rollback and recovery

Phase commit anchors:

- G2-A: `83a9428`, `616dc46`, `eb15481`, `9d401b2`.
- G2-B1: `19bcf6a`, `de93407`, `85aa35b`.
- G2-B2A: `7be575e`, `710297d`, `f1a58d3`, `f427984`, `71d9ca8`, `5ca6745`.
- G2-B2B-ZIP: `e124b5a`, `8901c8a`, `22c080b`, `fd4f807`, `3efb620`,
  `2c92303`, `f62ea5b`, `ced89c5`, `a831e94`.

To undo a future change, prefer a reviewable `git revert <commit>` on the
feature branch. Do not use `git reset --hard`, and do not discard the
user-owned `.idea/gradle.xml`; preserve it with a named stash only when a
workflow requires a clean tree.

Build APKs, source ZIPs, README, BUILD_INFO and checksum manifests are ignored
local data under `artifacts/builds/` and `backup/builds/`; they are not tracked
by Git and must be copied/verified from both locations before cleanup. The
current app database, imported immutable packs and staging/recovery state exist
only in the app's private device storage. Candidate files on `D:` and any local
credentials are outside this handoff and must not be copied.

Before ending any resumed session, update `BUILD_STATE.md`,
`WORKSPACE_SNAPSHOT.md` and `release_checklists/v4.16.md` with real evidence,
then stop at the user-approved scope.
