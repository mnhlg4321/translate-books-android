package com.ml.tblandroidtxt;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Initial Editorial UX: choose many TXT files, inspect filename pairing, then persist immutable snapshots. */
final class EditorialPageFactory {
    private final MainActivity a;
    EditorialPageFactory(MainActivity activity){a=activity;}
    View build(){
        ScrollView scroll=a.scroll(); LinearLayout root=a.pageRoot(); scroll.addView(root);
        LinearLayout intro=a.sectionCard("✎","Biên tập V5");
        TextView help=a.text("Tạo project một lần. Khi nhập chương, bạn có thể chọn nhiều TXT cùng lúc: RAW + DRAFT cho từng chương, và tùy chọn một GLOSSARY/PRONOUN chung. App xem trước cách ghép tên file trước khi lưu snapshot.",14,a.TEXT,false); help.setSingleLine(false); intro.addView(help);
        intro.addView(a.primaryButton("+ Tạo project biên tập",v->showCreateProject()),a.marginLP(-1,a.dp(48),0,12,0,0)); root.addView(intro);
        List<EditorialRepository.Project> projects; try(EditorialRepository repo=new EditorialRepository(a)){projects=repo.listProjects();}
        if(projects.isEmpty()){TextView empty=a.text("Chưa có project. Project chứa series, volume, workflow V5 và các chapter đã snapshot.",14,a.MUTED,false);empty.setGravity(Gravity.CENTER);empty.setSingleLine(false);root.addView(empty,new LinearLayout.LayoutParams(-1,a.dp(120)));}
        else for(EditorialRepository.Project project:projects) root.addView(projectCard(project),a.marginLP(-1,-2,0,0,0,10)); return scroll;
    }
    private View projectCard(EditorialRepository.Project project){
        LinearLayout box=a.card(14,a.FIELD,a.BORDER);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12)); box.addView(a.text("✎ "+project.seriesName+" • "+project.volumeName,16,a.TEXT,true));
        List<EditorialRepository.Chapter> chapters;try(EditorialRepository repo=new EditorialRepository(a)){chapters=repo.listChapters(project.id);} box.addView(a.text(chapters.size()+" chapter • V5 workflow snapshot",12,a.MUTED,false),a.marginLP(-1,-2,0,4,0,10));
        LinearLayout actions=a.rowContainer();actions.addView(a.primaryButton("Import chapters",v->a.chooseEditorialBatch(project.id)),new LinearLayout.LayoutParams(0,a.dp(46),1));actions.addView(a.space(8,1));actions.addView(a.secondaryButton("Preview list",v->showChapters(project,chapters)),new LinearLayout.LayoutParams(0,a.dp(46),1));box.addView(actions); if(!chapters.isEmpty()) box.addView(a.text(chapterSummary(chapters),12,a.MUTED,false),a.marginLP(-1,-2,0,10,0,0)); return box;
    }
    private String chapterSummary(List<EditorialRepository.Chapter> chapters){StringBuilder b=new StringBuilder();int max=Math.min(4,chapters.size());for(int i=0;i<max;i++){EditorialRepository.Chapter c=chapters.get(i);if(i>0)b.append('\n');b.append("• ").append(c.chapterKey).append(" — ").append(c.state.name());}if(chapters.size()>max)b.append("\n+").append(chapters.size()-max).append(" chapter khác");return b.toString();}
    private void showChapters(EditorialRepository.Project project,List<EditorialRepository.Chapter> chapters){new AlertDialog.Builder(a).setTitle(project.seriesName+" • "+project.volumeName).setMessage(chapters.isEmpty()?"Chưa có chapter. Bấm Import chapters để chọn nhiều TXT.":chapterSummary(chapters)).setPositiveButton("Import chapters",(d,w)->a.chooseEditorialBatch(project.id)).setNegativeButton("Đóng",null).show();}
    private void showCreateProject(){LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int p=a.dp(22);form.setPadding(p,0,p,0);EditText series=a.input("Series","");EditText volume=a.input("Volume","");form.addView(series);form.addView(volume,a.marginLP(-1,-2,0,8,0,0));new AlertDialog.Builder(a).setTitle("Tạo project biên tập").setMessage("Glossary/Pronoun đang chọn sẽ được dùng làm mặc định khi batch không kèm file riêng.").setView(form).setPositiveButton("Tạo",(d,w)->a.createEditorialProject(series.getText().toString(),volume.getText().toString())).setNegativeButton("Hủy",null).show();}
}
