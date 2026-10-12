package com.ml.tblandroidtxt;
/** Small line-oriented preview for MVP; source/output files remain immutable evidence. */
public final class EditorialTextDiff {
 public static String render(String before,String after,int maxLines){String[] a=lines(before),b=lines(after);StringBuilder out=new StringBuilder("DRAFT → VI_L2\n\n");int changed=0,shown=0,n=Math.max(a.length,b.length);for(int i=0;i<n;i++){String x=i<a.length?a[i]:"",y=i<b.length?b[i]:"";if(x.equals(y))continue;changed++;if(shown++<maxLines)out.append("@@ line ").append(i+1).append("\n- ").append(x).append("\n+ ").append(y).append("\n\n");}if(changed==0)out.append("Không có thay đổi theo dòng.\n");else if(changed>maxLines)out.append("… còn ").append(changed-maxLines).append(" dòng thay đổi.\n");return out.append("\nTổng dòng thay đổi: ").append(changed).toString();}
 private static String[] lines(String x){return (x==null?"":x).replace("\r\n","\n").replace('\r','\n').split("\n",-1);}private EditorialTextDiff(){}
}
