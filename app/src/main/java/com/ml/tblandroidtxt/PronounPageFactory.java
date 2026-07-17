package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Multi-profile Pronoun library aligned with the Glossaries workflow. */
class PronounPageFactory {
    private final MainActivity a;
    PronounPageFactory(MainActivity activity){a=activity;}

    View buildPronounsPage(){ScrollView scroll=a.scroll();LinearLayout root=a.pageRoot();scroll.addView(root);root.addView(a.editingPronoun==null?listPage():editorPage());return scroll;}

    View listPage(){
        LinearLayout panel=a.sectionCard("↔","Pronoun profiles");
        TextView desc=a.text("Quản lý nhiều bộ quan hệ xưng hô. Chỉ profile Active được đưa vào prompt của job mới.",13,a.MUTED,false);desc.setSingleLine(false);panel.addView(desc,a.marginLP(-1,-2,0,0,0,10));
        LinearLayout actions=a.rowContainer();actions.addView(a.primaryButton("+ Import pronoun",v->{a.pendingPronounReplaceId="";a.choosePronoun();}),new LinearLayout.LayoutParams(0,a.dp(48),1));actions.addView(a.space(8,1));actions.addView(a.secondaryButton("Health check",v->a.showGlossaryPronounHealth()),new LinearLayout.LayoutParams(0,a.dp(48),1));panel.addView(actions);
        PronounStore.Profile active=PronounStore.selected(a);TextView activeLabel=a.text(active==null?"Active pronoun: —":"Active pronoun: "+active.name+" • "+active.count()+" rules",13,active==null?a.MUTED:a.GREEN,true);activeLabel.setSingleLine(false);panel.addView(activeLabel,a.marginLP(-1,-2,0,10,0,12));
        List<PronounStore.Profile> profiles=PronounStore.loadAll(a);if(profiles.isEmpty()){TextView empty=a.text("Chưa có pronoun profile. Bấm Import pronoun để thêm CSV/TXT/JSON, sau đó chọn Use.",14,a.MUTED,false);empty.setGravity(Gravity.CENTER);empty.setSingleLine(false);panel.addView(empty,new LinearLayout.LayoutParams(-1,a.dp(110)));}
        else for(PronounStore.Profile p:profiles)panel.addView(item(p,active!=null&&TextUtils.equals(active.id,p.id)),a.marginLP(-1,-2,0,0,0,10));
        return panel;
    }

    View item(PronounStore.Profile p,boolean active){LinearLayout box=a.card(14,a.FIELD,active?a.GREEN:a.BORDER);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));LinearLayout title=a.rowContainer();TextView name=a.text((active?"✓ ":"↔ ")+a.nonEmpty(p.name,"Unnamed pronoun"),15,active?a.GREEN:a.TEXT,true);name.setSingleLine(false);title.addView(name,new LinearLayout.LayoutParams(0,-2,1));if(active)title.addView(a.chip("ACTIVE",a.GREEN,true));box.addView(title);PromptContextBuilder.ParseReport report=PromptContextBuilder.validate("",p.text);String meta=p.count()+" rules"+(report.conflicts.isEmpty()?"":" • warnings "+report.conflicts.size());box.addView(a.text(meta,12,report.conflicts.isEmpty()?a.MUTED:a.AMBER,false),a.marginLP(-1,-2,0,4,0,10));LinearLayout buttons=a.rowContainer();Button use=a.tinyButton(active?"Using":"Use",active?a.GREEN:a.BLUE,v->a.selectPronoun(p));use.setEnabled(!active);use.setAlpha(active?.65f:1f);buttons.addView(use,new LinearLayout.LayoutParams(0,a.dp(42),1));buttons.addView(a.space(6,1));buttons.addView(a.tinyButton("Edit",a.BLUE,v->{a.editingPronoun=p;a.switchTab("Pronouns");}),new LinearLayout.LayoutParams(0,a.dp(42),1));buttons.addView(a.space(6,1));buttons.addView(a.tinyButton("Del",a.RED,v->confirmDelete(p)),new LinearLayout.LayoutParams(0,a.dp(42),1));box.addView(buttons);return box;}

    View editorPage(){PronounStore.Profile p=a.editingPronoun;boolean active=TextUtils.equals(PronounStore.getSelectedId(a),p.id);LinearLayout panel=a.sectionCard("↔",a.nonEmpty(p.name,"Pronoun profile"));LinearLayout top=a.rowContainer();top.addView(a.secondaryButton("← Back",v->{a.editingPronoun=null;a.switchTab("Pronouns");}),new LinearLayout.LayoutParams(0,a.dp(44),1));top.addView(a.space(8,1));top.addView(a.primaryButton(active?"Active":"Use for translation",v->a.selectPronoun(p)),new LinearLayout.LayoutParams(0,a.dp(44),1));panel.addView(top);LinearLayout tools=a.rowContainer();tools.addView(a.secondaryButton("Rename",v->rename(p)),new LinearLayout.LayoutParams(0,a.dp(42),1));tools.addView(a.space(6,1));tools.addView(a.secondaryButton("Replace file",v->a.replacePronoun(p)),new LinearLayout.LayoutParams(0,a.dp(42),1));tools.addView(a.space(6,1));tools.addView(a.dangerButton("Delete",v->confirmDelete(p)),new LinearLayout.LayoutParams(0,a.dp(42),1));panel.addView(tools,a.marginLP(-1,-2,0,8,0,12));PromptContextBuilder.ParseReport report=PromptContextBuilder.validate(a.activeGlossaryPromptText(),p.text);panel.addView(a.text(p.count()+" pronoun rules • "+(report.malformedPronounRows.isEmpty()?"Valid":"Warnings: "+report.malformedPronounRows.size()),14,report.malformedPronounRows.isEmpty()?a.GREEN:a.AMBER,true));TextView preview=a.text(a.preview(p.text,6000),12,a.TEXT,false);preview.setTypeface(Typeface.MONOSPACE);preview.setTextIsSelectable(true);LinearLayout box=a.card(10,a.FIELD,a.BORDER);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));box.addView(preview);panel.addView(box,a.marginLP(-1,-2,0,10,0,0));return panel;}

    void rename(PronounStore.Profile p){EditText input=a.input("Profile name",p.name);new AlertDialog.Builder(a).setTitle("Rename pronoun profile").setView(input).setPositiveButton("Save",(d,w)->{String name=input.getText().toString().trim();if(!name.isEmpty()){p.name=name;PronounStore.upsert(a,p);if(TextUtils.equals(PronounStore.getSelectedId(a),p.id))a.applyActivePronoun(p);a.switchTab("Pronouns");}}).setNegativeButton("Cancel",null).show();}
    void confirmDelete(PronounStore.Profile p){if(!a.ensureConfigMutable())return;new AlertDialog.Builder(a).setTitle("Delete pronoun profile?").setMessage("Delete "+a.nonEmpty(p.name,"Unnamed pronoun")+" from the app library. The original file is not deleted.").setPositiveButton("Delete",(d,w)->{boolean active=TextUtils.equals(PronounStore.getSelectedId(a),p.id);PronounStore.delete(a,p.id);if(active)a.applyActivePronoun(null);a.editingPronoun=null;a.switchTab("Pronouns");}).setNegativeButton("Cancel",null).show();}
}
