# Build Toolchain Baseline Recovery — G2-T0A

Status: `PASS / REVIEW STOP BEFORE G2-T0B AND G2-C0B`

Date: `2026-08-04`

Scope: restore the committed code91 toolchain baseline and re-run the G2-C0A
regression through `gradlew.bat`. No APK was built or installed. No Editorial
Pack behavior, database, SAFE4 asset, execution gate, wrapper upgrade or AGP
upgrade was performed.

## 1. Branch, HEAD and starting state

- Branch at start and end: `feature/v4.16`.
- HEAD at start: `48e3e778012999cf681492bb2b06fcc861aaf928`.
- HEAD immediately before this documentation commit: the same
  `48e3e778012999cf681492bb2b06fcc861aaf928`; no source or toolchain commit
  was created during recovery.
- Starting status contained exactly six unstaged files:

      M .idea/compiler.xml
      M .idea/gradle.xml
      M .idea/misc.xml
      M build.gradle
      M gradle.properties
      M gradle/wrapper/gradle-wrapper.properties

The nearest documented baseline was the committed toolchain audit at
`48e3e778012999cf681492bb2b06fcc861aaf928`.

## 2. Dirty-file ownership and recovery

| File | Observed change | Ownership/action | Final state |
|---|---|---|---|
| `.idea/compiler.xml` | bytecode target `21` → `17` | User IDE state; not edited, staged, stashed or committed | Dirty and unchanged |
| `.idea/gradle.xml` | adds `editorial-engine` and `macrobenchmark` Gradle modules | User IDE state; not edited, staged, stashed or committed | Dirty and unchanged |
| `.idea/misc.xml` | language level `JDK_21` → `JDK_17` | User IDE state; not edited, staged, stashed or committed | Dirty and unchanged |
| `build.gradle` | AGP `8.7.3` → `9.2.1` for application/test plugins | Out-of-band toolchain drift; restored narrowly with `apply_patch` | Clean against HEAD |
| `gradle.properties` | ten AGP 9-era flags appended | Out-of-band toolchain drift; restored narrowly with `apply_patch` | Clean against HEAD |
| `gradle/wrapper/gradle-wrapper.properties` | distribution URL `9.3.0` → `9.4.1` | Out-of-band toolchain drift; restored narrowly with `apply_patch` | Clean against HEAD |

The three toolchain diffs were proven to be the same AGP/Gradle drift described
by the prior audit and matched neither the committed baseline nor the code91
source snapshot. `gradle.properties` contained no personal path, credential or
secret; the appended values were only AGP 9-era settings.

## 3. Preserved pre-recovery evidence

The exact three-file pre-recovery diff is retained at
`BUILD_TOOLCHAIN_DRIFT_G2_T0A.diff`.

- SHA-256:
  `0F6F4BE4D8B41051EEA5C9F6F4ECA15C399667F7B58801E7EF7822F53BC67F33`
- This evidence contains only the three reviewed toolchain diffs.
- The pre-recovery `.idea/*` SHA-256 values were recorded and matched again
  after recovery:
  - `.idea/compiler.xml`: `567DE0E16FBBDF30BEEDB8EC3C6A1EB613F4E6F583AF083A1D35F943B7F560C5`
  - `.idea/gradle.xml`: `3E21FA0FED3AB889B97731C84D309B5F9E896DE00B9FC1446BABC7A4B490E173`
  - `.idea/misc.xml`: `5FD7BB21D44895C1DADDFE3BDB15DE48C5CF50508B411A25115848D3C2B16E7F`

## 4. Restored baseline and code91 evidence

The restored repository values are:

- Wrapper distribution URL:
  `https://services.gradle.org/distributions/gradle-9.3.0-bin.zip`
- Wrapper Gradle: `9.3.0`.
- Android Gradle Plugin: `8.7.3` for `com.android.application` and
  `com.android.test`.
- Java source/target: `17` in `:editorial-engine`, `:app` and `:macrobenchmark`.
- `compileSdk`: `35`; `targetSdk`: `35`.
- Runtime JDK used for this verification: Android Studio JBR,
  `C:\Program Files\Android\Android Studio\jbr`, JetBrains OpenJDK
  `21.0.10`.
- No `distributionSha256Sum` is declared in the baseline wrapper properties;
  no checksum policy or wrapper upgrade was added in G2-T0A.

The immutable code91 source ZIP independently contains the same wrapper URL,
AGP version and Java/SDK settings. The accepted code91 identity remains:

- Event: `build-20260804-073059`.
- APK SHA-256:
  `959A25630941550E3D59CB2FBE75C1E3C6D33DCA2A109553C248CDA653E4DE90`.
- Source ZIP SHA-256:
  `A55B5AEDDD945F5247FC1A6177C80654C615DF664B34485F106B92C83B948BAD`.
- Artifact/backup payload hashes remain byte-identical.

## 5. Wrapper commands and results

All commands below set `JAVA_HOME` explicitly to Android Studio JBR 21. No
direct cached Gradle executable was used.

### Java runtime

Command:

    $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
    $env:Path="$env:JAVA_HOME\bin;$env:Path"
    java -version

Essential result: OpenJDK `21.0.10`, JetBrains runtime, build
`21.0.10+-14961533-b1163.108`.

Runtime property inspection reported:

    java.home = C:\Program Files\Android\Android Studio\jbr
    java.runtime.version = 21.0.10+-14961533-b1163.108
    java.vendor = JetBrains s.r.o.
    java.version = 21.0.10

### Wrapper version

Command:

    .\gradlew.bat --version

Result: `PASS`; Gradle `9.3.0`, launcher JVM `21.0.10`, daemon JVM JBR 21.

### G2-C0A regression

Command:

    .\gradlew.bat :editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon

Result: `BUILD SUCCESSFUL` through the repository Wrapper.

- `:editorial-engine:test`: `33/33`, 0 failures, 0 errors, 0 skipped.
- `:app:testDebugUnitTest`: `161/161`, 0 failures, 0 errors, 0 skipped.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS.
- Both `verifyApprovedLogo` and `verifyEditorialSafe4Pack` passed as part of
  the graph.

### Lint

Command:

    .\gradlew.bat :app:lintDebug --no-daemon

Result: `BUILD SUCCESSFUL`; 53 warnings, 0 errors.

### Diff check

Command:

    git diff --check

Result: `PASS`.

## 6. Scope and safety verification

- No APK build command and no `scripts/build-and-save.ps1` invocation was run.
  The latest accepted APK remains code91.
- No source, Editorial Pack implementation, SQLite schema/data, importer, UI,
  MainActivity, profile registry/selector or runtime compatibility wiring was
  changed.
- `EditorialSafe4Pack.java` still has only `PACK_INTEGRITY` in `IMPLEMENTED`,
  all capabilities in `REQUIRED`, and `executionEnabled()` still returns
  `IMPLEMENTED.containsAll(REQUIRED)`, therefore remains `false`.
- Canonical SAFE4 hashes remain:
  - Project: `C57100C45F16FC5A27E56AE17ABE919BE89DA55082A66D060DB504746ED8B717`
  - Prompt: `0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81`
  - Workflow: `7A434ADE77239DB33AF5A677456D0310AC11DC8391565FC4888236536FF96B5E`
- No candidate `DBE214...` or `3B2FCC...` was read, imported or activated.
- The existing device QA blocked row was not changed or removed.
- SQLite remains v14; no migration or database write was performed.

## 7. Final result and next step

G2-T0A: `PASS`.

The three toolchain files no longer drift from the committed baseline, the
Wrapper verifies Gradle 9.3.0, the required regression passes on JBR 21, and
the three `.idea/*` files remain user-owned and unstaged. This is a baseline
recovery result, not a fresh-clone checksum audit; the prior audit's separate
wrapper distribution checksum/documentation gap remains a future tooling
decision and was not changed here.

The exact final Git documentation commit is recorded by the handoff
verification after this file is committed; the implementation baseline for
that snapshot remains `48e3e778012999cf681492bb2b06fcc861aaf928`.

Proposed G2-T0B: user review of this PASS and explicit approval of the next
toolchain/reproducibility action. Do not start G2-C0B automatically. No
profile bundle, registry, selector, runtime wiring, certification, activation,
project binding, execution or release work is authorized by this document.
