package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Builds one isolated L1 package. It never accepts L2/L3 files or a prior conversation. */
public final class EditorialL1ContextBuilder {
    public static final int MAX_INLINE_SOURCE_CHARS = 26000;
    public static final class Context {
        public final PromptPair prompt; public final String manifestJson; public final String rawMapJson;
        Context(PromptPair prompt,String manifestJson,String rawMapJson){this.prompt=prompt;this.manifestJson=manifestJson;this.rawMapJson=rawMapJson;}
    }
    public static Context build(long chapterId,List<EditorialRepository.AssetSnapshot> assets) {
        if(chapterId<=0)throw new IllegalArgumentException("Chapter id is required");
        EnumSet<EditorialWorkflowV5.AssetRole> roles=EnumSet.noneOf(EditorialWorkflowV5.AssetRole.class); for(EditorialRepository.AssetSnapshot asset:assets)if(asset!=null)roles.add(asset.role);
        String roleError=EditorialWorkflowV5.validateContext(EditorialWorkflowV5.ContextPhase.L1_AUDIT,roles); if(roleError!=null)throw new IllegalArgumentException(roleError);
        EditorialRepository.AssetSnapshot raw=find(assets,EditorialWorkflowV5.AssetRole.RAW),draft=find(assets,EditorialWorkflowV5.AssetRole.DRAFT),glossary=find(assets,EditorialWorkflowV5.AssetRole.GLOSSARY),pronoun=find(assets,EditorialWorkflowV5.AssetRole.PRONOUN);
        int chars=raw.content.length()+draft.content.length()+glossary.content.length()+pronoun.content.length(); if(chars>MAX_INLINE_SOURCE_CHARS)throw new IllegalArgumentException("Chapter exceeds safe L1 context size; scene segmentation is required before audit");
        try {
            EditorialRawMap map=EditorialRawMap.create(raw.content); JSONObject manifest=new JSONObject(); JSONArray source=new JSONArray();
            for(EditorialRepository.AssetSnapshot asset:assets)source.put(new JSONObject().put("role",asset.role.name()).put("sha256",asset.sha256).put("name",asset.displayName)); manifest.put("chapterId",String.valueOf(chapterId)).put("rawMapHash",map.sha256).put("assets",source);
            JSONObject rawMap=new JSONObject().put("rawHash",map.sha256).put("anchorCount",map.anchors.size()); JSONArray anchors=new JSONArray();for(EditorialRawMap.Anchor anchor:map.anchors)anchors.put(new JSONObject().put("id",anchor.id).put("text",anchor.text));rawMap.put("anchors",anchors);
            String system="You are L1 diagnostic auditor for Japanese-to-Vietnamese fiction. Audit only; never rewrite or output a translation. Follow this order: read glossary/pronoun, read all RAW and create a scene ledger, then compare DRAFT sequentially. Return ONLY valid JSON matching this exact schema: "+EditorialEvidenceSchema.l1ReportSchema();
            String user="CHAPTER ID: "+chapterId+"\nRAW MAP (created before DRAFT review):\n"+rawMap+"\n\nGLOSSARY:\n"+glossary.content+"\n\nPRONOUN:\n"+pronoun.content+"\n\nRAW:\n"+raw.content+"\n\nDRAFT:\n"+draft.content+"\n\nExit gate: every scene CLOSED, anchors cover first to last RAW, every issue has both currentMeaning and rawEvidence. No prose outside JSON.";
            return new Context(new PromptPair(system,user),manifest.toString(),rawMap.toString());
        } catch(Exception error) { throw new IllegalStateException("Could not build L1 context",error); }
    }
    /** Creates the durable RAW-first chapter ledger used before any segmented L1 audit call. */
    public static EditorialSceneSegmenter.Plan segmentRawForL1(List<EditorialRepository.AssetSnapshot> assets) {
        EditorialRepository.AssetSnapshot raw=find(assets,EditorialWorkflowV5.AssetRole.RAW);
        return EditorialSceneSegmenter.split(raw.content);
    }
    /** Evidence-based draft pairing for segmented L1; ambiguous pairs stay explicit and cannot be auto-audited. */
    public static List<EditorialDraftSceneMapper.Pair> mapDraftForL1(List<EditorialRepository.AssetSnapshot> assets) {
        return EditorialDraftSceneMapper.map(find(assets,EditorialWorkflowV5.AssetRole.RAW).content,find(assets,EditorialWorkflowV5.AssetRole.DRAFT).content);
    }
    private static EditorialRepository.AssetSnapshot find(List<EditorialRepository.AssetSnapshot> assets,EditorialWorkflowV5.AssetRole role){for(EditorialRepository.AssetSnapshot asset:assets)if(asset.role==role)return asset;throw new IllegalArgumentException("Missing asset: "+role);}
    private EditorialL1ContextBuilder(){}
}
