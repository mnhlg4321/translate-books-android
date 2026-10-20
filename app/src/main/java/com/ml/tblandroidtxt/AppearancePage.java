package com.ml.tblandroidtxt;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Settings > Appearance: mode (fixed or following the system), palette(s), accent colour, app font size, density and corners, with a live
 * preview card and a reset. Every change redraws the preview at once and is applied to the whole app a moment later (the Activity is
 * rebuilt with the matching theme and comes back on this same page).
 */
final class AppearancePage {
    private static final long APPLY_DELAY_MS = 700L;

    private final MainActivity a;
    private final AppearanceSettings pending;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout previewHolder;
    private TextView accentError;
    private EditText accentField;

    AppearancePage(MainActivity activity) {
        a = activity;
        pending = AppearanceSettings.load(activity.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE));
    }

    View build(LinearLayout root, java.util.function.BiFunction<String, String, LinearLayout> group) {
        root.addView(previewGroup(group), margin(0, 0));
        root.addView(modeGroup(group), margin(8, 0));
        if (AppearanceSettings.MODE_SYSTEM.equals(pending.mode)) {
            root.addView(paletteGroup(group, "Khi điện thoại ở chế độ tối", "Palette dùng khi Android đang ở chế độ tối.", UiAppearance.dark(), pending.palette, false), margin(8, 0));
            root.addView(paletteGroup(group, "Khi điện thoại ở chế độ sáng", "Palette dùng khi Android đang ở chế độ sáng.", UiAppearance.light(), pending.paletteLight, true), margin(8, 0));
        } else {
            root.addView(paletteGroup(group, "Workspace color", "Màu nền của toàn app. Xanh lá, vàng và đỏ dành riêng cho trạng thái.", UiAppearance.all(), pending.palette, false), margin(8, 0));
        }
        root.addView(accentGroup(group), margin(8, 0));
        root.addView(choiceGroup(group, "Cỡ chữ của app", "Nhân thêm trên cỡ chữ hệ thống. Ô tiến trình vẫn giữ chiều cao và tự co chữ.",
                new String[]{"90%", "100%", "115%", "130%"}, indexOfPercent(pending.fontPercent), i -> { pending.fontPercent = AppearanceSettings.FONT_PERCENTS[i]; changed(); }), margin(8, 0));
        root.addView(choiceGroup(group, "Mật độ", "Gọn thu nhỏ khoảng cách; nút vẫn cao từ 44 dp.",
                new String[]{"Thoải mái", "Gọn"}, pending.density.equals(AppearanceSettings.DENSITY_COMPACT) ? 1 : 0,
                i -> { pending.density = i == 1 ? AppearanceSettings.DENSITY_COMPACT : AppearanceSettings.DENSITY_COMFORTABLE; changed(); }), margin(8, 0));
        root.addView(choiceGroup(group, "Bo góc", "Áp cho thẻ và nút.",
                new String[]{"Vuông", "Vừa", "Tròn"}, pending.corners.equals(AppearanceSettings.CORNERS_SQUARE) ? 0 : pending.corners.equals(AppearanceSettings.CORNERS_ROUND) ? 2 : 1,
                i -> { pending.corners = i == 0 ? AppearanceSettings.CORNERS_SQUARE : i == 2 ? AppearanceSettings.CORNERS_ROUND : AppearanceSettings.CORNERS_MEDIUM; changed(); }), margin(8, 0));
        Button reset = a.secondaryButton("Khôi phục mặc ịnh", v -> {
            handler.removeCallbacksAndMessages(null);
            a.commitAppearance(AppearanceSettings.defaults());
        });
        reset.setContentDescription("appearance-reset");
        root.addView(reset, margin(12, 8));
        return root;
    }

    private LinearLayout.LayoutParams margin(int top, int bottom) { return a.marginLP(-1, -2, 0, top, 0, bottom); }

    // ---- preview ----

    private LinearLayout previewGroup(java.util.function.BiFunction<String, String, LinearLayout> group) {
        LinearLayout g = group.apply("Xem trước", "Thẻ mẫu đổi ngay khi bạn chọn; cả app đổi theo sau một lúc.");
        previewHolder = new LinearLayout(a);
        previewHolder.setOrientation(LinearLayout.VERTICAL);
        previewHolder.setContentDescription("appearance-preview");
        g.addView(previewHolder, a.marginLP(-1, -2, 0, 6, 0, 0));
        drawPreview();
        return g;
    }

    /** The sample card, drawn with the colours and sizes of the pending choice (not of the app as it is now). */
    private void drawPreview() {
        if (previewHolder == null) return;
        previewHolder.removeAllViews();
        UiAppearance.Palette p = pending.resolve(a.systemIsNight());
        float font = pending.fontScale(), density = pending.densityScale(), corner = pending.cornerFactor();
        LinearLayout surface = new LinearLayout(a);
        surface.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(a.dp(12) * density);
        surface.setPadding(pad, pad, pad, pad);
        surface.setBackground(box(p.bg, p.border, a.dp(14) * corner));
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(box(p.card, p.border, a.dp(14) * corner));
        TextView title = label("Tiêu đề thẻ mẫu", 16 * font, p.text, true);
        TextView muted = label("Chữ phụ: mô tả ngắn dùng màu mờ.", 12 * font, p.muted, false);
        card.addView(title);
        card.addView(muted, a.marginLP(-1, -2, 0, 2, 0, 8));
        LinearLayout chips = new LinearLayout(a);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip("Xong", p.ok, p, font, corner));
        chips.addView(a.space(6, 1));
        chips.addView(chip("Cảnh báo", p.warn, p, font, corner));
        chips.addView(a.space(6, 1));
        chips.addView(chip("Lỗi", p.bad, p, font, corner));
        card.addView(chips, a.marginLP(-1, -2, 0, 0, 0, 8));
        LinearLayout buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView primary = label("Nút chính", 13 * font, p.onAccent, true);
        primary.setGravity(Gravity.CENTER);
        primary.setMinHeight(a.dp(44));
        primary.setBackground(box(p.accent, p.accent, a.dp(12) * corner));
        TextView secondary = label("Nút phụ", 13 * font, p.text, true);
        secondary.setGravity(Gravity.CENTER);
        secondary.setMinHeight(a.dp(44));
        secondary.setBackground(box(p.field, p.border, a.dp(12) * corner));
        buttons.addView(primary, new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(a.space(8, 1));
        buttons.addView(secondary, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(buttons);
        surface.addView(card);
        previewHolder.addView(surface);
    }

    private TextView label(String text, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView chip(String text, int color, UiAppearance.Palette p, float font, float corner) {
        TextView t = label(text, 12 * font, color, true);
        t.setPadding(a.dp(10), a.dp(4), a.dp(10), a.dp(4));
        t.setBackground(box(ColorMath.mix(color, p.card, 0.85f), color, a.dp(14) * corner));
        return t;
    }

    private GradientDrawable box(int fill, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        g.setStroke(a.dp(1), stroke);
        return g;
    }

    // ---- groups ----

    private LinearLayout modeGroup(java.util.function.BiFunction<String, String, LinearLayout> group) {
        LinearLayout g = group.apply("Chế độ", "Cố định một palette, hoặc tự đổi sáng/tối theo điện thoại.");
        LinearLayout row = a.rowContainer();
        boolean system = AppearanceSettings.MODE_SYSTEM.equals(pending.mode);
        Button fixed = system ? a.secondaryButton("Cố định", v -> setMode(AppearanceSettings.MODE_FIXED)) : a.primaryButton("✓ Cố định", v -> { });
        Button follow = system ? a.primaryButton("✓ Theo hệ thống", v -> { }) : a.secondaryButton("Theo hệ thống", v -> setMode(AppearanceSettings.MODE_SYSTEM));
        fixed.setContentDescription("appearance-mode-fixed");
        follow.setContentDescription("appearance-mode-system");
        row.addView(fixed, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(a.space(8, 1));
        row.addView(follow, new LinearLayout.LayoutParams(0, -2, 1));
        g.addView(row, a.marginLP(-1, -2, 0, 6, 0, 0));
        return g;
    }

    private void setMode(String mode) {
        pending.mode = mode;
        if (AppearanceSettings.MODE_FIXED.equals(mode) && UiAppearance.find(pending.palette).light) pending.palette = "default";
        changed();
    }

    private LinearLayout paletteGroup(java.util.function.BiFunction<String, String, LinearLayout> group, String title, String text, List<UiAppearance.Palette> list, String selectedId, boolean lightSlot) {
        LinearLayout g = group.apply(title, text);
        int columns = a.isWideLayout() ? 3 : 2;
        LinearLayout row = null;
        for (int i = 0; i < list.size(); i++) {
            if (i % columns == 0) { row = a.rowContainer(); g.addView(row, a.marginLP(-1, -2, 0, 4, 0, 4)); }
            if (row.getChildCount() > 0) row.addView(a.space(6, 1));
            row.addView(cell(list.get(i), list.get(i).id.equals(selectedId), lightSlot), new LinearLayout.LayoutParams(0, a.dp(86), 1));
        }
        if (row != null) while ((row.getChildCount() + 1) / 2 < columns) { row.addView(a.space(6, 1)); row.addView(new View(a), new LinearLayout.LayoutParams(0, a.dp(86), 1)); }
        return g;
    }

    private View cell(UiAppearance.Palette p, boolean selected, boolean lightSlot) {
        LinearLayout cell = new LinearLayout(a);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(a.dp(10), a.dp(9), a.dp(10), a.dp(9));
        TextView swatch = label("", 1, p.accent, false);
        swatch.setBackground(box(p.card, p.accent, a.dp(8) * a.cornerFactor));
        cell.addView(swatch, new LinearLayout.LayoutParams(-1, a.dp(34)));
        TextView name = label((selected ? "✓  " : "") + p.name, 12 * a.fontScale, selected ? a.TEXT : a.MUTED, selected);
        name.setGravity(Gravity.CENTER);
        cell.addView(name, new LinearLayout.LayoutParams(-1, a.dp(34)));
        a.tint(cell, p.panel, selected ? a.BLUE : a.BORDER, selected ? 2 : 1, 10);
        cell.setContentDescription("palette:" + p.id);
        cell.setOnClickListener(v -> {
            if (lightSlot || (AppearanceSettings.MODE_FIXED.equals(pending.mode) && p.light)) pending.paletteLight = p.id;
            if (!lightSlot) pending.palette = p.id;
            // A palette is applied at once; the other controls wait a moment so the preview can be seen.
            handler.removeCallbacksAndMessages(null);
            a.commitAppearance(pending);
        });
        return cell;
    }

    private LinearLayout accentGroup(java.util.function.BiFunction<String, String, LinearLayout> group) {
        LinearLayout g = group.apply("Màu nhấn", "8 màu có sẵn hoặc nhập mã hex. Màu khó đọc trên nền bị từ chối.");
        LinearLayout swatches = a.rowContainer();
        for (int i = 0; i < AppearanceSettings.PRESET_ACCENTS.length; i++) {
            final String hex = AppearanceSettings.PRESET_ACCENTS[i];
            int color = ColorMath.parseHex(hex);
            View sw = new View(a);
            boolean selected = hex.equalsIgnoreCase(pending.accent);
            sw.setBackground(box(color, selected ? a.TEXT : a.BORDER, a.dp(22) * a.cornerFactor));
            sw.setContentDescription("accent:" + hex);
            sw.setOnClickListener(v -> tryAccent(hex));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, a.dp(44), 1);
            lp.setMargins(a.dp(2), 0, a.dp(2), 0);
            swatches.addView(sw, lp);
        }
        g.addView(swatches, a.marginLP(-1, -2, 0, 6, 0, 6));
        LinearLayout typed = a.rowContainer();
        accentField = a.input("#RRGGBB", pending.accent);
        accentField.setSingleLine(true);
        accentField.setContentDescription("accent-hex");
        typed.addView(accentField, new LinearLayout.LayoutParams(0, -2, 1));
        typed.addView(a.space(8, 1));
        Button apply = a.secondaryButton("Áp dụng", v -> tryAccent(accentField.getText().toString()));
        apply.setContentDescription("accent-apply");
        typed.addView(apply, new LinearLayout.LayoutParams(-2, -2));
        g.addView(typed);
        accentError = label("", 12 * a.fontScale, a.RED, false);
        accentError.setContentDescription("accent-error");
        accentError.setVisibility(View.GONE);
        g.addView(accentError, a.marginLP(-1, -2, 0, 4, 0, 0));
        Button clear = a.secondaryButton("Dùng màu của palette", v -> { pending.accent = ""; changed(); });
        g.addView(clear, a.marginLP(-1, -2, 0, 6, 0, 0));
        return g;
    }

    /** Checks the accent against every palette it can be shown on (both slots when the system mode is followed). */
    private void tryAccent(String text) {
        String trimmed = text == null ? "" : text.trim();
        Integer color = ColorMath.parseHex(trimmed);
        String error = null;
        if (color == null) error = AppearanceSettings.accentError(null, a.appearance);
        else if (AppearanceSettings.MODE_SYSTEM.equals(pending.mode)) {
            error = AppearanceSettings.accentError(color, UiAppearance.find(pending.palette));
            if (error == null) error = AppearanceSettings.accentError(color, UiAppearance.find(pending.paletteLight));
        } else error = AppearanceSettings.accentError(color, UiAppearance.find(pending.palette));
        if (error != null) {
            accentError.setText(error);
            accentError.setVisibility(View.VISIBLE);
            return;
        }
        accentError.setVisibility(View.GONE);
        pending.accent = ColorMath.hex(color);
        changed();
    }

    private interface Choice { void pick(int index); }

    private LinearLayout choiceGroup(java.util.function.BiFunction<String, String, LinearLayout> group, String title, String text, String[] labels, int selected, Choice choice) {
        LinearLayout g = group.apply(title, text);
        LinearLayout row = a.rowContainer();
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button b = i == selected ? a.primaryButton("✓ " + labels[i], v -> { }) : a.secondaryButton(labels[i], v -> choice.pick(index));
            b.setContentDescription("choice:" + title + ":" + labels[i]);
            if (i > 0) row.addView(a.space(6, 1));
            row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        }
        g.addView(row, a.marginLP(-1, -2, 0, 6, 0, 0));
        return g;
    }

    private int indexOfPercent(int percent) {
        for (int i = 0; i < AppearanceSettings.FONT_PERCENTS.length; i++) if (AppearanceSettings.FONT_PERCENTS[i] == percent) return i;
        return 1;
    }

    /** The preview shows the new choice now; the app follows after a short pause (so several taps in a row apply once). */
    private void changed() {
        drawPreview();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> a.commitAppearance(pending), APPLY_DELAY_MS);
    }
}
