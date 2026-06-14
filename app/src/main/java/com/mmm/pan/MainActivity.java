package com.mmm.pan;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private WebView webView;
    private String currentProvider = "夸克网盘";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Color.parseColor("#F5F7FA"));
        navBar.setPadding(20, 20, 20, 20);

        Button btnQuark = new Button(this);
        btnQuark.setText("夸克网盘");
        btnQuark.setOnClickListener(v -> { currentProvider = "夸克网盘"; loadDrive("https://pan.quark.cn/"); });

        Button btnAli = new Button(this);
        btnAli.setText("阿里云盘");
        btnAli.setOnClickListener(v -> { currentProvider = "阿里云盘"; loadDrive("https://www.alipan.com/"); });

        Button btnBaidu = new Button(this);
        btnBaidu.setText("百度网盘");
        btnBaidu.setOnClickListener(v -> { currentProvider = "百度网盘"; loadDrive("https://pan.baidu.com/"); });

        Button btnDownloads = new Button(this);
        btnDownloads.setText("下载中心");
        btnDownloads.setTextColor(Color.parseColor("#3498DB"));
        btnDownloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));

        navBar.addView(btnQuark, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        navBar.addView(btnAli, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        navBar.addView(btnBaidu, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        navBar.addView(btnDownloads, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 Safari/537.36 Channel/pckk_other_ch");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new WebAppInterface(), "OperitNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
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
            "       var fids = [];" +
            "       if (location.host.includes('quark.cn')) {" +
            "           var fileNodes = document.querySelectorAll('.row.active');" +
            "           if (fileNodes.length === 0) {" +
            "               try {" +
            "                   var reactNode = document.querySelector('.file-list');" +
            "                   var propsKey = Object.keys(reactNode).find(function(k){return k.startsWith('__reactProps');});" +
            "                   var props = reactNode[propsKey];" +
            "                   fids = props.children.props.selectedRowKeys || [];" +
            "               } catch(e) {}" +
            "           }" +
            "       }" +
            "       window.OperitNative.onExtract(fids.join(','));" +
            "   } catch(e) { window.OperitNative.onExtract(''); }" +
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

    private class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void onExtract(String fidsString) {
            runOnUiThread(() -> {
                if (fidsString == null || fidsString.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "未能抓取到选中的文件，请先在页面上勾选！", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Toast.makeText(MainActivity.this, "已抓取文件ID，正在后台提取直链...", Toast.LENGTH_SHORT).show();
                
                String[] split = fidsString.split(",");
                List<String> fids = new ArrayList<>();
                for (String f : split) fids.add(f.trim());

                String currentUrl = webView.getUrl();
                ShareLinkParser.fetchDirectLinkByFidsAsync(MainActivity.this, currentProvider, currentUrl, fids, new ShareLinkParser.Callback() {
                    @Override
                    public void onSuccess(String directUrl, String filename) {
                        runOnUiThread(() -> onParsedSuccessfully(directUrl, filename));
                    }

                    @Override
                    public void onFail(String reason) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show());
                    }
                });
            });
        }
    }

    private void onParsedSuccessfully(String directUrl, String filename) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 成功提取文件");
        builder.setMessage("文件名: " + filename + "\n\n是否立即移交底层下载引擎？");
        builder.setPositiveButton("极速下载", (dialog, which) -> {
            DownloadTask task = new DownloadTask();
            task.id = UUID.randomUUID().toString();
            task.url = directUrl;
            task.name = filename;
            task.status = DownloadTask.STATUS_PAUSED;
            
            String headers = String.format("{\"Cookie\":\"%s\"}", CookieManager.getInstance().getCookie(webView.getUrl()));
            task.headersJson = headers;

            DownloadRepository.update(this, task);
            DownloadService.startTask(this, task.id);
            Toast.makeText(this, "已加入多线程极速下载队列", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
