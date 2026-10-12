package com.ml.tblandroidtxt;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The only door from the UI to a live L2/L3 chain. A run starts only when all of these hold:
 * the user confirmed the exact per-phase caps, the caps are valid, the settings can reach the qualified
 * route (so a setup error never burns a claimed attempt), nobody else is running the same chapter in this
 * process, and the durable state says the next stage was never attempted. A stage left CLAIMED or
 * RECOVERY_REQUIRED, or an already finished chapter, is refused here and is never retried automatically.
 * Pure Java over {@link Chain}, so the whole gate is covered by JVM tests.
 */
public final class EditorialChapterRunService {
    /** One chapter's durable chain as the service sees it; the real one wraps the coordinator. */
    public interface Chain {
        /** Read-only progress; never dispatches. */
        EditorialChapterFinalCoordinator.Inspection inspect();

        /** Null when the settings can reach the qualified route and a key is present; otherwise a typed code. */
        String preflightIssue();

        /** Runs or resumes the chain under exactly these caps. */
        EditorialChapterFinalCoordinator.Result run(EditorialChainBudgets budgets);
    }

    public record Outcome(boolean started, String reasonCode, EditorialChapterFinalCoordinator.Result result) { }

    private static final Set<String> ACTIVE = ConcurrentHashMap.newKeySet();

    private EditorialChapterRunService() { }

    public static String lockKey(long projectId, String chapterKey) {
        return projectId + ":" + chapterKey;
    }

    public static boolean isRunning(String lockKey) { return ACTIVE.contains(lockKey); }

    public static Outcome run(String lockKey, Chain chain, EditorialChainBudgets budgets, boolean userConfirmed) {
        if (!userConfirmed) return refused("RUN_NOT_CONFIRMED");
        if (chain == null || lockKey == null || lockKey.isEmpty()) return refused("RUN_CHAIN_MISSING");
        if (budgets == null || !budgets.valid()) return refused("RUN_BUDGET_INVALID");
        if (!ACTIVE.add(lockKey)) return refused("RUN_ALREADY_ACTIVE");
        try {
            String issue = chain.preflightIssue();
            if (issue != null) return refused("RUN_PREFLIGHT:" + issue);
            EditorialChapterProgress.Progress progress = chain.inspect().progress();
            boolean l1StartAllowed = budgets.includesL1()
                    && progress.next() == EditorialChapterProgress.NextAction.L1_REQUIRED;
            if (progress.next() != EditorialChapterProgress.NextAction.RUN_STAGE_WITH_AUTHORIZATION
                    && !l1StartAllowed) {
                return refused("RUN_NOT_ALLOWED:" + progress.reasonCode());
            }
            try {
                EditorialChapterFinalCoordinator.Result result = chain.run(budgets);
                return new Outcome(true, result.reasonCode(), result);
            } catch (RuntimeException error) {
                // The stage may be CLAIMED now; the next inspect() reports it and nothing re-dispatches it.
                return new Outcome(true, "RUN_UNEXPECTED_ERROR", null);
            }
        } finally {
            ACTIVE.remove(lockKey);
        }
    }

    private static Outcome refused(String reason) { return new Outcome(false, reason, null); }
}
