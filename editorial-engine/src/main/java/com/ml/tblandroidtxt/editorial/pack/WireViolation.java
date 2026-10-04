package com.ml.tblandroidtxt.editorial.pack;

import java.util.regex.Pattern;

/** A parser failure with a stable code and an app-authored JSON path. */
public final class WireViolation extends IllegalArgumentException {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,95}");
    private static final Pattern PATH = Pattern.compile("(?:[A-Za-z][A-Za-z0-9]*|[0-9]+)(?:\\.(?:[A-Za-z][A-Za-z0-9]*|[0-9]+))*");

    private final String code;
    private final String path;

    private WireViolation(String code, String path) {
        super(safeCode(code));
        this.code = safeCode(code);
        this.path = safePath(path);
    }

    public static WireViolation at(String code, String path) { return new WireViolation(code, path); }

    public static WireViolation from(RuntimeException error, String fallbackCode, String fallbackPath) {
        if (error instanceof WireViolation violation) return violation;
        String message = error == null ? null : error.getMessage();
        if (message != null && CODE.matcher(message).matches()) return at(message, fallbackPath);
        return at(fallbackCode, fallbackPath);
    }

    public String code() { return code; }
    public String path() { return path; }

    public static String safeMessage(RuntimeException error, String fallbackCode) {
        WireViolation violation = from(error, fallbackCode, "root");
        return violation.code + ":" + violation.path;
    }

    private static String safeCode(String code) {
        return code != null && CODE.matcher(code).matches() ? code : "WIRE_PARSE_FAILED";
    }

    private static String safePath(String path) {
        return path != null && PATH.matcher(path).matches() ? path : "root";
    }
}
