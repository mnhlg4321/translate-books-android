# Editorial API V1 — N5 adjudication (2026-10-07)

## Phạm vi và bằng chứng

Đây là phân xử offline của đủ 24 run N5 (12 Nhanh, 12 Kỹ) sau khi 4A đã PASS. Văn bản RAW/DRAFT, glossary, Pronoun, prompt và response chỉ được đọc từ `D:\P5E-private`; Git chỉ giữ line ID, hash và nhãn phân loại. Không chỉnh response, fixture, prompt, ngưỡng hay acceptance; không chọn ngầm lượt lặp tốt hơn để thay lượt cơ sở.

Bản ghi chi tiết không chứa văn bản sách: `D:\P5E-private\n5-adjudication\20261007\adjudication.json` (SHA-256 `B9C10626B123EB3E368AA854F870F4AA8FF0E2975A5556ED7D704417633249F1`). Nguồn run giữ nguyên tại `D:\P5E-private\p6-runs\<run-id>`; nguồn fixture giữ nguyên tại `D:\P5E-private\p6-fixtures\<fixture>`.

## Phương pháp

- Mỗi response được so với DRAFT cùng fixture rồi đối chiếu RAW, glossary và Pronoun scope; line ID là vị trí DRAFT/RAW đã khai, không phải bằng chứng duy nhất.
- Chuẩn hóa chỉ dùng để so sánh: NFC và trim biên; bỏ `《…》` chỉ khi đối chiếu quote của source-role RAW. Không tự đổi line, không tự tìm lại quote ở vị trí khác và không biến structural/semantic FAIL thành PASS.
- `IMPROVEMENT` = target gieo hoặc câu thiếu được sửa đúng theo RAW, hoặc sửa collateral có căn cứ; `NEUTRAL` = marker/layout, paraphrase giữ invariant hoặc target vẫn bỏ sót mà không tạo lỗi mới; `NEW_ERROR` = response làm sai invariant/omission/number/negation/speaker/pronoun/glossary hoặc mất nội dung.
- `fx-a02` trừ lỗi đã có trong DRAFT `E_L245_MEANING`; line đó không bị response mới chạm tới. `fx-a12` là ambiguous control, không tự coi paraphrase là lỗi.

## Bảng 24 run

| Run | Arm | Fixture | Response hash | Phân loại | Target / line ID | Collateral changed (count) | Mã lỗi mới | USD |
|---|---|---|---|---|---|---:|---|---:
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a02 | `997d01221ea2f099c52d389cecc54b7f18ddca0e10d8a56ac2f3a8092cfa731d` | **NEUTRAL** | — | 6 | — | 0.01070555 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a03 | `c86cc9f5025db83707b57dc434fffc07657791ef4d7ef3f31b1681d644a86da9` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99 | 4 | — | 0.01015768 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a04 | `e2cbde2310b758b5af1dbd454a1e8be04c6e02c7a41b7d94dfc95038c26e8dce` | **IMPROVEMENT** | T-S2:RESTORED_EXACT@L71 | 5 | — | 0.01016918 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a05 | `8da12d32e7aed3ada6bb702ea1ef94e6e76ac68489a3524f212fae23af84c737` | **NEUTRAL** | T-S3:MISSED@L327 | 6 | — | 0.01005213 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a07 | `3e3738be279ea8c8d0ed46a8324796045c7ba04967e25f1a43d9f3124d5244f1` | **IMPROVEMENT** | T-S4A:FIXED_MATCH@L5 | 6 | — | 0.01014783 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a08 | `50b827b1994e59cad02a6dc0c1db4429acdc4d0b3354bf904929613560ec9007` | **IMPROVEMENT** | T-S4B:FIXED_MATCH@L47 | 3 | — | 0.01038278 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a11 | `7678466a8a62da709908e28076f87908605de8d0fae2ce1196444bd91049a237` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99; T-S2:RESTORED_EXACT@L71; T-S3:FIXED_SEMANTIC_REVIEW@L327; T-S4A:FIXED_MATCH@L5; T-S5:FIXED_MATCH@L13; T-S6:FIXED_MATCH@L91 | 4 | — | 0.01005648 |
| `442ad6c1-404d-44b2-a5cc-303d8756c23a` | Nhanh | fx-a12 | `1d23153b0ed96cc53b1165a78363609a20d44dbd8bcf3fc6b3adab186f3962e7` | **NEUTRAL** | — | 5 | — | 0.01011448 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a02 | `3e3738be279ea8c8d0ed46a8324796045c7ba04967e25f1a43d9f3124d5244f1` | **NEUTRAL** | — | 6 | — | 0.01152576 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a03 | `d531f2cb99085887440055f0b987e1a753e94d2ee50081991f581a73e6d2a58e` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99 | 20 | — | 0.01182212 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a04 | `7c8a177826b343cbfad72db379f60e491089509eb7e72b273d1f8cc193bc5e7d` | **NEW_ERROR** | T-S2:RESTORED_EXACT@L71 | 309 | OMISSION@L74,381 | 0.00623703 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a05 | `87aba80be69ad2df92c2c4e5f8b9ffe95e08f59648e7cd34180e917ff467661e` | **NEUTRAL** | T-S3:MISSED@L327 | 5 | — | 0.01189474 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a07 | `6f96c3a4fa4791eded9c2a0c470b6131bb9581282c77acec693e7b87bbc9126e` | **IMPROVEMENT** | T-S4A:FIXED_MATCH@L5 | 4 | — | 0.01595112 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a08 | `63ce6b942355cdc4101c51d8b8d3f2e14468471208c89c36648f2f187761ddb0` | **IMPROVEMENT** | T-S4B:FIXED_MATCH@L47 | 4 | — | 0.01694860 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a11 | `c70d6b5603575829c35377d588565c766a824bc8d67d2628b34966e3fface5ea` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99; T-S2:RESTORED_EXACT@L71; T-S3:MISSED@L327; T-S4A:FIXED_MATCH@L5; T-S5:FIXED_MATCH@L13; T-S6:MISSED@L91 | 5 | — | 0.01628192 |
| `820a09ae-db0f-4884-a970-87a94e5eb89f` | Ky | fx-a12 | `303afef4274849b57107f6de341be641c5a11b60ac24d553155ca9699fbcec89` | **NEUTRAL** | — | 5 | — | 0.01626774 |
| `42ac94cb-46ab-4620-892b-5b1d731dcd87` | Nhanh | fx-a04 | `e2cbde2310b758b5af1dbd454a1e8be04c6e02c7a41b7d94dfc95038c26e8dce` | **IMPROVEMENT** | T-S2:RESTORED_EXACT@L71 | 5 | — | 0.00751906 |
| `42ac94cb-46ab-4620-892b-5b1d731dcd87` | Nhanh | fx-a11 | `be08c59de92318f727e3d3b7db8ef60778fe1f40c9b2874c56cbff15ae0a16c3` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99; T-S2:RESTORED_EXACT@L71; T-S3:MISSED@L327; T-S4A:FIXED_MATCH@L5; T-S5:FIXED_MATCH@L13; T-S6:MISSED@L91 | 4 | — | 0.00737238 |
| `ea17ed65-4b6e-42cc-b969-3d530d00fdd2` | Nhanh | fx-a04 | `a46adee3fe9192fb921ffc96d2db3200affeef1313c401613c6fa58cc40cdac6` | **IMPROVEMENT** | T-S2:RESTORED_EXACT@L71 | 6 | — | 0.00766666 |
| `ea17ed65-4b6e-42cc-b969-3d530d00fdd2` | Nhanh | fx-a11 | `79d0069efd470ac0a3fa0a07fa27686cc684fd2cd1779c0e5635c6912349a3cd` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99; T-S2:RESTORED_EXACT@L71; T-S3:MISSED@L327; T-S4A:FIXED_MATCH@L5; T-S5:FIXED_MATCH@L13; T-S6:MISSED@L91 | 3 | — | 0.00731718 |
| `ec0babff-9eed-419f-922b-630371ea4568` | Ky | fx-a04 | `50b827b1994e59cad02a6dc0c1db4429acdc4d0b3354bf904929613560ec9007` | **IMPROVEMENT** | T-S2:RESTORED_EXACT@L71 | 3 | — | 0.01138758 |
| `ec0babff-9eed-419f-922b-630371ea4568` | Ky | fx-a11 | `79d0069efd470ac0a3fa0a07fa27686cc684fd2cd1779c0e5635c6912349a3cd` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99; T-S2:RESTORED_EXACT@L71; T-S3:MISSED@L327; T-S4A:FIXED_MATCH@L5; T-S5:FIXED_MATCH@L13; T-S6:MISSED@L91 | 3 | — | 0.00896664 |
| `c1325bd8-0845-4dfe-badb-a5d26f0e0d54` | Ky | fx-a04 | `5dfc93d52166f8f953bd99e2b5cfdc365e34640035ea4e169e153a8cc8d3dc70` | **IMPROVEMENT** | T-S2:RESTORED_EXACT@L71 | 5 | — | 0.01141918 |
| `c1325bd8-0845-4dfe-badb-a5d26f0e0d54` | Ky | fx-a11 | `f3b3de7ad255dc7cecf37d3c508895b98408f07e9d7cacc5da5989b179804c4f` | **IMPROVEMENT** | T-S1:FIXED_MATCH@L99; T-S2:RESTORED_EXACT@L71; T-S3:MISSED@L327; T-S4A:FIXED_MATCH@L5; T-S5:FIXED_MATCH@L13; T-S6:FIXED_MATCH@L91 | 10 | — | 0.01527122 |

## Phân xử nổi bật

- Chỉ có một `NEW_ERROR`: Kỹ base `fx-a04`, response `7c8a177826b343cbfad72db379f60e491089509eb7e72b273d1f8cc193bc5e7d`. Model sửa target role ở `L71` nhưng trả 37/192 dòng; phần `L74–L381` mất là `OMISSION`, finish `stop`, không được coi là bản cuối hợp lệ.
- Nhanh base `fx-a11` sửa đúng `T-S3:MISSING_SENTENCE` tại `L327`; scorer ghi `CHANGED_UNVERIFIED` vì câu được chèn làm dịch line, nhưng đối chiếu RAW xác minh đúng nên phân loại `IMPROVEMENT`, không tính là lỗi mới.
- Nhanh và Kỹ trên `fx-a02` chỉ có thay marker/paraphrase ở line ID được liệt kê; `E_L245_MEANING` là lỗi có sẵn trong DRAFT và không bị response mới tạo. `fx-a12` không có lỗi invariant xác nhận.
- Các thay đổi thuật ngữ như `guild/hội`, `khu dân cư`, `nhóm/những người` chỉ được giữ là `NEUTRAL` hoặc `IMPROVEMENT` khi invariant RAW không đổi; không dùng số dòng, khoảng cách neo hay điểm máy làm bằng chứng semantic duy nhất.

## Quyết định chế độ mặc định

| Tiêu chí mục 6 | Kết quả |
|---|---|
| Recall máy | Nhanh 19/25; Kỹ 19/25 (cùng 25 cơ hội) |
| Phân xử độc lập | Nhanh có thêm T-S3 `fx-a11` được xác minh; Kỹ có 1 `NEW_ERROR:OMISSION` do run cụt |
| Chi phí | Nhanh 12 calls / USD 0.11166139; Kỹ 29 calls / USD 0.15397365 |
| Clean/ambiguous gate | Không có lỗi mới loại MEANING/OMISSION/NUMBER/NEGATION được xác nhận trên `fx-a02` sau khi trừ `E_L245_MEANING` hoặc trên `fx-a12`; không có truncation ở Nhanh |

**Kết luận:** chọn **Nhanh (E)** làm chế độ mặc định theo plan §6. Kỹ giữ làm tùy chọn, nhưng không được coi run cụt là PASS và không được chạy 4C bằng response đó. Acceptance, prompt và ngưỡng không đổi; semantic quality của model ngoài ma trận seeded vẫn `NOT_MEASURED`.

## Gate tiếp theo

4B offline đã đạt và được ghi vào plan §6. 4C chỉ được xem xét sau khi kiểm tra thực tế có input owner trong `D:\P5E-private\n6-inputs\`; nếu thiếu input thì dừng. Khi chạy, dùng whole flow, chế độ Nhanh, model/ledger/cap đã duyệt trong D-N6; không chạy chunk-pair live và không chạm pilot.
