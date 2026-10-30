# Workspace Snapshot

- Updated: 2026-10-30 (+07:00). Owner approved the release of 4.24.1 after checking the dev build on the phone; release notes, CHANGELOG and BUILD_STATE written, release build, checks, tag and archive follow.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24.1 (worktree D:\P5E-builds\wt-v4.24.1), created from tag v4.24.0
- Current commit: 4da681fb (implementation baseline before this snapshot commit)
- Current build: Phone 15e84958 runs 4.24.1-dev.5/code318 (SHA-256 5608DAC1...2D14, commit 8ff63926, branch feature/v4.24.1). Released v4.24.0/code313 remains the tag.
- Current phase: v4.24.1 release steps (build, checks, tag, archive).
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: R5 owner sign-off; R6 tag v4.24.0 + archive-release with the PC package (temporary worktree, short-lived feature/v4.24.0, msi ProductVersion > 4.24.101); close v4.22/v4.23 checklists as SUPERSEDED; R7 merge/push needs separate owner authority; msi signing optional.
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: RC 162b96f2 clean archive: app 749/5 skip/0 fail, core 61, jdbc 3, desktop 69, golden 14/14 twice, lint 0; Editorial and golden diffs empty. Emulator instrumented: 314 tests, 251 pass, 37 skip, 26 fail (22 identical on the v4.23 baseline build, 4 EditorialP5DVol5RawPilot not run on baseline). Macrobenchmark coldStartup median 3157.7-3408.1 ms vs v4.23 baseline 3373.2 ms.
- Next action: Build 4.24.1 with build-and-save.ps1 -ExactReleaseVersion 4.24.1, clean checks on that commit, tag v4.24.1, archive build and tag events; merge and push are the coordinator.
