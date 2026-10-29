# Workspace Snapshot

- Updated: 2026-10-29 (+07:00). R2 started: archive-release.ps1 can store a desktop package (pc/) with commit and hash checks (test-archive-release.ps1 passes), build-desktop.ps1 has -MsiVersion; RC builds from one commit next.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: ca9e950c (implementation baseline; later commits are docs only)
- Current build: phone installed 4.24-dev.5/code309; phone candidate APK 4.24-dev.6/code310 (SHA-256 DCA96241...528F, emulator only); PC 4.24-pc-dev.11 (zip B27A8D02..., msi 6B27CA53...705D) installed.
- Current phase: R2 (release candidate) then R3 (release checks).
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: owner installs dev.9 and uses it on real data (phone export import with counts, one real chapter); v4.23/v4.24 release decision (phone APK and PC msi together); signing the msi (optional).
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: clean archive of 9a62039d (coordinator): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 58, golden 14/14, lint 0 errors; Editorial and golden diffs empty; app/src/main diff = the JobStatusText core move only.
- Next action: build phone 4.24.0-rc.1 and PC 4.24.0-rc.1 from the same commit, then the R3 checks.
