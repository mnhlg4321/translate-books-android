package com.ml.tblandroidtxt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** One log line as the phone's LogStore writes it; the time follows the default locale, as on the phone. */
public final class LogFormat {
    private LogFormat() {}

    public static String formatLine(String msg) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        return "[" + time + "] " + (msg == null ? "" : msg) + "\n";
    }
}
