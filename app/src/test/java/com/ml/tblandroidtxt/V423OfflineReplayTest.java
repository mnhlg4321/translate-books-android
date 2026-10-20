package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v4.23 J6 offline replay (USD 0): the 58 answers the phone stored for UNCLOSED_BRACKET_OR_DIALOGUE rejections, run through the new validator.
 * The export lives outside Git (it holds book text) at the path of the environment variable TBL_V423_REPLAY or the private diagnosis folder;
 * without it the test is skipped. Only counts are printed.
 */
public class V423OfflineReplayTest {
    private static File replayFile() {
        String env = System.getenv("TBL_V423_REPLAY");
        File f = new File(env == null || env.isEmpty() ? "D:/P5E-private/v423-diagnosis-20261020/replay58.jsonl" : env);
        return f;
    }

    @Test public void theStoredRejectionsAreNamedCorrectlyAndNothingShortOfLinesIsAccepted() throws Exception {
        File f = replayFile();
        assumeTrue("replay export not present: " + f, f.isFile());
        int total = 0, accepted = 0, acceptedAfterRepair = 0, truncated = 0, missingLines = 0, stillBracket = 0, other = 0, acceptedShort = 0;
        Map<String, Integer> codes = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            for (String line; (line = r.readLine()) != null; ) {
                if (line.trim().isEmpty()) continue;
                JSONObject o = new JSONObject(line);
                total++;
                String source = o.getString("source");
                String content = o.getString("content");
                String extracted;
                try { extracted = PromptBuilder.extractTranslationStrict(content); } catch (Exception e) { extracted = content; }
                ResponseValidator.Repaired repaired = ResponseValidator.repairQuoteStyle(extracted, source);
                ResponseValidator.Result v = ResponseValidator.validate(content, repaired.text, source, "", o.getString("finish"), o.getInt("outTokens"), o.getInt("maxOut"), false);
                for (String e : v.errors) codes.merge(e, 1, Integer::sum);
                if (v.accepted) {
                    accepted++;
                    if (repaired.changed) acceptedAfterRepair++;
                    if (v.lineRatio >= 0 && v.lineRatio < ResponseValidator.MIN_LINE_RATIO) acceptedShort++;
                } else if (v.truncated) truncated++;
                else if (v.errors.contains("OUTPUT_MISSING_LINES")) missingLines++;
                else if (v.errors.contains("UNCLOSED_BRACKET_OR_DIALOGUE")) stillBracket++;
                else other++;
            }
        }
        System.out.println("V423_REPLAY total=" + total + " accepted=" + accepted + " (after quote repair " + acceptedAfterRepair + ") truncated=" + truncated
                + " missingLines=" + missingLines + " stillBracket=" + stillBracket + " other=" + other + " codes=" + codes);
        assertEquals(58, total);
        assertEquals("nothing short of lines is accepted", 0, acceptedShort);
        assertTrue("at least 7 mixed-quote answers are accepted after the repair", acceptedAfterRepair >= 7);
        assertTrue("answers that finished with length are named as cut", truncated >= 19);
    }
}
