package com.mmm.pan;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    EditText input;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Root ScrollView setup
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 100, 60, 60);
        scrollView.addView(root);

        // App Title
        TextView title = new TextView(this);
        title.setText("mmm");
        title.setTextSize(36);
        title.setTextColor(Color.parseColor("#2C3E50"));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.getPaint().setFakeBoldText(true);

        // App Subtitle
        TextView sub = new TextView(this);
        sub.setText("网盘分享链接解析与下载");
        sub.setTextSize(15);
        sub.setTextColor(Color.parseColor("#7F8C8D"));
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.setMargins(0, 10, 0, 80);

        // Card Container for Input
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createCardBackground());
        card.setElevation(16f);
        card.setPadding(40, 40, 40, 40);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 60);

        // Input Field
        input = new EditText(this);
        input.setHint("在此粘贴网盘分享链接\n例如: pan.baidu.com/s/...");
        input.setSingleLine(false);
        input.setMinLines(4);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackground(null);
        input.setTextSize(16);
        input.setTextColor(Color.parseColor("#34495E"));
        input.setHintTextColor(Color.parseColor("#BDC3C7"));

        card.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Action Buttons
        Button open = createPrimaryButton("解析并打开");
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140);
        btnParams.setMargins(0, 0, 0, 30);
        
        Button downloads = createSecondaryButton("下载管理");
        LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140);

        // Assembly
        root.addView(title);
        root.addView(sub, subParams);
        root.addView(card, cardParams);
        root.addView(open, btnParams);
        root.addView(downloads, btnParams2);

        setContentView(scrollView);

        // Logic
        String shared = readSharedText();
        if (!TextUtils.isEmpty(shared)) input.setText(shared);

        open.setOnClickListener(v -> {
            String text = input.getText().toString();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "请输入分享链接", Toast.LENGTH_SHORT).show();
                return;
            }
            ShareLinkParser.Result result = ShareLinkParser.parse(text);
            if (!result.ok) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
                return;
            }
            input.setText(result.url);
            Toast.makeText(this, "解析成功: " + result.message, Toast.LENGTH_SHORT).show();
            BrowserActivity.open(this, result.url);
        });

        downloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(32f);
        return drawable;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        
        GradientDrawable contentDrawable = new GradientDrawable();
        contentDrawable.setColor(Color.parseColor("#3498DB"));
        contentDrawable.setCornerRadius(24f);

        RippleDrawable rippleDrawable = new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#2980B9")),
                contentDrawable,
                null
        );
        button.setBackground(rippleDrawable);
        button.setElevation(8f);
        return button;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.parseColor("#3498DB"));
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setStateListAnimator(null);

        GradientDrawable contentDrawable = new GradientDrawable();
        contentDrawable.setColor(Color.parseColor("#EAF2F8"));
        contentDrawable.setCornerRadius(24f);
        contentDrawable.setStroke(3, Color.parseColor("#3498DB"));

        RippleDrawable rippleDrawable = new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#D4E6F1")),
                contentDrawable,
                null
        );
        button.setBackground(rippleDrawable);
        return button;
    }

    private String readSharedText() {
        Intent i = getIntent();
        if (Intent.ACTION_SEND.equals(i.getAction())) {
            return i.getStringExtra(Intent.EXTRA_TEXT);
        }
        return null;
    }
}
