package com.ml.tblandroidtxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Maps RAW and DRAFT only at explicit shared scene separators; never invents a positional correspondence. */
public final class EditorialDraftSceneMapper {
    public enum Status { ALIGNED, AMBIGUOUS, MISALIGNED }
    public static final class Pair { public final int ordinal; public final String raw,draft,rawStart,rawEnd; public final Status status; Pair(int ordinal,String raw,String draft,String rawStart,String rawEnd,Status status){this.ordinal=ordinal;this.raw=raw;this.draft=draft;this.rawStart=rawStart;this.rawEnd=rawEnd;this.status=status;} }
    public static List<Pair> map(String raw,String draft) {
        List<String> raws=parts(raw),drafts=parts(draft);ArrayList<Pair> pairs=new ArrayList<>();
        int anchor=1;if(raws.size()==1&&drafts.size()==1){int n=anchorCount(raws.get(0));pairs.add(new Pair(1,raws.get(0),drafts.get(0),id(anchor),id(anchor+n-1),Status.AMBIGUOUS));return pairs;}
        int count=Math.max(raws.size(),drafts.size());for(int i=0;i<count;i++){String r=i<raws.size()?raws.get(i):"";String d=i<drafts.size()?drafts.get(i):"";int n=anchorCount(r);pairs.add(new Pair(i+1,r,d,id(anchor),id(Math.max(anchor,anchor+n-1)),(i<raws.size()&&i<drafts.size()&&raws.size()==drafts.size())?Status.ALIGNED:Status.MISALIGNED));anchor+=n;}return pairs;
    }
    private static List<String> parts(String source){String value=source==null?"":source.replace("\r\n","\n").replace('\r','\n');ArrayList<String> out=new ArrayList<>();StringBuilder current=new StringBuilder();for(String line:value.split("(?<=\\n)",-1)){if(line.trim().matches("[◇◆＊*─—-]{3,}")){current.append(line);out.add(current.toString());current.setLength(0);}else current.append(line);}if(current.length()>0||out.isEmpty())out.add(current.toString());return Collections.unmodifiableList(out);}
    private static int anchorCount(String raw){return EditorialRawMap.create(raw).anchors.size();}private static String id(int n){return String.format(java.util.Locale.US,"p%06d",n);}
    private EditorialDraftSceneMapper(){}
}
