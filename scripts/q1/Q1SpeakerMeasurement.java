package q1;

import com.ml.tblandroidtxt.editorial.api.*;
import com.ml.tblandroidtxt.editorial.api.chunk.*;
import com.ml.tblandroidtxt.editorial.api.fix.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Reads only explicit private inputs. No FINAL, network, provider, app write, or text edit. */
public final class Q1SpeakerMeasurement {
    private record Ref(Q1SpeakerSidecar.Label label,String rationale) {}
    private static String read(Path p)throws Exception{return Files.readString(p,StandardCharsets.UTF_8).replace("\uFEFF","");}
    private static void write(Path p,String s)throws Exception{Files.writeString(p,s,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
    private static String safe(String s){return s.replace("\t"," ").replace("\n"," ").replace("\r","");}
    private static String html(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static List<String> csv(String line){
        List<String> out=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quote=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quote&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quote=!quote;}
            else if(c==','&&!quote){out.add(value.toString());value.setLength(0);}else value.append(c);}
        if(quote)throw new IllegalArgumentException("Q1_CSV_QUOTE");out.add(value.toString());return out;
    }
    private static List<EditInputs.GlossaryEntry> glossary(String text){
        List<EditInputs.GlossaryEntry> out=new ArrayList<>();for(String line:text.split("\n")){if(line.isBlank()||line.startsWith("source,"))continue;var c=csv(line);if(c.size()<2)continue;
            out.add(new EditInputs.GlossaryEntry(c.get(0).trim(),c.get(1).trim(),c.size()>2?c.get(2):"",c.size()>3?c.get(3):""));}return out;
    }
    private static Map<Integer,Ref> references(Path spec,LineUnits raw)throws Exception{
        Map<Integer,Ref> out=new TreeMap<>();for(var u:raw.units())if(u.text().contains("「")||u.text().contains("『"))
            out.put(u.index()+1,new Ref(new Q1SpeakerSidecar.Label("?","?"),"session inference: unresolved quotation/embedded quotation"));
        Set<Integer> used=new HashSet<>();for(String line:read(spec).split("\n")){if(line.isBlank()||line.startsWith("#"))continue;String[] c=line.split("\t",-1);
            if(c.length!=4)throw new IllegalArgumentException("Q1_REFERENCE_COLUMNS");for(String n:c[0].split(",")){int id=Integer.parseInt(n);
                if(!out.containsKey(id)||!used.add(id))throw new IllegalArgumentException("Q1_REFERENCE_ID "+id);
                out.put(id,new Ref(new Q1SpeakerSidecar.Label(c[1],c[2]),c[3]));}}
        return out;
    }
    private static int[] paragraphs(String text,LineUnits units){
        int[] out=new int[units.size()];int paragraph=0,index=0;boolean gap=true;
        for(String line:PairText.normalize(text).split("\n",-1)){if(PairText.isBlank(line)){gap=true;continue;}if(gap){paragraph++;gap=false;}out[index++]=paragraph;}return out;
    }
    // Exact arithmetic of app Chunker.approxTokens; defaults token450/balanced => hard675/soft540.
    private static int tokens(String text){double score=0;boolean word=false;for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);
        if((cp>=0x4E00&&cp<=0x9FFF)||(cp>=0x3040&&cp<=0x30FF)||(cp>=0xFF66&&cp<=0xFF9D)){score+=1;word=false;}
        else if(Character.isWhitespace(cp))word=false;else if(cp<128&&Character.isLetterOrDigit(cp)){score+=word?.25:1;word=true;}else{score+=.5;word=false;}}
        return Math.max(1,(int)Math.ceil(score));}
    private static Map<Integer,String> adjudications(Path p)throws Exception{
        Map<Integer,String> out=new TreeMap<>();for(String line:read(p).split("\n")){if(line.isBlank()||line.startsWith("#"))continue;var c=line.split("\t",-1);if(c.length!=3)throw new IllegalArgumentException("Q1_ADJUDICATION_COLUMNS");
            for(String id:c[0].split(","))if(out.put(Integer.parseInt(id),c[1])!=null)throw new IllegalArgumentException("Q1_ADJUDICATION_DUPLICATE");}return out;
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),event=Path.of(args[1]);
        measure("Kumo013","CH013",Path.of("D:/P5E-private/v422-q0/inputs"),"013_RAW_7.txt","013_RAW_7_translated.txt","013_GLOSSARY_KUMO_DESU_GA_NANI_KA_7.csv","CH013_PRONOUN_RC.csv",root,event);
        measure("JAKUAKU014","CH014",Path.of("D:/P5E-private/q2-inputs/014"),"014_RAW_JAKUAKU_MONSTER_VOL1.txt","014_JAKUAKU_MONSTER_VOL1_DRAFT.txt","014_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv","014_PRONOUN_JAKUAKU_MONSTER_VOL1.csv",root,event);
    }
    private static void measure(String name,String chapter,Path inputs,String rawName,String draftName,String glName,String prName,Path root,Path event)throws Exception{
        String raw=read(inputs.resolve(rawName)),draft=read(inputs.resolve(draftName)),g=read(inputs.resolve(glName)),pr=read(inputs.resolve(prName));var gl=glossary(g);
        // Baseline app defaults, not a recreation of unknown historical owner settings. Real engine finder per chunk.
        var planned=ChunkPlanner.plan(raw,draft,gl,g,pr,new ChunkPlanner.Settings("token",540,675,4096,400,1.3,Q1SpeakerMeasurement::tokens,Q1SpeakerMeasurement::tokens,ChapterVerdict.DEFAULT_SERIES_RATIO));
        var map=PairMaps.fromJobRows(chapter,"raw","draft",planned.rawRows(),planned.draftRows());
        if(!map.verify().isEmpty())throw new IllegalArgumentException("Q1_MAP_INVALID");
        var ru=planned.raw();var du=planned.draft();var refs=references(root.resolve("reference-"+name+".tsv"),ru);var paras=paragraphs(raw,ru);
        var projection=ReferenceProjector.project(new ReferenceProjector.Params(chapter,1,Arrays.stream(paras).max().orElse(1),raw,"","",Set.of("none")),gl,pr);
        var roster=Q1SpeakerSidecar.roster(projection.pronouns());
        for(var ref:refs.values())for(String entity:List.of(ref.label().speaker(),ref.label().listener()))
            if(!entity.equals("?")&&!entity.equals("@GROUP")&&!entity.equals("@NARRATOR")&&!roster.containsValue(entity))throw new IllegalArgumentException("Q1_REFERENCE_ENTITY");
        StringBuilder refOut=new StringBuilder("raw_unit1\tphysical_line1\tparagraph1\tspeaker\tlistener\torigin\trationale\n");
        for(var e:refs.entrySet()){var u=ru.units().get(e.getKey()-1);var r=e.getValue();refOut.append(e.getKey()).append('\t').append(u.physicalLine()).append('\t').append(paras[u.index()]).append('\t').append(r.label().speaker()).append('\t').append(r.label().listener()).append("\tSESSION_INFERENCE_NOT_GOLD\t").append(safe(r.rationale())).append('\n');}
        write(event.resolve("labels-"+name+".tsv"),refOut.toString());
        Map<Integer,Set<String>> baseline=new TreeMap<>();int baselinePoints=0;
        for(int ci=0;ci<map.entries().size();ci++){
            var found=FixPointFinder.find(map,map.entries().get(ci),gl,pr,null,planned.plan());baselinePoints+=found.points().size();int offset=planned.plan().chunks.get(ci).draftFrom();
            for(var p:found.points())if(!p.insertion())for(var type:p.types())baseline.computeIfAbsent(offset+p.draftLineIndex()+1,k->new TreeSet<>()).add(type.name());
        }
        Map<Integer,Q1SpeakerSidecar.Check> checks=new TreeMap<>();Map<Integer,Integer> rawIds=new TreeMap<>();
        Map<String,Integer> statuses=new TreeMap<>();
        for(var b:ChunkPlan.decodeBeads(planned.plan().beads)){
            if(b.rawCount()!=1||b.draftCount()!=1){for(int d=b.draftStart();d<b.draftEnd();d++){checks.put(d+1,new Q1SpeakerSidecar.Check("ALIGNMENT_UNRESOLVED","",List.of()));}continue;}
            int ri=b.rawStart(),di=b.draftStart();Ref ref=refs.get(ri+1);if(ref==null)continue;rawIds.put(di+1,ri+1);
            checks.put(di+1,Q1SpeakerSidecar.compare(du.units().get(di).text(),paras[ri],ref.label(),projection.pronouns()));
        }
        Set<Integer> union=new TreeSet<>(baseline.keySet());union.addAll(checks.keySet());
        StringBuilder detail=new StringBuilder("draft_unit1\tdraft_physical1\traw_unit1\tparagraph1\tspeaker\tlistener\tbaseline_types\tq1_status\tunexpected\trow_key\n");
        StringBuilder page=new StringBuilder("<!doctype html><meta charset='utf-8'><title>Q1 "+name+"</title><style>body{font:16px sans-serif}td{vertical-align:top;padding:8px;border:1px solid #bbb}table{border-collapse:collapse}</style><h1>Q1 "+name+"</h1><p>SESSION INFERENCE, not owner/model gold. Candidates are not automatic corrections. Full source context is in the private input files. No FINAL loaded.</p><table><tr><th>IDs / speaker → listener</th><th>RAW and neighbours</th><th>DRAFT</th><th>Existing finder / Q1</th></tr>");
        int candidates=0,unknown=0;
        for(int d:union){var c=checks.get(d);if(c!=null){statuses.merge(c.status(),1,Integer::sum);if(c.candidate())candidates++;}int r=rawIds.getOrDefault(d,0);var ref=refs.get(r);
            if(ref!=null&&(ref.label().speaker().equals("?")||ref.label().listener().equals("?")))unknown++;
            String speaker=ref==null?"?":ref.label().speaker(),listener=ref==null?"?":ref.label().listener();String types=String.join(",",baseline.getOrDefault(d,Set.of()));
            detail.append(d).append('\t').append(du.units().get(d-1).physicalLine()).append('\t').append(r).append('\t').append(r==0?0:paras[r-1]).append('\t').append(speaker).append('\t').append(listener).append('\t').append(types).append('\t').append(c==null?"NOT_DIALOGUE":c.status()).append('\t').append(c==null?"":String.join(",",c.unexpected())).append('\t').append(c==null?"":safe(c.row())).append('\n');
            if(!types.contains("ADDRESS")&&(c==null||!c.candidate()))continue;
            String context=r==0?"unresolved alignment":ru.analysis(Math.max(0,r-2),Math.min(ru.size(),r+1));
            page.append("<tr><td>D").append(d).append(" / RAW").append(r).append("<br>").append(html(speaker+" → "+listener)).append("</td><td>").append(html(context)).append("</td><td>").append(html(du.units().get(d-1).text())).append("</td><td>").append(html(types+" / "+(c==null?"NOT_DIALOGUE":c.status()+" "+c.unexpected()))).append("</td></tr>");
        }
        page.append("</table>");write(event.resolve("comparison-"+name+".tsv"),detail.toString());write(event.resolve("READING-"+name+".html"),page.toString());
        int address=(int)baseline.values().stream().filter(t->t.contains("ADDRESS")).count();
        String summary=name+" rawUnits="+ru.size()+" draftUnits="+du.size()+" quotedRawLines="+refs.size()+" labelsUnknown="+refs.values().stream().filter(x->x.label().speaker().equals("?")||x.label().listener().equals("?")).count()+" chunks="+planned.plan().chunks.size()+" baselinePoints="+baselinePoints+" baselineADDRESS="+address+" q1Candidates="+candidates+" statuses="+statuses;
        StringBuilder known=new StringBuilder("draft_unit1\toccurrences\tbaseline_ADDRESS\tq1_status\n");
        if(name.equals("JAKUAKU014")){
            String facts=read(Path.of("D:/P5E-private/product-plan-A/stepB-self-cases.json"));Matcher m=Pattern.compile("\\[\"014\",\\s*(\\d+),").matcher(facts);Map<Integer,Integer> occurrences=new TreeMap<>();while(m.find())occurrences.merge(Integer.parseInt(m.group(1))+1,1,Integer::sum);
            int caught=0,old=0;for(var e:occurrences.entrySet()){int d=e.getKey();var c=checks.get(d);boolean b=baseline.getOrDefault(d,Set.of()).contains("ADDRESS");if(b)old++;if(c!=null&&c.candidate())caught++;known.append(d).append('\t').append(e.getValue()).append('\t').append(b).append('\t').append(c==null?"NO_ALIGNED_LABEL":c.status()).append('\n');}
            summary+=" B_referenceLines="+occurrences.size()+" B_referenceOccurrences="+occurrences.values().stream().mapToInt(Integer::intValue).sum()+" B_baselineCoverage="+old+" B_q1Coverage="+caught;
            write(event.resolve("known-B-"+name+".tsv"),known.toString());
        }
        write(event.resolve("summary-"+name+".txt"),summary+"\n");System.out.println(summary);
        Path judgement=root.resolve("adjudication-"+name+".tsv");
        if(Files.exists(judgement)){
            var judgments=adjudications(judgement);Map<String,Integer> oldCounts=new TreeMap<>(),newCounts=new TreeMap<>();
            for(var e:baseline.entrySet())if(e.getValue().contains("ADDRESS"))oldCounts.merge(judgments.getOrDefault(e.getKey(),"NOT_REVIEWED"),1,Integer::sum);
            for(var e:checks.entrySet())if(e.getValue().candidate())newCounts.merge(judgments.getOrDefault(e.getKey(),"NOT_REVIEWED"),1,Integer::sum);
            String semantic=name+" SESSION_INFERENCE_ONLY baseline="+oldCounts+" q1="+newCounts;
            write(event.resolve("session-assessment-"+name+".txt"),semantic+"\n");System.out.println(semantic);
            write(event.resolve("adjudication-source-"+name+".tsv"),read(judgement));
        }
        StringBuilder hashes=new StringBuilder();for(String file:List.of(rawName,draftName,glName,prName))hashes.append(file).append('\t').append(PairText.sha256(read(inputs.resolve(file)))).append('\n');
        write(event.resolve("input-text-hashes-"+name+".tsv"),hashes.toString());
    }
}
