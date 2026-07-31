package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic RAW-only segmentation. Every anchor appears once; no text is silently dropped or overlapped. */
public final class EditorialSceneSegmenter {
    public static final int DEFAULT_MAX_RAW_CHARS = 7000;
    public static final class Scene { public final String id,startAnchor,endAnchor,raw; Scene(String id,String start,String end,String raw){this.id=id;this.startAnchor=start;this.endAnchor=end;this.raw=raw;} }
    public static final class Plan { public final EditorialRawMap map; public final List<Scene> scenes; Plan(EditorialRawMap map,List<Scene> scenes){this.map=map;this.scenes=Collections.unmodifiableList(scenes);} public String ledgerJson(){try{JSONObject root=new JSONObject().put("rawHash",map.sha256);JSONArray rows=new JSONArray();for(Scene s:scenes)rows.put(new JSONObject().put("id",s.id).put("rawStart",s.startAnchor).put("rawEnd",s.endAnchor).put("chars",s.raw.length()).put("status","PENDING_AUDIT"));return root.put("scenes",rows).toString();}catch(Exception e){throw new IllegalStateException(e);}} }
    public static Plan split(String raw){return split(raw,DEFAULT_MAX_RAW_CHARS);}
    public static Plan split(String raw,int maxChars){if(maxChars<256)throw new IllegalArgumentException("Segment size is too small");EditorialRawMap map=EditorialRawMap.create(raw);ArrayList<Scene> out=new ArrayList<>();int first=0,chars=0,ordinal=1;
        for(int i=0;i<map.anchors.size();i++){EditorialRawMap.Anchor a=map.anchors.get(i);if(i>first&&chars+a.text.length()>maxChars&&breakBefore(map,i)){out.add(scene(ordinal++,map,first,i));first=i;chars=0;}chars+=a.text.length();}
        if(first<map.anchors.size())out.add(scene(ordinal,map,first,map.anchors.size()));return new Plan(map,out);
    }
    private static boolean breakBefore(EditorialRawMap map,int index){EditorialRawMap.Anchor prior=map.anchors.get(index-1);EditorialRawMap.Anchor next=map.anchors.get(index);return prior.structuralMarker||next.structuralMarker||prior.text.trim().isEmpty()||next.text.trim().isEmpty()||index>0;}
    private static Scene scene(int ordinal,EditorialRawMap map,int from,int to){StringBuilder raw=new StringBuilder();for(int i=from;i<to;i++)raw.append(map.anchors.get(i).text);return new Scene(String.format(java.util.Locale.US,"scene-%03d",ordinal),map.anchors.get(from).id,map.anchors.get(to-1).id,raw.toString());}
    private EditorialSceneSegmenter(){}
}
