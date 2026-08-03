package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure filename mapper for the editorial batch-import preview. No files are saved here. */
final class EditorialImportPlanner {
    static final class Source {
        final String name, uri, content;
        Source(String name, String uri, String content) { this.name=safe(name); this.uri=safe(uri); this.content=safe(content); }
    }

    static final class ChapterPlan {
        enum Status { READY, NEEDS_REVIEW }
        enum ReferenceOrigin { CHAPTER_OVERRIDE, INHERITED_PROJECT_DEFAULT }
        final String key;
        final String normalizedKey;
        final boolean bundle;
        final boolean requireReferences;
        Source raw, draft, glossary, pronoun;
        ReferenceOrigin glossaryOrigin, pronounOrigin;
        String problem = "";
        ChapterPlan(String key, String normalizedKey) { this(key, normalizedKey, false, false); }
        ChapterPlan(String key, String normalizedKey, boolean bundle) {
            this(key, normalizedKey, bundle, bundle);
        }
        ChapterPlan(String key, String normalizedKey, boolean bundle, boolean requireReferences) {
            this.key=key; this.normalizedKey=normalizedKey; this.bundle=bundle; this.requireReferences=requireReferences;
        }
        boolean ready() {
            return problem.isEmpty() && raw != null && draft != null
                    && (!requireReferences || glossary != null);
        }
        EditorialSafe4Workflow.PronounStatus pronounStatus() {
            return pronoun == null ? EditorialSafe4Workflow.PronounStatus.NONE
                    : EditorialSafe4Workflow.PronounStatus.AVAILABLE;
        }
        Status status() { return ready() ? Status.READY : Status.NEEDS_REVIEW; }
    }

    static final class Result {
        final List<ChapterPlan> chapters;
        final Source glossary, pronoun;
        final List<String> warnings;
        final List<Source> unassigned;
        Result(List<ChapterPlan> chapters, Source glossary, Source pronoun, List<String> warnings) {
            this(chapters, glossary, pronoun, warnings, Collections.emptyList());
        }
        Result(List<ChapterPlan> chapters, Source glossary, Source pronoun, List<String> warnings, List<Source> unassigned) {
            this.chapters=Collections.unmodifiableList(new ArrayList<>(chapters)); this.glossary=glossary; this.pronoun=pronoun;
            this.warnings=Collections.unmodifiableList(new ArrayList<>(warnings));
            this.unassigned=Collections.unmodifiableList(new ArrayList<>(unassigned));
        }
        int readyCount(){int count=0;for(ChapterPlan p:chapters)if(p.ready())count++;return count;}
        int blockedCount(){return chapters.size()-readyCount();}
    }

    static Result plan(List<Source> sources) {
        Map<String,ChapterPlan> plans=new LinkedHashMap<>(); ArrayList<String>warnings=new ArrayList<>(); Source glossary=null,pronoun=null;
        if(sources!=null) for(Source source:sources) {
            if(source==null) { warnings.add("File: empty selection"); continue; }
            Role role=roleOf(source.name);
            if(role!=Role.UNKNOWN && !supportedExtension(source.name,role)) { warnings.add(source.name+": unsupported format for "+role); continue; }
            if(role==Role.UNKNOWN && !isTextExtension(source.name)) { warnings.add(source.name+": unsupported or unknown role"); continue; }
            if(role==Role.GLOSSARY) { if(glossary!=null) warnings.add(source.name+": duplicate glossary ignored"); else glossary=source; continue; }
            if(role==Role.PRONOUN) { if(pronoun!=null) warnings.add(source.name+": duplicate pronoun ignored"); else pronoun=source; continue; }
            addSource(plans,warnings,source,role);
        }
        ArrayList<ChapterPlan> result=new ArrayList<>(plans.values());
        for(ChapterPlan plan:result) if(plan.problem.isEmpty() && (plan.raw==null || plan.draft==null)) plan.problem="missing "+(plan.raw==null?"RAW":"DRAFT");
        sortPlans(result); return new Result(result,glossary,pronoun,warnings);
    }

    /** Maps files whose roles were chosen explicitly by the user. */
    static Result plan(List<Source> rawSources, List<Source> draftSources) {
        Map<String,ChapterPlan> plans=new LinkedHashMap<>(); ArrayList<String>warnings=new ArrayList<>();
        addExplicit(plans,warnings,rawSources,Role.RAW);
        addExplicit(plans,warnings,draftSources,Role.DRAFT);
        ArrayList<ChapterPlan> result=new ArrayList<>(plans.values());
        for(ChapterPlan plan:result) if(plan.problem.isEmpty()&&(plan.raw==null||plan.draft==null)) plan.problem="missing "+(plan.raw==null?"RAW":"DRAFT");
        sortPlans(result);
        return new Result(result,null,null,warnings);
    }

    /** Plans the normal RAW/DRAFT flow while resolving references from the current project defaults. */
    static Result planWithProjectDefaults(List<Source> rawSources, List<Source> draftSources,
                                          Source glossaryDefault, Source pronounDefault) {
        Result base=plan(rawSources,draftSources);
        ArrayList<ChapterPlan> chapters=new ArrayList<>();
        for(ChapterPlan source:base.chapters) {
            ChapterPlan copy=copyOf(source,true);
            if(copy.glossary==null&&glossaryDefault!=null) {
                copy.glossary=glossaryDefault; copy.glossaryOrigin=ChapterPlan.ReferenceOrigin.INHERITED_PROJECT_DEFAULT;
            }
            if(copy.pronoun==null&&pronounDefault!=null) {
                copy.pronoun=pronounDefault; copy.pronounOrigin=ChapterPlan.ReferenceOrigin.INHERITED_PROJECT_DEFAULT;
            }
            recomputeMissing(copy);
            chapters.add(copy);
        }
        return new Result(chapters,glossaryDefault,pronounDefault,base.warnings,base.unassigned);
    }

    /** Applies explicit chapter reference candidates without changing project defaults. */
    static Result withChapterOverrides(Result base, Map<String,Source> glossaryOverrides,
                                       Map<String,Source> pronounOverrides) {
        if(base==null)return null;
        ArrayList<ChapterPlan> chapters=new ArrayList<>();
        for(ChapterPlan source:base.chapters) {
            ChapterPlan copy=copyOf(source,true);
            Source glossaryOverride=findOverride(copy.normalizedKey,glossaryOverrides);
            Source pronounOverride=findOverride(copy.normalizedKey,pronounOverrides);
            if(glossaryOverride!=null) {
                copy.glossary=glossaryOverride; copy.glossaryOrigin=ChapterPlan.ReferenceOrigin.CHAPTER_OVERRIDE;
            }
            if(pronounOverride!=null) {
                copy.pronoun=pronounOverride; copy.pronounOrigin=ChapterPlan.ReferenceOrigin.CHAPTER_OVERRIDE;
            }
            recomputeMissing(copy);
            chapters.add(copy);
        }
        return new Result(chapters,base.glossary,base.pronoun,base.warnings,base.unassigned);
    }

    /** Plans a SAFE4 chapter bundle: RAW, DRAFT and GLOSSARY are required; PRONOUN may be NONE. */
    static Result planBundle(List<Source> sources) {
        Map<String,ChapterPlan> plans=new LinkedHashMap<>();
        ArrayList<String>warnings=new ArrayList<>();
        ArrayList<Source> unassigned=new ArrayList<>();
        if(sources!=null) for(Source source:sources) {
            if(source==null) { warnings.add("File: empty selection"); continue; }
            Role role=roleOf(source.name);
            if(role==Role.UNKNOWN) {
                warnings.add(source.name+": role is unclear; choose a role before saving");
                unassigned.add(source);
                continue;
            }
            if(!supportedExtension(source.name,role)) {
                warnings.add(source.name+": unsupported format for "+role+"; choose another file");
                unassigned.add(source);
                continue;
            }
            String key=chapterKey(source.name);
            if(!isNumericChapterKey(key)) {
                warnings.add(source.name+": chapter number/key is unclear; choose a chapter before saving");
                unassigned.add(source);
                continue;
            }
            addBundleSource(plans,warnings,source,role,key);
        }
        ArrayList<ChapterPlan> result=new ArrayList<>(plans.values());
        for(ChapterPlan plan:result) {
            if(plan.problem.isEmpty()) {
                String missing=missingBundleRoles(plan);
                if(!missing.isEmpty()) plan.problem="missing "+missing;
            }
        }
        sortPlans(result);
        return new Result(result,null,null,warnings,unassigned);
    }

    private static void addExplicit(Map<String,ChapterPlan> plans,List<String>warnings,List<Source> sources,Role role) {
        if(sources==null)return;
        for(Source source:sources) {
            if(source==null){warnings.add("File: empty selection");continue;}
            addSource(plans,warnings,source,role);
        }
    }

    private static void addSource(Map<String,ChapterPlan> plans,List<String>warnings,Source source,Role role) {
        if (source == null) { warnings.add("File: empty selection"); return; }
        String key=chapterKey(source.name); String normalized=normalizeIdentity(key); ChapterPlan plan=plans.get(normalized);
        if(plan==null){plan=new ChapterPlan(key,normalized);plans.put(normalized,plan);}
        if(role==Role.RAW){if(plan.raw!=null)plan.problem="multiple RAW files";else plan.raw=source;}
        else if(role==Role.DRAFT){
            if(plan.draft!=null)plan.problem="multiple DRAFT files";
            else if(plan.raw!=null&&sameSource(plan.raw,source))plan.problem="RAW and DRAFT use the same file";
            else plan.draft=source;
        } else plan.problem="name must include RAW or DRAFT";
    }

    private static void addBundleSource(Map<String,ChapterPlan> plans,List<String>warnings,Source source,Role role,String key) {
        String normalized=normalizeIdentity(key); ChapterPlan plan=plans.get(normalized);
        if(plan==null){plan=new ChapterPlan(key,normalized,true);plans.put(normalized,plan);}
        if(sameSource(plan.raw,source)||sameSource(plan.draft,source)||sameSource(plan.glossary,source)||sameSource(plan.pronoun,source)) {
            addProblem(plan,"same file assigned to multiple roles");
            return;
        }
        switch(role) {
            case RAW:
                if(plan.raw!=null) addProblem(plan,"multiple RAW files"); else plan.raw=source;
                break;
            case DRAFT:
                if(plan.draft!=null) addProblem(plan,"multiple DRAFT files"); else plan.draft=source;
                break;
            case GLOSSARY:
                if(plan.glossary!=null) addProblem(plan,"multiple GLOSSARY files"); else { plan.glossary=source; plan.glossaryOrigin=ChapterPlan.ReferenceOrigin.CHAPTER_OVERRIDE; }
                break;
            case PRONOUN:
                if(plan.pronoun!=null) addProblem(plan,"multiple PRONOUN files"); else { plan.pronoun=source; plan.pronounOrigin=ChapterPlan.ReferenceOrigin.CHAPTER_OVERRIDE; }
                break;
            default:
                warnings.add(source.name+": role is unclear; choose a role before saving");
                break;
        }
    }

    private static String missingBundleRoles(ChapterPlan plan) {
        return missingRequiredRoles(plan);
    }

    private static String missingRequiredRoles(ChapterPlan plan) {
        StringBuilder missing=new StringBuilder();
        if(plan.raw==null) missing.append("RAW");
        if(plan.draft==null) appendMissing(missing,"DRAFT");
        if(plan.requireReferences&&plan.glossary==null) appendMissing(missing,"GLOSSARY");
        return missing.toString();
    }

    private static ChapterPlan copyOf(ChapterPlan source,boolean requireReferences) {
        ChapterPlan copy=new ChapterPlan(source.key,source.normalizedKey,source.bundle,requireReferences);
        copy.raw=source.raw; copy.draft=source.draft; copy.glossary=source.glossary; copy.pronoun=source.pronoun;
        copy.glossaryOrigin=source.glossaryOrigin; copy.pronounOrigin=source.pronounOrigin; copy.problem=source.problem;
        return copy;
    }

    private static Source findOverride(String normalizedKey,Map<String,Source> overrides) {
        if(overrides==null||overrides.isEmpty())return null;
        for(Map.Entry<String,Source> entry:overrides.entrySet()) {
            if(normalizeIdentity(entry.getKey()).equals(normalizedKey))return entry.getValue();
        }
        return null;
    }

    private static void recomputeMissing(ChapterPlan plan) {
        if(plan.problem.startsWith("missing ")) plan.problem="";
        if(plan.problem.isEmpty()) {
            String missing=missingRequiredRoles(plan);
            if(!missing.isEmpty())plan.problem="missing "+missing;
        }
    }

    private static void appendMissing(StringBuilder missing,String role) {
        if(missing.length()>0) missing.append(", ");
        missing.append(role);
    }

    private static void addProblem(ChapterPlan plan,String problem) {
        if(plan.problem.isEmpty()) plan.problem=problem;
        else if(!plan.problem.contains(problem)) plan.problem += "; "+problem;
    }

    private static void sortPlans(List<ChapterPlan> plans) {
        Collections.sort(plans,(left,right)->{
            int numberLeft=chapterNumber(left.normalizedKey),numberRight=chapterNumber(right.normalizedKey);
            if(numberLeft!=numberRight)return Integer.compare(numberLeft,numberRight);
            return left.key.compareToIgnoreCase(right.key);
        });
    }

    private static int chapterNumber(String identity) {
        if(identity!=null&&identity.startsWith("number:"))try{return Integer.parseInt(identity.substring("number:".length()));}catch(NumberFormatException ignored){}
        return Integer.MAX_VALUE;
    }

    private static boolean sameSource(Source left,Source right) {
        if(left==null||right==null)return false;
        if(!left.uri.isEmpty()&&!right.uri.isEmpty()&&left.uri.equals(right.uri))return true;
        return left.uri.isEmpty()&&right.uri.isEmpty()&&left.name.equalsIgnoreCase(right.name);
    }

    private enum Role { RAW,DRAFT,GLOSSARY,PRONOUN,UNKNOWN }
    private static Role roleOf(String filename) {
        String n=filename.toLowerCase(Locale.ROOT);
        if(has(n,"glossary")||has(n,"terms")||has(n,"term"))return Role.GLOSSARY;
        if(has(n,"pronoun")||has(n,"xungho"))return Role.PRONOUN;
        if(has(n,"draft")||has(n,"vi_l2")||has(n,"vietnamese")||has(n,"translated")||has(n,"translation"))return Role.DRAFT;
        if(has(n,"raw")||has(n,"source")||has(n,"original")||has(n,"jp"))return Role.RAW;
        return Role.UNKNOWN;
    }

    private static String chapterKey(String filename) {
        String base=filename.replaceFirst("(?i)\\.[^.]+$","");
        Matcher chapter=Pattern.compile("(?i)^(?:chapter|chap|ch|chuong)?[ _.-]*(\\d{1,6})(?=[ _.-]|$)").matcher(base);
        if(chapter.find())return chapter.group(1);
        chapter=Pattern.compile("(?i)(?:^|[^a-z0-9])(?:chapter|chap|ch|chuong)[ _.-]*(\\d{1,6})(?=[^0-9]|$)").matcher(base);
        if(chapter.find())return chapter.group(1);
        base=base.replaceAll("(?i)(^|[_ .-])(raw|source|original|draft|vi_l2)(?=[_ .-]|$)","$1")
                .replaceAll("(?i)\\s*\\((vietnamese|translated|translation|draft|raw)\\)\\s*$","")
                .replaceAll("[_ .-]+","_").replaceAll("^[_ .-]+|[_ .-]+$","").trim();
        return base.isEmpty()?filename:base;
    }

    static String chapterKeyOf(String filename) { return chapterKey(filename); }
    static boolean hasChapterNumber(String filename) { return isNumericChapterKey(chapterKey(filename)); }

    private static boolean isNumericChapterKey(String key) {
        return key!=null&&key.matches("\\d{1,6}");
    }

    private static boolean supportedExtension(String filename,Role role) {
        String extension=extension(filename);
        if(role==Role.GLOSSARY||role==Role.PRONOUN) return extension.matches("csv|tsv|txt|md|json|yaml|yml");
        return extension.matches("txt|md|doc|rtf");
    }

    private static boolean isTextExtension(String filename) {
        String extension=extension(filename);
        return extension.matches("txt|md|csv|tsv|json|yaml|yml|doc|rtf");
    }

    private static String extension(String filename) {
        String value=filename==null?"":filename.toLowerCase(Locale.ROOT);
        int dot=value.lastIndexOf('.');
        return dot<0?"":value.substring(dot+1);
    }

    private static String normalizeIdentity(String key) {
        Matcher number=Pattern.compile("^(\\d{1,6})$").matcher(key==null?"":key.trim());
        if(number.matches())try{return "number:"+Integer.parseInt(number.group(1));}catch(NumberFormatException ignored){}
        String name=(key==null?"":key).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");
        return "name:"+name;
    }

    private static boolean has(String value,String token){return Pattern.compile("(^|[^a-z0-9])"+Pattern.quote(token)+"([^a-z0-9]|$)").matcher(value).find();}
    private static String safe(String value){return value==null?"":value.trim();}
    private static String name(Source source){return source==null?"File":safe(source.name);}
    private EditorialImportPlanner() {}
}
