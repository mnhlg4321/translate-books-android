# P5D — Controlled RAW Diagnostic Retry Preflight

Ngày kiểm tra: `2026-09-09` (+07:00)

## Quyết định hiện tại

```text
P5D_DOCUMENTATION_BASELINE_CONSISTENT
P5D_RECOVERY_FIXTURES_PASS
P5D_RAW_DIAGNOSTIC_AUTHORIZATION_DRAFT
NO_PROVIDER_CALL_IN_THIS_STEP
VOL5_RECOVERY_REQUIRED
EXTERNAL_CONFIRMED_CANCELLED
RECONCILE_NOT_AUTHORIZED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

Đây là preflight và bản nháp quyền gọi, không phải authorization đã cấp.
Chưa ghi reconciliation decision mới, chưa consume authorization mới và
chưa dispatch provider.

## Baseline và artifact

| Item | Value |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| HEAD | `81c6c3e2a7cf5c6fc53c210c762ee0ab6bd9c37b` |
| Worktree | clean trước thay đổi tài liệu preflight |
| Device | `15e84958`, API 35 |
| Installed validation package | `4.17-dev.21 / code189` |
| APK SHA-256 | `D66C3C816E29403508BF997413998683FDCAD6AC2C74F24DA40E9AF31570860C` |
| Test APK SHA-256 | `30EADECFA740326F5A3036584DB2F8613C90633E0DAF19DCF9472C0001EF2165` |
| Database schema | `22` |
| Canonical ZIP SHA-256 | `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java control ZIP SHA-256 | `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |

Ba authority hash không thay đổi: project
`1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD`, prompt
`D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` và
workflow `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730`.

## Binding và source identity

| Field | Value |
|---|---|
| Selector | `p5d-raw-mercedes-vol5-001` |
| Chapter | `001` |
| Binding identity | `2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` |
| Run declaration | `7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0` |
| Compatibility evaluation | `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` |
| Pack identity | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` |
| Profile identity | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Source mode/status | `NORMAL_FOUR_SOURCE` / `PRONOUN=AVAILABLE` |
| Execution/certification | `false` / `NOT_CERTIFIED` |

Identity sau đúng normalization của runner:

| Role | Normalized bytes | Normalized SHA-256 |
|---|---:|---|
| RAW | 23,814 | `A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE` |
| DRAFT | 26,462 | `64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5` |
| GLOSSARY | 3,249 | `4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314` |
| PRONOUN | 452 | `4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686` |

PRONOUN transport bytes được giữ nguyên là `455` bytes, SHA-256
`63E79EEBCBFE6BEDCEA088640339EB7D05AEDA75C28FD4A4E17B282B8ED1A49C`, có
UTF-8 BOM. Runner chỉ bỏ đúng ba byte BOM, không trim, không đổi newline,
Unicode hay nội dung; kết quả `452` bytes chính là identity đã pin.

## Attempt và recovery readback

| Field | Value |
|---|---|
| Original attempt | `157e3517b4b98535392db95f0c93285f08a0ac6b07341a82508ffc36aea9a5f0` |
| Local status | `RECOVERY_REQUIRED` |
| Local reason | `RETRY_PROVIDER_CALL_FAILED_UNKNOWN` |
| Response/report/receipt | absent / `0` / `0` bytes |
| Prior authorization | consumed; hash `f39ff4fd7503aa6746fc2f61f4ea75a06ec5989f187da240334822587eeaa212` |
| Prior generation | `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` |
| External classification | `EXTERNAL_CONFIRMED_CANCELLED` |
| Provider metadata | `23,674/90` tokens, displayed cost `$0.00484`, HTTP `200`, finish `cancelled` |
| Local reconciliation row | absent for the historical attempt |
| Local lifecycle row | absent for the historical attempt; it predates recorder wiring |

Không suy diễn actor cancellation từ `cancelled` hoặc mốc thời gian. Actor
lịch sử vẫn `UNKNOWN`; local transport/lifecycle harness đã được xác minh
riêng.

## Isolated gate evidence

| Check | Result |
|---|---|
| `EditorialP5CExactBindingFakeE2EInstrumentedTest` trên code189 | `13/13 PASS` |
| `EditorialP5CLiveRecoveryInspectionInstrumentedTest` trên app DB | `1/1 PASS` |
| `EditorialP5PilotExecutionBoundaryTest` với JDK 17+ | `16/16 PASS` |
| Provider calls trong preflight/fixture | `0` |
| DB reset/uninstall/force-stop pilot package | `0` |
| `git diff --check` trước tài liệu này | PASS |

Các fixture chứng minh recovery thiếu reconciliation không được reclaim,
reconciliation đúng phải khớp hash authorization mới và duplicate không tạo
call thứ hai; auth single-use vẫn `CONSUMED` sau DB reopen. Expiry/budget và
zero-provider preflight được kiểm tra ở engine boundary. Các test dùng DB cô
lập, không ghi vào VOL5 pilot DB.

## Bản nháp authorization mới — chưa cấp quyền

Authorization ID đề xuất:
`P5D-VOL5-RAW-DIAGNOSTIC-20260909-01`

SHA-256 của ID đề xuất (chỉ là fingerprint dự kiến, chưa phải receipt):
`a28d70c9f1e9b33160daa6d1614abae98f5a921621fbea49f92279f06bcdc00d`

| Field | Proposed value |
|---|---|
| Binding / selector / chapter | binding `2e5c80…2520` / `p5d-raw-mercedes-vol5-001` / `001` |
| Original attempt | `157e3517…a9a5f0` |
| Prior generation / result / cost | `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` / `CANCELLED` / `$0.00484` |
| Reconciliation evidence | `docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md` |
| Provider / model | `openrouter` / `openai/gpt-5.6-luna` |
| Endpoint/account fingerprint | `2cd5d48d21f3d99ad396614a8bee162e62a23360ca0a8a081187b26d046b34de` |
| Primary / schema repair / network retry | `1 / 0 / 0` |
| Input / output / total token caps | `100000 / 2048 / 100000` |
| Maximum additional cost | `$0.10` |
| Maximum elapsed time | `300000 ms` (5 minutes) |
| Chapter egress | `YES` — must be explicitly re-approved with this new grant |
| Request/full response retention | `NO / NO` |
| Redacted lifecycle metadata | `YES`, `HASH_ONLY` |
| Duplicate work/billing risk | explicit acknowledgement required |
| Issued/expires | generated at approval; five-minute single-use window |
| Authorized phase | `L1_RAW_DISCOVERY` only |
| RECONCILE/L2/L3/certification/general runnable | `NO / NO / NO / NO` |

This draft deliberately sets schema repair to `0` so one diagnostic primary
result is observed without a second semantic-adjacent call. No reconciliation
decision is written with this draft ID. Once explicitly approved, the
existing owner must persist the immutable `EXTERNAL_CONFIRMED_CANCELLED`
decision with this authorization hash before any dispatch.

## Next gate

Await explicit approval of the exact draft above. Until then: no provider call,
no new attempt claim, no authorization receipt consumption, no reconciliation
write and no RECONCILE authorization. If approved, run one RAW-only diagnostic
attempt, wait for terminal state/deadline, read back lifecycle and predecessor,
then stop before RECONCILE.
