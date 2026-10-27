package com.ml.tblandroidtxt;

import java.util.Locale;

/** Pure state-to-control mapping for the shared Translate run control bar. */
public final class RunControlModel {
    public enum State {
        READY,
        BLOCKED,
        PREPARING,
        STOPPING,
        UNKNOWN_DELIVERY,
        RUNNING,
        PAUSED,
        WORK_REMAINS,
        COMPLETED
    }

    public enum Action {
        NONE,
        START,
        STOP,
        REVIEW,
        PAUSE,
        RESUME,
        RETRY,
        OPEN_RESULT,
        MORE
    }

    public static final class Controls {
        public final State state;
        public final Action primaryAction;
        public final Action secondaryAction;
        public final String primaryLabel;
        public final String secondaryLabel;
        public final boolean primaryEnabled;
        public final String message;
        public final boolean hasOverflow;

        private Controls(State state, Action primaryAction, String primaryLabel,
                         boolean primaryEnabled, Action secondaryAction,
                         String secondaryLabel, String message, boolean hasOverflow) {
            this.state = state;
            this.primaryAction = primaryAction;
            this.primaryLabel = primaryLabel;
            this.primaryEnabled = primaryEnabled;
            this.secondaryAction = secondaryAction;
            this.secondaryLabel = secondaryLabel;
            this.message = message;
            this.hasOverflow = hasOverflow;
        }

        public String signature() {
            return state.name() + "|" + primaryAction.name() + "|" + primaryLabel
                    + "|" + primaryEnabled + "|" + secondaryAction.name() + "|"
                    + secondaryLabel + "|" + message + "|" + hasOverflow;
        }
    }

    private RunControlModel() {}

    /** Resolves the durable/runtime facts into one of the visible bar states. */
    public static State resolve(boolean active, String runtimeState, boolean pendingUnknown,
                                boolean pendingJob, int pendingFailedChunks,
                                boolean planReady, String blocker) {
        return resolve(active, runtimeState, pendingUnknown, pendingJob, pendingFailedChunks,
                planReady, blocker, true);
    }

    /**
     * Resolves the bar after checking that the pending job belongs to the
     * selection currently open on Translate. A different file/set remains a
     * context row, but cannot turn "Sẵn sàng" into "Dịch lại".
     */
    public static State resolve(boolean active, String runtimeState, boolean pendingUnknown,
                                boolean pendingJob, int pendingFailedChunks,
                                boolean planReady, String blocker,
                                boolean pendingMatchesSelection) {
        String state = runtimeState == null ? "" : runtimeState.trim().toUpperCase(Locale.ROOT);
        if (active) {
            if ("STOPPING".equals(state)) return State.STOPPING;
            if ("PAUSED".equals(state)) return State.PAUSED;
            if ("VALIDATING".equals(state) || "READY".equals(state)) return State.PREPARING;
            return State.RUNNING;
        }
        if (pendingMatchesSelection && pendingUnknown) return State.UNKNOWN_DELIVERY;
        if (pendingMatchesSelection && pendingJob && pendingFailedChunks > 0) return State.WORK_REMAINS;
        if (pendingMatchesSelection && pendingJob) return State.WORK_REMAINS;
        if ("COMPLETED".equals(state)) return State.COMPLETED;
        if (planReady && (blocker == null || blocker.trim().isEmpty())) return State.READY;
        return State.BLOCKED;
    }

    public static Controls controls(State state, int failedChunks) {
        State safe = state == null ? State.BLOCKED : state;
        int errors = Math.max(0, failedChunks);
        switch (safe) {
            case READY:
                return new Controls(safe, Action.START, "Bắt đầu", true, Action.NONE, "", "", true);
            case BLOCKED:
                return new Controls(safe, Action.START, "Bắt đầu", false, Action.NONE, "", "", true);
            case PREPARING:
                return new Controls(safe, Action.NONE, "", false, Action.STOP, "Dừng", "", false);
            case STOPPING:
                return new Controls(safe, Action.NONE, "", false, Action.NONE, "", "Đang dừng…", false);
            case UNKNOWN_DELIVERY:
                return new Controls(safe, Action.REVIEW, "Xem lại", true, Action.NONE, "", "", false);
            case RUNNING:
                return new Controls(safe, Action.PAUSE, "Tạm dừng", true, Action.STOP, "Dừng", "", true);
            case PAUSED:
                return new Controls(safe, Action.RESUME, "Tiếp tục", true, Action.STOP, "Dừng", "", false);
            case WORK_REMAINS:
                String label = errors > 0 ? "Dịch lại " + errors + " chunk lỗi" : "Tiếp tục";
                return new Controls(safe, errors > 0 ? Action.RETRY : Action.RESUME, label,
                        true, Action.NONE, "", "", true);
            case COMPLETED:
                return new Controls(safe, Action.OPEN_RESULT, "Mở kết quả", true, Action.RETRY, "Dịch lại", "", true);
            default:
                return new Controls(State.BLOCKED, Action.START, "Bắt đầu", false, Action.NONE, "", "", true);
        }
    }

    /** Stable, mode-independent mapping used by pure tests and both source modes. */
    public static Controls controlsForMode(State state, int failedChunks, boolean chapterSetMode) {
        // chapterSetMode is deliberately not part of the policy: TXT and Tập share one bar.
        return controls(state, failedChunks);
    }
}
