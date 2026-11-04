# Workspace Snapshot

- Updated: 2026-11-04 (+07:00), after the owner-confirmed installs of release 4.25.0 on the phone and on this PC.
- Current version: release `4.25.0`. Phone `15e84958` runs `4.25.0` / versionCode `329` (installed). PC runs `4.25.0` (MSI product version `4.25.200`, installed).
- Current branch: `feature/v4.25` in `D:\P5E-builds\wt-v4.25`; `main` = `feature/v4.25` = `origin/main` before this record commit. The record commit is local on `feature/v4.25`; nothing pushed.
- Current commit: the commit before this snapshot commit (`23cb3145`, publication record). Tag `v4.25.0` is on `cde799f0`.
- Current build: APK `4B82F0D5…7B10` (event `build-20261104-204947`), installed on the phone; PC MSI `F8B51AD8…FA77E` (event `build-20261104-213322`), installed on the PC.
- Current phase: release 4.25.0 installed on the owner phone and on this PC at the owner's request. Owner trial pending. Product decision served: ship v4.25.
- Completed tasks:
  - Phone `15e84958`: pre-install backup, validated install (`install-validated.ps1`, CheckOnly then install `-r`), version 329 verified, one launch with no crash lines, hashes of the database and all 14 shared_prefs files unchanged, post-install backup taken.
  - PC: data folder backed up (zip plus per-file SHA-256), MSI hash verified, quiet install exit 0, product `4.25.200` (one entry), 10 of 10 data files unchanged, one launch: process alive after 12 s, runtime log gained one line with no errors; app left open.
- Pending tasks:
  - Owner trial of the release on the phone and on the PC.
  - Coordinator review and publication of this install record (not pushed by this session).
- Known bugs/limits: emulator cold start +31 ms vs v4.24.1 (NOT_DIAGNOSED); real-phone performance NOT_MEASURED; APK debuggable; MSI unsigned; Editorial frozen. The undo-bar note line on the PC repeats the deletion sentence (cosmetic, from lib-a).
- Regression status: clean archive of `e014f8c9` green (app 766/0/5, golden 14/14, core 67/0/1, jdbc 3, desktop 83, editorial-engine 742/0/1, lint 0 errors); release APK on emulator: chapter-run 10/10, V425 3/3.
- Next action: owner trials release 4.25.0 on the phone and the PC and reports any issue; coordinator reviews and publishes the install record.
