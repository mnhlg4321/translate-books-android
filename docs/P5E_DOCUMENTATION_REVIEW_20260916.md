# Rà soát tài liệu P5E — 2026-09-16

> Historical checkpoint; not current execution authority. Current audit: docs/P5E_EXECUTION_AUDIT_20260917.md. Current continuation request: docs/P5E_NEXT_WORK_REQUEST_20260917.md. Do not repeat completed SQL repair or test-package installation from this document.

Phạm vi: rà soát tài liệu và bằng chứng cục bộ tại HEAD
`31a09d02323cd317a9089411901849e6d51b8f66`. Kết luận hiện hành là
`LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED`: không build APK, ADB, thiết bị,
credential hoặc provider. JVM compile/test offline được phép trong request mới.

Các tham chiếu dòng dưới đây mô tả bản baseline trước đồng bộ của parent;
không dùng số dòng đó để suy nội dung current. Kết luận cuối và next action
lấy từ `P5E_READINESS_AUDIT_20260916.md` cùng request hiện hành.

## Phát hiện chính

1. **Canonical/state/snapshot/proposal còn giữ nhãn local GREEN cũ trong khi
   behavioral collector đã có RED mới.**
   `EDITORIAL_RECOVERY_V4_18.md:3-12`, `BUILD_STATE.md:3-8,31-41`,
   `WORKSPACE_SNAPSHOT.md:10-19`, `P5E_RAW_AUTHORIZATION_PROPOSAL.md:3-6`
   đều ghi H1–H4 đã đóng ở mức offline, `F3_LOCAL_EVIDENCE_CHAIN_GREEN`, owner
   packet `PENDING`, A4.3/RAW/P6 chưa sẵn sàng. Nhưng source thật tại
   `scripts/p5e-raw-live-supervisor.ps1:1467-1517,1564-1570` có ba lỗi query/
   parser/NULL được tái hiện; vì vậy các nhãn H2/H4/F3 phải hạ về
   `LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED` cho đến khi behavioral fixture
   pass. `P5E_NEXT_WORK_REQUEST.md` hiện đã được thay bằng request bounded này,
   không còn là kế hoạch H1–H4 cũ.

2. **Tuyên bố H3/F3 “GREEN” có điều kiện bằng chứng chưa chạy golden test.**
   `docs/P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json` ghi
   `goldenSerializerTest.sourceTestPresent=true` nhưng `executed=false`, lý do
   `explicit_no_build_boundary`; cùng file ghi H3 `GREEN_HOST_CONTRACT` dựa
   mutation matrix synthetic. Proposal `P5E_RAW_AUTHORIZATION_PROPOSAL.md:56-62`
   và snapshot `WORKSPACE_SNAPSHOT.md:11-14` đã nói rõ synthetic chỉ là
   regression-only. Tuy nhiên acceptance criteria của chính work request
   `P5E_NEXT_WORK_REQUEST.md:34-38` yêu cầu golden bytes do production
   serializer tạo được verifier chấp nhận. Vì vậy “local evidence chain green”
   chưa thể dùng ở thời điểm này; phải chạy targeted JVM compile/test offline,
   không cần owner approval, APK build hay thiết bị, rồi cập nhật result/hash.

3. **“Ready for owner decision” hiện bị treo bởi RED behavioral mới.** JSON result
   cũ có
   `readinessProbe.allFourBlockersResolved=true`, `readyForOwnerDecision=true`,
   `readyForDispatch=false`; `ownerProvenance.status=PENDING` và
   `liveState.a4_3=NOT_ISSUED`; nhưng sau RED mới, owner packet không nên được
   coi là ready for review. Câu chuẩn tạm thời là: “local behavioral validation
   failed; chưa owner-ready, chưa dispatch-ready; F1 permission vẫn là bước
   sau”. Các phần lịch sử (`P5E_READINESS_REAUDIT_20260915.md:1-18`) không nâng
   được current readiness.

4. **Nguyên nhân overthinking chủ yếu là topology tài liệu và status lặp, không
   phải thiếu thêm gate kỹ thuật.** Audit cũ đã chỉ ra trực tiếp các bản current
   lẫn lịch sử, nhiều next-action và lệch workflow tại
   `P5E_AUDIT_20260914.md:65-94`; request cũ nay đã được thay bằng kế hoạch
   48 bước chỉ cho SQL/collector/golden. Cách bounded:
   coi `EDITORIAL_RECOVERY_V4_18.md` + `BUILD_STATE.md` + snapshot + local JSON
   là authority current; chỉ mở lại các gate liên quan khi query/parser/golden
   test thay đổi hoặc fail. Không reimplement H1–H4.

5. **Readiness chưa đồng nghĩa P5/P6.** Các current banner đều đúng khi giữ
   `RAW_NOT_RUN`, `RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED`, `P6_NOT_READY`
   (`EDITORIAL_RECOVERY_V4_18.md:6,135-172`; `BUILD_STATE.md:31-41`). Không có
   bằng chứng cục bộ nào cho phép nâng thành RAW accepted, P5 exit hoặc P6
   ready. `P5E_RECONCILIATION_RECORD.md:3-6` cũng ghi rõ các phần thân lịch sử
   có thể xung đột banner và phải đọc theo banner hiện hành.

## Next steps có giới hạn

- Chạy request mới theo thứ tự refreeze helper hash → cập nhật command pin →
  tính command hash → cập nhật packet/provenance table; không bắt buộc giữ các
  hash cũ sau sửa.
- Chạy SQLite behavioral fixtures và targeted golden JVM test offline; không cần
  owner approval. Nếu fail, giữ `LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED`.
- Chỉ sau local PASS mới chờ owner cung cấp F1 fingerprint độc lập và quyền
  account/readback/RAW; không dispatch từ trạng thái hiện tại.

## Kiểm tra bổ sung: “executable collector” chưa được behavioral-QA

Các tài liệu hiện hành mô tả collector như đã đóng H2/H4 dựa trên static
source mapping, self-test và typed-stop fixture (`P5E_LOCAL_EVIDENCE_CHAIN_RESULT_20260915.json:evidence.h2/h4`).
Rà soát source cho thấy cần hạ mức claim cho tới khi chạy fixture SQLite trong
memory hoặc file tạm:

- `scripts/p5e-raw-live-supervisor.ps1:1467-1470` phát `SELECT 'SCHEMA' ...
  (SELECT user_version)`. SQLite không cung cấp `user_version` như cột trong
  SELECT này; pragma phải được đọc bằng cú pháp pragma phù hợp. Query có thể
  fail trước khi parser được kiểm tra.
- `:1506-1517` tạo `LINEAGE` với tag cộng 11 giá trị (tổng 12 cột), trong khi
  parser `:1570` bắt buộc 17 cột. Đây là mismatch hành vi query→parser, dù
  static count/fixture không bắt được.
- `:1482-1489` nối nhiều trường nullable bằng `||`. Với attempt ở trạng thái
  `CLAIMED`/`RECOVERY` mà `report_bytes` hoặc `receipt_bytes` còn NULL, toàn
  bộ dòng `ATTEMPT` có thể thành NULL và bị ẩn khỏi readback; điều này phá mục
  tiêu typed recovery/unknown.

Đây là lỗi boundary cụ thể, không phải lý do xin quyền device/provider. Do đó
  `H2/H4 resolved` hiện chỉ nên ghi là **static/offline contract evidence**;
  chưa có behavioral PASS của collector trên SQLite thật. Không báo mismatch
  INPUT: `:1478-1481` phát 7 cột gồm tag + 6 giá trị và parser `:1566` kỳ vọng
  đúng 7 cột.

## Next request bounded được đề xuất

Mở một local-only repair/test nhỏ, không build APK và không cần owner/device:

1. Sửa query SCHEMA, LINEAGE và cách serialize NULL theo schema hiện hành; giữ
   field order/contract rõ ràng. `metrics_json=''` là hợp lệ vì cột NOT NULL;
   một blob NULL đơn lẻ là negative inconsistency, còn cả report/receipt NULL
   là trạng thái hợp lệ cho CLAIMED/RECOVERY.
2. Tạo SQLite fixture tạm có các trạng thái `UNUSED`, `CLAIMED`, `RECOVERY`,
   report/receipt NULL hoặc đầy đủ; chạy chính `Get-P5EConsistentDatabaseReadback`
   qua sqlite3 thật, rồi feed output vào parser. Assert số cột, row identity,
   lineage counts và việc NULL không làm mất row.
3. Bắt buộc chạy targeted `EditorialP5PilotExecutionBoundaryTest` (golden
   production serializer → verifier) trong cùng local test request. Đây là
   Java/unit test, không phải APK build, không cần ADB/device/credential/provider.
4. Kiểm cả report và receipt sai bundle/predecessor; dù hai blob nhất quán với
   nhau, verifier vẫn phải reject theo expected identity.
5. Chỉ sau khi query→parser fixture và golden test có command/result/hash cụ thể
   mới nâng H2/H4/H3 thành behavioral local PASS; nếu fail, giữ F3 repair-required.
   Owner provenance/RAW permission vẫn là bước sau.
