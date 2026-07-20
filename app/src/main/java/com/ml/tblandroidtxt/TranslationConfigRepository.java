package com.ml.tblandroidtxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Single durable source for small imported translation configuration files.
 * SAF URIs are used only while importing; runtime reads use the private copy.
 */
public final class TranslationConfigRepository {
    public enum Type { INSTRUCTION_YAML, GLOSSARY, PRONOUN, ENVIRONMENT }
    private static final String PREF = "translation_config_repository";
    private static final String DIR = "translation-config";

    public static final class Entry {
        public final Type type;
        public final String path, name, checksum, sourceUri, parseStatus, parseDetail;
        public final long importedAt;
        Entry(Type type, String path, String name, String checksum, String sourceUri,
              long importedAt, String parseStatus, String parseDetail) {
            this.type=type; this.path=path; this.name=name; this.checksum=checksum;
            this.sourceUri=sourceUri; this.importedAt=importedAt;
            this.parseStatus=parseStatus; this.parseDetail=parseDetail;
        }
        public boolean valid() { return "VALID".equals(parseStatus) && path != null && new File(path).isFile(); }
    }

    private final Context context;
    private final SharedPreferences prefs;

    private TranslationConfigRepository(Context context) {
        this.context=context.getApplicationContext();
        this.prefs=this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static TranslationConfigRepository get(Context context) { return new TranslationConfigRepository(context); }

    public Entry importFromUri(Type type, Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException(type+": picker returned an empty URI");
        String name=FileUtil.displayName(context,uri);
        String text=FileUtil.readText(context,uri);
        String detail=validate(type,name,text);
        long now=System.currentTimeMillis();
        File directory=new File(context.getFilesDir(),DIR);
        if(!directory.exists()&&!directory.mkdirs()) throw new IllegalStateException("Cannot create private config storage");
        String suffix=type.name().toLowerCase()+"-"+now+".txt";
        File target=new File(directory,suffix), temp=new File(directory,suffix+".tmp");
        try(FileOutputStream out=new FileOutputStream(temp)){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
        if(!temp.renameTo(target)) { temp.delete(); throw new IllegalStateException("Cannot commit private config copy"); }
        String hash=HashUtil.sha256(text);
        prefs.edit().putString(key(type,"path"),target.getAbsolutePath()).putString(key(type,"name"),name)
                .putString(key(type,"checksum"),hash).putString(key(type,"sourceUri"),uri.toString())
                .putLong(key(type,"importedAt"),now).putString(key(type,"parseStatus"),"VALID")
                .putString(key(type,"parseDetail"),detail).apply();
        LogStore.append(context,"CONFIG_IMPORT type="+type+" uri="+uri+" persistedRead="+FileUtil.hasPersistedRead(context,uri)+" internal="+target.getAbsolutePath()+" checksum="+hash+" parse="+detail);
        return entry(type);
    }

    public Entry entry(Type type) {
        return new Entry(type,prefs.getString(key(type,"path"),""),prefs.getString(key(type,"name"),""),
                prefs.getString(key(type,"checksum"),""),prefs.getString(key(type,"sourceUri"),""),
                prefs.getLong(key(type,"importedAt"),0),prefs.getString(key(type,"parseStatus"),"NOT_IMPORTED"),
                prefs.getString(key(type,"parseDetail"),"not imported"));
    }

    public String read(Type type) throws Exception {
        Entry e=entry(type);
        if(!e.valid()) throw new IllegalStateException(type+" internal copy is missing or invalid: "+e.parseDetail);
        String text=new String(java.nio.file.Files.readAllBytes(new File(e.path).toPath()), StandardCharsets.UTF_8);
        if(!HashUtil.sha256(text).equals(e.checksum)) throw new IllegalStateException(type+" internal checksum mismatch");
        return text;
    }

    public String validateRuntime(Type type) {
        try { String text=read(type); return validate(type,entry(type).name,text); }
        catch(Exception e) { return type+" internal copy: "+e.getMessage(); }
    }

    public void clear(Type type) { prefs.edit().remove(key(type,"path")).remove(key(type,"name")).remove(key(type,"checksum")).remove(key(type,"sourceUri")).remove(key(type,"importedAt")).remove(key(type,"parseStatus")).remove(key(type,"parseDetail")).apply(); }

    private static String key(Type t,String name){return t.name()+"."+name;}
    private static String validate(Type type,String name,String text) {
        if(text==null||text.trim().isEmpty()) throw new IllegalArgumentException(type+" is empty");
        if(type==Type.INSTRUCTION_YAML) { CustomInstructions c=YamlInstructionParser.parse(name,text); if(!c.hasTranslation()&&!c.hasRefinement()) throw new IllegalArgumentException("YAML has no translation or refinement block"); return "translation="+c.hasTranslation()+", refinement="+c.hasRefinement(); }
        if(type==Type.GLOSSARY) { int count=GlossaryStore.parseTerms(name,text).size(); if(count==0) throw new IllegalArgumentException("No valid glossary terms"); return "entries="+count; }
        if(type==Type.PRONOUN) { PromptContextBuilder.ParseReport r=PromptContextBuilder.validate("",text); if(!r.malformedPronounRows.isEmpty()) throw new IllegalArgumentException("Malformed pronoun rows="+r.malformedPronounRows.size()); return "rules="+r.explicitPronouns.size(); }
        EnvParser.parse(text); return "environment parsed";
    }
}
