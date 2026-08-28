# Workspace Snapshot

- Updated: `2026-08-28`.
- Status: `D1_BASELINE_COMPLETE / D2_GLOSSARY4_READY`.
- Current version: `v4.17-translation-profile-compatibility`; no v4.17 APK exists.
- Current branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113); D0 commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- Current commit baseline: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`, the implementation/documentation baseline immediately before this D1 state/snapshot commit; not self-referential. Confirm actual HEAD when resuming.
- Current build: recovery code113 APK SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no build, install or replacement.
- Current phase: `D2_GLOSSARY4`.
- Completed tasks: reconciled D1 on exact code113; verified the real Glossary five-column projection and Pronoun seven-column projection; directly reread the canonical `MainActivity.java` and `TranslationEngine.java`; proved legacy three-column behavior; completed final `85/85` focused JVM assertions with zero failures/errors/skips.
- Pending tasks: `D2_GLOSSARY4` → `D3_PRONOUN7` → `D4_INTEGRATION` → `D5_RELEASE` strictly in order.
- Known bugs/blockers: Glossary note is read as aliases then lost at the store adapter; CSV priority is dropped; P3 header becomes `from → speaker: target`; P3 tail `self/call/scope/note` is lost at parser projection. These are baseline observations, not desired contracts.
- Regression status: final focused JVM run `85 tests / 0 failures / 0 errors / 0 skipped`; initial SDK-path failure and test-only generator expectation failure were repaired in this branch; no production-source diff, APK, Android/device/API/RSC/Editorial action.
- Protected state: original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its dirty/untracked state remain untouched; old v4.16 D1 commits are read-only references only.
- Exact next action: write focused failing/acceptance tests for Glossary4 before changing production.

This is current-only state; Git history preserves prior snapshots.
