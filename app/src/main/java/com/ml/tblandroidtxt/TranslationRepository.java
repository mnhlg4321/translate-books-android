package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TranslationRepository extends SQLiteOpenHelper {
    private static final String DB = "tbl_android_txt.db";
    private static final int VER = 23;

    public TranslationRepository(Context context) { this(context, DB); }

    /** Package-private database-name seam used by isolated migration/import tests. */
    TranslationRepository(Context context, String databaseName) { super(context, databaseName, null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE jobs (id INTEGER PRIMARY KEY AUTOINCREMENT, input_uri TEXT, output_uri TEXT, file_name TEXT, source_language TEXT, target_language TEXT, status TEXT, created_at INTEGER, updated_at INTEGER, settings TEXT, input_hash TEXT, settings_hash TEXT, integrity_status TEXT, output_hash TEXT, config_revision INTEGER, app_version TEXT, completed_at INTEGER, prepared_batch_id TEXT DEFAULT '', prepared_input_ordinal INTEGER DEFAULT -1, start_session_id TEXT DEFAULT '')");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_start_session ON jobs(start_session_id,prepared_input_ordinal) WHERE start_session_id<>''");
        db.execSQL("CREATE TABLE chunks (job_id INTEGER, idx INTEGER, stable_id TEXT, start_offset INTEGER, end_offset INTEGER, context_start_offset INTEGER, context_end_offset INTEGER, source TEXT, source_hash TEXT, normalized_source_hash TEXT, translated TEXT, status TEXT, error TEXT, request_hash TEXT, response_hash TEXT, attempt INTEGER, created_at INTEGER, updated_at INTEGER, persisted_response TEXT, rejected_response TEXT, finish_reason TEXT, validation_warnings TEXT, qa_json TEXT, result_source TEXT, parent_stable_id TEXT, PRIMARY KEY(job_id, idx))");
        createReliabilityTables(db);
        createMetricsTable(db);
        createBenchmarkTables(db);
        createPreparedPlanTables(db);
        createEditorialTables(db);
        createEditorialPackTables(db);
        createEditorialPackCompatibilityEvaluationTables(db);
        createEditorialLineageTables(db);
        createEditorialIdentityTables(db);
        createEditorialRunDeclarationEventTables(db);
        createEditorialP4BindingTables(db);
        createEditorialP5CTables(db);
        createEditorialP5DTables(db);
        createEditorialP5DTransportFields(db);
        createEditorialP5DRecoveryHistory(db);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE jobs ADD COLUMN input_hash TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE jobs ADD COLUMN settings_hash TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 3) createMetricsTable(db);
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE chunk_metrics ADD COLUMN baseline_input_tokens INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        }
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE chunk_metrics ADD COLUMN cached_input_tokens INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE chunk_metrics ADD COLUMN usage_source TEXT DEFAULT 'UNKNOWN'"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE chunk_metrics ADD COLUMN cost_status TEXT DEFAULT 'UNKNOWN_USAGE'"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE chunk_metrics ADD COLUMN logical_request_id TEXT DEFAULT ''"); } catch (Exception ignored) {}
        }
        if (oldVersion < 6) createBenchmarkTables(db);
        if (oldVersion < 7) {
            try { db.execSQL("ALTER TABLE benchmark_results ADD COLUMN usage_source TEXT DEFAULT 'UNKNOWN'"); } catch(Exception ignored) {}
            try { db.execSQL("ALTER TABLE benchmark_results ADD COLUMN cost_status TEXT DEFAULT 'UNKNOWN_USAGE'"); } catch(Exception ignored) {}
        }
        if (oldVersion < 8) {
            for (String sql : DatabaseMigrationSpec.from42To43()) safeExec(db, sql);
            createReliabilityTables(db);
            backfillReliabilityMetadata(db);
            recoverInterruptedChunks(db);
        }
        if (oldVersion < 9) {
            createPreparedPlanTables(db);
            safeExec(db, "ALTER TABLE jobs ADD COLUMN prepared_batch_id TEXT DEFAULT ''");
            safeExec(db, "ALTER TABLE jobs ADD COLUMN prepared_input_ordinal INTEGER DEFAULT -1");
            safeExec(db, "ALTER TABLE chunk_attempts ADD COLUMN phase TEXT DEFAULT 'translate'");
            safeExec(db, "ALTER TABLE chunk_attempts ADD COLUMN logical_request_id TEXT DEFAULT ''");
            safeExec(db, "ALTER TABLE chunk_attempts ADD COLUMN provider_response_id TEXT DEFAULT ''");
            safeExec(db, "ALTER TABLE chunk_attempts ADD COLUMN response_content TEXT DEFAULT ''");
            safeExec(db, "ALTER TABLE chunk_attempts ADD COLUMN network_state TEXT DEFAULT 'CREATED'");
            safeExec(db, "CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_prepared_identity ON jobs(prepared_batch_id,prepared_input_ordinal) WHERE prepared_batch_id<>''");
        }
        if (oldVersion < 10) {
            safeExec(db, "DROP INDEX IF EXISTS idx_jobs_prepared_identity");
            safeExec(db, "ALTER TABLE jobs ADD COLUMN start_session_id TEXT DEFAULT ''");
            safeExec(db, "CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_start_session ON jobs(start_session_id,prepared_input_ordinal) WHERE start_session_id<>''");
        }
        if (oldVersion < 11) createEditorialTables(db);
        if (oldVersion < 12) createEditorialProjectAssets(db);
        if (oldVersion < 13) createEditorialReferenceProfiles(db);
        if (oldVersion < 14) createEditorialPackTables(db);
        if (oldVersion < 15) createEditorialPackCompatibilityEvaluationTables(db);
        if (oldVersion < 16) createEditorialLineageTables(db);
        if (oldVersion < 17) createEditorialIdentityTables(db);
        if (oldVersion < 18) createEditorialRunDeclarationEventTables(db);
        if (oldVersion < 19) createEditorialP4BindingTables(db);
        if (oldVersion < 20) createEditorialP5CTables(db);
        if (oldVersion < 21) createEditorialP5DTables(db);
        if (oldVersion < 22) createEditorialP5DTransportFields(db);
        if (oldVersion < 23) createEditorialP5DRecoveryHistory(db);
    }

    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Editorial database downgrade is not supported");
    }

    private static void safeExec(SQLiteDatabase db, String sql) { try { db.execSQL(sql); } catch (Exception ignored) {} }
    private static void createReliabilityTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chunk_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, stable_id TEXT, chunk_idx INTEGER, attempt INTEGER, phase TEXT DEFAULT 'translate', status TEXT, network_state TEXT DEFAULT 'CREATED', logical_request_id TEXT DEFAULT '', request_hash TEXT, response_hash TEXT, response_preview TEXT, response_content TEXT DEFAULT '', provider_response_id TEXT DEFAULT '', finish_reason TEXT, validation_result TEXT, error TEXT, model TEXT, provider TEXT, input_tokens INTEGER DEFAULT 0, output_tokens INTEGER DEFAULT 0, cached_tokens INTEGER DEFAULT 0, actual_cost REAL DEFAULT 0, usage_source TEXT DEFAULT 'UNKNOWN', created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunk_attempts_job ON chunk_attempts(job_id,chunk_idx,attempt)");
        db.execSQL("CREATE TABLE IF NOT EXISTS config_revisions (job_id INTEGER, revision INTEGER, first_chunk_idx INTEGER, settings_json TEXT, settings_hash TEXT, created_at INTEGER, PRIMARY KEY(job_id,revision))");
    }

    private static void createPreparedPlanTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS prepared_batches (id TEXT PRIMARY KEY, selection_key TEXT UNIQUE, construction_key TEXT, estimate_key TEXT, settings_json TEXT, status TEXT, progress INTEGER, error TEXT, exact_chunks INTEGER, source_tokens INTEGER, input_low INTEGER, input_high INTEGER, output_low INTEGER, output_high INTEGER, total_low INTEGER, total_high INTEGER, expected_cached INTEGER, expected_uncached INTEGER, cost_low REAL, cost_high REAL, no_cache_low REAL, no_cache_high REAL, pricing_available INTEGER, cache_pricing_available INTEGER, tokenizer_state TEXT, pricing_source TEXT, created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS prepared_inputs (batch_id TEXT, ordinal INTEGER, input_uri TEXT, display_name TEXT, input_hash TEXT, encoding TEXT, chunk_count INTEGER, PRIMARY KEY(batch_id,ordinal))");
        db.execSQL("CREATE TABLE IF NOT EXISTS prepared_chunks (batch_id TEXT, input_ordinal INTEGER, idx INTEGER, stable_id TEXT, start_offset INTEGER, end_offset INTEGER, context_start_offset INTEGER, context_end_offset INTEGER, context_before TEXT, source TEXT, context_after TEXT, source_hash TEXT, normalized_source_hash TEXT, parent_stable_id TEXT, PRIMARY KEY(batch_id,input_ordinal,idx))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_prepared_selection ON prepared_batches(selection_key,status)");
    }

    private static void createEditorialTables(SQLiteDatabase db) {
        for (String sql : EditorialMigrationSpec.from10To11()) safeExec(db, sql);
        createEditorialProjectAssets(db);
        createEditorialReferenceProfiles(db);
    }
    private static void createEditorialProjectAssets(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from11To12())safeExec(db,sql);}
    private static void createEditorialReferenceProfiles(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from12To13())safeExec(db,sql);}
    private static void createEditorialPackTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from13To14())db.execSQL(sql);}
    private static void createEditorialPackCompatibilityEvaluationTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from14To15())db.execSQL(sql);}
    private static void createEditorialLineageTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from15To16())db.execSQL(sql);}
    private static void createEditorialIdentityTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from16To17())db.execSQL(sql);}
    private static void createEditorialRunDeclarationEventTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from17To18())db.execSQL(sql);}
    private static void createEditorialP4BindingTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from18To19())db.execSQL(sql);}
    private static void createEditorialP5CTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from19To20())db.execSQL(sql);}
    private static void createEditorialP5DTables(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from20To21())db.execSQL(sql);}
    private static void createEditorialP5DTransportFields(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from21To22())safeExec(db,sql);}
    private static void createEditorialP5DRecoveryHistory(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from22To23())db.execSQL(sql);}

    SQLiteDatabase editorialWritableDatabase() { return getWritableDatabase(); }
    SQLiteDatabase editorialReadableDatabase() { return getReadableDatabase(); }

    private static void recoverInterruptedChunks(SQLiteDatabase db) {
        ContentValues unknown=new ContentValues();unknown.put("status","DELIVERY_UNKNOWN");unknown.put("error","Process died after request sending began; review before retrying to avoid duplicate billing");unknown.put("updated_at",System.currentTimeMillis());
        db.update("chunks",unknown,"UPPER(status) IN ('PREPARING','REQUESTING') AND EXISTS (SELECT 1 FROM chunk_attempts a WHERE a.job_id=chunks.job_id AND a.chunk_idx=chunks.idx AND UPPER(a.status) IN ('SEND_STARTED','REQUEST_BODY_SENT','RESPONSE_HEADERS'))",null);
        ContentValues v=new ContentValues();v.put("status","RETRYABLE_ERROR");v.put("error","Interrupted before network send; safe to retry");v.put("updated_at",System.currentTimeMillis());
        db.update("chunks",v,"UPPER(status) IN ('PREPARING','REQUESTING')",null);
        ContentValues jobs=new ContentValues();jobs.put("status","NEEDS_REVIEW");jobs.put("updated_at",System.currentTimeMillis());db.update("jobs",jobs,"EXISTS (SELECT 1 FROM chunks c WHERE c.job_id=jobs.id AND UPPER(c.status)='DELIVERY_UNKNOWN')",null);
        ContentValues validating=new ContentValues();validating.put("status","RESPONSE_RECEIVED");validating.put("error","Validation interrupted; persisted response requires revalidation");validating.put("updated_at",System.currentTimeMillis());
        db.update("chunks",validating,"UPPER(status)='VALIDATING' AND persisted_response<>''",null);
    }

    private static void backfillReliabilityMetadata(SQLiteDatabase db){
        long currentJob=-1;int offset=0;
        try(Cursor c=db.rawQuery("SELECT job_id,idx,source,status,stable_id,start_offset,end_offset,updated_at FROM chunks ORDER BY job_id,idx",null)){
            while(c.moveToNext()){long job=c.getLong(0);if(job!=currentJob){currentJob=job;offset=0;}int idx=c.getInt(1);String source=safe(c.getString(2));String status=safe(c.getString(3));String stable=safe(c.getString(4));int start=c.getInt(5),end=c.getInt(6);ContentValues v=new ContentValues();
                if(start<0||end<0){start=offset;end=start+source.length();v.put("start_offset",start);v.put("end_offset",end);v.put("context_start_offset",start);v.put("context_end_offset",end);}offset=end;
                String sourceHash=HashUtil.sha256(source);v.put("source_hash",sourceHash);v.put("normalized_source_hash",HashUtil.sha256(Chunker.normalizeSource(source)));if(stable.isEmpty())v.put("stable_id",Chunk.stableIdentity(start,end,sourceHash,""));v.put("created_at",c.getLong(7));
                if("done".equalsIgnoreCase(status))v.put("status","COMPLETED");else if("error".equalsIgnoreCase(status))v.put("status","RETRYABLE_ERROR");else if("pending".equalsIgnoreCase(status))v.put("status","PENDING");
                db.update("chunks",v,"job_id=? AND idx=?",args(job,idx));
            }
        }
    }
    @Override public void onOpen(SQLiteDatabase db) { super.onOpen(db); }
    public void recoverInterruptedState(){recoverInterruptedChunks(getWritableDatabase());}

    public void savePreparedBatch(PreparedBatch batch) {
        if (batch == null || !batch.ready()) throw new IllegalArgumentException("Prepared batch is not ready");
        SQLiteDatabase db=getWritableDatabase(); long now=System.currentTimeMillis(); db.beginTransaction();
        try {
            try(Cursor old=db.rawQuery("SELECT id FROM prepared_batches WHERE selection_key=?",new String[]{batch.selectionKey})) {
                while(old.moveToNext()) deletePreparedBatch(db,old.getString(0));
            }
            CostEstimator.Estimate e=batch.estimate; ContentValues b=new ContentValues();
            b.put("id",batch.id);b.put("selection_key",batch.selectionKey);b.put("construction_key",batch.constructionKey);b.put("estimate_key",batch.estimateKey);
            b.put("settings_json",batch.settingsJson);b.put("status","READY");b.put("progress",100);b.put("error","");b.put("exact_chunks",batch.exactChunkCount());
            b.put("source_tokens",e.sourceTokens);b.put("input_low",e.inputTokensLow);b.put("input_high",e.inputTokensHigh);b.put("output_low",e.outputTokensLow);b.put("output_high",e.outputTokensHigh);
            b.put("total_low",e.totalTokensLow);b.put("total_high",e.totalTokensHigh);b.put("expected_cached",e.expectedCachedInputTokens);b.put("expected_uncached",e.expectedUncachedInputTokens);
            b.put("cost_low",e.costLow);b.put("cost_high",e.costHigh);b.put("no_cache_low",e.noCacheCostLow);b.put("no_cache_high",e.noCacheCostHigh);
            b.put("pricing_available",e.pricingAvailable?1:0);b.put("cache_pricing_available",e.cachePricingAvailable?1:0);b.put("tokenizer_state",safe(e.tokenizerState));b.put("pricing_source",safe(e.pricingSource));
            b.put("created_at",batch.createdAt);b.put("updated_at",now);db.insertOrThrow("prepared_batches",null,b);
            for(PreparedBatch.Input input:batch.inputs){ContentValues in=new ContentValues();in.put("batch_id",batch.id);in.put("ordinal",input.ordinal);in.put("input_uri",input.uri==null?"":input.uri.toString());in.put("display_name",input.displayName);in.put("input_hash",input.inputHash);in.put("encoding",input.encoding);in.put("chunk_count",input.chunks.size());db.insertOrThrow("prepared_inputs",null,in);
                for(Chunk c:input.chunks){ContentValues ch=new ContentValues();ch.put("batch_id",batch.id);ch.put("input_ordinal",input.ordinal);ch.put("idx",c.index);ch.put("stable_id",c.stableId);ch.put("start_offset",c.startOffset);ch.put("end_offset",c.endOffset);ch.put("context_start_offset",c.contextStartOffset);ch.put("context_end_offset",c.contextEndOffset);ch.put("context_before",c.contextBefore);ch.put("source",c.mainContent);ch.put("context_after",c.contextAfter);ch.put("source_hash",c.sourceHash);ch.put("normalized_source_hash",c.normalizedSourceHash);ch.put("parent_stable_id",c.parentStableId);db.insertOrThrow("prepared_chunks",null,ch);}
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public PreparedBatch getPreparedBatchBySelectionKey(String key) {
        if(key==null||key.isEmpty())return null;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM prepared_batches WHERE selection_key=? AND status='READY' ORDER BY updated_at DESC LIMIT 1",new String[]{key})) { return c.moveToFirst()?getPreparedBatch(c.getString(0)):null; }
    }

    public PreparedBatch getPreparedBatch(String id) {
        if(id==null||id.isEmpty())return null; PreparedBatch b=new PreparedBatch();
        String sql="SELECT id,selection_key,construction_key,estimate_key,settings_json,status,progress,error,created_at,source_tokens,input_low,input_high,output_low,output_high,total_low,total_high,expected_cached,expected_uncached,cost_low,cost_high,no_cache_low,no_cache_high,pricing_available,cache_pricing_available,tokenizer_state,pricing_source FROM prepared_batches WHERE id=?";
        try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{id})) { if(!c.moveToFirst())return null;b.id=c.getString(0);b.selectionKey=c.getString(1);b.constructionKey=c.getString(2);b.estimateKey=c.getString(3);b.settingsJson=c.getString(4);try{b.status=PreparedBatch.Status.valueOf(c.getString(5));}catch(Exception ignored){b.status=PreparedBatch.Status.ERROR;}b.progress=c.getInt(6);b.error=safe(c.getString(7));b.createdAt=c.getLong(8);CostEstimator.Estimate e=new CostEstimator.Estimate();e.sourceTokens=c.getInt(9);e.inputTokensLow=c.getInt(10);e.inputTokensHigh=c.getInt(11);e.outputTokensLow=c.getInt(12);e.outputTokensHigh=c.getInt(13);e.totalTokensLow=c.getInt(14);e.totalTokensHigh=c.getInt(15);e.expectedCachedInputTokens=c.getInt(16);e.expectedUncachedInputTokens=c.getInt(17);e.costLow=c.getDouble(18);e.costHigh=c.getDouble(19);e.noCacheCostLow=c.getDouble(20);e.noCacheCostHigh=c.getDouble(21);e.pricingAvailable=c.getInt(22)!=0;e.cachePricingAvailable=c.getInt(23)!=0;e.tokenizerState=safe(c.getString(24));e.pricingSource=safe(c.getString(25));b.estimate=e;}
        try(Cursor c=getReadableDatabase().rawQuery("SELECT ordinal,input_uri,display_name,input_hash,encoding,chunk_count FROM prepared_inputs WHERE batch_id=? ORDER BY ordinal",new String[]{id})) {while(c.moveToNext()){PreparedBatch.Input in=new PreparedBatch.Input();in.ordinal=c.getInt(0);in.uri=android.net.Uri.parse(c.getString(1));in.displayName=safe(c.getString(2));in.inputHash=safe(c.getString(3));in.encoding=safe(c.getString(4));in.chunks=getPreparedChunks(id,in.ordinal);b.inputs.add(in);b.estimate.chunks+=in.chunks.size();}}
        return b;
    }

    public List<Chunk> getPreparedChunks(String batchId,int ordinal){ArrayList<Chunk> out=new ArrayList<>();String sql="SELECT idx,start_offset,end_offset,context_before,source,context_after,parent_stable_id,stable_id FROM prepared_chunks WHERE batch_id=? AND input_ordinal=? ORDER BY idx";try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{batchId,String.valueOf(ordinal)})){while(c.moveToNext()){Chunk ch=new Chunk(c.getInt(0),c.getInt(1),c.getInt(2),safe(c.getString(3)),safe(c.getString(4)),safe(c.getString(5)),safe(c.getString(6)));String stable=safe(c.getString(7));if(!stable.isEmpty())ch.stableId=stable;out.add(ch);}}Chunker.assignParagraphRanges(out);return out;}

    private static void deletePreparedBatch(SQLiteDatabase db,String id){db.delete("prepared_chunks","batch_id=?",new String[]{id});db.delete("prepared_inputs","batch_id=?",new String[]{id});db.delete("prepared_batches","id=?",new String[]{id});}

    private static void createBenchmarkTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS benchmark_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, status TEXT, model TEXT, provider TEXT, budget_usd REAL, spent_usd REAL DEFAULT 0, sample_count INTEGER, created_at INTEGER, updated_at INTEGER, error TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS benchmark_results (session_id INTEGER, chunk_idx INTEGER, pipeline TEXT, blind_label TEXT, input_tokens INTEGER, cached_tokens INTEGER, output_tokens INTEGER, cost_usd REAL, usage_source TEXT DEFAULT 'UNKNOWN', cost_status TEXT DEFAULT 'UNKNOWN_USAGE', duration_ms INTEGER, finish_reason TEXT, quality_issues TEXT, output TEXT, PRIMARY KEY(session_id,chunk_idx,pipeline))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_benchmark_job ON benchmark_sessions(job_id,created_at)");
    }

    public long createBenchmarkSession(long jobId, AppSettings s, double budgetUsd, int samples) {
        ContentValues v=new ContentValues(); long now=System.currentTimeMillis();
        v.put("job_id",jobId); v.put("status","running"); v.put("model",s.model); v.put("provider",s.provider);
        v.put("budget_usd",budgetUsd); v.put("spent_usd",0); v.put("sample_count",samples); v.put("created_at",now); v.put("updated_at",now); v.put("error","");
        return getWritableDatabase().insert("benchmark_sessions",null,v);
    }

    public void saveBenchmarkResult(long sessionId, int chunkIndex, String pipeline, String blindLabel,
                                    OpenAICompatibleClient.ChatResult usage, double cost, long durationMs,
                                    String finishReason, String qualityIssues, String output) {
        ContentValues v=new ContentValues(); v.put("session_id",sessionId); v.put("chunk_idx",chunkIndex); v.put("pipeline",pipeline); v.put("blind_label",blindLabel);
        v.put("input_tokens",usage==null?0:usage.promptTokens); v.put("cached_tokens",usage==null?0:usage.cachedPromptTokens); v.put("output_tokens",usage==null?0:usage.completionTokens);
        v.put("cost_usd",cost); v.put("duration_ms",durationMs); v.put("finish_reason",finishReason); v.put("quality_issues",qualityIssues); v.put("output",output==null?"":output);
        v.put("usage_source",usage!=null&&usage.usageReported?"PROVIDER":"UNKNOWN_USAGE");
        v.put("cost_status",usage!=null&&usage.providerCostReported?"PROVIDER_REPORTED":usage!=null&&usage.usageReported?"USAGE_DERIVED":"UNKNOWN_USAGE");
        getWritableDatabase().insertWithOnConflict("benchmark_results",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void finishBenchmark(long sessionId, String status, double spent, String error) {
        ContentValues v=new ContentValues(); v.put("status",status); v.put("spent_usd",spent); v.put("updated_at",System.currentTimeMillis()); v.put("error",error==null?"":error);
        getWritableDatabase().update("benchmark_sessions",v,"id=?",new String[]{String.valueOf(sessionId)});
    }

    public BenchmarkSummary latestBenchmark(long jobId) {
        String sql="SELECT id,status,model,provider,budget_usd,spent_usd,sample_count,created_at,error FROM benchmark_sessions WHERE job_id=? ORDER BY created_at DESC LIMIT 1";
        try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(jobId)})){ if(c.moveToFirst()){ BenchmarkSummary b=new BenchmarkSummary(); b.id=c.getLong(0);b.status=c.getString(1);b.model=c.getString(2);b.provider=c.getString(3);b.budgetUsd=c.getDouble(4);b.spentUsd=c.getDouble(5);b.sampleCount=c.getInt(6);b.createdAt=c.getLong(7);b.error=c.getString(8); return b; }} return null;
    }

    public List<BenchmarkResultRow> benchmarkResults(long sessionId) {
        ArrayList<BenchmarkResultRow> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT chunk_idx,pipeline,blind_label,input_tokens,cached_tokens,output_tokens,cost_usd,usage_source,cost_status,duration_ms,finish_reason,quality_issues,output FROM benchmark_results WHERE session_id=? ORDER BY chunk_idx,blind_label",new String[]{String.valueOf(sessionId)})){
            while(c.moveToNext()){BenchmarkResultRow r=new BenchmarkResultRow();r.chunkIndex=c.getInt(0);r.pipeline=c.getString(1);r.blindLabel=c.getString(2);r.inputTokens=c.getInt(3);r.cachedTokens=c.getInt(4);r.outputTokens=c.getInt(5);r.costUsd=c.getDouble(6);r.usageSource=c.getString(7);r.costStatus=c.getString(8);r.durationMs=c.getLong(9);r.finishReason=c.getString(10);r.qualityIssues=c.getString(11);r.output=c.getString(12);out.add(r);}
        } return out;
    }

    public static class BenchmarkSummary { public long id,createdAt; public String status,model,provider,error; public double budgetUsd,spentUsd; public int sampleCount; }
    public static class BenchmarkResultRow { public int chunkIndex,inputTokens,cachedTokens,outputTokens; public String pipeline,blindLabel,usageSource,costStatus,finishReason,qualityIssues,output; public double costUsd; public long durationMs; }

    private static void createMetricsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chunk_metrics (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, chunk_idx INTEGER, phase TEXT, attempt INTEGER, preset TEXT, provider TEXT, model TEXT, source_chars INTEGER, raw_tokens INTEGER, system_tokens INTEGER, instruction_tokens INTEGER, glossary_tokens INTEGER, pronoun_tokens INTEGER, history_tokens INTEGER, context_tokens INTEGER, estimated_input_tokens INTEGER, actual_input_tokens INTEGER, output_tokens INTEGER, retry_tokens INTEGER, max_output_tokens INTEGER, baseline_input_tokens INTEGER DEFAULT 0, cached_input_tokens INTEGER DEFAULT 0, usage_source TEXT DEFAULT 'UNKNOWN', cost_status TEXT DEFAULT 'UNKNOWN_USAGE', logical_request_id TEXT DEFAULT '', duration_ms INTEGER, estimated_cost REAL, usage_cost REAL, outcome TEXT, retry_reason TEXT, created_at INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunk_metrics_job ON chunk_metrics(job_id, chunk_idx, phase, attempt)");
    }

    public void recordMetric(ChunkMetric m) {
        if (m == null || m.jobId <= 0) return;
        ContentValues v = new ContentValues();
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
        getWritableDatabase().insert("chunk_metrics", null, v);
    }

    public MetricsSummary getMetricsSummary(long jobId) {
        MetricsSummary m = new MetricsSummary();
        String sql = "SELECT COALESCE(SUM(raw_tokens),0),COALESCE(SUM(system_tokens),0),COALESCE(SUM(instruction_tokens),0),COALESCE(SUM(glossary_tokens),0),COALESCE(SUM(pronoun_tokens),0),COALESCE(SUM(history_tokens+context_tokens),0),COALESCE(SUM(output_tokens),0),COALESCE(SUM(retry_tokens),0),COALESCE(SUM(CASE WHEN cost_status IN ('USAGE_DERIVED','PROVIDER_REPORTED') THEN usage_cost ELSE 0 END),0),COUNT(*),COALESCE(SUM(CASE WHEN outcome='success' THEN 1 ELSE 0 END),0),COALESCE(SUM(baseline_input_tokens),0),COALESCE(SUM(estimated_input_tokens),0),COALESCE(SUM(cached_input_tokens),0),COALESCE(SUM(CASE WHEN usage_source!='PROVIDER' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN cost_status LIKE 'UNKNOWN%' THEN 1 ELSE 0 END),0),COALESCE(SUM(actual_input_tokens),0),COALESCE(SUM(MAX(0,actual_input_tokens-cached_input_tokens)),0),COALESCE(SUM(CASE WHEN estimated_cost>=0 THEN estimated_cost ELSE 0 END),0),COALESCE(SUM(CASE WHEN usage_source='PROVIDER' THEN 1 ELSE 0 END),0) FROM chunk_metrics WHERE job_id=?";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(jobId)})) {
            if (c.moveToFirst()) { m.rawTokens=c.getLong(0); m.systemTokens=c.getLong(1); m.instructionTokens=c.getLong(2); m.glossaryTokens=c.getLong(3); m.pronounTokens=c.getLong(4); m.contextTokens=c.getLong(5); m.outputTokens=c.getLong(6); m.retryTokens=c.getLong(7); m.totalCost=c.getDouble(8); m.attempts=c.getInt(9); m.successes=c.getInt(10); m.baselineInputTokens=c.getLong(11); m.estimatedInputTokens=c.getLong(12); m.cachedInputTokens=c.getLong(13); m.estimatedUsageRows=c.getInt(14); m.unknownCostRows=c.getInt(15); m.actualInputTokens=c.getLong(16); m.uncachedInputTokens=c.getLong(17); m.estimatedCostBefore=c.getDouble(18); m.providerUsageRows=c.getInt(19); }
        }
        return m;
    }

    public TokenCalibration getTokenCalibration(long jobId){TokenCalibration t=new TokenCalibration();String sql="SELECT COALESCE(SUM(estimated_input_tokens),0),COALESCE(SUM(actual_input_tokens),0),COUNT(*) FROM chunk_metrics WHERE job_id=? AND usage_source='PROVIDER' AND actual_input_tokens>0";try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(jobId)})){if(c.moveToFirst()){t.estimated=c.getLong(0);t.actual=c.getLong(1);t.samples=c.getInt(2);}}return t;}
    public static class TokenCalibration { public long estimated,actual;public int samples;public double ratio(){return estimated<=0?1d:actual/(double)estimated;} }

    public static class MetricsSummary {
        public long rawTokens, systemTokens, instructionTokens, glossaryTokens, pronounTokens, contextTokens, outputTokens, retryTokens, baselineInputTokens, estimatedInputTokens;
        public long cachedInputTokens, actualInputTokens, uncachedInputTokens; public double totalCost, estimatedCostBefore; public int attempts, successes, estimatedUsageRows, unknownCostRows, providerUsageRows;
        public long estimatedSavedInputTokens() { return Math.max(0, baselineInputTokens - estimatedInputTokens); }
    }

    public long createJob(String inputUri, String outputUri, String fileName, AppSettings s, List<Chunk> chunks, String inputHash, String settingsHash) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try { long id=createJobRows(db,inputUri,outputUri,fileName,s,chunks,inputHash,settingsHash,"",-1,"");db.setTransactionSuccessful();return id; }
        finally { db.endTransaction(); }
    }

    private long createJobRows(SQLiteDatabase db,String inputUri,String outputUri,String fileName,AppSettings s,List<Chunk> chunks,String inputHash,String settingsHash,String preparedBatchId,int preparedOrdinal,String startSessionId) {
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("input_uri", inputUri);
        cv.put("output_uri", outputUri);
        cv.put("file_name", fileName);
        cv.put("source_language", s.sourceLanguage);
        cv.put("target_language", s.targetLanguage);
        cv.put("status", "READY");
        cv.put("created_at", now);
        cv.put("updated_at", now);
        cv.put("settings", SettingsStore.toJson(s));
        cv.put("input_hash", inputHash == null ? "" : inputHash);
        cv.put("settings_hash", settingsHash == null ? "" : settingsHash);
        cv.put("integrity_status", "PENDING"); cv.put("config_revision",1); cv.put("app_version",AppBuildInfo.VERSION_NAME);
        if(preparedBatchId!=null&&!preparedBatchId.isEmpty()){cv.put("prepared_batch_id",preparedBatchId);cv.put("prepared_input_ordinal",preparedOrdinal);}
        if(startSessionId!=null&&!startSessionId.isEmpty())cv.put("start_session_id",startSessionId);
        long id = db.insertOrThrow("jobs", null, cv);
        ContentValues revision=new ContentValues();revision.put("job_id",id);revision.put("revision",1);revision.put("first_chunk_idx",0);revision.put("settings_json",SettingsStore.toJson(s));revision.put("settings_hash",settingsHash==null?"":settingsHash);revision.put("created_at",now);db.insertOrThrow("config_revisions",null,revision);
        for (Chunk c : chunks) {
            ContentValues cc = new ContentValues();
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
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{try(Cursor c=db.rawQuery("SELECT id FROM jobs WHERE start_session_id=? AND prepared_input_ordinal=? ORDER BY id DESC LIMIT 1",new String[]{safe(startSessionId),String.valueOf(ordinal)})){if(c.moveToFirst()){long id=c.getLong(0);db.setTransactionSuccessful();return id;}}
            long id=createJobRows(db,inputUri,outputUri,fileName,s,chunks,inputHash,settingsHash,safe(batchId),ordinal,startSessionId);db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }

    public Job findDoneJobByHashes(String inputHash, String settingsHash) {
        if (inputHash == null || inputHash.isEmpty() || settingsHash == null || settingsHash.isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs WHERE input_hash=? AND settings_hash=? AND UPPER(status) IN ('DONE','COMPLETED','COMPLETED_WITH_WARNINGS') ORDER BY updated_at DESC LIMIT 1", new String[]{inputHash, settingsHash})) {
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
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs WHERE UPPER(status) IN ('RUNNING','PAUSED','ERROR','RETRYABLE_ERROR','NEEDS_REVIEW','FAILED_INTEGRITY_CHECK') ORDER BY updated_at DESC LIMIT 1", null)) {
            if (c.moveToFirst()) {
                return readJob(c);
            }
        }
        return null;
    }


    public Job getLastJobWithFailedChunks() {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT j.id,j.input_uri,j.output_uri,j.file_name,j.settings,j.status " +
                "FROM jobs j WHERE EXISTS (SELECT 1 FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) " +
                "ORDER BY j.updated_at DESC LIMIT 1";
        try (Cursor c = db.rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                return readJob(c);
            }
        }
        return null;
    }


    public Job getJob(long jobId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs WHERE id=? LIMIT 1", new String[]{String.valueOf(jobId)})) {
            if (c.moveToFirst()) return readJob(c);
        }
        return null;
    }

    public String getInputHash(long jobId){try(Cursor c=getReadableDatabase().rawQuery("SELECT input_hash FROM jobs WHERE id=?",new String[]{String.valueOf(jobId)})){return c.moveToFirst()?safe(c.getString(0)):"";}}

    /** Applies an immutable settings snapshot from the first unfinished chunk onward. */
    public int applyConfigRevision(long jobId, int firstChunkIndex, AppSettings settings) {
        if (settings == null) throw new IllegalArgumentException("Settings are required");
        SQLiteDatabase db=getWritableDatabase(); long now=System.currentTimeMillis(); int next=1;
        try(Cursor c=db.rawQuery("SELECT COALESCE(MAX(revision),0)+1 FROM config_revisions WHERE job_id=?",new String[]{String.valueOf(jobId)})){if(c.moveToFirst())next=c.getInt(0);}
        AppSettings normalized=AppValidator.normalize(settings); String json=SettingsStore.toJson(normalized); String hash=HashUtil.settingsHash(normalized);
        db.beginTransaction();
        try {
            ContentValues r=new ContentValues();r.put("job_id",jobId);r.put("revision",next);r.put("first_chunk_idx",Math.max(0,firstChunkIndex));r.put("settings_json",json);r.put("settings_hash",hash);r.put("created_at",now);db.insertOrThrow("config_revisions",null,r);
            ContentValues j=new ContentValues();j.put("settings",json);j.put("settings_hash",hash);j.put("config_revision",next);j.put("updated_at",now);db.update("jobs",j,"id=?",new String[]{String.valueOf(jobId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        ObservabilityLog.event("config_revision_applied","job",jobId,"revision",next,"firstChunk",firstChunkIndex,"provider",normalized.provider,"model",normalized.model);
        return next;
    }

    public int firstRunnableChunk(long jobId) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MIN(idx),-1) FROM chunks WHERE job_id=? AND UPPER(status) NOT IN ('SUPERSEDED','DONE','COMPLETED','DELIVERY_UNKNOWN')",new String[]{String.valueOf(jobId)})){return c.moveToFirst()?c.getInt(0):-1;}
    }

    public String chunkStatus(long jobId,int idx){try(Cursor c=getReadableDatabase().rawQuery("SELECT status FROM chunks WHERE job_id=? AND idx=?",args(jobId,idx))){return c.moveToFirst()?safe(c.getString(0)):"";}}

    public List<ChunkRow> getChunkRows(long jobId) {
        ArrayList<ChunkRow> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT idx,source,translated,status,error,stable_id,start_offset,end_offset,source_hash,normalized_source_hash,request_hash,response_hash,attempt,finish_reason,validation_warnings,qa_json,result_source,rejected_response,parent_stable_id,persisted_response FROM chunks WHERE job_id=? AND UPPER(status)<>'SUPERSEDED' ORDER BY idx", new String[]{String.valueOf(jobId)})) {
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
        if(children==null||children.size()<2)throw new IllegalArgumentException("Context overflow chunk cannot be split further");SQLiteDatabase db=getWritableDatabase();long now=System.currentTimeMillis();int delta=children.size()-1;db.beginTransaction();
        try{db.execSQL("UPDATE chunks SET idx=idx+100000 WHERE job_id=? AND idx>?",new Object[]{jobId,original.index});db.execSQL("UPDATE chunks SET idx=idx-100000+? WHERE job_id=? AND idx>=100000",new Object[]{delta,jobId});int supersededIndex=-1;try(Cursor c=db.rawQuery("SELECT COALESCE(MIN(idx),0)-1 FROM chunks WHERE job_id=?",new String[]{String.valueOf(jobId)})){if(c.moveToFirst())supersededIndex=c.getInt(0);}ContentValues old=new ContentValues();old.put("idx",supersededIndex);old.put("status","SUPERSEDED");old.put("error","Context limit exceeded; replaced by "+children.size()+" stable subchunks");old.put("updated_at",now);db.update("chunks",old,"job_id=? AND idx=?",args(jobId,original.index));
            for(int i=0;i<children.size();i++){Chunk child=children.get(i);child.index=original.index+i;ContentValues v=new ContentValues();v.put("job_id",jobId);v.put("idx",child.index);v.put("stable_id",child.stableId);v.put("start_offset",child.startOffset);v.put("end_offset",child.endOffset);v.put("context_start_offset",child.contextStartOffset);v.put("context_end_offset",child.contextEndOffset);v.put("source",child.mainContent);v.put("source_hash",child.sourceHash);v.put("normalized_source_hash",child.normalizedSourceHash);v.put("translated","");v.put("status","PENDING");v.put("error","");v.put("attempt",0);v.put("created_at",now);v.put("updated_at",now);v.put("result_source","PROVIDER");v.put("parent_stable_id",original.stableId);db.insertOrThrow("chunks",null,v);}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    public void markChunkDone(long jobId, int idx, String text) {
        commitValidatedChunk(jobId, idx, text, "", "PROVIDER");
    }

    public void beginChunkAttempt(long jobId, Chunk chunk, int attempt, String requestHash, AppSettings settings) {
        beginChunkAttempt(jobId,chunk,attempt,"translate",requestHash,jobId+":"+chunk.index+":translate:"+attempt,settings);
    }

    public void beginChunkAttempt(long jobId,Chunk chunk,int attempt,String phase,String requestHash,String logicalRequestId,AppSettings settings){
        SQLiteDatabase db=getWritableDatabase();long now=System.currentTimeMillis();
        boolean preservesAccepted=hasAcceptedTranslation(db,jobId,chunk.index);ContentValues c=new ContentValues();c.put("status",preservesAccepted?"COMPLETED":"REQUESTING");c.put("attempt",attempt);c.put("request_hash",safe(requestHash));c.put("error","");c.put("updated_at",now);db.update("chunks",c,"job_id=? AND idx=?",args(jobId,chunk.index));
        ContentValues a=new ContentValues();a.put("job_id",jobId);a.put("stable_id",chunk.stableId);a.put("chunk_idx",chunk.index);a.put("attempt",attempt);a.put("phase",safe(phase));a.put("status","CREATED");a.put("network_state","CREATED");a.put("logical_request_id",safe(logicalRequestId));a.put("request_hash",safe(requestHash));a.put("model",settings==null?"":settings.model);a.put("provider",settings==null?"":settings.provider);a.put("created_at",now);a.put("updated_at",now);db.insertOrThrow("chunk_attempts",null,a);
    }

    public void markAttemptPhase(long jobId,int idx,int attempt,String phase,String state){ContentValues v=new ContentValues();v.put("status",safe(state));v.put("network_state",safe(state));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("chunk_attempts",v,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});ObservabilityLog.event("attempt_state","job",jobId,"chunk",idx,"attempt",attempt,"phase",phase,"state",state);}

    public void markValidating(long jobId,int idx,int attempt,String phase){SQLiteDatabase db=getWritableDatabase();ContentValues c=new ContentValues();if(!hasAcceptedTranslation(db,jobId,idx)){c.put("status","VALIDATING");c.put("updated_at",System.currentTimeMillis());db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));}markAttemptPhase(jobId,idx,attempt,phase,"VALIDATING");}

    public void markAttemptFailedBeforeSend(long jobId,int idx,int attempt,String phase,String error){ContentValues v=new ContentValues();v.put("status","FAILED_BEFORE_SEND");v.put("network_state","FAILED_BEFORE_SEND");v.put("error",safe(error));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("chunk_attempts",v,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});}

    public void persistReceivedResponse(long jobId,int idx,int attempt,OpenAICompatibleClient.ChatResult result) {
        persistReceivedResponse(jobId,idx,attempt,"translate",result);
    }

    public void persistReceivedResponse(long jobId,int idx,int attempt,String phase,OpenAICompatibleClient.ChatResult result) {
        String raw=result==null?"":result.content,finishReason=result==null?"":result.finishReason;SQLiteDatabase db=getWritableDatabase();long now=System.currentTimeMillis();String hash=HashUtil.sha256(raw);
        boolean preservesAccepted=hasAcceptedTranslation(db,jobId,idx);ContentValues c=new ContentValues();c.put("status",preservesAccepted?"COMPLETED":"RESPONSE_RECEIVED");c.put("persisted_response",safe(raw));if(!preservesAccepted)c.put("response_hash",hash);c.put("finish_reason",safe(finishReason));c.put("updated_at",now);db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));
        ContentValues a=new ContentValues();a.put("status","RESPONSE_RECEIVED");a.put("network_state","RESPONSE_RECEIVED");a.put("response_hash",hash);a.put("response_preview",redactedPreview(raw));a.put("response_content",safe(raw));a.put("provider_response_id",result==null?"":safe(result.providerResponseId));a.put("finish_reason",safe(finishReason));a.put("input_tokens",result==null?0:result.promptTokens);a.put("output_tokens",result==null?0:result.completionTokens);a.put("cached_tokens",result==null?0:result.cachedPromptTokens);a.put("actual_cost",result!=null&&result.providerCostReported?result.providerCost:0);a.put("usage_source",result!=null&&result.usageReported?"PROVIDER":"ESTIMATED");a.put("updated_at",now);db.update("chunk_attempts",a,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});
    }

    public void markDeliveryUnknown(long jobId,int idx,int attempt,String phase,String error){SQLiteDatabase db=getWritableDatabase();ContentValues c=new ContentValues();c.put("status","DELIVERY_UNKNOWN");c.put("error",safe(error));c.put("updated_at",System.currentTimeMillis());db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));markAttemptPhase(jobId,idx,attempt,phase,"DELIVERY_UNKNOWN");touchJob(jobId,"NEEDS_REVIEW");}

    public void rejectResponse(long jobId,int idx,int attempt,String raw,String reason,boolean retryable) {
        rejectResponse(jobId,idx,attempt,"translate",raw,reason,retryable);
    }

    public void rejectResponse(long jobId,int idx,int attempt,String phase,String raw,String reason,boolean retryable) {
        SQLiteDatabase db=getWritableDatabase();long now=System.currentTimeMillis();
        boolean preservesAccepted=hasAcceptedTranslation(db,jobId,idx);ContentValues c=new ContentValues();c.put("status",preservesAccepted?"COMPLETED":retryable?"RETRYABLE_ERROR":"PERMANENT_ERROR");c.put("rejected_response",safe(raw));c.put("error",(preservesAccepted?"Candidate rejected; prior accepted output retained: ":"")+safe(reason));c.put("updated_at",now);db.update("chunks",c,"job_id=? AND idx=?",args(jobId,idx));
        ContentValues a=new ContentValues();a.put("status","REJECTED");a.put("validation_result",safe(reason));a.put("error",safe(reason));a.put("updated_at",now);db.update("chunk_attempts",a,"job_id=? AND chunk_idx=? AND attempt=? AND phase=?",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(attempt),safe(phase)});
    }

    public void commitValidatedChunk(long jobId, int idx, String text, String warnings, String resultSource) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("Cannot commit blank translation");
        SQLiteDatabase db=getWritableDatabase();long now=System.currentTimeMillis();db.beginTransaction();
        try {
        ContentValues cv = new ContentValues();
        cv.put("translated", text);
        cv.put("status", "COMPLETED");
        cv.put("error", "");
        cv.put("response_hash",HashUtil.sha256(text));cv.put("validation_warnings",safe(warnings));cv.put("result_source",safe(resultSource));cv.put("persisted_response","");
        cv.put("updated_at", System.currentTimeMillis());
        if(db.update("chunks", cv, "job_id=? AND idx=?", args(jobId,idx))!=1)throw new IllegalStateException("Chunk missing during commit");
        ContentValues attempt=new ContentValues();attempt.put("status","ACCEPTED");attempt.put("validation_result","PASSED"+(warnings==null||warnings.isEmpty()?"":";"+warnings));attempt.put("updated_at",now);db.update("chunk_attempts",attempt,"job_id=? AND chunk_idx=? AND attempt=(SELECT MAX(attempt) FROM chunk_attempts WHERE job_id=? AND chunk_idx=?)",new String[]{String.valueOf(jobId),String.valueOf(idx),String.valueOf(jobId),String.valueOf(idx)});
        ContentValues j=new ContentValues();j.put("status","running");j.put("updated_at",now);db.update("jobs",j,"id=?",new String[]{String.valueOf(jobId)});
        db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void commitManualReplacement(long jobId,int idx,String text,String warnings){
        long now=System.currentTimeMillis();SQLiteDatabase db=getWritableDatabase();int attempt=1;String stable="";try(Cursor c=db.rawQuery("SELECT COALESCE(attempt,0)+1,stable_id FROM chunks WHERE job_id=? AND idx=?",args(jobId,idx))){if(c.moveToFirst()){attempt=c.getInt(0);stable=safe(c.getString(1));}}
        ContentValues a=new ContentValues();a.put("job_id",jobId);a.put("stable_id",stable);a.put("chunk_idx",idx);a.put("attempt",attempt);a.put("status","MANUAL_VALIDATED");a.put("response_hash",HashUtil.sha256(text));a.put("response_preview",redactedPreview(text));a.put("finish_reason","manual");a.put("validation_result","PASSED;"+safe(warnings));a.put("model","manual");a.put("provider","manual");a.put("created_at",now);a.put("updated_at",now);db.insert("chunk_attempts",null,a);
        ContentValues count=new ContentValues();count.put("attempt",attempt);db.update("chunks",count,"job_id=? AND idx=?",args(jobId,idx));commitValidatedChunk(jobId,idx,text,warnings,"MANUAL");
    }

    public void markChunkError(long jobId, int idx, String error) {
        SQLiteDatabase db=getWritableDatabase();boolean preservesAccepted=hasAcceptedTranslation(db,jobId,idx);
        ContentValues cv = new ContentValues();
        cv.put("status", preservesAccepted?"COMPLETED":"RETRYABLE_ERROR");
        cv.put("error", error == null ? "" : error);
        cv.put("updated_at", System.currentTimeMillis());
        int changed=db.update("chunks", cv, "job_id=? AND idx=? AND UPPER(status)<>'DELIVERY_UNKNOWN'", new String[]{String.valueOf(jobId), String.valueOf(idx)});
        touchJob(jobId, changed==0||preservesAccepted?"NEEDS_REVIEW":"error");
    }

    public void markChunkPermanentError(long jobId,int idx,String error){ContentValues v=new ContentValues();v.put("status","PERMANENT_ERROR");v.put("error",safe(error));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("chunks",v,"job_id=? AND idx=?",args(jobId,idx));touchJob(jobId,"NEEDS_REVIEW");}

    public void touchJob(long jobId, String status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("jobs", cv, "id=?", new String[]{String.valueOf(jobId)});
    }

    public void updateJobOutputUri(long jobId,String outputUri){ContentValues v=new ContentValues();v.put("output_uri",safe(outputUri));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("jobs",v,"id=?",new String[]{String.valueOf(jobId)});}

    public OutputIntegrityAudit.Result finishJobWithAudit(long jobId) {
        OutputIntegrityAudit.Result audit=OutputIntegrityAudit.audit(getChunkRows(jobId));
        ContentValues v=new ContentValues();v.put("integrity_status",audit.status());v.put("status",audit.status());v.put("updated_at",System.currentTimeMillis());if(audit.passed())v.put("completed_at",System.currentTimeMillis());getWritableDatabase().update("jobs",v,"id=?",new String[]{String.valueOf(jobId)});return audit;
    }
    public void finishJob(long jobId) { finishJobWithAudit(jobId); }


    public List<JobSummary> getJobSummaries(int limit) {
        ArrayList<JobSummary> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending " +
                "FROM jobs j ORDER BY j.updated_at DESC LIMIT ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(Math.max(1, limit))})) {
            while (c.moveToNext()) list.add(readJobSummary(c));
        }
        return list;
    }

    public JobSummary getJobSummary(long jobId) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT j.id,j.file_name,j.status,j.source_language,j.target_language,j.created_at,j.updated_at,j.input_uri,j.output_uri," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)<>'SUPERSEDED') AS total," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('DONE','COMPLETED')) AS done," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status) IN ('ERROR','RETRYABLE_ERROR','PERMANENT_ERROR')) AS failed," +
                "(SELECT COUNT(*) FROM chunks c WHERE c.job_id=j.id AND UPPER(c.status)='PENDING') AS pending " +
                "FROM jobs j WHERE j.id=? LIMIT 1";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(jobId)})) {
            if (c.moveToFirst()) return readJobSummary(c);
        }
        return null;
    }

    public void deleteJob(long jobId) {
        SQLiteDatabase db = getWritableDatabase();
        try(Cursor c=db.rawQuery("SELECT id FROM benchmark_sessions WHERE job_id=?",new String[]{String.valueOf(jobId)})){while(c.moveToNext())db.delete("benchmark_results","session_id=?",new String[]{String.valueOf(c.getLong(0))});}
        db.delete("benchmark_sessions","job_id=?",new String[]{String.valueOf(jobId)});
        db.delete("chunk_metrics", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("chunk_attempts", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("config_revisions", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("chunks", "job_id=?", new String[]{String.valueOf(jobId)});
        db.delete("jobs", "id=?", new String[]{String.valueOf(jobId)});
    }

    public List<Job> getRecentJobs(int limit) {
        ArrayList<Job> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id,input_uri,output_uri,file_name,settings,status FROM jobs ORDER BY updated_at DESC LIMIT ?", new String[]{String.valueOf(Math.max(1, limit))})) {
            while (c.moveToNext()) {
                list.add(readJob(c));
            }
        }
        return list;
    }

    private Job readJob(Cursor c) {
        Job j = new Job();
        j.id = c.getLong(0); j.inputUri = c.getString(1); j.outputUri = c.getString(2); j.fileName = c.getString(3); j.settingsJson = c.getString(4); j.status = c.getString(5);
        return j;
    }


    private JobSummary readJobSummary(Cursor c) {
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
        return r;
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

    public List<AttemptRow> getAttemptRows(long jobId,int idx){ArrayList<AttemptRow>out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT attempt,status,phase,network_state,logical_request_id,request_hash,response_hash,response_preview,provider_response_id,finish_reason,validation_result,error,model,provider,input_tokens,output_tokens,cached_tokens,actual_cost,usage_source,created_at,updated_at FROM chunk_attempts WHERE job_id=? AND chunk_idx=? ORDER BY id",args(jobId,idx))){while(c.moveToNext()){AttemptRow a=new AttemptRow();a.attempt=c.getInt(0);a.status=c.getString(1);a.phase=c.getString(2);a.networkState=c.getString(3);a.logicalRequestId=c.getString(4);a.requestHash=c.getString(5);a.responseHash=c.getString(6);a.responsePreview=c.getString(7);a.providerResponseId=c.getString(8);a.finishReason=c.getString(9);a.validationResult=c.getString(10);a.error=c.getString(11);a.model=c.getString(12);a.provider=c.getString(13);a.inputTokens=c.getInt(14);a.outputTokens=c.getInt(15);a.cachedTokens=c.getInt(16);a.actualCost=c.getDouble(17);a.usageSource=c.getString(18);a.createdAt=c.getLong(19);a.updatedAt=c.getLong(20);out.add(a);}}return out;}
    public static class AttemptRow {public int attempt,inputTokens,outputTokens,cachedTokens;public double actualCost;public String status,phase,networkState,logicalRequestId,providerResponseId,requestHash,responseHash,responsePreview,finishReason,validationResult,error,model,provider,usageSource;public long createdAt,updatedAt;}

    public String preparedBatchIdentity(long jobId){try(Cursor c=getReadableDatabase().rawQuery("SELECT prepared_batch_id,prepared_input_ordinal FROM jobs WHERE id=?",new String[]{String.valueOf(jobId)})){return c.moveToFirst()?safe(c.getString(0))+":"+c.getInt(1):"";}}
    private static boolean isCompleted(String s){return "done".equalsIgnoreCase(s)||"COMPLETED".equalsIgnoreCase(s);}
    private static String[] args(long jobId,int idx){return new String[]{String.valueOf(jobId),String.valueOf(idx)};}
    private static String safe(String s){return s==null?"":s;}
    private static String redactedPreview(String s){String v=safe(s).replaceAll("\\s+"," ").trim();return v.length()>240?v.substring(0,240)+"…":v;}
    private static boolean hasAcceptedTranslation(SQLiteDatabase db,long jobId,int idx){try(Cursor c=db.rawQuery("SELECT translated,status FROM chunks WHERE job_id=? AND idx=?",args(jobId,idx))){if(c.moveToFirst()){String text=safe(c.getString(0)),status=safe(c.getString(1));return !text.isEmpty()&&(isCompleted(status)||"REQUESTING_REPLACEMENT".equalsIgnoreCase(status));}}return false;}
}
