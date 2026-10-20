package com.ml.tblandroidtxt;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A text button that never has a fixed height: a layout asked to give it 36, 40 or 48 dp gets "wrap content" instead, with a minimum of
 * 48 dp, so a long label on a narrow button or a larger system font grows the button instead of cutting the text.
 */
final class WrapButton extends Button {
    static final int MIN_HEIGHT_DP = 48;

    WrapButton(Context context) {
        super(context);
        int min = Math.round(MIN_HEIGHT_DP * context.getResources().getDisplayMetrics().density);
        setMinHeight(min);
        setMinimumHeight(min);
        setSingleLine(false);
    }

    @Override public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params != null) params.height = heightFor(params.height);
        super.setLayoutParams(params);
    }

    /** What a fixed height becomes; shared with the unit test. */
    static int heightFor(int requested) { return requested > 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : requested; }
}
