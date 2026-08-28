# Git Workflow

Từ v4.8 trở đi, `DEVELOPMENT_WORKFLOW.md` và checklist 14 bước là gate bắt buộc. Nếu bất kỳ gate nào fail thì không được tag, backup, xuất artifact hoặc tuyên bố hoàn tất.

Quy trình này áp dụng cho mọi thay đổi của dự án kể từ sau tag `v4.7`.

## Luồng bắt buộc

```text
main
  -> feature/vX.Y
  -> các commit nhỏ theo từng nhóm sửa độc lập
  -> regression
  -> commit kết quả regression (chỉ khi có file kết quả thay đổi)
  -> tạo APK và checksum
  -> annotated tag vX.Y
  -> merge --no-ff về main
```

Không commit trực tiếp lên `main`. Không squash khi merge vì phải giữ lại lịch sử các commit nhỏ.

Một release chỉ có một feature branch và một checklist. “Session” là một lượt tiếp tục trên branch đó, không phải một lý do tạo branch mới. Local test/build failure được sửa và commit trên cùng branch.

## 0. Active v4.17 historical recovery exception

`main` hiện cũ hơn baseline dịch đã được owner chọn, nên v4.17 dùng ngoại lệ phục hồi có kiểm soát:

- canonical plan: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`;
- exact source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113);
- verified baseline APK SHA-256: `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`;
- single branch: `feature/v4.17-translation-profile-compatibility`;
- isolated worktree: `C:\Users\ADMIN\Documents\App Translate Books-translation-profile`;
- original dirty workspace and later RSC/Editorial evidence are protected and must not be reset, moved, deleted, staged, or merged into this release by default;
- every new APK must use Android `versionCode` greater than 168.

This exception authorizes only the v4.17 scope frozen in the canonical plan. It does not establish a general practice of branching from arbitrary historical commits.

## 1. Bắt đầu phiên bản

Thông thường chỉ bắt đầu khi working tree trên `main` sạch:

```powershell
git switch main
git status --short
git switch -c feature/vX.Y
```

Mỗi branch phiên bản dùng đúng mẫu `feature/vX.Y`, ví dụ `feature/v4.8`.

Nếu canonical plan có historical recovery exception như v4.17, tạo một worktree sạch trực tiếp từ exact approved commit và ghi evidence vào checklist. Sau đó mọi lượt làm việc dùng lại worktree/branch đó. Không checkout/reset workspace dirty để mô phỏng một baseline sạch.

## 2. Chia commit

Mỗi nhóm thay đổi độc lập có một commit riêng. Không gom sửa giao diện, hiệu năng, estimator và regression vào cùng một commit.

Ví dụ lịch sử hợp lệ:

```text
fix(scroll): remove nested scrolling regression
perf(dashboard): reduce redundant dashboard rendering
perf(estimator): reuse cached estimation inputs
test(regression): record v4.8 regression results
docs(release): prepare v4.8 release metadata
```

Message dùng dạng:

```text
<type>(<scope>): <mô tả rõ ràng>
```

Các type được chấp nhận: `feat`, `fix`, `perf`, `refactor`, `test`, `build`, `docs`, `chore`. Scope có thể bỏ qua khi không cần thiết.

Trước mỗi commit:

```powershell
git diff
git diff --cached
```

Chỉ stage file thuộc cùng một nhóm sửa. Không dùng commit rỗng, không dùng `--allow-empty`, và không tạo commit chỉ để đánh dấu một bước nếu không có file thực sự thay đổi.

## 3. Regression

Sau các commit triển khai, chạy tối thiểu:

```powershell
.\scripts\build-and-save.ps1
```

Script này chạy `clean`, unit test, lint và `assembleDebug`, sau đó lưu APK cùng README, metadata, checksum và source snapshot vào cả artifact lẫn backup. Không chạy `assembleDebug` trực tiếp.

Khi có thiết bị Android phù hợp, chạy thêm:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Ghi command, môi trường, số test pass/fail/skip, lint, artifact và known issues vào `QA_REPORT_vX_Y.md` hoặc báo cáo release tương ứng. Commit báo cáo bằng message `test(regression): record vX.Y regression results` nếu báo cáo có thay đổi. Nếu kết quả không làm thay đổi file nào thì không tạo commit rỗng.

Không tiếp tục phát hành khi regression fail, trừ khi lỗi được ghi nhận rõ và có quyết định chấp nhận release.

## 4. APK và metadata release

Tạo APK sau khi regression đạt yêu cầu:

```powershell
.\scripts\build-and-save.ps1
```

Nếu cần cài lên thiết bị, chỉ dùng `.\scripts\build-and-save.ps1 -Install`; script luôn archive trước rồi mới cài. Xác nhận version, tên APK, kích thước, SHA-256, README và hai đường dẫn artifact/backup. Cập nhật riêng các file sau nếu thông tin thay đổi:

- `BUILD_STATE.md`
- `RELEASE_NOTES.md`
- `CHANGELOG.md`
- báo cáo QA của phiên bản

Commit metadata release riêng, ví dụ `docs(release): prepare v4.8 release metadata`. APK và AAB vẫn tuân theo `.gitignore`; artifact nhị phân được lưu/phân phối ngoài Git, còn checksum và bằng chứng QA có thể được commit.

## 5. Tag và merge

Tag annotated chỉ được tạo trên commit release đã qua regression và đã có APK:

```powershell
git tag -a vX.Y -m "Translate Books vX.Y"
git show vX.Y --no-patch
```

Không di chuyển hoặc ghi đè tag đã phát hành.

Sau khi tag, merge branch về `main` mà không squash:

```powershell
git switch main
git merge --no-ff feature/vX.Y -m "Merge feature/vX.Y"
git status --short
git log --oneline --decorate --graph -20
```

Tag nằm trên commit release của branch và commit đó phải là ancestor của `main` sau merge.

## 5.1. Immutable artifact và backup

Sau mỗi build thành công và sau mỗi release tag, chạy `scripts/archive-release.ps1` với event id duy nhất:

```powershell
# Ví dụ build thành công
.\scripts\archive-release.ps1 -Version 4.8.0 -EventId build-20260717-210000 -GitRef HEAD ...

# Ví dụ tag release
.\scripts\archive-release.ps1 -Version 4.8.0 -EventId tag-v4.8.0 -GitRef v4.8.0 ...
```

Mỗi lần chạy phải tạo hai bản giống nhau và bất biến:

```text
artifacts/releases/vX.Y.Z/<event>/
backup/vX.Y.Z/<event>/
```

Script phải dừng nếu một trong hai thư mục đích đã tồn tại. Không xóa, ghi đè hoặc tái sử dụng backup cũ.

Mỗi payload bắt buộc có:

- APK;
- `SHA256SUMS.txt`;
- QA report;
- `CHANGELOG.md`;
- `BUILD_STATE.md`;
- `RELEASE_NOTES.md`;
- Perfetto trace/report;
- Macrobenchmark output/report;
- screenshots;
- video;
- `project_source_vX.Y.Z.zip` tạo bằng `git archive` từ đúng Git ref/tag.

`build/` và `app/build/` chỉ là đầu vào tạm thời. Phải archive APK và bằng chứng quan trọng ngay sau build thành công, trước bất kỳ lần `gradlew clean` tiếp theo.

## 6. Điều kiện hoàn tất

Một phiên bản chỉ hoàn tất khi:

- branch đúng mẫu và xuất phát từ `main` sạch;
- mỗi nhóm sửa độc lập có commit rõ ràng;
- không có commit rỗng;
- regression đã được chạy và ghi nhận;
- APK cùng checksum đã được xác nhận;
- tag annotated `vX.Y` tồn tại;
- branch được merge `--no-ff` vào `main`;
- working tree cuối cùng sạch.

## 7. Workspace Snapshot

Đọc `WORKSPACE_SNAPSHOT.md` khi bắt đầu hoặc tiếp tục công việc. Cập nhật file ngay khi xảy ra điều kiện đầu tiên:

- khoảng 30 phút làm việc;
- khoảng 10 commit kể từ snapshot gần nhất;
- hoàn thành một nhóm chức năng độc lập;
- trạng thái regression, build, tag, merge, blocker hoặc known bug thay đổi;
- chuẩn bị dừng hoặc bàn giao phiên làm việc.

Snapshot phải ghi đủ version, branch, commit baseline, build, completed tasks, pending tasks, known bugs, regression status và next step. Snapshot là một thay đổi tài liệu thực; chỉ commit khi nội dung đã thay đổi và không bao giờ tạo commit rỗng.
