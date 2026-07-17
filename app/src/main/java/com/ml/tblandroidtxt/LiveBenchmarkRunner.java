package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Service-owned paid A/B runner. It only runs after an explicit UI budget confirmation. */
public final class LiveBenchmarkRunner {
    public interface CancelChecker { boolean cancelled(); }
    public interface Events { void log(String message); }
    public static class Result { public long sessionId; public double spentUsd; public int samples, requests; }

    public static Result run(TranslationRepository repo, long jobId, double budgetUsd, CancelChecker cancel, Events events) throws Exception {
        if (budgetUsd <= 0) throw new IllegalArgumentException("Benchmark budget must be greater than zero");
        TranslationRepository.Job job=repo.getJob(jobId); if(job==null) throw new IllegalArgumentException("Job not found: "+jobId);
        AppSettings original=AppValidator.normalize(SettingsStore.fromJson(job.settingsJson));
        String validation=AppValidator.validateForTranslation(original); if(validation!=null) throw new IllegalArgumentException(validation);
        List<TranslationRepository.ChunkRow> all=repo.getChunkRows(jobId); if(all.isEmpty()) throw new IllegalArgumentException("Job has no chunks");
        List<TranslationRepository.ChunkRow> samples=selectSamples(all);
        ModelCatalog.ModelInfo pricing=ModelCatalog.findModelInfo(original.provider,original.model);
        if(pricing==null || !pricing.hasPricing()) throw new IllegalArgumentException("Chưa có dữ liệu giá cho model benchmark");
        int sharedMaxOutput=original.maxOutputTokens;
        double worst=estimateWorst(samples,original,pricing,sharedMaxOutput);
        if(worst>budgetUsd) throw new IllegalArgumentException("Benchmark worst-case "+CostEstimator.money(worst)+" exceeds budget "+CostEstimator.money(budgetUsd));
        long session=repo.createBenchmarkSession(jobId,original,budgetUsd,samples.size()); Result result=new Result();result.sessionId=session;result.samples=samples.size();
        try {
            for(int position=0;position<samples.size();position++){
                TranslationRepository.ChunkRow row=samples.get(position); String previous=previousOutput(all,row.index);
                // Alternate execution order to reduce provider load/order bias.
                String[] order=position%2==0?new String[]{"full","optimized"}:new String[]{"optimized","full"};
                for(String pipeline:order){
                    if(cancel!=null&&cancel.cancelled()) throw new InterruptedException("Benchmark cancelled");
                    AppSettings settings=original.copy();settings.optimizationPreset=pipeline.equals("full")?"full":"balanced";
                    Chunk chunk=new Chunk(row.index,"",row.source,""); PromptPlan plan=PromptPlan.forTranslation(chunk,previous,settings);
                    double requestWorst=ModelCatalog.usageCost(pricing,plan.totalInputTokens,0,sharedMaxOutput);
                    if(result.spentUsd+requestWorst>budgetUsd) throw new IllegalStateException("Budget guard stopped before next request");
                    String blind=(position%2==0)==pipeline.equals("full")?"X":"Y"; long started=System.currentTimeMillis();
                    OpenAICompatibleClient.ChatResult chat=OpenAICompatibleClient.chatWithUsage(settings,plan.prompt,sharedMaxOutput,"benchmark:"+session+":"+row.index+":"+pipeline);
                    String output=PromptBuilder.extractTranslationStrict(chat.content); long duration=System.currentTimeMillis()-started;
                    double cost=chat.providerCostReported?chat.providerCost:ModelCatalog.usageCost(pricing,chat.promptTokens,chat.cachedPromptTokens,chat.completionTokens); if(Double.isNaN(cost)) cost=0;
                    result.spentUsd+=cost;result.requests++;
                    TranslationQualityChecks.Result quality=TranslationQualityChecks.inspect(row.source,output,previous);
                    repo.saveBenchmarkResult(session,row.index,pipeline,blind,chat,cost,duration,chat.finishReason,quality.issues.toString(),output);
                    if(events!=null)events.log("Benchmark chunk "+(row.index+1)+" output "+blind+" complete; spent "+CostEstimator.money(result.spentUsd));
                }
            }
            repo.finishBenchmark(session,"done",result.spentUsd,""); return result;
        } catch(Exception e){repo.finishBenchmark(session,e instanceof InterruptedException?"cancelled":"error",result.spentUsd,e.getMessage());throw e;}
    }

    static List<TranslationRepository.ChunkRow> selectSamples(List<TranslationRepository.ChunkRow> all){
        LinkedHashSet<Integer> indexes=new LinkedHashSet<>();indexes.add(0);indexes.add(all.size()/2);indexes.add(all.size()-1);
        ArrayList<TranslationRepository.ChunkRow> out=new ArrayList<>();for(int i:indexes)if(i>=0&&i<all.size())out.add(all.get(i));return out;
    }
    static double estimateWorst(List<TranslationRepository.ChunkRow> rows,AppSettings s,ModelCatalog.ModelInfo pricing,int maxOutput){
        double total=0;for(TranslationRepository.ChunkRow row:rows){Chunk c=new Chunk(row.index,"",row.source,"");AppSettings a=s.copy();a.optimizationPreset="full";AppSettings b=s.copy();b.optimizationPreset="balanced";total+=ModelCatalog.usageCost(pricing,PromptPlan.forTranslation(c,"",a).totalInputTokens,0,maxOutput);total+=ModelCatalog.usageCost(pricing,PromptPlan.forTranslation(c,"",b).totalInputTokens,0,maxOutput);}return total;
    }
    private static String previousOutput(List<TranslationRepository.ChunkRow> rows,int idx){String out="";for(TranslationRepository.ChunkRow r:rows){if(r.index>=idx)break;if(r.translated!=null&&!r.translated.trim().isEmpty())out=r.translated;}return out.length()<=400?out:out.substring(out.length()-400);}
    private LiveBenchmarkRunner(){}
}
