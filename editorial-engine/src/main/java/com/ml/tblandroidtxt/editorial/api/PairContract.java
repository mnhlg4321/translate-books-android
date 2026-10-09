package com.ml.tblandroidtxt.editorial.api;

/** Constants of the chunk-pair contract (docs/EDITORIAL_API_V1_CHUNK_PAIR_OFFLINE_PACKAGE_20261006.md, section 10). */
public final class PairContract {
    /** Implementation revision of the specification; evidence quotes it together with the spec file hash and the commit. */
    public static final String REVISION = "CP-IMPL-3";
    public static final String NORMALIZATION_REVISION = "NFC-LF-1";

    /** Characters of reference-only context kept on each side of a main range. */
    public static final int CONTEXT_CHARS = 400;
    /** Below this many characters in the draft range the character ratio is not used (section 4.2). */
    public static final int RATIO_MIN_CHARS = 80;
    /** Absolute ceiling of a candidate for such a short range. */
    public static final int SHORT_MAX_CANDIDATE_CHARS = 320;
    /** Paragraph label put in front of RAW paragraphs when a pronoun row applies to part of the main range. */
    public static final String LABEL_OPEN = "⟦P";
    public static final String LABEL_CLOSE = "⟧";

    private PairContract() { }

    public static String paragraphLabel(int paragraph) {
        return LABEL_OPEN + String.format(java.util.Locale.ROOT, "%03d", paragraph) + LABEL_CLOSE;
    }
}
