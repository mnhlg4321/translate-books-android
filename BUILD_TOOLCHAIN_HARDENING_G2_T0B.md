# Build Toolchain Hardening — G2-T0B

Status: `PASS / REVIEW STOP`

Date: `2026-08-04`

Scope: pin the existing Gradle/AGP baseline, require JDK 21 before an archive
build, and record build provenance. No G2-C0B/C0C work, Editorial Pack
feature, database/migration change, SAFE4 change, candidate import, tag, push,
or release archive was performed.

## 1. Baseline and final implementation state

| Item | Start | End of implementation/build baseline |
|---|---|---|
| Branch | `feature/v4.16` | `feature/v4.16` |
| HEAD | `0aaf131c7ba2fc7b3f31aae790792f1cb20214e0` | `8fbc8d1ed004c1524e001b7bbd0a2b321584331b` |
| Worktree | Only user-owned `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml` modified and unstaged | The same three `.idea/*` files remain modified and unstaged; implementation files are committed |
| Existing accepted APK | `4.16-dev.29` / code `91` | New `4.16-dev.30` / code `92` archived; not installed |

The end HEAD above is the exact implementation commit used by the new APK and
source snapshot. Documentation/state updates are intentionally recorded after
that implementation baseline; the snapshot uses the same non-self-referential
baseline convention required by `AGENTS.md`.

## 2. Changed files

- `gradle/wrapper/gradle-wrapper.properties` — added the official Gradle
  9.3.0 binary distribution SHA-256; the distribution URL remains Gradle
  9.3.0.
- `scripts/verify-java-toolchain.ps1` — explicit JDK preflight with JDK major
  21 enforcement, safe error messages, and JSON output for the build script.
- `scripts/test-java-toolchain-preflight.ps1` — four positive/negative
  preflight checks.
- `scripts/build-and-save.ps1` — preflight before build, wrapper version and
  configuration checks, and additive toolchain provenance in `BUILD_INFO.json`
  and `README.md`. Existing metadata keys remain unchanged for old tooling.
- `BUILD_TOOLCHAIN_HARDENING_G2_T0B.md` — this evidence document.
- `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, and `release_checklists/v4.16.md`
  — current build/state/checklist evidence.

No `.idea/*` file was edited, staged, committed, deleted, or overwritten.
There are no changes to `build.gradle`, application source, Editorial Pack
manifest/registry/importer, `EditorialSafe4Pack.executionEnabled()`, SAFE4
hashes, database schema/data, or migration code.

## 3. Gradle checksum evidence

The pinned wrapper values are:

```text
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.0-bin.zip
distributionSha256Sum=0d585f69da091fc5b2beced877feab55a3064d43b8a1d46aeb07996b0915e0e0
```

Official Gradle checksum source:

`https://services.gradle.org/distributions/gradle-9.3.0-bin.zip.sha256`

The official sidecar returned the full checksum
`0d585f69da091fc5b2beced877feab55a3064d43b8a1d46aeb07996b0915e0e0`. Its
filename is `gradle-9.3.0-bin.zip`, exactly matching the binary filename in
`distributionUrl`; no wrapper upgrade or checksum guess was used.

Wrapper verification output:

```text
Command: .\gradlew.bat --version
Result: PASS
Gradle 9.3.0
Launcher JVM: 21.0.10 (JetBrains s.r.o.)
Daemon JVM: C:\Program Files\Android\Android Studio\jbr
```

The command ran through the repository Wrapper. No cached Gradle executable
was used as a replacement.

## 4. JDK policy and preflight evidence

- Build runtime policy: JDK major `21` exactly.
- Java source compatibility: `17`.
- Java target compatibility: `17`.
- The build accepts JDK selection only through `-JavaHome` or `JAVA_HOME`.
- The selected JDK is applied only to the current PowerShell process; system
  environment variables are never changed.
- A Java runtime found only on `PATH` is not selected implicitly. Java 8 on
  `PATH` is diagnosed and rejected early.
- Error messages report the required/found major or the remediation, without
  echoing user paths, account names, or secrets.

Command:

```text
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-java-toolchain-preflight.ps1
```

Result: `PASS`, `4/4`:

1. JDK 21 accepted through `-JavaHome`.
2. Java 8 rejected with a clear major-version error.
3. Missing `JAVA_HOME` rejected without implicit runtime selection.
4. Invalid Java path rejected without exposing the supplied path.

The successful archive build used Android Studio JBR `21.0.10`, vendor
`JetBrains s.r.o.`, runtime `OpenJDK Runtime Environment`.

## 5. Required regression evidence

All Gradle commands used the repository Wrapper with JDK 21:

```text
.\gradlew.bat --version
```

PASS: Gradle `9.3.0`; launcher and daemon runtime JDK `21.0.10`.

```text
.\gradlew.bat :editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon
```

PASS: `:editorial-engine:test` `33/33`; `:app:testDebugUnitTest`
`161/161`; instrumentation Java compilation PASS; 0 failures, errors, or
skips. Existing logo and canonical SAFE4 guards also passed.

```text
.\gradlew.bat :app:lintDebug --no-daemon
```

PASS: 0 errors and 53 existing warnings.

```text
git diff --check
```

PASS.

The four preflight tests and PowerShell parser check also passed after the
archive script changes. No direct cached Gradle invocation was used.

## 6. Archive-first build evidence

Command (no `-Install`):

```text
.\scripts\build-and-save.ps1 -Series 4.16-dev -JavaHome "<JDK-21-directory>"
```

The script selected the next values; no code was hardcoded or reused:

- Version: `4.16-dev.30`
- Version code: `92` (previous accepted code `91`)
- Event: `build-20260804-182006`
- Source commit recorded by the build: `8fbc8d1ed004c1524e001b7bbd0a2b321584331b`
- Branch: `feature/v4.16`
- APK: `TranslateBooks-v4.16-dev.30-code92.apk`
- APK SHA-256: `07BC98B22019832AFD37D0307E691957FBDC47D1C0E929949535D69AFB472801`
- Source ZIP: `project_source_build-20260804-182006.zip`
- Source ZIP SHA-256: `BD6BCDC832DC5C3D9AFFA3E1C597A12D090F560F6B8D6319D0D65F7FB22C1126`

The APK was archived before any optional installation. No installation was
requested and no release archive/tag was created.

Artifact:

`artifacts/builds/v4.16-dev.30/build-20260804-182006/`

Backup:

`backup/builds/v4.16-dev.30/build-20260804-182006/`

Both payloads contain the same five files: APK, `README.md`, `BUILD_INFO.json`,
`SHA256SUMS.txt`, and the tracked-source ZIP. Relative paths, lengths and
SHA-256 values match for all five files. The source snapshot ref in
`BUILD_INFO.json` is `f12f989849dee6f35353dcbbdd6117a9ed1b6927`; its only
working-tree delta from the implementation commit is the three protected
`.idea/*` files.

## 7. Provenance written to BUILD_INFO.json and README.md

The new payload records, in addition to the existing version/event/branch/
commit/APK/source hashes:

- Gradle `9.3.0`.
- Gradle distribution URL and full SHA-256.
- Official checksum source URL.
- AGP `8.7.3`.
- Java version `21.0.10`, runtime name/version and vendor.
- JDK major `21` and the explicit JDK-major policy.
- Java source/target `17`/`17`.
- `compileSdk` `35` and `targetSdk` `35`.
- Git commit, branch, build event, APK SHA-256 and source ZIP SHA-256.

The JSON retains `schemaVersion: 1` and all prior metadata keys; the
provenance keys are additive, so existing artifact readers remain compatible.
No JDK path, account path, secret, token, or personal environment value is
written to the artifact metadata or README.

## 8. Final scope and decision

- G2-T0B: `PASS`.
- Editorial functionality: unchanged; no G2-C0B/C0C implementation started.
- SAFE4 execution remains disabled/blocked and canonical hashes are unchanged.
- Database/migration state is unchanged.
- Candidate DBE214... and 3B2FCC... were not read, imported, or activated.
- No push, merge, tag, or release archive was performed.
- Final worktree state: only the three user-owned `.idea/*` files are
  unstaged; all G2-T0B implementation/documentation changes are staged or
  committed according to the handoff workflow.

Recommended next step: review this document and separately approve G2-C0B.
G2-C0B is not implemented in this task.
