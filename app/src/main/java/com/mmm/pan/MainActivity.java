package com.mmm.pan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 核心一：强制应用级别硬件加速 (解决底层花屏撕裂)
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // 导航栏 (对齐萌娘助手)
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

        // 核心二：极限配置的 WebView 引擎
        webView = new WebView(this);
        // 针对不同系统版本，动态切换层类型以防止闪烁和花屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0 以上，对于复杂的网页，有时关闭硬件加速层反而能解决花屏 (如果全局加速导致了冲突)
            // 但考虑到夸克有大量动画，先尝试硬件层
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        
        webView.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        // 伪装PC客户端，绕过网盘的风控
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 Safari/537.36 Channel/pckk_other_ch");
        
        // 允许跨域和混合内容，避免样式加载失败
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // 注册JS桥接
        webView.addJavascriptInterface(new WebAppInterface(), "OperitNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 核心三：注入竞品灵魂逻辑
                injectLinkSwiftScript(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        root.addView(navBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        loadDrive("https://pan.quark.cn/");
    }

    private void loadDrive(String url) {
        webView.loadUrl(url);
    }

    private void injectLinkSwiftScript(WebView view, String url) {
        // 从萌娘助手 ceshi.js 移植的注入逻辑
        String js = "javascript:(function() {" +
            "if(document.getElementById('operit-btn-wrap')) return;" +
            "var wrap = document.createElement('div');" +
            "wrap.id = 'operit-btn-wrap';" +
            "wrap.style.cssText = 'position:fixed; bottom:20px; right:20px; z-index:999999; display:flex; flex-direction:column; gap:10px;';" +
            
            "var btn = document.createElement('button');" +
            "btn.innerHTML = '✨ 提取直链下载';" +
            "btn.style.cssText = 'background:#0d53ff; color:white; padding:12px 24px; border-radius:24px; font-size:16px; font-weight:bold; box-shadow: 0 4px 12px rgba(13,83,255,0.4); border:none;';" +
            "btn.onclick = function() {" +
            "   try {" +
            "       window.OperitNative.onExtractBtnClick();" +
            "   } catch(e) { alert(e); }" +
            "};" +
            
            "wrap.appendChild(btn);" +
            "document.body.appendChild(wrap);" +
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

    // 与注入的JS通信的接口
    private class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void onExtractBtnClick() {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "已触发底层原生解析！", Toast.LENGTH_SHORT).show();
                // 下一步将在这里调用 ShareLinkParser
            });
        }
    }
}
