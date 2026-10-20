package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Multi-profile pronoun library mirroring GlossaryStore's active-selection workflow. */
public final class PronounStore {
    private static final String PREF="pronoun_store", KEY_ITEMS="profiles_json", KEY_SELECTED="selected_profile_id", KEY_MIGRATED="legacy_migrated";
    private static volatile String cachedJson;
    private static volatile List<Profile> cachedProfiles;
    private static volatile String corruptJson;
    public static class Profile {
        public String id=UUID.randomUUID().toString(),name="New pronoun",uri="",text="";
        public long createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis();
        public int count(){return RuleCounts.of(text);}
    }
    /** The rule count of a Pronoun text, analysed once per text (a Library page asks for it for every card, twice). */
    static final class RuleCounts {
        private static final java.util.LinkedHashMap<String,Integer> CACHE=new java.util.LinkedHashMap<String,Integer>(64,0.75f,true){@Override protected boolean removeEldestEntry(java.util.Map.Entry<String,Integer> e){return size()>128;}};
        static int calls;
        static synchronized int of(String text){
            String t=text==null?"":text;
            String key=t.length()+":"+t.hashCode();
            Integer known=CACHE.get(key);
            if(known!=null)return known;
            calls++;
            int n=PromptContextBuilder.validate("",t).explicitPronouns.size();
            CACHE.put(key,n);
            return n;
        }
    }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public static List<Profile> loadAll(Context c){return loadAll(prefs(c));}
    /** An unreadable store is preserved under a new key and locked for writes; it never reads as an empty library. */
    static synchronized List<Profile> loadAll(SharedPreferences pref){String raw=pref.getString(KEY_ITEMS,"[]");if(cachedProfiles!=null&&Objects.equals(cachedJson,raw))return new ArrayList<>(cachedProfiles);List<Profile> parsed=parse(raw);if(parsed==null){LibraryPrefs.preserveCorrupt(pref,KEY_ITEMS,raw);corruptJson=raw;cachedJson=null;cachedProfiles=null;return new ArrayList<>();}corruptJson=null;cachedJson=raw;cachedProfiles=new ArrayList<>(parsed);return new ArrayList<>(parsed);}
    /** The stored list, or null when any part of it cannot be read. */
    static List<Profile> parse(String raw){ArrayList<Profile> out=new ArrayList<>();try{JSONArray a=new JSONArray(raw==null?"[]":raw);for(int i=0;i<a.length();i++)out.add(fromJson(a.getJSONObject(i)));return out;}catch(Exception e){return null;}}
    static String serialize(List<Profile> list)throws Exception{JSONArray a=new JSONArray();for(Profile p:list)a.put(toJson(p));return a.toString();}
    public static boolean isCorrupt(Context c){loadAll(c);return corruptJson!=null;}
    /** The unreadable stored text while the store is locked, else null. */
    public static String corruptText(Context c){loadAll(c);return corruptJson;}
    public static boolean saveAll(Context c,List<Profile> list){return saveAll(prefs(c),list);}
    /** Writes synchronously; refuses while the store is unreadable or when any profile cannot be serialized. */
    static synchronized boolean saveAll(SharedPreferences pref,List<Profile> list){loadAll(pref);if(corruptJson!=null||list==null)return false;String raw;try{raw=serialize(list);}catch(Exception e){return false;}if(!pref.edit().putString(KEY_ITEMS,raw).commit())return false;cachedJson=raw;cachedProfiles=new ArrayList<>(list);return true;}
    /** The new profile, or null when the store refused the write. */
    public static Profile importProfile(Context c,String name,String uri,String text){Profile p=new Profile();p.name=safe(name).isEmpty()?"Pronoun":name.trim();p.uri=safe(uri);p.text=safe(text);ArrayList<Profile> all=new ArrayList<>(loadAll(c));all.add(0,p);return saveAll(c,all)?p:null;}
    public static Profile importAndSelectProfile(Context c,String name,String uri,String text){Profile p=new Profile();p.name=safe(name).isEmpty()?"Pronoun":name.trim();p.uri=safe(uri);p.text=safe(text);return saveAndSelect(c,p)?p:null;}
    public static boolean upsert(Context c,Profile p){if(p==null)return false;p.updatedAt=System.currentTimeMillis();ArrayList<Profile> all=new ArrayList<>(loadAll(c));boolean found=false;for(int i=0;i<all.size();i++)if(TextUtils.equals(all.get(i).id,p.id)){all.set(i,p);found=true;break;}if(!found)all.add(0,p);return saveAll(c,all);}
    public static boolean delete(Context c,String id){ArrayList<Profile> all=new ArrayList<>(loadAll(c));for(int i=all.size()-1;i>=0;i--)if(TextUtils.equals(all.get(i).id,id))all.remove(i);if(!saveAll(c,all))return false;if(TextUtils.equals(getSelectedId(c),id))setSelectedId(c,"");return true;}
    public static Profile find(Context c,String id){for(Profile p:loadAll(c))if(TextUtils.equals(p.id,id))return p;return null;}
    public static String getSelectedId(Context c){return prefs(c).getString(KEY_SELECTED,"");}
    public static boolean setSelectedId(Context c,String id){return prefs(c).edit().putString(KEY_SELECTED,safe(id)).commit();}
    public static synchronized boolean saveAndSelect(Context c,Profile p){if(p==null)return false;SharedPreferences pref=prefs(c);ArrayList<Profile> all=new ArrayList<>(loadAll(pref));if(corruptJson!=null)return false;p.updatedAt=System.currentTimeMillis();boolean found=false;for(int i=0;i<all.size();i++)if(TextUtils.equals(all.get(i).id,p.id)){all.set(i,p);found=true;break;}if(!found)all.add(0,p);String raw;try{raw=serialize(all);}catch(Exception e){return false;}boolean saved=pref.edit().putString(KEY_ITEMS,raw).putString(KEY_SELECTED,p.id).commit();if(saved){cachedJson=raw;cachedProfiles=new ArrayList<>(all);}return saved;}
    public static Profile selected(Context c){return find(c,getSelectedId(c));}
    public static String selectedPromptText(Context c){Profile p=selected(c);return p==null?"":safe(p.text);}
    public static void migrateLegacy(Context c,AppSettings s){SharedPreferences pref=prefs(c);if(pref.getBoolean(KEY_MIGRATED,false))return;List<Profile> current=loadAll(c);if(corruptJson!=null)return;if(current.isEmpty()&&s!=null&&!safe(s.pronounText).trim().isEmpty()){Profile p=importProfile(c,safe(s.pronounName).isEmpty()?"Imported pronoun":s.pronounName,s.pronounUri,s.pronounText);if(p==null)return;setSelectedId(c,p.id);}pref.edit().putBoolean(KEY_MIGRATED,true).apply();}
    static JSONObject toJson(Profile p)throws Exception{JSONObject o=new JSONObject();o.put("id",p.id);o.put("name",p.name);o.put("uri",p.uri);o.put("text",p.text);o.put("createdAt",p.createdAt);o.put("updatedAt",p.updatedAt);return o;}
    static Profile fromJson(JSONObject o){Profile p=new Profile();p.id=o.optString("id",p.id);p.name=o.optString("name",p.name);p.uri=o.optString("uri","");p.text=o.optString("text","");p.createdAt=o.optLong("createdAt",p.createdAt);p.updatedAt=o.optLong("updatedAt",p.updatedAt);return p;}
    private static String safe(String s){return s==null?"":s;}
    private PronounStore(){}
}
