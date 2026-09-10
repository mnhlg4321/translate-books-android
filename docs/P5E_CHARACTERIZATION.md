# P5E.2 — Characterization output hiện tại

Ngày: `2026-09-11` (+07:00)
Phạm vi: source VOL5 đã pin trong app-owned validation data; không gửi provider.

## Current baseline

| Hạng mục | Giá trị |
|---|---|
| Pre-upgrade validation build | `v4.17-p5e.3 / versionCode 199`, reconstructed data |
| Current installed validation build | `v4.17-p5e.6 / versionCode 202`, source commit `4140651d860e4ee11ce7e074970761666c575594`, guarded G2 readback passed |
| Candidate APK SHA-256 | `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0` |
| Current database schema | `v24` |
| Last-known original pilot predecessor | `code196 / schema v24`; historical and unavailable |
| Canonical/final schemas | Giữ nguyên; final report `safe4.full.report-l1.v1`, receipt `safe4.full.receipt.v1` |
| Existing authorization output cap | `4,096` tokens requested/effective |
| Pinned RAW | `23,814` bytes, SHA-256 `A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE` |

Code189/code191/code196 là historical evidence. Build đang cài để kiểm tra
local là candidate code202/schema v24 với validation data
`RECONSTRUCTED_ONLY`; pre-upgrade code199 chỉ là baseline readback. Không có
tokenizer chính thức hoặc provider call trong characterization này.

## Current full response shape trước P5E

Schema hiện tại yêu cầu model trả một artifact đầy đủ, gồm identity, ledger,
gate map, evidence/disposition và đồng thời `beforeText`/`afterText`. Với RAW
discovery, hai field text phải chứa cùng một RAW. Vì vậy chỉ riêng nội dung hai
bản sao source đã là:

```text
23,814 + 23,814 = 47,628 UTF-8 source bytes
```

Đo end-to-end không provider trên đúng pinned RAW, bằng canonical JSON hiện tại,
cho kết quả:

| Thành phần serialized độc lập | Bytes |
|---|---:|
| Hai field source (`beforeText` + `afterText`) gồm key/syntax/escaping | 48,426 |
| Nội dung hai bản sao source, chưa tính JSON | 47,628 |
| JSON escaping + key/syntax của hai field source | 798 |
| Ledger tối thiểu hiện tại | 160 |
| Gates hiện tại | 231 |
| Identities/stable anchor | 507 |
| Evidence + disposition + preserved/release fields | 298 |
| Full shape đo lại sau khi ghép | 49,665 |

Các dòng thành phần được serialize độc lập để chỉ ra nơi chi phí phát sinh nên
không cộng máy móc thành full-shape total vì mỗi fragment có delimiter riêng.
Full-shape total `49,665` là kết quả ghép và serialize lại một lần.

Không có GPT tokenizer chính thức trong engine test classpath. Do đó phép quy
đổi sau chỉ là ước lượng rõ giới hạn, không phải token count provider:

```text
full shape: ceil(49,665 / 4) = 12,417 estimated tokens
duplicated source alone: ceil(47,628 / 4) = 11,907 estimated tokens
```

Ngay cả cách ước lượng 4-byte/token bảo thủ này cũng lớn hơn cap `4,096` nhiều
lần. Kết luận chắc chắn theo bytes là full artifact không phù hợp với một wire
response bị giới hạn `4,096` tokens; token count chính xác chỉ có thể lấy từ
tokenizer/provider metadata tương ứng.

Bằng chứng test-first nằm ở
`editorial-engine/src/test/java/com/ml/tblandroidtxt/editorial/pack/EditorialP5RawWireContractTest.java`:
test giữ assertion `47,628` duplicated source bytes và kiểm tra shape cũ vượt
local wire budget trước provider dispatch.

## Compact response sau thiết kế P5E

Wire DTO mới chỉ giữ findings/ledger, gate observations, evidence references,
preserved inventory, disposition và hai replay echo cần thiết. Worst-case
canonical compact payload đo được là:

```text
worstCaseWireBytes = 2,785
MAX_WIRE_BYTES     = 3,584
effective cap      = 4,096 output tokens
```

Như vậy worst case còn `799` bytes dưới hard byte ceiling `3,584` và `1,311`
bytes dưới mốc so sánh `4,096` bytes. Theo cùng heuristic 4-byte/token, payload
này khoảng `697` tokens; đây là characterization margin, không phải provider
usage claim. Không có field nào chứa chapter-sized text và không có field nào
có thể lặp lại toàn bộ RAW.

Sau QA bổ sung, wire còn fail-closed với duplicate finding IDs, duplicate
evidence refs, evidence refs không nằm trong root inventory, population vượt
`MAX_FINDINGS`, preserved/disposition vượt limit và local ledger thiếu coverage.
Việc root inventory không chứng minh evidence tồn tại bên ngoài response; đó là
giới hạn chủ ý vì request hiện tại không mang một evidence database mới.

## Kết luận cap

```text
FULL_CURRENT_SCHEMA_WITH_RAW_DUPLICATION: NOT_ACCEPTABLE
COMPACT_WIRE_WORST_CASE: 2,785 BYTES <= 3,584 HARD BYTE CEILING
OUTPUT_CAP_4,096: RETAINED
OUTPUT_CAP_8,192+: NOT JUSTIFIED
```

P5E không đổi canonical pack, profile, authority, final REPORT_L1 hoặc receipt
schema. App lấy RAW từ exact pinned bytes, materialize `beforeText=afterText`,
bắt buộc `declaredChanges=[]`, rồi mới dựng/validate artifact final hiện hành.
