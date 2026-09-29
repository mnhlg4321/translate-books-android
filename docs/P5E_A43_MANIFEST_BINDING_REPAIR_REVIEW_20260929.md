# P5E A4.3 manifest-binding repair — final offline review

Review date: `2026-09-29`

Runtime candidate: `b69cfcd7f397f7a4cd5aa2e7aae7b3f463032bc1`

Verdict:

`PASS / 0 BLOCKER / 0 HIGH / 0 MEDIUM / 0 LOW / OWNER_REVIEW_REQUIRED / NOT_DISPATCHED / P6_NOT_READY`

## Finding đã sửa

Owner launch dùng packet trước dừng tại
`MANIFEST_BINDING_LITERAL_MISSING_STOP:p5e.a43.owner-decision-receipt.v1`.
Command dừng trong `Assert-ManifestBinding`, trước receipt read, decision/
receipt reservation, event creation và ADB. Đây là packet documentation-
binding defect, không phải key, account, USB, package hoặc provider failure.

Manifest mới chứa đủ exact receipt schema, stdout `4194304` và stderr
`1048576` byte. Command pin đúng manifest mới. QA dependency/binding hiện so
toàn bộ literal set mà command live kiểm tra, nên cùng mismatch không thể tiếp
tục lọt qua suite hiện tại.

## Bằng chứng phản biện

1. Main repair QA `21/21 PASS`; test binding mới nằm trong
   `GREEN-dependency-hash-order-and-command-binding`.
2. Atomicity/environment `28/28`, binding `262/262`, regression `175/175`, DB
   host readback `56/56` không regress.
3. Pre-reservation smoke trên exact repaired command đi qua manifest gate và
   dừng đúng tại `P5E_OWNER_RECEIPT_PATH_MISSING_STOP`.
4. Smoke không cung cấp receipt hoặc expected digest, vì vậy không thể reserve
   decision/event và không thể gọi ADB.
5. Exact clean archive của commit `b69cfcd7` tái chạy toàn bộ suite và PASS;
   không dùng workspace fallback/cached result.
6. Manifest SHA-256 là
   `DF239F267245B9636C0A30367DCF07AFA0D5FCBAC1229163C87CA2FE3932236C`;
   command SHA-256 là
   `FDF60C2478275160654EFD548FE59340CC0E58826929DACD072B9C3AD872147F`.
7. Helper, guard, exporter, bridge, toolchain, APK/source/certificate và serial
   không đổi.

## Phản biện rủi ro còn lại

- Approval/receipt cũ không bị consume, nhưng không được chuyển sang packet mới
  vì exact manifest/command hashes đã đổi.
- Owner root cũ phải giữ nguyên như historical pre-reservation evidence; không
  chạy lại, sửa hoặc cleanup.
- Packet mới chưa được owner approve và chưa chứng minh live Before/RAW/After.
- Offline PASS không cho phép tự tạo receipt, mở event hoặc mở P6.

Không còn finding offline trong phạm vi manifest-binding repair. Next action
duy nhất là owner review exact packet mới và chọn `STOP_NO_EVENT` hoặc cấp một
approval mới bind các hash mới. Không reuse receipt/launcher/decision cũ.
