package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;
import com.ml.tblandroidtxt.editorial.api.PairText;

import org.json.JSONObject;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Pair runs over the in-memory store and a scripted provider: gates, reservation before dispatch, unknown outcomes, restarts, export. */
public final class EditorialPairRunServiceTest {
    private static final BigDecimal CAP = new BigDecimal("0.05");
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private final InMemoryPairRunStore memory = new InMemoryPairRunStore();

    // ---- fixtures

    static String rawRow(int i) { return EditorialPairTestData.rawRow(i); }

    static String draftRow(int i) { return EditorialPairTestData.draftRow(i); }

    static EditorialPairSource source(int rows) {
        EditorialPairSource base = EditorialPairTestData.source(rows);
        List<String> flagged = new ArrayList<>();
        for (String row : base.draftRows) flagged.add(row.replace("dòng 3:", "dòng 3: かな"));
        return new EditorialPairSource(base.kind, base.ref, base.label, base.chapterId, base.rawRows, flagged, base.lineageIssues);
    }

    static String draftPart(EditorialApiFlow.Request request) { return EditorialPairTestData.draftPart(request); }

    interface Edit { String apply(int callIndex, String draft); }

    private static FakeEditorialApiProvider provider(Edit edit) {
        return new FakeEditorialApiProvider((request, index) -> {
            if (request.prompt().user().contains("# NUMBERED FIX POINTS\n")) {
                return targetedText(request, index, edit::apply, "stop", new BigDecimal("0.0001"));
            }
            return FakeEditorialApiProvider.text("<EDITED>" + edit.apply(index, draftPart(request)) + "</EDITED>", "stop");
        });
    }

    private static EditorialApiFlow.StepResponse targetedText(EditorialApiFlow.Request request, int index,
            java.util.function.BiFunction<Integer, String, String> edit, String finish, BigDecimal cost) {
        return new EditorialApiFlow.StepResponse(EditorialPairTestData.targetedAnswer(request, index, edit), finish,
                1500, 400, cost, true, "fake-model", "fake-route", "");
    }

    private EditorialPairRunService service(EditorialPairRunStore store, EditorialApiProvider provider) {
        return new EditorialPairRunService(store, provider, PRICING, 60_000L);
    }

    private PairRun prepare(EditorialPairRunService service, EditorialPairSource source, String arm) throws Exception {
        return service.prepare(1, source, "", "", null, "model-x", "Vietnamese", CAP, arm);
    }

    private static JSONObject gate(PairItem item) throws Exception { return new JSONObject(item.gateJson); }

    // ---- prepare

    @Test public void aRunNeedsAnExplicitLinkOnEverySideAndSaysWhyOtherwise() throws Exception {
        EditorialPairRunService svc = service(memory, provider((i, d) -> d));
        List<String> raw = new ArrayList<>(List.of(rawRow(1), rawRow(2)));
        List<String> draft = new ArrayList<>();
        draft.add(draftRow(1));
        draft.add(null);
        try {
            prepare(svc, new EditorialPairSource("JOB", "7", "x", "", raw, draft, List.of()), "C");
            fail("a missing pair cannot run");
        } catch (EditorialPairRunService.PrepareException e) {
            assertTrue(e.blockers.toString(), e.blockers.contains("MISSING_PAIRS:1"));
        }
        try {
            prepare(svc, EditorialPairSource.unmappedFiles("raw.txt + draft.txt"), "C");
            fail("two independent files have no mapping");
        } catch (EditorialPairRunService.PrepareException e) {
            assertTrue(e.blockers.contains("SOURCE_NO_EXPLICIT_MAPPING"));
        }
        try {
            prepare(svc, new EditorialPairSource("JOB", "7", "x", "", raw, List.of(draftRow(1), draftRow(2)), List.of("ROW_OFFSET_GAP:2")), "C");
            fail("a broken lineage cannot run");
        } catch (EditorialPairRunService.PrepareException e) {
            assertTrue(e.blockers.contains("SOURCE_ROW_OFFSET_GAP:2"));
        }
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 4000; i++) huge.append("長い文章がずっと続く。".repeat(1)).append('\n');
        String longRaw = huge.toString().repeat(4);
        try {
            prepare(svc, new EditorialPairSource("JOB", "7", "x", "", List.of(longRaw), List.of(draftRow(1)), List.of()), "C");
            fail("a pair that cannot fit one request is not run");
        } catch (EditorialPairRunService.PrepareException e) {
            assertTrue(e.blockers.toString(), e.blockers.toString().contains("TOO_LONG"));
        }
        assertTrue(memory.runsOf(1).isEmpty());
    }

    // ---- a clean run

    @Test public void changedContractNeverSilentlyResumesAnOldRun() throws Exception {
        FakeEditorialApiProvider fake = provider((i, d) -> d);
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(2), "C");
        run.contractRevision = "CP-IMPL-1";
        memory.updateRun(run);
        try {
            svc.execute(run.id, null);
            fail("old prompt cannot be resumed under new rules");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("PAIR_CONTRACT_CHANGED"));
        }
        assertEquals(0, fake.requests.size());
        assertNotNull(svc.exportPlan(run.id));
        assertEquals("CP-IMPL-1", svc.get(run.id).contractRevision);
    }

    @Test public void aRunThatChangesNothingIsAcceptedPairByPairAndMergesBackToTheDraft() throws Exception {
        FakeEditorialApiProvider fake = provider((i, d) -> d);
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(4), "C");
        assertEquals(RunState.PREPARED, run.state);
        PairRun done = svc.execute(run.id, null);
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        assertEquals(4, fake.requests.size());
        assertEquals(4, done.calls);
        for (EditorialApiFlow.Request request : fake.requests) {
            assertEquals(com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Step.EDIT, request.step());
            assertFalse(request.prompt().system().contains("<NOTES>"));
            assertTrue(request.prompt().originalSourceFiles().isEmpty());
        }
        assertEquals(0, done.warnings);
        for (PairItem i : svc.items(run.id)) {
            assertEquals(PairState.ACCEPTED, i.state);
            assertTrue(gate(i).getBoolean("verbatim"));
            assertEquals(0, gate(i).getJSONArray("codes").length());
        }
        EditorialPairSnapshot snapshot = EditorialPairSnapshot.of(done);
        assertEquals(snapshot.map.draft.text, done.mergedText);
        assertTrue(done.mergeReceiptJson.contains("\"outputSha256\":\"" + PairText.sha256(done.mergedText) + "\""));
        // the reservation was opened before every call and settled once with the real charge
        List<PairReservation> all = memory.allReservations(run.id);
        assertEquals(4, all.size());
        for (PairReservation r : all) { assertEquals(PairReservation.SETTLED, r.state); assertEquals(0, new BigDecimal("0.0001").compareTo(r.settledUsd)); }
        List<String> journal = memory.journal(run.id);
        assertTrue(journal.get(0).endsWith("RESERVE"));
        assertTrue(journal.get(1).endsWith("RESERVED"));
        assertTrue(journal.get(2).endsWith("SENT"));
        assertTrue(journal.get(3).contains("RECEIVED"));
        EditorialPairRunService.ExportPlan plan = svc.exportPlan(run.id);
        assertTrue(plan.complete);
        assertTrue(plan.label, plan.label.contains("chưa được chấm nghĩa"));
        assertFalse(plan.label.contains("đã đạt"));
        assertEquals(snapshot.map.draft.text, plan.text);
    }

    @Test public void aChunkWithoutAnyDeterministicFixPointIsStoredUnchangedWithoutAProviderCall() throws Exception {
        FakeEditorialApiProvider never = new FakeEditorialApiProvider((request, index) -> { throw new AssertionError("no-point chunk must not call a provider"); });
        EditorialPairRunService svc = service(memory, never);
        EditorialPairSource plain = EditorialPairTestData.source(2);
        PairRun run = svc.prepare(1, plain, "", "", null, "model-x", "Vietnamese", CAP, "C");
        PairRun done = svc.execute(run.id, null);
        assertEquals(0, never.requests.size());
        assertEquals(0, done.calls);
        assertEquals(BigDecimal.ZERO, done.usd);
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        for (PairItem item : svc.items(run.id)) {
            assertEquals(PairState.ACCEPTED, item.state);
            assertEquals(0, item.calls);
            assertEquals(BigDecimal.ZERO, item.usd);
            assertEquals("NO_FIX_POINTS", gate(item).getString("status"));
        }
        assertTrue(memory.allReservations(run.id).isEmpty());
        assertEquals(EditorialPairSnapshot.of(run).map.draft.text, done.mergedText);
    }

    @Test public void theRequestOfEachPairIsReservedAndJournalledBeforeTheProviderIsCalled() throws Exception {
        List<String> seenStates = new ArrayList<>();
        EditorialPairRunService[] holder = new EditorialPairRunService[1];
        PairRun[] runHolder = new PairRun[1];
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> {
            PairItem item = memory.items(runHolder[0].id).get(index);
            seenStates.add(item.state + "/" + memory.openReservations(runHolder[0].id).size());
            return targetedText(request, index, (i, d) -> d, "stop", new BigDecimal("0.0001"));
        });
        holder[0] = service(memory, fake);
        runHolder[0] = prepare(holder[0], source(3), "C");
        holder[0].execute(runHolder[0].id, null);
        assertEquals(List.of("E_SENT/1", "E_SENT/1", "E_SENT/1"), seenStates);
    }

    // ---- inconvenient but valid answers (STRUCTURAL only)

    @Test public void targetedFixesApplyOnlyToDeclaredLinesAndUnsafeAnswersStopTheRun() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> {
            if (index == 2) return targetedText(request, index, (i, d) -> "<EDITED>unsafe</EDITED>", "stop", new BigDecimal("0.0001"));
            return targetedText(request, index, (i, d) -> index == 1 ? d.replace("かな", "đã sửa") : "=",
                    "stop", new BigDecimal("0.0001"));
        });
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(4), "C");
        PairRun done = svc.execute(run.id, null);
        List<PairItem> items = svc.items(run.id);
        assertEquals(PairState.ACCEPTED, items.get(0).state);
        assertEquals(PairState.ACCEPTED, items.get(1).state);
        assertTrue(items.get(1).candidateText.contains("dòng 3: đã sửa"));
        assertEquals(PairState.STRUCTURE_BLOCKED, items.get(2).state);
        assertTrue(items.get(2).gateJson.contains("TARGETED_LINE_REJECTED"));
        assertEquals(PairState.IMPORTED, items.get(3).state);
        assertEquals(3, fake.requests.size());
        assertEquals(RunState.INCOMPLETE, done.state);
        assertTrue(svc.exportPlan(run.id).text.contains("dòng 3: かな"));
    }

    @Test public void aShortTargetAnswerIsBlockedInsteadOfLosingTheRestOfItsLine() throws Exception {
        String longLine = "Bản DRAFT có đủ nội dung để kiểm tra phần sửa theo điểm mục tiêu ".repeat(7) + "かな";
        List<String> raw = List.of("「長い行の意味を保ってください。」\n\n");
        List<String> draft = List.of(longLine + "\n\n");
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> targetedText(request, index, (i, d) -> "短い", "stop", new BigDecimal("0.0001")));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = svc.prepare(1, new EditorialPairSource("JOB", "7", "x", "", raw, draft, List.of()), "", "", null, "m", "Vietnamese", CAP, "C");
        PairRun done = svc.execute(run.id, null);
        PairItem cut = svc.items(run.id).get(0);
        assertEquals(PairState.STRUCTURE_BLOCKED, cut.state);
        assertTrue(cut.gateJson.contains("TARGETED_LINE_REJECTED") && cut.gateJson.contains("LINE_LENGTH_RATIO"));
        assertEquals(longLine, cut.candidateText);
        assertTrue(cut.responseText.contains("[1]"));
        assertEquals(RunState.FINAL_BLOCKED, done.state);
        assertEquals("", done.mergedText);
        EditorialPairRunService.ExportPlan plan = svc.exportPlan(run.id);
        assertFalse(plan.complete);
        assertTrue(plan.label.startsWith("Bản tạm: 0/1"));
        assertTrue("the rejected answer never replaces the source line", plan.text.contains(longLine));
    }

    @Test public void anAnswerClosedWithTheWrongTagIsAVisibleWarningAndTheRunKeepsGoing() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> targetedText(request, index,
                (i, d) -> "=" + (index == 1 ? "\nunrecognized extra text" : ""), "stop", new BigDecimal("0.0001")));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(3), "C");
        PairRun done = svc.execute(run.id, null);
        assertEquals(3, fake.requests.size());          // a warning does not stop the batch
        List<PairItem> items = svc.items(run.id);
        assertEquals(PairState.ACCEPTED, items.get(0).state);
        assertEquals(PairState.WARN_REVIEW, items.get(1).state);
        assertEquals("WARN", gate(items.get(1)).getString("status"));
        assertTrue(items.get(1).gateJson.contains("TARGETED_IGNORED_LINES"));
        assertFalse(items.get(0).gateJson.contains("TARGETED_IGNORED_LINES"));
        assertEquals(PairState.ACCEPTED, items.get(2).state);
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);   // a warning is shown to the person, it does not hold the merge back
        assertEquals(1, done.warnings);
        EditorialPairRunService.ExportPlan plan = svc.exportPlan(run.id);
        assertTrue(plan.complete);
        assertTrue(plan.label, plan.label.contains("1 cảnh báo"));
    }

    @Test public void answersWithoutTagsWrongPairAndProviderErrorsEndAsBlockedPairsWithTheirCodes() throws Exception {
        List<String> answers = List.of("không có thẻ nào", "<WRONG_PAIR>RAW và DRAFT khác chương: tên khác</WRONG_PAIR>");
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> FakeEditorialApiProvider.text(answers.get(index), "stop"));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(3), "C");
        PairRun done = svc.execute(run.id, null);
        List<PairItem> items = svc.items(run.id);
        assertEquals(PairState.STRUCTURE_BLOCKED, items.get(0).state);
        assertTrue(items.get(0).gateJson.contains("FORMAT"));
        assertEquals(1, fake.requests.size());
        assertEquals(PairState.IMPORTED, items.get(1).state);
        // Explicit continuation inspects the next pair; it never retries the malformed one.
        done = svc.execute(run.id, null);
        items = svc.items(run.id);
        assertEquals(PairState.STRUCTURE_BLOCKED, items.get(1).state);
        assertTrue(items.get(1).error.startsWith("TARGETED_ANSWER_MISSING"));
        assertEquals("a wrong pair stops the run: the map may be wrong", PairState.IMPORTED, items.get(2).state);
        assertEquals(2, fake.requests.size());
        assertEquals(RunState.INCOMPLETE, done.state);
    }

    @Test public void aDefiniteFailureIsRetriedOnceAtZeroCostThenTheBatchStops() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> index < 2
                ? EditorialApiFlow.StepResponse.failure("ApiHttpException: 429")
                : targetedText(request, index, (i, d) -> "=", "stop", new BigDecimal("0.0001")));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(2), "C");
        PairRun done = svc.execute(run.id, null);
        List<PairItem> items = svc.items(run.id);
        assertEquals(PairState.STRUCTURE_BLOCKED, items.get(0).state);
        assertTrue(items.get(0).gateJson.contains("PROVIDER_FAILURE"));
        assertEquals(2, items.get(0).calls);
        assertEquals(PairState.IMPORTED, items.get(1).state);
        assertEquals(2, fake.requests.size());
        assertEquals(0, items.get(0).usd.signum());
        assertTrue(items.get(0).costKnown);
        int open = memory.openReservations(run.id).size();
        assertEquals("every reservation of a call that never billed is closed at zero", 0, open);
        assertEquals(RunState.INCOMPLETE, done.state);
    }

    // ---- unknown outcome, reservation refusal, overrun, price

    @Test public void anUnknownOutcomeStopsTheRunKeepsThePriorPairsAndNeverResends() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> index == 1
                ? EditorialApiFlow.StepResponse.unknownOutcome("SocketTimeoutException")
                : targetedText(request, index, (i, d) -> "=", "stop", new BigDecimal("0.0001")));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(4), "C");
        PairRun done = svc.execute(run.id, null);
        List<PairItem> items = svc.items(run.id);
        assertEquals(PairState.ACCEPTED, items.get(0).state);
        assertEquals(PairState.UNKNOWN, items.get(1).state);
        assertEquals(PairState.IMPORTED, items.get(2).state);
        assertEquals(RunState.UNKNOWN, done.state);
        assertFalse("the cost of an unknown call is not zero", items.get(1).costKnown);
        assertEquals(1, memory.openReservations(run.id).size());
        assertFalse(done.costKnown);
        // running again does not resend the unknown pair and does not continue past it
        int before = fake.requests.size();
        PairRun again = svc.execute(run.id, null);
        assertEquals("the unknown pair is never asked again, the remaining ones go on", before + 2, fake.requests.size());
        assertEquals(PairState.UNKNOWN, svc.items(run.id).get(1).state);
        assertEquals(RunState.UNKNOWN, again.state);
        assertEquals(1, memory.openReservations(run.id).size());
    }

    @Test public void aRefusedReservationSendsNothingAndLeavesTheRestUnstarted() throws Exception {
        FakeEditorialApiProvider fake = provider((i, d) -> d);
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = svc.prepare(1, source(3), "", "", null, "m", "Vietnamese", new BigDecimal("0.00001"), "C");
        PairRun done = svc.execute(run.id, null);
        assertEquals(0, fake.requests.size());
        assertEquals(PairState.RESERVE_FAILED, svc.items(run.id).get(0).state);
        assertEquals(PairState.IMPORTED, svc.items(run.id).get(1).state);
        assertEquals(RunState.INCOMPLETE, done.state);
        assertEquals(0, done.calls);
        assertTrue(memory.allReservations(run.id).isEmpty());
    }

    @Test public void aChargeAboveItsReservationIsRecordedExactlyAndStopsFurtherSending() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, d) -> "="), "stop", 1500, 400, new BigDecimal("5.0"), true, "m", "r", ""));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = svc.prepare(1, source(3), "", "", null, "m", "Vietnamese", new BigDecimal("100"), "C");
        PairRun done = svc.execute(run.id, null);
        assertEquals(1, fake.requests.size());
        assertTrue(done.costOverrun);
        assertEquals(0, new BigDecimal("5.0").compareTo(done.usd));
        PairReservation r = memory.allReservations(run.id).get(0);
        assertEquals(0, new BigDecimal("5.0").compareTo(r.settledUsd));
        assertEquals(RunState.INCOMPLETE, done.state);
    }

    @Test public void anAnswerWithoutAPriceSettlesAtTheWorstCaseAndMarksTheRunCostUnknown() throws Exception {
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, d) -> "="), "stop", 1500, 400, BigDecimal.ZERO, false, "m", "r", ""));
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = prepare(svc, source(2), "C");
        PairRun done = svc.execute(run.id, null);
        assertFalse(done.costKnown);
        for (PairReservation r : memory.allReservations(run.id)) {
            assertEquals(PairReservation.SETTLED, r.state);
            assertTrue(r.settledUsd.signum() > 0);
            assertEquals("PRICE_UNKNOWN_RESERVED_AT_WORST", r.reason);
        }
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
    }

    // ---- process death windows (host simulation of the journal, not a device)

    private PairRun crashAt(FaultyPairRunStore faulty, EditorialPairRunService svc, long runId) {
        try {
            svc.execute(runId, null);
            fail("the simulated process death did not happen");
        } catch (FaultyPairRunStore.SimulatedCrash expected) {
            // the process is gone; ACTIVE is released by the finally block of the dead call
        }
        return memory.getRun(runId);
    }

    @Test public void deathAfterTheReservationButBeforeTheJournalLeavesAnOrphanThatRestartsAsUnknown() throws Exception {
        FaultyPairRunStore faulty = new FaultyPairRunStore(memory).crashAfter("reserve");
        FakeEditorialApiProvider fake = provider((i, d) -> d);
        EditorialPairRunService svc = service(faulty, fake);
        PairRun run = prepare(svc, source(2), "C");
        crashAt(faulty, svc, run.id);
        assertEquals(0, fake.requests.size());
        faulty.disarm();
        EditorialPairRunService restarted = service(memory, fake);
        assertTrue(restarted.needsRecovery(run.id));
        restarted.recover(run.id);
        assertEquals(PairState.UNKNOWN, restarted.items(run.id).get(0).state);
        assertEquals(1, memory.openReservations(run.id).size());
        assertEquals(RunState.UNKNOWN, restarted.get(run.id).state);
        assertEquals(0, fake.requests.size());
    }

    @Test public void deathAfterReservedButBeforeSentIsProvablyNotDispatchedAndSettlesAtZero() throws Exception {
        FaultyPairRunStore faulty = new FaultyPairRunStore(memory).crashAfter("state:E_RESERVED");
        FakeEditorialApiProvider fake = provider((i, d) -> d);
        EditorialPairRunService svc = service(faulty, fake);
        PairRun run = prepare(svc, source(2), "C");
        crashAt(faulty, svc, run.id);
        assertEquals(0, fake.requests.size());
        faulty.disarm();
        EditorialPairRunService restarted = service(memory, fake);
        restarted.recover(run.id);
        assertEquals(PairState.IMPORTED, restarted.items(run.id).get(0).state);
        assertTrue(memory.openReservations(run.id).isEmpty());
        assertEquals(PairReservation.SETTLED_ZERO, memory.allReservations(run.id).get(0).state);
        // the person runs again: now it is sent exactly once per pair
        PairRun done = restarted.execute(run.id, null);
        assertEquals(2, fake.requests.size());
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
    }

    @Test public void deathAfterSentIsUnknownNeverResentAndKeepsEarlierPairs() throws Exception {
        FaultyPairRunStore faulty = new FaultyPairRunStore(memory);
        FakeEditorialApiProvider fake = new FakeEditorialApiProvider((request, index) -> {
            if (index == 1) faulty.crashBefore("commitReceived"); // the answer came back, the process died before it was stored
            return targetedText(request, index, (i, d) -> "=", "stop", new BigDecimal("0.0001"));
        });
        EditorialPairRunService svc = service(faulty, fake);
        PairRun run = prepare(svc, source(3), "C");
        crashAt(faulty, svc, run.id);
        assertEquals(2, fake.requests.size());
        assertEquals(PairState.ACCEPTED, memory.items(run.id).get(0).state);
        assertEquals(PairState.E_SENT, memory.items(run.id).get(1).state);
        faulty.disarm();
        EditorialPairRunService restarted = service(memory, fake);
        restarted.recover(run.id);
        assertEquals(PairState.ACCEPTED, restarted.items(run.id).get(0).state);
        assertEquals(PairState.UNKNOWN, restarted.items(run.id).get(1).state);
        assertEquals(1, memory.openReservations(run.id).size());
        PairRun again = restarted.execute(run.id, null);
        assertEquals("only the untouched pair is sent after the restart", 3, fake.requests.size());
        assertEquals(PairState.UNKNOWN, restarted.items(run.id).get(1).state);
        assertEquals(RunState.UNKNOWN, again.state);
    }

    @Test public void deathAfterTheCommitKeepsTheAnswerAndNeverAsksThatPairAgain() throws Exception {
        FaultyPairRunStore faulty = new FaultyPairRunStore(memory).crashAfter("commitReceived");
        FakeEditorialApiProvider fake = provider((i, d) -> d);
        EditorialPairRunService svc = service(faulty, fake);
        PairRun run = prepare(svc, source(2), "C");
        crashAt(faulty, svc, run.id);
        assertEquals(1, fake.requests.size());
        faulty.disarm();
        EditorialPairRunService restarted = service(memory, fake);
        assertFalse(restarted.needsRecovery(run.id));
        PairRun done = restarted.execute(run.id, null);
        assertEquals("the committed pair is not asked again", 2, fake.requests.size());
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        for (PairReservation r : memory.allReservations(run.id)) assertEquals(PairReservation.SETTLED, r.state);
    }

    @Test public void aSettledPairCannotBeSettledTwice() throws Exception {
        EditorialPairRunService svc = service(memory, provider((i, d) -> d));
        PairRun run = prepare(svc, source(1), "C");
        svc.execute(run.id, null);
        PairReservation r = memory.allReservations(run.id).get(0);
        try {
            memory.settleZero(r.callId, "again");
            fail("a second settlement must be refused");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("SETTLE_TWICE"));
        }
    }

    // ---- W and C on the same snapshot, reopen

    @Test public void wholeAndChunkRunOnTheSameSnapshotWithTheCallsOfTheirShape() throws Exception {
        FakeEditorialApiProvider fakeW = provider((i, d) -> d);
        FakeEditorialApiProvider fakeC = provider((i, d) -> d);
        InMemoryPairRunStore storeW = new InMemoryPairRunStore();
        InMemoryPairRunStore storeC = new InMemoryPairRunStore();
        EditorialPairRunService w = service(storeW, fakeW);
        EditorialPairRunService c = service(storeC, fakeC);
        PairRun runW = w.prepare(1, source(4), "", "", null, "m", "Vietnamese", CAP, "W");
        PairRun runC = c.prepare(1, source(4), "", "", null, "m", "Vietnamese", CAP, "C");
        assertEquals(runW.mapHash, runC.mapHash);
        PairRun doneW = w.execute(runW.id, null);
        PairRun doneC = c.execute(runC.id, null);
        assertEquals(1, fakeW.requests.size());
        assertEquals(4, fakeC.requests.size());
        assertEquals(RunState.FINAL_ELIGIBLE, doneW.state);
        assertEquals(doneW.mergedText, doneC.mergedText); // both unchanged: the same document
        assertTrue(fakeW.requests.get(0).prompt().user().startsWith("# RAW\n"));
        assertFalse(fakeW.requests.get(0).prompt().user().contains("REFERENCE ONLY"));
    }

    @Test public void reopeningAStoredRunNeedsNeitherTheFilesNorTheProvider() throws Exception {
        FakeEditorialApiProvider fake = provider((i, d) -> i == 1 ? d.replace("dòng 3", "dòng ba") : d);
        EditorialPairRunService svc = service(memory, fake);
        PairRun run = svc.prepare(1, source(3), "", "", null, "m", "Vietnamese", CAP, "C");
        svc.execute(run.id, null);
        int requests = fake.requests.size();
        FakeEditorialApiProvider mustNotBeCalled = new FakeEditorialApiProvider((request, index) -> { throw new AssertionError("reopen must not call the provider"); });
        EditorialPairRunService reopened = service(memory, mustNotBeCalled);
        PairRun stored = reopened.get(run.id);
        assertEquals(RunState.FINAL_ELIGIBLE, stored.state);
        assertEquals(requests, fake.requests.size());
        assertTrue(reopened.exportPlan(run.id).text.contains("dòng ba"));
        assertEquals(stored.rawRowsJson, memory.getRun(run.id).rawRowsJson);
        assertNull(reopened.get(999));
        assertNotNull(EditorialPairSnapshot.of(stored).map);
    }

    @Test public void aStoredRunWhoseRowsWereTamperedWithIsRefusedNotMerged() throws Exception {
        EditorialPairRunService svc = service(memory, provider((i, d) -> d));
        PairRun run = prepare(svc, source(2), "C");
        PairRun tampered = memory.getRun(run.id);
        tampered.draftRowsJson = EditorialPairSnapshot.rowsJson(List.of(draftRow(1), draftRow(9)));
        memory.updateRun(tampered);
        try {
            svc.execute(run.id, null);
            fail("a changed snapshot must be refused");
        } catch (EditorialPairSnapshot.Mismatch expected) {
            assertTrue(expected.getMessage().contains("map hash"));
        }
    }
}
