package com.ml.tblandroidtxt;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only look at who refers to a library item, for the delete warnings. Editorial combinations are only read here
 * (columns {@code glossary_id} / {@code pronoun_id} of {@code editorial_combos}); nothing of Editorial is written or called.
 */
final class LibraryUsage {
    enum Kind { RAW, GLOSSARY, PRONOUN }

    private LibraryUsage() {}

    /** Names of Editorial combinations that use the glossary or pronoun profile with this id. */
    static List<String> editorialCombos(Context c, Kind kind, String id) {
        if (kind == Kind.RAW) return new ArrayList<>();
        String column = kind == Kind.GLOSSARY ? "glossary_id" : "pronoun_id";
        return names(c, "SELECT name FROM editorial_combos WHERE " + column + "=? ORDER BY id", id);
    }

    /** Names of chapter sets that have a row pointing at this item. */
    static List<String> chapterSets(Context c, Kind kind, String id) {
        String column = kind == Kind.RAW ? "raw_id" : kind == Kind.GLOSSARY ? "glossary_id" : "pronoun_id";
        return names(c, "SELECT DISTINCT s.name FROM chapter_sets s JOIN chapter_set_rows r ON r.set_id=s.id WHERE r." + column + "=? ORDER BY s.name", id);
    }

    private static List<String> names(Context c, String sql, String id) {
        ArrayList<String> out = new ArrayList<>();
        TranslationRepository repo = new TranslationRepository(c);
        try (Cursor cur = repo.getReadableDatabase().rawQuery(sql, new String[]{id == null ? "" : id})) {
            while (cur.moveToNext()) out.add(cur.getString(0));
        } catch (Exception ignored) {
            // A table that is missing reads as "not used"; the delete still asks for confirmation on other grounds.
        } finally { repo.close(); }
        return out;
    }
}
