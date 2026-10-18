package com.ml.tblandroidtxt;

/** Internal RAW files are addressed as {@code tblraw://<id>}; they need no persisted SAF grant. */
final class RawUri {
    static final String SCHEME = "tblraw";
    static final String PREFIX = SCHEME + "://";

    static String of(String id) { return PREFIX + id; }

    static boolean isRaw(String uri) { return uri != null && uri.startsWith(PREFIX) && uri.length() > PREFIX.length(); }

    /** The RAW id, or an empty string when {@code uri} is not an internal RAW address. Ids are restricted so they cannot escape the folder. */
    static String idOf(String uri) {
        if (!isRaw(uri)) return "";
        String id = uri.substring(PREFIX.length());
        return id.matches("[A-Za-z0-9-]{1,64}") ? id : "";
    }

    private RawUri() {}
}
