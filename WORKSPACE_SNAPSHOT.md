# Workspace Snapshot

- Updated: 2026-11-04 (+07:00), after the lib-a coordinator follow-up code was written and desktop-tested in the worktree; clean-archive run, screenshots and PC package come in later commits.
- Current version: v4.25 development build `4.25-dev.9` / Android versionCode `328` (phone, unchanged). PC package for the follow-up is not built yet (target `4.25-pc-dev.3`).
- Current branch: `feature/v4.25` in `D:\P5E-builds\wt-v4.25` (owner-specified existing worktree).
- Current commit: `31eed2ac` — baseline immediately before this snapshot commit (lib-a code `16f2a1ca`, lib-a evidence `31eed2ac`).
- Current build: none for the follow-up yet.
- Current phase: lib-a follow-up (coordinator review of `16f2a1ca`): Tập tick boxes restored, bulk-delete dialogs (Xóa / Vẫn xóa, Cancel default), red danger delete button, one Dùng button on Glossary and Pronoun, second Tập in the QA seed, undo-bar and dialog screenshots. Code written; desktop tests 83 total, 0 failures, 0 skipped in the worktree.
- Completed tasks (follow-up): SetsPage no longer replaces the TickList cell factory (status colours moved into TickList); delete dialogs first button "Xóa", second "Vẫn xóa", Cancel is default on all Library bulk deletes; `.danger` style with `-tbl-on-err` (= onColor of the error colour, contrast tested on every palette); Glossary and Pronoun keep one "Dùng" (ticked row when exactly one is ticked, else the focused row); QaSeed adds `QA_tap_mot_chuong`; Screenshots: Tập 1-of-2 selection, second-confirmation dialog, undo bar after a QA delete.
- Pending tasks: clean archive of the code commit with the four test tasks; screenshots from the clean archive, inspect light and dark; evidence commit; PC package `4.25-pc-dev` from a clean tree (no install); checklist and snapshot.
- Known bugs/limits: none new. Phone and provider behaviour NOT_MEASURED; no provider calls.
- Regression status: worktree desktop tests 83 total, 0 failures, 0 skipped (before the code commit). Clean-archive run not yet done.
- Next action: commit the follow-up code, then run the clean-archive tests from that commit.
