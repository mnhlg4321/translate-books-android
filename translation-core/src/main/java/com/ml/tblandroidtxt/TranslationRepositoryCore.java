package com.ml.tblandroidtxt;


import java.util.ArrayList;
import java.util.List;

public class TranslationRepositoryCore {
    private final SqlDb sqlDb;

    protected TranslationRepositoryCore(SqlDb sqlDb) { this.sqlDb = sqlDb; }

    protected final SqlDb db() { return sqlDb; }

    /** Version name stored with each new job; the Android app returns its BuildConfig name. */
    protected String appVersionName() { return ""; }

    public List<Chunk> getPreparedChunks(String batchId,int ordinal){ArrayList<Chunk> out=new ArrayList<>();String sql="SELECT idx,start_offset,end_offset,context_before,source,context_after,parent_stable_id,stable_id FROM prepared_chunks WHERE batch_id=? AND input_ordinal=? ORDER BY idx";try(DbCursor c=db().rawQuery(sql,new String[]{batchId,String.valueOf(ordinal)})){while(c.moveToNext()){Chunk ch=new Chunk(c.getInt(0),c.getInt(1),c.getInt(2),safe(c.getString(3)),safe(c.getString(4)),safe(c.getString(5)),safe(c.getString(6)));String stable=safe(c.getString(7));if(!stable.isEmpty())ch.stableId=stable;out.add(ch);}}Chunker.assignParagraphRanges(out);return out;}



    public long createBenchmarkSession(long jobId, AppSettings s, double budgetUsd, int samples) {
        DbValues v=new DbValues(); long now=System.currentTimeMillis();
        v.put("job_id",jobId); v.put("status","running"); v.put("model",s.model); v.put("provider",s.provider);
        v.put("budget_usd",budgetUsd); v.put("spent_usd",0); v.put("sample_count",samples); v.put("created_at",now); v.put("updated_at",now); v.put("error","");
        return db().insert("benchmark_sessions",null,v);
    }

    public void saveBenchmarkResult(long sessionId, int chunkIndex, String pipeline, String blindLabel,
                                    OpenAICompatibleClient.ChatResult usage, double cost, long durationMs,
                                    String finishReason, String qualityIssues, String output) {
        DbValues v=new DbValues(); v.put("session_id",sessionId); v.put("chunk_idx",chunkIndex); v.put("pipeline",pipeline); v.put("blind_label",blindLabel);
        v.put("input_tokens",usage==null?0:usage.promptTokens); v.put("cached_tokens",usage==null?0:usage.cachedPromptTokens); v.put("output_tokens",usage==null?0:usage.completionTokens);
        v.put("cost_usd",cost); v.put("duration_ms",durationMs); v.put("finish_reason",finishReason); v.put("quality_issues",qualityIssues); v.put("output",output==null?"":output);
        v.put("usage_source",usage!=null&&usage.usageReported?"PROVIDER":"UNKNOWN_USAGE");
        v.put("cost_status",usage!=null&&usage.providerCostReported?"PROVIDER_REPORTED":usage!=null&&usage.usageReported?"USAGE_DERIVED":"UNKNOWN_USAGE");
        db().insertWithOnConflict("benchmark_results",null,v,SqlDb.CONFLICT_REPLACE);
    }

    public void finishBenchmark(long sessionId, String status, double spent, String error) {
        DbValues v=new DbValues(); v.put("status",status); v.put("spent_usd",spent); v.put("updated_at",System.currentTimeMillis()); v.put("error",error==null?"":error);
        db().update("benchmark_sessions",v,"id=?",new String[]{String.valueOf(sessionId)});
    }

    public BenchmarkSummary latestBenchmark(long jobId) {
        String sql="SELECT id,status,model,provider,budget_usd,spent_usd,sample_count,created_at,error FROM benchmark_sessions WHERE job_id=? ORDER BY created_at DESC LIMIT 1";
        try(DbCursor c=db().rawQuery(sql,new String[]{String.valueOf(jobId)})){ if(c.moveToFirst()){ BenchmarkSummary b=new BenchmarkSummary(); b.id=c.getLong(0);b.status=c.getString(1);b.model=c.getString(2);b.provider=c.getString(3);b.budgetUsd=c.getDouble(4);b.spentUsd=c.getDouble(5);b.sampleCount=c.getInt(6);b.createdAt=c.getLong(7);b.error=c.getString(8); return b; }} return null;
    }

    public List<BenchmarkResultRow> benchmarkResults(long sessionId) {
        ArrayList<BenchmarkResultRow> out=new ArrayList<>();
        try(DbCursor c=db().rawQuery("SELECT chunk_idx,pipeline,blind_label,input_tokens,cached_tokens,output_tokens,cost_usd,usage_source,cost_status,duration_ms,finish_reason,quality_issues,output FROM benchmark_results WHERE session_id=? ORDER BY chunk_idx,blind_label",new String[]{String.valueOf(sessionId)})){
            while(c.moveToNext()){BenchmarkResultRow r=new BenchmarkResultRow();r.chunkIndex=c.getInt(0);r.pipeline=c.getString(1);r.blindLabel=c.getString(2);r.inputTokens=c.getInt(3);r.cachedTokens=c.getInt(4);r.outputTokens=c.getInt(5);r.costUsd=c.getDouble(6);r.usageSource=c.getString(7);r.costStatus=c.getString(8);r.durationMs=c.getLong(9);r.finishReason=c.getString(10);r.qualityIssues=c.getString(11);r.output=c.getString(12);out.add(r);}
        } return out;
    }

    public static class BenchmarkSummary { public long id,createdAt; public String status,model,provider,error; public double budgetUsd,spentUsd; public int sampleCount; }
    public static class BenchmarkResultRow { public int chunkIndex,inputTokens,cachedTokens,outputTokens; public String pipeline,blindLabel,usageSource,costStatus,finishReason,qualityIssues,output; public double costUsd; public long durationMs; }


    public void recordMetric(ChunkMetric m) {
        if (m == null || m.jobId <= 0) return;
        DbValues v = new DbValues();
        v.put("job_id", m.jobId); v.put("chunk_idx", m.chunkIndex); v.put("phase", m.phase); v.put("attempt", m.attempt);
        v.put("preset", m.preset); v.put("provider", m.provider); v.put("model", m.model); v.put("source_chars", m.sourceChars);
        v.put("raw_tokens", m.rawTokens); v.put("system_tokens", m.systemTokens); v.put("instruction_tokens", m.instructionTokens);
        v.put("glossary_tokens", m.glossaryTokens); v.put("pronoun_tokens", m.pronounTokens); v.put("history_tokens", m.historyTokens);
        v.put("context_tokens", m.contextTokens); v.put("estimated_input_tokens", m.estimatedInputTokens); v.put("actual_input_tokens", m.actualInputTokens);
        v.put("output_tokens", m.outputTokens); v.put("retry_tokens", m.retryTokens); v.put("max_output_tokens", m.maxOutputTokens);
        v.put("baseline_input_tokens", m.baselineInputTokens);
        v.put("cached_input_tokens", m.cachedInputTokens); v.put("usage_source", m.usageSource);
        v.put("cost_status", m.costStatus); v.put("logical_request_id", m.logicalRequestId);
        v.put("duration_ms", m.durationMs); v.put("estimated_cost", m.estimatedCost); v.put("usage_cost", m.usageCost);
        v.put("outcome", m.outcome); v.put("retry_reason", m.retryReason); v.put("created_at", System.currentTimeMillis());
        db().insert("chunk_metrics", null, v);
    }

    public MetricsSummary getMetricsSummary(long jobId) {
        MetricsSummary m = new MetricsSummary();
        String sql = "SELECT COALESCE(SUM(raw_tokens),0),COALESCE(SUM(system_tokens),0),COALESCE(SUM(instruction_tokens),0),COALESCE(SUM(glossary_tokens),0),COALESCE(SUM(pronoun_tokens),0),COALESCE(SUM(history_tokens+context_tokens),0),COALESCE(SUM(output_tokens),0),COALESCE(SUM(retry_tokens),0),COALESCE(SUM(CASE WHEN cost_status IN ('USAGE_DERIVED','PROVIDER_REPORTED') THEN usage_cost ELSE 0 END),0),COUNT(*),COALESCE(SUM(CASE WHEN outcome='success' THEN 1 ELSE 0 END),0),COALESCE(SUM(baseline_input_tokens),0),COALESCE(SUM(estimated_input_tokens),0),COALESCE(SUM(cached_input_tokens),0),COALESCE(SUM(CASE WHEN usage_source!='PROVIDER' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN cost_status LIKE 'UNKNOWN%' THEN 1 ELSE 0 END),0),COALESCE(SUM(actual_input_tokens),0),COALESCE(SUM(MAX(0,actual_input_tokens-cached_input_tokens)),0),COALESCE(SUM(CASE WHEN estimated_cost>=0 THEN estimated_cost ELSE 0 END),0),COALESCE(SUM(CASE WHEN usage_source='PROVIDER' THEN 1 ELSE 0 END),0) FROM chunk_metrics WHERE job_id=?";
        try (DbCursor c = db().rawQuery(sql, new String[]{String.valueOf(jobId)})) {
            if (c.moveToFirst()) { m.rawTokens=c.getLong(0); m.systemTokens=c.getLong(1); m.instructionTokens=c.getLong(2); m.glossaryTokens=c.getLong(3); m.pronounTokens=c.getLong(4); m.contextTokens=c.getLong(5); m.outputTokens=c.getLong(6); m.retryTokens=c.getLong(7); m.totalCost=c.getDouble(8); m.attempts=c.getInt(9); m.successes=c.getInt(10); m.baselineInputTokens=c.getLong(11); m.estimatedInputTokens=c.getLong(12); m.cachedInputTokens=c.getLong(13); m.estimatedUsageRows=c.getInt(14); m.unknownCostRows=c.getInt(15); m.actualInputTokens=c.getLong(16); m.uncachedInputTokens=c.getLong(17); m.estimatedCostBefore=c.getDouble(18); m.providerUsageRows=c.getInt(19); }
        }
        return m;
    }

    public TokenCalibration getTokenCalibration(long jobId){TokenCalibration t=new TokenCalibration();String sql="SELECT COALESCE(SUM(estimated_input_tokens),0),COALESCE(SUM(actual_input_tokens),0),COUNT(*) FROM chunk_metrics WHERE job_id=? AND usage_source='PROVIDER' AND actual_input_tokens>0";try(DbCursor c=db().rawQuery(sql,new String[]{String.valueOf(jobId)})){if(c.moveToFirst()){t.estimated=c.getLong(0);t.actual=c.getLong(1);t.samples=c.getInt(2);}}return t;}
    public static class TokenCalibration { public long estimated,actual;public int samples;public double ratio(){return estimated<=0?1d:actual/(double)estimated;} }

    public static class MetricsSummary {
        public long rawTokens, systemTokens, instructionTokens, glossaryTokens, pronounTokens, contextTokens, outputTokens, retryTokens, baselineInputTokens, estimatedInputTokens;
        public long cachedInputTokens, actualInputTokens, uncachedInputTokens; public double totalCost, estimatedCostBefore; public int attempts, successes, estimatedUsageRows, unknownCostRows, providerUsageRows;
        public long estimatedSavedInputTokens() { return Math.max(0, baselineInputTokens - estimatedInputTokens); }
    }

    public long createJob(String inputUri, String outputUri, String fileName, AppSettings s, List<Chunk> chunks, String inputHash, String settingsHash) {
        SqlDb db = db();
        db.beginTransaction();
        try { long id=createJobRows(db,inputUri,outputUri,fileName,s,chunks,inputHash,settingsHash,"",-1,"");db.setTransactionSuccessful();return id; }
        finally { db.endTransaction(); }
    }

    private long createJobRows(SqlDb db,String inputUri,String outputUri,String fileName,AppSettings s,List<Chunk> chunks,String inputHash,String settingsHash,String preparedBatchId,int preparedOrdinal,String startSessionId) {
        long now = System.currentTimeMillis();
        DbValues cv = new DbValues();
        cv.put("input_uri", inputUri);
        cv.put("output_uri", outputUri);
        cv.put("file_name", fileName);
        cv.put("source_language", s.sourceLanguage);
        cv.put("target_language", s.targetLanguage);
        cv.put("status", "READY");
        cv.put("created_at", now);
        cv.put("updated_at", now);
        cv.put("settings", SettingsJson.toPersistedJson(s));
        cv.put("input_hash", inputHash == null ? "" : inputHash);
        cv.put("settings_hash", settingsHash == null ? "" : settingsHash);
        cv.put("integrity_status", "PENDING"); cv.put("config_revision",1); cv.put("app_version",appVersionName());
        if(preparedBatchId!=null&&!preparedBatchId.isEmpty()){cv.put("prepared_batch_id",preparedBatchId);cv.put("prepared_input_ordinal",preparedOrdinal);}
        if(startSessionId!=null&&!startSessionId.isEmpty())cv.put("start_session_id",startSessionId);
        long id = db.insertOrThrow("jobs", null, cv);
        DbValues revision=new DbValues();revision.put("job_id",id);revision.put("revision",1);revision.put("first_chunk_idx",0);revision.put("settings_json",SettingsJson.toPersistedJson(s));revision.put("settings_hash",settingsHash==null?"":settingsHash);revision.put("created_at",now);db.insertOrThrow("config_revisions",null,revision);
        for (Chunk c : chunks) {
            DbValues cc = new DbValues();
            cc.put("job_id", id);
            cc.put("idx", c.index);
            cc.put("source", c.mainContent);
            cc.put("translated", "");
            cc.put("stable_id", c.stableId); cc.put("start_offset",c.startOffset);cc.put("end_offset",c.endOffset);
            cc.put("context_start_offset",c.contextStartOffset);cc.put("context_end_offset",c.contextEndOffset);
            cc.put("source_hash",c.sourceHash);cc.put("normalized_source_hash",c.normalizedSourceHash);cc.put("parent_stable_id",c.parentStableId);
            cc.put("status", "PENDING"); cc.put("attempt",0);cc.put("created_at",now);cc.put("result_source","PROVIDER");
            cc.put("error", "");
            cc.put("updated_at", now);
            db.insertOrThrow("chunks", null, cc);
        }
        return id;
    }

    public long createJob(String inputUri, String outputUri, String fileName, AppSettings s, List<Chunk> chunks) {
        return createJob(inputUri, outputUri, fileName, s, chunks, "", "");
    }

    public synchronized long createJobFromPrepared(String batchId,String startSessionId,int ordinal,String inputUri,String outputUri,String fileName,AppSettings s,List<Chunk> chunks,String inputHash,String settingsHash){
        if(startSessionId==null||startSessionId.trim().isEmpty())throw new IllegalArgumentException("Fresh Start requires a unique start session id");
        SqlDb db=db();db.beginTransaction();
        try{try(DbCursor c=db.rawQuery("SELECT id FROM jobs WHERE start_session_id=? AND prepared_input_ordinal=? ORDER BY id DESC LIMIT 1",new String[]{safe(startSessionId),String.valueOf(ordinal)})){if(c.moveToFirst()){long id=c.getLong(0);db.setTransactionSuccessful();return id;}}
            long id=createJobRows(db,inputUri,outputUri,fileName,s,chunks,inputHash,settingsHash,safe(batchId),ordinal,startSessionId);db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }

    public Job findDoneJobByHashes(String inputHash, String settingsHash) {
        if (inputHash == null || inputHash.isEmpty() || settingsHash == null || settingsHash.isEmpty()) return null;
        SqlDb db = db();
        try (DbCursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs WHERE input_hash=? AND settings_hash=? AND UPPER(status) IN (" + JobStatus.sqlFinished() + ") ORDER BY updated_at DESC LIMIT 1", new String[]{inputHash, settingsHash})) {
            if (c.moveToFirst()) return readJob(c);
        }
        return null;
    }

    public String assembleOutput(long jobId, boolean bilingual) {
        List<ChunkRow> rows = getChunkRows(jobId);
        StringBuilder sb = new StringBuilder();
        for (ChunkRow r : rows) {
            if (!isCompleted(r.status) || r.translated == null || r.translated.isEmpty()) break;
            if (bilingual) sb.append("[SOURCE]\n").append(r.source).append("\n[TRANSLATION]\n").append(r.translated).append("\n\n");
            else {
                sb.append(r.translated);
                if (!r.translated.endsWith("\n")) sb.append('\n');
            }
        }
        return sb.toString();
    }

    public Job getLastIncompleteJob() {
        SqlDb db = db();
        try (DbCursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs WHERE UPPER(status) IN (" + JobStatus.sqlUnfinishedShown() + ") ORDER BY updated_at DESC LIMIT 1", null)) {
            if (c.moveToFirst()) {
                return readJob(c);
            }
        }
        return null;
    }


    public Job getLastJobWithFailedChunks() {
        SqlDb db = db();
        String sql = "SELECT j.id,j.input_uri,j.output_uri,j.file_name,j.settings,j.status " +
                "FROM jobs j WHERE EXISTS (SELECT 1 FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) " +
                "ORDER BY j.updated_at DESC LIMIT 1";
        try (DbCursor c = db.rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                return readJob(c);
            }
        }
        return null;
    }


    public Job getJob(long jobId) {
        SqlDb db = db();
        try (DbCursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs WHERE id=? LIMIT 1", new String[]{String.valueOf(jobId)})) {
            if (c.moveToFirst()) return readJob(c);
        }
        return null;
    }

    public String getInputHash(long jobId){try(DbCursor c=db().rawQuery("SELECT input_hash FROM jobs WHERE id=?",new String[]{String.valueOf(jobId)})){return c.moveToFirst()?safe(c.getString(0)):"";}}

    /** Applies an immutable settings snapshot from the first unfinished chunk onward. */
    public int applyConfigRevision(long jobId, int firstChunkIndex, AppSettings settings) {
        if (settings == null) throw new IllegalArgumentException("Settings are required");
        SqlDb db=db(); long now=System.currentTimeMillis(); int next=1;
        try(DbCursor c=db.rawQuery("SELECT COALESCE(MAX(revision),0)+1 FROM config_revisions WHERE job_id=?",new String[]{String.valueOf(jobId)})){if(c.moveToFirst())next=c.getInt(0);}
        AppSettings normalized=AppValidator.normalize(settings); String json=SettingsJson.toPersistedJson(normalized); String hash=HashUtil.settingsHash(normalized);
        db.beginTransaction();
        try {
            DbValues r=new DbValues();r.put("job_id",jobId);r.put("revision",next);r.put("first_chunk_idx",Math.max(0,firstChunkIndex));r.put("settings_json",json);r.put("settings_hash",hash);r.put("created_at",now);db.insertOrThrow("config_revisions",null,r);
            DbValues j=new DbValues();j.put("settings",json);j.put("settings_hash",hash);j.put("config_revision",next);j.put("updated_at",now);db.update("jobs",j,"id=?",new String[]{String.valueOf(jobId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        ObservabilityLog.event("config_revision_applied","job",jobId,"revision",next,"firstChunk",firstChunkIndex,"provider",normalized.provider,"model",normalized.model);
        return next;
    }

    public int firstRunnableChunk(long jobId) {
        try(DbCursor c=db().rawQuery("SELECT COALESCE(MIN(idx),-1) FROM chunks WHERE job_id=? AND UPPER(status) NOT IN ('SUPERSEDED','DONE','COMPLETED','DELIVERY_UNKNOWN')",new String[]{String.valueOf(jobId)})){return c.moveToFirst()?c.getInt(0):-1;}
    }

    public String chunkStatus(long jobId,int idx){try(DbCursor c=db().rawQuery("SELECT status FROM chunks WHERE job_id=? AND idx=?",args(jobId,idx))){return c.moveToFirst()?safe(c.getString(0)):"";}}

    public List<ChunkRow> getChunkRows(long jobId) {
        ArrayList<ChunkRow> list = new ArrayList<>();
        SqlDb db = db();
        try (DbCursor c = db.rawQuery("SELECT idx,source,translated,status,error,stable_id,start_offset,end_offset,source_hash,normalized_source_hash,request_hash,response_hash,attempt,finish_reason,validation_warnings,qa_json,result_source,rejected_response,parent_stable_id,persisted_response FROM chunks WHERE job_id=? AND UPPER(status)<>'SUPERSEDED' ORDER BY idx", new String[]{String.valueOf(jobId)})) {
            while (c.moveToNext()) {
                ChunkRow r = new ChunkRow();
                r.index = c.getInt(0); r.source = c.getString(1); r.translated = c.getString(2); r.status = c.getString(3); r.error = c.getString(4);
                r.stableId=c.getString(5);r.startOffset=c.getInt(6);r.endOffset=c.getInt(7);r.sourceHash=c.getString(8);r.normalizedSourceHash=c.getString(9);r.requestHash=c.getString(10);r.responseHash=c.getString(11);r.attempt=c.getInt(12);r.finishReason=c.getString(13);r.validationWarnings=c.getString(14);r.qaJson=c.getString(15);r.resultSource=c.getString(16);r.rejectedResponse=c.getString(17);r.parentStableId=c.getString(18);r.persistedResponse=c.getString(19);
                list.add(r);
            }
        }
        return list;
    }

    public void supersedeAndInsertSubchunks(long jobId,Chunk original,List<Chunk> children){
        if(children==null||children.size()<2)throw new IllegalArgumentException("Context overflow chunk cannot be split further");SqlDb db=db();long now=System.currentTimeMillis();int delta=children.size()-1;db.beginTransaction();
        try{db.execSQL("UPDATE chunks SET idx=idx+100000 WHERE job_id=? AND idx>?",new Object[]{jobId,original.index});db.execSQL("UPDATE chunks SET idx=idx-100000+? WHERE job_id=? AND idx>=100000",new Object[]{delta,jobId});int supersededIndex=-1;try(DbCursor c=db.rawQuery("SELECT COALESCE(MIN(idx),0)-1 FROM chunks WHERE job_id=?",new String[]{String.valueOf(jobId)})){if(c.moveToFirst())supersededIndex=c.getInt(0);}DbValues old=new DbValues();old.put("idx",supersededIndex);old.put("status","SUPERSEDED");old.put("error","Context limit exceeded; replaced by "+children.size()+" stable subchunks");old.put("updated_at",now);db.update("chunks",old,"job_id=? AND idx=?",args(jobId,original.index));
            for(int i=0;i<children.size();i++){Chunk child=children.get(i);child.index=original.index+i;DbValues v=new DbValues();v.put("job_id",jobId);v.put("idx",child.index);v.put("stable_id",child.stableId);v.put("start_offset",child.startOffset);v.put("end_offset",child.endOffset);v.put("context_start_offset",child.contextStartOffset);v.put("context_end_offset",child.contextEndOffset);v.put("source",child.mainContent);v.put("source_hash",child.sourceHash);v.put("normalized_source_hash",child.normalizedSourceHash);v.put("translated","");v.put("status","PENDING");v.put("error","");v.put("attempt",0);v.put("created_at",now);v.put("updated_at",now);v.put("result_source","PROVIDER");v.put("parent_stable_id",original.stableId);db.insertOrThrow("chunks",null,v);}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    public void markChunkDone(long jobId, int idx, String text) {
        commitValidatedChunk(jobId, idx, text, "", "PROVIDER");
    }

    public void beginChunkAttempt(long jobId, Chunk chunk, int attempt, String requestHash, AppSettings settings) {
        beginChunkAttempt(jobId,chunk,attempt,"translate",requestHash,jobId+":"+chunk.index+":translate:"+attempt,settings);
    }

    public void beginChunkAttempt(long jobId,Chunk chunk,int attempt,String phase,String requestHash,String logicalRequestId,AppSettings settings){
        SqlDb db=db();long now=System.currentTimeMillis();
        boolean preservesAccepted=hasAcceptedTranslation(db,jobId,chunk.index);DbValues c=new DbValues();c.put("status",preservesAccepted?"COMPLETED":"REQUESTING");c.put("attempt",attempt);c.put("request_hash",safe(requestHash));c.put("error","");c.put("updated_at",now);db.update("chunks",c,"job_id=? AND idx=?",args(jobId,chunk.index));
        DbValues a=new DbValues();a.put("job_id",jobId);a.put("stable_id",chunk.stableId);a.put("chunk_idx",chunk.index);a.put("attempt",attempt);a.put("phase",safe(phase));a.put("status","CREATED");a.put("network_state","CREATED");a.put("logical_request_id",safe(logicalRequestId));a.put("request_hash",safe(requestHash));a.put("model",settings==null?"":settings.model);a.put("provider",settings==null?"":settings.provider);a.put("created_at",now);a.put("updated_at",now);db.insertOrThrow("chunk_attempts",null,a);
    }

    public void markAttemptPhase(long jobId,int idx,int attempt,String phase,String state){DbValues v=new DbValues();v.put("status",safe(state));v.put("network_state",safe(state));v.put("updated_at",System.currentTimeMillis());db().update("chunk_attempts",v,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});ObservabilityLog.event("attempt_state","job",jobId,"chunk",idx,"attempt",attempt,"phase",phase,"state",state);}

    public void markValidating(long jobId,int idx,int attempt,String phase){SqlDb db=db();DbValues c=new DbValues();if(!hasAcceptedTranslation(db,jobId,idx)){c.put("status","VALIDATING");c.put("updated_at",System.currentTimeMillis());db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));}markAttemptPhase(jobId,idx,attempt,phase,"VALIDATING");}

    public void markAttemptFailedBeforeSend(long jobId,int idx,int attempt,String phase,String error){DbValues v=new DbValues();v.put("status","FAILED_BEFORE_SEND");v.put("network_state","FAILED_BEFORE_SEND");v.put("error",safe(error));v.put("updated_at",System.currentTimeMillis());db().update("chunk_attempts",v,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});}

    public void persistReceivedResponse(long jobId,int idx,int attempt,OpenAICompatibleClient.ChatResult result) {
        persistReceivedResponse(jobId,idx,attempt,"translate",result);
    }

    public void persistReceivedResponse(long jobId,int idx,int attempt,String phase,OpenAICompatibleClient.ChatResult result) {
        String raw=result==null?"":result.content,finishReason=result==null?"":result.finishReason;SqlDb db=db();long now=System.currentTimeMillis();String hash=HashUtil.sha256(raw);
        boolean preservesAccepted=hasAcceptedTranslation(db,jobId,idx);DbValues c=new DbValues();c.put("status",preservesAccepted?"COMPLETED":"RESPONSE_RECEIVED");c.put("persisted_response",safe(raw));if(!preservesAccepted)c.put("response_hash",hash);c.put("finish_reason",safe(finishReason));c.put("updated_at",now);db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));
        DbValues a=new DbValues();a.put("status","RESPONSE_RECEIVED");a.put("network_state","RESPONSE_RECEIVED");a.put("response_hash",hash);a.put("response_preview",redactedPreview(raw));a.put("response_content",safe(raw));a.put("provider_response_id",result==null?"":safe(result.providerResponseId));a.put("finish_reason",safe(finishReason));a.put("input_tokens",result==null?0:result.promptTokens);a.put("output_tokens",result==null?0:result.completionTokens);a.put("cached_tokens",result==null?0:result.cachedPromptTokens);a.put("actual_cost",result!=null&&result.providerCostReported?result.providerCost:0);a.put("usage_source",result!=null&&result.usageReported?"PROVIDER":"ESTIMATED");a.put("updated_at",now);db.update("chunk_attempts",a,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});
    }

    public void markDeliveryUnknown(long jobId,int idx,int attempt,String phase,String error){SqlDb db=db();DbValues c=new DbValues();c.put("status","DELIVERY_UNKNOWN");c.put("error",safe(error));c.put("updated_at",System.currentTimeMillis());db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));markAttemptPhase(jobId,idx,attempt,phase,"DELIVERY_UNKNOWN");touchJob(jobId,"NEEDS_REVIEW");}

    public void rejectResponse(long jobId,int idx,int attempt,String raw,String reason,boolean retryable) {
        rejectResponse(jobId,idx,attempt,"translate",raw,reason,retryable);
    }

    public void rejectResponse(long jobId,int idx,int attempt,String phase,String raw,String reason,boolean retryable) {
        SqlDb db=db();long now=System.currentTimeMillis();
        boolean preservesAccepted=hasAcceptedTranslation(db,jobId,idx);DbValues c=new DbValues();c.put("status",preservesAccepted?"COMPLETED":retryable?"RETRYABLE_ERROR":"PERMANENT_ERROR");c.put("rejected_response",safe(raw));c.put("error",(preservesAccepted?"Candidate rejected; prior accepted output retained: ":"")+safe(reason));c.put("updated_at",now);db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));
        DbValues a=new DbValues();a.put("status","REJECTED");a.put("validation_result",safe(reason));a.put("error",safe(reason));a.put("updated_at",now);db.update("chunk_attempts",a,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});
    }

    public void commitValidatedChunk(long jobId, int idx, String text, String warnings, String resultSource) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("Cannot commit blank translation");
        SqlDb db=db();long now=System.currentTimeMillis();db.beginTransaction();
        try {
        DbValues cv = new DbValues();
        cv.put("translated", text);
        cv.put("status", "COMPLETED");
        cv.put("error", "");
        cv.put("response_hash",HashUtil.sha256(text));cv.put("validation_warnings",safe(warnings));cv.put("result_source",safe(resultSource));cv.put("persisted_response","");
        cv.put("updated_at", System.currentTimeMillis());
        if(db.update("chunks", cv, "job_id=? AND idx=?", args(jobId,idx))!=1)throw new IllegalStateException("Chunk missing during commit");
        DbValues attempt=new DbValues();attempt.put("status","ACCEPTED");attempt.put("validation_result","PASSED"+(warnings==null||warnings.isEmpty()?"":";"+warnings));attempt.put("updated_at",now);db.update("chunk_attempts",attempt,"job_id=? AND chunk_idx=? AND attempt=(SELECT MAX(attempt) FROM chunk_attempts WHERE job_id=? AND chunk_idx=?)",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(jobId),String.valueOf(idx)});
        DbValues j=new DbValues();j.put("status","running");j.put("updated_at",now);db.update("jobs",j,"id=?",new String[]{String.valueOf(jobId)});
        db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void commitManualReplacement(long jobId,int idx,String text,String warnings){
        long now=System.currentTimeMillis();SqlDb db=db();int attempt=1;String stable="";try(DbCursor c=db.rawQuery("SELECT COALESCE(attempt,0)+1,stable_id FROM chunks WHERE job_id=? AND idx=?",args(jobId,idx))){if(c.moveToFirst()){attempt=c.getInt(0);stable=safe(c.getString(1));}}
        DbValues a=new DbValues();a.put("job_id",jobId);a.put("stable_id",stable);a.put("chunk_idx",idx);a.put("attempt",attempt);a.put("status","MANUAL_VALIDATED");a.put("response_hash",HashUtil.sha256(text));a.put("response_preview",redactedPreview(text));a.put("finish_reason","manual");a.put("validation_result","PASSED;"+safe(warnings));a.put("model","manual");a.put("provider","manual");a.put("created_at",now);a.put("updated_at",now);db.insert("chunk_attempts",null,a);
        DbValues count=new DbValues();count.put("attempt",attempt);db.update("chunks",count,"job_id=? AND idx=?",args(jobId,idx));commitValidatedChunk(jobId,idx,text,warnings,"MANUAL");
    }

    public void markChunkError(long jobId, int idx, String error) {
        SqlDb db=db();boolean preservesAccepted=hasAcceptedTranslation(db,jobId,idx);
        DbValues cv = new DbValues();
        cv.put("status", preservesAccepted?"COMPLETED":"RETRYABLE_ERROR");
        cv.put("error", error == null ? "" : error);
        cv.put("updated_at", System.currentTimeMillis());
        int changed=db.update("chunks", cv, "job_id=? AND idx=? AND UPPER(status)<>'DELIVERY_UNKNOWN'", new String[]{String.valueOf(jobId), String.valueOf(idx)});
        touchJob(jobId, changed==0||preservesAccepted?"NEEDS_REVIEW":"error");
    }

    public void markChunkPermanentError(long jobId,int idx,String error){DbValues v=new DbValues();v.put("status","PERMANENT_ERROR");v.put("error",safe(error));v.put("updated_at",System.currentTimeMillis());db().update("chunks",v,"job_id=? AND idx=?",args(jobId,idx));touchJob(jobId,"NEEDS_REVIEW");}

    public void touchJob(long jobId, String status) {
        DbValues cv = new DbValues();
        cv.put("status", status);
        cv.put("updated_at", System.currentTimeMillis());
        db().update("jobs", cv, "id=?", new String[]{String.valueOf(jobId)});
    }

    public void updateJobOutputUri(long jobId,String outputUri){DbValues v=new DbValues();v.put("output_uri",safe(outputUri));v.put("updated_at",System.currentTimeMillis());db().update("jobs",v,"id=?",new String[]{String.valueOf(jobId)});}

    public OutputIntegrityAudit.Result finishJobWithAudit(long jobId) {
        OutputIntegrityAudit.Result audit=OutputIntegrityAudit.audit(getChunkRows(jobId));
        DbValues v=new DbValues();v.put("integrity_status",audit.status());v.put("status",audit.status());v.put("updated_at",System.currentTimeMillis());if(audit.passed())v.put("completed_at",System.currentTimeMillis());db().update("jobs",v,"id=?",new String[]{String.valueOf(jobId)});return audit;
    }
    public void finishJob(long jobId) { finishJobWithAudit(jobId); }


    public List<JobSummary> getJobSummaries(int limit) {
        ArrayList<JobSummary> list = new ArrayList<>();
        SqlDb db = db();
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending," +
                "COALESCE(j.input_hash,''),COALESCE(j.start_session_id,''),COALESCE(j.prepared_batch_id,''),COALESCE(j.prepared_input_ordinal,0)," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='DELIVERY_UNKNOWN') AS unknown " +
                "FROM jobs j ORDER BY j.updated_at DESC LIMIT ?";
        try (DbCursor c = db.rawQuery(sql, new String[]{String.valueOf(Math.max(1, limit))})) {
            while (c.moveToNext()) list.add(readJobSummary(c));
        }
        attachUsage(db, list);
        return list;
    }

    /**
     * Keyset page for the user-facing history screen.  It deliberately orders
     * by the immutable job id rather than updated_at, because touching a job
     * must not move it between history pages.
     *
     * @param beforeId rows must have an id lower than this value; use a value
     *                 below one for the first page
     * @param fileQuery optional case-insensitive filename fragment
     * @param allowedJobIds null for all jobs, or the workspace-owned ids for a
     *                      selected chapter set
     */
    public List<JobSummary> getJobSummariesPage(long beforeId, int limit,
                                                String fileQuery,
                                                java.util.Collection<Long> allowedJobIds) {
        ArrayList<JobSummary> list = new ArrayList<>();
        if (allowedJobIds != null && allowedJobIds.isEmpty()) return list;
        StringBuilder where = new StringBuilder("1=1");
        ArrayList<String> args = new ArrayList<>();
        if (beforeId > 0) { where.append(" AND j.id<?"); args.add(String.valueOf(beforeId)); }
        if (fileQuery != null && !fileQuery.trim().isEmpty()) {
            where.append(" AND LOWER(COALESCE(j.file_name,'')) LIKE ?");
            args.add("%" + fileQuery.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (allowedJobIds != null) {
            StringBuilder ids = new StringBuilder();
            for (Long id : allowedJobIds) {
                if (id == null || id.longValue() <= 0) continue;
                if (ids.length() > 0) ids.append(',');
                ids.append('?');
                args.add(String.valueOf(id.longValue()));
            }
            if (ids.length() == 0) return list;
            where.append(" AND j.id IN (").append(ids).append(')');
        }
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending," +
                "COALESCE(j.input_hash,''),COALESCE(j.start_session_id,''),COALESCE(j.prepared_batch_id,''),COALESCE(j.prepared_input_ordinal,0)," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='DELIVERY_UNKNOWN') AS unknown " +
                "FROM jobs j WHERE " + where + " ORDER BY j.id DESC LIMIT ?";
        args.add(String.valueOf(Math.max(1, limit)));
        SqlDb db = db();
        try (DbCursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) list.add(readJobSummary(c));
        }
        attachUsage(db, list);
        return list;
    }

    /** Small separate query for Current job: unfinished rows older than the 150-row recent cache still count. */
    public List<JobSummary> getCurrentUnfinishedSummaries(int limit) {
        ArrayList<JobSummary> list = new ArrayList<>();
        SqlDb db = db();
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending," +
                "COALESCE(j.input_hash,''),COALESCE(j.start_session_id,''),COALESCE(j.prepared_batch_id,''),COALESCE(j.prepared_input_ordinal,0)," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='DELIVERY_UNKNOWN') AS unknown " +
                "FROM jobs j WHERE UPPER(COALESCE(j.status,'')) IN (" + JobStatus.sqlUnfinishedShown() + ") " +
                "ORDER BY j.updated_at DESC,j.id DESC LIMIT ?";
        try (DbCursor c = db.rawQuery(sql, new String[]{String.valueOf(Math.max(1, limit))})) {
            while (c.moveToNext()) list.add(readJobSummary(c));
        }
        attachUsage(db, list);
        return list;
    }

    /**
     * Full read-only job lookup for one or more chapter-set runs. This has no
     * LIMIT: the ordinary Jobs cache intentionally remains capped at 150, while
     * the workspace navigator must also see older runs and resumed job ids.
     */
    public List<JobSummary> getJobSummariesForSessions(List<String> sessionIds, List<Long> resumedJobIds) {
        ArrayList<JobSummary> list = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();
        if (sessionIds != null) for (String id : sessionIds) {
            if (id == null || id.trim().isEmpty()) continue;
            if (where.length() > 0) where.append(',');
            where.append('?'); args.add(id);
        }
        StringBuilder ids = new StringBuilder();
        if (resumedJobIds != null) for (Long id : resumedJobIds) {
            if (id == null || id.longValue() <= 0) continue;
            if (ids.length() > 0) ids.append(',');
            ids.append('?'); args.add(String.valueOf(id.longValue()));
        }
        if (where.length() == 0 && ids.length() == 0) return list;
        StringBuilder predicate = new StringBuilder("(");
        if (where.length() > 0) predicate.append("j.start_session_id IN (").append(where).append(')');
        if (ids.length() > 0) {
            if (where.length() > 0) predicate.append(" OR ");
            predicate.append("j.id IN (").append(ids).append(')');
        }
        predicate.append(')');
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending," +
                "COALESCE(j.input_hash,''),COALESCE(j.start_session_id,''),COALESCE(j.prepared_batch_id,''),COALESCE(j.prepared_input_ordinal,0)," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='DELIVERY_UNKNOWN') AS unknown " +
                "FROM jobs j WHERE " + predicate + " ORDER BY j.updated_at DESC, j.id DESC";
        SqlDb db = db();
        try (DbCursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) list.add(readJobSummary(c));
        }
        attachUsage(db, list);
        return list;
    }

    private static void attachUsage(SqlDb db, List<JobSummary> jobs) {
        if (jobs.isEmpty()) return;
        java.util.HashMap<Long, JobSummary> byId = new java.util.HashMap<>();
        StringBuilder ids = new StringBuilder();
        for (JobSummary j : jobs) { byId.put(j.id, j); if (ids.length() > 0) ids.append(','); ids.append(j.id); }
        try (DbCursor c = db.rawQuery("SELECT job_id,cost_status,COALESCE(usage_cost,0),COALESCE(actual_input_tokens,0),COALESCE(cached_input_tokens,0),COALESCE(output_tokens,0) FROM chunk_metrics WHERE job_id IN (" + ids + ")", null)) {
            while (c.moveToNext()) {
                JobSummary j = byId.get(c.getLong(0));
                if (j == null) continue;
                if (j.usage == null) j.usage = new UsageTotals();
                j.usage.add(c.getString(1), c.getDouble(2), c.getLong(3), c.getLong(4), c.getLong(5));
            }
        }
    }

    /** Spend since {@code sinceMillis}: every provider call counted once, plus the chapters (jobs) finished in that window. */
    public UsageTotals getUsageTotals(long sinceMillis) {
        UsageTotals t = new UsageTotals();
        SqlDb db = db();
        try (DbCursor c = db.rawQuery("SELECT cost_status,COALESCE(usage_cost,0),COALESCE(actual_input_tokens,0),COALESCE(cached_input_tokens,0),COALESCE(output_tokens,0) FROM chunk_metrics WHERE created_at>=CAST(? AS INTEGER)", new String[]{String.valueOf(sinceMillis)})) {
            while (c.moveToNext()) t.add(c.getString(0), c.getDouble(1), c.getLong(2), c.getLong(3), c.getLong(4));
        }
        try (DbCursor c = db.rawQuery("SELECT COUNT(*) FROM jobs WHERE UPPER(COALESCE(status,'')) IN (" + JobStatus.sqlFinished() + ") AND COALESCE(NULLIF(completed_at,0),updated_at)>=CAST(? AS INTEGER)", new String[]{String.valueOf(sinceMillis)})) {
            if (c.moveToFirst()) t.chaptersDone = c.getInt(0);
        }
        return t;
    }

    /** Takes the job off the unfinished-job card without deleting anything (chunks, tokens and cost stay). */
    public void dismissJob(long jobId) { touchJob(jobId, JobStatus.DISMISSED_VALUE); }

    public JobSummary getJobSummary(long jobId) {
        SqlDb db = db();
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending " +
                "FROM jobs j WHERE j.id=? LIMIT 1";
        try (DbCursor c = db.rawQuery(sql, new String[]{String.valueOf(jobId)})) {
            if (c.moveToFirst()) return readJobSummary(c);
        }
        return null;
    }

    public void deleteJob(long jobId) {
        SqlDb db = db();
        try(DbCursor c=db.rawQuery("SELECT id FROM benchmark_sessions WHERE job_id=?",new String[]{String.valueOf(jobId)})){while(c.moveToNext())db.delete("benchmark_results","session_id=?",new String[]{String.valueOf(c.getLong(0))});}
        db.delete("benchmark_sessions","job_id=?",new String[]{String.valueOf(jobId)});
        db.delete("chunk_metrics", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("chunk_attempts", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("config_revisions", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("chunks", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("jobs", "id=?", new String[]{String.valueOf(jobId)});
    }

    public List<Job> getRecentJobs(int limit) {
        ArrayList<Job> list = new ArrayList<>();
        SqlDb db = db();
        try (DbCursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs ORDER BY updated_at DESC LIMIT ?", new String[]{String.valueOf(Math.max(1, limit))})) {
            while (c.moveToNext()) {
                list.add(readJob(c));
            }
        }
        return list;
    }

    private Job readJob(DbCursor c) {
        Job j = new Job();
        j.id = c.getLong(0); j.inputUri = c.getString(1); j.outputUri = c.getString(2); j.fileName = c.getString(3); j.settingsJson = c.getString(4); j.status = c.getString(5);
        return j;
    }


    private JobSummary readJobSummary(DbCursor c) {
        JobSummary r = new JobSummary();
        r.id = c.getLong(0);
        r.fileName = c.getString(1);
        r.status = c.getString(2);
        r.sourceLanguage = c.getString(3);
        r.targetLanguage = c.getString(4);
        r.createdAt = c.getLong(5);
        r.updatedAt = c.getLong(6);
        r.inputUri = c.getString(7);
        r.outputUri = c.getString(8);
        r.totalChunks = c.getInt(9);
        r.doneChunks = c.getInt(10);
        r.failedChunks = c.getInt(11);
        r.pendingChunks = c.getInt(12);
        if (c.getColumnCount() > 16) {
            r.inputHash = c.getString(13);
            r.startSessionId = c.getString(14);
            r.preparedBatchId = c.getString(15);
            r.preparedOrdinal = c.getInt(16);
        }
        if (c.getColumnCount() > 17) r.unknownChunks = c.getInt(17);
        return r;
    }

    public static class ConflictJob { public long id; public String inputUri; public String inputHash; }

    /** Jobs marked INPUT_CONFLICT that a prepared run created; the one-time line-ending repair looks at these only. */
    public List<ConflictJob> getRepairableInputConflicts() {
        ArrayList<ConflictJob> out = new ArrayList<>();
        try (DbCursor c = db().rawQuery("SELECT id,input_uri,input_hash FROM jobs WHERE UPPER(COALESCE(status,''))='INPUT_CONFLICT' AND COALESCE(prepared_batch_id,'')<>'' AND COALESCE(input_hash,'')<>''", null)) {
            while (c.moveToNext()) { ConflictJob j = new ConflictJob(); j.id = c.getLong(0); j.inputUri = safe(c.getString(1)); j.inputHash = safe(c.getString(2)); out.add(j); }
        }
        return out;
    }

    public static class JobSummary {
        public long id;
        public String fileName;
        public String status;
        public String sourceLanguage;
        public String targetLanguage;
        public String inputUri;
        public String outputUri;
        public long createdAt;
        public long updatedAt;
        public int totalChunks;
        public int doneChunks;
        public int failedChunks;
        public int pendingChunks;
        /** Chunks whose request has unknown delivery: such a job cannot be continued without a look first. */
        public int unknownChunks;
        public String inputHash = "";
        public String startSessionId = "";
        public String preparedBatchId = "";
        public int preparedOrdinal;
        /** Tokens and money from chunk_metrics; null when the job has no metric rows. */
        public UsageTotals usage;
    }

    /** A job that still has chunks to translate, with the chunk counts the Translate screen shows. */
    public static class JobProgress {
        public long id;
        public String fileName, status, inputHash, inputUri, outputUri;
        public String startSessionId = "", preparedBatchId = "";
        public int preparedOrdinal;
        public int total, done, error, unknown, pending;
        public int remaining() { return Math.max(0, total - done); }
    }

    /** Unfinished jobs (not completed / cancelled / input conflict, with at least one chunk not done), newest first; {@code inputHashes} null = any. */
    public List<JobProgress> getIncompleteJobs(java.util.Collection<String> inputHashes, int limit) {
        ArrayList<JobProgress> out = new ArrayList<>();
        StringBuilder where = new StringBuilder("UPPER(COALESCE(j.status,'')) IN (" + JobStatus.sqlUnfinishedShown() + ") AND EXISTS (SELECT 1 FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED' AND UPPER(c.status) NOT IN ('DONE','COMPLETED'))"
                + " AND NOT EXISTS (SELECT 1 FROM jobs f WHERE f.id>j.id AND COALESCE(j.input_hash,'')<>'' AND f.input_hash=j.input_hash AND UPPER(COALESCE(f.status,'')) IN (" + JobStatus.sqlFinished() + "))");
        ArrayList<String> args = new ArrayList<>();
        if (inputHashes != null) {
            if (inputHashes.isEmpty()) return out;
            where.append(" AND j.input_hash IN (");
            boolean first = true;
            for (String h : inputHashes) { where.append(first ? "?" : ",?"); first = false; args.add(h); }
            where.append(")");
        }
        String sql = "SELECT j.id,j.file_name,j.status,j.input_hash,j.input_uri,j.output_uri,COALESCE(j.start_session_id,''),COALESCE(j.prepared_batch_id,''),COALESCE(j.prepared_input_ordinal,0) FROM jobs j WHERE " + where + " ORDER BY j.updated_at DESC LIMIT " + Math.max(1, limit);
        SqlDb db = db();
        try (DbCursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                JobProgress p = new JobProgress();
                p.id = c.getLong(0); p.fileName = safe(c.getString(1)); p.status = safe(c.getString(2)); p.inputHash = safe(c.getString(3)); p.inputUri = safe(c.getString(4)); p.outputUri = safe(c.getString(5));
                p.startSessionId = safe(c.getString(6)); p.preparedBatchId = safe(c.getString(7)); p.preparedOrdinal = c.getInt(8);
                out.add(p);
            }
        }
        for (JobProgress p : out) {
            try (DbCursor c = db.rawQuery("SELECT UPPER(status),COUNT(*) FROM chunks WHERE job_id=? AND UPPER(status)<>'SUPERSEDED' GROUP BY UPPER(status)", new String[]{String.valueOf(p.id)})) {
                while (c.moveToNext()) {
                    String st = c.getString(0); int n = c.getInt(1);
                    p.total += n;
                    if ("DONE".equals(st) || "COMPLETED".equals(st)) p.done += n;
                    else if (st.contains("ERROR")) p.error += n;
                    else if ("DELIVERY_UNKNOWN".equals(st)) p.unknown += n;
                    else p.pending += n;
                }
            }
        }
        return out;
    }

    public static class Job {
        public long id;
        public String inputUri;
        public String outputUri;
        public String fileName;
        public String settingsJson;
        public String status;
    }

    public static class ChunkRow {
        public int index;
        public String source;
        public String translated;
        public String status;
        public String error;
        public String stableId,sourceHash,normalizedSourceHash,requestHash,responseHash,finishReason,validationWarnings,qaJson,resultSource,rejectedResponse,parentStableId,persistedResponse;
        public int startOffset,endOffset,attempt;
    }

    public List<AttemptRow> getAttemptRows(long jobId,int idx){ArrayList<AttemptRow>out=new ArrayList<>();try(DbCursor c=db().rawQuery("SELECT attempt,status,phase,network_state,logical_request_id,request_hash,response_hash,response_preview,provider_response_id,finish_reason,validation_result,error,model,provider,input_tokens,output_tokens,cached_tokens,actual_cost,usage_source,created_at,updated_at FROM chunk_attempts WHERE job_id=? AND chunk_idx=? ORDER BY id",args(jobId,idx))){while(c.moveToNext()){AttemptRow a=new AttemptRow();a.attempt=c.getInt(0);a.status=c.getString(1);a.phase=c.getString(2);a.networkState=c.getString(3);a.logicalRequestId=c.getString(4);a.requestHash=c.getString(5);a.responseHash=c.getString(6);a.responsePreview=c.getString(7);a.providerResponseId=c.getString(8);a.finishReason=c.getString(9);a.validationResult=c.getString(10);a.error=c.getString(11);a.model=c.getString(12);a.provider=c.getString(13);a.inputTokens=c.getInt(14);a.outputTokens=c.getInt(15);a.cachedTokens=c.getInt(16);a.actualCost=c.getDouble(17);a.usageSource=c.getString(18);a.createdAt=c.getLong(19);a.updatedAt=c.getLong(20);out.add(a);}}return out;}
    public static class AttemptRow {public int attempt,inputTokens,outputTokens,cachedTokens;public double actualCost;public String status,phase,networkState,logicalRequestId,providerResponseId,requestHash,responseHash,responsePreview,finishReason,validationResult,error,model,provider,usageSource;public long createdAt,updatedAt;}

    public String preparedBatchIdentity(long jobId){try(DbCursor c=db().rawQuery("SELECT prepared_batch_id,prepared_input_ordinal FROM jobs WHERE id=?",new String[]{String.valueOf(jobId)})){return c.moveToFirst()?safe(c.getString(0))+":"+c.getInt(1):"";}}
    static boolean isCompleted(String s){return "done".equalsIgnoreCase(s)||"COMPLETED".equalsIgnoreCase(s);}
    static String[] args(long jobId,int idx){return new String[]{String.valueOf(jobId),String.valueOf(idx)};}
    static String safe(String s){return s==null?"":s;}
    private static String redactedPreview(String s){String v=safe(s).replaceAll("\\s+"," ").trim();return v.length()>240?v.substring(0,240)+"…":v;}
    private static boolean hasAcceptedTranslation(SqlDb db,long jobId,int idx){try(DbCursor c=db.rawQuery("SELECT translated,status FROM chunks WHERE job_id=? AND idx=?",args(jobId,idx))){if(c.moveToFirst()){String text=safe(c.getString(0)),status=safe(c.getString(1));return !text.isEmpty()&&(isCompleted(status)||"REQUESTING_REPLACEMENT".equalsIgnoreCase(status));}}return false;}
}
