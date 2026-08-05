# Editorial Pack Platform — G2-C0C Runtime Profile Resolution

Status: `BLOCKED` — C0C-A `PASS`; C0C-B was stopped before production wiring.

Date: `2026-08-05`

This handoff records the approved C0C-A pure-JVM resolver/adapter seam and the
review stop that prevents C0C-B from writing incomplete trusted-profile
evidence into SQLite v14. No certification, activation, project binding,
execution, folder import, migration, APK build or device import was performed.

## Baseline and final state

- Branch: `feature/v4.16` at start and end.
- HEAD at start: `3597e9d556e7e0edaef51267d990129cdb2cacc5`.
- C0C-A implementation baseline: `91b9bbed92d4180dd6a038a2812818664d6a411a`.
  The later documentation/state commit is intentionally not self-referenced
  here; the final branch HEAD must be checked when this handoff is resumed.
- Baseline trusted profile canonical hash:
  `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`.
- Baseline machine-contract fingerprint:
  `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`.
- Production capability recognized by the C0B evidence catalog:
  `pack.integrity.sha256.v1` only.
- Gradle `9.3.0`, AGP `8.7.3`, JDK runtime JBR `21.0.10`, Java source/target
  `17`, compile/target SDK `35`, SQLite v14.
- Latest accepted artifact is unchanged: `4.16-dev.30` / code `92`, event
  `build-20260804-182006`.

## Files changed

- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityEvaluationResult.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityReasonCode.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineProfileAdapter.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineProfileResolver.java`
- `editorial-engine/src/test/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineProfileResolverTest.java`
- `WORKSPACE_SNAPSHOT.md`
- This handoff, `BUILD_STATE.md` and `release_checklists/v4.16.md` are the
  documentation/state updates for the review stop.

The three user-owned files `.idea/compiler.xml`, `.idea/gradle.xml` and
`.idea/misc.xml` were not edited, staged, stashed, committed or overwritten.
No app importer, MainActivity, UI, database/migration, SAFE4, manifest/registry
or candidate-pack file was changed.

## C0C-A — pure-JVM resolver and adapter

The resolver receives an imported `EditorialPackManifest` and the bundled
read-only `EditorialEngineContractProfileRegistry`. It performs this exact
sequence:

1. Read only already validated registry entries; a registry exception, null
   entry, invalid profile, duplicate identity or duplicate canonical hash is a
   hard failure.
2. Revalidate the profile object and independently detect canonical profile
   hash and machine-contract fingerprint mismatches.
3. Keep only active profiles with an executable contract: both contract bounds,
   at least one supported schema and at least one supported phase are required.
   Contract versions are checked against the approved inclusive bounds and
   schema versions against the exact allow-list.
4. Return `UNSUPPORTED_CONTRACT`, `UNSUPPORTED_SCHEMA` or
   `NO_TRUSTED_PROFILE` when there is no candidate. Return
   `AMBIGUOUS_TRUSTED_PROFILE` when more than one candidate remains; there is
   no latest/closest/default priority.
5. Convert the one selected trusted profile through
   `EditorialEngineProfileAdapter` and call the existing G2-A
   `EditorialCompatibilityEvaluator`.

The imported manifest never selects a profile and no profile is read from the
ZIP, SQLite, SAF, filesystem outside the APK/source bundle, network or `D:`.

### Adapter field mapping

`EditorialEngineProfileAdapter` maps only evaluator facts:

- `engineVersion` → evaluator engine version.
- `implementedCapabilities` → evaluator capability set and contract support
  capability set. The production evidence boundary still recognizes only
  `pack.integrity.sha256.v1`.
- Approved contract bounds/schema allow-list plus the incoming manifest key →
  one evaluator `ContractSupport` whose trusted fingerprint is the profile's
  `machineContractFingerprint`.
- Each declarative C0A adapter descriptor → evaluator `AdapterSupport`; the
  installed flag is derived only from the profile's bundled adapter ID list.

Profile ID/version, canonical profile hash, creation time, source commit and
deprecation metadata are not copied into evaluator facts. They remain identity
and trust evidence on the orchestration result. The adapter does not create
executable adapters or infer missing semantic fields.

`EditorialCompatibilityEvaluationResult` is immutable, exposes the selected
trusted profile separately, sorts missing capabilities deterministically and
does not expose certification, activation, binding or execution operations.

## Stable reason codes and state mapping

The result model defines the required stable codes:

`NO_TRUSTED_PROFILE`, `TRUSTED_REGISTRY_INVALID`, `PROFILE_HASH_MISMATCH`,
`MACHINE_FINGERPRINT_MISMATCH`, `AMBIGUOUS_TRUSTED_PROFILE`,
`UNSUPPORTED_CONTRACT`, `UNSUPPORTED_SCHEMA`, `ADAPTER_REQUIRED`,
`MISSING_ENGINE_CAPABILITY`, `ENGINE_UPGRADE_REQUIRED`, `INVALID_PACK` and
`COMPATIBILITY_PERSISTENCE_FAILURE`.

The intended C0C-B mapping, not yet persisted because of the blocker, is:

| Evaluator outcome | Storage state |
|---|---|
| `DATA_COMPATIBLE` | `STORED_READY_FOR_CERTIFICATION` |
| `ADAPTER_REQUIRED` | `STORED_BLOCKED` |
| `ENGINE_UPGRADE_REQUIRED` | `STORED_BLOCKED` |
| `INVALID` | existing invalid/rejection policy; never ready |
| `BLOCKED` | `STORED_BLOCKED` |

`STORED_READY_FOR_CERTIFICATION` would mean only integrity and compatibility
were evaluated. It would not mean certified, active, executable, project-bound
or SAFE4-complete.

## Why C0C-B is BLOCKED

The live v14 migration specification and compatibility table contain:

`canonical_pack_hash`, `engine_version_used`,
`machine_contract_fingerprint`, `compatibility_class`, `required_class`,
`blocked_reason` and `evaluated_at`.

They do not contain trusted profile ID/version/hash, adapter-set hash,
capability fingerprint or evaluation-context identity. The reviewed plan
explicitly forbids serializing those identities into `blocked_reason`, changing
old rows or pretending that the machine fingerprint alone identifies a
profile. Therefore C0C-B cannot persist the requested trusted-profile
provenance without a separate additive immutable migration. No migration was
created or executed in this task.

There is a second semantic blocker. The one bundled production profile is
intentionally a truthful bootstrap profile with null contract bounds, empty
schema/phase/context/gate/release/adapter descriptors and only the integrity
capability. It is a no-executable-contract profile, not a wildcard. An
integrity-only fixture cannot be reported as `DATA_COMPATIBLE` until a reviewed
production contract/schema and machine semantics are added to the profile.
Adding guessed values would violate C0A/C0B trust boundaries.

Consequently, no production resolver was injected into
`EditorialPackImportService`, no MainActivity composition root was changed, no
compatibility row was written, no existing pack/QA row was reevaluated and no
UI state was changed. The app remains at its prior fail-closed behavior.

## Test and regression evidence

Focused C0C-A suite:

- `EditorialEngineProfileResolverTest`: `6/6` pass, `0` failures, `0` errors,
  `0` skips.
- Covered production no-executable-contract rejection, adapter mapping without
  metadata leakage, profile hash mismatch, machine fingerprint mismatch,
  duplicate trusted identity, deterministic missing-capability ordering and
  immutable result collections.

Required Wrapper/JDK evidence:

```text
.\gradlew.bat --version
Gradle 9.3.0; Launcher JVM 21.0.10; Android Studio JBR 21

.\gradlew.bat :editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon
PASS: editorial-engine 56/56; app JVM 161/161; instrumentation compilation PASS; 0 failures/errors/skips.

.\gradlew.bat :app:lintDebug --no-daemon
PASS: 0 errors; 53 existing warnings.

git diff --check
PASS.
```

JDK preflight `scripts/test-java-toolchain-preflight.ps1`: `4/4` pass — JDK
21 accepted, Java 8 rejected, missing `JAVA_HOME` rejected, invalid path
rejected without exposing the supplied path. All Gradle commands used the
repository Wrapper; no cached Gradle executable was used as a replacement.

## Artifact, device and safety boundary

No archive-first build was run because C0C-B was blocked before production
wiring. The latest accepted code92 artifact remains unchanged:

- APK SHA-256:
  `07BC98B22019832AFD37D0307E691957FBDC47D1C0E929949535D69AFB472801`.
- Source ZIP SHA-256:
  `BD6BCDC832DC5C3D9AFFA3E1C597A12D090F560F6B8D6319D0D65F7FB22C1126`.
- Artifact/backup five-file parity remains verified for event
  `build-20260804-182006`.
- `BUILD_INFO.json` provenance remains the G2-T0B record for Gradle/AGP/JDK/
  SDK/Git/APK/source ZIP.

Device QA: `PENDING / NOT RUN`. No new APK was built or installed, and no
fixture was imported. The existing code92/latest accepted artifact and the
existing blocked QA row were not changed. `EditorialSafe4Pack.executionEnabled()`
remains `false`; the canonical SAFE4 hashes and nine missing capabilities are
unchanged. Candidate `DBE214...` and `3B2FCC...` were not read, imported or
activated.

## Worktree and decision

The final worktree after documentation commit contains only the three protected
user-owned `.idea/*` modifications, all unstaged. No push, merge or tag was
created.

Decision: `G2-C0C BLOCKED` (`C0C-A PASS`; `C0C-B BLOCKED`).

Next proposed step, not implemented: separately review and approve an additive
immutable evidence migration plus reviewed executable contract semantics for
the production profile, then resume C0C-B. Certification, activation, project
binding, execution and SAFE4 capability implementation remain out of scope.
