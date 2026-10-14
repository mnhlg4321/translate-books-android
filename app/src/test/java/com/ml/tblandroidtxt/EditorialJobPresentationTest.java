package com.ml.tblandroidtxt;
import org.junit.Test;
import java.util.List;
import com.ml.tblandroidtxt.EditorialPairModels.*;
import com.ml.tblandroidtxt.editorial.api.PairStates.*;
import static org.junit.Assert.*;
public class EditorialJobPresentationTest {
    @Test public void interruptedProgressComesFromStoredItemsAndOffersResume() {
        PairRun run=new PairRun();run.started=true;run.state=RunState.RUNNING;
        PairItem done=new PairItem();done.state=PairState.ACCEPTED;PairItem pending=new PairItem();pending.state=PairState.IMPORTED;
        assertEquals(1,EditorialJobPresentation.done(List.of(done,pending)));
        assertTrue(EditorialJobPresentation.headline(run,List.of(done,pending),false).contains("Đã dừng ở đoạn 1/2"));
        assertTrue(EditorialJobPresentation.canResume(run,List.of(done,pending),false));
        assertFalse(EditorialJobPresentation.canResume(run,List.of(done,pending),true));
    }
    @Test public void completedAndOldContractRunsNeverOfferPaidResume() {
        PairRun run=new PairRun();run.state=RunState.FINAL_ELIGIBLE;PairItem done=new PairItem();done.state=PairState.ACCEPTED;
        assertFalse(EditorialJobPresentation.canResume(run,List.of(done),false));assertTrue(EditorialJobPresentation.headline(run,List.of(done),false).startsWith("Đã hoàn tất"));
        run.contractRevision="CP-IMPL-19";PairItem pending=new PairItem();assertFalse(EditorialJobPresentation.canResume(run,List.of(pending),false));
    }
}
