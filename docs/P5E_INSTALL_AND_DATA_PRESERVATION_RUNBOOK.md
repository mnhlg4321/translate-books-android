# P5E — Cài đặt có kiểm soát và bảo toàn pilot data

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
evidence. The stale `f319…` value was a runner expectation; the persisted fresh
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

Current boundary result:

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

This result closes only the local zero-call boundary. The next step is to
prepare and separately request exact P5E.9B RAW authorization; do not create or
consume it automatically.

## P5E.9A-LQ — current production lineage-query stop

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

The candidate-aligned test APK is outside Git and not installed:

```text
path=D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk
package=com.ml.tblandroidtxt.test
sha256=9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
sourceSnapshot=995d3b6c9678e93905b3802cf22eee0b091b1bb3
```

The installed device remains code206 and the reconstructed current DB remains
unchanged. This runbook therefore stops at `NEW_CANDIDATE_OWNER_APPROVAL_REQUIRED`;
no fresh candidate device verification or valid-authorization local-path claim
is made. The next action is a separate owner approval pinning both artifacts,
followed by snapshot/restore and guarded installation. It is not P5E.9B
authorization and does not permit a provider call.
