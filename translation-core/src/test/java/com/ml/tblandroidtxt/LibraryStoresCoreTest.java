package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Glossary, Pronoun and Chapter-set cores over the in-memory ports, with self-written data. */
public class LibraryStoresCoreTest {
    private static final String GLOSSARY_JSON = "[{\"id\":\"g1\",\"name\":\"Mẫu\",\"sourceLang\":\"ja\",\"targetLang\":\"vi\",\"createdAt\":10,\"updatedAt\":20,"
            + "\"terms\":[{\"source\":\"魔王\",\"target\":\"Ma vương\",\"category\":\"character\",\"note\":\"ghi chú\"},"
            + "{\"source\":\"城\",\"target\":\"thành\",\"category\":\"term\",\"note\":\"\"}]}]";
    private static final String PRONOUN_JSON = "[{\"id\":\"p1\",\"name\":\"Xưng hô\",\"uri\":\"u\",\"text\":\"from,to,pronoun\\nA,B,tao/mày\",\"createdAt\":1,\"updatedAt\":2}]";

    @Before @After public void resetCaches() {
        GlossaryStoreCore.resetForTest();
        PronounStoreCore.resetForTest();
    }

    private static boolean sameJson(String a, String b) throws Exception {
        return new JSONArray(a).similar(new JSONArray(b));
    }

    @Test public void glossaryRoundTripKeepsTheStoredJson() throws Exception {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        store.edit().putString(GlossaryStoreCore.KEY_GLOSSARIES, GLOSSARY_JSON).commit();
        List<GlossaryStoreCore.Glossary> list = GlossaryStoreCore.loadAll(store);
        assertEquals(1, list.size());
        GlossaryStoreCore.Glossary g = list.get(0);
        assertEquals("g1", g.id);
        assertEquals("Mẫu", g.name);
        assertEquals(2, g.count());
        assertEquals("ghi chú", g.terms.get(0).note);
        assertTrue(GlossaryStoreCore.saveAll(store, list));
        assertTrue(sameJson(GLOSSARY_JSON, store.getString(GlossaryStoreCore.KEY_GLOSSARIES, "")));
        assertEquals("# Glossary: Mẫu\nSource, Target, Category\n魔王 => Ma vương [character] | ghi chú\n城 => thành [term]\n",
                GlossaryStoreCore.toPromptText(g).replace("\r", ""));
    }

    @Test public void glossaryCrudAndSelection() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        GlossaryStoreCore.Glossary created = GlossaryStoreCore.create(store, "  Bộ A ");
        assertNotNull(created);
        assertEquals("Bộ A", created.name);
        assertEquals(1, GlossaryStoreCore.loadAll(store).size());
        assertFalse(GlossaryStoreCore.isCorrupt(store));
        assertTrue(GlossaryStoreCore.setSelectedId(store, created.id));
        assertEquals(created.id, GlossaryStoreCore.getSelectedId(store));
        assertEquals(created.id, GlossaryStoreCore.selected(store).id);
        created.terms.add(new GlossaryStoreCore.Term("a", "b", "term", "n"));
        assertTrue(GlossaryStoreCore.upsert(store, created));
        assertEquals(1, GlossaryStoreCore.find(store, created.id).count());
        assertTrue(GlossaryStoreCore.delete(store, created.id));
        assertNull(GlossaryStoreCore.find(store, created.id));
        assertEquals("deleting the selected glossary clears the selection", "", GlossaryStoreCore.getSelectedId(store));
    }

    @Test public void unreadableGlossaryStoreIsPreservedAndLocked() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        store.edit().putString(GlossaryStoreCore.KEY_GLOSSARIES, "{not json").commit();
        assertTrue(GlossaryStoreCore.loadAll(store).isEmpty());
        assertTrue(GlossaryStoreCore.isCorrupt(store));
        assertEquals("{not json", GlossaryStoreCore.corruptText(store));
        assertFalse("a locked store refuses writes", GlossaryStoreCore.saveAll(store, new ArrayList<>()));
        assertNull("the unreadable value is untouched", GlossaryStoreCore.create(store, "x"));
        boolean kept = false;
        for (String key : store.getAll().keySet()) {
            if (key.startsWith(GlossaryStoreCore.KEY_GLOSSARIES + LibraryPrefs.CORRUPT_SUFFIX)) kept = true;
        }
        assertTrue("a copy is stored under a *_corrupt_* key", kept);
        assertEquals("{not json", store.getString(GlossaryStoreCore.KEY_GLOSSARIES, ""));
    }

    @Test public void pronounRoundTripAndSelection() throws Exception {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        store.edit().putString(PronounStoreCore.KEY_ITEMS, PRONOUN_JSON).commit();
        List<PronounStoreCore.Profile> list = PronounStoreCore.loadAll(store);
        assertEquals(1, list.size());
        assertEquals("p1", list.get(0).id);
        assertEquals("from,to,pronoun\nA,B,tao/mày", list.get(0).text);
        assertTrue(PronounStoreCore.saveAll(store, list));
        assertTrue(sameJson(PRONOUN_JSON, store.getString(PronounStoreCore.KEY_ITEMS, "")));

        assertTrue(PronounStoreCore.setSelectedId(store, "p1"));
        assertEquals("from,to,pronoun\nA,B,tao/mày", PronounStoreCore.selectedPromptText(store));
        PronounStoreCore.Profile imported = PronounStoreCore.importAndSelectProfile(store, "", "uri", "x");
        assertNotNull(imported);
        assertEquals("Pronoun", imported.name);
        assertEquals(imported.id, PronounStoreCore.getSelectedId(store));
        assertEquals(2, PronounStoreCore.loadAll(store).size());
        assertTrue(PronounStoreCore.delete(store, imported.id));
        assertEquals("", PronounStoreCore.getSelectedId(store));
    }

    @Test public void pronounMigrationRunsOnce() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        AppSettings s = new AppSettings();
        s.pronounText = "from,to,pronoun\nA,B,tao/mày";
        s.pronounName = "Cũ";
        PronounStoreCore.migrateLegacy(store, s);
        List<PronounStoreCore.Profile> list = PronounStoreCore.loadAll(store);
        assertEquals(1, list.size());
        assertEquals("Cũ", list.get(0).name);
        assertEquals(list.get(0).id, PronounStoreCore.getSelectedId(store));
        PronounStoreCore.migrateLegacy(store, s);
        assertEquals("the legacy_migrated flag stops a second import", 1, PronounStoreCore.loadAll(store).size());
    }

    @Test public void chapterSetsRoundTripOverSqlite() throws Exception {
        TestDb t = TestDb.openWithSchema();
        try {
            ChapterSetStoreCore.SetData data = new ChapterSetStoreCore.SetData();
            data.id = "set-1";
            data.name = "Tập 1";
            data.rows.add(new ChapterSetStoreCore.Row(1, "raw-a", "g1", "p1"));
            data.rows.add(new ChapterSetStoreCore.Row(ChapterNumber.NONE, "raw-b", "", ""));
            ChapterSetStoreCore.save(t.db, data);

            ChapterSetStoreCore.SetData back = ChapterSetStoreCore.get(t.db, "set-1");
            assertNotNull(back);
            assertEquals("Tập 1", back.name);
            assertEquals(2, back.rows.size());
            assertEquals(1, back.rows.get(0).chapterNo);
            assertEquals("raw-a", back.rows.get(0).rawId);
            assertEquals("g1", back.rows.get(0).glossaryId);
            assertEquals(ChapterNumber.NONE, back.rows.get(1).chapterNo);
            assertEquals("raw-b", back.rows.get(1).rawId);

            List<ChapterSetStoreCore.SetInfo> sets = ChapterSetStoreCore.listSets(t.db);
            assertEquals(1, sets.size());
            assertEquals(2, sets.get(0).chapters);

            data.name = "Tập 1 (sửa)";
            data.rows.remove(1);
            ChapterSetStoreCore.save(t.db, data);
            back = ChapterSetStoreCore.get(t.db, "set-1");
            assertEquals("Tập 1 (sửa)", back.name);
            assertEquals(1, back.rows.size());

            ChapterSetStoreCore.delete(t.db, java.util.Collections.singletonList("set-1"));
            assertNull(ChapterSetStoreCore.get(t.db, "set-1"));
            assertTrue(ChapterSetStoreCore.listSets(t.db).isEmpty());
        } finally {
            t.close();
        }
    }

    @Test public void workspaceModelMergesLiveSetsAndSavedRuns() {
        ChapterSetStoreCore.SetInfo live = new ChapterSetStoreCore.SetInfo();
        live.id = "s1";
        live.name = "Tập 1";
        live.chapters = 3;
        live.updatedAt = 50;
        List<WorkspaceSetModelCore.Entry> merged = WorkspaceSetModelCore.merge(java.util.Collections.singletonList(live), null, null);
        assertEquals(1, merged.size());
        assertEquals("Tập 1", merged.get(0).label());
        assertEquals(3, merged.get(0).unfinished());
        assertEquals(1, WorkspaceSetModelCore.recent(merged, 5).size());
        assertEquals(0, WorkspaceSetModelCore.recent(merged, 0).size());
    }
}
