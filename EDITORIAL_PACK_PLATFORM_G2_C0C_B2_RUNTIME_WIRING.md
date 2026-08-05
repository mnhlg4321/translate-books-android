# Editorial Pack Platform G2-C0C-B2 — Runtime Wiring

Date: 2026-08-05
Branch: `feature/v4.16`
Baseline HEAD: `922247b0ab78b924ef1c71f26e7d5f0a8d11534f`
Implementation HEAD before this documentation handoff: `6e148264e46279dbcf5340a6ee4b4a78a108fc07`
SQLite: v15
`EditorialSafe4Pack.executionEnabled()`: `false`

## Scope and changed files

G2-C0C-B2 connects the bundled trusted-profile resolver to new-pack import after integrity validation. It persists trusted compatibility provenance append-only through the existing v15 DAO and keeps the current production profile fail-closed. No certification, activation, project binding, execution, re-evaluation, migration or production profile change was made.

Implementation commits: `ce40cce`, `af899d8`, `c2e2837`, `99e39e9`, `6e14826`.

Scoped source/test files:

- `app/src/main/java/com/ml/tblandroidtxt/EditorialPackImportPageFactory.java`
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPackImportResult.java`
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPackImportService.java`
- `app/src/main/java/com/ml/tblandroidtxt/SqliteEditorialPackRegistry.java`
- `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialPackRuntimeWiringInstrumentedTest.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityEvaluator.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityProvenance.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialCompatibilityReasonCode.java`
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineProfileResolver.java`
- `editorial-engine/src/test/java/com/ml/tblandroidtxt/editorial/pack/EditorialEngineProfileResolverTest.java`

No `.idea/*` file is included. No production profile resource, SQLite migration or SAFE4 source/hash changed.

## Runtime dependency graph and sequence

`MainActivity` remains dispatch-only. `EditorialPackImportPageFactory` is the composition root and loads `BundledEditorialEngineContractProfileRegistry` once for the import coordinator. A registry-load failure creates only `EditorialEngineProfileResolver.failedClosed()`; it does not create empty fallback facts.

The worker sequence is:

1. SAF stream is opened once on the coordinator worker.
2. Import service snapshots the ZIP/input and validates integrity.
3. Existing identity/hash is checked before compatibility resolution. A duplicate returns the persisted pack result without resolver invocation or a new evaluation row.
4. A new pack is resolved by C0C-A from the bundled registry, adapted and evaluated outside the UI thread.
5. The decision is converted to a canonical v15 evaluation context. Timestamp, locale, UI wording, raw pack content, prompt/project/workflow bytes, machine paths and secrets are excluded.
6. Immutable storage is moved content-addressably.
7. Pack/files, the v14 compatibility projection, the v15 trusted evidence row and the import final state are committed in one SQLite transaction.
8. The UI receives and later reads the persisted blocked/result projection through the read-only registry. Opening or rebuilding the Editorial page does not evaluate or write.

## Eligibility and state gate

The production profile remains `com.ml.tblandroidtxt.editorial.engine.bootstrap` version `1.0.0`, canonical profile hash `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`, machine fingerprint `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`. Its only implemented capability is `pack.integrity.sha256.v1`; it has no executable contract bounds/schema/phase descriptor.

Therefore a production import resolves the trusted profile only for provenance, returns `PROFILE_NON_EXECUTABLE` with `ENGINE_UPGRADE_REQUIRED`, and stores `STORED_BLOCKED`. A defensive storage gate also rejects any accidental `DATA_COMPATIBLE` result from a non-executable trusted profile. No production `DATA_COMPATIBLE`, `STORED_READY_FOR_CERTIFICATION`, Certified, Active or Executable state was created.

Decision mapping:

| Compatibility decision | B2 storage state |
|---|---|
| `PROFILE_NON_EXECUTABLE` | `STORED_BLOCKED` |
| `MISSING_ENGINE_CAPABILITY` | `STORED_BLOCKED` |
| `ENGINE_UPGRADE_REQUIRED` | `STORED_BLOCKED` |
| `ADAPTER_REQUIRED` | `STORED_BLOCKED` |
| unsupported contract/schema | `STORED_BLOCKED` |
| invalid/ambiguous trusted registry | `STORED_BLOCKED` |
| evaluation or persistence failure | never ready; blocked import/fail-closed |
| invalid integrity | existing rejection policy |

## Stable reason codes

The resolver/import boundary uses machine-readable codes, separate from UI wording: `TRUSTED_REGISTRY_INVALID`, `NO_TRUSTED_PROFILE`, `AMBIGUOUS_TRUSTED_PROFILE`, `UNSUPPORTED_CONTRACT`, `UNSUPPORTED_SCHEMA`, `PROFILE_NON_EXECUTABLE`, `MISSING_ENGINE_CAPABILITY`, `ADAPTER_REQUIRED`, `ENGINE_UPGRADE_REQUIRED`, `INVALID_PACK`, `COMPATIBILITY_EVALUATION_FAILURE` and `COMPATIBILITY_PERSISTENCE_FAILURE`.

Missing capabilities are sorted before returning and before composing the persisted blocker detail. Persistence failure rolls back the pack/files/v14/v15 transaction, retains immutable storage for recovery, and marks the import with the stable `COMPATIBILITY_PERSISTENCE_FAILURE` prefix.

## SQLite v15 evidence written

For a resolved trusted profile, `EditorialPackCompatibilityEvaluationDao.append()` uses INSERT only and writes:

- pack canonical hash;
- trusted profile ID/version and canonical profile hash;
- trusted machine-contract fingerprint;
- evaluator contract version `editorial-compatibility-v1`;
- deterministic adapter-set and capability fingerprints;
- canonical evaluation-context fingerprint;
- compatibility outcome, stable reason code and sorted blocker details;
- evaluation timestamp;
- immutable evaluation identity `importId:compatibility:v1`, import ID, pack row ID and v14 compatibility-result ID.

If no trusted profile is available, the v15 table is not written because its trusted fields are NOT NULL. The existing v14 blocker projection is used with no fabricated profile identity/hash; this is an unattested fail-closed result, not trusted evidence. Legacy rows remain `LEGACY_UNATTESTED` and are never backfilled.

## Regression and device evidence

Final Wrapper/JDK commands and results:

```text
.\gradlew.bat --version
Gradle 9.3.0; Launcher/Daemon JBR 21.0.10 (JetBrains)

.\gradlew.bat :editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon
editorial-engine: 60/60 pass, 0 fail, 0 error, 0 skip
app JVM: 162/162 pass, 0 fail, 0 error, 0 skip
instrumentation Java compilation: PASS

.\gradlew.bat :app:lintDebug --no-daemon
0 errors, 53 existing warnings

git diff --check
PASS
```

The final connected class `EditorialPackRuntimeWiringInstrumentedTest` passed 5/5 on OnePlus `CPH2691`, Android 15/API 35, using the archived APK without clearing app data:

- production non-executable profile: `ENGINE_UPGRADE_REQUIRED` / `STORED_BLOCKED`, full profile hash/fingerprint/context persisted;
- test-only unsupported contract: `UNSUPPORTED_CONTRACT`, blocked v14 projection, no v15 trusted row and no fallback;
- v15 append failure: transaction rollback, no pack/compatibility/evidence row and no ready state;
- duplicate import: `Pack đã tồn tại` result path, resolver called once, no second evaluation/history row;
- process restart: persisted profile provenance and blocker remain readable.

The test-only profile is not in the production registry and is not production capability evidence. `EditorialSafe4Pack.executionEnabled()` remains false.

## Archive and parity

Archive-first command:

```text
.\scripts\build-and-save.ps1 -Series 4.16-dev
```

Final build: `4.16-dev.36`, versionCode `98`, event `build-20260805-095511`, source commit `6e148264e46279dbcf5340a6ee4b4a78a108fc07`.

- APK SHA-256: `9E147135DEA5D37EDFF5220D5EC8C44CE2686438DE3831BC5EC564E7BC752EC8`
- Source ZIP SHA-256: `B8B518270092D03ECC4D479DC93AB2C55EFF027A395600582EECAA5797799F22`
- Artifact: `artifacts/builds/v4.16-dev.36/build-20260805-095511/`
- Backup: `backup/builds/v4.16-dev.36/build-20260805-095511/`
- Artifact/backup parity: PASS, five files identical by name, length and SHA-256.
- `BUILD_INFO.json` was inspected and contains Gradle 9.3.0, distribution URL/checksum, AGP 8.7.3, JDK/JBR 21.0.10 and vendor/runtime, JDK major policy 21, Java source/target 17, compileSdk/targetSdk 35, commit/branch/event, APK hash and source ZIP hash.

The earlier code94 duplicate test failure exposed the v14 unique import-hash index and was corrected before the final accepted code98 archive. No failed candidate was installed as the final build.

## Safety boundary and next step

No certification, Golden Replay, activation/default profile, project binding, execution, folder import, adapter execution, SAFE4 capability implementation, production profile edit, SQLite migration, pack re-evaluation, QA-row mutation, candidate import, push, merge or tag was performed. Existing legacy/QA rows are not reevaluated or changed; no app data was cleared.

```text
G2-C0C-B2_IMPLEMENTATION: PASS
SAFE4_EXECUTION_READINESS: BLOCKED
```

The next proposed step is a separately reviewed phase to define and implement the missing executable contract/capabilities. Do not proceed directly to certification or activation.

## Worktree at review stop

The final documentation/state commit is intentionally not self-referenced by this file. After that commit, the only worktree changes must remain the three protected, unstaged user files:

```text
.idea/compiler.xml
.idea/gradle.xml
.idea/misc.xml
```
