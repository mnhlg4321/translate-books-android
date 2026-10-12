# Tiêu chí nghiệm thu đầu ra biên tập cuối

## 1. Mục đích và phạm vi

Tài liệu này là đặc tả nghiệm thu hỗ trợ cho việc hoàn thành L1–L3 và giao
toàn văn đã biên tập. Đây không phải kế hoạch mới và không mở thêm phạm vi
thực thi. Ưu tiên sản phẩm là bản edited text cuối đã được lưu, mở lại được
và xuất được sau khi qua L3; bản dịch đơn thuần, A4.3 PASS hoặc APK không tự
đáp ứng nghiệm thu.

Nguồn điều khiển trực tiếp:

- `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\CHATGPT\PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt`, các mục về chain, intermediate artifact, L1, Speaker Proof và release gate.
- `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\CHATGPT\WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt`, Section I (hợp đồng/input), Section IV (L1), Section V (L2), Section VI (L3/release), Section IX (Stop Receipt).
- `EDITORIAL_RECOVERY_V4_18.md`, bản canonical “hoàn thiện bản biên tập cuối” cập nhật 2026-10-01, đặc biệt Sections 1, 3, 5, 6, 7 và 9.

Nếu có khác biệt, Project/Workflow đã pin trong chain và recovery authority
hiện hành có ưu tiên tương ứng; không tự suy ra chapter, ngân sách hay quyền
thực thi còn thiếu.

## 1A. Hướng hiện hành: EDITORIAL_API_V1 (D-N1..D-N3 đã duyệt 2026-10-05)

Từ D-N2, tiêu chí **artifact** L1–L3 dưới đây (REPORT_L1, VI_L2, CHANGE_MAP_L2, ledger, Speaker Proof, Stop Receipt) chỉ còn áp dụng cho đường SAFE4 đóng băng và cho việc đọc lại bằng chứng lịch sử. Chúng **không** dùng để nghiệm thu `EDITORIAL_API_V1`. Tiêu chí **chất lượng** không bị hạ; mọi mục sau phải có bằng chứng, không suy từ fake provider:

1. Sản phẩm là bản văn cuối đã lưu, mở lại được sau khi process bị tắt, và xuất TXT UTF-8 có SHA-256 đọc lại bằng `final_text`. Bản cuối chỉ chứa văn bản biên tập.
2. Chất lượng đo trên A/B N5 (ma trận 24 run, 25 target mỗi nhánh, xem `EDITORIAL_API_V1_PLAN_20261005.md` mục 6): nhánh tốt nhất sửa đúng ≥ 15/25 target; 0 lỗi mới loại MEANING/OMISSION/NUMBER/NEGATION trên fixture sạch (control `fx-a02` không phải bằng chứng sạch: trừ lỗi đã có sẵn trong DRAFT đã phân xử); 0 truncation. Số đo "lỗi mới" là `NOT_MEASURED` cho đến khi phân xử độc lập xong.
3. Chấp nhận cuối: **3 chương thật** đại diện do chế độ đã chọn tạo ra, owner đọc và chấp nhận từng chương. Hiện **0/3** đã được chứng minh; P7 chưa đạt. Chạy fake provider, dry-run runner hay điểm `score_run.py` trên fixture gieo lỗi không được tính vào 3 chương này.
4. Mỗi run lưu model, route, contract revision, Quality Core SHA-256, source commit, APK version/code, call/token/USD và trạng thái chi phí (đã biết / chưa rõ). Một lượt gọi có kết quả không rõ (timeout sau khi gửi, 5xx) không được gửi lại tự động và không được ghi như chi phí 0.
5. Quyền live (provider thật) chỉ có khi owner duyệt D-N4 với danh sách run, trần và đơn giá cụ thể; tài liệu này không tạo quyền đó.


## 2. Cái gì được giao và cái gì chỉ là trạng thái nội bộ

Người dùng mặc định nhận bản văn cuối có thể dùng ngay; bản cuối chỉ chứa văn
bản biên tập, không lẫn report, JSON hay lời giải thích. QA receipt và pair
delta giữ nội bộ cho thực thi/kiểm chứng/phục hồi, chỉ giao khi người dùng yêu
cầu. Workflow Section VI vẫn quy định tối đa ba file cho payload nội bộ PASS:

1. `[ID]_FINAL_QA_[SERIES].txt`: chỉ bản dịch cuối;
2. `[ID]_QA_RECEIPT_[SERIES].txt`: manifest, counts/anchors, gates, preserved,
   QA changes và hai biên bản phản biện;
3. `[ID]_PAIR_DELTA_QA_[SERIES].txt`: chỉ khi có delta QA thật; nếu không,
   receipt ghi `PAIR_DELTA_QA=NONE`.

`REPORT_L1`, `VI_L2`, `CHANGE_MAP_L2`, ledger, Error/Change IDs, Speaker Proof,
manifest, hash/count, Stop Receipt và các bản ghi kiểm tra là trạng thái/evidence
nội bộ cần giữ để chứng minh chain. Chúng không mặc định là các gói giao riêng.
Intermediate output chỉ là trạng thái nội bộ; không biến chúng thành các
milestone giao hàng độc lập. Manifest, ledger, counts, anchors, hashes, diff,
proof và receipt là bằng chứng nội bộ bắt buộc dù không giao riêng.

`CONTENT_BLOCKED` là stop class cho xung đột nội dung có chứng cứ chưa thể
sửa, giữ hoặc hoàn nguyên an toàn. Nó chỉ chứng minh hành vi dừng đúng; không
phải kết quả biên tập thành công, không thay cho FINAL và không được tính vào
số chapter đạt nghiệm thu.

## 3. Chuỗi phase, input/output và chất lượng tối thiểu

### L1 — preflight rồi audit

Input normal là đúng `RAW + original DRAFT + GLOSSARY + PRONOUN`. Phải inventory
đúng file, đọc được bytes, kiểm parse/encoding/schema và pin identity/hash bắt
buộc trong app Android theo kế hoạch này. Glossary hợp lệ là schema 5 cột; Pronoun authoritative là
`from,speaker,target,self,call,scope,note` (7 cột). Pair Context là tùy chọn.

Sau preflight PASS, đọc raw trước draft, lập và đóng các raw unit/scene cùng
TG/SR/RC ledgers, Pair Records, speaker uncertainties, Error Ledger và protected
spans. Mọi candidate phải có trạng thái; preserved phải có exact anchor và
evidence limit. L1 không sửa draft.

Output bắt buộc là `REPORT_L1` có Source Manifest, summary, ledgers, records,
receipt evidence và preserved counts. Chain phải pin chapter/series/version,
revision và identity để L2 dùng lại đúng nguồn.

### L2 — edit có kiểm soát

Input phải là đúng RAW, original DRAFT, GLOSSARY và `REPORT_L1` cùng
manifest/version/revision/hash; effective statuses phải khớp L1. Thiếu hoặc sai
artifact là `INPUT_REQUIRED`, chưa được xuất VI_L2.

Đọc raw độc lập, tái lập candidate counts, sửa theo coverage/semantic/title/
glossary rồi relation và naturalness cục bộ. Mỗi edit có Error ID và Change Map;
đổi thoại bắt buộc có Speaker Proof dựa trên raw, listener và anchor trước/sau.
Không đủ evidence để chứng minh target tốt hơn thì `PRESERVE_DRAFT` exact span,
không tự block và không cấp canon.

Đóng L2 bằng reconstruct `DRAFT→VI_L2`, đối chiếu Change Map, chạy lại TG/SR/RC
và đọc liền toàn bản. Output nội bộ là `VI_L2`, `CHANGE_MAP_L2` và
`PAIR_DELTA_L2` chỉ khi có delta thật; VI_L2 chỉ chứa bản dịch.

### L3 — raw-first QA và release

Input phải thêm `REPORT_L1`, `VI_L2`, `CHANGE_MAP_L2` đúng chain. Trước khi dùng
PASS cũ làm evidence, đọc raw, dựng expected model và raw-side candidates; sau
đó đọc toàn VI_L2, reconcile nguồn trước và reconstruct `DRAFT→VI_L2`.

Phải chạy adversarial coverage và adversarial regression. Mọi QA edit có QA
Error/Change ID trước edit; receipt gộp QA Change Map; thoại vẫn cần Speaker
Proof. Sau bất kỳ sửa nào phải rebuild ledgers/diff/gates và đọc lại từ đầu.

Release numbers phải đồng thời bằng không: raw unit chưa xử lý, TG/SR/RC
candidate chưa xử lý, proven unresolved release conflict, unaccounted changed
anchor và protected-span regression. Preserved rows được báo riêng, có anchor/
evidence-limit, đã xử lý nhưng không tạo canon hay PASS proof.

QA receipt phải dẫn được manifest/identity, từng ledger và count, equations,
Pair IDs/status, actual diffs `DRAFT→VI_L2` và `VI_L2→FINAL`, probes/findings/
actions, preserved inventory, final-read order marker và app-generated FINAL
hash/count. Hash/count là bằng chứng bắt buộc; không hạ thành “nếu host hỗ trợ”.
Khi PASS, `Stop Receipt=NONE`.

## 4. Ba chapter đại diện

Nghiệm thu sản phẩm phải chứng minh ít nhất ba chapter đại diện hoàn thành
thành công L1–L3 và có FINAL hợp lệ, phù hợp `EDITORIAL_RECOVERY_V4_18.md`
Sections 1 và 5. Ba category đại diện là: (1) chapter ngắn, (2) chapter dày
thoại/xưng hô, (3) chapter dài gần giới hạn. Đây là category kiểm tra, không
phải tên chapter được chọn. Tên, nguồn, thứ tự và ngân sách cụ thể chưa được
owner chốt; không tự đặt thay.

Mỗi chapter đại diện phải đạt các tiêu chí giống nhau:

- có manifest nhận diện được RAW/DRAFT/chapter/series/version và chain revision;
- có L1 report đóng đủ raw units/TG/SR/RC, preserved và proof được đếm;
- có L2 final-read, actual diff và Change Map; mọi dialogue change có Speaker
  Proof, không có unaccounted change;
- có L3 raw-first re-audit, adversarial records, release numbers bằng 0 và
  receipt liên kết đến FINAL;
- FINAL là văn bản hoàn chỉnh của chapter, lưu được, mở lại giữ đúng nội dung,
  xuất được thành TXT UTF-8 và app tạo hash/count khớp file đã mở lại.

Mỗi chapter đạt đủ các điều kiện trên được ghi `FINAL_OUTPUT_ACCEPTED` cho
chapter đó. Một chapter dừng chỉ khi Stop Receipt typed nêu đủ class, reason
code, phase, blocking gate, evidence, affected scope, recovery, resume point và
retryability. Content stop là negative evidence riêng: dù stop đúng contract,
nó không thay thế bất kỳ một trong ba FINAL thành công. Uncertainty đã giữ bằng
`PRESERVE_DRAFT` không tự biến thành block.

## 5. Semantics dừng và bằng chứng lưu bền vững

L1 preflight fail thì chỉ xuất Stop Receipt; không semantic audit, partial report,
finding hay downstream gate. L2/L3 thiếu/sai predecessor hoặc identity thì
`INPUT_REQUIRED`; schema/receipt sai là `REPAIR_REQUIRED`; output bị cắt là
`RETRY_REQUIRED`; proven conflict không thể sửa/giữ/hoàn nguyên an toàn là
`CONTENT_BLOCKED`. Không dùng `BLOCKED`, `FAIL` hoặc `UNCERTAIN` đứng một mình.

Trước khi gọi là hoàn tất, lưu cùng evidence tối thiểu:

- file FINAL và QA receipt đúng tên/series/chapter;
- manifest và identity/count/hash do app tạo, cùng chain revision;
- receipt có final-read marker, diff/gate counts, preserved inventory và
  `Stop Receipt=NONE` nếu PASS;
- thao tác save thành công, lần mở lại đọc đúng FINAL, và lần export tạo đúng
  file có thể kiểm tra bằng app-generated hash/count và UTF-8/readback result.

Nếu save, reopen hoặc export không chứng minh được, trạng thái là chưa nghiệm
thu; không bù bằng content block, A4.3 PASS, APK, hay intermediate artifact.

## 6. Điều kiện kết luận

Kết luận `FINAL_OUTPUT_ACCEPTED` được ghi riêng cho từng chapter khi chapter đó
đạt criteria ở Section 4. Kết luận `PRODUCT_FINAL_OUTPUT_ACCEPTED` chỉ được
ghi khi có đủ ba chapter thành công, mỗi chapter thuộc một category đại diện,
toàn chain L1→L2→L3 đúng revision/identity, FINAL đã lưu–mở lại–xuất đúng và
app hash/count khớp. Content stop hợp lệ không làm tròn đủ ba chapter. Chapter,
ngân sách, mode nguồn và owner selection còn thiếu phải giữ nguyên là unresolved
owner decision; tài liệu này không tự cấp quyền để chọn thay.
