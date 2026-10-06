package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wraps a pair store and "kills the process" at one named window: an {@link Error} is thrown before or after the operation, the
 * way a death would cut between ledger, journal, answer and commit. The data written so far stays in the wrapped store, so a new
 * service on the same store is a restart. Host simulation only: it proves the journal logic, not a device.
 */
public final class FaultyPairRunStore implements EditorialPairRunStore {
    public static final class SimulatedCrash extends Error {
        public SimulatedCrash(String where) { super(where); }
    }

    private final EditorialPairRunStore inner;
    private final Set<String> before = new HashSet<>();
    private final Set<String> after = new HashSet<>();

    public FaultyPairRunStore(EditorialPairRunStore inner) { this.inner = inner; }

    /** Names: {@code reserve}, {@code state:E_RESERVED}, {@code state:E_SENT}, {@code commitReceived}, {@code commitUnknown}, {@code settleZero}. */
    public FaultyPairRunStore crashBefore(String op) { before.add(op); return this; }

    public FaultyPairRunStore crashAfter(String op) { after.add(op); return this; }

    public void disarm() { before.clear(); after.clear(); }

    private void pre(String op) { if (before.remove(op)) throw new SimulatedCrash("before " + op); }

    private void post(String op) { if (after.remove(op)) throw new SimulatedCrash("after " + op); }

    @Override public long insertRun(PairRun run, List<PairItem> items) { return inner.insertRun(run, items); }

    @Override public PairRun getRun(long id) { return inner.getRun(id); }

    @Override public void updateRun(PairRun run) { inner.updateRun(run); }

    @Override public PairRun latestRun(long comboId) { return inner.latestRun(comboId); }

    @Override public List<PairRun> runsOf(long comboId) { return inner.runsOf(comboId); }

    @Override public List<PairItem> items(long runId) { return inner.items(runId); }

    @Override public PairItem item(long runId, String pairId) { return inner.item(runId, pairId); }

    @Override public void deleteRun(long id) { inner.deleteRun(id); }

    @Override public boolean reserve(PairReservation reservation, BigDecimal capUsd) {
        pre("reserve");
        boolean ok = inner.reserve(reservation, capUsd);
        post("reserve");
        return ok;
    }

    @Override public void setItemState(long runId, String pairId, PairState from, PairState to, int attempt, String requestId, String journalEvent) {
        pre("state:" + to);
        inner.setItemState(runId, pairId, from, to, attempt, requestId, journalEvent);
        post("state:" + to);
    }

    @Override public void bumpAttempt(long runId, String pairId, int attempt, String requestId) { inner.bumpAttempt(runId, pairId, attempt, requestId); }

    @Override public void commitReceived(long runId, PairItem received, String callId, BigDecimal settledUsd, String settleReason) {
        pre("commitReceived");
        inner.commitReceived(runId, received, callId, settledUsd, settleReason);
        post("commitReceived");
    }

    @Override public void commitFailedNotBilled(long runId, PairItem failed) { inner.commitFailedNotBilled(runId, failed); }

    @Override public void commitUnknown(long runId, String pairId, String error) {
        pre("commitUnknown");
        inner.commitUnknown(runId, pairId, error);
        post("commitUnknown");
    }

    @Override public void settleZero(String callId, String reason) {
        pre("settleZero");
        inner.settleZero(callId, reason);
        post("settleZero");
    }

    @Override public void revertToImported(long runId, String pairId, String reason) { inner.revertToImported(runId, pairId, reason); }

    @Override public void resolveWarning(long runId, String pairId, PairState to, String note) { inner.resolveWarning(runId, pairId, to, note); }

    @Override public List<PairReservation> openReservations(long runId) { return inner.openReservations(runId); }

    @Override public BigDecimal exposure(long runId) { return inner.exposure(runId); }

    @Override public List<String> journal(long runId) { return inner.journal(runId); }
}
