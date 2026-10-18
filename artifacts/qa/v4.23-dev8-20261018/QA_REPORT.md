# v4.23 — bản dev `4.23-dev.8` (code 283): nút hành động không bị cắt chữ

Ngày 2026-10-18, nhánh `feature/v4.23`. Emulator, provider giả. Điện thoại `15e84958` đang chạy `4.23-dev.7`; dev.8 chưa cài.

## 1. Lỗi và sửa
Owner báo nút "Retry failed chunks" ở màn Dịch bị cắt dòng thứ hai khi chữ lớn. Nguyên nhân: các hàng nút ở `TranslatePageFactory` dùng chiều cao cố định 40–48 dp. Sửa: mọi nút trọng số trong file đó dùng `wrap_content` (15 chỗ: Start/Pause/Resume/Cancel, Prompt preview, Retry failed chunks, Chọn file output/thư mục, Xem/Chọn/Thay đổi/Bỏ chọn, Language profile, Export Log). Thêm test thiết bị `v_actionButtonsGrowWithTheirLabel` (chiều cao các nút Start, Pause, Resume, Cancel, Retry = `WRAP_CONTENT`).

## 2. Bản dev
| Mục | Giá trị |
|---|---|
| Bản | `4.23-dev.8` / code 283, event `build-20261018-230733`, source `c69fa011` |
| APK | `D:\App Translate Books\artifacts\builds\v4.23-dev.8\build-20261018-230733\TranslateBooks-v4.23-dev.8-code283.apk`, SHA-256 `5B2C46497FAE1AE98EE63757CD548A29E294D36986310B2E2F77BD9D92C932D6`, chứng chỉ `47F31389…EF3C155` |
| Source ZIP | `4FA1A29A…C0B3` |
| APK test | `v423-dev8-test-20261018`, `29FDD18E…8A283` |

## 3. Hồi quy archive sạch (`c69fa011`)
Engine 742 / 1 SKIP; app debug, release, benchmark 570 mỗi biến thể / 5 SKIP; 0 FAIL; lint 0 lỗi / 65 cảnh báo; AndroidTest compile PASS. Editorial: `git diff 5d81061f -- editorial-engine 'app/.../Editorial*'` rỗng.

## 4. QA thiết bị (emulator-5554)
205 phát hiện / **189 PASS / 16 SKIP / 0 FAIL** / 0 INCOMPLETE (frozen 117/116/1, chunk 27/21/6, product 15/15/0, v4.22 13/6/7, v4.23 18/16/2, restart 15/15/0). `V423ResumeInstrumentedTest` 8/8.

## 5. Ảnh (`media/`, đã xem)
`38-font13-pending-card-and-buttons` (cỡ chữ 1.3: "Retry failed chunks" hiện đủ hai dòng, thẻ "Job đang dở" và ba nút đủ chữ), `39-start-question`, `40-state-after-retry`. Cỡ chữ emulator đã trả về 1.0.

## 6. Giới hạn
- Ở chữ lớn, hai nút cạnh nhau có thể cao khác nhau (nút ngắn chữ thấp hơn nút hai dòng): chỉ khác thẩm mỹ, không mất chữ.
- Khối "Current job" ở tab Jobs vẫn hiện job gần cập nhật nhất (kể cả `NEEDS_REVIEW`); chưa sửa.
- Chỉ trên emulator; trên điện thoại là NOT_MEASURED tới khi owner duyệt cài.
