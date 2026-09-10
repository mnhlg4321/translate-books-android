# P5E.3–P5E.6 — Compact RAW wire contract

## Tên và ownership

Provider response có tên riêng:

```text
wire schema version: safe4.raw.discovery.wire.v1
schema name:         safe4_raw_discovery_v1
```

Artifact app chứng nhận vẫn có tên khác và không bị thay đổi:

```text
final report:   safe4.full.report-l1.v1 / REPORT_L1
final receipt:  safe4.full.receipt.v1
```

`EditorialP5RawWireContract` và `EditorialP5RawWireResponse` chỉ là DTO/validator
model-facing. Provider không được trả lại canonical pack/profile bytes, authority
bytes, full source text, final identities do app sở hữu, `beforeText`,
`afterText` hoặc edit.

## Wire fields

Root object đóng (`additionalProperties=false`) và mọi nested object cũng đóng.
Các field bắt buộc:

| Field | Ý nghĩa và giới hạn |
|---|---|
| `wireSchemaVersion` | exact `safe4.raw.discovery.wire.v1` |
| `attemptIdentity` | replay echo exact SHA-256 của attempt hiện hành |
| `requestEnvelopeHash` | replay echo exact request envelope hiện hành |
| `findings` | tối đa `4` entries; mỗi entry có item ID, disposition, tối đa `2` evidence refs và model flag |
| `gateObservations` | đúng toàn bộ gate IDs hiện hành; value chỉ `PASS` hoặc `NOT_APPLICABLE` |
| `evidenceRefs` | tối đa `8`, mỗi ref dài tối đa `48` ký tự |
| `preservedInventory` | tối đa `4`, mỗi item ID dài tối đa `48` ký tự |
| `declaredChanges` | bắt buộc array rỗng; local parser từ chối mọi phần tử |
| `disposition` | enum disposition; phase exact `L1`; bounded text tối đa `32`; evidence tối đa `4` |
| `modelDeclaredPass` | chỉ là observation không có authority; local validators quyết định |

`itemId`/preserved ID tối đa `48`; mọi token/ref không có newline, whitespace
hoặc free-form chapter payload. Không có `minItems` bắt buộc khiến model phải tạo
ledger giả; hard maximum do schema và local parser cùng giữ.

Các finding ID phải unique; root evidence inventory phải unique và bao phủ mọi
evidence ref ở finding/disposition. Đây là coverage của wire response, không
phải bằng chứng rằng ref có tồn tại trong external evidence store. Local ledger
validator vẫn đối chiếu exhaustive population của app; nếu population lớn hơn
`MAX_FINDINGS=4`, engine dừng trước provider để tránh silent drop.

## Materialization rule cho RAW

1. App tạo request từ exact persisted binding và exact pinned source bytes.
2. App lấy `RAW` từ `request.visibleSources()`; model response không cung cấp RAW.
3. App decode strict UTF-8 và kiểm tra round-trip byte equality.
4. App đặt `beforeText=afterText` từ đúng chuỗi RAW đó.
5. App đặt `declaredChanges=[]`; bất kỳ output edit nào đều bị reject.
6. App-owned identities, canonical pack/profile references, receipt và final
   `REPORT_L1` được materialize/validate cục bộ theo schema hiện hành.

Wire response không tự cấp quyền certify, propagate, execute hoặc mở
RECONCILE. `modelDeclaredPass` chỉ được ghi nhận sau local ledger/gate/diff/
receipt validation.

## Hard size budget

```text
authorization output cap: 4,096 tokens
local compact wire ceiling: 3,584 UTF-8 bytes
measured declared worst case: 2,785 UTF-8 bytes
schema repair calls: 0 for first P5E acceptance
automatic network retries: 0
```

`MAX_WIRE_BYTES` thấp hơn cap để giữ margin cho transport/serialization. Local
parser từ chối body vượt ceiling trước materialization. Không tăng cap lên
`8,192+` vì output cũ đã được chứng minh bằng characterization là chapter-sized.
`worstCaseWireBytes()` dùng các ID/ref unique ở đúng độ dài cực đại; vì vậy
margin `2,785` là một fixture hợp lệ, không dựa vào duplicate để giảm/đổi kích
thước.

## Request mode

Chỉ RAW route dùng:

```json
{
  "stream": false,
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "safe4_raw_discovery_v1",
      "strict": true
    }
  },
  "provider": {"require_parameters": true},
  "reasoning_effort": "minimal"
}
```

Không bật response-healing plugin. `reasoning_effort=minimal` là policy explicit
(OpenRouter ghi rõ đây là shorthand của `reasoning.effort`). OpenRouter báo
`completion_tokens` là aggregate output, trong đó `reasoning_tokens` là phần con;
adapter lưu reasoning riêng để audit và tính aggregate output/cost không
double-count. Adapter route chỉ tới provider hỗ trợ required parameters;
`require_parameters` để OpenRouter loại provider không hỗ trợ structured output.

## Replay và identity

Wire bỏ các identity app-owned không làm yếu binding:

- `attemptIdentity` và `requestEnvelopeHash` phải match exact request đang chạy;
- engine/store còn kiểm tra binding, run declaration, pack/profile, evaluation,
  chapter, phase, predecessor, source hashes và provider/model;
- final report chỉ lấy identity từ request/context app-owned;
- response echo sai attempt hoặc envelope bị reject;
- predecessor khác không được reclaim/commit;
- response model tự khai PASS không vượt local validators;
- wire từ binding/chapter/phase khác không được materialize.

Sửa identity hardening tại `d39bca7` được giữ nguyên. Không có đường fallback
để model trở thành owner của canonical identity.

## QA boundary

Provider strict JSON Schema và local parser cùng đóng unknown fields, giới hạn
counts/lengths, reject malformed/truncated/over-ceiling body và reject non-empty
`declaredChanges`. Engine không commit khi response STOP, thiếu coverage, sai
binding/replay hoặc quá population bound. `modelDeclaredPass` không vượt qua
local ledger/gate/diff/receipt validators.
