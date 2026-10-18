package com.ml.tblandroidtxt;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * A slot that shows what is chosen: label, status chip, a Chọn / Đổi button and every chosen name on its own lines (wrap_content,
 * at most three lines per name). It replaces the buttons that used to carry a file name inside a fixed-height box.
 */
final class SelectedFilesSlot {
    enum Status { NOT_CHOSEN, CHOSEN, OFF }

    private static final int MAX_SHOWN = 5;

    private SelectedFilesSlot() {}

    /**
     * @param names    the chosen names, in order
     * @param metas    one short note per name (e.g. "Chương 1"), may be empty strings
     * @param offText  chip text when {@code status} is OFF (e.g. "Không dùng")
     */
    static View build(MainActivity a, String label, String icon, List<String> names, List<String> metas, Status status, String offText,
                      String actionLabel, Runnable action) {
        LinearLayout box = a.card(14, a.FIELD, status == Status.CHOSEN ? a.GREEN : status == Status.NOT_CHOSEN ? a.AMBER : a.BORDER);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
        box.setContentDescription("slot:" + label);

        LinearLayout head = a.rowContainer();
        head.addView(a.text(label, 15, a.TEXT, true), new LinearLayout.LayoutParams(a.dp(84), -2));
        String chipText = status == Status.CHOSEN ? "✓ Đã chọn " + names.size() : status == Status.OFF ? offText : "⚠ Chưa chọn";
        TextView chip = a.text(chipText, 13, status == Status.CHOSEN ? a.GREEN : status == Status.OFF ? a.MUTED : a.AMBER, true);
        chip.setSingleLine(false);
        chip.setContentDescription("slot-status:" + label);
        head.addView(chip, new LinearLayout.LayoutParams(0, -2, 1));
        Button act = a.secondaryButton(actionLabel, v -> action.run());
        act.setContentDescription("slot-action:" + label);
        head.addView(act, new LinearLayout.LayoutParams(-2, a.dp(40)));
        box.addView(head);

        LinearLayout lines = new LinearLayout(a);
        lines.setOrientation(LinearLayout.VERTICAL);
        box.addView(lines, a.marginLP(-1, -2, 0, 4, 0, 0));
        boolean[] expanded = {false};
        Runnable render = new Runnable() {
            @Override public void run() {
                lines.removeAllViews();
                int shown = expanded[0] ? names.size() : Math.min(MAX_SHOWN, names.size());
                for (int i = 0; i < shown; i++) {
                    String meta = i < metas.size() && !metas.get(i).isEmpty() ? "  · " + metas.get(i) : "";
                    TextView t = a.text(icon + " " + names.get(i) + meta, 13, a.TEXT, false);
                    t.setSingleLine(false);
                    t.setMaxLines(Integer.MAX_VALUE);
                    
                    t.setContentDescription("slot-name:" + names.get(i));
                    lines.addView(t, a.marginLP(-1, -2, 8, 2, 0, 0));
                }
                if (names.size() > MAX_SHOWN) {
                    TextView more = a.text(expanded[0] ? "Thu gọn ▴" : "và " + (names.size() - MAX_SHOWN) + " tệp nữa ▾", 13, a.BLUE, true);
                    more.setGravity(Gravity.START);
                    more.setPadding(a.dp(8), a.dp(6), a.dp(8), a.dp(6));
                    more.setOnClickListener(v -> { expanded[0] = !expanded[0]; this.run(); });
                    lines.addView(more);
                }
            }
        };
        render.run();
        return box;
    }
}
