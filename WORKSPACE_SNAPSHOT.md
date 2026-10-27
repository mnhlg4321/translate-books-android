# Workspace Snapshot

- Updated: 2026-10-23 (+07:00). B1–B3 implementation, clean archive regression, dev.26 install, and bounded phone trial are complete.
- Current version: v4.23.0 in development; retained candidate **4.23-dev.26/code304**. Shipped release remains v4.21.0/code271.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24); feature/v4.23 still open for the v4.23 release
- Current commit: 88fd1c81 (implementation baseline before this snapshot commit)
- Current build: `4.23-dev.26/code304`, event `build-20261023-220435`, source `be5017d0`; APK SHA-256 `312D592B7654C64DD7E780B0B508A147B78B7D747FA5008B342D842AB373EDC6`; the clean archive source is `D:/P5E-builds/v423-polish-b1b3-clean-20261023-01/`.
- Current phase: v4.24 PC shared core - P0a and P0b done and verified; translation-core holds 71 files; 4.24-dev.1/code305 installed on phone 15e84958 with owner approval (given in the side session), runs without crashes
- Completed tasks: B1 single-line Library tabs with adaptive horizontal scroll and pure sizing test; B2 `plainCard`, repeated-heading removal, RAW one-line description, and pure header test; B3 disabled recommendation undo with helper/accessibility text and pure state test; full worktree regression; clean archive regression; retained dev.26 APK; pre-install phone backup; validated install; B1 and B3 screenshots; filtered settings round-trip. No provider call, Editorial file, prompt/instruction, translation logic, schema, or personal data change.
- Pending tasks: B2 Glossary/Pronoun/Tập phone screenshots remain NOT_MEASURED because the device had no `QA_` library item and non-QA_ content was not photographed. No provider, paid action, delete action, merge, tag, or push is pending.
- Known bugs/limits: `recommendationUndo` remains in memory only by design; system font 130% was NOT_MEASURED because the shell lacked `WRITE_SETTINGS`; existing lint warnings remain but lint has 0 errors. Backups remain outside Git and were not extracted into the evidence folder.
- Regression status: clean `git archive` at `D:/P5E-builds/v423-polish-b1b3-clean-20261023-01/` passed `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestJavaWithJavac`, and `:app:lintDebug` with 730 tests, 5 SKIP, 0 FAIL/ERROR, and 0 lint errors.
- Next action: coordinator writes the P1a work request (move the storage/file layer: *Store, TranslationRepository, FileUtil, SettingsStore behind ports, plus the 16 files still blocked) for owner review.
