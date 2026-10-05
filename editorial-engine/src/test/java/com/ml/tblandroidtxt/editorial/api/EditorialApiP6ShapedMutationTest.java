package com.ml.tblandroidtxt.editorial.api;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.Mode;
import com.ml.tblandroidtxt.editorial.api.EditorialApiContract.RunState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The irregularities that stopped P6 (ruby readings in quotes, quotes that do not match, blank lines, extra fields, wrong
 * numbers of columns) rebuilt as model answers for the new flow: not one of them may throw or stop the run. Synthetic text.
 */
public final class EditorialApiP6ShapedMutationTest {
    private static final String RAW = "太郎《たろう》は赤い扉を開けた。\n\n花子が言った。「五百メートル先です」\n";
    private static final String DRAFT = "Taro mở cánh cửa đỏ.\n\nHanako nói: 「Cách năm mươi mét.」\n";
    private static final String EDITED = "<EDITED>Taro mở cánh cửa đỏ.\n\nHanako nói: 「Cách năm trăm mét.」\n</EDITED>";

    private static EditorialApiFlow.StepResponse ok(String content) {
        return new EditorialApiFlow.StepResponse(content, "stop", 100, 50, BigDecimal.ONE.movePointLeft(3), true, "m", "r", "");
    }

    private static String check(String body) {
        return "{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"issues\":[" + body + "]}";
    }

    private static String issue(String quote, String raw, String kind, String fix, String extra) {
        return "{\"edited_quote\":\"" + quote + "\",\"raw_quote\":\"" + raw + "\",\"kind\":\"" + kind + "\",\"fix\":\"" + fix + "\"" + extra + "}";
    }

    private static EditorialApiFlow.Outcome run(Mode mode, String editAnswer, String... checkAnswers) {
        EditorialApiFlow flow = new EditorialApiFlow(new EditInputs(RAW, DRAFT, "Vietnamese", List.of(), ""), EditorialApiFlow.Config.of(mode));
        List<String> script = new ArrayList<>();
        script.add(editAnswer);
        script.addAll(List.of(checkAnswers));
        int next = 0;
        int calls = 0;
        while (flow.nextRequest() != null) {
            String answer = next < script.size() ? script.get(next++) : PASS_FALLBACK;
            flow.accept(ok(answer));
            assertTrue("bounded", ++calls <= 6);
        }
        assertNotNull(flow.outcome());
        return flow.outcome();
    }

    private static final String PASS_FALLBACK = "{\"verdict\":\"PASS\",\"wrong_pair_evidence\":\"\",\"issues\":[]}";

    @Test public void checkAnswersShapedLikeTheP6SlipsNeverStopTheRun() {
        List<String> answers = new ArrayList<>();
        // quote with a ruby reading that EDITED does not have, quote from RAW instead of EDITED
        answers.add(check(issue("太郎《たろう》", "太郎", "MEANING", "Taro", "")));
        // quote that does not occur at all
        answers.add(check(issue("câu không có", "x", "OMISSION", "y", "")));
        // blank lines and whitespace-only quotes
        answers.add(check(issue("", "", "MEANING", "z", "") + "," + issue("   ", "a", "MEANING", "z", "")));
        // extra fields at both levels
        answers.add(check(issue("Hanako", "花子", "SPEAKER", "", ",\"severity\":\"high\",\"line\":3")).replace("{\"verdict\"", "{\"note\":1,\"verdict\""));
        // unknown kind, kind in lower case, missing kind
        answers.add(check(issue("Taro", "", "WRONG_KIND", "", "") + "," + issue("Hanako", "", "number", "", "")));
        answers.add(check("{\"edited_quote\":\"Taro\"}"));
        // a quote that occurs twice
        answers.add(check(issue("a", "", "MEANING", "b", "")));
        // fix that equals the quote, fix with a line break
        answers.add(check(issue("Taro", "", "MEANING", "Taro", "") + "," + issue("Hanako", "", "MEANING", "Hanako\\nHanako", "")));
        // items that are not objects, issues that is not an array
        answers.add("{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"issues\":[1,\"x\",null,[]]}");
        answers.add("{\"verdict\":\"ISSUES\",\"wrong_pair_evidence\":\"\",\"issues\":\"none\"}");
        // unknown verdict, verdict in lower case, verdict missing
        answers.add("{\"verdict\":\"MAYBE\",\"wrong_pair_evidence\":\"\",\"issues\":[]}");
        answers.add("{\"verdict\":\"pass\",\"issues\":[]}");
        answers.add("{\"issues\":[]}");
        // overlong values
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5000; i++) big.append('x');
        answers.add(check(issue(big.toString(), big.toString(), "MEANING", big.toString(), "")));
        for (String answer : answers) {
            for (Mode mode : new Mode[] {Mode.THOROUGH}) {
                EditorialApiFlow.Outcome outcome = run(mode, EDITED, answer, PASS_FALLBACK);
                String where = answer.length() > 80 ? answer.substring(0, 80) : answer;
                assertTrue(where + " -> " + outcome.state(), outcome.state() == RunState.FINAL_OK || outcome.state() == RunState.FINAL_NOTES);
                assertFalse(where, outcome.finalText().isEmpty());
                assertFalse(where, outcome.checkUnavailable() && !answer.contains("{") );
            }
        }
    }

    @Test public void editAnswersShapedLikeTheP6SlipsAreEitherTextOrOneRetryNeverAnException() {
        String[] answers = {
                EDITED,
                "<EDITED>" + DRAFT + "</EDITED>\n<NOTES>\n太郎《たろう》 | MEANING | furigana\n</NOTES>",
                EDITED + "<NOTES>không có cột</NOTES>",
                EDITED + "<NOTES>\n\n\n</NOTES>",
                EDITED + "<NOTES>a | b | c | d | e</NOTES>",
                "<EDITED>" + DRAFT + "</EDITED><EDITED>lần hai</EDITED>",
                "Chào bạn!\n" + EDITED,
                "<edited>chữ thường</edited>",
                "",
                "\n\n",
                "<EDITED></EDITED>",
        };
        for (String answer : answers) {
            EditorialApiFlow.Outcome outcome = run(Mode.QUICK, answer, PASS_FALLBACK);
            assertTrue(answer, outcome.calls() <= 2);
            assertTrue(answer, outcome.state() == RunState.FINAL_OK || outcome.state() == RunState.FINAL_NOTES
                    || outcome.state() == RunState.RETRY_REQUIRED);
            // a retry is answered with the same text, so a failing shape ends as RETRY_REQUIRED with the draft kept
            if (outcome.state() == RunState.RETRY_REQUIRED) assertEquals(DRAFT, outcome.finalText());
        }
    }

    @Test public void aGlossaryAndPronounHeavyInputNeverBreaksTheFlowEither() {
        List<EditInputs.GlossaryEntry> glossary = new ArrayList<>();
        for (int i = 0; i < 500; i++) glossary.add(new EditInputs.GlossaryEntry(i % 2 == 0 ? "太郎" : "花子", "T" + i, "name", "n"));
        EditorialApiFlow flow = new EditorialApiFlow(new EditInputs(RAW, DRAFT, "Vietnamese", glossary, ",,,\n\"\"\"\n太郎"),
                EditorialApiFlow.Config.of(Mode.QUICK));
        assertNotNull(flow.nextRequest());
        flow.accept(ok(EDITED));
        assertTrue(flow.done());
        assertTrue(flow.outcome().guards().has(EditGuards.Code.GLOSSARY_WARN));
    }
}
