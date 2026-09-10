package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4Binding;
import com.ml.tblandroidtxt.editorial.pack.EditorialP4SourceIdentity;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotRequest;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackFileRole;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4Contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * App-owned P5C boundary for one exact persisted P4 binding and one chapter.
 * It is intentionally provider-injected: no provider, global activation or
 * certification path is created here.
 */
public final class EditorialP5CExactBindingExecution {
    public enum Status { COMMITTED, ALREADY_COMMITTED, STOP }

    public record Result(Status status, String reasonCode,
                         EditorialP5PilotResult rawResult,
                         EditorialP5PilotResult reconcileResult,
                         int providerCalls,
                         boolean executionAllowed,
                         String certificationState) {
        public Result {
            Objects.requireNonNull(status, "status");
            if (reasonCode == null || reasonCode.isBlank()) {
                throw new IllegalArgumentException("P5C result reason is required");
            }
            if (providerCalls < 0) throw new IllegalArgumentException("provider calls cannot be negative");
            if (executionAllowed) throw new IllegalArgumentException("P5C execution must remain disabled");
            if (!"NOT_CERTIFIED".equals(certificationState)) {
                throw new IllegalArgumentException("P5C cannot certify a pack");
            }
        }

        public boolean accepted() {
            return status == Status.COMMITTED || status == Status.ALREADY_COMMITTED;
        }
    }

    private final TranslationRepository database;
    private final EditorialPackStorageLayout storage;
    private final EditorialP4BindingDao bindings;
    private final EditorialP5PilotExecution engine;

    public EditorialP5CExactBindingExecution(TranslationRepository database,
                                             EditorialPackStorageLayout storage) {
        this(database, storage, System::currentTimeMillis);
    }

    EditorialP5CExactBindingExecution(TranslationRepository database,
                                      EditorialPackStorageLayout storage,
                                      EditorialP5PilotExecution.Clock clock) {
        this.database = Objects.requireNonNull(database, "database");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.bindings = new EditorialP4BindingDao(database);
        this.engine = new EditorialP5PilotExecution(Objects.requireNonNull(clock, "clock"));
    }

    /**
     * Executes the two separately authorized L1 phases. The selector is the
     * persisted P4 attempt-request selector, never a latest-pack lookup.
     */
    public Result execute(long projectId, String attemptRequestSelector, String chapterKey,
                          EditorialP5PilotAuthorization rawAuthorization,
                          EditorialP5PilotAuthorization reconcileAuthorization,
                          EditorialP5PilotProvider provider) {
        AtomicInteger providerCalls = new AtomicInteger();
        try {
            if (projectId <= 0 || blank(attemptRequestSelector) || blank(chapterKey)) {
                return stop("P5C_EXACT_BINDING_INPUT_INVALID", providerCalls.get());
            }
            if (rawAuthorization == null || reconcileAuthorization == null) {
                return stop("LIVE_AUTHORIZATION_INCOMPLETE", providerCalls.get());
            }
            if (provider == null) return stop("LIVE_PROVIDER_NOT_CONFIGURED", providerCalls.get());
            EditorialP4Binding binding = bindings.findByAttemptRequestSelector(
                    attemptRequestSelector).orElse(null);
            if (binding == null || projectIdFor(binding) != projectId) {
                return stop("P5C_EXACT_BINDING_NOT_FOUND", providerCalls.get());
            }
            if (!chapterExists(projectId, chapterKey)) {
                return stop("INPUT_CHAPTER_NOT_FOUND", providerCalls.get());
            }
            if (!authorizationMatches(rawAuthorization, binding, chapterKey,
                    "L1_RAW_DISCOVERY")
                    || !authorizationMatches(reconcileAuthorization, binding, chapterKey,
                    "L1_RECONCILE")
                    || !sameProviderScope(rawAuthorization, reconcileAuthorization)) {
                return stop("LIVE_AUTHORIZATION_INCOMPLETE", providerCalls.get());
            }
            List<EditorialP4InputSource> currentSources = currentSources(projectId, chapterKey, binding);
            EditorialP4ResumeResult resume = new EditorialP4BindingTransactionService(database, storage)
                    .resumeProject(projectId, attemptRequestSelector, currentSources);
            if (resume.code() != EditorialP4ResumeResult.Code.RESTORED
                    || resume.binding() == null
                    || !resume.binding().bindingIdentity().equals(binding.bindingIdentity())) {
                return stop("STOP_SOURCE_DRIFT", providerCalls.get());
            }

            EditorialPackManifest manifest = resolveManifest(binding);
            EditorialP5PilotRequest.PackAuthority authority = resolveAuthority(binding, manifest);
            List<EditorialP5PilotRequest.SourceBytes> sources = sourceBytes(binding, currentSources);
            List<String> stableAnchors = List.of("chapter:" + chapterKey);
            List<String> populationIds = List.of("population:" + chapterKey);
            EditorialP5PilotRequest rawRequest = new EditorialP5PilotRequest(binding, manifest,
                    authority, chapterKey, EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                    sources, binding.runDeclarationIdentity(), stableAnchors, populationIds,
                    true, boundedOutputTokens(rawAuthorization));

            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5PilotProvider countedProvider = countedProvider(provider, providerCalls);
            EditorialP5PilotResult rawResult = engine.execute(rawRequest, rawAuthorization,
                    countedProvider, attemptStore);
            if (!committedLike(rawResult)) {
                return new Result(Status.STOP, rawResult.reasonCode(), rawResult, null,
                        providerCalls.get(), false, "NOT_CERTIFIED");
            }
            // Read the durable predecessor back before constructing RECONCILE.
            // The second phase never relies on an in-memory result as its
            // predecessor authority.
            EditorialP5PilotResult.CommittedResult rawPersisted = attemptStore.findCommitted(
                    rawResult.committedResult().attemptIdentity()).orElse(null);
            if (rawPersisted == null) {
                return stop("RETRY_RAW_PREDECESSOR_READBACK_FAILED", providerCalls.get());
            }
            String predecessor = rawPersisted.attemptIdentity();
            EditorialP5PilotRequest reconcileRequest = new EditorialP5PilotRequest(binding, manifest,
                    authority, chapterKey, EditorialP5PilotRequest.Phase.L1_RECONCILE,
                    sources, predecessor, stableAnchors, populationIds, true,
                    boundedOutputTokens(reconcileAuthorization));
            EditorialP5PilotResult reconcileResult = engine.execute(reconcileRequest,
                    reconcileAuthorization, countedProvider, attemptStore);
            if (!committedLike(reconcileResult)) {
                return new Result(Status.STOP, reconcileResult.reasonCode(), rawResult,
                        reconcileResult, providerCalls.get(), false, "NOT_CERTIFIED");
            }
            if (attemptStore.findCommitted(reconcileResult.committedResult().attemptIdentity()).isEmpty()) {
                return new Result(Status.STOP, "RETRY_RECONCILE_RESULT_READBACK_FAILED", rawResult,
                        reconcileResult, providerCalls.get(), false, "NOT_CERTIFIED");
            }
            Status status = rawResult.outcome() == EditorialP5PilotResult.Outcome.ALREADY_COMMITTED
                    && reconcileResult.outcome() == EditorialP5PilotResult.Outcome.ALREADY_COMMITTED
                    ? Status.ALREADY_COMMITTED : Status.COMMITTED;
            return new Result(status, status == Status.ALREADY_COMMITTED
                    ? "P5C_ALREADY_COMMITTED" : "P5C_L1_PAIR_COMMITTED", rawResult,
                    reconcileResult, providerCalls.get(), false, "NOT_CERTIFIED");
        } catch (IOException | RuntimeException error) {
            return stop(errorCode(error), providerCalls.get());
        }
    }

    /**
     * Executes only the explicitly authorized RAW discovery phase.  This is
     * intentionally a separate entry point so a caller cannot satisfy the
     * two-phase API with an invented RECONCILE authorization.  A committed RAW
     * result is returned only after its durable predecessor has been read back;
     * this method never constructs or invokes a RECONCILE request.
     */
    public Result executeRaw(long projectId, String attemptRequestSelector, String chapterKey,
                             EditorialP5PilotAuthorization rawAuthorization,
                             EditorialP5PilotProvider provider) {
        AtomicInteger providerCalls = new AtomicInteger();
        try {
            if (projectId <= 0 || blank(attemptRequestSelector) || blank(chapterKey)) {
                return stop("P5C_EXACT_BINDING_INPUT_INVALID", providerCalls.get());
            }
            if (rawAuthorization == null) {
                return stop("LIVE_AUTHORIZATION_INCOMPLETE", providerCalls.get());
            }
            if (provider == null) return stop("LIVE_PROVIDER_NOT_CONFIGURED", providerCalls.get());
            EditorialP4Binding binding = bindings.findByAttemptRequestSelector(
                    attemptRequestSelector).orElse(null);
            if (binding == null || projectIdFor(binding) != projectId) {
                return stop("P5C_EXACT_BINDING_NOT_FOUND", providerCalls.get());
            }
            if (!chapterExists(projectId, chapterKey)) {
                return stop("INPUT_CHAPTER_NOT_FOUND", providerCalls.get());
            }
            if (!authorizationMatches(rawAuthorization, binding, chapterKey,
                    "L1_RAW_DISCOVERY")) {
                return stop("LIVE_AUTHORIZATION_INCOMPLETE", providerCalls.get());
            }
            List<EditorialP4InputSource> currentSources = currentSources(projectId, chapterKey, binding);
            EditorialP4ResumeResult resume = new EditorialP4BindingTransactionService(database, storage)
                    .resumeProject(projectId, attemptRequestSelector, currentSources);
            if (resume.code() != EditorialP4ResumeResult.Code.RESTORED
                    || resume.binding() == null
                    || !resume.binding().bindingIdentity().equals(binding.bindingIdentity())) {
                return stop("STOP_SOURCE_DRIFT", providerCalls.get());
            }

            EditorialPackManifest manifest = resolveManifest(binding);
            EditorialP5PilotRequest.PackAuthority authority = resolveAuthority(binding, manifest);
            List<EditorialP5PilotRequest.SourceBytes> sources = sourceBytes(binding, currentSources);
            List<String> stableAnchors = List.of("chapter:" + chapterKey);
            List<String> populationIds = List.of("population:" + chapterKey);
            EditorialP5PilotRequest rawRequest = new EditorialP5PilotRequest(binding, manifest,
                    authority, chapterKey, EditorialP5PilotRequest.Phase.L1_RAW_DISCOVERY,
                    sources, binding.runDeclarationIdentity(), stableAnchors, populationIds,
                    true, boundedOutputTokens(rawAuthorization));

            EditorialP5CAttemptStore attemptStore = new EditorialP5CAttemptStore(database);
            EditorialP5PilotProvider countedProvider = countedProvider(provider, providerCalls);
            EditorialP5PilotResult rawResult = engine.execute(rawRequest, rawAuthorization,
                    countedProvider, attemptStore);
            if (!committedLike(rawResult)) {
                String reason = rawResult == null ? "P5C_RAW_RESULT_MISSING" : rawResult.reasonCode();
                return new Result(Status.STOP, reason, rawResult, null,
                        providerCalls.get(), false, "NOT_CERTIFIED");
            }
            if (attemptStore.findCommitted(rawResult.committedResult().attemptIdentity()).isEmpty()) {
                return stop("RETRY_RAW_PREDECESSOR_READBACK_FAILED", providerCalls.get());
            }
            Status status = rawResult.outcome() == EditorialP5PilotResult.Outcome.ALREADY_COMMITTED
                    ? Status.ALREADY_COMMITTED : Status.COMMITTED;
            return new Result(status,
                    status == Status.ALREADY_COMMITTED
                            ? "P5C_RAW_ALREADY_COMMITTED" : "P5C_RAW_COMMITTED",
                    rawResult, null, providerCalls.get(), false, "NOT_CERTIFIED");
        } catch (IOException | RuntimeException error) {
            return stop(errorCode(error), providerCalls.get());
        }
    }

    /**
     * Keeps the call counter transparent to the engine. In particular, the
     * attempt deadline must reach the real provider rather than stopping at a
     * counting lambda.
     */
    private static EditorialP5PilotProvider countedProvider(
            EditorialP5PilotProvider provider, AtomicInteger providerCalls) {
        return new EditorialP5PilotProvider() {
            @Override public void beginAttempt(long maximumExecutionTimeMillis) {
                provider.beginAttempt(maximumExecutionTimeMillis);
            }

            @Override public Response call(Request request) throws Exception {
                providerCalls.incrementAndGet();
                return provider.call(request);
            }
        };
    }

    private EditorialPackManifest resolveManifest(EditorialP4Binding binding) {
        EditorialPackSelectionCandidate candidate = new EditorialPackSelectionPolicy(database, storage)
                .resolve(binding.packId(), binding.packVersion()).orElseThrow(
                        () -> new IllegalStateException("P5C_PACK_NOT_SELECTABLE"));
        if (!binding.canonicalPackHash().equals(candidate.canonicalPackHash())
                || !binding.manifestFingerprint().equals(
                EditorialPackSelectionPolicy.manifestFingerprint(candidate.manifest()))
                || !binding.canonicalProfileHash().equals(candidate.canonicalProfileHash())
                || !binding.machineContractFingerprint().equals(candidate.machineContractFingerprint())
                || !binding.compatibilityEvaluationId().equals(candidate.evaluation().evaluationId())) {
            throw new IllegalStateException("P5C_EXACT_PACK_PROFILE_EVALUATION_MISMATCH");
        }
        return candidate.manifest();
    }

    private EditorialP5PilotRequest.PackAuthority resolveAuthority(
            EditorialP4Binding binding, EditorialPackManifest manifest) throws IOException {
        if (!storage.hasImmutableMarker(binding.canonicalPackHash())) {
            throw new IllegalStateException("P5C_IMMUTABLE_STORAGE_MISSING");
        }
        EnumMap<EditorialPackFileRole, byte[]> values = new EnumMap<>(EditorialPackFileRole.class);
        for (EditorialPackManifest.FileEntry file : manifest.fileEntries()) {
            Path path = storage.immutableEntry(binding.canonicalPackHash(), file.path());
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("P5C_AUTHORITY_STORAGE_INVALID");
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length != file.byteLength()
                    || !file.sha256().equals(EditorialCanonicalJson.sha256Hex(bytes))) {
                throw new IllegalStateException("P5C_AUTHORITY_BYTES_DRIFT");
            }
            values.put(file.role(), bytes);
        }
        return new EditorialP5PilotRequest.PackAuthority(values);
    }

    private List<EditorialP4InputSource> currentSources(long projectId, String chapterKey,
                                                        EditorialP4Binding binding) {
        EditorialRepository repository = new EditorialRepository(database);
        EditorialRepository.Chapter chapter = repository.listChapters(projectId).stream()
                .filter(value -> chapterKey.equals(value.chapterKey)).findFirst().orElseThrow(
                        () -> new IllegalStateException("INPUT_CHAPTER_NOT_FOUND"));
        Map<String, EditorialRepository.AssetSnapshot> assets = new HashMap<>();
        for (EditorialRepository.AssetSnapshot asset : repository.chapterAssets(chapter.id)) {
            assets.put(asset.role.name(), asset);
        }
        ArrayList<EditorialP4InputSource> sources = new ArrayList<>();
        for (EditorialP4SourceIdentity identity : binding.inputs()) {
            EditorialRepository.AssetSnapshot asset = assets.get(identity.role());
            if (asset == null) throw new IllegalStateException("INPUT_SOURCE_FILE_MISSING:" + identity.role());
            sources.add(new EditorialP4InputSource(identity.role(), asset.sourceUri,
                    asset.content.getBytes(StandardCharsets.UTF_8), identity.encoding(),
                    identity.schemaStatus(), identity.ordinal()));
        }
        return List.copyOf(sources);
    }

    private static List<EditorialP5PilotRequest.SourceBytes> sourceBytes(
            EditorialP4Binding binding, List<EditorialP4InputSource> current) {
        Map<String, EditorialP4InputSource> byKey = new HashMap<>();
        for (EditorialP4InputSource source : current) {
            byKey.put(source.role() + "\u0000" + source.ordinal(), source);
        }
        ArrayList<EditorialP5PilotRequest.SourceBytes> result = new ArrayList<>();
        for (EditorialP4SourceIdentity identity : binding.inputs()) {
            EditorialP4InputSource source = byKey.get(identity.role() + "\u0000" + identity.ordinal());
            if (source == null) throw new IllegalStateException("P5C_SOURCE_IDENTITY_MISSING");
            result.add(new EditorialP5PilotRequest.SourceBytes(identity.role(), source.sourceReference(),
                    source.bytes(), identity.encoding(), schemaId(identity.role()), identity.schemaStatus(),
                    identity.ordinal()));
        }
        return List.copyOf(result);
    }

    private static String schemaId(String role) {
        return EditorialSafe4Contract.GLOSSARY.equals(role) ? "safe4.full.glossary.v1"
                : EditorialSafe4Contract.PRONOUN.equals(role) ? "safe4.full.pronoun.v1" : "text.v1";
    }

    private long projectIdFor(EditorialP4Binding binding) {
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT project_row_id FROM editorial_p4_bindings WHERE binding_identity=?",
                new String[]{binding.bindingIdentity()})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private boolean chapterExists(long projectId, String chapterKey) {
        try (android.database.Cursor cursor = database.editorialReadableDatabase().rawQuery(
                "SELECT 1 FROM editorial_chapters WHERE project_id=? AND chapter_key=? LIMIT 1",
                new String[]{String.valueOf(projectId), chapterKey})) {
            return cursor.moveToFirst();
        }
    }

    private static boolean authorizationMatches(EditorialP5PilotAuthorization authorization,
                                                EditorialP4Binding binding, String chapterKey,
                                                String phase) {
        return phase.equals(authorization.phase()) && authorization.singleUse()
                && authorization.projectBindingIdentity().equals(binding.bindingIdentity())
                && authorization.runDeclarationIdentity().equals(binding.runDeclarationIdentity())
                && authorization.canonicalPackHash().equals(binding.canonicalPackHash())
                && authorization.canonicalProfileHash().equals(binding.canonicalProfileHash())
                && authorization.compatibilityEvaluationId().equals(binding.compatibilityEvaluationId())
                && authorization.chapterKey().equals(chapterKey);
    }

    private static boolean sameProviderScope(EditorialP5PilotAuthorization raw,
                                             EditorialP5PilotAuthorization reconcile) {
        return raw.provider().equals(reconcile.provider()) && raw.model().equals(reconcile.model())
                && raw.endpointAccountFingerprint().equals(reconcile.endpointAccountFingerprint())
                && raw.allowChapterToProvider() == reconcile.allowChapterToProvider();
    }

    private static boolean committedLike(EditorialP5PilotResult result) {
        return result != null && (result.outcome() == EditorialP5PilotResult.Outcome.COMMITTED
                || result.outcome() == EditorialP5PilotResult.Outcome.ALREADY_COMMITTED)
                && result.committedResult() != null;
    }

    static int boundedOutputTokens(EditorialP5PilotAuthorization authorization) {
        Objects.requireNonNull(authorization, "authorization");
        // The authorization is the single source of truth. The HTTP client
        // receives this exact value; invalid non-positive caps are rejected by
        // the execution boundary before a provider can be called.
        return authorization.maximumOutputTokens();
    }

    private static Result stop(String reason, int providerCalls) {
        return new Result(Status.STOP, reason, null, null, providerCalls, false, "NOT_CERTIFIED");
    }

    private static String errorCode(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "P5C_EXECUTION_BOUNDARY_FAILURE";
        if (message.matches("[A-Z0-9_:-]+")) return message;
        return "P5C_EXECUTION_BOUNDARY_FAILURE";
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
