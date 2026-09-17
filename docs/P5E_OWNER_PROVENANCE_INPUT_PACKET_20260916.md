# P5E — owner provenance/input packet

Ngày: `2026-09-16`
Trạng thái local: `P5E_LOCAL_BEHAVIORAL_GATE_GREEN / OWNER_ACCOUNT_CHECK_SCOPE_RECEIVED / ACCOUNT_CHECK_ARTIFACT_BUILT_NOT_INSTALLED / TEST_PACKAGE_REPLACEMENT_NOT_EXECUTED_DEVICE_UNAVAILABLE / ACCOUNT_CHECK_NOT_EXECUTED / A4_3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY`.

## Owner input received — limited account-check scope

On `2026-09-16`, the owner attested that the API key currently stored in the
Android app belongs to the permitted OpenRouter account for P5E, and approved
one device-side memory-only account check whose only result may be
`MATCH`/`MISMATCH`. The scope explicitly excludes logging or exporting the key,
fingerprint or endpoint, provider calls, database writes and RAW dispatch.
No secret or fingerprint value is recorded here.

This input does not make the current pinned RAW APK executable for that operation.
The pinned `EditorialP5EFreshRawLiveInstrumentedTest#authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn`
computes and compares the fingerprint, but then proceeds into DB/preflight and
`dispatchRaw`; it is not an account-only method. The current AndroidTest pin
has no separate memory-only `MATCH`/`MISMATCH` entry point. A separately
qualified test-only method and host runner have now been added and built; the
replacement artifact is not installed. No trusted expected fingerprint is
available through the process-only channel, so the check result remains
`NOT_EXECUTED`, not `MATCH` or `MISMATCH`. A bounded read-only preflight then
used two adb commands for the exact serial and returned `DEVICE_NOT_FOUND` for
both; install attempts and device mutations were `0`.

The new boundary loads settings in memory, compares to an owner-controlled
process-only expected value, emits only `MATCH`/`MISMATCH`, and has no
DB/provider/RAW path. The host runner hash-checks itself, the existing helper,
and the replacement APK before reading the expected value or creating an adb
process. Its offline missing-value and fake-process checks are recorded in
`docs/P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json`. This approval was used
only to prepare the test-only artifact. The one replacement was not attempted
because the exact serial was unavailable; the expected value is also still
required through the owner-controlled process-only channel.

## Điều còn thiếu trước khi chạy account check hoặc mở event RAW

Không cần gửi secret trong chat/Git. Các mục còn thiếu hoặc cần giữ riêng là:

1. Nhãn operator/project/account dùng để xác định đúng tài khoản và thời điểm
   xác minh.
2. Nguồn provenance độc lập cho expected endpoint-account fingerprint: mã tham
   chiếu tới trusted enrollment/record và mapping account/key. Không gửi actual
   fingerprint, API key, raw endpoint, settings hay credential qua chat, Git,
   log hoặc command text.
3. Account-check scope này đã được owner cho phép: memory-only
   load/normalize/hash/compare, kết quả chỉ `MATCH`/`MISMATCH`, không lưu hai
   digest. Expected value vẫn phải đi qua process-only channel từ nguồn độc
   lập; không lấy actual trên thiết bị làm expected.
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
| AndroidTest APK (historical RAW/A4 pin) | `57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA` | unchanged; not the account-check replacement |
| Account-check AndroidTest APK | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` | `p5e-account-check-20260916-01`, built/not installed |
| Account-check AndroidTest source ZIP | `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F` | source commit `9e5ffb7819bfb91dcb8ed9e25c901ab10aa48390` |
| Account-check source | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` | exact class/method; test-only |
| Account-check host runner | `0722A243C92724D59AFB7CF4B6DE674024F9BAE4FD76A712DF0210AF25B036F3` | exact script; no retry/redispatch |
| Account-check local result | `48300DF171FA4B44F4A35A50EAB7A03AB0D31E37344C45975A73190C458B68D8` | read-only preflight `DEVICE_NOT_FOUND`; no install/account result; expected value absent |
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
offline trên fixture; account-check source/runner cũng đã được build và kiểm
offline. Read-only preflight exact serial mới nhất trả `DEVICE_NOT_FOUND`, nên
chưa có installed-pin readback, expected account provenance hay live RAW. Vì
vậy `TEST_PACKAGE_REPLACEMENT_NOT_EXECUTED_DEVICE_UNAVAILABLE`,
`ACCOUNT_CHECK_NOT_EXECUTED`, `OWNER_PACKET_PENDING` và `RAW_NOT_RUN` là nhất
quán; expected fingerprint process-only và mọi readback/RAW permission còn chờ
đúng preconditions/owner. Current sections nay đã đồng bộ; chỉ dùng canonical current pins, packet này và
`docs/P5E_SQL_BEHAVIORAL_RESULT_20260916.json`/`docs/P5E_ACCOUNT_CHECK_LOCAL_RESULT_20260916.json`;
không mở lại H1–H4 khi input không đổi.
