# Workspace Snapshot

- Updated: 2026-10-01 (+07:00): owner chose option B for event 7 (RAW attempt 7a5e3428… is the only RECONCILE predecessor for chapter 001; formal verdict RAW_NOT_ACCEPTED unchanged). The M4 RECONCILE package is complete offline (app entry, opt-in instrumented test, lean host script). G3/G4 offline added the L2 OpenRouter adapter and the L3 boundary. No provider, device, build or DB action.
- Current version: active release v4.18; installed production 4.18-p5e.3/code211 (APK E9CF282C…), AndroidTest CCAAE0AD…; neither contains this work.
- Current branch: feature/v4.18-p5e-runner-repair-20260917; same continuation, no new branch/release/checklist.
- Current commit: e8b21d26 — implementation baseline immediately before this snapshot update (4a603695 M4 entry, 1f8e7334 M4 test, 63b288fe L2 adapter, a64ae460 M4 host script, e8b21d26 L3 boundary).
- Current build: 4.18-p5e.3/code211, build-20261001-190833 (unchanged). A new build from HEAD has schema v25 and migrates the pilot DB on first open.
- Current phase: P5 incomplete; EVENT7_RAW_ACCEPTED_AS_PREDECESSOR_BY_OWNER_EXCEPTION_B / M4_OFFLINE_READY / M4_LIVE_SCOPE_APPROVAL_PENDING / P5_EXIT_NOT_CLAIMED; P6 offline: L2 + L3 engine boundaries, L2 adapter and v25 store done; coordinator/UI/export not started.
- Completed tasks: G1; G2 offline (compact RECONCILE wire, executeReconcile, dispatchReconcile, RECONCILE-only fresh adapter, M4 instrumented test, M4 host script); G3 (reconstructor, L2_EDIT boundary, v25 store, L2 adapter); G4 core (L3 boundary, release numbers, QA_RECEIPT).
- Pending tasks: owner approval of the M4 scope; M4 build/install/event, M5–M7; L3 OpenRouter adapter, L2_RAW_DISCOVERY count check; G5 app coordinator L1→L2→L3 + UI/save/reopen/export; G6 three finals; P7.
- Known bugs/gaps: none of the new code has run on a device; M4 host script runs live only via library functions untested on hardware; its stdout summary drops the p5e.reconcile.v1 status keys (DB readback is the authority); L2/L3 edits are line-level only; collector fix 3057919b not yet exercised live; historical owner-window failure UNRESOLVED; code196 loss stays FAIL.
- Regression status: editorial-engine 244/244, app unit 276/276, lintDebug PASS, androidTest compile PASS, M4 host self-test 74/74; no device test run.
- Workspace/data: owner .idea and old P5E docs edits plus untracked evidence preserved and not staged; event 7 evidence unchanged; device DB 9fa69f6b… holds the committed RAW and must not be restored.
- Evidence pointers: docs/P5E_EVENT7_SUPPLEMENTARY_VERIFICATION_20261001.md (section 5 = decision B); EDITORIAL_RECOVERY_V4_18.md sections 9a and "G2 chi tiết"; scripts/p5e-m4-reconcile-event.ps1.
- Current Next action: owner approves the M4 scope — build and install new production/test APKs on serial 15e84958 (v24→v25 migration with a pre-install DB copy), then one RECONCILE call capped at USD 0.05 / 104,096 tokens / 120 s via scripts/p5e-m4-reconcile-event.ps1.
