package com.ml.tblandroidtxt;

import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * D3 acceptance tests for the seven-field Pronoun runtime.
 *
 * Reflection is used only for the fields and overloads that D3 is expected to
 * add. That keeps the intentional pre-production red run compilable against
 * the code113 baseline while making the required runtime contract explicit.
 */
public class PronounSevenFieldRuntimeTest {
    private static final String HEADER = "from,speaker,target,self,call,scope,note";
    private static final String NOTE = "Fixture P3 thật của Mercedes";
    private static final String P3_ROW = "私,Mercedes,Basil,ta,ngươi,CH004:p052-p153," + NOTE;
    private static final String ARROW = "\u2192";

    @Test public void p3HeaderIsSkippedAndAllSevenFieldsAreProjected() {
        String raw = HEADER + "\n" + P3_ROW + "\n";
        assertEquals(7, csvColumnCount(raw.split("\\r?\\n")[0]));
        assertEquals(7, csvColumnCount(raw.split("\\r?\\n")[1]));

        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        assertEquals("P3 header must not become a rule", 1, report.explicitPronouns.size());
        PromptContextBuilder.PronounRule rule = report.explicitPronouns.get(0);
        assertEquals("私", fieldValue(rule, "from"));
        assertEquals("Mercedes", fieldValue(rule, "speaker"));
        assertEquals("Basil", fieldValue(rule, "target"));
        assertEquals("ta", fieldValue(rule, "self"));
        assertEquals("ngươi", fieldValue(rule, "call"));
        assertEquals("CH004:p052-p153", fieldValue(rule, "scope"));
        assertEquals(NOTE, fieldValue(rule, "note"));
        assertEquals("Mercedes " + ARROW + " Basil: ta/ngươi | " + NOTE, rule.text);
        assertFalse("P3 must not use speaker as the legacy target projection",
                "Mercedes".equals(fieldValue(rule, "targetName")));
        assertFalse("P3 target must not be projected as legacy details",
                "Basil".equals(fieldValue(rule, "details")));
    }

    @Test public void headerlessThreeAndSevenColumnRowsUseSeparateMappings() {
        String raw = "Alice,Bob,chị/em\n" + P3_ROW + "\n";
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        assertEquals(2, report.explicitPronouns.size());
        PromptContextBuilder.PronounRule legacy = report.explicitPronouns.get(0);
        assertEquals("Alice", legacy.sourceName);
        assertEquals("Bob", legacy.targetName);
        assertEquals("chị/em", legacy.details);
        PromptContextBuilder.PronounRule p3 = report.explicitPronouns.get(1);
        assertEquals("私", fieldValue(p3, "from"));
        assertEquals("Mercedes", fieldValue(p3, "speaker"));
        assertEquals("Basil", fieldValue(p3, "target"));
    }

    @Test public void p3MalformedRowsDoNotFallbackToLegacyAndOtherRowsSurvive() {
        String raw = HEADER + "\n"
                + "私,Mercedes,Basil,ta,ngươi,p000,bad scope\n"
                + "Alice,Bob,chị/em,unexpected\n"
                + "Carol,Carolyn,Caro,ta,ngươi,p050,valid\n";
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        assertEquals(2, report.explicitPronouns.size());
        assertTrue(report.malformedPronounRows.toString().contains("p000"));
        assertTrue(report.malformedPronounRows.toString().contains("expected 7 Pronoun columns"));
        assertEquals("Carol", fieldValue(report.explicitPronouns.get(1), "from"));
        assertEquals("Carolyn", fieldValue(report.explicitPronouns.get(1), "speaker"));
        assertEquals("Caro", fieldValue(report.explicitPronouns.get(1), "target"));
        assertFalse("A malformed seven-column row must not become a legacy rule",
                report.explicitPronouns.toString().contains("Mercedes"));

        PromptContextBuilder.ParseReport headerlessUnsupported = PromptContextBuilder.validate("",
                "Alice,Bob,chị/em,unexpected");
        assertTrue(headerlessUnsupported.explicitPronouns.isEmpty());
        assertTrue(headerlessUnsupported.malformedPronounRows.toString().contains("unsupported Pronoun column count"));

        PromptContextBuilder.ParseReport missingRequired = PromptContextBuilder.validate("",
                HEADER + "\n私,,Basil,ta,ngươi,*,missing speaker");
        assertTrue(missingRequired.explicitPronouns.isEmpty());
        assertTrue(missingRequired.malformedPronounRows.toString().contains("missing from/speaker/target"));
    }

    @Test public void bomUnicodeEmptyFieldsAndCsvQuotesSurvive() {
        String quote = String.valueOf((char) 34);
        String quotedNote = "T\u00ean, Mercedes " + quote + "quoted" + quote;
        String quotedCsvNote = quote + "T\u00ean, Mercedes " + quote + quote + "quoted" + quote + quote + quote;
        String raw = "\uFEFF" + HEADER + "\n"
                + "\u79c1,Mercedes,Basil,\"\",\"\",*," + quotedCsvNote + "\n";
        PromptContextBuilder.ParseReport p3 = PromptContextBuilder.validate("", raw);
        assertEquals(1, p3.explicitPronouns.size());
        PromptContextBuilder.PronounRule rule = p3.explicitPronouns.get(0);
        assertEquals("", fieldValue(rule, "self"));
        assertEquals("", fieldValue(rule, "call"));
        assertEquals("*", fieldValue(rule, "scope"));
        assertEquals(quotedNote, fieldValue(rule, "note"));

        PromptContextBuilder.ParseReport legacy = PromptContextBuilder.validate("",
                "from,to,pronoun\nAlice,Bob,\"chị,em\"\n");
        assertEquals(1, legacy.explicitPronouns.size());
        assertEquals("chị,em", legacy.explicitPronouns.get(0).details);
        assertEquals("Alice " + ARROW + " Bob: chị,em", legacy.explicitPronouns.get(0).text);

        PromptContextBuilder.ParseReport arrow = PromptContextBuilder.validate("",
                "Alice " + ARROW + " Bob: \"quoted\"\n");
        assertEquals(1, arrow.explicitPronouns.size());
        assertEquals("Alice " + ARROW + " Bob: \"quoted\"", arrow.explicitPronouns.get(0).text);
    }

    @Test public void multilineCsvIsMalformedAndNeverPartiallyInjected() {
        String raw = HEADER + "\n"
                + "私,Mercedes,Basil,ta,ngươi,*,\"line one\nline two\"";
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        assertTrue(report.explicitPronouns.isEmpty());
        assertTrue(report.malformedPronounRows.toString().contains("multiline CSV fields are not supported"));
        assertEquals(0, PromptContextBuilder.build("", raw, "私", new AppSettings()).pronounCount);
    }

    @Test public void scopeParserIsInclusiveAndFailClosed() {
        String raw = HEADER + "\n"
                + "A050,Speaker,Target,ta,ngươi,p050,one\n"
                + "A052,Speaker,Target,ta,ngươi,p052-p153,two\n"
                + "A100,Speaker,Target,ta,ngươi,CH004:p052-p153,three\n"
                + "A200,Speaker,Target,ta,ngươi,*,four\n"
                + "A201,Speaker,Target,ta,ngươi,,five\n"
                + "Bad,Speaker,Target,ta,ngươi,p000,bad\n"
                + "Reverse,Speaker,Target,ta,ngươi,p153-p052,bad\n"
                + "Prefix,Speaker,Target,ta,ngươi,XX004:p050,bad\n";
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        assertEquals(8, report.explicitPronouns.size());
        assertScope(findRule(report, "A050"), 50, 50, true, false);
        assertScope(findRule(report, "A052"), 52, 153, true, false);
        assertScope(findRule(report, "A100"), 52, 153, true, false);
        assertScope(findRule(report, "A200"), -1, -1, true, true);
        assertScope(findRule(report, "A201"), -1, -1, true, true);
        assertFalse(booleanField(findRule(report, "Bad"), "scopeValid"));
        assertFalse(booleanField(findRule(report, "Reverse"), "scopeValid"));
        assertFalse(booleanField(findRule(report, "Prefix"), "scopeValid"));
    }

    @Test public void matchingRequiresFromCueAndInclusiveParagraphOverlap() {
        String p050 = HEADER + "\n私,Mercedes,Basil,ta,ngươi,p050,note";
        assertEquals(1, buildAt(p050, "私 が話した", 50, 50).pronounCount);
        assertEquals(1, buildAt(p050, "私 が話した", 49, 50).pronounCount);
        assertEquals(0, buildAt(p050, "私 が話した", 51, 51).pronounCount);
        assertEquals(0, buildAt(p050, "Mercedes Basil", 50, 50).pronounCount);
        assertEquals(0, buildAt(p050, "note", 50, 50).pronounCount);

        String chapterWide = HEADER + "\n私,Mercedes,Basil,ta,ngươi,*,wide";
        assertEquals(1, buildAt(chapterWide, "私", 12, 12).pronounCount);
        assertEquals(0, buildAt(chapterWide, "Mercedes Basil", 12, 12).pronounCount);

        String unknownRange = HEADER + "\n私,Mercedes,Basil,ta,ngươi,p050,scoped";
        assertEquals(0, PromptContextBuilder.build("", unknownRange, "私", new AppSettings()).pronounCount);

        String chapterRange = HEADER + "\n私,Mercedes,Basil,ta,ngươi,CH004:p052-p153,chapter";
        assertEquals(1, buildAt(chapterRange, "私", 153, 153).pronounCount);
        assertEquals(0, buildAt(chapterRange, "私", 154, 154).pronounCount);
    }

    @Test public void boundedRuleContextCanSupplyFromCueButNotSpeakerOrTarget() {
        String p3 = HEADER + "\n私,Mercedes,Basil,ta,ngươi,*,context";
        assertEquals(1, buildWithContextAt(p3, "unrelated", "私 appears nearby", 20, 20).pronounCount);
        assertEquals(0, buildWithContextAt(p3, "unrelated", "Mercedes Basil", 20, 20).pronounCount);
    }

    @Test public void unsupportedPronounHeaderDoesNotTriggerColumnCountInference() {
        String raw = "from,speaker,target,self,call,scope,note,extra\n"
                + P3_ROW + ",ignored\n";
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate("", raw);
        assertTrue(report.explicitPronouns.isEmpty());
        assertTrue(report.malformedPronounRows.toString().contains("unsupported Pronoun header"));
    }

    @Test public void p3CompilerUsesSemanticPairAndOmitsOnlyBlankPronouns() {
        String both = HEADER + "\n私,Mercedes,Basil,ta,ngươi,*,ghi chú";
        String bothLine = "Mercedes " + ARROW + " Basil: ta/ngươi | ghi chú";
        assertExactLine(buildAt(both, "私", 1, 1), bothLine);

        String selfOnly = HEADER + "\n私,Mercedes,Basil,ta,,*,";
        assertExactLine(buildAt(selfOnly, "私", 1, 1), "Mercedes " + ARROW + " Basil: ta/(omit)");

        String callOnly = HEADER + "\n私,Mercedes,Basil,,ngươi,*,";
        assertExactLine(buildAt(callOnly, "私", 1, 1), "Mercedes " + ARROW + " Basil: (omit)/ngươi");

        String none = HEADER + "\n私,Mercedes,Basil,,,*,ghi chú";
        assertExactLine(buildAt(none, "私", 1, 1), "Mercedes " + ARROW + " Basil: omit pronouns | ghi chú");
        assertFalse(buildAt(none, "私", 1, 1).pronouns.contains("*"));
    }

    @Test public void p3DedupeIgnoresScopeButKeepsPayloadAndLimitForty() {
        String duplicate = HEADER + "\n"
                + "私,Mercedes,Basil,ta,ngươi,p050,same\n"
                + "私,Mercedes,Basil,ta,ngươi,p051,same\n";
        PromptContextBuilder.ContextBlock one = buildAt(duplicate, "私", 50, 51);
        assertEquals(1, one.pronounCount);

        StringBuilder many = new StringBuilder(HEADER).append('\n');
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < 45; i++) {
            String from = "Name" + i;
            many.append(from).append(",Speaker").append(i).append(",Target").append(i)
                    .append(",ta,ngươi,*,note").append(i).append('\n');
            if (i > 0) chunk.append(' ');
            chunk.append(from);
        }
        AppSettings settings = new AppSettings();
        assertEquals(40, settings.pronounInjectLimit);
        assertEquals(40, PromptContextBuilder.build("", many.toString(), chunk.toString(), settings).pronounCount);
    }

    @Test public void chunkParagraphRangesAreDeterministicAndDoNotChangeIdentity() throws Exception {
        String source = "one\n\nparagraph\n\nthree";
        Chunk first = new Chunk(0, 0, 8, "", source.substring(0, 8), "", "");
        Chunk second = new Chunk(1, 8, source.length(), "", source.substring(8), "", "");
        String firstStable = first.stableId;
        String firstHash = first.sourceHash;
        ArrayList<Chunk> chunks = new ArrayList<>();
        chunks.add(first);
        chunks.add(second);
        assignParagraphRanges(chunks);
        assertEquals(1, intField(first, "paragraphStart"));
        assertEquals(2, intField(first, "paragraphEnd"));
        assertEquals(2, intField(second, "paragraphStart"));
        assertEquals(3, intField(second, "paragraphEnd"));
        assertEquals(firstStable, first.stableId);
        assertEquals(firstHash, first.sourceHash);
        assignParagraphRanges(chunks);
        assertEquals(1, intField(first, "paragraphStart"));
        assertEquals(3, intField(second, "paragraphEnd"));

        AppSettings settings = new AppSettings();
        settings.chunkMode = "char";
        settings.maxCharsPerChunk = 7;
        settings.contextOverlapEnabled = false;
        List<Chunk> generated = Chunker.chunkText("Heading\r\n\r\nline one\r\nline two\r\n\r\nline three", settings);
        assertTrue(generated.size() > 1);
        for (Chunk c : generated) {
            assertTrue(intField(c, "paragraphStart") >= 1);
            assertTrue(intField(c, "paragraphEnd") >= intField(c, "paragraphStart"));
        }
    }

    @Test public void settingsPromptPlanAndEstimatorUseTheSameP3Snapshot() throws Exception {
        String p3 = HEADER + "\n私,Mercedes,Basil,ta,ngươi,*,Tên Mercedes";
        AppSettings settings = new AppSettings();
        settings.pronounText = p3;
        AppSettings restored = SettingsStore.fromJson(SettingsStore.toJson(settings));
        assertEquals(p3, restored.pronounText);

        Chunk chunk = new Chunk(0, "", "私", "");
        setIntField(chunk, "paragraphStart", 1);
        setIntField(chunk, "paragraphEnd", 1);
        PromptPlan plan = PromptPlan.translation(chunk, "", restored);
        String line = "Mercedes " + ARROW + " Basil: ta/ngươi | Tên Mercedes";
        assertTrue(plan.pronoun.contains(line));
        assertEquals(1, plan.pronounLockCount);
        assertEquals(1, count(plan.prompt.system, line));

        AppSettings full = restored.copy();
        full.optimizationPreset = "full";
        PromptPlan fullPlan = PromptPlan.forTranslation(chunk, "", full);
        assertTrue(fullPlan.prompt.system.contains(line));
        assertTrue(previewAt(p3, "私", "私", 1, 1).contains(line));

        Chunk contextOnly = new Chunk(0, "私", "unrelated", "");
        setIntField(contextOnly, "paragraphStart", 1);
        setIntField(contextOnly, "paragraphEnd", 1);
        PromptPlan fullContextPlan = PromptPlan.forTranslation(contextOnly, "", full);
        assertTrue(fullContextPlan.prompt.system.contains(line));
        assertTrue(fullContextPlan.pronoun.contains(line));

        CostEstimator.Estimate withNote = CostEstimator.estimatePreparedChunks(
                singleton(chunk), restored);
        AppSettings unmatched = restored.copy();
        unmatched.pronounText = HEADER + "\nOther,Mercedes,Basil,ta,ngươi,*,long unmatched note";
        CostEstimator.Estimate withoutMatch = CostEstimator.estimatePreparedChunks(
                singleton(chunk), unmatched);
        assertTrue(withNote.totalTokensHigh > withoutMatch.totalTokensHigh);
        assertFalse(plan.prompt.system.contains("from,speaker,target"));
        assertFalse(plan.prompt.system.contains("scope"));
    }

    @Test public void p3NoteIsNotASeparateLegacyOrGlossaryCue() {
        String p3 = HEADER + "\n私,Mercedes,Basil,ta,ngươi,*,only note cue";
        PromptContextBuilder.ContextBlock noteOnly = PromptContextBuilder.build("", p3, "only note cue", new AppSettings());
        assertEquals(0, noteOnly.pronounCount);

        String glossary = "source,target,category,note\nクロ,Kuro,character,pronoun ta/ngươi";
        PromptContextBuilder.ContextBlock noP3 = PromptContextBuilder.build(glossary, p3, "クロ", new AppSettings());
        assertEquals(0, noP3.pronounCount);
        assertEquals(1, noP3.glossaryCount);
    }

    @Test public void profileJsonAndJobSettingsKeepRawP3Text() throws Exception {
        PronounStore.Profile profile = new PronounStore.Profile();
        profile.id = "p3-profile";
        profile.name = "Mercedes P3";
        profile.uri = "content://p3";
        profile.text = HEADER + "\n" + P3_ROW;

        Method toJson = PronounStore.class.getDeclaredMethod("toJson", PronounStore.Profile.class);
        toJson.setAccessible(true);
        JSONObject persisted = (JSONObject) toJson.invoke(null, profile);
        assertEquals(profile.text, persisted.getString("text"));

        Method fromJson = PronounStore.class.getDeclaredMethod("fromJson", JSONObject.class);
        fromJson.setAccessible(true);
        PronounStore.Profile restored = (PronounStore.Profile) fromJson.invoke(null, persisted);
        assertEquals(profile.id, restored.id);
        assertEquals(profile.text, restored.text);
        assertEquals(1, restored.count());

        JSONObject legacy = new JSONObject().put("id", "legacy").put("text", "Alice,Bob,chị/em");
        PronounStore.Profile legacyRestored = (PronounStore.Profile) fromJson.invoke(null, legacy);
        assertEquals("Alice,Bob,chị/em", legacyRestored.text);
        assertEquals(1, legacyRestored.count());

        AppSettings job = new AppSettings();
        job.selectedPronounId = profile.id;
        job.pronounText = profile.text;
        AppSettings jobRestored = SettingsStore.fromJson(SettingsStore.toJson(job));
        assertEquals(profile.id, jobRestored.selectedPronounId);
        assertEquals(profile.text, jobRestored.pronounText);
    }

    @Test public void paragraphAssignmentRejectsAnOffsetGapClearly() {
        ArrayList<Chunk> chunks = new ArrayList<>();
        chunks.add(new Chunk(0, 0, 3, "", "one", "", ""));
        chunks.add(new Chunk(1, 4, 7, "", "two", "", ""));
        try {
            Chunker.assignParagraphRanges(chunks);
            fail("Expected a discontinuous chunk list to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("continuous") || expected.getMessage().contains("gap"));
        }
    }

    @Test public void contextOverflowSplitReannotatesWithoutChangingChildIdentity() {
        AppSettings settings = new AppSettings();
        settings.chunkMode = "char";
        settings.maxCharsPerChunk = 8;
        settings.contextOverlapEnabled = false;
        String source = "私 first line\n\n私 second line";
        Chunk parent = new Chunk(4, 0, source.length(), "", source, "", "");
        setIntField(parent, "paragraphStart", 1);
        setIntField(parent, "paragraphEnd", 2);
        List<Chunk> children = Chunker.splitForContextOverflow(parent, settings);
        assertTrue(children.size() >= 2);
        int cursor = 0;
        StringBuilder rebuilt = new StringBuilder();
        for (Chunk child : children) {
            assertEquals(cursor, child.startOffset);
            assertTrue(child.endOffset > child.startOffset);
            assertEquals(24, child.stableId.length());
            assertEquals(HashUtil.sha256(child.mainContent), child.sourceHash);
            assertTrue(intField(child, "paragraphStart") >= 1);
            assertTrue(intField(child, "paragraphEnd") >= intField(child, "paragraphStart"));
            rebuilt.append(child.mainContent);
            cursor = child.endOffset;
        }
        assertEquals(source, rebuilt.toString());
        assertEquals(source.length(), cursor);
    }

    private static PromptContextBuilder.ContextBlock buildAt(String pronouns, String chunk, int start, int end) {
        try {
            Method method = PromptContextBuilder.class.getMethod("build", String.class, String.class,
                    String.class, int.class, int.class, AppSettings.class);
            return (PromptContextBuilder.ContextBlock) method.invoke(null, "", pronouns, chunk, start, end, new AppSettings());
        } catch (Exception e) {
            fail("Missing scoped PromptContextBuilder.build overload: " + rootMessage(e));
            return new PromptContextBuilder.ContextBlock();
        }
    }

    private static PromptContextBuilder.ContextBlock buildWithContextAt(String pronouns, String chunk,
                                                                         String ruleContext, int start, int end) {
        try {
            Method method = PromptContextBuilder.class.getMethod("buildWithRuleContext", String.class,
                    String.class, String.class, String.class, int.class, int.class, AppSettings.class);
            return (PromptContextBuilder.ContextBlock) method.invoke(null, "", pronouns, chunk, ruleContext,
                    start, end, new AppSettings());
        } catch (Exception e) {
            fail("Missing scoped PromptContextBuilder.buildWithRuleContext overload: " + rootMessage(e));
            return new PromptContextBuilder.ContextBlock();
        }
    }

    private static String previewAt(String pronouns, String chunk, String ruleContext, int start, int end) {
        try {
            Method method = PromptContextBuilder.class.getMethod("preview", String.class, String.class,
                    String.class, String.class, int.class, int.class, AppSettings.class);
            return (String) method.invoke(null, "", pronouns, chunk, ruleContext, start, end, new AppSettings());
        } catch (Exception e) {
            fail("Missing scoped PromptContextBuilder.preview overload: " + rootMessage(e));
            return "";
        }
    }

    private static void assertExactLine(PromptContextBuilder.ContextBlock block, String expected) {
        assertEquals(1, block.pronounCount);
        assertEquals(expected + "\n", block.pronouns);
        assertEquals(1, count(block.pronouns, expected));
    }

    private static void assignParagraphRanges(List<Chunk> chunks) throws Exception {
        try {
            Method method = Chunker.class.getMethod("assignParagraphRanges", List.class);
            method.invoke(null, chunks);
        } catch (Exception e) {
            fail("Missing Chunker.assignParagraphRanges(List<Chunk>): " + rootMessage(e));
        }
    }

    private static PromptContextBuilder.PronounRule findRule(PromptContextBuilder.ParseReport report, String from) {
        for (PromptContextBuilder.PronounRule rule : report.explicitPronouns) {
            if (from.equals(fieldValue(rule, "from"))) return rule;
        }
        fail("Missing parsed rule for " + from);
        return report.explicitPronouns.get(0);
    }

    private static void assertScope(PromptContextBuilder.PronounRule rule, int start, int end,
                                    boolean valid, boolean chapterWide) {
        assertEquals(start, intField(rule, "scopeStart"));
        assertEquals(end, intField(rule, "scopeEnd"));
        assertEquals(valid, booleanField(rule, "scopeValid"));
        assertEquals(chapterWide, booleanField(rule, "chapterWide"));
    }

    private static String fieldValue(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(object);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception missing) {
            fail("Missing PronounRule field " + name);
            return "";
        }
    }

    private static int intField(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(object);
        } catch (Exception missing) {
            fail("Missing integer field " + name + " on " + object.getClass().getName());
            return -1;
        }
    }

    private static boolean booleanField(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getBoolean(object);
        } catch (Exception missing) {
            fail("Missing boolean field " + name + " on " + object.getClass().getName());
            return false;
        }
    }

    private static void setIntField(Object object, String name, int value) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(object, value);
        } catch (Exception missing) {
            fail("Missing integer field " + name + " on " + object.getClass().getName());
        }
    }

    private static int csvColumnCount(String line) {
        boolean quoted = false;
        int columns = 1;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') i++;
                else quoted = !quoted;
            } else if (ch == ',' && !quoted) columns++;
        }
        return columns;
    }

    private static List<Chunk> singleton(Chunk chunk) {
        ArrayList<Chunk> result = new ArrayList<>();
        result.add(chunk);
        return result;
    }

    private static int count(String text, String needle) {
        int result = 0;
        int from = 0;
        while (needle != null && !needle.isEmpty()) {
            int at = text.indexOf(needle, from);
            if (at < 0) break;
            result++;
            from = at + needle.length();
        }
        return result;
    }

    private static String rootMessage(Exception e) {
        Throwable cause = e instanceof InvocationTargetException && ((InvocationTargetException) e).getCause() != null
                ? ((InvocationTargetException) e).getCause() : e;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
