package com.ml.tblandroidtxt;
import android.view.View;
import android.widget.*;
import java.util.List;
import com.ml.tblandroidtxt.EditorialPairModels.*;

final class EditorialJobCard {
    static View build(MainActivity a,EditorialApiCombo combo,PairRun run,List<PairItem> items){
        boolean active=EditorialPairForegroundService.isActive(run.id)||EditorialPairRunService.isActive(run.id);
        LinearLayout card=a.sectionCard("✎",combo.name.isEmpty()?"Lượt Biên tập":combo.name);
        TextView headline=a.text(EditorialJobPresentation.headline(run,items,active),14,a.TEXT,true);headline.setSingleLine(false);card.addView(headline);
        ProgressBar bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,items.size()));bar.setProgress(EditorialJobPresentation.done(items));card.addView(bar);
        if(active)card.addView(a.dangerButton("Hủy sau đoạn hiện tại",v->EditorialPairForegroundService.cancelRun(run.id)));
        if(EditorialJobPresentation.canResume(run,items,active))card.addView(a.primaryButton("Chạy tiếp",v->{a.editorialApi().pair.openResult(combo.id);a.editorialApi().pair.resumeRun();a.switchTab("Editorial");}));
        card.addView(a.secondaryButton("Mở kết quả",v->{a.editorialApi().pair.openResult(combo.id);a.switchTab("Editorial");}));
        return card;
    }
    static void addJobs(MainActivity a,LinearLayout root){
        try(SqliteEditorialApiStore combos=new SqliteEditorialApiStore(a);SqliteEditorialPairRunStore runs=new SqliteEditorialPairRunStore(a)){
            for(EditorialApiCombo combo:combos.listCombos()){PairRun run=runs.latestRun(combo.id);if(run!=null)root.addView(build(a,combo,run,runs.items(run.id)),a.marginLP(-1,-2,0,0,0,10));}
        }
    }
}
