# Workspace Instructions

## Required startup

For v4.8 and later, follow `DEVELOPMENT_WORKFLOW.md`. Its 14 steps describe one complete release lifecycle, not one chat/work session. Create the release branch and release checklist once. Every later session resumes them; it must not create another branch, plan, checklist, or release track unless the owner changes the release scope.

At the start of every session, read the active canonical plan first, then `BUILD_STATE.md`, then `WORKSPACE_SNAPSHOT.md`, and verify the current branch, commit, and status. For the active v4.17 recovery, the canonical plan is `TRANSLATION_PROFILE_RECOVERY_V4_17.md`, the only branch is `feature/v4.17-translation-profile-compatibility`, and the only checklist is `release_checklists/v4.17-translation-profile-compatibility.md`.

Read `GIT_WORKFLOW.md` before modifying this project. Verify the current branch, commit, status, version, and build artifact against the snapshot.

Create the version checklist from `release_checklists/TEMPLATE.md` only when a new release lifecycle begins. Do not mark a step complete without concrete Evidence. A local test, compile, lint, or build failure is `FAILED_REPAIRING`: diagnose, patch, and rerun it in the same phase and branch. It is not a reason to create a review-stop package or report the release as externally blocked. Stop dependent release/tag work while the failure remains, and never claim completion without evidence.

Use `BLOCKED_EXTERNAL` only when a required external resource or authority is unavailable and no safe local alternative exists, or when proceeding requires a destructive or materially out-of-scope owner decision. Historical blockers from frozen tracks do not block the active release.

## Active v4.17 scope guard

The v4.17 release must follow the ordered phases in `TRANSLATION_PROFILE_RECOVERY_V4_17.md`: `D0_DOCS` -> `D1_BASELINE` -> `D2_GLOSSARY4` -> `D3_PRONOUN7` -> `D4_INTEGRATION` -> `D5_RELEASE`.

Do not add automatic chapter mapping, RSC/Relation-Speaker discovery, Editorial activation, manifests, session receipts, IPC/AVD gates, live canaries, or unrelated UI/database work. The user selects one RAW chapter and its matching Glossary/Pronoun files manually. Glossary runtime uses `source,target,category,note` and ignores `priority`; Pronoun uses `from,speaker,target,self,call,scope,note` while preserving the legacy three-column format.

## Snapshot policy

Update `WORKSPACE_SNAPSHOT.md` whenever the first applicable condition occurs:

- approximately 30 minutes of active work have elapsed since the snapshot;
- approximately 10 commits have been created since the snapshot;
- an independent functional group has been completed;
- regression, build, tag, merge, blocker, or known-bug state changes;
- work is about to pause or hand off.

`WORKSPACE_SNAPSHOT.md` is current state, not an append-only history. Replace its current values instead of appending a new historical section. Git history and immutable artifacts preserve prior snapshots.

Every update must contain accurate values for Current version, Current branch, Current commit, Current build, Current phase, Completed tasks, Pending tasks, Known bugs, Regression status, and exactly one Next action.

The `Current commit` value is the implementation baseline immediately before the snapshot commit. State this explicitly; do not invent a self-referential commit hash. Confirm actual `HEAD` when resuming.

Commit a snapshot update only when the file content changed. Never create an empty snapshot commit.

Before ending work, verify that the snapshot describes the latest completed group and the exact next action. A handoff uses only branch, HEAD, current phase, completed work, test/build evidence, and next action; do not introduce session IDs, mode dispatchers, state hash chains, or owner-decision receipts for ordinary repository work.

## Artifact and backup policy

Every development APK build must use `scripts/build-and-save.ps1`. Direct `assembleDebug` and Android Studio **Build APK(s)** are forbidden because they do not guarantee durable retention.

The script must assign a unique numbered `versionName`, select an increasing Android `versionCode`, and preserve the APK before any optional installation. Every successful development build must create the same immutable payload in both `artifacts/builds/v<version>/<event>` and `backup/builds/v<version>/<event>`. Each payload must contain the APK, a per-build README, `BUILD_INFO.json`, SHA-256 manifest, and an exact tracked-source ZIP. Never overwrite or reuse an existing version/event directory.

For release candidates and release tags, additionally run `scripts/archive-release.ps1` with a unique event id. A release build event uses `build-YYYYMMDD-HHMMSS`; a tag event uses `tag-vX.Y.Z`. The release payload must exist in both `artifacts/releases/vX.Y.Z/<event>` and `backup/vX.Y.Z/<event>` and contain the APK, SHA-256 manifest, QA report, CHANGELOG, BUILD_STATE, RELEASE_NOTES, Perfetto evidence, Macrobenchmark evidence, screenshots, video, and `project_source_vX.Y.Z.zip` generated from the exact Git ref being archived.

Do not treat `build/` or `app/build/` as durable storage. Copy the APK and all important evidence out immediately after a successful build and before any `gradlew clean`.

Do not fabricate missing benchmark evidence. A historical limitation may use an explicit status report, but a new release must retain the real Macrobenchmark output required by its release criteria.
