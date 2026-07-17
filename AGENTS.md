# Workspace Instructions

## Required startup

For v4.8 and later, follow `DEVELOPMENT_WORKFLOW.md` in its exact 14-step order. Start every session by reading `BUILD_STATE.md`, then `WORKSPACE_SNAPSHOT.md`, then verify the current commit/status, and only then create a new feature branch.

Read `GIT_WORKFLOW.md` before modifying this project. Verify the current branch, commit, status, version, and build artifact against the snapshot.

Create the version checklist from `release_checklists/TEMPLATE.md`. Do not mark a step complete without concrete Evidence. If any step fails, stop dependent work, record the failure, and do not claim the session or release is complete.

## Snapshot policy

Update `WORKSPACE_SNAPSHOT.md` whenever the first applicable condition occurs:

- approximately 30 minutes of active work have elapsed since the snapshot;
- approximately 10 commits have been created since the snapshot;
- an independent functional group has been completed;
- regression, build, tag, merge, blocker, or known-bug state changes;
- work is about to pause or hand off.

Every update must contain accurate values for Current version, Current branch, Current commit, Current build, Completed tasks, Pending tasks, Known bugs, Regression status, and Next step.

The `Current commit` value is the implementation baseline immediately before the snapshot commit. State this explicitly; do not invent a self-referential commit hash. Confirm actual `HEAD` when resuming.

Commit a snapshot update only when the file content changed. Never create an empty snapshot commit.

Before ending work, verify that the snapshot describes the latest completed group and the exact next action.

## Artifact and backup policy

After every successful build and after every release tag, run `scripts/archive-release.ps1` with a unique event id. A build event uses `build-YYYYMMDD-HHMMSS`; a tag event uses `tag-vX.Y.Z`.

The immutable payload must exist in both `artifacts/releases/vX.Y.Z/<event>` and `backup/vX.Y.Z/<event>`. Never overwrite or reuse an existing event directory.

Every payload must contain the APK, SHA-256 manifest, QA report, CHANGELOG, BUILD_STATE, RELEASE_NOTES, Perfetto evidence, Macrobenchmark evidence, screenshots, video, and `project_source_vX.Y.Z.zip` generated from the exact Git ref being archived.

Do not treat `build/` or `app/build/` as durable storage. Copy the APK and all important evidence out immediately after a successful build and before any `gradlew clean`.

Do not fabricate missing benchmark evidence. A historical limitation may use an explicit status report, but a new release must retain the real Macrobenchmark output required by its release criteria.

