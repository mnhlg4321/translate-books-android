package com.ml.tblandroidtxt.editorial.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts only the explicitly delimited final text from a V5 chat turn. */
public final class V5FinalExtractor {
    private static final Pattern FINAL = Pattern.compile("<FINAL>(.*?)</FINAL>", Pattern.DOTALL);
    private static final Pattern EXPLICIT_FILE = Pattern.compile(
            "(?is)===\\s*FILE:\\s*[^\\r\\n]*FINAL_QA[^\\r\\n]*?\\.txt\\s*===\\s*\\r?\\n(.*?)\\r?\\n===\\s*END FILE\\s*===");
    private static final Pattern FILE_HEADER = Pattern.compile(
            "(?im)^\\s*(?:===\\s*FILE:\\s*)?(?:#{1,6}\\s*)?(?:\\*\\*)?(?:FILE\\s*:\\s*)?[^\\r\\n]*FINAL_QA[^\\r\\n]*?\\.txt(?:\\*\\*)?(?:\\s*===)?\\s*$");
    private static final Pattern NEXT_ARTIFACT = Pattern.compile(
            "(?im)^\\s*(?:===\\s*FILE:|#{1,6}\\s*[^\\r\\n]*(?:QA_RECEIPT|PAIR_DELTA_QA)|[^\\r\\n]*(?:QA_RECEIPT|PAIR_DELTA_QA)[^\\r\\n]*\\.txt)");
    private static final Pattern FENCE_START = Pattern.compile("(?m)^\\s*```[^\\r\\n]*\\r?\\n");
    private static final Pattern FENCE_END = Pattern.compile("(?m)^\\s*```\\s*$");

    private V5FinalExtractor() { }

    public static String extract(String response) {
        if (response == null) return "";
        Matcher matcher = FINAL.matcher(response);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value != null && !value.isBlank()) return value.strip();
        }
        matcher = EXPLICIT_FILE.matcher(response);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value != null && !value.isBlank()) return value.strip();
        }
        matcher = FILE_HEADER.matcher(response);
        if (!matcher.find()) return "";
        int start = matcher.end();
        while (start < response.length() && (response.charAt(start) == '\r' || response.charAt(start) == '\n')) start++;
        String remainder = response.substring(start);
        Matcher fenceStart = FENCE_START.matcher(remainder);
        if (fenceStart.lookingAt()) {
            String fenced = remainder.substring(fenceStart.end());
            Matcher fenceEnd = FENCE_END.matcher(fenced);
            if (fenceEnd.find()) return fenced.substring(0, fenceEnd.start()).strip();
            return "";
        }
        Matcher next = NEXT_ARTIFACT.matcher(remainder);
        int end = next.find() ? next.start() : remainder.length();
        return remainder.substring(0, end).strip();
    }
}
