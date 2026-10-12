# P6 R6 — phân xử ngữ nghĩa G1 và tác động normalization (2026-10-05)

Trạng thái: hoàn tất offline, hồi cứu sau Z5. Không provider call, không APK, không emulator/pilot. Đây là đánh giá độc lập của detection; nó không thay đổi validator, acceptance, response, fixture, ledger hay scorer đóng băng.

## Phạm vi và phương pháp

Đã kiểm đủ 12 output L1_RECONCILE của G1: lượt cơ sở gồm fx-a03, fx-a04, fx-a05, fx-a07, fx-a08, fx-a11, fx-a02, fx-a12; hai lượt lặp lần lượt gồm fx-a11/fx-a04. Mỗi target được tính một lần trong từng run có target đó. Không chọn ngầm lượt lặp tốt hơn. Mỗi finding được đọc đối chiếu RAW, DRAFT, glossary và Pronoun scope ở bản private; số dòng chỉ là điểm bắt đầu kiểm, không phải bằng chứng duy nhất.

Evidence private giữ nguyên tại D:\P5E-private\p6-runs\<run-id>\results\<fixture>\responses\002-L1_RECONCILE.json. Bảng dưới ghi SHA-256 của đúng response đó, không ghi nội dung sách.

Các dấu neo dùng trong bảng:

- raw L<n> là dòng RAW trước khi parser đổi tham chiếu wire L<n> thành u:<line>:<hash>.
- draft L<n> là anchor đã commit trong REPORT_L1. d=0 nghĩa là draftAnchorDerivedFromQuote=0 và maxDraftAnchorDeviation=0.
- MATCH T-* chỉ detection của target; nó không nói target đã được sửa. G1 chạy L1_ONLY, nên repair là NOT_MEASURED.

## 12 output đã khóa

Tất cả response dùng safe4.l1.reconcile-ledger.wire.v3, report safe4.full.report-l1.v2, contractRevision=L1_LEDGER_V10; nguồn RAW chung có rawSha256=a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be, inventorySha256=1e2360ee616ae6a487ebeca65025b93415d29f78242e6582c6e4c7cb173db64f, 191 unit. Manifest fixture cơ sở SHA-256 8c3868ba9710fbf13822fc9e9662391a37b84518d30bcc6f0317920282bd9309; manifest hai lượt lặp SHA-256 973be41be221dd98b329d070d50e203e326bff743123ea553b4c947a1bf0d6f8; pack/profile lần lượt 497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d và beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21.

| nhóm | run id | fixture | response SHA-256 | target detection sau phân xử | findings ngoài target | normalization dup/speaker/anchor |
|---|---|---|---|---|---|---:|
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a02 | 46004a957a6ec51f50417fd77254987357a39c44daebeb9ed7f3892f3d11b689 | control, không có target; không coi là clean sau khi đọc finding | E_L245_MEANING confirmed additional defect | 1/1/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a03 | 6dd1e64cc8c8c17b9ee8f48e49fe338d038cfc47c0d461d63c79c10a0fa18d1b | T-S1 MATCH (E-L99-UNTRANSLATED) | E-L63-ROLE preference | 2/0/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a04 | 39dc1fbcb7a13f9a96f0cee497d8514e0061f229f3cdb092e15ec9deebeb304a | T-S2 MISSED | — | 0/0/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a05 | 632cf32051f7ab40225a6fb63b0e400cf76aa45a096e40ec546fe7e96bd26557 | T-S3 MISSED | E-L115-01 false positive; E-L105-01 confirmed additional defect; E-L182-01 unresolved | 3/0/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a07 | 6c26328b376f689e2fa1641b24c2215317ec5693ac22f760375566423bed47c7 | T-S4A MATCH (E-L5-NUMBER) | — | 1/0/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a08 | cff71f430bde93b48c3959b9e7a565314d75440bbb5185a6ad622dbece099536 | T-S4B MISSED | — | 0/0/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a11 | a156521410ff0daf8a87445a0dd53c4b7e7a9f0030bacf90a41152861e1bdef8 | T-S1, T-S4A MATCH; T-S2, T-S3, T-S5, T-S6 MISSED | E3 false positive | 3/3/0 |
| base | 74a53417-249b-419f-8b29-33f0f30fa5f9 | fx-a12 | 2b135e9e747b10b75803636cb0e1f549f3c453a3742badb30044af384155243d | ambiguous control, no target; no finding retained | — | 0/1/0 |
| repeat 1 | 5d438ce3-1b6b-4fcd-8434-aa98deaa84a1 | fx-a04 | 8847d80731155f5af751dc2227de32f93bdaeb94b55a41460240381788bb0f93 | T-S2 MATCH (E-L71-FACTION) | — | 1/0/0 |
| repeat 1 | 5d438ce3-1b6b-4fcd-8434-aa98deaa84a1 | fx-a11 | cdc5d0030f6775b1a60975bd183704f2d3405748944d690c4aff60ed6001d086 | T-S1, T-S4A MATCH; T-S2, T-S3, T-S5, T-S6 MISSED | — | 2/0/0 |
| repeat 2 | 87f9fe8d-3e50-46e8-8775-dc37080889eb | fx-a04 | f909e948390b5017d6fee126108f6c53d4c800cd4db58ab41d4e0a56845e1d27 | T-S2 MISSED | — | 0/8/0 |
| repeat 2 | 87f9fe8d-3e50-46e8-8775-dc37080889eb | fx-a11 | 175e5339c9e15bcff7d09e2cca435fc3261038202f48a56a465f667de1f5f42a | T-S1, T-S2, T-S4A MATCH; T-S3, T-S5, T-S6 MISSED | E003 false positive | 4/0/0 |

dup/speaker/anchor là tổng số mục bị loại khỏi list / bản ghi speaker bị loại / anchor DRAFT được app dời. Trên 12 run: duplicateReferencesRemoved=17, speakerRecordsDropped=13, draftAnchorDerivedFromQuote=0, độ lệch lớn nhất 0.

## Detection theo target

| target | lớp | số cơ hội | detected | missed | finding khớp | kết luận |
|---|---|---:|---:|---:|---|---|
| T-S1 | untranslated | 4 | 4 | 0 | E-L99-UNTRANSLATED, E2, E-UNTRANS-001, E004 | target rõ; furigana chỉ ảnh hưởng so khớp quote, không đổi nghĩa lỗi |
| T-S2 | role/faction swap | 6 | 2 | 4 | E-L71-FACTION, E002 | không suy từ khoảng cách; phải kiểm đúng hai phe và chiều so sánh |
| T-S3 | missing sentence | 4 | 0 | 4 | — | không có finding hợp lệ |
| T-S4A | number | 4 | 4 | 0 | E-L5-NUMBER, E1, E-NUM-001, E001 | đúng số liệu RAW/DRAFT |
| T-S4B | negation | 1 | 0 | 1 | — | không có finding |
| T-S5 | glossary term | 3 | 0 | 3 | — | không có finding |
| T-S6 | address profile | 3 | 0 | 3 | — | không có finding |

Tổng seeded-target opportunities là 25, detected 10, missed 15; đây là retrospective detection, không phải repair recall. Single-target runs là 3/7; fx-a11 lần lượt 2/6, 2/6, 3/6. Không chọn lượt lặp tốt nhất để thay thế run cơ sở.

## Phân xử từng finding

| run/fixture | finding | neo RAW → DRAFT trước/sau | phân loại | lý do ngắn |
|---|---|---|---|---|
| base fx-a02 | E_L245_MEANING | L245 → L243, giữ; d=0 | CONFIRMED_ADDITIONAL_DEFECT | RAW nói đêm là thời gian nên hoạt động; DRAFT đảo thành không thích hợp. Control danh nghĩa không phải bằng chứng sạch. |
| base fx-a03 | E-L63-ROLE | L63 → L63, giữ; d=0 | PREFERENCE | Cụm tiếng Việt vẫn đọc được là các tinh nhuệ làm hộ vệ cho Sieglinde; finding phản ánh lựa chọn diễn đạt, chưa chứng minh đổi vai. |
| base fx-a03 | E-L99-UNTRANSLATED | L99 → L99, giữ; d=0 | MATCH T-S1 | Thuật ngữ Nhật còn trong DRAFT; source-role furigana được bỏ khi so quote nhưng không làm mất defect. |
| base fx-a05 | E-L115-01 | L115 → L115, giữ; d=0 | FALSE_POSITIVE | Observation tự ghi bản dịch phù hợp và không nêu lỗi có thể kiểm chứng. |
| base fx-a05 | E-L105-01 | L105 → L105, giữ; d=0 | CONFIRMED_ADDITIONAL_DEFECT | Câu thoại của Mercedes dùng “kể cả bác”, làm đổi người tự xưng theo Pronoun scope; đây là lỗi nghĩa ngoài target gieo. |
| base fx-a05 | E-L182-01 | L183 → L182, giữ; d=0 | UNRESOLVED / INSUFFICIENT_EVIDENCE | 孫 chỉ chứng minh quan hệ cháu/grandchild. Không đủ căn cứ suy “cháu nội”; invariant quan hệ phải giữ, câu Việt hiện tại cần ngữ cảnh rộng hơn để quyết định. |
| base fx-a07 | E-L5-NUMBER | L5 → L5, giữ; d=0 | MATCH T-S4A | Một cạnh bị đổi 500 thành 400. |
| base fx-a11 | E1 | L5 → L5, giữ; d=0 | MATCH T-S4A | Cùng lỗi số liệu của fixture. |
| base fx-a11 | E2 | L99 → L99, giữ; d=0 | MATCH T-S1 | Cùng lỗi untranslated. |
| base fx-a11 | E3 | L119 → L121, giữ; d=0 | FALSE_POSITIVE | Marker ảnh có mặt ở RAW/DRAFT L121; quote hành động ở L119 không chứng minh metadata là phần thêm. Cùng dòng gần không đủ để gọi lỗi. |
| repeat 1 fx-a04 | E-L71-FACTION | L71 → L71, giữ; d=0 | MATCH T-S2 | Finding kiểm đúng chiều so sánh Elfe/vampire. |
| repeat 1 fx-a11 | E-NUM-001 | L5 → L5, giữ; d=0 | MATCH T-S4A | Đúng target số. |
| repeat 1 fx-a11 | E-UNTRANS-001 | L99 → L99, giữ; d=0 | MATCH T-S1 | Đúng target untranslated. |
| repeat 2 fx-a11 | E001 | L5 → L5, giữ; d=0 | MATCH T-S4A | Đúng target số. |
| repeat 2 fx-a11 | E002 | L71 → L71, giữ; d=0 | MATCH T-S2 | Đúng target role/faction swap. |
| repeat 2 fx-a11 | E003 | L81 → L81, giữ; d=0 | FALSE_POSITIVE | DRAFT đã nói Hannah là hộ vệ cho công chúa; quote bị cắt trước phần kết và không chứng minh omission. |
| repeat 2 fx-a11 | E004 | L99 → L99, giữ; d=0 | MATCH T-S1 | Đúng target untranslated. |

Tổng finding rows: 17; matched seeded target: 10; unmatched: 7 gồm 2 confirmed additional defects, 3 false positives, 1 preference và 1 unresolved. Đây là phân loại semantically reviewed, không phải nhãn parser.

## Kiểm tác động normalization

### Đã quan sát

1. Wire reference resolution: mọi rawUnits từ L<n> được đổi thành u:<line>:<hash> trong REPORT_L1. Đây là canonicalization bắt buộc của app, không phải re-anchor. rawQuote vẫn kiểm đúng unit chứa quote.
2. NFC/trim/furigana theo source role: quote của target T-S1 bỏ phần đọc 《…》 nhưng vẫn khớp RAW L99. Chuẩn hóa này chỉ quyết định chuỗi con; nó không tạo finding, thay quote hoặc chuyển dòng.
3. Khử tham chiếu lặp: duplicateReferencesRemoved=17; các mục lặp là overlap occurrenceUnits với rawUnits hoặc lặp trong list. Candidate/error/span identity trùng không được tự khử; không có bằng chứng nó đã bị nới.
4. Loại speaker record trỏ dòng trống: 13 record bị loại, tất cả là tham chiếu L126, L140, L152, L154, L162, L164, L186, L192 vào dòng RAW trống; report ghi đúng speakerRecordsDroppedPaths. Findings, coverage, resolutions và protected spans của các run vẫn giữ nguyên. Đây là side-note bookkeeping; speaker proof của L2/L3 được kiểm bằng trường riêng.
5. Protected spans: cả 12 response có số lượng và giá trị spanId/start/end/source/reason giống report đã commit. Không quan sát protectedSpanIdAssigned, swap/clamp/drop hoặc reason default; bookkeepingNoteCount=0 ở mọi report.

### Không quan sát / chưa được đo

- draftAnchorDerivedFromQuote là 0/12, nên chưa có run thật nào kích hoạt dời anchor, tie-break trong cửa sổ ±3, quote lặp, range nhiều dòng hoặc MISSING.after clamp.
- Không có finding/protected span/speaker list chạm giới hạn; không có truncation. Max quan sát: 4 findings, 32 speaker records trên wire, 19 protected spans, thấp hơn caps 48/200/100.
- Tất cả optional keys của 12 response đều hiện diện; không có bằng chứng runtime cho defaulting của listener, reason, evidenceRefs, candidateIds, occurrenceUnits, evidenceLimit khi key bị bỏ.
- G1 L1_ONLY giữ DRAFT làm final.txt; chưa có VI_L2/FINAL để đo tác động normalization lên text downstream. Vì vậy không được gọi speaker drop hoặc canonicalization là “không ảnh hưởng chất lượng”.

### Chuỗi wire → parser → artifact → downstream

002-L1_RECONCILE.json (wire v3, L<n>) → EditorialL1Ledger.parseReconcile / EditorialReferenceNormalization (EditorialL1Ledger.java:212-329, :414-576) → REPORT_L1 schema v2 với normalizations và full unit ids (EditorialL1Ledger.java:668-719) → G1 scorer đọc final.txt DRAFT. Không có bước nào tự điền quote, tự đổi finding sang dòng gần hơn hoặc sửa DRAFT. Các kết luận semantic ở trên được làm sau chuỗi này bằng RAW/DRAFT private, không lấy khoảng cách neo làm oracle.

## Detection tách khỏi repair

- Detection: một target được tính detected khi finding chỉ đúng lỗi theo RAW/DRAFT/glossary/pronoun scope; vị trí chỉ là điều kiện định vị. Finding mới được tính thêm defect nếu invariant nghĩa và evidence quote đều đúng. False positive, preference và unresolved không được cộng recall.
- Repair: cần so sánh VI_L2/FINAL đã commit với target invariant, kiểm collateral và regression. G1 không tạo artifact đó, nên mọi SEMANTIC_EVAL=FAIL đóng băng vẫn chỉ nói DRAFT còn defect; repair recall = NOT_MEASURED.
- L2: đánh dấu riêng INHERITED_RESOLUTION cho finding có trong REPORT_L1 được truyền vào và INDEPENDENT_DISCOVERY cho finding mới. Không dùng finding false positive làm target sửa.
- L3: đánh dấu RESIDUAL_DEFECT trên VI_L2 đầu vào và NEW_REGRESSION trên output; predecessor dựng bằng fake chỉ đo isolation, không chứng minh end-to-end.

## Hạn chế và chi phí

Run metadata có fixture, phase, contract, token và cost nhưng không có model/route/source commit/prompt revision. Kế hoạch Z5 và build evidence ghim build p6.24/code237; bản phân xử không suy thêm model/route từ đó. Các giá trị thiếu này phải được ghi từ metadata thật trước G2; không được điền bằng trí nhớ hoặc mặc định.

Không có provider call trong gói này; chi phí gói = USD 0. G1 ledger sau Z5 vẫn 41 settled calls / USD 0.28859445 / 1.00, còn USD 0.71140555, pending 0; số dư này không tự cấp quyền G2.

Kết luận của tài liệu là retrospective detection và normalization impact; không có response nào được đổi thành hợp lệ và không có acceptance nào được nới.
