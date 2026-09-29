# Yêu cầu làm việc tiếp theo — owner review packet sau manifest-binding repair

## Trạng thái

`OFFLINE_MANIFEST_BINDING_REPAIR_PASS / PREVIOUS_APPROVAL_NOT_CONSUMED / PREVIOUS_RECEIPT_NOT_CONSUMED_SUPERSEDED / EVENT_NOT_OPENED / ADB_NOT_CALLED / OWNER_REVIEW_REQUIRED_FOR_NEW_HASHES / P6_NOT_READY`

Owner launch ngày 2026-09-29 dừng tại:

`MANIFEST_BINDING_LITERAL_MISSING_STOP:p5e.a43.owner-decision-receipt.v1`

Command dừng trước khi đọc/consume receipt, trước decision reservation, event
directory và ADB. Full key không xuất hiện trong log. Parent Process bindings
đã được clear trong `finally`.

Private receipt/launcher cũ dưới
`D:\P5E-private\p5e-a43-owner-live-20260929-073456445-c9a89b709b0143569451550c4e859779`
là `NOT_CONSUMED` nhưng `SUPERSEDED_NOT_REUSABLE`, vì manifest/command hashes
đã đổi sau repair. Không xóa, rename, sửa hoặc chạy lại thư mục này.

## Nguyên nhân và sửa chữa

Command có một `Assert-ManifestBinding` fail-closed kiểm tra các literal bắt
buộc. Manifest cũ thiếu exact receipt schema và hai capture byte-cap literals.
QA cũ chỉ kiểm command pin manifest/dependencies, chưa so toàn bộ needle set
của live assertion với manifest nên để lọt mismatch.

Repair đã:

1. thêm `p5e.a43.owner-decision-receipt.v1` vào manifest;
2. thêm stdout `4194304` và stderr `1048576` byte vào manifest;
3. mở rộng regression hiện có để kiểm toàn bộ manifest-binding literal mà
   command live yêu cầu;
4. repin command tới manifest mới;
5. chạy main QA `21/21 PASS` cùng atomic `28/28`, binding `262/262`, regression
   `175/175` và DB `56/56`;
6. chạy pre-reservation smoke: manifest gate PASS và command dừng đúng tại
   `P5E_OWNER_RECEIPT_PATH_MISSING_STOP`, trước receipt/reservation/event/ADB;
7. tái dựng từ exact Git archive `b69cfcd7f397f7a4cd5aa2e7aae7b3f463032bc1`: PASS.

## Exact packet mới

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Runtime candidate: `b69cfcd7f397f7a4cd5aa2e7aae7b3f463032bc1`.
- Serial: `15e84958`.
- Manifest SHA-256:
  `DF239F267245B9636C0A30367DCF07AFA0D5FCBAC1229163C87CA2FE3932236C`.
- Command SHA-256:
  `FDF60C2478275160654EFD548FE59340CC0E58826929DACD072B9C3AD872147F`.
- Helper SHA-256:
  `4DAD6E30928DD0D78396BDDFA57AFA1053E75481524D2EA05659227869C57444`.
- Runtime guard SHA-256:
  `C31217CDBD40F22DB9A74AFB529B1ECC33F725F485ECD462992073235F59ADD8`.
- Exporter SHA-256:
  `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99`.
- SQLite bridge SHA-256:
  `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111`.
- Toolchain SHA-256:
  `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8`.
- Certificate SHA-256:
  `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.

## Bằng chứng

- Repair QA:
  `docs/P5E_A43_MANIFEST_BINDING_REPAIR_QA_20260929.json`, SHA-256
  `092936E7FFB23DDF350CE86927A660D54A007EFF5A9DBE12AAFBABD6474DB234`.
- Pre-reservation smoke:
  `docs/P5E_A43_MANIFEST_BINDING_PRE_RESERVATION_SMOKE_20260929.json`,
  SHA-256
  `2876329BFEFF4076DF72FF63E1F2E93BA3A982F3A4AF9EC1DA9EF0DBB96BE4EC`.
- Clean archive:
  `docs/P5E_A43_MANIFEST_BINDING_REPAIR_ARCHIVE_CLEAN_20260929.json`, SHA-256
  `98E70897496122BCDA705D2E45D453564647EEDD306C22910086E48EDEA43F28`.

All live counters trong repair/smoke/archive là `0`: ADB, device, provider,
credential read, DB write, build/install, RAW và redispatch.

## Owner cần quyết định

Packet byte đã đổi nên quyết định cũ không được chuyển sang packet mới, dù
decision/receipt cũ chưa bị consume. Owner chọn đúng một kết quả:

1. `STOP_NO_EVENT`; hoặc
2. `APPROVE_ONE_FRESH_EVENT` bind exact toàn bộ hash ở trên.

Nếu approve, scope giữ nguyên: đúng một fresh A4.3 event, read-only Before/
After, account comparison memory-only, một fresh authorization, tối đa một
RAW/GLOSSARY provider call, allowlisted DB writes, budget `USD 0.05`, không
retry/fallback/repair-in-event/RECONCILE/cleanup/restore/redispatch/
build-install/P6.

## Trình tự sau approval mới

1. Tạo owner root, decision ID, receipt và launcher hoàn toàn mới.
2. Receipt phải dùng manifest/command hashes mới trong tài liệu này.
3. Nạp expected digest từ full key đang lưu trong app vào đúng Process; không
   gửi key/digest cho agent và không in giá trị.
4. Bind fresh receipt path/hash và command hash mới trong cùng Process.
5. Kiểm presence/shape và exact file hashes; không tạo event nếu check fail.
6. Chạy command đúng một lần.
7. Nếu command tạo reservation rồi dừng ở bất kỳ typed stop nào: đóng event và
   không retry.
8. Sau terminal, clear Process bindings nhưng giữ nguyên receipt/launcher/event
   evidence; không cleanup hoặc reuse.

Không được dùng lại receipt, launcher, decision ID, command hash hoặc owner
directory của lần dừng trước.
