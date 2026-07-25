# Translate Books 4.14 Release Notes

Planned release date: 2026-07-25

Git tag: `v4.14` (created only after all release gates pass)

## Summary

Version 4.14 makes builds durable and easier to identify while restoring useful translation visibility. Every accepted build is numbered and archived before installation, glossary imports can inherit their filenames, the Translate dashboard again shows the latest accepted output and exact rule usage, and the approved bright cool logo becomes the permanent app identity.

## Highlights

- Archive every successful APK with README, build metadata, SHA-256 manifest, and exact source ZIP in both `artifacts/builds/` and `backup/builds/`.
- Assign increasing development version names and Android version codes without relying on temporary Gradle output.
- Use an exact release mode for public `versionName 4.14`.
- Adopt the exact imported filename for a single glossary file while retaining deterministic naming for multiple imports and preserving explicit custom names.
- Restore the bounded latest-translation preview and current chunk information.
- Show exact Glossary and Pronoun rule counts from the same `PromptPlan` used by translation/refinement requests.
- Use the approved bright cool logo for launcher, round launcher, and in-app header, with an exact-hash build guard.
- Add a dedicated non-debuggable AndroidX Macrobenchmark target for retained startup metrics and Perfetto traces.

## Pre-tag verification

- Exact archive-first `4.14`/code57 APK build and installation passed.
- 106 JVM tests passed; Android Lint completed with 0 errors and 54 warnings.
- Connected Android instrumentation completed with 11 passed and 1 opt-in paid case skipped.
- Controlled output-permission-loss proof passed with a prepared local TXT, the exact blocker, and Start disabled.
- Physical-device AndroidX Macrobenchmark passed five cold-start iterations with a 322.81 ms median and retained five Perfetto traces.
- Final screenshots, screenshot-sequence AVI, APK/source hashes, and artifact/backup parity passed.

Final counts, hashes, artifact locations, benchmark measurements, and known limitations are recorded in `QA_REPORT_v4_14.md` and `BUILD_STATE.md`.

## Distribution

The project currently produces a locally signed APK release and immutable local archive. APK binaries are intentionally ignored by Git and must be distributed separately from the source repository.
