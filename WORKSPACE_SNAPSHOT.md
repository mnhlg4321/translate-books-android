# Workspace Snapshot

- Updated: 2026-10-04 (+07:00): U1–U4 complete locally through `be87734b`; engine 380/380 and app 341/341 PASS offline. Replay-all: older RAW reproduces `L1_UNIT_UNKNOWN:coverage.0.from`, current RAW passes, exact `fx-a03` RECONCILE passes with one duplicate reference removed. The short source excerpt was replaced by synthetic furigana text and no matching excerpt remains in the current repository.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: `be87734b345babeab27432dbad39efef17523828` — implementation baseline immediately before this snapshot commit; confirm actual HEAD on resume.
- Current build: `4.18-p6.20`/code233, event `build-20261004-103109`, source `3480dda8`; APK SHA-256 `746AF50FCD1ADCCD246665FD15A6CF162B2BDC8758DB5BF1C89C29E5E81C1E49`, exact-source ZIP SHA-256 `281DF34DAEF12459ED1255F26C89CA25C29D454BBDA4718208B0D33DDB5F275F`. AndroidTest event `p6-ruby-anchor-3480dda8-20261004-01`, APK SHA-256 `316E03A3F6657B6A84F7BD5DC9939E9F262FEC9E67C8DBDB91E90CEBBC5104C4`. Both payloads matched artifacts/backup and are installed on `emulator-5554` only.
- Current phase: item 11 U1–U4 PASS offline; exact `fx-a03` RECONCILE replay gate is PASS. U5 remains; U6 may begin only after wrapper/emulator checks and a passing replay-all.
- Completed tasks: contract revision `L1_LEDGER_V7`; deterministic list-reference normalization in L1/L2/L3 with artifact counts; rejection of quotes empty after normalization; replay-all independently validates the older RAW refusal, current RAW pass and exact RECONCILE pass; source excerpt removed in favor of synthetic fixture/report text. Engine 380/380 and app 341/341 PASS offline.
- Pending tasks: U5 wrapper build from HEAD, durable archive, emulator install/preflight/fake CHAIN/negative gate; then U6 may continue G1 under D-G1c while within the same ledger cap and stopping conditions.
- Known bugs: the old RAW response is expected to remain invalid with `L1_UNIT_UNKNOWN:coverage.0.from`.
- Regression status: latest G1 ledger readback is 5,237 bytes, SHA-256 `D4826B2BCA52494441B9D056323FB5D14E98CD15D9DF651AC77027F82F33D14E`; 7 settled calls / 14 entries, USD `0.05538945` / `1.00`, USD `0.94461055` remaining, 0 pending UNKNOWN. No new provider call; no G2 and no pilot access.
- Next action: build from the new HEAD through `scripts/build-and-save.ps1`, archive both payloads, and complete U5 emulator checks before any G1 continuation.
