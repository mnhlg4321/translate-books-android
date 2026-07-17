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
    private static final String[] CATEGORIES={"General","Translation","Provider","Prompt","Performance","Appearance","Logging","About"};
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
        heading.addView(a.text("Settings",24,a.TEXT,true));
        TextView summary=a.text("App, translation and provider preferences. Tap a section to expand it.",13,a.MUTED,false);summary.setSingleLine(false);heading.addView(summary);
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
        TextView icon=a.text(categoryIcon(category),19,expanded?a.BLUE:a.MUTED,true);icon.setGravity(Gravity.CENTER);header.addView(icon,new LinearLayout.LayoutParams(a.dp(36),a.dp(48)));
        LinearLayout titles=new LinearLayout(a);titles.setOrientation(LinearLayout.VERTICAL);TextView title=a.text(category,16,a.TEXT,true);TextView description=a.text(categoryDescription(category),12,a.MUTED,false);description.setSingleLine(false);titles.addView(title);titles.addView(description);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=a.text(expanded?"▴":"▾",16,a.MUTED,true);arrow.setGravity(Gravity.CENTER);arrow.setContentDescription(expanded?"Collapse "+category:"Expand "+category);header.addView(arrow,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));
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
            if(show&&!contentBuilt[0]){bodyContent.addView(buildCategory(category),new LinearLayout.LayoutParams(-1,-2));contentBuilt[0]=true;}
            body.setVisibility(show?View.VISIBLE:View.GONE);
            icon.setTextColor(show?a.BLUE:a.MUTED);arrow.setText(show?"\u25B4":"\u25BE");arrow.setContentDescription(show?"Collapse "+category:"Expand "+category);
            a.recordSettingsSectionState(category,show);
        });
        return section;
    }

    View categoryNavigation(){
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(a.dp(8),a.dp(10),a.dp(8),a.dp(10));
        list.addView(a.text("Settings",21,a.TEXT,true),a.marginLP(-1,-2,0,4,0,12));
        for(String c:CATEGORIES)list.addView(categoryButton(c),a.marginLP(-1,a.dp(48),0,2,0,2));a.tint(list,a.PANEL,a.BORDER,1,0);return list;
    }

    Button categoryButton(String category){boolean active=category.equals(a.settingsCategory);Button b=a.button(categoryIcon(category)+"  "+category,active?a.CARD:a.PANEL,active?a.TEXT:a.MUTED,v->a.switchSettingsCategory(category));b.setGravity(Gravity.CENTER_VERTICAL);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setElevation(0);b.setStateListAnimator(null);a.tint(b,active?a.CARD:a.PANEL,active?a.BLUE:a.PANEL,1,9);return b;}
    void showCategoryPicker(){String[] labels=new String[CATEGORIES.length];for(int i=0;i<CATEGORIES.length;i++)labels[i]=categoryIcon(CATEGORIES[i])+"  "+CATEGORIES[i];new android.app.AlertDialog.Builder(a).setTitle("Settings category").setItems(labels,(d,w)->a.switchSettingsCategory(CATEGORIES[w])).show();}
    String categoryIcon(String c){if(c.equals("General"))return "⌂";if(c.equals("Translation"))return "文";if(c.equals("Provider"))return "◎";if(c.equals("Prompt"))return "{}";if(c.equals("Performance"))return "⚡";if(c.equals("Appearance"))return "◉";if(c.equals("Logging"))return "≡";return "ⓘ";}

    View pageHeading(){LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(a.dp(8),a.dp(4),a.dp(8),a.dp(14));box.addView(a.text(a.settingsCategory,24,a.TEXT,true));TextView d=a.text(categoryDescription(a.settingsCategory),13,a.MUTED,false);d.setSingleLine(false);box.addView(d);return box;}
    String categoryDescription(String c){if(c.equals("General"))return "Language, output naming and everyday defaults.";if(c.equals("Translation"))return "Quality and output behavior for new jobs.";if(c.equals("Provider"))return "Model connection, credential, pricing and endpoint.";if(c.equals("Prompt"))return "Instruction YAML, environment profiles and prompt inspection.";if(c.equals("Performance"))return "Chunk, context, retry and cost safeguards.";if(c.equals("Appearance"))return "Choose a workspace palette. Status colors keep their semantic meaning.";if(c.equals("Logging"))return "Local diagnostics, export and privacy controls.";return "Build, capabilities and release information.";}

    View buildCategory(String c){if(c.equals("Translation"))return translation();if(c.equals("Provider"))return provider();if(c.equals("Prompt"))return prompt();if(c.equals("Performance"))return performance();if(c.equals("Appearance"))return appearance();if(c.equals("Logging"))return logging();if(c.equals("About"))return about();return general();}

    LinearLayout group(String title,String description){LinearLayout g=new LinearLayout(a);g.setOrientation(LinearLayout.VERTICAL);g.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(12));TextView t=a.text(title,16,a.TEXT,true);g.addView(t);if(description!=null&&!description.isEmpty()){TextView d=a.text(description,12,a.MUTED,false);d.setSingleLine(false);g.addView(d,a.marginLP(-1,-2,0,2,0,8));}a.tint(g,a.PANEL,a.BORDER,1,12);return g;}

    View general(){
        LinearLayout root=column();LinearLayout language=group("Language & files","Defaults used by the next translation job.");
        a.sourceField=prepareField(a.input("Japanese","Japanese"));a.targetField=prepareField(a.input("Vietnamese","Vietnamese"));
        language.addView(settingsPair(settingsFieldBlock("SOURCE LANGUAGE",a.sourceField),settingsFieldBlock("TARGET LANGUAGE",a.targetField)));
        a.outputPatternField=prepareField(a.input("{originalName} ({targetLang}).{ext}","{originalName} ({targetLang}).{ext}"));language.addView(settingsFieldBlock("OUTPUT FILE NAME",a.outputPatternField));root.addView(language);
        LinearLayout quick=group("Workspace","Common destinations remain available from the primary navigation.");quick.addView(settingLink("Translation workspace","Select input, configuration and start a job","Translate"));quick.addView(settingLink("Translation library","Files, glossaries, pronouns and samples","Files"));root.addView(quick,a.marginLP(-1,-2,0,10,0,0));return root;
    }

    View translation(){LinearLayout root=column();LinearLayout behavior=group("Output behavior","Changes apply to new jobs; an active job keeps its snapshot.");a.optimizationPresetField=prepareField(a.input("quality / balanced / economy / full","balanced"));makePicker(a.optimizationPresetField,v->showQualityPreset());behavior.addView(pickerBlock("QUALITY PRESET",a.optimizationPresetField,v->showQualityPreset()));a.refineBox=a.check("Refine after translation",false,"Run the YAML refinement phase after the draft.");a.bilingualBox=a.check("Bilingual output",false,"Write source and translation together for review.");a.partialBox=a.check("Save partial output",true,"Update the output safely after every completed chunk.");a.bindEstimateCheck(a.refineBox);a.bindEstimateCheck(a.bilingualBox);a.bindEstimateCheck(a.partialBox);behavior.addView(a.refineBox);behavior.addView(a.bilingualBox);behavior.addView(a.partialBox);root.addView(behavior);return root;}

    void showQualityPreset(){String[] labels={"High quality","Balanced","Economy","Full baseline"};String[] values={"quality","balanced","economy","full"};new android.app.AlertDialog.Builder(a).setTitle("Quality preset").setItems(labels,(d,w)->{a.optimizationPresetField.setText(values[w]);a.saveSettingsFromUi();}).show();}

    View provider(){
        LinearLayout root=column();LinearLayout connection=group("Provider connection","Credential stays on this device and is used only for requests you start.");
        a.providerField=prepareField(a.input("AI provider","openrouter"));makePicker(a.providerField,v->a.showProviderPicker());a.modelField=prepareField(a.input("Model","openai/gpt-5.4-mini"));makePicker(a.modelField,v->a.showModelPicker());
        a.apiKeyField=prepareField(a.input("API key",""));a.apiKeyField.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);a.baseUrlField=prepareField(a.input("Base URL","https://openrouter.ai/api/v1/chat/completions"));
        connection.addView(pickerBlock("PROVIDER",a.providerField,v->a.showProviderPicker()));connection.addView(pickerBlock("MODEL",a.modelField,v->a.showModelPicker()));a.modelPriceLabel=a.text("Price: —",12,a.MUTED,false);connection.addView(a.modelPriceLabel);connection.addView(settingsFieldBlock("API KEY",a.apiKeyField));connection.addView(settingsFieldBlock("ENDPOINT",a.baseUrlField));
        LinearLayout actions=a.rowContainer();actions.addView(a.primaryButton("Save connection",v->a.saveSettingsFromUi()),new LinearLayout.LayoutParams(0,a.dp(48),1));actions.addView(a.space(8,1));actions.addView(a.secondaryButton("Test API",v->a.quickTestApi()),new LinearLayout.LayoutParams(0,a.dp(48),1));connection.addView(actions);root.addView(connection);
        LinearLayout diagnostics=group("Model diagnostics","Compatibility, pricing and tokenizer state. API keys and private text are never included.");
        a.modelDiagnosticsText=a.text(a.buildModelDiagnostics(),12,a.TEXT,false);a.modelDiagnosticsText.setTypeface(android.graphics.Typeface.MONOSPACE);a.modelDiagnosticsText.setTextIsSelectable(true);a.modelDiagnosticsText.setSingleLine(false);diagnostics.addView(a.modelDiagnosticsText);
        LinearLayout diagnosticsActions=a.rowContainer();diagnosticsActions.addView(a.secondaryButton("Refresh catalog",v->a.refreshModelCatalog()),new LinearLayout.LayoutParams(0,a.dp(46),1));diagnosticsActions.addView(a.space(6,1));diagnosticsActions.addView(a.secondaryButton("Recalculate",v->a.updateInputEstimate()),new LinearLayout.LayoutParams(0,a.dp(46),1));diagnosticsActions.addView(a.space(6,1));diagnosticsActions.addView(a.secondaryButton("Copy",v->a.copyModelDiagnostics()),new LinearLayout.LayoutParams(0,a.dp(46),1));diagnostics.addView(diagnosticsActions,a.marginLP(-1,-2,0,8,0,0));root.addView(diagnostics,a.marginLP(-1,-2,0,10,0,0));
        LinearLayout presets=group("Provider presets","Apply a compatible endpoint without changing unrelated translation settings.");LinearLayout row=a.rowContainer();row.addView(a.secondaryButton("OpenRouter",v->a.preset("openrouter")),new LinearLayout.LayoutParams(0,a.dp(48),1));row.addView(a.space(6,1));row.addView(a.secondaryButton("OpenAI",v->a.preset("openai")),new LinearLayout.LayoutParams(0,a.dp(48),1));row.addView(a.space(6,1));row.addView(a.secondaryButton("DeepSeek",v->a.preset("deepseek")),new LinearLayout.LayoutParams(0,a.dp(48),1));presets.addView(row);root.addView(presets,a.marginLP(-1,-2,0,10,0,0));return root;
    }

    View prompt(){LinearLayout root=column();LinearLayout files=group("Instruction & profiles","Inspect or replace prompt inputs without searching through performance settings.");a.yamlFileLabel=a.text("Instruction YAML: not selected",13,a.MUTED,false);a.envFileLabel=a.text("Environment profile: not selected",13,a.MUTED,false);files.addView(settingAction("Instruction YAML",a.yamlFileLabel,"Choose",v->a.chooseYaml()));PronounStore.Profile activePronoun=PronounStore.selected(a);TextView pronoun=a.text(activePronoun==null?"Active pronoun: none":"Active pronoun: "+activePronoun.name+" • "+activePronoun.count()+" rules",13,activePronoun==null?a.MUTED:a.GREEN,false);files.addView(settingAction("Pronoun profiles",pronoun,"Manage",v->a.switchTab("Pronouns")));files.addView(settingAction("Environment profile",a.envFileLabel,"Import",v->a.chooseEnv()));LinearLayout row=a.rowContainer();row.addView(a.secondaryButton("Prompt preview",v->a.showPromptPreview()),new LinearLayout.LayoutParams(0,a.dp(48),1));row.addView(a.space(8,1));row.addView(a.secondaryButton("Export profile",v->a.exportProfile()),new LinearLayout.LayoutParams(0,a.dp(48),1));files.addView(row);root.addView(files);return root;}

    View performance(){
        LinearLayout root=column();LinearLayout safe=group("Runtime safeguards","Technical controls are grouped away from everyday translation choices.");
        a.chunkModeField=prepareField(a.input("token or char","token"));a.maxTokensField=prepareField(a.number("MAX_TOKENS_PER_CHUNK","450"));a.maxCharsField=prepareField(a.number("MAX_CHARS_PER_CHUNK","0"));a.softRatioField=prepareField(a.decimal("SOFT_LIMIT_RATIO","0.8"));a.contextField=prepareField(a.number("CONTEXT_CHARS","400"));a.timeoutField=prepareField(a.number("REQUEST_TIMEOUT","300"));a.attemptsField=prepareField(a.number("MAX_ATTEMPTS","3"));a.tempField=prepareField(a.decimal("TEMPERATURE","0.3"));a.maxOutputField=prepareField(a.number("MAX_OUTPUT_TOKENS","4096"));a.costLimitField=prepareField(a.decimal("COST LIMIT USD","0"));a.glossaryLimitField=prepareField(a.number("Glossary rows/chunk","80"));a.pronounLimitField=prepareField(a.number("Pronoun rules/chunk","40"));
        a.initialRetryDelayField=prepareField(a.number("INITIAL RETRY MS","1000"));a.maxRetryDelayField=prepareField(a.number("MAX RETRY MS","60000"));a.costWarningField=prepareField(a.decimal("COST WARNING USD","0"));a.maxRetryCostField=prepareField(a.decimal("MAX RETRY COST","0"));a.maxPaidRetriesField=prepareField(a.number("MAX PAID RETRIES","0"));
        safe.addView(settingsPair(settingsFieldBlock("CHUNK MODE",a.chunkModeField),settingsFieldBlock("SOFT LIMIT",a.softRatioField)));safe.addView(settingsPair(settingsFieldBlock("MAX TOKENS",a.maxTokensField),settingsFieldBlock("MAX CHARS",a.maxCharsField)));safe.addView(settingsPair(settingsFieldBlock("CONTEXT",a.contextField),settingsFieldBlock("MAX OUTPUT",a.maxOutputField)));safe.addView(settingsPair(settingsFieldBlock("TIMEOUT",a.timeoutField),settingsFieldBlock("RETRY",a.attemptsField)));safe.addView(settingsPair(settingsFieldBlock("TEMPERATURE",a.tempField),settingsFieldBlock("COST LIMIT USD",a.costLimitField)));safe.addView(settingsPair(settingsFieldBlock("GLOSSARY LIMIT",a.glossaryLimitField),settingsFieldBlock("PRONOUN LIMIT",a.pronounLimitField)));
        safe.addView(settingsPair(settingsFieldBlock("INITIAL RETRY DELAY",a.initialRetryDelayField),settingsFieldBlock("MAX RETRY DELAY",a.maxRetryDelayField)));safe.addView(settingsPair(settingsFieldBlock("COST WARNING",a.costWarningField),settingsFieldBlock("MAX RETRY COST",a.maxRetryCostField)));safe.addView(settingsFieldBlock("MAX PAID RETRIES (0 = unlimited)",a.maxPaidRetriesField));
        a.costLimitBox=a.check("Stop on cost limit",false,"Stop before starting the next chunk after reaching the limit.");a.bindEstimateCheck(a.costLimitBox);safe.addView(a.costLimitBox);a.overlapBox=a.check("Context overlap",false,"Reference-only context; never assembled into output.");safe.addView(a.overlapBox);a.retryEmptyBox=a.check("Retry empty responses",true,"");a.retryTruncationBox=a.check("Retry likely truncation",true,"");a.retryValidationBox=a.check("Retry validation failures",true,"");a.stopUnknownPricingBox=a.check("Stop if pricing becomes unknown",false,"");safe.addView(a.retryEmptyBox);safe.addView(a.retryTruncationBox);safe.addView(a.retryValidationBox);safe.addView(a.stopUnknownPricingBox);safe.addView(a.primaryButton("Save performance settings",v->a.saveSettingsFromUi()),new LinearLayout.LayoutParams(-1,a.dp(48)));root.addView(safe);return root;
    }

    View appearance(){LinearLayout root=column();LinearLayout palettes=group("Workspace color","Color identifies the workspace; green, amber and red remain reserved for status.");int columns=a.isWideLayout()?3:2;ListBuilder builder=new ListBuilder(palettes,columns);for(UiAppearance.Palette p:UiAppearance.all())builder.add(paletteCell(p));builder.finish();root.addView(palettes);return root;}
    View paletteCell(UiAppearance.Palette p){boolean selected=a.appearance!=null&&a.appearance.id.equals(p.id);LinearLayout cell=new LinearLayout(a);cell.setOrientation(LinearLayout.VERTICAL);cell.setPadding(a.dp(10),a.dp(9),a.dp(10),a.dp(9));TextView swatch=a.text("",1,p.accent,false);a.tint(swatch,p.card,p.accent,selected?2:1,8);cell.addView(swatch,new LinearLayout.LayoutParams(-1,a.dp(34)));TextView name=a.text((selected?"✓  ":"")+p.name,12,selected?a.TEXT:a.MUTED,selected);name.setGravity(Gravity.CENTER);cell.addView(name,new LinearLayout.LayoutParams(-1,a.dp(34)));a.tint(cell,p.panel,selected?a.BLUE:a.BORDER,selected?2:1,10);cell.setOnClickListener(v->a.selectAppearance(p.id));return cell;}
    class ListBuilder{final LinearLayout parent;final int columns;LinearLayout row;int count;ListBuilder(LinearLayout p,int c){parent=p;columns=c;}void add(View v){if(count%columns==0){row=a.rowContainer();parent.addView(row,a.marginLP(-1,-2,0,4,0,4));}if(row.getChildCount()>0)row.addView(a.space(6,1));row.addView(v,new LinearLayout.LayoutParams(0,a.dp(86),1));count++;}void finish(){if(row!=null)while((row.getChildCount()+1)/2<columns){row.addView(a.space(6,1));row.addView(new View(a),new LinearLayout.LayoutParams(0,a.dp(86),1));}}}

    View logging(){LinearLayout root=column();LinearLayout local=group("Logging & diagnostics","Logs stay local unless you explicitly export them.");local.addView(a.secondaryButton("Export all logs",v->a.exportLog()),new LinearLayout.LayoutParams(-1,a.dp(48)));local.addView(a.dangerButton("Clear runtime logs",v->a.clearRuntimeLog()),a.marginLP(-1,a.dp(48),0,8,0,0));root.addView(local);LinearLayout developer=group("Developer tools","Prompt dumps, runtime logs, benchmarks and chunk inspection. Content is created only after you open it.");developer.addView(a.secondaryButton("Open Developer tools",v->a.openDeveloperTools()),new LinearLayout.LayoutParams(-1,a.dp(48)));root.addView(developer,a.marginLP(-1,-2,0,10,0,0));return root;}

    View about(){LinearLayout root=column();LinearLayout app=group("Translate Books with LLMs",AppBuildInfo.RELEASE_LABEL);TextView version=a.text("Version "+AppBuildInfo.exportVersionLine(),14,a.TEXT,true);app.addView(version);app.addView(a.text("Android "+android.os.Build.VERSION.RELEASE+" • API "+android.os.Build.VERSION.SDK_INT,12,a.MUTED,false));TextView capabilities=a.text("Provider capabilities are detected per adapter. Translation, checkpoints and benchmarks remain service-owned.",12,a.MUTED,false);capabilities.setSingleLine(false);app.addView(capabilities,a.marginLP(-1,-2,0,8,0,0));root.addView(app);return root;}

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
    void makePicker(android.widget.EditText field,View.OnClickListener listener){field.setFocusable(false);field.setFocusableInTouchMode(false);field.setClickable(true);field.setOnClickListener(listener);}
    View pickerBlock(String label,android.widget.EditText field,View.OnClickListener listener){LinearLayout block=settingsFieldBlock(label,field);block.setClickable(true);block.setOnClickListener(listener);return block;}
    View settingLink(String title,String description,String tab){LinearLayout row=settingRow(title,description);row.setMinimumHeight(a.dp(48));row.setOnClickListener(v->a.switchTab(tab));TextView arrow=a.text("›",22,a.MUTED,false);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));return row;}
    View settingAction(String title,TextView value,String action,View.OnClickListener listener){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,a.dp(6),0,a.dp(6));LinearLayout top=a.rowContainer();LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);text.addView(a.text(title,14,a.TEXT,true));text.addView(value);top.addView(text,new LinearLayout.LayoutParams(0,-2,1));top.addView(a.secondaryButton(action,listener),new LinearLayout.LayoutParams(a.dp(104),a.dp(48)));row.addView(top);return row;}
    LinearLayout settingRow(String title,String description){LinearLayout row=a.rowContainer();row.setPadding(a.dp(4),a.dp(6),a.dp(4),a.dp(6));LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);text.addView(a.text(title,14,a.TEXT,true));TextView d=a.text(description,12,a.MUTED,false);d.setSingleLine(false);text.addView(d);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));return row;}
}
