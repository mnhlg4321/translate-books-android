package com.ml.tblandroidtxt;

/** Single source of truth for the effective paid-request cap of one run. */
public final class CostLimitPolicy {
    public static final double MIN_SAFE_CAP_USD = 0.10;

    public static double defaultSafeCapUsd(double expectedPlanCostUsd) {
        double expected = Double.isNaN(expectedPlanCostUsd) || expectedPlanCostUsd < 0 ? 0 : expectedPlanCostUsd;
        return Math.max(MIN_SAFE_CAP_USD, expected * 3.0);
    }

    /** -1 means explicitly unlimited; positive values are the only caps the service should enforce. */
    public static double effectiveCapUsd(AppSettings settings, double expectedPlanCostUsd, boolean unlimited) {
        if (unlimited) return -1.0;
        if (settings != null && settings.stopOnCostLimit && settings.costLimitUsd > 0) return settings.costLimitUsd;
        return defaultSafeCapUsd(expectedPlanCostUsd);
    }

    private CostLimitPolicy() {}
}
