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
        LinearLayout actions=a.rowContainer();actions.addView(a.primaryButton("Import chapters",v->a.chooseEditorialBatch(project.id)),new LinearLayout.LayoutParams(0,a.dp(46),1));actions.addView(a.space(8,1));actions.addView(a.secondaryButton("Preview list",v->showChapters(project,chapters)),new LinearLayout.LayoutParams(0,a.dp(46),1));box.addView(actions);
        for(EditorialRepository.Chapter chapter:chapters){LinearLayout row=a.rowContainer();TextView label=a.text(chapter.chapterKey+" • "+chapter.state.name(),12,chapter.state==EditorialWorkflowV5.ChapterState.L1_READY||chapter.state==EditorialWorkflowV5.ChapterState.L1_CLOSED||chapter.state==EditorialWorkflowV5.ChapterState.L2_CLOSED||chapter.state==EditorialWorkflowV5.ChapterState.RELEASE_READY?a.GREEN:chapter.state==EditorialWorkflowV5.ChapterState.FAILED?a.RED:a.MUTED,false);row.addView(label,new LinearLayout.LayoutParams(0,a.dp(42),1));if(chapter.state==EditorialWorkflowV5.ChapterState.L1_CLOSED){row.addView(a.tinyButton("Report",a.BLUE,v->a.showEditorialL1Report(chapter.id,chapter.chapterKey)),new LinearLayout.LayoutParams(a.dp(72),a.dp(42)));row.addView(a.tinyButton("Run L2",a.GREEN,v->a.confirmRunEditorialL2(chapter.id)),new LinearLayout.LayoutParams(a.dp(72),a.dp(42)));}else if(chapter.state==EditorialWorkflowV5.ChapterState.L2_CLOSED){row.addView(a.tinyButton("VI_L2",a.BLUE,v->a.showEditorialL2(chapter.id,chapter.chapterKey)),new LinearLayout.LayoutParams(a.dp(72),a.dp(42)));row.addView(a.tinyButton("Run L3",a.GREEN,v->a.confirmRunEditorialL3(chapter.id)),new LinearLayout.LayoutParams(a.dp(72),a.dp(42)));}else if(chapter.state==EditorialWorkflowV5.ChapterState.RELEASE_READY)row.addView(a.tinyButton("FINAL",a.BLUE,v->a.showEditorialL3(chapter.id,chapter.chapterKey)),new LinearLayout.LayoutParams(a.dp(84),a.dp(42)));else if(chapter.state==EditorialWorkflowV5.ChapterState.FAILED){String l1Failure,l2Failure,l3Failure;try(EditorialRepository repo=new EditorialRepository(a)){l1Failure=repo.latestEvidenceForChapter(chapter.id,"L1_FAILURE");l2Failure=repo.latestEvidenceForChapter(chapter.id,"L2_FAILURE");l3Failure=repo.latestEvidenceForChapter(chapter.id,"L3_FAILURE");}if(!l3Failure.isEmpty())row.addView(a.tinyButton("L3 Error",a.RED,v->a.showResult("L3 failed",l3Failure)),new LinearLayout.LayoutParams(a.dp(84),a.dp(42)));else if(!l2Failure.isEmpty())row.addView(a.tinyButton("Retry L2",a.AMBER,v->a.confirmRetryEditorialL2(chapter.id,l2Failure)),new LinearLayout.LayoutParams(a.dp(84),a.dp(42)));else row.addView(a.tinyButton("Retry",a.AMBER,v->a.confirmRetryEditorialL1(chapter.id,l1Failure)),new LinearLayout.LayoutParams(a.dp(84),a.dp(42)));}else{android.widget.Button run=a.tinyButton("Run L1",a.BLUE,v->a.confirmRunEditorialL1(chapter.id));run.setEnabled(chapter.state==EditorialWorkflowV5.ChapterState.L1_READY);run.setAlpha(run.isEnabled()?1f:.45f);row.addView(run,new LinearLayout.LayoutParams(a.dp(84),a.dp(42)));}box.addView(row,a.marginLP(-1,-2,0,8,0,0));} return box;
    }
    private String chapterSummary(List<EditorialRepository.Chapter> chapters){StringBuilder b=new StringBuilder();int max=Math.min(4,chapters.size());for(int i=0;i<max;i++){EditorialRepository.Chapter c=chapters.get(i);if(i>0)b.append('\n');b.append("• ").append(c.chapterKey).append(" — ").append(c.state.name());}if(chapters.size()>max)b.append("\n+").append(chapters.size()-max).append(" chapter khác");return b.toString();}
    private void showChapters(EditorialRepository.Project project,List<EditorialRepository.Chapter> chapters){new AlertDialog.Builder(a).setTitle(project.seriesName+" • "+project.volumeName).setMessage(chapters.isEmpty()?"Chưa có chapter. Bấm Import chapters để chọn nhiều TXT.":chapterSummary(chapters)).setPositiveButton("Import chapters",(d,w)->a.chooseEditorialBatch(project.id)).setNegativeButton("Đóng",null).show();}
    private void showCreateProject(){LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int p=a.dp(22);form.setPadding(p,0,p,0);EditText series=a.input("Series","");EditText volume=a.input("Volume","");form.addView(series);form.addView(volume,a.marginLP(-1,-2,0,8,0,0));new AlertDialog.Builder(a).setTitle("Tạo project biên tập").setMessage("Glossary/Pronoun đang chọn sẽ được dùng làm mặc định khi batch không kèm file riêng.").setView(form).setPositiveButton("Tạo",(d,w)->a.createEditorialProject(series.getText().toString(),volume.getText().toString())).setNegativeButton("Hủy",null).show();}
}
