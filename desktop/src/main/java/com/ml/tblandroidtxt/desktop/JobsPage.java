package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.DesktopHistory;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.TranslationRepositoryCore;
import com.ml.tblandroidtxt.WorkspaceFilterModel;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Công việc: what is unfinished (with resume / retry / dismiss), today's and the week's spend, recent runs, and the Lịch sử tab. */
final class JobsPage {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final HistoryPanel history;
    private final TableView<TranslationRepositoryCore.JobSummary> jobsTable = new TableView<>();
    private final TableView<DesktopHistory.Entry> recent = new TableView<>();
    private final Label todayLabel = new Label();
    private final Label weekLabel = new Label();
    private final Label note = new Label();
    private final Button setChip = new Button();
    private final Button resume = new Button("Tiếp tục");
    private final Button retry = new Button("Dịch lại lỗi");
    private final Button dismiss = new Button("Bỏ job");
    private final Button openResult = new Button("Mở kết quả");
    private final Button openDetail = new Button("Mở chi tiết công việc");
    private final Button latest = new Button("Tiếp tục gần nhất");
    private final Button recentDetail = new Button("Mở chi tiết lượt này");
    private final ToggleButton activeTab = new ToggleButton("Đang làm");
    private final ToggleButton historyTab = new ToggleButton("Lịch sử");
    private String setId = "";
    private String setName = "";
    private final Node root;

    JobsPage(DesktopApp app, DesktopRuntime runtime, HistoryPanel history) {
        this.app = app;
        this.runtime = runtime;
        this.history = history;
        this.root = build();
    }

    Node node() { return root; }

    private Node build() {
        jobsTable.getColumns().add(column("Tệp", 260, j -> j.fileName));
        jobsTable.getColumns().add(column("Trạng thái", 110, j -> DesktopHistory.statusText(j.status)));
        jobsTable.getColumns().add(column("Chunk xong", 90, j -> j.doneChunks + "/" + j.totalChunks));
        jobsTable.getColumns().add(column("Lỗi", 50, j -> String.valueOf(j.failedChunks)));
        jobsTable.getColumns().add(column("Token / chi phí", 230, j -> j.usage == null ? "Chưa có token/chi phí" : DesktopHistory.usageText(j)));
        jobsTable.getColumns().add(column("Cập nhật", 130, j -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(j.updatedAt))));
        jobsTable.setPlaceholder(new Label("Không có job nào đang dở"));
        jobsTable.setPrefHeight(170);
        recent.getColumns().add(entryColumn("Lượt / tệp", 300, en -> en.title));
        recent.getColumns().add(entryColumn("Trạng thái", 130, en -> en.statusText));
        recent.getColumns().add(entryColumn("Tiến độ", 170, en -> en.progressText));
        recent.getColumns().add(entryColumn("Chi phí", 90, en -> en.costText));
        recent.setPlaceholder(new Label("Chưa có công việc gần đây."));
        VBox.setVgrow(recent, Priority.ALWAYS);
        jobsTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons());
        recent.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons());

        for (Button b : new Button[]{retry, dismiss, openResult, openDetail, latest, recentDetail, setChip}) b.getStyleClass().add("secondary");
        resume.getStyleClass().add("primary");
        resume.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
            if (j != null && confirmSpend("Tiếp tục job #" + j.id + "?", j)) { runtime.confirmSpend(); runtime.resumeJob(j.id); }
            app.refreshControls();
        });
        retry.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
            if (j != null && confirmSpend("Dịch lại chunk lỗi của job #" + j.id + "?", j)) { runtime.confirmSpend(); runtime.retryFailed(j.id); }
            app.refreshControls();
        });
        dismiss.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
            if (j == null) return;
            if (!app.confirm("Bỏ job này?", "Job sẽ không hiện ở đây nữa; lịch sử token và chi phí vẫn giữ.")) return;
            runtime.dismissJob(j.id);
            say("Đã bỏ job #" + j.id + ".", false);
            refresh();
        });
        openResult.setOnAction(e -> openJobOutput(jobsTable.getSelectionModel().getSelectedItem()));
        openDetail.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
            if (j != null) { showHistory(); history.focusJob(j.fileName); }
        });
        recentDetail.setOnAction(e -> {
            DesktopHistory.Entry en = recent.getSelectionModel().getSelectedItem();
            if (en != null) { showHistory(); history.focusJob(en.jobs.isEmpty() ? en.title : en.jobs.get(0).fileName); }
        });
        latest.setOnAction(e -> {
            TranslationRepositoryCore.JobSummary j = runtime.latestResumable();
            if (j == null) { say("Không có job nào có thể tiếp tục.", true); return; }
            if (confirmSpend("Tiếp tục job gần nhất (#" + j.id + ")?", j)) { runtime.confirmSpend(); runtime.resumeJob(j.id); }
            app.refreshControls();
        });
        Button reload = new Button("↻ Làm mới");
        reload.getStyleClass().add("secondary");
        reload.setOnAction(e -> refresh());
        setChip.setOnAction(e -> {
            if (WorkspaceFilterModel.active(setId)) setFilter("", "");
            else chooseSet();
        });
        note.setWrapText(true);
        todayLabel.setWrapText(true);
        weekLabel.setWrapText(true);

        VBox totals = DesktopApp.card(new Label("Token và chi phí"), todayLabel, weekLabel);
        VBox running = DesktopApp.card(new Label("Đang làm"), jobsTable, new FlowPane(8, 8, resume, retry, dismiss, openResult, openDetail, latest, reload), note);
        VBox recentCard = DesktopApp.card(new Label("Gần đây (cả các lượt chạy tập)"), recent, new HBox(8, recentDetail));
        VBox.setVgrow(recentCard, Priority.ALWAYS);
        VBox activePane = new VBox(10, setChip, totals, running, recentCard);
        VBox.setVgrow(activePane, Priority.ALWAYS);

        Node historyNode = history.node();
        historyNode.setVisible(false);
        historyNode.setManaged(false);
        StackPane stack = new StackPane(activePane, historyNode);
        VBox.setVgrow(stack, Priority.ALWAYS);
        ToggleGroup tabs = new ToggleGroup();
        activeTab.setToggleGroup(tabs);
        historyTab.setToggleGroup(tabs);
        activeTab.getStyleClass().add("nav-button");
        historyTab.getStyleClass().add("nav-button");
        activeTab.setMinWidth(Region.USE_PREF_SIZE);
        historyTab.setMinWidth(Region.USE_PREF_SIZE);
        activeTab.setSelected(true);
        activeTab.setOnAction(e -> {
            activeTab.setSelected(true);
            activePane.setVisible(true); activePane.setManaged(true);
            historyNode.setVisible(false); historyNode.setManaged(false);
            refresh();
        });
        historyTab.setOnAction(e -> {
            historyTab.setSelected(true);
            activePane.setVisible(false); activePane.setManaged(false);
            historyNode.setVisible(true); historyNode.setManaged(true);
            history.reload();
        });
        updateChip();
        updateButtons();
        return DesktopApp.page(NavModel.title(NavModel.JOBS), new HBox(8, activeTab, historyTab), stack);
    }

    void showHistory() { historyTab.fire(); }

    String setFilterId() { return setId; }

    private void say(String text, boolean error) {
        note.setText(text);
        note.getStyleClass().removeAll("err", "ok");
        note.getStyleClass().add(error ? "err" : "ok");
    }

    // ---- set filter ----

    void chooseSetFilter() { chooseSet(); }

    private void chooseSet() {
        List<DesktopRuntime.SetCard> sets = runtime.setCards();
        if (sets.isEmpty()) { say("Chưa có tập nào để lọc.", true); return; }
        List<String> labels = new ArrayList<>();
        for (DesktopRuntime.SetCard c : sets) labels.add(c.name + " (" + c.chapters + " chương)");
        javafx.scene.control.ChoiceDialog<String> d = new javafx.scene.control.ChoiceDialog<>(labels.get(0), labels);
        d.setTitle("Lọc Công việc theo Tập");
        d.setHeaderText("Lọc Công việc theo Tập");
        d.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<String> r = app.showDialog(d);
        if (!r.isPresent()) return;
        DesktopRuntime.SetCard c = sets.get(labels.indexOf(r.get()));
        setFilter(c.id, c.name);
    }

    void setFilter(String id, String name) {
        setId = id == null ? "" : id;
        setName = name == null ? "" : name;
        updateChip();
        history.setSetFilter(setId, setName);
        refresh();
    }

    private void updateChip() {
        setChip.setText(WorkspaceFilterModel.chipText(setId, setName));
        setChip.setAccessibleText(WorkspaceFilterModel.active(setId) ? "Bỏ lọc theo Tập" : "Lọc Công việc theo Tập");
        setChip.setTooltip(new javafx.scene.control.Tooltip(WorkspaceFilterModel.active(setId) ? "Bỏ lọc theo Tập" : "Lọc Công việc theo Tập"));
    }

    // ---- data ----

    void refresh() {
        List<TranslationRepositoryCore.JobSummary> list = new ArrayList<>();
        java.util.Set<Long> allowed = WorkspaceFilterModel.active(setId) ? runtime.history().jobIdsOfSet(setId) : null;
        for (TranslationRepositoryCore.JobSummary j : runtime.jobs()) {
            if (!DesktopHistory.isUnfinished(j)) continue;
            if (allowed != null && !allowed.contains(j.id)) continue;
            list.add(j);
        }
        jobsTable.getItems().setAll(list);
        String[] lines = runtime.history().usageLines();
        todayLabel.setText(lines[0]);
        weekLabel.setText(lines[1]);
        DesktopHistory.Page page = runtime.history().page(-1, DesktopHistory.ALL, new HashSet<String>(), "", setId);
        recent.getItems().setAll(page.entries.size() > 30 ? page.entries.subList(0, 30) : page.entries);
        updateButtons();
    }

    void updateButtons() {
        boolean running = runtime.isRunning();
        TranslationRepositoryCore.JobSummary j = jobsTable.getSelectionModel().getSelectedItem();
        boolean has = j != null && !running;
        resume.setDisable(!has || !DesktopHistory.canResume(j));
        retry.setDisable(!has || !DesktopHistory.canRetryFailed(j));
        dismiss.setDisable(j == null || running);
        openResult.setDisable(j == null);
        openDetail.setDisable(j == null);
        latest.setDisable(running);
        recentDetail.setDisable(recent.getSelectionModel().getSelectedItem() == null);
        if (j != null && DesktopHistory.needsReview(j)) say("Cần xem lại trạng thái gửi trước khi chạy lại.", true);
    }

    private void openJobOutput(TranslationRepositoryCore.JobSummary j) {
        if (j == null) return;
        DesktopHistory.Detail d = runtime.history().detail(j.id);
        if (d == null || d.outputFile == null || !java.nio.file.Files.exists(d.outputFile)) { say("Chưa có tệp output cho job này.", true); return; }
        try { java.awt.Desktop.getDesktop().open(d.outputFile.toFile()); } catch (Exception e) { say("Không mở được output: " + e.getMessage(), true); }
    }

    private boolean confirmSpend(String title, TranslationRepositoryCore.JobSummary j) {
        AppSettings s = app.currentSettings();
        return app.confirm(title, "Model: " + s.model + "\nReasoning effort: " + s.reasoningEffort + "\nTrần chi phí: " + CostEstimator.money(TranslatePage.effectiveCap(s))
                + "\nChunk xong " + j.doneChunks + "/" + j.totalChunks + ", lỗi " + j.failedChunks + "\n\nTiếp tục sẽ gọi provider và tốn tiền thật.");
    }

    private static <T> TableColumn<T, String> column(String title, double width, java.util.function.Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    private static TableColumn<DesktopHistory.Entry, String> entryColumn(String title, double width, java.util.function.Function<DesktopHistory.Entry, String> value) {
        return column(title, width, value);
    }
}
