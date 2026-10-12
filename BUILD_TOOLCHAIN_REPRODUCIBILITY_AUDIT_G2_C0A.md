# Build Toolchain Reproducibility Audit — G2-C0A

Status: FAIL / BLOCKED / REVIEW STOP

Date: 2026-08-04

Scope: read-only audit before G2-C0B. No source, database, wrapper, AGP, APK,
tag or remote was changed.

## 1. Baseline and actual state

Expected baseline:

- Branch: feature/v4.16
- HEAD: d88979e30fd9b9ccecdf290c0781d23a30fe3833
- Worktree: only user-owned .idea/gradle.xml
- Latest accepted APK: 4.16-dev.29/code91

Branch and HEAD match. The worktree assertion does not match. Actual
git status --short --branch output:

    ## feature/v4.16
     M .idea/compiler.xml
     M .idea/gradle.xml
     M .idea/misc.xml
     M build.gradle
     M gradle.properties
     M gradle/wrapper/gradle-wrapper.properties

These six changes were not created, reset, staged or otherwise modified by this
audit. .idea/gradle.xml remains unstaged.

| Input | HEAD d88979e | Working tree at audit time |
|---|---|---|
| Gradle wrapper | 9.3.0 | 9.4.1 |
| Android Gradle Plugin | 8.7.3 | 9.2.1 |
| Gradle properties | original AndroidX/JVM settings | ten additional AGP 9-era settings |
| IDE Java/compiler settings | JDK 21 / bytecode 21 | language/bytecode 17 while JBR name remains 21 |

This is a baseline integrity blocker. Results from the current dirty toolchain
cannot be attributed to clean HEAD.

## 2. Repository values

### Wrapper

HEAD gradle/wrapper/gradle-wrapper.properties contains:

    distributionBase=GRADLE_USER_HOME
    distributionPath=wrapper/dists
    distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.0-bin.zip
    networkTimeout=10000
    validateDistributionUrl=true
    zipStoreBase=GRADLE_USER_HOME
    zipStorePath=wrapper/dists

The current uncommitted file changes only the distribution URL to
https://services.gradle.org/distributions/gradle-9.4.1-bin.zip.

No distributionSha256Sum is declared in HEAD or in the working tree.

### Android Gradle Plugin and modules

HEAD build.gradle declares:

    com.android.application: 8.7.3
    com.android.test:        8.7.3

The uncommitted working tree declares 9.2.1 for both plugins. There is no
version catalog (libs.versions.toml) in the repository.

settings.gradle includes:

    :app
    :macrobenchmark
    :editorial-engine

scripts/build-and-save.ps1 resolves the repository root's gradlew.bat and
invokes that wrapper. It does not call an external Gradle executable.

### Java, SDK and source level

- :app: compileSdk 35, targetSdk 35, minSdk 26.
- :macrobenchmark: compileSdk 35, targetSdk 35, minSdk 26.
- :editorial-engine, :app and :macrobenchmark use Java source/target 17.
- AGP 8.7 requires JDK 17; AGP 9.2 also requires JDK 17.
- The current shell has no JAVA_HOME. java resolves to
  C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe and
  reports 1.8.0_501.
- Android Studio JBR is present at
  C:\Program Files\Android\Android Studio\jbr\bin\java.exe and reports
  Java 21.0.10 when selected explicitly.

Official references:

- AGP compatibility table:
  https://developer.android.com/build/releases/about-agp
- AGP 8.7 release notes:
  https://developer.android.com/build/releases/agp-8-7-0-release-notes
- Android build JDK guidance:
  https://developer.android.com/build/jdks

The published compatibility table says AGP 8.7 requires Gradle 8.9, while AGP
9.2 requires Gradle 9.4.1. Therefore committed HEAD, AGP 8.7.3 plus Gradle
9.3.0, is within the published range. The 9.4.1 requirement applies to the
uncommitted AGP 9.2.1 change, not to HEAD.

## 3. Commands and essential outputs

### Required wrapper command

Command: .\gradlew.bat --version

With the default shell environment, JAVA_HOME was empty and Java 8 was on PATH.
The current dirty wrapper attempted to download Gradle 9.4.1 and failed before
Gradle startup:

    Downloading https://services.gradle.org/distributions/gradle-9.4.1-bin.zip
    java.net.SocketException: Permission denied: connect

With JAVA_HOME set to Android Studio JBR and GRADLE_USER_HOME explicitly set to
C:\Users\ADMIN\.gradle, the wrapper failed while opening the existing wrapper
lock:

    java.io.FileNotFoundException:
    C:\Users\ADMIN\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1-bin.zip.lck
    (Access is denied)

This is a cache-permission/network failure, not a clean-clone reproducibility
pass.

### Direct cached Gradle diagnostics

The following commands were diagnostic only and did not build an APK:

    $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
    & C:\Users\ADMIN\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat --version

Result: PASS. Gradle 9.4.1; launcher and daemon JVM 21.0.10 from Android
Studio JBR.

    $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
    & C:\Users\ADMIN\.gradle\wrapper\dists\gradle-9.3.0-bin\79n14ral3mx1ozqr3csh2u872\gradle-9.3.0\bin\gradle.bat --version

Result: PASS. Gradle 9.3.0; launcher and daemon JVM 21.0.10 from Android
Studio JBR.

The Gradle 9.4.1 daemon check returned: No Gradle daemons are running.
Historical daemon directories exist for both 9.3.0 and 9.4.1.

### 9.3.0 configuration diagnostic on the dirty tree

The tracked 9.3.0 wrapper could not be invoked without changing the current
working wrapper file, which this audit was forbidden to do. The equivalent
direct 9.3.0 diagnostic was run against the actual dirty tree:

    $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
    & C:\Users\ADMIN\.gradle\wrapper\dists\gradle-9.3.0-bin\79n14ral3mx1ozqr3csh2u872\gradle-9.3.0\bin\gradle.bat --offline --no-daemon help

Exact essential failure:

    Plugin [id: 'com.android.application', version: '9.2.1', apply: false]
    was not found ...
    could not resolve plugin artifact
    com.android.application:com.android.application.gradle.plugin:9.2.1

The same offline plugin-resolution failure occurred with direct Gradle 9.4.1.
The current environment lacks a complete offline plugin-resolution state.
This does not prove that committed AGP 8.7.3 fails under Gradle 9.3.0; it is a
dirty-tree dependency/cache failure.

## 4. Code91 provenance

The immutable code91 source ZIP is authoritative for the APK source. Its
extracted build files contain:

    gradle-wrapper.properties: Gradle 9.3.0
    build.gradle:              AGP 8.7.3

BUILD_INFO.json and the per-build README record version, event, branch, source
commit and APK/source hashes, but do not record Gradle, AGP or JDK.

Historical daemon evidence at
C:\Users\ADMIN\.gradle\daemon\9.3.0\daemon-3720.out.log records successful
build executions around the code91 event time with:

    javaHome=C:\Program Files\Android\Android Studio\jbr
    javaVersion=21
    BUILD SUCCESSFUL

Supported provenance:

- Gradle 9.3.0: source snapshot and daemon evidence.
- AGP 8.7.3: source snapshot.
- JDK JBR 21: historical daemon evidence, not recorded in BUILD_INFO.

The JDK portion is historical local evidence, not a portable artifact claim.

## 5. Root cause and classification

### Root cause

The reported “AGP requires 9.4.1” conflated:

1. committed HEAD: AGP 8.7.3 with wrapper 9.3.0; and
2. uncommitted working-tree drift: AGP 9.2.1 with wrapper 9.4.1.

The current environment adds a second independent failure: default Java 8,
while the Android build requires JDK 17 or newer. Wrapper distribution/cache
lock permissions and network access are also unreliable in this shell.

### Classification

Primary: C — AGP_OR_BUILD_SCRIPT_REGRESSION, specifically an
uncommitted/out-of-band toolchain change, not a committed G2-C0A regression.

Secondary: D — ENVIRONMENT_ONLY, for Java 8 default selection, network denial
and wrapper-cache lock permission failure.

Not B — WRAPPER_UPGRADE_REQUIRED: committed AGP 8.7.3 does not require
Gradle 9.4.1. A wrapper upgrade is relevant only if the uncommitted AGP 9.2.1
upgrade is intentionally approved as a separate change.

Not pure A — DOCUMENTATION_ERROR: the 9.4.1 requirement is valid for AGP
9.2.1, but was incorrectly applied to the committed AGP 8.7.3 baseline.

## 6. Reproducibility decision

FAIL for the current audit state.

Reasons:

- the worktree does not match the declared baseline;
- no wrapper distribution checksum is declared;
- the default JDK is unsuitable and is not pinned by the repository;
- gradlew.bat --version cannot complete in the current environment without
  network/cache write access;
- G2-C0A regression used a direct cached Gradle 9.4.1 executable, not a
  verified clean-clone wrapper run;
- code91 metadata omits Gradle, AGP and JDK provenance.

The committed baseline itself is not shown to require a wrapper upgrade. A
clean clone with writable wrapper cache, network access to the declared URL,
JDK 17+ and committed AGP 8.7.3 should use wrapper Gradle 9.3.0.

## 7. Minimum proposed changes — not executed

1. Resolve ownership of the six uncommitted files. Do not reset, checkout or
   overwrite them automatically.
2. If the AGP 9.2.1/Gradle 9.4.1 upgrade is intentional, handle it as a
   separate approved toolchain change with its own small commit, compatibility
   regression and artifact provenance. It is not part of C0A or C0B.
3. If the intended baseline is HEAD, restore/clean the toolchain only after
   explicit approval, then rerun C0A using .\gradlew.bat with JDK 17+.
4. Add a reviewed wrapper distribution checksum and document JDK selection. Do
   not use a local cached Gradle executable as the canonical command.
5. Extend future BUILD_INFO.json/README generation to record wrapper Gradle,
   AGP and JDK versions. This requires a later approved build-tooling change;
   it was not done in this audit.

## 8. Test matrix after an approved fix

| Scenario | Required evidence |
|---|---|
| Fresh clone, writable empty GRADLE_USER_HOME, JDK 17+ | .\gradlew.bat --version downloads 9.3.0, verifies checksum and exits 0 |
| Fresh clone baseline | .\gradlew.bat help with committed AGP 8.7.3 succeeds |
| G2-C0A regression | engine test, app JVM test and instrumentation Java compilation via wrapper |
| Repeat with populated cache | Same results; no direct Gradle invocation |
| Clean network-disabled clone | Explicit dependency-resolution failure, never a false PASS from cache |
| Negative JDK check | Java 8 is rejected with an explicit JDK requirement |
| Future APK build | Only scripts/build-and-save.ps1; BUILD_INFO includes toolchain provenance |

No APK or connected device run is part of this audit matrix.

## 9. Rollback

- No rollback was executed.
- A future approved toolchain commit can be reverted with a reviewable Git
  revert; do not use reset --hard or overwrite user files.
- Do not move tag v4.15, overwrite code91 artifacts, change SQLite v14 or
  modify imported pack rows.
- C0A source and its pure-JVM test commits remain intact.

## 10. Impact and C0B decision

G2-C0A functionality is unchanged and its source commits remain present. Its
regression evidence must be relabeled as environment-bound until the same
tests pass through the committed wrapper on a clean, JDK-pinned checkout.

Code91 APK/source ZIP hashes, installation evidence, ZIP-import device QA, the
retained STORED_BLOCKED row, canonical SAFE4 hashes and candidate boundaries
are unchanged.

Decision: DO NOT CONTINUE TO G2-C0B. Resolve worktree/toolchain drift,
pin/verify JDK and wrapper provenance, and rerun wrapper-based regression first.
No profile bundle, registry, selector, runtime wiring or APK was created by
this audit.
