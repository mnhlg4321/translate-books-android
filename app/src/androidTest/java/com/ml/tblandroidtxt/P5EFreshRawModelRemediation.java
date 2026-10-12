package com.ml.tblandroidtxt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test-only, fail-closed decision logic for the A4 persisted-model correction.
 * It never owns a Context or a SharedPreferences instance: the instrumented
 * adapter performs the one-key commit, while this class verifies the plan and
 * the post-write invariants without exposing preference values.
 */
final class P5EFreshRawModelRemediation {
    static final String MODEL_KEY = "model";
    static final String EXPECTED_MODEL = "openai/gpt-5.6-luna";

    private P5EFreshRawModelRemediation() { }

    static Plan plan(Map<String, ?> before, boolean optedIn, boolean identityMatches) {
        Map<String, ?> safeBefore = before == null
                ? Collections.emptyMap() : Collections.unmodifiableMap(
                        new LinkedHashMap<>(before));
        if (!optedIn) return new Plan(Status.OPT_IN_REQUIRED, safeBefore, false);
        if (!identityMatches) return new Plan(Status.IDENTITY_MISMATCH, safeBefore, false);
        boolean alreadyCorrect = EXPECTED_MODEL.equals(safeBefore.get(MODEL_KEY));
        return new Plan(alreadyCorrect ? Status.NO_OP : Status.WRITE_REQUIRED,
                safeBefore, !alreadyCorrect);
    }

    static Result complete(Plan plan, boolean writeCommitted, Map<String, ?> after) {
        if (plan == null) return Result.failed(Status.INVALID_PLAN, false, false, false);
        if (plan.status == Status.OPT_IN_REQUIRED || plan.status == Status.IDENTITY_MISMATCH) {
            return Result.failed(plan.status, false, false, false);
        }
        Map<String, ?> safeAfter = after == null ? Collections.emptyMap() : after;
        if (plan.writeRequired && !writeCommitted) {
            return Result.failed(Status.WRITE_FAILED, false, true, false);
        }
        if (!EXPECTED_MODEL.equals(safeAfter.get(MODEL_KEY))) {
            return Result.failed(Status.READBACK_FAILED, false, plan.writeRequired, false);
        }
        if (!otherSettingsUnchanged(plan.before, safeAfter)) {
            return Result.failed(Status.OTHER_SETTINGS_CHANGED, false, plan.writeRequired, false);
        }
        return new Result(Status.APPLIED, true, plan.writeRequired, true);
    }

    static boolean otherSettingsUnchanged(Map<String, ?> before, Map<String, ?> after) {
        Map<String, ?> left = before == null ? Collections.emptyMap() : before;
        Map<String, ?> right = after == null ? Collections.emptyMap() : after;
        for (String key : left.keySet()) {
            if (MODEL_KEY.equals(key)) continue;
            if (!java.util.Objects.equals(left.get(key), right.get(key))) return false;
        }
        for (String key : right.keySet()) {
            if (MODEL_KEY.equals(key)) continue;
            if (!java.util.Objects.equals(left.get(key), right.get(key))) return false;
        }
        return true;
    }

    enum Status {
        OPT_IN_REQUIRED,
        IDENTITY_MISMATCH,
        WRITE_REQUIRED,
        NO_OP,
        WRITE_FAILED,
        READBACK_FAILED,
        OTHER_SETTINGS_CHANGED,
        INVALID_PLAN,
        APPLIED
    }

    static final class Plan {
        final Status status;
        final Map<String, ?> before;
        final boolean writeRequired;

        private Plan(Status status, Map<String, ?> before, boolean writeRequired) {
            this.status = status;
            this.before = before;
            this.writeRequired = writeRequired;
        }
    }

    static final class Result {
        final Status status;
        final boolean success;
        final boolean writeAttempted;
        final boolean otherSettingsUnchanged;

        private Result(Status status, boolean success, boolean writeAttempted,
                        boolean otherSettingsUnchanged) {
            this.status = status;
            this.success = success;
            this.writeAttempted = writeAttempted;
            this.otherSettingsUnchanged = otherSettingsUnchanged;
        }

        private static Result failed(Status status, boolean success, boolean writeAttempted,
                                     boolean otherSettingsUnchanged) {
            return new Result(status, success, writeAttempted, otherSettingsUnchanged);
        }
    }
}
