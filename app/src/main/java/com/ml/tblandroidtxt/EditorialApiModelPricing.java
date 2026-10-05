package com.ml.tblandroidtxt;

import java.math.BigDecimal;

/** Prices from the app's model catalog; a model with an unknown price falls back to the conservative rate so the cap still protects. */
public final class EditorialApiModelPricing implements EditorialApiRunService.Pricing {
    private static final BigDecimal MILLION = new BigDecimal(1_000_000);
    private final String provider;

    public EditorialApiModelPricing(String provider) { this.provider = provider == null ? "" : provider; }

    @Override public BigDecimal inputPerToken(String model) {
        ModelCatalog.ModelInfo info = ModelCatalog.findModelInfo(provider, model);
        if (info == null || !info.inputPriceKnown || !finite(info.inputPerMillion)) return EditorialApiRunService.CONSERVATIVE.inputPerToken(model);
        return BigDecimal.valueOf(info.inputPerMillion).divide(MILLION);
    }

    @Override public BigDecimal outputPerToken(String model) {
        ModelCatalog.ModelInfo info = ModelCatalog.findModelInfo(provider, model);
        if (info == null || !info.outputPriceKnown || !finite(info.outputPerMillion)) return EditorialApiRunService.CONSERVATIVE.outputPerToken(model);
        return BigDecimal.valueOf(info.outputPerMillion).divide(MILLION);
    }

    private static boolean finite(double value) { return Double.isFinite(value) && value >= 0; }
}
