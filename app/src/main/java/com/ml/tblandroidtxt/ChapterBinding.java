package com.ml.tblandroidtxt;

import org.json.JSONObject;

/**
 * What one chapter of a chapter-set run translates with: its RAW and the exact Glossary / Pronoun text taken when the run was
 * prepared or started. The texts and their hashes are copied, so later edits in the Library cannot change a run that has started.
 */
final class ChapterBinding {
    String rawId = "";
    String rawName = "";
    int chapterNo = ChapterNumber.NONE;
    String glossaryId = "";
    String glossaryName = "";
    String glossaryText = "";
    String glossaryHash = "";
    String pronounId = "";
    String pronounName = "";
    String pronounText = "";
    String pronounHash = "";

    /** Binding for a RAW with the chosen profiles; a null profile means "Không dùng" (no glossary / no pronoun text for this chapter). */
    static ChapterBinding of(String rawId, String rawName, int chapterNo, GlossaryStore.Glossary glossary, PronounStore.Profile pronoun) {
        ChapterBinding b = new ChapterBinding();
        b.rawId = rawId == null ? "" : rawId;
        b.rawName = rawName == null ? "" : rawName;
        b.chapterNo = chapterNo;
        if (glossary != null) {
            b.glossaryId = glossary.id;
            b.glossaryName = glossary.name == null ? "" : glossary.name;
            b.glossaryText = GlossaryStore.toPromptText(glossary);
        }
        if (pronoun != null) {
            b.pronounId = pronoun.id;
            b.pronounName = pronoun.name == null ? "" : pronoun.name;
            b.pronounText = pronoun.text == null ? "" : pronoun.text;
        }
        b.glossaryHash = HashUtil.sha256(b.glossaryText);
        b.pronounHash = HashUtil.sha256(b.pronounText);
        return b;
    }

    /** The run's settings for this chapter: a copy of {@code base} with this chapter's Glossary and Pronoun. {@code base} is not changed. */
    AppSettings applyTo(AppSettings base) {
        AppSettings s = base.copy();
        s.glossaryText = glossaryText;
        s.selectedGlossaryId = glossaryId;
        s.selectedGlossaryName = glossaryName;
        s.pronounText = pronounText;
        s.pronounUri = "";
        s.pronounName = pronounName;
        s.selectedPronounId = pronounId;
        s.selectedPronounName = pronounName;
        return s;
    }

    String rawUri() { return RawUri.of(rawId); }

    /** Short text for the plan key and logs; contains hashes only, never profile text. */
    String keyPart() { return rawId + "|g:" + glossaryHash + "|p:" + pronounHash; }

    JSONObject toJson() throws Exception {
        return new JSONObject().put("rawId", rawId).put("rawName", rawName).put("chapterNo", chapterNo)
                .put("glossaryId", glossaryId).put("glossaryName", glossaryName).put("glossaryText", glossaryText).put("glossaryHash", glossaryHash)
                .put("pronounId", pronounId).put("pronounName", pronounName).put("pronounText", pronounText).put("pronounHash", pronounHash);
    }

    static ChapterBinding fromJson(JSONObject o) {
        ChapterBinding b = new ChapterBinding();
        b.rawId = o.optString("rawId"); b.rawName = o.optString("rawName"); b.chapterNo = o.optInt("chapterNo", ChapterNumber.NONE);
        b.glossaryId = o.optString("glossaryId"); b.glossaryName = o.optString("glossaryName"); b.glossaryText = o.optString("glossaryText"); b.glossaryHash = o.optString("glossaryHash");
        b.pronounId = o.optString("pronounId"); b.pronounName = o.optString("pronounName"); b.pronounText = o.optString("pronounText"); b.pronounHash = o.optString("pronounHash");
        return b;
    }

    /** True when the stored hashes still match the stored texts (a damaged snapshot file must not be used). */
    boolean intact() {
        return HashUtil.sha256(glossaryText).equals(glossaryHash) && HashUtil.sha256(pronounText).equals(pronounHash);
    }
}
