package com.ml.tblandroidtxt;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


class SamplePageFactory {
    private final MainActivity a;

    SamplePageFactory(MainActivity activity) {
        this.a = activity;
    }


    View buildSamplePage() {
        ScrollView scroll = a.scroll();
        LinearLayout root = a.pageRoot();
        scroll.addView(root);
        LinearLayout sample = a.sectionCard("⚗", "Sample Config");
        a.sampleText = a.text(a.sampleConfig(), 12, a.TEXT, false);
        a.sampleText.setTypeface(Typeface.MONOSPACE);
        a.sampleText.setTextIsSelectable(true);
        sample.addView(a.sampleText);
        root.addView(sample);
        return scroll;
    }
}
