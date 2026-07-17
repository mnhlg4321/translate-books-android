# Workspace Instructions

## Required startup

Read `WORKSPACE_SNAPSHOT.md` and `GIT_WORKFLOW.md` before modifying this project. Verify the current branch, commit, status, version, and build artifact against the snapshot.

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

