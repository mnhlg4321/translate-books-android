# Workspace Snapshot

- Updated: 2026-10-29 (+07:00). R2 and R3 DONE on RC commit 162b96f2 (APK 4.24.0-rc.1/code311, PC rc.1 msi ProductVersion 4.24.101). Evidence and QA_REPORT.md in artifacts/qa/v4.24.0-rc1/. Nothing installed on the real phone.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: ca9e950c (implementation baseline; later commits are docs only)
- Current build: RC APK 4.24.0-rc.1/code311 (SHA-256 B9235830...B5A8), PC RC 4.24.0-rc.1 (zip 6D9AAAD4..., msi E9C50F46...FFD8). Phone 15e84958 still runs 4.24-dev.5/code309.
- Current phase: R3 done; R4 (owner install approval on the phone) is next.
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: owner installs dev.9 and uses it on real data (phone export import with counts, one real chapter); v4.23/v4.24 release decision (phone APK and PC msi together); signing the msi (optional).
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: RC 162b96f2 clean archive: app 749/5 skip/0 fail, core 61, jdbc 3, desktop 69, golden 14/14 twice, lint 0; Editorial and golden diffs empty. Emulator instrumented: 314 tests, 251 pass, 37 skip, 26 fail (22 identical on the v4.23 baseline build, 4 EditorialP5DVol5RawPilot not run on baseline). Macrobenchmark coldStartup median 3157.7-3408.1 ms vs v4.23 baseline 3373.2 ms.
- Next action: R4: owner types "duyệt cài" to install 4.24.0-rc.1 on phone 15e84958; then R5/R6 per docs/V424_RELEASE_PLAN.md (final msi ProductVersion must exceed 4.24.101, e.g. 4.24.200).
