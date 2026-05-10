package com.mmm.pan;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private WebView webView;
    private String targetDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        targetDrive = getIntent().getStringExtra("DRIVE_TYPE");
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        
        // 监控网页加载完成，如果是夸克/百度网盘的首页，说明登录可能成功，尝试抓取 Cookie
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                checkAndSaveCookie(url);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());

        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        loadDriveLogin();
    }

    private void loadDriveLogin() {
        if ("QUARK".equals(targetDrive)) {
            webView.loadUrl("https://pan.quark.cn/");
        } else if ("BAIDU".equals(targetDrive)) {
            webView.loadUrl("https://pan.baidu.com/disk/main");
        } else {
            Toast.makeText(this, "未知的网盘类型", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkAndSaveCookie(String currentUrl) {
        String cookie = CookieManager.getInstance().getCookie(currentUrl);
        if (cookie != null && cookie.contains("ptoken=")) { // 夸克常用凭证
            saveCookieAndExit("QUARK", cookie);
        } else if (cookie != null && cookie.contains("BDUSS=")) { // 百度常用凭证
            saveCookieAndExit("BAIDU", cookie);
        }
    }

    private void saveCookieAndExit(String driveType, String cookieStr) {
        getSharedPreferences("DriveAuth", MODE_PRIVATE)
                .edit()
                .putString(driveType + "_COOKIE", cookieStr)
                .apply();
        Toast.makeText(this, "✅ 登录凭证获取成功，已开启极速与大文件权限", Toast.LENGTH_LONG).show();
        finish();
    }
}
