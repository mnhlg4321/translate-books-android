package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence of chunk-pair runs. The operations are the units of the dispatch journal: the ledger and the item state live in
 * one database, so the answer, its settlement and the new state commit together, and every window between two operations is
 * one a restart can recognise ({@code PairStates.recover}).
 */
public interface EditorialPairRunStore {
    /** Stores the run and its items in one step; returns the run id. */
    long insertRun(PairRun run, List<PairItem> items);

    PairRun getRun(long id);

    void updateRun(PairRun run);

    PairRun latestRun(long comboId);

    List<PairRun> runsOf(long comboId);

    List<PairItem> items(long runId);

    PairItem item(long runId, String pairId);

    void deleteRun(long id);

    // ---- ledger and journal, in dispatch order

    /** Opens a reservation unless settled + open + worst would exceed the cap. {@code false} = refused, nothing written. */
    boolean reserve(PairReservation reservation, BigDecimal capUsd);

    /** Moves the item between two states the contract allows and writes the journal line. */
    void setItemState(long runId, String pairId, PairState from, PairState to, int attempt, String requestId, String journalEvent);

    /** Commits a zero-call, unchanged DRAFT pair whose deterministic scan found no repair point. */
    void commitNoFixPoints(long runId, String pairId, String unchangedText, String detailsJson);

    /** Another attempt of an item already in E_SENT after a call that provably never billed. */
    void bumpAttempt(long runId, String pairId, int attempt, String requestId);

    /** The answer, the new item state and the settlement of its reservation in one transaction. */
    void commitReceived(long runId, PairItem received, String callId, BigDecimal settledUsd, String settleReason);

    /** Every attempt provably never billed and none succeeded: E_SENT becomes STRUCTURE_BLOCKED with the failure recorded. */
    void commitFailedNotBilled(long runId, PairItem failed);

    /** The outcome of the call cannot be told: the item becomes UNKNOWN and any open reservation stays open. */
    void commitUnknown(long runId, String pairId, String error);

    /** The request provably never left: closes the reservation at zero. */
    void settleZero(String callId, String reason);

    /** Settles every open reservation of the pair at zero and puts the item back to IMPORTED (recovery of E_RESERVED). */
    void revertToImported(long runId, String pairId, String reason);

    /** Resolution of a warning by a person: WARN_REVIEW to ACCEPTED or REJECTED. */
    void resolveWarning(long runId, String pairId, PairState to, String note);

    List<PairReservation> openReservations(long runId);

    /** Settled amounts plus the worst case of every open reservation of the run. */
    BigDecimal exposure(long runId);

    List<String> journal(long runId);
}
