package com.ml.tblandroidtxt.editorial.api;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * States of a pair and of a run, the allowed moves, the run state derived from the pairs, and what to do about a pair found
 * half done after the process died. Pure; the app persists the states and the ledger in one database.
 */
public final class PairStates {
    public enum PairState {
        IMPORTED, RESERVE_FAILED, E_RESERVED, E_SENT, E_RECEIVED, STRUCTURE_BLOCKED, WARN_REVIEW, CHECK_PENDING, ACCEPTED, REJECTED, UNKNOWN, MISSING
    }

    public enum RunState { PREPARED, RUNNING, PAUSED, INCOMPLETE, UNKNOWN, FINAL_BLOCKED, FINAL_ELIGIBLE }

    /** What recovery does with a pair whose process died: nothing, give the reservation back at zero, or mark the pair unknown. */
    public enum Recovery { NONE, SETTLE_ZERO_NOT_DISPATCHED, MARK_UNKNOWN }

    private static final Map<PairState, Set<PairState>> MOVES = new EnumMap<>(PairState.class);

    static {
        MOVES.put(PairState.IMPORTED, EnumSet.of(PairState.E_RESERVED, PairState.RESERVE_FAILED, PairState.UNKNOWN, PairState.ACCEPTED));
        MOVES.put(PairState.E_RESERVED, EnumSet.of(PairState.E_SENT, PairState.IMPORTED));
        MOVES.put(PairState.E_SENT, EnumSet.of(PairState.E_RECEIVED, PairState.UNKNOWN));
        MOVES.put(PairState.E_RECEIVED, EnumSet.of(PairState.STRUCTURE_BLOCKED, PairState.WARN_REVIEW, PairState.ACCEPTED));
        MOVES.put(PairState.WARN_REVIEW, EnumSet.of(PairState.ACCEPTED, PairState.REJECTED));
        for (PairState terminal : new PairState[] {PairState.ACCEPTED, PairState.REJECTED, PairState.STRUCTURE_BLOCKED, PairState.UNKNOWN,
                PairState.MISSING, PairState.RESERVE_FAILED, PairState.CHECK_PENDING}) {
            MOVES.put(terminal, EnumSet.noneOf(PairState.class));
        }
    }

    private PairStates() { }

    public static boolean canMove(PairState from, PairState to) { return MOVES.get(from).contains(to); }

    public static boolean terminal(PairState state) { return MOVES.get(state).isEmpty(); }

    /** The state a received candidate takes from its structural result. */
    public static PairState afterReceive(StructuralGate.Status status) {
        switch (status) {
            case BLOCK: return PairState.STRUCTURE_BLOCKED;
            case WARN: return PairState.WARN_REVIEW;
            default: return PairState.ACCEPTED;
        }
    }

    /** States whose candidate can be merged: accepted, or kept with a warning. */
    public static boolean mergeable(PairState state) { return state == PairState.ACCEPTED || state == PairState.WARN_REVIEW; }

    public static int warnings(List<PairState> states) {
        int n = 0;
        for (PairState s : states) if (s == PairState.WARN_REVIEW) n++;
        return n;
    }

    /**
     * @param started whether any pair ever left IMPORTED
     * @param executing whether this process is running the pairs now
     * @param paused whether the person paused the run
     */
    public static RunState derive(List<PairState> states, boolean started, boolean executing, boolean paused) {
        boolean unknown = false;
        boolean pending = false;
        boolean blocked = false;
        boolean allImported = true;
        for (PairState s : states) {
            if (s != PairState.IMPORTED) allImported = false;
            switch (s) {
                case UNKNOWN: unknown = true; break;
                case IMPORTED: case E_RESERVED: case E_SENT: case RESERVE_FAILED: case MISSING: case CHECK_PENDING: case E_RECEIVED: pending = true; break;
                case STRUCTURE_BLOCKED: case REJECTED: blocked = true; break;
                default: break;
            }
        }
        if (states.isEmpty()) return RunState.PREPARED;
        if (executing) return RunState.RUNNING;
        if (unknown) return RunState.UNKNOWN;
        if (pending) {
            if (allImported && !started) return RunState.PREPARED;
            return paused ? RunState.PAUSED : RunState.INCOMPLETE;
        }
        return blocked ? RunState.FINAL_BLOCKED : RunState.FINAL_ELIGIBLE;
    }

    /**
     * What to do with a pair found in {@code persisted} when the app starts again.
     *
     * @param reservationOpen the ledger holds a reservation for the pair that was never settled
     */
    public static Recovery recover(PairState persisted, boolean reservationOpen) {
        switch (persisted) {
            case E_RESERVED: return Recovery.SETTLE_ZERO_NOT_DISPATCHED; // journal says reserved, never marked sent
            case E_SENT: return Recovery.MARK_UNKNOWN; // may have reached the provider
            case IMPORTED: return reservationOpen ? Recovery.MARK_UNKNOWN : Recovery.NONE; // reserved, died before the journal
            case E_RECEIVED: case ACCEPTED: case WARN_REVIEW: case STRUCTURE_BLOCKED: case REJECTED:
                return reservationOpen ? Recovery.MARK_UNKNOWN : Recovery.NONE; // a settled call never leaves a reservation open
            default: return Recovery.NONE;
        }
    }
}
