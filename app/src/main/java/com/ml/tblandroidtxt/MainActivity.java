package com.ml.tblandroidtxt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    static final int REQ_INPUT = 10;
    static final int REQ_OUTPUT = 11;
    static final int REQ_YAML = 12;
    static final int REQ_ENV = 13;
    static final int REQ_NOTI = 14;
    static final int REQ_GLOSSARY = 15; // legacy one-shot import
    static final int REQ_OUTPUT_TREE = 16;
    static final int REQ_GLOSSARY_MULTI_IMPORT = 17;
    static final int REQ_EXPORT_ENV = 18;
    static final int REQ_EXPORT_PROFILE = 19;
    static final int REQ_IMPORT_PROFILE = 20;
    static final int REQ_PRONOUN = 21;
    static final int REQ_EXPORT_LOG = 22;
    static final int REQ_EXPORT_JOB_LOG = 23;
    static final int REQ_EDITORIAL_BATCH = 24;
    static final int REQ_EDITORIAL_REFERENCE = 28;
    static final int REQ_EDITORIAL_DRAFT = 29;
    static final int REQ_EDITORIAL_BUNDLE = 30;

    int BG, PANEL, CARD, FIELD, BORDER, TEXT, MUTED, BLUE, CYAN;
    final int GREEN = Color.rgb(43, 207, 126); // semantic success/running
    final int RED = Color.rgb(238, 70, 70);    // semantic error/destructive
    final int AMBER = Color.rgb(245, 180, 67); // semantic warning/paused
    UiAppearance.Palette appearance;

    Uri inputUri;
    final ArrayList<Uri> inputUris = new ArrayList<>();
    Uri outputUri;
    Uri outputTreeUri;
    Uri yamlUri;
    Uri envUri;
    Uri glossaryUri;
    Uri pronounUri;

    FrameLayout contentFrame;
    LinearLayout appRoot, tabBar;
    TextView inputFileLabel, outputFileLabel, outputFolderLabel, yamlFileLabel, envFileLabel, glossaryFileLabel, pronounFileLabel, activeGlossaryLabel;
    TextView translateMeta, statusBanner, resultTitle, resultMeta, logView, glossaryPreview, filesSummary, sampleText;
    TextView topTitle, topSubtitle, topStatusChip;
    ScrollView logScroll;
    TextView estimateText, modelPriceLabel, modelDiagnosticsText;
    TextView trackingTitle, trackingFile, metricChunks, metricCompleted, metricFailed, metricFallbacks, metricElapsed, metricRemaining, metricCost, metricTokens;
    TextView metricCurrentChunk, metricGlossaryLocks, metricPronounLocks, lockUsageMeta, previewMeta, previewText;
    LinearLayout trackingCard, estimatePanel, outputFolderFilesList, actionControlRow;
    TextView translateGlossaryChip, translatePronounChip, translateInstructionChip;
    LinearLayout resultCard;
    ProgressBar progress;
    Button startButton, pauseButton, resumeButton, cancelButton, retryButton, pricingRetryButton, outputFolderButton;
    boolean translationActive = false;
    int estimateSeq = 0;
    volatile CostEstimator.Estimate lastEstimate;
    volatile String lastEstimateModelId = "";
    volatile PreparedBatch preparedBatch;
    final ExecutorService preflightExecutor=Executors.newSingleThreadExecutor();
    final Map<String,View> pageCache=new HashMap<>();
    volatile List<TranslationRepository.JobSummary> cachedJobSummaries=java.util.Collections.emptyList();
    volatile String cachedRuntimeLog="";
    final AtomicInteger preflightGeneration=new AtomicInteger();
    volatile String validatedPlanId="";
    volatile long validatedPlanAt;
    volatile String cachedProviderMeta = "";
    String renderedTopState = "";
    final Handler runtimeTimerHandler = new Handler(Looper.getMainLooper());
    boolean appInForeground;
    String timedRuntimeState = "IDLE";
    long timedElapsedMs;
    long timedRemainingMs = -1L;
    long timedAtRealtime;
    final Runnable runtimeTimerTick = new Runnable() {
        @Override public void run() {
            if (!shouldRunRuntimeTimer()) return;
            long delta = Math.max(0L, SystemClock.elapsedRealtime() - timedAtRealtime);
            if (metricElapsed != null) metricElapsed.setText("Elapsed " + CostEstimator.duration(timedElapsedMs + delta));
            if (metricRemaining != null) metricRemaining.setText(TranslationDashboardFormatter.remaining(timedRemainingMs < 0 ? -1L : Math.max(0L, timedRemainingMs - delta)));
            runtimeTimerHandler.postDelayed(this, 3000L);
        }
    };
    final ModelCatalog.Listener catalogListener = catalogState -> runOnUiThread(() -> {
        updateMetaLine();
        PreparedBatch plan=currentPreparedBatch();
        if(plan!=null&&plan.ready()&&!PreparationCoordinator.constructionKey(collectSettings()).equals(plan.constructionKey))updateInputEstimate();else updatePricingOnly();
        updateModelDiagnostics();
    });

    EditText providerField, baseUrlField, apiKeyField, modelField, sourceField, targetField, outputPatternField, chunkModeField, optimizationPresetField;
    EditText maxTokensField, maxCharsField, softRatioField, timeoutField, attemptsField, tempField, maxOutputField, contextField, costLimitField, glossaryLimitField, pronounLimitField, initialRetryDelayField, maxRetryDelayField, costWarningField, maxRetryCostField, maxPaidRetriesField;
    EditText manualGlossarySource, manualGlossaryTarget, manualGlossaryCategory, glossaryNameField, glossarySourceField, glossaryTargetField;
    CheckBox refineBox, bilingualBox, partialBox, costLimitBox, overlapBox, retryEmptyBox, retryTruncationBox, retryValidationBox, stopUnknownPricingBox;

    String currentTab = "Translate";
    String settingsCategory = "General";
    boolean settingsSectionExpanded = true;
    boolean hydratingSettings = false;
    String pendingExportKind = "";
    long pendingExportJobId = -1L;
    boolean pendingExportJobJson = false;
    String lastGlossaryText = "";
    String lastInstructionName = "—";
    String lastGlossaryName = "—";
    GlossaryStore.Glossary editingGlossary = null;
    PronounStore.Profile editingPronoun = null;
    String pendingPronounReplaceId = "";
    long pendingEditorialProjectId = -1L;
    final Map<Long,ArrayList<EditorialImportPlanner.Source>> pendingEditorialRaw=new HashMap<>();
    final Map<Long,ArrayList<EditorialImportPlanner.Source>> pendingEditorialDraft=new HashMap<>();
    final Map<Long,Map<String,EditorialImportPlanner.Source>> pendingEditorialGlossaryOverrides=new HashMap<>();
    final Map<Long,Map<String,EditorialImportPlanner.Source>> pendingEditorialPronounOverrides=new HashMap<>();
    long pendingEditorialReferenceProjectId = -1L;
    EditorialSafe4Workflow.AssetRole pendingEditorialReferenceRole = null;
    EditorialPackImportCoordinator editorialPackImportCoordinator;

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!TranslatorService.ACTION_PROGRESS.equals(intent.getAction())) return;
            String log = intent.getStringExtra(TranslatorService.EXTRA_LOG);
            int p = intent.getIntExtra(TranslatorService.EXTRA_PROGRESS, -1);
            String status = intent.getStringExtra(TranslatorService.EXTRA_STATUS);
            if (p >= 0 && progress != null) progress.setProgress(p);
            if (log != null && !log.isEmpty()) appendServiceLog(log);
            updateTrackingFromIntent(intent);
            if ("running".equals(status) || "paused".equals(status) || "stopping".equals(status)) translationActive = true;
            if ("done".equals(status) || "error".equals(status) || "cancelled".equals(status)) translationActive = false;
            updateActionButtons();
            if (appRoot != null) appRoot.postDelayed(() -> {
                translationActive = TranslationUiStatePolicy.isTranslationActive(TranslatorService.isActive(), status);
                updateActionButtons();
            }, 350);
            if (statusBanner != null && log != null) {
                statusBanner.setText(statusText(status, log));
                statusBanner.setVisibility(View.VISIBLE);
                tint(statusBanner, statusColor(status), 1, 14);
            }
            if ("done".equals(status)) {
                showResult("Output ready", "Saved and verified");
                toast("Dịch hoàn tất");
            } else if ("error".equals(status)) toast("Có lỗi khi dịch");
            else if ("stopping".equals(status)) toast("Đang dừng dịch");
            else if ("paused".equals(status)) toast("Đã tạm dừng");
            else if ("cancelled".equals(status)) toast("Đã hủy");
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ObservabilityLog.initialize(this);
        ModelCatalog.addListener(catalogListener);
        ModelCatalog.initialize(this);
        applyAppearance(UiAppearance.load(this));
        AppSettings saved = SettingsStore.load(this);
        cachedProviderMeta = providerMeta(saved);
        PronounStore.migrateLegacy(this,saved);
        PronounStore.Profile activePronoun=PronounStore.selected(this);
        if(activePronoun!=null){saved.pronounText=activePronoun.text;saved.pronounUri=activePronoun.uri;saved.pronounName=activePronoun.name;saved.selectedPronounId=activePronoun.id;saved.selectedPronounName=activePronoun.name;SettingsStore.save(this,saved);}
        if (saved.instructionUri != null && !saved.instructionUri.isEmpty()) yamlUri = Uri.parse(saved.instructionUri);
        if (saved.envUri != null && !saved.envUri.isEmpty()) envUri = Uri.parse(saved.envUri);
        if (saved.outputUri != null && !saved.outputUri.isEmpty()) outputUri = Uri.parse(saved.outputUri);
        if (saved.outputTreeUri != null && !saved.outputTreeUri.isEmpty()) outputTreeUri = Uri.parse(saved.outputTreeUri);
        if (saved.pronounUri != null && !saved.pronounUri.isEmpty()) pronounUri = Uri.parse(saved.pronounUri);
        lastInstructionName = saved.instructionName == null || saved.instructionName.isEmpty() ? "—" : saved.instructionName;
        if (saved.selectedGlossaryName != null && !saved.selectedGlossaryName.isEmpty()) lastGlossaryName = saved.selectedGlossaryName;
        if (saved.glossaryText != null) lastGlossaryText = saved.glossaryText;
        for (String raw : UiSessionStore.loadInputUris(this)) {
            if (raw != null && !raw.trim().isEmpty()) inputUris.add(Uri.parse(raw));
        }
        inputUri = inputUris.isEmpty() ? null : inputUris.get(0);
        if (b != null) { currentTab = b.getString("currentTab", currentTab); settingsCategory=b.getString("settingsCategory",settingsCategory); settingsSectionExpanded=b.getBoolean("settingsSectionExpanded",true); }
        translationActive = TranslatorService.isActive();
        buildUi();
        refreshJobCacheAsync(false);
        requestNotificationPermission();
        switchTab(currentTab);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentTab", currentTab);
        outState.putString("settingsCategory",settingsCategory);
        outState.putBoolean("settingsSectionExpanded",settingsSectionExpanded);
        UiSessionStore.saveInputUris(this, inputUris);
    }

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onStart() {
        super.onStart();
        appInForeground = true;
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, new IntentFilter(TranslatorService.ACTION_PROGRESS), Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, new IntentFilter(TranslatorService.ACTION_PROGRESS));
        if (trackingCard != null) restoreRuntimeStateToUi();
        updateRuntimeTimer();
    }

    @Override protected void onStop() {
        silentPersistCurrentUi();
        super.onStop();
        appInForeground = false;
        runtimeTimerHandler.removeCallbacks(runtimeTimerTick);
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    @Override protected void onDestroy() {
        ModelCatalog.removeListener(catalogListener);
        preflightGeneration.incrementAndGet();
        preflightExecutor.shutdownNow();
        if (editorialPackImportCoordinator != null) {
            editorialPackImportCoordinator.close();
            editorialPackImportCoordinator = null;
        }
        super.onDestroy();
    }

    void buildUi() {
        pageCache.clear();
        configureSystemBars();
        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(BG);
        appRoot.setPadding(dp(10), dp(8), dp(10), dp(8));

        // v3.0 keeps the stable tab navigation from v2.6.8+ and only polishes the shell safely.
        // Do not reintroduce DrawerLayout here; it was the risky v2.6.7 regression path.
        appRoot.addView(buildTopBar());
        contentFrame = new FrameLayout(this);
        boolean wide=isWideLayout();
        if(wide){
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);
            body.addView(buildPrimaryNavigation(true),new LinearLayout.LayoutParams(dp(112),-1));
            body.addView(contentFrame,new LinearLayout.LayoutParams(0,-1,1));
            appRoot.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        }else{
            appRoot.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));
            appRoot.addView(buildPrimaryNavigation(false),new LinearLayout.LayoutParams(-1,dp(58)));
        }
        setContentView(appRoot);
        applySystemInsetPadding();
    }

    void applyAppearance(UiAppearance.Palette p){appearance=p;BG=p.bg;PANEL=p.panel;CARD=p.card;FIELD=p.field;BORDER=p.border;TEXT=p.text;MUTED=p.muted;BLUE=p.accent;CYAN=p.accentAlt;}
    boolean isWideLayout(){return getResources().getConfiguration().screenWidthDp>=720;}
    void selectAppearance(String id){
        silentPersistCurrentUi();UiAppearance.save(this,id);applyAppearance(UiAppearance.find(id));String tab=currentTab;
        buildUi();currentTab=tab;switchTab(tab);
    }
    void switchSettingsCategory(String category){
        if(!SettingsNavigationPolicy.shouldSwitch(settingsCategory,category)){if(!settingsSectionExpanded)toggleSettingsSection(category);return;}silentPersistCurrentUi();settingsCategory=category;settingsSectionExpanded=true;
        if(!"Settings".equals(currentTab)){switchTab("Settings");return;}
        invalidatePage("Settings");switchTab("Settings");
    }

    void toggleSettingsSection(String category) {
        if (category == null || category.trim().isEmpty()) return;
        silentPersistCurrentUi();
        if (category.equals(settingsCategory)) settingsSectionExpanded = !settingsSectionExpanded;
        else { settingsCategory = category; settingsSectionExpanded = true; }
        if (!"Settings".equals(currentTab)) { switchTab("Settings"); return; }
        invalidatePage("Settings"); switchTab("Settings");
    }

    void recordSettingsSectionState(String category, boolean expanded) {
        if (category == null || category.trim().isEmpty()) return;
        settingsCategory = category;
        settingsSectionExpanded = expanded;
    }

    void configureSystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }
    }

    void applySystemInsetPadding() {
        if (appRoot == null) return;
        appRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            appRoot.setPadding(dp(10) + left, dp(8) + top, dp(10) + right, dp(8) + bottom);
            return insets;
        });
        appRoot.requestApplyInsets();
    }

    View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, 0, 0, dp(8));

        ImageView badge = new ImageView(this);
        badge.setImageResource(R.drawable.translate_books_logo);
        badge.setScaleType(ImageView.ScaleType.CENTER_CROP);
        badge.setContentDescription("Translate Books logo");
        bar.addView(badge, new LinearLayout.LayoutParams(dp(44), dp(42)));
        bar.addView(space(10, 1));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);
        topTitle = text(AppBuildInfo.APP_TITLE, 17, TEXT, true);
        topTitle.setSingleLine(true);
        topTitle.setEllipsize(TextUtils.TruncateAt.END);
        topSubtitle = text(currentTab + " • " + compactProviderMeta(), 12, MUTED, false);
        topSubtitle.setSingleLine(true);
        topSubtitle.setEllipsize(TextUtils.TruncateAt.END);
        titleBox.addView(topTitle, new LinearLayout.LayoutParams(-1, -2));
        titleBox.addView(topSubtitle, new LinearLayout.LayoutParams(-1, -2));
        bar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        topStatusChip = text("IDLE", 11, MUTED, true);
        renderedTopState = "";
        topStatusChip.setGravity(Gravity.CENTER);
        topStatusChip.setPadding(dp(10), dp(5), dp(10), dp(5));
        tint(topStatusChip, FIELD, BORDER, 1, 14);
        bar.addView(topStatusChip, new LinearLayout.LayoutParams(-2, dp(34)));
        bar.addView(space(8, 1));

        Button refresh = button("↻", FIELD, TEXT, v -> refreshCurrentPage());
        refresh.setTextSize(18);
        refresh.setMinWidth(0);
        refresh.setMinimumWidth(0);
        bar.addView(refresh, new LinearLayout.LayoutParams(dp(44), dp(42)));
        return bar;
    }

    View buildPrimaryNavigation(boolean vertical) {
        tabBar = new LinearLayout(this);
        tabBar.setOrientation(vertical?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        tabBar.setGravity(vertical?Gravity.TOP:Gravity.CENTER);
        tabBar.setPadding(vertical?dp(4):0,vertical?dp(8):dp(4),vertical?dp(8):0,vertical?0:dp(4));
        String[] tabs={"Translate","Jobs","Editorial","Library","Settings"};
        for(String t:tabs){Button b=tabButton(t);tabBar.addView(b,vertical?new LinearLayout.LayoutParams(-1,dp(58)):new LinearLayout.LayoutParams(0,-1,1));}
        tint(tabBar,PANEL,BORDER,1,vertical?0:14);
        return tabBar;
    }

    Button tabButton(String name) {
        Button b = new Button(this);
        b.setText(primaryIcon(name) + "\n" + name);
        b.setTextSize(isWideLayout()?11:10);
        b.setAllCaps(false);
        b.setSingleLine(false);
        b.setMaxLines(2);
        b.setTextColor(TEXT);
        b.setPadding(dp(2), dp(2), dp(2), dp(2));
        b.setOnClickListener(v -> switchTab(name));
        b.setGravity(Gravity.CENTER);b.setMinWidth(0);b.setMinimumWidth(0);
        boolean active = primaryActive(name);
        b.setTag(active);
        tint(b, active ? CARD : PANEL, active ? BLUE : PANEL, 1, 10);
        return b;
    }

    String primaryIcon(String name){if("Translate".equals(name))return "↗";if("Jobs".equals(name))return "◷";if("Editorial".equals(name))return "✎";if("Library".equals(name))return "▤";return "⚙";}
    boolean primaryActive(String name){if("Library".equals(name))return "Files".equals(currentTab)||"Glossaries".equals(currentTab)||"Pronouns".equals(currentTab)||"Sample".equals(currentTab);return name.equals(currentTab);}

    void switchTab(String name) {
        if("Library".equals(name))name="Files";
        if (name.equals(currentTab) && ("Glossaries".equals(name) || "Pronouns".equals(name))) invalidatePage(name);
        currentTab = name;
        refreshTabs();
        View page = pageCache.get(name);
        boolean created = page == null;
        if (created) {
            page = buildPageForTab(name);
            pageCache.put(name, page);
            contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
        } else if (page.getParent() != contentFrame) {
            if (page.getParent() instanceof ViewGroup) ((ViewGroup) page.getParent()).removeView(page);
            contentFrame.addView(page, new FrameLayout.LayoutParams(-1, -1));
        }
        for (int i = 0; i < contentFrame.getChildCount(); i++) {
            View child = contentFrame.getChildAt(i);
            child.setVisibility(child == page ? View.VISIBLE : View.GONE);
        }
        if (created && ("Translate".equals(name) || "Settings".equals(name))) fillSettings(SettingsStore.load(this));
        updateRuntimeTimer();
    }

    void invalidatePage(String name) {
        if (name == null) return;
        View removed = pageCache.remove(name);
        if (removed != null && removed.getParent() == contentFrame) contentFrame.removeView(removed);
    }

    void refreshPagePreservingScroll(String name) {
        ScrollView previousScroll = findScrollView(pageCache.get(name));
        int previousY = previousScroll == null ? 0 : previousScroll.getScrollY();
        invalidatePage(name);
        switchTab(name);
        if (previousY <= 0) return;
        ScrollView replacementScroll = findScrollView(pageCache.get(name));
        if (replacementScroll == null) return;
        replacementScroll.post(() -> {
            if (!name.equals(currentTab) || findScrollView(pageCache.get(name)) != replacementScroll) return;
            View child = replacementScroll.getChildCount() == 0 ? null : replacementScroll.getChildAt(0);
            int maxY = child == null ? 0 : Math.max(0, child.getHeight() - replacementScroll.getHeight());
            replacementScroll.scrollTo(0, Math.min(previousY, maxY));
        });
    }

    ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ScrollView found = findScrollView(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    @Override public void onBackPressed() {
        AppBackNavigationPolicy.Action action = AppBackNavigationPolicy.resolve(
                currentTab, editingGlossary != null, editingPronoun != null, settingsCategory);
        if (action == AppBackNavigationPolicy.Action.SAVE_AND_CLOSE_GLOSSARY) {
            saveEditingGlossary(); editingGlossary = null; invalidatePage("Glossaries"); switchTab("Glossaries"); return;
        }
        if (action == AppBackNavigationPolicy.Action.CLOSE_PRONOUN) {
            editingPronoun = null; invalidatePage("Pronouns"); switchTab("Pronouns"); return;
        }
        if (action == AppBackNavigationPolicy.Action.OPEN_LIBRARY_HOME) { switchTab("Files"); return; }
        if (action == AppBackNavigationPolicy.Action.OPEN_SETTINGS_GENERAL) {
            silentPersistCurrentUi(); settingsCategory = "General"; settingsSectionExpanded = true;
            invalidatePage("Settings"); switchTab("Settings"); return;
        }
        if (action == AppBackNavigationPolicy.Action.OPEN_TRANSLATE) { switchTab("Translate"); return; }
        super.onBackPressed();
    }

    View buildPageForTab(String name) {
        if ("Translate".equals(name)) return buildTranslatePage();
        if ("Settings".equals(name)) return buildSettingsPage();
        if ("Editorial".equals(name)) return buildEditorialPage();
        if ("Glossaries".equals(name)||"Pronouns".equals(name)||"Files".equals(name)||"Sample".equals(name)) return buildLibraryDestination(name);
        if ("Jobs".equals(name)) return buildJobsPage();
        return buildSamplePage();
    }

    View buildLibraryDestination(String selected){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(0,dp(4),0,dp(6));
        String[] pages={"Files","Glossaries","Pronouns","Sample"};for(String page:pages){Button b=button(page,selected.equals(page)?CARD:FIELD,selected.equals(page)?TEXT:MUTED,v->switchTab(page));b.setTextSize(12);b.setMinWidth(0);b.setMinimumWidth(0);tint(b,selected.equals(page)?CARD:FIELD,selected.equals(page)?BLUE:BORDER,1,9);nav.addView(b,new LinearLayout.LayoutParams(-2,dp(38)));}
        scroll.addView(nav);root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(48)));
        View page="Files".equals(selected)?buildFilesPage():"Glossaries".equals(selected)?buildGlossariesPage():"Pronouns".equals(selected)?buildPronounsPage():buildSamplePage();
        root.addView(page,new LinearLayout.LayoutParams(-1,0,1));return root;
    }



    void silentPersistCurrentUi() {
        if (hydratingSettings) return;
        try {
            if (providerField != null || sourceField != null || refineBox != null) {
                SettingsStore.save(this, collectSettings());
            }
            saveRuntimeUris();
        } catch (Exception ignored) {}
    }

    void bindEstimateInput(EditText field) {
        if (field == null) return;
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (hydratingSettings) return;
                if (field.getTag() instanceof Runnable) field.removeCallbacks((Runnable)field.getTag());
                Runnable update = () -> {
                    silentPersistCurrentUi();
                    updateMetaLine();
                    updateInputEstimate();
                };
                field.setTag(update);
                field.postDelayed(update, 250);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    void bindEstimateCheck(CheckBox box) {
        if (box == null) return;
        box.setOnCheckedChangeListener((button, checked) -> {
            if (hydratingSettings) return;
            silentPersistCurrentUi();
            updateInputEstimate();
        });
    }

    void saveRuntimeUris() {
        AppSettings s = SettingsStore.load(this);
        if (outputUri != null) {
            s.outputUri = outputUri.toString();
            s.outputName = FileUtil.displayName(this, outputUri);
        }
        if (outputTreeUri != null) {
            s.outputTreeUri = outputTreeUri.toString();
            s.outputTreeName = FileUtil.treeName(outputTreeUri);
        }
        if (outputUri == null) { s.outputUri = ""; s.outputName = ""; }
        if (outputTreeUri == null) { s.outputTreeUri = ""; s.outputTreeName = ""; }
        if (pronounUri != null) {
            s.pronounUri = pronounUri.toString();
            s.pronounName = FileUtil.displayName(this, pronounUri);
        }
        SettingsStore.save(this, s);
    }

    void refreshCurrentPage() {
        try {
            silentPersistCurrentUi();
            AppSettings saved = SettingsStore.load(this);
            if (saved.outputUri != null && !saved.outputUri.isEmpty()) outputUri = Uri.parse(saved.outputUri);
            if (saved.outputTreeUri != null && !saved.outputTreeUri.isEmpty()) outputTreeUri = Uri.parse(saved.outputTreeUri);
            if (saved.pronounUri != null && !saved.pronounUri.isEmpty()) pronounUri = Uri.parse(saved.pronounUri);
            invalidatePage(currentTab);
            switchTab(currentTab);
            refreshFileSummary();
            updateActionButtons();
            if (TranslatorService.isActive()) appendLog("Đã refresh UI; job đang chạy vẫn tiếp tục, không tạo job mới.");
            else appendLog("Đã refresh: settings, glossary, recent jobs và output folder");
            toast("Đã refresh dữ liệu");
        } finally { /* Refresh is button-only; there is no gesture interceptor. */ }
    }

    void refreshTabs() {
        if (topSubtitle != null) topSubtitle.setText(currentTab + " • " + compactProviderMeta());
        refreshTopStatusChip();
        if (tabBar == null) return;
        for (int i = 0; i < tabBar.getChildCount(); i++) {
            View v = tabBar.getChildAt(i);
            if (v instanceof Button) {
                Button b = (Button) v;
                String label=b.getText().toString();boolean active=(label.contains("Library")&&primaryActive("Library"))||label.contains(currentTab);
                if (b.getTag() instanceof Boolean && ((Boolean) b.getTag()) == active) continue;
                b.setTag(active);
                b.setTextColor(active ? TEXT : MUTED);
                tint(b, active ? CARD : FIELD, active ? BLUE : BORDER, 1, 9);
            }
        }
    }
    void refreshTopStatusChip() {
        if (topStatusChip == null) return;
        boolean active = TranslatorService.isActive() || translationActive;
        String state = active ? "RUNNING" : "IDLE";
        if (!active) {
            Intent saved = RuntimeStateStore.toIntent(this);
            if (saved != null) {
                String durable = saved.getStringExtra(TranslatorService.EXTRA_STATE);
                if (durable != null && !durable.trim().isEmpty()) state = durable.trim().toUpperCase(java.util.Locale.ROOT);
            }
        }
        if (state.equals(renderedTopState)) return;
        renderedTopState = state;
        boolean healthy = "RUNNING".equals(state) || "COMPLETED".equals(state) || "READY".equals(state);
        boolean problem = "FAILED".equals(state);
        int foreground = healthy ? GREEN : (problem ? RED : MUTED);
        int background = healthy ? Color.rgb(12, 42, 29) : (problem ? Color.rgb(48, 20, 20) : FIELD);
        int border = healthy ? GREEN : (problem ? RED : BORDER);
        topStatusChip.setText(state);
        topStatusChip.setTextColor(foreground);
        tint(topStatusChip, background, border, 1, 14);
    }

    String compactProviderMeta() {
        if (cachedProviderMeta != null && !cachedProviderMeta.isEmpty()) return cachedProviderMeta;
        try {
            AppSettings s = SettingsStore.load(this);
            cachedProviderMeta = providerMeta(s);
            return cachedProviderMeta;
        } catch (Exception e) {
            return "OpenAI-compatible workflow";
        }
    }

    String providerMeta(AppSettings s) {
        String provider = s == null || s.provider == null || s.provider.isEmpty() ? "provider" : s.provider;
        String source = s == null || s.sourceLanguage == null || s.sourceLanguage.isEmpty() ? "JA" : s.sourceLanguage;
        String target = s == null || s.targetLanguage == null || s.targetLanguage.isEmpty() ? "VI" : s.targetLanguage;
        return provider + " • " + source + " → " + target;
    }

    View buildTranslatePage() { return new TranslatePageFactory(this).buildTranslatePage(); }
    View buildSettingsPage() { return new SettingsPageFactory(this).buildSettingsPage(); }
    View buildGlossariesPage() { return new GlossaryPageFactory(this).buildGlossariesPage(); }
    View buildPronounsPage() { return new PronounPageFactory(this).buildPronounsPage(); }


    View buildFilesPage() { return new FilesPageFactory(this).buildFilesPage(); }



    View buildJobsPage() {
        return new JobsMainPageFactory(this).build();
    }
    View buildEditorialPage() { return new EditorialPageFactory(this).build(); }

    /** Thin request entry point; ZIP parsing, validation and persistence stay outside Activity. */
    void openEditorialPackZipImport() { EditorialPackImportPageFactory.showInstructions(this); }

    void attachEditorialPackImportCoordinator(EditorialPackImportCoordinator coordinator) {
        if (editorialPackImportCoordinator != null) editorialPackImportCoordinator.close();
        editorialPackImportCoordinator = coordinator;
    }

    JobsPageFactory developerToolsFactory() {
        return new JobsPageFactory(
                this,
                BG, PANEL, CARD, FIELD, BORDER, TEXT, MUTED, BLUE, GREEN, CYAN, RED, AMBER,
                () -> refreshJobCacheAsync(true),
                this::resumeCheckpoint,
                this::retryFailedChunks,
                this::exportLog,
                this::clearRuntimeLog,
                this::resumeJob,
                this::retryJobFailedChunks,
                this::exportJobLog,
                this::appendLog,
                this::toast
        );
    }

    void openJobDetails(long jobId) { developerToolsFactory().openJobDetails(jobId); }
    void openDeveloperTools() { developerToolsFactory().showDeveloperTools(); }

    List<TranslationRepository.JobSummary> jobSummariesSnapshot(){return new ArrayList<>(cachedJobSummaries);}
    String runtimeLogSnapshot(){return cachedRuntimeLog==null?"":cachedRuntimeLog;}
    void refreshJobCacheAsync(boolean showFeedback){
        preflightExecutor.submit(()->{
            JobStore store=new JobStore(this);List<TranslationRepository.JobSummary> jobs;
            try{jobs=store.recentSummaries(80);}finally{store.close();}
            String logs=LogStore.read(this);cachedJobSummaries=jobs;cachedRuntimeLog=logs;
            runOnUiThread(()->{invalidatePage("Jobs");if("Jobs".equals(currentTab))switchTab("Jobs");if(showFeedback)toast("Jobs refreshed");});
        });
    }

    View buildSamplePage() { return new SamplePageFactory(this).buildSamplePage(); }


    void chooseInput() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_INPUT);
    }

    void chooseOutputFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(i, REQ_OUTPUT_TREE);
    }

    void chooseOutput() {
        String title = "translated.txt";
        if (inputUri != null) {
            String name = FileUtil.displayName(this, inputUri);
            int dot = name.lastIndexOf('.');
            String target = targetField == null ? SettingsStore.load(this).targetLanguage : targetField.getText().toString().trim();
            if (dot > 0) title = name.substring(0, dot) + " (" + target + ")" + name.substring(dot);
            else title = name + " (" + target + ").txt";
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, title);
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_OUTPUT);
    }

    void chooseYaml() {
        if (!ensureConfigMutable()) return;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_YAML);
    }

    void chooseEnv() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_ENV);
    }


    void exportEnv() {
        saveSettingsFromUi();
        pendingExportKind = "env";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, "tbl_android_export.env");
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_EXPORT_ENV);
    }

    void exportProfile() {
        saveSettingsFromUi();
        pendingExportKind = "profile";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "tbl_android_profile.json");
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_EXPORT_PROFILE);
    }

    void importProfile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_IMPORT_PROFILE);
    }


    void exportLog() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, "tbl_android_debug_log_" + AppBuildInfo.safeFileSuffix() + ".txt");
        startActivityForResult(i, REQ_EXPORT_LOG);
    }

    void exportJobLog(long jobId) {
        new AlertDialog.Builder(this).setTitle("Export job report")
                .setItems(new String[]{"JSON (machine-readable)","Text (human-readable)"},(d,which)->startJobReportExport(jobId,which==0)).show();
    }

    private void startJobReportExport(long jobId,boolean json) {
        pendingExportJobId = jobId; pendingExportJobJson=json;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(json?"application/json":"text/plain");
        i.putExtra(Intent.EXTRA_TITLE, "tbl_job_" + jobId + "_report."+(json?"json":"txt"));
        startActivityForResult(i, REQ_EXPORT_JOB_LOG);
    }

    void clearRuntimeLog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear runtime log?")
                .setMessage("Chỉ xóa file log nội bộ, không xóa checkpoint/job.")
                .setPositiveButton("Clear", (d, w) -> {
                    LogStore.clear(this);
                    DebugTraceStore.clear(this);
                    if (logView != null) logView.setText("");
                    toast("Đã xóa log và API trace");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void chooseGlossary() {
        if (!ensureConfigMutable()) return;
        startActivityForResult(configImportIntent(true), REQ_GLOSSARY);
    }

    void choosePronoun() {
        if (!ensureConfigMutable()) return;
        startActivityForResult(configImportIntent(pendingPronounReplaceId == null
                || pendingPronounReplaceId.isEmpty()), REQ_PRONOUN);
    }

    Intent configImportIntent(boolean allowMultiple) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return i;
    }

    void createEditorialProject(String series, String volume) {
        if (series == null || series.trim().isEmpty() || volume == null || volume.trim().isEmpty()) { toast("Nhập Series và Volume"); return; }
        try (EditorialRepository repo = new EditorialRepository(this)) {
            EditorialRepository.Project project = new EditorialRepository.Project(); project.seriesName=series.trim(); project.volumeName=volume.trim();
            project.workflowVersion=EditorialSafe4Pack.VERSION; project.workflowHash=EditorialSafe4Pack.PACK_HASH; repo.createProject(project);
            invalidatePage("Editorial"); switchTab("Editorial"); toast("Đã tạo project biên tập");
        } catch (Exception error) { toast("Không tạo được project: "+error.getMessage()); }
    }

    void createEditorialProjectWithP4Binding(String series, String volume, String packId,
                                             String packVersion, String raw, String draft,
                                             String glossary, String pronoun) {
        if (series == null || series.trim().isEmpty() || volume == null || volume.trim().isEmpty()) {
            toast("Nhập Series và Volume");
            return;
        }
        if (packId == null || packId.trim().isEmpty() || packVersion == null || packVersion.trim().isEmpty()) {
            toast("Phải chọn một Editorial Pack cụ thể");
            return;
        }
        if (raw == null || raw.trim().isEmpty() || draft == null || draft.trim().isEmpty()
                || glossary == null || glossary.trim().isEmpty()) {
            toast("RAW, DRAFT và Glossary là bắt buộc cho setup");
            return;
        }
        String selector = "p4-ui-" + UUID.randomUUID();
        String inputPrefix = selector + "-";
        ArrayList<EditorialP4InputSource> sources = new ArrayList<>();
        sources.add(new EditorialP4InputSource("RAW", inputPrefix + "raw",
                raw.getBytes(java.nio.charset.StandardCharsets.UTF_8), "UTF-8",
                "UNVALIDATED_SETUP_INPUT", 0L));
        sources.add(new EditorialP4InputSource("DRAFT", inputPrefix + "draft",
                draft.getBytes(java.nio.charset.StandardCharsets.UTF_8), "UTF-8",
                "UNVALIDATED_SETUP_INPUT", 0L));
        sources.add(new EditorialP4InputSource("GLOSSARY", inputPrefix + "glossary",
                glossary.getBytes(java.nio.charset.StandardCharsets.UTF_8), "UTF-8",
                "UNVALIDATED_SETUP_INPUT", 0L));
        String pronounStatus = pronoun == null || pronoun.trim().isEmpty() ? "NONE" : "AVAILABLE";
        if ("AVAILABLE".equals(pronounStatus)) {
            sources.add(new EditorialP4InputSource("PRONOUN", inputPrefix + "pronoun",
                    pronoun.getBytes(java.nio.charset.StandardCharsets.UTF_8), "UTF-8",
                    "UNVALIDATED_SETUP_INPUT", 0L));
        }
        EditorialP4SetupRequest request = new EditorialP4SetupRequest(
                selector, series.trim(), volume.trim(), packId.trim(), packVersion.trim(),
                "ui-project-" + series.trim() + "-" + volume.trim(), "ui-scope-" + selector,
                sources, "NORMAL", "AVAILABLE", pronounStatus, "NONE",
                "USER_CONFIRMED_NORMAL_UI", "EDITORIAL_SETUP", "L1_SOURCE_PREFLIGHT",
                "manifest-attestation-v1", com.ml.tblandroidtxt.editorial.pack.EditorialLineageNodeKind.ROOT,
                null, System.currentTimeMillis());
        try (TranslationRepository database = new TranslationRepository(this)) {
            EditorialPackStorageLayout storage = new EditorialPackStorageLayout(getFilesDir().toPath());
            EditorialP4BindingResult result = new EditorialP4BindingTransactionService(database, storage)
                    .createSetup(request);
            if (result.code() == EditorialP4BindingResult.Code.APPENDED
                    || result.code() == EditorialP4BindingResult.Code.ALREADY_EXISTS) {
                invalidatePage("Editorial");
                switchTab("Editorial");
                toast("Đã lưu binding " + packId + " v" + packVersion
                        + " • Chờ chứng nhận • Execution đang khóa");
            } else {
                showResult("Không tạo được setup P4", result.detail());
            }
        } catch (Exception error) {
            showResult("Không tạo được setup P4", AppValidator.readableError(error));
        }
    }

    void updateEditorialProject(long projectId, String series, String volume) {
        if (series == null || series.trim().isEmpty() || volume == null || volume.trim().isEmpty()) { toast("Nhập Series và Volume"); return; }
        try (EditorialRepository repo = new EditorialRepository(this)) {
            repo.updateProjectIdentity(projectId,series,volume);
            invalidatePage("Editorial"); switchTab("Editorial"); toast("Đã cập nhật project biên tập");
        } catch (Exception error) { showResult("Không sửa được project",AppValidator.readableError(error)); }
    }

    void chooseEditorialBatch(long projectId) { chooseEditorialChapterFiles(projectId,true); }

    void chooseEditorialDraft(long projectId) { chooseEditorialChapterFiles(projectId,false); }

    void chooseEditorialBundle(long projectId) {
        if(projectId<=0)return;
        pendingEditorialProjectId=projectId;
        startActivityForResult(configImportIntent(true),REQ_EDITORIAL_BUNDLE);
    }

    private void chooseEditorialChapterFiles(long projectId,boolean raw) {
        if(projectId<=0)return;pendingEditorialProjectId=projectId;
        Intent intent=configImportIntent(true);
        startActivityForResult(intent,raw?REQ_EDITORIAL_BATCH:REQ_EDITORIAL_DRAFT);
    }

    int editorialSelectionCount(long projectId,boolean raw){ArrayList<EditorialImportPlanner.Source> files=(raw?pendingEditorialRaw:pendingEditorialDraft).get(projectId);return files==null?0:files.size();}
    String editorialSelectionSummary(long projectId,boolean raw){ArrayList<EditorialImportPlanner.Source> files=(raw?pendingEditorialRaw:pendingEditorialDraft).get(projectId);if(files==null||files.isEmpty())return "chưa chọn";StringBuilder out=new StringBuilder();int max=Math.min(2,files.size());for(int i=0;i<max;i++){if(i>0)out.append(" • ");out.append(files.get(i).name);}if(files.size()>max)out.append(" • +").append(files.size()-max);return out.toString();}

    void previewEditorialSelection(long projectId){
        ArrayList<EditorialImportPlanner.Source> raws=pendingEditorialRaw.get(projectId),drafts=pendingEditorialDraft.get(projectId);
        if(raws==null||raws.isEmpty()||drafts==null||drafts.isEmpty()){toast("Chọn riêng cả RAW và DRAFT trước");return;}
        EditorialImportPlanner.Source glossary=null,pronoun=null;
        try(EditorialRepository repo=new EditorialRepository(this)) {
            glossary=editorialSource(repo.projectReference(projectId,EditorialSafe4Workflow.AssetRole.GLOSSARY));
            pronoun=editorialSource(repo.projectReference(projectId,EditorialSafe4Workflow.AssetRole.PRONOUN));
        }
        EditorialImportPlanner.Result plan=EditorialImportPlanner.planWithProjectDefaults(raws,drafts,glossary,pronoun);
        plan=EditorialImportPlanner.withChapterOverrides(plan,pendingEditorialGlossaryOverrides.get(projectId),pendingEditorialPronounOverrides.get(projectId));
        previewEditorialPlan(projectId,plan,java.util.Collections.emptyList());
    }

    void chooseEditorialReference(long projectId, EditorialSafe4Workflow.AssetRole role) {
        if(projectId<=0||(role!=EditorialSafe4Workflow.AssetRole.GLOSSARY&&role!=EditorialSafe4Workflow.AssetRole.PRONOUN))return;
        pendingEditorialReferenceProjectId=projectId;pendingEditorialReferenceRole=role;
        startActivityForResult(configImportIntent(true),REQ_EDITORIAL_REFERENCE);
    }

    void manageEditorialReferences(long projectId,EditorialSafe4Workflow.AssetRole role){
        List<EditorialRepository.ReferenceProfile> profiles;try(EditorialRepository repo=new EditorialRepository(this)){profiles=repo.projectReferenceProfiles(projectId,role);}
        if(profiles.isEmpty()){chooseEditorialReference(projectId,role);return;}
        String[] names=new String[profiles.size()];for(int i=0;i<profiles.size();i++)names[i]=(profiles.get(i).active?"✓ ACTIVE • ":"")+profiles.get(i).displayName;
        new AlertDialog.Builder(this).setTitle((role==EditorialSafe4Workflow.AssetRole.GLOSSARY?"Glossary":"Pronoun")+" của project").setSingleChoiceItems(names,-1,(dialog,which)->{EditorialRepository.ReferenceProfile selected=profiles.get(which);new AlertDialog.Builder(this).setTitle(selected.displayName).setItems(new String[]{selected.active?"Đang ACTIVE":"Chọn ACTIVE","Xóa profile"},(d,action)->{try(EditorialRepository repo=new EditorialRepository(this)){if(action==0&&!selected.active){repo.selectProjectReference(projectId,selected.id,role);toast("Đã chọn ACTIVE: "+selected.displayName);}else if(action==1){repo.deleteProjectReference(projectId,selected.id,role);toast("Đã xóa profile");}invalidatePage("Editorial");if("Editorial".equals(currentTab))switchTab("Editorial");}catch(Exception error){showResult("Không cập nhật được profile",AppValidator.readableError(error));}}).setNegativeButton("Hủy",null).show();dialog.dismiss();}).setPositiveButton("Import nhiều file",(d,w)->chooseEditorialReference(projectId,role)).setNegativeButton("Đóng",null).show();
    }

    void previewEditorialBatch(Intent data, int takeFlags) {
        final long projectId=pendingEditorialProjectId; pendingEditorialProjectId=-1L;
        ArrayList<EditorialImportPlanner.Source> files=new ArrayList<>(); ArrayList<String> failures=new ArrayList<>();
        for(Uri selected:selectedDocumentUris(data)) try { FileUtil.takePersistable(this,selected,takeFlags,true,false); files.add(new EditorialImportPlanner.Source(FileUtil.displayName(this,selected),selected.toString(),FileUtil.readText(this,selected))); }
        catch(Exception error){failures.add(FileUtil.displayName(this,selected)+": "+error.getMessage());}
        pendingEditorialRaw.put(projectId,files);invalidatePage("Editorial");if("Editorial".equals(currentTab))switchTab("Editorial");toast("Đã chọn "+files.size()+" file RAW");
        if(pendingEditorialDraft.containsKey(projectId))previewEditorialSelection(projectId);else if(!failures.isEmpty())showResult("RAW import",namesSummary(failures));
    }

    void previewEditorialDraft(Intent data,int takeFlags){
        final long projectId=pendingEditorialProjectId;pendingEditorialProjectId=-1L;
        ArrayList<EditorialImportPlanner.Source> files=new ArrayList<>();ArrayList<String> failures=new ArrayList<>();
        for(Uri selected:selectedDocumentUris(data))try{FileUtil.takePersistable(this,selected,takeFlags,true,false);files.add(new EditorialImportPlanner.Source(FileUtil.displayName(this,selected),selected.toString(),FileUtil.readText(this,selected)));}catch(Exception error){failures.add(FileUtil.displayName(this,selected)+": "+error.getMessage());}
        pendingEditorialDraft.put(projectId,files);invalidatePage("Editorial");if("Editorial".equals(currentTab))switchTab("Editorial");toast("Đã chọn "+files.size()+" file DRAFT");
        if(pendingEditorialRaw.containsKey(projectId))previewEditorialSelection(projectId);else if(!failures.isEmpty())showResult("DRAFT import",namesSummary(failures));
    }

    void previewEditorialBundle(Intent data,int takeFlags){
        final long projectId=pendingEditorialProjectId;pendingEditorialProjectId=-1L;
        ArrayList<EditorialImportPlanner.Source> files=new ArrayList<>();ArrayList<String> failures=new ArrayList<>();
        for(Uri selected:selectedDocumentUris(data))try{FileUtil.takePersistable(this,selected,takeFlags,true,false);files.add(new EditorialImportPlanner.Source(FileUtil.displayName(this,selected),selected.toString(),FileUtil.readText(this,selected)));}catch(Exception error){failures.add(FileUtil.displayName(this,selected)+": "+error.getMessage());}
        previewEditorialPlan(projectId,EditorialImportPlanner.planBundle(files),failures);
    }

    void importEditorialReferences(Intent data,int takeFlags,long projectId,EditorialSafe4Workflow.AssetRole role){
        ArrayList<String> failed=new ArrayList<>(),suggestions=new ArrayList<>();
        ArrayList<EditorialRepository.AssetSnapshot> projectDefaults=new ArrayList<>();
        for(Uri selected:selectedDocumentUris(data))try{
            FileUtil.takePersistable(this,selected,takeFlags,true,false);
            String name=FileUtil.displayName(this,selected),content=FileUtil.readText(this,selected);
            LibraryImportPlanner.Source source=new LibraryImportPlanner.Source(name,content);
            boolean valid=role==EditorialSafe4Workflow.AssetRole.GLOSSARY
                    ?!LibraryImportPlanner.glossaries(java.util.Collections.singletonList(source)).imported.isEmpty()
                    :!LibraryImportPlanner.pronouns(java.util.Collections.singletonList(source)).imported.isEmpty();
            if(!valid)throw new IllegalArgumentException(role==EditorialSafe4Workflow.AssetRole.GLOSSARY?"không có cặp thuật ngữ hợp lệ":"không có quy tắc đại từ hợp lệ");
            EditorialImportPlanner.Source candidate=new EditorialImportPlanner.Source(name,selected.toString(),content);
            if(EditorialImportPlanner.hasChapterNumber(name)){
                Map<Long,Map<String,EditorialImportPlanner.Source>> target=role==EditorialSafe4Workflow.AssetRole.GLOSSARY?pendingEditorialGlossaryOverrides:pendingEditorialPronounOverrides;
                Map<String,EditorialImportPlanner.Source> byChapter=target.get(projectId);
                if(byChapter==null){byChapter=new HashMap<>();target.put(projectId,byChapter);}
                String chapterKey=EditorialImportPlanner.chapterKeyOf(name);byChapter.put(chapterKey,candidate);
                suggestions.add(name+" → Chapter "+chapterKey+" (Chapter override pending)");
            } else {
                projectDefaults.add(new EditorialRepository.AssetSnapshot(role,selected.toString(),name,content));
            }
        }catch(Exception error){failed.add(FileUtil.displayName(this,selected)+": "+error.getMessage());}
        if(projectDefaults.isEmpty()){
            finishEditorialReferenceImport(projectId,role,0,suggestions,failed);
            return;
        }
        StringBuilder message=new StringBuilder("Các file không có chapter number sẽ trở thành project default sau khi lưu:\n");
        for(EditorialRepository.AssetSnapshot asset:projectDefaults)message.append("• ").append(asset.displayName).append('\n');
        new AlertDialog.Builder(this).setTitle("Lưu làm project default?").setMessage(message.toString())
                .setPositiveButton("Lưu project default",(d,w)->{
                    int saved=0;
                    for(EditorialRepository.AssetSnapshot asset:projectDefaults)try(EditorialRepository repo=new EditorialRepository(this)){repo.saveProjectReference(projectId,asset);saved++;}catch(Exception error){failed.add(asset.displayName+": "+error.getMessage());}
                    finishEditorialReferenceImport(projectId,role,saved,suggestions,failed);
                }).setNegativeButton("Không lưu",(d,w)->finishEditorialReferenceImport(projectId,role,0,suggestions,failed)).show();
    }

    private void finishEditorialReferenceImport(long projectId,EditorialSafe4Workflow.AssetRole role,int imported,List<String> suggestions,List<String> failed){
        invalidatePage("Editorial");if("Editorial".equals(currentTab))switchTab("Editorial");
        if(imported>0)toast("Đã lưu "+imported+" project default "+(role==EditorialSafe4Workflow.AssetRole.GLOSSARY?"Glossary":"Pronoun"));
        else if(!suggestions.isEmpty())toast("Đã ghi nhận chapter override đề xuất");
        ArrayList<String> messages=new ArrayList<>();messages.addAll(suggestions);messages.addAll(failed);
        if(!messages.isEmpty())showResult("Editorial reference mapping",namesSummary(messages));
    }

    private void previewEditorialPlan(long projectId,EditorialImportPlanner.Result plan,List<String> failures){
        EditorialImportPreviewDialog.show(this, projectId, plan, failures);
    }

    void persistEditorialBatch(long projectId, EditorialImportPlanner.Result plan) {
        int saved=0; ArrayList<String> failed=new ArrayList<>(); try(EditorialRepository repo=new EditorialRepository(this)) {
            for(EditorialImportPlanner.ChapterPlan chapter:plan.chapters) if(chapter.ready()) try {
                if(chapter.glossary==null)throw new IllegalStateException("Glossary chưa có nguồn rõ ràng");
                ArrayList<EditorialRepository.AssetSnapshot> assets=new ArrayList<>(); assets.add(editorialAsset(EditorialSafe4Workflow.AssetRole.RAW,chapter.raw)); assets.add(editorialAsset(EditorialSafe4Workflow.AssetRole.DRAFT,chapter.draft)); assets.add(editorialAsset(EditorialSafe4Workflow.AssetRole.GLOSSARY,chapter.glossary)); if(chapter.pronoun!=null)assets.add(editorialAsset(EditorialSafe4Workflow.AssetRole.PRONOUN,chapter.pronoun)); repo.createChapter(projectId,chapter.key,chapter.key,assets); saved++;
            } catch(Exception error){failed.add(chapter.key+": "+error.getMessage());}
        } catch(Exception error){failed.add(error.getMessage());}
        if(saved>0){pendingEditorialRaw.remove(projectId);pendingEditorialDraft.remove(projectId);pendingEditorialGlossaryOverrides.remove(projectId);pendingEditorialPronounOverrides.remove(projectId);}invalidatePage("Editorial");switchTab("Editorial");toast("Đã lưu "+saved+" chapter"+(failed.isEmpty()?"":"; lỗi "+failed.size()));if(!failed.isEmpty())showResult("Editorial import",namesSummary(failed));
    }
    EditorialRepository.AssetSnapshot editorialAsset(EditorialSafe4Workflow.AssetRole role, EditorialImportPlanner.Source source) { return new EditorialRepository.AssetSnapshot(role,source.uri,source.name,source.content); }
    EditorialImportPlanner.Source editorialSource(EditorialRepository.AssetSnapshot source) { return source==null?null:new EditorialImportPlanner.Source(source.displayName,source.sourceUri,source.content); }

    void replacePronoun(PronounStore.Profile profile){if(profile==null)return;pendingPronounReplaceId=profile.id;choosePronoun();}

    ArrayList<Uri> selectedDocumentUris(Intent data) {
        java.util.LinkedHashSet<Uri> unique = new java.util.LinkedHashSet<>();
        if (data == null) return new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri selected = clip.getItemAt(i).getUri();
                if (selected != null) unique.add(selected);
            }
        }
        if (data.getData() != null) unique.add(data.getData());
        return new ArrayList<>(unique);
    }

    static final class SelectedTextBatch {
        final ArrayList<LibraryImportPlanner.Source> sources = new ArrayList<>();
        final ArrayList<String> failed = new ArrayList<>();
    }

    SelectedTextBatch readSelectedTextFiles(Intent data, int takeFlags) {
        SelectedTextBatch batch = new SelectedTextBatch();
        for (Uri selected : selectedDocumentUris(data)) {
            String name = FileUtil.displayName(this, selected);
            try {
                FileUtil.takePersistable(this, selected, takeFlags, true, false);
                batch.sources.add(new LibraryImportPlanner.Source(name, FileUtil.readText(this, selected)));
            } catch (Exception error) {
                batch.failed.add(name + ": " + error.getMessage());
            }
        }
        return batch;
    }

    void importGlossaryProfiles(Intent data, int takeFlags) {
        SelectedTextBatch files = readSelectedTextFiles(data, takeFlags);
        LibraryImportPlanner.Result<GlossaryStore.Glossary> planned =
                LibraryImportPlanner.glossaries(files.sources);
        files.failed.addAll(planned.failed);
        if (planned.imported.isEmpty()) {
            toast("Không có glossary hợp lệ");
            appendLog("Glossary multi-import failed: " + namesSummary(files.failed));
            showResult("Glossary import failed", namesSummary(files.failed));
            return;
        }
        ArrayList<GlossaryStore.Glossary> all = new ArrayList<>(planned.imported);
        all.addAll(GlossaryStore.loadAll(this));
        GlossaryStore.saveAll(this, all);
        GlossaryStore.Glossary active = planned.imported.get(0);
        lastGlossaryName = active.name;
        lastGlossaryText = GlossaryStore.toPromptText(active);
        glossaryUri = null;
        setLabel(glossaryFileLabel, "Glossary: " + active.name);
        editingGlossary = null;
        selectGlossary(active, false);
        String skipped = files.failed.isEmpty() ? "" : "\nBỏ qua "
                + files.failed.size() + " file: " + namesSummary(files.failed);
        toast("Đã tạo " + planned.imported.size() + " glossary");
        appendLog("Glossary multi-import: created=" + planned.imported.size()
                + ", skipped=" + files.failed.size());
        showResult("Glossaries imported",
                planned.imported.size() + " profile(s), mỗi file là một glossary." + skipped);
    }

    void importPronounProfiles(Intent data, int takeFlags) {
        SelectedTextBatch files = readSelectedTextFiles(data, takeFlags);
        LibraryImportPlanner.Result<PronounStore.Profile> planned =
                LibraryImportPlanner.pronouns(files.sources);
        files.failed.addAll(planned.failed);
        if (planned.imported.isEmpty()) {
            toast("Không có pronoun hợp lệ");
            appendLog("Pronoun multi-import failed: " + namesSummary(files.failed));
            showResult("Pronoun import failed", namesSummary(files.failed));
            return;
        }
        ArrayList<PronounStore.Profile> all = new ArrayList<>(planned.imported);
        all.addAll(PronounStore.loadAll(this));
        PronounStore.saveAll(this, all);
        PronounStore.Profile active = planned.imported.get(0);
        pronounUri = null;
        setLabel(pronounFileLabel, "Pronoun: " + active.name);
        editingPronoun = null;
        pendingPronounReplaceId = "";
        selectPronoun(active, false);
        String skipped = files.failed.isEmpty() ? "" : "\nBỏ qua "
                + files.failed.size() + " file: " + namesSummary(files.failed);
        toast("Đã tạo " + planned.imported.size() + " pronoun profile");
        appendLog("Pronoun multi-import: created=" + planned.imported.size()
                + ", skipped=" + files.failed.size());
        showResult("Pronouns imported",
                planned.imported.size() + " profile(s), mỗi file là một pronoun." + skipped);
    }

    void replacePronounFromUri(Uri uri, int takeFlags) {
        try {
            if (uri == null) throw new IllegalArgumentException("Picker returned an empty file");
            FileUtil.takePersistable(this, uri, takeFlags, true, false);
            String name = FileUtil.displayName(this, uri);
            String text = FileUtil.readText(this, uri);
            LibraryImportPlanner.Result<PronounStore.Profile> validation =
                    LibraryImportPlanner.pronouns(java.util.Collections.singletonList(
                            new LibraryImportPlanner.Source(name, text)));
            if (validation.imported.isEmpty()) throw new IllegalArgumentException(
                    validation.failed.isEmpty() ? "No valid pronoun rules" : validation.failed.get(0));
            PronounStore.Profile profile = PronounStore.find(this, pendingPronounReplaceId);
            if (profile == null) throw new IllegalStateException("Pronoun profile no longer exists");
            profile.uri = "";
            profile.text = text;
            if (profile.name == null || profile.name.startsWith("New pronoun")) profile.name = name;
            if (!PronounStore.saveAndSelect(this, profile)) {
                throw new IllegalStateException("Could not persist pronoun profile");
            }
            editingPronoun = null;
            applyActivePronoun(profile);
            toast("Đã thay file pronoun: " + name);
            appendLog("Pronoun replaced: " + name);
            refreshPronounsPage();
        } catch (Exception error) {
            toast("Không đọc được pronoun");
            appendLog("Không đọc được pronoun: " + error.getMessage());
        } finally {
            pendingPronounReplaceId = "";
        }
    }

    void selectPronoun(PronounStore.Profile profile){
        selectPronoun(profile, true);
    }

    void selectPronoun(PronounStore.Profile profile, boolean preserveListScroll){
        boolean preserveScroll=preserveListScroll&&editingPronoun==null&&"Pronouns".equals(currentTab);
        if(!ensureConfigMutable()||profile==null)return;if(!PronounStore.saveAndSelect(this,profile)){toast("Could not save pronoun");return;}applyActivePronoun(profile);toast("Active pronoun: "+profile.name);
        if(preserveScroll)refreshPagePreservingScroll("Pronouns");else refreshPronounsPage();
    }

    void refreshPronounsPage(){invalidatePage("Pronouns");switchTab("Pronouns");}

    void applyActivePronoun(PronounStore.Profile profile){AppSettings s=SettingsStore.load(this);if(profile==null){pronounUri=null;s.pronounUri="";s.pronounName="";s.pronounText="";s.selectedPronounId="";s.selectedPronounName="";}else{pronounUri=profile.uri==null||profile.uri.isEmpty()?null:Uri.parse(profile.uri);s.pronounUri=profile.uri;s.pronounName=profile.name;s.pronounText=profile.text;s.selectedPronounId=profile.id;s.selectedPronounName=profile.name;}SettingsStore.save(this,s);updateMetaLine();updateInputEstimate();refreshTranslationConfigViews();}

    void clearPronoun() {
        if (!ensureConfigMutable()) return;
        TranslationConfigRepository.get(this).clear(TranslationConfigRepository.Type.PRONOUN);
        PronounStore.setSelectedId(this,"");applyActivePronoun(null);
        appendLog("Đã bỏ Pronoun đang dùng");
        toast("Đã bỏ Pronoun");
        switchTab("Pronouns");
    }

    void clearInstruction() {
        if (!ensureConfigMutable()) return;
        TranslationConfigRepository.get(this).clear(TranslationConfigRepository.Type.INSTRUCTION_YAML);
        yamlUri = null;
        lastInstructionName = "—";
        AppSettings s = SettingsStore.load(this);
        s.instructionUri = "";
        s.instructionName = "";
        s.translationInstructions = "";
        s.refinementInstructions = "";
        SettingsStore.save(this, s);
        appendLog("Đã bỏ Instruction YAML đang dùng");
        toast("Instruction YAML: Không sử dụng");
        refreshTranslationConfigViews();
    }

    void clearGlossarySelection() {
        if (!ensureConfigMutable()) return;
        TranslationConfigRepository.get(this).clear(TranslationConfigRepository.Type.GLOSSARY);
        GlossaryStore.setSelectedId(this, "");
        glossaryUri = null;
        lastGlossaryName = "—";
        lastGlossaryText = "";
        AppSettings s = SettingsStore.load(this);
        s.selectedGlossaryId = "";
        s.selectedGlossaryName = "";
        s.glossaryText = "";
        SettingsStore.save(this, s);
        appendLog("Đã bỏ Glossary đang dùng");
        toast("Glossary: Không sử dụng");
        refreshTranslationConfigViews();
    }

    boolean ensureConfigMutable() {
        if (!TranslatorService.isActive() && !translationActive) return true;
        toast("Không thể đổi cấu hình khi job đang chạy");
        appendLog("Thay đổi cấu hình bị chặn vì TranslatorService đang active");
        return false;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (EditorialPackSafBridge.isRequest(requestCode)) {
            EditorialPackImportCoordinator coordinator = editorialPackImportCoordinator;
            if (coordinator != null) {
                Uri selected = EditorialPackSafBridge.selectedUri(resultCode, data);
                if (selected == null) {
                    if (resultCode == RESULT_CANCELED) coordinator.onPickerCancelled();
                    else coordinator.onUriSelected(null);
                } else {
                    coordinator.onUriSelected(selected);
                }
            }
            return;
        }
        if (resultCode != RESULT_OK || data == null) {
            if (requestCode == REQ_PRONOUN) pendingPronounReplaceId = "";
            if (requestCode == REQ_EDITORIAL_REFERENCE) { pendingEditorialReferenceProjectId=-1L; pendingEditorialReferenceRole=null; }
            if(requestCode==REQ_EDITORIAL_BATCH||requestCode==REQ_EDITORIAL_DRAFT||requestCode==REQ_EDITORIAL_BUNDLE)pendingEditorialProjectId=-1L;
            return;
        }
        Uri uri = data.getData();
        if (uri == null && data.getClipData() == null) {
            if (requestCode == REQ_PRONOUN) pendingPronounReplaceId = "";
            return;
        }
        int flags = data.getFlags();
        int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        if (requestCode == REQ_INPUT) {
            inputUris.clear();
            ClipData clip = data.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri u = clip.getItemAt(i).getUri();
                    if (u != null) { inputUris.add(u); FileUtil.takePersistable(this, u, Intent.FLAG_GRANT_READ_URI_PERMISSION, true, false); }
                }
            } else if (uri != null) {
                inputUris.add(uri);
                FileUtil.takePersistable(this, uri, takeFlags, true, false);
            }
            inputUri = inputUris.isEmpty() ? null : inputUris.get(0);
            UiSessionStore.saveInputUris(this, inputUris);
            if (!TranslatorService.isActive()) {
                RuntimeStateStore.clear(this);
                translationActive = false;
                if (trackingCard != null) trackingCard.setVisibility(View.GONE);
                if (statusBanner != null) statusBanner.setVisibility(View.GONE);
                if (resultCard != null) resultCard.setVisibility(View.GONE);
                if (progress != null) progress.setProgress(0);
                renderedTopState = "";
                refreshTopStatusChip();
            }
            String label = inputUris.size() <= 1 ? "📄 " + FileUtil.displayName(this, inputUri) : "📚 " + inputUris.size() + " TXT files selected";
            setLabel(inputFileLabel, label);
            toast("Đã chọn " + inputUris.size() + " file TXT"); appendLog("Đã chọn input: " + label); updateInputEstimate();
        } else if (requestCode == REQ_OUTPUT) {
            outputUri = uri;
            outputTreeUri = null;
            FileUtil.takePersistable(this, uri, takeFlags, true, true);
            setLabel(outputFileLabel, "Single output TXT: " + FileUtil.displayName(this, uri) + " (" + FileUtil.accessBadge(this, uri, true) + ")");
            saveRuntimeUris();
            toast("Đã chọn output"); appendLog("Đã chọn output: " + FileUtil.displayName(this, uri));
        } else if (requestCode == REQ_OUTPUT_TREE) {
            outputTreeUri = uri;
            outputUri = null;
            boolean persisted=FileUtil.takePersistable(this, uri, takeFlags, true, true);
            String writable=FileUtil.validateTreeWritable(this,uri,"Output folder");
            if(writable!=null){outputTreeUri=null;setLabel(outputFolderLabel,"Output folder permission lost — Choose output folder again");appendLog("OUTPUT_IMPORT_FAILED uri="+uri+", persistedRead="+FileUtil.hasPersistedRead(this,uri)+", persistedWrite="+FileUtil.hasPersistedWrite(this,uri)+", probe="+writable);saveRuntimeUris();updateActionButtons();return;}
            setLabel(outputFolderLabel, "Output folder: " + FileUtil.treeName(uri) + " (Writable)");
            saveRuntimeUris();
            toast("Đã chọn output folder"); appendLog("OUTPUT_IMPORT_OK uri="+uri+", persistedResult="+persisted+", persistedRead="+FileUtil.hasPersistedRead(this,uri)+", persistedWrite="+FileUtil.hasPersistedWrite(this,uri)+", probe=write/delete OK");updateActionButtons();
        } else if (requestCode == REQ_YAML) {
            if (!ensureConfigMutable()) return;
            try {
                boolean persisted=FileUtil.takePersistable(this, uri, takeFlags, true, false);
                TranslationConfigRepository repository=TranslationConfigRepository.get(this);
                TranslationConfigRepository.Entry imported=repository.importFromUri(TranslationConfigRepository.Type.INSTRUCTION_YAML,uri);
                String instructionText=repository.read(TranslationConfigRepository.Type.INSTRUCTION_YAML);
                CustomInstructions ci = YamlInstructionParser.parse(imported.name, instructionText);
                AppSettings s = collectSettings();
                yamlUri = null;
                lastInstructionName = imported.name;
                s.instructionUri = "";
                s.instructionName = imported.name;
                s.translationInstructions = ci.translation == null ? "" : ci.translation;
                s.refinementInstructions = ci.refinement == null ? "" : ci.refinement;
                SettingsStore.save(this, s);
                setLabel(yamlFileLabel, "Instructions: " + imported.name + " • internal copy valid");
                appendLog("Instruction import succeeded: uri="+uri+", persistedRead="+persisted+", internal="+imported.path+", "+imported.parseDetail);
                toast("Đã nạp instruction: " + lastInstructionName);
                appendLog("Đã nạp instruction: " + lastInstructionName + " (translation=" + ci.hasTranslation() + ", refinement=" + ci.hasRefinement() + ")");
            } catch (Exception e) {
                toast("Không đọc được instruction");
                appendLog("Không đọc được instruction: " + e.getMessage());
            }
            refreshTranslationConfigViews(); updateMetaLine(); updateInputEstimate();
        } else if (requestCode == REQ_ENV) {
            envUri = uri;
            FileUtil.takePersistable(this, uri, takeFlags, true, false);
            String envName = FileUtil.displayName(this, uri);
            setLabel(envFileLabel, ".env: " + envName);
            try {
                TranslationConfigRepository repo=TranslationConfigRepository.get(this);
                TranslationConfigRepository.Entry entry=repo.importFromUri(TranslationConfigRepository.Type.ENVIRONMENT,uri);
                String envText = repo.read(TranslationConfigRepository.Type.ENVIRONMENT);
                AppSettings imported = AppSettings.fromEnv(EnvParser.parse(envText), collectSettings());
                envUri=null; imported.envUri = ""; imported.envName = entry.name;
                // Giữ instruction/glossary đã chọn nếu .env không ghi đè.
                AppSettings old = SettingsStore.load(this);
                if (imported.instructionUri == null || imported.instructionUri.isEmpty()) { imported.instructionUri = old.instructionUri; imported.instructionName = old.instructionName; }
                if (imported.selectedGlossaryId == null || imported.selectedGlossaryId.isEmpty()) { imported.selectedGlossaryId = old.selectedGlossaryId; imported.selectedGlossaryName = old.selectedGlossaryName; imported.glossaryText = old.glossaryText; }
                if (imported.pronounUri == null || imported.pronounUri.isEmpty()) { imported.pronounUri = old.pronounUri; imported.pronounName = old.pronounName; imported.pronounText = old.pronounText; }
                SettingsStore.save(this, imported);
                fillSettings(imported); updateInputEstimate();
                toast("Đã nạp .env: " + envName);
                appendLog("Đã import .env vào settings: " + envName);
            } catch (Exception e) {
                toast("Không đọc được .env");
                appendLog("Không đọc được .env: " + e.getMessage());
            }
        } else if (requestCode == REQ_EXPORT_ENV) {
            try {
                FileUtil.writeText(this, uri, SettingsStore.toEnv(collectSettings()));
                toast("Đã export .env");
                appendLog("Export .env: " + FileUtil.displayName(this, uri));
            } catch (Exception e) { toast("Lỗi export .env"); appendLog("Lỗi export .env: " + e.getMessage()); }
        } else if (requestCode == REQ_EXPORT_PROFILE) {
            try {
                JSONObject o = new JSONObject(SettingsStore.toJson(collectSettings()));
                FileUtil.writeText(this, uri, o.toString(2));
                toast("Đã export profile JSON");
                appendLog("Export profile: " + FileUtil.displayName(this, uri));
            } catch (Exception e) { toast("Lỗi export profile"); appendLog("Lỗi export profile: " + e.getMessage()); }
        } else if (requestCode == REQ_EXPORT_LOG) {
            try {
                FileUtil.writeText(this, uri, LogStore.exportBundle(this));
                toast("Đã export log");
                appendLog("Export log: " + FileUtil.displayName(this, uri));
            } catch (Exception e) { toast("Lỗi export log"); appendLog("Lỗi export log: " + e.getMessage()); }
        } else if (requestCode == REQ_EXPORT_JOB_LOG) {
            try {
                long jobId = pendingExportJobId;
                JobStore store=new JobStore(this);
                FileUtil.writeText(this, uri, pendingExportJobJson?store.exportJobJson(jobId):store.exportJobBundle(jobId));
                toast("Đã export job report");
                appendLog("Export job report #" + jobId + ": " + FileUtil.displayName(this, uri));
                pendingExportJobId = -1L;
            } catch (Exception e) { toast("Lỗi export job report"); appendLog("Lỗi export job report: " + e.getMessage()); }
        } else if(requestCode==REQ_EDITORIAL_REFERENCE){long projectId=pendingEditorialReferenceProjectId;EditorialSafe4Workflow.AssetRole role=pendingEditorialReferenceRole;pendingEditorialReferenceProjectId=-1L;pendingEditorialReferenceRole=null;importEditorialReferences(data,takeFlags,projectId,role);
        } else if (requestCode == REQ_IMPORT_PROFILE) {
            try {
                FileUtil.takePersistable(this, uri, takeFlags, true, false);
                String profileText = FileUtil.readText(this, uri);
                AppSettings imported = SettingsStore.fromJson(profileText);
                SettingsStore.save(this, imported);
                fillSettings(imported);
                updateInputEstimate();
                toast("Đã import profile: " + FileUtil.displayName(this, uri));
                appendLog("Import profile: " + FileUtil.displayName(this, uri));
            } catch (Exception e) { toast("Lỗi import profile"); appendLog("Lỗi import profile: " + e.getMessage()); }
        } else if (requestCode == REQ_PRONOUN) {
            if (!ensureConfigMutable()) {
                pendingPronounReplaceId = "";
                return;
            }
            if (pendingPronounReplaceId != null && !pendingPronounReplaceId.isEmpty()) {
                replacePronounFromUri(uri, takeFlags);
            } else {
                importPronounProfiles(data, takeFlags);
            }
        } else if (requestCode == REQ_GLOSSARY) {
            if (!ensureConfigMutable()) return;
            importGlossaryProfiles(data, takeFlags);
        } else if (requestCode == REQ_GLOSSARY_MULTI_IMPORT) {
            if (!ensureConfigMutable()) return;
            importTermsIntoEditingGlossary(data);
        } else if (requestCode == REQ_EDITORIAL_BATCH) {
            previewEditorialBatch(data, takeFlags);
        } else if(requestCode==REQ_EDITORIAL_DRAFT){
            previewEditorialDraft(data,takeFlags);
        } else if(requestCode==REQ_EDITORIAL_BUNDLE){
            previewEditorialBundle(data,takeFlags);
        }
        refreshFileSummary();
    }

    void startTranslation() {
        appendLog("START_CLICK received");
        PreparedBatch readyPlan=currentPreparedBatch();
        if(readyPlan==null||!readyPlan.ready()){
            toast("Please wait for chunk preparation to finish");
            appendLog("Start blocked: prepared chunk plan is not ready");
            return;
        }
        if (inputUris.isEmpty()) { toast("Chưa chọn TXT input"); return; }
        AppSettings s = collectSettings();
        String validation = startReadinessError();
        if (validation != null) { toast(validation); appendLog("Validation: " + validation); return; }
        String construction=PreparationCoordinator.constructionKey(s);
        if(!construction.equals(readyPlan.constructionKey)){appendLog("Start blocked: chunk settings changed after preparation");updateInputEstimate();toast("Chunk settings changed; rebuilding the chunk plan");return;}
        final int generation=preflightGeneration.incrementAndGet();setButtonEnabled(startButton,false);
        preflightExecutor.submit(()->{String readiness=startReadinessError();PromptContextBuilder.ParseReport locks=readiness==null?PromptContextBuilder.validate(s.glossaryText,s.pronounText):null;runOnUiThread(()->{if(generation!=preflightGeneration.get()||isFinishing()||isDestroyed())return;if(readiness!=null){appendLog("START_BLOCKED resource="+readiness);updateActionButtons();showResult("Start blocked",readiness);return;}validatedPlanId=readyPlan.id;validatedPlanAt=System.currentTimeMillis();appendLog("START_PREFLIGHT_OK preparedPlan="+readyPlan.id);showPreparedPreflight(s,locks);});});
    }

    private void showPreparedPreflight(AppSettings s,PromptContextBuilder.ParseReport locks){CostEstimator.Estimate e=estimateCurrentBatch(s);boolean warning=e==null||!e.pricingAvailable||(locks!=null&&(!locks.conflicts.isEmpty()||!locks.malformedGlossaryRows.isEmpty()||!locks.malformedPronounRows.isEmpty()))||(e!=null&&s.stopOnCostLimit&&s.costLimitUsd>0&&e.noCacheCostHigh>s.costLimitUsd);StringBuilder message=new StringBuilder(TranslationDashboardFormatter.prepared(e,preparedBatch.exactChunkCount()));if(warning)message.append("\n\nSome optional pricing or configuration checks have warnings. Translation can still start when blocking checks pass.");new AlertDialog.Builder(this).setTitle("Ready to translate").setMessage(message.toString()).setPositiveButton("Start",(d,w)->startTranslationNow()).setNegativeButton("Cancel",(d,w)->updateActionButtons()).setOnCancelListener(d->updateActionButtons()).show();}

    private boolean showReliabilityPreflight(AppSettings s) {
        boolean batch=inputUris.size()>1;
        if(batch&&outputTreeUri==null){toast("Batch requires an output folder");return true;}
        if(!batch&&outputUri==null&&outputTreeUri==null){toast("Choose an output TXT or folder");return true;}
        String access=validateSelectedFileAccess(batch);if(access!=null){showResult("Blocking pre-flight error",access);return true;}
        CostEstimator.Estimate e=estimateCurrentBatch(s);StringBuilder pre=new StringBuilder("BLOCKING ERRORS\nNone\n\nWARNINGS\n");boolean warning=false;
        if(e==null||!e.pricingAvailable){pre.append("• Pricing unavailable; token estimate remains valid\n");warning=true;}
        if(e!=null&&s.stopOnCostLimit&&s.costLimitUsd>0&&e.noCacheCostHigh>s.costLimitUsd){pre.append("• Estimated high cost exceeds hard budget\n");warning=true;}
        PromptContextBuilder.ParseReport locks=PromptContextBuilder.validate(s.glossaryText,s.pronounText);
        if(!locks.conflicts.isEmpty()||!locks.malformedGlossaryRows.isEmpty()||!locks.malformedPronounRows.isEmpty()){pre.append("• Glossary/pronoun validation warnings\n");warning=true;}
        if(!warning)pre.append("None\n");
        pre.append("\nINFORMATION\nProvider/model: ").append(s.provider).append(" / ").append(s.model)
                .append("\nTokens: ").append(e==null?"not calculated":CostEstimator.tokenRange(e.totalTokensLow,e.totalTokensHigh))
                .append("\nEstimated normal cost: ").append(e==null||!e.pricingAvailable?"Pricing unavailable":CostEstimator.money(e.costLow)+"–"+CostEstimator.money(e.costHigh))
                .append("\nWorst case with retries: ").append(e==null||!e.pricingAvailable?"Pricing unavailable":CostEstimator.money(e.noCacheCostHigh*Math.max(1,s.maxAttempts)))
                .append("\nHard budget: ").append(s.stopOnCostLimit?CostEstimator.money(s.costLimitUsd):"disabled")
                .append("\nOutput: writable and permission retained");
        final boolean hasWarnings=warning;
        new AlertDialog.Builder(this).setTitle("Translation pre-flight").setMessage(pre.toString()).setPositiveButton(hasWarnings?"Proceed with warnings":"Start",(d,w)->startTranslationNow()).setNegativeButton("Cancel",null).show();
        return true;
    }

    void startTranslationNow() {
        PreparedBatch readyPlan=currentPreparedBatch();
        if(readyPlan==null||!readyPlan.ready()){
            toast("Exact chunk plan is not ready");appendLog("Start blocked: missing/stale prepared plan");return;
        }
        if (!TranslationUiStatePolicy.mayDispatchStart(true, TranslatorService.isActive(), translationActive)) {
            toast("Đang có job dịch chạy; không tạo job mới để tránh dịch lặp/tốn API");
            appendLog("Start bị chặn vì TranslatorService đang chạy");
            return;
        }
        if (inputUris.isEmpty()) { toast("Chưa chọn TXT input"); return; }
        boolean batch = inputUris.size() > 1;
        if (batch && outputTreeUri == null) { toast("Batch nhiều file cần chọn output folder ở tab Files"); return; }
        if (!batch && outputUri == null && outputTreeUri == null) { toast("Chưa chọn output TXT hoặc output folder"); return; }
        String finalReadiness=startReadinessError();
        if(finalReadiness!=null){appendLog("START_DISPATCH_BLOCKED resource="+finalReadiness);showResult("Start blocked",finalReadiness);updateActionButtons();return;}
        saveRuntimeUris();
        AppSettings s = collectSettings();
        SettingsStore.save(this, s);
        Intent i = new Intent(this, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_START);
        i.putExtra("inputUris", inputUrisJson());
        i.putExtra("outputUri", outputUri == null ? "" : outputUri.toString());
        i.putExtra("outputTreeUri", outputTreeUri == null ? "" : outputTreeUri.toString());
        i.putExtra("yamlUri", yamlUri == null ? "" : yamlUri.toString());
        i.putExtra("envUri", envUri == null ? "" : envUri.toString());
        i.putExtra("glossaryUri", glossaryUri == null ? "" : glossaryUri.toString());
        i.putExtra("pronounUri", pronounUri == null ? "" : pronounUri.toString());
        i.putExtra("settings", SettingsStore.toJson(s));
        i.putExtra("preparedBatchId",readyPlan.id);
        String startSessionId = UUID.randomUUID().toString();
        i.putExtra("startSessionId", startSessionId);
        CostEstimator.Estimate expected = readyPlan.estimate;
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, expected == null ? 0 : Math.max(0, expected.totalTokensHigh));
        i.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, expected != null && expected.pricingAvailable && !Double.isNaN(expected.costHigh) ? Math.max(0, expected.costHigh) : 0d);
        try { startForegroundService(i); }
        catch(RuntimeException e){translationActive=false;updateActionButtons();String message="Cannot start translation service: "+AppValidator.readableError(e);appendLog(message);showResult("Translation could not start",message);toast("Translation was not started");return;}
        showPendingStartMetrics(readyPlan);
        translationActive = true;
        updateActionButtons();
        showResult("Translation submitted", inputUris.size() + " file(s)");
        toast("Bắt đầu dịch"); appendLog("Bắt đầu dịch batch: " + inputUris.size() + " file(s)");
    }

    void showPendingStartMetrics(PreparedBatch batch) {
        if (batch == null) return;
        CostEstimator.Estimate expected = batch.estimate;
        Intent state = new Intent(TranslatorService.ACTION_PROGRESS);
        state.putExtra(TranslatorService.EXTRA_STATE, TranslationJobState.State.VALIDATING.name());
        state.putExtra(TranslatorService.EXTRA_STATUS, "validating");
        state.putExtra(TranslatorService.EXTRA_PHASE, "Validating");
        state.putExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, batch.exactChunkCount());
        state.putExtra(TranslatorService.EXTRA_COMPLETED, 0);
        state.putExtra(TranslatorService.EXTRA_FAILED, 0);
        state.putExtra(TranslatorService.EXTRA_FALLBACKS, 0);
        state.putExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 0);
        state.putExtra(TranslatorService.EXTRA_TOTAL_COST, 0d);
        state.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, expected == null ? 0 : Math.max(0, expected.totalTokensHigh));
        state.putExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, expected != null && expected.pricingAvailable && !Double.isNaN(expected.costHigh) ? Math.max(0, expected.costHigh) : 0d);
        state.putExtra(TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, false);
        state.putExtra(TranslatorService.EXTRA_ELAPSED_MS, 0L);
        state.putExtra(TranslatorService.EXTRA_REMAINING_MS, -1L);
        updateTrackingFromIntent(state);
        if (progress != null) progress.setProgress(0);
    }

    String validateSelectedFileAccess(boolean batch) {
        if (inputUris.isEmpty()) return "Chưa chọn TXT input";
        for (int i = 0; i < inputUris.size(); i++) {
            Uri u = inputUris.get(i);
            String err = FileUtil.validateReadable(this, u, "Input #" + (i + 1));
            if (err != null) return err;
        }
        if (batch) {
            String err = FileUtil.validateTreeWritable(this, outputTreeUri, "Output folder");
            if (err != null) return err;
        } else if (outputUri != null) {
            String err = FileUtil.validateWritable(this, outputUri, "Single output TXT");
            if (err != null) return err;
        } else {
            String err = FileUtil.validateTreeWritable(this, outputTreeUri, "Output folder");
            if (err != null) return err;
        }
        if (yamlUri != null) {
            String err = FileUtil.validateReadable(this, yamlUri, "Instructions file");
            if (err != null) return err;
        }
        if (envUri != null) {
            String err = FileUtil.validateReadable(this, envUri, ".env file");
            if (err != null) return err;
        }
        if (glossaryUri != null) {
            String err = FileUtil.validateReadable(this, glossaryUri, "Glossary file");
            if (err != null) return err;
        }
        if (pronounUri != null) {
            String err = FileUtil.validateReadable(this, pronounUri, "Pronoun file");
            if (err != null) return err;
        }
        return null;
    }

    void resumeCheckpoint() {
        if (!TranslationUiStatePolicy.mayStartNewJob(TranslatorService.isActive(), translationActive)) {
            toast("Đang có job dịch chạy; không resume checkpoint để tránh chạy trùng");
            appendLog("Resume checkpoint bị chặn vì job đang chạy");
            return;
        }
        Intent i = new Intent(this, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_RESUME_LAST);
        startForegroundService(i);
        translationActive = true;
        updateActionButtons();
        toast("Đang resume checkpoint"); appendLog("Resume checkpoint");
    }

    void retryFailedChunks() {
        if (!TranslationUiStatePolicy.mayStartNewJob(TranslatorService.isActive(), translationActive)) {
            toast("Đang có job dịch chạy; không retry để tránh chạy trùng");
            appendLog("Retry failed bị chặn vì job đang chạy");
            return;
        }
        Intent i = new Intent(this, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_RETRY_FAILED);
        startForegroundService(i);
        translationActive = true;
        updateActionButtons();
        toast("Đang retry các chunk lỗi"); appendLog("Retry failed chunks only");
    }

    void resumeJob(long jobId) {
        if (!TranslationUiStatePolicy.mayStartNewJob(TranslatorService.isActive(), translationActive)) {
            toast("Đang có job dịch chạy; không resume job khác để tránh chạy trùng");
            appendLog("Resume job #" + jobId + " bị chặn vì job đang chạy");
            return;
        }
        Intent i = new Intent(this, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_RESUME_JOB);
        i.putExtra("jobId", jobId);
        startForegroundService(i);
        translationActive = true;
        updateActionButtons();
        toast("Đang resume job #" + jobId); appendLog("Resume selected job #" + jobId);
    }

    void retryJobFailedChunks(long jobId) {
        if (!TranslationUiStatePolicy.mayStartNewJob(TranslatorService.isActive(), translationActive)) {
            toast("Đang có job dịch chạy; không retry job khác để tránh chạy trùng");
            appendLog("Retry job #" + jobId + " bị chặn vì job đang chạy");
            return;
        }
        Intent i = new Intent(this, TranslatorService.class);
        i.setAction(TranslatorService.ACTION_RETRY_JOB_FAILED);
        i.putExtra("jobId", jobId);
        startForegroundService(i);
        translationActive = true;
        updateActionButtons();
        toast("Đang retry chunk lỗi của job #" + jobId); appendLog("Retry failed chunks for selected job #" + jobId);
    }

    void sendSvc(String action) {
        if (!TranslatorService.isActive() && !translationActive) {
            toast("Không có job đang chạy");
            appendLog("Lệnh service bị bỏ qua vì không có job đang chạy");
            return;
        }
        Intent i = new Intent(this, TranslatorService.class);
        i.setAction(action);
        startService(i);
        if (TranslatorService.ACTION_PAUSE.equals(action)) toast("Đã gửi lệnh Pause");
        else if (TranslatorService.ACTION_RESUME.equals(action)) toast("Đã gửi lệnh Resume");
        else if (TranslatorService.ACTION_CANCEL.equals(action)) toast("Đã gửi lệnh Cancel");
    }

    void updateActionButtons() {
        boolean active = TranslatorService.isActive() || translationActive;
        PreparedBatch plan=currentPreparedBatch();
        boolean failed=false;if(!active){JobStore store=new JobStore(this);try{failed=store.hasFailedChunks();}finally{store.close();}}
        String blocker=!active&&plan!=null&&plan.ready()?startReadinessError():"Preparing TXT chunk plan";
        setButtonEnabled(startButton, !active&&plan!=null&&plan.ready()&&blocker==null);
        if (!active && outputTreeUri != null) {
            String outputError = FileUtil.validateTreeWritable(this, outputTreeUri, "Output folder");
            if (outputError != null) {
                setLabel(outputFolderLabel, "Output folder permission lost");
                if (outputFolderButton != null) outputFolderButton.setText("Choose output folder again");
            } else if (outputFolderButton != null) outputFolderButton.setText("Chọn thư mục");
        }
        if(!active&&statusBanner!=null){
            if(blocker!=null){statusBanner.setVisibility(View.VISIBLE);statusBanner.setText("Not ready: "+blocker);}
            else statusBanner.setVisibility(View.GONE);
        }
        setButtonEnabled(retryButton, !active&&failed);
        setButtonEnabled(pauseButton, active);
        setButtonEnabled(resumeButton, active);
        setButtonEnabled(cancelButton, active);
        if(pauseButton!=null)pauseButton.setVisibility(active?View.VISIBLE:View.GONE);
        if(resumeButton!=null)resumeButton.setVisibility(active?View.VISIBLE:View.GONE);
        if(cancelButton!=null)cancelButton.setVisibility(active?View.VISIBLE:View.GONE);
        if(retryButton!=null)retryButton.setVisibility(!active&&failed?View.VISIBLE:View.GONE);
        if(trackingCard!=null)trackingCard.setVisibility(active||RuntimeStateStore.toIntent(this)!=null?View.VISIBLE:View.GONE);
        refreshTopStatusChip();
    }

    /** Returns the exact blocking resource, never a generic YAML error. */
    String startReadinessError() {
        if(inputUris.isEmpty()) return "TXT input: choose TXT again";
        AppSettings settings=collectSettings();
        String provider=AppValidator.validateForTranslation(settings);
        if(provider!=null) return "Provider/model: "+provider;
        String instruction=validateInstructionForStart(settings);
        if(instruction!=null) return instruction;
        for(Uri input:inputUris){String error=FileUtil.validateReadable(this,input,"TXT input");if(error!=null)return error;}
        boolean batch=inputUris.size()>1;
        String output=outputTreeUri!=null?FileUtil.validateTreeWritable(this,outputTreeUri,"Output folder")
                : (batch?FileUtil.validateTreeWritable(this,null,"Output folder"):FileUtil.validateWritable(this,outputUri,"Output TXT"));
        if(output!=null)return output;
        PromptContextBuilder.ParseReport locks=PromptContextBuilder.validate(settings.glossaryText,settings.pronounText);
        if(!locks.malformedGlossaryRows.isEmpty())return "Glossary: malformed entries="+locks.malformedGlossaryRows.size();
        if(!locks.malformedPronounRows.isEmpty())return "Pronoun: malformed rules="+locks.malformedPronounRows.size();
        return null;
    }

    void setButtonEnabled(Button b, boolean enabled) {
        if (b == null) return;
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1.0f : 0.45f);
    }

    void saveSettingsFromUi() {
        AppSettings saved = AppValidator.normalize(collectSettings());
        SettingsStore.save(this, saved);
        toast(saved.apiKey == null || saved.apiKey.trim().isEmpty() ? "Đã lưu settings" : "Đã lưu settings + API key");
        appendLog("Đã lưu settings" + ((saved.apiKey == null || saved.apiKey.trim().isEmpty()) ? "" : " + API key"));
        if (activeGlossaryLabel != null) activeGlossaryLabel.setText(activeGlossaryText());
        updateMetaLine();
        updateInputEstimate();
    }

    AppSettings collectSettings() {
        AppSettings s = SettingsStore.load(this);
        if (providerField != null) s.provider = providerField.getText().toString().trim();
        if (baseUrlField != null) s.baseUrl = AppSettings.normalizeEndpoint(baseUrlField.getText().toString().trim());
        if (apiKeyField != null) s.apiKey = apiKeyField.getText().toString().trim();
        if (modelField != null) s.model = modelField.getText().toString().trim();
        if (sourceField != null) s.sourceLanguage = sourceField.getText().toString().trim();
        if (targetField != null) s.targetLanguage = targetField.getText().toString().trim();
        if (outputPatternField != null) s.outputFilenamePattern = outputPatternField.getText().toString().trim();
        if (chunkModeField != null) s.chunkMode = chunkModeField.getText().toString().trim();
        if (optimizationPresetField != null) s.optimizationPreset = optimizationPresetField.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
        if (maxTokensField != null) s.maxTokensPerChunk = intVal(maxTokensField, s.maxTokensPerChunk);
        if (maxCharsField != null) s.maxCharsPerChunk = intVal(maxCharsField, s.maxCharsPerChunk);
        if (softRatioField != null) s.softLimitRatio = floatVal(softRatioField, s.softLimitRatio);
        if (contextField != null) s.contextChars = intVal(contextField, s.contextChars);
        if (timeoutField != null) s.timeoutSeconds = intVal(timeoutField, s.timeoutSeconds);
        if (attemptsField != null) s.maxAttempts = intVal(attemptsField, s.maxAttempts);
        if(initialRetryDelayField!=null)s.initialRetryDelayMs=intVal(initialRetryDelayField,s.initialRetryDelayMs);if(maxRetryDelayField!=null)s.maxRetryDelayMs=intVal(maxRetryDelayField,s.maxRetryDelayMs);
        if (tempField != null) s.temperature = floatVal(tempField, s.temperature);
        if (maxOutputField != null) s.maxOutputTokens = intVal(maxOutputField, s.maxOutputTokens);
        if (costLimitField != null) s.costLimitUsd = doubleVal(costLimitField, s.costLimitUsd);
        if(costWarningField!=null)s.costWarningUsd=doubleVal(costWarningField,s.costWarningUsd);if(maxRetryCostField!=null)s.maxRetryCostUsd=doubleVal(maxRetryCostField,s.maxRetryCostUsd);if(maxPaidRetriesField!=null)s.maxPaidRetries=intVal(maxPaidRetriesField,s.maxPaidRetries);
        if (glossaryLimitField != null) s.glossaryInjectLimit = intVal(glossaryLimitField, s.glossaryInjectLimit);
        if (pronounLimitField != null) s.pronounInjectLimit = intVal(pronounLimitField, s.pronounInjectLimit);
        if (refineBox != null) s.refineAfter = refineBox.isChecked();
        if (bilingualBox != null) s.bilingualOutput = bilingualBox.isChecked();
        if (partialBox != null) s.savePartialOutput = partialBox.isChecked();
        if (costLimitBox != null) s.stopOnCostLimit = costLimitBox.isChecked();
        if(overlapBox!=null)s.contextOverlapEnabled=overlapBox.isChecked();if(retryEmptyBox!=null)s.retryOnEmpty=retryEmptyBox.isChecked();if(retryTruncationBox!=null)s.retryOnTruncation=retryTruncationBox.isChecked();if(retryValidationBox!=null)s.retryOnValidationFailure=retryValidationBox.isChecked();if(stopUnknownPricingBox!=null)s.stopWhenPricingUnknown=stopUnknownPricingBox.isChecked();
        if (yamlUri != null) s.instructionUri = yamlUri.toString();
        if (lastInstructionName != null && !"—".equals(lastInstructionName)) s.instructionName = lastInstructionName;
        if (envUri != null) s.envUri = envUri.toString();
        if (pronounUri != null) s.pronounUri = pronounUri.toString();
        if (envFileLabel != null) {
            String lbl = envFileLabel.getText().toString();
            if (lbl.startsWith(".env: ")) s.envName = lbl.substring(6);
        }
        if (pronounUri != null) {
            s.pronounUri = pronounUri.toString();
            s.pronounName = FileUtil.displayName(this, pronounUri);
            if (s.pronounText == null || s.pronounText.trim().isEmpty()) {
                try { s.pronounText = FileUtil.readText(this, pronounUri); } catch (Exception ignored) {}
            }
        }
        if (outputUri != null) { s.outputUri = outputUri.toString(); s.outputName = FileUtil.displayName(this, outputUri); }
        if (outputTreeUri != null) { s.outputTreeUri = outputTreeUri.toString(); s.outputTreeName = FileUtil.treeName(outputTreeUri); }
        GlossaryStore.Glossary selected = GlossaryStore.selected(this);
        if (selected != null) {
            s.selectedGlossaryId = selected.id;
            s.selectedGlossaryName = selected.name;
            s.glossaryText = GlossaryStore.toPromptText(selected);
        } else if (lastGlossaryText != null && !lastGlossaryText.trim().isEmpty()) {
            s.glossaryText = lastGlossaryText;
        }
        PronounStore.Profile selectedPronoun=PronounStore.selected(this);
        if(selectedPronoun!=null){s.pronounUri=selectedPronoun.uri;s.pronounName=selectedPronoun.name;s.pronounText=selectedPronoun.text;s.selectedPronounId=selectedPronoun.id;s.selectedPronounName=selectedPronoun.name;}
        return AppValidator.normalize(s);
    }

    void loadSettingsToUi() { fillSettings(SettingsStore.load(this)); }

    void fillSettings(AppSettings s) {
        boolean wasHydrating = hydratingSettings;
        hydratingSettings = true;
        try {
            if (providerField != null) providerField.setText(s.provider);
            if (baseUrlField != null) baseUrlField.setText(s.baseUrl);
            if (apiKeyField != null) apiKeyField.setText(s.apiKey);
            if (modelField != null) modelField.setText(s.model);
            if (sourceField != null) sourceField.setText(s.sourceLanguage);
            if (targetField != null) targetField.setText(s.targetLanguage);
            if (outputPatternField != null) outputPatternField.setText(s.outputFilenamePattern);
            if (chunkModeField != null) chunkModeField.setText(s.chunkMode);
            if (optimizationPresetField != null) optimizationPresetField.setText(s.optimizationPreset);
            if (maxTokensField != null) maxTokensField.setText(String.valueOf(s.maxTokensPerChunk));
            if (maxCharsField != null) maxCharsField.setText(String.valueOf(s.maxCharsPerChunk));
            if (softRatioField != null) softRatioField.setText(String.valueOf(s.softLimitRatio));
            if (contextField != null) contextField.setText(String.valueOf(s.contextChars));
            if (timeoutField != null) timeoutField.setText(String.valueOf(s.timeoutSeconds));
            if (attemptsField != null) attemptsField.setText(String.valueOf(s.maxAttempts));
            if(initialRetryDelayField!=null)initialRetryDelayField.setText(String.valueOf(s.initialRetryDelayMs));if(maxRetryDelayField!=null)maxRetryDelayField.setText(String.valueOf(s.maxRetryDelayMs));
            if (tempField != null) tempField.setText(String.valueOf(s.temperature));
            if (maxOutputField != null) maxOutputField.setText(String.valueOf(s.maxOutputTokens));
            if (costLimitField != null) costLimitField.setText(s.costLimitUsd <= 0 ? "0" : String.valueOf(s.costLimitUsd));
            if(costWarningField!=null)costWarningField.setText(String.valueOf(s.costWarningUsd));if(maxRetryCostField!=null)maxRetryCostField.setText(String.valueOf(s.maxRetryCostUsd));if(maxPaidRetriesField!=null)maxPaidRetriesField.setText(String.valueOf(s.maxPaidRetries));
            if (glossaryLimitField != null) glossaryLimitField.setText(String.valueOf(s.glossaryInjectLimit));
            if (pronounLimitField != null) pronounLimitField.setText(String.valueOf(s.pronounInjectLimit));
            if (refineBox != null) refineBox.setChecked(s.refineAfter);
            if (bilingualBox != null) bilingualBox.setChecked(s.bilingualOutput);
            if (partialBox != null) partialBox.setChecked(s.savePartialOutput);
            if (costLimitBox != null) costLimitBox.setChecked(s.stopOnCostLimit);
            if(overlapBox!=null)overlapBox.setChecked(s.contextOverlapEnabled);if(retryEmptyBox!=null)retryEmptyBox.setChecked(s.retryOnEmpty);if(retryTruncationBox!=null)retryTruncationBox.setChecked(s.retryOnTruncation);if(retryValidationBox!=null)retryValidationBox.setChecked(s.retryOnValidationFailure);if(stopUnknownPricingBox!=null)stopUnknownPricingBox.setChecked(s.stopWhenPricingUnknown);
            if (s.instructionUri != null && !s.instructionUri.isEmpty()) yamlUri = Uri.parse(s.instructionUri);
            if (s.envUri != null && !s.envUri.isEmpty()) envUri = Uri.parse(s.envUri);
            if (s.outputUri != null && !s.outputUri.isEmpty()) outputUri = Uri.parse(s.outputUri);
            if (s.outputTreeUri != null && !s.outputTreeUri.isEmpty()) outputTreeUri = Uri.parse(s.outputTreeUri);
            if (s.pronounUri != null && !s.pronounUri.isEmpty()) pronounUri = Uri.parse(s.pronounUri);
            if (s.instructionName != null && !s.instructionName.isEmpty()) lastInstructionName = s.instructionName;
            if (s.selectedGlossaryName != null && !s.selectedGlossaryName.isEmpty()) lastGlossaryName = s.selectedGlossaryName;
            if (s.glossaryText != null && !s.glossaryText.isEmpty()) lastGlossaryText = s.glossaryText;
            TranslationConfigRepository configRepository=TranslationConfigRepository.get(this);
            TranslationConfigRepository.Entry instructionEntry=configRepository.entry(TranslationConfigRepository.Type.INSTRUCTION_YAML);
            TranslationConfigRepository.Entry environmentEntry=configRepository.entry(TranslationConfigRepository.Type.ENVIRONMENT);
            if (yamlFileLabel != null) yamlFileLabel.setText("Instructions: " + (instructionEntry.valid() ? instructionEntry.name + " • internal copy valid" : "chưa chọn"));
            if (envFileLabel != null) envFileLabel.setText(".env: " + (environmentEntry.valid() ? environmentEntry.name + " • internal copy valid" : "chưa chọn"));
            if (pronounFileLabel != null) pronounFileLabel.setText("Pronoun: " + (s.pronounName == null || s.pronounName.isEmpty() ? "chưa chọn" : s.pronounName));
            if (outputFileLabel != null) outputFileLabel.setText(outputUri == null ? "Single output TXT: chưa chọn" : "Single output TXT: " + FileUtil.displayName(this, outputUri) + " (" + FileUtil.accessBadge(this, outputUri, true) + ")");
            if (outputFolderLabel != null) outputFolderLabel.setText(outputTreeUri == null ? "Output folder: chưa chọn" : "Output folder: " + FileUtil.treeName(outputTreeUri) + " (" + FileUtil.accessBadge(this, outputTreeUri, true) + ")");
            if (activeGlossaryLabel != null) activeGlossaryLabel.setText(activeGlossaryText());
            updateMetaLine();
        } finally {
            hydratingSettings = wasHydrating;
        }
    }

    void requestNotificationPermission() {
        if ("benchmark".equals(BuildConfig.BUILD_TYPE)) return;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTI);
        }
    }

    void preset(String p) {
        if (providerField != null) providerField.setText(p);
        if (baseUrlField != null) baseUrlField.setText(ModelCatalog.defaultBaseUrl(p));
        saveSettingsFromUi();
        updateMetaLine();
        toast("Đã chọn preset " + p);
    }



    void quickTestApi() {
        saveRuntimeUris();
        AppSettings s = collectSettings();
        SettingsStore.save(this, s);
        toast("Đang test API...");
        appendLog("Quick test API: " + s.provider + " / " + s.model);
        new Thread(() -> {
            try {
                PromptPair p = new PromptPair("You are a connection tester. Reply only OK.", "Reply only: OK");
                OpenAICompatibleClient.ChatResult r = OpenAICompatibleClient.chatWithUsage(s, p);
                runOnUiThread(() -> {
                    toast("API OK • " + r.totalTokens + " tokens");
                    appendLog("API OK • tokens=" + r.totalTokens + " • response=" + r.content.trim());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    String err = AppValidator.readableError(e);
                    toast("API lỗi: " + err);
                    appendLog("API test lỗi: " + err);
                });
            }
        }).start();
    }

    void showLanguageProfilePicker() {
        final String[] labels = {
                "Japanese → Vietnamese Light Novel",
                "English → Vietnamese",
                "Chinese → Vietnamese Web Novel",
                "Korean → Vietnamese",
                "Vietnamese → English",
                "Custom"
        };
        new AlertDialog.Builder(this)
                .setTitle("Language profile")
                .setItems(labels, (dialog, which) -> {
                    String label = labels[which];
                    if (sourceField != null && targetField != null) {
                        if (which == 0) { sourceField.setText("Japanese"); targetField.setText("Vietnamese"); }
                        else if (which == 1) { sourceField.setText("English"); targetField.setText("Vietnamese"); }
                        else if (which == 2) { sourceField.setText("Chinese"); targetField.setText("Vietnamese"); }
                        else if (which == 3) { sourceField.setText("Korean"); targetField.setText("Vietnamese"); }
                        else if (which == 4) { sourceField.setText("Vietnamese"); targetField.setText("English"); }
                    }
                    AppSettings s = collectSettings();
                    s.languageProfile = label;
                    SettingsStore.save(this, s);
                    fillSettings(s);
                    toast("Đã chọn profile: " + label);
                    appendLog("Language profile: " + label);
                    updateInputEstimate();
                })
                .show();
    }

    void showProviderPicker() {
        final String[] providers = ModelCatalog.providers();
        new AlertDialog.Builder(this)
                .setTitle("Chọn AI provider")
                .setItems(providers, (dialog, which) -> {
                    String p = providers[which];
                    if (providerField != null) providerField.setText(p);
                    if (baseUrlField != null) baseUrlField.setText(ModelCatalog.defaultBaseUrl(p));
                    saveSettingsFromUi();
                    toast("Đã chọn provider: " + p);
                    updateMetaLine();
                })
                .show();
    }

    void showModelPicker() {
        String provider = providerField == null ? SettingsStore.load(this).provider : providerField.getText().toString().trim();
        if (provider.toLowerCase(Locale.ROOT).contains("openrouter")) {
            if (ModelCatalog.state().models.isEmpty()) ModelCatalog.refreshAsync(this);
            showModelDialog(ModelCatalog.availableModelInfos(provider), true);
        } else {
            showModelDialog(ModelCatalog.fallbackModelInfos(provider), false);
        }
    }

    void showModelDialog(List<ModelCatalog.ModelInfo> models, boolean fromOpenRouter) {
        final ArrayList<ModelCatalog.ModelInfo> all = new ArrayList<>();
        if (models != null) all.addAll(models);
        String selected = ModelCatalog.normalizeModelId(modelField == null ? SettingsStore.load(this).model : modelField.getText().toString());
        boolean selectedPresent = false;
        for (ModelCatalog.ModelInfo model : all) if (model.id.equals(selected)) { selectedPresent = true; break; }
        if (!selected.isEmpty() && !selectedPresent) {
            all.add(new ModelCatalog.ModelInfo(selected, "Saved model — currently unavailable in catalog",
                    0, false, 0, false, 0, false, 0,
                    providerField == null ? "" : providerField.getText().toString(),
                    "pricing-unavailable", 0, "saved model not present in latest catalog"));
        }
        sortModels(all);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        box.setPadding(pad, dp(4), pad, 0);
        EditText search = input("Search model name or exact ID", "");
        search.setSingleLine(true);
        box.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
        TextView catalogStatus = text(modelCatalogStatusLine(), 11, MUTED, false);
        box.addView(catalogStatus, marginLP(-1, -2, 0, 6, 0, 6));
        ListView list = new ListView(this);
        final ArrayList<ModelCatalog.ModelInfo> shown = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        box.addView(list, new LinearLayout.LayoutParams(-1, dp(520)));
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(fromOpenRouter ? "Select OpenRouter model" : "Select model")
                .setView(box).setPositiveButton("Refresh pricing", null)
                .setNeutralButton("Custom model", null).setNegativeButton("Close", null).create();

        Runnable refreshRows = () -> filterModelRows(all, shown, adapter, search.getText().toString(), selected);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshRows.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= shown.size()) return;
            selectModel(shown.get(position).id);
            dialog.dismiss();
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= shown.size()) return false;
            ModelCatalog.ModelInfo model = shown.get(position);
            boolean favorite = ModelPreferences.toggleFavorite(this, model.id);
            toast(favorite ? "Added to favorites" : "Removed from favorites");
            sortModels(all); refreshRows.run(); return true;
        });
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> refreshModelCatalog());
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> { dialog.dismiss(); promptCustomModel(); });
            refreshRows.run();
        });
        dialog.show();
    }

    void filterModelRows(List<ModelCatalog.ModelInfo> all, List<ModelCatalog.ModelInfo> shown,
                         ArrayAdapter<String> adapter, String query, String selected) {
        shown.clear(); adapter.clear();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        java.util.Set<String> favorites = ModelPreferences.favorites(this);
        for (ModelCatalog.ModelInfo model : all) {
            String haystack = (model.name + " " + model.id + " " + model.provider).toLowerCase(Locale.ROOT);
            if (!needle.isEmpty() && !haystack.contains(needle)) continue;
            shown.add(model);
            String prefix = model.id.equals(selected) ? "✓ " : favorites.contains(model.id) ? "★ " : "";
            adapter.add(prefix + model.display());
        }
        adapter.notifyDataSetChanged();
    }

    void sortModels(List<ModelCatalog.ModelInfo> models) {
        List<String> recent = ModelPreferences.recent(this);
        java.util.Set<String> favorites = ModelPreferences.favorites(this);
        models.sort((left, right) -> {
            int li = recent.indexOf(left.id), ri = recent.indexOf(right.id);
            if (li >= 0 || ri >= 0) {
                if (li < 0) return 1;
                if (ri < 0) return -1;
                if (li != ri) return Integer.compare(li, ri);
            }
            int favorite = Boolean.compare(favorites.contains(right.id), favorites.contains(left.id));
            if (favorite != 0) return favorite;
            int provider = left.provider.compareToIgnoreCase(right.provider);
            if (provider != 0) return provider;
            return left.name.compareToIgnoreCase(right.name);
        });
    }

    void selectModel(String rawId) {
        String id = ModelCatalog.normalizeModelId(rawId);
        if (id.isEmpty()) return;
        if (modelField != null) modelField.setText(id);
        AppSettings settings = SettingsStore.load(this);
        settings.model = id;
        if (providerField != null) settings.provider = providerField.getText().toString().trim();
        SettingsStore.save(this, AppValidator.normalize(settings));
        ModelPreferences.recordRecent(this, id);
        ObservabilityLog.event("model_selected", "provider", settings.provider, "model", id);
        toast("Selected model: " + id);
        updateMetaLine(); updateInputEstimate();
    }

    void promptCustomModel() {
        final EditText e = input("Nhập model id", modelField == null ? "" : modelField.getText().toString());
        e.setSingleLine(true);
        int pad = dp(20);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(e, new FrameLayout.LayoutParams(-1, dp(52)));
        new AlertDialog.Builder(this)
                .setTitle("Custom model")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String v = e.getText().toString().trim();
                    if (!v.isEmpty() && modelField != null) {
                        selectModel(v);
                        toast("Đã lưu custom model");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void addManualTerm() {
        if (!ensureConfigMutable()) return;
        String src = manualGlossarySource == null ? "" : manualGlossarySource.getText().toString().trim();
        String tgt = manualGlossaryTarget == null ? "" : manualGlossaryTarget.getText().toString().trim();
        String cat = manualGlossaryCategory == null ? "" : manualGlossaryCategory.getText().toString().trim();
        if (src.isEmpty() || tgt.isEmpty()) { toast("Cần nhập Source và Target"); return; }
        if (editingGlossary == null) editingGlossary = GlossaryStore.create(this, "New glossary");
        editingGlossary.terms.add(new GlossaryStore.Term(src, tgt, cat.isEmpty() ? "term" : cat));
        saveEditingGlossary();
        updateGlossaryPreview();
        manualGlossarySource.setText(""); manualGlossaryTarget.setText("");
        toast("Đã thêm thuật ngữ");
        appendLog("Thêm glossary term: " + src + " → " + tgt);
    }

    void chooseGlossaryMulti() {
        if (!ensureConfigMutable()) return;
        saveEditingGlossary();
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_GLOSSARY_MULTI_IMPORT);
    }

    void importTermsIntoEditingGlossary(Intent data) {
        if (editingGlossary == null) editingGlossary = GlossaryStore.create(this, "New glossary");
        int totalTerms = 0;
        ArrayList<Uri> selectedUris = new ArrayList<>();
        ArrayList<String> successfulNames = new ArrayList<>();
        ArrayList<String> importedNames = new ArrayList<>();
        ArrayList<String> failedNames = new ArrayList<>();
        GlossaryStore.Glossary staged = new GlossaryStore.Glossary();
        try {
            ClipData clip = data.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri u = clip.getItemAt(i).getUri();
                    if (u != null) selectedUris.add(u);
                }
            } else if (data.getData() != null) {
                selectedUris.add(data.getData());
            }
            for (Uri u : selectedUris) {
                String fileName = FileUtil.displayName(this, u);
                try {
                    FileUtil.takePersistable(this, u, Intent.FLAG_GRANT_READ_URI_PERMISSION, true, false);
                    String text = FileUtil.readText(this, u);
                    List<GlossaryStore.Term> terms = GlossaryStore.parseTerms(fileName, text);
                    if (terms.isEmpty()) throw new IllegalArgumentException("No valid glossary terms");
                    GlossaryStore.mergeTerms(staged, terms);
                    successfulNames.add(fileName);
                    importedNames.add(fileName + " (" + terms.size() + ")");
                    totalTerms += terms.size();
                } catch (Exception fileError) {
                    failedNames.add(fileName + ": " + fileError.getMessage());
                }
            }
            if (totalTerms == 0) throw new IllegalArgumentException("No valid glossary terms found");
            GlossaryStore.mergeTerms(editingGlossary, staged.terms);
            String suggestedName = GlossaryStore.suggestedImportName(editingGlossary.name, successfulNames);
            editingGlossary.name = suggestedName;
            if (glossaryNameField != null) glossaryNameField.setText(suggestedName);
            saveEditingGlossary();
            String imported = namesSummary(importedNames);
            String validation = PromptContextBuilder.validateText(GlossaryStore.toPromptText(editingGlossary), currentPronounText());
            GlossaryStore.Glossary importedGlossary = editingGlossary;
            selectGlossary(importedGlossary);
            toast("Đã import " + totalTerms + " terms từ: " + imported);
            String skipped = failedNames.isEmpty() ? "" : "\nBỏ qua " + failedNames.size() + " file: " + namesSummary(failedNames);
            appendLog("Glossary import: " + successfulNames.size() + " file(s), " + totalTerms + " term(s): " + imported + (failedNames.isEmpty() ? "" : " • skipped=" + failedNames.size()) + " • " + oneLine(validation));
            showResult("Glossary import validated", totalTerms + " term(s) từ " + imported + skipped + "\n" + validation);
        } catch (Exception e) {
            toast("Lỗi import glossary");
            appendLog("Lỗi import glossary: " + e.getMessage());
        }
    }

    void selectGlossary(GlossaryStore.Glossary g) {
        selectGlossary(g, true);
    }

    void selectGlossary(GlossaryStore.Glossary g, boolean preserveListScroll) {
        if (!ensureConfigMutable()) return;
        if (g == null) return;
        boolean preserveScroll = preserveListScroll && editingGlossary == null && "Glossaries".equals(currentTab);
        GlossaryStore.setSelectedId(this, g.id);
        AppSettings s = collectSettings();
        s.selectedGlossaryId = g.id;
        s.selectedGlossaryName = g.name;
        s.glossaryText = GlossaryStore.toPromptText(g);
        SettingsStore.save(this, s);
        lastGlossaryName = g.name;
        lastGlossaryText = s.glossaryText;
        if (activeGlossaryLabel != null) activeGlossaryLabel.setText(activeGlossaryText());
        toast("Đã chọn glossary: " + g.name);
        editingGlossary = null;
        if (preserveScroll) refreshPagePreservingScroll("Glossaries");
        else switchTab("Glossaries");
    }

    void saveEditingGlossary() {
        if (TranslatorService.isActive() || translationActive) return;
        if (editingGlossary == null) return;
        if (glossaryNameField != null) editingGlossary.name = glossaryNameField.getText().toString().trim();
        if (glossarySourceField != null) editingGlossary.sourceLang = glossarySourceField.getText().toString().trim();
        if (glossaryTargetField != null) editingGlossary.targetLang = glossaryTargetField.getText().toString().trim();
        if (editingGlossary.name == null || editingGlossary.name.trim().isEmpty()) editingGlossary.name = "New glossary";
        GlossaryStore.upsert(this, editingGlossary);
    }

    void updateGlossaryPreview() {
        if (glossaryPreview == null || editingGlossary == null) return;
        StringBuilder sb = new StringBuilder();
        String validation = PromptContextBuilder.validateText(GlossaryStore.toPromptText(editingGlossary), currentPronounText());
        sb.append("[VALIDATION]\n").append(validation).append("\n\n[TERMS PREVIEW]\n");
        int shown = 0;
        for (GlossaryStore.Term t : editingGlossary.terms) {
            if (shown >= 80) { sb.append("...\n"); break; }
            sb.append(t.source).append(" → ").append(t.target);
            if (t.category != null && !t.category.isEmpty()) sb.append("  [").append(t.category).append("]");
            sb.append('\n');
            shown++;
        }
        glossaryPreview.setText(editingGlossary.terms == null || editingGlossary.terms.isEmpty() ? "No terms yet. Import CSV/TXT/JSON or add manually.\n\n" + validation : sb.toString());
    }


    String activeGlossaryPromptText() {
        if (editingGlossary != null) return GlossaryStore.toPromptText(editingGlossary);
        GlossaryStore.Glossary selected = GlossaryStore.selected(this);
        if (selected != null) return GlossaryStore.toPromptText(selected);
        if (lastGlossaryText != null && !lastGlossaryText.trim().isEmpty()) return lastGlossaryText;
        AppSettings saved = SettingsStore.load(this);
        return saved.glossaryText == null ? "" : saved.glossaryText;
    }

    String currentPronounText() {
        PronounStore.Profile selected=PronounStore.selected(this);if(selected!=null)return selected.text;
        AppSettings saved = SettingsStore.load(this);
        if (saved.pronounText != null && !saved.pronounText.trim().isEmpty()) return saved.pronounText;
        try {
            if (pronounUri != null) return FileUtil.readText(this, pronounUri);
        } catch (Exception ignored) {}
        return "";
    }

    void showGlossaryPronounHealth() {
        String glossary = activeGlossaryPromptText();
        String pronoun = currentPronounText();
        PromptContextBuilder.ParseReport r = PromptContextBuilder.validate(glossary, pronoun);
        StringBuilder sb = new StringBuilder();
        sb.append(r.summaryText());
        sb.append("\n\nPriority rule:\n");
        sb.append("- File Pronoun riêng ưu tiên hơn ghi chú xưng hô trong glossary.\n");
        sb.append("- Ghi chú xưng hô trong glossary vẫn được giữ làm name/character lock nếu có file Pronoun riêng.\n");
        sb.append("- Prompt chỉ inject rule match với chunk, không nhét toàn bộ glossary/pronoun.\n");
        showLongText("Glossary / Pronoun health", sb.toString());
        appendLog("Glossary/pronoun health checked • " + oneLine(r.summaryText()));
    }

    String activeGlossaryText() {
        GlossaryStore.Glossary g = GlossaryStore.selected(this);
        if (g == null) return "Active glossary: —";
        return "Active glossary: " + g.name + " • " + g.count() + " terms";
    }

    AppSettings settingsForTranslationDisplay() {
        AppSettings editable = collectSettings();
        if (TranslatorService.isActive()) {
            Intent runtime = RuntimeStateStore.toIntent(this);
            long jobId = runtime == null ? -1L : runtime.getLongExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, -1L);
            if (jobId > 0) {
                TranslationRepository repo = new TranslationRepository(this);
                try {
                    TranslationRepository.Job job = repo.getJob(jobId);
                    if (job != null) return TranslationConfigState.selectSettings(
                            editable, SettingsStore.fromJson(job.settingsJson), true);
                } finally {
                    repo.close();
                }
            }
        }
        return TranslationConfigState.selectSettings(editable, null, false);
    }

    TranslationConfigState translationConfigState() {
        boolean active = TranslatorService.isActive();
        Intent runtime = RuntimeStateStore.toIntent(this);
        long jobId = active && runtime != null
                ? runtime.getLongExtra(TranslatorService.EXTRA_ACTIVE_JOB_ID, -1L) : -1L;
        AppSettings s = settingsForTranslationDisplay();
        PromptContextBuilder.ParseReport report = PromptContextBuilder.validate(s.glossaryText, s.pronounText);
        int glossaryWarnings = report.conflicts.size() + report.malformedGlossaryRows.size();
        int pronounWarnings = report.malformedPronounRows.size();

        boolean hasTranslation = s.translationInstructions != null && !s.translationInstructions.trim().isEmpty();
        boolean hasRefinement = s.refinementInstructions != null && !s.refinementInstructions.trim().isEmpty();
        String instructionError = "";
        if (!active && s.instructionName != null && !s.instructionName.trim().isEmpty()) {
            TranslationConfigRepository repository=TranslationConfigRepository.get(this);
            TranslationConfigRepository.Entry internal=repository.entry(TranslationConfigRepository.Type.INSTRUCTION_YAML);
            if (!internal.valid()) instructionError="Instruction YAML internal copy is unavailable; import it again.";
            else try {
                CustomInstructions parsed=YamlInstructionParser.parse(internal.name,repository.read(TranslationConfigRepository.Type.INSTRUCTION_YAML));
                hasTranslation=parsed.hasTranslation(); hasRefinement=parsed.hasRefinement();
            } catch(Exception e) { instructionError="Instruction YAML internal copy failed: "+e.getMessage(); }
        } else if (!active && s.instructionUri != null && !s.instructionUri.trim().isEmpty()) {
            instructionError="Instruction YAML still references an external URI; import it again.";
        }

        TranslationConfigState.Item glossary = TranslationConfigState.glossary(
                s.selectedGlossaryName, report.glossaryTerms.size(), glossaryWarnings);
        TranslationConfigState.Item pronoun = TranslationConfigState.pronoun(
                s.pronounName, report.explicitPronouns.size(), pronounWarnings);
        TranslationConfigState.Item instruction = TranslationConfigState.instruction(
                s.instructionName, hasTranslation, hasRefinement, s.refineAfter, instructionError);
        return new TranslationConfigState(glossary, pronoun, instruction, active && jobId > 0, jobId);
    }

    void refreshTranslationConfigViews() {
        TranslationConfigState state = translationConfigState();
        if (translateGlossaryChip != null) translateGlossaryChip.setText(state.glossary.displayText());
        if (translatePronounChip != null) translatePronounChip.setText(state.pronoun.displayText());
        if (translateInstructionChip != null) translateInstructionChip.setText(state.instruction.displayText());
    }

    void showTranslationConfig(String kind) {
        AppSettings s = settingsForTranslationDisplay();
        TranslationConfigState state = translationConfigState();
        if ("glossary".equals(kind)) {
            showLongText("Glossaries • " + state.glossary.statusLabel(),
                    state.glossary.displayText() + "\n\n" + nonEmpty(s.glossaryText, "Không sử dụng"));
        } else if ("pronoun".equals(kind)) {
            showLongText("Pronoun • " + state.pronoun.statusLabel(),
                    state.pronoun.displayText() + "\n\n" + nonEmpty(s.pronounText, "Không sử dụng"));
        } else {
            String body = state.instruction.displayText()
                    + "\n\n[TRANSLATION]\n" + nonEmpty(s.translationInstructions, "Không sử dụng")
                    + "\n\n[REFINEMENT]\n" + nonEmpty(s.refinementInstructions, "Không sử dụng");
            showLongText("Instruction YAML • " + state.instruction.statusLabel(), body);
        }
    }

    String validateInstructionForStart(AppSettings s) {
        if (s == null) return "Instruction settings không hợp lệ";
        boolean hasTranslation = s.translationInstructions != null && !s.translationInstructions.trim().isEmpty();
        boolean hasRefinement = s.refinementInstructions != null && !s.refinementInstructions.trim().isEmpty();
        String uriText = s.instructionUri == null ? "" : s.instructionUri.trim();
        TranslationConfigRepository repository=TranslationConfigRepository.get(this);
        TranslationConfigRepository.Entry internal=repository.entry(TranslationConfigRepository.Type.INSTRUCTION_YAML);
        if (s.instructionName != null && !s.instructionName.trim().isEmpty()) {
            if (!internal.valid()) return "Instruction YAML internal copy is unavailable; import the YAML again.";
            try {
                CustomInstructions parsed=YamlInstructionParser.parse(internal.name,repository.read(TranslationConfigRepository.Type.INSTRUCTION_YAML));
                if(!parsed.hasTranslation()&&!parsed.hasRefinement()) return "Instruction YAML internal copy has no translation or refinement block";
                return null;
            } catch(Exception e) { return "Instruction YAML internal copy failed validation: "+e.getMessage(); }
        }
        if (uriText.isEmpty()) {
            return null; // Optional and not selected, or embedded profile instructions.
        }
        return "Instruction YAML still references an external URI; import it again to create an internal copy.";
    }


    String currentInputLabel() {
        if (inputUris.isEmpty()) return "Chưa chọn file TXT";
        if (inputUris.size() == 1) return "📄 " + FileUtil.displayName(this, inputUris.get(0));
        return "📚 " + inputUris.size() + " TXT files selected";
    }

    String inputUrisJson() {
        JSONArray arr = new JSONArray();
        try { for (Uri u : inputUris) arr.put(u.toString()); } catch (Exception ignored) {}
        return arr.toString();
    }



    CostEstimator.Estimate estimateCurrentBatch(AppSettings s) {
        PreparedBatch batch=currentPreparedBatch();
        if(batch==null||!batch.ready())return null;
        CostEstimator.applyPricing(batch.estimate,s);
        return batch.estimate;
    }

    void showPromptPreview() {
        if (inputUris.isEmpty()) { toast("Chưa chọn TXT để preview prompt"); return; }
        PromptPreviewDialog.showForInput(
                this,
                inputUris.get(0),
                collectSettings(),
                TEXT, MUTED, FIELD, BORDER,
                this::appendLog,
                this::toast
        );
    }

    void updateInputEstimate() {
        if (estimateText == null || estimatePanel == null) return;
        boolean usePreparedPipeline = true;
        if (usePreparedPipeline) {
            if (inputUris.isEmpty()) {
                PreparationCoordinator.get(this).cancel(); preparedBatch=null; lastEstimate=null;
                estimateText.setText("0 chunks\nEstimated tokens unavailable\nEstimated cost unavailable");
                estimatePanel.setVisibility(View.VISIBLE);
                if(pricingRetryButton!=null)pricingRetryButton.setVisibility(View.GONE);
                updateActionButtons(); return;
            }
            AppSettings settings=collectSettings();
            estimateText.setText("Preparing chunks…");
            estimatePanel.setVisibility(View.VISIBLE);
            PreparationCoordinator.get(this).request(inputUris,settings,this::renderPreparedBatch);
            updateActionButtons(); return;
        }
        if (inputUris.isEmpty()) {
            estimateText.setText("Estimated tokens: select an input TXT to calculate\nEstimated cost: waiting for input");
            estimatePanel.setVisibility(View.VISIBLE);
            if (pricingRetryButton != null) pricingRetryButton.setVisibility(View.VISIBLE);
            updateModelDiagnostics();
            return;
        }
        final int seq = ++estimateSeq;
        final ArrayList<Uri> snapshot = new ArrayList<>(inputUris);
        final AppSettings s = collectSettings();
        estimateText.setText("Estimated tokens: calculating…\nEstimated cost: checking model pricing…");
        estimatePanel.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                double low = 0, high = 0, noCacheLow = 0, noCacheHigh = 0;
                int tokens = 0, inputLow = 0, inputHigh = 0, outputLow = 0, outputHigh = 0;
                int chunks = 0, words = 0, chars = 0, lines = 0;
                int breakdownSource = 0, breakdownInstruction = 0, breakdownGlossary = 0, breakdownPronoun = 0, breakdownContext = 0;
                int expectedCached = 0, expectedUncached = 0;
                boolean cachePricingAvailable = false;
                boolean pricingAvailable = true;
                boolean genericTokenizer = false;
                String firstEncoding = "";
                for (Uri u : snapshot) {
                    FileUtil.TextReadResult rr = FileUtil.readTextDetailed(this, u);
                    String text = rr.text;
                    if (firstEncoding.isEmpty()) firstEncoding = rr.encoding;
                    FileUtil.TextStats stats = FileUtil.textStats(text);
                    CostEstimator.Estimate e = CostEstimator.estimate(text, s);
                    if (e.pricingAvailable) {
                        low += e.costLow; high += e.costHigh;
                        noCacheLow += e.noCacheCostLow; noCacheHigh += e.noCacheCostHigh;
                    } else pricingAvailable = false;
                    tokens += e.sourceTokens;
                    inputLow += e.inputTokensLow; inputHigh += e.inputTokensHigh;
                    outputLow += e.outputTokensLow; outputHigh += e.outputTokensHigh;
                    chunks += e.chunks;
                    words += stats.words;
                    chars += stats.characters;
                    lines += stats.lines;
                    breakdownSource += e.breakdownSource; breakdownInstruction += e.breakdownInstruction;
                    breakdownGlossary += e.breakdownGlossary; breakdownPronoun += e.breakdownPronoun;
                    breakdownContext += e.breakdownContext;
                    expectedCached += e.expectedCachedInputTokens; expectedUncached += e.expectedUncachedInputTokens;
                    cachePricingAvailable |= e.cachePricingAvailable;
                    genericTokenizer |= e.genericTokenizer;
                }
                ModelCatalog.ModelInfo m = ModelCatalog.findModelInfo(s.provider, s.model);
                ModelCatalog.CatalogState catalogState = ModelCatalog.state();
                int totalLow = (int)Math.min(Integer.MAX_VALUE, (long)inputLow + outputLow);
                int totalHigh = (int)Math.min(Integer.MAX_VALUE, (long)inputHigh + outputHigh);
                String costLine;
                if (pricingAvailable && m.hasPricing()) {
                    costLine = CostEstimator.money(low) + " – " + CostEstimator.money(high);
                } else if (catalogState.loadingState == ModelCatalog.LoadingState.LOADING) {
                    costLine = "Loading model pricing…";
                } else if (catalogState.loadingState == ModelCatalog.LoadingState.ERROR) {
                    costLine = "Pricing unavailable — token estimate is still valid";
                } else costLine = "Pricing unavailable";
                String label = "Estimated input: " + CostEstimator.tokenRange(inputLow, inputHigh)
                        + "\nEstimated output: " + CostEstimator.tokenRange(outputLow, outputHigh)
                        + " (configured max " + CostEstimator.fmt((long)Math.max(1, chunks) * s.maxOutputTokens) + ")"
                        + "\nEstimated total: " + CostEstimator.tokenRange(totalLow, totalHigh)
                        + "\nEstimated cost: " + costLine
                        + (cachePricingAvailable && pricingAvailable ? "\nCold/no-cache: " + CostEstimator.money(noCacheLow) + " – " + CostEstimator.money(noCacheHigh) : "")
                        + "\nTokenizer: " + (genericTokenizer ? "generic approximation" : "catalog model approximation")
                        + "\nPricing source: " + m.pricingSource
                        + "\n\n" + CostEstimator.fmt(words) + " words"
                        + " • " + CostEstimator.fmt(chars) + " chars"
                        + " • " + CostEstimator.fmt(lines) + " lines"
                        + " • " + CostEstimator.fmt(tokens) + " source tokens"
                        + " • " + chunks + " chunks"
                        + (firstEncoding.isEmpty() ? "" : " • " + firstEncoding)
                        + "\n" + m.priceSuffix()
                        + "\nPrompt Breakdown"
                        + "\nSource: " + CostEstimator.fmt(breakdownSource)
                        + "  •  Instruction: " + CostEstimator.fmt(breakdownInstruction)
                        + "\nGlossary: " + CostEstimator.fmt(breakdownGlossary)
                        + "  •  Pronoun: " + CostEstimator.fmt(breakdownPronoun)
                        + "  •  Context: " + CostEstimator.fmt(breakdownContext)
                        + "\nPrompt input: " + CostEstimator.fmt(expectedCached + expectedUncached)
                        + (cachePricingAvailable ? "  •  Expected cached: " + CostEstimator.fmt(expectedCached) : "");
                if (snapshot.size() > 1) label = snapshot.size() + " files • " + label;
                CostEstimator.Estimate aggregate = new CostEstimator.Estimate();
                aggregate.sourceTokens=tokens; aggregate.chunks=chunks; aggregate.inputTokensLow=inputLow; aggregate.inputTokensHigh=inputHigh;
                aggregate.outputTokensLow=outputLow; aggregate.outputTokensHigh=outputHigh; aggregate.totalTokensLow=totalLow; aggregate.totalTokensHigh=totalHigh;
                aggregate.breakdownSource=breakdownSource; aggregate.breakdownInstruction=breakdownInstruction; aggregate.breakdownGlossary=breakdownGlossary;
                aggregate.breakdownPronoun=breakdownPronoun; aggregate.breakdownContext=breakdownContext; aggregate.expectedCachedInputTokens=expectedCached;
                aggregate.expectedUncachedInputTokens=expectedUncached; aggregate.costLow=pricingAvailable?low:Double.NaN; aggregate.costHigh=pricingAvailable?high:Double.NaN;
                aggregate.noCacheCostLow=pricingAvailable?noCacheLow:Double.NaN; aggregate.noCacheCostHigh=pricingAvailable?noCacheHigh:Double.NaN;
                aggregate.pricingAvailable=pricingAvailable&&m.hasPricing(); aggregate.cachePricingAvailable=cachePricingAvailable; aggregate.genericTokenizer=genericTokenizer;
                aggregate.tokenizerState=genericTokenizer?"generic approximation":"catalog model approximation"; aggregate.pricingSource=m.pricingSource;
                final String finalLabel = label;
                runOnUiThread(() -> {
                    if (seq != estimateSeq || estimateText == null || estimatePanel == null) return;
                    lastEstimate = aggregate;
                    lastEstimateModelId = s.model;
                    estimateText.setText(finalLabel);
                    estimatePanel.setVisibility(View.VISIBLE);
                    if (pricingRetryButton != null) pricingRetryButton.setVisibility((!aggregate.pricingAvailable || catalogState.loadingState == ModelCatalog.LoadingState.ERROR) ? View.VISIBLE : View.GONE);
                    updateModelDiagnostics();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (seq != estimateSeq || estimateText == null || estimatePanel == null) return;
                    estimateText.setText("Estimated: không đọc được file để ước tính");
                    estimatePanel.setVisibility(View.VISIBLE);
                    if (pricingRetryButton != null) pricingRetryButton.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    PreparedBatch currentPreparedBatch() {
        PreparedBatch current = PreparationCoordinator.get(this).current();
        return current != null ? current : preparedBatch;
    }

    void renderPreparedBatch(PreparedBatch batch) {
        if(estimateText==null||estimatePanel==null||batch==null)return;
        // A cancelled preparation can still post a callback after a newer preparation.
        // Always render the coordinator's current batch; otherwise a stale PREPARING
        // instance can overwrite READY and keep Start incorrectly disabled.
        PreparedBatch current = PreparationCoordinator.get(this).current();
        if (current != null && batch != current) return;
        preparedBatch=current != null ? current : batch;
        batch=preparedBatch;
        if(batch.status==PreparedBatch.Status.PREPARING){
            estimateText.setText("Preparing chunks: "+batch.progress+"%");
        }else if(batch.status==PreparedBatch.Status.ERROR){
            estimateText.setText("Chunk preparation failed");
            appendLog("Preparation failed: "+batch.error);
        }else if(batch.ready()){
            CostEstimator.Estimate e=batch.estimate;lastEstimate=e;lastEstimateModelId=collectSettings().model;
            estimateText.setText(TranslationDashboardFormatter.prepared(e, batch.exactChunkCount()));
            if(pricingRetryButton!=null)pricingRetryButton.setVisibility(View.GONE);
            updateModelDiagnostics();
        }
        updateActionButtons();
    }

    void updatePricingOnly(){
        if(inputUris.isEmpty())return;
        PreparationCoordinator.get(this).repriceCurrent(collectSettings(),this::renderPreparedBatch);
    }

    void updateMetaLine() {
        AppSettings s = collectSettings();
        cachedProviderMeta = providerMeta(s);
        if (translateMeta != null) translateMeta.setText(nonEmpty(s.provider, "Provider") + " · " + nonEmpty(s.model, "Model chưa chọn") + " · " + nonEmpty(s.sourceLanguage, "Source") + " → " + nonEmpty(s.targetLanguage, "Target"));
        refreshTranslationConfigViews();
        if (modelPriceLabel != null) {
            ModelCatalog.ModelInfo mi = ModelCatalog.findModelInfo(s.provider, s.model);
            modelPriceLabel.setText("Price: " + mi.priceSuffix() + " • " + mi.pricingSource);
        }
        updateModelDiagnostics();
    }

    String modelCatalogStatusLine() {
        ModelCatalog.CatalogState state = ModelCatalog.state();
        if (state.loadingState == ModelCatalog.LoadingState.LOADING) return "Loading model pricing…";
        if (state.loadingState == ModelCatalog.LoadingState.ERROR) return "Pricing refresh failed; cached catalog retained";
        if (state.models.isEmpty()) return "Offline fallback catalog";
        return state.models.size() + " models • " + (state.fromCache ? (state.cacheExpired(System.currentTimeMillis()) ? "stale cached catalog" : "cached catalog") : "live catalog");
    }

    void refreshModelCatalog() {
        toast("Refreshing model catalog…");
        ModelCatalog.refreshAsync(this);
        updateInputEstimate();
    }

    void updateModelDiagnostics() {
        if (modelDiagnosticsText == null) return;
        modelDiagnosticsText.setText(buildModelDiagnostics());
    }

    String buildModelDiagnostics() {
        AppSettings s = collectSettings();
        ModelCatalog.ModelInfo model = ModelCatalog.findModelInfo(s.provider, s.model);
        ModelCatalog.CatalogState catalog = ModelCatalog.state();
        CostEstimator.Estimate estimate = s.model.equals(lastEstimateModelId) ? lastEstimate : null;
        String refreshed = catalog.lastSuccessfulRefresh <= 0 ? "never" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(catalog.lastSuccessfulRefresh));
        StringBuilder out = new StringBuilder();
        out.append("Provider: ").append(s.provider).append('\n');
        out.append("Exact model ID: ").append(s.model).append('\n');
        out.append("In active catalog: ").append(ModelCatalog.existsInActiveCatalog(s.model) ? "yes" : "no").append('\n');
        out.append("Pricing source: ").append(model.pricingSource).append('\n');
        out.append("Input / output / cached per M: ").append(model.priceSuffix()).append('\n');
        out.append("Context length: ").append(model.contextLength > 0 ? CostEstimator.fmt(model.contextLength) : "unknown").append('\n');
        out.append("Last pricing refresh: ").append(refreshed).append('\n');
        out.append("Catalog state: ").append(catalog.loadingState).append(catalog.error.isEmpty() ? "" : " — " + catalog.error).append('\n');
        out.append("Tokenizer: ").append(estimate == null ? "not calculated" : estimate.tokenizerState).append('\n');
        out.append("Estimated input: ").append(estimate == null ? "not calculated" : CostEstimator.tokenRange(estimate.inputTokensLow, estimate.inputTokensHigh)).append('\n');
        out.append("Estimated output: ").append(estimate == null ? "not calculated" : CostEstimator.tokenRange(estimate.outputTokensLow, estimate.outputTokensHigh));
        return out.toString();
    }

    void copyModelDiagnostics() {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Model diagnostics", buildModelDiagnostics()));
        toast("Model diagnostics copied (API key excluded)");
    }

    void refreshFileSummary() {
        if (filesSummary != null) filesSummary.setText(fileSummaryText());
    }

    void refreshFilesPage() {
        invalidatePage("Files");
        switchTab("Files");
        final Uri tree = outputTreeUri;
        final LinearLayout target = outputFolderFilesList;
        if (tree == null || target == null) return;
        target.removeAllViews();
        target.addView(text("Đang kiểm tra quyền và quét file…", 13, MUTED, false));
        preflightExecutor.submit(() -> {
            String permission = FileUtil.validateTreeWritable(this, tree, "Output folder");
            List<FileUtil.TreeEntry> files = permission == null ? FileUtil.listFilesInTree(this, tree, 50) : java.util.Collections.emptyList();
            runOnUiThread(() -> {
                if (target != outputFolderFilesList || isFinishing() || isDestroyed()) return;
                new FilesPageFactory(this).renderOutputFolderFiles(target, permission, files);
            });
        });
    }

    String fileSummaryText() {
        String in = inputUris.isEmpty() ? "—" : inputUris.size() + " TXT file(s) selected";
        AppSettings saved = SettingsStore.load(this);
        String out = outputUri == null ? "—" : nonEmpty(saved.outputName, "output.txt") + " (" + FileUtil.accessBadge(this, outputUri, true) + ")";
        String folder = outputTreeUri == null ? "—" : FileUtil.treeName(outputTreeUri) + " (" + FileUtil.accessBadge(this, outputTreeUri, true) + ")";
        return "Input: " + in + "\nSingle output: " + out + "\nOutput folder: " + folder;
    }

    String sampleConfig() {
        return "# tbl_android.env\n" +
                "LLM_PROVIDER=openrouter\n" +
                "OPENROUTER_API_KEY=sk-or-v1-...\n" +
                "DEFAULT_MODEL=openai/gpt-5.4-mini\n" +
                "DEFAULT_SOURCE_LANGUAGE=Japanese\n" +
                "DEFAULT_TARGET_LANGUAGE=Vietnamese\n" +
                "OUTPUT_FILENAME_PATTERN={originalName} ({targetLang}).{ext}\n" +
                "CHUNK_MODE=token\n" +
                "MAX_TOKENS_PER_CHUNK=450\n" +
                "MAX_CHARS_PER_CHUNK=0\n" +
                "SOFT_LIMIT_RATIO=0.8\n" +
                "REQUEST_TIMEOUT=300\n" +
                "TEMPERATURE=0.3\n" +
                "STOP_ON_COST_LIMIT=false\n" +
                "COST_LIMIT_USD=0\n\n" +
                "# lightnovel.yaml\n" +
                "translation: |-\n" +
                "  Dịch Nhật sang Việt tự nhiên, giữ dấu thoại Nhật.\n\n" +
                "refinement: |-\n" +
                "  Đối chiếu raw và bản dịch, sửa sai ý, sót ý, xưng hô.\n";
    }


    void restoreRuntimeStateToUi() {
        Intent last = RuntimeStateStore.toIntent(this);
        if (last == null) return;
        int p = last.getIntExtra(TranslatorService.EXTRA_PROGRESS, -1);
        String status = last.getStringExtra(TranslatorService.EXTRA_STATUS);
        String log = last.getStringExtra(TranslatorService.EXTRA_LOG);
        if (p >= 0 && progress != null) progress.setProgress(p);
        updateTrackingFromIntent(last);
        boolean active = TranslatorService.isActive();
        translationActive = TranslationUiStatePolicy.isTranslationActive(active, status);
        updateActionButtons();
        refreshTopStatusChip();
        if (statusBanner != null && log != null && !log.trim().isEmpty()) {
            statusBanner.setText(statusText(status, log));
            statusBanner.setVisibility(View.VISIBLE);
            tint(statusBanner, statusColor(status), 1, 14);
        }
        if ("done".equals(status) && resultCard != null) {
            showResult("Output ready", "Saved and verified");
        }
    }

    void updateTrackingFromIntent(Intent intent) {
        if (trackingCard != null) trackingCard.setVisibility(View.VISIBLE);
        String state = intent.getStringExtra(TranslatorService.EXTRA_STATE);
        if (state == null || state.trim().isEmpty()) state = "IDLE";
        String file = intent.getStringExtra(TranslatorService.EXTRA_FILE_NAME);
        int total = intent.getIntExtra(TranslatorService.EXTRA_TOTAL_CHUNKS, 0);
        int completed = intent.getIntExtra(TranslatorService.EXTRA_COMPLETED, 0);
        int failed = intent.getIntExtra(TranslatorService.EXTRA_FAILED, 0);
        int fallbacks = intent.getIntExtra(TranslatorService.EXTRA_FALLBACKS, 0);
        int tokens = intent.getIntExtra(TranslatorService.EXTRA_TOTAL_TOKENS, 0);
        double cost = intent.getDoubleExtra(TranslatorService.EXTRA_TOTAL_COST, 0d);
        int estimatedTokens = intent.getIntExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_TOKENS, 0);
        double estimatedCost = intent.getDoubleExtra(TranslatorService.EXTRA_ESTIMATED_TOTAL_COST, 0d);
        boolean providerUsageComplete = intent.getBooleanExtra(TranslatorService.EXTRA_PROVIDER_USAGE_COMPLETE, false);
        long elapsed = intent.getLongExtra(TranslatorService.EXTRA_ELAPSED_MS, 0L);
        long remain = intent.getLongExtra(TranslatorService.EXTRA_REMAINING_MS, 0L);
        int currentChunk = intent.getIntExtra(TranslatorService.EXTRA_CURRENT_CHUNK, 0);
        int glossaryLocks = intent.getIntExtra(TranslatorService.EXTRA_GLOSSARY_LOCKS, -1);
        int pronounLocks = intent.getIntExtra(TranslatorService.EXTRA_PRONOUN_LOCKS, -1);
        int lockChunk = intent.getIntExtra(TranslatorService.EXTRA_LOCK_CHUNK, 0);
        String lockPhase = intent.getStringExtra(TranslatorService.EXTRA_LOCK_PHASE);
        String preview = intent.getStringExtra(TranslatorService.EXTRA_PREVIEW);
        int previewChunk = intent.getIntExtra(TranslatorService.EXTRA_PREVIEW_CHUNK, 0);
        timedRuntimeState = state;
        timedElapsedMs = Math.max(0L, elapsed);
        timedRemainingMs = remain;
        timedAtRealtime = SystemClock.elapsedRealtime();
        if (trackingTitle != null) trackingTitle.setText(dashboardTitle(state));
        if (trackingFile != null) trackingFile.setText(nonEmpty(file, "Translation session"));
        if (metricChunks != null) metricChunks.setText(String.valueOf(total));
        if (metricCompleted != null) metricCompleted.setText(TranslationDashboardFormatter.progress(completed, total));
        if (metricFailed != null) metricFailed.setText(failed + " failed");
        if (metricFallbacks != null) metricFallbacks.setText(fallbacks + " fallbacks");
        if (metricElapsed != null) metricElapsed.setText("Elapsed " + CostEstimator.duration(elapsed));
        if (metricRemaining != null) metricRemaining.setText(TranslationDashboardFormatter.remaining(remain));
        if (metricCost != null) metricCost.setText(TranslationDashboardFormatter.cost(state, cost, estimatedCost, providerUsageComplete));
        if (metricTokens != null) metricTokens.setText(TranslationDashboardFormatter.tokens(state, tokens, estimatedTokens, providerUsageComplete));
        if (metricCurrentChunk != null) metricCurrentChunk.setText(TranslationDashboardFormatter.currentChunk(currentChunk, total));
        if (metricGlossaryLocks != null) metricGlossaryLocks.setText(TranslationDashboardFormatter.lockCount(glossaryLocks));
        if (metricPronounLocks != null) metricPronounLocks.setText(TranslationDashboardFormatter.lockCount(pronounLocks));
        if (lockUsageMeta != null) lockUsageMeta.setText(TranslationDashboardFormatter.lockMeta(lockPhase, lockChunk, total));
        if (previewMeta != null) previewMeta.setText(TranslationDashboardFormatter.previewMeta(previewChunk, total, preview));
        if (previewText != null) previewText.setText(preview == null || preview.trim().isEmpty()
                ? "The latest accepted translation will appear here." : preview);
        updateRuntimeTimer();
    }

    boolean shouldRunRuntimeTimer() {
        return appInForeground && "Translate".equals(currentTab) && "RUNNING".equalsIgnoreCase(timedRuntimeState);
    }

    void updateRuntimeTimer() {
        runtimeTimerHandler.removeCallbacks(runtimeTimerTick);
        if (shouldRunRuntimeTimer()) runtimeTimerHandler.postDelayed(runtimeTimerTick, 3000L);
    }

    String dashboardTitle(String state) {
        if ("COMPLETED".equalsIgnoreCase(state)) return "Completed";
        if ("PAUSED".equalsIgnoreCase(state)) return "Paused";
        if ("FAILED".equalsIgnoreCase(state)) return "Failed";
        if ("VALIDATING".equalsIgnoreCase(state)) return "Starting";
        return "Running";
    }

    String statusText(String status, String log) {
        if ("done".equals(status)) return "✅ " + log;
        if ("error".equals(status)) return "⚠ " + log;
        if ("paused".equals(status)) return "⏸ " + log;
        if ("stopping".equals(status)) return "⏳ " + log;
        if ("cancelled".equals(status)) return "✕ " + log;
        return "▶ " + log;
    }

    int statusColor(String status) {
        if ("done".equals(status)) return Color.rgb(16, 74, 48);
        if ("error".equals(status)) return Color.rgb(95, 40, 42);
        if ("paused".equals(status)) return Color.rgb(91, 68, 29);
        if ("stopping".equals(status)) return Color.rgb(91, 68, 29);
        return Color.rgb(31, 61, 94);
    }

    void showResult(String title, String meta) {
        if (resultCard == null) return;
        resultTitle.setText("✅ " + title);
        resultMeta.setText(meta);
        resultCard.setVisibility(View.VISIBLE);
    }

    void appendLog(String msg) {
        appendLog(msg, true);
    }

    void appendServiceLog(String msg) {
        appendLog(msg, false);
    }

    void appendLog(String msg, boolean persist) {
        if (persist) LogStore.append(this, msg);
        if (logView == null) return;
        String time = new SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(new Date());
        String line = "[" + time + "] " + msg + "\n";
        logView.setText(tail(logView.getText().toString() + line, 12000));
        if (logScroll != null) logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    String namesSummary(List<String> names) {
        if (names == null || names.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i >= 3) { sb.append(" +").append(names.size() - i).append(" file"); break; }
            if (sb.length() > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    void showLongText(String title, String body) {
        TextView tv = new TextView(this);
        tv.setText(body == null ? "" : body);
        tv.setTextSize(12);
        tv.setTextColor(TEXT);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        tv.setSingleLine(false);
        ScrollView sv = new ScrollView(this);
        sv.setPadding(dp(10), dp(10), dp(10), dp(10));
        sv.addView(tv);
        new AlertDialog.Builder(this).setTitle(title).setView(sv).setPositiveButton("OK", null).show();
    }

    String oneLine(String s) {
        if (s == null) return "";
        String v = s.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return v.length() <= 220 ? v : v.substring(0, 220) + "...";
    }

    int intVal(EditText e, int def) { try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception ex) { return def; } }
    float floatVal(EditText e, float def) { try { return Float.parseFloat(e.getText().toString().trim()); } catch (Exception ex) { return def; } }
    double doubleVal(EditText e, double def) { try { return Double.parseDouble(e.getText().toString().trim()); } catch (Exception ex) { return def; } }
    String shortLang(String lang) {
        if (lang == null) return "—";
        String l = lang.trim().toLowerCase(Locale.ROOT);
        if (l.startsWith("japanese")) return "JA";
        if (l.startsWith("vietnamese")) return "VI";
        if (l.startsWith("english")) return "EN";
        if (l.startsWith("chinese")) return "ZH";
        if (l.startsWith("korean")) return "KO";
        if (lang.trim().length() <= 3) return lang.trim().toUpperCase(Locale.ROOT);
        return lang.trim().substring(0, Math.min(3, lang.trim().length())).toUpperCase(Locale.ROOT);
    }

    String nonEmpty(String s, String def) { return s == null || s.trim().isEmpty() ? def : s.trim(); }
    String preview(String s, int max) { if (s == null || s.trim().isEmpty()) return "Trống"; return s.length() <= max ? s : s.substring(0, max) + "\n..."; }
    String tail(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(s.length() - max); }
    void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
    void setLabel(TextView tv, String text) { if (tv != null) tv.setText(text); }

    ScrollView scroll() {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(false);
        s.setVerticalScrollBarEnabled(true);
        s.setScrollbarFadingEnabled(false);
        s.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        return s;
    }
    LinearLayout pageRoot() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(0, dp(10), 0, dp(24)); return r; }
    LinearLayout rowContainer() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    View space(int w, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), dp(h))); return v; }

    LinearLayout sectionCard(String icon, String title) {
        LinearLayout c = card(18, PANEL, BORDER);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout head = rowContainer();
        head.addView(text(icon, 20, BLUE, false));
        head.addView(space(10, 1));
        head.addView(text(title, 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        c.addView(head, marginLP(-1, -2, 0, 0, 0, 14));
        c.setLayoutParams(marginLP(-1, -2, 0, 0, 0, 14));
        return c;
    }

    LinearLayout card(int radius, int color, int stroke) {
        LinearLayout l = new LinearLayout(this);
        tint(l, color, stroke, 1, radius);
        return l;
    }

    void tint(View v, int fill, int stroke, int strokeWidth, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(strokeWidth), stroke);
        v.setBackground(g);
    }
    void tint(View v, int fill, int strokeWidth, int radius) { tint(v, fill, BORDER, strokeWidth, radius); }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    TextView chip(String s, int accent, boolean greenDot) {
        TextView v = text(s, 12, greenDot ? GREEN : accent, true);
        v.setSingleLine(false);
        v.setMaxLines(2);
        v.setPadding(dp(10), dp(5), dp(10), dp(5));
        tint(v, FIELD, accent, 1, 16);
        LinearLayout.LayoutParams lp = marginLP(-2, -2, 0, 3, 0, 3);
        v.setLayoutParams(lp);
        return v;
    }

    EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(14);
        e.setSingleLine(false);
        e.setMinLines(1);
        e.setMaxLines(3);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        e.setPadding(dp(12), 0, dp(12), 0);
        tint(e, FIELD, BORDER, 1, 12);
        return e;
    }

    EditText number(String hint, String value) { EditText e = input(hint, value); e.setInputType(InputType.TYPE_CLASS_NUMBER); return e; }
    EditText decimal(String hint, String value) { EditText e = input(hint, value); e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); return e; }

    LinearLayout fieldBlock(String label, EditText field) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label, 11, MUTED, true);
        l.setLetterSpacing(0.12f);
        box.addView(l, marginLP(-1, -2, 0, 0, 0, 5));
        box.addView(field, new LinearLayout.LayoutParams(-1, dp(48)));
        box.setPadding(0, 0, 0, dp(10));
        return box;
    }

    LinearLayout twoFields(View a, View b) {
        LinearLayout row = rowContainer();
        row.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(space(12, 1));
        row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    LinearLayout rowButtons(View left, View right) {
        LinearLayout row = rowContainer();
        row.addView(left, new LinearLayout.LayoutParams(dp(165), dp(46)));
        row.addView(space(12, 1));
        row.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        row.setPadding(0, dp(8), 0, dp(8));
        return row;
    }

    CheckBox check(String text, boolean checked, String desc) {
        CheckBox b = new CheckBox(this);
        b.setText(text + "\n" + desc);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setChecked(checked);
        b.setPadding(0, dp(4), 0, dp(4));
        return b;
    }

    Button tinyButton(String s, int color, View.OnClickListener l) {
        Button b = button(s, FIELD, TEXT, l);
        b.setTextColor(color == RED ? RED : (color == GREEN ? GREEN : BLUE));
        b.setTextSize(10);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(36));
        lp.setMargins(0, 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    Button primaryButton(String s, View.OnClickListener l) { return button(s, BLUE, TEXT, l); }
    Button secondaryButton(String s, View.OnClickListener l) { return button(s, FIELD, TEXT, l); }
    Button dangerButton(String s, View.OnClickListener l) { return button(s, RED, Color.WHITE, l); }

    Button button(String s, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(fg);
        b.setOnClickListener(l);
        tint(b, bg, bg == FIELD ? BORDER : bg, 1, 12);
        return b;
    }

    LinearLayout.LayoutParams marginLP(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    String shortTabName(String name) {
        if ("Translate".equals(name)) return "Translate";
        if ("Settings".equals(name)) return "Settings";
        if ("Glossaries".equals(name)) return "Glossary";
        if ("Pronouns".equals(name)) return "Pronoun";
        if ("Files".equals(name)) return "Files";
        if ("Jobs".equals(name)) return "Jobs";
        return "Sample";
    }

    String iconForTab(String name) {
        if ("Translate".equals(name)) return "文";
        if ("Settings".equals(name)) return "☷";
        if ("Glossaries".equals(name)) return "📖";
        if ("Pronouns".equals(name)) return "💬";
        if ("Files".equals(name)) return "📁";
        if ("Jobs".equals(name)) return "🧾";
        return "⚗";
    }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
