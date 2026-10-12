# P5E A4.3 — final executable approval manifest

Document status: `P5E_A43_FINAL_EXECUTABLE_PACKET_READY / OWNER_DECISION_PENDING / NOT_DISPATCHED / P6_NOT_READY`.

Work package: `P5E_A43_FINAL_EXECUTABLE_PACKET_REPIN`.
Packet identifier: `P5E-A43-FINAL-EXECUTABLE-20260926-01`.

This is a new approval manifest for one possible future A4.3 event. It is not
an authorization, does not create an authorization row, and has not been
executed. The owner decision is intentionally pending. The command, helper,
exporter, bridge, artifact and certificate pins below are immutable inputs for
the command; the manifest does not contain its own hash, the helper hash, or
the command self-hash.

## Boundary and closed-event preservation

- Canonical plan: `EDITORIAL_RECOVERY_V4_18.md`.
- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- HEAD at final packet freeze: `2b34266eb60e3439864c80aae5d30ae141cfe8aa`.
- The worktree is intentionally dirty with pre-existing owner changes. This
  packet does not reset, checkout, clean, stage, build, install or overwrite
  unrelated files.
- All earlier events remain closed/consumed and are not inputs to this packet:
  `raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586` and
  `raw-live-20260926-024601307-fd3c6cfb9afb4aa794f60718ba9632b1`.
- The latest closed event result is
  `docs/P5E_A43_EVENT_RESULT_20260926.json`, SHA-256
  `D7E3B0332FE9818639D4F77F75B340FB9C2BE9236399176A29E890E7FFE777C6`.
  Its event plan, collector outcome and command log remain immutable; no
  instrumentation stdout/stderr, provider payload or credential was read.
- The previous repair packet is consumed/non-reusable for this work package:
  manifest `669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147`
  and command
  `A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08`.
  Neither path is a fallback, and neither is invoked by the new command.

## Fixed A4.3 scope

- Device serial: `15e84958`.
- Production package/version/code: `com.ml.tblandroidtxt` /
  `4.17-p5e.11` / `207`.
- Test package/target/runner: `com.ml.tblandroidtxt.test` /
  `com.ml.tblandroidtxt` /
  `androidx.test.runner.AndroidJUnitRunner`.
- Phase: `L1_RAW_DISCOVERY`.
- Project row/chapter/selector: `2` / `001` /
  `p5e-fresh-mercedes-vol5-20260911-01`.
- Attempt identity:
  `7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e`.
- Request identity:
  `ae328c3d771112ce73e9e9d6cba0bb951f930042fc6851a31a96f15f7b70ee06`.
- Binding identity:
  `845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf`.
- Run declaration identity:
  `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc`.
- Evaluation: `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`.
- Pack hash:
  `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d`.
- Profile hash:
  `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21`.
- Pack-manifest fingerprint:
  `3e88503e312db8da351ca574820c98216ab6fd3fa233e35aedb0db379e50013a`.
- Input-scope-manifest fingerprint:
  `0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da`.
- Route fingerprint:
  `23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c`.
- Authorization ID remains an offline currently-unused contract identity only:
  `P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01`, SHA-256
  `0AA82C5897E3DF3EC8A7A1586736DBF184B316C66EC165E95E64E8E4832145EB`.
  A consumed/unexpected row is a typed stop; this packet creates no row.

## Exact artifact and certificate pins

All primary/backup pairs must be regular, non-reparse files with matching
lengths and hashes before any future `PrepareEvent`. No APK is rebuilt or
installed by this work package.

| Input | Exact path or identity | SHA-256 |
|---|---|---|
| Production APK | `TranslateBooks-v4.17-p5e.11-code207.apk` in the immutable `build-20260911-201725` artifact/backup pair | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` |
| Production source ZIP | `project_source_build-20260911-201725.zip` in the same artifact/backup pair | `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` |
| Production BUILD_INFO | `BUILD_INFO.json` in the same artifact/backup pair | `DB20DA0CF708410AAAB65E5AF69ADF89A769ED62240B577A89CA4E2007FB7F06` |
| AndroidTest APK | `app-debug-androidTest.apk` in immutable event `p5e-account-check-20260916-01` artifact/backup pair | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |
| AndroidTest source ZIP | `project_source_p5e-account-check-20260916-01.zip` in the same artifact/backup pair | `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F` |
| AndroidTest BUILD_INFO | `BUILD_INFO.json` in the same artifact/backup pair | `772F32E23AEF3537FEF00DACE8E4B9B994448BFEE2071150CF0E0540DDCCC5A9` |
| Production/test certificate | SHA-256 certificate identity | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| AndroidTest source commit | BUILD_INFO-declared source commit | `9e5ffb7819bfb91dcb8ed9e25c901ab10aa48390` |

The full absolute paths and both pair sides are pinned in the new command.

## Database/readback contract

- Event-plan schema: `p5e.raw.event-plan.v3`.
- Collection mode: `READ_ONLY_ADB_BINARY_EXPORT_HOST_SQLITE`.
- Readback schemas: `p5e.raw.readback.v2`,
  `p5e.raw.readback.provenance.v2`, and
  `p5e.raw.readback.producer-input.v2`.
- The Android live path has zero `sqlite3` call sites. The main database is
  exported as exact binary bytes through the allowlisted app-private path into
  bounded `.partial` host files, then hash-checked and renamed atomically.
- Presence semantics are typed: exit `0` is `PRESENT`, exit `1` is valid
  `ABSENT`, and timeout, launch, capture, redaction or other nonzero outcomes
  are typed stops. No checkpoint, delete, restore, MTP or device write is
  allowed.
- WAL/SHM snapshots are either a stable complete matching set or a typed stop;
  no checkpoint or deletion is used. Settings remains a separate read-only
  presence/hash gate and is never substituted for the database snapshot.
- Host bridge: `docs/P5E_SQLITE_BRIDGE.py`, SHA-256
  `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`.
- Binary exporter: `scripts/p5e-db-binary-export.ps1`, SHA-256
  `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06`.
- Current helper: `scripts/p5e-raw-live-supervisor.ps1`. Its exact required
  SHA-256 is pinned by the new command and final provenance, not embedded here
  to avoid a circular manifest/helper hash.

## Helper, toolchain and execution boundary

- The new command is
  `docs/P5E_A43_FINAL_EXECUTABLE_COMMAND_20260926.txt`.
- The helper is required at SHA-256
  `17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E`.
- Toolchain resolver:
  `scripts/p5e-raw-toolchain.ps1`, SHA-256
  `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`.
- `local.properties` SHA-256:
  `71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2`.
- Build-tools contract: `35.0.0`; signer launch kind: `JAVA_JAR`; Java path:
  `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`.
- Before dot-sourcing or external access, every live helper phase checks the
  expected exporter and bridge path, regular-file state, canonical path,
  ancestor/reparse guards and exact SHA-256. The same expected pins travel
  through PrepareEvent, Before, Dispatch, After and VerifyOutcome.
- The command self-hash is process-only `P5E_A43_COMMAND_SHA256`; it is never
  embedded in the command. Missing or incorrect self-hash stops before helper,
  account, device or provider access.
- Future dispatch uses one newly-created event directory only. It never opens,
  renames, retries, or redispatches a closed event.
- Host observation is bounded to `240000 ms`; authorization validity is
  `180000 ms`; execution deadline is `120000 ms`. Timeout, exception, unknown
  state or missing After readback never triggers redispatch.

## RAW projection, account isolation and write boundary

- Only RAW and GLOSSARY are visible. DRAFT and PRONOUN remain hidden.
- RAW: `23814` bytes, SHA-256
  `A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE`.
- GLOSSARY: `3249` bytes, SHA-256
  `4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314`.
- DRAFT hidden hash:
  `64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5`.
- PRONOUN hidden hash:
  `4947FF9184995BE5F850F2323FBEA04C67302FB8D5AFB63CF12202B44720686`.
- Route/model: `openrouter` / `openai/gpt-5.6-luna` / `openai`;
  fallback/plugins are disabled; `stream=false`; structured output is strict.
- Budget: one primary semantic call, zero schema-repair calls, zero network
  retries, `100000/4096/104096` input/output/total token caps, USD `0.05`
  maximum, and `120000 ms` execution deadline.
- The account comparison is memory-only and owner-controlled. This packet
  reads no credential, endpoint or expected account digest and stores none.
  The process-only expected value is not requested from the agent.
- Any future event may use only the existing allowlisted atomic attempt,
  authorization receipt, lifecycle and committed-result writes. No binding,
  input, run, pack, profile, settings, reconciliation, delete, cleanup,
  restore, retry, fallback or RECONCILE action is allowed.

## Offline decision state

- Offline DB host-readback repair: PASS, `56/56`.
- Captured-export control and binding-tuple repair: PASS, `262/262`; regression
  matrix: `175/175`.
- Final packet QA, including dependency/hash order, command-to-manifest/helper
  binding, captured-export control, PrepareEvent simulation, path/reparse
  guards, expected-value isolation, redaction/secret scan, timeout/no-retry,
  consumed-event rejection, PowerShell 5.1 parse and `git diff --check`: must
  be PASS before this packet is handed to the owner.
- A4.3: not issued. RAW: not run. P5 exit: not claimed. P6: not ready.
- Live counters for this work package: ADB `0`, device reads/writes `0`,
  provider `0`, credential `0`, database writes `0`, build/install `0`, RAW
  dispatch `0`, redispatch `0`.
- Owner decision: pending one exact new event decision; no decision from any
  closed event is reused.

## Freeze and authority rule

Freeze order is exporter/bridge → this manifest → helper → new command → final
QA/provenance/Luna review. If any pinned byte changes, recompute all downstream
hashes and rerun affected QA before owner review. The final owner request is
the only current authorization request; all earlier requests are historical,
closed or consumed and are not fallback inputs.
