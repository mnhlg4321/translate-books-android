package com.ml.tblandroidtxt;

import android.content.Context;
import android.widget.ScrollView;

/** Short previews wrap their content; long previews use available screen space and remain scrollable. */
final class TranslationPreviewScrollView extends ScrollView {
    TranslationPreviewScrollView(Context context) { super(context); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int screenDp = getResources().getConfiguration().screenHeightDp;
        int capDp = Math.max(160, Math.min(480, Math.round(screenDp * 0.45f)));
        int cap = Math.round(capDp * getResources().getDisplayMetrics().density);
        int size = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED
                ? cap : Math.min(cap, MeasureSpec.getSize(heightSpec));
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST));
    }
}
