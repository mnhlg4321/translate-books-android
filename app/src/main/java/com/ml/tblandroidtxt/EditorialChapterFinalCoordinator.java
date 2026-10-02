package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One chapter, one writer: committed REPORT_L1 -> L2_EDIT -> L3 -> FINAL. Each
 * stage is durable and idempotent, so a restart resumes from the last committed
 * stage and never re-dispatches a stage whose external state is unknown. The
 * same class backs the UI and the instrumented tests; there is no separate CLI path.
 */
public final class EditorialChapterFinalCoordinator {
    public enum Stage { L1_INCOMPLETE, L2, L3, FINAL }

    public record Result(Stage stage, boolean finalReady, String reasonCode,
                         EditorialL2Execution.Committed finalArtifact, int providerCalls) {
        public Result {
            Objects.requireNonNull(stage, "stage");
            if (reasonCode == null || reasonCode.isBlank()) throw new IllegalArgumentException("reason is required");
        }
    }

    public record ExportResult(boolean verified, String reasonCode, String sha256, long byteCount) { }

    @FunctionalInterface
    public interface OutputOpener { OutputStream open() throws IOException; }

    @FunctionalInterface
    public interface InputOpener { InputStream open() throws IOException; }

    private final TranslationRepository database;
    private final EditorialPackStorageLayout storage;

    public EditorialChapterFinalCoordinator(TranslationRepository database, EditorialPackStorageLayout storage) {
        this.database = Objects.requireNonNull(database, "database");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /**
     * Runs or resumes L2 then L3 for a chapter whose L1 is committed. Each budget
     * covers its own stage; providers are injected so tests use fakes and the app
     * uses the OpenRouter adapters under an explicit user authorization.
     */
    public Result runToFinal(long projectId, String selector, String chapterKey,
                             EditorialL2Execution.Budget l2Budget, EditorialL2Execution.Provider l2Provider,
                             EditorialL2Execution.Budget l3Budget, EditorialL2Execution.Provider l3Provider) {
        Optional<EditorialP5CExactBindingExecution.CommittedL1> l1;
        try {
            l1 = new EditorialP5CExactBindingExecution(database, storage).committedL1(projectId, selector, chapterKey);
        } catch (IOException | RuntimeException error) {
            return new Result(Stage.L1_INCOMPLETE, false, "INPUT_L1_READBACK_FAILED", null, 0);
        }
        if (l1.isEmpty()) return new Result(Stage.L1_INCOMPLETE, false, "INPUT_REPORT_L1_NOT_COMMITTED", null, 0);
        EditorialP5CExactBindingExecution.CommittedL1 chain = l1.get();
        // Protected spans are not yet carried by REPORT_L1; the set stays empty until it does.
        Set<Integer> protectedLines = Set.of();

        EditorialL2Execution.Result l2 = new EditorialL2Execution().execute(
                new EditorialL2Execution.Request(chain.context(), chain.reportL1AttemptIdentity(),
                        chain.reportL1Bytes(), protectedLines),
                l2Budget, l2Provider, new EditorialPhaseArtifactStore(database, chapterKey,
                        EditorialPhaseArtifactStore.L2_PHASE));
        if (!l2.accepted()) return new Result(Stage.L2, false, l2.reasonCode(), null, l2.providerCalls());

        EditorialL3Execution.Result l3 = new EditorialL3Execution().execute(
                new EditorialL3Execution.Request(chain.context(), chain.reportL1AttemptIdentity(),
                        chain.reportL1Bytes(), l2.committed(), protectedLines),
                l3Budget, l3Provider, new EditorialPhaseArtifactStore(database, chapterKey,
                        EditorialPhaseArtifactStore.L3_PHASE));
        int calls = l2.providerCalls() + l3.providerCalls();
        if (!l3.accepted()) return new Result(Stage.L3, false, l3.reasonCode(), null, calls);
        return new Result(Stage.FINAL, true, l3.reasonCode(), l3.committed(), calls);
    }

    /**
     * Writes exactly the FINAL bytes (UTF-8 text only, no report or JSON), then
     * reads the destination back and compares SHA-256 and length. A failed or
     * cancelled export never touches the stored FINAL.
     */
    public static ExportResult exportTxt(EditorialL2Execution.Committed finalArtifact,
                                         OutputOpener output, InputOpener readback) {
        if (finalArtifact == null) return new ExportResult(false, "EXPORT_FINAL_MISSING", "", 0L);
        byte[] bytes = finalArtifact.viL2Bytes();
        if (!EditorialCanonicalJson.sha256Hex(bytes).equals(finalArtifact.viL2Sha256())) {
            return new ExportResult(false, "EXPORT_FINAL_INTEGRITY_INVALID", "", 0L);
        }
        try (OutputStream out = output.open()) {
            out.write(bytes);
            out.flush();
        } catch (IOException | RuntimeException error) {
            return new ExportResult(false, "EXPORT_WRITE_FAILED", "", 0L);
        }
        try (InputStream in = readback.open()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
                total += read;
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            boolean ok = hex.toString().equals(finalArtifact.viL2Sha256()) && total == bytes.length;
            return new ExportResult(ok, ok ? "EXPORT_VERIFIED" : "EXPORT_READBACK_MISMATCH", hex.toString(), total);
        } catch (Exception error) {
            return new ExportResult(false, "EXPORT_READBACK_FAILED", "", 0L);
        }
    }
}
