# Workspace Snapshot

- Updated: 2026-10-29 (+07:00). R3 in progress on RC commit 162b96f2 (RC APK 4.24.0-rc.1/code311, PC rc.1 installed over dev.11). Emulator full run found failures: 21 were tests broken by the core move (fixed in test code, no production change), plus V423Preview test fixed; the rest fail the same way on a v4.23 baseline build (pre-existing, listed in QA_REPORT).
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: ca9e950c (implementation baseline; later commits are docs only)
- Current build: phone installed 4.24-dev.5/code309; RC APK 4.24.0-rc.1/code311 (SHA-256 B9235830...B5A8, not installed on the phone); PC RC 4.24.0-rc.1 (zip 6D9AAAD4..., msi E9C50F46...FFD8).
- Current phase: R3 release checks on the RC commit 162b96f2.
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: owner installs dev.9 and uses it on real data (phone export import with counts, one real chapter); v4.23/v4.24 release decision (phone APK and PC msi together); signing the msi (optional).
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: clean archive of 9a62039d (coordinator): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 58, golden 14/14, lint 0 errors; Editorial and golden diffs empty; app/src/main diff = the JobStatusText core move only.
- Next action: final full instrumented run with the fixed test APK, Macrobenchmark and Perfetto, phone screenshots and video, QA_REPORT.md under artifacts/qa/v4.24.0-rc1/.
