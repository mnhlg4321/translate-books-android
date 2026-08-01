# Editorial v4.16 handoff

This document is the portable handoff for the unfinished Editorial mapping work. It records the problem investigation, the decisions that are already implemented, the evidence that exists, the remaining design, and the exact workflow for continuing on another machine.

The document is intentionally self-contained, but the repository files named below remain the source of truth for build state, release gates, and implementation details.

## 1. Handoff status

Snapshot date: 2026-08-01.

- Working branch: `feature/v4.16`.
- Actual `HEAD` at handoff: `5589af0c50d4e4e946068bd409b73bbe027100ae` (`test(editorial): record project reference ownership`). Confirm this again before editing.
- The only known worktree change is the pre-existing user-owned `.idea/gradle.xml`. Keep it unchanged, do not stage it, and do not include it in product commits.
- The annotated tag `v4.15` is immutable. Do not move, delete, recreate, or force-push it.
- Released baseline: `v4.15` / version code `62`.
- Latest development build: `4.16-dev.22` / version code `84`; it is archived and installable, but v4.16 is not complete.
- Current scope: Editorial mapping contract and project-default/chapter-override ownership are implemented. Revision-safe editing of an existing chapter, stale-run invalidation, hands-on Editorial QA, and the v4.16 release gates remain open.

Never describe v4.16 as released or complete while QA, backup, tag, and export gates remain unchecked in `release_checklists/v4.16.md`.

## 2. The problem that was investigated

The device screenshot showed chapter cards for `005`, `006`, and `007` with an apparently identical Glossary/Pronoun source. The visible four-row snapshot was useful, but it did not prove that each reference belonged to the chapter being edited.

The root cause was ownership ambiguity:

1. RAW and DRAFT were selected per chapter, but the persistence path looked up the project ACTIVE Glossary and Pronoun for every chapter in the batch.
2. Project reference persistence makes the latest imported profile ACTIVE for its role. Therefore a later `007_FINAL_GLOSSARY...` could become the source displayed for earlier chapters unless the chapter-local association was retained.
3. The UI displayed a snapshot but had no explicit distinction between an inherited project default and a chapter-specific override.
4. An import with an unclear filename could be silently treated as a default or attached to the wrong chapter.
5. Existing immutable Editorial snapshots have no safe “edit mapping in place” operation yet. A future edit must create a new revision and invalidate dependent runs rather than mutate evidence already used by L1/L2/L3.

The design therefore treats a four-file mapping as a contract and treats filename inference as a proposal that requires a visible, reviewable result before persistence.

## 3. Decisions that are now the contract

### 3.1 Four roles and chapter identity

Each chapter bundle has exactly these roles:

| Role | Accepted source | Identification rule |
|---|---|---|
| `RAW` | text file | filename role token such as `RAW`; chapter key is parsed from the filename |
| `DRAFT` | text file | filename role/language marker such as `DRAFT` or Vietnamese suffix; chapter key is parsed from the filename |
| `GLOSSARY` | CSV or supported text mapping | Glossary tokens plus canonical chapter key |
| `PRONOUN` | CSV or supported pronoun mapping | Pronoun tokens plus canonical chapter key |

The planner groups by the canonical numeric chapter key, not by the last imported file or by list position. For example, `005`, `006`, and `007` are distinct keys even if their files were selected together.

Rules:

- The filename and supported format identify a role; content validation still runs before save.
- CSV is valid for Glossary and Pronoun.
- A file without enough identifying information is only an unassigned suggestion. It is never automatically saved.
- A duplicate RAW, DRAFT, Glossary, or Pronoun for one chapter is `NEEDS_REVIEW` and blocks save.
- A missing role makes the chapter `NEEDS_REVIEW`; only a complete four-role bundle is `READY`.
- A chapter-numbered reference is proposed for the matching chapter. It cannot change the ACTIVE project default merely because it was imported.
- A reference without a chapter number can become a project default only after explicit user confirmation.

Implementation entry points:

- `app/src/main/java/com/ml/tblandroidtxt/EditorialImportPlanner.java`
- `app/src/test/java/com/ml/tblandroidtxt/EditorialImportPlannerTest.java`
- `EditorialImportPlanner.planBundle(...)` detects roles, canonicalizes chapter keys, reports duplicates/missing roles, and retains unclear files in `unassigned`.

### 3.2 Project default versus chapter override

There are two deliberate flows:

| User action | Glossary/Pronoun ownership | ACTIVE project mutation |
|---|---|---|
| Select RAW + DRAFT only | Inherited project default | None |
| Select a complete four-file bundle | Chapter override for that chapter | None |
| Import a chapter-less Glossary/Pronoun | Proposed project default | Only after confirmation |
| Import a chapter-numbered Glossary/Pronoun | Proposed chapter override | None until the reviewed bundle is saved |

The preview must show the final ownership that will be saved, not only the selected filename:

```text
Glossary
Nguồn: Chapter override
File: 005_FINAL_GLOSSARY_MERCEDES_VOL3.csv
```

or:

```text
Glossary
Nguồn: Inherited project default
Profile: Series A glossary
```

The implementation uses `ChapterPlan.ReferenceOrigin` with `CHAPTER_OVERRIDE` and `INHERITED_PROJECT_DEFAULT`. It keeps pending chapter-numbered overrides separate from project ACTIVE profiles until the user saves the reviewed plan.

Relevant implementation:

- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`: bundle picker, pending per-project chapter overrides, confirmation for chapter-less defaults, and batch persistence.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialImportPreviewDialog.java`: ownership labels, READY/NEEDS REVIEW state, warnings, and save gate.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPageFactory.java`: entry point `Nhập gói 4 file cho từng chapter`.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialRepository.java`: project reference profiles and immutable asset persistence.

## 4. What is implemented and evidenced

### Step 1: mapping contract

Implemented in:

- `36f42e7 fix(editorial): define four-role chapter bundle mapping`
- `bef3199 test(editorial): record four-role mapping contract`

The tests cover chapters `005`, `006`, and `007`, including the negative requirement that no chapter `005` or `006` receives a `007_FINAL_...` reference. The planner test also covers duplicate/missing roles, CSV references, unclear files, and READY/NEEDS_REVIEW states.

### Step 2: ownership separation

Implemented in:

- `4aca556 feat(editorial): separate project defaults and chapter overrides`
- `5589af0 test(editorial): record project reference ownership`

The tests cover inherited defaults, a matching chapter override replacing only that chapter, and a missing-default block. The persistence path now uses the planned Glossary/Pronoun source for each chapter instead of blindly reading the project ACTIVE source for every chapter.

### Regression evidence

The latest source regression after step 2 passed:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
```

Result: `160/160` JVM tests passed with 0 failures, errors, or skips. Targeted `EditorialImportPlannerTest` passed `14/14`. `git diff --check` passed.

This is source-test evidence. It is not evidence that the latest step 2 source is installed on the device; code84 was built before the latest step 2 commit and must not be presented as a step 2 device verification.

### Latest durable development build

The last accepted archive-first build is from an earlier source point:

- Version: `4.16-dev.22` / code `84`.
- Event: `build-20260801-133953`.
- Source commit recorded by the build: `b6936fb905459ac35759c8a33c7a4927071e5eba`.
- APK: `artifacts/builds/v4.16-dev.22/build-20260801-133953/TranslateBooks-v4.16-dev.22-code84.apk`.
- Mirror: `backup/builds/v4.16-dev.22/build-20260801-133953/`.
- APK SHA-256: `CDE2085EEEA959C38840C3E1F9197506BB54637EBEB38D717430CEA680892A51`.
- Source ZIP SHA-256: `4FECD8253B3608547581F72141949F482EDCCE95235B0BB5D6D16DA31CF835E2`.
- Build result: 153 JVM tests, lint 0 errors/53 warnings, artifact/backup parity passed.
- Device result: installed on OnePlus CPH2691 / Android 15; package identity and cold launch passed. Manual Editorial QA remains pending.

The build and backup directories are ignored by Git. A clean clone will not contain these APK/evidence payloads; copy both durable directories separately when transferring an installable build is required.

## 5. Remaining implementation plan

Do the following in order. Keep each independently testable group in a small commit and update the checklist/snapshot when the group changes the next step or regression state.

### Step A — finish revision-safe mapping editing

Goal: an existing chapter card gets a visible `Sửa mapping` action without changing the historical snapshot used by any run.

1. Add an explicit revision model. Prefer additive schema changes over changing existing snapshot rows. A practical shape is a chapter revision row with `chapter_id`, monotonic `revision`, `state` (`ACTIVE`, `SUPERSEDED`, `INVALIDATED`), created timestamp, and a content/hash identity.
2. Store the four role bindings for each revision, including source URI/display name, size, SHA-256, role, and ownership (`INHERITED_PROJECT_DEFAULT` or `CHAPTER_OVERRIDE`). Do not store only the current ACTIVE profile ID; the resolved source must remain reproducible.
3. Migrate existing chapters as revision 1. Preserve their old snapshot rows and hash evidence. The migration must be additive and idempotent.
4. Open the existing four-file picker with the current mapping prefilled. The user may replace one role, replace the entire bundle, or cancel. A partial replacement stays a draft proposal until all four roles are valid.
5. Show a before/after diff: old source name/hash versus proposed source name/hash, and inherited versus override ownership. Require explicit confirmation for a new revision.
6. On save, write the new revision atomically, make it the active mapping, and retain the old revision as historical evidence. Never update a previous snapshot row in place.

Acceptance criteria:

- Cancel leaves the active revision and all four immutable source snapshots unchanged.
- Replacing Glossary for chapter `005` changes only `005`; chapters `006` and `007` keep their previous resolved sources.
- A changed RAW or DRAFT causes a new mapping revision even when Glossary/Pronoun are inherited.
- Any duplicate, missing, unclear, or invalid role blocks the revision save.

### Step B — invalidate stale work safely

When a new mapping revision is saved:

- mark dependent queued/running Editorial runs as stale or require a deliberate restart;
- prevent a stale run from writing output into the new revision;
- retain the old run, context manifest, hashes, and evidence for audit;
- show the user which chapter/run is affected and why;
- make retry create a fresh run bound to the new revision, not mutate the old run.

The invalidation key should include the chapter revision identity and all four source hashes. A profile name alone is insufficient because a file can be replaced under a similar name.

### Step C — verify UI semantics on device

After a new archive-first build is installed, verify on the actual device:

- chapter `005`, `006`, `007` show their own RAW, DRAFT, Glossary, and Pronoun rows;
- the preview is scrollable and clearly distinguishes READY from NEEDS REVIEW;
- inherited rows say `Nguồn: Inherited project default` and show the profile;
- override rows say `Nguồn: Chapter override` and show the file;
- chapter-numbered Glossary/Pronoun import does not change the ACTIVE project profile;
- chapter-less import asks before saving as a project default;
- a duplicate or incomplete bundle cannot be saved;
- editing one chapter does not change the other chapter cards;
- canceling an edit leaves the original mapping unchanged.

Record concrete evidence in the release checklist. Do not infer this from unit tests alone.

### Step D — archive-first development build

Only after source regression passes and the device is available, run the repository script:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' stash push -m 'preserve-user-idea-during-v4.16-editorial-build' -- .idea/gradle.xml
try { .\scripts\build-and-save.ps1 -Series 4.16-dev -Install }
finally { git -c safe.directory='C:/Users/ADMIN/Documents/App Translate Books' stash pop }
```

The script must choose a new numbered version and event, archive the APK before installation, create matching payloads under `artifacts/builds/` and `backup/builds/`, and retain the README, `BUILD_INFO.json`, SHA-256 manifest, and exact tracked-source ZIP. Do not use direct `assembleDebug`, Android Studio Build APK(s), or an existing build directory as durable storage.

After the script, verify the payload pair, APK identity, source commit, install result, and cold launch. Then update `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, and `release_checklists/v4.16.md` with exact evidence.

### Step E — v4.16 QA and release gates

Run the complete matrix in `QA_SCOPE_v4_16.md`, including manual Editorial mapping, persistence/reload, SAF/export, retry/stale-run behavior, and the required device checks. Keep checklist steps 8, 10, 11, and 14 open until their evidence exists. A development build is not a release candidate merely because it installs.

Only after all required evidence passes may the normal workflow proceed to release archive, immutable tag, backup/export verification, and merge. The `v4.15` tag must remain untouched throughout.

## 6. Required workflow on another machine

At the start of every session, follow the repository instructions in this order:

1. Read `AGENTS.md`.
2. Read `BUILD_STATE.md`.
3. Read `WORKSPACE_SNAPSHOT.md`.
4. Confirm actual `HEAD`, branch, status, version, tag, and latest artifact; compare them with the snapshot rather than trusting a stored hash.
5. Read `GIT_WORKFLOW.md`.
6. Read `DEVELOPMENT_WORKFLOW.md` and follow its exact 14-step order.
7. Confirm `feature/v4.16`; do not create a competing branch or switch to `main` for this work.
8. Keep `.idea/gradle.xml` out of product commits. If the build script requires a clean tracked tree, stash only that file and restore it in `finally`.
9. Before each commit, run the required tests and update the relevant checklist evidence.
10. Before pausing, update the snapshot with the exact next action and the current baseline commit.

Useful checks:

```powershell
git status --short --branch
git rev-parse HEAD
git log -5 --oneline --decorate
git show-ref --tags v4.15
git tag --points-at v4.15
```

The output must confirm that `v4.15` still resolves to the existing annotated tag object `cb474d2dd22763f67e24b4c0f57a27b7ae4689e7` and peeled release target `292b24e2ac7dec7b9635b8d0e72f76745ddf432c`. Never force-update that ref.

## 7. Portable resource map

### State, policy, and gates

- `BUILD_STATE.md` — accepted release/build identity, checksums, device state, regression status, known limitations.
- `WORKSPACE_SNAPSHOT.md` — resumable current state and exact next step.
- `DEVELOPMENT_WORKFLOW.md` — mandatory 14-step development order.
- `GIT_WORKFLOW.md` — branch, commit, tag, snapshot, and release rules.
- `AGENTS.md` — startup and artifact policy applied by the workspace.
- `release_checklists/v4.16.md` — evidence-backed checklist; unchecked items are intentionally not complete.
- `QA_SCOPE_v4_16.md` — full release-candidate QA matrix.

### Product and Editorial design

- `EDITORIAL_WORKFLOW_V5_PLAN.md` — broader V5 context, data model, state machine, gates, and phased roadmap.
- `EDITORIAL_HANDOFF_V4_16.md` — this focused mapping/ownership/revision handoff.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialImportPlanner.java` — four-role mapping contract and ownership resolution.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialImportPreviewDialog.java` — review dialog and save gate.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPageFactory.java` — Editorial page and import controls.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java` — picker flow, pending overrides/default confirmation, and persistence orchestration.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialRepository.java` — project-owned profiles and immutable Editorial assets.
- `app/src/test/java/com/ml/tblandroidtxt/EditorialImportPlannerTest.java` — chapter isolation, duplicate/missing role, inheritance, and override tests.
- `app/src/test/java/com/ml/tblandroidtxt/EditorialPersistenceSpecTest.java` — project/reference persistence coverage.

### Build and verification tools

- `scripts/build-and-save.ps1` — mandatory numbered archive-first development build and optional install.
- `scripts/archive-release.ps1` — immutable release archive; do not use for ordinary development builds.
- `scripts/verify-release-workflow.ps1` — checklist gate verification.
- `BUILDING.md` — build prerequisites and command details.
- `artifacts/builds/v4.16-dev.22/build-20260801-133953/` — latest local development payload.
- `backup/builds/v4.16-dev.22/build-20260801-133953/` — matching local backup payload.

The last two directories are intentionally ignored by Git. For a machine transfer, copy them as binary evidence in addition to cloning the repository; do not add APKs or local backup payloads to Git just to make them visible in a clone.

## 8. Transfer checklist

- [ ] Clone or copy the repository, including Git history and this handoff file.
- [ ] Confirm branch `feature/v4.16` and actual `HEAD`.
- [ ] Confirm tag `v4.15` is unchanged.
- [ ] Preserve the user-owned `.idea/gradle.xml` separately; never overwrite it with a clone copy.
- [ ] Copy `artifacts/builds/v4.16-dev.22/` and `backup/builds/v4.16-dev.22/` if the old APK/evidence is needed offline.
- [ ] Install Android Studio SDK 35, JDK 17, PowerShell, Git, and platform tools.
- [ ] Connect the OnePlus CPH2691 / Android 15 only when device install/QA is needed.
- [ ] Read the state/policy files before editing.
- [ ] Run the source regression before touching the next implementation group.
- [ ] Implement revision-safe editing and stale invalidation before claiming the Editorial plan is complete.
- [ ] Build the new development APK only with `scripts/build-and-save.ps1`.
- [ ] Record all new evidence in the checklist, build state, and snapshot.

## 9. Explicit non-goals and safety notes

- Do not silently guess a chapter from file order.
- Do not make an imported chapter-numbered reference ACTIVE at project level.
- Do not overwrite an immutable input snapshot or historical run evidence.
- Do not use a filename or profile display name as a substitute for content/hash identity.
- Do not claim manual device QA from JVM tests, an old APK, or an unverified screenshot.
- Do not claim v4.16 complete while QA and release gates are open.
- Do not move or rewrite tag `v4.15`.
- Do not commit API keys, local device state, APKs, or ignored artifact stores.
