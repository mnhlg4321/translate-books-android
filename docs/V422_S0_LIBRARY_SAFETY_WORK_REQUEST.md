# v4.22 — Yêu cầu làm việc S0: an toàn dữ liệu thư viện và Xuất/Nhập thư viện

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (khối v4.22). Checklist: `release_checklists/v4.22.md`. Nhánh duy nhất: `feature/v4.22`, worktree `D:\P5E-builds\wt-v4.22`. Không làm trong `D:\App Translate Books`.

**Quyết định owner 2026-10-18:** đưa S0 của `docs/V423_LIBRARY_BATCH_PROPOSAL.md` vào 4.22.0; S1–S5 sang 4.23 (S1 + S2 trước).

**Quyết định sản phẩm phục vụ:** owner không mất Glossary/Pronoun khi app bị tắt, khi dữ liệu lưu bị hỏng, hoặc khi cài bản phát hành 4.22.0. Sau khi cài 4.22.0 (không debuggable) thì `run-as` không còn dùng được. Từ đó "Xuất thư viện" là cách sao lưu duy nhất.

Yêu cầu này chèn vào `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md` giữa F1 và F2. Bản `4.22-dev.3` phải có cả R và S0.

## 1. Hiện trạng (mã `db71bef5`, đường dẫn trong `app/src/main/java/com/ml/tblandroidtxt/`)

| # | Vấn đề | Chỗ trong mã | Hệ quả |
|---|---|---|---|
| D1 | `GlossaryStore.saveAll` và `setSelectedId` dùng `apply()` (ghi bất đồng bộ) | `GlossaryStore:68`, `:106` | App bị tắt ngay sau khi nhập/sửa thì mất thay đổi (R1 đã gặp trong test) |
| D2 | `GlossaryStore.loadAll` và `PronounStore.loadAll` nuốt lỗi đọc, trả danh sách thiếu hoặc rỗng | `GlossaryStore:57-60`, `PronounStore:22` | Lần lưu kế tiếp ghi danh sách rỗng đè lên dữ liệu: **mất toàn bộ thư viện**, không báo |
| D3 | Nhập nhiều tệp tự chọn tệp đầu làm Active | `MainActivity.importGlossaryProfiles` (`:1132-1138`), `importPronounProfiles` (`:1162-1167`) | Glossary/Pronoun đang dùng cho bản dịch bị đổi ngầm |
| D4 | Không có chức năng xuất/nhập thư viện | — | Không có cách sao lưu sau khi cài bản phát hành |
| D5 | `glossaryRow` là mã chết, có nút Del không hỏi lại | `GlossaryPageFactory:143-164` | Rủi ro nếu bị nối lại vào giao diện |
| D6 | Nhãn `— → —` (ngôn ngữ để trống) | `GlossaryPageFactory:109` | Thông tin vô nghĩa trên mọi thẻ |

Thấy thêm khi nghiên cứu, **ngoài phạm vi S0**: nút "Export profile" có sẵn (`MainActivity:1402-1408`, `SettingsStore:120`) ghi cả **API key dạng chữ thường** vào tệp JSON. Owner chọn bỏ key trong 4.22: bước S0.8.

## 2. Quyền

- Không gọi provider. Mọi test dùng dữ liệu tổng hợp, USD 0.
- Test thiết bị chỉ chạy trên `emulator-5554`. Điện thoại theo F5/F8 của yêu cầu thử thiết bị, mỗi lần cài cần owner duyệt riêng.
- Push `feature/v4.22` sau khi quét bí mật/văn bản sách. Không tag, không merge.

## 3. Ràng buộc

- Không đổi schema DB. Không đổi khóa/định dạng SharedPreferences hiện có (`glossary_store/glossaries_json`, `pronoun_store/profiles_json`): dữ liệu thật trên điện thoại phải đọc được y nguyên.
- Không đổi luồng dịch hay Editorial. Không đổi hợp đồng `CP-IMPL-21`.
- Chữ hiển thị bằng tiếng Việt.
- Không thêm thư viện ngoài. ZIP dùng `java.util.zip`.

## 4. Các bước

**S0.1 — Ghi đồng bộ (D1).**
- `GlossaryStore.saveAll` và `setSelectedId` đổi sang `commit()`. `saveAll` trả `boolean`, và nơi gọi báo lỗi khi `false` (giống `PronounStore.saveAndSelect`).
- Cache chỉ cập nhật khi `commit()` thành công.
- `PronounStore.saveAll` cũng trả kết quả `commit()`.
- Test host: `saveAll` trả false thì cache không đổi. Test hiện có vẫn xanh.

**S0.2 — Đọc hỏng thì chặn ghi (D2).**
- Khi `loadAll` gặp lỗi phân tích JSON của Glossary hoặc Pronoun:
  - giữ nguyên chuỗi gốc;
  - chép chuỗi gốc sang khóa mới cùng tệp prefs (`glossaries_json_corrupt_<epochMillis>` / `profiles_json_corrupt_<epochMillis>`) bằng `commit()`;
  - đặt trạng thái `CORRUPT` cho store.
- Ở trạng thái `CORRUPT`, mọi `saveAll` / `upsert` / `delete` / `saveAndSelect` từ chối ghi và trả lỗi. Không bao giờ ghi danh sách rỗng đè lên chuỗi khác `[]`.
- Giao diện Glossaries/Pronouns hiện thẻ đỏ: "Dữ liệu glossary đọc lỗi; app đã giữ bản gốc và tạm khóa ghi". Thẻ có nút "Xuất dữ liệu gốc" (ghi chuỗi gốc ra tệp qua SAF để owner gửi kiểm).
- Một mục hỏng trong mảng tính là cả kho hỏng. Không âm thầm bỏ từng mục.
- Test host:
  - chuỗi hỏng → `loadAll` báo `CORRUPT`, bản sao có ở khóa mới, `saveAll` bị từ chối, chuỗi gốc không đổi;
  - chuỗi `[]` và chuỗi hợp lệ → hành vi như cũ;
  - test cho cả hai store.

**S0.3 — Nhập không đổi Active (D3).**
- Nhập nhiều Glossary/Pronoun chỉ thêm profile, không gọi `selectGlossary` / `selectPronoun`. Profile Active (kể cả khi đang không có Active) giữ nguyên.
- Thông báo sau khi nhập: "Đã thêm n glossary. Bấm Use để dùng cho bản dịch."
- "Replace file" (`pendingPronounReplaceId`, thay tệp của một profile) giữ hành vi cũ.
- Test: test host/presenter cho hai trường hợp (đang có Active, chưa có Active). Sửa các androidTest đang dựa vào tự chọn (tìm bằng `importGlossaryProfiles` / `importPronounProfiles` và các test thư viện). Ghi lại test nào phải sửa và lý do.

**S0.4 — Xuất thư viện (D4).**
- Library, tab `Files` (trang đầu Library), thêm thẻ "Sao lưu thư viện" với hai nút "Xuất thư viện" và "Nhập thư viện".
- Xuất: `ACTION_CREATE_DOCUMENT`, tên gợi ý `TranslateBooks-library-<yyyyMMdd-HHmmss>.zip`. Nội dung:
  - `library.json`:
    - `format: "tbl-library"`, `formatVersion: 1`, `appVersion`, `createdAt`;
    - `glossaries[]` (đủ trường như `GlossaryStore.toJson`: id, name, sourceLang, targetLang, createdAt, updatedAt, terms);
    - `pronouns[]` (đủ trường như `PronounStore.toJson`);
    - `active: {glossaryId, pronounId}`;
    - `sha256` của từng mục, tính trên JSON chuẩn của mục đó.
  - Bản đọc được (chỉ để người xem, không dùng khi nhập):
    - `glossaries/<nnn>_<tên an toàn>.csv` với cột `source,target,category,note`;
    - `pronouns/<nnn>_<tên an toàn>.txt` (văn bản gốc).
  - `README.txt` một đoạn tiếng Việt: tệp gồm gì, nhập lại ra sao.
- **Không** đưa API key, cấu hình provider, Settings, job, dữ liệu Editorial vào ZIP.
- Ghi xong thì đọc lại tệp vừa ghi, kiểm `library.json` phân tích được và số mục khớp. Báo "Đã xuất n glossary, m pronoun (k KB)".
- Không xuất khi store đang `CORRUPT`; khi đó dùng nút "Xuất dữ liệu gốc" ở S0.2.
- Thời điểm xuất được lưu lại và hiện trên thẻ ("Lần xuất gần nhất: …").

**S0.5 — Nhập thư viện (D4).**
- `ACTION_OPEN_DOCUMENT`. Chỉ đọc mục `library.json` trong ZIP; bỏ qua mọi mục khác. Giới hạn kích thước giải nén (ví dụ 50 MB) và số mục.
- Kiểm `format`/`formatVersion`, `sha256` từng mục. Mục sai hash hoặc không phân tích được → bỏ qua và liệt kê.
- Gộp, **không bao giờ ghi đè hay xóa**:
  - id chưa có → thêm, giữ id. Tổ hợp Editorial tham chiếu theo id nên khôi phục trên máy mới vẫn khớp.
  - id đã có, nội dung giống → bỏ qua (đếm "đã có").
  - id đã có, nội dung khác → thêm thành mục mới (id mới, tên thêm " (nhập <ngày>)").
  - Nội dung trùng với một mục khác id → bỏ qua (đếm "trùng").
- Trước khi ghi, hiện hộp xem trước: "Sẽ thêm a glossary, b pronoun; bỏ qua c đã có, d trùng, e lỗi" → owner bấm "Nhập".
- Ghi một lần cho mỗi store bằng `commit()`. Không đổi Active (giống S0.3). Mục `active` trong ZIP chỉ để tham khảo, hiện trong thông báo.
- Bị chặn khi store `CORRUPT` hoặc khi `ensureConfigMutable()` từ chối (đang có job dịch chạy).
- Logic xuất/nhập nằm trong lớp thuần Java (ví dụ `LibraryArchive`): dựng ZIP từ hai danh sách, đọc ZIP thành kế hoạch gộp. Activity chỉ lo SAF và hộp thoại.
- Test host cho `LibraryArchive`:
  - khứ hồi giữ đúng mọi trường và hash;
  - bốn trường hợp gộp;
  - ZIP thiếu `library.json`, sai `format`, sai hash, mục quá lớn;
  - tên tệp có ký tự lạ, tên trùng;
  - ZIP không chứa chuỗi API key của `AppSettings` mẫu.

**S0.6 — Dọn giao diện nhỏ (D5, D6).**
- Xóa `glossaryRow` sau khi xác nhận không còn ai gọi.
- Thẻ Glossary thay `— → —` bằng `n mục · x nhân vật · y thuật ngữ` (đếm theo `category`). Ngôn ngữ chỉ hiện khi khác `—`.

**S0.8 — "Export profile" không ghi API key (owner chọn (a) 2026-10-18).**
- `MainActivity` nhánh `REQ_EXPORT_PROFILE` (`:1402-1408`) xóa khóa `apiKey` khỏi JSON trước khi ghi. Dùng hàm thuần (ví dụ `SettingsStore.toExportJson(AppSettings)`), không đổi `SettingsStore.toJson` vì job, kế hoạch dịch và intent vẫn cần đủ settings.
- Nhập profile (`REQ_IMPORT_PROFILE`): tệp không có `apiKey` thì **giữ API key đang lưu**, không xóa. Hiện `fromJson` đặt mặc định rỗng rồi `SettingsStore.save` ghi đè, nên khi nhập phải gán lại key hiện tại nếu tệp không có khóa này. Tệp cũ có `apiKey` vẫn được đọc như trước.
- Ghi chú trên nút hoặc thông báo sau khi xuất: "Tệp không chứa API key".
- Không đổi "Export .env" (`SettingsStore:176`). Đó là tệp cấu hình provider, chủ đích chứa `API_KEY`.
- Test host:
  - JSON xuất không có `apiKey`, còn đủ các khóa khác;
  - nhập tệp không có key thì giữ key đang lưu;
  - nhập tệp cũ có key thì dùng key trong tệp.
- Còn lại, ngoài S0: báo cáo job (`JobStore:104`) in nguyên `settingsJson` khi không phân tích được, và chuỗi đó có thể chứa key. Ghi vào known limits, để 4.23.

**S0.7 — Test thiết bị** (`emulator-5554`, gộp vào QA của F4 trong yêu cầu thử thiết bị). Một androidTest, dữ liệu tổng hợp:
- nhập 3 glossary + 2 pronoun → Active không đổi;
- xuất thư viện ra một tệp ZIP. Dùng URI tệp của test thay cho bộ chọn hệ thống, như các test media hiện có; ghi rõ là bộ chọn hệ thống chưa được thử;
- xóa 1 glossary, nhập lại ZIP → glossary đó trở lại với id cũ, các mục khác báo "đã có";
- ghi chuỗi hỏng vào prefs → trang Glossaries hiện thẻ đỏ, ghi bị chặn, bản sao có trong prefs;
- force-stop ngay sau khi nhập → mở lại, dữ liệu còn đủ (kiểm `commit()`);
- dọn dữ liệu thử, khôi phục prefs như trước khi chạy.

Chụp ảnh thẻ "Sao lưu thư viện", hộp xem trước khi nhập và thẻ đỏ, đưa vào walkthrough 4.22.

## 5. Tiêu chí PASS

- Hồi quy archive sạch (F2): engine, app debug/release/benchmark, lint 0 lỗi, androidTest compile. Test mới xanh, test cũ xanh hoặc đã sửa có ghi lý do.
- Test thiết bị S0.7 PASS trên emulator. Hash prefs/DB của emulator được khôi phục.
- Dữ liệu thật của owner đọc được không đổi: test host nạp một chuỗi JSON dạng cũ (tổng hợp, cùng cấu trúc) và kiểm `loadAll` không thay đổi gì.
- ZIP không chứa API key (test S0.5).

## 6. Thay đổi kéo theo

- `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md`:
  - F2–F4 chạy sau S0;
  - phiếu F6 thêm T11 (Xuất thư viện trên điện thoại bằng bộ chọn hệ thống, mở ZIP thấy đủ glossary/pronoun) và T12 (Nhập lại ZIP vừa xuất: báo "đã có", không trùng, Active không đổi);
  - F8: owner **dùng "Xuất thư viện" và giữ tệp ZIP ngoài điện thoại** trước khi cài 4.22.0.
- `RELEASE_NOTES.md` / `CHANGELOG.md` mục 4.22.0 (bước E2):
  - ghi đồng bộ;
  - chặn ghi khi dữ liệu hỏng;
  - nhập không đổi Active;
  - Xuất/Nhập thư viện (không gồm API key, Settings, job, dữ liệu Editorial);
  - giới hạn: chưa gồm RAW / bộ chương (4.23).
- `release_checklists/v4.22.md` bước 05 thêm commit S0. `BUILD_STATE.md` và snapshot cập nhật khi S0 xong.

## 7. Quyết định đã có

- 2026-10-18: owner chọn (a) cho "Export profile": bỏ API key trong 4.22, làm ở S0.8.

## 8. Không làm

Không làm tab RAW, chọn nhiều, bộ chương, dịch theo chương (S1–S5, 4.23). Không đổi schema DB hay khóa prefs. Không gọi provider. Không chạy test trên điện thoại. Không cài khi chưa duyệt. Không tag/merge.
