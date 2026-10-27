package com.ml.tblandroidtxt;

import java.util.HashSet;
import java.util.Set;

/**
 * Tokens and money spent by one run (one press of Start, every chapter and every retry together). The cost limit is checked against this
 * meter, not against the job being worked on, so a limit of USD 1 stops a 14-chapter set at USD 1 and not at USD 14. When a run continues
 * an earlier job, what that job already cost is loaded once.
 */
final class RunCostMeter {
    private long tokens;
    private double cost;
    private final Set<Long> preloaded = new HashSet<>();

    synchronized void reset() { tokens = 0; cost = 0; preloaded.clear(); }

    synchronized void addUsage(long usedTokens, double usedCost) {
        tokens += Math.max(0, usedTokens);
        if (!Double.isNaN(usedCost) && usedCost > 0) cost += usedCost;
    }

    /** Adds the spend a continued job already has, once per job for this run; returns whether it was added. */
    synchronized boolean preloadContinuedJob(long jobId, double jobCost) {
        if (jobId <= 0 || !preloaded.add(jobId)) return false;
        if (!Double.isNaN(jobCost) && jobCost > 0) cost += jobCost;
        return true;
    }

    synchronized long tokens() { return tokens; }

    synchronized double cost() { return cost; }

    /** True when the limit is on and the whole run has already spent it. */
    synchronized boolean limitReached(boolean stopOnCostLimit, double limitUsd) {
        return stopOnCostLimit && limitUsd > 0 && cost >= limitUsd;
    }
}
