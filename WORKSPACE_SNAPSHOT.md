# Workspace Snapshot

- Updated: 2026-10-29 (+07:00). P4 (desktop UI parity) in progress: sections 1-7 done, 8-10 pending.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309**. Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23. P4 builds no APK and installs nothing.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: 9590f2d7 (implementation baseline before this snapshot commit; section 7 is committed together with this snapshot)
- Current build: phone 4.24-dev.5/code309 (SHA-256 93E86E94...B766); PC package 4.24-pc-dev.6 (zip + unsigned .msi) until P4 builds a new one.
- Current phase: P4 section 8 (Cai dat) next
- Completed tasks: P0a..P3. P4 sections 1 navigation, 2 RAW, 3 Glossary, 4 Pronoun, 5 Tap, 6 Dich, 7 Cong viec/Lich su, each with tests (T1-T5, T7). One core move: JobsMainPageFactory.statusText -> translation-core JobStatusText (commit 9590f2d7). Clean-archive check of sections 1-5 (dbe4ff2a): app 747/5 skip/0 fail, core 61, jdbc 3, desktop 46, lint 0, golden 14/14 twice, Editorial diff empty.
- Pending tasks: P4 sections 8 Cai dat (+T6, T8), 9 Giao dien, 10 hoan thien chung; clean-archive checks after section 6/9/10; screenshots in D:\P5E-builds\qa-p4\shots; new PC package with -Msi.
- Known bugs/limits: schema upgrade v1-v29 NOT_MEASURED. P3 QA seed pronoun files used scope "all" (invalid, refused by the phone rule); fixed to "*" in P4. Whole-volume live run needs a separate owner spend approval, otherwise NOT_MEASURED.
- Regression status: sections 1-5 verified on a clean archive (see Completed tasks); sections 6-7 compiled and desktop tests pass, full clean check pending.
- Next action: implement P4 section 8 (settings groups, auto-save, confirmation dialogs for provider calls).
