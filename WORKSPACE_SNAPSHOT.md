# Workspace Snapshot

- Updated: 2026-10-29 (+07:00). P4b done and checked on a clean archive of 277a5558; PC package 4.24-pc-dev.8 built. Live whole-volume run on the PC done once (owner approval typed): 2 chapters x 2 chunks, 4 calls, 0.004660 USD of 0.05, pause excluded from elapsed.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: 9a62039d (implementation baseline before this snapshot commit)
- Current build: phone 4.24-dev.5/code309 (SHA-256 93E86E94...B766); PC package 4.24-pc-dev.7 (build-20261029-072703, from 9a62039d; zip SHA-256 9870D8B4...C9C6, msi B3FEDAB3...2CC1).
- Current phase: owner hands-on test of the PC app and reading of the two QA_ outputs (Glossary names for the characters were not applied in the live run: see the checklist).
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: P4b fixes (prompt preview dialog empty in screenshot 034, English Close button, run-row label Hoàn tất vs Xong, sidebar focus ring stuck on Dịch); owner installs the next PC package and uses it; real phone export import (counts); whole-volume live run (needs spend approval); v4.23/v4.24 release decision.
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: clean archive of 9a62039d (coordinator): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 58, golden 14/14, lint 0 errors; Editorial and golden diffs empty; app/src/main diff = the JobStatusText core move only.
- Next action: owner reads out-volume outputs and decides on the character-name Glossary finding; then the v4.24 release decision.
