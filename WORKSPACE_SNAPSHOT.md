# Workspace Snapshot

- Updated: 2026-10-14 (+07:00): v4.22 steps U1, U2, U3, P and G implemented; Q1 offline design committed; clean-archive regression, dev build and emulator QA next.
- Current version: v4.22.0 in development; shipped 4.21.0 / code 271 (tag `v4.21.0`), installed on the owner phone `15e84958`.
- Current branch: `feature/v4.22` in worktree `D:\P5E-builds\wt-v4.22` (from `main` `7c44f3a2`); not pushed yet.
- Current commit: `56789cac` is the implementation baseline immediately before this snapshot commit (not a self-reference); actual HEAD verified.
- Current build: none for v4.22. Shipped 4.21.0 / code 271 in `artifacts/releases/v4.21.0/tag-v4.21.0/`.
- Current phase: step05 develop: U1/U2 clean-archive PASS earlier; U3 and P committed (`239c3299`, `9c467ffc`, `56789cac`) without a recorded clean-archive run; G (glossary false points) done in this commit; Q1 offline design done (sidecar, no app change).
- Completed tasks: U1 four-file setup, U2 readable results, U3 Editorial settings, P foreground job; G rules (a) one-character Han source inside a longer word, (b) pronoun-row `from` wording inside a quotation, (c) character entry already referred to by a third-person pronoun with no other mid-sentence proper name; measured glossary points 21 -> 18 on 9 chapters (8 JAKUAKU + Kumo 013): the 3 removed are the known false points of Kumo 013, 0 JAKUAKU points where the owner FINAL added the term were removed; Q1 design document and offline sidecar with 36 assertions (reran: identical numbers).
- Pending tasks: clean-archive regression of the head commit (engine, app debug/release/benchmark, lint, AndroidTest compile); dev build `4.22-dev` (versionCode above 271) via `scripts/build-and-save.ps1`; emulator-5554 QA with the fake provider and screenshots; scan and push `feature/v4.22`; send the owner screenshots and APK path; owner approval is needed to install on the phone and again for release.
- Known bugs/limits: Editorial does not know who speaks each line (Q1 offline: reference attribution cuts inferred false ADDRESS points by 83-87% but catches 1 of 2 known Kumo errors, no paid measurement proposed); glossary rule (c) is skipped when the DRAFT line has another mid-sentence capitalised name; release builds are not debuggable so `run-as` backups do not work on the phone.
- Regression status: host U1/U2 archive PASS (engine/app debug-release-benchmark/lint/AndroidTest compile); engine glossary tests 10 PASS locally; full clean-archive run of the head commit pending.
- Semantic progress: Q0 on Kumo ch 013: 2 real pronoun errors in 220 lines, 0 fixed by the app (4.21.0); no new model measurement.
- Spend: USD 0 for v4.22; no provider call.
- Next action: run the clean-archive regression of the head commit, then build the dev APK and run the emulator QA; no phone install, provider call, tag or merge.
