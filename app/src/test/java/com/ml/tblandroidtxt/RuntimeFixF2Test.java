package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

/** F2: a translated response must not silently retain a full Japanese source line. */
public class RuntimeFixF2Test {
    @Test public void syntheticAggregateFixtureRejectsTwoRetainedJapaneseLines() {
        String output = aggregateOutput();
        ResponseValidator.Result result = strict(output, englishSource(62), Collections.emptyList());
        assertFalse(result.accepted);
        assertTrue(result.errors.contains("RETAINED_SOURCE_LINE"));
        assertEquals(2, result.retainedSourceLineCount);
        assertEquals(61, result.firstRetainedSourceLine);
        assertTrue(result.retainedSourceLineRatio >= 0.40d);
        assertFalse(result.readable().contains("これは"));
    }

    @Test public void oldOverloadRemainsOptInOffForManualReplace() {
        ResponseValidator.Result result = ResponseValidator.validate(
                "dịch", "Bản dịch vẫn còn これは未翻訳の台詞です。", "English source", "",
                "stop", 20, 200, false);
        assertTrue(result.accepted);
        assertFalse(result.errors.contains("RETAINED_SOURCE_LINE"));
    }

    @Test public void rubyAndGlossaryLocksAreNotCountedAsRetainedSource() {
        ResponseValidator.Result ruby = strict(
                "Câu đã dịch 漢字《かな》 xong.", "English source", Collections.emptyList());
        assertTrue(ruby.accepted);

        ResponseValidator.Result locked = strict(
                "Tên riêng 新宿御苑公園 được giữ nguyên.", "English source",
                Collections.singletonList("新宿御苑公園"));
        assertTrue(locked.accepted);
    }

    @Test public void shortKatakanaNameIsNotAWholeRetainedSourceLine() {
        ResponseValidator.Result result = strict(
                "Bản dịch của カイ đã hoàn tất.", "English source", Collections.emptyList());
        assertTrue(result.accepted);
    }

    @Test public void recoveryUsesTheSameStrictOverloadAndPromptCarriesGlossaryTargets() {
        AppSettings settings = new AppSettings();
        settings.glossaryText = "source,target,category,note\nAlice,新宿御苑公園,term,\n";
        Chunk chunk = new Chunk(2, "", "Alice vẫn xuất hiện.", "");
        PromptPlan plan = PromptPlan.forTranslation(chunk, "", settings);
        assertTrue(plan.glossaryTargets.contains("新宿御苑公園"));

        String retained = "Bản dịch vẫn còn これは未翻訳の台詞です。";
        ResponseValidator.Result branch = strict(retained, chunk.mainContent, plan.glossaryTargets);
        ResponseValidator.Result recovery = strict(retained, chunk.mainContent, plan.glossaryTargets);
        assertFalse(branch.accepted);
        assertFalse(recovery.accepted);
        assertTrue(branch.errors.contains("RETAINED_SOURCE_LINE"));
        assertTrue(recovery.errors.contains("RETAINED_SOURCE_LINE"));
    }

    private static ResponseValidator.Result strict(String output, String source, java.util.Collection<String> targets) {
        return ResponseValidator.validate(output, output, source, "", "stop", 20, 200, targets, true);
    }

    private static String aggregateOutput() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 60; i++) b.append("Dòng tiếng Việt đã được dịch đầy đủ số ").append(i).append(".\n");
        b.append("これはまだ翻訳されていない台詞です。\n");
        b.append("もう一つの日本語の台詞が残っています。\n");
        return b.toString();
    }

    private static String englishSource(int lines) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lines; i++) b.append("The translated source line number ").append(i).append(" is complete.\n");
        return b.toString();
    }
}
