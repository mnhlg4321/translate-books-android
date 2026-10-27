package com.ml.tblandroidtxt;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** Android face of ChapterSetStore: each call opens the app database and runs the core query on it. */
final class ChapterSetStore extends ChapterSetStoreCore {
    private ChapterSetStore() {}

    static List<SetInfo> listSets(Context c) {
        TranslationRepository repo = new TranslationRepository(c);
        try { return ChapterSetStoreCore.listSets(repo.db()); } finally { repo.close(); }
    }

    static SetData get(Context c, String id) {
        TranslationRepository repo = new TranslationRepository(c);
        try { return ChapterSetStoreCore.get(repo.db(), id); } finally { repo.close(); }
    }

    /** Saves (insert or replace) a whole set in one transaction. */
    static void save(Context c, SetData data) {
        TranslationRepository repo = new TranslationRepository(c);
        try { ChapterSetStoreCore.save(repo.db(), data); } finally { repo.close(); }
    }

    static void delete(Context c, List<String> ids) {
        TranslationRepository repo = new TranslationRepository(c);
        try { ChapterSetStoreCore.delete(repo.db(), ids); } finally { repo.close(); }
    }

    static List<SetData> snapshot(Context c, List<String> ids) {
        ArrayList<SetData> out = new ArrayList<>();
        for (String id : ids) { SetData d = get(c, id); if (d != null) out.add(d); }
        return out;
    }

    /** Undo of a delete: the sets come back with their old ids and rows. */
    static void restore(Context c, List<SetData> sets) {
        for (SetData d : sets) save(c, d);
    }
}
