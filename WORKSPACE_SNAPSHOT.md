# Workspace Snapshot

- Updated: 2026-10-23 (+07:00). Phase A UX and runtime F6b implementation is complete; this snapshot records the dev.24 candidate before the owner installation decision.
- Current version: v4.23.0 in development; verified candidate **4.23-dev.24/code302**. Shipped release remains v4.21.0/code271.
- Current branch: `feature/v4.23`, `D:/P5E-builds/wt-v4.23`; protected v4.18 checkout and owner media remain untouched. No merge, tag, push, provider call, or APK install in this phase.
- Current commit: **874a030dcd97066a4f36198e091ce2b559a8d00c** is the implementation baseline immediately before this snapshot update commit.
- Current build: 4.23-dev.24, event `build-20261023-203439`, source `874a030dcd97066a4f36198e091ce2b559a8d00c`; APK SHA `4E58C487EC7B68DE995826758C42659DE1FE12FCB755797A96D10F6F52FE0C76`. Artifact and backup payloads exist at `artifacts/builds/v4.23-dev.24/build-20261023-203439/` and `backup/builds/v4.23-dev.24/build-20261023-203439/`.
- Current phase: F6b and UX Phase A offline implementation complete; clean archive regression and dev.24 build passed. Pre-install phone backup completed; installation and phone UI trial are waiting for the owner’s exact `duyệt cài` approval.
- Completed tasks: F6b exact threshold test (`9724/11160`), U1 state control bar, U2 P1/P2, U3 Jobs/history/recent/filter models and UI, Java unit tests, lint, AndroidTest Java compilation, clean archive regression, and build dev.24. No provider was called and no Editorial file/schema/common request-body change was made.
- Pending tasks: owner installation approval; after approval only `install -r`, then the Phase A phone checks and screenshot of Công việc, followed by final report. No provider call, merge, tag, or push is authorized by this snapshot.
- Known bugs/limits: phone UI trial is `NOT_MEASURED` until installation; semantic evaluation remains `NOT_MEASURED`; F3b is intentionally not done. Device pre-install backup tar opened with `1,397` entries.
- Regression status: clean archive `D:/P5E-builds/v423-clean-regression-20261023-03/source/` passed `testDebugUnitTest`, `testBenchmarkUnitTest`, `testReleaseUnitTest`, `lintDebug`, `lintBenchmark`, `lintRelease`, and `compileDebugAndroidTestJavaWithJavac`; wrapper build passed. No API key, book text, or settings JSON was committed.
- Spend: no provider calls in this phase; `$0.00` new spend. APK SHA and the three pre-install backup file hashes are recorded outside Git in `D:/App Translate Books/App Translate Books-device-backups/v4.23-dev.24-code302/20261023-203650-pre-install/`.
- Next action: wait for the owner to reply exactly `duyệt cài`; then run only the validated `install -r` flow and the non-paid phone checks.
