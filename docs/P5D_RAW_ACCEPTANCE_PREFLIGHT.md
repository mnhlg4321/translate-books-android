# P5D — Output Budget Alignment và RAW Acceptance Preflight

Ngày kiểm tra: `2026-09-09` (+07:00)

## Quyết định hiện tại

```text
TRANSPORT_AND_LIFECYCLE_LIVE_VERIFIED
RAW_OUTPUT_TRUNCATION_CONFIRMED
RAW_PREDECESSOR_REQUIRED
OUTPUT_BUDGET_ALIGNMENT_PASS
RECOVERY_HISTORY_PRESERVED
RAW_ACCEPTANCE_INCOMPLETE
P6_NOT_READY
NEW_RAW_AUTHORIZATION_REQUIRED
RECONCILE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

Đây là preflight chuẩn bị cho một lần RAW acceptance mới. Chưa có provider
call nào trong bước alignment này; authorization ở cuối tài liệu chỉ là bản
nháp chờ người dùng cấp quyền. Authorization diagnostic trước đó đã consumed
và không được tái sử dụng.

## Baseline và artifact đã kiểm chứng

| Item | Giá trị |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| HEAD trước snapshot tài liệu | `a865b0203c8f25d1de48ea15d6406d599cc961af` |
| Device | `15e84958` |
| Validation package | `4.17-dev.23 / code191` |
| APK SHA-256 | `5F3C841F590C6F7AA3E19F625D2B387E55B2D6BFD3C50A93D61F0337E6A2340C` |
| Test APK SHA-256 | `8A58C19EEC98BB1BC1F1042A624EDD52E3DB67B05BF906A3241D64D339AF249D` |
| Build event | `build-20260909-213020` |
| Build source commit | `a865b0203c8f25d1de48ea15d6406d599cc961af` |
| Database schema | `23` |
| Canonical ZIP SHA-256 | `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java control ZIP SHA-256 | `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Canonical pack hash | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Profile resource SHA-256 | `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` |

Ba authority bytes vẫn đúng, không BOM và không newline normalization:

| Authority | Bytes | SHA-256 |
|---|---:|---|
| Project Instruction | 9,485 | `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD` |
| Prompt đầu chat | 8,852 | `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` |
| Workflow ba lượt | 34,917 | `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730` |

Artifact phân phối nằm tại:

```text
artifacts/builds/v4.17-dev.23/build-20260909-213020/
backup/builds/v4.17-dev.23/build-20260909-213020/
```

Đây là validation artifact, không phải V4.18 release build. Production APK đã
được cài nâng cấp bằng `adb install -r`; không uninstall, không reset và không
đổi binding VOL5.

## Exact VOL5 binding và source identity

| Field | Giá trị |
|---|---|
| Selector | `p5d-raw-mercedes-vol5-001` |
| Chapter | `001` |
| Binding identity | `2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` |
| Run declaration | `7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0` |
| Compatibility evaluation | `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` |
| Source mode | `NORMAL_FOUR_SOURCE` |
| Pronoun status | `AVAILABLE` |
| Execution/certification | `false` / `NOT_CERTIFIED` |

Source identity được đọc theo đúng đường chuẩn hóa của runner:

| Role | Bytes sau chuẩn hóa | SHA-256 |
|---|---:|---|
| RAW | 23,814 | `A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE` |
| DRAFT | 26,462 | `64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5` |
| GLOSSARY | 3,249 | `4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314` |
| PRONOUN | 452 | `4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686` |

PRONOUN transport identity được ghi riêng: `455` bytes, SHA-256
`63E79EEBCBFE6BEDCEA088640339EB7D05AEDA75C28FD4A4E17B282B8ED1A49C`; chỉ
bỏ đúng UTF-8 BOM ba byte để tạo `452` bytes đã pin. Không trim, đổi line
ending, normalize Unicode hoặc thay source.

## Diagnostic history được bảo toàn

Attempt immutable cũ:

| Field | Giá trị |
|---|---|
| Attempt | `157e3517b4b98535392db95f0c93285f08a0ac6b07341a82508ffc36aea9a5f0` |
| Status | `RECOVERY_REQUIRED` |
| Prior generation | `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` |
| External result | `EXTERNAL_CONFIRMED_CANCELLED` |
| Prior displayed cost | `$0.00484` |
| Prior authorization | `P5D-VOL5-RAW-DIAGNOSTIC-20260909-01`, consumed |

Attempt diagnostic mới nhất:

| Field | Giá trị |
|---|---|
| Generation | `gen-1788959113-A9RinufLgb63vTqkAEwE` |
| Request | `req-1788959113-sRWHWKCcGAHmWqJxDqWz` |
| Provider response | `resp_0f12eb892139409f016aa1598a01ac87d1a3454c3c4d46dedf` |
| HTTP/content type | `200` / `application/json` |
| Finish | `length` / `max_output_tokens` |
| Usage | `20,327 input / 2,048 output / 22,375 total` |
| Provider-reported cost | `$0.0075392` |
| Local lifecycle | `RESPONSE_BODY_COMPLETE`, read back được |
| Local result | `RETRY_OUTPUT_TRUNCATED` |
| REPORT_L1 / receipt / predecessor | `0` / `0` / chưa commit |
| Cancellation source | chưa xác định; không phải cancellation của attempt này |

Lần diagnostic cho thấy transport hoàn tất nhưng output semantic bị cắt ở
`2,048`. Không chạy thêm provider call chỉ để lặp lại kết luận này.

## Audit output budget và nguồn quyết định

Nguồn quyết định duy nhất là `authorization.maximumOutputTokens`. Effective cap
phải bằng requested authorization cap và phải được đưa nguyên vẹn đến HTTP
`max_tokens`; không được silent clamp.

| Boundary | Kết quả alignment |
|---|---|
| Authorization | `maximumOutputTokens` là requested cap |
| `EditorialP5CExactBindingExecution` | `boundedOutputTokens()` trả đúng authorization cap |
| `OpenRouterEditorialP5PilotProvider` | giữ đúng cap; cap `<= 0` bị từ chối |
| `OpenAICompatibleClient` | `buildChatRequestBody(..., cap)` ghi đúng `max_tokens`; cap `<= 0` bị từ chối |
| Engine boundary | token cap không hợp lệ dừng typed trước provider |
| Acceptance/accounting | output usage và total usage vẫn được validate cục bộ; không biến thiếu usage thành actual `$0` |

Các literal `2,048` còn lại chỉ thuộc lịch sử diagnostic đã consumed hoặc
benchmark legacy không phải P5 pilot. Chúng không được dùng cho acceptance mới.
Test `EditorialP5COutputBudgetTest` chứng minh cap `4,096`, `8,192` và cap thấp
hơn được truyền đúng, đồng thời cap không hợp lệ bị chặn.

## Đánh giá chọn cap

Fixture output tổng hợp hợp lệ trong
`OpenRouterEditorialP5PilotProviderTest` và fake E2E bao phủ identity envelope,
ledger, population, evidence refs, gate map và disposition. Fixture này dùng để
đo cấu trúc tối thiểu và kiểm validator; nó không được coi là dự báo chính xác
độ dài output model. Prior live result là bằng chứng thực tế duy nhất về việc
`2,048` không đủ.

Một rendering tổng hợp cùng cấu trúc (một ledger entry, chín gate IDs và đủ
identity/disposition fields) có `1,248` UTF-8 bytes, tương đương khoảng `312`
tokens theo heuristic thô `4 bytes/token`. Đây chỉ là lower-bound structural
measurement; không dùng heuristic này để dự báo output model hay thay thế
budget evidence thực tế.

Pricing tham chiếu hiện tại của model trên OpenRouter là `$0.20/M` input và
`$1.20/M` output: [OpenRouter GPT-5.6 Luna pricing](https://openrouter.ai/openai/gpt-5.6-luna-20260709).
Với input diagnostic `20,327` làm cơ sở so sánh:

| Candidate | Max total tokens theo input cũ | Chi phí lý thuyết tối đa theo standard rate* | Quyết định |
|---:|---:|---:|---|
| 2,048 | 22,375 | `$0.00652` | Loại — đã truncated thực tế |
| 4,096 | 24,423 | `$0.0089806` | Chọn |
| 8,192 | 28,519 | `$0.0138958` | Chưa cần bằng chứng |

`*` Đây là ước tính theo input/output cap và standard rate, không phải billing
cam kết; routing, cache hoặc provider metadata có thể làm actual cost khác.
Cap `4,096` được chọn vì gấp đôi cap đã chứng minh thiếu, có headroom cho
envelope RAW nhưng không tăng gấp bốn data exposure/cost/time như `8,192` khi
chưa có evidence bắt buộc. Authorization vẫn giữ total-token cap `100,000`, cost
cap `$0.10`, deadline `300,000 ms`; requested cap và effective cap đều là
`4,096`.

## Recovery history v22 → v23

Schema v22 chỉ có một reconciliation row immutable. Test-first proof cho thấy
quyết định cancelled đầu tiên không thể chứa thêm quyết định truncated thứ hai
mà không overwrite; đó là gap lưu lịch sử, không phải lý do đổi binding.

Schema v23 thêm đúng một bảng append-only:

```text
editorial_p5d_reconciliation_history
```

Bảng dùng `decision_identity` hash làm khóa, giữ classification/evidence/account
fingerprint/billing/decision/retry authorization/decidedAt, có foreign key về
attempt, index theo attempt và trigger cấm update/delete. Primary row cũ không
bị thay thế; `recordRecoveryDecision()` idempotent với cùng decision và append
decision khác vào history. Không lưu request body, response body, source text,
API key hoặc exception message.

Bằng chứng:

- `EditorialP5CExactBindingFakeE2EInstrumentedTest` — cancelled decision và
  truncated decision cùng tồn tại, primary vẫn giữ quyết định đầu tiên.
- `EditorialP5DVol5RawPilotInstrumentedTest#v23UpgradePreservesVol5RecoveryIdentityAndHistory`
  — device readback `1/1`; status `RECOVERY_REQUIRED`, recovery reason, auth
  count và primary reconciliation giữ nguyên, history table hiện diện, không
  reset/uninstall pilot package.
- Schema regression group — `41/41 PASS`, gồm fresh schema, immutable triggers
  và migration/readback assertions.

## Validation evidence sau alignment

| Scope | Lệnh/đường chạy | Kết quả |
|---|---|---:|
| Host engine | `:editorial-engine:test --no-daemon` | `181/181 PASS` |
| Host app all unit variants | `:app:test --no-daemon` | `657/657 PASS` (`219` mỗi debug/release/benchmark) |
| Device v23 VOL5 readback | `v23UpgradePreservesVol5RecoveryIdentityAndHistory` | `1/1 PASS` |
| Device fake E2E/recovery | `EditorialP5CExactBindingFakeE2EInstrumentedTest` | `14/14 PASS` |
| Device schema/migration | 5 schema classes | `41/41 PASS` |
| Device importer/P1/P2 | 3 classes | `23/23 PASS` |
| Device P3B/P4 | trusted profile + P4 binding | `5/5 PASS` |
| External static qualification | authority `TESTS/test_full_release.ps1` | `306 PASS / 0 FAIL` |
| Provider calls trong alignment/preflight | fake/local only | `0` |

Không lặp lại delay harness toàn bộ vì transport/cancellation không thay đổi
trong alignment này. Full instrumentation `130/130` của code189 vẫn là
historical evidence; focused suites trên code191 là bằng chứng hiện tại cho
phạm vi thay đổi. Live RAW acceptance chưa chạy.

## Bản nháp authorization mới — CHƯA CẤP

```text
P5D RAW ACCEPTANCE AUTHORIZATION — CHỜ DUYỆT, CHƯA CẤP

Authorization ID (proposed): P5D-VOL5-RAW-ACCEPTANCE-20260909-01
Authorization ID SHA-256 (proposed fingerprint):
  d116a03f995480c19187ea6531dbf4fc4e91690178f13841854297b62cf01693
Binding / selector / chapter:
  2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520 /
  p5d-raw-mercedes-vol5-001 / 001
Run declaration:
  7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0
Compatibility evaluation:
  f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1
Canonical pack hash:
  497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
Canonical profile hash:
  beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
Expected source hashes:
  RAW     A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE
  DRAFT   64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5
  GLOSSARY 4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314
  PRONOUN 4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686

Prior truncated generation:
  gen-1788959113-A9RinufLgb63vTqkAEwE
Prior usage/cost/reason:
  20,327 input / 2,048 output / 22,375 total; $0.0075392;
  RETRY_OUTPUT_TRUNCATED (finish=length)
Prior cancelled generation retained:
  gen-1788910936-DHfTNOyDlU3f3PJOAvqb; EXTERNAL_CONFIRMED_CANCELLED; $0.00484

Provider / model: OpenRouter / openai/gpt-5.6-luna
Endpoint/account fingerprint:
  2cd5d48d21f3d99ad396614a8bee162e62a23360ca0a8a081187b26d046b34de
Egress: YES
Requested output cap: 4096
Effective output cap: 4096
Input / output / total token caps: 100000 / 4096 / 100000
Maximum additional cost: $0.10
Maximum elapsed time: 300000 ms (5 minutes)
Maximum primary calls: 1
Maximum schema-repair calls: 0
Automatic network retries: 0
Request/full-response storage: NO / NO
Redacted lifecycle metadata: YES; HASH_ONLY
Duplicate work/billing risk: YES

Authorized phase: L1_RAW_DISCOVERY only
RECONCILE: NO
L2/L3: NO
Certification: NO
General runnable declaration: NO
Single-use: YES
Status: UNISSUED; provider calls: 0
```

Authorization này chưa mở quyền dispatch. Khi được duyệt, runner phải đọc lại
exact persisted binding, source hashes, recovery history và effective cap ngay
trước call; chỉ một RAW primary request được phép. Nếu output hợp lệ thì commit
RAW predecessor/readback rồi dừng với `RECONCILE_AUTHORIZATION_REQUIRED`. Nếu
truncated, sai schema, identity mismatch hoặc provider error thì giữ typed
recovery evidence và không tự gọi lần hai.

## Không được kết luận từ preflight này

```text
LIVE_RAW_ACCEPTANCE_PASS       = CHƯA CÓ
RAW_PREDECESSOR_COMMITTED      = CHƯA CÓ
REPORT_L1_VALIDATED             = CHƯA CÓ
L1_RECEIPT_VALIDATED            = CHƯA CÓ
P5_L1_PILOT_COMPLETE            = CHƯA CÓ
P6_READY                        = KHÔNG
CERTIFIED / GLOBALLY_RUNNABLE   = KHÔNG
```
