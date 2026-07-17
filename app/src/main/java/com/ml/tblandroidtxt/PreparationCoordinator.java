package com.ml.tblandroidtxt;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Owns the single cancellable preparation task for the process. */
public final class PreparationCoordinator {
    public interface Listener { void onChanged(PreparedBatch batch); }

    private static volatile PreparationCoordinator instance;
    private final Context appContext;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "translation-plan-preparer");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger generation = new AtomicInteger();
    private volatile PreparedBatch current;
    private volatile Future<?> active;
    private volatile ScheduledFuture<?> pending;

    private PreparationCoordinator(Context context) { appContext = context.getApplicationContext(); }

    public static PreparationCoordinator get(Context context) {
        if (instance == null) synchronized (PreparationCoordinator.class) {
            if (instance == null) instance = new PreparationCoordinator(context);
        }
        return instance;
    }

    public PreparedBatch current() { return current; }

    public synchronized void cancel() {
        generation.incrementAndGet();
        if (pending != null) pending.cancel(false);
        if (active != null) active.cancel(true);
        pending = null;
        active = null;
    }

    public synchronized void request(List<Uri> uris, AppSettings settings, Listener listener) {
        final ArrayList<Uri> files = new ArrayList<>();
        if (uris != null) for (Uri uri : uris) if (uri != null) files.add(uri);
        final AppSettings snapshot = AppValidator.normalize(settings == null ? new AppSettings() : settings.copy());
        final PreparedBatch existing = current;
        cancel();
        final int run = generation.incrementAndGet();
        PreparedBatch preparing = new PreparedBatch();
        preparing.constructionKey = constructionKey(snapshot);
        preparing.estimateKey = estimateKey(snapshot, preparing.constructionKey);
        preparing.settingsJson = SettingsStore.toJson(snapshot);
        preparing.status = PreparedBatch.Status.PREPARING;
        current = preparing;
        deliver(listener, preparing);
        pending = worker.schedule(() -> {
            active = worker.submit(() -> resolveOrPrepare(run, files, snapshot, existing, listener));
        }, 300, TimeUnit.MILLISECONDS);
    }

    private void resolveOrPrepare(int run,List<Uri> files,AppSettings settings,PreparedBatch existing,Listener listener){
        try{
            check(run);
            String selectionKey=selectionKey(appContext,files,settings);
            if(existing!=null&&existing.ready()&&selectionKey.equals(existing.selectionKey)){
                existing.settingsJson=SettingsStore.toJson(settings);existing.estimateKey=estimateKey(settings,existing.constructionKey);reprice(existing,settings);current=existing;deliver(listener,existing);return;
            }
            TranslationRepository repo=new TranslationRepository(appContext);PreparedBatch cached;
            try{cached=repo.getPreparedBatchBySelectionKey(selectionKey);}finally{repo.close();}
            check(run);
            if(cached!=null&&cached.ready()){
                cached.settingsJson=SettingsStore.toJson(settings);cached.estimateKey=estimateKey(settings,cached.constructionKey);reprice(cached,settings);current=cached;deliver(listener,cached);return;
            }
            prepare(run,files,settings,selectionKey,listener);
        }catch(InterruptedException e){Thread.currentThread().interrupt();}
        catch(Exception e){if(run==generation.get()){PreparedBatch failed=new PreparedBatch();failed.status=PreparedBatch.Status.ERROR;failed.error=AppValidator.readableError(e);current=failed;deliver(listener,failed);}}
    }

    public void repriceCurrent(AppSettings settings, Listener listener) {
        PreparedBatch batch = current;
        if (batch == null || !batch.ready()) return;
        reprice(batch, AppValidator.normalize(settings == null ? new AppSettings() : settings.copy()));
        deliver(listener, batch);
    }

    private synchronized void reestimate(PreparedBatch batch,AppSettings settings,String nextKey,Listener listener){
        cancel();final int run=generation.incrementAndGet();batch.status=PreparedBatch.Status.PREPARING;batch.progress=85;current=batch;deliver(listener,batch);
        active=worker.submit(()->{try{CostEstimator.Estimate total=new CostEstimator.Estimate();for(PreparedBatch.Input input:batch.inputs){check(run);CostEstimator.addInto(total,CostEstimator.estimatePreparedChunksLightweight(input.chunks,settings));}batch.estimate=total;batch.estimateKey=nextKey;batch.settingsJson=SettingsStore.toJson(settings);batch.status=PreparedBatch.Status.READY;batch.progress=100;TranslationRepository repo=new TranslationRepository(appContext);repo.savePreparedBatch(batch);repo.close();current=batch;deliver(listener,batch);}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(Exception e){if(run==generation.get()){batch.status=PreparedBatch.Status.ERROR;batch.error=AppValidator.readableError(e);current=batch;deliver(listener,batch);}}});
    }

    private void prepare(int run, List<Uri> files, AppSettings settings, String selectionKey, Listener listener) {
        long started = System.currentTimeMillis();
        PreparedBatch batch = new PreparedBatch();
        batch.id = HashUtil.sha256(selectionKey + ":" + started).substring(0, 24);
        batch.selectionKey = selectionKey;
        batch.constructionKey = constructionKey(settings);
        batch.estimateKey = estimateKey(settings, batch.constructionKey);
        batch.settingsJson = SettingsStore.toJson(settings);
        batch.createdAt = started;
        batch.status = PreparedBatch.Status.PREPARING;
        try {
            if (files.isEmpty()) throw new IllegalArgumentException("Select at least one TXT file");
            CostEstimator.Estimate aggregate = new CostEstimator.Estimate();
            for (int i = 0; i < files.size(); i++) {
                check(run);
                Uri uri = files.get(i);
                updateProgress(batch, Math.round(i * 85f / files.size()), listener);
                FileUtil.TextReadResult read = FileUtil.readTextDetailed(appContext, uri);
                check(run);
                String normalized = Chunker.normalizeSource(read.text);
                String name = FileUtil.displayName(appContext, uri);
                String invalid = AppValidator.validateSourceText(normalized, name);
                if (invalid != null) throw new IllegalArgumentException(invalid);
                PreparedBatch.Input input = new PreparedBatch.Input();
                input.ordinal = i;
                input.uri = uri;
                input.displayName = name;
                input.encoding = read.encoding;
                input.inputHash = HashUtil.sha256(normalized);
                input.chunks = Chunker.chunkText(normalized, settings);
                check(run);
                CostEstimator.Estimate estimate = CostEstimator.estimatePreparedChunksLightweight(input.chunks, settings);
                CostEstimator.addInto(aggregate, estimate);
                batch.inputs.add(input);
            }
            batch.estimate = aggregate;
            batch.status = PreparedBatch.Status.READY;
            batch.progress = 100;
            TranslationRepository repo = new TranslationRepository(appContext);
            repo.savePreparedBatch(batch);
            repo.close();
            check(run);
            current = batch;
            ObservabilityLog.event("preparation_ready", "batch", batch.id, "files", batch.inputs.size(),
                    "chunks", batch.exactChunkCount(), "elapsedMs", System.currentTimeMillis() - started);
            deliver(listener, batch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            batch.status = PreparedBatch.Status.CANCELLED;
        } catch (Exception e) {
            if (run != generation.get()) return;
            batch.status = PreparedBatch.Status.ERROR;
            batch.error = AppValidator.readableError(e);
            current = batch;
            ObservabilityLog.event("preparation_error", "error", e.getClass().getSimpleName(), "message", batch.error);
            deliver(listener, batch);
        }
    }

    private void updateProgress(PreparedBatch batch, int progress, Listener listener) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException("Preparation cancelled");
        batch.progress = Math.max(0, Math.min(99, progress));
        current = batch;
        deliver(listener, batch);
    }

    private void check(int run) throws InterruptedException {
        if (Thread.interrupted() || run != generation.get()) throw new InterruptedException("Stale preparation cancelled");
    }

    private void reprice(PreparedBatch batch, AppSettings settings) {
        if (batch.estimate != null) CostEstimator.applyPricing(batch.estimate, settings);
    }

    private void deliver(Listener listener, PreparedBatch batch) {
        if (listener != null) main.post(() -> listener.onChanged(batch));
    }

    static String selectionKey(Context context, List<Uri> uris, AppSettings settings) {
        StringBuilder key = new StringBuilder(constructionKey(settings));
        if (uris != null) for (Uri uri : uris) key.append('\n').append(FileUtil.identityOf(context,uri));
        return HashUtil.sha256(key.toString());
    }

    static String constructionKey(AppSettings s) {
        AppSettings x = AppValidator.normalize(s == null ? new AppSettings() : s.copy());
        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(x.provider, x.model);
        int context = model == null || model.contextLength <= 0 ? 32000 : model.contextLength;
        return HashUtil.sha256(String.format(Locale.ROOT,
                "chunk-v45-core\n%s\n%s\n%d\n%d\n%.5f\n%s\n%d\n%s\n%b\n%d\n%d",
                x.chunkMode, x.model, x.maxTokensPerChunk, x.maxCharsPerChunk, x.softLimitRatio,
                x.optimizationPreset, x.maxOutputTokens, x.provider, x.contextOverlapEnabled, x.contextChars, context));
    }

    static String estimateKey(AppSettings s, String construction) {
        AppSettings x = s == null ? new AppSettings() : s;
        return HashUtil.sha256(construction + '\n' + x.sourceLanguage + '\n' + x.targetLanguage + '\n'
                + x.translationInstructions + '\n' + x.refinementInstructions + '\n' + x.glossaryText + '\n'
                + x.pronounText + '\n' + x.glossaryInjectLimit + '\n' + x.pronounInjectLimit + '\n' + x.refineAfter);
    }
}
