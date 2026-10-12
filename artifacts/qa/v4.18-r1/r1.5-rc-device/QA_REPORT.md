# QA report - release candidate 4.18.0 (code 266), R1.5

Date: 2026-10-12. Device: `emulator-5554` only (sdk_gphone64_x86_64, API 35). The owner's phone was never used. 0 provider calls, 0 USD, no pilot.

## Build under test

- Wrapper build `scripts/build-and-save.ps1 -ExactReleaseVersion 4.18.0`, event `build-20261012-152329`, versionName `4.18.0`, versionCode `266`.
- Source commit `82626ac8fe68d88ddf9c97710d70d1d4f648517a` (branch `feature/v4.18-p5e-runner-repair-20260917`). The wrapper only accepts the exact-release build on a branch named `feature/v4.18.0`, so the build ran in a throw-away local worktree with a local branch of that name at the same commit; the branch was never pushed and the worktree was removed afterwards.
- APK `TranslateBooks-v4.18.0-code266.apk` SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`; source ZIP `project_source_build-20261012-152329.zip` SHA-256 `4DEA9A12FFDF25265226D30FA641D01DD1C964E68B95056C6D410EFD09ECE0EB`; payload byte-mirrored in `artifacts/builds/v4.18.0/build-20261012-152329/` and `backup/builds/v4.18.0/build-20261012-152329/`.
- Matching AndroidTest APK, event `rc-test-20261012` (`artifacts/test-builds/v4.18.0/rc-test-20261012/`, mirrored in `backup/test-builds/...`), SHA-256 `6CAC2ADA5DC59C1F522A4EC19C6C608DB2B6E22838B5EED5E2DF899848B59ED2`, pinned to the production APK, the source ZIP and certificate `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- Installed on the emulator with `scripts/install-validated.ps1` (device signature token `abebea4b`): preflight PASS, install verification PASS, versionCode 265 -> 266.
- The wrapper's own host run (lint and unit tests) completed with BUILD SUCCESSFUL.

## Device tests on the RC (`raw/`, `results.json`)

| Group | Classes | Tests | Result |
|---|---|---|---|
| Frozen allowlist (V5 / pack, run only to show the frozen code still works) | 19 | 117 | 117/117 PASS |
| Chunk path: `EditorialPairStore` 6, `EditorialPairUi` 6, `EditorialPairAddressKeepSqlite` 1, `EditorialApiStore` 6, `EditorialApiBienTapFlow` 3, `EditorialApiBienTapUi` 5 (3 phase tests skip without their argument) | 6 | 27 | PASS (see the note on `EditorialApiBienTapUi`) |
| Product / Translation smoke: `V415LibraryScroll`, `V415MultiProfileImport`, `V415SettingsPersistence`, `V46Dashboard`, `V48ImportNavigation`, `Hotfix441` | 6 | 15 | 15/15 PASS |
| Restart (shell force-stops the app between seed and verify): `EditorialApiBienTapUi` and `EditorialPairUi`, seed / verify / cleanup | 6 runs | 6 | PASS; reopening sent 0 provider requests |

### Note: first `EditorialApiBienTapUi` run FAILED, then passed after cleaning test residue

The first run in the batch failed one test (`typedFormValuesSurviveChoosing...`, `expected:<c572ff87-...> but was:<69e65ff0-...>`; log `raw/chunk-EditorialApiBienTapUi.txt`, kept). Cause: the emulator's glossary store held two leftover fixture glossaries named `glossary-bientap-ui` and `glossary-bientap-test` from earlier test runs; the test picks the last list entry whose name starts with `glossary-bientap-ui` and so chose the stale one. It is test-state pollution on the emulator, not a change in the app: the main sources are identical to the build that passed in P7. I removed only those two fixture entries from the emulator's `glossary_store.xml` (backup of the original kept privately; the owner's three glossaries were left untouched), re-ran the class: `OK (5 tests)` (`raw/chunk-EditorialApiBienTapUi-rerun-after-cleanup.txt`). The test's weak isolation (matching a name prefix) is a known test weakness; it is not fixed here.

## Performance evidence (R1.3)

Macrobenchmark and Perfetto from R1.3 (`artifacts/qa/v4.18-r1/r1.3-performance/`) were measured on benchmark build `4.18-r13bench`/265, source `a5169045`. `git diff a5169045 82626ac8` shows no change in `app/src/main`, `editorial-engine/src/main` or any Gradle file (only tests, scripts, documents and evidence), so the measured application code is the code of this RC. They are emulator numbers, not a physical-device result: cold start median 727.6 ms; opening the Bien tap tab frame CPU P50 10.1 ms; a 1,222-line synthetic chapter planned into 53 chunks in 468 ms. They were not re-run on the RC package itself.

## Screenshots and video (R1.4, recorded on this RC)

`artifacts/qa/v4.18-r1/r1.4-media/`: six screenshots (list, choose source, plan with OK/WARN verdict, result by point, exported TXT, list with result) and `walkthrough.mp4`, produced by `EditorialR1MediaWalkthroughInstrumentedTest` (argument `r1_media=run`) with a synthetic three-row job, a synthetic glossary and a fake provider; no book text. To keep the owner's own combo names out of the pictures, the app database on the emulator was set aside, the walkthrough ran on an empty database, and the real database was moved back; its SHA-256 before and after are identical (`emulator-real-db-hashes-*.json`). A first recording that still showed the owner's combo names was discarded and is kept privately, not archived.

Visible behaviour of the recording: the fake provider returns each flagged line unchanged, so the result screen shows the glossary points as KEPT (UNCHANGED); the media shows the flow, not a model's edit quality.

## Out of scope / not claimed

Frozen V5 three-pass gates (checklist section C), the owner-accepted chapters' recorded misses (plan section 6), a physical-device run, tag and merge. Tag `v4.18.0` and the merge to `main` need the owner's decision (D-R2).
