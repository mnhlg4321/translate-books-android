# Translate Books — Thiết kế Refined Minimalism

**Trạng thái:** ĐÃ PHẢN BIỆN (agent phản biện đọc mã `feature/v4.23`, 2026-10-21; mọi khẳng định đã đối chiếu mã).

**Nguồn:**
- Chỉ thị "TRANSLATE BOOKS — REFINED UI/UX DIRECTIVE" của owner (2026-10-21).
- Skill `ui-ux-pro-max`: `--design-system`, các tìm kiếm `color` / `ux` / `web`, và `references/pro-rules.md`.

**Phạm vi:** chỉ giao diện. Không đổi business logic, pipeline API, chunking, retry, resume, output, lưu tệp hay chuỗi trạng thái đã lưu. Không sửa mã `Editorial*`.

## 1. Kết quả từ skill (đã lọc)

- **Phong cách:** Minimalism & Swiss Style. Có chế độ sáng và tối; cần tương phản 4.5:1, focus nhìn thấy được, giảm chuyển động.
- **Bỏ phần không hợp:**
  - pattern landing page;
  - font web (Playfair/Inter): app native, chữ tiếng Việt có dấu nên dùng font hệ thống;
  - bảng màu teal + cam: chỉ thị yêu cầu một màu nhấn.
- **Quy tắc dùng được:**
  - vùng chạm 48 dp; khoảng cách giữa hai vùng chạm ≥ 8 dp;
  - trạng thái phải có icon hoặc chữ, không chỉ dựa vào màu;
  - giãn dòng 1.5–1.75; mỗi dòng 65–75 ký tự;
  - không dùng emoji làm icon cấu trúc;
  - khoảng cách theo nhịp 4/8 dp;
  - divider phải thấy được ở cả hai theme;
  - kiểm tương phản theme tối riêng.

## 2. Audit hiện tại (file:dòng ở `app/src/main/java/com/ml/tblandroidtxt`)

| Vấn đề | Bằng chứng |
|---|---|
| Chữ nhỏ | Ngoài Biên tập có ~140 lần gọi `text(…)` với cỡ cố định; ~91 lần dưới 14sp (2×10, 12×11, ~42×12, ~35×13). Cỡ mặc định của các helper: `tinyButton` 11, `chip` 12, `check` 13, `button` 13, `input` 14 (`MainActivity:3733–3838`). Thanh nav 10–11sp và **không** theo `fontScale` (`:795`) |
| Thẻ lồng thẻ, nhiều viền | `sectionCard` = `card(18, PANEL, BORDER)` có viền. Activity Log lồng một `card(16, FIELD, BORDER)` bên trong (`TranslatePageFactory:465–495`). Mỗi ô số liệu cũng là một thẻ có viền (`:455`) |
| Viết hoa toàn bộ (phần hiển thị) | Nhãn `fieldBlock` 11sp, giãn chữ 0.12, viết hoa (`MainActivity:3776`, `SettingsPageFactory:140`, ví dụ "SOURCE LANGUAGE"). Chip "ACTIVE" (`PronounPageFactory:36`). Chip trạng thái thô `RUNNING/READY` (`TranslatePageFactory:280`). `topStatusChip` "IDLE" 11sp (`MainActivity:765`). Riêng `COMPLETED` trong `JobStatus`/`RunPanelFormatter` là chuỗi so sánh, **không phải hiển thị**: không sửa |
| Icon | Emoji ở tiêu đề `sectionCard` (📁📄📖📚🧾🪵). Nav dùng ký tự ↗◷✎▤⚙ (`primaryIcon`, `:811`) |
| Ngôn ngữ trộn | Nhãn nav Translate/Jobs/Library/Settings (`:786`). "Create / choose output TXT" (`FilesPageFactory:65`). "Health check", "Use/Edit/Del" (`GlossaryPageFactory:75,128–132`) |
| Trạng thái lặp | Chip trạng thái trên đầu trang và chip trong bảng tiến trình |
| Nút ngang hàng | Glossary/Pronoun có 3 nút trên mỗi thẻ, khoảng cách `space(6)` (< 8 dp) |

**Đã có sẵn:**
- 15 palette tối, cộng Light và Paper, cộng `accentAlt`.
- Chế độ theo hệ thống với `palette`/`paletteLight` riêng.
- Kiểm tương phản cho màu nhấn tự chọn (`AppearanceSettings:10–88`).
- Settings đã là accordion chỉ mở một mục mỗi lần (`SettingsPageFactory:34–49`).

## 3. Hệ thống thiết kế

### 3.1 Nguyên tắc triển khai (theo phản biện)

- **Không đổi nghĩa của `a.text(sp)`.** Biên tập gọi hàm này 61 lần, 79/95 cỡ cố định của Biên tập dưới 14sp; ép cỡ sẽ làm vỡ bảng dày đặc của Biên tập.
- Thêm **helper theo vai trò**: `body()`, `support()`, `caption()`, `label()`, `sectionTitle()`, `statusChip()`, `icon()`. Chuyển từng màn **ngoài Biên tập** sang các helper này.
- Giữ tên token cũ (`BORDER`, `FIELD`, `PANEL`, `MUTED`…) làm **alias**, vì Biên tập đọc trực tiếp các tên này.
- Chỉ đổi helper dùng chung khi Biên tập vẫn ổn sau thay đổi (đệm và màu của nút, chip, ô nhập). Mỗi lần đổi chụp ảnh Biên tập trước/sau.
- Không dùng cờ `classic|refined`. Mỗi bước là commit riêng, hoàn nguyên bằng git.

### 3.2 Token màu — theme "Trung tính" mới (mặc định đề xuất)

| Token | Light | Dark | Paper |
|---|---|---|---|
| `bg` | `#ECEEF1` | `#111214` | `#EFE8D9` |
| `surface` (nhóm) | `#FFFFFF` (1.16:1 so với bg) | `#1D1F22` (1.13:1) | `#FBF8F1` (1.15:1) |
| `raised` (ô nhập, hàng đang chọn) | `#F1F3F5` | `#272A2E` | `#F3EDE1` |
| `text` | `#1D2125` (≥ 13.9:1) | `#E6E7E9` (≥ 11.6:1) | `#2B2620` (≥ 12.3:1) |
| `text2` | `#4D5460` (≥ 6.5:1) | `#A9AFB8` (≥ 6.5:1) | `#5A5144` (≥ 6.4:1) |
| `divider` (hairline trang trí) | `#D5DAE0` | `#3A3D43` | `#DDD3BF` |
| `outline` (viền ô nhập, nút phụ ≥ 3:1) | `#7D8590` (≥ 3.35:1) | `#7A808A` (≥ 3.6:1) | `#8C8170` (≥ 3.28:1) |
| `accent` mặc định **Graphite** | `#22262B` | `#E6E7E9` | `#3A332B` |
| `onAccent` | `#FFFFFF` (15.2:1) | `#16181B` (14.4:1) | `#FBF8F1` (11.7:1) |
| `ok` / `warn` / `err` | `#146C2E` / `#8A5A00` / `#C62828` | `#4ADE80` / `#FBBF24` / `#F87171` | `#2E6B2F` / `#7A5200` / `#A3261F` |

Tỉ lệ ghi là giá trị thấp nhất trên `bg`, `surface` và `raised`. Mọi chữ thường ≥ 4.5:1; viền điều khiển ≥ 3:1.

- **Màu nhấn có thể chọn:** Graphite (mặc định), Teal `#0F766E`/`#2DD4BF`, Ocean, Rose…, hoặc mã hex. Màu nào cũng qua bộ kiểm `accentError` hiện có, so với các bề mặt mới.
- **15 palette cũ giữ nguyên** dưới tên "Cổ điển". Không tự chuyển palette owner đã chọn. Owner chuyển sang "Trung tính" bằng một lần chọn trong Giao diện.
- Trạng thái luôn có icon (✓ / ! / ×, vector) và chữ tiếng Việt.

### 3.3 Chữ (font hệ thống; nhân với cỡ chữ app và cỡ chữ hệ thống)

| Vai trò | sp / giãn dòng | Đậm | Dùng cho |
|---|---|---|---|
| `title` | 22 / 28 | Medium | tiêu đề màn |
| `section` | 17 / 24 | Medium | tiêu đề nhóm, không emoji |
| `body` | **16 / 24** | Regular | nội dung, tên tệp |
| `support` | **14 / 20** | Regular | metadata, giải thích |
| `label` | 14 / 20 | Medium | nhãn trường, nhãn nút; dạng câu, không giãn chữ |
| `caption` | 13 / 18 | Regular | chỉ số liệu phụ trong ô thống kê và nhãn nav |
| `reading` | 17 / 28 | Regular | khung văn bản dài (preview); dòng tối đa ~70 ký tự trên tablet |

### 3.4 Hình khối và khoảng cách

- Lề 16 dp; giữa các nhóm 24 dp; giữa các hàng 12–16 dp; giữa các vùng chạm ≥ 8 dp.
- **Nhóm** = `surface`, bo 16 dp, **một viền hairline `divider`**, không bóng. Bên trong: hàng ngăn bằng divider, không lồng thẻ.
- **Thẻ** chỉ cho đối tượng độc lập: một job, một lượt chạy.
- **Nút:**
  - Chính: `accent`/`onAccent`, cao 48 dp, mỗi vùng chỉ một nút chính.
  - Phụ: viền `outline`, chữ `text`.
  - Văn bản: chữ `accent`.
  - Nguy hiểm: chữ `err`, nằm trong menu hoặc hộp xác nhận.
- **Ô nhập:** nền `raised`, viền `outline`, nhãn ở trên, lỗi ngay dưới ô.
- **Chip trạng thái:** icon + chữ, nền `raised`, không viền màu.

### 3.5 Icon

- Material Symbols (Apache 2.0, thêm mục NOTICE), khoảng 20 tệp `VectorDrawable` XML.
- Helper `icon(id, color)` dùng `mutate().setTint(token)`.
- Nút chỉ có icon phải có `contentDescription`.

### 3.6 Chuyển động

- Chỉ ripple 100–150 ms và mở/thu nhóm 150–200 ms.
- Tôn trọng tắt animation hệ thống.

### 3.7 Phản hồi

- "Đã lưu": một dòng nhỏ cạnh trường vừa đổi, tự ẩn, thay cho nhiều toast liên tiếp.
- Lỗi nhập hiện ngay dưới ô.

## 4. Theo màn hình

- **Dịch:**
  - Bốn nhóm: Nguồn, Cấu hình, Xác nhận, Bắt đầu. Mỗi nhóm luôn hiện dòng tóm tắt, bấm vào để mở. Cảnh báo không bao giờ bị thu gọn.
  - Thông số chuyên sâu nằm trong "Nâng cao".
  - Khi đang chạy: bảng tiến trình J3 dạng phẳng, ô số chia bằng divider. Ô cao tối thiểu 56 dp, **wrap theo nội dung**; còn 2 cột khi cỡ chữ ≥ 115% hoặc màn < 360 dp.
  - Bỏ chip trạng thái trùng ở đầu trang.
  - Activity Log nằm trong "Chi tiết kỹ thuật".
- **Jobs:**
  - Dòng tổng của hôm nay ở đầu trang.
  - Mỗi lượt là một hàng: tên, thanh mảnh, "10/14 chương · 2 lỗi", chi phí, một màu trạng thái.
  - Nút hay dùng ("Tiếp tục") luôn hiện. Bỏ, Xóa, Retry nằm trong ⋮.
- **Library:**
  - Thanh phân đoạn, ô tìm, "N mục", "+ Thêm", nút "Chọn".
  - Mỗi hàng gồm: icon loại, tên (16sp, 2 dòng, cắt giữa), metadata (14sp), **nút hay dùng hiện sẵn** (ví dụ "Dùng"), và ⋮ chứa Xem, Đổi tên, Thay tệp, Xuất, Xóa.
  - Nhấn giữ để chọn nhiều vẫn giữ nguyên.
- **Biên tập:** không sửa mã. Chỉ nhận thay đổi an toàn qua helper và token; có ảnh trước/sau ở mọi palette. Nhu cầu chữ đọc lớn hơn của Biên tập ghi lại để làm riêng.
- **Settings:** giữ accordion một mục, bỏ lồng thẻ, nhãn 14sp dạng câu. Mỗi trường là một hàng "nhãn + giá trị"; trường ít dùng nằm trong "Nâng cao". Không làm trang con, để không đụng điều hướng Back và predictive back.
- **Điều hướng (cập nhật 2026-10-21):** quyết định drawer A mới nhất thay phương án nav dưới của bản thiết kế ban đầu. Giữ ☰, tối đa ba tập gần đây, tiêu đề trang hiện tại và shortcut trạng thái; không dựng lại thanh dưới/dọc trong các bước Refined.

## 5. Thứ tự triển khai

| Bước | Nội dung |
|---|---|
| R0a | • Thêm helper vai trò.<br>• Nhãn `fieldBlock` lên 14sp, dạng câu.<br>• Trạng thái hiển thị bằng tiếng Việt (chỉ đổi phần hiển thị).<br>• Bỏ trạng thái trùng.<br>• Chuyển các màn ngoài Biên tập sang `body`/`support`.<br>• Nav dịch sang tiếng Việt và theo cỡ chữ. |
| R0b | • Theme "Trung tính" với token mới.<br>• Nhóm không lồng thẻ, có hairline.<br>• Ô thống kê wrap theo nội dung.<br>• Chụp ảnh Biên tập trước/sau. |
| R1 | • Icon vector thay emoji và ký tự. |
| R2 | • Library dạng hàng, nút hay dùng hiện sẵn, menu ⋮. |
| R3 | • Dịch dạng bốn nhóm có dòng tóm tắt. |
| R4 | • Jobs dạng hàng theo lượt, dòng tổng hôm nay. |
| R5 | • Settings restyle trong accordion. |

## 6. Kiểm thử

- Cỡ chữ app 90 / 130% × cỡ chữ hệ thống 1.3 / 2.0.
- Bề rộng 320 / 360 dp, tablet, drawer ở màn ngang; không có nav dọc.
- Đủ 17 palette, màu nhấn tự chọn, chế độ theo hệ thống.
- Ảnh trước/sau của Biên tập ở cùng cài đặt.
- Trạng thái trống, đang tải, lỗi (không có RAW, lỗi provider, 0 job).
- Bàn phím che ô nhập.
- TalkBack đọc được nút chỉ có icon.
- Giảm chuyển động.
- Thước đo: số lần dùng chữ có nghĩa < 14sp ngoài Biên tập = 0; không thẻ nào lồng thẻ; mỗi vùng ≤ 1 nút chính; khoảng cách giữa hai vùng chạm ≥ 8 dp; không chức năng nào mất hoặc cần thêm hơn 1 lần chạm để tới.

## 7. Rủi ro

- Thay đổi helper dùng chung lan sang Biên tập, mà Biên tập không sửa được trong đợt này.
- Thêm theme "Trung tính" có thể khiến owner bất ngờ nếu mặc định đổi. Vì vậy chỉ đặt làm mặc định cho cài đặt mới; máy owner giữ palette đang dùng.
- Đưa thao tác vào ⋮ làm giảm khả năng phát hiện. Cách giảm: luôn hiện thao tác hay dùng nhất.

## 8. Tiếp tục thiết kế — màn Dịch đang chạy (2026-10-21)

Quyết định phục vụ: đọc được tiến bộ dịch thật, biết rõ chương đang chạy và nguồn của đoạn đang xem; giảm vùng trống và các khung thống kê chiếm diện tích. Áp dụng skill `C:/Users/ADMIN/.claude/skills/ui-ux-pro-max/SKILL.md`; query UX `progress feedback loading state` trả đúng nhóm Feedback/Progress Indicators và Loading Indicators. Dùng kết quả cho tiến trình có bước rõ ràng, bố cục ổn định và thông báo chờ có nghĩa; native Java Views hiện có, không đổi stack.

**Đã triển khai trong dev.20:** preview giữ qua chương; dòng nguồn riêng “Đã nhận chunk 5/5 · 001.txt” khi chương hiện tại là002/chunk1/2; chữ16sp, metadata/toggle14sp, giãn dòng1.4; thông báo chờ tiếng Việt; giữ chọn chữ/cuộn/thu gọn. Ảnh tổng hợp thật và giới hạn ở báo cáo QA preview. Không đổi màu/theme owner đang dùng.

**Bố cục lịch sử trước yêu cầu owner về tên đầy đủ/đơn vị (được thay bởi mục9 dưới đây):**

```text
☰  Dịch                           [Đang dịch]

Tập KUMO_JP_VOL12
Chương 3/23 · 2 xong · 0 lỗi
────────────── thanh tiến độ tập ──────────────
003_KUMO_JP_VOL12.txt
Đang dịch chunk 1/2
────────────── thanh tiến độ chương ───────────

8.4k token       $0.0042       00:51 đã chạy
~08:57 còn lại   G14 · P9      Chi tiết ▸

Bản dịch gần nhất                       [Thu gọn]
Đã nhận chunk 2/2 · 002_KUMO_JP_VOL12.txt
Nội dung đọc được, có thể cuộn và chọn chữ…

[Tạm dừng]                 [Dừng…]
```

- Một bề mặt tiến trình, dùng khoảng cách và divider giữa nhóm; không thêm viền riêng cho từng số. Token/chi phí/thời gian theo lượt, tên tệp/chunk theo chương; nguồn preview thuộc chính chunk được nhận. Không suy ra “xong chương3” từ việc đang gọi chunk1.
- “Chi tiết” chứa lỗi/fallback và số liệu kỹ thuật; cảnh báo chặn, không rõ đã gửi và chạm trần chi phí luôn hiện, không giấu trong accordion. Không đưa nội dung chẩn đoán lập trình vào luồng người dùng.
- Chỉ hiện hành động có tác dụng: đang chạy → Tạm dừng; đã tạm dừng → Tiếp tục; Dừng cần xác nhận vì ảnh hưởng job. Không để Start vô hiệu chiếm hàng hành động lúc đang chạy. Giữ entry/ủy quyền và hành vi hiện tại, không thay service khi làm nhóm giao diện này.
- Preview lần đầu: thông báo rõ chưa có chunk được chấp nhận trong **lượt này**. Giữa hai chương: giữ nội dung trước với đúng tên nguồn. Lỗi chunk: giữ nội dung tốt trước đó và cảnh báo riêng. Lượt mới: xóa preview cũ, không để dữ liệu lượt trước trông như tiến bộ mới.
- Cỡ chữ lớn/màn hẹp: thống kê từ ba xuống hai cột, tên tệp được xuống dòng, nút cao tối thiểu48dp; không ép cỡ chữ để nhét. Dùng token Paper/Light/Dark hiện có, kiểm ảnh riêng ở130%; TalkBack vẫn cần đo thực tế.

Triển khai tiếp trong nhóm R0b/R3 hiện có sau nghiệm thu bản sửa, không tạo nhánh/gói/cổng mới. Tiêu chí: vẫn đọc được nguồn preview, hành động và cảnh báo ở màn hẹp/cỡ chữ lớn; không thêm lần chạm để tạm dừng/tiếp tục, không mất thao tác hoặc thay đổi nội dung đầu ra.

## 9. Owner yêu cầu dễ hiểu và ưu tiên vùng đọc — cập nhật 2026-10-22

Phục vụ quyết định: theo dõi dịch mà không phải giải mã nhãn. Đã dùng skill Claude `C:/Users/ADMIN/.claude/skills/ui-ux-pro-max/SKILL.md`, quy tắc Color Only, Chip Collection Reflow, đọc trên mobile và vùng chạm. Luna làm audit/diff review độc lập; coordinator kiểm lại mã và ảnh thật. Native Java Views, giữ palette và cỡ chữ owner.

Đã triển khai trong **dev.21/code299, production source4849de19**:

- Thay sáu ô thống kê lồng thẻ bằng hai cột nhãn/giá trị phẳng; dưới340dp, app font từ115% hoặc system font từ150% chuyển một cột. Chữ số16sp, nhãn14sp, wrap theo nội dung, không tự thu nhỏ.
- `91.100 token`, `5 phút 38 s`, `Còn lại (ước tính)`, `0 chunk lỗi`, `Glossaries: 9 luật · Pronoun: 5 luật`. `s` giữ theo yêu cầu owner. Thiếu phép đo luật hiện “chưa đo”. `fb` trước là fallback; service chỉ reset/persist/broadcast mà không tăng, nên bỏ khỏi phần hiển thị thay vì đổi tên một số chưa được đo.
- Chi phí/token cả lượt và chi phí/token chương đang dịch có nhãn riêng. File hiện tại không lặp trên dòng chunk. Không thay cách tính hoặc dữ liệu dịch.
- Shortcut trên cùng luôn có chữ trạng thái, không chỉ chấm màu; vẫn mở Công việc. Màn hẹp/chữ lớn cho trạng thái một hàng riêng. Nút từ48dp và có mô tả truy cập.
- Preview mặc định mở; gộp tên vùng và chunk thành một dòng có thể wrap, nguồn tệp dòng sau; bỏ tiêu đề/accepted metadata lặp. Chữ nội dung16sp, chọn chữ và cuộn được. Nội dung ngắn co theo nội dung; nội dung dài có viewport45% chiều cao màn, chặn trong160–480dp thay150dp cố định. Có nút Ẩn/Xem. Không mở rộng giới hạn nội dung preview1200 ký tự hiện có.
- Bắt đầu/Tạm dừng/Tiếp tục/Dừng dịch/Dịch lại chunk lỗi dùng tiếng Việt; giữ handler, xác nhận và quyền chi hiện có.

Kết quả và ảnh thật: `artifacts/qa/v4.23-readability-20261022/QA_REPORT.md`. Chưa coi R0a–R5 hoàn tất: các đề xuất đưa log vào Chi tiết kỹ thuật, bớt nút vô hiệu và đổi toàn bộ icon vẫn còn. Mục4/5 là định hướng rộng; bố cục đang chạy ở mục9 thay các thông số cột/nhãn cũ trong mục4/8.

### Phản biện các màn còn lại, theo bằng chứng mã

| Ưu tiên / màn | Vấn đề và nguyên nhân | Hướng xử lý trong nhóm hiện có | Ownership / kiểm / điểm dừng |
|---|---|---|---|
| R3 Dịch cấu hình | `TranslatePageFactory:62,64,72,104,138` trộn SOURCE/TARGET LANGUAGE, Prompt preview, Refresh model pricing; `:297` còn hướng dẫn UI2.9 đã cũ. Log kỹ thuật chiếm vùng luồng chính (`:435`). | Nhãn tiếng Việt theo tác vụ, tóm tắt cấu hình, gom chẩn đoán vào Chi tiết kỹ thuật; cảnh báo chặn luôn hiện. Không đổi prompt/model/service. | UI factory + test; kiểm cùng lựa chọn trước/sau, refresh giá không tự gọi model, Start/Pause/Resume giữ số lần chạm. Dừng nếu cần thay preparation hoặc engine. |
| R2 Glossaries/Pronoun | `GlossaryPageFactory:128–132` Using/Use/Edit/Del, cao42dp; editor`:176–197` nút44dp và nhãn Anh. Tên khó hiểu và cắt chữ cùng gốc ép chiều cao. | Dùng/Đang dùng/Sửa/Xóa; hàng tự cao, min48dp; thao tác hay dùng luôn hiện, nguy hiểm có xác nhận. RAW là mẫu ngôn ngữ dễ hiểu hơn. | Glossary/Pronoun factory, không schema/store; kiểm active selection, sửa/lưu/xóa/undo,320dp và font130%. Không giấu thao tác chỉ để giảm nút. |
| R4 Công việc | Nhiều hành động và dữ liệu chẩn đoán cạnh nội dung người dùng; còn nhãn nội bộ. | Hàng tên–tiến độ–trạng thái bằng chữ, Tiếp tục hiện sẵn; lỗi rõ nguyên nhân và hành động; chi tiết kỹ thuật mở riêng. | Jobs UI; giữ lọc hai tập/job tệp lẻ, route/back và resume; provider thật chỉ khi có spend riêng. |
| R5 Cài đặt | Các nhóm có nhiều thông số/chữ kỹ thuật và thẻ lồng; chưa audit bằng ảnh mọi trường. | Giữ accordion hiện có, nhãn+giá trị, giải thích ngắn và Nâng cao. | Settings UI; kiểm lưu/mở lại và chế độ theo hệ thống; không đổi giá trị mặc định owner. |

Không mở rộng sang Biên tập trong nhóm này. Không chuyển sang trả toàn văn hoặc đổi schema: lỗi hiện tại ở trình bày, không phải transport/prompt/parser/ngữ nghĩa. Hash, số đếm, trạng thái vẫn do app quản lý. Các fixture tổng hợp chỉ chứng minh trình bày/lifecycle, không chứng minh chất lượng dịch. TalkBack, tất cả17 palette và luồng bàn phím toàn app chưa đo đầy đủ.

## 10. Gói việc tiếp theo — R3a + R2 và thử dev.22 trên điện thoại (2026-10-22)

**Phục vụ quyết định:** owner dùng màn Dịch và Glossary/Pronoun bằng tiếng Việt nhất quán, không có nút dưới 48 dp, và nghiệm thu được trên điện thoại thật. Owner yêu cầu ngày 2026-10-21: thử trực tiếp trên máy, không qua emulator.

### 10.1 Đã làm ở phiên điều phối (commit `fix(ui): keep the target language visible…`, chưa build)

| Lỗi owner nêu | Nguyên nhân đã chứng minh | Sửa | Kiểm |
|---|---|---|---|
| Màn hẹp cắt mất "Vietnamese" ở dòng phụ trên cùng | `MainActivity.providerMeta` ghép theo thứ tự `provider • nguồn → đích`, và `topSubtitle.setSingleLine(true)`. Vì vậy phần bị cắt luôn là ngôn ngữ đích (ảnh `screenshots-clean/run-panel-paper-100-360x800.png`, `…-130-320x800.png`) | Đổi thứ tự thành `nguồn → đích • provider` (`providerMetaLine`); dòng phụ tối đa 2 dòng | `ProviderMetaLineTest`. Ảnh trên điện thoại: NOT_MEASURED |
| Nhãn "Chương" chạm thanh tiến độ ở 320 dp, app 130% + hệ thống 130% | `TranslatePageFactory.barRow` đặt bề rộng nhãn là `dp(52) × cỡ chữ app`. Còn chữ thì là `sp × cỡ chữ app`, tức nhân thêm cỡ chữ hệ thống | Bề rộng nhãn = `60 sp × cỡ chữ app` + 8 dp đệm; dòng dưới thụt đúng bằng cột nhãn | Toàn app 674 test: 669 PASS, 5 SKIP. Ảnh: NOT_MEASURED |
| Cặp ảnh `run-panel-*`/`preview-area-*` trùng SHA | Owner xem `screenshots-final/`, là bộ chụp lỗi trước khi chờ compositor. Bộ bàn giao là `screenshots-clean/` | Ghi rõ trong `QA_REPORT.md` thư mục nào là bằng chứng | 10 ảnh `screenshots-clean` có SHA khác nhau, PNG hợp lệ |

### 10.2 Thuật ngữ (khuyến nghị của điều phối; owner có thể đổi)

- **Giữ "chunk"** (chữ thường trong câu tiếng Việt) cho phần dịch. Lý do:
  - owner dùng từ này hằng ngày;
  - "đoạn" đã là thuật ngữ của Biên tập (`EditorialJobPresentation:15`, `EditorialPairPageFactory:130`) và chỉ một khái niệm khác. Dùng chung chữ sẽ gây nhầm.
- **Giữ tên riêng:** Glossary/Glossaries, Pronoun, prompt, token, tên provider, tên ngôn ngữ.
- **Việt hóa nhãn thao tác:**

| Hiện tại | Đổi thành |
|---|---|
| SOURCE LANGUAGE / TARGET LANGUAGE | Ngôn ngữ nguồn / Ngôn ngữ đích |
| Prompt preview | Xem trước prompt |
| Refresh model pricing | Cập nhật giá model |
| Activity Log, Clear Log, Export Log | Nhật ký, Xóa nhật ký, Xuất nhật ký |
| Using / Use / Edit / Del | Đang dùng / Dùng / Sửa / Xóa |
| Close (hộp thoại phía dịch) | Đóng |

### 10.3 Việc làm

1. **R3a — màn Dịch, phần cấu hình** (`TranslatePageFactory:62,64,72,104,138,297,435`):
   - đổi nhãn theo 10.2;
   - bỏ dòng hướng dẫn UI 2.9 đã cũ;
   - đưa thẻ Nhật ký vào mục "Chi tiết kỹ thuật" thu gọn, mặc định đóng. Cảnh báo chặn chạy vẫn luôn hiện.
   - Không đổi handler, prompt, model, service, preparation.
2. **R2 — Glossary/Pronoun** (`GlossaryPageFactory:128–132,176–197`; `PronounPageFactory` tương ứng):
   - đổi nhãn theo 10.2;
   - hàng tự cao theo nội dung, mọi nút ≥ 48 dp;
   - Xóa vẫn có xác nhận;
   - không đổi store/schema, không giấu thao tác.
3. **Không làm** trong gói này: R4/R5, Biên tập, đổi icon toàn app.

**Phạm vi được sửa:** chỉ `TranslatePageFactory`, `GlossaryPageFactory`, `PronounPageFactory`, các chuỗi gắn ở `MainActivity`, và test của chúng. Không chạm `Editorial*`, `editorial-engine/`, `TranslatorService`, `TranslationEngine`, store, schema.

### 10.4 Test và tiêu chí

**Test host:**
- test formatter và nhãn mới;
- `rg` các chuỗi cũ trong bảng 10.2 trên các factory thuộc gói trả 0 dòng (trừ tên riêng);
- test cũ có assert chuỗi thì sửa trong cùng commit.

**Điện thoại 15e84958**, theo đúng mục 3 của `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md`:
- sao lưu trước, chờ owner trả lời "duyệt cài", chỉ cài bằng `install -r`;
- không bấm nút tốn tiền;
- chỉ dùng dữ liệu `QA_`;
- chụp ảnh bằng `shell screencap` rồi `pull`.

| # | Kiểm | PASS khi |
|---|---|---|
| D1 | Dòng phụ trên cùng ở cỡ chữ app 100% và 130% (Cài đặt > Appearance) | thấy đủ "Japanese → Vietnamese"; provider có thể xuống dòng hoặc bị cắt |
| D2 | Màn Dịch đang dừng/tạm dừng, cỡ chữ 130% | "Tập"/"Chương" không chạm thanh tiến độ |
| D3 | Màn Dịch phần cấu hình, Glossary, Pronoun | không còn nhãn nào trong bảng 10.2; mọi nút cao ≥ 48 dp × mật độ (đo bằng `uiautomator dump`) |
| D4 | Nhật ký | mặc định thu gọn; mở ra vẫn đủ log; Xóa/Xuất hoạt động |
| D5 | Glossary `QA_`: Dùng → Sửa → Lưu → Xóa (có xác nhận) | trạng thái "Đang dùng" đúng; không mất Glossary nào của owner |
| D6 | TalkBack đọc thanh trên, ngăn ☰, màn Dịch | **chỉ khi owner cho phép bật TalkBack** trong terminal, và tắt lại ngay sau đó; nếu không thì ghi NOT_MEASURED |

**Điểm dừng:**
1. hồi quy archive sạch;
2. build `4.23-dev.22` (code > 299);
3. sao lưu điện thoại, **dừng chờ owner duyệt cài**;
4. cài, chạy D1–D6, rồi **dừng** và báo cáo.

**Bằng chứng phải giữ:**
- ảnh PNG hợp lệ; mỗi ảnh một trạng thái, SHA khác nhau; manifest ghi ảnh nào cho mục nào;
- XML `uiautomator` cho D3;
- đường dẫn sao lưu.

**Không đo trong gói này:** chất lượng dịch, A2/A4 đang dịch thật (cần quyền chi tiền riêng).
