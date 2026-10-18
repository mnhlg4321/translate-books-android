# Đề xuất: Thư viện RAW, chọn hàng loạt, dịch nhiều chương mỗi chương một Glossary/Pronoun

Trạng thái: ĐỀ XUẤT đã qua phản biện (agent phản biện đọc mã `feature/v4.22` `db71bef5`, 2026-10-18). Chưa phải yêu cầu làm việc; chờ owner chọn các mục ở mục 7. Không đụng mã, không build.

**Quyết định sản phẩm phục vụ:** owner dịch nhiều chương liền, mỗi chương có Glossary/Pronoun riêng (ví dụ `013_Glossary_…csv` và `025_RAW_…txt`), mà không phải chọn lại RAW + Glossary + Pronoun cho từng chương.

## 1. Owner yêu cầu (2026-10-18, ảnh màn hình 4.22-dev.2)

| # | Yêu cầu |
|---|---|
| Y1 | Library có thêm mục RAW, nhập hàng loạt như Glossary/Pronoun |
| Y2 | Chế độ chọn nhiều mục trong Library (như "Select items" của ChatGPT) để xóa hàng loạt và các thao tác khác |
| Y3 | Tạo sẵn hàng loạt tổ hợp RAW–Glossary–Pronoun rồi chọn ở màn Dịch |

## 2. Hiện trạng trong mã (đã kiểm, file:dòng trong `app/src/main/java/com/ml/tblandroidtxt/`)

- **Dịch nhiều file đã có**, nhưng cả batch dùng **một** `AppSettings` với một Glossary/Pronoun đang Active (`TranslatorService:329,342`). Mỗi file thành một job và job lưu settings riêng (`TranslationRepository:338,360`); resume/retry đọc settings của job (`TranslatorService:435,523`), nên khi settings từng job đúng thì resume cũng đúng.
- **Một chương lỗi dừng cả batch:** lỗi chunk ném lại (`TranslatorService:679`), vòng lặp file không bắt lỗi (`:338-343`).
- **Trần chi phí tính theo từng file**, không theo cả batch (`resetMetrics` đặt `metricCost=0` mỗi file, `:350,363`).
- Glossary/Pronoun lưu thành **một chuỗi JSON lớn trong SharedPreferences**. `GlossaryStore.saveAll` dùng `apply()` bất đồng bộ (`GlossaryStore:68`). `loadAll` nuốt lỗi đọc và trả danh sách rỗng (`:57-60`): nếu chuỗi hỏng, lần lưu kế tiếp **xóa sạch mọi glossary**.
- Nhập nhiều glossary **tự đổi glossary Active** sang tệp đầu tiên (`MainActivity:1132-1138`); không kiểm trùng; tên profile = tên tệp (đổi tên là mất số chương).
- Đọc input yêu cầu quyền URI lưu bền (`FileUtil.validateReadable:322-324`), nên tệp nằm trong bộ nhớ app hiện **không đọc được** qua đường này; `displayName` của tệp nội bộ sẽ ra tên id; resume đọc lại `input_uri` và so hash (`TranslatorService:439-442`).
- Quyền URI không bao giờ được trả lại (không có `releasePersistableUriPermission`), Android giới hạn số quyền lưu bền.
- `allowBackup="true"` không có quy tắc (`AndroidManifest:9`); Auto Backup giới hạn 25 MB. Bản phát hành không `run-as` được. **App chưa có chức năng xuất thư viện.**
- Thanh tab Library đã cuộn ngang được (`MainActivity:524`); thêm tab là vấn đề dễ thấy, không phải lỗi bố cục.
- Không có thành phần chọn nhiều dùng chung. Xóa Glossary/Pronoun không kiểm tổ hợp Editorial đang dùng (`GlossaryStore:92-97`, `SqliteEditorialApiStore:70-71`).
- `GlossaryPageFactory:143-164` (`glossaryRow`) là mã chết có nút Del không xác nhận. Nhãn `— → —` là ngôn ngữ để trống (`:109`).

## 3. Phản biện đã sửa gì trong bản nháp

| Bản nháp | Sai ở đâu | Sửa thành |
|---|---|---|
| Lưu bảng glossary theo từng chương trong `prepared_batches.settings_json` | Intent luôn gửi `settings` nên service bỏ qua giá trị trong batch (`TranslatorService:329`, `MainActivity:1536`); kế hoạch dùng lại thì `settingsJson` bị ghi đè (`PreparationCoordinator:78,84,100`); `selectionKey` không chứa glossary (`:181-195`), nên hai bộ chương cùng RAW dùng chung một kế hoạch | **Ảnh chụp lúc bấm Start**: một tệp nội bộ đặt tên theo `startSessionId` chứa {chương → id, hash, nội dung Glossary/Pronoun}; intent chỉ mang id phiên (tránh vượt giới hạn kích thước intent); `runSinglePreparedJob` áp lên **bản sao** `AppSettings`; `jobs.settings` mang tiếp |
| (không nói) | Bản sao settings theo chương có thể bị `SettingsStore.save` ghi thành cấu hình chung (`MainActivity:1526`, `TranslatorService:337`) | Cấm lưu bản theo chương làm cấu hình chung; test kiểm cấu hình Active không đổi sau khi chạy |
| (không nói) | Kiểm tra trước Start, xem trước prompt, ô "Locks" chỉ nhìn Glossary Active (`MainActivity:1481,1494,2278-2321`, `TranslatePageFactory:276`) | Chế độ bộ chương hiển thị bảng theo chương; xem trước prompt chọn được chương |
| "Trần chi phí cho cả hàng đợi" | Chưa tồn tại | Ghi rõ là hành vi mới; giai đoạn đầu giữ trần theo chương, hiện tổng ước tính trước khi chạy |
| "Lỗi một chương thì chạy tiếp" | Nguy hiểm nếu lỗi là hết tiền/khóa sai/mạng: chạy lỗi lần lượt từng chương và có thể tốn tiền | Chỉ chạy tiếp với lỗi riêng của chương (kiểm toàn vẹn `:684`, chunk không chia nhỏ được `:669`); **dừng** với trần chi phí, không rõ đã gửi (`:631`), lỗi xác thực/hạn mức/mạng |
| RAW lưu bằng tệp JSON | Không có giao dịch, khó nối với `jobs` | **Bảng SQLite mới** cho chỉ mục RAW và bộ chương (đã có khung migration, `VER=29`); quy tắc "không đổi schema" chỉ áp cho 4.22 |
| Mục RAW làm trước (L1) | Giao ít giá trị Y3 nhất mà vẫn phải sửa `FileUtil` | Lõi dịch theo chương làm trước, dùng ngay "Browse TXT(s)" sẵn có; tab RAW làm sau |
| Hash RAW tự tính | `jobs.input_hash` tính trên văn bản đã chuẩn hóa (`PreparationCoordinator:131`) | Chỉ mục RAW lưu đúng hash đó để biết chương nào đã có bản dịch |

## 4. Đề xuất cuối

### S0 — An toàn dữ liệu (rủi ro thấp; nên làm trước mọi thứ)
1. `GlossaryStore` dùng `commit()` cho lưu danh sách và lựa chọn Active.
2. `loadAll` của Glossary/Pronoun: chuỗi hỏng thì **báo lỗi và chặn ghi**, không trả rỗng rồi ghi đè.
3. Nhập hàng loạt không tự đổi profile Active.
4. **"Xuất thư viện"**: Glossary + Pronoun (và sau này RAW, bộ chương) thành một ZIP qua SAF; **"Nhập thư viện"** đọc lại, bỏ trùng theo nội dung.
5. Xóa mã chết `glossaryRow`; thay `— → —` bằng số chương + số mục theo loại.

### S1 — Lõi dịch: mỗi chương một Glossary/Pronoun
- Ảnh chụp theo phiên lúc Start (mục 3); mỗi job lưu đúng Glossary/Pronoun của chương; cấu hình Active không đổi.
- Quy tắc dừng/chạy tiếp khi lỗi như mục 3.
- Không đổi giao diện ngoài phần cần cho S2.

### S2 — Giá trị Y3 sớm nhất: "Ghép Glossary/Pronoun theo chương"
- Màn Dịch: chọn nhiều RAW bằng "Browse TXT(s)" như hiện nay → nút **"Ghép theo chương"**.
- App **gợi ý** Glossary/Pronoun cùng số đầu tên tệp; bảng xác nhận: xanh (đủ), vàng (thiếu một loại), đỏ (nhiều ứng viên hoặc không có số). Owner sửa từng dòng bằng danh sách chọn, rồi bấm Chạy.
- Tinh thần quy định v4.17 "không tự ghép chương": chỉ gợi ý theo số đầu tên tệp, hiện mọi dòng, **owner xác nhận** trước khi chạy, không bao giờ ghép lại âm thầm.
- Tách số chương: số đầu tên tệp (`025_…` → 25, `25` = `025`); không lấy số trong `VOL08`, `Ex2`; trùng số hoặc không có số → đỏ.
- Ước tính chi phí từng chương và tổng trước khi chạy.

### S3 — Bộ chương lưu sẵn và chọn nhiều
- **Bộ chương** (bảng SQLite): tên + các dòng {số chương, RAW, Glossary id, Pronoun id}; lưu kết quả bảng ghép S2 để chạy lại lần sau; màn Dịch chọn "Nguồn: Tệp TXT | Bộ chương", tick chương (mặc định chương chưa có bản dịch).
- **Chế độ chọn nhiều** dùng chung cho Glossary, Pronoun, Bộ chương (sau là RAW): nút "Chọn" hoặc nhấn giữ → ô tròn, "n đã chọn · Chọn tất cả · Hủy", thanh dưới: Xóa / Xuất / (RAW: Tạo bộ chương).
- Xóa: hộp xác nhận nêu số mục; mục đang Active, đang dùng trong bộ chương, tổ hợp Editorial hoặc job chưa xong → liệt kê và chặn (hoặc xác nhận lần hai); thao tác qua `ensureConfigMutable` và lưu một lần.
- Sắp xếp theo số chương (số, không theo chữ), ô tìm kiếm.
- Lưu tên tệp gốc và số chương thành trường riêng để đổi tên không làm mất ghép.

### S4 — Tab RAW
- Nhập nhiều TXT, **sao chép nội dung vào app** (không giữ quyền URI), bảng SQLite chỉ mục (tên tệp gốc, số chương, số ký tự, hash chuẩn hóa như `jobs.input_hash`), bỏ trùng.
- `FileUtil` thêm đường đọc RAW nội bộ (không đòi quyền URI; tên hiển thị = tên tệp gốc).
- Không cho xóa RAW đang có job chưa xong (resume cần đọc lại).
- Loại thư mục RAW khỏi Auto Backup (giới hạn 25 MB); sao lưu bằng "Xuất thư viện".

### S5 — Bộ chương → tổ hợp Editorial
- Sau khi dịch xong, tạo tổ hợp Editorial từ RAW + bản dịch vừa ra + cùng Glossary/Pronoun. Làm sau khi Editorial 4.22 xong. "Bộ chương" và `EditorialApiCombo` để riêng (tổ hợp là một RAW + một DRAFT + cài đặt Editorial, không có nhóm), nhưng dùng chung dạng dòng {RAW, Glossary id, Pronoun id}.

## 5. Kiểm thử bắt buộc

| Rủi ro | Test |
|---|---|
| Chương dịch nhầm Glossary | 3 input, 3 glossary khác nhau, provider giả: kiểm prompt từng chương và `jobs.settings` |
| Kế hoạch dùng lại giữa hai bộ cùng RAW | ánh xạ theo chương sống qua các nhánh dùng lại (`PreparationCoordinator:78,84`) |
| Ghi đè cấu hình chung | `SettingsStore` và profile Active không đổi sau khi chạy bộ chương |
| Resume sau khi app bị tắt | force-stop giữa chương 2; chạy tiếp dùng đúng ảnh chụp chương 2 |
| Phân loại lỗi | lỗi chi phí, không rõ đã gửi, xác thực → dừng; lỗi kiểm toàn vẹn → sang chương sau |
| Tách số chương | `025`/`25`, `VOL08_013`, `Ex2`, trùng số, không có số |
| Chuỗi glossary hỏng | không bị ghi đè thành rỗng |
| RAW nội bộ | `validateReadable`, `displayName`, tên output đúng; xóa RAW có job dở bị chặn |
| Xóa hàng loạt | mục đang được tổ hợp Editorial/bộ chương dùng bị chặn hoặc cảnh báo |
| Xuất/nhập thư viện | khứ hồi giữ đúng nội dung và hash; nhập lại không tạo trùng |

Tất cả trên host + `emulator-5554` với provider giả; USD 0.

## 6. Nhánh và phát hành

Khuyến nghị: **(b) phát hành 4.22.0 trước, rồi tạo `feature/v4.23` từ tag `v4.22.0`.**
- v4.22 đang có yêu cầu thử thiết bị và phát hành (`docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md`); điện thoại chạy `4.22-dev.2`. Sửa lõi dịch trên cùng nhánh sẽ làm mất giá trị bằng chứng thử.
- (c) làm thẳng trên `feature/v4.22`: tệ nhất. (a) tách `feature/v4.23` từ HEAD v4.22 chỉ hợp cho tài liệu thiết kế.
- **Ngoại lệ đáng cân nhắc:** sau khi cài bản phát hành 4.22.0 (không debuggable) thì không còn sao lưu `run-as`. "Xuất thư viện" (S0 mục 4) và sửa `commit()`/`loadAll` (S0 mục 1–2) nhỏ, rủi ro thấp, bảo vệ dữ liệu thật của owner → **đề nghị đưa S0 vào 4.22.0**, còn S1–S5 sang 4.23.

## 7. Owner cần quyết

**Đã quyết 2026-10-18:** S0 vào 4.22.0 (`docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md`). Owner sau đó yêu cầu làm tab RAW, chọn hàng loạt và bộ chương cho luồng Dịch, không động vào Biên tập: yêu cầu làm việc `docs/V423_TRANSLATE_LIBRARY_WORK_REQUEST.md` (thứ tự L1 RAW, L2 chọn hàng loạt, L3 bộ chương, L4 dịch theo bộ chương; câu 3–4 dùng mặc định trong đó).

1. Đưa S0 (an toàn dữ liệu + xuất/nhập thư viện) vào 4.22.0, hay để 4.23?
2. Thứ tự 4.23: S1 + S2 trước (dịch nhiều chương ngay với "Browse TXT(s)"), rồi S3, S4? Hay owner muốn tab RAW (S4) sớm hơn dù giá trị Y3 đến chậm?
3. Khi một chương lỗi riêng (không phải lỗi tiền/khóa/mạng): chạy tiếp chương sau (đề xuất) hay dừng cả hàng đợi như hiện nay?
4. Trần chi phí: giữ theo từng chương (đề xuất cho S1–S2) hay cần thêm trần cho cả hàng đợi?
