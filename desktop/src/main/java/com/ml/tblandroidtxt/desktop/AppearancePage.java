package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.DesktopRuntime;
import com.ml.tblandroidtxt.UiAppearanceCore;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/** Cài đặt · Giao diện: fixed or follow Windows, light and dark palette, text size, density, corners, a live preview, and the defaults. */
final class AppearancePage {
    private final DesktopApp app;
    private final DesktopRuntime runtime;
    private final RadioButton fixed = new RadioButton("Cố định");
    private final RadioButton system = new RadioButton("Theo hệ thống");
    private final ComboBox<UiAppearanceCore.Palette> fixedBox = new ComboBox<>();
    private final ComboBox<UiAppearanceCore.Palette> lightBox = new ComboBox<>();
    private final ComboBox<UiAppearanceCore.Palette> darkBox = new ComboBox<>();
    private final ComboBox<Integer> fontBox = new ComboBox<>();
    private final ComboBox<String[]> densityBox = new ComboBox<>();
    private final ComboBox<String[]> cornersBox = new ComboBox<>();
    private final Button restore = new Button("Khôi phục mặc định");
    private final Label note = new Label();
    private boolean loading;
    private final Node root;

    AppearancePage(DesktopApp app, DesktopRuntime runtime) {
        this.app = app;
        this.runtime = runtime;
        this.root = build();
        reload();
    }

    Node node() { return root; }

    private static StringConverter<UiAppearanceCore.Palette> paletteNames() {
        return new StringConverter<UiAppearanceCore.Palette>() {
            @Override public String toString(UiAppearanceCore.Palette p) { return p == null ? "" : p.name + (p.light ? " (sáng)" : " (tối)"); }
            @Override public UiAppearanceCore.Palette fromString(String s) { return null; }
        };
    }

    private static StringConverter<String[]> pairNames() {
        return new StringConverter<String[]>() {
            @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
            @Override public String[] fromString(String s) { return null; }
        };
    }

    private Node build() {
        ToggleGroup mode = new ToggleGroup();
        fixed.setToggleGroup(mode);
        system.setToggleGroup(mode);
        fixed.setOnAction(e -> changed());
        system.setOnAction(e -> changed());
        fixedBox.getItems().setAll(UiAppearanceCore.all());
        lightBox.getItems().setAll(DesktopAppearance.lightPalettes());
        darkBox.getItems().setAll(DesktopAppearance.darkPalettes());
        for (ComboBox<UiAppearanceCore.Palette> b : java.util.Arrays.asList(fixedBox, lightBox, darkBox)) {
            b.setConverter(paletteNames());
            b.setOnAction(e -> changed());
        }
        for (int p : DesktopAppearance.FONT_PERCENTS) fontBox.getItems().add(p);
        fontBox.setConverter(new StringConverter<Integer>() {
            @Override public String toString(Integer v) { return v == null ? "" : v + "%"; }
            @Override public Integer fromString(String s) { return null; }
        });
        fontBox.setOnAction(e -> changed());
        densityBox.getItems().setAll(new String[]{DesktopAppearance.DENSITY_COMPACT, "Gọn"}, new String[]{DesktopAppearance.DENSITY_COMFORTABLE, "Thoải mái"});
        cornersBox.getItems().setAll(new String[]{DesktopAppearance.CORNERS_SQUARE, "Vuông"}, new String[]{DesktopAppearance.CORNERS_MEDIUM, "Vừa"},
                new String[]{DesktopAppearance.CORNERS_ROUND, "Tròn"});
        densityBox.setConverter(pairNames());
        cornersBox.setConverter(pairNames());
        densityBox.setOnAction(e -> changed());
        cornersBox.setOnAction(e -> changed());
        restore.getStyleClass().add("secondary");
        restore.setOnAction(e -> {
            if (!app.confirm("Khôi phục giao diện mặc định?", "Bảng màu, cỡ chữ, mật độ và bo góc về như lúc cài.")) return;
            DesktopAppearance d = DesktopAppearance.defaults();
            d.save(runtime.appearancePrefs());
            app.applyAppearance(d);
            reload();
            note.setText("Đã khôi phục giao diện mặc định.");
        });
        note.setWrapText(true);

        VBox choices = DesktopApp.card(new Label("Giao diện"),
                row("Chế độ", new HBox(14, fixed, system)),
                row("Bảng màu (chế độ cố định)", fixedBox),
                row("Bảng màu sáng (theo hệ thống)", lightBox),
                row("Bảng màu tối (theo hệ thống)", darkBox),
                row("Cỡ chữ", fontBox), row("Mật độ", densityBox), row("Bo góc", cornersBox), new HBox(10, restore, note));

        // the preview is made of the real controls, so it follows every change at once
        Button primary = new Button("Nút chính");
        primary.getStyleClass().add("primary");
        Button secondary = new Button("Nút phụ");
        secondary.getStyleClass().add("secondary");
        TextField field = new TextField("Ô nhập mẫu");
        Label ok = new Label("Xong · màu báo thành công");
        ok.getStyleClass().add("ok");
        Label warn = new Label("Cảnh báo · màu báo chú ý");
        warn.getStyleClass().add("warn");
        Label err = new Label("Lỗi · màu báo lỗi");
        err.getStyleClass().add("err");
        Label muted = new Label("Chữ phụ cho giải thích ngắn.");
        muted.getStyleClass().add("muted");
        VBox preview = DesktopApp.card(new Label("Xem trước"), new HBox(10, primary, secondary, field), ok, warn, err, muted);
        return DesktopApp.page(NavModel.title(NavModel.APPEARANCE), choices, preview);
    }

    private static HBox row(String text, Node field) {
        Label l = new Label(text);
        l.setMinWidth(230);
        l.getStyleClass().add("muted");
        HBox h = new HBox(10, l, field);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (field instanceof Region) HBox.setHgrow(field, Priority.NEVER);
        return h;
    }

    void reload() {
        loading = true;
        DesktopAppearance a = DesktopAppearance.load(runtime.appearancePrefs());
        fixed.setSelected(DesktopAppearance.MODE_FIXED.equals(a.mode));
        system.setSelected(DesktopAppearance.MODE_SYSTEM.equals(a.mode));
        fixedBox.setValue(UiAppearanceCore.find(a.palette));
        lightBox.setValue(UiAppearanceCore.find(a.paletteLight));
        darkBox.setValue(UiAppearanceCore.find(a.paletteDark));
        fontBox.setValue(a.fontPercent);
        for (String[] d : densityBox.getItems()) if (d[0].equals(a.density)) densityBox.setValue(d);
        for (String[] c : cornersBox.getItems()) if (c[0].equals(a.corners)) cornersBox.setValue(c);
        updateEnabled();
        loading = false;
    }

    private void updateEnabled() {
        boolean sys = system.isSelected();
        fixedBox.setDisable(sys);
        lightBox.setDisable(!sys);
        darkBox.setDisable(!sys);
    }

    private void changed() {
        if (loading) return;
        DesktopAppearance a = new DesktopAppearance();
        a.mode = system.isSelected() ? DesktopAppearance.MODE_SYSTEM : DesktopAppearance.MODE_FIXED;
        if (fixedBox.getValue() != null) a.palette = fixedBox.getValue().id;
        if (lightBox.getValue() != null) a.paletteLight = lightBox.getValue().id;
        if (darkBox.getValue() != null) a.paletteDark = darkBox.getValue().id;
        if (fontBox.getValue() != null) a.fontPercent = fontBox.getValue();
        if (densityBox.getValue() != null) a.density = densityBox.getValue()[0];
        if (cornersBox.getValue() != null) a.corners = cornersBox.getValue()[0];
        updateEnabled();
        a.save(runtime.appearancePrefs());
        app.applyAppearance(a);
        note.setText("");
    }

    /** QA: sets the controls to a state, as a person would. */
    void set(DesktopAppearance a) {
        a.save(runtime.appearancePrefs());
        reload();
        app.applyAppearance(a);
    }
}
