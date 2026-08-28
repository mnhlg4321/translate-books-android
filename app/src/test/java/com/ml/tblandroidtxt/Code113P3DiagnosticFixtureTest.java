package com.ml.tblandroidtxt;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization only: records the known code113 P3 projection bug.
 *
 * The assertions deliberately describe the current bad projection. They are
 * not the D3 behavior contract and must be replaced/extended when D3 lands.
 */
public class Code113P3DiagnosticFixtureTest {
    private static final String RESOURCE = "fixtures/code113/p3-seven-column-pronoun.csv";

    @Test public void code113P3ProjectionIsThreeColumnsAndLegacyStillWorks() throws Exception {
        String raw = readResource(RESOURCE);
        String[] lines = raw.trim().split("\\r?\\n");
        assertEquals(2, lines.length);
        assertEquals(7, csvColumnCount(lines[0]));
        assertEquals(7, csvColumnCount(lines[1]));

        PromptContextBuilder.ParseReport parsed = PromptContextBuilder.validate("", raw);
        assertEquals(2, parsed.explicitPronouns.size());

        PromptContextBuilder.PronounRule header = parsed.explicitPronouns.get(0);
        PromptContextBuilder.PronounRule data = parsed.explicitPronouns.get(1);
        assertEquals("from → speaker: target", header.text);
        assertEquals("私 → Mercedes: Basil", data.text);
        assertEquals("私", data.sourceName);
        assertEquals("Mercedes", data.targetName);
        assertEquals("Basil", data.details);

        // The raw seven-column tail exists, but code113 does not project it.
        assertTrue(raw.contains("ta,ngươi,CH004:p052-p153,Fixture P3 thật của Mercedes"));
        assertFalse(data.text.contains("ta"));
        assertFalse(data.text.contains("ngươi"));
        assertFalse(data.text.contains("CH004:p052-p153"));
        assertFalse(data.text.contains("Fixture P3 thật của Mercedes"));
        assertFalse(hasPronounRuleField("self"));
        assertFalse(hasPronounRuleField("call"));
        assertFalse(hasPronounRuleField("scope"));
        assertFalse(hasPronounRuleField("note"));

        String legacy = "from,to,pronoun\nAlice,Bob,chị/em\n";
        PromptContextBuilder.ParseReport legacyParsed = PromptContextBuilder.validate("", legacy);
        assertEquals(1, legacyParsed.explicitPronouns.size());
        PromptContextBuilder.ContextBlock legacyMatch = PromptContextBuilder.build(
                "", legacy, "Alice gặp Bob.", new AppSettings());
        assertEquals(1, legacyMatch.pronounCount);
        assertTrue(legacyMatch.pronouns.contains("Alice → Bob: chị/em"));

        StringBuilder manyGlobalRules = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            manyGlobalRules.append("GLOBAL: diagnostic rule ").append(i).append('\n');
        }
        PromptContextBuilder.ContextBlock limited = PromptContextBuilder.build(
                "", manyGlobalRules.toString(), "unrelated text", new AppSettings());
        assertEquals(40, new AppSettings().pronounInjectLimit);
        assertEquals(40, limited.pronounCount);

        String diagnostic = "baseline.code113.expected.error=true\n"
                + "raw.header.column.count=" + csvColumnCount(lines[0]) + "\n"
                + "raw.data.column.count=" + csvColumnCount(lines[1]) + "\n"
                + "header.rule=" + header.text + "\n"
                + "data.rule=" + data.text + "\n"
                + "data.projected.from=" + data.sourceName + "\n"
                + "data.projected.speaker=" + data.targetName + "\n"
                + "data.projected.target=" + data.details + "\n"
                + "tail.self.call.scope.note=LOST\n"
                + "legacy3.rule=" + legacyParsed.explicitPronouns.get(0).text + "\n"
                + "legacy3.match.count=" + legacyMatch.pronounCount + "\n"
                + "default.pronoun.limit.observed=" + limited.pronounCount;
        System.out.println(diagnostic);
    }

    private static String readResource(String name) throws Exception {
        InputStream in = Code113P3DiagnosticFixtureTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(name, in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static int csvColumnCount(String line) {
        boolean quoted = false;
        int columns = 1;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') quoted = !quoted;
            else if (ch == ',' && !quoted) columns++;
        }
        return columns;
    }

    private static boolean hasPronounRuleField(String name) {
        try {
            PromptContextBuilder.PronounRule.class.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException absent) {
            return false;
        }
    }
}
