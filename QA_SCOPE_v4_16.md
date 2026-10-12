# QA Scope — v4.16 Release Candidate

## Candidate objective

Validate Editorial Workflow V5 end to end without regressing the existing Translate, Jobs, Library, Settings, Glossary or Pronoun workflows. The exact release candidate must be built and retained through `scripts/build-and-save.ps1`, then tested on the archived APK identity.

## Current development baseline

- Latest archived development build: `4.16-dev.18` / code `80`; automated JVM/lint passed, device acceptance pending connection.
- Build event: `build-20260801-042014`.
- Source commit: `d8fa109`.
- APK SHA-256: `664A6FBF6C47B52CA842571F1B30C2B6BE948BC4A69458BA2FA44857BB8680B5`.
- Automated baseline: 147 JVM tests, lint 0 errors/53 warnings, Editorial device suite 10/10.
- Code79 contains the responsive two-line five-tab navigation and a persisted per-project Release folder/direct-write path. Device visual verification is pending because the connected phone is secured at the lock screen; no attempt was made to bypass it.

## Manual Release walkthrough completed on code78

- Device: OnePlus CPH2691, Android 15 / API 35, 1264×2780.
- A uniquely named disposable project/chapter was seeded at `RELEASE_READY` and removed after QA.
- Opening Release showed the gate/redaction confirmation before the system picker.
- Aborting the picker and restarting the caller retained `RELEASE_READY`; no QA ZIP existed before the successful save.
- Successful Save returned to Editorial, changed the chapter to `RELEASED`, and removed the Release action.
- The actual 1,473-byte ZIP opened successfully and contained exactly `FINAL_QA.txt`, `EVIDENCE_REDACTED.json`, and `SHA256SUMS.txt`.
- Both checksums in `SHA256SUMS.txt` matched the extracted files.
- The evidence manifest used `hash-length-status-only`; test RAW, DRAFT, Glossary, Pronoun, prompt and input-manifest secrets were absent.
- The exact device-side QA ZIP and disposable project were deleted after capture. The test instrumentation package was uninstalled.
- Retained evidence is mirrored under `artifacts/qa/v4.16/manual-release-code78/` and `backup/qa/v4.16/manual-release-code78/`.

## RC test matrix

### P0 — release blockers

1. Archive-first exact v4.16 candidate build, artifact/backup parity, checksum manifest and source ZIP verification.
2. Full JVM suite and Android Lint on the exact candidate source.
3. Full connected instrumentation, with paid real-provider tests skipped unless explicitly authorized.
4. Existing Translate smoke test: single TXT and multi-TXT selection, translate start/cancel/resume/retry, output write and job-log export.
5. Editorial import: batch filename mapping, project-owned Glossary/Pronoun selection/update, proof that Translation profiles are never used as fallback, duplicate/missing-role rejection and immutable chapter asset hashes.
6. L1: inline and segmented audit, REPORT_L1 contract/export, failed-scene-only retry and no translation mutation.
7. L2: RAW-first barrier, structural/model mapping, segmented checkpoints, aggregate VI_L2 and failed-scene-only retry.
8. L3: context isolation before REPORT_L1, independent scene checkpoints, chapter voice audit, final scene checkpoints and failed-scene-only retry.
9. Release: all persisted gates revalidated, open/stale/mismatched run rejected, save failure/cancel retains `RELEASE_READY`, successful write records manifest/hash and transitions once to `RELEASED`.
10. ZIP inspection: exact three-entry contract, UTF-8 FINAL_QA, checksum verification, no source/config/prompt/issue-quote/reasoning leakage.
11. Database upgrade from the released v4.15 schema with existing translation/config data preserved; include v11→v12 project references, cross-project isolation, Series/Volume correction and duplicate-identity rejection.
12. Process-death/cold-start checks during L1/L2/L3 failure and while the Release picker is open.

### P1 — usability and compatibility

1. Phone portrait, phone landscape and one compact-width/emulated layout.
2. Long series/chapter names, Vietnamese/Japanese filenames, spaces and filesystem-invalid characters in suggested ZIP names.
3. System DocumentsUI plus the OnePlus file-provider path; explicit cancel, Back, destination change, overwrite prompt, permission loss and out-of-space behavior.
4. Accessibility: touch target size, TalkBack labels, focus order, status meaning without color and text scaling at 100%/130%/largest.
5. Visual review of all five bottom navigation labels. Code78 visibly truncates several labels at 1264 px width; this requires an RC accept/fix decision.
6. Release destination discoverability. OnePlus restored the last-used folder (`3.RAW`) during QA, so the confirmation/picker copy must make destination choice clear.
7. Projects with 0, 1, 20 and 100 chapters; scrolling, refresh and reopening released chapters.

### P2 — performance and retained evidence

1. Cold-start Macrobenchmark and Perfetto evidence on the exact candidate.
2. Memory/time checks for long-chapter segmented L1/L2/L3 and ZIP creation of large FINAL_QA output.
3. Token/cost totals and model/prompt/workflow hashes remain available in local audit data without leaking into redacted content.
4. Actual-device screenshots, UI hierarchy, instrumentation XML, benchmark JSON/traces and a documented video or screenshot-sequence fallback.

## Exit criteria

- No P0 failure or unresolved data-loss/privacy issue.
- Every mandatory workflow gate has concrete retained evidence.
- Artifact and backup trees match by relative path, length and SHA-256.
- Exact APK identity is installed and verified after QA cleanup.
- Checklist steps 1–9 pass before PreTag; no tag, release archive or publication occurs while this scope remains incomplete.

## Current RC decisions required

- Verify on an unlocked physical device that the implemented two-line navigation removes clipping at 1264 px width and at large text sizes.
- Verify the implemented per-project Release folder through real SAF selection, persisted permission, direct ZIP write, duplicate naming and permission-loss fallback.
- Run an explicit visible Cancel/Back test on every supported picker; code78 verified abort/process-restart safety, but the OnePlus provider exposed no dedicated Cancel button in its save surface.
