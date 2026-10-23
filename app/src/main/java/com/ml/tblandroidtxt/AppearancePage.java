package com.ml.tblandroidtxt;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Settings > Appearance: palette, system mode, font size and the compact extra layout controls. */
final class AppearancePage {
    private static final long APPLY_DELAY_MS = 700L;
    private final MainActivity a;
    private final AppearanceSettings pending;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout previewHolder;

    AppearancePage(MainActivity activity) {
        a = activity;
        pending = AppearanceSettings.load(activity.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE));
    }

    View build(LinearLayout root, java.util.function.BiFunction<String, String, LinearLayout> group) {
        root.addView(previewGroup(group), margin(0, 0));
        root.addView(modeGroup(group), margin(8, 0));
        boolean system = AppearanceSettings.MODE_SYSTEM.equals(pending.mode);
        String lightSelected = system ? pending.paletteLight : (UiAppearance.find(pending.palette).light ? pending.palette : "");
        String darkSelected = system ? pending.palette : (!UiAppearance.find(pending.palette).light ? pending.palette : "");
        root.addView(paletteGroup(group, "Sáng", "Giao diện sáng dùng trực tiếp hoặc khi điện thoại ở chế độ sáng.", UiAppearance.light(), lightSelected, true), margin(8, 0));
        root.addView(paletteGroup(group, "Tối", "Giao diện tối dùng trực tiếp hoặc khi điện thoại ở chế độ tối.", UiAppearance.dark(), darkSelected, false), margin(8, 0));
        root.addView(choiceGroup(group, "Cỡ chữ", "Nhân thêm trên cỡ chữ hệ thống; các nút vẫn giữ vùng chạm tối thiểu 48 dp.",
                new String[]{"90%", "100%", "115%", "130%"}, indexOfPercent(pending.fontPercent), i -> { pending.fontPercent = AppearanceSettings.FONT_PERCENTS[i]; changed(); }), margin(8, 0));
        LinearLayout extra = group.apply("Tùy chỉnh thêm", "Thu gọn các lựa chọn ảnh hưởng khoảng cách và góc thẻ.");
        extra.addView(choiceRow("Mật độ", new String[]{"Thoải mái", "Gọn"}, pending.density.equals(AppearanceSettings.DENSITY_COMPACT) ? 1 : 0,
                i -> { pending.density = i == 1 ? AppearanceSettings.DENSITY_COMPACT : AppearanceSettings.DENSITY_COMFORTABLE; changed(); }));
        extra.addView(choiceRow("Bo góc", new String[]{"Vuông", "Vừa", "Tròn"}, pending.corners.equals(AppearanceSettings.CORNERS_SQUARE) ? 0 : pending.corners.equals(AppearanceSettings.CORNERS_ROUND) ? 2 : 1,
                i -> { pending.corners = i == 0 ? AppearanceSettings.CORNERS_SQUARE : i == 2 ? AppearanceSettings.CORNERS_ROUND : AppearanceSettings.CORNERS_MEDIUM; changed(); }));
        root.addView(extra, margin(8, 0));
        Button reset = a.secondaryButton("Khôi phục mặc định", v -> { handler.removeCallbacksAndMessages(null); a.commitAppearance(AppearanceSettings.defaults()); });
        reset.setContentDescription("appearance-reset");
        root.addView(reset, margin(12, 8));
        return root;
    }

    private LinearLayout.LayoutParams margin(int top, int bottom) { return a.marginLP(-1, -2, 0, top, 0, bottom); }

    private LinearLayout previewGroup(java.util.function.BiFunction<String, String, LinearLayout> group) {
        LinearLayout g = group.apply("Xem trước", "Một dải nhỏ minh họa nền, chữ và trạng thái của giao diện.");
        previewHolder = new LinearLayout(a);
        previewHolder.setOrientation(LinearLayout.HORIZONTAL);
        previewHolder.setGravity(Gravity.CENTER_VERTICAL);
        previewHolder.setContentDescription("appearance-preview");
        g.addView(previewHolder, a.marginLP(-1, a.dp(64), 0, 6, 0, 0));
        drawPreview();
        return g;
    }

    private void drawPreview() {
        if (previewHolder == null) return;
        previewHolder.removeAllViews();
        UiAppearance.Palette p = pending.resolve(a.systemIsNight());
        LinearLayout surface = new LinearLayout(a);
        surface.setGravity(Gravity.CENTER_VERTICAL);
        surface.setPadding(a.dp(10), a.dp(6), a.dp(10), a.dp(6));
        surface.setBackground(box(p.bg, p.border, a.dp(10) * pending.cornerFactor()));
        TextView title = label("Translate Books", 14 * pending.fontScale(), p.text, true);
        surface.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        surface.addView(chip("Đang chạy", p.ok, p));
        surface.addView(a.space(6, 1));
        TextView action = label("Nút chính", 12 * pending.fontScale(), p.onAccent, true);
        action.setGravity(Gravity.CENTER);
        action.setBackground(box(p.accent, p.accent, a.dp(9) * pending.cornerFactor()));
        surface.addView(action, new LinearLayout.LayoutParams(a.dp(96), a.dp(48)));
        previewHolder.addView(surface, new LinearLayout.LayoutParams(-1, -1));
    }

    private TextView label(String text, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(text); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView chip(String text, int color, UiAppearance.Palette p) {
        TextView t = label(text, 12 * pending.fontScale(), color, true);
        t.setGravity(Gravity.CENTER); t.setPadding(a.dp(8), 0, a.dp(8), 0);
        t.setBackground(box(ColorMath.mix(color, p.card, 0.85f), color, a.dp(10) * pending.cornerFactor()));
        return t;
    }

    private GradientDrawable box(int fill, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(radius); g.setStroke(a.dp(1), stroke); return g;
    }

    private LinearLayout modeGroup(java.util.function.BiFunction<String, String, LinearLayout> group) {
        LinearLayout g = group.apply("Chế độ", "Cố định một giao diện, hoặc tự đổi sáng/tối theo điện thoại.");
        LinearLayout row = a.rowContainer();
        boolean system = AppearanceSettings.MODE_SYSTEM.equals(pending.mode);
        Button fixed = system ? a.secondaryButton("Cố định", v -> setMode(AppearanceSettings.MODE_FIXED)) : a.primaryButton("✓ Cố định", v -> { });
        Button follow = system ? a.primaryButton("✓ Theo hệ thống", v -> { }) : a.secondaryButton("Theo hệ thống", v -> setMode(AppearanceSettings.MODE_SYSTEM));
        fixed.setContentDescription("appearance-mode-fixed"); follow.setContentDescription("appearance-mode-system");
        row.addView(fixed, new LinearLayout.LayoutParams(0, a.dp(48), 1)); row.addView(a.space(8, 1)); row.addView(follow, new LinearLayout.LayoutParams(0, a.dp(48), 1));
        g.addView(row, a.marginLP(-1, -2, 0, 6, 0, 0)); return g;
    }

    private void setMode(String mode) { pending.mode = mode; changed(); }

    private LinearLayout paletteGroup(java.util.function.BiFunction<String, String, LinearLayout> group, String title, String description,
                                      List<UiAppearance.Palette> list, String selectedId, boolean lightSlot) {
        LinearLayout g = group.apply(title, description);
        int columns = a.isWideLayout() ? 3 : 2; LinearLayout row = null;
        for (int i = 0; i < list.size(); i++) {
            if (i % columns == 0) { row = a.rowContainer(); g.addView(row, a.marginLP(-1, -2, 0, 4, 0, 4)); }
            if (row.getChildCount() > 0) row.addView(a.space(6, 1));
            UiAppearance.Palette p = list.get(i);
            row.addView(cell(p, p.id.equals(selectedId), lightSlot), new LinearLayout.LayoutParams(0, a.dp(86), 1));
        }
        if (row != null) while ((row.getChildCount() + 1) / 2 < columns) { row.addView(a.space(6, 1)); row.addView(new View(a), new LinearLayout.LayoutParams(0, a.dp(86), 1)); }
        return g;
    }

    private View cell(UiAppearance.Palette p, boolean selected, boolean lightSlot) {
        LinearLayout cell = new LinearLayout(a); cell.setOrientation(LinearLayout.VERTICAL); cell.setPadding(a.dp(10), a.dp(9), a.dp(10), a.dp(9));
        TextView swatch = label("", 1, p.accent, false); swatch.setBackground(box(p.card, p.accent, a.dp(8) * pending.cornerFactor())); cell.addView(swatch, new LinearLayout.LayoutParams(-1, a.dp(34)));
        TextView name = label((selected ? "✓  " : "") + p.name, 12 * a.fontScale, selected ? p.text : p.muted, selected); name.setGravity(Gravity.CENTER); cell.addView(name, new LinearLayout.LayoutParams(-1, a.dp(34)));
        a.tint(cell, p.panel, selected ? a.BLUE : a.BORDER, selected ? 2 : 1, 10); cell.setContentDescription("palette:" + p.id);
        cell.setOnClickListener(v -> { if (AppearanceSettings.MODE_SYSTEM.equals(pending.mode)) { if (lightSlot) pending.paletteLight = p.id; else pending.palette = p.id; } else pending.palette = p.id; a.commitAppearance(pending); });
        return cell;
    }

    private LinearLayout choiceGroup(java.util.function.BiFunction<String, String, LinearLayout> group, String title, String description, String[] labels, int selected, Choice choice) {
        LinearLayout g = group.apply(title, description); g.addView(choiceRow(null, labels, selected, choice)); return g;
    }

    private LinearLayout choiceRow(String heading, String[] labels, int selected, Choice choice) {
        LinearLayout wrap = new LinearLayout(a); wrap.setOrientation(LinearLayout.VERTICAL);
        if (heading != null) wrap.addView(a.text(heading, 13, a.TEXT, true));
        LinearLayout row = a.rowContainer();
        for (int i = 0; i < labels.length; i++) { final int index = i; if (i > 0) row.addView(a.space(6, 1)); Button b = i == selected ? a.primaryButton("✓ " + labels[i], v -> { }) : a.secondaryButton(labels[i], v -> choice.pick(index)); b.setContentDescription("choice:" + (heading == null ? "appearance" : heading) + ":" + labels[i]); row.addView(b, new LinearLayout.LayoutParams(0, a.dp(48), 1)); }
        wrap.addView(row, a.marginLP(-1, -2, 0, 6, 0, 0)); return wrap;
    }

    private int indexOfPercent(int percent) { for (int i = 0; i < AppearanceSettings.FONT_PERCENTS.length; i++) if (AppearanceSettings.FONT_PERCENTS[i] == percent) return i; return 1; }
    private interface Choice { void pick(int index); }

    private void changed() { drawPreview(); handler.removeCallbacksAndMessages(null); handler.postDelayed(() -> a.commitAppearance(pending), APPLY_DELAY_MS); }
}
