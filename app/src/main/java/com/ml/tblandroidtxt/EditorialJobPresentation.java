package com.ml.tblandroidtxt;
import java.util.List;
import com.ml.tblandroidtxt.EditorialPairModels.*;
import com.ml.tblandroidtxt.editorial.api.PairStates.*;

final class EditorialJobPresentation {
    static int done(List<PairItem> items) {
        int done=0;for(PairItem item:items)if(item.state!=PairState.IMPORTED&&item.state!=PairState.E_RESERVED&&item.state!=PairState.E_SENT)done++;return done;
    }
    static boolean canResume(PairRun run,List<PairItem> items,boolean active) {
        if(active||!com.ml.tblandroidtxt.editorial.api.PairContract.REVISION.equals(run.contractRevision))return false;
        for(PairItem item:items)if(item.state==PairState.IMPORTED)return true;return false;
    }
    static String headline(PairRun run,List<PairItem> items,boolean active) {
        int done=done(items);String progress="Đoạn "+done+"/"+items.size()+" · "+EditorialApiPresenter.usd(run.usd);
        if(active)return "Đang biên tập · "+progress;
        if(run.state==RunState.FINAL_ELIGIBLE)return "Đã hoàn tất · "+progress;
        if(canResume(run,items,false)&&run.started)return "Đã dừng ở đoạn "+done+"/"+items.size()+" — Chạy tiếp · "+EditorialApiPresenter.usd(run.usd);
        return EditorialPairPresenter.runHeadline(run,false,EditorialPairPresenter.interrupted(run,items))+" · "+progress;
    }
}
