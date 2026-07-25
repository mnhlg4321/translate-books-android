package com.ml.tblandroidtxt.macrobenchmark;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public final class BenchmarkHostActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View content = new View(this);
        content.setContentDescription("Macrobenchmark host");
        setContentView(content);
    }
}
