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
    static final String IMPORT_LABEL = "+ Nhập Pronoun";
    static final String HEALTH_CHECK_LABEL = "Kiểm tra dữ liệu";
    static final String USING_LABEL = "Đang dùng";
    static final String USE_LABEL = "Dùng";
    static final String EDIT_LABEL = "Sửa";
    static final String DELETE_LABEL = "Xóa";
    static final String BACK_LABEL = "← Quay lại";
    static final String USE_FOR_TRANSLATION_LABEL = "Dùng cho bản dịch";
    static final String RENAME_LABEL = "Đổi tên";
    static final String REPLACE_FILE_LABEL = "Thay file";

    private final MainActivity a;
    PronounPageFactory(MainActivity activity){a=activity;}

    View buildPronounsPage(){ScrollView scroll=a.scroll();LinearLayout root=a.pageRoot();scroll.addView(root);if(PronounStore.isCorrupt(a)){a.editingPronoun=null;root.addView(a.libraryLockedCard("Pronoun"));}else root.addView(a.editingPronoun==null?listPage():editorPage());return scroll;}

    View listPage(){
        LinearLayout panel=a.plainCard();
        TextView desc=a.text("Có thể chọn nhiều file cùng lúc; mỗi file tạo một hồ sơ Pronoun riêng. Chỉ Pronoun đang dùng được đưa vào prompt.",13,a.MUTED,false);desc.setSingleLine(false);panel.addView(desc,a.marginLP(-1,-2,0,0,0,10));
        LinearLayout actions=a.rowContainer();actions.addView(a.primaryButton(IMPORT_LABEL,v->{a.pendingPronounReplaceId="";a.choosePronoun();}),new LinearLayout.LayoutParams(0,-2,1));actions.addView(a.space(8,1));actions.addView(a.secondaryButton(HEALTH_CHECK_LABEL,v->a.showGlossaryPronounHealth()),new LinearLayout.LayoutParams(0,-2,1));panel.addView(actions);
        PronounStore.Profile active=PronounStore.selected(a);TextView activeLabel=a.text(active==null?"Pronoun đang dùng: —":"Pronoun đang dùng: "+active.name+" • "+active.count()+" quy tắc",13,active==null?a.MUTED:a.GREEN,true);activeLabel.setSingleLine(false);panel.addView(activeLabel,a.marginLP(-1,-2,0,10,0,12));
        List<PronounStore.Profile> profiles=PronounStore.loadAll(a);
        LibrarySelectionController selection=a.bulkUi.controller(LibraryBulkUi.TAB_PRONOUNS);
        if(selection.isActive()){java.util.ArrayList<String> ids=new java.util.ArrayList<>();for(PronounStore.Profile p:profiles)ids.add(p.id);panel.addView(a.bulkUi.selectionHeader(LibraryBulkUi.TAB_PRONOUNS,ids),a.marginLP(-1,-2,0,0,0,10));}
        else if(!profiles.isEmpty())panel.addView(a.bulkUi.headerRow(LibraryBulkUi.TAB_PRONOUNS,profiles.size()),a.marginLP(-1,-2,0,0,0,10));
        if(profiles.isEmpty()){TextView empty=a.text("Chưa có hồ sơ Pronoun. Bấm Nhập Pronoun để thêm CSV/TXT/JSON, sau đó chọn Dùng.",14,a.MUTED,false);empty.setGravity(Gravity.CENTER);empty.setSingleLine(false);panel.addView(empty,new LinearLayout.LayoutParams(-1,a.dp(110)));}
        else for(PronounStore.Profile p:profiles){boolean isActive=active!=null&&TextUtils.equals(active.id,p.id);View card=selection.isActive()?a.bulkUi.selectableCard(LibraryBulkUi.TAB_PRONOUNS,p.id,a.nonEmpty(p.name,"Pronoun chưa đặt tên"),p.count()+" quy tắc"+(isActive?" • Đang dùng":""),isActive?a.GREEN:a.BORDER):item(p,isActive);panel.addView(card,a.marginLP(-1,-2,0,0,0,10));}
        return panel;
    }

    View item(PronounStore.Profile p,boolean active){LinearLayout box=a.card(14,a.FIELD,active?a.GREEN:a.BORDER);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));LinearLayout title=a.rowContainer();TextView name=a.text(a.nonEmpty(p.name,"Pronoun chưa đặt tên"),15,active?a.GREEN:a.TEXT,true);name.setSingleLine(false);title.addView(name,new LinearLayout.LayoutParams(0,-2,1));if(active)title.addView(a.chip(USING_LABEL,a.GREEN,true));box.addView(title);PromptContextBuilder.ParseReport report=PromptContextBuilder.validate("",p.text);String meta=p.count()+" quy tắc"+(report.conflicts.isEmpty()?"":" • cảnh báo "+report.conflicts.size());box.addView(a.text(meta,12,report.conflicts.isEmpty()?a.MUTED:a.AMBER,false),a.marginLP(-1,-2,0,4,0,10));LinearLayout buttons=a.rowContainer();if(!active){buttons.addView(a.tinyButton(USE_LABEL,a.BLUE,v->a.selectPronoun(p)),new LinearLayout.LayoutParams(0,-2,1));buttons.addView(a.space(6,1));}buttons.addView(a.tinyButton(EDIT_LABEL,a.BLUE,v->{a.editingPronoun=p;a.switchTab("Pronouns");}),new LinearLayout.LayoutParams(0,-2,1));buttons.addView(a.space(6,1));buttons.addView(a.tinyButton(DELETE_LABEL,a.RED,v->a.bulkUi.deleteIds(LibraryBulkUi.TAB_PRONOUNS,java.util.Collections.singletonList(p.id))),new LinearLayout.LayoutParams(0,-2,1));box.addView(buttons);a.bulkUi.attachLongPress(box,LibraryBulkUi.TAB_PRONOUNS,p.id);return box;}

    View editorPage(){PronounStore.Profile p=a.editingPronoun;boolean active=TextUtils.equals(PronounStore.getSelectedId(a),p.id);LinearLayout panel=a.sectionCard("",a.nonEmpty(p.name,"Hồ sơ Pronoun"));LinearLayout top=a.rowContainer();top.addView(a.secondaryButton(BACK_LABEL,v->{a.editingPronoun=null;a.switchTab("Pronouns");}),new LinearLayout.LayoutParams(0,-2,1));top.addView(a.space(8,1));top.addView(a.primaryButton(active?USING_LABEL:USE_FOR_TRANSLATION_LABEL,v->a.selectPronoun(p)),new LinearLayout.LayoutParams(0,-2,1));panel.addView(top);LinearLayout tools=a.rowContainer();tools.addView(a.secondaryButton(RENAME_LABEL,v->rename(p)),new LinearLayout.LayoutParams(0,-2,1));tools.addView(a.space(6,1));tools.addView(a.secondaryButton(REPLACE_FILE_LABEL,v->a.replacePronoun(p)),new LinearLayout.LayoutParams(0,-2,1));tools.addView(a.space(6,1));tools.addView(a.dangerButton(DELETE_LABEL,v->confirmDelete(p)),new LinearLayout.LayoutParams(0,-2,1));panel.addView(tools,a.marginLP(-1,-2,0,8,0,12));PromptContextBuilder.ParseReport report=PromptContextBuilder.validate(a.activeGlossaryPromptText(),p.text);panel.addView(a.text(p.count()+" quy tắc Pronoun • "+(report.malformedPronounRows.isEmpty()?"Hợp lệ":"Cảnh báo: "+report.malformedPronounRows.size()),14,report.malformedPronounRows.isEmpty()?a.GREEN:a.AMBER,true));TextView preview=a.text(a.preview(p.text,6000),12,a.TEXT,false);preview.setTypeface(Typeface.MONOSPACE);preview.setTextIsSelectable(true);LinearLayout box=a.card(10,a.FIELD,a.BORDER);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));box.addView(preview);panel.addView(box,a.marginLP(-1,-2,0,10,0,0));return panel;}

    void rename(PronounStore.Profile p){EditText input=a.input("Tên hồ sơ",p.name);new AlertDialog.Builder(a).setTitle("Đổi tên hồ sơ Pronoun").setView(input).setPositiveButton("Lưu",(d,w)->{String name=input.getText().toString().trim();if(!name.isEmpty()){p.name=name;if(!PronounStore.upsert(a,p)){a.toast(MainActivity.LIBRARY_WRITE_REFUSED);return;}if(TextUtils.equals(PronounStore.getSelectedId(a),p.id))a.applyActivePronoun(p);a.switchTab("Pronouns");}}).setNegativeButton("Hủy",null).show();}
    void confirmDelete(PronounStore.Profile p){if(!a.ensureConfigMutable())return;new AlertDialog.Builder(a).setTitle("Xóa hồ sơ Pronoun?").setMessage("Xóa "+a.nonEmpty(p.name,"Pronoun chưa đặt tên")+" khỏi thư viện của app. File gốc trong máy không bị xóa.").setPositiveButton(DELETE_LABEL,(d,w)->{boolean active=TextUtils.equals(PronounStore.getSelectedId(a),p.id);if(!PronounStore.delete(a,p.id)){a.toast(MainActivity.LIBRARY_WRITE_REFUSED);return;}if(active)a.applyActivePronoun(null);a.editingPronoun=null;a.switchTab("Pronouns");}).setNegativeButton("Hủy",null).show();}
}
