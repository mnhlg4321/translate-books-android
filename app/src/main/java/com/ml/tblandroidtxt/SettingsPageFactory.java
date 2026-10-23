package com.ml.tblandroidtxt;

import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Adaptive category/detail settings workspace introduced in v4 alpha. */
class SettingsPageFactory {
    private final MainActivity a;
    private static final String[] CATEGORIES=SettingsCategoryModel.ids();
    SettingsPageFactory(MainActivity activity){a=activity;}

    View buildSettingsPage(){
        if(!a.isWideLayout())return buildPhoneSettingsPage();
        LinearLayout shell=new LinearLayout(a);shell.setOrientation(a.isWideLayout()?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);shell.setBackgroundColor(a.BG);
        View nav=categoryNavigation();
        shell.addView(nav,new LinearLayout.LayoutParams(a.dp(220),-1));
        ScrollView detail=settingsScroll();LinearLayout content=a.pageRoot();content.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(24));detail.addView(content,new ScrollView.LayoutParams(-1,-2));
        content.addView(pageHeading());content.addView(buildCategory(a.settingsCategory));
        shell.addView(detail,new LinearLayout.LayoutParams(0,-1,1));
        return shell;
    }

    View buildPhoneSettingsPage(){
        ScrollView scroll=settingsScroll();
        LinearLayout content=a.pageRoot();content.setPadding(a.dp(2),a.dp(8),a.dp(2),a.dp(28));
        LinearLayout heading=new LinearLayout(a);heading.setOrientation(LinearLayout.VERTICAL);heading.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(12));
        heading.addView(a.text("Cài đặt",24,a.TEXT,true));
        TextView summary=a.text("Thiết lập Dịch, Provider, Giao diện và mục nâng cao. Chạm một mục để mở.",13,a.MUTED,false);summary.setSingleLine(false);heading.addView(summary);
        content.addView(heading);
        for(String category:CATEGORIES){
            boolean expanded=category.equals(a.settingsCategory)&&a.settingsSectionExpanded;
            content.addView(expandableSection(category,expanded),a.marginLP(-1,-2,0,0,0,10));
        }
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        return scroll;
    }

    ScrollView settingsScroll(){ScrollView s=new ScrollView(a);s.setFillViewport(true);s.setClipToPadding(false);s.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);s.setVerticalScrollBarEnabled(false);return s;}

    View expandableSection(String category,boolean expanded){
        LinearLayout section=new LinearLayout(a);section.setOrientation(LinearLayout.VERTICAL);section.setClipChildren(false);section.setClipToPadding(false);section.setElevation(0);a.tint(section,a.PANEL,a.BORDER,1,12);
        LinearLayout header=a.rowContainer();header.setMinimumHeight(a.dp(64));header.setPadding(a.dp(14),a.dp(8),a.dp(8),a.dp(8));header.setClickable(true);header.setFocusable(true);header.setElevation(0);
        LinearLayout titles=new LinearLayout(a);titles.setOrientation(LinearLayout.VERTICAL);TextView title=a.text(SettingsCategoryModel.label(category),16,a.TEXT,true);TextView description=a.text(categoryDescription(category),12,a.MUTED,false);description.setSingleLine(false);titles.addView(title);titles.addView(description);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=a.text(expanded?"▴":"▾",16,a.MUTED,true);arrow.setGravity(Gravity.CENTER);arrow.setContentDescription(expanded?"Thu gọn "+SettingsCategoryModel.label(category):"Mở "+SettingsCategoryModel.label(category));header.addView(arrow,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
        section.addView(header,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setClipChildren(false);
        View divider=new View(a);divider.setBackgroundColor(a.BORDER);body.addView(divider,new LinearLayout.LayoutParams(-1,a.dp(1)));
        LinearLayout bodyContent=new LinearLayout(a);bodyContent.setOrientation(LinearLayout.VERTICAL);bodyContent.setPadding(a.dp(10),a.dp(10),a.dp(10),a.dp(12));bodyContent.setClipChildren(false);
        final boolean[] contentBuilt={expanded};
        if(expanded)bodyContent.addView(buildCategory(category),new LinearLayout.LayoutParams(-1,-2));
        body.addView(bodyContent,new LinearLayout.LayoutParams(-1,-2));
        body.setVisibility(expanded?View.VISIBLE:View.GONE);section.addView(body,new LinearLayout.LayoutParams(-1,-2));
        header.setOnClickListener(v->{
            boolean show=body.getVisibility()!=View.VISIBLE;
            if(show&&!contentBuilt[0]){bodyContent.addView(buildCategory(category),new LinearLayout.LayoutParams(-1,-2));contentBuilt[0]=true;a.fillSettings(SettingsStore.load(a));}
            body.setVisibility(show?View.VISIBLE:View.GONE);
            arrow.setText(show?"\u25B4":"\u25BE");arrow.setContentDescription(show?"Thu gọn "+category:"Mở "+category);
            a.recordSettingsSectionState(category,show);
        });
        return section;
    }

    View categoryNavigation(){
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(a.dp(8),a.dp(10),a.dp(8),a.dp(10));
        list.addView(a.text("Cài đặt",21,a.TEXT,true),a.marginLP(-1,-2,0,4,0,12));
        for(String c:CATEGORIES)list.addView(categoryButton(c),a.marginLP(-1,a.dp(48),0,2,0,2));a.tint(list,a.PANEL,a.BORDER,1,0);return list;
    }

    Button categoryButton(String category){boolean active=category.equals(a.settingsCategory);Button b=a.button(SettingsCategoryModel.label(category),active?a.CARD:a.PANEL,active?a.TEXT:a.MUTED,v->a.switchSettingsCategory(category));b.setGravity(Gravity.CENTER_VERTICAL);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(a.dp(48));b.setMinimumHeight(a.dp(48));b.setElevation(0);b.setStateListAnimator(null);a.tint(b,active?a.CARD:a.PANEL,active?a.BLUE:a.PANEL,1,9);return b;}
    void showCategoryPicker(){String[] labels=new String[CATEGORIES.length];for(int i=0;i<CATEGORIES.length;i++)labels[i]=SettingsCategoryModel.label(CATEGORIES[i]);new android.app.AlertDialog.Builder(a).setTitle("Danh mục Cài đặt").setItems(labels,(d,w)->a.switchSettingsCategory(CATEGORIES[w])).show();}

    View pageHeading(){LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(14));box.addView(a.text(SettingsCategoryModel.label(a.settingsCategory),24,a.TEXT,true));TextView d=a.text(categoryDescription(a.settingsCategory),13,a.MUTED,false);d.setSingleLine(false);box.addView(d);return box;}
    String categoryDescription(String c){if(c.equals("Biên tập"))return "Mô hình, chi phí và các loại kiểm tra cho lượt Biên tập mới.";if(c.equals("General"))return "Ngôn ngữ, tên tệp, instruction và hành vi lượt dịch.";if(c.equals("Provider"))return "Provider, model, thông tin xác thực, mức suy luận và chi phí.";if(c.equals("Appearance"))return "Chọn giao diện, cỡ chữ và bố cục của toàn app.";return "Hiệu năng, chẩn đoán, log và thông tin bản dựng.";}

    View buildCategory(String c){c=SettingsCategoryModel.normalize(c);if(c.equals("Biên tập"))return EditorialSettingsPage.build(a);if(c.equals("Provider"))return provider();if(c.equals("Appearance"))return appearance();if(c.equals("Advanced"))return advanced();return general();}

    LinearLayout group(String title,String description){LinearLayout g=new LinearLayout(a);g.setOrientation(LinearLayout.VERTICAL);g.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(12));TextView t=a.text(title,16,a.TEXT,true);g.addView(t);if(description!=null&&!description.isEmpty()){TextView d=a.text(description,12,a.MUTED,false);d.setSingleLine(false);g.addView(d,a.marginLP(-1,-2,0,2,0,8));}a.tint(g,a.PANEL,a.BORDER,1,12);return g;}

    View general(){
        LinearLayout root=column();LinearLayout language=group("Ngôn ngữ và tệp","Thiết lập mặc định cho lượt dịch kế tiếp.");
        a.sourceField=prepareField(a.input("Japanese","Japanese"));a.targetField=prepareField(a.input("Vietnamese","Vietnamese"));
        language.addView(settingsPair(settingsFieldBlock("Ngôn ngữ nguồn",a.sourceField),settingsFieldBlock("Ngôn ngữ đích",a.targetField)));
        a.outputPatternField=prepareField(a.input("{originalName} ({targetLang}).{ext}","{originalName} ({targetLang}).{ext}"));language.addView(settingsFieldBlock("Tên tệp đầu ra",a.outputPatternField));root.addView(language);
        root.addView(recommendationCard(), a.marginLP(-1, -2, 0, 0, 0, 10));
        LinearLayout quick=group("Không gian làm việc","Các màn chính luôn có ở thanh điều hướng.");quick.addView(settingLink("Màn Dịch","Chọn tệp, cấu hình và bắt đầu một lượt","Translate"));quick.addView(settingLink("Thư viện","RAW, Glossary, Pronoun và Tập","RAW"));quick.addView(settingLink("Công việc","Theo dõi lượt đang chạy và lịch sử","Jobs"));root.addView(quick,a.marginLP(-1,-2,0,10,0,0));
        root.addView(translation(), a.marginLP(-1, -2, 0, 0, 0, 10));
        root.addView(prompt(), a.marginLP(-1, -2, 0, 0, 0, 10));
        return root;
    }

    View recommendationCard(){
        LinearLayout box=group("Cài đặt khuyến nghị", "Chỉ đề xuất trường an toàn cho runtime; không đụng model, key, Base URL, instruction, Glossary, Pronoun hay giới hạn tiền.");
        java.util.List<SettingsRecommendationModel.Item> items=SettingsRecommendationModel.diffs(a.collectSettings());
        TextView summary=a.text("Cài đặt khuyến nghị · "+items.size()+" mục khác",14,a.TEXT,true);summary.setSingleLine(false);box.addView(summary);
        LinearLayout table=new LinearLayout(a);table.setOrientation(LinearLayout.VERTICAL);table.setVisibility(View.GONE);
        if(items.isEmpty()){table.addView(a.text("Thiết lập hiện tại đã phù hợp.",13,a.GREEN,false));}
        else for(SettingsRecommendationModel.Item item:items){TextView row=a.text(item.key+"\nHiện tại: "+item.current+" · Khuyến nghị: "+item.recommended+"\n"+item.reason,12,a.TEXT,false);row.setSingleLine(false);row.setPadding(0,a.dp(6),0,a.dp(6));table.addView(row);}
        box.addView(table);
        LinearLayout actions=a.rowContainer();android.widget.Button toggle=a.secondaryButton("Xem bảng",null);toggle.setOnClickListener(v->{table.setVisibility(table.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);toggle.setText(table.getVisibility()==View.VISIBLE?"Thu gọn":"Xem bảng");});actions.addView(toggle,new LinearLayout.LayoutParams(0,a.dp(48),1));actions.addView(a.space(8,1));
        android.widget.Button apply=a.primaryButton("Áp dụng",v->a.applyRecommendedSettings());apply.setEnabled(!items.isEmpty());actions.addView(apply,new LinearLayout.LayoutParams(0,a.dp(48),1));box.addView(actions);
        boolean canUndo=RecommendationUndoModel.isEnabled(a.recommendationUndo!=null);
        android.widget.Button undo=a.secondaryButton("Hoàn tác",v->a.undoRecommendedSettings());
        a.setButtonEnabled(undo,canUndo);
        undo.setContentDescription(RecommendationUndoModel.contentDescription(canUndo));
        box.addView(undo,new LinearLayout.LayoutParams(-1,a.dp(48)));
        if(!canUndo){
            TextView hint=a.text(RecommendationUndoModel.helperText(false),12,a.MUTED,false);
            hint.setSingleLine(false);
            box.addView(hint,a.marginLP(-1,-2,0,4,0,0));
        }
        return box;
    }

    View translation(){LinearLayout root=column();LinearLayout behavior=group("Hành vi đầu ra","Áp dụng cho lượt mới; job đang chạy giữ ảnh chụp thiết lập lúc bắt đầu.");a.optimizationPresetField=prepareField(a.input("quality / balanced / economy / full","balanced"));makePicker(a.optimizationPresetField,v->showQualityPreset());behavior.addView(pickerBlock("MỨC CHẤT LƯỢNG",a.optimizationPresetField,v->showQualityPreset()));a.refineBox=a.check("Làm mượt sau khi dịch",false,"Chạy bước refinement trong YAML sau bản nháp.");a.bilingualBox=a.check("Đầu ra song ngữ",false,"Giữ nội dung gốc và bản dịch để đối chiếu.");a.partialBox=a.check("Lưu đầu ra từng phần",true,"Cập nhật an toàn sau mỗi chunk hoàn tất.");a.bindEstimateCheck(a.refineBox);a.bindEstimateCheck(a.bilingualBox);a.bindEstimateCheck(a.partialBox);behavior.addView(a.refineBox);behavior.addView(a.bilingualBox);behavior.addView(a.partialBox);root.addView(behavior);return root;}

    /** A reasoning model thinks inside the output budget; below 8192 a long chunk can be cut. Shown as advice only, the setting is never changed for the owner. */
    View maxOutputHint(){
        TextView hint=a.text("Gợi ý: model suy luận (gpt-5, o-series, R1…) dùng cả token đầu ra để suy nghĩ; nên đặt MAX OUTPUT 8192 để bản dịch dài không bị cắt. App không tự đổi giá trị của bạn.",12,a.MUTED,false);
        hint.setSingleLine(false);hint.setPadding(0,0,0,a.dp(8));return hint;
    }
    void showReasoningEffort(){String[] labels={"Tự động (thấp với model suy luận)","Tắt (không gửi)","Thấp","Vừa","Cao · có thể tốn hơn"};String[] values={"auto","off","low","medium","high"};new android.app.AlertDialog.Builder(a).setTitle("Mức suy luận khi dịch").setItems(labels,(d,w)->{if(w==4){new android.app.AlertDialog.Builder(a).setTitle("Bật mức Cao?").setMessage("Mức suy luận Cao có thể dùng nhiều token hơn và tăng chi phí lượt dịch.").setNegativeButton("Hủy",null).setPositiveButton("Bật",(x,y)->{a.reasoningEffortField.setText(values[w]);a.saveSettingsFromUi();}).show();}else{a.reasoningEffortField.setText(values[w]);a.saveSettingsFromUi();}}).show();}

    void showQualityPreset(){String[] labels={"Chất lượng cao","Cân bằng","Tiết kiệm","Đầy đủ"};String[] values={"quality","balanced","economy","full"};new android.app.AlertDialog.Builder(a).setTitle("Mức chất lượng").setItems(labels,(d,w)->{a.optimizationPresetField.setText(values[w]);a.saveSettingsFromUi();}).show();}

    View provider(){
        LinearLayout root=column();LinearLayout connection=group("Kết nối Provider","API key nằm trên thiết bị và chỉ dùng cho lượt bạn chủ động bắt đầu.");
        a.providerField=prepareField(a.input("AI provider","openrouter"));noSuggestions(a.providerField);makePicker(a.providerField,v->a.showProviderPicker());
        a.modelField=prepareField(a.input("Model","openai/gpt-5.4-mini"));noSuggestions(a.modelField);makePicker(a.modelField,v->a.showModelPicker());
        a.apiKeyField=prepareField(a.input("API key",""));a.apiKeyField.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        a.baseUrlField=prepareField(a.input("Base URL","https://openrouter.ai/api/v1/chat/completions"));noSuggestions(a.baseUrlField);
        connection.addView(pickerBlock("PROVIDER",a.providerField,v->a.showProviderPicker()));connection.addView(pickerBlock("MODEL",a.modelField,v->a.showModelPicker()));
        a.modelPriceLabel=a.text("Giá: —",12,a.MUTED,false);connection.addView(a.modelPriceLabel);
        LinearLayout keyBlock=settingsFieldBlock("API KEY",a.apiKeyField);a.apiKeyStatusLabel=a.text("Chưa lưu API key",12,a.MUTED,false);keyBlock.addView(a.apiKeyStatusLabel,a.marginLP(-1,-2,0,0,0,4));
        final boolean[] revealed={false};Button reveal=a.secondaryButton("Hiện",v->{revealed[0]=!revealed[0];a.apiKeyField.setInputType(InputType.TYPE_CLASS_TEXT|(revealed[0]?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD)|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);a.apiKeyField.setSelection(a.apiKeyField.length());((Button)v).setText(revealed[0]?"Ẩn":"Hiện");});keyBlock.addView(reveal,new LinearLayout.LayoutParams(-1,a.dp(48)));connection.addView(keyBlock);
        a.apiTestStatusLabel=a.text("Chưa kiểm tra API",12,a.MUTED,false);connection.addView(a.apiTestStatusLabel,a.marginLP(-1,-2,0,0,0,4));
        connection.addView(settingsFieldBlock("BASE URL",a.baseUrlField));
        LinearLayout actions=a.rowContainer();actions.addView(a.primaryButton("Lưu kết nối",v->a.saveSettingsFromUi()),new LinearLayout.LayoutParams(0,a.dp(48),1));actions.addView(a.space(8,1));actions.addView(a.secondaryButton("Kiểm tra API",v->a.quickTestApi()),new LinearLayout.LayoutParams(0,a.dp(48),1));connection.addView(actions);root.addView(connection);
        LinearLayout presets=group("Preset Provider","Chọn endpoint tương thích mà không đổi các thiết lập dịch khác.");LinearLayout row=a.rowContainer();row.addView(a.secondaryButton("OpenRouter",v->a.preset("openrouter")),new LinearLayout.LayoutParams(0,a.dp(48),1));row.addView(a.space(6,1));row.addView(a.secondaryButton("OpenAI",v->a.preset("openai")),new LinearLayout.LayoutParams(0,a.dp(48),1));row.addView(a.space(6,1));row.addView(a.secondaryButton("DeepSeek",v->a.preset("deepseek")),new LinearLayout.LayoutParams(0,a.dp(48),1));presets.addView(row);root.addView(presets,a.marginLP(-1,-2,0,10,0,0));
        LinearLayout diagnostics=group("Chẩn đoán model","Tương thích, giá và tokenizer. API key cùng văn bản riêng không được đưa vào kết quả.");
        root.addView(runtimeLimits(),a.marginLP(-1,-2,0,0,0,10));
        a.modelDiagnosticsText=a.text(a.buildModelDiagnostics(),12,a.TEXT,false);a.modelDiagnosticsText.setTypeface(android.graphics.Typeface.MONOSPACE);a.modelDiagnosticsText.setTextIsSelectable(true);a.modelDiagnosticsText.setSingleLine(false);diagnostics.addView(a.modelDiagnosticsText);
        LinearLayout diagnosticsActions=a.rowContainer();diagnosticsActions.addView(a.secondaryButton("Làm mới catalog",v->a.refreshModelCatalog()),new LinearLayout.LayoutParams(0,a.dp(48),1));diagnosticsActions.addView(a.space(6,1));diagnosticsActions.addView(a.secondaryButton("Tính lại",v->a.updateInputEstimate()),new LinearLayout.LayoutParams(0,a.dp(48),1));diagnosticsActions.addView(a.space(6,1));diagnosticsActions.addView(a.secondaryButton("Sao chép",v->a.copyModelDiagnostics()),new LinearLayout.LayoutParams(0,a.dp(48),1));diagnostics.addView(diagnosticsActions,a.marginLP(-1,-2,0,8,0,0));root.addView(diagnostics,a.marginLP(-1,-2,0,10,0,0));return root;
    }

    View prompt(){LinearLayout root=column();LinearLayout files=group("Instruction và profile","Chọn instruction YAML hoặc xuất profile để sao lưu cấu hình không chứa API key.");a.yamlFileLabel=a.text("Instruction YAML: chưa chọn",13,a.MUTED,false);files.addView(settingAction("Instruction YAML",a.yamlFileLabel,"Chọn",v->a.chooseYaml()));files.addView(a.secondaryButton("Export profile",v->a.exportProfile()),new LinearLayout.LayoutParams(-1,a.dp(48)));root.addView(files);return root;}

    View advanced(){LinearLayout root=column();root.addView(performance(),a.marginLP(-1,-2,0,0,0,10));root.addView(logging(),a.marginLP(-1,-2,0,0,0,10));root.addView(about());return root;}

    View performance(){
        LinearLayout root=column();LinearLayout chunk=group("Chunk","Giới hạn theo token/ký tự và quy tắc chèn; các giá trị tự lưu khi sửa.");
        a.chunkModeField=prepareField(a.input("token hoặc ký tự","token"));a.maxTokensField=prepareField(a.number("Số token/chunk","450"));a.maxCharsField=prepareField(a.number("Số ký tự/chunk","0"));a.softRatioField=prepareField(a.decimal("Tỷ lệ mềm","0.8"));a.contextField=prepareField(a.number("Ngữ cảnh (ký tự)","400"));a.tempField=prepareField(a.decimal("Nhiệt độ","0.3"));a.maxOutputField=prepareField(a.number("MAX OUTPUT TOKENS","4096"));a.glossaryLimitField=prepareField(a.number("Số dòng Glossary/chunk","80"));a.pronounLimitField=prepareField(a.number("Số quy tắc Pronoun/chunk","40"));
        a.timeoutField=null;a.attemptsField=null;a.initialRetryDelayField=null;a.maxRetryDelayField=null;a.costLimitField=null;a.costWarningField=null;a.maxRetryCostField=null;a.maxPaidRetriesField=null;a.costLimitBox=null;a.unlimitedCostBox=null;a.retryEmptyBox=null;a.retryTruncationBox=null;a.retryValidationBox=null;a.stopUnknownPricingBox=null;a.reasoningEffortField=null;
        chunk.addView(settingsPair(settingsFieldBlock("Chế độ chunk",a.chunkModeField),settingsFieldBlock("Tỷ lệ mềm",a.softRatioField)));chunk.addView(settingsPair(settingsFieldBlock("Token/chunk",a.maxTokensField),settingsFieldBlock("Ký tự/chunk",a.maxCharsField)));chunk.addView(settingsPair(settingsFieldBlock("Ngữ cảnh (ký tự)",a.contextField),settingsFieldBlock("MAX OUTPUT",a.maxOutputField)));chunk.addView(maxOutputHint());chunk.addView(settingsPair(settingsFieldBlock("Nhiệt độ",a.tempField),settingsFieldBlock("Glossary/chunk",a.glossaryLimitField)));chunk.addView(settingsFieldBlock("Pronoun/chunk",a.pronounLimitField));a.overlapBox=a.check("Ngữ cảnh chồng lấn",false,"Chỉ dùng làm ngữ cảnh tham khảo.");a.bindEstimateCheck(a.overlapBox);chunk.addView(a.overlapBox);chunk.addView(a.text("Đã lưu tự động khi sửa",12,a.GREEN,false),new LinearLayout.LayoutParams(-1,a.dp(32)));root.addView(chunk);return root;
    }

    View runtimeLimits(){
        LinearLayout root=column();a.overlapBox=null;LinearLayout retry=group("Gọi lại và thời gian chờ","Tự lưu khi sửa; đơn vị thời gian là giây hoặc mili-giây.");
        a.timeoutField=prepareField(a.number("Thời gian chờ (giây)","300"));a.attemptsField=prepareField(a.number("Số lần thử tối đa","3"));a.initialRetryDelayField=prepareField(a.number("Độ trễ gọi lại (ms)","1000"));a.maxRetryDelayField=prepareField(a.number("Độ trễ tối đa (ms)","60000"));a.reasoningEffortField=prepareField(a.input("tự động / tắt / thấp / vừa / cao","auto"));makePicker(a.reasoningEffortField,v->showReasoningEffort());
        retry.addView(settingsPair(settingsFieldBlock("Thời gian chờ (giây)",a.timeoutField),settingsFieldBlock("Số lần thử tối đa",a.attemptsField)));retry.addView(settingsPair(settingsFieldBlock("Độ trễ gọi lại (ms)",a.initialRetryDelayField),settingsFieldBlock("Độ trễ tối đa (ms)",a.maxRetryDelayField)));
        a.retryEmptyBox=a.check("Gọi lại khi phản hồi rỗng",true,"");a.retryTruncationBox=a.check("Gọi lại khi có dấu hiệu bị cắt",true,"");a.retryValidationBox=a.check("Gọi lại khi kiểm tra thất bại",true,"");a.bindEstimateCheck(a.retryEmptyBox);a.bindEstimateCheck(a.retryTruncationBox);a.bindEstimateCheck(a.retryValidationBox);retry.addView(pickerBlock("MỨC SUY LUẬN",a.reasoningEffortField,v->showReasoningEffort()));retry.addView(a.retryEmptyBox);retry.addView(a.retryTruncationBox);retry.addView(a.retryValidationBox);root.addView(retry);
        LinearLayout cost=group("Chi phí","0 dùng trần mặc định an toàn; bật Không giới hạn chỉ sau khi xác nhận.");a.costLimitField=prepareField(a.decimal("Trần chi phí (USD)","0"));a.costWarningField=prepareField(a.decimal("Cảnh báo chi phí (USD)","0"));a.maxRetryCostField=prepareField(a.decimal("Trần gọi lại (USD, chưa có tác dụng)","0"));a.maxPaidRetriesField=prepareField(a.number("Số lần gọi lại trả tiền (chưa có tác dụng)","0"));cost.addView(settingsPair(settingsFieldBlock("Trần chi phí (USD)",a.costLimitField),settingsFieldBlock("Cảnh báo chi phí (USD)",a.costWarningField)));cost.addView(settingsPair(settingsFieldBlock("Trần gọi lại (USD, chưa có tác dụng)",a.maxRetryCostField),settingsFieldBlock("Số lần gọi lại trả tiền (chưa có tác dụng)",a.maxPaidRetriesField)));
        a.costLimitBox=a.check("Dừng khi chạm trần chi phí",false,"Dừng trước chunk kế tiếp khi đã chạm trần.");a.bindEstimateCheck(a.costLimitBox);cost.addView(a.costLimitBox);a.unlimitedCostBox=a.check("Không giới hạn",false,"Bỏ trần an toàn cho lượt này; chỉ bật sau khi xác nhận.");a.unlimitedCostBox.setOnCheckedChangeListener((button,checked)->{if(a.hydratingSettings)return;if(checked)new android.app.AlertDialog.Builder(a).setTitle("Bỏ trần chi phí?").setMessage("Lượt dịch có thể phát sinh chi phí không giới hạn.").setNegativeButton("Hủy",(d,w)->{a.hydratingSettings=true;button.setChecked(false);a.hydratingSettings=false;}).setPositiveButton("Xác nhận",(d,w)->a.saveSettingsFromUi()).show();else a.persistUiSettingsAsync();});cost.addView(a.unlimitedCostBox);a.stopUnknownPricingBox=a.check("Dừng khi chưa biết giá",false,"");a.bindEstimateCheck(a.stopUnknownPricingBox);cost.addView(a.stopUnknownPricingBox);cost.addView(a.text("Đã lưu tự động khi sửa",12,a.GREEN,false),new LinearLayout.LayoutParams(-1,a.dp(32)));root.addView(cost);return root;
    }

    View appearance(){LinearLayout root=column();return new AppearancePage(a).build(root,(title,description)->group(title,description));}
    View paletteCell(UiAppearance.Palette p){boolean selected=a.appearance!=null&&a.appearance.id.equals(p.id);LinearLayout cell=new LinearLayout(a);cell.setOrientation(LinearLayout.VERTICAL);cell.setPadding(a.dp(10),a.dp(9),a.dp(10),a.dp(9));TextView swatch=a.text("",1,p.accent,false);a.tint(swatch,p.card,p.accent,selected?2:1,8);cell.addView(swatch,new LinearLayout.LayoutParams(-1,a.dp(34)));TextView name=a.text((selected?"✓  ":"")+p.name,12,selected?a.TEXT:a.MUTED,selected);name.setGravity(Gravity.CENTER);cell.addView(name,new LinearLayout.LayoutParams(-1,a.dp(34)));a.tint(cell,p.panel,selected?a.BLUE:a.BORDER,selected?2:1,10);cell.setOnClickListener(v->a.selectAppearance(p.id));return cell;}
    class ListBuilder{final LinearLayout parent;final int columns;LinearLayout row;int count;ListBuilder(LinearLayout p,int c){parent=p;columns=c;}void add(View v){if(count%columns==0){row=a.rowContainer();parent.addView(row,a.marginLP(-1,-2,0,4,0,4));}if(row.getChildCount()>0)row.addView(a.space(6,1));row.addView(v,new LinearLayout.LayoutParams(0,a.dp(86),1));count++;}void finish(){if(row!=null)while((row.getChildCount()+1)/2<columns){row.addView(a.space(6,1));row.addView(new View(a),new LinearLayout.LayoutParams(0,a.dp(86),1));}}}

    View logging(){LinearLayout root=column();LinearLayout local=group("Log và chẩn đoán","Log chỉ ở trên thiết bị nếu bạn không chủ động xuất.");local.addView(a.secondaryButton("Xuất toàn bộ log",v->a.exportLog()),new LinearLayout.LayoutParams(-1,a.dp(48)));local.addView(a.dangerButton("Xóa log runtime",v->a.clearRuntimeLog()),a.marginLP(-1,a.dp(48),0,8,0,0));root.addView(local);LinearLayout developer=group("Công cụ phát triển","Prompt dump, log runtime, benchmark và kiểm tra chunk chỉ được tạo khi bạn mở.");developer.addView(a.secondaryButton("Mở công cụ phát triển",v->a.openDeveloperTools()),new LinearLayout.LayoutParams(-1,a.dp(48)));root.addView(developer,a.marginLP(-1,-2,0,10,0,0));return root;}

    View about(){LinearLayout root=column();LinearLayout app=group("Translate Books với LLM",AppBuildInfo.RELEASE_LABEL);TextView version=a.text("Bản "+AppBuildInfo.exportVersionLine(),14,a.TEXT,true);app.addView(version);app.addView(a.text("Android "+android.os.Build.VERSION.RELEASE+" • API "+android.os.Build.VERSION.SDK_INT,12,a.MUTED,false));TextView capabilities=a.text("Khả năng của Provider được đọc theo adapter. Dịch, checkpoint và benchmark vẫn do service quản lý.",12,a.MUTED,false);capabilities.setSingleLine(false);app.addView(capabilities,a.marginLP(-1,-2,0,8,0,0));root.addView(app);return root;}

    LinearLayout column(){LinearLayout c=new LinearLayout(a);c.setOrientation(LinearLayout.VERTICAL);return c;}
    <T extends android.widget.EditText> T prepareField(T field){field.setSingleLine(true);field.setVerticalScrollBarEnabled(false);field.setNestedScrollingEnabled(false);field.setMinimumHeight(a.dp(48));a.bindEstimateInput(field);return field;}
    LinearLayout settingsFieldBlock(String label,android.widget.EditText field){
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);TextView l=a.text(label,11,a.MUTED,true);l.setLetterSpacing(0.12f);l.setSingleLine(false);box.addView(l,a.marginLP(-1,-2,0,0,0,5));
        field.setMinHeight(a.dp(48));box.addView(field,new LinearLayout.LayoutParams(-1,-2));box.setPadding(0,0,0,a.dp(10));return box;
    }
    View settingsPair(View left,View right){
        int width=a.getResources().getConfiguration().screenWidthDp;float scale=a.getResources().getConfiguration().fontScale;
        if(!SettingsNavigationPolicy.shouldStackFields(width,scale))return a.twoFields(left,right);
        LinearLayout stack=new LinearLayout(a);stack.setOrientation(LinearLayout.VERTICAL);stack.addView(left,new LinearLayout.LayoutParams(-1,-2));stack.addView(right,new LinearLayout.LayoutParams(-1,-2));return stack;
    }
    void noSuggestions(android.widget.EditText field){field.setInputType(field.getInputType()|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);}
    void makePicker(android.widget.EditText field,View.OnClickListener listener){field.setFocusable(false);field.setFocusableInTouchMode(false);field.setClickable(true);field.setOnClickListener(listener);}
    View pickerBlock(String label,android.widget.EditText field,View.OnClickListener listener){LinearLayout block=settingsFieldBlock(label,field);block.setClickable(true);block.setOnClickListener(listener);return block;}
    View settingLink(String title,String description,String tab){LinearLayout row=settingRow(title,description);row.setMinimumHeight(a.dp(48));row.setOnClickListener(v->a.switchTab(tab));TextView arrow=a.text("›",22,a.MUTED,false);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));return row;}
    View settingAction(String title,TextView value,String action,View.OnClickListener listener){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,a.dp(6),0,a.dp(6));LinearLayout top=a.rowContainer();LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);text.addView(a.text(title,14,a.TEXT,true));text.addView(value);top.addView(text,new LinearLayout.LayoutParams(0,-2,1));top.addView(a.secondaryButton(action,listener),new LinearLayout.LayoutParams(a.dp(104),a.dp(48)));row.addView(top);return row;}
    LinearLayout settingRow(String title,String description){LinearLayout row=a.rowContainer();row.setPadding(a.dp(4),a.dp(6),a.dp(4),a.dp(6));LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);text.addView(a.text(title,14,a.TEXT,true));TextView d=a.text(description,12,a.MUTED,false);d.setSingleLine(false);text.addView(d);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));return row;}
}
