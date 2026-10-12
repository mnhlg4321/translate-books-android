# Yêu cầu làm việc + đánh giá sau account MATCH — 2026-09-24

## Quyết định hiện tại

Đủ điều kiện đóng bước account và chuyển sang hoàn thiện gói A4.3 offline. CHƯA đủ điều kiện dispatch A4.3/RAW hoặc chuyển P6. Đây là yêu cầu sửa tích hợp cụ thể trong P5E hiện hữu, không tạo phase/branch/release mới và không phải quyền thao tác device/provider.

Baseline: 356508d28bc7ff7b04a886335a67f7124859cf9f; branch feature/v4.18-p5e-runner-repair-20260917; workspace D:\App Translate Books.

## Kết quả đã kiểm chứng và nguyên nhân vòng lặp

Account event route-corrected-01 trả MATCH, preflight 7/7, một runner launch, exit0 và identity/terminal/capture/redaction hợp lệ. Đã hash-check đủ ba receipt theo P5E_ACCOUNT_IDENTITY_ROUTE_CORRECTED_EVENT_RESULT_20260924.json; không đọc stdout/stderr, key hoặc expected value. Đây là evidence tại thời điểm event, không phải bảo đảm cấu hình/DB bất biến mãi mãi.

Chuỗi lỗi trước đây có ba nhóm: fixture/parser không mô phỏng đủ layout/lifecycle; test account gộp route/digest/exception thành MISMATCH; tài liệu giữ nhiều nhánh “current/next” của thời điểm cũ. Sửa cấu hình rồi MATCH chứng minh predicate hiện đã đúng, nhưng receipt cũ không chỉ ra chính xác trường cấu hình nào từng sai. Không diễn giải lịch sử thành key lỗi.

Vướng mắc còn lại được xác định bằng file, không suy đoán:
1. RAW command đang dùng đường dẫn nested worktree và helper 364A6A…6FFE7. Helper ở đúng đường dẫn cũ thực sự còn hash đó: đây là lựa chọn revision cũ, không phải kết luận file hash hỏng. Helper root đã sửa là B491DD…31897.
2. Manifest/RAW command vẫn pin test APK 57EC99…FDEA; receipt account mới dùng test APK 058BE8…158E8. Không được chạy RAW với sự khác biệt này hoặc đổi hash cho qua. Cần chứng minh artifact/source/entrypoint tương thích và cập nhật provenance đồng bộ.
3. Helper còn source commit d51b7f3c cho RAW test; current RAW Java file không diff với commit đó, nhưng điều này chưa chứng minh toàn bộ test APK replacement có thể được dùng như artifact RAW.
4. UNUSED authorization/fresh DB tuple ở A4.2 là evidence lịch sử. MATCH không đọc DB và không làm mới các điều kiện đó.
5. Chưa có quyền read-only collector trước/sau, runtime authorization, một RAW/GLOSSARY egress và các DB writes allowlisted. Quyền account đã tiêu thụ không bao gồm chúng.

## Phạm vi và đầu ra bắt buộc của yêu cầu tiếp theo

Hoàn thiện ngay các thay đổi host/docs cần thiết để tạo MỘT packet A4.3 có thể review; không chỉ viết thêm một plan rồi kết thúc. Dùng Luna cho kiểm tra artifact/provenance và phản biện. Không device/ADB/provider/credential/DB live/build/install trong công việc offline này.

Đầu ra: manifest + command + helper + artifact/source provenance thống nhất; QA nhắm đúng phần thay đổi; một bảng gate PASS/PENDING; một quyết định owner cho phạm vi live chính xác nếu packet đủ điều kiện. Nếu còn thiếu bằng chứng thì chỉ ra file/contract cụ thể, không xin “duyệt để tiếp tục nghiên cứu”.

## Các bước nhỏ theo thứ tự

1. Đọc canonical → BUILD_STATE → snapshot; xác nhận HEAD/branch/status. Giữ thay đổi có sẵn, không reset nested worktree hay loader đã pin.
2. Xác minh hash result MATCH và ba receipt; đối chiếu account result/launch/identity. Dùng lại evidence này, không chạy thêm account-only event hoặc xin lại key/nhãn/provenance.
3. Kiểm kê các thành phần tham gia RAW: command, manifest, helper, artifact contract, collector SQL/parser, verifier, live AndroidTest method và APK/source ZIP. Lập duy nhất bảng path/hash/source commit; tách lịch sử khỏi lựa chọn thực thi mới.
4. PARITY_PENDING: chưa xác minh đủ bundle BUILD_INFO/source ZIP/backup của test APK 058BE8… trong audit này. APK pull-back được tham chiếu tại D:\P5E-private\p5e-account-check-install-replacement-20260917-113601720-80675f1e5c4746da97f0a884ee319472\installed-test-package.apk; source ZIP hash theo hồ sơ là 5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F, phải tìm và xác minh path/bytes cùng BUILD_INFO/backup trước khi chọn. Dùng bundle đã xác minh để kiểm package/runner/certificate, source archive, RAW live method và các dependency ảnh hưởng request/DB/budget. Chứng minh parity với artifact gốc, không chỉ so một file Java hoặc thấy account method PASS.
5. Nếu parity đủ: chọn immutable APK/source archive đã xác minh tương ứng bản account replacement, đổi metadata provenance đúng source artifact, giữ riêng mốc source contract RAW nếu cần. Không lấy trạng thái đã cài làm authority offline: identity của package đang cài chỉ được xác minh lại trong preflight A4.3 read-only được duyệt sau này. Không gắn source commit d51b7f3c giả cho toàn APK mới. Nếu parity không đủ: dừng lựa chọn artifact, mô tả chính xác khác biệt; không tự build/install/restore test cũ.
6. Chọn một helper root đã sửa làm runtime authority; loại đường dẫn nested runtime cũ khỏi command mới. Giữ old hashes/receipts như lịch sử trong Git. Kiểm full instrumentation component, transport, redaction, timeout và path/reparse guard; không sửa rộng khi không có lỗi.
7. Đối chiếu manifest với helper/parser: production code207/certificate, APK đã chọn, phase L1_RAW_DISCOVERY, project2/chapter001/selector, attempt/request/envelope/body/route/binding/run/pack/profile identities. Không đổi identity chỉ vì đổi ngày, không tự tạo authorizationId mới.
8. Giữ budget đã chốt: primary calls1, repair0, network retries0, input100000, output4096, total104096, cost capUSD0.05, execution120000ms; authorization validity180000ms và host observation240000ms. Timestamps tạo đúng điểm ngay trước dispatch được duyệt, không từ lúc prepare/collect kéo dài. Không tự nâng cost cap khi provider không đáp ứng; usage/cost thiếu là UNKNOWN, không phải0.
9. Xác định evidence nào cần làm mới trong collector live trước claim: package/hash/certificate, route/account check trong live method, schema/integrity/FK, settings invariants, fresh tuple/unused authorization và exact request. Không lấy account MATCH thay DB readback. Liệt kê những kiểm tra này vào phạm vi owner duyệt, không chạy trước để lấy bằng chứng.
10. Kiểm dòng đời command PrepareEvent → Before readback → tối đa1 Dispatch → After/recovery readback → VerifyOutcome. Bắt lỗi exception/process/timeout để bảo toàn same-event recovery sau khi dispatch có thể đã bắt đầu; không chỉ bảo đảm recovery khi có exit code bình thường. Không launch lại hoặc đổi event để vượt marker.
11. Kiểm collector đọc nhất quán WAL, giữ NULL sentinel; verifier kiểm allowed row set, atomic claim, durable COMMITTED/report/receipt exact bytes/hash/length/pair, token/cost/time và lifecycle. Không yêu cầu toàn DB hash trước/sau giống nhau vì RAW cho phép write có giới hạn.
12. Chạy QA đúng phần đổi: pin/path/selected artifact reject; full RAW command orchestration với fake process; prior SQL/golden evidence chỉ dùng lại khi dependency không đổi. Fixture phải mô phỏng actual producer contract; có success và failure sau dispatch, timeout/USB loss, missing readback, wrong identity, malformed/oversized output, unknown cost, consumed authorization, duplicate claim và no-redispatch. Không chạy toàn suite hoặc build APK vì đổi tài liệu.
13. QA độc lập/phản biện Luna: tìm trường hợp gate PASS nhưng artifact sai; recovery bị bỏ qua; secret lọt argv/log; command pin kiểm muộn; expiry bị refresh; retry ngầm; RESULT_OK nhưng DB chưa COMMITTED. Sửa lỗi tái hiện được ngay cùng branch trước bàn giao, không chuyển thành “review pending” vô hạn.
14. Đóng băng theo thứ tự artifact/source selection → manifest → helper → command → hash/provenance. Command phải kiểm hash chính nó bên ngoài trước invoke và mọi dependency trước sensitive/device action. Hash phải lấy từ bytes cuối, không chép pin lịch sử; không hash-self-reference.
15. Đồng bộ proposal, canonical/state/snapshot/checklist: account MATCH đóng; packet kỹ thuật PASS hay thiếu gì; quyền live vẫn NOT_ISSUED. Đánh dấu các banner cũ là lịch sử, không xóa receipt bất biến. Snapshot chỉ một Next action.
16. Khi tất cả offline gates PASS, trình MỘT quyết định owner cho packet có hash cuối: collector read-only trước/sau + memory-only account check trong live method + tối đa một RAW/GLOSSARY call + runtime authorization + đúng DB writes allowlisted. Không yêu cầu owner tự tìm hash, viết provenance lại hoặc duyệt từng substep. Chuẩn bị xong packet mới hỏi quyết định; công việc này không thực thi quyết định đó.

## Phạm vi live cần trình, chưa được cấp

Serial15e84958; production giữ nguyên. Chỉ RAW/GLOSSARY ra model, DRAFT/PRONOUN hidden; provider/model/upstream giữ openrouter/openai-gpt-5.6-luna/openai theo giá trị literal trong code/manifest (model chính xác: openai/gpt-5.6-luna). Không fallback, schema repair, retry, RECONCILE, settings mutation, cleanup hay restore.

DB writes chỉ exact attempt + authorization receipt atomic claim, allowlisted lifecycle, recovery status hoặc COMMITTED report/receipt/metrics của tuple được duyệt. Các write này phải được nêu rõ trong quyết định; không dùng quyền “read-only account” để làm chúng.

## Điểm dừng và phân loại kết quả

- Pin/route/account/freshness/expiry lỗi trước claim: không dispatch; trạng thái chưa consumed chỉ được kết luận từ evidence.
- Provider error/timeout/USB loss sau possible dispatch: same-event readback/recovery trong quyền đã cấp; không retry, không đoán provider chưa nhận request.
- Output sai schema/semantic hoặc usage/cost thiếu: không accept RAW, không tự sửa nội dung hoặc tăng budget.
- Instrumentation OK nhưng durable state khác COMMITTED hoặc bytes/metrics không hợp lệ: RAW_NOT_ACCEPTED.
- COMMITTED + artifact/metrics/readback hợp lệ: có thể ghi RAW predecessor accepted; không tự mở P6. Đánh giá riêng tiêu chí P5 và RECONCILE nếu canonical yêu cầu.
- Thiếu readback/receipt: ACCEPTANCE_NOT_PROVEN/UNKNOWN; giữ trạng thái, không cleanup hoặc redispatch.

## Chống vòng lặp và điều kiện hoàn tất

Không account retry, không nghiên cứu lại parser đã PASS, không plan mới thay cho sửa tích hợp, không đổi branch/release, không tạo ma trận QA chỉ phản chiếu code. Chỉ mở lại việc cũ khi có file diff hoặc counterexample cụ thể.

Hoàn tất công việc tiếp theo khi packet RAW thống nhất artifact/host/source và QA được chứng minh, hoặc chỉ còn một bất tương thích artifact cụ thể cần owner quyết định. Chưa đủ để tuyên bố P5/P6 PASS. Báo cáo chỉ: gate đã đóng, gate còn thiếu, evidence/hash cuối, action counters và một Next action.

Phạm vi rà soát phiên lập yêu cầu: inventory tài liệu tracked và đọc sâu canonical/state/checklist, chuỗi account receipts, RAW manifest/proposal/command, helper và hợp đồng RAW/P5/P6 liên quan; không tuyên bố đọc từng dòng toàn bộ tài liệu lịch sử.