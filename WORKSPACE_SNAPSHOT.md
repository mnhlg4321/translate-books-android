# Workspace Snapshot

- Updated: 2026-10-30 (+07:00). Release APK 4.24.0/code313 installed on phone 15e84958 (owner "duyệt cài"): backup before and after identical (SHA-256 750399B4...EBC9), counts identical, crash buffer 0 for the app. Not pushed by this session.
- Current version: v4.24.0 released locally (phone APK 4.24.0/code313, PC 4.24.0 msi ProductVersion 4.24.200). Phone 15e84958 runs RC 4.24.0-rc.1/code311.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: 07eb7b34 (implementation baseline before this snapshot commit; equals origin/main)
- Current build: Phone 15e84958 runs release APK 4.24.0/code313 (SHA-256 4ABB5FF1...C3EF). PC 4.24.0 msi ProductVersion 4.24.200 (CF31D304...559D).
- Current phase: v4.24.0 released; no active release work.
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, 8 Cai dat, 9 Giao dien, 10 hoan thien (window size/place memory, background import/export, focus ring, Vietnamese dialog buttons), tests T1-T8. One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: optional install of the release APK (code 313) on phone 15e84958 (owner types "duyệt cài"; phone runs rc.1/311 with the same app source); offline v18 measurement after the next real volume; Editorial frozen; msi signing optional.
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. The phone's prompt preview differs from the real prompt outside preset full (known issue, phone untouched). Sets do not travel in the library ZIP (by design, stated in the screens). Pronoun cell editing is NOT_DONE (read-only table plus raw text editor, section 14.2).
- Regression status: RC 162b96f2 clean archive: app 749/5 skip/0 fail, core 61, jdbc 3, desktop 69, golden 14/14 twice, lint 0; Editorial and golden diffs empty. Emulator instrumented: 314 tests, 251 pass, 37 skip, 26 fail (22 identical on the v4.23 baseline build, 4 EditorialP5DVol5RawPilot not run on baseline). Macrobenchmark coldStartup median 3157.7-3408.1 ms vs v4.23 baseline 3373.2 ms.
- Next action: owner decides whether to install the release APK 4.24.0/313 on the phone (backup, counts, install-validated.ps1).
