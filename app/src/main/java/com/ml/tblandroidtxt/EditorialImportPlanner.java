package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure filename mapper for the editorial batch-import preview. No files are saved here. */
final class EditorialImportPlanner {
    static final class Source {
        final String name, uri, content;
        Source(String name, String uri, String content) { this.name=safe(name); this.uri=safe(uri); this.content=safe(content); }
    }
    static final class ChapterPlan {
        final String key;
        Source raw, draft;
        String problem = "";
        ChapterPlan(String key) { this.key=key; }
        boolean ready() { return problem.isEmpty() && raw != null && draft != null; }
    }
    static final class Result {
        final List<ChapterPlan> chapters;
        final Source glossary, pronoun;
        final List<String> warnings;
        Result(List<ChapterPlan> chapters, Source glossary, Source pronoun, List<String> warnings) {
            this.chapters=Collections.unmodifiableList(new ArrayList<>(chapters)); this.glossary=glossary; this.pronoun=pronoun;
            this.warnings=Collections.unmodifiableList(new ArrayList<>(warnings));
        }
        int readyCount(){int count=0;for(ChapterPlan p:chapters)if(p.ready())count++;return count;}
    }

    static Result plan(List<Source> sources) {
        Map<String,ChapterPlan> plans=new LinkedHashMap<>(); ArrayList<String>warnings=new ArrayList<>(); Source glossary=null,pronoun=null;
        if(sources!=null) for(Source source:sources) {
            if(source==null || !source.name.toLowerCase(Locale.ROOT).endsWith(".txt")) { warnings.add(name(source)+": only .txt is supported"); continue; }
            Role role=roleOf(source.name);
            if(role==Role.GLOSSARY) { if(glossary!=null) warnings.add(source.name+": duplicate glossary ignored"); else glossary=source; continue; }
            if(role==Role.PRONOUN) { if(pronoun!=null) warnings.add(source.name+": duplicate pronoun ignored"); else pronoun=source; continue; }
            String key=chapterKey(source.name,role); ChapterPlan plan=plans.get(key); if(plan==null){plan=new ChapterPlan(key);plans.put(key,plan);}
            if(role==Role.RAW) { if(plan.raw!=null) plan.problem="multiple RAW files"; else plan.raw=source; }
            else if(role==Role.DRAFT) { if(plan.draft!=null) plan.problem="multiple DRAFT files"; else plan.draft=source; }
            else { plan.problem="name must include RAW or DRAFT"; }
        }
        ArrayList<ChapterPlan> result=new ArrayList<>(plans.values());
        for(ChapterPlan plan:result) if(plan.problem.isEmpty() && (plan.raw==null || plan.draft==null)) plan.problem="missing "+(plan.raw==null?"RAW":"DRAFT");
        Collections.sort(result, Comparator.comparing(p->p.key)); return new Result(result,glossary,pronoun,warnings);
    }

    /** Maps files whose roles were chosen explicitly by the user. */
    static Result plan(List<Source> rawSources, List<Source> draftSources) {
        Map<String,ChapterPlan> plans=new LinkedHashMap<>(); ArrayList<String>warnings=new ArrayList<>();
        addExplicit(plans,warnings,rawSources,Role.RAW);
        addExplicit(plans,warnings,draftSources,Role.DRAFT);
        ArrayList<ChapterPlan> result=new ArrayList<>(plans.values());
        for(ChapterPlan plan:result) if(plan.problem.isEmpty()&&(plan.raw==null||plan.draft==null)) plan.problem="missing "+(plan.raw==null?"RAW":"DRAFT");
        Collections.sort(result,Comparator.comparing(p->p.key));
        return new Result(result,null,null,warnings);
    }

    private static void addExplicit(Map<String,ChapterPlan> plans,List<String>warnings,List<Source> sources,Role role) {
        if(sources==null)return;
        for(Source source:sources) {
            if(source==null){warnings.add("File: empty selection");continue;}
            String key=chapterKey(source.name,role); ChapterPlan plan=plans.get(key);
            if(plan==null){plan=new ChapterPlan(key);plans.put(key,plan);}
            if(role==Role.RAW){if(plan.raw!=null)plan.problem="multiple RAW files";else plan.raw=source;}
            else if(role==Role.DRAFT){if(plan.draft!=null)plan.problem="multiple DRAFT files";else plan.draft=source;}
        }
    }

    private enum Role { RAW,DRAFT,GLOSSARY,PRONOUN,UNKNOWN }
    private static Role roleOf(String filename) { String n=filename.toLowerCase(Locale.ROOT); if(has(n,"glossary"))return Role.GLOSSARY; if(has(n,"pronoun"))return Role.PRONOUN; if(has(n,"draft")||has(n,"vi_l2"))return Role.DRAFT; if(has(n,"raw")||has(n,"source")||has(n,"original"))return Role.RAW; return Role.UNKNOWN; }
    private static String chapterKey(String filename,Role role) { String base=filename.replaceFirst("(?i)\\.[^.]+$",""); base=base.replaceAll("(?i)(^|[_ .-])(raw|source|original|draft|vi_l2)(?=[_ .-]|$)","$1").replaceAll("[_ .-]{2,}","_").replaceAll("^[_ .-]+|[_ .-]+$","").trim(); return base.isEmpty()?filename:base; }
    private static boolean has(String value,String token){return value.matches(".*(^|[_ .-])"+token+"([_ .-]|$).*");}
    private static String safe(String value){return value==null?"":value.trim();}
    private static String name(Source source){return source==null?"File":safe(source.name);}
    private EditorialImportPlanner() {}
}
