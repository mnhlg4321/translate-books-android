package com.ml.tblandroidtxt.editorial.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts only the explicitly delimited final text from a V5 chat turn. */
public final class V5FinalExtractor {
    private static final Pattern FINAL = Pattern.compile("<FINAL>(.*?)</FINAL>", Pattern.DOTALL);

    private V5FinalExtractor() { }

    public static String extract(String response) {
        if (response == null) return "";
        Matcher matcher = FINAL.matcher(response);
        if (!matcher.find()) return "";
        String value = matcher.group(1);
        return value == null ? "" : value.strip();
    }
}
