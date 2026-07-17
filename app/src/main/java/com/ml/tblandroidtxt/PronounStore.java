package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Multi-profile pronoun library mirroring GlossaryStore's active-selection workflow. */
public final class PronounStore {
    private static final String PREF="pronoun_store", KEY_ITEMS="profiles_json", KEY_SELECTED="selected_profile_id", KEY_MIGRATED="legacy_migrated";
    private static volatile String cachedJson;
    private static volatile List<Profile> cachedProfiles;
    public static class Profile {
        public String id=UUID.randomUUID().toString(),name="New pronoun",uri="",text="";
        public long createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis();
        public int count(){return PromptContextBuilder.validate("",text).explicitPronouns.size();}
    }
    public static synchronized List<Profile> loadAll(Context c){ArrayList<Profile> out=new ArrayList<>();String raw=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY_ITEMS,"[]");if(cachedProfiles!=null&&TextUtils.equals(cachedJson,raw))return new ArrayList<>(cachedProfiles);try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++)out.add(fromJson(a.getJSONObject(i)));}catch(Exception ignored){}cachedJson=raw;cachedProfiles=new ArrayList<>(out);return out;}
    public static synchronized void saveAll(Context c,List<Profile> list){JSONArray a=new JSONArray();try{for(Profile p:list)a.put(toJson(p));}catch(Exception ignored){}String raw=a.toString();cachedJson=raw;cachedProfiles=new ArrayList<>(list);c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY_ITEMS,raw).commit();}
    public static Profile importProfile(Context c,String name,String uri,String text){Profile p=new Profile();p.name=safe(name).isEmpty()?"Pronoun":name.trim();p.uri=safe(uri);p.text=safe(text);ArrayList<Profile> all=new ArrayList<>(loadAll(c));all.add(0,p);saveAll(c,all);return p;}
    public static Profile importAndSelectProfile(Context c,String name,String uri,String text){Profile p=new Profile();p.name=safe(name).isEmpty()?"Pronoun":name.trim();p.uri=safe(uri);p.text=safe(text);return saveAndSelect(c,p)?p:null;}
    public static void upsert(Context c,Profile p){if(p==null)return;p.updatedAt=System.currentTimeMillis();ArrayList<Profile> all=new ArrayList<>(loadAll(c));boolean found=false;for(int i=0;i<all.size();i++)if(TextUtils.equals(all.get(i).id,p.id)){all.set(i,p);found=true;break;}if(!found)all.add(0,p);saveAll(c,all);}
    public static void delete(Context c,String id){ArrayList<Profile> all=new ArrayList<>(loadAll(c));for(int i=all.size()-1;i>=0;i--)if(TextUtils.equals(all.get(i).id,id))all.remove(i);saveAll(c,all);if(TextUtils.equals(getSelectedId(c),id))setSelectedId(c,"");}
    public static Profile find(Context c,String id){for(Profile p:loadAll(c))if(TextUtils.equals(p.id,id))return p;return null;}
    public static String getSelectedId(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY_SELECTED,"");}
    public static void setSelectedId(Context c,String id){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY_SELECTED,safe(id)).commit();}
    public static synchronized boolean saveAndSelect(Context c,Profile p){if(p==null)return false;p.updatedAt=System.currentTimeMillis();ArrayList<Profile> all=new ArrayList<>(loadAll(c));boolean found=false;for(int i=0;i<all.size();i++)if(TextUtils.equals(all.get(i).id,p.id)){all.set(i,p);found=true;break;}if(!found)all.add(0,p);JSONArray a=new JSONArray();try{for(Profile item:all)a.put(toJson(item));}catch(Exception e){return false;}String raw=a.toString();boolean saved=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY_ITEMS,raw).putString(KEY_SELECTED,p.id).commit();if(saved){cachedJson=raw;cachedProfiles=new ArrayList<>(all);}return saved;}
    public static Profile selected(Context c){return find(c,getSelectedId(c));}
    public static String selectedPromptText(Context c){Profile p=selected(c);return p==null?"":safe(p.text);}
    public static void migrateLegacy(Context c,AppSettings s){SharedPreferences pref=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);if(pref.getBoolean(KEY_MIGRATED,false))return;if(loadAll(c).isEmpty()&&s!=null&&!safe(s.pronounText).trim().isEmpty()){Profile p=importProfile(c,safe(s.pronounName).isEmpty()?"Imported pronoun":s.pronounName,s.pronounUri,s.pronounText);setSelectedId(c,p.id);}pref.edit().putBoolean(KEY_MIGRATED,true).apply();}
    private static JSONObject toJson(Profile p)throws Exception{JSONObject o=new JSONObject();o.put("id",p.id);o.put("name",p.name);o.put("uri",p.uri);o.put("text",p.text);o.put("createdAt",p.createdAt);o.put("updatedAt",p.updatedAt);return o;}
    private static Profile fromJson(JSONObject o){Profile p=new Profile();p.id=o.optString("id",p.id);p.name=o.optString("name",p.name);p.uri=o.optString("uri","");p.text=o.optString("text","");p.createdAt=o.optLong("createdAt",p.createdAt);p.updatedAt=o.optLong("updatedAt",p.updatedAt);return p;}
    private static String safe(String s){return s==null?"":s;}
    private PronounStore(){}
}
