package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Android face of the translation engine: the retry/refine/validation logic lives in TranslationEngineCore;
 * this class keeps the Context- and Uri-bound input reading and the single-file partial output writer.
 */
public class TranslationEngine extends TranslationEngineCore {
    public static class PreparedInput {
        public Uri inputUri;
        public String inputText;
        public String fileName;
        public List<Chunk> chunks;
        public String inputHash;
        public String settingsHash;
    }

    private final Context appContext;

    public TranslationEngine(Context context) {
        super(null, null);
        this.appContext = context.getApplicationContext();
        setTraceSink(new AndroidTraceSink(appContext));
    }

    public TranslationEngine(Context context, TranslationRepository repository) {
        super(repository, null);
        this.appContext = context.getApplicationContext();
        setTraceSink(new AndroidTraceSink(appContext));
    }

    public TranslationEngine(Context context, TranslationRepository repository, ProviderClient providerClient) {
        super(repository, providerClient);
        this.appContext = context.getApplicationContext();
        setTraceSink(new AndroidTraceSink(appContext));
    }

    /** JVM-test seam for provider-behaviour tests; it never creates a network client or touches Android files. */
    TranslationEngine(ProviderClient providerClient) {
        super(null, providerClient);
        this.appContext = null;
    }

    public ArrayList<Uri> readInputUris(Intent intent) throws Exception {
        ArrayList<Uri> inputs = new ArrayList<>();
        if (intent == null) return inputs;

        // Current UI sends a JSON string for backward compatibility with older service code.
        // Also accept StringArrayListExtra so future UI code can pass a typed list safely.
        String json = intent.getStringExtra("inputUris");
        if (json != null && !json.isEmpty()) {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) inputs.add(Uri.parse(arr.getString(i)));
        } else {
            ArrayList<String> many = intent.getStringArrayListExtra("inputUris");
            if (many != null) for (String s : many) if (s != null && !s.isEmpty()) inputs.add(Uri.parse(s));
        }
        if (inputs.isEmpty()) {
            String one = intent.getStringExtra("inputUri");
            if (one != null && !one.isEmpty()) inputs.add(Uri.parse(one));
        }
        return inputs;
    }

    public AppSettings loadExternalConfig(Intent intent, AppSettings settings) throws Exception {
        AppSettings s = settings == null ? new AppSettings() : settings;
        if (intent == null) return AppValidator.normalize(s);
        String yamlUriStr = intent.getStringExtra("yamlUri");
        String envUriStr = intent.getStringExtra("envUri");
        String glossaryUriStr = intent.getStringExtra("glossaryUri");
        String pronounUriStr = intent.getStringExtra("pronounUri");
        if (envUriStr != null && !envUriStr.isEmpty()) {
            String envText = FileUtil.readText(appContext, Uri.parse(envUriStr));
            s = AppSettings.fromEnv(EnvParser.parse(envText), s);
        }
        if (yamlUriStr != null && !yamlUriStr.isEmpty()) {
            Uri yuri = Uri.parse(yamlUriStr);
            CustomInstructions ci = YamlInstructionParser.parse(FileUtil.displayName(appContext, yuri), FileUtil.readText(appContext, yuri));
            if (ci.hasTranslation()) s.translationInstructions = ci.translation;
            if (ci.hasRefinement()) s.refinementInstructions = ci.refinement;
        }
        if (glossaryUriStr != null && !glossaryUriStr.isEmpty()) {
            Uri guri = Uri.parse(glossaryUriStr);
            s.glossaryText = FileUtil.readText(appContext, guri);
        }
        if (pronounUriStr != null && !pronounUriStr.isEmpty()) {
            Uri puri = Uri.parse(pronounUriStr);
            s.pronounText = FileUtil.readText(appContext, puri);
            s.pronounUri = pronounUriStr;
            s.pronounName = FileUtil.displayName(appContext, puri);
        }
        return AppValidator.normalize(s);
    }

    public PreparedInput prepareInput(Uri inputUri, AppSettings settings) throws Exception {
        if (inputUri == null) throw new IllegalArgumentException("Input TXT is empty");
        AppSettings s = AppValidator.normalize(settings);
        String input = FileUtil.readText(appContext, inputUri);
        String fileName = FileUtil.displayName(appContext, inputUri);
        String inputProblem = AppValidator.validateSourceText(input, fileName);
        if (inputProblem != null) throw new IllegalArgumentException(inputProblem);
        List<Chunk> chunks = Chunker.chunkText(input, s);
        if (chunks.isEmpty()) throw new IllegalArgumentException(fileName + ": không tạo được chunk từ file input");

        PreparedInput p = new PreparedInput();
        p.inputUri = inputUri;
        p.inputText = input;
        p.fileName = fileName;
        p.chunks = chunks;
        p.inputHash = HashUtil.sha256(input);
        p.settingsHash = HashUtil.settingsHash(s);
        return p;
    }

    public void updatePartialOutput(TranslationRepository repo, long jobId, Uri outputUri, AppSettings settings) throws Exception {
        String assembled=assembleOutput(repo.getChunkRows(jobId), settings != null && settings.bilingualOutput, true);
        if(!assembled.isEmpty())FileUtil.writeTextVerified(appContext, outputUri, assembled);
    }
}
