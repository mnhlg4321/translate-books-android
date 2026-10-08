package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.api.EditorialApiContract;

import java.math.BigDecimal;

/** One run of the edit/check flow on a snapshot of the sources; opened again later without calling the provider. */
public final class EditorialApiRun {
    public long id;
    public long comboId;
    public String contractRevision = EditorialApiContract.CONTRACT_REVISION;
    public String qualityCoreSha256 = "";
    public String model = "";
    public EditorialApiContract.Mode mode = EditorialApiContract.Mode.QUICK;
    public EditorialApiContract.RunState state = EditorialApiContract.RunState.RUNNING;

    public String rawSha256 = "";
    public String rawText = "";
    public String draftSha256 = "";
    public String draftText = "";
    public String glossarySha256 = "";
    public String glossaryText = "";
    public String pronounSha256 = "";
    public String pronounText = "";
    /** JSON snapshot of the original V5 source attachments; empty for the ordinary E path. */
    public String originalSourceFilesJson = "[]";

    public String editedText = "";
    public String finalText = "";
    public String notesJson = "[]";
    public String issuesJson = "[]";
    public String guardsJson = "{}";
    public String stepsJson = "[]";
    public String wrongPairEvidence = "";
    public int calls;
    public long inputTokens;
    public long outputTokens;
    public BigDecimal usd = BigDecimal.ZERO;
    public boolean costKnown = true;
    public int glossaryEntries;
    public int pronounRows;
    public String error = "";
    public long createdAt;
    public long updatedAt;

    public boolean finished() { return state != EditorialApiContract.RunState.RUNNING; }

    public EditorialApiRun copy() {
        EditorialApiRun r = new EditorialApiRun();
        r.id = id; r.comboId = comboId; r.contractRevision = contractRevision; r.qualityCoreSha256 = qualityCoreSha256;
        r.model = model; r.mode = mode; r.state = state;
        r.rawSha256 = rawSha256; r.rawText = rawText; r.draftSha256 = draftSha256; r.draftText = draftText;
        r.glossarySha256 = glossarySha256; r.glossaryText = glossaryText; r.pronounSha256 = pronounSha256; r.pronounText = pronounText;
        r.originalSourceFilesJson = originalSourceFilesJson;
        r.editedText = editedText; r.finalText = finalText; r.notesJson = notesJson; r.issuesJson = issuesJson;
        r.guardsJson = guardsJson; r.stepsJson = stepsJson; r.wrongPairEvidence = wrongPairEvidence; r.calls = calls;
        r.inputTokens = inputTokens; r.outputTokens = outputTokens; r.usd = usd; r.costKnown = costKnown;
        r.glossaryEntries = glossaryEntries; r.pronounRows = pronounRows; r.error = error;
        r.createdAt = createdAt; r.updatedAt = updatedAt;
        return r;
    }
}
