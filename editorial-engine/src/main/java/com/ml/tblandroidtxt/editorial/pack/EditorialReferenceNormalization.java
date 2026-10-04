package com.ml.tblandroidtxt.editorial.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic normalization for repeated list references; entity identities remain parser-validated. */
final class EditorialReferenceNormalization {
    static final class Counter {
        private int duplicateReferencesRemoved;
        private int draftAnchorsDerivedFromQuote;
        private int maxDraftAnchorDeviation;
        private final List<String> speakerRecordsDropped = new ArrayList<>();

        int duplicateReferencesRemoved() { return duplicateReferencesRemoved; }

        int draftAnchorsDerivedFromQuote() { return draftAnchorsDerivedFromQuote; }

        int maxDraftAnchorDeviation() { return maxDraftAnchorDeviation; }

        /** Paths of the speaker records the app dropped because their unit reference was not a unit line. */
        List<String> speakerRecordsDropped() { return List.copyOf(speakerRecordsDropped); }

        void speakerRecordDropped(String path) { speakerRecordsDropped.add(path); }

        /** The app moved a hinted line anchor onto the line that carries the quote. */
        void draftAnchorDerived(int deviationLines) {
            draftAnchorsDerivedFromQuote++;
            maxDraftAnchorDeviation = Math.max(maxDraftAnchorDeviation, deviationLines);
        }

        <T> List<T> distinct(List<T> values) {
            Set<T> seen = new HashSet<>();
            List<T> result = new ArrayList<>(values.size());
            for (T value : values) {
                if (seen.add(value)) result.add(value);
                else duplicateReferencesRemoved++;
            }
            return List.copyOf(result);
        }

        boolean addReference(Set<String> seen, String value) {
            if (seen.add(value)) return true;
            duplicateReferencesRemoved++;
            return false;
        }
    }

    private EditorialReferenceNormalization() { }
}
