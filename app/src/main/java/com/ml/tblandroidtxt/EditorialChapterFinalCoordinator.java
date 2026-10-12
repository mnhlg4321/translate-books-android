package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;
import com.ml.tblandroidtxt.editorial.pack.EditorialContractRevision;
import com.ml.tblandroidtxt.editorial.pack.EditorialL1Ledger;
import com.ml.tblandroidtxt.editorial.pack.EditorialL2Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialL3Execution;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotAuthorization;
import com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotProvider;
import com.ml.tblandroidtxt.editorial.pack.EditorialQaReceiptValidator;

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
     * Runs or resumes L2 (blind discovery + edit) then L3 (blind re-audit + reconcile) for a chapter whose
     * L1 is committed. The per-phase caps and the chain cap come from {@code budgets}; providers are
     * injected so tests use fakes and the app uses the OpenRouter adapters under an explicit user
     * authorization. Nothing here retries, repairs or re-dispatches a stage whose state is unknown.
     */
    public Result runToFinal(long projectId, String selector, String chapterKey, EditorialChainBudgets budgets,
                             EditorialL2Execution.Provider l2Provider, EditorialL2Execution.Provider l3Provider) {
        if (budgets == null || !budgets.valid()) {
            return new Result(Stage.L2, false, "L2_BUDGET_REQUIRED", null, 0);
        }
        Optional<EditorialP5CExactBindingExecution.CommittedL1> l1;
        try {
            l1 = committedChain(projectId, selector, chapterKey);
        } catch (IOException | RuntimeException error) {
            return new Result(Stage.L1_INCOMPLETE, false, "INPUT_L1_READBACK_FAILED", null, 0);
        }
        if (l1.isEmpty()) return new Result(Stage.L1_INCOMPLETE, false, "INPUT_REPORT_L1_NOT_COMMITTED", null, 0);
        EditorialP5CExactBindingExecution.CommittedL1 chain = l1.get();
        boolean ledger = EditorialContractRevision.isLedger(chain.context().contractRevision());
        if (ledger && budgets.finalRead() == null) {
            return new Result(Stage.L2, false, "L2_FINAL_READ_BUDGET_REQUIRED", null, 0);
        }
        // The protected spans of a ledger-contract REPORT_L1 (DRAFT numbering) go to both stages; the legacy
        // report carries none. Each stage maps them through the line map of the stages before it.
        Set<Integer> protectedLines = protectedLines(chain);
        EditorialL2Execution.Result l2 = new EditorialL2Execution().execute(
                new EditorialL2Execution.Request(chain.context(), chain.reportL1AttemptIdentity(),
                        chain.reportL1Bytes(), protectedLines),
                budgets.discovery(), budgets.edit(), budgets.finalRead(), l2Provider,
                new EditorialPhaseArtifactStore(database, chapterKey, EditorialPhaseArtifactStore.L2_PHASE));
        if (!l2.accepted()) return new Result(Stage.L2, false, l2.reasonCode(), null, l2.providerCalls());

        EditorialL3Execution.Result l3 = new EditorialL3Execution().execute(
                new EditorialL3Execution.Request(chain.context(), chain.reportL1AttemptIdentity(),
                        chain.reportL1Bytes(), l2.committed(), protectedLines),
                budgets.reaudit(), budgets.reconcile(), budgets.finalRead(), l3Provider,
                new EditorialPhaseArtifactStore(database, chapterKey, EditorialPhaseArtifactStore.L3_PHASE));
        int calls = l2.providerCalls() + l3.providerCalls();
        if (!l3.accepted()) return new Result(Stage.L3, false, l3.reasonCode(), null, calls);
        return new Result(Stage.FINAL, true, l3.reasonCode(), l3.committed(), calls);
    }

    /**
     * Starts or resumes the complete ledger-v2 chapter action at L1. RAW and RECONCILE keep their own durable
     * attempt identities; after a restart, a committed phase is read back and an UNKNOWN phase is refused by
     * the attempt store before the provider can be called. Only after both L1 artifacts read back does the
     * same coordinator continue through L2, L3, and FINAL.
     */
    public Result runFromL1(long projectId, String selector, String chapterKey, EditorialChainBudgets budgets,
                            EditorialP5PilotAuthorization rawAuthorization,
                            EditorialP5PilotAuthorization reconcileAuthorization,
                            EditorialP5PilotProvider l1Provider,
                            EditorialL2Execution.Provider l2Provider,
                            EditorialL2Execution.Provider l3Provider) {
        if (budgets == null || !budgets.valid() || !budgets.includesL1() || budgets.finalRead() == null) {
            return new Result(Stage.L1_INCOMPLETE, false, "L1_FULL_CHAIN_BUDGET_REQUIRED", null, 0);
        }
        if (!authorizationFits(rawAuthorization, "L1_RAW_DISCOVERY", budgets.l1Raw())
                || !authorizationFits(reconcileAuthorization, "L1_RECONCILE", budgets.l1Reconcile())) {
            return new Result(Stage.L1_INCOMPLETE, false, "L1_AUTHORIZATION_SCOPE_MISMATCH", null, 0);
        }
        if (l1Provider == null || l2Provider == null || l3Provider == null) {
            return new Result(Stage.L1_INCOMPLETE, false, "L1_PROVIDER_NOT_CONFIGURED", null, 0);
        }
        EditorialP5CExactBindingExecution l1 = EditorialP5CExactBindingExecution.forContract(
                database, storage, EditorialContractRevision.CURRENT_LEDGER);
        EditorialP5CExactBindingExecution.Result raw = l1.executeRaw(projectId, selector, chapterKey,
                rawAuthorization, l1Provider);
        if (!raw.accepted()) {
            return new Result(Stage.L1_INCOMPLETE, false, raw.reasonCode(), null, raw.providerCalls());
        }
        EditorialP5CExactBindingExecution.Result reconcile = l1.executeReconcile(projectId, selector, chapterKey,
                reconcileAuthorization, l1Provider);
        int l1Calls = raw.providerCalls() + reconcile.providerCalls();
        if (!reconcile.accepted()) {
            return new Result(Stage.L1_INCOMPLETE, false, reconcile.reasonCode(), null, l1Calls);
        }
        Result remainder = runToFinal(projectId, selector, chapterKey, budgets, l2Provider, l3Provider);
        return new Result(remainder.stage(), remainder.finalReady(), remainder.reasonCode(),
                remainder.finalArtifact(), l1Calls + remainder.providerCalls());
    }

    private static boolean authorizationFits(EditorialP5PilotAuthorization authorization, String phase,
                                             EditorialL2Execution.Budget budget) {
        if (authorization == null || budget == null) return false;
        int inputTokens = (budget.maximumInputBytes() + 1) / 2;
        return phase.equals(authorization.phase())
                && authorization.maximumPrimarySemanticCalls() == 1
                && authorization.maximumSchemaRepairCalls() == 0
                && authorization.maximumNetworkRetries() == 0
                && authorization.maximumInputTokens() == inputTokens
                && authorization.maximumOutputTokens() == budget.maximumOutputTokens()
                && authorization.maximumTotalTokens() == inputTokens + budget.maximumOutputTokens()
                && authorization.maximumTotalCost().compareTo(budget.maximumCost()) == 0
                && authorization.maximumExecutionTimeMillis() == budget.maximumExecutionTimeMillis()
                && authorization.allowChapterToProvider()
                && !authorization.allowFullModelResponseStorage()
                && !authorization.allowRequestBodyStorage()
                && "HASH_ONLY".equals(authorization.evidenceRedactionPolicy())
                && authorization.singleUse();
    }

    /**
     * The committed L1 chain of a chapter: the ledger-contract chain when there is one, otherwise the legacy
     * chain (history stays readable; a legacy report is never fed to a ledger chain).
     */
    private Optional<EditorialP5CExactBindingExecution.CommittedL1> committedChain(
            long projectId, String selector, String chapterKey) throws IOException {
        Optional<EditorialP5CExactBindingExecution.CommittedL1> ledger = EditorialP5CExactBindingExecution
                .forContract(database, storage, EditorialContractRevision.CURRENT_LEDGER)
                .committedL1(projectId, selector, chapterKey);
        if (ledger.isPresent()) return ledger;
        return new EditorialP5CExactBindingExecution(database, storage).committedL1(projectId, selector, chapterKey);
    }

    static Set<Integer> protectedLines(EditorialP5CExactBindingExecution.CommittedL1 chain) {
        if (!EditorialContractRevision.isLedger(chain.context().contractRevision())) return Set.of();
        return EditorialL1Ledger.protectedLines(EditorialL1Ledger.parseBody(
                EditorialCanonicalJson.parseObject(chain.reportL1Bytes())).protectedSpans());
    }

    /** Durable progress of a chapter plus the committed FINAL when there is one. */
    public record Inspection(EditorialChapterProgress.Progress progress, EditorialL2Execution.Committed finalArtifact,
                             String receiptStatus, String l1ContractRevision, boolean stateReadable) {
        public Inspection {
            receiptStatus = receiptStatus == null ? "NOT_APPLICABLE" : receiptStatus;
            l1ContractRevision = l1ContractRevision == null ? "" : l1ContractRevision;
        }

        public Inspection(EditorialChapterProgress.Progress progress, EditorialL2Execution.Committed finalArtifact,
                          String receiptStatus, String l1ContractRevision) {
            this(progress, finalArtifact, receiptStatus, l1ContractRevision, true);
        }

        public Inspection(EditorialChapterProgress.Progress progress, EditorialL2Execution.Committed finalArtifact,
                          String receiptStatus) {
            this(progress, finalArtifact, receiptStatus, "", true);
        }

        public Inspection(EditorialChapterProgress.Progress progress, EditorialL2Execution.Committed finalArtifact) {
            this(progress, finalArtifact, "NOT_APPLICABLE", "", true);
        }
    }

    /**
     * Read-only: derives the chapter's progress from durable rows without claiming, dispatching or
     * repairing anything, so the UI can show it after any restart or process death.
     */
    public Inspection inspect(long projectId, String selector, String chapterKey) {
        Optional<EditorialP5CExactBindingExecution.CommittedL1> l1;
        try {
            l1 = committedChain(projectId, selector, chapterKey);
        } catch (IOException | RuntimeException error) {
            return new Inspection(EditorialChapterProgress.unreadable("INPUT_L1_READBACK_FAILED"), null,
                    "NOT_APPLICABLE", "", false);
        }
        if (l1.isEmpty()) return new Inspection(EditorialChapterProgress.derive(false, null, null), null);
        EditorialP5CExactBindingExecution.CommittedL1 chain = l1.get();
        EditorialPhaseArtifactStore l2Store = new EditorialPhaseArtifactStore(database, chapterKey,
                EditorialPhaseArtifactStore.L2_PHASE);
        EditorialPhaseArtifactStore l3Store = new EditorialPhaseArtifactStore(database, chapterKey,
                EditorialPhaseArtifactStore.L3_PHASE);
        try {
            String l2Identity = new EditorialL2Execution.Request(chain.context(), chain.reportL1AttemptIdentity(),
                    chain.reportL1Bytes(), Set.of()).attemptIdentity();
            EditorialChapterProgress.StageRow l2Row = l2Store.inspect(l2Identity).orElse(null);
            EditorialChapterProgress.StageRow l3Row = null;
            EditorialL2Execution.Committed finalArtifact = null;
            Optional<EditorialL2Execution.Committed> l2Committed = l2Store.findCommitted(l2Identity);
            if (l2Committed.isPresent()) {
                String l3Identity = new EditorialL3Execution.Request(chain.context(), chain.reportL1AttemptIdentity(),
                        chain.reportL1Bytes(), l2Committed.get(), Set.of()).attemptIdentity();
                l3Row = l3Store.inspect(l3Identity).orElse(null);
                finalArtifact = l3Store.findCommitted(l3Identity).orElse(null);
            }
            String receiptStatus = "NOT_APPLICABLE";
            if (finalArtifact != null && EditorialContractRevision.isLedger(chain.context().contractRevision())) {
                // a ledger-contract FINAL is only final when its receipt is backed by operations on these exact bytes
                EditorialQaReceiptValidator.Result receipt = EditorialQaReceiptValidator.validate(
                        finalArtifact.changeMapBytes(), finalArtifact.viL2Bytes());
                receiptStatus = receipt.valid() ? "VERIFIED" : "INVALID:" + receipt.issues().get(0);
                if (!receipt.valid() && l3Row != null) {
                    l3Row = new EditorialChapterProgress.StageRow(l3Row.status(), l3Row.recoveryReasonCode(), false);
                    finalArtifact = null;
                }
            } else if (finalArtifact != null) {
                receiptStatus = "LEGACY_UNVERIFIED";
            }
            EditorialChapterProgress.Progress progress = EditorialChapterProgress.derive(true, l2Row, l3Row);
            return new Inspection(progress, progress.finalReady() ? finalArtifact : null, receiptStatus,
                    chain.context().contractRevision());
        } catch (RuntimeException error) {
            return new Inspection(EditorialChapterProgress.unreadable("INPUT_CHAPTER_STATE_READBACK_FAILED"), null,
                    "NOT_APPLICABLE", chain.context().contractRevision(), false);
        }
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
        // a receipt of the ledger contract must be backed by its recorded operations; legacy receipts stay exportable
        EditorialQaReceiptValidator.Result receipt = EditorialQaReceiptValidator.validate(finalArtifact.changeMapBytes(), bytes);
        if (!receipt.valid()) return new ExportResult(false, "EXPORT_RECEIPT_INVALID", "", 0L);
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
