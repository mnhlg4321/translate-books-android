package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds a release artifact while exposing only the final text and non-content audit metadata. */
public final class EditorialReleaseBundle {
    public static final class Result {
        public final byte[] zip; public final String manifest; public final String sha256;
        Result(byte[] zip,String manifest){this.zip=zip;this.manifest=manifest;this.sha256=HashUtil.sha256(zip);}
    }

    public static Result build(EditorialRepository repo,long chapterId,long generatedAt) throws Exception {
        EditorialRepository.Chapter chapter=repo.getChapter(chapterId);
        if(chapter==null||chapter.state!=EditorialWorkflowV5.ChapterState.RELEASE_READY)throw new IllegalStateException("Chapter must be RELEASE_READY");
        long runId=repo.latestRunId(chapterId,"L3");EditorialRepository.Run run=repo.getRun(runId);
        if(run==null||!"CLOSED".equals(run.state))throw new IllegalStateException("Latest L3 run is not closed");
        String finalJson=repo.evidencePayload(runId,"FINAL_QA_JSON"),finalText=repo.evidencePayload(runId,"FINAL_QA_TEXT");
        String error=EditorialEvidenceValidator.validateL3Output(finalJson);if(error!=null)throw new IllegalStateException("FINAL_QA is invalid: "+error);
        JSONObject finalRoot=new JSONObject(finalJson);if(!String.valueOf(chapterId).equals(finalRoot.optString("chapterId"))||!finalText.equals(finalRoot.optString("output")))throw new IllegalStateException("FINAL_QA evidence mismatch");
        Map<EditorialWorkflowV5.Gate,EditorialWorkflowV5.GateStatus> gates=repo.gateStatuses(runId);
        if(!EditorialWorkflowV5.mayRelease(gates,"CLOSED".equals(finalRoot.optString("crossSceneVoiceAudit")),"CLOSED".equals(finalRoot.optString("finalReadThrough"))))throw new IllegalStateException("Release gates are not closed");
        EditorialRepository.Project project=repo.getProject(chapter.projectId);if(project==null)throw new IllegalStateException("Editorial project not found");
        JSONObject manifest=new JSONObject().put("schema","editorial-v5-release-redacted-1").put("generatedAt",generatedAt).put("chapterId",String.valueOf(chapter.id)).put("chapterKey",chapter.chapterKey).put("title",chapter.title).put("series",project.seriesName).put("volume",project.volumeName).put("workflowVersion",project.workflowVersion).put("workflowHash",run.workflowHash).put("l3RunId",run.id).put("provider",run.provider).put("model",run.model).put("promptHash",run.promptHash).put("inputManifestHash",HashUtil.sha256(run.inputManifestJson)).put("finalQaSha256",HashUtil.sha256(finalText)).put("finalQaBytes",finalText.getBytes(StandardCharsets.UTF_8).length).put("crossSceneVoiceAudit","CLOSED").put("finalReadThrough","CLOSED");
        JSONObject gateJson=new JSONObject();for(EditorialWorkflowV5.Gate gate:EditorialWorkflowV5.Gate.values())gateJson.put(gate.name(),gates.get(gate)==null?"MISSING":gates.get(gate).name());manifest.put("gates",gateJson);
        JSONArray evidence=new JSONArray();List<EditorialRepository.EvidenceSummary> summaries=repo.evidenceSummaries(runId);for(EditorialRepository.EvidenceSummary s:summaries)evidence.put(new JSONObject().put("type",s.type).put("sha256",s.hash).put("payloadLength",s.payloadLength).put("createdAt",s.createdAt));manifest.put("evidence",evidence).put("redaction",new JSONObject().put("contentExcluded",new JSONArray().put("RAW").put("DRAFT").put("GLOSSARY").put("PRONOUN").put("REPORT_L1").put("VI_L2").put("PROMPT").put("ISSUE_QUOTES").put("REASONING")).put("evidencePolicy","hash-length-status-only"));
        byte[] finalBytes=finalText.getBytes(StandardCharsets.UTF_8),manifestBytes=manifest.toString(2).getBytes(StandardCharsets.UTF_8);String sums=HashUtil.sha256(finalBytes)+"  FINAL_QA.txt\n"+HashUtil.sha256(manifestBytes)+"  EVIDENCE_REDACTED.json\n";
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){put(zip,"FINAL_QA.txt",finalBytes);put(zip,"EVIDENCE_REDACTED.json",manifestBytes);put(zip,"SHA256SUMS.txt",sums.getBytes(StandardCharsets.UTF_8));}return new Result(bytes.toByteArray(),manifest.toString());
    }
    private static void put(ZipOutputStream zip,String name,byte[] bytes)throws Exception{ZipEntry entry=new ZipEntry(name);entry.setTime(0L);zip.putNextEntry(entry);zip.write(bytes);zip.closeEntry();}
    private EditorialReleaseBundle(){}
}
