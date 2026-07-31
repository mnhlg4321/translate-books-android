package com.ml.tblandroidtxt;

import org.json.JSONArray;
import org.json.JSONObject;

/** Contract for one checkpointable L1 scene audit; global gates are deliberately absent. */
public final class EditorialL1SceneContract {
    public static String schema(){return "{\"sceneId\":\"string\",\"rawStart\":\"anchor\",\"rawEnd\":\"anchor\",\"coverage\":\"ALIGNED|MISSING|EXTRA|MISALIGNED\",\"status\":\"CLOSED\",\"issues\":[{\"id\":\"string\",\"severity\":\"CRITICAL|MAJOR|MINOR\",\"category\":\"COVERAGE|FIDELITY|REFERENCE|VOICE|LOGIC|ACTION|STRUCTURE|CONTINUITY\",\"rawAnchor\":\"string\",\"draftAnchor\":\"string\",\"currentMeaning\":\"string\",\"rawEvidence\":\"string\",\"deviationImpact\":\"string\",\"l2Scope\":\"string\"}]}";}
    public static String validate(String text){try{JSONObject r=new JSONObject(text);if(blank(r.optString("sceneId"))||blank(r.optString("rawStart"))||blank(r.optString("rawEnd")))return "Missing scene anchors";if(!one(r.optString("coverage"),"ALIGNED","MISSING","EXTRA","MISALIGNED")||!"CLOSED".equals(r.optString("status")))return "Scene is not closed";JSONArray issues=r.optJSONArray("issues");if(issues==null)return "Missing scene issues";for(int i=0;i<issues.length();i++){JSONObject x=issues.optJSONObject(i);if(x==null||blank(x.optString("id"))||!one(x.optString("severity"),"CRITICAL","MAJOR","MINOR")||!one(x.optString("category"),"COVERAGE","FIDELITY","REFERENCE","VOICE","LOGIC","ACTION","STRUCTURE","CONTINUITY")||blank(x.optString("rawAnchor"))||blank(x.optString("draftAnchor"))||blank(x.optString("currentMeaning"))||blank(x.optString("rawEvidence"))||blank(x.optString("deviationImpact"))||blank(x.optString("l2Scope")))return "Incomplete scene issue";}return null;}catch(Exception e){return "Invalid scene JSON";}}
    private static boolean blank(String x){return x==null||x.trim().isEmpty();}private static boolean one(String x,String... a){for(String y:a)if(y.equals(x))return true;return false;}private EditorialL1SceneContract(){}
}
