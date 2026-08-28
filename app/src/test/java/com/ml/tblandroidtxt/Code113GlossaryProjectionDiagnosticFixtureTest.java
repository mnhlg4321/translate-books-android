package com.ml.tblandroidtxt;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Characterization only: records the code113 five-column Glossary projection. */
public class Code113GlossaryProjectionDiagnosticFixtureTest {
    private static final String RESOURCE = "fixtures/code113/glossary-five-column.csv";
    private static final String NOTE = "Tên Mercedes đặt cho nhân vật này";

    @Test public void code113GlossaryFiveColumnsLoseNoteAtStoreAdapter() throws Exception {
        String raw = readResource(RESOURCE);
        String[] lines = raw.trim().split("\\r?\\n");
        assertEquals(2, lines.length);
        String[] row = lines[1].split(",", -1);
        assertEquals(5, row.length);
        assertEquals("クロ", row[0]);
        assertEquals("Kuro", row[1]);
        assertEquals("character", row[2]);
        assertEquals(NOTE, row[3]);
        assertEquals("high", row[4]);

        PromptContextBuilder.ParseReport parsed = PromptContextBuilder.validate(raw, "");
        assertEquals(1, parsed.glossaryTerms.size());
        PromptContextBuilder.TermEntry entry = parsed.glossaryTerms.get(0);
        assertEquals("クロ", entry.source);
        assertEquals("Kuro", entry.target);
        assertEquals("character", entry.category);
        assertEquals(NOTE, entry.aliases);
        assertEquals(0, entry.priority);

        List<GlossaryStore.Term> adapted = GlossaryStore.parseTerms("Mercedes.csv", raw);
        assertEquals(1, adapted.size());
        GlossaryStore.Term term = adapted.get(0);
        assertEquals("クロ", term.source);
        assertEquals("Kuro", term.target);
        assertEquals("character", term.category);
        assertFalse(hasTermField("note"));
        assertFalse(hasTermField("aliases"));
        assertFalse(hasTermField("priority"));

        GlossaryStore.Glossary glossary = new GlossaryStore.Glossary();
        glossary.name = "Mercedes";
        glossary.terms.add(term);
        String prompt = GlossaryStore.toPromptText(glossary);
        String[] promptLines = prompt.trim().split("\\r?\\n");
        assertEquals("クロ => Kuro [character]", promptLines[2]);
        assertTrue(prompt.contains("クロ => Kuro [character]"));
        assertFalse(prompt.contains(NOTE));
        assertFalse(prompt.contains("high"));

        String duplicateRows = "source,target,category,note,priority\n"
                + "クロ,Kuro,character,first,high\n"
                + "クロ,Kuro,term,second,low\n";
        assertEquals(1, GlossaryStore.parseTerms("duplicate.csv", duplicateRows).size());

        StringBuilder manyGlobalTerms = new StringBuilder("source,target,category,note,priority\n");
        for (int i = 0; i < 85; i++) {
            manyGlobalTerms.append("g").append(i).append(",t").append(i)
                    .append(",global,note,high\n");
        }
        PromptContextBuilder.ContextBlock limited = PromptContextBuilder.build(
                manyGlobalTerms.toString(), "", "unrelated text", new AppSettings());
        assertEquals(80, new AppSettings().glossaryInjectLimit);
        assertEquals(80, limited.glossaryCount);

        String diagnostic = "baseline.code113.expected.error=true\n"
                + "raw.row.column.count=" + row.length + "\n"
                + "entry.source=" + entry.source + "\n"
                + "entry.target=" + entry.target + "\n"
                + "entry.category=" + entry.category + "\n"
                + "entry.aliases(note)=" + entry.aliases + "\n"
                + "entry.priority=" + entry.priority + " (fifth CSV column ignored)\n"
                + "store.term=source,target,category only\n"
                + "prompt.line=" + promptLines[2] + "\n"
                + "store.adapter.note.aliases=LOST\n"
                + "store.adapter.priority=LOST\n"
                + "dedupe.same-source-target.count="
                + GlossaryStore.parseTerms("duplicate.csv", duplicateRows).size() + "\n"
                + "default.glossary.limit.observed=" + limited.glossaryCount;
        System.out.println(diagnostic);
    }

    private static String readResource(String name) throws Exception {
        InputStream in = Code113GlossaryProjectionDiagnosticFixtureTest.class.getClassLoader().getResourceAsStream(name);
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

    private static boolean hasTermField(String name) {
        try {
            GlossaryStore.Term.class.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException absent) {
            return false;
        }
    }
}
