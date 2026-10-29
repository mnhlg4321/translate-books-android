# Kế hoạch phát hành v4.24.0 (điện thoại + PC)

- Quyết định sản phẩm: owner muốn dùng app trên điện thoại và trên PC song song, cùng một phiên bản. Bản đang phát hành vẫn là v4.21.0 (tag `v4.21.0`, có trên `main` và GitHub). Ba nhánh sau đó chưa có tag nào: v4.22, v4.23 và v4.24.
- Lập ngày 2026-10-29, gốc `ca47a000` trên `feature/v4.24`.

## 1. Hiện trạng (đã kiểm)

| Nhánh | So với nhánh trước | Trạng thái | Nằm trong v4.24? |
|---|---|---|---|
| `main` | v4.21.0 | đã phát hành, đã push | — |
| `feature/v4.22` | +27 commit | checklist `IN_PROGRESS`, bản cuối `4.22-dev.3` | có |
| `feature/v4.23` | +158 commit | checklist `IN_PROGRESS`; Thư viện RAW, Tập, dịch theo chương, polish B1–B3 (dev.26) | có |
| `feature/v4.24` | +90 commit | lõi chung + app PC (P0a–P4e); điện thoại chạy `4.24-dev.5`/code 309, PC có gói `4.24-pc-dev.9` | — |

- `feature/v4.24` đã chứa trọn v4.22 và v4.23 (kiểm bằng `merge-base --is-ancestor`).
- Từ ngày 2026-10-28, điện thoại của owner chạy bản v4.24, tức đã gồm cả v4.22 và v4.23.

## 2. Đề xuất: phát hành gộp một lần, `v4.24.0`

- Tag `v4.24.0` trên `feature/v4.24`. Đóng checklist v4.22 và v4.23 với trạng thái `SUPERSEDED` (gộp vào v4.24.0) và không tag riêng hai bản này.
- Lý do:
  - Owner không còn dùng bản v4.23 riêng.
  - Tag v4.23 trước thì phải làm đủ 14 bước và bộ bằng chứng phát hành (Perfetto, Macrobenchmark, video) cho một bản không ai cài.
  - Điện thoại và PC cần cùng một commit thì mới "cùng phiên bản" thật.
- Phương án khác: tag v4.23.0 trước rồi mới v4.24.0. Tốn gấp đôi công kiểm phát hành mà không thêm giá trị sử dụng nào. Không khuyến nghị.
- Tên phiên bản:
  - điện thoại `4.24.0` (versionCode mới, lớn hơn 309);
  - PC `4.24.0`, cùng commit, tên gói `TranslateBooks-PC-4.24.0`;
  - một tag `v4.24.0` dùng chung cho cả hai.

## 3. Các bước (theo `DEVELOPMENT_WORKFLOW.md`, checklist `release_checklists/v4.24-pc-shared-core.md`)

| Bước | Việc | Ai | Tiêu chí đạt | Tiền |
|---|---|---|---|---|
| R0 | **Owner dùng thật.** Cài PC `dev.9`. Xuất hồ sơ và thư viện từ điện thoại, nhập vào PC. Dịch 1 chương sách thật trên PC; dùng điện thoại `dev.5` như thường ngày. | owner; người điều phối kiểm số đếm | Số RAW/Glossary/Pronoun trên PC bằng điện thoại. Job PC ở trạng thái COMPLETED. Owner liệt kê lỗi (nếu có). | Lượt dịch của owner tính vào key của owner, do owner tự bấm trong app. Người điều phối và session phụ không gọi provider. |
| R1 | **Sửa lỗi R0** (nếu có), chỉ sửa lỗi, không thêm tính năng. Chốt phạm vi. | session phụ | Mỗi lỗi có test hồi quy; bộ kiểm sạch đạt. | 0 |
| R2 | **Bản ứng viên (RC):** APK bằng `scripts/build-and-save.ps1` và gói PC bằng `scripts/build-desktop.ps1 -Msi`, từ cùng một commit. | session phụ | Cùng commit nguồn, SHA-256 ghi ở cả `artifacts/` và `backup/`. | 0 |
| R3 | **Bộ kiểm phát hành:** archive sạch (app, core, jdbc, desktop, test vàng 14/14, lint); instrumented trên emulator với provider giả; Perfetto và Macrobenchmark thật trên emulator; ảnh chụp và video (điện thoại + PC). | session phụ | Như v4.21: 0 FAIL, bằng chứng thật, không bịa số. | 0 |
| R4 | **Cài RC:** điện thoại `install -r` (sao lưu trước, owner gõ "duyệt cài" trong cửa sổ session); PC cài `.msi` đè. | owner duyệt; session phụ làm | Số đếm trước và sau khi cài bằng nhau; không có crash. | 0 |
| R5 | **Owner ký duyệt phát hành.** | owner | Owner nói "duyệt phát hành v4.24.0". | — |
| R6 | **Tag và lưu trữ:** tag `v4.24.0`; `scripts/archive-release.ps1` với event `tag-v4.24.0`. Gói phát hành có thêm gói PC (zip + msi + SHA-256) cạnh APK. Cập nhật BUILD_STATE và snapshot, xuất artifact (bước 10–14). | session phụ; người điều phối kiểm | Bộ bằng chứng có đủ ở `artifacts/releases/v4.24.0/` và `backup/v4.24.0/`. | 0 |
| R7 | **Xuất bản:** merge `feature/v4.24` vào `main` rồi push nhánh và tag lên `origin`. | cần owner cấp quyền riêng | `main` = tag; trên GitHub có tag. | 0 |

## 4. Việc kỹ thuật cần thêm trước R6

- `scripts/archive-release.ps1` hiện chỉ đóng gói bằng chứng của APK. Cần nhận thêm gói PC (zip, msi, BUILD_INFO, SHA-256) mà không đổi hành vi cũ. Làm ở R2 và kiểm bằng một event thử.
- Đóng checklist v4.22 và v4.23 với trạng thái `SUPERSEDED`, ghi tham chiếu sang v4.24.0. Không xóa bằng chứng cũ.
- `PRODUCT_DIRECTION_PLAN.md` mục 10 trên `main` được cập nhật khi merge ở R7.

## 5. Giới hạn đã biết đi kèm bản phát hành

- Biên tập vẫn đóng băng trên cả hai bản; PC không có Biên tập.
- Bộ cài `.msi` chưa ký số nên SmartScreen sẽ cảnh báo. Owner dùng cá nhân thì chấp nhận được; muốn ký số thì cần mua chứng chỉ, là một quyết định khác.
- Tập (bộ chương) không đi theo gói thư viện giữa điện thoại và PC.
- Pronoun trên PC sửa dạng văn bản, chưa sửa được từng ô.
- Nâng cấp schema DB từ v1–v29 trên PC: `NOT_MEASURED`. PC chỉ dùng schema v30.
- Chất lượng dịch dùng prompt v18 như điện thoại; chưa đo lại trên sách thật dịch bằng PC.

## 6. Quyết định cần owner

1. Phát hành gộp `v4.24.0` (khuyến nghị) hay tag v4.23.0 trước.
2. Làm R0: dùng thật trên PC.
3. Sau này: quyền push và merge ở R7.

## 7. Quyết định của owner (2026-10-29)

- Owner bấm "Duyệt gộp v4.24.0". Phát hành gộp `v4.24.0` cho điện thoại và PC. Checklist v4.22 và v4.23 sẽ đóng `SUPERSEDED` ở R6 và không có tag riêng.
- R0 bắt đầu: owner dùng thật trên PC `4.24-pc-dev.9`. Quyền push và merge (R7) chưa được cấp.
