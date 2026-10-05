package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import org.json.JSONObject;

import java.math.BigDecimal;

/** A saved RAW + DRAFT (+ Glossary / Pronoun) selection with its settings; the unit the person runs. */
public final class EditorialApiCombo {
    public static final BigDecimal DEFAULT_MAX_USD = new BigDecimal("0.10");

    /** Per-combo choices stored as JSON in {@code settings_json}. */
    public static final class Settings {
        public EditorialApiContract.Mode mode = EditorialApiContract.Mode.QUICK;
        /** Empty = the model of the app settings. */
        public String model = "";
        public BigDecimal maxUsdPerChapter = DEFAULT_MAX_USD;

        public String toJson() {
            try {
                return new JSONObject().put("mode", mode.name()).put("model", model == null ? "" : model)
                        .put("maxUsd", maxUsdPerChapter.toPlainString()).toString();
            } catch (Exception impossible) {
                throw new IllegalStateException(impossible);
            }
        }

        public static Settings fromJson(String json) {
            Settings settings = new Settings();
            try {
                JSONObject o = new JSONObject(json == null || json.isEmpty() ? "{}" : json);
                try { settings.mode = EditorialApiContract.Mode.valueOf(o.optString("mode", "QUICK")); } catch (IllegalArgumentException ignored) { }
                settings.model = o.optString("model", "");
                try {
                    BigDecimal cap = new BigDecimal(o.optString("maxUsd", DEFAULT_MAX_USD.toPlainString()));
                    if (cap.signum() > 0) settings.maxUsdPerChapter = cap;
                } catch (NumberFormatException ignored) { }
            } catch (Exception ignored) {
                // an unreadable value falls back to the defaults
            }
            return settings;
        }
    }

    public long id;
    public String name = "";
    public String rawUri = "";
    public String rawName = "";
    public String draftUri = "";
    public String draftName = "";
    /** {@link GlossaryStore} / {@link PronounStore} profile ids; empty = not used. */
    public String glossaryId = "";
    public String pronounId = "";
    public String settingsJson = "{}";
    public long createdAt;
    public long updatedAt;

    public Settings settings() { return Settings.fromJson(settingsJson); }

    public EditorialApiCombo copy() {
        EditorialApiCombo c = new EditorialApiCombo();
        c.id = id; c.name = name; c.rawUri = rawUri; c.rawName = rawName; c.draftUri = draftUri; c.draftName = draftName;
        c.glossaryId = glossaryId; c.pronounId = pronounId; c.settingsJson = settingsJson; c.createdAt = createdAt; c.updatedAt = updatedAt;
        return c;
    }

    /** A suggested name such as {@code raw001+draft001+glossary001+pronoun001} from the chosen sources. */
    public static String suggestName(String rawName, String draftName, String glossaryName, String pronounName) {
        StringBuilder out = new StringBuilder(stem(rawName, "raw"));
        out.append('+').append(stem(draftName, "draft"));
        if (glossaryName != null && !glossaryName.isEmpty()) out.append('+').append(stem(glossaryName, "glossary"));
        if (pronounName != null && !pronounName.isEmpty()) out.append('+').append(stem(pronounName, "pronoun"));
        return out.toString();
    }

    private static String stem(String name, String fallback) {
        String base = name == null ? "" : name.trim();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        return base.isEmpty() ? fallback : base;
    }
}
