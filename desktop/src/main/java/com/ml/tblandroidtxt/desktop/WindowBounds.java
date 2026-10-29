package com.ml.tblandroidtxt.desktop;

import com.ml.tblandroidtxt.KeyValueStore;

import java.util.List;

/** Size and place of the main window, kept between runs. A saved place that is no longer on any screen (monitor unplugged) is ignored. */
final class WindowBounds {
    final double x, y, width, height;
    final boolean maximized;

    WindowBounds(double x, double y, double width, double height, boolean maximized) {
        this.x = x; this.y = y; this.width = width; this.height = height; this.maximized = maximized;
    }

    static WindowBounds load(KeyValueStore p) {
        try {
            double w = Double.parseDouble(p.getString("win_w", ""));
            double h = Double.parseDouble(p.getString("win_h", ""));
            double x = Double.parseDouble(p.getString("win_x", ""));
            double y = Double.parseDouble(p.getString("win_y", ""));
            if (w < 800 || h < 500 || Double.isNaN(x) || Double.isNaN(y)) return null;
            return new WindowBounds(x, y, w, h, "true".equals(p.getString("win_max", "false")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void save(KeyValueStore p) {
        p.edit().putString("win_x", String.valueOf(x)).putString("win_y", String.valueOf(y)).putString("win_w", String.valueOf(width))
                .putString("win_h", String.valueOf(height)).putString("win_max", String.valueOf(maximized)).commit();
    }

    /** True when at least a 100 x 60 piece of the window's top-left area is inside one of the screens (x, y, width, height each). */
    boolean visibleOn(List<double[]> screens) {
        for (double[] s : screens) {
            double ox = Math.min(x + width, s[0] + s[2]) - Math.max(x, s[0]);
            double oy = Math.min(y + height, s[1] + s[3]) - Math.max(y, s[1]);
            if (ox >= 100 && oy >= 60) return true;
        }
        return false;
    }
}
