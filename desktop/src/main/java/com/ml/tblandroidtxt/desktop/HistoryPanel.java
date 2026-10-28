package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.CostEstimator;
import com.ml.tblandroidtxt.DesktopHistory;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.TranslationRepositoryCore;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Lịch sử" of the jobs page: newest first, 50 per page, the chapters of one run folded into one line, a status filter, and a detail
 * with numbers only (never chunk text) plus a button that opens the output folder.
 */
final class HistoryPanel {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final TableView<DesktopHistory.Entry> entries = new TableView<>();
    private final TableView<TranslationRepositoryCore.JobSummary> chapters = new TableView<>();
    private final TableView<TranslationRepositoryCore.JobAttemptRow> attempts = new TableView<>();
    private final ComboBox<String[]> filter = new ComboBox<>();
    private final Button more = new Button("Tải thêm");
    private final Button openOutput = new Button("Mở thư mục output");
    private final Label detail = new Label("Chọn một dòng để xem chi tiết");
    private final Set<String> shownSessions = new HashSet<>();
    private final List<DesktopHistory.Entry> loaded = new ArrayList<>();
    private long cursor = -1;
    private boolean hasMore;
    private DesktopHistory.Detail currentDetail;
    private final Node root;

    HistoryPanel(DesktopApp app, DesktopRuntime runtime) {
        this.app = app;
        this.runtime = runtime;
        this.root = build();
    }

    Node node() { return root; }

    private Node build() {
        filter.getItems().add(new String[]{DesktopHistory.ALL, "Tất cả"});
        filter.getItems().add(new String[]{DesktopHistory.DONE, "Hoàn tất"});
        filter.getItems().add(new String[]{DesktopHistory.FAILED, "Có lỗi"});
        filter.getItems().add(new String[]{DesktopHistory.UNFINISHED, "Chưa xong"});
        filter.setConverter(new javafx.util.StringConverter<String[]>() {
            @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
            @Override public String[] fromString(String s) { return null; }
        });
        filter.getSelectionModel().select(0);
        filter.setOnAction(e -> reload());
        more.getStyleClass().add("secondary");
        more.setOnAction(e -> loadMore());
        openOutput.getStyleClass().add("secondary");
        openOutput.setDisable(true);
        openOutput.setOnAction(e -> {
            if (currentDetail != null && currentDetail.outputFile != null && currentDetail.outputFile.getParent() != null) app.openFolder(currentDetail.outputFile.getParent());
        });

        entries.getColumns().add(column("Lượt / tệp", 330, en -> en.title));
        entries.getColumns().add(column("Trạng thái", 150, en -> en.statusText));
        entries.getColumns().add(column("Tiến độ", 170, en -> en.progressText));
        entries.getColumns().add(column("Chi phí", 90, en -> en.costText));
        entries.getColumns().add(column("Cập nhật", 140, en -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(en.updatedAt))));
        entries.setPlaceholder(new Label("Chưa có lịch sử"));
        entries.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showEntry(b));
        entries.setPrefHeight(150);
        entries.setMinHeight(120);

        chapters.getColumns().add(column("Chương / tệp", 300, j -> j.fileName));
        chapters.getColumns().add(column("Trạng thái", 150, j -> DesktopHistory.statusText(j.status)));
        chapters.getColumns().add(column("Chunk xong", 90, j -> j.doneChunks + "/" + j.totalChunks));
        chapters.getColumns().add(column("Lỗi", 60, j -> String.valueOf(j.failedChunks)));
        chapters.getColumns().add(column("Chi phí", 90, j -> Double.isNaN(DesktopRuntime.jobCost(j)) ? "" : CostEstimator.money(DesktopRuntime.jobCost(j))));
        chapters.setPlaceholder(new Label(""));
        chapters.setPrefHeight(110);
        chapters.setMinHeight(90);
        chapters.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showJob(b));

        attempts.getColumns().add(attemptColumn("Chunk", 60, r -> String.valueOf(r.chunkIdx + 1)));
        attempts.getColumns().add(attemptColumn("Lượt", 50, r -> String.valueOf(r.attempt)));
        attempts.getColumns().add(attemptColumn("Pha", 90, r -> r.phase));
        attempts.getColumns().add(attemptColumn("Trạng thái", 150, r -> r.status));
        attempts.getColumns().add(attemptColumn("Kết thúc", 90, r -> r.finishReason));
        attempts.getColumns().add(attemptColumn("Token vào", 80, r -> String.valueOf(r.inputTokens)));
        attempts.getColumns().add(attemptColumn("Token ra", 80, r -> String.valueOf(r.outputTokens)));
        attempts.getColumns().add(attemptColumn("Chi phí", 90, r -> r.cost > 0 ? CostEstimator.money(r.cost) : ""));
        attempts.setPlaceholder(new Label("Các lượt thử của job sẽ hiện ở đây"));
        attempts.setMinHeight(100);
        VBox.setVgrow(attempts, Priority.ALWAYS);
        detail.setWrapText(true);
        detail.getStyleClass().add("muted");

        HBox bar = new HBox(10, new Label("Lọc:"), filter, more);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox box = new VBox(10, bar, entries, chapters, detail, attempts, new HBox(10, openOutput));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private static <T> TableColumn<T, String> column(String title, double width, java.util.function.Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(value.apply(cd.getValue())));
        return c;
    }

    private static TableColumn<TranslationRepositoryCore.JobAttemptRow, String> attemptColumn(String title, double width,
                                                                                               java.util.function.Function<TranslationRepositoryCore.JobAttemptRow, String> value) {
        return column(title, width, value);
    }

    private String currentFilter() {
        String[] f = filter.getValue();
        return f == null ? DesktopHistory.ALL : f[0];
    }

    /** First page again (after the filter changed, a run ended or the tab was opened). */
    void reload() {
        loaded.clear();
        shownSessions.clear();
        cursor = -1;
        hasMore = false;
        loadMore();
    }

    private void loadMore() {
        DesktopHistory.Page page = runtime.history().page(cursor, currentFilter(), shownSessions);
        loaded.addAll(page.entries);
        cursor = page.nextCursor;
        hasMore = page.hasMore;
        entries.getItems().setAll(loaded);
        more.setDisable(!hasMore);
        if (loaded.isEmpty() && hasMore) loadMore();
    }

    private void showEntry(DesktopHistory.Entry e) {
        chapters.getItems().clear();
        attempts.getItems().clear();
        currentDetail = null;
        openOutput.setDisable(true);
        if (e == null) {
            detail.setText("Chọn một dòng để xem chi tiết");
            return;
        }
        chapters.getItems().setAll(e.jobs);
        if (e.jobs.size() == 1) chapters.getSelectionModel().select(0);
        else detail.setText(e.jobs.size() + " chương của lượt này. Chọn một chương để xem các lượt thử.");
    }

    private void showJob(TranslationRepositoryCore.JobSummary j) {
        attempts.getItems().clear();
        currentDetail = null;
        openOutput.setDisable(true);
        if (j == null) return;
        DesktopHistory.Detail d = runtime.history().detail(j.id);
        if (d == null) return;
        currentDetail = d;
        attempts.getItems().setAll(d.attempts);
        StringBuilder sb = new StringBuilder("Job #" + d.job.id + " · chunk xong " + d.job.doneChunks + "/" + d.job.totalChunks + ", lỗi " + d.job.failedChunks);
        double cost = DesktopRuntime.jobCost(d.job);
        sb.append(" · chi phí ").append(Double.isNaN(cost) ? "chưa có" : CostEstimator.money(cost));
        sb.append(" · ").append(DesktopRuntime.jobCalls(d.job)).append(" request");
        if (!d.chunkStatuses.isEmpty()) sb.append("\nTrạng thái chunk: ").append(join(d.chunkStatuses));
        if (!d.finishReasons.isEmpty()) sb.append("\nLý do kết thúc: ").append(join(d.finishReasons));
        detail.setText(sb.toString());
        openOutput.setDisable(d.outputFile == null || d.outputFile.getParent() == null);
    }

    private static String join(Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(e.getKey()).append(' ').append(e.getValue());
        }
        return sb.toString();
    }

    // QA hooks (screenshots): select the first line without a click.
    void selectFirst() { if (!entries.getItems().isEmpty()) entries.getSelectionModel().select(0); }

}
