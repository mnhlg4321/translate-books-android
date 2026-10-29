package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.AppSettings;
import com.ml.tblandroidtxt.DesktopPromptPreview;
import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.TranslationRepositoryCore;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** The body of "Xem trước prompt": chunk picker, system and user prompt, matched Glossary/Pronoun lines. It loads chunk 1 as soon as it is built. */
final class PromptPreviewView {
    final ComboBox<Integer> pick = new ComboBox<>();
    final TextArea system = readOnly(), user = readOnly(), matched = readOnly();
    final Label info = new Label();
    private final DesktopRuntime runtime;
    private final Path rawFile;
    private final Supplier<AppSettings> settings;
    private final ScrollPane node;
    private String copy = "";

    PromptPreviewView(DesktopRuntime runtime, Path rawFile, Supplier<AppSettings> settings) throws Exception {
        this.runtime = runtime;
        this.rawFile = rawFile;
        this.settings = settings;
        system.setPrefRowCount(6);
        user.setPrefRowCount(8);
        matched.setPrefRowCount(5);
        info.setWrapText(true);
        info.getStyleClass().add("muted");
        int count = runtime.previewChunkCount(rawFile, settings.get());
        for (int i = 0; i < count; i++) pick.getItems().add(i);
        pick.setConverter(new StringConverter<Integer>() {
            @Override public String toString(Integer i) { return i == null ? "" : "Chunk " + (i + 1) + " / " + pick.getItems().size(); }
            @Override public Integer fromString(String s) { return null; }
        });
        VBox box = new VBox(8, new HBox(10, new Label("Chunk"), pick), info, new Label("System prompt"), system, new Label("User prompt"), user, matched);
        node = new ScrollPane(box);
        node.setFitToWidth(true);
        node.setPrefSize(720, 520);
        // ComboBox fires its action only for a choice made by the person, not for setValue in code, so the content is loaded here
        // and every later change of the value (by the person or by code) goes through the value listener
        pick.setValue(0);
        load();
        pick.valueProperty().addListener((o, a, b) -> load());
    }

    Node node() { return node; }

    String copyText() { return copy; }

    void load() {
        Integer i = pick.getValue();
        if (i == null) return;
        try {
            String previous = "";
            if (i > 0 && runtime.lastJobId() > 0) {
                List<TranslationRepositoryCore.ChunkRow> rows = runtime.chunkRows(runtime.lastJobId());
                if (i - 1 < rows.size() && rows.get(i - 1).translated != null) previous = rows.get(i - 1).translated;
            }
            DesktopPromptPreview.Result r = runtime.previewPrompt(rawFile, settings.get(), i, previous);
            system.setText(r.system);
            user.setText(r.user);
            StringBuilder m = new StringBuilder("Glossary khớp chunk này (" + r.glossaryLines.size() + "):\n");
            for (String line : r.glossaryLines) m.append("  ").append(line).append('\n');
            m.append("Pronoun khớp chunk này (").append(r.pronounLines.size()).append("):\n");
            for (String line : r.pronounLines) m.append("  ").append(line).append('\n');
            matched.setText(m.toString());
            info.setText("Chunk " + (r.index + 1) + "/" + r.chunkCount + " · ≈ " + r.inputTokens + " token đầu vào"
                    + (i > 0 ? " · phần lịch sử lấy từ bản dịch chunk trước của lượt dịch gần nhất (nếu có)" : ""));
            copy = r.copyText();
        } catch (Exception e) {
            info.setText("Không xem trước được: " + e.getMessage());
        }
    }

    private static TextArea readOnly() {
        TextArea a = new TextArea();
        a.setEditable(false);
        a.setWrapText(true);
        return a;
    }
}
