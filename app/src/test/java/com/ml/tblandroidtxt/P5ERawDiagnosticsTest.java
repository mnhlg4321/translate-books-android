package com.ml.tblandroidtxt;

import org.json.JSONException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class P5ERawDiagnosticsTest {
    @Test public void tokenKeepsVocabularyAndBoundsLength() {
        assertEquals("RAW_WIRE_BYTE_LIMIT_EXCEEDED", P5ERawDiagnostics.token("RAW_WIRE_BYTE_LIMIT_EXCEEDED"));
        assertEquals("a_b_c", P5ERawDiagnostics.token("a b\nc"));
        assertEquals("null", P5ERawDiagnostics.token(null));
        assertEquals(120, P5ERawDiagnostics.token("x".repeat(500)).length());
    }

    @Test public void describeKeepsOnlyFixedCodeMessagesOfOurOwnExceptions() {
        assertEquals("IllegalArgumentException:RAW_WIRE_ITEM_ID_INVALID",
                P5ERawDiagnostics.describe(new IllegalArgumentException("RAW_WIRE_ITEM_ID_INVALID")));
        assertEquals("IllegalArgumentException:findings_limit_exceeded",
                P5ERawDiagnostics.describe(new IllegalArgumentException("findings limit exceeded")));
    }

    @Test public void describeSuppressesLibraryAndFreeTextMessages() {
        String quoted = "Unterminated string at 12 of {\"secret\":\"book text\"}";
        String json = P5ERawDiagnostics.describe(new JSONException(quoted));
        assertEquals("JSONException:message_suppressed", json);
        assertFalse(json.contains("book"));
        String free = P5ERawDiagnostics.describe(new IllegalArgumentException("chapter says \"hello\" {x}"));
        assertEquals("IllegalArgumentException:message_suppressed", free);
        assertTrue(P5ERawDiagnostics.describe(null).startsWith("none"));
    }

    @Test public void loggingNeverThrowsWithoutAndroidRuntime() {
        P5ERawDiagnostics.chat("stop", 1, 2, 3, 4, 5, true, true, "0.001");
        P5ERawDiagnostics.parseRejected(new IllegalArgumentException("RAW_WIRE_SCHEMA_INVALID"), 3);
        P5ERawDiagnostics.stop("P5E_FRESH_RAW_AUTHORIZATION_MISMATCH");
        P5ERawDiagnostics.dispatch(null);
    }
}
