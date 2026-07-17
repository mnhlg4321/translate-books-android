# Git Workflow

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

## 1. Bắt đầu phiên bản

Chỉ bắt đầu khi working tree trên `main` sạch:

```powershell
git switch main
git status --short
git switch -c feature/vX.Y
```

Mỗi branch phiên bản dùng đúng mẫu `feature/vX.Y`, ví dụ `feature/v4.8`.

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
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

Khi có thiết bị Android phù hợp, chạy thêm:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Ghi command, môi trường, số test pass/fail/skip, lint, artifact và known issues vào `QA_REPORT_vX_Y.md` hoặc báo cáo release tương ứng. Commit báo cáo bằng message `test(regression): record vX.Y regression results` nếu báo cáo có thay đổi. Nếu kết quả không làm thay đổi file nào thì không tạo commit rỗng.

Không tiếp tục phát hành khi regression fail, trừ khi lỗi được ghi nhận rõ và có quyết định chấp nhận release.

## 4. APK và metadata release

Tạo APK sau khi regression đạt yêu cầu:

```powershell
.\gradlew.bat assembleDebug
```

Xác nhận version, tên APK, kích thước và SHA-256. Cập nhật riêng các file sau nếu thông tin thay đổi:

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

