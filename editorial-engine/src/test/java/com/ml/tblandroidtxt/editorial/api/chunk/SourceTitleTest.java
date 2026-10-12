package com.ml.tblandroidtxt.editorial.api.chunk;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * S1 on the real file names of the owner's two sets (names only, no content): 28 light-novel chapters and 85 web-novel
 * chapters, listed in {@code src/test/resources/cs1/file-names.txt}.
 */
public final class SourceTitleTest {
    private static Map<String, List<String>> names() throws IOException {
        Map<String, List<String>> sets = new LinkedHashMap<>();
        try (InputStream in = SourceTitleTest.class.getResourceAsStream("/cs1/file-names.txt");
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab < 0) continue;
                sets.computeIfAbsent(line.substring(0, tab), k -> new ArrayList<>()).add(line.substring(tab + 1));
            }
        }
        return sets;
    }

    @Test public void titlesAreReadFromTheOwnersNamePatterns() {
        assertEquals("獅子身中の怪異", SourceTitle.of("0043-042　獅子身中の怪異.txt"));
        assertEquals("獅子身中の怪異", SourceTitle.of("043_獅子身中の怪異_translated.txt"));
        assertEquals("人間の着ぐるみ", SourceTitle.of("044_人間の着ぐるみ_translated_v2.txt"));
        assertEquals("【書籍版発売記念】EX1 ひとりじめの日", SourceTitle.of("0085-【書籍版発売記念】EX1　ひとりじめの日.txt"));
        assertEquals("【書籍版発売記念】EX1 ひとりじめの日", SourceTitle.of("085_【書籍版発売記念】ＥＸ１　ひとりじめの日_translated.txt"));
        assertEquals("chapter name", SourceTitle.of("012_chapter name_translated(1).txt"));
        // the light-novel files carry a number, a role and the series and no title
        for (String n : new String[] {"007_RAW_JAKUAKU_MONSTER_VOL1.txt", "002_RAW_JAKUAKU_MONSTER_VOL1_DRAFT.txt", "006_JAKUAKU_MONSTER_VOL1_DRAFT.txt",
                "017_JAKUAKU_MONSTER_VOL1_translated.txt", "003_FINAL_QA_JAKUAKU_MONSTER_VOL1(1).txt", "040_FINAL_QA_JAKUAKU_MONSTER.txt",
                "007_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv", "007_PRONOUN_JAKUAKU_MONSTER_VOL1.csv"}) {
            assertEquals(n, "", SourceTitle.of(n));
        }
        assertEquals("", SourceTitle.of(null));
    }

    @Test public void positionIsTheLeadingNumberAndNotTheChapterNumberOfTheStory() {
        assertEquals(43, SourceTitle.position("0043-042　獅子身中の怪異.txt"));
        assertEquals(43, SourceTitle.position("043_獅子身中の怪異_translated.txt"));
        assertEquals(-1, SourceTitle.position("notes.txt"));
    }

    @Test public void allTwentyEightLightNovelChaptersPairByPosition() throws IOException {
        Map<String, List<String>> s = names();
        List<SourceTitle.Pair> pairs = SourceTitle.pair(s.get("LN_RAW"), s.get("LN_DRAFT"));
        assertEquals(28, pairs.size());
        for (SourceTitle.Pair p : pairs) {
            assertEquals("ORDER", p.method());
            assertEquals(SourceTitle.position(s.get("LN_RAW").get(p.rawIndex())), SourceTitle.position(s.get("LN_DRAFT").get(p.draftIndex())));
        }
    }

    @Test public void allEightyFiveWebNovelChaptersPairByTitleEvenWhenTheStoryNumbersDrift() throws IOException {
        Map<String, List<String>> s = names();
        List<SourceTitle.Pair> pairs = SourceTitle.pair(s.get("WN_RAW"), s.get("WN_DRAFT"));
        assertEquals(85, pairs.size());
        for (SourceTitle.Pair p : pairs) {
            assertEquals("TITLE", p.method());
            assertEquals(SourceTitle.position(s.get("WN_RAW").get(p.rawIndex())), SourceTitle.position(s.get("WN_DRAFT").get(p.draftIndex())));
        }
        // the file after the two settings files has a story number 3 below its position; pairing by that number would be wrong
        String raw = "0043-042　獅子身中の怪異.txt";
        assertTrue(s.get("WN_RAW").contains(raw));
        int draftIndex = pairs.stream().filter(p -> s.get("WN_RAW").get(p.rawIndex()).equals(raw)).findFirst().orElseThrow().draftIndex();
        assertTrue(s.get("WN_DRAFT").get(draftIndex).startsWith("043_"));
    }

    @Test public void glossaryAndPronounFollowTheDraftPosition() throws IOException {
        Map<String, List<String>> s = names();
        for (String set : new String[] {"LN", "WN"}) {
            List<String> drafts = s.get(set + "_DRAFT");
            for (String other : new String[] {"GLOSSARY", "PRONOUN", "FINAL"}) {
                List<String> files = s.get(set + "_" + other);
                int[] attached = SourceTitle.attach(drafts, files);
                for (int i = 0; i < drafts.size(); i++) {
                    assertTrue(set + " " + other + " " + i, attached[i] >= 0);
                    assertEquals(SourceTitle.position(drafts.get(i)), SourceTitle.position(files.get(attached[i])));
                }
            }
        }
    }

    @Test public void duplicateTitlesOrUnevenLeftoversAreNotGuessed() {
        List<String> raw = List.of("001_Same.txt", "002_Same.txt", "003_Other.txt");
        List<String> draft = List.of("001_Same_translated.txt", "003_Other_translated.txt");
        List<SourceTitle.Pair> pairs = SourceTitle.pair(raw, draft);
        assertEquals(1, pairs.size());
        assertEquals("Other", SourceTitle.of(raw.get(pairs.get(0).rawIndex())));
    }
}
