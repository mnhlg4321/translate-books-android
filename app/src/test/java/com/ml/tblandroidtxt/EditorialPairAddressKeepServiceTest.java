package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.EditorialApiFlow;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import com.ml.tblandroidtxt.editorial.api.PairStates.RunState;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Service, store and reopen over synthetic characters: an ADDRESS answer that the pronoun rows cannot justify keeps only its own
 * DRAFT line, and the valid repairs of the same chunk are applied, stored and still there after the run is reopened.
 */
public final class EditorialPairAddressKeepServiceTest {
    private static final String PRONOUN = "from,speaker,target,self,call,scope,note\nリナ,Aki,Rina,mình,Rina,*,close friends\n森先生,Aki,Mori,em,cô,*,teacher\n";
    private static final String GLOSSARY = "竜王\tLong Vương\tname\t\n";
    private static final String RAW = "「リナ……お願い、それを覚えさせて」\n竜王が静かに現れた。\n「それ、ちゃんと覚えたいな」\n\n";
    private static final String DRAFT = "「Rina…… làm ơn, hãy cho tớ học cái đó nhé」\nRồng vương đã lặng lẽ xuất hiện trước mặt.\n「Tớ muốn học thật kỹ cái đó」\n\n";
    private static final BigDecimal CAP = new BigDecimal("0.05");
    private static final EditorialApiRunService.Pricing PRICING = new EditorialApiRunService.Pricing() {
        @Override public BigDecimal inputPerToken(String model) { return new BigDecimal("0.00000025"); }

        @Override public BigDecimal outputPerToken(String model) { return new BigDecimal("0.0000012"); }
    };

    private final InMemoryPairRunStore store = new InMemoryPairRunStore();

    private static EditorialPairSource source() {
        return new EditorialPairSource(EditorialPairModels.SOURCE_JOB, "7", "truyen.txt", "", List.of(RAW), List.of(DRAFT), List.of());
    }

    /**
     * Returns an answer per point from the point's own DRAFT line: the first cites Aki → Rina (R1) yet uses a form of another row,
     * the others are valid repairs (the ADDRESS one cites R1). {@code brokenAnswers} drops a protected symbol from that many
     * answers: 1 = the first, 2 = the first and the glossary repair.
     */
    private static FakeEditorialApiProvider provider(int brokenAnswers) {
        return new FakeEditorialApiProvider((request, index) -> new EditorialApiFlow.StepResponse(
                EditorialPairTestData.targetedAnswer(request, index, (i, d) -> {
                    if (d.contains("Rina")) return "R1 " + (brokenAnswers >= 1 ? d.replace("tớ", "em").replace("「", "") : d.replace("tớ", "em"));
                    if (d.contains("Rồng vương")) return d.replace("Rồng vương", "Long Vương") + (brokenAnswers >= 2 ? "!" : "");
                    return "R1 " + d.replace("Tớ", "Mình");
                }), "stop", 1500, 400, new BigDecimal("0.0001"), true, "fake-model", "fake-route", ""));
    }

    private EditorialPairRunService service(EditorialApiProvider provider) {
        return new EditorialPairRunService(store, provider, PRICING, 60_000L);
    }

    private PairRun run(EditorialPairRunService service) throws Exception {
        PairRun prepared = service.prepare(1, source(), GLOSSARY, PRONOUN, null, "model-x", "Vietnamese", CAP, "C");
        return service.execute(prepared.id, null);
    }

    private static JSONObject point(PairItem item, int id) throws Exception {
        JSONArray points = new JSONObject(item.gateJson).getJSONArray("fixPoints");
        for (int i = 0; i < points.length(); i++) if (points.getJSONObject(i).getInt("id") == id) return points.getJSONObject(i);
        throw new AssertionError("no point " + id);
    }

    @Test public void anAddressPointKeptAsDraftDoesNotBlockTheValidRepairsAndSurvivesReopening() throws Exception {
        FakeEditorialApiProvider fake = provider(0);
        EditorialPairRunService svc = service(fake);
        PairRun done = run(svc);

        assertEquals(1, fake.requests.size());
        PairItem item = svc.items(done.id).get(0);
        assertEquals(PairState.ACCEPTED, item.state);
        assertEquals("", item.error == null ? "" : item.error);
        assertFalse(item.gateJson.contains("TARGETED_LINE_REJECTED"));
        JSONObject address = point(item, 1);
        assertEquals("KEPT", address.getString("status"));
        assertEquals("ADDRESS_FORM_NOT_IN_CITED_ROW", address.getString("reason"));
        assertEquals("FIXED", point(item, 2).getString("status"));
        assertEquals("FIXED", point(item, 3).getString("status"));

        String expected = "「Rina…… làm ơn, hãy cho tớ học cái đó nhé」\nLong Vương đã lặng lẽ xuất hiện trước mặt.\n「Mình muốn học thật kỹ cái đó」\n\n";
        assertEquals(expected.trim(), item.candidateText.trim());
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        assertEquals(expected.trim(), svc.exportPlan(done.id).text.trim());

        // reopen: a new service over the same store, a provider that must not be called
        FakeEditorialApiProvider silent = new FakeEditorialApiProvider((request, index) -> { throw new AssertionError("reopen must not call the provider"); });
        EditorialPairRunService reopened = service(silent);
        PairRun stored = reopened.get(done.id);
        assertEquals(RunState.FINAL_ELIGIBLE, stored.state);
        PairItem again = reopened.items(done.id).get(0);
        assertEquals(PairState.ACCEPTED, again.state);
        assertEquals(expected.trim(), again.candidateText.trim());
        assertEquals("ADDRESS_FORM_NOT_IN_CITED_ROW", point(again, 1).getString("reason"));
        assertEquals("FIXED", point(again, 2).getString("status"));
        assertEquals(expected.trim(), reopened.exportPlan(done.id).text.trim());
        assertEquals(0, silent.requests.size());
    }

    /** v4.22: the refused answer is shown as a proposal and a person can apply it; the export then carries it (Kumo ch 013 line 151). */
    @Test public void aRefusedProposalIsShownAndAppliedByThePersonIntoTheExport() throws Exception {
        EditorialPairRunService svc = service(provider(0));
        PairRun done = run(svc);
        PairItem item = svc.items(done.id).get(0);
        JSONObject refused = point(item, 1);
        assertEquals("「Rina…… làm ơn, hãy cho em học cái đó nhé」", refused.getString("proposed"));
        assertEquals(1, refused.getInt("citedRow"));
        assertTrue(refused.getString("citedRowText").startsWith("R1 リナ · Aki → Rina"));
        assertTrue(EditorialResultPresentation.awaiting(item, refused));
        assertTrue(EditorialResultPresentation.summary(done, List.of(item), 2).contains("1 bị chặn"));
        assertTrue(EditorialResultPresentation.proposalHint(refused).contains("Yuki-chan/em"));

        assertEquals(null, svc.applyProposal(done.id, item.pairId, 1));
        String expected = "「Rina…… làm ơn, hãy cho em học cái đó nhé」\nLong Vương đã lặng lẽ xuất hiện trước mặt.\n「Mình muốn học thật kỹ cái đó」";
        assertEquals(expected, svc.exportPlan(done.id).text.trim());
        assertEquals(RunState.FINAL_ELIGIBLE, svc.get(done.id).state);
        assertTrue(svc.get(done.id).mergedText.trim().equals(expected));
        PairItem applied = svc.items(done.id).get(0);
        JSONObject owner = point(applied, 1);
        assertEquals("OWNER_APPLIED", owner.getString("status"));
        assertEquals("ADDRESS_FORM_NOT_IN_CITED_ROW", owner.getString("blockedReason"));
        assertTrue(EditorialResultPresentation.applied(applied, owner));
        assertFalse(EditorialResultPresentation.awaiting(applied, owner));
        assertTrue(store.journal(done.id).stream().anyMatch(e -> e.endsWith("PROPOSAL_APPLIED id=1")));
        assertEquals("Không có đề xuất để áp dụng.", svc.applyProposal(done.id, item.pairId, 1));
        assertEquals("Không có đề xuất để áp dụng.", svc.applyProposal(done.id, item.pairId, 2));
    }

    @Test public void aProposalOfABlockedChunkCannotBeApplied() throws Exception {
        EditorialPairRunService svc = service(provider(2));
        PairRun done = run(svc);
        PairItem item = svc.items(done.id).get(0);
        assertEquals("Đoạn này không nằm trong bản ghép.", svc.applyProposal(done.id, item.pairId, 1));
        assertEquals(DRAFT.trim(), svc.exportPlan(done.id).text.trim());
    }

    @Test public void oneMalformedAnswerKeepsItsDraftLineWhileTheOtherRepairsApply() throws Exception {
        EditorialPairRunService svc = service(provider(1));
        PairRun done = run(svc);
        PairItem item = svc.items(done.id).get(0);
        assertEquals(PairState.ACCEPTED, item.state);
        assertFalse(item.gateJson.contains("TARGETED_LINE_REJECTED"));
        assertEquals("KEPT", point(item, 1).getString("status"));
        assertEquals("PROTECTED_SYMBOL_CHANGE", point(item, 1).getString("reason"));
        assertEquals("FIXED", point(item, 2).getString("status"));
        assertEquals("FIXED", point(item, 3).getString("status"));
        assertEquals(RunState.FINAL_ELIGIBLE, done.state);
        assertEquals("「Rina…… làm ơn, hãy cho tớ học cái đó nhé」\nLong Vương đã lặng lẽ xuất hiện trước mặt.\n「Mình muốn học thật kỹ cái đó」",
                svc.exportPlan(done.id).text.trim());
    }

    @Test public void twoMalformedAnswersStillBlockTheChunk() throws Exception {
        EditorialPairRunService svc = service(provider(2));
        PairRun done = run(svc);
        PairItem item = svc.items(done.id).get(0);
        assertEquals(PairState.STRUCTURE_BLOCKED, item.state);
        assertTrue(item.gateJson.contains("TARGETED_LINE_REJECTED"));
        assertTrue(item.gateJson.contains("PROTECTED_SYMBOL_CHANGE"));
        assertEquals(DRAFT.trim(), svc.exportPlan(done.id).text.trim());
    }
}
