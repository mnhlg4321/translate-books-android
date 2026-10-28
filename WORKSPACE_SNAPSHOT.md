# Workspace Snapshot

- Updated: 2026-10-28 (+07:00). P3 in progress: profile/library transfer, PC RAW library, ChapterQueue in the core are done and tested; whole-volume screen, history, palettes, cleanups, the single phone build and the installer question are still to do.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.4/code308** (P2 stage 1, installed with owner approval). Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: 1f88db7b (implementation baseline before this snapshot commit)
- Current build: phone `4.24-dev.4/code308`; PC package `4.24-pc-dev.4` (ZIP with bundled runtime, SHA-256 `4FDF299253342EA3DBDE340896E95650AEDFF13F026C68C2752FAC6C0C291D64`).
- Current phase: P3 desktop full (docs/V424_P3_DESKTOP_FULL_WORK_REQUEST.md, section 6b wins), step 3 of 8.
- Completed tasks: P0a..P2 (golden prompt tests, translation-core, ports + JDBC, desktop shell with one real run of $0.005693). P3: SettingsJson.toExportJson/fromImportedProfile (shared keys only), DesktopRawStore + tblraw:// reads, DesktopRuntime bridges for profile and library ZIP, ChapterQueue (loop body token-identical to the old TranslatorService loop) with 8 JVM tests.
- Pending tasks: whole-volume PC screen, history tab, Palette in core with 5 themes and font sizes, cleanups (decodeText delegate, unused overload, byte-vector test), one phone build 4.24-dev.5 with backup/counts and the owner's typed "duyệt cài", installer question (JDK/WiX) to the owner.
- Known bugs/limits: schema upgrade v1-v29 is NOT_MEASURED (only the v30 schema from the phone backup is used). Lint has existing warnings, 0 errors. Whole-volume live run needs a separate owner spend approval, otherwise NOT_MEASURED.
- Regression status: last clean-archive regression (P2, `d93968f4`): app 748 / 5 SKIP / 0 FAIL, core 44, jdbc 3, desktop 10, lint 0 errors, golden 14/14 twice. Since then core and desktop test suites pass in the worktree; the full app regression is rerun before the P3 phone build.
- Next action: wire the PC whole-volume screen onto ChapterQueue (step 3 of the coordinator order), then history, palettes, cleanups.
