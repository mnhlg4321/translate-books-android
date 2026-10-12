package com.ml.tblandroidtxt;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialRequiredInputRoleContract;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Package-private SQLite and canonical-row helpers; raw connections stay inside this package. */
final class EditorialIdentityDaoSupport {
    private EditorialIdentityDaoSupport() { }

    static boolean hasRow(SQLiteDatabase db, String table, String column, String value) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + table + " WHERE " + column + "=? LIMIT 1",
                new String[]{value})) {
            return cursor.moveToFirst();
        }
    }

    static EditorialRequiredInputRoleContract readRoleContract(String canonical) {
        Object parsed = EditorialCanonicalJson.parse(canonical.getBytes(StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> map)) throw new IllegalArgumentException("required role JSON must be object");
        Object version = map.get("contractVersion");
        Object roles = map.get("requiredRoles");
        if (!(version instanceof String) || !(roles instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("required role JSON is incomplete");
        }
        Set<String> roleSet = new HashSet<>();
        for (Object role : list) {
            if (!(role instanceof String)) throw new IllegalArgumentException("required role is not text");
            roleSet.add((String) role);
        }
        return new EditorialRequiredInputRoleContract((String) version, roleSet);
    }

    static String safe(String value) { return value == null ? "" : value; }

    static <T> EditorialIdentityAppendResult<T> result(
            EditorialIdentityPersistenceCode code, T value, String identity, String detail) {
        return EditorialIdentityAppendResult.of(code, value, identity, detail);
    }
}
