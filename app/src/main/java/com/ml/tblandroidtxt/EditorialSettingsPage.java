package com.ml.tblandroidtxt;
import android.widget.*;
import android.app.AlertDialog;
import java.math.BigDecimal;
import com.ml.tblandroidtxt.EditorialPairModels.PairRun;
import com.ml.tblandroidtxt.editorial.api.*;
import com.ml.tblandroidtxt.editorial.api.fix.*;

final class EditorialSettingsPage {
    static LinearLayout build(MainActivity a) {
        EditorialPreferences saved=SettingsStore.load(a).editorial;
        LinearLayout card=a.sectionCard("✎","Biên tập");
        card.addView(a.text("Áp dụng cho lượt mới; tổ hợp có thể ghi đè mô hình và trần chi phí.",13,a.MUTED,false));
        EditText model=a.input("Để trống: dùng mô hình của luồng dịch",saved.model);
        EditText reasoning=a.input("Để trống: theo mặc định mô hình",saved.reasoning);
        EditText cap=a.decimal("0.10",saved.cap.toPlainString());
        card.addView(a.fieldBlock("MÔ HÌNH",model));card.addView(a.fieldBlock("MỨC SUY LUẬN",reasoning));
        card.addView(a.fieldBlock("TRẦN CHI PHÍ MỖI CHƯƠNG (USD)",cap));
        CheckBox address=a.check("Xưng hô",saved.address,"Kiểm cách tự xưng và gọi nhau.");
        CheckBox glossary=a.check("Thuật ngữ",saved.glossary,"Kiểm theo Glossary.");
        CheckBox kana=a.check("Chữ Nhật còn sót",saved.kana,"Gồm câu RAW chưa được dịch.");
        CheckBox punctuation=a.check("Dấu câu theo RAW",saved.punctuation,"Khôi phục tất định, không gọi model.");
        card.addView(address);card.addView(glossary);card.addView(kana);card.addView(punctuation);
        card.addView(a.primaryButton("Lưu cài đặt Biên tập",v->{
            try {
                BigDecimal max=new BigDecimal(cap.getText().toString().trim().replace(',','.'));if(max.signum()<=0)throw new IllegalArgumentException();
                String effort=reasoning.getText().toString().trim();if(!java.util.List.of("","none","minimal","low","medium","high","xhigh").contains(effort))throw new IllegalArgumentException();
                AppSettings settings=SettingsStore.load(a);EditorialPreferences p=new EditorialPreferences();
                p.model=model.getText().toString().trim();p.reasoning=effort;p.cap=max;p.address=address.isChecked();p.glossary=glossary.isChecked();p.kana=kana.isChecked();p.punctuation=punctuation.isChecked();
                settings.editorial=p;SettingsStore.save(a,settings);a.toast("Đã lưu cài đặt Biên tập");
            }catch(Exception invalid){a.toast("Kiểm tra trần chi phí và mức suy luận (low, medium, high hoặc để trống).");}
        }));
        card.addView(a.secondaryButton("Xem trước prompt một đoạn",v->preview(a)));
        return card;
    }
    private static void preview(MainActivity a) {
        EditorialPairUiController c=a.editorialApi().pair;
        if(c.source==null||c.references==null){a.toast("Chọn nguồn ở Biên tập và mở màn xem trước các đoạn trước.");a.switchTab("Editorial");return;}
        PairRun seed=new PairRun();seed.chapterId=c.source.chapterId;seed.sourceRef=c.source.ref;
        seed.rawRowsJson=EditorialPairSnapshot.rowsJson(c.source.rawRows);seed.draftRowsJson=EditorialPairSnapshot.rowsJson(c.source.draftRows);
        seed.glossaryText=c.references.glossaryText;seed.pronounText=c.references.pronounText;
        seed.chunkPlanJson=c.source.plan==null?"":c.source.plan.toJson();
        AppSettings settings=SettingsStore.load(a);seed.cueFields=settings.editorial.freeze(null);
        EditorialPairSnapshot snapshot=EditorialPairSnapshot.of(seed);
        String[] names=new String[snapshot.map.entries().size()];for(int i=0;i<names.length;i++)names[i]="Đoạn "+(i+1);
        new AlertDialog.Builder(a).setTitle("Chọn đoạn xem prompt").setItems(names,(dialog,index)->{
            PairMap.Entry entry=snapshot.map.entries().get(index);
            FixPointFinder.Result found=settings.editorial.filter(FixPointFinder.find(snapshot.map,entry,snapshot.glossary,snapshot.pronounText,snapshot.cueFields,snapshot.chunkPlan));
            String body;
            if(found.points().isEmpty())body="Đoạn không còn điểm cần model theo các loại đang bật; không gửi yêu cầu.";
            else {ApiPrompt prompt=EditorialPairRunService.targetedPrompt(snapshot,entry,found.points(),settings.targetLanguage);body="Chỉ xem trước, không gửi yêu cầu.\n\n[SYSTEM]\n"+prompt.system()+"\n\n[USER]\n"+prompt.user();}
            PromptPreviewDialog.showTextDialog(a,"Prompt Biên tập · "+names[index],body,a.TEXT);
        }).setNegativeButton("Đóng",null).show();
    }
}
