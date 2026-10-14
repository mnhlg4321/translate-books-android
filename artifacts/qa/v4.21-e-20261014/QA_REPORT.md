# v4.21 E — RC4.21.0/code271, CP-IMPL-19; QA và hiệu năng đạt

Quyền: docs/V421_E_RELEASE_WORK_REQUEST.md; quyết định sản phẩm là phát hành P2 dấu câu theo RAW. Baseline đầu phiên feature/v4.21 tại235304ea; E2 ghi chú phát hành commit riêng cb288c9a5bd574cd85fcde329a2f9d6d1e4514c7. USD0, provider0; chỉ emulator-5554/tbl-code113-dqa-api35; không wipe/clear/uninstall, không đụng điện thoại hay checkout của owner.

## E2/E1 — source sạch đã kiểm

RELEASE_NOTES/CHANGELOG nêu ba nhóm dấu RAW, CP-IMPL-19 cần lượt Biên tập mới, one-to-one và giới hạn sửa tay. Archive sạch cb288c9a; JDK21.0.10/Gradle9.3.0/offline.

| Nhóm | PASS | SKIP | FAIL |
|---|---:|---:|---:|
| engine |726|1|0|
| app debug |471|4|0|
| app release |471|4|0|
| app benchmark |471|4|0|
| scripts/p6 |106|0|0|
| scripts/chunk |24|0|0|

Lint0 lỗi/57 cảnh báo; compile AndroidTest PASS. PASS/SKIP/FAIL từng method nằm trong XML và log Python riêng: D:/P5E-private/v421-e-host-evidence; đã sao lưu cùng vùng riêng. Không đưa stdout fixture sách vào Git. SUMMARY ghi hash tập bằng chứng343 file, không tính hai log emulator còn mở. SKIP không tính PASS.

## E3 — RC và test được giữ trước cài

- Production wrapper build-and-save.ps1 -ExactReleaseVersion4.21.0, local feature/v4.21.0 tại cb288c9a; code271; event build-20261014-082641.
- APK SHA256: 66B9E2E3B5D32854893A279147693925C56AD75561E86B8A219683367F8A088F.
- SourceZIP SHA256: 57F9DABC5C9EF704133DBEB7BB668DEA7BD65402501468F0EE83D82E0E6AAAAA.
- Test wrapper event test-20261014-0828, cùng sourceZIP, ghim productionAPK/sourceZIP/cert; testAPK SHA256 CC7EFF9FDD165B24C81C2DD6806E9D8103FD33A920CA696BC18BFD5DF1ABA644; không build lại production.
- Cert SHA256 47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155; signer đọc lại abebea4b.
- Production/test payload đủ APK, README, BUILD_INFO, checksum, sourceZIP; byte-verified tại D:/App Translate Books/artifacts và backup/builds|test-builds/v4.21.0/<event>, cùng hai mirror worktree, trước cài. Không ghi đè event.
- Emulator ban đầu tắt; khởi động đúng AVD, giữ dữ liệu. install-validated preflight/install/readback PASS, RC code271. Mã sản xuất không thay đổi sau build.

## E4 — provider giả

Residue glossary test0 trước chạy. Frozen19/chunk6/product6 lớp và6 pha seed/verify/cleanup mới; không dùng lại log PASS4.20. instrumentation_summary.py:165 methods,158 PASS/7 SKIP/0 FAIL/0 INCOMPLETE. Frozen116/1, chunk21/6, product15/0, restart6/0 (PASS/SKIP). Force-stop giữa seed/verify; SQLite, mở lại, xuất/readback bằng dữ liệu tổng hợp PASS. System picker NOT_MEASURED. Không có test hỏng cần sửa, không gọi provider thật.

## E5 — đạt sau lượt chạy lại được owner duyệt

Dùng đúng APK benchmark đã lưu, SHA256 DB3CB8097B34E6D24A4CB9A95E82D2436007E3449C6432799EAF710FF4FA6819; không rebuild, không sửa mã production/benchmark/UI/Gradle hoặc tiêu chí. APK benchmark/test/sourceZIP đã lưu và mirror trước cài.

- Lượt đầu:1 PASS/1 FAIL, coldStartup median850.8378ms; openEditorialTab lỗi FrameTimingQuery không có expect/actual frame slices. Giữ nguyên macrobenchmark-instrument.txt, method summary và6trace trong failed-attempt-traces; 50/50 hash bằng chứng phiên trước đã kiểm không đổi trước khi cập nhật báo cáo.
- Chẩn đoán owner: trace lỗi vẫn có10 Choreographer#doFrame như lượt4.20 đạt, không có hộp thoại xin quyền; **lượt đầu lỗi do thiếu dữ liệu khung hình của SurfaceFlinger**, không phải mã app/benchmark thay đổi. So Git xác nhận benchmark/Gradle/MainActivity không đổi. Không suy luận màn hình tắt/headless là nguyên nhân.
- Lượt chạy lại riêng StartupMacrobenchmark#openEditorialTab:5 iterations,1 PASS/0 SKIP/0 FAIL; CPU frameP50 10.47055ms. Log và5trace mới lưu riêng trong open-tab-rerun; không thay log lỗi cũ.
- Lượt đầy đủ sau đó:2 methods PASS/0 SKIP/0 FAIL,5 iterations mỗi method; cold-start median861.66ms; tab Biên tập CPU frameP50 12.4882ms. Log macrobenchmark-full-rerun-instrument.txt,10trace mới và benchmarkData.json lưu riêng trong macrobenchmark.
- Planning trên RC271:1222 RAW/DRAFT units,53chunks,verdictOK,best-of-three432ms; assertion dưới1000ms PASS. Perfetto15giây v421-e-plan.pftrace là đầu ra mới.
- 11trace nghiệm thu mới; tổng22trace gồm6lượt lỗi,5lượt riêng,10lượt đầy đủ,1planning. Tất cả đầu ra thật, không tái dùng số4.20. Chỉ là số emulator, không phép so sánh nhân quả hoặc đo điện thoại.
- Phép đo riêng đã đạt nên không cần cài benchmark4.20 hay restart emulator. Sau tất cả phép đo cài lại RC4.21.0/code271 bằng install-validated, đọc lại signerabebea4b/code271 PASS. Provider0/USD0.

## E6 — media trên RC271

EditorialR1MediaWalkthroughInstrumentedTest r1_media=run:1PASS/0SKIP/0FAIL. Sáu ảnh đã kiểm trực quan, walkthrough.mp4 thật3.832.003bytes. Dữ liệu synthetic/fake-provider, không văn bản sách. DB chính và journal tạm dời, khôi phục hash khớp (database-preservation.json); không clear/wipe/uninstall. Mở lại/xuất/readback controller được kiểm; Android system picker NOT_MEASURED. Không dùng media để kết luận chất lượng ngữ nghĩa.

## E7 — chuẩn bị đóng gói và conditional release

E1/E4 không có FAIL; lỗi đầu E5 đã giải thích và chạy lại đạt, giữ nguyên lịch sử FAIL. Production/test payload đủ và hashkhớp; E5/E6 có đầu ra thật; production bằng đúng commit đã E1 cb288c9a. Checklist05–09 được ghi cùng commit QA này; PreTag và archive chỉ thực hiện sau khi commit sạch. Chưa tuyên bố tag/main/push hoàn tất trước xác nhận.

Next action: hoàn tất E7–E9 đúng quyền, giữ nguyên giới hạn và evidence lịch sử; USD0.
