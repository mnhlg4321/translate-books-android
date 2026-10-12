# Editorial API V1 — Q1: đưa chất lượng về chuẩn bản FINAL của owner (2026-10-07)

Coordinator: Claude. Baseline: HEAD `9524482b`. Owner đọc 3 chương N6 và **không chấp nhận chương nào** (chat 2026-10-07): chất lượng thấp hơn rõ so với bản FINAL owner đã biên tập; chương 001 còn sót tiếng Nhật; ký tự đặc biệt bị đổi; các sửa của app ít và không quan trọng. Kết quả N6: **0/3 chương đạt**.

Bản FINAL thủ công của owner (`D:\Ebooks\JAKUAKU MONSTER\5.FINAL\`, 28 chương) chỉ dùng làm **reference đánh giá độc lập**: không đưa vào prompt, không dùng để chỉnh prompt trên tập holdout, không bắt khớp nguyên văn. Mọi phân tích chỉ lưu số đếm/ID dòng trong Git; văn bản sách ở `D:\P5E-private`.

## 1. Chẩn đoán bằng số liệu (coordinator tự đo, chỉ đếm)

### 1.1 App gần như không thêm giá trị so với DRAFT

| Chương | Dòng owner sửa so với DRAFT | Dòng app sửa | Trùng với chỗ owner sửa | Độ giống FINAL: DRAFT → app |
|---|---:|---:|---:|---|
| 001 | 15 (9%) | 5 | 4 | 0.996 → 0.996 |
| 002 | 14 (13%) | 4 | 2 | 0.994 → 0.991 (tệ hơn) |
| 003 | 22 (19%) | 4 | 3 | 0.989 → 0.990 |

Toàn bộ 28 chương: owner sửa **910/4956 dòng (18%)**; app ở N6 sửa ~3% và chỉ chạm ~9/51 chỗ owner sửa.

### 1.2 Trong 13 dòng app sửa

- Đúng như owner (4): 001 L21, L162; 003 L51, L75.
- Đúng một phần (2): 001 L45, L77.
- **Lỗi mới do app gây ra (4):** 003 L1 đưa chữ Hán `三` vào tiêu đề (đúng lời owner: "sót tiếng Nhật"); 003 L37 đổi `?` thành `？` toàn khổ ("đổi ký tự đặc biệt"); 002 L24 đổi người dính máu từ "tôi" sang "cô bé" (sai nghĩa); 002 L76 bỏ "Từ giờ".
- Còn lại (3): thay đổi nhỏ không cần thiết.
- Bỏ sót: 001 L91 tiếng Nhật còn nguyên trong DRAFT (`あはっ…`) — app không dịch.

### 1.3 Owner thường sửa những gì (mẫu 001, 004, 007; ID dòng trong file riêng)

| Nhóm | Ví dụ dạng (không trích nguyên văn) | Ai nên làm |
|---|---|---|
| A. Ký hiệu theo RAW | DRAFT dùng `【・・】` → RAW/FINAL `《・・》`; `“…”` → `〝…〟`; `…` → `……` theo RAW; `『』` lồng; `：`/`／`/số toàn khổ trong dòng trạng thái | **App, tất định** (so dòng RAW↔DRAFT) |
| B. Rác/sót chưa dịch | kana còn sót, phiên âm ruby `《かな》` chép từ RAW, chữ Latin toàn khổ (`Ｔầng`) | App phát hiện; model dịch phần kana |
| C. Nhất quán thuật ngữ | `zombie` → `Zombie` theo glossary | App phát hiện theo glossary; model sửa |
| D. Xưng hô/ngôi kể | ngôi kể "mình" → "tôi"; "tôi" → "tớ" trong thoại với một người; bỏ `-kun` trong lời kể | Model, theo pronoun/glossary, app gợi ý chỗ lệch |
| E. Nghĩa và chọn từ | sắc thái từ (vd 女々しい), "cảnh giới" → "cảnh báo", thì/thể | Model |
| F. Cấu trúc dòng theo RAW | gộp/tách dòng để khớp số dòng RAW (DRAFT lệch ±1–4 dòng ở 13/28 chương) | App phát hiện vùng lệch; model sửa |

### 1.4 Nguyên nhân

1. **Lỗi luật của chính plan (coordinator nhận):** Quality Core mục 7 viết "Giữ ký hiệu…" nên model giữ ký hiệu **sai của DRAFT** thay vì theo RAW. Mục 5 "sửa tối thiểu" làm model rất dè dặt. Chuẩn của owner (v5) là RAW quyết định ký hiệu và cấu trúc.
2. **Phép đo N5 không đại diện:** fixture là lỗi gieo một dòng, rõ ràng; lỗi thật (nhóm A–F) tinh và dày hơn. 19/25 trên fixture không dự báo chất lượng chương thật.
3. **Model giao việc nhận diện tất định:** nhóm A, B, C, F là việc app làm chính xác được bằng so dòng; giao cho model nhỏ thì bị bỏ sót hoặc sinh lỗi (`三`, `？`).
4. **Năng lực model:** `openai/gpt-5.6-luna` là lớp rẻ; owner tạo FINAL bằng model mạnh trong Project. Chưa đo được ảnh hưởng — cần A/B.

## 2. Hướng sửa (đo trước, đổi sau)

1. **Bộ chấm theo FINAL** (offline, miễn phí): đo mọi cấu hình so với bản FINAL của owner, tách dev/holdout.
2. **Chuẩn hóa tất định theo RAW** (offline, miễn phí): app tự sửa nhóm A và đánh dấu nhóm B/C/F trước khi gọi model. Đo riêng phần này trên toàn bộ 28 chương — không tốn tiền.
3. **Quality Core v2 + gợi ý theo dòng**: viết lại luật theo chuẩn owner; đưa danh sách chỗ app phát hiện vào prompt; guard chặn dòng sinh thêm chữ Nhật/ký tự toàn khổ không có lý do.
4. **A/B model**: luna so với một model mạnh hơn trên cùng cấu hình.
5. Owner đọc 3 chương holdout của cấu hình tốt nhất — đó là cổng thật.

## 3. Tập đánh giá (khóa trước khi chỉnh bất cứ thứ gì)

- **Dev (được xem FINAL khi phân tích):** 001–008.
- **Holdout (không ai xem FINAL để chỉnh luật):** 010, 013, 016, 019, 022, 025 — chọn cố định "mỗi 3 chương từ 010", gồm 4 chương lệch dòng.
- **Dự trữ, không dùng:** các chương còn lại.
- Manifest Git chỉ ghi số chương + SHA-256 của RAW/DRAFT/FINAL/Glossary/Pronoun; file ở `D:\Ebooks\JAKUAKU MONSTER\` và bản sao chạy ở `D:\P5E-private\q1-inputs\`.

## 4. Yêu cầu làm việc Q1 (cho phiên Codex)

Branch giữ nguyên. Không đụng pilot, không đụng chunk-pair, không đổi UI (U1 do phiên khác làm song song; ownership tách bạch dưới đây). FINAL của owner không bao giờ nằm trong prompt, request, response hay fixture gửi model.

### Ownership

| Phần | File | Ghi chú |
|---|---|---|
| Bộ chấm | `scripts/p6/score_vs_final.py` (+ test) | mới |
| Chuẩn hóa + phát hiện | `editorial-engine/.../api/RawAlignedNormalizer.java` (mới), `LineAlign` (mới nếu cần) | thuần JVM |
| Prompt + guard | `QualityCore`, `EditPromptBuilder`, `EditGuards`, `EditorialApiFlow` | tăng `CONTRACT_REVISION` → `EDITORIAL_API_V1.2` |
| Runner | runner fixture hiện có: thêm chế độ đọc tập chương thật + model override | không đổi UI |

U1 sở hữu file UI trong `app/`; Q1 không sửa chúng. Build: phiên nào build sau dùng versionCode cao hơn bản đã archive.

### Q1.1 — Bộ chấm theo FINAL (offline)

Đầu vào: RAW, DRAFT, bản app, FINAL (đường dẫn riêng). Căn dòng không rỗng bằng `difflib` như các script coordinator đã dùng. Chỉ số mỗi chương và tổng:

- `fix_recall`: trong các dòng owner sửa, tỷ lệ dòng bản app **gần FINAL hơn** DRAFT (độ giống ký tự tăng ≥ 0.02) và tỷ lệ khớp gần như nguyên văn (≥ 0.97).
- `regression`: trong các dòng owner **không** sửa, số dòng app sửa; trong mọi dòng app sửa, số dòng **xa FINAL hơn** DRAFT.
- `hard`: dòng chứa kana/Hán mà dòng DRAFT tương ứng không có (sinh thêm); dòng còn kana khi FINAL không còn; dòng còn ruby `《かな》`; Latin toàn khổ; lệch số ký hiệu (`《》〝〟『』：／……`) so với dòng RAW; số dòng so với RAW.
- `similarity`: độ giống cả chương với FINAL của DRAFT và của app.

Báo cáo JSON + bảng tóm tắt; mặc định không in văn bản sách. Test với file tổng hợp tự tạo. **Baseline** ngay: DRAFT và 3 bản N6 (001–003).

### Q1.2 — Chuẩn hóa tất định theo RAW (offline)

`RawAlignedNormalizer` nhận RAW + DRAFT (+ glossary), căn dòng, trả DRAFT đã chuẩn hóa + danh sách phát hiện:

- **Tự sửa (chỉ khi dòng RAW tương ứng được căn chắc chắn):** `【・…】` → `《・…》` khi RAW có `《・…》`; `“x”` → `〝x〟` khi RAW có `〝〟` ở dòng đó; `…` lẻ → `……` khi RAW dùng `……`; dấu `：`/`／`/số toàn khổ trong dòng trạng thái `【…】` theo RAW; xóa ruby `《kana》` bị chép sang; Latin toàn khổ → thường. Mỗi sửa ghi `(dòng, loại)`.
- **Chỉ phát hiện (đưa cho model):** dòng còn kana/Hán; thuật ngữ glossary sai hoa/thường hoặc sai dạng; vùng số dòng lệch RAW; dòng ngôi kể dùng đại từ khác hồ sơ pronoun của người kể.
- Không đổi chữ ngoài ký hiệu; không đụng dòng căn không chắc.

Test: từng quy tắc có ca đúng, ca không áp (RAW không có ký hiệu đó), ca dòng căn không chắc. Đo offline bằng Q1.1 trên **cả 28 chương**: bao nhiêu % dòng owner sửa được phần chuẩn hóa này giải quyết, và **0 dòng xa FINAL hơn** là yêu cầu (nếu có, sửa quy tắc hoặc bỏ quy tắc đó). Kết quả này không tốn tiền và quyết định có đưa chuẩn hóa vào luồng hay không.

### Q1.3 — Quality Core v2, gợi ý theo dòng, guard

- Viết lại Quality Core theo chuẩn owner (mục 1.3, nhóm D–F): RAW quyết định ký hiệu, cấu trúc dòng và nghĩa; dịch mọi phần tiếng Nhật còn sót trừ tên giữ nguyên theo glossary; ngôi kể và xưng hô theo hồ sơ pronoun; thuật ngữ theo glossary; sửa mọi dòng sai nghĩa/sai sắc thái/không tự nhiên; giữ nguyên dòng đã đúng. Bỏ câu "sửa tối thiểu" và "giữ ký hiệu" kiểu cũ.
- Prompt E nhận DRAFT **đã chuẩn hóa** (Q1.2) và mục "Điểm app phát hiện" (dòng + loại, không kèm đáp án).
- Guard mới (tất định, sau E): dòng nào bản app sinh thêm kana/Hán mà dòng DRAFT/RAW-glossary không có → trả dòng đó về DRAFT đã chuẩn hóa và ghi chú; `?`/`!` bị đổi sang toàn khổ khi DRAFT dùng nửa khổ → trả về nửa khổ. Áp chuẩn hóa Q1.2 lần nữa lên kết quả.
- Test: guard với ca `三`, `？` của N6 (tổng hợp lại bằng văn bản tự viết); prompt có đủ mục và không chứa FINAL.

### Q1.4 — Replay offline và chuẩn bị A/B

- Chạy lại cấu hình mới trên **response N6 đã lưu** chỉ cho phần app (chuẩn hóa + guard) để chứng minh guard chặn `三`/`？` mà không đổi dòng khác.
- Chuẩn bị manifest A/B (chưa chạy): cấu hình `C0` (V1.1 hiện tại, luna), `C1` (V1.2, luna), `C2` (V1.2, model mạnh hơn). Liệt kê 2–3 model OpenRouter ứng viên với giá input/output hiện hành, ước tính USD mỗi chương từ prompt thật của dev, worst-case reservation, tổng.
- Build qua wrapper (code > bản cao nhất đã archive), cài chỉ `emulator-5554`, test host + instrumented Editorial API như 4A.

**Dừng sau Q1.4**, báo cáo; Q1.5 cần owner duyệt D-Q1.

### Q1.5 — A/B live (chỉ khi owner duyệt D-Q1)

1. Dev 001–008: `C0` (bỏ 001–003 vì đã có N6), `C1`, `C2`. Chấm bằng Q1.1. Chọn cấu hình tốt nhất theo `fix_recall` cao nhất với `regression` không tăng và `hard` sinh thêm = 0.
2. Holdout 010, 013, 016, 019, 022, 025: chạy cấu hình tốt nhất và `C0`. Không chỉnh gì giữa dev và holdout.
3. Cổng đề xuất (owner chỉnh được): trên holdout, cấu hình tốt nhất có `hard` sinh thêm = 0, không truncation; `fix_recall` (gần FINAL hơn) ≥ 50%; số dòng app sửa mà xa FINAL hơn ≤ 2% số dòng; độ giống FINAL tăng ở ≥ 5/6 chương.
4. Xuất bản app của 3 chương holdout (do coordinator chọn theo số liệu, không theo điểm cao nhất) sang `D:\P5E-private\q1-outputs\` để **owner đọc và quyết định chấp nhận** — đây là cổng 3 chương FINAL thay cho N6.

### Bằng chứng

`docs/EDITORIAL_API_V1_Q1_EXECUTION_<date>.md`: commit, test, APK, bảng Q1.1 theo chương (chỉ số), kết quả chuẩn hóa trên 28 chương, guard replay, manifest A/B và ngân sách; sau Q1.5: bảng dev/holdout, call/token/USD, ledger. Cập nhật §10, snapshot, BUILD_STATE. Push sau mỗi gói, không force-push.

## 5. Quyết định cần owner

| ID | Nội dung | Khuyến nghị |
|---|---|---|
| D-Q1 | Cho phép Q1.5 sau khi Q1.4 PASS: ledger mới `Q1-<date>` trần **USD 2.00**, dev 8 + holdout 6 chương như mục 4, model `C2` chọn trong danh sách Q1.4 với ước tính ≤ USD 0.15/chương; chỉ emulator | Duyệt trước để Codex chạy liền sau Q1.4; nếu ước tính vượt trần thì Codex dừng và hỏi |
| — | Dùng 28 bản FINAL làm reference đánh giá (không đưa vào prompt) | Mặc định theo quy tắc hiện có, không cần duyệt thêm |

## 6. Quyết định đã nhận

Owner (chat, 2026-10-07): "duyệt D-Q1".

Phạm vi được phép: Q1.5 chạy ngay sau khi Q1.4 PASS, không cần hỏi lại. Ledger mới `Q1-<date>`, trần **USD 2.00**; dev 001–008 (C0 bỏ 001–003 vì đã có N6, C1, C2) và holdout 010/013/016/019/022/025 (cấu hình tốt nhất + C0); `C2` là một model trong danh sách Q1.4 có ước tính ≤ USD 0.15/chương; trần chương theo ước tính worst-case của từng cấu hình; chỉ `emulator-5554`, không pilot, không chunk-pair. Dừng và hỏi owner nếu: tổng ước tính hoặc reservation vượt USD 2.00, không có model ứng viên ≤ USD 0.15/chương, UNKNOWN cost, lỗi hạ tầng, hoặc cần đổi tập dev/holdout. Không chỉnh luật/prompt/ngưỡng giữa dev và holdout. Sau holdout: xuất 3 chương sang `D:P5E-privateq1-outputs` cho owner đọc và dừng.
