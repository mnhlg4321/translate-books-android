package com.ml.tblandroidtxt.editorial.api;

import java.util.List;

/** One request the app sends: system and user text plus what the app records about it. */
public record ApiPrompt(EditorialApiContract.Step step, String system, String user, int glossaryEntries, int pronounRows,
                        String qualityCoreSha256, List<EditInputs.OriginalSourceFile> originalSourceFiles) {
    public ApiPrompt {
        system = system == null ? "" : system;
        user = user == null ? "" : user;
        originalSourceFiles = originalSourceFiles == null ? List.of() : List.copyOf(originalSourceFiles);
    }

    /** Backwards-compatible constructor for check/pair prompts and older callers. */
    public ApiPrompt(EditorialApiContract.Step step, String system, String user, int glossaryEntries, int pronounRows,
                     String qualityCoreSha256) {
        this(step, system, user, glossaryEntries, pronounRows, qualityCoreSha256, List.of());
    }

    /** Rough input size for budget checks: one token per three characters is deliberately generous for Japanese and Vietnamese. */
    public long estimatedInputTokens() {
        return ((long) system.length() + user.length()) / 3L + 1L;
    }
}
