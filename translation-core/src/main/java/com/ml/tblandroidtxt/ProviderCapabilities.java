package com.ml.tblandroidtxt;

import java.util.Locale;

public final class ProviderCapabilities {
    public final boolean usageDetails, cachedTokenReporting, promptCaching, retryAfter, batch;
    private ProviderCapabilities(boolean usageDetails, boolean cachedTokenReporting, boolean promptCaching, boolean retryAfter, boolean batch) {
        this.usageDetails=usageDetails; this.cachedTokenReporting=cachedTokenReporting; this.promptCaching=promptCaching; this.retryAfter=retryAfter; this.batch=batch;
    }
    public static ProviderCapabilities forSettings(AppSettings s) {
        String p=(s==null?"":s.provider+" "+s.baseUrl).toLowerCase(Locale.ROOT);
        if(p.contains("openrouter")) return new ProviderCapabilities(true,true,true,true,false);
        if(p.contains("openai")) return new ProviderCapabilities(true,true,true,true,true);
        if(p.contains("deepseek")) return new ProviderCapabilities(true,true,true,true,false);
        return new ProviderCapabilities(false,false,false,false,false);
    }
    public String summary() { return "usage="+usageDetails+", cachedUsage="+cachedTokenReporting+", promptCache="+promptCaching+", retryAfter="+retryAfter+", batch="+batch; }
}
