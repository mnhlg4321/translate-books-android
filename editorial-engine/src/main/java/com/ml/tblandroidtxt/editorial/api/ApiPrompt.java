package com.ml.tblandroidtxt.editorial.api;

/** One request the app sends: system and user text plus what the app records about it. */
public record ApiPrompt(EditorialApiContract.Step step, String system, String user, int glossaryEntries, int pronounRows,
                        String qualityCoreSha256) {
    public ApiPrompt {
        system = system == null ? "" : system;
        user = user == null ? "" : user;
    }

    /** Rough input size for budget checks: one token per three characters is deliberately generous for Japanese and Vietnamese. */
    public long estimatedInputTokens() {
        return ((long) system.length() + user.length()) / 3L + 1L;
    }
}
