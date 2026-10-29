# Workspace Snapshot

- Updated: 2026-10-30 (+07:00). R6: release 4.24.0 built from e176d23f (APK code313, PC msi 4.24.200), clean checks and msi-over-RC done, checklist steps 05-09 ticked; tag, archive and fast-forward follow.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: 4da681fb (implementation baseline before this snapshot commit)
- Current build: Release APK 4.24.0/code313 (SHA-256 4ABB5FF1...C3EF), PC 4.24.0 (zip 78829A33..., msi CF31D304...559D, ProductVersion 4.24.200). Phone 15e84958 runs RC 4.24.0-rc.1/code311; release APK not installed.
- Current phase: R6 tag and archive.
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: R5 owner sign-off; R6 tag v4.24.0 + archive-release with the PC package (temporary worktree, short-lived feature/v4.24.0, msi ProductVersion > 4.24.101); close v4.22/v4.23 checklists as SUPERSEDED; R7 merge/push needs separate owner authority; msi signing optional.
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: RC 162b96f2 clean archive: app 749/5 skip/0 fail, core 61, jdbc 3, desktop 69, golden 14/14 twice, lint 0; Editorial and golden diffs empty. Emulator instrumented: 314 tests, 251 pass, 37 skip, 26 fail (22 identical on the v4.23 baseline build, 4 EditorialP5DVol5RawPilot not run on baseline). Macrobenchmark coldStartup median 3157.7-3408.1 ms vs v4.23 baseline 3373.2 ms.
- Next action: Tag v4.24.0 on the checklist commit, archive build and tag events, fast-forward feature/v4.24; then R7 (publish) needs the owner.
