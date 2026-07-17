package com.ml.tblandroidtxt;

/** Explicit lifecycle for a translation execution session. */
public final class TranslationJobState {
    public enum State { IDLE, VALIDATING, READY, RUNNING, PAUSED, COMPLETED, FAILED }

    public static boolean canTransition(State from, State to) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        switch (from) {
            case IDLE: return to == State.VALIDATING;
            case VALIDATING: return to == State.READY || to == State.FAILED;
            case READY: return to == State.RUNNING || to == State.FAILED;
            case RUNNING: return to == State.PAUSED || to == State.COMPLETED || to == State.FAILED;
            case PAUSED: return to == State.RUNNING || to == State.FAILED;
            case COMPLETED:
            case FAILED: return to == State.IDLE || to == State.VALIDATING;
            default: return false;
        }
    }

    public static long remainingMs(State state, int completed, int total, long elapsedMs) {
        if (state == State.COMPLETED) return 0L;
        if (state != State.RUNNING || completed <= 0 || total <= 0) return -1L;
        long perChunk = Math.max(1L, elapsedMs) / completed;
        return Math.max(0L, perChunk * Math.max(0, total - completed));
    }

    private TranslationJobState() {}
}
