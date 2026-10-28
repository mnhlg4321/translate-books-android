package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * RunSnapshot.FIELDS must name every Intent extra the old TranslatorService.broadcast() added: the 37 keys recorded at the
 * P1b B0 baseline. EXTRA_EFFECTIVE_COST_LIMIT_USD is an input extra (read, never broadcast).
 */
public class RunSnapshotKeysTest {
    private static final String[] B0_BROADCAST_KEYS = {
            TranslatorService.EXTRA_STATUS, TranslatorService.EXTRA_PROGRESS, TranslatorService.EXTRA_LOG,
            TranslatorService.EXTRA_PHASE, TranslatorService.EXTRA_FILE_NAME, TranslatorService.EXTRA_TOTAL_CHUNKS,
            TranslatorService.EXTRA_COMPLETED, TranslatorService.EXTRA_FAILED, TranslatorService.EXTRA_FALLBACKS,
            TranslatorService.EXTRA_TOTAL_TOKENS, TranslatorService.EXTRA_TOTAL_COST,
            TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, TranslatorService.EXTRA_ESTIMATED_TOTAL_COST,
            TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, TranslatorService.EXTRA_ELAPSED_MS,
            TranslatorService.EXTRA_REMAINING_MS, TranslatorService.EXTRA_PREVIEW, TranslatorService.EXTRA_CURRENT_CHUNK,
            TranslatorService.EXTRA_GLOSSARY_LOCKS, TranslatorService.EXTRA_PRONOUN_LOCKS,
            TranslatorService.EXTRA_LOCK_CHUNK, TranslatorService.EXTRA_LOCK_PHASE, TranslatorService.EXTRA_PREVIEW_CHUNK,
            TranslatorService.EXTRA_PREVIEW_FILE, TranslatorService.EXTRA_PREVIEW_TOTAL,
            TranslatorService.EXTRA_ACTIVE_JOB_ID, TranslatorService.EXTRA_STATE, TranslatorService.EXTRA_SESSION_TOKENS,
            TranslatorService.EXTRA_SESSION_COST, TranslatorService.EXTRA_SESSION_START_MS,
            TranslatorService.EXTRA_SESSION_END_MS, TranslatorService.EXTRA_PAUSED_AT_MS,
            TranslatorService.EXTRA_PAUSED_TOTAL_MS, TranslatorService.EXTRA_SET_TOTAL, TranslatorService.EXTRA_SET_DONE,
            TranslatorService.EXTRA_SET_FAILED, TranslatorService.EXTRA_SET_INDEX,
    };

    @Test public void snapshotFieldTableCoversEveryBroadcastKey() {
        HashSet<String> table = new HashSet<>();
        for (String[] row : RunSnapshot.FIELDS) table.add(row[0]);
        assertEquals(37, B0_BROADCAST_KEYS.length);
        assertEquals(37, RunSnapshot.FIELDS.length);
        assertEquals(new HashSet<>(java.util.Arrays.asList(B0_BROADCAST_KEYS)), table);
    }

    @Test public void everyExtraConstantIsEitherBroadcastOrTheCostLimitInput() throws Exception {
        List<String> extras = new ArrayList<>();
        for (Field f : TranslatorService.class.getFields()) {
            if (f.getName().startsWith("EXTRA_") && Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                extras.add((String) f.get(null));
            }
        }
        extras.remove(TranslatorService.EXTRA_EFFECTIVE_COST_LIMIT_USD);
        assertEquals(new HashSet<>(extras), new HashSet<>(java.util.Arrays.asList(B0_BROADCAST_KEYS)));
        assertTrue(extras.size() == 37);
    }

    @Test public void tableFieldNamesExistOnTheSnapshot() throws Exception {
        for (String[] row : RunSnapshot.FIELDS) RunSnapshot.class.getField(row[2]);
    }
}
