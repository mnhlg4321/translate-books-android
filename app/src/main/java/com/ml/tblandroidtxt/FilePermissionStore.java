package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.UriPermission;

import java.util.List;

/**
 * Small SAF permission inspector for Android 11+ file workflows.
 * It does not own permissions; Android's ContentResolver does. This class only
 * summarizes persisted grants so UI/logs can explain why a file may fail after restart.
 */
public class FilePermissionStore {
    public static String summary(Context context) {
        if (context == null) return "SAF permissions: —";
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        int read = 0, write = 0;
        for (UriPermission p : permissions) {
            if (p == null) continue;
            if (p.isReadPermission()) read++;
            if (p.isWritePermission()) write++;
        }
        return "SAF permissions: " + permissions.size() + " persisted grant(s), read=" + read + ", write=" + write;
    }
}
