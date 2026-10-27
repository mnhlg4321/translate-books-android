package com.ml.tblandroidtxt;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.*;
import com.ml.tblandroidtxt.editorial.api.fix.*;

/** Stored in AppSettings and frozen in the existing cue-fields text; no database migration. */
public final class EditorialPreferences {
    public String model="", reasoning="medium";
    public BigDecimal cap=new BigDecimal("0.10");
    public boolean address=true, glossary=true, kana=true, punctuation=true;
    private static final String MARKER="v422=";
    public String toJson() {
        try{return new JSONObject().put("model",model).put("reasoning",reasoning).put("cap",cap.toPlainString())
                .put("address",address).put("glossary",glossary).put("kana",kana).put("punctuation",punctuation).toString();}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static EditorialPreferences fromJson(String json) {
        EditorialPreferences p=new EditorialPreferences();
        try {JSONObject o=new JSONObject(json==null||json.isEmpty()?"{}":json);p.model=o.optString("model","");p.reasoning=o.optString("reasoning","medium");
            BigDecimal cap=new BigDecimal(o.optString("cap","0.10"));if(cap.signum()>0)p.cap=cap;
            p.address=o.optBoolean("address",true);p.glossary=o.optBoolean("glossary",true);p.kana=o.optBoolean("kana",true);p.punctuation=o.optBoolean("punctuation",true);
        }catch(Exception ignored){}return p;
    }
    public EditorialPreferences copy(){return fromJson(toJson());}
    public String freeze(String cues){return (cues==null||cues.isEmpty()?"from,speaker,target":cues)+","+MARKER
            +Base64.getUrlEncoder().withoutPadding().encodeToString(toJson().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public static EditorialPreferences frozen(String cues){
        for(String value:(cues==null?"":cues).split(","))if(value.startsWith(MARKER))
            return fromJson(new String(Base64.getUrlDecoder().decode(value.substring(MARKER.length())),java.nio.charset.StandardCharsets.UTF_8));
        return new EditorialPreferences();
    }
    private boolean enabled(FixPoint.Type type){switch(type){case ADDRESS:return address;case GLOSSARY:return glossary;case KANA:case MISSING:return kana;default:return false;}}
    public FixPointFinder.Result filter(FixPointFinder.Result found){
        List<FixPoint> out=new ArrayList<>();
        for(FixPoint point:found.points()){
            Set<FixPoint.Type> types=new LinkedHashSet<>();for(FixPoint.Type type:point.types())if(enabled(type))types.add(type);
            if(types.isEmpty())continue;
            List<String> rules=new ArrayList<>();for(String rule:point.rules()){
                boolean keep=false;for(FixPoint.Type type:types)if(rule.startsWith(type.name()+":"))keep=true;
                if(keep)rules.add(rule);
            }
            if(rules.isEmpty())continue;
            out.add(new FixPoint(point.id(),types,point.draftLineIndex(),point.draftText(),point.rawText(),rules,
                    types.contains(FixPoint.Type.ADDRESS)?point.addressRows():List.of(),point.rawContext(),point.sceneEntities()));
        }
        return new FixPointFinder.Result(out,found.ambiguousGlossarySplits(),found.alignedRawLines(),found.alignedDraftLines(),
                punctuation?found.punctuation():List.of());
    }
}
