# Workspace Snapshot

- Updated: 2026-10-29 (+07:00). P4 (desktop UI parity): sections 1-10 implemented; final clean-archive check, new PC package and report pending.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: b70f66c2 (implementation baseline before this snapshot commit; section 10 is committed together with this snapshot)
- Current build: phone 4.24-dev.5/code309 (SHA-256 93E86E94...B766); PC package 4.24-pc-dev.6 until P4 builds a new one with scripts/build-desktop.ps1 -Msi.
- Current phase: P4 closing: clean-archive regression of all ten sections, PC package, screenshots, report.
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: clean-archive regression of the final commit, new PC package with -Msi (SHA-256 of zip and msi), report to the coordinator. Not measured: provider-calling buttons were never pressed (no spend).
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: sections 1-5 verified on a clean archive; sections 1-10 desktop tests pass in the worktree; the final clean-archive run is the next step.
- Next action: run the clean-archive regression on the final commit, build the PC package, write the report.
