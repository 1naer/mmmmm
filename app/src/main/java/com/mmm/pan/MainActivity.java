package com.mmm.pan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends Activity {
    private EditText input;
    private Button openBtn;
    private WebView headlessWebView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private AlertDialog loadingDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 100, 60, 60);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("mmm");
        title.setTextSize(36);
        title.setTextColor(Color.parseColor("#2C3E50"));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.getPaint().setFakeBoldText(true);

        TextView sub = new TextView(this);
        sub.setText("全网盘无感解析与极速下载");
        sub.setTextSize(15);
        sub.setTextColor(Color.parseColor("#7F8C8D"));
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.setMargins(0, 10, 0, 80);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createCardBackground());
        card.setElevation(16f);
        card.setPadding(40, 40, 40, 40);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 60);

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

        openBtn = createPrimaryButton("智能解析提取");
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140);
        btnParams.setMargins(0, 0, 0, 30);
        
        Button downloadsBtn = createSecondaryButton("下载管理中心");
        LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140);
        btnParams2.setMargins(0, 0, 0, 40);

        root.addView(title);
        root.addView(sub, subParams);
        root.addView(card, cardParams);
        root.addView(openBtn, btnParams);
        root.addView(downloadsBtn, btnParams2);

        // 动态生成所有网盘的登录授权按钮
        String[] drives = {"百度网盘", "夸克网盘", "阿里云盘", "天翼云盘", "迅雷云盘", "115网盘", "123云盘"};
        LinearLayout currentAuthRow = null;
        for (int i = 0; i < drives.length; i++) {
            if (i % 2 == 0) {
                currentAuthRow = new LinearLayout(this);
                currentAuthRow.setOrientation(LinearLayout.HORIZONTAL);
                root.addView(currentAuthRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            
            Button authBtn = createAuthButton("授权 " + drives[i]);
            final String dType = drives[i];
            authBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra("DRIVE_TYPE", dType);
                startActivity(intent);
            });
            
            LinearLayout.LayoutParams authParams = new LinearLayout.LayoutParams(0, 110, 1);
            authParams.setMargins(i % 2 == 0 ? 0 : 10, 0, i % 2 == 0 ? 10 : 0, 20);
            currentAuthRow.addView(authBtn, authParams);
        }

        setContentView(scrollView);

        String shared = readSharedText();
        if (!TextUtils.isEmpty(shared)) input.setText(shared);

        openBtn.setOnClickListener(v -> {
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
            startSilentParsing(result.url, result.provider);
        });

        downloadsBtn.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));
        setupHeadlessWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupHeadlessWebView() {
        headlessWebView = new WebView(this);
        WebSettings settings = headlessWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        
        headlessWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void startDownload(String url, String filename) {
                mainHandler.post(() -> onParsedSuccessfully(url, filename));
            }
        }, "MMM_NATIVE");

        headlessWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectStubScript();
            }
        });
    }

    private void injectStubScript() {
        try {
            InputStream is = getAssets().open("scripts/quark_stub.js");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String script = new String(buffer, "UTF-8");
            headlessWebView.evaluateJavascript(script, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSilentParsing(String url, String provider) {
        showLoadingDialog();
        headlessWebView.clearHistory();
        headlessWebView.clearCache(true);

        // 全网盘全局 Cookie 注入
        String savedCookie = getSharedPreferences("DriveAuth", MODE_PRIVATE).getString(provider + "_COOKIE", "");

        if (!TextUtils.isEmpty(savedCookie)) {
            CookieManager.getInstance().setCookie(url, savedCookie);
            CookieManager.getInstance().flush();
        }

        headlessWebView.loadUrl(url);
        
        mainHandler.postDelayed(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                hideLoadingDialog();
                Toast.makeText(this, "解析超时，可能是未登录导致的限制，请先在下方点击【授权】登录对应网盘", Toast.LENGTH_LONG).show();
            }
        }, 15000);
    }

    private void onParsedSuccessfully(String directUrl, String filename) {
        hideLoadingDialog();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 成功提取文件");
        builder.setMessage("文件名: " + filename + "\n\n是否立即创建极速下载任务？");
        builder.setPositiveButton("极速下载", (dialog, which) -> {
            DownloadTask task = new DownloadTask();
            task.id = UUID.randomUUID().toString();
            task.url = directUrl;
            task.name = filename;
            task.status = DownloadTask.STATUS_PAUSED;
            
            // 写入带有Cookie认证的鉴权头给多线程引擎
            String headers = String.format("{\"Cookie\":\"%s\"}", CookieManager.getInstance().getCookie(directUrl));
            task.headersJson = headers;

            DownloadRepository.update(this, task);
            DownloadService.startTask(this, task.id);
            Toast.makeText(this, "已加入多线程极速下载队列", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("正在进行无感解析");
            builder.setMessage("引擎正在后台提取真实直链，请稍候...");
            builder.setCancelable(false);
            loadingDialog = builder.create();
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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

        RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#2980B9")), contentDrawable, null);
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

        RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#D4E6F1")), contentDrawable, null);
        button.setBackground(rippleDrawable);
        return button;
    }

    private Button createAuthButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.parseColor("#8E44AD"));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setStateListAnimator(null);

        GradientDrawable contentDrawable = new GradientDrawable();
        contentDrawable.setColor(Color.parseColor("#F4ECF7"));
        contentDrawable.setCornerRadius(18f);

        RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#D7BDE2")), contentDrawable, null);
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
