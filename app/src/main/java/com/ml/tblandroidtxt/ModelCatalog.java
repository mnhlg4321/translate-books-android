package com.ml.tblandroidtxt;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Dynamic model catalog with a small exact-match fallback for offline defaults. */
public class ModelCatalog {
    private static final String CACHE_FILE = "openrouter_model_catalog_v42.json";
    public static final long CACHE_EXPIRY_MS = 24L * 60L * 60L * 1000L;

    public enum LoadingState { IDLE, LOADING, READY, ERROR }

    public interface Listener { void onCatalogChanged(CatalogState state); }

    public static final class CatalogState {
        public final List<ModelInfo> models;
        public final LoadingState loadingState;
        public final String error;
        public final long lastSuccessfulRefresh;
        public final boolean fromCache;

        CatalogState(List<ModelInfo> models, LoadingState loadingState, String error,
                     long lastSuccessfulRefresh, boolean fromCache) {
            this.models = Collections.unmodifiableList(new ArrayList<>(models));
            this.loadingState = loadingState;
            this.error = error == null ? "" : error;
            this.lastSuccessfulRefresh = lastSuccessfulRefresh;
            this.fromCache = fromCache;
        }

        public boolean cacheExpired(long now) {
            return lastSuccessfulRefresh <= 0 || now - lastSuccessfulRefresh > CACHE_EXPIRY_MS;
        }
    }

    public static class ModelInfo {
        public final String id;
        public final String name;
        public final double inputPerMillion;
        public final double outputPerMillion;
        public final double cachedInputPerMillion;
        public final boolean inputPriceKnown;
        public final boolean outputPriceKnown;
        public final boolean cachedInputPriceKnown;
        public final int contextLength;
        public final String provider;
        public final String pricingSource;
        public final long pricingRetrievedAt;
        public final String pricingError;

        public ModelInfo(String id, double inputPerMillion, double outputPerMillion,
                         int contextLength, String provider) {
            this(id, id, inputPerMillion, true, outputPerMillion, true,
                    0, false, contextLength, provider, "bundled", 0, "");
        }

        public ModelInfo(String id, double inputPerMillion, double outputPerMillion,
                         double cachedInputPerMillion, int contextLength, String provider,
                         String pricingSource) {
            this(id, id, inputPerMillion, true, outputPerMillion, true,
                    cachedInputPerMillion, true, contextLength, provider, pricingSource, 0, "");
        }

        public ModelInfo(String id, String name,
                         double inputPerMillion, boolean inputPriceKnown,
                         double outputPerMillion, boolean outputPriceKnown,
                         double cachedInputPerMillion, boolean cachedInputPriceKnown,
                         int contextLength, String provider, String pricingSource,
                         long pricingRetrievedAt, String pricingError) {
            this.id = normalizeModelId(id);
            this.name = name == null || name.trim().isEmpty() ? this.id : name.trim();
            this.inputPerMillion = inputPerMillion;
            this.outputPerMillion = outputPerMillion;
            this.cachedInputPerMillion = cachedInputPerMillion;
            this.inputPriceKnown = inputPriceKnown;
            this.outputPriceKnown = outputPriceKnown;
            this.cachedInputPriceKnown = cachedInputPriceKnown;
            this.contextLength = Math.max(0, contextLength);
            this.provider = provider == null ? "" : provider.trim();
            this.pricingSource = pricingSource == null ? "unknown" : pricingSource;
            this.pricingRetrievedAt = Math.max(0, pricingRetrievedAt);
            this.pricingError = pricingError == null ? "" : pricingError;
        }

        public static ModelInfo unavailable(String id, String provider) {
            return new ModelInfo(id, id, 0, false, 0, false, 0, false,
                    0, provider, "pricing-unavailable", 0, "model not present in catalog");
        }

        public boolean hasPricing() { return inputPriceKnown && outputPriceKnown; }

        public String display() {
            StringBuilder text = new StringBuilder();
            if (!name.equals(id)) text.append(name).append('\n');
            text.append(id).append('\n')
                    .append("Input: ").append(rate(inputPerMillion, inputPriceKnown))
                    .append("/M  •  Output: ").append(rate(outputPerMillion, outputPriceKnown)).append("/M");
            if (cachedInputPriceKnown) text.append("  •  Cached: ").append(rate(cachedInputPerMillion, true)).append("/M");
            text.append('\n').append("Context: ").append(contextLength > 0 ? CostEstimator.fmt(contextLength) : "unknown")
                    .append("  •  ").append(hasPricing() ? pricingSource : "pricing unavailable");
            return text.toString();
        }

        public String priceSuffix() {
            return "(In: " + rate(inputPerMillion, inputPriceKnown) + "/M, Cached: "
                    + rate(cachedInputPerMillion, cachedInputPriceKnown) + "/M, Out: "
                    + rate(outputPerMillion, outputPriceKnown) + "/M)";
        }

        private static String rate(double value, boolean known) {
            return known ? money(value) : "unknown";
        }
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile CatalogState state = new CatalogState(Collections.emptyList(), LoadingState.IDLE, "", 0, false);
    private static volatile Map<String, ModelInfo> dynamicById = Collections.emptyMap();
    private static volatile boolean initialized;

    public static String[] providers() { return new String[]{"openrouter", "openai", "deepseek", "custom"}; }

    public static void initialize(Context context) {
        if (context == null) return;
        ObservabilityLog.initialize(context);
        if (!initialized) {
            synchronized (ModelCatalog.class) {
                if (!initialized) {
                    loadCachedCatalog(context.getApplicationContext());
                    initialized = true;
                }
            }
        }
        refreshAsync(context.getApplicationContext());
    }

    public static void addListener(Listener listener) { if (listener != null) LISTENERS.addIfAbsent(listener); }
    public static void removeListener(Listener listener) { LISTENERS.remove(listener); }
    public static CatalogState state() { return state; }

    public static void refreshAsync(Context context) {
        if (context == null || state.loadingState == LoadingState.LOADING) return;
        setState(new CatalogState(state.models, LoadingState.LOADING, "", state.lastSuccessfulRefresh, state.fromCache));
        new Thread(() -> {
            try {
                long retrievedAt = System.currentTimeMillis();
                List<ModelInfo> fetched = fetchOpenRouterModelInfosOrThrow(retrievedAt);
                if (fetched.isEmpty()) throw new IllegalStateException("OpenRouter returned an empty model catalog");
                installDynamic(fetched);
                saveCachedCatalog(context, fetched, retrievedAt);
                ObservabilityLog.event("catalog_refresh", "result", "success", "models", fetched.size(), "retrievedAt", retrievedAt);
                setState(new CatalogState(fetched, LoadingState.READY, "", retrievedAt, false));
            } catch (Exception e) {
                String message = ApiErrorParser.describe(e);
                ObservabilityLog.event("catalog_refresh", "result", "error", "message", message,
                        "retainedModels", state.models.size());
                setState(new CatalogState(state.models, LoadingState.ERROR, message,
                        state.lastSuccessfulRefresh, state.fromCache));
            }
        }, "openrouter-catalog-refresh").start();
    }

    private static void setState(CatalogState next) {
        state = next;
        for (Listener listener : LISTENERS) {
            try { listener.onCatalogChanged(next); } catch (Exception ignored) {}
        }
    }

    private static void installDynamic(List<ModelInfo> models) {
        LinkedHashMap<String, ModelInfo> map = new LinkedHashMap<>();
        for (ModelInfo model : models) map.put(key(model.id), model);
        dynamicById = Collections.unmodifiableMap(map);
    }

    public static String defaultBaseUrl(String provider) {
        if (provider == null) return AppSettings.defaultBaseUrl(null);
        String p = provider.toLowerCase(Locale.ROOT);
        if (p.contains("openrouter")) return "https://openrouter.ai/api/v1/chat/completions";
        if (p.contains("deepseek")) return "https://api.deepseek.com/v1/chat/completions";
        if (p.contains("custom")) return "";
        return "https://api.openai.com/v1/chat/completions";
    }

    public static List<String> fallbackModels(String provider) {
        List<String> ids = new ArrayList<>();
        for (ModelInfo m : fallbackModelInfos(provider)) ids.add(m.id);
        return ids;
    }

    public static List<ModelInfo> fallbackModelInfos(String provider) {
        String p = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        ArrayList<ModelInfo> out = new ArrayList<>();
        if (p.contains("openrouter")) {
            out.add(new ModelInfo("openai/gpt-5.4-mini", 0.75, 4.50, 0.075, 128000, "openrouter", "bundled-offline"));
            out.add(new ModelInfo("openai/gpt-5.5-thinking", 1.25, 10.00, 128000, "openrouter"));
            out.add(new ModelInfo("anthropic/claude-sonnet-4.6", 3.00, 15.00, 200000, "openrouter"));
            out.add(new ModelInfo("google/gemini-2.5-flash", 0.30, 2.50, 1000000, "openrouter"));
            return out;
        }
        if (p.contains("deepseek")) {
            out.add(new ModelInfo("deepseek-chat", 0.27, 1.10, 64000, "deepseek"));
            out.add(new ModelInfo("deepseek-reasoner", 0.55, 2.19, 64000, "deepseek"));
            return out;
        }
        out.add(new ModelInfo("gpt-5.4-mini", 0.75, 4.50, 128000, "openai"));
        out.add(new ModelInfo("gpt-5.5-thinking", 1.25, 10.00, 128000, "openai"));
        out.add(new ModelInfo("gpt-4.1-mini", 0.40, 1.60, 128000, "openai"));
        return out;
    }

    public static List<ModelInfo> availableModelInfos(String provider) {
        String p = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        if (p.contains("openrouter") && !state.models.isEmpty()) return new ArrayList<>(state.models);
        return fallbackModelInfos(provider);
    }

    /** Compatibility entry point. Successful results are installed as the active catalog. */
    public static List<ModelInfo> fetchOpenRouterModelInfos() {
        try {
            long now = System.currentTimeMillis();
            List<ModelInfo> fetched = fetchOpenRouterModelInfosOrThrow(now);
            installDynamic(fetched);
            setState(new CatalogState(fetched, LoadingState.READY, "", now, false));
            return fetched;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private static List<ModelInfo> fetchOpenRouterModelInfosOrThrow(long retrievedAt) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS).build();
        Request req = new Request.Builder().url("https://openrouter.ai/api/v1/models").get()
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "TranslateBooksWithLLMsAndroid/4.4.1").build();
        try (Response response = client.newCall(req).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody == null ? "" : responseBody.string();
            if (!response.isSuccessful()) throw new ApiHttpException(response.code(), 0,
                    ApiErrorParser.fromHttp(response.code(), body));
            return parseOpenRouterResponse(body, "openrouter-live", retrievedAt);
        }
    }

    public static List<ModelInfo> parseOpenRouterResponse(String body, String source, long retrievedAt) {
        ArrayList<ModelInfo> out = new ArrayList<>();
        JSONArray data;
        try { data = new JSONObject(body == null ? "{}" : body).optJSONArray("data"); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid OpenRouter catalog JSON", e); }
        if (data == null) return out;
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;
            String id = normalizeModelId(item.optString("id", ""));
            if (id.isEmpty()) continue;
            JSONObject pricing = item.optJSONObject("pricing");
            PriceValue input = firstPrice(pricing, "prompt", "input");
            PriceValue output = firstPrice(pricing, "completion", "output");
            PriceValue cached = firstPrice(pricing, "input_cache_read", "cache_read", "cached_input", "prompt_cache_read");
            StringBuilder errors = new StringBuilder();
            addPriceError(errors, "input", input);
            addPriceError(errors, "output", output);
            addPriceError(errors, "cached", cached);
            out.add(new ModelInfo(id, item.optString("name", id),
                    input.perMillion, input.valid,
                    output.perMillion, output.valid,
                    cached.perMillion, cached.valid,
                    nonNegativeInt(item.opt("context_length")), "openrouter", source,
                    retrievedAt, errors.toString()));
        }
        int priced = 0, zeroPriced = 0, parseErrors = 0;
        for (ModelInfo model : out) {
            if (model.hasPricing()) priced++;
            if (model.hasPricing() && model.inputPerMillion == 0 && model.outputPerMillion == 0) zeroPriced++;
            if (!model.pricingError.isEmpty()) parseErrors++;
        }
        ObservabilityLog.event("pricing_parse", "source", source, "models", out.size(),
                "priced", priced, "zeroPriced", zeroPriced, "parseErrors", parseErrors,
                "retrievedAt", retrievedAt);
        return out;
    }

    private static void addPriceError(StringBuilder errors, String field, PriceValue price) {
        if (!price.present || price.valid) return;
        if (errors.length() > 0) errors.append("; ");
        errors.append(field).append(": ").append(price.error);
    }

    private static PriceValue firstPrice(JSONObject pricing, String... keys) {
        if (pricing == null) return PriceValue.missing();
        for (String field : keys) {
            if (!pricing.has(field) || pricing.isNull(field)) continue;
            return parsePricePerMillion(pricing.opt(field));
        }
        return PriceValue.missing();
    }

    static PriceValue parsePricePerMillion(Object raw) {
        if (raw == null || raw == JSONObject.NULL) return PriceValue.missing();
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) return PriceValue.invalid("empty numeric value");
        try {
            double perToken = Double.parseDouble(text);
            if (!Double.isFinite(perToken) || perToken < 0) return PriceValue.invalid("invalid numeric value");
            return PriceValue.valid(perToken * 1_000_000d);
        } catch (NumberFormatException e) {
            return PriceValue.invalid("unparseable numeric value");
        }
    }

    static final class PriceValue {
        final boolean present, valid;
        final double perMillion;
        final String error;
        private PriceValue(boolean present, boolean valid, double perMillion, String error) {
            this.present = present; this.valid = valid; this.perMillion = perMillion; this.error = error;
        }
        static PriceValue missing() { return new PriceValue(false, false, 0, "missing"); }
        static PriceValue invalid(String error) { return new PriceValue(true, false, 0, error); }
        static PriceValue valid(double value) { return new PriceValue(true, true, value, ""); }
    }

    public static ModelInfo findModelInfo(String provider, String model) {
        String needle = normalizeModelId(model);
        boolean openRouter = provider != null && provider.toLowerCase(Locale.ROOT).contains("openrouter");
        ModelInfo found = openRouter ? dynamicById.get(key(needle)) : null;
        boolean mayUseOfflineFallback = !openRouter || state.models.isEmpty();
        if (found == null && mayUseOfflineFallback) {
            for (ModelInfo fallback : fallbackModelInfos(provider)) {
                if (fallback.id.equals(needle)) { found = fallback; break; }
            }
        }
        if (found == null) found = ModelInfo.unavailable(needle, provider);
        ObservabilityLog.event("catalog_lookup", "provider", provider, "model", needle,
                "result", found.hasPricing() ? "priced" : "unavailable", "source", found.pricingSource);
        return found;
    }

    public static boolean existsInActiveCatalog(String modelId) {
        return dynamicById.containsKey(key(normalizeModelId(modelId)));
    }

    public static String normalizeModelId(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s*/\\s*", "/");
    }

    private static String key(String model) { return normalizeModelId(model).toLowerCase(Locale.ROOT); }

    public static double usageCost(ModelInfo m, int inputTokens, int cachedInputTokens, int outputTokens) {
        if (m == null || !m.hasPricing()) return Double.NaN;
        int cached = Math.max(0, Math.min(inputTokens, cachedInputTokens));
        int uncached = Math.max(0, inputTokens - cached);
        double cachedRate = m.cachedInputPriceKnown ? m.cachedInputPerMillion : m.inputPerMillion;
        return uncached / 1_000_000d * m.inputPerMillion
                + cached / 1_000_000d * cachedRate
                + Math.max(0, outputTokens) / 1_000_000d * m.outputPerMillion;
    }

    public static String money(double value) {
        if (Double.isNaN(value)) return "unknown";
        if (value == 0) return "$0";
        if (value < 1) return String.format(Locale.US, "$%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return String.format(Locale.US, "$%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static int nonNegativeInt(Object raw) {
        if (raw == null || raw == JSONObject.NULL) return 0;
        try { return Math.max(0, (int)Math.min(Integer.MAX_VALUE, Double.parseDouble(String.valueOf(raw)))); }
        catch (Exception ignored) { return 0; }
    }

    private static void loadCachedCatalog(Context context) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return;
        try {
            byte[] bytes = new byte[(int)Math.min(Integer.MAX_VALUE, file.length())];
            int count;
            try (FileInputStream input = new FileInputStream(file)) { count = input.read(bytes); }
            if (count <= 0) return;
            JSONObject root = new JSONObject(new String(bytes, 0, count, StandardCharsets.UTF_8));
            long retrievedAt = root.optLong("retrieved_at", 0);
            List<ModelInfo> cached = parseOpenRouterResponse(root.optString("response", "{}"), "openrouter-cache", retrievedAt);
            if (cached.isEmpty()) return;
            installDynamic(cached);
            setState(new CatalogState(cached, LoadingState.IDLE, "", retrievedAt, true));
            ObservabilityLog.event("catalog_cache", "result", "loaded", "models", cached.size(), "retrievedAt", retrievedAt);
        } catch (Exception e) {
            ObservabilityLog.event("catalog_cache", "result", "error", "message", ApiErrorParser.describe(e));
        }
    }

    private static void saveCachedCatalog(Context context, List<ModelInfo> models, long retrievedAt) throws Exception {
        JSONArray data = new JSONArray();
        for (ModelInfo model : models) {
            JSONObject pricing = new JSONObject();
            if (model.inputPriceKnown) pricing.put("prompt", model.inputPerMillion / 1_000_000d);
            if (model.outputPriceKnown) pricing.put("completion", model.outputPerMillion / 1_000_000d);
            if (model.cachedInputPriceKnown) pricing.put("input_cache_read", model.cachedInputPerMillion / 1_000_000d);
            data.put(new JSONObject().put("id", model.id).put("name", model.name)
                    .put("context_length", model.contextLength).put("pricing", pricing));
        }
        JSONObject root = new JSONObject().put("retrieved_at", retrievedAt)
                .put("response", new JSONObject().put("data", data).toString());
        File file = new File(context.getFilesDir(), CACHE_FILE);
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(root.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    static void installForTests(List<ModelInfo> models, LoadingState loadingState) {
        List<ModelInfo> safe = models == null ? Collections.emptyList() : models;
        installDynamic(safe);
        state = new CatalogState(safe, loadingState, "", System.currentTimeMillis(), false);
    }
}
