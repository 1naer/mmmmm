package com.mmm.pan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1:1 复刻竞品：核心是一个全屏WebView，用于直接加载网盘网页
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // 顶部的极简网盘切换栏
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Color.parseColor("#F5F7FA"));
        navBar.setPadding(20, 20, 20, 20);

        Button btnQuark = new Button(this);
        btnQuark.setText("夸克网盘");
        btnQuark.setOnClickListener(v -> loadDrive("https://pan.quark.cn/"));

        Button btnAli = new Button(this);
        btnAli.setText("阿里云盘");
        btnAli.setOnClickListener(v -> loadDrive("https://www.alipan.com/"));

        Button btnDownloads = new Button(this);
        btnDownloads.setText("下载中心");
        btnDownloads.setTextColor(Color.parseColor("#3498DB"));
        btnDownloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));

        navBar.addView(btnQuark, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        navBar.addView(btnAli, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        navBar.addView(btnDownloads, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 底部的核心WebView
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // 模拟PC端UA，很多网盘的移动端网页功能受限，竞品通常会伪装PC
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 这里是核心：页面加载完后，我们要把类似于ceshi.js的脚本注入进去
                // 在页面上生成“下载助手”的按钮
                injectLinkSwiftScript(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        root.addView(navBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        // 默认加载夸克网盘
        loadDrive("https://pan.quark.cn/");
    }

    private void loadDrive(String url) {
        webView.loadUrl(url);
    }

    private void injectLinkSwiftScript(WebView view, String url) {
        // 这是竞品最核心的技术手段：动态向网盘页面注入 JS，劫持页面 DOM 生成下载按钮
        String js = "javascript:(function() {" +
            "if(document.getElementById('operit-download-btn')) return;" +
            "var btn = document.createElement('button');" +
            "btn.id = 'operit-download-btn';" +
            "btn.innerHTML = '✨ 提取直链下载';" +
            "btn.style.cssText = 'position:fixed; bottom:20px; right:20px; z-index:99999; background:#3498DB; color:white; padding:15px 25px; border-radius:30px; font-size:16px; font-weight:bold; box-shadow: 0 4px 10px rgba(0,0,0,0.3); border:none;';" +
            "btn.onclick = function() {" +
            "   /* 这里后续对接 ShareLinkParser 的底层原生接口 */" +
            "   alert('正在对接底层原生下载隧道...');" +
            "};" +
            "document.body.appendChild(btn);" +
        "})()";
        view.evaluateJavascript(js, null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
