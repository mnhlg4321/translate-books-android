# P5E — Cài đặt có kiểm soát và bảo toàn pilot data

> Current audit (2026-09-14): P5/P5E remains incomplete; P6 is not ready. The A4.3 packet is preserved at its original hashes and must not be dispatched unchanged. Host command quoting (F2), account-fingerprint provisioning (F1), and live outcome verification (F3) are addressed by docs/P5E_NEXT_WORK_REQUEST.md. The single next action is to complete that bounded host preparation. Earlier owner-review next actions below describe the f8fe433e baseline and are superseded by this audit; historical evidence and consumed approvals remain unchanged.


Tài liệu này chỉ áp dụng cho validation/pilot có package
`com.ml.tblandroidtxt`. Nó không cấp quyền provider và không thay thế
authorization/preflight. Không chạy `uninstall`, `pm clear`, reset, downgrade
hoặc `--downgrade` trên device đang giữ dữ liệu pilot.

## Incident đã biết

Evidence còn lại trong Gradle daemon log:

```text
C:\Users\ADMIN\.gradle\daemon\9.3.0\daemon-39212.out.log
```

Log ghi nhận `AndroidTestApkInstallerPlugin` cố cài
`TranslateBooks-v4.13-p0-dev-debug.apk` (`versionCode=48`) lên package đang ở
`versionCode=197`, rồi nhận:

```text
INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected: Update version code 48 is older than current 197
```

Log không chứa lệnh `uninstall`, `pm clear` hoặc reset; PowerShell history cũng
không còn lệnh phù hợp. Kết luận chắc chắn chỉ là connected installer đã đi vào
đường cài lower-version và sau đó package/data biến mất trước lần validation kế
tiếp; actor cleanup chính xác không được suy diễn từ evidence hiện có.
`daemon-39532.out.log` chỉ chứng minh hậu quả quan sát được: các test sau đó
không còn đọc được VOL5 DB/fixture và attempt. Không dùng log này để tuyên bố
khôi phục lịch sử.

## Guard bắt buộc

`scripts/install-validated.ps1` kiểm tra theo thứ tự:

1. APK tồn tại, đúng package và đúng `versionCode` bằng `aapt`.
2. SHA-256 toàn bộ APK khớp exact digest được pin trong preflight.
3. APK có certificate SHA-256 bằng `apksigner` và khớp exact certificate digest
   được pin trong preflight.
4. Device serial explicit ở trạng thái `device`.
5. Package hiện hữu không được có version cao hơn APK; package hiện hữu phải
   khớp đúng signature token đã đọc trước đó từ `dumpsys package`.
6. Chỉ sau khi preflight pass mới chạy `adb install -r`; sau đó đọc lại
   package/version/signature. Không có fallback, uninstall, clear hay
   `--downgrade`.

Kiểm tra không cài đặt:

```powershell
$env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
.\scripts\install-validated.ps1 `
  -ApkPath '.\artifacts\builds\v4.17-p5e.6\build-20260911-034554\TranslateBooks-v4.17-p5e.6-code202.apk' `
  -Serial '15e84958' `
  -ExpectedVersionCode 202 `
  -ExpectedApkSha256 '8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0' `
  -ExpectedApkCertificateSha256 '47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155' `
  -ExpectedDeviceSignatureToken 'abebea4b' `
  -CheckOnly
```

`-AllowMissingPackage` chỉ được dùng khi owner đã xác nhận target là một
device/package rỗng và đang tạo pilot mới; không dùng trên device hiện tại.
`build-and-save.ps1 -Install` không còn gọi ADB trực tiếp: nó bắt buộc serial,
certificate digest pin explicit và chuyển hash của APK đã archive vào guard này.
Gradle cũng fail-closed với mọi task
`connected*AndroidTest`; assemble, install và instrumentation phải là ba bước
tách biệt.

Lệnh candidate code202 ở trên đã pass check-only. Sau owner approval và G1
`BACKUP_RESTORE_G1_PASS`, cùng lệnh với `-CheckOnly` bỏ đi đã được chạy đúng một
lần; `adb install -r` trả `Success`, rồi post-install package/signature đọc lại
code202 đạt. Không được chạy lệnh này như fallback cho package thiếu hoặc
version mismatch.

Device-side `dumpsys` chỉ cung cấp short signature token (`abebea4b`), còn APK
được kiểm tra bằng certificate SHA-256 đầy đủ. Khi package đã tồn tại,
`adb install -r` vẫn là lớp Android cryptographic signer check cuối cùng; guard
không ép bypass nếu signer không tương thích.

## Backup/restore policy

Chỉ tìm backup của code196 ở các root đã biết: `artifacts`, `backup` của
repository hiện tại và các evidence path đã ghi trong P5E. Không có trusted
code196 DB; `D:\Ebooks\New folder\metadata.db` là metadata không liên quan.
Sau owner approval, snapshot WAL-aware của DB reconstructed hiện hành đã được
tạo ngoài Git và restore thử trong môi trường cô lập đạt. Snapshot này bảo vệ
thao tác upgrade hiện tại, không phục hồi lịch sử code196. Gate lịch sử vẫn là:

```text
PILOT_DATA_PRESERVATION_FAILED
RECONSTRUCTED_ONLY
```

Backup mới, nếu owner phê duyệt fresh pilot, phải là SQLite consistent snapshot
do app/SQLite owner thực hiện, xử lý đúng WAL (`.backup` hoặc checkpoint/copy
được owner kiểm soát), kèm manifest và SHA-256; phải restore thử ở môi trường
cô lập trước khi dùng. Không đưa DB, secret, request/response body hoặc nội
dung sách vào Git hay báo cáo công khai.

## Quy tắc fresh pilot

Fresh pilot là một lineage mới, không phải recovery: tạo project/run/binding/
attempt identity mới, giữ compatibility evaluation là immutable trusted-evaluation
record gắn với import cụ thể theo P4. Canonical pack hash chỉ là content identity;
exact evaluation ID được official setup freeze trong binding và không interchangeable
với evaluation của import khác. Tạo authorization single-use
mới chỉ ở vòng live và giữ source hash nếu exact bytes không đổi. Không sửa nhãn, không tạo receipt cho attempt cũ, không biến
authorization đã consumed thành khả dụng và không trộn các row reconstructed
với lịch sử code196. Owner đã phê duyệt local-only; G1 backup/restore, G2
candidate upgrade/data readback, G3 fresh setup và G4 candidate-aligned fake QA
đã đạt. Không có live attempt/authorization nào được tạo.

## Evidence G1/G2 sau owner approval

Owner approval được ghi nhận lúc `2026-09-11T06:00:59+07:00`. Snapshot private
ngoài Git tại `D:\P5E-private\fresh-pilot-20260911-060059` có manifest SHA-256
`1C495F95B0572458B8405B599A7D11CC80F6770316080A1772EF379A17AC7C64`, phân loại
`RECONSTRUCTED_ONLY`. Snapshot SQLite và restore copy có schema v24,
`integrity_check=ok`, không có foreign-key violation, và source/pack hashes
trùng nhau; đây là `BACKUP_RESTORE_G1_PASS`, không phải recovery code196.

Candidate code202 được cài đúng một lần bằng `scripts/install-validated.ps1`
sau G1, với package/version/APK SHA-256/certificate/device token đúng. Readback
package code202, DB hash `6D5B73B7C8B19A32E196C772C4BE72DB5F815FD14DF66CEA1F3B9808C62379A5`,
schema/source/pack hashes và các invariant SQLite giữ nguyên; đây là
`CANDIDATE_UPGRADE_G2_PASS`. Không có uninstall, clear, reset, downgrade,
fallback, connected test, provider call hay authorization mới.

G1/G2 chỉ bảo vệ và kiểm chứng dữ liệu reconstructed hiện hành. Claim lịch sử
vẫn giữ nguyên:

```text
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
```

## Evidence G3/G4 — fresh lineage và candidate QA

G3 tạo fresh pilot qua setup path chính thức với selector
`p5e-fresh-mercedes-vol5-20260911-01`, binding
`845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf` và run
declaration `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc`.
Selector/binding/run cũ không được tái sử dụng; current DB sau readback có
schema v24, `2` projects, `2` chapters, `8` assets, `2` P4 bindings, `2`
revisions, `2` scopes, `2` declarations và `0` P5C attempts / P5D authorization
receipts / reconciliation rows. Source bytes và canonical pack/profile hashes
được giữ nguyên, gồm quy tắc PRONOUN BOM hiện hành.

Snapshot fresh được lưu ngoài Git tại
`D:\P5E-private\fresh-pilot-20260911-060059\fresh-pilot-snapshot`, phân loại
`RECONSTRUCTED_ONLY_FRESH_PILOT`, manifest SHA-256
`2BEA88D4B562DFFA0CEAE401E1BFA0ED50B353CCD014AA9840F15CE1FA7F35EF`; DB hash
là `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`.

G4 dùng candidate-aligned AndroidTest APK SHA-256
`63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC`,
test-source commit `424278e44c042b882d1888f45d5c4b5b944e0dca`, certificate
SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`.
Lớp `EditorialP5EFreshPilotInstrumentedTest` chạy trực tiếp bằng
`adb shell am instrument` và đạt `4/4`, exit `0`:

- fresh setup/readback xác nhận binding mới và bảo toàn lineage reconstructed;
- compact RAW success/replay, exact app-owned before/after và
  `declaredChanges=[]` chỉ commit trong isolated DB/storage;
- malformed/truncated/oversized/unknown/change/replay và fault STOP không tạo
  partial commit, repair, report, receipt hoặc RECONCILE.

Không dùng `connectedDebugAndroidTest`, không gọi provider/API thật, không tạo
RAW authorization và không biến fake predecessor thành bằng chứng live. Trạng
thái kết thúc của vòng G3/G4 local trước exact-freeze là:

```text
FRESH_PILOT_LOCAL_VERIFIED
RAW_AUTHORIZATION_REQUIRED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
```

## Historical P5E.9A-EVAL provenance correction — pre-QF result

The earlier code202/G3/G4 and code204 freeze entries above remain prior
evidence. The stale `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` value was a runner expectation; the persisted fresh
binding was already correct with the official import-scoped evaluation
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`. The minimal correction
is committed at `049e72b5769f8b3fdcb6f50646d1f0ead3043940`; no SQL, migration,
canonical pack, profile or authority changed.

The committed candidate is code206 (`v4.17-p5e.10`), source commit
`049e72b5769f8b3fdcb6f50646d1f0ead3043940`, APK SHA-256
`F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080`,
certificate SHA-256
`47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, source
ZIP SHA-256
`5BA76881BA87313C69B3E6221676487A88FDFD9A68D61860521131EABC03E4B8`. Artifact
and backup payloads are byte-identical. The candidate-aligned test APK is
outside Git at
`D:\P5E-private\fresh-raw-boundary-20260911-1836\candidate-code206-test-apk\app-debug-androidTest.apk`,
SHA-256 `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2`,
test-source commit `f90c0372c019c0d3970efb298b64b6a4addcfd4f`, same certificate.
The intermediate code205 archive is retained as non-candidate build evidence;
code204 remains pre-correction evidence.

The pinned private snapshot was reused because the pre-install device DB hash
matched exactly. `scripts/install-validated.ps1 -CheckOnly` passed the exact
serial/package/version/APK/certificate/signature pins; the same script then
performed exactly one code202 → code206 upgrade and returned install verification
PASS. Readback shows serial `15e84958`, package `com.ml.tblandroidtxt`, installed
code206 (`v4.17-p5e.10`), signature token `abebea4b`, schema v24, process absent,
and unchanged DB SHA-256
`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`.
The fresh selector/binding/run/chapter and official evaluation
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` read back correctly; the
snapshot manifest remains outside Git at
`D:\P5E-private\fresh-raw-boundary-20260911-1810\SNAPSHOT_MANIFEST.json` with
SHA-256
`6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28`.
The superseded candidate-aligned test APK was installed and the direct boundary
class ran 5 tests: 4 passed; 1 failed before provider execution because the test
queried nonexistent `binding_identity` on `editorial_p5d_reconciliation`, which
is keyed by `attempt_identity`. Logcat recorded `providerCalls=0`, `attempts=0`,
`authorizations=0`, `reconciliation=0`; post-test DB hash remained unchanged.
Host engine/app variants, lint, AndroidTest compilation and static checks pass;
no authorization, attempt, reconciliation, repair, retry or provider call
occurred.

The test-only correction is committed separately at
`34a4ec2832d71a488a2531a0e69a85261e9c9b9b`; it uses the required JOIN through
`editorial_p5d_reconciliation.attempt_identity` to
`editorial_p5c_attempts.binding_identity`. AndroidTest compilation and the new
test APK build passed. The new APK is outside Git at
`D:\P5E-private\fresh-raw-boundary-20260911-qf-test-apk\app-debug-androidTest.apk`,
SHA-256
`68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A`, certificate
SHA-256
`47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; it has
not been installed pending separate owner approval. The old test APK is
superseded and must not be rerun.

Historical stop state:

```text
P5E_9A_EVALUATION_PROVENANCE_RESOLVED
P5E_9A_CODE206_GUARDED_INSTALL_PASS
P5E_9A_DEVICE_ZERO_CALL_QA_BLOCKED_TEST_QUERY
P5E_9A_FRESH_RAW_BOUNDARY_LOCAL_PASS: NOT_REACHED
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
```

At that historical stop point the corrected test APK still awaited separate
owner approval. The later approval and QF device rerun are recorded below. Do
not create RAW authorization or call a provider from this historical evidence.

## Historical P5E.9A-QF — code206 zero-call device result

The owner separately approved the exact corrected test artifact. Only
`com.ml.tblandroidtxt.test` was replaced on `15e84958`; production code206 was
not reinstalled or modified. Device readback matched the approved test APK
SHA-256 `68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A`,
certificate `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`,
production code206 APK SHA-256
`F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080`, and
signature token `abebea4b`.

The required direct instrumentation sequence passed without skip:

```text
historicalP5dRunnerRejectsFreshSelectorBeforeProvider: 1/1 PASS
EditorialP5EFreshRawBoundaryInstrumentedTest: 5/5 PASS
```

The class read-only checks confirmed schema v24, the exact fresh selector,
chapter, binding, run and official evaluation
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`. The corrected query
joins reconciliation to attempts through `attempt_identity` and filters the
fresh binding on the attempts table. Post-run DB SHA-256 remained
`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`; attempts,
authorization receipts, reconciliation/history, lifecycle and report/receipt
counts remained zero. Logcat recorded `providerCalls=0`; no provider, API,
authorization, attempt, retry, repair or RECONCILE action occurred.

Historical local boundary result (superseded as a current-ready claim):

```text
P5E_9A_FRESH_RAW_BOUNDARY_LOCAL_PASS
FRESH_RAW_EXACT_PREFLIGHT_READY
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

This historical result does not override the later A2 fail-closed route
precondition. It is not exact-preflight readiness and it is not permission to
reuse the A2 approval or prepare a live authorization.

## P5E.9A-LQ — historical production lineage-query stop

The QF device result above is historical code206 evidence. LQ found that the
production fresh runner used `binding_identity` directly on
`editorial_p5d_reconciliation`, although schema v24 stores only
`attempt_identity`. The historical QF device run reproduced
`no such column: binding_identity` without touching the current pilot DB or
calling a provider. The new disposable-v24 regression fixture is compiled but
not yet executed because the code207 candidate is awaiting approval.

Production fix `ff6821a` centralizes the read-only lineage check. It joins
reconciliation, reconciliation history and network lifecycle through
`attempt_identity` to `editorial_p5c_attempts.binding_identity`, reads all five
lineage owners before calculating status, and maps any query/schema failure to
typed `P5E_FRESH_RAW_LINEAGE_CHECK_FAILED`. No schema, migration, pack,
profile, authority, wire schema, report/receipt schema, route or cap changed.

Host qualification passed after the fix: engine `200/200`, app
debug/release/benchmark `232/232` each, lint debug PASS and AndroidTest compile
PASS. Provider calls and current-DB mutation were `0`.

The new candidate is archived but not installed:

```text
version=v4.17-p5e.11
versionCode=207
event=build-20260911-201725
apkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
sourceZipSha256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
artifactBackupByteEqual=true
```

The prior candidate-aligned test APK is superseded, not approved and not
installed:

```text
path=D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk
package=com.ml.tblandroidtxt.test
sha256=9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
sourceSnapshot=995d3b6c9678e93905b3802cf22eee0b091b1bb3
status=SUPERSEDED_NOT_APPROVED_NOT_INSTALLED
```

## P5E.9A-LQ-QF2 — code207 test-artifact alignment (pre-DV host stop, historical)

QF2 corrected only the AndroidTest version expectation from `206L` to `207L`
and renamed the schema-failure test to match its typed-check scope. The
production source, candidate APK, schema, canonical pack/profile/authority,
binding/evaluation/run/chapter, route and output cap were not changed. The
AndroidTest compilation passed, the test APK was built outside Git, and no
production APK was rebuilt.

```text
testPackage=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
targetCandidateVersion=v4.17-p5e.11
targetCandidateVersionCode=207
testRunner=androidx.test.runner.AndroidJUnitRunner
testSourceCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkBytes=1313798
testApkLastWrite=2026-09-11 20:42:59 +07:00
testArtifactCaptured=2026-09-11 20:43:14 +07:00
testApkVersionCodeMetadata=not_present_in_androidTest_manifest
status=BUILT_NOT_INSTALLED_NOT_APPROVED
```

The frozen production candidate was re-hashed without rebuilding:

```text
productionVersion=v4.17-p5e.11
productionVersionCode=207
productionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
productionSourceZipSha256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
productionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
artifactBackupByteEqual=true
productionRebuild=false
```

The installed device remains the prior code206 baseline and this QF2 run did
not issue any device or adb operation. The current DB remains classified as
`RECONSTRUCTED_ONLY`; no DB was opened or changed. Provider calls,
authorization creation/consumption, attempts, reconciliation, retry and repair
are all `0` for QF2. The old test artifact is superseded. A separate owner
approval is required for the new test APK before replacing only
`com.ml.tblandroidtxt.test` and running direct instrumentation. This is not
P5E.9B authorization and does not permit a provider call.

The historical next action at that time was exactly that test-artifact
approval; RAW authorization was not to be prepared or consumed until the device
helper gate had been executed. This historical step is superseded by the
current A4.1 host-contract gate.

## P5E.9A-LQ-DV — historical guarded code207 install and zero-call device verification

The separate owner approval `P5E_9A_LQ_DV_OWNER_APPROVAL` was received and
used only for the listed local/device actions. The current production package
was read before and after each permitted operation. The code207 candidate was
installed exactly once through `scripts/install-validated.ps1` after
CheckOnly, snapshot and isolated restore passed. The existing test package was
replaced exactly once with the approved artifact. No uninstall, clear, reset,
downgrade, fallback, connected AndroidTest or second production install was
performed.

```text
device=15e84958
productionPackage=com.ml.tblandroidtxt
installedProductionVersion=v4.17-p5e.11
installedProductionVersionCode=207
installedProductionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
installedProductionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
deviceSignatureToken=abebea4b
schemaVersion=24
dataClassification=RECONSTRUCTED_ONLY
productionUpgradeCount=1
testPackageReplacementCount=1
connectedAndroidTest=NOT_RUN
```

### Backup and restore evidence

The one approved snapshot force-stop was used for snapshot capture only. The
WAL-aware capture found the database and a zero-length journal, with no WAL or
SHM sidecar. The snapshot and restore artifacts remain outside Git:

```text
snapshotRoot=D:\P5E-private\p5e-9a-lq-dv-snapshot-20260911-210256
snapshotManifestSha256=BBE47271716785B1ED89F888748428C9A0437A47FF61804942FAF202BBE49C76
snapshotDatabaseSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
snapshotJournalSha256=E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855
snapshotWalPresent=false
snapshotShmPresent=false
snapshotRestoreRoot=D:\P5E-private\p5e-9a-lq-dv-restore-20260911-210256
snapshotRestoreStatus=PASS_DATA_LEVEL_ONLY
postInstallReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postinstall-20260911-210550
postRunReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postrun-verified2-20260911-211156
```

The isolated restore reported SQLite `integrity_check=ok`, schema v24, zero
foreign-key violations and the exact fresh binding. Post-install and post-run
readback preserved the DB SHA-256 and source/pack/profile identities. The
fresh project has one canonical chapter `001`; two global `001` rows are
expected because another reconstructed project is retained.

```text
dbBeforeSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbAfterSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
databaseHashUnchanged=true
freshSelector=p5e-fresh-mercedes-vol5-20260911-01
freshChapterKey=001
freshBinding=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
freshRunDeclaration=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
freshEvaluation=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
freshPackSha256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
freshProfileSha256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
freshProjectChapterRows=1
globalChapterKey001Rows=2
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportBytesNonEmpty=0
receiptBytesNonEmpty=0
partialCommit=false
automaticRedispatch=false
providerCalls=0
```

### Direct zero-call instrumentation

The installed test package was verified as the approved APK before direct
instrumentation. The run used `adb shell am instrument` only, with the
production package left untouched:

```text
testPackage=com.ml.tblandroidtxt.test
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testSourceCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
testRunner=androidx.test.runner.AndroidJUnitRunner
schemaFailureMethod=1/1 PASS
chapterSemanticsMethod=1/1 PASS
qfSingleMethod=1/1 PASS
qfBoundaryClass=5/5 PASS
lineageClass=13/13 PASS
allTestsNoSkip=true
providerCalls=0
```

The lineage class used disposable databases for mutation cases. The current
pilot DB was used only for read-only tuple, preflight and count assertions.
The production-owned helper now parses the schema-v24 attempt-identity joins,
returns typed failure for isolated schema defects and does not fall back to an
unused lineage. No authorization or attempt was created.

### Historical DV boundary

```text
P5E_9A_LQ_TEST_VERSION_PIN_CORRECTED
CODE207_PRODUCTION_CANDIDATE_UNCHANGED
CODE207_DEVICE_VERIFIED
P5E_9A_QF_ZERO_CALL_BOUNDARY_PASS
P5E_9A_LQ_PRODUCTION_LINEAGE_QUERY_FIX_PASS
P5E_9A_LQ_DEVICE_ZERO_CALL_PASS
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_PASS
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

This does not claim `P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS` or a live
RAW acceptance: the owner approval forbade valid-authorization dispatch. It is
historical DV evidence and not the current A3 boundary.

## Current active boundary — P5E.9B-A4.2 exact preflight pass; RAW proposal prepared, not issued

A4.2 did not reuse the consumed A2/A3.2 approvals. It started from execution
HEAD `005317cd83f107edbf275734cb2977b9929e88ce` on branch
`fix/v4.18-p5e-9b-a4-1`; the test artifact source/archive commit is
`d51b7f3c16bdc482513b9904db07b97daed592d1`. The owner-approved boundary
performed one test-package replacement and one exact-preflight invocation.
Production was not operated on; no provider, authorization, attempt or
reconciliation creation occurred.

The host RED audit confirmed that the previous A4 artifact's manifest already
contained `reconciliationCreated=false`, while the parser required it and the
actual `redactedPreflightStatus()` mapping omitted it. The test-only fix adds
the mapping immediately after `attemptCreated`. The emitter-contract test reads
the actual AndroidTest source and compares mapping coverage with parser
required/allowed/strict-boolean sets; parser fixtures remain supplemental.

Host QA passed with JDK `21.0.10`, pinned SDK `android-35`, Gradle `9.3.0`:
targeted parser/emitter-contract tests, AndroidTest compilation, production
diff guard `0`, secret scan and `git diff --check`. The test-only archive path
is `scripts/build-and-save-android-test.ps1`; it runs only
`:app:assembleDebugAndroidTest`, records an exact tracked-source ZIP and
SHA-256 manifest, and mirrors the payload to artifact and backup roots. The
artifact below was then used once as the test package and read back exactly:

```text
path=D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk
bytes=1155224
sha256=57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA
certificateSha256=47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155
package=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
runner=androidx.test.runner.AndroidJUnitRunner
sourceCommit=d51b7f3c16bdc482513b9904db07b97daed592d1
sourceZipSha256=382EC5D12FC786BC358316434692E49A73BD62C2F9DD9AC6BD1BC9274CE0B3FA
productionApkRebuilt=false
providerCalls=0
backupByteIdentical=true
```

The previous A4 artifact
`DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B` is
retained and classified `SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING`; it
must not be installed. The prior exact-preflight `OK (1 test)`/terminal `-1`
observation remains historical evidence-channel failure, not exact-preflight
readiness. A4.2 now supersedes that pending device boundary: its exact
preflight was accepted after the corrected status emitter and complete
post-readback. The current action is owner review of the prepared A4.3 RAW
authorization packet derived from the A4.2 evidence. It remains RAW-only with
one primary call, zero repair/retry and no RECONCILE; P5/P5E exit, execution,
certification and P6 remain closed.

A4.2 host preparation is recorded in
`D:\P5E-private\a4-2-exact-preflight-host-prep-20260914-184255833` with
checksum-file SHA-256
`CCE290D4ABC11E58623633C5E2208BFCD51C7CA9A44A8978DEB5ACBCC78D59E3`.
Its preparation document SHA-256 is
`C6B1CD88C212EA51DA698DF3AA89335FD0C6B7334E53B2617CD0DCFF45945BD6`.
The completed device evidence is in
`D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591` with
`A4-2-EVIDENCE-MANIFEST.md` SHA-256
`7FBBECD3E2A8868D42D34CB3F9F2F8BA8CDAC236B4C7161F6747CBB19EE2474D`.
It records one `OK (1 test)`, terminal `-1`, parser acceptance, all route and
preservation booleans true, raw output SHA-256
`1FFE572DF3ADEBA6A8AB55061BD75F2F5EEBBC479F8BB0D1EA97DB3957D1577C`, and
WAL-aware before/after preservation. The A4.2 approval is consumed and not
reusable.

## P5E.9B-A4.3 — owner-approval packet prepared, not issued

The current owner-review files are:

- docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md
- docs/P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md, SHA-256
  DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501
- docs/P5E_RAW_AUTHORIZATION_COMMAND.txt, SHA-256
31B075935CECA342A249961F8E871410A700478B780A827F25D21DDAEE13BE98

This packet keeps the production code207 APK/certificate and the A4.2 test
APK SHA-256 57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA.
It permits only RAW/GLOSSARY egress to the pinned route for one primary
L1_RAW_DISCOVERY call; DRAFT/PRONOUN remain hidden. The current harness
authorization ID is retained and A4.2 evidence shows it unused.

The endpoint account fingerprint is still pending. Before authorization
construction, owner approval must permit the source-defined memory-only check:
load SettingsStore settings, normalize the endpoint, hash UTF-8 endpoint +
newline + the in-memory credential, and compare to the owner-supplied 64-hex
fingerprint. No credential/settings content may be written to command, logs or
evidence. A mismatch or unverifiable account stops before authorization or
provider dispatch. No device credential read or instrumentation rerun occurred
in this audit.

Runtime timeout/DB rule: the live method owns its internal preflightOnly and
UNUSED-lineage checks; no separate instrumentation preflight is allowed. The
host observation window is 240000 ms. Any timeout, missing post-check or
inconsistent state stops without retry, cleanup, restore or redispatch.

## Historical active boundary — P5E A4 model remediation and preflight evidence blocker

The canonical A4 pin table is maintained in `EDITORIAL_RECOVERY_V4_18.md`;
the facts below are its current-result reference, not a second authority table.

The A4 device event is
`D:\P5E-private\a4-model-preflight-device-20260913-200643711`. The read-only
baseline verified device `15e84958`, production package code207 with the pinned
APK/certificate/signature, schema v24, DB SHA-256
`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`, SQLite
integrity `ok`, FK violations `0`, the exact fresh tuple and zero lineage. No
production or test PID/job was active, so no force-stop was required.

The test package was replaced once with A4 artifact SHA
`5D248FFD33F52AC649966C4135773708C7CA747CE34C28BB763B40EC1FE81467`. The
model-remediation method ran once and reported model write/readback success,
route match and preservation of all non-model settings. The settings file was
`PRESENT` before and after; only its hash was retained, changing from
`C2FC2DC71F3687E09F6D3899A99F397C3B75AD05B402D176367C03A0280E8062` to
`4A0AA4564B62D5F9852A108B1D7991DA21AEF46DCDA4ACBC485E6E45CD3214F9`.

The exact-preflight method ran once. It returned `OK (1 test)`, terminal
instrumentation code `-1`, all route booleans true, valid conjunction, DB
preservation true and provider calls `0`. It was not accepted because the
versioned status output omitted required
`p5e.preflight.v2.reconciliationCreated`; the host parser produced
`MISSING_FIELD:reconciliationCreated`. This is an evidence-channel blocker,
not permission to infer exact-preflight readiness or rerun the method.

Post-readback still matched the DB hash/schema/integrity/FK, exact fresh tuple,
source identities and all zero lineage/report/receipt counts. The one-shot
window is consumed. A test-only correction was committed at
`911fb355a2148feb8c7ec4b60a843c547596bf56` and built privately at
`D:\P5E-private\a4-fresh-raw-model-preflight-statusfix-20260913-202218809\app-debug-androidTest.apk`
with SHA-256
`DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B`; it is
`SUPERSEDED_NOT_INSTALLED_INVALID_STATUS_MAPPING`, not installed and must not
be used. The current A4.1 owner-approval path is recorded above. No provider,
authorization, attempt, reconciliation, repair, retry or production-package
operation occurred.
The A4 device evidence manifest SHA-256 is
`4C902DDEAEC7554C42EDB65F53ECC73403ECB45F4CE0A8D0BF74DB3312844EF0`; the
new test-artifact manifest SHA-256 is
`E333AC3A3FC21A808F69EDFC71ACD9710629C012C6DCE091C19B7F86ED282109`.

## Historical A3.2C delayed read-only closure blocked

The current state is the A2 fail-closed result, not the historical
`FRESH_RAW_EXACT_PREFLIGHT_READY` result above. A2 evidence SHA-256 is
`42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`.
The single-run A2 approval is consumed and cannot be reused.

A3.1/A3.1R and the A3.2 host preparation are historical evidence. The owner-
approved A3.2 run replaced the test package once and invoked exactly one
diagnostic method. The raw result carried four redacted booleans through
instrumentation status: provider `true`, model `false`, endpoint `true`, route
`false`; the conjunction was valid and the test result was `OK (1 test)`.
The A3.2 run is not a valid-authorization or exact-preflight acceptance.

Pre-run read-only verification passed for production code207, the A2 test APK,
DB schema/hash/integrity/FK, fresh tuple/source hashes and zero lineage counts.
The A3R test package was replaced once and immediately read back with its exact
hash. During the first post-run read-only sequence the device became unavailable
to ADB. Settings-after, DB-after and complete lineage preservation are unknown;
no retry, workaround or rollback was performed.
The current PreTag result is `FAIL` at `Step 05`; the earlier `Step 09` result
is historical A3.1 evidence and is not the current gate result.

```text
testSourceCommit=89eef75a4a62e5674d02b7e48eaaff012d9a7ae0
testApkPath=D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk
testApkSha256=64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testPackage=com.ml.tblandroidtxt.test
testTargetPackage=com.ml.tblandroidtxt
testRunner=androidx.test.runner.AndroidJUnitRunner
testApkBytes=1326212
testApkInstalled=false
testApkStatus=SUPERSEDED_NOT_INSTALLED
providerCalls=0
deviceOperations=0
currentDbMutation=0
```

The corrected A3R artifact is private, built from test-only commit
`9b59ce39b326d5e81e62861b150a618a5e80cddc`, and has the following immutable
identity:

```text
testSourceCommit=9b59ce39b326d5e81e62861b150a618a5e80cddc
testApkPath=D:\P5E-private\fresh-raw-route-diagnostic-a3r-20260912-005638832-test-apk\app-debug-androidTest.apk
testApkSha256=F19051D849139CB66C4005342AF45DEE62F2A1C7C44D7F0316EC810F8F4DBD1E
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testPackage=com.ml.tblandroidtxt.test
testTargetPackage=com.ml.tblandroidtxt
testRunner=androidx.test.runner.AndroidJUnitRunner
testApkBytes=1326403
testApkInstalled=true
testApkStatus=INSTALLED_TEST_ONLY_ONCE; post-run pull not validated after device loss
diagnosticStatusKeys=4
providerCalls=0
deviceOperations=0
currentDbMutation=0
```

The A3.2 approval was consumed for one test-package replacement and one
diagnostic method. The raw result emitted `providerMatch=true`,
`modelMatch=false`, `endpointMatch=true`, and `routeMatch=false`; the
conjunction was valid and the test result was `OK (1 test)`. The result is an
observed model mismatch only; it is not exact-preflight readiness or a valid
authorization path. Android returned terminal instrumentation code `-1`, the
success sentinel; the earlier host parser's requirement for terminal code `0`
was a parser defect, and no rerun was made.

Private evidence event:
`D:\P5E-private\fresh-raw-route-diagnostic-a3.2-run-20260912-012928513`.
The first post-run read-only sequence returned `device '15e84958' not found`
while pulling the test package and checking process state. No retry, force-stop,
workaround or rollback was performed. Settings-after, DB-after and complete
lineage preservation are unknown, so A3.2 acceptance is not granted. The A2
approval remains historical and is not reusable.

The permitted settings evidence is a SHA-256 of the entire settings file
before and after the one diagnostic invocation, or an `ABSENT` marker when the
file does not exist. It must not pull, serialize or log file content, XML,
paths, setting values, API-key presence or API keys. The diagnostic invocation
does not run the synthetic test cases; those remain offline preparation only.

```text
P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED
P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED
P5E_9B_A3_1_TECHNICAL_PASS
P5E_9B_A3_1R_DOCUMENTATION_AND_EVIDENCE_PASS
P5E_WORKFLOW_PRETAG_FAIL_CLOSED
P5E_9B_A3_2_ROUTE_DIAGNOSTIC_OUTPUT_OBSERVED
P5E_9B_A3_2_MODEL_MISMATCH_OBSERVED
P5E_9B_A3_2_POST_RUN_DEVICE_UNAVAILABLE
P5E_9B_A3_2C_DEVICE_PROCESS_ACTIVE
P5E_9B_A3_2C_DELAYED_READBACK_NOT_REACHED
P5E_9B_A3_2_PRESERVATION_NOT_PROVEN
P5E_9B_A4_REMEDIATION_NOT_SELECTED
P5E_9B_ROUTE_DIAGNOSTIC_A3_ARTIFACT_SUPERSEDED_NOT_INSTALLED
P5E_9B_ROUTE_DIAGNOSTIC_A3R_ARTIFACT_INSTALLED_TEST_ONLY
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
```

## P5E.9B-A3.2C — delayed read-only preservation closure stop

The host baseline passed at branch `feature/v4.18`, HEAD
`94d2cd535bc728df47391edead23ed298093e03b`, with a clean worktree and PreTag
`FAIL` at Step 05. The prior A3.2 evidence was rehashed without editing: its
stored manifest declares 27 entries although the task text described 24; all
27 matched and the manifest SHA-256 remains
`12898E7A7DFFDCBEEF4E2E0BF6794E88C90EDE4E6DAD15D0147C9710ABF137CB`. The raw
instrumentation and corrected redacted parse agree on one completed test,
route booleans provider `true`, model `false`, endpoint `true`, route `false`,
valid conjunction and final instrumentation code `-1`; this is a route-output
result, not preservation or acceptance.

The one-shot availability check found the expected device, then the required
read-only process check found production PID `420` active and no test PID. The
process was not force-stopped, so package/settings/database delayed readback was
not attempted. Evidence is in
`D:\P5E-private\fresh-raw-route-diagnostic-a3.2c-20260912-070137359` with event
manifest SHA-256
`2EB4EDEC9376B7FAE692A5050105C8E263C7B223B3B2B7C03FC992C43BC70615`.
The partial old `test-after-run.apk` remains preserved and is classified as
`INTERRUPTED_PULL_EVIDENCE_NOT_AN_INSTALLED_APK_IDENTITY`.

The settings file was `ABSENT` in the A3.2 pre-run evidence. Source
characterization records that `SettingsStore.load` falls back to `AppSettings`
defaults: provider and endpoint match the policy, while default model
`anthropic/claude-sonnet-4.6` mismatches the required fresh RAW model. This is
an explanation consistent with A3.2, not historical A2 proof. The historical
next action at that time was a separately authorized read-only window after
both processes were idle; no
automatic retry or force-stop is permitted. A4 is not selected and has not been
executed.
