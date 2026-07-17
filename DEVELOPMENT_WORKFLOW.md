# Development Workflow v4.8+

Quy trình này bắt buộc cho mọi phiên phát triển từ v4.8 trở đi. Thứ tự không được thay đổi và không được bỏ qua bước.

## Quy tắc hoàn thành

- Mỗi phiên bản tạo một checklist từ `release_checklists/TEMPLATE.md`.
- Mỗi bước chỉ được đánh dấu hoàn tất khi có Evidence cụ thể: commit, command/result, file, hash, tag hoặc đường dẫn archive.
- Nếu một bước fail, giữ nguyên trạng thái chưa hoàn tất, ghi lỗi vào Evidence/Pending tasks, dừng các bước phụ thuộc và không tuyên bố hoàn thành.
- Không dùng kết quả build/regression/QA của phiên bản cũ để đánh dấu pass cho phiên bản mới.
- Không tạo commit rỗng để đánh dấu bước.

## 14 bước cố định

1. **Đọc BUILD_STATE.md** — xác nhận version, SDK, build, artifact, regression và known issues hiện tại.
2. **Đọc WORKSPACE_SNAPSHOT.md** — xác nhận completed/pending tasks, blocker và next step.
3. **Xác nhận commit hiện tại** — ghi `git rev-parse HEAD` và `git status --short --branch`; working tree phải sạch trước khi tách branch.
4. **Tạo branch feature mới** — từ `main`, đúng mẫu `feature/vX.Y`; không tái sử dụng branch đã merge.
5. **Phát triển** — chia từng nhóm sửa độc lập thành commit nhỏ, message rõ ràng.
6. **Regression** — chạy suite phù hợp; ghi pass/fail/skip và command chính xác.
7. **Build** — build thành công; APK phải được chuyển khỏi `build/` ngay lập tức.
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

