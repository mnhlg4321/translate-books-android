# Workspace Snapshot

- Updated: 2026-10-27 (+07:00). P1a (storage into translation-core) in progress; B1-B3 done.
- Current version: v4.24 in development (PC shared core); phone `15e84958` runs **4.24-dev.1/code305** (P0b, installed with owner approval). Shipped release remains v4.21.0/code271; v4.23 is still open on feature/v4.23.
- Current branch: feature/v4.24 (worktree D:\P5E-builds\wt-v4.24)
- Current commit: c7c53be2 (implementation baseline before this snapshot commit; P1a work request commit)
- Current build: `4.24-dev.1/code305`, event `build-20261027-164332`, source `90c18b7b`; APK SHA-256 `B85D2F1715F08A57672BF2274A6F96E9FAD5684A110C53C746B719FADEDA82AC`.
- Current phase: P1a B3 done (TranslationRepositoryCore over SqlDb in translation-core, app TranslationRepository extends it; OutputIntegrityAudit and SettingsJson moved/added); B4 next (Glossary/Pronoun/ChapterSet/Workspace cores), then B5 (move remaining 12 files).
- Completed tasks: P0a golden prompt tests (12 combinations); P0b translation-core with 71 moved files and seams S1-S5, installed on the phone; P1a B0 baselines (816 string literals / 368 unique, javap API list, 79/41/67 ContentValues/Cursor/SQLiteDatabase counts in TranslationRepository) and B1 ports.
- Pending tasks: P1a B2-B5, clean-archive regression, build 4.24-dev.2, phone backup and pre-install counts, owner "duyệt cài", post-install count comparison.
- Known bugs/limits: schema upgrade v1-v29 is NOT_MEASURED (no historical schemas); only the v30 schema from the phone backup is used as fixture. Existing lint warnings remain, lint has 0 errors.
- Regression status: B1 worktree run: app 744 tests / 5 SKIP / 0 FAIL, translation-core 16 tests / 0 FAIL, androidTest compile PASS. Last clean-archive regression (P0b, `90c18b7b`): app 744 / 5 SKIP / 0 FAIL, core 3, lint 0 errors.
- Next action: implement P1a B4 (GlossaryStoreCore, PronounStoreCore, ChapterSetStoreCore, WorkspaceSetModelCore behind KeyValueStore/SqlDb).
