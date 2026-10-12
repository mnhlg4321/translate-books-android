# Yêu cầu làm việc — P6: từ L1 đã commit tới bản cuối đầu tiên (v4.18)

Ngày lập: 2026-10-02. Người giao: owner. Dùng nguyên văn tài liệu này làm chỉ dẫn cho một phiên làm việc mới.
Repository `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, mốc bắt đầu = commit chứa tài liệu này (sau `d71865f1`). Không tạo branch, release hay checklist mới.

## 1. Hiện trạng đã kiểm (2026-10-02)

| Hạng mục | Sự thật | Nguồn |
|---|---|---|
| L1 chương 001 | RAW `7a5e3428…` (ngoại lệ B) → RECONCILE `7483b211…` `COMMITTED`; REPORT_L1 `b33baf33…`, receipt `11754968…`, cùng hàng; 1 call, USD 0.00747405 | `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` mục "Event M4 lần 2"; `D:\P5E-private\reconcile-m4-20261002T003446Z-…\M4_OUTCOME.json` |
| P5 | `P5_EXIT_PASS_UNDER_EXCEPTION_B` ở mức app/DB; M6 chỉ là đọc lại file DB sau force-stop, chưa qua UI | snapshot, checklist |
| Thiết bị pilot | `15e84958` online; production `4.18-p5e.5`/213 (`88854E47…`), test `26CB0563…`; DB v25 `06C4C48E…` chứa RAW + RECONCILE | `adb` chỉ-đọc lúc lập tài liệu |
| Mã offline (chưa có trong APK 213) | L2_EDIT + L3 boundary, adapter L2/L3, coordinator `runToFinal` + `exportTxt`, G5 tiến độ/xem bản cuối/xuất TXT trên thẻ chương (`a6da1a35`) | git log |
| Kiểm thử | engine 247/247, app unit 298/298, lint PASS, androidTest compile PASS. Chưa test instrumented mới nào chạy trên máy | snapshot |
| Emulator | AVD `tbl-code113-dqa-api35` (android-35, x86_64, system image có sẵn) — dùng được cho test instrumented với fake provider, không đụng dữ liệu pilot | `~/.android/avd` |

## 2. Phân tích

1. **L1 hiện chỉ là bộ khung.** REPORT_L1 thật có `populationTotal=1`, `accountedTotal=1`, `findingCount=0`: app chỉ cấp một population cho cả chương (`population:001`), wire L1 giới hạn 4 findings. L1 chứng minh được chain/identity/atomic commit, **không** chứng minh ledger raw unit/TG/SR/RC theo acceptance. Chất lượng bản cuối hiện phụ thuộc vào L2/L3.
2. **`L2_RAW_DISCOVERY` chưa nối.** Đối chiếu count với REPORT_L1 không có sức phân biệt vì REPORT_L1 không có count theo ledger.
3. **Rủi ro lớn nhất còn lại** là chưa có lượt L2/L3 thật nào: chưa biết model trả change rows đúng anchor không, độ dài output L2_EDIT với chương ~26 KB, chi phí thật, và tỷ lệ REPAIR_REQUIRED.
4. **Câu hỏi "cài và test bản build mới trên máy được chưa":**
   - Test instrumented mới (coordinator, store, lineage) được viết cô lập: DB tên ngẫu nhiên, pack trong thư mục cache, xóa sau khi chạy; không đụng `tbl_android_txt.db`. Dù vậy chúng chạy trong process và thư mục dữ liệu của app pilot (ghi file tạm, khởi động process app), còn `connectedAndroidTest` gỡ app (mất dữ liệu) và bị cấm trên pilot. Chạy chúng trên **emulator** với APK đã archive trước; trên pilot chỉ chạy bằng `am instrument` từng lớp khi owner cho phép riêng.
   - Cài production mới (có UI G5) lên pilot: **làm được về kỹ thuật** (không có migration mới; v25 giữ nguyên; `install -r` giữ dữ liệu) nhưng cần owner duyệt, sao lưu DB trước, và DB hash sẽ đổi khi mở app nên mọi event sau phải đo lại M0. Lợi ích hiện tại nhỏ (UI chỉ hiện "L1 xong, L2 chưa bắt đầu", chưa có FINAL để xem/xuất). Khuyến nghị: gộp vào **một** build sau khi xong W1–W2, rồi dùng chính build đó cho lượt L2/L3 thật.

## 3. Quyết định owner (mỗi mục có khuyến nghị; owner sửa trực tiếp trước khi giao nếu khác)

| # | Quyết định | Khuyến nghị | Đã chốt |
|---|---|---|---|
| D1 | Hướng `L2_RAW_DISCOVERY` | **(d)**: một call mù chỉ RAW + GLOSSARY lập danh sách candidate (UNIT/TG/SR/RC, anchor RAW); app chuyển danh sách vào `L2_EDIT` như block do app sở hữu; `L2_EDIT` phải giải quyết từng candidate (PROCESSED/PRESERVED/UNPROCESSED/CONFLICT); app đếm, UNPROCESSED > 0 → REPAIR_REQUIRED, CONFLICT → CONTENT_BLOCKED. Đúng contract pha, không đổi wire L1 đã pin, không chạy lại L1. (a) đổi wire đã pin và chạy lại L1; (b) lệch contract; (c) vô nghĩa | ☑ (d) ☐ khác: … |
| D2 | Chương đại diện | Chương 1 = `001` (đã có L1, thuộc nhóm ngắn hay không do owner xác nhận). Hai chương còn lại: phiên mới liệt kê ứng viên từ nguồn pilot sẵn có (chỉ đọc, kèm kích thước/mật độ thoại) để owner chọn: một chương dày thoại/xưng hô, một chương dài gần giới hạn | ☑ 001 đồng ý; hai chương còn lại: … |
| D3 | Ngân sách theo pha cho **một** chuỗi L2/L3 của chương 001 | `L2_RAW_DISCOVERY` 1 call, output ≤ 8,192 token, ≤ USD 0.05; `L2_EDIT` 1 call, output ≤ 16,384 token, ≤ USD 0.10; `L3_RAW_FIRST_REAUDIT` 1 call, output ≤ 8,192, ≤ USD 0.05; `L3_RECONCILE` 1 call, output ≤ 16,384, ≤ USD 0.10. Input ≤ 200,000 byte mỗi call. Trần chuỗi **USD 0.30**, 0 repair, 0 retry, 180 s/call. Route OpenRouter `openai/gpt-5.6-luna` như M4. Ước tính thực tế theo M4: vài cent | ☑ đồng ý ☐ khác: … |
| D4 | Cài build mới lên pilot `15e84958` (data giữ, sao lưu DB trước) | Đồng ý, nhưng chỉ sau W1–W3 đạt, một lần cho cả UI và lượt L2/L3 | ☑ đồng ý |

Chỉ phần đã đánh dấu ☑ mới là quyền. Mục nào để trống: làm các việc không phụ thuộc rồi hỏi đúng mục đó.

## 4. Công việc theo thứ tự

| Nhóm | Việc | Phụ thuộc | Đầu ra / kiểm thử | PASS | Dừng / đổi cách |
|---|---|---|---|---|---|
| **W1** offline | Hiện thực D1: wire `safe4.l2.raw-discovery.wire.v1` (candidate list, giống grammar re-audit L3), call blind với projection `L2_RAW_DISCOVERY` (chỉ RAW + GLOSSARY), block `L2_RAW_CANDIDATES` vào `L2_EDIT`, `resolutions` trong wire L2, app đếm; adapter OpenRouter cho call mới; `runToFinal` chạy 3 bước L2 (discovery → edit) rồi L3; attempt identity đổi theo thiết kế mới (chưa có L2 nào commit nên không ảnh hưởng dữ liệu) | D1 | JVM: visibility (discovery không thấy DRAFT/REPORT_L1), thiếu UNIT → REPAIR, candidate không giải quyết → REPAIR, CONFLICT → CONTENT_BLOCKED, resume không gọi lại; adapter không chạm mạng | engine + app unit đạt, lint | Hai vòng cùng chữ ký lỗi → đổi cách, ghi lý do |
| **W2** offline | Nút chạy L2/L3 trên thẻ chương: hộp thoại cấp phép hiển thị đúng trần D3 cho từng pha, người dùng xác nhận; chạy nền qua `runToFinal` với adapter thật; tiến độ qua `inspect`; trạng thái UNKNOWN/RECOVERY hiển thị và **không** tự chạy lại; khóa một-writer-một-chương; nút chỉ bật khi L1 COMMITTED | W1 | JVM + fake provider: double-tap không tạo hai lượt, process death giữa call → hiện cần quyết định, trần sai → không chạy | app unit, lint, androidTest compile | Như trên |
| **W3** emulator | Build bằng `scripts/build-and-save.ps1` + `scripts/build-and-save-android-test.ps1` (archive hai nơi); khởi động AVD `tbl-code113-dqa-api35`; `adb -s <emulator> install` hai APK đã archive; `am instrument -w -e class …` cho: `EditorialChapterFinalCoordinatorInstrumentedTest`, `EditorialPhaseArtifactStoreInstrumentedTest`, `EditorialP5EReconcileLineageInstrumentedTest`, `EditorialP5CExactBindingFakeE2EInstrumentedTest`, lớp test UI mới của W2 nếu có. Không dùng `connectedAndroidTest` | W1, W2 | Kết quả từng lớp (số test, lỗi) lưu vào payload QA | Tất cả đạt trên emulator | Lỗi mã: sửa trong phase, build lại. Emulator không chạy được: ghi nguyên nhân, không thay bằng pilot |
| **W4** pilot | Nếu D4 ☑: M0 chỉ-đọc (`scripts/p5e-m4-m0-observation.ps1`), sao lưu DB, cài production (và test nếu đổi) **đúng bản W3**, đọc lại hash; mở app, kiểm UI thẻ chương 001 hiển thị L1 COMMITTED, L2 chưa chạy; force-stop, mở lại, vẫn đúng | W3, D4 | Ảnh chụp màn hình + hash APK/DB trước/sau | UI hiển thị đúng trạng thái bền | Hash lệch → dừng trước khi cài; không khôi phục DB |
| **W5** live | Nếu D1, D3 ☑ và W4 xong: **một** chuỗi L2/L3 cho chương 001 từ UI (hoặc runner opt-in theo mẫu M4 nếu UI chưa đủ để thu evidence) với đúng trần D3; trước chạy M0; sau chạy đọc lại DB (L2/L3 rows, hash VI_L2/CHANGE_MAP/FINAL/QA_RECEIPT), `logcat -d -s P5E_RAW:V` | W4, D1, D3 | Evidence dưới `D:\P5E-private\p6-chain-001-<UTC>\` | `L3_FINAL_COMMITTED`, release numbers = 0 | STOP/UNKNOWN: ghi typed result, **không** gọi lại; sửa offline; lượt mới cần quyền mới |
| **W6** final | Nếu W5 đạt: xem bản cuối trong app; force-stop, mở lại, hash giống; xuất TXT qua SAF (owner chọn thư mục), app báo `EXPORT_VERIFIED`; kéo file về host chỉ-đọc, so SHA-256 = FINAL; owner đọc mẫu bản cuối đối chiếu RAW/DRAFT | W5 | Bảng evidence theo `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` mục 4–5 | Ứng viên `FINAL_OUTPUT_ACCEPTED` cho chương 001 (owner xác nhận chất lượng) | Không đạt chất lượng: ghi lỗi theo pha, sửa prompt/contract offline |

W1 → W2 → W3 làm ngay, không cần quyền. W4–W6 chỉ khi các mục tương ứng ở mục 3 đã ☑.

## 5. Giới hạn và quy tắc

- Không gọi lại RAW hay RECONCILE; không đổi dữ liệu L1 đã commit; không khôi phục DB; không `connectedAndroidTest` trên pilot; không xử lý API key (endpoint account fingerprint, nếu runner cần, do owner đưa).
- Mọi build qua script lưu hai nơi; không build cho thay đổi docs thuần; không rerun suite khi bytes không đổi.
- Mỗi lỗi: giả thuyết → tái hiện → sửa → bằng chứng mới; hai vòng cùng chữ ký lỗi thì đổi cách.
- Sub-agent chỉ nhận việc nhỏ độc lập (một test class, một tài liệu); phiên chính giữ quyết định kỹ thuật; không để hai agent sửa cùng một file.
- Commit theo nhóm, có dòng `Co-Authored-By`; push cùng branch; không stage `.idea` hay docs P5E cũ của owner; không đưa DB, key, nội dung model vào Git.
- Sau mỗi nhóm: cập nhật `EDITORIAL_RECOVERY_V4_18.md` (mục 9a/10, đúng một next action), `HANDOFF.md`, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, checklist v4.18.

## 6. Báo cáo cuối phiên

1. Điều mới được chứng minh (kèm hash/số liệu). 2. Chi phí và số call thật. 3. Lỗi đã sửa, test đã chạy, giới hạn. 4. Gate đạt/chưa đạt (P6 từng phần, `FINAL_OUTPUT_ACCEPTED`). 5. Commit/push. 6. Đúng một bước tiếp theo hoặc một quyết định cụ thể cần owner.
