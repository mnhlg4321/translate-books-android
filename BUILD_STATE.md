# Build State

- Status: `V4_17_TRANSLATION_PROFILE_RECOVERY / D0_DOCS_COMPLETE / D1_BASELINE_READY`.
- Active authority: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`.
- Active branch: `feature/v4.17-translation-profile-compatibility`.
- Current implementation baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7`, the exact code113 source baseline immediately before the D0 documentation changes; not self-referential.
- Current released baseline: `v4.15` / code62 remains unchanged.
- Frozen later development artifact: `v4.16-dev.104` / code168, `TranslateBooks-v4.16-dev.104-code168.apk`, 3,331,375 bytes, SHA-256 `E235BB3640039E48DD1E1C70264A25158ED4B6E9146860A6730644790E585F35`. Its RSC/Relation-Speaker, Editorial activation, IPC and live-canary next actions are historical and not active for v4.17.
- Recovery source artifact: `v4.16-dev.51` / code113, event `build-20260806-175154`, `TranslateBooks-v4.16-dev.51-code113.apk`, 2,786,334 bytes, SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; source archive SHA-256 `1C7363410BF113C99F3AF76A30F47BC4F458B207364A1C275D45A64D2722C8C1`.
- Next build policy: no APK has been built for v4.17. The first v4.17 APK must use `scripts/build-and-save.ps1`, a unique version name and Android `versionCode` greater than 168.
- Product scope: chapter-by-chapter manual RAW/Glossary/Pronoun selection; Glossary runtime fields `source,target,category,note` with `priority` ignored; Pronoun fields `from,speaker,target,self,call,scope,note` plus legacy three-column compatibility.
- Completed: isolated clean worktree and single release branch created from code113; single release checklist and canonical recovery plan created; workflow, README and current-state files normalized; documentation sequence/schema/link/current-only-state checks PASS; application-source diff count is 0.
- Pending: D1 baseline characterization before any product-behavior change, followed strictly by D2 Glossary, D3 Pronoun, D4 integration and D5 release.
- Known issues: the original later-track workspace contains protected user-owned dirty/untracked evidence and remains untouched. No current external blocker is known for the v4.17 local recovery path.
- Regression status: D0 documentation consistency PASS and `git diff --check` PASS; application-source diff count 0. Application regression has not started; no application source, APK, database, provider, emulator or user data has been changed.
- Exact next action: execute D1 baseline inventory and focused code113 characterization tests before changing production behavior.

Git history preserves earlier BUILD_STATE entries. This file intentionally contains current state only.
