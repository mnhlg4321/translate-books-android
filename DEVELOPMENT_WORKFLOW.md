# Development Workflow v4.8+

Quy trình này bắt buộc cho mỗi vòng đời release từ v4.8 trở đi. Mười bốn bước là thứ tự của toàn release, không phải danh sách phải khởi tạo lại trong mỗi lượt làm việc. Một release dùng một canonical plan, một branch và một checklist từ lúc bắt đầu đến khi hoàn tất.

Khi tiếp tục một release đang dở, lượt làm việc chỉ đọc lại authority/state, xác minh branch/HEAD/status rồi tiếp tục đúng phase và next action. Không chạy lại bước tạo branch/checklist và không mở work package mới nếu scope release không đổi.

## Quy tắc hoàn thành

- Mỗi phiên bản tạo một checklist từ `release_checklists/TEMPLATE.md`.
- Mỗi bước chỉ được đánh dấu hoàn tất khi có Evidence cụ thể: commit, command/result, file, hash, tag hoặc đường dẫn archive.
- Nếu local test, compile, lint hoặc build fail, giữ bước chưa hoàn tất, ghi `FAILED_REPAIRING`, sửa trong cùng branch/phase rồi chạy lại. Chỉ dừng các bước phát hành phụ thuộc như tag/archive; không tạo plan, branch, checklist, review-stop hay phiên bản phụ cho lỗi đó.
- Chỉ dùng `BLOCKED_EXTERNAL` khi thiếu tài nguyên/quyền bên ngoài không có phương án local an toàn, hoặc khi cần một quyết định phá hủy hay thay đổi scope sản phẩm. Blocker của track historical không áp dụng cho release hiện tại.
- Không dùng kết quả build/regression/QA của phiên bản cũ để đánh dấu pass cho phiên bản mới.
- Không tạo commit rỗng để đánh dấu bước.

## 14 bước cố định

1. **Đọc BUILD_STATE.md** — xác nhận version, SDK, build, artifact, regression và known issues hiện tại.
2. **Đọc WORKSPACE_SNAPSHOT.md** — xác nhận completed/pending tasks, blocker và next step.
3. **Xác nhận commit hiện tại** — ghi `git rev-parse HEAD` và `git status --short --branch`; working tree phải sạch trước khi tách branch.
4. **Tạo branch feature mới một lần** — mặc định từ `main`, đúng mẫu `feature/vX.Y`; không tái sử dụng branch đã merge. Historical recovery được phép tách từ commit chính xác do owner chỉ định khi canonical plan ghi baseline, lý do, artifact/hash và yêu cầu versionCode tăng. Mọi lượt sau resume branch này.
5. **Phát triển** — chia từng nhóm sửa độc lập thành commit nhỏ, message rõ ràng.
6. **Regression** — chạy suite phù hợp; ghi pass/fail/skip và command chính xác.
7. **Build** — chạy `.\scripts\build-and-save.ps1`; mỗi build phải có version riêng, README/checksum/source ZIP và bản sao bất biến trong cả `artifacts/builds/` lẫn `backup/builds/` trước khi được cài lên thiết bị. Cấm build APK trực tiếp.
8. **QA** — hoàn tất QA report, device/manual checks, performance evidence và known limitations.
9. **Commit** — commit đầy đủ code, regression và QA theo từng nhóm; working tree phải sạch.
10. **Tag** — chỉ tạo annotated tag sau khi gate PreTag xác nhận bước 1–9.
11. **Backup** — tạo archive/backup bất biến sau tag; không ghi đè event cũ.
12. **Cập nhật BUILD_STATE** — ghi version/build/tag/commit/artifact/backup/regression/known issues mới.
13. **Cập nhật WORKSPACE_SNAPSHOT** — ghi trạng thái bàn giao và next step chính xác.
14. **Xuất artifact** — xác minh APK, checksum, release evidence và source ZIP ở cả artifact lẫn backup.

## Gates

```powershell
# Trước bước 10: bắt buộc hoàn tất 1–9
.\scripts\verify-release-workflow.ps1 -ChecklistPath release_checklists\vX.Y.Z.md -Gate PreTag

# Trước bước 11: bắt buộc hoàn tất 1–10
.\scripts\verify-release-workflow.ps1 -ChecklistPath release_checklists\vX.Y.Z.md -Gate PreBackup

# Trước khi tuyên bố hoàn tất: bắt buộc hoàn tất 1–14
.\scripts\verify-release-workflow.ps1 -ChecklistPath release_checklists\vX.Y.Z.md -Gate Complete
```

Mọi gate phải trả về exit code 0. Exit code khác 0 có nghĩa phiên bản chưa hoàn tất.

## Chu kỳ sửa lỗi trong một release

```text
develop
  -> targeted test
  -> fail: FAILED_REPAIRING -> diagnose -> patch -> targeted test lại
  -> pass: phase tiếp theo
  -> full regression
  -> numbered build
  -> QA/archive/release gates
```

Local failure không phải lý do dừng toàn bộ hướng phát triển. Không có review-stop bắt buộc giữa các phase nội bộ đã được canonical plan phê duyệt.

## Lệnh build bắt buộc

```powershell
# Build, đánh số và lưu hai bản; không cài
.\scripts\build-and-save.ps1

# Build, lưu hai bản trước rồi mới cài vào thiết bị
.\scripts\build-and-save.ps1 -Install `
  -DeviceSerial '<explicit-serial>' `
  -ExpectedDeviceSignatureToken '<preflight-device-token>' `
  -ExpectedApkCertificateSha256 '<64-hex-certificate-digest>'
```

Không chạy `assembleDebug` trực tiếp và không dùng Android Studio **Build APK(s)**. Xem `BUILDING.md` để biết cấu trúc artifact và quy tắc đánh số.
