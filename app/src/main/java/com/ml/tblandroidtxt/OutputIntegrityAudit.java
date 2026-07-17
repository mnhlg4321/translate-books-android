package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class OutputIntegrityAudit {
    public static Result audit(List<TranslationRepository.ChunkRow> rows) {
        Result r=new Result(); if(rows==null||rows.isEmpty()){r.errors.add("NO_ACTIVE_CHUNKS");return r;}
        HashSet<Integer>indices=new HashSet<>();int expected=0,expectedOffset=0;String prior="";boolean offsetsKnown=true;
        for(TranslationRepository.ChunkRow c:rows){
            if("SUPERSEDED".equalsIgnoreCase(c.status)||"superseded".equalsIgnoreCase(c.status))continue;
            if(!indices.add(c.index))r.errors.add("DUPLICATE_INDEX:"+c.index);
            if(c.index!=expected)r.errors.add("MISSING_OR_REORDERED_INDEX:"+expected); expected++;
            if(c.startOffset<0||c.endOffset<0)offsetsKnown=false;else{if(c.startOffset!=expectedOffset)r.errors.add("SOURCE_GAP_OR_OVERLAP:"+c.index);if(c.endOffset<c.startOffset)r.errors.add("INVALID_SOURCE_RANGE:"+c.index);expectedOffset=c.endOffset;}
            if(c.sourceHash!=null&&!c.sourceHash.isEmpty()&&!c.sourceHash.equals(HashUtil.sha256(c.source)))r.errors.add("SOURCE_HASH_MISMATCH:"+c.index);
            if(!"COMPLETED".equalsIgnoreCase(c.status)&&!"done".equalsIgnoreCase(c.status))r.errors.add("INCOMPLETE_CHUNK:"+c.index);
            if(c.translated==null||c.translated.trim().isEmpty())r.errors.add("BLANK_OUTPUT:"+c.index);
            String lower=c.translated==null?"":c.translated.trim().toLowerCase(Locale.ROOT);
            if(lower.startsWith("error:")||lower.contains("invalid api key"))r.errors.add("PROVIDER_ERROR_OUTPUT:"+c.index);
            if(c.rejectedResponse!=null&&!c.rejectedResponse.isEmpty()&&normalize(c.rejectedResponse).equals(normalize(c.translated)))r.errors.add("REJECTED_RESPONSE_ACCEPTED:"+c.index);
            if(!prior.isEmpty()&&normalize(prior).equals(normalize(c.translated)))r.warnings.add("DUPLICATE_ADJACENT_OUTPUT:"+c.index);
            prior=c.translated==null?"":c.translated;
        }
        if(!offsetsKnown)r.warnings.add("SOURCE_OFFSETS_UNKNOWN_FOR_MIGRATED_JOB");
        return r;
    }
    public static class Result { public final List<String>errors=new ArrayList<>(),warnings=new ArrayList<>();public boolean passed(){return errors.isEmpty();}public String status(){return !passed()?"FAILED_INTEGRITY_CHECK":warnings.isEmpty()?"COMPLETED":"COMPLETED_WITH_WARNINGS";} }
    private static String normalize(String s){return s==null?"":s.replaceAll("\\s+"," ").trim();} private OutputIntegrityAudit(){}
}
