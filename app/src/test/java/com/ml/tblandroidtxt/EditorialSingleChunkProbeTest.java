package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairMap;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;
import com.ml.tblandroidtxt.editorial.api.PairText;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The single-chunk experiment (docs/EDITORIAL_CHUNK_PLAN.md section 6.2): only the selected chunk reaches the provider, once,
 * with the request the full run would have built for it; a wrong guard is refused before any reservation or call.
 */
public final class EditorialSingleChunkProbeTest {
    private static final BigDecimal CAP = new BigDecimal("0.05");
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };
    private static final int ROWS = 7;
    private static final int TARGET = 5;
    private static final String GLOSSARY = "source,target,category,note\n花子,Hanako,character,tên riêng\n";

    /** Two paragraphs per chunk, so a pronoun row that covers one of them is a partial row with paragraph labels. */
    private static EditorialPairSource source() {
        java.util.List<String> raw = new java.util.ArrayList<>();
        java.util.List<String> draft = new java.util.ArrayList<>();
        for (int i = 1; i <= ROWS; i++) {
            raw.add("第" + i + "章の本文です。「花子は太郎に言った。」\n\n「今日は静かな夜だね。」と花子は笑った。\n\n");
            draft.add("Hàng " + i + ", đoạn 1: 「cậu có đủ chữ かな để cổng cấu trúc đo。」\n\nHàng " + i + ", đoạn 2: nội dung bản nháp có đủ chữ để cổng cấu trúc đo.\n\n");
        }
        return new EditorialPairSource(EditorialPairModels.SOURCE_JOB, "7", "truyen.txt", "", raw, draft, java.util.List.of());
    }

    private final InMemoryPairRunStore memory = new InMemoryPairRunStore();

    private EditorialPairRunService service(EditorialApiProvider provider) {
        return new EditorialPairRunService(memory, provider, PRICING, 60_000L);
    }

    /** A pronoun row whose scope covers only the first paragraph of the chunk, so the chunk carries a partial row and paragraph labels. */
    private static String pronounFor(PairRun run, int ordinal) {
        PairMap map = EditorialPairSnapshot.of(run).map;
        PairMap.Entry entry = chunk(map, ordinal);
        int first = map.rawParagraphStart(entry);
        String p = String.format("p%03d", first);
        return "from,speaker,target,self,call,scope,note\n"
                + "花子,Hanako,Taro,tôi,Taro," + p + "-" + p + ",lời gọi lạnh\n"
                + "私,@NARRATOR,@AUDIENCE,tôi,,*,giọng kể\n";
    }

    private static PairMap.Entry chunk(PairMap map, int ordinal) {
        for (PairMap.Entry e : map.entries()) if (e.displayOrdinal() == ordinal) return e;
        throw new AssertionError("no chunk " + ordinal);
    }

    private PairRun prepare(EditorialPairRunService svc) throws Exception {
        PairRun bare = svc.prepare(1, source(), GLOSSARY, "", null, "model-x", "Vietnamese", CAP, EditorialPairModels.ARM_CHUNK);
        String pronouns = pronounFor(bare, TARGET);
        return svc.prepare(1, source(), GLOSSARY, pronouns, null, "model-x", "Vietnamese", CAP, EditorialPairModels.ARM_CHUNK);
    }

    private static EditorialSingleChunkProbe.Selector selectorFor(PairRun run, int ordinal) {
        PairMap map = EditorialPairSnapshot.of(run).map;
        PairMap.Entry e = chunk(map, ordinal);
        return new EditorialSingleChunkProbe.Selector(ordinal, e.pairId(), run.mapHash, map.rawParagraphStart(e), map.rawParagraphEnd(e),
                PairText.sha256(map.rawText(e)), PairText.sha256(map.draftText(e)));
    }

    private static FakeEditorialApiProvider echo() {
        return new FakeEditorialApiProvider((request, index) -> request.prompt().user().contains("# NUMBERED FIX POINTS\n")
                ? new EditorialApiFlow.StepResponse(EditorialPairTestData.targetedAnswer(request, index, (i, d) -> "="), "stop", 100, 100,
                        new BigDecimal("0.0001"), true, "fake-model", "fake-route", "")
                : FakeEditorialApiProvider.edited(EditorialPairTestData.draftPart(request)));
    }

    @Test public void onlyTheSelectedChunkIsSentAndTheOthersStayUnsent() throws Exception {
        FakeEditorialApiProvider fake = echo();
        EditorialSingleChunkProbe.OneCallProvider capped = new EditorialSingleChunkProbe.OneCallProvider(fake);
        EditorialPairRunService svc = service(capped);
        PairRun run = prepare(svc);
        String pairId = EditorialSingleChunkProbe.resolve(run, selectorFor(run, TARGET));
        PairRun done = svc.executeOnly(run.id, pairId, null);

        assertEquals(1, fake.requests.size());
        assertEquals(1, capped.dispatched());
        assertEquals(0, capped.refused());
        assertEquals(1, done.calls);
        List<PairItem> items = svc.items(run.id);
        assertEquals(ROWS, items.size());
        for (PairItem i : items) {
            if (i.pairId.equals(pairId)) {
                assertEquals(PairState.ACCEPTED, i.state);
                assertEquals(1, i.calls);
            } else {
                assertEquals("chunk " + i.ordinal + " must stay unsent", PairState.IMPORTED, i.state);
                assertEquals(0, i.calls);
            }
        }
        assertEquals(1, memory.allReservations(run.id).size());
        assertEquals("a single chunk is never a final chapter", "", done.mergedText);
        assertNotEquals(RunState.FINAL_ELIGIBLE, done.state);
        assertFalse(svc.exportPlan(run.id).complete);
        // the request asked for exactly the draft of the selected chunk
        PairMap map = EditorialPairSnapshot.of(run).map;
        assertTrue(fake.requests.get(0).prompt().user().contains(PairText.trim(map.draftText(chunk(map, TARGET)))));
    }

    @Test public void theSingleRequestIsTheOneTheFullRunBuildsForThatChunk() throws Exception {
        FakeEditorialApiProvider full = echo();
        EditorialPairRunService fullSvc = service(full);
        PairRun fullRun = prepare(fullSvc);
        fullSvc.execute(fullRun.id, null);
        assertEquals(ROWS, full.requests.size());

        InMemoryPairRunStore other = new InMemoryPairRunStore();
        FakeEditorialApiProvider single = echo();
        EditorialPairRunService singleSvc = new EditorialPairRunService(other, new EditorialSingleChunkProbe.OneCallProvider(single), PRICING, 60_000L);
        PairRun run = singleSvc.prepare(1, source(), GLOSSARY, fullRun.pronounText, null, "model-x", "Vietnamese", CAP, EditorialPairModels.ARM_CHUNK);
        singleSvc.executeOnly(run.id, EditorialSingleChunkProbe.resolve(run, selectorFor(run, TARGET)), null);

        assertEquals(1, single.requests.size());
        com.ml.tblandroidtxt.editorial.api.ApiPrompt expected = full.requests.get(TARGET - 1).prompt();
        com.ml.tblandroidtxt.editorial.api.ApiPrompt actual = single.requests.get(0).prompt();
        assertEquals("system (core, glossary, pronoun rows, labels, output contract)", expected.system(), actual.system());
        assertEquals("user (RAW part, RAW/DRAFT context, DRAFT part)", expected.user(), actual.user());
        assertEquals(full.maxOutputTokens.get(TARGET - 1), single.maxOutputTokens.get(0));
        assertTrue("the deterministic point types reach the request", actual.user().contains("KANA"));
        assertTrue(actual.user().contains("# NUMBERED FIX POINTS"));
        assertTrue("neighbouring text is read-only", actual.user().contains("RAW CONTEXT BEFORE (read-only)"));
    }

    @Test public void aFailureOrUnknownOutcomeNeverTriggersASecondRequest() throws Exception {
        for (EditorialApiFlow.StepResponse bad : new EditorialApiFlow.StepResponse[] {
                EditorialApiFlow.StepResponse.failure("HTTP_500"), EditorialApiFlow.StepResponse.unknownOutcome("SocketTimeoutException"),
                FakeEditorialApiProvider.text("không có thẻ nào", "stop")}) {
            FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> bad);
            EditorialSingleChunkProbe.OneCallProvider capped = new EditorialSingleChunkProbe.OneCallProvider(fake);
            InMemoryPairRunStore store = new InMemoryPairRunStore();
            EditorialPairRunService svc = new EditorialPairRunService(store, capped, PRICING, 60_000L);
            PairRun run = svc.prepare(1, source(), GLOSSARY, "", null, "model-x", "Vietnamese", CAP, EditorialPairModels.ARM_CHUNK);
            String pairId = EditorialSingleChunkProbe.resolve(run, selectorFor(run, TARGET));
            svc.executeOnly(run.id, pairId, null);
            assertEquals(bad.error() + bad.content(), 1, fake.requests.size());
            assertEquals(0, capped.refused());
            for (PairItem i : svc.items(run.id)) if (!i.pairId.equals(pairId)) assertEquals(PairState.IMPORTED, i.state);
            // asking again is refused up front: the pair is no longer unsent, and the cap would refuse anyway
            try {
                svc.executeOnly(run.id, pairId, null);
                fail("a second dispatch for the same chunk must be refused");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().startsWith("PAIR_NOT_UNSENT"));
            }
            assertEquals(1, fake.requests.size());
        }
    }

    @Test public void theProviderWrapperRefusesEveryRequestAfterTheFirst() throws Exception {
        FakeEditorialApiProvider fake = echo();
        EditorialSingleChunkProbe.OneCallProvider capped = new EditorialSingleChunkProbe.OneCallProvider(fake);
        EditorialPairRunService svc = service(capped);
        PairRun run = prepare(svc);
        EditorialApiFlow.Request request = new EditorialApiFlow.Request(com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Step.EDIT, 1,
                com.ml.tblandroidtxt.editorial.api.PairPromptBuilder.buildPair(EditorialPairSnapshot.of(run).map, chunk(EditorialPairSnapshot.of(run).map, TARGET),
                        "Vietnamese", EditorialPairSnapshot.of(run).glossary, "", java.util.Set.of()), false);
        assertEquals("", capped.call(request, "m", 100, 1000).error());
        EditorialApiFlow.StepResponse second = capped.call(request, "m", 100, 1000);
        assertEquals(EditorialSingleChunkProbe.OneCallProvider.LIMIT_ERROR, second.error());
        capped.call(request, "m", 100, 1000);
        assertEquals(1, fake.requests.size());
        assertEquals(1, capped.dispatched());
        assertEquals(2, capped.refused());
    }

    @Test public void aWrongSelectorIsRefusedBeforeAnyReservationOrCall() throws Exception {
        FakeEditorialApiProvider fake = echo();
        EditorialPairRunService svc = service(fake);
        PairRun run = prepare(svc);
        EditorialSingleChunkProbe.Selector good = selectorFor(run, TARGET);
        String h = "0".repeat(64);
        Object[][] bad = {
                {"SELECTOR_ORDINAL_NOT_UNIQUE", new EditorialSingleChunkProbe.Selector(ROWS + 3, good.pairId(), good.mapHash(), good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), good.draftRangeSha256())},
                {"SELECTOR_PAIR_ID_MISMATCH", new EditorialSingleChunkProbe.Selector(TARGET - 1, good.pairId(), good.mapHash(), good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), good.draftRangeSha256())},
                {"SELECTOR_MAP_HASH_MISMATCH", new EditorialSingleChunkProbe.Selector(TARGET, good.pairId(), h, good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), good.draftRangeSha256())},
                {"SELECTOR_PARAGRAPH_RANGE_MISMATCH", new EditorialSingleChunkProbe.Selector(TARGET, good.pairId(), good.mapHash(), good.firstParagraph(), good.lastParagraph() + 1, good.rawRangeSha256(), good.draftRangeSha256())},
                {"SELECTOR_RAW_RANGE_HASH_MISMATCH", new EditorialSingleChunkProbe.Selector(TARGET, good.pairId(), good.mapHash(), good.firstParagraph(), good.lastParagraph(), h, good.draftRangeSha256())},
                {"SELECTOR_DRAFT_RANGE_HASH_MISMATCH", new EditorialSingleChunkProbe.Selector(TARGET, good.pairId(), good.mapHash(), good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), h)},
                {"SELECTOR_INCOMPLETE", new EditorialSingleChunkProbe.Selector(TARGET, good.pairId(), "", good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), good.draftRangeSha256())},
                {"SELECTOR_INCOMPLETE", new EditorialSingleChunkProbe.Selector(TARGET, null, good.mapHash(), good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), good.draftRangeSha256())},
                {"SELECTOR_INCOMPLETE", new EditorialSingleChunkProbe.Selector(0, good.pairId(), good.mapHash(), good.firstParagraph(), good.lastParagraph(), good.rawRangeSha256(), good.draftRangeSha256())},
        };
        for (Object[] row : bad) {
            try {
                EditorialSingleChunkProbe.resolve(run, (EditorialSingleChunkProbe.Selector) row[1]);
                fail("expected " + row[0]);
            } catch (EditorialSingleChunkProbe.SelectionException e) {
                assertEquals(row[0], e.code);
            }
        }
        try {
            EditorialSingleChunkProbe.resolve(run, null);
            fail("a missing selector is refused");
        } catch (EditorialSingleChunkProbe.SelectionException e) {
            assertEquals("SELECTOR_MISSING", e.code);
        }
        // a whole-chapter run has no chunks to select
        PairRun whole = svc.prepare(1, source(), GLOSSARY, "", null, "model-x", "Vietnamese", CAP, EditorialPairModels.ARM_WHOLE);
        try {
            EditorialSingleChunkProbe.resolve(whole, good);
            fail("whole-chapter run refused");
        } catch (EditorialSingleChunkProbe.SelectionException e) {
            assertEquals("SELECTOR_NEEDS_CHUNK_RUN", e.code);
        }
        // a pair id that is not in the run is refused before a reservation exists
        try {
            svc.executeOnly(run.id, "no-such-pair", null);
            fail("unknown pair refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("PAIR_NOT_IN_RUN"));
        }
        assertEquals(0, fake.requests.size());
        assertEquals(0, memory.allReservations(run.id).size());
    }

    @Test public void theFullRunStillSendsEveryChunkAndRetriesAClearFailureOnce() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> index == 0
                ? EditorialApiFlow.StepResponse.failure("HTTP_500")
                : new EditorialApiFlow.StepResponse(EditorialPairTestData.targetedAnswer(request, index, (i, d) -> "="), "stop", 100, 100,
                        new BigDecimal("0.0001"), true, "fake-model", "fake-route", ""));
        EditorialPairRunService svc = service(fake);
        PairRun run = prepare(svc);
        PairRun done = svc.execute(run.id, null);
        assertEquals("one failure retried once, then every chunk", ROWS + 1, fake.requests.size());
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
    }
}
