package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * D2 acceptance tests for the four-field Glossary runtime.
 *
 * These tests deliberately cover the five-column import shape as well: the
 * fifth priority column is input metadata and must not enter runtime state.
 */
public class GlossaryFourFieldRuntimeTest {
    private static final String KURO = "\u30af\u30ed";
    private static final String NOTE = "T\u00ean Mercedes \u0111\u1eb7t cho nh\u00e2n v\u1eadt n\u00e0y";

    @Test public void parsesLegacyTwoThreeFourAndFiveColumns() {
        PromptContextBuilder.ParseReport two = PromptContextBuilder.validate(
                KURO + ",Kuro", "");
        assertEquals(1, two.glossaryTerms.size());
        assertEntry(two.glossaryTerms.get(0), KURO, "Kuro", "term", "", "");

        PromptContextBuilder.ParseReport three = PromptContextBuilder.validate(
                "source,target,category\n" + KURO + ",Kuro,character", "");
        assertEquals(1, three.glossaryTerms.size());
        assertEntry(three.glossaryTerms.get(0), KURO, "Kuro", "character", "", "");

        PromptContextBuilder.ParseReport four = PromptContextBuilder.validate(
                "source,target,category,note\n" + KURO + ",Kuro,character," + NOTE, "");
        assertEquals(1, four.glossaryTerms.size());
        assertEntry(four.glossaryTerms.get(0), KURO, "Kuro", "character", NOTE, "");

        PromptContextBuilder.ParseReport fourWithoutHeader = PromptContextBuilder.validate(
                KURO + ",Kuro,character," + NOTE, "");
        assertEquals(1, fourWithoutHeader.glossaryTerms.size());
        assertEntry(fourWithoutHeader.glossaryTerms.get(0), KURO, "Kuro", "character", NOTE, "");

        PromptContextBuilder.ParseReport five = PromptContextBuilder.validate(
                "source,target,category,note,priority\n" + KURO + ",Kuro,character," + NOTE + ",high", "");
        assertEquals(1, five.glossaryTerms.size());
        assertEntry(five.glossaryTerms.get(0), KURO, "Kuro", "character", NOTE, "");
    }

    @Test public void acceptsEmptyNoteBomUnicodeAndQuotedComma() {
        PromptContextBuilder.ParseReport empty = PromptContextBuilder.validate(
                "source,target,category,note\n" + KURO + ",Kuro,character,", "");
        assertEquals(1, empty.glossaryTerms.size());
        assertEquals("", fieldValue(empty.glossaryTerms.get(0), "note"));

        String quotedNote = "T\u00ean, Mercedes \u0111\u1eb7t cho nh\u00e2n v\u1eadt n\u00e0y";
        PromptContextBuilder.ParseReport bom = PromptContextBuilder.validate(
                "\uFEFFsource,target,category,note\n" + KURO + ",Kuro,character,\"" + quotedNote + "\"", "");
        assertEquals(1, bom.glossaryTerms.size());
        assertEquals(KURO, bom.glossaryTerms.get(0).source);
        assertEquals(quotedNote, fieldValue(bom.glossaryTerms.get(0), "note"));

        String escapedQuote = "T\u00ean \"Mercedes\" \u0111\u1eb7t cho nh\u00e2n v\u1eadt n\u00e0y";
        PromptContextBuilder.ParseReport escaped = PromptContextBuilder.validate(
                "source,target,category,note\n" + KURO + ",Kuro,character,\"T\u00ean \"\"Mercedes\"\" \u0111\u1eb7t cho nh\u00e2n v\u1eadt n\u00e0y\"", "");
        assertEquals(1, escaped.glossaryTerms.size());
        assertEquals(escapedQuote, fieldValue(escaped.glossaryTerms.get(0), "note"));
    }

    @Test public void rejectsUnsupportedMultilineCsvNoteWithoutPartialInjection() {
        String multiline = "source,target,category,note\n"
                + KURO + ",Kuro,character,\"line one\nline two\"";
        PromptContextBuilder.ParseReport parsed = PromptContextBuilder.validate(multiline, "");
        assertTrue(parsed.glossaryTerms.isEmpty());
        assertTrue(parsed.malformedGlossaryRows.toString().contains("multiline CSV fields are not supported"));
    }

    @Test public void explicitAliasesHeaderRetainsOnlyLegacyAliasCompatibility() {
        String text = "source,target,category,aliases\nHero,Anh h\u00f9ng,term,Yuusha";
        PromptContextBuilder.ParseReport parsed = PromptContextBuilder.validate(text, "");
        assertEquals(1, parsed.glossaryTerms.size());
        PromptContextBuilder.TermEntry entry = parsed.glossaryTerms.get(0);
        assertEquals("", fieldValue(entry, "note"));
        assertEquals("Yuusha", entry.aliases);

        PromptContextBuilder.ContextBlock block = PromptContextBuilder.build(text, "", "Yuusha", new AppSettings());
        assertEquals(1, block.glossaryCount);
        assertTrue(block.glossary.contains("Hero => Anh h\u00f9ng [term]"));
        assertFalse(block.glossary.contains("Yuusha"));
    }

    @Test public void adapterAndJsonRoundTripKeepNoteButNeverPriority() throws Exception {
        List<GlossaryStore.Term> adapted = GlossaryStore.parseTerms(
                "fixture.csv",
                "source,target,category,note,priority\n" + KURO + ",Kuro,character," + NOTE + ",high");
        assertEquals(1, adapted.size());
        GlossaryStore.Term term = adapted.get(0);
        assertEquals(KURO, term.source);
        assertEquals("Kuro", term.target);
        assertEquals("character", term.category);
        assertEquals(NOTE, fieldValue(term, "note"));
        assertFalse(hasField(PromptContextBuilder.TermEntry.class, "priority"));
        assertFalse(hasField(GlossaryStore.Term.class, "priority"));
        assertFalse(hasField(GlossaryStore.Term.class, "aliases"));

        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.id = "d2-profile";
        glossary.name = "Mercedes";
        glossary.terms.add(term);
        JSONObject persisted = invokeToJson(glossary);
        JSONObject persistedTerm = persisted.getJSONArray("terms").getJSONObject(0);
        assertEquals(NOTE, persistedTerm.getString("note"));
        assertFalse(persistedTerm.has("priority"));
        assertFalse(persistedTerm.has("aliases"));

        GlossaryStore.Glossary reloaded = invokeFromJson(persisted);
        assertEquals("d2-profile", reloaded.id);
        assertEquals(NOTE, fieldValue(reloaded.terms.get(0), "note"));

        AppSettings snapshot = new AppSettings();
        snapshot.selectedGlossaryId = glossary.id;
        snapshot.glossaryText = GlossaryStore.toPromptText(glossary);
        AppSettings restoredSnapshot = SettingsStore.fromJson(SettingsStore.toJson(snapshot));
        assertEquals(glossary.id, restoredSnapshot.selectedGlossaryId);
        assertEquals(snapshot.glossaryText, restoredSnapshot.glossaryText);
        assertTrue(restoredSnapshot.glossaryText.contains(NOTE));
        assertFalse(restoredSnapshot.glossaryText.contains("priority"));

        JSONObject legacy = new JSONObject();
        legacy.put("id", "legacy-profile");
        legacy.put("terms", new JSONArray().put(new JSONObject()
                .put("source", KURO).put("target", "Kuro").put("category", "character")));
        GlossaryStore.Glossary legacyLoaded = invokeFromJson(legacy);
        assertEquals("", fieldValue(legacyLoaded.terms.get(0), "note"));

        GlossaryStore.Term threeField = new GlossaryStore.Term(KURO, "Kuro", "character");
        assertEquals("", fieldValue(threeField, "note"));
        GlossaryStore.Term nullNote = newTermWithNote(KURO, "Kuro", "character", null);
        assertEquals("", fieldValue(nullNote, "note"));
    }

    @Test public void matchedNoteIsCompiledOnceAndInternalPromptRoundTripsIt() {
        String five = "source,target,category,note,priority\n" + KURO + ",Kuro,character," + NOTE + ",high";
        AppSettings settings = new AppSettings();
        PromptContextBuilder.ContextBlock matched = PromptContextBuilder.build(five, "", KURO, settings);
        String expected = KURO + " => Kuro [character] | " + NOTE;
        assertEquals(1, matched.glossaryCount);
        assertEquals(expected + "\n", matched.glossary);
        assertEquals(1, count(matched.glossary, NOTE));
        assertFalse(matched.glossary.contains("priority"));
        assertFalse(matched.glossary.contains("source,target"));

        settings.glossaryText = five;
        PromptPlan plan = PromptPlan.translation(new Chunk(0, "", KURO, ""), "", settings);
        assertTrue(plan.prompt.system.contains(expected));
        assertTrue(PromptContextBuilder.preview(five, "", KURO, settings).contains(expected));

        PromptContextBuilder.ContextBlock unmatched = PromptContextBuilder.build(five, "", NOTE, settings);
        assertEquals(0, unmatched.glossaryCount);
        assertFalse(unmatched.glossary.contains(NOTE));

        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.terms.add(newTermWithNote(KURO, "Kuro", "character", "left | right"));
        String compact = GlossaryStore.toPromptText(glossary);
        PromptContextBuilder.ContextBlock roundTrip = PromptContextBuilder.build(compact, "", KURO, settings);
        assertEquals(KURO + " => Kuro [character] | left | right\n", roundTrip.glossary);
        assertEquals(1, count(roundTrip.glossary, "left | right"));
    }

    @Test public void noteIsNotMatchCueOrPronounRuleAndEmptyNoteKeepsOldFormat() {
        String text = "source,target,category,note\n" + KURO + ",Kuro,character," + NOTE;
        PromptContextBuilder.MatchReport noteOnly = PromptContextBuilder.match(text, "", NOTE, new AppSettings());
        assertEquals(0, noteOnly.block.glossaryCount);
        assertEquals(0, noteOnly.block.pronounCount);

        String empty = "source,target,category,note\n" + KURO + ",Kuro,character,";
        PromptContextBuilder.ContextBlock block = PromptContextBuilder.build(empty, "", KURO, new AppSettings());
        assertEquals(KURO + " => Kuro [character]\n", block.glossary);
        assertFalse(block.glossary.contains(" |"));
    }

    @Test public void priorityDoesNotRankOrChangeDedupeAndGlossaryLimitStaysEighty() {
        String json = "[{\"source\":\"g0\",\"target\":\"t0\",\"category\":\"global\",\"priority\":0},"
                + "{\"source\":\"g1\",\"target\":\"t1\",\"category\":\"global\",\"priority\":100}]";
        PromptContextBuilder.ContextBlock ranked = PromptContextBuilder.build(json, "", "unrelated", new AppSettings());
        assertTrue(ranked.glossary.indexOf("g0 =>") < ranked.glossary.indexOf("g1 =>"));

        String duplicate = "source,target,category,note,priority\n"
                + KURO + ",Kuro,character,first,high\n"
                + KURO + ",Kuro,term,second,low\n";
        List<GlossaryStore.Term> terms = GlossaryStore.parseTerms("duplicate.csv", duplicate);
        assertEquals(1, terms.size());
        assertEquals("first", fieldValue(terms.get(0), "note"));

        StringBuilder many = new StringBuilder("source,target,category,note,priority\n");
        for (int i = 0; i < 85; i++) {
            many.append("g").append(i).append(",t").append(i).append(",global,note,high\n");
        }
        PromptContextBuilder.ContextBlock limited = PromptContextBuilder.build(
                many.toString(), "", "unrelated", new AppSettings());
        assertEquals(80, new AppSettings().glossaryInjectLimit);
        assertEquals(80, limited.glossaryCount);
        assertEquals(40, new AppSettings().pronounInjectLimit);
    }

    @Test public void estimatorCountsOnlyMatchedNoteAndIgnoresPriority() {
        String longNote = repeat("T\u00ean Mercedes \u0111\u1eb7t cho nh\u00e2n v\u1eadt n\u00e0y ", 20);
        AppSettings withoutNote = new AppSettings();
        withoutNote.glossaryText = "source,target,category,note\n" + KURO + ",Kuro,character,";
        AppSettings withNote = new AppSettings();
        withNote.glossaryText = "source,target,category,note,priority\n" + KURO + ",Kuro,character," + longNote + ",high";
        AppSettings unmatched = new AppSettings();
        unmatched.glossaryText = "source,target,category,note,priority\nother,Other,character," + longNote + ",high";
        AppSettings priorityOnly = new AppSettings();
        priorityOnly.glossaryText = "source,target,category,note,priority\n" + KURO + ",Kuro,character," + longNote + ",low";

        CostEstimator.Estimate base = CostEstimator.estimate(KURO, withoutNote);
        CostEstimator.Estimate injected = CostEstimator.estimate(KURO, withNote);
        CostEstimator.Estimate notInjected = CostEstimator.estimate(KURO, unmatched);
        CostEstimator.Estimate differentPriority = CostEstimator.estimate(KURO, priorityOnly);
        CostEstimator.Estimate noGlossary = CostEstimator.estimate(KURO, new AppSettings());

        assertTrue(injected.totalTokensHigh > base.totalTokensHigh);
        assertEquals(noGlossary.totalTokensHigh, notInjected.totalTokensHigh);
        assertEquals(injected.totalTokensHigh, differentPriority.totalTokensHigh);
        assertEquals(1, count(PromptContextBuilder.build(withNote.glossaryText, "", KURO, withNote).glossary, longNote.trim()));
        assertEquals(0, PromptContextBuilder.build(unmatched.glossaryText, "", KURO, unmatched).glossaryCount);
    }

    private static void assertEntry(PromptContextBuilder.TermEntry entry, String source, String target,
                                    String category, String note, String aliases) {
        assertEquals(source, entry.source);
        assertEquals(target, entry.target);
        assertEquals(category, entry.category);
        assertEquals(note, fieldValue(entry, "note"));
        assertEquals(aliases, entry.aliases);
    }

    private static JSONObject invokeToJson(GlossaryStore.Glossary glossary) throws Exception {
        Method method = GlossaryStore.class.getDeclaredMethod("toJson", GlossaryStore.Glossary.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, glossary);
    }

    private static GlossaryStore.Term newTermWithNote(String source, String target, String category, String note) {
        try {
            return GlossaryStore.Term.class
                    .getConstructor(String.class, String.class, String.class, String.class)
                    .newInstance(source, target, category, note);
        } catch (Exception missing) {
            fail("Missing four-field GlossaryStore.Term constructor");
            return new GlossaryStore.Term();
        }
    }

    private static GlossaryStore.Glossary invokeFromJson(JSONObject object) throws Exception {
        Method method = GlossaryStore.class.getDeclaredMethod("fromJson", JSONObject.class);
        method.setAccessible(true);
        return (GlossaryStore.Glossary) method.invoke(null, object);
    }

    private static String fieldValue(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(object);
            return value == null ? null : String.valueOf(value);
        } catch (Exception missing) {
            fail("Missing runtime field " + name + " on " + object.getClass().getName());
            return null;
        }
    }

    private static boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException absent) {
            return false;
        }
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
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
}
