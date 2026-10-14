package com.ml.tblandroidtxt;
import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.ml.tblandroidtxt.EditorialPairModels.*;
import com.ml.tblandroidtxt.editorial.api.PairStates.PairState;
import static org.junit.Assert.*;
public class EditorialResultPresentationTest {
    @Test public void everyAddressAndApplierReasonHasVietnameseText() throws Exception {
        for(String file:List.of("AddressEditGuard", "TargetedFixApplier")) {
            String source=Files.readString(Paths.get("../editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/api/fix/"+file+".java"));
            Matcher matcher=Pattern.compile("\\\"([A-Z][A-Z_]+)\\\"").matcher(source);
            while(matcher.find()) {String code=matcher.group(1);if(code.equals("ADDRESS_")||code.equals("BLOCK"))continue;
                assertNotEquals(code,"Lý do khác",EditorialResultPresentation.reason(code));}
        }
        assertEquals("Lý do khác",EditorialResultPresentation.reason("UNRECOGNIZED_FUTURE_CODE"));
    }
    @Test public void blockedFixedProposalIsNotAnAppliedEdit() throws Exception {
        PairItem item=new PairItem();item.state=PairState.STRUCTURE_BLOCKED;
        JSONObject p=new JSONObject().put("id",1).put("status","FIXED").put("reason","REPLACED").put("before","A").put("after","B");
        item.gateJson=new JSONObject().put("fixPoints",new JSONArray().put(p)).toString();
        assertFalse(EditorialResultPresentation.applied(item,p));
        assertTrue(EditorialResultPresentation.readablePoints(item,0).isEmpty());
        assertEquals(1,EditorialResultPresentation.readablePoints(item,2).size());
        assertTrue(EditorialResultPresentation.summary(new PairRun(),List.of(item),0).contains("0 dòng đã sửa"));
        item.state=PairState.ACCEPTED;assertTrue(EditorialResultPresentation.applied(item,p));
        assertEquals(1,EditorialResultPresentation.readablePoints(item,0).size());
    }
    @Test public void missingEvidenceAndUnreachedChunksAreNotZeroErrorSuccess() {
        PairItem pending=new PairItem();pending.state=PairState.IMPORTED;
        assertTrue(EditorialResultPresentation.summary(new PairRun(),List.of(pending),0).contains("còn 1 đoạn chưa chạy"));
        assertTrue(EditorialResultPresentation.readablePoints(pending,0).isEmpty());
    }
}
