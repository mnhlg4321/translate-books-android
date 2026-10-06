package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.PairContract;
import com.ml.tblandroidtxt.editorial.api.PairStates;

import java.math.BigDecimal;

/** Rows of the chunk-pair run: the run with its frozen sources, one item per pair, and the reservations of the paid calls. */
public final class EditorialPairModels {
    private EditorialPairModels() { }

    public static final String ARM_CHUNK = "C";
    public static final String ARM_WHOLE = "W";
    public static final String SOURCE_JOB = "JOB";
    public static final String SOURCE_FILES = "FILES";

    /** One run on a snapshot. The sources are copied into the row: reopening never needs the original files or the job. */
    public static final class PairRun {
        public long id;
        public long comboId;
        public String arm = ARM_CHUNK;
        public String contractRevision = PairContract.REVISION;
        public String model = "";
        public String targetLanguage = "Vietnamese";
        public PairStates.RunState state = PairStates.RunState.PREPARED;
        public String sourceKind = SOURCE_JOB;
        public String sourceRef = "";
        public String sourceLabel = "";
        public String chapterId = "";
        public String rawRowsJson = "[]";
        public String draftRowsJson = "[]";
        public String glossaryText = "";
        public String glossarySha256 = "";
        public String pronounText = "";
        public String pronounSha256 = "";
        public String cueFields = "from,speaker,target";
        public String mapRevision = "";
        public String mapHash = "";
        public BigDecimal capUsd = new BigDecimal("0.10");
        public boolean started;
        public boolean paused;
        public boolean costOverrun;
        public String mergedText = "";
        public String mergeReceiptJson = "";
        public int warnings;
        public int calls;
        public long inputTokens;
        public long outputTokens;
        public BigDecimal usd = BigDecimal.ZERO;
        public boolean costKnown = true;
        public String error = "";
        public long createdAt;
        public long updatedAt;

        public PairRun copy() {
            PairRun r = new PairRun();
            r.id = id; r.comboId = comboId; r.arm = arm; r.contractRevision = contractRevision; r.model = model; r.targetLanguage = targetLanguage;
            r.state = state; r.sourceKind = sourceKind; r.sourceRef = sourceRef; r.sourceLabel = sourceLabel; r.chapterId = chapterId;
            r.rawRowsJson = rawRowsJson; r.draftRowsJson = draftRowsJson; r.glossaryText = glossaryText; r.glossarySha256 = glossarySha256;
            r.pronounText = pronounText; r.pronounSha256 = pronounSha256; r.cueFields = cueFields; r.mapRevision = mapRevision; r.mapHash = mapHash;
            r.capUsd = capUsd; r.started = started; r.paused = paused; r.costOverrun = costOverrun; r.mergedText = mergedText;
            r.mergeReceiptJson = mergeReceiptJson; r.warnings = warnings; r.calls = calls; r.inputTokens = inputTokens; r.outputTokens = outputTokens;
            r.usd = usd; r.costKnown = costKnown; r.error = error; r.createdAt = createdAt; r.updatedAt = updatedAt;
            return r;
        }
    }

    /** One pair of a run. {@code candidateText} and {@code responseText} are kept even when the gate blocks, for diagnosis. */
    public static final class PairItem {
        public long id;
        public long runId;
        public String pairId = "";
        public int ordinal;
        public PairStates.PairState state = PairStates.PairState.IMPORTED;
        public int attempt;
        /** Counts how often recovery gave the pair back as never dispatched; part of the request identity so a new try is a new call. */
        public int generation;
        public String requestId = "";
        public String candidateText = "";
        public String responseText = "";
        public String gateJson = "";
        public String responseHash = "";
        public int calls;
        public long inputTokens;
        public long outputTokens;
        public BigDecimal usd = BigDecimal.ZERO;
        public boolean costKnown = true;
        public String error = "";
        public long updatedAt;

        public PairItem copy() {
            PairItem i = new PairItem();
            i.id = id; i.runId = runId; i.pairId = pairId; i.ordinal = ordinal; i.state = state; i.attempt = attempt; i.generation = generation; i.requestId = requestId;
            i.candidateText = candidateText; i.responseText = responseText; i.gateJson = gateJson; i.responseHash = responseHash; i.calls = calls;
            i.inputTokens = inputTokens; i.outputTokens = outputTokens; i.usd = usd; i.costKnown = costKnown; i.error = error; i.updatedAt = updatedAt;
            return i;
        }
    }

    /** The durable reservation of one paid call: opened before dispatch, settled once with the real charge or at zero. */
    public static final class PairReservation {
        public static final String RESERVED = "RESERVED";
        public static final String SETTLED = "SETTLED";
        public static final String SETTLED_ZERO = "SETTLED_ZERO";

        public String callId = "";
        public long runId;
        public String pairId = "";
        public BigDecimal worstUsd = BigDecimal.ZERO;
        public String state = RESERVED;
        public BigDecimal settledUsd = BigDecimal.ZERO;
        public String reason = "";

        public boolean open() { return RESERVED.equals(state); }
    }
}
