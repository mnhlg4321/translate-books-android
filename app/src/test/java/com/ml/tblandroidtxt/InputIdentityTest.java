package com.ml.tblandroidtxt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A file with Windows line endings must still match the hash the preparation plan stored (of the normalized text). */
public class InputIdentityTest {
    private static final String LF = "第一行\n第二行\n\n第三行\n";
    private static final String CRLF = LF.replace("\n", "\r\n");
    private static final String CR = LF.replace("\n", "\r");

    private static String planHash(String text) { return HashUtil.sha256(Chunker.normalizeSource(text)); }

    @Test public void crlfFileMatchesThePlanHash() {
        assertEquals(InputIdentity.Match.NORMALIZED, InputIdentity.match(planHash(CRLF), CRLF));
        assertEquals(InputIdentity.Match.NORMALIZED, InputIdentity.match(planHash(CRLF), CR));
        assertEquals(InputIdentity.Match.RAW, InputIdentity.match(planHash(LF), LF));
    }

    @Test public void aBomBeforeCrlfStillMatchesAfterTheReaderDroppedIt() {
        // FileUtil drops the BOM on every read, so both the plan and resume hash the same text without it.
        String read = "﻿第一行\r\n第二行".substring(1);
        assertTrue(InputIdentity.matches(planHash("第一行\r\n第二行"), read));
    }

    @Test public void aHashOfTheRawTextStillMatches() {
        assertEquals(InputIdentity.Match.RAW, InputIdentity.match(HashUtil.sha256(CRLF), CRLF));
    }

    @Test public void changedContentIsStillAConflict() {
        assertEquals(InputIdentity.Match.NONE, InputIdentity.match(planHash(CRLF), CRLF.replace("第三行", "第四行")));
        assertFalse(InputIdentity.matches(planHash(LF), LF + "追加"));
    }

    @Test public void emptyStoredHashIsNotAConflict() {
        assertTrue(InputIdentity.matches("", "anything"));
        assertTrue(InputIdentity.matches(null, "anything"));
    }
}
