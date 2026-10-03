package com.ml.tblandroidtxt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class EditorialP6GroupSpendLedgerTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void settledSpendSurvivesRestartAndEachEntryChainsToItsPredecessor() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("group.jsonl");
        EditorialP6GroupSpendLedger first = ledger(path, "0.50");
        first.reserve("call-1", "L1_RAW_DISCOVERY", new BigDecimal("0.20"));
        EditorialP6GroupSpendLedger.Snapshot settled = first.settle("call-1", new BigDecimal("0.08"));
        assertEquals(new BigDecimal("0.08"), settled.exposedUsd());

        EditorialP6GroupSpendLedger.Snapshot reopened = ledger(path, "0.50").inspect();
        assertEquals(new BigDecimal("0.08"), reopened.exposedUsd());
        assertEquals(new BigDecimal("0.08"), reopened.settledUsd());
        assertEquals(0, reopened.pendingCalls());
        assertEquals(2, reopened.entries());
        assertTrue(reopened.lastEntryHash().matches("[0-9a-f]{64}"));
    }

    @Test public void worstCaseIsReservedBeforeCallAndAnUnknownReservationBlocksAfterRestart() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("unknown.jsonl");
        EditorialP6GroupSpendLedger first = ledger(path, "0.10");
        first.reserve("call-unknown", "L2_EDIT", new BigDecimal("0.09"));
        try {
            ledger(path, "0.10").reserve("call-after-unknown", "L2_FINAL_READ", new BigDecimal("0.01"));
            fail("an unresolved call must prevent later provider dispatch");
        } catch (IllegalStateException expected) {
            assertEquals("P6_SPEND_UNKNOWN_PREDECESSOR", expected.getMessage());
        }
        assertEquals(1, ledger(path, "0.10").inspect().pendingCalls());
    }

    @Test public void capRejectsBeforeAppendingTheNextCall() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("cap.jsonl");
        EditorialP6GroupSpendLedger ledger = ledger(path, "0.10");
        ledger.reserve("call-1", "L2_EDIT", new BigDecimal("0.08"));
        ledger.settle("call-1", new BigDecimal("0.08"));
        long entriesBefore = Files.readAllLines(path).size();
        try {
            ledger.reserve("call-2", "L2_FINAL_READ", new BigDecimal("0.03"));
            fail("reservation would exceed the group cap");
        } catch (IllegalStateException expected) {
            assertEquals("P6_SPEND_GROUP_CAP_EXCEEDED", expected.getMessage());
        }
        assertEquals(entriesBefore, Files.readAllLines(path).size());
    }

    @Test public void modifiedLedgerBytesAreRejectedOnReopen() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("tampered.jsonl");
        EditorialP6GroupSpendLedger ledger = ledger(path, "0.50");
        ledger.reserve("call-1", "L1_RAW_DISCOVERY", new BigDecimal("0.20"));
        String body = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        Files.write(path, body.replace("L1_RAW_DISCOVERY", "L1_RECONCILE").getBytes(StandardCharsets.UTF_8));
        try {
            ledger(path, "0.50").inspect();
            fail("modified bytes must fail the hash-chain verification");
        } catch (IllegalStateException expected) {
            assertEquals("P6_SPEND_LEDGER_HASH_CHAIN_INVALID", expected.getMessage());
        }
    }

    private static EditorialP6GroupSpendLedger ledger(Path path, String cap) {
        return new EditorialP6GroupSpendLedger(path, "group-test", new BigDecimal(cap));
    }
}
