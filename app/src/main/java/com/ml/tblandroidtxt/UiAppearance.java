package com.ml.tblandroidtxt;

import android.content.Context;
import java.util.Arrays;
import java.util.List;

/** Product UI palettes. Semantic success/warning/error colors remain independent. */
public final class UiAppearance {
    public static final class Palette {
        public final String id,name; public final int bg,panel,card,field,border,text,muted,accent,accentAlt;
        Palette(String id,String name,String bg,String panel,String card,String field,String border,String text,String muted,String accent,String accentAlt){this.id=id;this.name=name;this.bg=c(bg);this.panel=c(panel);this.card=c(card);this.field=c(field);this.border=c(border);this.text=c(text);this.muted=c(muted);this.accent=c(accent);this.accentAlt=c(accentAlt);}
    }
    private static final List<Palette> PALETTES= Arrays.asList(
            p("default","Default","#0E0F11","#232324","#2B2B2C","#1B1B1C","#434344","#F5F7FA","#A1A8B3","#5C91EB","#36B9C5"),
            p("blue","Ocean Blue","#0D1219","#182433","#203044","#131C28","#344B66","#F2F6FC","#9EAEC0","#4C9AFF","#4BC0D9"),
            p("teal","Seafoam","#0C1516","#172728","#203536","#122021","#345354","#F1F8F7","#9CB5B3","#46B9AE","#50C8C2"),
            p("mint","Mint","#0D1512","#192820","#21352A","#132019","#365442","#F2F8F4","#9FB5A7","#52B788","#62C6A0"),
            p("green","Forest","#10150F","#20291E","#2A3627","#182016","#45573F","#F4F8F1","#A7B4A2","#6AAA64","#72C085"),
            p("gold","Yellow Gold","#17140D","#2B2618","#393220","#211D12","#5A5030","#FAF7EC","#BDB39A","#D0A83A","#E0BC55"),
            p("orange","Orange","#18110D","#302117","#3D2A1D","#251A12","#61452E","#FBF4EE","#C0AA99","#E1843D","#E8A15C"),
            p("rose","Rose","#180F13","#2F1D25","#3D2630","#24171D","#60404D","#FAF1F5","#BEA3AE","#D36B91","#E084A3"),
            p("red","Brick Red","#180F0F","#301D1D","#3E2727","#251717","#624040","#FAF1F1","#BFA4A4","#CC6464","#DD7D72"),
            p("violet","Violet","#130F19","#261D31","#322740","#1D1725","#514064","#F6F2FA","#AEA2BC","#9871D8","#B188E2"),
            p("iris","Iris Pastel","#121119","#242231","#302E40","#1B1A25","#4C4963","#F5F4FA","#ABA8BD","#8580C9","#A39DE0"),
            p("slate","Blue Gray","#101316","#20262B","#2A3239","#181D21","#424E57","#F3F6F8","#A3ADB5","#7895AA","#78B5C5"),
            p("graphite","Gray Dark","#111111","#242424","#303030","#1B1B1B","#4B4B4B","#F5F5F5","#AAAAAA","#8AA7D8","#77B8B4"),
            p("coffee","Coffee","#15110F","#29211D","#362B26","#201916","#55463E","#F8F3F0","#B6A8A0","#B98568","#C69B76"),
            p("camo","Camouflage","#11140F","#22271C","#2D3425","#191D15","#48513C","#F4F6F0","#A8B09E","#82945B","#9DA66B")
    );
    public static List<Palette> all(){return PALETTES;}
    public static Palette find(String id){for(Palette p:PALETTES)if(p.id.equals(id))return p;return PALETTES.get(0);}
    public static Palette load(Context c){return find(c.getSharedPreferences("appearance",Context.MODE_PRIVATE).getString("palette","default"));}
    public static void save(Context c,String id){c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putString("palette",find(id).id).apply();}
    private static Palette p(String...v){return new Palette(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10]);}
    private static int c(String s){String v=s.startsWith("#")?s.substring(1):s;long n=Long.parseLong(v,16);if(v.length()==6)n|=0xFF000000L;return(int)n;}
    private UiAppearance(){}
}
