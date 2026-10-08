package com.ml.tblandroidtxt.editorial.api;

import java.util.List;

/** Builds the edit call (step E): quality standard, filtered reference, RAW, DRAFT and the tag contract. */
public final class EditPromptBuilder {
    public static final String EDITED_OPEN = "<EDITED>";
    public static final String EDITED_CLOSE = "</EDITED>";
    public static final String NOTES_OPEN = "<NOTES>";
    public static final String NOTES_CLOSE = "</NOTES>";
    public static final String WRONG_PAIR_OPEN = "<WRONG_PAIR>";
    public static final String WRONG_PAIR_CLOSE = "</WRONG_PAIR>";

    public static final String NO_REFERENCE =
            "No glossary and no pronoun rows are supplied. Keep the way characters are named and addressed that is already "
            + "consistent in the DRAFT, unless RAW contradicts it.";

    public static final String OUTPUT_CONTRACT =
            "OUTPUT CONTRACT\n"
            + "Return the complete corrected text between " + EDITED_OPEN + " and " + EDITED_CLOSE + ". Do not shorten it and do not "
            + "add anything inside the tags that is not part of the text.\n"
            + "After it you may add " + NOTES_OPEN + " ... " + NOTES_CLOSE + " with one line per decision worth a human look, in the form "
            + "quote from your text | kind | short reason. Notes are optional.\n"
            + "If RAW and DRAFT are clearly two different chapters, return " + WRONG_PAIR_OPEN + "one sentence of evidence with a short "
            + "quote from each" + WRONG_PAIR_CLOSE + " instead of " + EDITED_OPEN + ". A missing sentence or a wrong number is a defect "
            + "to fix, not a wrong pair.\n";

    private EditPromptBuilder() { }


    /**
     * The glossary and pronoun sections, identical for the edit and the check so both see the same authoritative
     * reference. The lists are the ones already filtered by RAW occurrence; nothing is added when both are empty.
     */
    static void appendReferences(StringBuilder system, List<EditInputs.GlossaryEntry> glossary, List<String> pronouns) {
        if (!glossary.isEmpty()) {
            system.append("\n# GLOSSARY (source | target | category | note)\n");
            for (EditInputs.GlossaryEntry entry : glossary) {
                system.append(entry.source().trim()).append(" | ").append(entry.target().trim());
                if (!entry.category().isBlank() || !entry.note().isBlank()) {
                    system.append(" | ").append(entry.category().trim()).append(" | ").append(entry.note().trim());
                }
                system.append('\n');
            }
        }
        if (!pronouns.isEmpty()) {
            system.append("\n# PRONOUNS (from, speaker, target, self, call, scope, note)\n");
            for (String row : pronouns) system.append(row).append('\n');
        }
    }

    public static ApiPrompt build(EditInputs inputs) {
        return build(inputs, RawAlignedNormalizer.normalize(inputs.raw(), inputs.draft(), inputs.glossary()));
    }

    static ApiPrompt build(EditInputs inputs, RawAlignedNormalizer.Result normalized) {
        List<EditInputs.GlossaryEntry> glossary = ReferenceFilter.glossary(inputs.raw(), inputs.glossary());
        List<String> pronouns = ReferenceFilter.pronounRows(inputs.raw(), inputs.pronounCsv());
        StringBuilder system = new StringBuilder(QualityCore.editPrompt(inputs.targetLanguage()));
        if (glossary.isEmpty() && pronouns.isEmpty()) {
            system.append('\n').append(NO_REFERENCE).append('\n');
        }
        appendReferences(system, glossary, pronouns);
        system.append("\n# APP DETECTIONS\n");
        if (normalized.detections().isEmpty()) system.append("(none)\n");
        for (RawAlignedNormalizer.Detection detection : normalized.detections()) {
            system.append("- line ").append(detection.draftLine()).append(": ")
                    .append(detection.kind()).append(" (").append(detection.detail()).append(")\n");
        }
        system.append('\n').append(OUTPUT_CONTRACT);
        String user = "# RAW\n" + inputs.raw() + "\n\n# DRAFT\n" + normalized.text() + "\n";
        return new ApiPrompt(EditorialApiContract.Step.EDIT, system.toString(), user, glossary.size(), pronouns.size(),
                QualityCore.editSha256(), inputs.originalSourceFiles());
    }
}
