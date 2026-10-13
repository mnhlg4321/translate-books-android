# v4.19 X4 — gói đo 006 với prompt D-A mọi self

Trạng thái: PREPARED, chờ quyền chi riêng. Nguồn đã kiểm: `ca59f275c1ba97c980848329eba3379b0b7b10ad`; production change duy nhất là luật tên-call trong `TargetedFixPrompt.java`. Chưa có lời gọi provider, APK hoặc thao tác thiết bị. Gói B 008 cũ không chạy.

## Phạm vi đề nghị

- Một lần chạy mới từ đầu, chỉ chương 006, cả chương theo chunk hiện hành; `openai/gpt-5.6-luna`, reasoning `medium`, tuần tự, không retry/tự chạy lại/tiếp tục lượt cũ.
- 15 chunk, tối đa 14 lời gọi; chunk không có điểm không gọi. 74 điểm: ADDRESS 65, GLOSSARY 5, KANA 4. Không đổi finder, guard, căn/cắt/ghép, ngưỡng hoặc file đầu vào.
- Sổ mới đề nghị `V419-X5-006-<date>-<unique-event>`, trần tổng **USD 0.10**. Chưa tạo sổ. Owner phải cấp quyền cho chương, số lượt, tối đa 14 calls, model/reasoning và trần này trước khi thực thi.
- Đầu ra và dữ liệu sách ở thư mục private mới; Git chỉ nhận hash, đếm, nhãn và báo cáo tổng hợp. Build/device QA nếu cần cho cơ chế chạy thật được xử lý trong phạm vi tiếp theo, không được suy quyền từ gói offline này. APK v4.18.0 có prompt cũ nên không dùng để đo prompt mới.

## Input pin

Nguồn `D:/P5E-private/q2-inputs/006/`:

| File | SHA-256 |
|---|---|
| RAW | `8F691176D9886364FA7E1753E185E4E3D8882B8CCA2EE0CA95363E5A2BFCEF66` |
| DRAFT | `ED6C6D3D9ACAAB4E6F26993D5997FA57C74F65259F744744C51CA5E436B068CC` |
| Glossary | `81BB2463389A47B0E97DF0D33FD3BFDABE001479635B3BDD690EC85AC91AA0EA` |
| Pronoun | `E7CF500B191AD32683F6F9CCC83F7E6D14463AE10DFB71EC62A054AAD09813A2` |

## Ước tính trần

Đo offline trên archive sạch `ca59f275` bằng `EditorialPairC6CorpusMeasurementTest.measurePrivateCorpusWhenConfigured`: 73,220 token đầu vào ước tính, 57,344 token giữ chỗ đầu ra (14 × 4,096; gồm reasoning). XML bằng chứng: `artifacts/qa/v4.19-x4-ca59f275/measurement-estimate.xml`.

Dùng giá **giả định theo lịch sử đã quan sát** luna 0.25 USD/M input và 1.20 USD/M output, không giả định cache: `73220 × 0.25/M + 57344 × 1.20/M = USD 0.0871178`, làm tròn lên **0.087118**. Đề nghị **0.10 USD**, dư khoảng 14.8% so với estimate. Đây là ước tính token/cost, không phải giá provider được xác nhận hôm nay hay bảo đảm chất lượng.

Trước lần chạy được cấp quyền, kiểm giá đã cấu hình và reservation dùng đúng giá; nếu giá UNKNOWN, dùng fallback 5/25 USD/M sẽ thành **1.799700 USD**, vượt trần, phải dừng. Không dùng giá runner cũ 0.20/M để giữ chỗ thấp hơn giả định 0.25/M. Không sửa runner trong X4. Cổng trước mỗi call: đã quyết toán + mọi giữ chỗ đang treo + giữ chỗ call mới ≤ 0.10; không nâng trần tự động.

## Chấm và điểm dừng

- D-A: call là tên, mọi self; tên bắt buộc ở lời gọi trực tiếp. Chỉ cậu ngôi hai ngoài lời gọi được tùy chọn: giữ hoặc đúng tên đều ĐÚNG; sửa self vẫn bắt buộc nếu sai. Call là đại từ (`cô`, …) vẫn theo hàng.
- Suite 21 known / 137 clean. 23 điểm tùy chọn D-A trên ba replay; trong 006 có 8 điểm: 7:3, 8:4, 9:1, 11:4, 11:5, 13:4, 14:2, 14:3. Các complete-line hash cho phép nằm trong `optionalDa` của chỉ mục bản sạch. 7:3 lấy nền currentAfter để giữ sửa lời gọi Fuyu, không cho hoàn tác về Mitsurugi.
- Năm điểm bị liệt kê nhầm ở mục 9.3 cũ (006 1:2, 1:4, 1:7, 2:1, 8:2) vẫn là control nghiêm: ngôi ba, danh xưng/vocative hoặc call đại từ. Chúng không được nới theo D-A.
- Sửa được ít nhất một lỗi self/call đại từ thật theo RAW + hàng; không có sửa sai mới, không cho các sửa sai cũ lọt lại, không đổi ngoài điểm. FINAL chỉ tham chiếu, owner đọc là cổng quyết định; replay không chứng minh model mới sẽ sửa được lỗi.
- Bộ chấm lần đo mới dùng `verify-replay --allow-da-choices` với TSV đã map point id/chunk vào run baseline `006-C6E`; đây là chế độ explicit riêng cho lựa chọn D-A. Default replay vẫn nghiêm. Mọi thay đổi ngoài danh sách hash, kể cả thêm đại từ khác/đổi chữ/dấu câu, là hồi quy bản sạch. Những điểm known thay đổi vẫn phải được đọc/chấm, không tự nâng thành PASS.
- Dừng ngay nếu bản sạch đổi ngoài lựa chọn D-A, sửa sai cũ lọt lại, vượt trần, thiếu giá/cost, finish `length`, response rỗng hoặc lỗi hạ tầng. Không retry, không đổi prompt/model/input sau một failure trong cùng lượt.

**Next action:** owner cấp quyền chi riêng cho một lượt 006 theo gói này; cho tới đó mọi provider call vẫn bằng 0.
