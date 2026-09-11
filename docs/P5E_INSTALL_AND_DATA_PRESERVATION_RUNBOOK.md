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

## P5E.9A freeze correction — current result

The code202/G3/G4 entries above are retained as prior evidence. For the current
exact-binding boundary, the patched candidate is code204 (`v4.17-p5e.8`), source
commit `a88673ab113c0877119d078c2fb569fd4f39c55a`, APK SHA-256
`6BECD0F89ABAD308617CBB864AB38B62BCBDAE00E876BE96D57E3AD3FD8F8C83`, and
certificate SHA-256
`47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`. It is
archived in both artifact/backup roots but was not installed. The corresponding
test APK is outside Git and has SHA-256
`41350151A4AB6AAA8C25A5B7AE0FE71D7297ECD1C3535233D23F11A5B487B144`.

The read-only freeze on `15e84958` confirmed package `com.ml.tblandroidtxt`,
installed code202, signature token `abebea4b`, schema v24, the requested
selector/binding/run/chapter, source/pack/profile hashes and zero attempts,
authorization receipts, reconciliation, history, lifecycle and report/receipt
rows. It found one fail-closed mismatch: observed evaluation
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` versus pinned
`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1`.

Because the evaluation identity is wrong, stop before candidate install,
device instrumentation and exact preflight. Do not change the row with SQL,
silently repin the evaluation, or reuse the old owner approval for code204. A
new private reconstructed snapshot/isolated SQLite data-level restore is at
`D:\P5E-private\fresh-raw-boundary-20260911-1810` with manifest SHA-256
`6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28`; it is
not code196 recovery and not full post-upgrade app restore evidence.

Current stop state:

```text
P5E_9A_FRESH_RAW_BOUNDARY_LOCAL_PASS: NOT_REACHED
P5E_9A_DEVICE_FREEZE_BLOCKED_FRESH_EVALUATION_MISMATCH
RAW_AUTHORIZATION_REQUIRED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
```
