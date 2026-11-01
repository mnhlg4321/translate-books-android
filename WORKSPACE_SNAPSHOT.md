# Workspace Snapshot

- Updated: 2026-11-01 (+07:00).
- Current version: v4.25 in progress; no v4.25 build exists yet.
- Current branch: `feature/v4.25` in `D:\P5E-builds\wt-v4.25` (existing owner-specified worktree).
- Current commit: `8551f78e0f59332ca34259c29c054d27e8358659` — implementation baseline immediately before this snapshot commit; it equals `main` and descends from annotated tag `v4.24.1` (tag target `01ab5313fd7f7c2e9bd97f96b307cc10a1c9720f`).
- Current build: inherited release APK v4.24.1/code319, SHA-256 `655576E98D440C754E1BC8CF0A1F611DADDE97F64C0820887217A3713397D00C` (build event `build-20261030-073455`); this is not a v4.25 build.
- Current phase: v4.25 scope preparation; the work request and checklist were reviewed and the initial documentation commit records this snapshot. Product decision served: whether to keep the bounded Translate reliability and clarity changes in a v4.25 release after review.
- Completed tasks: verified branch/worktree, HEAD, `main`, tag ancestry, configured artifact junction targets and `local.properties`; reviewed and corrected the v4.25 work request's branch and tag lineage; recorded checklist evidence for startup steps 01–04.
- Pending tasks: implement Q1–Q4 and L1 from `docs/V425_TRANSLATE_SCREEN_WORK_REQUEST.md`; run its requested regression and emulator QA; build only with `scripts/build-and-save.ps1`. Provider calls need separate spend authority, phone installation needs separate owner approval, and tag/merge/push remain outside this setup task.
- Known bugs/limits: the v4.25 request documents job 354's repeated `RETAINED_SOURCE_LINE` rejection for retained Japanese lines and 1,349 output-recovery copies (about 55 MB) on the phone; neither is fixed in this branch yet. Haiku behavior after a repair hint is `NOT_MEASURED`.
- Regression status: v4.25 regression not run (documentation-only kickoff). The v4.24.1 baseline in `BUILD_STATE.md` reports app 753 pass / 5 skip / 0 fail, core 61, JDBC 3, desktop 70, golden 14/14 twice, lint 0 errors; those results are not v4.25 evidence.
- Next action: implement Q1's app-generated repair hint for `RETAINED_SOURCE_LINE` retries in this existing worktree, with provider calls and phone actions kept at zero.
