package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.EditorialPairModels.PairItem;
import com.ml.tblandroidtxt.EditorialPairModels.PairReservation;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.PairStates;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory twin of the SQLite pair store for JVM tests and the offline fixture runner; same rules, no Android. */
public final class InMemoryPairRunStore implements EditorialPairRunStore {
    private final Map<Long, PairRun> runs = new LinkedHashMap<>();
    private final Map<Long, List<PairItem>> items = new LinkedHashMap<>();
    private final Map<String, PairReservation> reservations = new LinkedHashMap<>();
    private final List<String> journal = new ArrayList<>();
    private long nextId = 1;
    public int writes;

    @Override public synchronized long insertRun(PairRun run, List<PairItem> newItems) {
        run.id = nextId++;
        run.createdAt = run.updatedAt = System.currentTimeMillis();
        runs.put(run.id, run.copy());
        List<PairItem> copies = new ArrayList<>();
        for (PairItem i : newItems) {
            PairItem c = i.copy();
            c.runId = run.id;
            c.id = nextId++;
            copies.add(c);
        }
        items.put(run.id, copies);
        writes++;
        return run.id;
    }

    @Override public synchronized PairRun getRun(long id) { PairRun r = runs.get(id); return r == null ? null : r.copy(); }

    @Override public synchronized void updateRun(PairRun run) { run.updatedAt = System.currentTimeMillis(); runs.put(run.id, run.copy()); writes++; }

    @Override public synchronized PairRun latestRun(long comboId) {
        PairRun best = null;
        for (PairRun r : runs.values()) if (r.comboId == comboId && (best == null || r.id > best.id)) best = r;
        return best == null ? null : best.copy();
    }

    @Override public synchronized List<PairRun> runsOf(long comboId) {
        List<PairRun> out = new ArrayList<>();
        for (PairRun r : runs.values()) if (r.comboId == comboId) out.add(r.copy());
        return out;
    }

    @Override public synchronized List<PairItem> items(long runId) {
        List<PairItem> out = new ArrayList<>();
        for (PairItem i : items.getOrDefault(runId, List.of())) out.add(i.copy());
        return out;
    }

    @Override public synchronized PairItem item(long runId, String pairId) {
        for (PairItem i : items.getOrDefault(runId, List.of())) if (i.pairId.equals(pairId)) return i.copy();
        return null;
    }

    @Override public synchronized void deleteRun(long id) {
        runs.remove(id);
        items.remove(id);
        reservations.values().removeIf(r -> r.runId == id);
    }

    private PairItem live(long runId, String pairId) {
        for (PairItem i : items.getOrDefault(runId, List.of())) if (i.pairId.equals(pairId)) return i;
        throw new IllegalStateException("no item " + pairId);
    }

    @Override public synchronized boolean reserve(PairReservation reservation, BigDecimal capUsd) {
        if (reservations.containsKey(reservation.callId)) throw new IllegalStateException("RESERVATION_EXISTS " + reservation.callId);
        if (exposure(reservation.runId).add(reservation.worstUsd).compareTo(capUsd) > 0) return false;
        PairReservation copy = new PairReservation();
        copy.callId = reservation.callId; copy.runId = reservation.runId; copy.pairId = reservation.pairId; copy.worstUsd = reservation.worstUsd;
        reservations.put(copy.callId, copy);
        journal.add(reservation.runId + " " + reservation.pairId + " RESERVE");
        writes++;
        return true;
    }

    @Override public synchronized void setItemState(long runId, String pairId, PairState from, PairState to, int attempt, String requestId, String journalEvent) {
        PairItem i = live(runId, pairId);
        if (i.state != from) throw new IllegalStateException("EXPECTED " + from + " BUT " + i.state);
        if (!PairStates.canMove(from, to)) throw new IllegalStateException("MOVE_NOT_ALLOWED " + from + "->" + to);
        i.state = to;
        i.attempt = attempt;
        if (requestId != null && !requestId.isEmpty()) i.requestId = requestId;
        journal.add(runId + " " + pairId + " " + journalEvent);
        writes++;
    }

    @Override public synchronized void bumpAttempt(long runId, String pairId, int attempt, String requestId) {
        PairItem i = live(runId, pairId);
        if (i.state != PairState.E_SENT) throw new IllegalStateException("BUMP_ONLY_WHEN_SENT");
        i.attempt = attempt;
        i.requestId = requestId;
        journal.add(runId + " " + pairId + " RETRY_" + attempt);
        writes++;
    }

    @Override public synchronized void commitReceived(long runId, PairItem received, String callId, BigDecimal settledUsd, String settleReason) {
        PairItem i = live(runId, received.pairId);
        if (i.state != PairState.E_SENT) throw new IllegalStateException("RECEIVE_ONLY_WHEN_SENT " + i.state);
        PairReservation r = reservations.get(callId);
        if (r == null || !r.open()) throw new IllegalStateException("SETTLE_TWICE_OR_MISSING " + callId);
        r.state = settledUsd.signum() == 0 ? PairReservation.SETTLED_ZERO : PairReservation.SETTLED;
        r.settledUsd = settledUsd;
        r.reason = settleReason;
        PairState target = received.state;
        if (!PairStates.canMove(PairState.E_RECEIVED, target)) throw new IllegalStateException("BAD_TARGET " + target);
        i.state = target;
        i.candidateText = received.candidateText; i.responseText = received.responseText; i.gateJson = received.gateJson;
        i.responseHash = received.responseHash; i.calls = received.calls; i.inputTokens = received.inputTokens; i.outputTokens = received.outputTokens;
        i.usd = received.usd; i.costKnown = received.costKnown; i.error = received.error; i.attempt = received.attempt;
        i.updatedAt = System.currentTimeMillis();
        journal.add(runId + " " + received.pairId + " RECEIVED " + target);
        writes++;
    }

    @Override public synchronized void commitFailedNotBilled(long runId, PairItem failed) {
        PairItem i = live(runId, failed.pairId);
        if (i.state != PairState.E_SENT) throw new IllegalStateException("FAIL_ONLY_WHEN_SENT " + i.state);
        i.state = PairState.STRUCTURE_BLOCKED;
        i.gateJson = failed.gateJson; i.calls = failed.calls; i.inputTokens = failed.inputTokens; i.outputTokens = failed.outputTokens;
        i.error = failed.error; i.usd = BigDecimal.ZERO; i.costKnown = true; i.attempt = failed.attempt;
        journal.add(runId + " " + failed.pairId + " FAILED_NOT_BILLED");
        writes++;
    }

    @Override public synchronized void commitUnknown(long runId, String pairId, String error) {
        PairItem i = live(runId, pairId);
        i.state = PairState.UNKNOWN;
        i.error = error;
        i.costKnown = false;
        journal.add(runId + " " + pairId + " UNKNOWN");
        writes++;
    }

    @Override public synchronized void settleZero(String callId, String reason) {
        PairReservation r = reservations.get(callId);
        if (r == null || !r.open()) throw new IllegalStateException("SETTLE_TWICE_OR_MISSING " + callId);
        r.state = PairReservation.SETTLED_ZERO;
        r.settledUsd = BigDecimal.ZERO;
        r.reason = reason;
        writes++;
    }

    @Override public synchronized void revertToImported(long runId, String pairId, String reason) {
        PairItem i = live(runId, pairId);
        for (PairReservation r : reservations.values()) {
            if (r.runId == runId && r.pairId.equals(pairId) && r.open()) { r.state = PairReservation.SETTLED_ZERO; r.settledUsd = BigDecimal.ZERO; r.reason = reason; }
        }
        i.state = PairState.IMPORTED;
        i.generation++;
        journal.add(runId + " " + pairId + " REVERTED " + reason);
        writes++;
    }

    @Override public synchronized void resolveWarning(long runId, String pairId, PairState to, String note) {
        PairItem i = live(runId, pairId);
        if (i.state != PairState.WARN_REVIEW || !PairStates.canMove(PairState.WARN_REVIEW, to)) throw new IllegalStateException("NOT_A_WARNING");
        i.state = to;
        journal.add(runId + " " + pairId + " WARNING_" + to + " " + note);
        writes++;
    }

    @Override public synchronized List<PairReservation> openReservations(long runId) {
        List<PairReservation> out = new ArrayList<>();
        for (PairReservation r : reservations.values()) if (r.runId == runId && r.open()) out.add(r);
        return out;
    }

    @Override public synchronized BigDecimal exposure(long runId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PairReservation r : reservations.values()) {
            if (r.runId != runId) continue;
            sum = sum.add(r.open() ? r.worstUsd : r.settledUsd);
        }
        return sum;
    }

    @Override public synchronized List<String> journal(long runId) {
        List<String> out = new ArrayList<>();
        for (String line : journal) if (line.startsWith(runId + " ")) out.add(line);
        return out;
    }

    /** Test access to every reservation of the run, settled or not. */
    public synchronized List<PairReservation> allReservations(long runId) {
        List<PairReservation> out = new ArrayList<>();
        for (PairReservation r : reservations.values()) if (r.runId == runId) out.add(r);
        return out;
    }
}
