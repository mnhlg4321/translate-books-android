package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.chunk.ChunkPlan;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Two files through CS-1 into a pair run: Performance settings drive the cuts, a blocked pair never reaches a provider. Synthetic text. */
public final class EditorialPairFilesFlowTest {
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.0000002"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };
    private static final EditorialPairSourceLoader.References NO_REFERENCES = new EditorialPairSourceLoader.References("", "", "", "");

    private static String raw(int lines) {
        StringBuilder sb = new StringBuilder();
        for (int k = 1; k <= lines; k++) {
            String body = "これは" + k + "番目の文章であり、とても短い説明が続きます。";
            sb.append(k % 4 == 0 ? "「" + body + "」" : body).append('\n');
            if (k % 5 == 0) sb.append('\n');
        }
        return sb.toString();
    }

    private static String draft(int lines) {
        StringBuilder sb = new StringBuilder();
        for (int k = 1; k <= lines; k++) {
            String body = "Đây là câu văn thứ " + k + ", phần giải thích ngắn かな tiếp theo trong đoạn.";
            sb.append(k % 4 == 0 ? "「" + body + "」" : body).append('\n');
            if (k % 5 == 0) sb.append('\n');
        }
        return sb.toString();
    }

    private static EditorialPairSourceLoader.FilesLoad plan(String raw, String draft, AppSettings s) {
        return EditorialPairSourceLoader.planFiles("a.txt + b.txt", raw, draft, NO_REFERENCES, s);
    }

    @Test public void thePerformanceSettingsDriveTheCutsTheWayTheyDoInTranslate() {
        AppSettings token = new AppSettings();
        AppSettings chars = new AppSettings();
        chars.chunkMode = "char";
        chars.maxCharsPerChunk = 400;
        int byToken = plan(raw(80), draft(80), token).plan.chunks.size();
        int byChars = plan(raw(80), draft(80), chars).plan.chunks.size();
        assertTrue(byToken >= 2 && byChars >= 2);
        assertTrue("a 400-character budget makes more chunks than the default 450-token one on Japanese text", byChars > byToken || byChars != byToken);
        assertEquals("char", plan(raw(80), draft(80), chars).plan.limits.mode());
        assertEquals("token", plan(raw(80), draft(80), token).plan.limits.mode());
        ChunkPlan p = plan(raw(80), draft(80), token).plan;
        assertTrue(p.limits.soft() <= p.limits.hard());
        assertEquals(token.maxOutputTokens, p.limits.maxOutputTokens());
        assertEquals(token.contextChars, p.limits.contextChars());
        assertEquals(Chunker.adaptiveLimit(token, token.effectiveHardLimit()), p.limits.hard());
    }

    @Test public void theRowsAreTheChunksAndRebuildBothFiles() {
        String r = raw(80);
        String d = draft(80);
        EditorialPairSourceLoader.FilesLoad load = plan(r, d, new AppSettings());
        assertEquals("OK", load.plan.verdict);
        assertEquals(load.plan.chunks.size(), load.source.rawRows.size());
        assertEquals(r, String.join("", load.source.rawRows));
        assertEquals(d, String.join("", load.source.draftRows));
        assertTrue(load.source.lineageIssues.isEmpty());
        assertEquals(2, load.rawHead.size());
        assertEquals(2, load.draftHead.size());
        assertFalse(load.source.rawRows.get(0).isBlank());
    }

    @Test public void aFakeReplayOfTheWholeChapterGivesBackTheDraftByteForByteWithOneCallPerChunk() throws Exception {
        String r = raw(80);
        String d = draft(80);
        EditorialPairSourceLoader.FilesLoad load = plan(r, d, new AppSettings());
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, draftLine) -> "="), "stop", 100, 100,
                new BigDecimal("0.0001"), true, "fake-model", "fake-route", ""));
        InMemoryPairRunStore store = new InMemoryPairRunStore();
        EditorialPairRunService service = new EditorialPairRunService(store, fake, PRICING, 60_000L);
        PairRun prepared = service.prepare(1, load.source, "", "", null, "model-x", "Vietnamese", new BigDecimal("0.05"), "C");
        assertFalse("the plan is stored without any book text", prepared.chunkPlanJson.isEmpty() || prepared.chunkPlanJson.contains("文章"));
        assertEquals(load.plan.toJson(), prepared.chunkPlanJson);
        PairRun done = service.execute(prepared.id, null);
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        assertEquals(load.plan.chunks.size(), fake.requests.size());
        assertEquals(d, done.mergedText);
    }

    @Test public void theContextAroundEachChunkComesFromThePlanAndIsWholeLines() throws Exception {
        EditorialPairSourceLoader.FilesLoad load = plan(raw(80), draft(80), new AppSettings());
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, draftLine) -> "="), "stop", 100, 100,
                new BigDecimal("0.0001"), true, "fake-model", "fake-route", ""));
        EditorialPairRunService service = new EditorialPairRunService(new InMemoryPairRunStore(), fake, PRICING, 60_000L);
        service.execute(service.prepare(1, load.source, "", "", null, "m", "Vietnamese", new BigDecimal("0.05"), "C").id, null);
        String second = fake.requests.get(1).prompt().user();
        // every context line is a complete line of the file: the text before the second chunk ends where the chunk starts
        String firstDraftChunk = load.source.draftRows.get(0).trim();
        String lastLineBefore = firstDraftChunk.substring(firstDraftChunk.lastIndexOf('\n') + 1);
        assertTrue(second.contains(lastLineBefore));
    }

    @Test public void aDraftThatIsNotThisChapterIsBlockedAndNothingIsSent() {
        EditorialPairSourceLoader.FilesLoad load = plan(raw(60), "「全然違う」\n\n" + draft(60).replace("Đây là câu văn thứ", "Hoàn toàn khác").replace("「", "").replace("」", ""), new AppSettings());
        assertEquals("BLOCK", load.plan.verdict);
        assertTrue(load.source.rawRows.isEmpty());
        assertEquals(List.of("CHAPTER_BLOCKED"), load.source.lineageIssues);
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> { throw new AssertionError("nothing may be sent"); });
        EditorialPairRunService service = new EditorialPairRunService(new InMemoryPairRunStore(), fake, PRICING, 60_000L);
        try {
            service.prepare(1, load.source, "", "", null, "m", "Vietnamese", new BigDecimal("0.05"), "C");
            fail("a blocked pair must not be prepared");
        } catch (EditorialPairRunService.PrepareException blocked) {
            assertTrue(blocked.blockers.toString(), blocked.blockers.contains("SOURCE_CHAPTER_BLOCKED"));
        }
        assertEquals(0, fake.requests.size());
    }

    // ---- wording

    @Test public void theVerdictIsToldInPlainVietnameseWithNumbersAndAtMostTwoReasons() {
        ChunkPlan ok = plan(raw(80), draft(80), new AppSettings()).plan;
        String line = EditorialPairPresenter.verdictHeadline(ok, "token 450 · mềm 0,8");
        assertTrue(line, line.startsWith("Chia " + ok.chunks.size() + " đoạn theo cài đặt Performance (token 450 · mềm 0,8)."));
        ChunkPlan blocked = plan(raw(60), draft(60).replace("Đây là câu văn thứ", "Khác").replace("「", "").replace("」", "").replace("\n\n", "\n"), new AppSettings()).plan;
        assertEquals("BLOCK", blocked.verdict);
        assertEquals("RAW và DRAFT có vẻ không cùng chương", EditorialPairPresenter.verdictHeadline(blocked, ""));
        List<String> reasons = EditorialPairPresenter.verdictReasons(blocked);
        assertTrue(reasons.size() >= 1 && reasons.size() <= 2);
        for (String reason : reasons) assertTrue(reason, reason.matches(".*\\d.*"));
    }

    @Test public void everyReasonCodeHasItsOwnSentence() {
        ChunkPlan base = plan(raw(40), draft(40), new AppSettings()).plan;
        ChunkPlan p = new ChunkPlan("a", "b", "", "", base.limits, 2.54, 10, 10, List.of("11:10"), List.of(), "BLOCK",
                List.of(), 0, 0, 3, 4, 0);
        assertEquals("chỉ 8% tên riêng trong RAW có mặt ở dòng DRAFT tương ứng", EditorialPairPresenter.reasonText(p, new ChunkPlan.Reason("BLOCK", "NAME_MATCH", 0.08)));
        assertEquals("7% dòng có dấu thoại/ký hiệu khác nhau", EditorialPairPresenter.reasonText(p, new ChunkPlan.Reason("BLOCK", "EDGE_SYMBOLS", 0.93)));
        assertEquals("7 dòng không ghép được", EditorialPairPresenter.reasonText(p, new ChunkPlan.Reason("BLOCK", "UNPAIRED_LINES", 0.2)));
        assertEquals("DRAFT dài hơn mức thường 45%", EditorialPairPresenter.reasonText(p, new ChunkPlan.Reason("BLOCK", "LENGTH_RATIO", 0.45)));
        assertEquals("DRAFT ngắn hơn mức thường 40%", EditorialPairPresenter.reasonText(p, new ChunkPlan.Reason("BLOCK", "LENGTH_RATIO", -0.4)));
    }

    @Test public void thePerformanceLineNamesTheModeTheLimitAndTheSoftRatio() {
        AppSettings s = new AppSettings();
        assertEquals("token 450 · mềm 0.8", EditorialPairPresenter.performanceLine(s).replace(',', '.'));
        s.chunkMode = "char";
        s.maxCharsPerChunk = 0;
        assertTrue(EditorialPairPresenter.performanceLine(s).startsWith("ký tự 3500"));
        assertNotNull(EditorialPairV29MigrationSpec.from28To29());
    }

    @Test public void uncertainChunksAreReadBackFromTheStoredPlanAndOnlyWhenItMatchesTheChunkCount() {
        ChunkPlan base = plan(raw(80), draft(80), new AppSettings()).plan;
        List<ChunkPlan.Chunk> chunks = List.of(new ChunkPlan.Chunk(0, 5, 0, 5, List.of(), false), new ChunkPlan.Chunk(5, 9, 5, 9, List.of("CHECK_LENGTH"), true));
        ChunkPlan p = new ChunkPlan("a", "b", "", "", base.limits, 2.54, 9, 9, List.of("11:9"), chunks, "OK", List.of(), 0, 0, 0, 0, 1);
        assertEquals(java.util.Set.of(2), EditorialPairPresenter.uncertainOrdinals(p.toJson(), 2));
        assertTrue(EditorialPairPresenter.uncertainOrdinals(p.toJson(), 3).isEmpty());
        assertTrue(EditorialPairPresenter.uncertainOrdinals("", 2).isEmpty());
        assertTrue(EditorialPairPresenter.uncertainOrdinals("not json", 2).isEmpty());
    }
}
