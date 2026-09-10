# P5E — Cài đặt có kiểm soát và bảo toàn pilot data

Tài liệu này chỉ áp dụng cho validation/pilot có package
`com.ml.tblandroidtxt`. Nó không cấp quyền provider, không khởi tạo fresh pilot
và không thay thế authorization/preflight. Không chạy `uninstall`, `pm clear`,
reset, downgrade hoặc `--downgrade` trên device đang giữ dữ liệu pilot.

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
không còn lệnh phù hợp. Vì vậy kết luận chắc chắn là connected installer đã
đi vào đường cài lower-version và sau đó package/data biến mất trước lần
validation kế tiếp; actor cleanup chính xác không được suy diễn từ evidence
hiện có. `daemon-39532.out.log` chỉ chứng minh hậu quả quan sát được: các test
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

Lệnh candidate code202 ở trên là `-CheckOnly`; nó đã pass preflight metadata
nhưng không chứng minh candidate đã được cài hoặc device-verified. Sau khi owner
phê duyệt, chỉ được bỏ `-CheckOnly` sau khi backup/restore reconstructed DB đạt;
không được chạy lệnh này như fallback cho package thiếu hoặc version mismatch.

Device-side `dumpsys` chỉ cung cấp short signature token (`abebea4b`), còn APK
được kiểm tra bằng certificate SHA-256 đầy đủ. Khi package đã tồn tại,
`adb install -r` vẫn là lớp Android cryptographic signer check cuối cùng; guard
không ép bypass nếu signer không tương thích.

## Backup/restore policy

Chỉ tìm backup ở các root đã biết: `artifacts`, `backup` của repository hiện tại
và các evidence path đã ghi trong P5E. Lần kiểm tra hiện tại không tìm thấy
`.db`, `.sqlite`, `.sqlite3` hoặc file recovery nào; `D:\Ebooks\New folder\metadata.db`
là metadata không liên quan. Không có backup đáng tin cậy nên không có restore
test và gate lịch sử vẫn là:

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

Fresh pilot là một lineage mới, không phải recovery: tạo evaluation/run/binding/
attempt identity mới, authorization single-use mới và giữ source hash nếu exact
bytes không đổi. Không sửa nhãn, không tạo receipt cho attempt cũ, không biến
authorization đã consumed thành khả dụng và không trộn các row reconstructed
với lịch sử code196. Vòng công việc hiện tại chỉ đề xuất đường này; chưa
khởi tạo fresh pilot.
