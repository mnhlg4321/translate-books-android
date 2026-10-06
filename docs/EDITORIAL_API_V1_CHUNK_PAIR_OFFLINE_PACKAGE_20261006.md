# Editorial API V1 — gói offline cặp chunk, structural gate và đối chứng E (canonical CP-OFFLINE-2)

Ngày cập nhật: 2026-10-06 (+07:00)
Revision tài liệu: CP-OFFLINE-2
Kế hoạch mẹ: [EDITORIAL_API_V1_PLAN_20261005.md](EDITORIAL_API_V1_PLAN_20261005.md)
Branch: feature/v4.18-p5e-runner-repair-20260917
HEAD đã kiểm tra: 37889b0570ff3c3e0f2c175a4472416ea28ecd18

## 1. Mục tiêu, quyền hạn và trạng thái

Gói này chuẩn bị kiểm chứng offline cho biên tập theo cặp chunk trong kế hoạch v4.18. Nó tách ba câu hỏi:

1. **Cấu trúc và provenance có an toàn không?** Mapping, identity, separator, prompt scope, snapshot và merge phải đúng trước khi nói về nghĩa.
2. **E toàn chương và E/chunk khác nhau thế nào?** Đây là đối chứng chính; cùng source snapshot, cùng Quality Core, cùng model/prompt revision và cùng reference policy.
3. **Chất lượng nghĩa ra sao?** Chỉ là phép đo semantic riêng sau structural gate; trong gói này vẫn NOT_MEASURED.

Gói này không mở branch/release/checklist mới, không thay acceptance của API V1 và không tạo quyền live. Lượt hiện tại chỉ sửa tài liệu canonical, plan và snapshot. Không runtime, không build, không cài/đụng device hoặc pilot, không provider, không commit và không push. Trạng thái gói là **PREPARED / DOCUMENT-ONLY / RUNTIME-NOT-EXECUTED**.

Phạm vi chỉnh trong revision này (không đổi mã, không mở gói mới): (1) tách **separator biên ghép** khỏi **layout bên trong candidate** (§3.3, §4.2); (2) số dòng đổi **không bao giờ tự BLOCK**, dấu so sánh ngưỡng chữ được chốt bằng số nguyên, mọi ngưỡng chỉ là hàng rào nghi ngờ, và `REFLOW_ONLY` không phát sinh khi candidate nguyên văn: SIZE theo bậc chỉ cảnh báo số dòng, BLOCK dựa trên mất/thừa chữ và marker; đối chứng sửa đoạn hợp lệ, chunk ngắn, gộp dòng giữ nguyên chữ, khôi phục đoạn thiếu và replay N5 (§4.2, GATE-04/05/07/08); (3) chốt **đường xử lý WARN** (§4.4); (4) đặc tả **scope pronoun trước khi chia MAIN/CONTEXT**, bảo toàn phạm vi áp dụng trong prompt và khi dedupe (khóa gộp gồm `from`), **không tự suy ưu tiên giữa các scope** (xung đột cùng phạm vi được giữ và báo), và đối chứng W/C (§5, SCOPE-04/05); (5) **reservation trước dispatch** (§7, §9.3); (6) đồng bộ canonical mục 10, plan 7.1, snapshot và sửa bảng/state (§7, §8).

Job **Dịch** là nguồn read-only. Gói không tách RAW và DRAFT theo ordinal để tự ghép, không ghi đè row chunk, output, status, ledger hay artifact của job Dịch. Response, ledger và prompt mẫu chưa dùng của các phiên khác được giữ nguyên ngoài Git.

## 2. Ownership và đầu ra bắt buộc

| Ownership | Phạm vi chịu trách nhiệm | Evidence phải giao |
|---|---|---|
| **ENGINE** | RawManifest, DraftManifest, PairMap, identity, scope/context filter, StructuralGate, prompt E, candidate result và ChunkMerge thuần JVM | Test mapping/identity/separator/scope/gate/merge; manifest và receipt hash-bound |
| **APP** | Nhập cặp từ job Dịch ở chế độ đọc, snapshot, journal trạng thái, phục hồi, không resend, ghép/xuất atomic | Test import không mutation, process/reopen, run state và output read-back |
| **QA** | Fixture tổng hợp, đối chứng nhập ngoài, lỗi biên/thiếu/UNKNOWN, replay N5, tách structural khỏi semantic, secret/evidence scan | Ma trận case, expected typed result, báo cáo PASS/FAIL/NOT_MEASURED |
| **COORDINATOR** | Đồng bộ canonical/plan/snapshot, kiểm scope, kiểm ngân sách theo manifest, giữ stop rule | Tài liệu này, mục plan 7.1 và snapshot cùng revision; không dispatch |
| **OWNER** | Duyệt phạm vi đo và cap được tính sau dry-run; quyết định mở gói triển khai tiếp theo | Một quyết định riêng, không suy ra từ tài liệu này |

Đầu ra của gói triển khai sau (chưa làm trong lượt này) phải giữ đúng bốn lớp: manifest/mapping, structural evidence, semantic evaluation, cost/provenance. Không dùng một nhãn PASS chung để che một lớp chưa đo.

## 3. Mô hình mapping RAW–DRAFT độc lập nhưng liên kết explicit

### 3.1 Hai manifest và hai hệ tọa độ

RAW và DRAFT có tọa độ riêng, không dùng số dòng hoặc ordinal của bản này để suy ra bản kia.

RawManifest gồm:

- rawDocId, rawNormalizedSha256, normalizationRevision, separatorProfile;
- rawChunkId ổn định, startOffset/endOffset nửa mở trên chuỗi RAW đã chuẩn hóa, mainHash, và `rawParagraphStart`/`rawParagraphEnd` đánh số **toàn chương** (đoạn = khối không rỗng ngăn bởi dòng trống, đánh số từ 1, gồm cả hai đầu; là tọa độ duy nhất mà scope pronoun được phép dùng, §5.1), cùng `chapterId` nếu nguồn có;
- contextBefore/contextAfter chỉ là vùng tham khảo, có offset/hash riêng và không nằm trong coverage chính.

DraftManifest có các trường tương ứng nhưng dùng draftDocId, draftNormalizedSha256, draftChunkId và tọa độ DRAFT. Offset là code-unit offset theo quy ước Chunker hiện hành; line/paragraph number chỉ là metadata đọc được, không phải identity.

PairMap là bảng liên kết tường minh:

~~~text
mapRevision
rawDocId + rawChunkId + rawRange
draftDocId + draftChunkId + draftRange
pairOrdinalForDisplay
mappingReason (explicit-fixture | user-confirmed | imported)
~~~

pairOrdinalForDisplay chỉ để hiển thị. Identity không được dựa vào nó. Mapping chính của E/chunk là 1 RAW range ↔ 1 DRAFT range. Một-nhiều hoặc nhiều-một chỉ được dùng khi có một CompositePair chứa danh sách thành viên đã khai báo và separator plan riêng; không tự split, zip, re-anchor, chọn cặp gần nhất hoặc dùng khoảng cách neo làm bằng chứng.

### 3.2 Identity và provenance

Các identity không được nhập nhằng:

| Identity | Cách tạo/ý nghĩa |
|---|---|
| documentId | hash của URI/display identity đã snapshot; không thay thế content hash |
| rawChunkId / draftChunkId | stable identity của range + main hash + parent id theo Chunk hiện hành |
| pairId | sha256(mapRevision + rawDocId + draftDocId + rawChunkId + draftChunkId + rawRange + draftRange) |
| runId | identity của một snapshot/đối chứng; không tái sử dụng run cũ |
| requestId | runId + pairId + promptRevision + attempt; E toàn chương dùng pseudo-pair WHOLE |
| responseHash | hash response nguyên bản, lưu ngoài source fixture nếu có văn bản riêng tư |

Mỗi candidate, structural result, semantic result và merge receipt đều phải mang runId, pairId/WHOLE, source hashes, mapping hash, prompt revision và response hash. Hash mismatch hoặc map revision mismatch là lỗi provenance/structure, không phải lỗi nghĩa.

### 3.3 Separator, layout bên trong và coverage

Chuẩn hóa identity dùng CRLF/CR → LF; snapshot vẫn giữ originalSeparatorProfile gồm LF, CRLF, CR, MIXED và trạng thái trailing newline. Mọi ký tự separator (newline, dòng trống, boundary marker như dòng chỉ có `◆`) thuộc đúng **một** trong ba loại sau, không có loại thứ tư và không có separator thuộc context:

| Loại | Là gì | Ai sở hữu | Model có được tạo/đổi không |
|---|---|---|---|
| **INTERNAL layout** | newline, dòng trống và marker nằm **giữa** hai ký tự nội dung của cùng một main range | candidate của pair đó | Có, nhưng chỉ trong phạm vi chữ ký layout đã cho phép (§4.2) |
| **BOUNDARY separator** | chuỗi separator nằm **giữa** main range k và main range k+1 (kể cả dòng trống phân đoạn), dùng chính profile của DRAFT | BoundaryPlan của run (một mục `boundaryAfter(pair k)`) | **Không**. Không có trong prompt như văn bản để sửa, không nằm trong envelope trả về |
| **DOCUMENT edge** | separator ở đầu và cuối cả văn bản (leading blank, trailing newline) | edge plan của snapshot | Không |

Quy tắc đo và ghép:

- Ranh giới của một main range là ký tự nội dung đầu và cuối của đơn vị; boundary separator liền sau nó thuộc `boundaryAfter`, không thuộc main range, không thuộc context. Context overlap không sở hữu separator nào.
- Layout signature nội bộ (`internalLayoutFingerprint`) chỉ ghi dãy: số newline mỗi chỗ ngắt, độ dài từng dòng trống, vị trí marker theo thứ tự — trên LF đã chuẩn hóa, không ghi nội dung sách. BoundaryPlan có fingerprint riêng (`boundaryPlanHash`); hai fingerprint không bao giờ so sánh với nhau và không dùng để bù cho nhau.
- Candidate chỉ trả **nội dung của main range** (gồm layout nội bộ). Đầu hoặc cuối candidate có whitespace thuộc lớp boundary (newline/dòng trống): nếu sau khi cắt đúng phần dư đó, layout nội bộ khớp chữ ký thì gate là WARN `BOUNDARY_WS_TRIMMED` và receipt ghi hash phần đã cắt; nếu cắt xong vẫn không khớp thì BLOCK. App không tự chèn hay xóa separator nội bộ để vượt gate.
- Merge dựng output bằng `edgeLead + cand₁ + boundary₁ + cand₂ + … + candₙ + edgeTrail`, theo thứ tự DRAFT range; boundary và edge lấy từ plan, không bao giờ từ response. Sau đó kiểm lại draftCoverage và rawCoverage của PairMap. Separator trùng ở biên (hai bên cùng mang) hoặc mất ở biên là lỗi `BOUNDARY_*` của merge, tách khỏi lỗi `LAYOUT` của candidate.

MergeReceipt tối thiểu gồm runId, mapHash, rawCoverageHash, draftCoverageHash, **boundaryPlanHash**, edgePlanHash, danh sách (pairId, internalLayoutFingerprint, trimmedBoundaryWsHash), ordered pair IDs, output hash, status và danh sách gap/overlap. Context không bao giờ được nối vào output.

## 4. Structural gate độc lập với semantic evaluation

### 4.1 Hai kết quả riêng

Mọi response có hai kết quả không gộp:

~~~text
structureStatus = PASS | WARN | BLOCK
semanticStatus   = NOT_RUN | NOT_MEASURED | ADJUDICATED_PASS | ADJUDICATED_FAIL | UNRESOLVED
~~~

structureStatus=BLOCK loại response khỏi semantic score, giữ response/DRAFT và chặn merge/FINAL. WARN được chuyển tiếp cho semantic evaluation nhưng không bị đếm thành MEANING/OMISSION. semanticStatus=NOT_MEASURED không được đổi thành PASS vì structural gate xanh. `structureStatus=BLOCK` cũng không phải kết luận rằng model dịch sai nghĩa: nó chỉ nói response không đủ tin cậy để ghép/giao tự động (xem “hàng rào nghi ngờ” ở §4.2).

### 4.2 Các cổng cấu trúc

Nguyên tắc: **số dòng đổi không phải bằng chứng mất nội dung và không bao giờ tự BLOCK.** Gộp hoặc tách dòng, khôi phục một đoạn bị thiếu, hay đổi cách xuống dòng đều làm số dòng đổi mà nội dung không mất. Số dòng chỉ sinh WARN `LINE_DELTA`; BLOCK dựa trên **chữ** (mất/thừa nội dung), marker biên và các lỗi định danh/phong bì.

| Gate | BLOCK khi | WARN khi | Evidence |
|---|---|---|---|
| IDENTITY | response gắn sai runId/pairId, request hash/prompt revision không khớp, map hash sai | không có | typed PAIR_ID_MISMATCH/PROVENANCE_MISMATCH |
| ENVELOPE | thiếu hoặc lặp EDITED, EDITED rỗng, thẻ lạ không tách được, finish_reason không hợp lệ | NOTES parse được nhưng có dòng không chuẩn | raw response hash + parser result |
| LAYOUT (nội bộ) | **số lượng hoặc thứ tự** boundary marker trong candidate khác DRAFT range (mất/nhân/đảo marker, ví dụ `◆`), output vượt range | marker còn đủ và đúng thứ tự nhưng không còn nằm riêng một dòng; dòng trống nội bộ đổi độ dài; `REFLOW_ONLY` (xem dưới) | `internalLayoutFingerprint` kỳ vọng/thực tế (không ghi văn bản) |
| BOUNDARY (biên ghép) | candidate mang whitespace biên mà cắt xong vẫn lệch layout; merge thấy boundary trùng hoặc mất | whitespace biên cắt được và layout khớp (`BOUNDARY_WS_TRIMMED`) | boundaryPlanHash, trimmedBoundaryWsHash |
| COVERAGE | pair gap/overlap, main range không khớp manifest, merge không exact, context bị ghép | không có | MergeReceipt và Chunker.verifyCoverage |
| SIZE (chữ) | `2c < d` (`CHARS_LOSS`) hoặc `c > 2d` (`CHARS_GROWTH`), candidate rỗng, hoặc vượt hard limit của fixture | `5c < 4d` hoặc `4c > 5d` (khi chưa tới BLOCK); hoặc `LINE_DELTA` (số dòng lệch theo bảng dưới) | số chữ trước-sau, số dòng trước-sau; không gọi đây là lỗi nghĩa |
| META | Markdown/metadata/giải thích (kể cả nhãn đoạn `⟦Pnnn⟧` của §5.1) lọt vào EDITED và không bóc tách an toàn | phần rò rỉ có thể tách mà không đổi văn bản | extracted body hash |

**Đại lượng và dấu so sánh (chốt).** L = số dòng không trống của DRAFT range, L' của candidate, Δ = |L' − L|. **Chữ** = ký tự không phải whitespace sau chuẩn hóa NFC và LF; d = chữ(DRAFT range), c = chữ(candidate), đều là số nguyên, nên mọi so sánh dưới đây dùng số nguyên, không dùng số thực: Cw = c/d chỉ để đọc. Đếm chữ bỏ qua mọi separator nên không đổi khi gộp/tách dòng.

| Điều kiện (d ≥ 80) | Kết quả | Cw tương ứng |
|---|---|---|
| `2c < d` | BLOCK `CHARS_LOSS` | Cw < 0.50 |
| `c > 2d` | BLOCK `CHARS_GROWTH` | Cw > 2.00 |
| `5c < 4d` (và không BLOCK) | WARN | 0.50 ≤ Cw < 0.80 |
| `4c > 5d` (và không BLOCK) | WARN | 1.25 < Cw ≤ 2.00 |
| còn lại | không có mã từ cổng chữ | 0.80 ≤ Cw ≤ 1.25 |

Các biên chính xác thuộc về mức nhẹ hơn: Cw = 0.50 và Cw = 2.00 là WARN (không BLOCK); Cw = 0.80 và Cw = 1.25 là không có mã. Khi d < 80 (một dòng thoại ngắn dịch ngắn hơn vẫn hợp lệ) cổng chữ không dùng tỷ lệ: BLOCK chỉ khi `c = 0` hoặc `c > 320`, và không có WARN theo chữ.

**`REFLOW_ONLY`.** `textFingerprint` = sha256 của chuỗi chữ (đã bỏ whitespace) theo thứ tự. Gate ghi `REFLOW_ONLY` (WARN, không bao giờ BLOCK, miễn marker còn đủ và đúng thứ tự) **chỉ khi cả hai điều kiện cùng đúng**: `textFingerprint(candidate) == textFingerprint(DRAFT range)` **và** cách xuống dòng khác (`L' ≠ L` hoặc `internalLayoutFingerprint` khác). Nếu candidate **nguyên văn** (bằng DRAFT range sau chuẩn hóa NFC/LF) và layout không đổi thì **không** sinh `REFLOW_ONLY` và không sinh `LINE_DELTA`; kết quả do các gate còn lại quyết định (thường PASS). Không có mã riêng cho trường hợp “không đổi”.

**Hàng rào nghi ngờ, không phải chứng minh lỗi nghĩa.** Mọi ngưỡng trong §4.2 (chữ, `LINE_DELTA`, marker, hard limit) là hàng rào nghi ngờ về cấu trúc. BLOCK nghĩa là “không đủ tin cậy để ghép hoặc giao tự động”: response bị đưa ra khỏi semantic score (`NOT_RUN`) và run `FINAL_BLOCKED`, không có nghĩa là model đã làm sai nghĩa hay bỏ nội dung có chủ đích. Ngược lại PASS/WARN không chứng minh bản sửa đúng: một câu bị bỏ mà tỷ lệ chữ còn 0.9 vẫn qua cổng, và việc đó chỉ semantic mới đo được. Cổng có thể chặn nhầm (ví dụ khôi phục một đoạn lớn làm `c > 2d`); xử lý bằng quyết định của người đọc ở một run mới, không nâng ngưỡng tại chỗ và không đổi nhãn BLOCK thành lỗi nghĩa.

**Ngưỡng `LINE_DELTA` (chỉ WARN, khóa trong fixture manifest trước khi chạy test):**

| Bậc | Điều kiện | WARN |
|---|---|---|
| Chunk thường | L ≥ 6 | Δ ≥ max(2, ⌈0.10·L⌉) |
| Chunk ngắn | 1 ≤ L ≤ 5 | Δ ≥ 1 |

Kết quả cuối của một candidate là mức nặng nhất trong các gate; vì `LINE_DELTA` chỉ có mức WARN, **bỏ riêng cổng số dòng cũng không làm đổi kết quả của bất kỳ ca BLOCK nào dưới đây**. Đây là ngưỡng an toàn cấu trúc, không phải thước đo chất lượng dịch.

**Đối chứng bắt buộc để gate không chặn nhầm (và vẫn chặn đúng):**

- *Sửa đoạn hợp lệ* (GATE-04): candidate đổi nội dung của một đoạn (thay câu, đổi đại từ, sửa số) với L' = L, Cw trong [0.97, 1.03], marker nguyên vẹn ⇒ `structureStatus=PASS`, semantic NOT_RUN→NOT_MEASURED.
- *Chunk ngắn* (GATE-05): DRAFT range 1–3 dòng sửa đúng nghĩa với L' = L ⇒ PASS dù một dòng chiếm 33–100%; L = 3 → L' = 2 là WARN `LINE_DELTA`; L = 1 bị tách thành 2 dòng là WARN; **L = 3 → L' = 1 giữ nguyên chữ là WARN `REFLOW_ONLY`, không BLOCK**; candidate rỗng là BLOCK.
- *Gộp dòng giữ nguyên chữ* (GATE-07): DRAFT range 40 dòng, candidate gộp 10 cặp dòng (L' = 30, Δ = 10, textFingerprint không đổi, marker đủ) ⇒ WARN (`REFLOW_ONLY` + `LINE_DELTA`), **không BLOCK**; gộp cả đoạn còn 1 dòng mà thứ tự chữ và marker không đổi ⇒ vẫn WARN. Đối chiếu ngược: cùng L' = 30 nhưng chữ chỉ còn 45% (số nguyên thỏa `2c < d`, ví dụ d = 1000, c = 450; Cw đúng 0.50 là WARN, không dùng làm ví dụ BLOCK) ⇒ BLOCK `CHARS_LOSS`; vậy chỉ chữ mất mới BLOCK, không phải dòng. Candidate **nguyên văn** (bằng DRAFT range, layout không đổi) ⇒ PASS, không `REFLOW_ONLY`, không `LINE_DELTA`.
- *Khôi phục đoạn thiếu* (GATE-08): DRAFT range 40 dòng thiếu một đoạn 8 dòng có trong RAW; candidate khôi phục đoạn đó (L' = 48, Δ = 8, Cw ≈ 1.20, marker đủ) ⇒ WARN `LINE_DELTA`, **không BLOCK**, vẫn chuyển cho semantic (kiểm đoạn khôi phục có đúng RAW không là việc của semantic, gate không phán). Khôi phục một câu bị thiếu trong cùng dòng (Δ = 0) ⇒ PASS. Candidate phình ra Cw > 2.00 (không phân biệt được khôi phục với chèn đệm) ⇒ BLOCK `CHARS_GROWTH`.
- *Replay N5* (GATE-02, REPLAY-N5 — giữ nguyên): 192 → 37 dòng, Cw ≈ 0.2, marker `◆` 2 → 1 ⇒ SIZE=BLOCK (`CHARS_LOSS`) và LAYOUT=BLOCK (marker mất), cộng `LINE_DELTA` WARN ghi nhận (Δ = 155); `structureStatus=BLOCK`, semantic NOT_RUN, run FINAL_BLOCKED, giữ DRAFT. Kết quả này **không phụ thuộc số dòng**: bỏ `LINE_DELTA` thì replay vẫn BLOCK bởi hai mã còn lại.

### 4.3 Candidate và semantic

CandidateChecker gọi StructuralGate trước. Khi gate không BLOCK, nó chỉ tạo candidate envelope cho semantic evaluator; nó không tự sửa quote, re-anchor, điền text thiếu hoặc chọn một response lặp khác. Semantic scorer/human adjudicator chấm riêng target, lỗi mới, collateral và repair recall. Trong gói offline này, các chỉ số đó là NOT_MEASURED nếu chưa có adjudication độc lập.

### 4.4 Đường xử lý WARN (chốt)

WARN không phải BLOCK trá hình và không phải PASS trá hình. Đường đi duy nhất:

1. Gate trả `structureStatus=WARN` kèm danh sách `warnCodes` (gate, giá trị đo, ngưỡng). Pair vào trạng thái `WARN_REVIEW`; candidate **vẫn được giữ và vẫn được ghép** (không mất công đã trả), nhưng không tính là PASS sạch.
2. Danh sách warnCodes được đưa vào semantic evaluator như điểm cần xem (giống `POINTS TO LOOK AT`), và **không** được đếm thành MEANING/OMISSION/NUMBER/NEGATION. Tính chất của WARN không đổi ngưỡng semantic.
3. Run chỉ lên `FINAL_ELIGIBLE` khi mọi pair ở `ACCEPTED` hoặc `WARN_REVIEW` và không có BLOCK/UNKNOWN/MISSING; nếu có ít nhất một `WARN_REVIEW` thì run mang cờ `warnings=N` và UI/export phải hiện "có cảnh báo cấu trúc" cùng danh sách codes, không dùng nhãn "đã đạt". Nếu không có WARN, run là `FINAL_ELIGIBLE` sạch.
4. Một WARN chỉ chuyển thành `ACCEPTED` khi người đọc/adjudicator ghi quyết định cho đúng pair đó (hash candidate + warnCodes); không có tự động nâng cấp, không có bỏ qua hàng loạt.
5. Không có vòng gửi lại để "sửa" WARN: WARN không kích hoạt provider. Đếm WARN theo gate là một số đo của báo cáo structural (W so với C).
6. Quy tắc tương thích N5: trong luồng hiện tại (không chia chunk), cờ guard `STRUCTURE_WARN/REWRITE_WARN/…` vẫn chỉ gắn nhãn; việc nâng các mức vượt ngưỡng BLOCK thành chặn là phần của gói triển khai sau và cần owner duyệt, không thay đổi trong tài liệu này.

## 5. Reference filter theo scope và context

ReferenceScopeFilter không được gọi trên toàn inputs.raw cho từng chunk. Nó nhận RawManifest của pair và trả kept, dropped, reason, matchedRegions theo thứ tự ổn định. Thứ tự đánh giá là **cố định**: (A) parse, (B) scope của row, (C) cue trong MAIN/CONTEXT, (D) dedupe có bảo toàn phạm vi. Scope của row luôn được xét **trước** khi phân vùng MAIN/CONTEXT, và chỉ trên MAIN.

### 5.1 Đặc tả scope pronoun (trước MAIN/CONTEXT)

Nguồn sự thật cho cú pháp là parser hiện hành của luồng Dịch (`PromptContextBuilder.applyScope`), không phát minh định dạng mới:

- **Giá trị `scope`** (cột 6 của Pronoun 7 cột `from,speaker,target,self,call,scope,note`): rỗng hoặc `*` = chapter-wide; `pNNN` hoặc `pNNN-pMMM` = khoảng đoạn RAW, có thể có tiền tố `CHnnn:`. Số đoạn bắt đầu từ 1, đoạn là khối không rỗng ngăn bởi dòng trống sau chuẩn hóa CRLF/CR. `p000`, khoảng ngược, tiền tố hỏng hoặc chữ lạ là scope **không hợp lệ** → fail-closed: row bị loại với reason `SCOPE_INVALID`, không bao giờ vào prompt, nhưng vẫn được ghi trong báo cáo drop. Pronoun 3 cột (legacy) không có scope: coi là chapter-wide.
- **Tọa độ đoạn.** Số đoạn trong scope là số đoạn **RAW của toàn chương**. RawManifest phải khai báo cho từng main range `rawParagraphStart/rawParagraphEnd` (đánh số toàn chương, bao gồm cả hai đầu) và `chapterId` nếu có. Số đoạn không được suy ra từ chỉ số chunk, ordinal của pair hay số dòng. Context có `contextParagraphStart/End` riêng chỉ để hiển thị, không tham gia scope.
- **Tiền tố `CHnnn:`**: khớp `chapterId` của RawManifest. Không có chapterId hoặc khác nhau → loại với `SCOPE_CHAPTER_MISMATCH` (fail-closed). Không có tiền tố → áp cho chương đang chạy.
- **Eligibility theo scope (bước B):** row hợp lệ chỉ đủ điều kiện khi `[scopeStart, scopeEnd]` giao với `[rawParagraphStart, rawParagraphEnd]` của **main range** của pair. Scope chỉ giao với đoạn context mà không giao main → loại với `SCOPE_OUTSIDE_MAIN`; context không bao giờ mở rộng scope. Ở arm W (toàn chương), khoảng so sánh là `[1, số đoạn của chương]`, nên mọi scope hợp lệ của chương đều đủ điều kiện.
- **Trường cue.** Bộ trường dùng làm cue (bước C) là một tham số của fixture manifest, `pronounCueFields`, được ghi trước khi chạy. Mặc định giữ hành vi API V1 hiện tại và N5: `from`, `speaker`, `target` (hoặc chỉ `from` với 3 cột). Luồng Dịch chỉ dùng `from`; nếu muốn thống nhất thì đó là một quyết định riêng của owner và phải áp **đồng nhất cho cả W và C** để đối chứng hợp lệ. Row không có cue nào (chỉ `*`/`-`/rỗng) là GLOBAL-cue: giữ khi scope đủ điều kiện.

Lưu ý lệch hiện có: `editorial.api.ReferenceFilter` (runtime N5) **chưa đọc cột scope** và khớp cue trên toàn RAW; đặc tả này vì vậy là yêu cầu cho gói ENGINE sau, và kết quả N5 cho pronoun không được diễn giải như đã áp scope.

### 5.2 Bảo toàn phạm vi áp dụng: effective scope, prompt và dedupe

Sau bước B mỗi row giữ một **effective scope** = scope ∩ `[rawParagraphStart, rawParagraphEnd]` của main range, biểu diễn bằng danh sách khoảng đoạn toàn chương đã gộp và sắp xếp. Row là **full** nếu effective scope phủ cả main range (mọi row chapter-wide đều full), là **partial** nếu không. Phạm vi không được biến mất ở bất kỳ bước nào sau đó:

- **Trong prompt.** Row full ghi như N5 (không thêm gì). Row partial ghi kèm `[áp dụng đoạn a–b, c–d]` (số đoạn toàn chương). Nếu prompt có **ít nhất một** row partial thì phần RAW của prompt được gắn nhãn đoạn `⟦Pnnn⟧` ở đầu mỗi đoạn (số đoạn toàn chương) để model đối chiếu; nhãn **chỉ nằm trong phần RAW**, không nằm trong DRAFT, và rò vào EDITED là lỗi META (§4.2). Prompt không có row partial thì giữ nguyên định dạng N5, không có nhãn. Model không nhìn thấy chuỗi `scope` thô, chỉ thấy phạm vi đã chuẩn hóa này.
- **Khi dedupe (bước D).** Khóa gộp là `(from,speaker,target,self,call,note)` — `from` là cue nên thuộc khóa. Các row cùng khóa gộp thành một row với effective scope là **hợp** các khoảng (không bao giờ mất khoảng nào). Hai row khác khóa **không bao giờ gộp**, kể cả khi chỉ khác `from` hoặc cùng `(speaker,target)`. Scope không nằm trong khóa gộp nhưng luôn được cộng dồn vào kết quả gộp. Lý do cần `from` trong khóa: xem phản ví dụ W/C bên dưới.
- **Xung đột (không tự suy ưu tiên).** Hai row đủ điều kiện có cùng `(speaker,target)` nhưng khác `(self,call)` mà effective scope chung ít nhất một đoạn là xung đột quy tắc, **bất kể quan hệ giữa hai scope** (bằng nhau, giao một phần hay một scope nằm trọn trong scope kia). App **không** chọn bên “cụ thể hơn”, không trừ đoạn khỏi row rộng và không suy ra ưu tiên nào: giữ cả hai row nguyên effective scope, kèm tag, và ghi `REFERENCE_CONFLICT` cùng khoảng đoạn chung (WARN của reference, chuyển semantic như điểm cần xem, không tính là lỗi nghĩa). Hai scope **rời nhau** không phải xung đột: mỗi row chỉ áp cho đoạn của mình. Quy tắc này áp giống hệt cho W và C.
- **Bất biến W/C.** Với mọi đoạn RAW p, tập rule áp dụng cho p và tập xung đột chứa p ở arm W bằng ở arm C (qua các pair chứa p), tính theo effective scope không trừ. Vi phạm là lỗi provenance của filter, không phải lỗi nghĩa.
- **Phản ví dụ W/C cho khóa dedupe thiếu `from`.** Hai row R1 và R2 giống hệt nhau ở `speaker,target,self,call,note` nhưng R1 có `from=A`, scope `p1–p10`, còn R2 có `from=B`, scope `p11–p20`; chương chỉ có A trong `p1–p10` và chỉ có B trong `p11–p20`. Nếu khóa không có `from`, hai row gộp thành một (cue giữ lại là A, scope `p1–p20`). Arm W thấy A trong chương nên áp rule cho cả `p1–p20`, kể cả `p11–p20` nơi cue thật là B; arm C với pair `p11–p20` có scope hợp lệ nhưng không thấy A trong main nên **loại** row, rule biến mất ở `p11–p20`. W và C khác nhau trên cùng đoạn: vi phạm bất biến. Khóa có `from` giữ R1 và R2 riêng: W áp R1 ở `p1–p10` và R2 ở `p11–p20`, C ở pair `p11–p20` chỉ có R2; hai arm trùng nhau từng đoạn (SCOPE-05).

### 5.3 Phân vùng MAIN/CONTEXT (bước C)

1. GLOBAL (glossary không có scope, hoặc row pronoun GLOBAL-cue đã qua bước B): giữ một lần cho mọi pair; glossary dedupe theo reference identity.
2. RAW_MAIN: glossary source và cue của pronoun chỉ match `raw.mainContent` của pair, sau khi row đã qua bước B.
3. RAW_CONTEXT: chỉ dùng vùng contextBefore/contextAfter bounded để giữ liên tục speaker/pronoun, **chỉ cho row đã qua bước B trên main**; row được đánh dấu CONTEXT_ONLY, không được coi context-only occurrence là bằng chứng target trong main range, và không đếm vào coverage. Đoạn context không nhận nhãn `⟦Pnnn⟧` làm căn cứ áp dụng scope.
4. DRAFT_MAIN/DRAFT_CONTEXT: không dùng để phát hiện glossary/pronoun. DRAFT chỉ giúp E hiểu bản nháp và giúp separator/layout gate.
5. Legacy 3-column và 7-column dùng parser hiện hành; row có cue ngoài main/context bị loại với `NO_CUE_IN_REGION`.
6. Row trùng giữa main và context gộp `matchedRegions={MAIN,CONTEXT}` thành một row, không nhân bản prompt (không phải dedupe theo scope). Prompt ghi counts theo region, theo reason bị loại và số row partial/xung đột, không đưa nội dung diagnostic lạ vào model.

### 5.4 Test của reference

SCOPE-01 (global/raw-main/raw-context/draft-only/legacy), SCOPE-02 (scope `*`, `pNNN`, `pNNN-pMMM`, `CHnnn:`, hỏng, chỉ giao context, giao main một phần, arm W), SCOPE-03 (cue fields pin; dedupe cộng dồn scope; không mất khoảng) SCOPE-04 (xung đột rule ở scope khác nhau cho W/C) và SCOPE-05 (phản ví dụ khóa dedupe thiếu `from`) phải chứng minh: glossary chỉ ở chunk khác không lọt vào pair hiện tại; pronoun chỉ ở context không thành target bắt buộc; row có scope ngoài main bị loại dù cue xuất hiện ở context; global row không bị loại; scope hỏng không bao giờ vào prompt; hai row cùng nội dung ở scope khác nhau gộp thành một row có hợp khoảng; hai row khác `(self,call)` ở scope rời nhau **không** gộp và mỗi pair thấy đúng row của mình, còn pair/arm W phủ cả hai thấy cả hai kèm tag và nhãn đoạn; mọi cặp scope chung ít nhất một đoạn (bằng nhau, giao một phần, lồng nhau) ra `REFERENCE_CONFLICT` với cả hai row giữ nguyên scope, không có ưu tiên nào được suy ra; hai row chỉ khác `from` không gộp; bất biến W/C đúng cho mọi đoạn. scope/context là provenance của filter, không phải semantic proof.

## 6. Đối chứng chính: E toàn chương so với E/chunk

Đối chứng mới không so Nhanh/Kỹ và không đưa C/C2 vào primary arm.

| Arm | Request contract | Output |
|---|---|---|
| W — E/whole | Một E cho toàn chapter; reference scope toàn chapter; WHOLE request identity | Một candidate toàn chapter, structural gate và semantic result riêng |
| C — E/chunk | Một E cho từng PairMap pair; reference scope/context của pair; không thấy main content của pair khác | Candidate từng pair, merge theo DRAFT range và MergeReceipt |

Hai arm dùng cùng RAW/DRAFT snapshot, model, Quality Core, prompt revision, separator policy, stop rule và source/reference hashes. Không chọn ngầm lượt lặp tốt nhất; repeats (nếu owner duyệt) là strata đã khai báo trước và đều phải báo.

Chỉ số báo cáo theo ba lớp:

- **Structural:** envelope/layout/coverage BLOCK/WARN/PASS, merge success, pair count, gap/overlap, process/reopen/no-resend.
- **Semantic:** target fixed, new MEANING/OMISSION/NUMBER/NEGATION, collateral, repair recall; chỉ tính trên response qua structural gate và chỉ kết luận sau adjudication.
- **Cost/provenance:** whole calls, actual pair calls, input/output tokens, actual USD, response hashes, mapping/prompt/source revision.

Structural failure không được tính là semantic error; semantic score không được dùng để che structural failure. MODEL_QUALITY=NOT_MEASURED cho tới khi người đọc phân xử phần ngoài target.

## 7. App persistence, reservation trước dispatch và merge contract

- Import từ TranslationRepository.getJob/getChunkRows chỉ đọc source, translated, stable/range/hash/status. Không gọi mutation API của job Dịch.
- Snapshot ghi runId, hai manifest, PairMap, separator plans (BoundaryPlan, edge plan), reference snapshot hash, prompt revision, pinned pricing và source bytes/hash. Mở lại đọc snapshot, không gọi lại provider.

### 7.1 Reservation trước dispatch

Mọi request trả phí (pair, WHOLE, và lần retry kỹ thuật được phép) đi qua đúng một chuỗi bước, theo thứ tự và mỗi bước ghi bền trước bước sau:

| Bước | Hành động | Ghi bền | Nếu tiến trình chết ngay sau bước này |
|---|---|---|---|
| 1 | Tính `worstCaseUsd` của request từ prompt thật: (token vào ước lượng) × giá vào đã pin + (max output token) × giá ra đã pin; giá không biết dùng mức bảo thủ | không | không có gì xảy ra |
| 2 | Kiểm trần và **reserve** trong ledger nhóm (`callId = requestId`): từ chối nếu `settled + pending + worstCaseUsd` vượt trần nhóm hoặc trần chương | dòng RESERVE trong ledger hash-chain | Reservation mồ côi (không có journal pair) → mở lại coi là **UNKNOWN**, giữ nguyên reservation, dừng nhóm |
| 3 | Journal pair `E_RESERVED` | journal | Chắc chắn **chưa gửi** → mở lại settle reservation bằng 0 với lý do `NOT_DISPATCHED`, pair về `IMPORTED` (không tự gửi lại) |
| 4 | Journal pair `E_SENT` **ngay trước** lệnh gọi provider | journal | Có thể đã gửi → `UNKNOWN`: reservation giữ pending, không resend, không ghi usage 0 |
| 5 | Gọi provider | — | như bước 4 |
| 6 | Nhận kết quả → settle theo chi phí thật (không cắt xuống mức reservation; vượt reservation là `COST_OVERRUN`, dừng nhóm, giữ bằng chứng) → journal `E_RECEIVED` | ledger SETTLE + journal | settle thiếu → coi là UNKNOWN cho tới khi đối soát |

Bước 2 bị từ chối ⇒ pair `RESERVE_FAILED`, **không dispatch**, không retry; run `INCOMPLETE` và báo cáo ghi phần đã làm. Một lỗi chứng minh chưa dispatch vẫn có tối đa một retry transport, mỗi retry là một request mới có reservation riêng; kết quả không rõ thì không retry. Reservation không phải ước tính ngân sách: ngân sách nhóm ở §9, reservation là hàng rào từng lần gọi.

### 7.2 Trạng thái pair và run

Pair state: `IMPORTED`, `RESERVE_FAILED`, `E_RESERVED`, `E_SENT`, `E_RECEIVED`, `STRUCTURE_BLOCKED`, `WARN_REVIEW`, `CHECK_PENDING` (chỉ khi C/C2 được bật, không có trong hai arm chính), `ACCEPTED`, `REJECTED`, `UNKNOWN`, `MISSING`. Chuyển trạng thái hợp lệ:

| Từ | Sang | Điều kiện |
|---|---|---|
| IMPORTED | E_RESERVED / RESERVE_FAILED | bước 2 thành công / bị từ chối |
| E_RESERVED | E_SENT | bước 4; hoặc về IMPORTED khi chết trước bước 4 (settle 0 `NOT_DISPATCHED`) |
| E_SENT | E_RECEIVED / UNKNOWN | có kết quả đọc được / không rõ kết quả |
| E_RECEIVED | STRUCTURE_BLOCKED / WARN_REVIEW / ACCEPTED | structureStatus = BLOCK / WARN / PASS |
| WARN_REVIEW | ACCEPTED / REJECTED | quyết định ghi riêng cho pair (hash candidate + warnCodes) |
| ACCEPTED, REJECTED, STRUCTURE_BLOCKED, UNKNOWN, MISSING, RESERVE_FAILED | (cuối) | chỉ thoát bằng một run mới có runId mới; không sửa tại chỗ |

Run state: `PREPARED`, `RUNNING`, `PAUSED`, `INCOMPLETE`, `UNKNOWN`, `FINAL_BLOCKED`, `FINAL_ELIGIBLE`. `FINAL_ELIGIBLE` chỉ khi mọi pair mapping hợp lệ, mọi pair ở `ACCEPTED` hoặc `WARN_REVIEW`, raw/draft coverage exact, không còn BLOCK/UNKNOWN/MISSING/RESERVE_FAILED/REJECTED và merge receipt hợp lệ; có `WARN_REVIEW` thì run mang `warnings=N` (§4.4). `FINAL_BLOCKED` khi có STRUCTURE_BLOCKED hoặc merge lỗi; `UNKNOWN` khi có pair UNKNOWN; `INCOMPLETE` khi còn pair chưa chạy (kể cả RESERVE_FAILED). Semantic quality vẫn có thể NOT_MEASURED; khi đó UI/export phải ghi rõ “chờ adjudication”, không gọi là chương đã đạt.

- Process death sau khi request đã bắt đầu (bước 4) chuyển pair thành UNKNOWN; không resend tự động và không ghi usage thành zero. Pair đã commit giữ nguyên tiến độ. Output merge ghi atomic, read-back hash; không ghi vào output URI của job Dịch.

## 8. Ma trận test offline và tiêu chí PASS

### 8.1 Test bắt buộc

| ID | Test | Expected |
|---|---|---|
| MAP-RAW-01 | RAW manifest coverage CRLF/Unicode/paragraph boundary, đánh số đoạn toàn chương | exact normalized RAW coverage, stable rawChunkId, rawParagraphStart/End đúng |
| MAP-DRAFT-01 | DRAFT manifest có separator/trailing newline riêng | exact normalized DRAFT coverage, stable draftChunkId |
| MAP-PAIR-01 | explicit 1:1 map, map hash round-trip | pairId/requestId ổn định trước-sau snapshot |
| MAP-PAIR-02 | missing/gap/overlap/hash mismatch và composite map không khai báo | typed mapping reject, không gửi E |
| ID-01 | đổi ordinal nhưng giữ ranges/hashes; sau đó đổi content hoặc map revision | lần đầu identity không đổi; lần sau identity đổi và provenance reject |
| SEP-01 | CRLF/CR/LF, blank-line run, trailing newline, context overlap | mỗi separator thuộc đúng một loại (INTERNAL/BOUNDARY/EDGE); không trùng, không mất, không thuộc context |
| SEP-02 | candidate có/không có whitespace biên; marker nội bộ đúng/sai thứ tự; merge boundary trùng/mất | whitespace biên cắt được → WARN `BOUNDARY_WS_TRIMMED`; cắt xong còn lệch → BLOCK; số lượng/thứ tự marker lệch → LAYOUT BLOCK; boundary trùng/mất → `BOUNDARY_*` của merge; `boundaryPlanHash` và `internalLayoutFingerprint` tách bạch |
| SCOPE-01 | global, raw-main, raw-context, draft-only, legacy 3/7-column | kept/dropped/matchedRegions ổn định; không leakage |
| SCOPE-02 | scope pronoun `*`, `pNNN`, `pNNN-pMMM`, `CHnnn:`, scope hỏng, chỉ giao context, giao main một phần, arm W | scope xét trước MAIN/CONTEXT và chỉ trên main; hỏng/ngoài main bị loại với reason typed; model không thấy chuỗi scope thô, chỉ thấy effective scope của row partial (§5.2) |
| SCOPE-03 | `pronounCueFields` pin; dedupe row cùng khóa `(from,speaker,target,self,call,note)` nhưng scope khác nhau; row chỉ khác `from` | cue fields đồng nhất cho W và C; row cùng khóa gộp thành một row với hợp khoảng, không mất khoảng nào; row khác khóa (kể cả chỉ khác `from`) không gộp |
| SCOPE-04 | xung đột: cùng `(speaker,target)` khác `(self,call)` ở scope rời nhau, một scope nằm trọn trong scope kia, scope giao một phần, scope bằng nhau; chạy W và C trên cùng chương | rời nhau: không xung đột, mỗi pair chỉ thấy row của mình, W thấy cả hai kèm tag + nhãn đoạn; lồng nhau/giao một phần/bằng nhau: giữ cả hai row nguyên scope + `REFERENCE_CONFLICT` với khoảng đoạn chung, **không** row nào thắng hay bị trừ; bất biến "rule và xung đột áp dụng cho từng đoạn RAW ở W = ở C"; không có row partial thì prompt giữ định dạng N5 (không nhãn) |
| SCOPE-05 | phản ví dụ W/C: R1 `from=A` scope `p1–p10`, R2 `from=B` scope `p11–p20`, các trường còn lại giống nhau; chạy W và C | R1/R2 không gộp; W áp R1 ở `p1–p10` và R2 ở `p11–p20`; C ở pair `p11–p20` chỉ có R2; áp dụng theo từng đoạn giống nhau. Test âm: khóa không có `from` phải bị phát hiện là vi phạm bất biến |
| PROMPT-01 | whole prompt và từng pair prompt | whole thấy chapter; pair chỉ thấy pair main + context được đánh dấu |
| GATE-01 | thiếu/lặp tag, empty, wrong pair, non-stop | ENVELOPE/IDENTITY=BLOCK, semantic NOT_RUN |
| GATE-02 | N5 192→37 (replay), separator loss, metadata leak | SIZE `CHARS_LOSS` + LAYOUT marker loss + META → BLOCK, `LINE_DELTA` chỉ WARN ghi nhận, FINAL_BLOCKED, giữ DRAFT; kết quả không đổi nếu bỏ cổng số dòng |
| GATE-03 | chữ ở mức WARN: `5c < 4d` hoặc `4c > 5d` nhưng chưa BLOCK; hoặc `LINE_DELTA` đơn thuần | WARN, được chuyển semantic, không tính semantic error; không BLOCK |
| GATE-04 | sửa đoạn hợp lệ: đổi câu/đại từ/số với L' = L, Cw 0.97–1.03, marker đủ | PASS, không BLOCK/WARN cấu trúc |
| GATE-05 | chunk ngắn (L = 1…5): sửa hợp lệ L' = L; L = 3→2; L = 1 tách 2 dòng; L = 3→1 giữ nguyên chữ; candidate rỗng; DRAFT < 80 chữ | PASS; WARN; WARN; WARN `REFLOW_ONLY` (không BLOCK); BLOCK; không áp ngưỡng Cw, chỉ BLOCK khi rỗng hoặc > 320 chữ |
| GATE-06 | đường WARN: pair WARN ghép được, người đọc ACCEPT/REJECT, đếm WARN theo gate | WARN_REVIEW giữ candidate, không gọi provider, `warnings=N` hiện ở UI/export; chỉ ACCEPTED khi có quyết định ghi riêng; không bị đếm thành lỗi nghĩa |
| GATE-07 | gộp dòng giữ nguyên chữ: 40→30 (10 cặp), 40→1, cùng 30 dòng nhưng chữ còn 45% (d = 1000, c = 450, `2c < d`); candidate nguyên văn bằng DRAFT range | WARN `REFLOW_ONLY`+`LINE_DELTA`; WARN; BLOCK `CHARS_LOSS` (chỉ ca mất chữ bị chặn); nguyên văn + layout không đổi → PASS, **không** `REFLOW_ONLY`/`LINE_DELTA`; `REFLOW_ONLY` đòi `textFingerprint` bằng nhau **và** layout khác |
| GATE-08 | khôi phục đoạn thiếu: +8 dòng/Cw ≈ 1.20; khôi phục một câu trong cùng dòng; Cw > 2.00 | WARN `LINE_DELTA` (không BLOCK, chuyển semantic); PASS; BLOCK `CHARS_GROWTH` |
| GATE-09 | dấu so sánh ngưỡng chữ ở biên (d ≥ 80): `2c = d`, `2c = d − 1`, `c = 2d`, `c = 2d + 1`, `5c = 4d`, `5c = 4d − 1`, `4c = 5d`, `4c = 5d + 1`; và d < 80 với `c = 0`, `c = 320`, `c = 321` | WARN; BLOCK `CHARS_LOSS`; WARN; BLOCK `CHARS_GROWTH`; không có mã; WARN; không có mã; WARN; BLOCK; không có mã; BLOCK. Tính bằng số nguyên; kết quả giống nhau trên mọi nền tảng |
| MERGE-01 | accepted pairs ordered, duplicate, gap, overlap, context | exact DRAFT merge hoặc typed MERGE_*; không FINAL khi lỗi |
| MERGE-02 | RAW coverage và DRAFT coverage khác nhau | cả hai receipt được kiểm, không dùng raw ordinal cho draft |
| RES-01 | reservation trước dispatch: reserve bị từ chối vì trần; chết sau RESERVE trước journal; chết sau E_RESERVED trước E_SENT; chết sau E_SENT; settle vượt reservation | RESERVE_FAILED không gọi provider; reservation mồ côi → UNKNOWN; E_RESERVED → settle 0 `NOT_DISPATCHED`, không tự gửi; E_SENT → UNKNOWN pending; vượt → COST_OVERRUN, dừng, giữ bằng chứng; không ghi usage 0 khi UNKNOWN |
| SAVE-01 | reopen sau E_RECEIVED/WARN_REVIEW/process death | state/progress/hash còn nguyên, không resend committed pair |
| JOB-01 | import ngoài job Dịch và import từ job Dịch | đối chứng ngoài không mutation; job Dịch byte/status/ledger bất biến |
| UNKNOWN-01 | unknown sau send ở pair giữa | prior accepted giữ, pair UNKNOWN, run UNKNOWN/FINAL_BLOCKED |
| COMPARE-01 | cùng snapshot chạy W và C | actual calls = chapters và actual pairs; không chọn repeat tốt nhất |
| REPLAY-N5 | replay private response/hash hoặc redacted structural twin (192→37, marker 2→1) | cùng structural codes như GATE-02 (SIZE `CHARS_LOSS`, LAYOUT marker loss; `LINE_DELTA` chỉ WARN); semantic NOT_RUN; không tạo semantic PASS/FINAL; kết quả giữ nguyên khi bỏ cổng số dòng |
| QA-01 | secret/private-text scan và evidence manifest | không lộ khóa/sách/response; evidence paths/hash giữ nguyên |

### 8.2 PASS gate

Gói offline được gọi **PASS** chỉ khi tất cả điều kiện sau có evidence process thật hoặc fixture hash-bound:

1. MAP-*, ID-*, SEP-* (gồm SEP-02 tách boundary khỏi layout nội bộ) đạt 100%; hai manifest phủ đúng riêng và PairMap explicit, không có zip/re-anchor ngầm.
2. SCOPE-* (gồm scope pronoun xét trước MAIN/CONTEXT, bảo toàn phạm vi trong prompt/dedupe với `from` trong khóa, xung đột cùng phạm vi được giữ không suy ưu tiên, và phản ví dụ W/C ở SCOPE-04/05) đạt 100%; reference ngoài scope/context-only không leakage, global row được giữ đúng, prompt counts tái lập.
3. GATE-* đạt 100%, gồm sửa đoạn hợp lệ (GATE-04), chunk ngắn (GATE-05), đường WARN (GATE-06), gộp dòng giữ nguyên chữ (GATE-07) khôi phục đoạn thiếu (GATE-08) và dấu so sánh biên (GATE-09), với bảo đảm số dòng không bao giờ tự BLOCK, `REFLOW_ONLY` không sinh khi nguyên văn, và các ngưỡng chỉ là hàng rào nghi ngờ; structural BLOCK/WARN tách khỏi semantic result; N5 replay đúng STRUCTURE_BLOCK + semantic NOT_RUN + FINAL_BLOCKED.
4. MERGE-* đạt 100% trên case hợp lệ và trả đúng typed failure trên gap/overlap/context; không ghi job Dịch.
5. RES-*, SAVE-*, JOB-*, UNKNOWN-* chứng minh reservation trước dispatch, snapshot, tiến độ, no-resend và read-only provenance.
6. COMPARE-01 chứng minh cùng snapshot chạy được đối chứng W/C theo số cặp thực, không chọn ngầm repeat tốt nhất; semantic quality vẫn NOT_MEASURED nếu chưa adjudicate.
7. QA-01 đạt và diff không chứa private text/credential/evidence mutation.

### 8.3 Trạng thái bằng chứng hiện tại

Đã có tiền đề từ Chunker.verifyCoverage/ReliabilityV43Test, EditorialApiRunServiceTest, TranslationRepository và báo cáo N5 structural-loss. Chưa có evidence runtime cho hai manifest, PairMap, separator receipt, scope/context filter per pair, W-vs-C comparator hoặc replay ở lớp mới. Vì vậy bản tài liệu này **chưa tự nhận PASS runtime**.

## 9. Đề xuất đo sau khi gói offline PASS

Chỉ mở một ledger mới sau khi owner duyệt cap được tính từ manifest. Không lấy số dư N5 làm ngân sách mặc định.

### 9.1 Đơn vị tính

Với N chapter đã khóa và P_i là số pair hợp lệ thực tế trong PairMap của chapter i:

~~~text
P_total       = Σ P_i
paidCalls_W   = N
paidCalls_C   = P_total
basePaidCalls = N + P_total
~~~

Không có paid retry sau khi request đã gửi. Một lỗi chứng minh chưa dispatch được retry tối đa một lần ở lớp transport, và retry đó là một request mới có reservation riêng (§7.1); lỗi UNKNOWN dừng nhóm và không retry. Do đó call cap trả phí được tính theo N + P_total, không theo con số giả định cố định; số reservation bị từ chối hoặc hoàn về `NOT_DISPATCHED` không phải là lượt gọi trả phí và được báo riêng.

### 9.2 Ngân sách

Dry-run phải tính estimateWhole_i và estimateChunk_ij từ prompt thật, reference scope thật và model/prompt revision đã pin, và tính **reservation từng request** `worstCase_k` (§7.1) theo cùng giá đã pin. Thứ tự chạy là một phần của manifest (mặc định W trước, rồi C, từng chapter theo thứ tự đã khóa), vì cap phải đủ cho điểm đỉnh của chuỗi reservation chứ không chỉ cho tổng ước tính:

~~~text
baseUsd         = Σ estimateWhole_i + Σ estimateChunk_ij
peakExposureUsd = max over k ( Σ_{j<k} estimate_j + worstCase_k )
reserveUsd      = roundUpToCent( max(1.20 × baseUsd, peakExposureUsd) )
~~~

reserveUsd là cap của nhóm nếu không vượt hard ceiling owner đã duyệt; nếu tính ra vượt ceiling, không dispatch mà trình lại cap. Nếu giữa chừng `settled + pending + worstCase_k` vượt cap thì request k bị `RESERVE_FAILED` (không dispatch) và run `INCOMPLETE`; không nâng cap tại chỗ. Không dùng estimate để ghi actual cost; actual cost đến từ ledger.

Ví dụ minh họa, không phải ngân sách được cấp: nếu N=3, manifest có P_i=3,5,7 (P_total=15), dry-run ra USD 0.016 cho mỗi whole E và USD 0.006 cho mỗi chunk E, thì baseUsd=3×0.016+15×0.006=0.138 và 1.20×baseUsd=0.1656. Giả sử reservation worst-case của whole E là USD 0.045 và của chunk E là USD 0.012, chạy W trước C: điểm đỉnh là ở chunk cuối, 0.132 đã tiêu + 0.012 = 0.144, nhỏ hơn 0.1656, nên cap dự phòng làm tròn là **USD 0.17** và paid call cap là **18**. Nếu thứ tự đổi (ví dụ một whole E ở cuối) thì đỉnh có thể là 0.138 − 0.016 + 0.045 = 0.167 và cap phải tính lại theo `max(...)` ở trên. Nếu PairMap thực tế có số khác, cap và call count phải đổi theo số đó.

### 9.3 Cách so sánh

Arm W và C chạy cùng N chapter, cùng snapshot, cùng stop rule, cùng reference policy (kể cả `pronounCueFields`, §5.1) và cùng cơ chế reservation trước dispatch (§7.1). Không chọn arm theo khoảng cách neo, một repeat tốt nhất hoặc structural warning bị bỏ qua. Báo cáo phải có bảng chapter/pair, structural status, semantic status, merge receipt, actual cost và lý do stop. Semantic score chỉ được tính sau adjudication độc lập; trước đó ghi NOT_MEASURED.

Một bước tiếp theo duy nhất: owner review revision CP-OFFLINE-2, đặc biệt structural gate và công thức N + P_total, rồi quyết định có mở gói triển khai engine/app/QA offline hay tiếp tục giữ chunking sau N6. Không có quyền build/device/provider/live từ tài liệu này.

## 10. Triển khai (revision `CP-IMPL-1`, 2026-10-06)

Mục này là hợp đồng triển khai của đợt viết mã; nó không thay đổi các ngưỡng ở §4. Nguồn sự thật của ngưỡng chữ là công thức số nguyên ở §4.2 và GATE-09; không sửa ngưỡng để test PASS.

### 10.1 Ghim nội dung đặc tả

Tên `CP-OFFLINE-2` không đủ phân biệt các lần chỉnh. Evidence của đợt này ghi (a) revision triển khai `CP-IMPL-1` (hằng `PairContract.REVISION` trong engine), (b) SHA-256 của chính file này tại commit được báo cáo, (c) commit nguồn. Báo cáo bàn giao nêu cả ba; một PASS gắn với tổ hợp đó, không gắn với tên chung.

### 10.2 Hai giới hạn MVP

1. Đường chạy được trước tiên là **job Dịch** có liên kết RAW–translated kiểm chứng được: RAW = các `source` của job (phủ chuẩn hóa nguồn đúng một lần, kiểm bằng `Chunker.verifyCoverage`), DRAFT = các `translated` đã hoàn tất, theo cùng thứ tự row; liên kết là chính row (một row = một cặp). Row chưa hoàn tất hoặc rỗng là cặp THIẾU (không giả thành hoàn chỉnh) và run không chạy được cho tới khi job có đủ.
2. **File nhập ngoài** được lưu thành tổ hợp và xem trước (tên file, số ký tự, mô tả nguồn) nhưng **chỉ chạy chunk khi có PairMap hợp lệ** do nguồn khai báo tường minh. App không tự cắt hai file độc lập rồi ghép theo thứ tự, tên file hay độ giống; không có mapping thì hiển thị lý do "chưa thể chạy theo cặp" và chỉ có luồng toàn chương cũ.

Một cặp có ước lượng đầu vào vượt `SourceCheck.MAX_ESTIMATED_TOKENS` là `TOO_LONG`: run không chạy được, lý do hiển thị rõ; không tự chia RAW riêng rồi gán cả DRAFT cho phần con.

### 10.3 Ownership theo file và thứ tự phụ thuộc

Một writer cho mỗi file; thứ tự là thứ tự phụ thuộc (mỗi bước chỉ dùng bước trước).

| # | Ownership | File chính | Đầu ra / test |
|---|---|---|---|
| 1 | ENGINE | `editorial/api/pair/PairText`, `DocManifest`, `PairMap`, `BoundaryPlan` | manifest hai phía, PairMap tường minh (kể cả composite khai báo), identity, bounds/gap/overlap/hash; no-op merge; test Unicode/CRLF/trailing newline/đoạn rỗng/hash sai/thiếu cặp/composite |
| 2 | ENGINE | `ReferenceProjector` | parse+kiểm scope trước cue, effective scope, dedupe có `from`, xung đột giữ nguyên, partial tag; test W/C (scope rời, lồng, bằng nhau, giao một phần, cue chỉ trong context, thiếu `from`) |
| 3 | ENGINE | `StructuralGate` | công thức số nguyên, `LINE_DELTA`, `REFLOW_ONLY`, marker, META, ENVELOPE, IDENTITY; GATE-01…09, replay N5 192→37 |
| 4 | ENGINE | `PairPromptBuilder`, `ChunkMerge`, `PairStates` | prompt E theo cặp (RAW chính, DRAFT, context tách bạch, reference đã chiếu, nhãn RAW); merge có receipt; trạng thái pair/run và recovery; MERGE-*, RES-01 (mô hình thuần) |
| 5 | APP | `PairSourceFromJob`, `EditorialPairRunService`, `PairRunStore` (+ SQLite, bộ nhớ), migration v27 | import read-only từ job Dịch, snapshot, reservation → journal → gọi → settle, UNKNOWN không resend, phục hồi theo cửa sổ ngắt, export atomic |
| 6 | UI | `EditorialApiPairPage`, `UiController`, `Presenter` | chọn job/tổ hợp, bỏ chọn glossary/pronoun, xem cặp thiếu/cảnh báo, tiến độ, kết quả, diff, xuất có nhãn |
| 7 | QA | test engine/app/androidTest (NOT_RUN trên thiết bị), fake provider đa dạng, replay N5 | PASS/FAIL/NOT_RUN theo claim → evidence → kết luận |

### 10.4 Việc app giữ, không giao cho model

Hash, ID nội bộ, đếm dòng/ký tự, journal, ledger, receipt, diff và nhãn trạng thái do app tạo. Prompt không yêu cầu model tính hash, đếm, báo cáo nhiều tầng hay tạo chứng từ; đầu ra của model là `<EDITED>` (và `<NOTES>` tùy chọn) như N1.

### 10.5 Phạm vi chưa chạy

Không thiết bị/emulator, không provider thật, không push. Các test androidTest và process-death thật chỉ được viết và compile (NOT_RUN). Tài liệu này không công nhận ba chương FINAL hay P7.
