package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** v4.22 S0.6: the glossary card shows counts by category instead of an empty "— → —". */
public class GlossaryCardMetaTest {
    @Test public void countsByCategoryWithoutEmptyLanguages() {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.terms.add(new GlossaryStore.Term("花子", "Hanako", "character"));
        g.terms.add(new GlossaryStore.Term("魔法", "ma pháp", "term"));
        g.terms.add(new GlossaryStore.Term("森", "rừng", "place"));
        assertEquals("3 mục · 1 nhân vật · 2 thuật ngữ", GlossaryPageFactory.cardMeta(g));
    }

    @Test public void languagesShowOnlyWhenFilledIn() {
        GlossaryStore.Glossary g = new GlossaryStore.Glossary();
        g.sourceLang = "Japanese";
        assertEquals("0 mục · 0 nhân vật · 0 thuật ngữ • Japanese → —", GlossaryPageFactory.cardMeta(g));
    }
}
