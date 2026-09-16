# P5E — owner provenance/input packet

Ngày: `2026-09-16`
Trạng thái local: `P5E_LOCAL_BEHAVIORAL_GATE_GREEN / OWNER_PACKET_PENDING / A4_3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY`.

## Owner input received — limited account-check scope

On `2026-09-16`, the owner attested that the API key currently stored in the
Android app belongs to the permitted OpenRouter account for P5E, and approved
one device-side memory-only account check whose only result may be
`MATCH`/`MISMATCH`. The scope explicitly excludes logging or exporting the key,
fingerprint or endpoint, provider calls, database writes and RAW dispatch.
No secret or fingerprint value is recorded here.

This input does not make the current pinned APK executable for that operation.
The pinned `EditorialP5EFreshRawLiveInstrumentedTest#authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn`
computes and compares the fingerprint, but then proceeds into DB/preflight and
`dispatchRaw`; it is not an account-only method. The current AndroidTest pin
has no separate memory-only `MATCH`/`MISMATCH` entry point, and no trusted
expected fingerprint is available through the process-only channel. Therefore
the check result is `NOT_EXECUTED`, not `MATCH` or `MISMATCH`; no device action
was taken.

The exact missing boundary is a separately qualified test-only account verifier
that loads settings in memory, compares to an owner-controlled process-only
expected value, emits only `MATCH`/`MISMATCH`, and has no DB/provider/RAW path.
Building/installing or repinning that test-only artifact is outside the prior
local authorization and is not performed by this input.

## Điều owner cần xem và cung cấp

Owner chỉ cần gửi một quyết định không chứa secret, gồm:

1. Nhãn operator/project/account dùng để xác định đúng tài khoản và thời điểm
   xác minh.
2. Nguồn provenance độc lập cho expected endpoint-account fingerprint: mã tham
   chiếu tới trusted enrollment/record và mapping account/key. Không gửi actual
   fingerprint, API key, raw endpoint, settings hay credential qua chat, Git,
   log hoặc command text.
3. Cho phép memory-only load/normalize/hash/compare account settings; kết quả
   chỉ là `MATCH`/`MISMATCH`, không lưu hai digest.
4. Cho phép read-only package/APK/certificate, settings, WAL-aware SQLite,
   attempt/authorization/lifecycle, report/receipt/metrics và allowlisted
   lineage readback.
5. Cho phép đúng một RAW/GLOSSARY L1 primary call với output cap/route/phase/
   bundle/predecessor/DB effects như manifest; không cho repair, retry,
   fallback, response healing hoặc RECONCILE trong event.
6. Xác nhận cửa sổ thời gian và người chịu trách nhiệm quyết định khi outcome
   là unknown/recovery. Đây là approval cho một event cụ thể, không tái sử dụng
   approval cũ.

## Hashes cần review trước quyết định

Các pin hiện hành lấy từ `WORKSPACE_SNAPSHOT.md:10,15` và phải được recheck
ngay trước owner decision:

| Thành phần | SHA-256 | Ý nghĩa/trạng thái |
|---|---|---|
| Approval manifest | `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501` | scope cố định; owner review |
| Command | `47044AB73C0B76A00E3E40A85D6E893B0F94C015F6332036484EA5ABB5FA55AB` | review-only; phụ thuộc helper pin |
| Helper/collector | `364A6AA2C52A90E7AD20F28EC6C46A0EAD1BA39E8909727BEA7396287896FFE7` | source hiện hành; runtime hash gate |
| Artifact contract | `FFE70A70E622706FABFA49D5843310ECD5A283B1CA114E32C636EA26B9FAE4BF` | report/receipt source-derived |
| Production APK | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` | frozen code207 artifact |
| AndroidTest APK | `57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA` | frozen test artifact |
| Serializer source | `1222B8AC9B79DAFC659DD364F50849DFBA4782C181606A92DA47EBD8C6164E3C` | production bytes source |

Thứ tự re-freeze nếu có sửa local pin: hash helper → cập nhật command helper
pin → hash command → cập nhật packet/provenance table. Không coi hash cũ trong
proposal lịch sử là bắt buộc. Proposal, snapshot và packet current đã pin các
hash ở bảng trên. `30B50…` và `4D68…` chỉ còn ở evidence lịch sử 2026-09-15;
không dùng chúng để approve hoặc chạy event hiện tại.

## Stop conditions

Dừng trước authorization/dispatch khi expected provenance không độc lập hoặc
không mapping được account; actual mismatch; bất kỳ hash/package/source/schema/
route/identity nào lệch; thiết bị không sẵn sàng; competing writer/process,
expired window, collector/verifier thiếu; credential/raw endpoint xuất hiện
trong output; hoặc outcome trước đó unknown. Sau provider reach, timeout,
process death hay thiếu post-readback: giữ `UNKNOWN`/`RECOVERY_REQUIRED`, đọc
cùng event nếu được, không refresh ID, retry, repair hoặc redispatch.

## Readiness và nguyên nhân overthinking còn lại

Local behavioral PASS chứng minh query/parser/collector/verifier và golden JVM
offline trên fixture; nó không chứng minh installed-pin readback, account
provenance hay live RAW. Vì vậy `OWNER_PACKET_PENDING` và `RAW_NOT_RUN` là nhất
quán; chỉ F1 provenance và explicit scope còn chờ owner sau khi các hash trên
được review. Overthinking trước đây đến từ proposal cũ trộn hash lịch sử với
current packet và lặp lại các gate đã đóng. Current sections nay đã đồng bộ;
chỉ dùng canonical current pins, packet này và
`docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`; không mở lại H1–H4 khi input
không đổi.
