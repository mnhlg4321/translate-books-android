# Workspace Snapshot

- Updated: 2026-10-12 (+07:00): release v4.19 opened (branch, checklist 01-04, canonical plan); offline pronoun-miss suite and diagnosis in progress.
- Current version: v4.19.0 in development; shipped baseline v4.18.0 (tag `v4.18.0` on `9d02976e`), not changed.
- Current branch: `feature/v4.19` in worktree `D:\P5E-builds\wt-v4.19` (cut from `main`).
- Current commit: `998da341f4794987c6c309eb0fd1fbb3950348e6` is the implementation baseline immediately before this snapshot commit (not a self-reference); it equals `main` and `origin/main`. Confirm actual HEAD when resuming.
- Current build: none for v4.19. Last production build 4.18.0 / code 266 from `82626ac8`, APK SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`; events `build-20261012-153252`, `build-20261012-161228`, `tag-v4.18.0` in `artifacts/releases/v4.18.0/` and `backup/v4.18.0/`.
- Current phase: X0_DOCS done; X1_SUITE (known-error suite + separate clean control set) and X2_DIAGNOSE (layers S1-S5) next. Canonical plan `EDITORIAL_PRONOUN_RECOVERY_V4_19.md`.
- Completed tasks: v4.19 branch and checklist created once (checklist steps 01-04 with evidence); canonical plan written; readings of `docs/EDITORIAL_CHUNK_PLAN.md` section 6, `RELEASE_NOTES.md`, finder and checklist code, and the private adjudication files (`D:\P5E-private\c6-guard-20261012\`, labels only counted) done.
- Pending tasks: build the suite and clean set; diagnose by layer on the 8-chapter corpus; propose the fix for the owner; then fix, regression, build, QA, tag, archive per the 14 steps. Checklist steps 05-14 open.
- Known bugs/limits (open, from v4.18.0): 006 keeps 13 pronoun misses; 008 keeps 7 `cậu` misses; 011 keeps 2:2 (correct fix blocked by the third-person rule), 12:2 (uncertain), 10:6 (variance); 007 chunk 005 historical wrong edits; rows with only `self` or only `call` produce no fix point. The model-side effect of CP-IMPL-14 was measured on 011 only. Historical WAL/SHM UNKNOWN and V5 flags are unchanged and out of scope. The owner's workspace `D:\App Translate Books` keeps 163 uncommitted entries that belong to no release commit.
- Regression status: no v4.19 regression run yet; no code changed. v4.18.0 totals are historical (frozen 116 PASS / 1 SKIP, chunk 21 PASS / 6 SKIP, product 15 PASS).
- Semantic progress: unchanged 3/3 owner-accepted chapters (006, 008, 011) with recorded limits; no new measurement.
- Spend: 0 provider calls / USD 0; no spend authority exists for v4.19.
- Next action: finish X1_SUITE and X2_DIAGNOSE offline (USD 0), then present X3_PROPOSE to the owner; no provider.
