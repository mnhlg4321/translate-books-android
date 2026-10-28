# Workspace Snapshot

- Updated: 2026-10-28 (+07:00). P3 done except the installer (waits for the owner) and the unmeasured live/phone-import checks; phone runs 4.24-dev.5.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.5/code309** (installed with owner approval). Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: df0f6179 (implementation baseline before this snapshot commit)
- Current build: phone 4.24-dev.5/code309 (SHA-256 93E86E94...B766); PC package 4.24-pc-dev.4.
- Current phase: P3 desktop full finished except step 8 (installer)
- Completed tasks: P0a..P2 (golden prompt tests, translation-core, ports + JDBC, desktop shell with one real run of $0.005693). P3: SettingsJson.toExportJson/fromImportedProfile (shared keys only), DesktopRawStore + tblraw:// reads, DesktopRuntime bridges for profile and library ZIP, ChapterQueue (loop body token-identical to the old TranslatorService loop) with 8 JVM tests.
- Pending tasks: installer (needs the owner to approve Temurin JDK 21 + WiX 3.x downloads); import of the owner's real phone export; whole-volume live run (needs spend approval).
- Known bugs/limits: schema upgrade v1-v29 is NOT_MEASURED (only the v30 schema from the phone backup is used). Lint has existing warnings, 0 errors. Whole-volume live run needs a separate owner spend approval, otherwise NOT_MEASURED.
- Regression status: clean archive of 3981b385: app 747 / 5 SKIP / 0 FAIL, core 61, jdbc 3, desktop 26, lint 0 errors, golden 14/14 twice, app:dependencies identical to P0b, Editorial diff empty.
- Next action: the owner decides on the JDK/WiX download for the .msi installer.
