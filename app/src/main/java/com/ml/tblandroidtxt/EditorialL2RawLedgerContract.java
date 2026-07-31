package com.ml.tblandroidtxt;
import org.json.JSONArray;import org.json.JSONObject;
/** Independent RAW-only ledger contract that must close before L2 may see DRAFT or REPORT_L1. */
public final class EditorialL2RawLedgerContract {
 public static String schema(){return "{\"chapterId\":\"string\",\"scenes\":[{\"id\":\"string\",\"rawStart\":\"string\",\"rawEnd\":\"string\",\"pov\":\"string\",\"cast\":\"string\",\"event\":\"string\",\"risk\":\"string\",\"status\":\"CLOSED\"}]}";}
 public static String validate(String text,String chapterId){try{JSONObject root=new JSONObject(text);if(!chapterId.equals(root.optString("chapterId")))return "RAW ledger chapterId mismatch";JSONArray scenes=root.optJSONArray("scenes");if(scenes==null||scenes.length()==0)return "RAW ledger requires scenes";for(int i=0;i<scenes.length();i++){JSONObject s=scenes.optJSONObject(i);if(s==null||blank(s.optString("id"))||blank(s.optString("rawStart"))||blank(s.optString("rawEnd"))||blank(s.optString("pov"))||blank(s.optString("cast"))||blank(s.optString("event"))||blank(s.optString("risk"))||!"CLOSED".equals(s.optString("status")))return "RAW ledger scene is incomplete";}return null;}catch(Exception e){return "Invalid RAW ledger JSON";}}
 private static boolean blank(String x){return x==null||x.trim().isEmpty();}private EditorialL2RawLedgerContract(){}
}
