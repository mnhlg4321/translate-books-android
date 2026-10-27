package com.ml.tblandroidtxt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TranslationRepository extends TranslationRepositoryCore implements AutoCloseable {
    private static final String DB = "tbl_android_txt.db";
    static final int VER = 30;

    private final TranslationDbHelper helper;

    public TranslationRepository(Context context) { this(context, DB); }

    /** Package-private database-name seam used by isolated migration/import tests. */
    TranslationRepository(Context context, String databaseName) { this(new TranslationDbHelper(context, databaseName)); }

    private TranslationRepository(TranslationDbHelper helper) {
        super(new AndroidSqlDb(helper));
        this.helper = helper;
    }

    public SQLiteDatabase getReadableDatabase() { return helper.getReadableDatabase(); }

    public SQLiteDatabase getWritableDatabase() { return helper.getWritableDatabase(); }

    @Override public void close() { helper.close(); }

    @Override protected String appVersionName() { return AppBuildInfo.VERSION_NAME; }

    /**
     * Schema owner. It stays in this file, at its original indentation, because source-text tests assert the
     * migration wiring here; the table creation helpers below stay with it.
     */
    static final class TranslationDbHelper extends SQLiteOpenHelper {
    TranslationDbHelper(Context context, String databaseName) { super(context, databaseName, null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE jobs (id INTEGER PRIMARY KEY AUTOINCREMENT, input_uri TEXT, output_uri TEXT, file_name TEXT, source_language TEXT, target_language TEXT, status TEXT, created_at INTEGER, updated_at INTEGER, settings TEXT, input_hash TEXT, settings_hash TEXT, integrity_status TEXT, output_hash TEXT, config_revision INTEGER, app_version TEXT, completed_at INTEGER, prepared_batch_id TEXT DEFAULT '', prepared_input_ordinal INTEGER DEFAULT -1, start_session_id TEXT DEFAULT '')");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_start_session ON jobs(start_session_id,prepared_input_ordinal) WHERE start_session_id<>''");
        db.execSQL("CREATE TABLE chunks (job_id INTEGER, idx INTEGER, stable_id TEXT, start_offset INTEGER, end_offset INTEGER, context_start_offset INTEGER, context_end_offset INTEGER, source TEXT, source_hash TEXT, normalized_source_hash TEXT, translated TEXT, status TEXT, error TEXT, request_hash TEXT, response_hash TEXT, attempt INTEGER, created_at INTEGER, updated_at INTEGER, persisted_response TEXT, rejected_response TEXT, finish_reason TEXT, validation_warnings TEXT, qa_json TEXT, result_source TEXT, parent_stable_id TEXT, PRIMARY KEY(job_id, idx))");
        createReliabilityTables(db);
        createMetricsTable(db);
        createBenchmarkTables(db);
        createPreparedPlanTables(db);
        createLibraryTables(db);
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
        createEditorialP5DBodyProgress(db);
        createEditorialPhaseArtifacts(db);
        createEditorialApiTables(db);
        createEditorialPairTables(db);
        migrateEditorialApiRunsToV28(db);
        migrateEditorialPairRunsToV29(db);
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
        if (oldVersion < 24) createEditorialP5DBodyProgress(db);
        if (oldVersion < 25) createEditorialPhaseArtifacts(db);
        if (oldVersion < 26) createEditorialApiTables(db);
        if (oldVersion < 27) createEditorialPairTables(db);
        if (oldVersion < 28) migrateEditorialApiRunsToV28(db);
        if (oldVersion < 29) migrateEditorialPairRunsToV29(db);
        if (oldVersion < 30) createLibraryTables(db);
    }

    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Editorial database downgrade is not supported");
    }
    @Override public void onOpen(SQLiteDatabase db) { super.onOpen(db); }
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
    private static void createEditorialApiTables(SQLiteDatabase db) { for (String sql : EditorialApiMigrationSpec.from25To26()) db.execSQL(sql); }
    private static void createEditorialPairTables(SQLiteDatabase db) { for (String sql : EditorialPairMigrationSpec.from26To27()) db.execSQL(sql); }
    private static void migrateEditorialApiRunsToV28(SQLiteDatabase db) { for (String sql : EditorialApiV28MigrationSpec.from27To28()) db.execSQL(sql); }
    private static void createLibraryTables(SQLiteDatabase db) { for (String sql : LibraryMigrationSpec.from29To30()) db.execSQL(sql); }
    private static void migrateEditorialPairRunsToV29(SQLiteDatabase db) { for (String sql : EditorialPairV29MigrationSpec.from28To29()) db.execSQL(sql); }
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
    private static void createEditorialP5DBodyProgress(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from23To24())safeExec(db,sql);}
    private static void createEditorialPhaseArtifacts(SQLiteDatabase db){for(String sql:EditorialMigrationSpec.from24To25())db.execSQL(sql);}
    SQLiteDatabase editorialWritableDatabase() { return getWritableDatabase(); }
    SQLiteDatabase editorialReadableDatabase() { return getReadableDatabase(); }
    private static void recoverInterruptedChunks(SQLiteDatabase db) {
        ContentValues unknown=new ContentValues();unknown.put("status","DELIVERY_UNKNOWN");unknown.put("error","Process died after request sending began; review before retrying to avoid duplicate billing");unknown.put("updated_at",System.currentTimeMillis());
        db.update("chunks",unknown,"UPPER(status) IN ('PREPARING','REQUESTING') AND EXISTS (SELECT 1 FROM chunk_attempts a WHERE a.job_id=chunks.job_id AND a.chunk_idx=chunks.idx AND UPPER(a.status) IN ('SEND_STARTED','REQUEST_BODY_SENT','RESPONSE_HEADERS'))",null);
        ContentValues v=new ContentValues();v.put("status","RETRYABLE_ERROR");v.put("error","Interrupted before network send; safe to retry");v.put("updated_at",System.currentTimeMillis());
        db.update("chunks",v,"UPPER(status) IN ('PREPARING','REQUESTING')",null);
        ContentValues jobs=new ContentValues();jobs.put("status","NEEDS_REVIEW");jobs.put("updated_at",System.currentTimeMillis());db.update("jobs",jobs,"UPPER(COALESCE(status,'')) IN ("+JobStatus.sqlIn(JobStatus.Kind.ACTIVE,JobStatus.Kind.CONTINUABLE)+") AND EXISTS (SELECT 1 FROM chunks c WHERE c.job_id=jobs.id AND UPPER(c.status)='DELIVERY_UNKNOWN')",null);
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
    private static void deletePreparedBatch(SQLiteDatabase db,String id){db.delete("prepared_chunks","batch_id=?",new String[]{id});db.delete("prepared_inputs","batch_id=?",new String[]{id});db.delete("prepared_batches","id=?",new String[]{id});}
    private static void createBenchmarkTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS benchmark_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, status TEXT, model TEXT, provider TEXT, budget_usd REAL, spent_usd REAL DEFAULT 0, sample_count INTEGER, created_at INTEGER, updated_at INTEGER, error TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS benchmark_results (session_id INTEGER, chunk_idx INTEGER, pipeline TEXT, blind_label TEXT, input_tokens INTEGER, cached_tokens INTEGER, output_tokens INTEGER, cost_usd REAL, usage_source TEXT DEFAULT 'UNKNOWN', cost_status TEXT DEFAULT 'UNKNOWN_USAGE', duration_ms INTEGER, finish_reason TEXT, quality_issues TEXT, output TEXT, PRIMARY KEY(session_id,chunk_idx,pipeline))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_benchmark_job ON benchmark_sessions(job_id,created_at)");
    }
    private static void createMetricsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chunk_metrics (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER, chunk_idx INTEGER, phase TEXT, attempt INTEGER, preset TEXT, provider TEXT, model TEXT, source_chars INTEGER, raw_tokens INTEGER, system_tokens INTEGER, instruction_tokens INTEGER, glossary_tokens INTEGER, pronoun_tokens INTEGER, history_tokens INTEGER, context_tokens INTEGER, estimated_input_tokens INTEGER, actual_input_tokens INTEGER, output_tokens INTEGER, retry_tokens INTEGER, max_output_tokens INTEGER, baseline_input_tokens INTEGER DEFAULT 0, cached_input_tokens INTEGER DEFAULT 0, usage_source TEXT DEFAULT 'UNKNOWN', cost_status TEXT DEFAULT 'UNKNOWN_USAGE', logical_request_id TEXT DEFAULT '', duration_ms INTEGER, estimated_cost REAL, usage_cost REAL, outcome TEXT, retry_reason TEXT, created_at INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunk_metrics_job ON chunk_metrics(job_id, chunk_idx, phase, attempt)");
    }
}
