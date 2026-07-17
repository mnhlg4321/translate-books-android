package com.ml.tblandroidtxt;
public final class BudgetPolicy {
    public static Decision beforePaidRequest(AppSettings s,double actualCost,double retryCost,int paidRetries,boolean pricingKnown){
        if(s==null)return new Decision(true,"");if(s.stopWhenPricingUnknown&&!pricingKnown)return new Decision(false,"PRICING_UNKNOWN");
        if(s.stopOnCostLimit&&s.costLimitUsd>0&&actualCost>=s.costLimitUsd)return new Decision(false,"HARD_JOB_BUDGET");
        if(s.maxRetryCostUsd>0&&retryCost>=s.maxRetryCostUsd)return new Decision(false,"MAX_RETRY_COST");
        if(s.maxPaidRetries>0&&paidRetries>=s.maxPaidRetries)return new Decision(false,"MAX_PAID_RETRIES");return new Decision(true,"");
    }
    public static class Decision{public final boolean allowed;public final String reason;Decision(boolean a,String r){allowed=a;reason=r;}}private BudgetPolicy(){}
}
