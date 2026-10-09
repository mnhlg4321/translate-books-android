package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The address checklist: where the app tells the model to look, from certain facts only; it never carries book text or an answer. */
public final class AddressChecklistTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note\n";
    private static final List<String> RAW_ROWS = List.of(
            "「吉岡さん、私だよ。覚えてるよね！」\n\n私は必死で呼びかけた。\n\n「ありがとう、吉岡さん」\n\n「だれ？」\n");
    private static final List<String> DRAFT_ROWS = List.of(
            "「Yoshioka, là tôi đây. Cậu vẫn nhớ chứ, cậu?」\n\nTôi gọi cô ta, nhưng cô ta không đáp.\n\n「Cảm ơn Yoshioka」\n\n「Ai đó, em?」\n");

    private static PairMap map(List<String> raw, List<String> draft) {
        DocManifest r = DocManifest.fromRows(DocManifest.Kind.RAW, "raw", "CH007", raw, false);
        DocManifest d = DocManifest.fromRows(DocManifest.Kind.DRAFT, "draft", "CH007", draft, true);
        List<PairMap.Spec> specs = new ArrayList<>();
        for (int i = 0; i < r.units().size(); i++) specs.add(PairMap.Spec.one(r.units().get(i).id(), d.units().get(i).id(), "job-row"));
        return PairMap.build("m1", r, d, specs);
    }

    private static ApiPrompt prompt(String rowCsv) {
        PairMap map = map(RAW_ROWS, DRAFT_ROWS);
        return PairPromptBuilder.buildPair(map, map.entries().get(0), "Vietnamese", List.of(), HEADER + rowCsv, null);
    }

    private static String section(ApiPrompt p) {
        int a = p.system().indexOf("# ADDRESS CHECK");
        if (a < 0) return "";
        return p.system().substring(a, p.system().indexOf("OUTPUT CONTRACT"));
    }

    @Test public void listsOnlyQuotedParagraphsInScopeWhoseDraftUsesAnotherAddressWord() {
        ApiPrompt p = prompt("吉岡さん,Erika,Yoshioka,tôi,Yoshioka,p001-p003,lạnh\n");
        String s = section(p);
        assertTrue(s, s.contains("Row 吉岡さん (Erika -> Yoshioka; self \"tôi\", call \"Yoshioka\")"));
        assertTrue(s, s.contains("quoted RAW paragraphs in scope P001, P003"));
        assertTrue(s, s.contains("P001 \"cậu\" x2"));
        assertFalse("narration is not a place to check", s.contains("P002"));
        assertFalse("a paragraph already using the row's forms is not listed as a candidate", s.contains("P003 \""));
        assertFalse("outside the row's scope", s.contains("P004"));
        assertTrue(p.system().indexOf("# ADDRESS CHECK") > p.system().indexOf("# PRONOUNS"));
        assertTrue(p.system().indexOf("# ADDRESS CHECK") < p.system().indexOf("OUTPUT CONTRACT"));
        assertFalse("the user message is untouched", p.user().contains("ADDRESS CHECK"));
    }

    @Test public void theChecklistCarriesLabelsAndWordsOnlyNeverBookTextOrTheAnswer() {
        String s = section(prompt("吉岡さん,Erika,Yoshioka,tôi,Yoshioka,p001-p003,lạnh\n"));
        assertFalse(s.contains("nhớ chứ"));
        assertFalse(s.contains("覚えてる"));
        assertFalse("it does not say what to write beyond naming the row's own fields", s.contains("replace"));
        assertTrue(s.contains("it says where to look, not what is correct"));
    }

    @Test public void aRowWithoutACallOrWithNothingToCheckAddsNoSection() {
        assertEquals("", section(prompt("私,@NARRATOR,@AUDIENCE,tôi,,*,kể\n")));
        assertEquals("a row whose call is the word the DRAFT already uses has nothing to check", "",
                section(prompt("吉岡さん,Erika,Yoshioka,tôi,cậu,p001-p003,thân\n")));
        assertEquals("", section(prompt("")));
    }

    @Test public void paragraphsThatDoNotLineUpOneToOneGetNoChecklist() {
        PairMap map = map(RAW_ROWS, List.of("「Yoshioka, là tôi đây. Cậu vẫn nhớ chứ?」\n\nTôi gọi cô ta.\n"));
        ApiPrompt p = PairPromptBuilder.buildPair(map, map.entries().get(0), "Vietnamese", List.of(), HEADER + "吉岡さん,Erika,Yoshioka,tôi,Yoshioka,p001-p003,lạnh\n", null);
        assertFalse(p.system().contains("ADDRESS CHECK"));
    }

    @Test public void theWholeChapterRequestIsUnchanged() {
        PairMap map = map(RAW_ROWS, DRAFT_ROWS);
        ApiPrompt whole = PairPromptBuilder.buildWhole(map, "Vietnamese", List.of(), HEADER + "吉岡さん,Erika,Yoshioka,tôi,Yoshioka,p001-p003,lạnh\n", null);
        assertFalse(whole.system().contains("ADDRESS CHECK"));
    }

    @Test public void aDraftThatRunsTwoParagraphsTogetherStillPairsLineByLineWhenEveryRawParagraphIsOneLine() {
        List<String> raw = List.of("「吉岡さん、私だよ。」\n\n私は呼びかけた。\n\n「ありがとう」\n");
        List<String> draft = List.of("「Yoshioka, là tôi đây. Cậu vẫn nhớ chứ?」\n\nTôi gọi cô ta.\n「Cảm ơn Yoshioka」\n");
        PairMap map = map(raw, draft);
        ApiPrompt p = PairPromptBuilder.buildPair(map, map.entries().get(0), "Vietnamese", List.of(), HEADER + "吉岡さん,Erika,Yoshioka,tôi,Yoshioka,p001-p003,lạnh\n", null);
        assertTrue(section(p), section(p).contains("P001 \"cậu\""));
        // paragraphs that really have several lines cannot be paired by line: no checklist
        PairMap multi = map(List.of("「吉岡さん」\n私は\n\n「だれ」\n"), List.of("「Yoshioka, cậu」\n\nTôi\n\nCòn lại\n"));
        ApiPrompt q = PairPromptBuilder.buildPair(multi, multi.entries().get(0), "Vietnamese", List.of(), HEADER + "吉岡さん,Erika,Yoshioka,tôi,Yoshioka,p001-p002,lạnh\n", null);
        assertFalse(q.system().contains("ADDRESS CHECK"));
    }

    @Test public void tokensAreWholeWordsInNfcAndTheRowsOwnFormsAreExcluded() {
        assertEquals(List.of("cậu", "vẫn", "nhớ"), AddressChecklist.tokens("Cậu vẫn nhớ!"));
        assertEquals(List.of("cậu"), AddressChecklist.tokens("Cậu".replace("ậ", "ậ")));
        List<String> blocks = AddressChecklist.paragraphs("a\nb\n\n\n c\n");
        assertEquals(List.of("a\nb", " c"), blocks);
        assertFalse("a word that only contains an address word is not one", AddressChecklist.tokens("cậubé").contains("cậu"));
    }
}
