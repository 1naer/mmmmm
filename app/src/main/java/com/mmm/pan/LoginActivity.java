package com.mmm.pan;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
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
        if ("夸克网盘".equals(targetDrive)) {
            webView.loadUrl("https://pan.quark.cn/");
        } else if ("百度网盘".equals(targetDrive)) {
            webView.loadUrl("https://pan.baidu.com/disk/main");
        } else if ("阿里云盘".equals(targetDrive)) {
            webView.loadUrl("https://www.aliyundrive.com/drive/");
        } else if ("天翼云盘".equals(targetDrive)) {
            webView.loadUrl("https://cloud.189.cn/web/login.html");
        } else if ("迅雷云盘".equals(targetDrive)) {
            webView.loadUrl("https://pan.xunlei.com/");
        } else if ("123云盘".equals(targetDrive)) {
            webView.loadUrl("https://www.123pan.com/");
        } else if ("115网盘".equals(targetDrive)) {
            webView.loadUrl("https://115.com/");
        } else {
            Toast.makeText(this, "未知的网盘类型", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkAndSaveCookie(String currentUrl) {
        String cookie = CookieManager.getInstance().getCookie(currentUrl);
        if (cookie == null || cookie.isEmpty()) return;

        boolean isLogged = false;
        if ("夸克网盘".equals(targetDrive) && cookie.contains("ptoken=")) isLogged = true;
        else if ("百度网盘".equals(targetDrive) && cookie.contains("BDUSS=")) isLogged = true;
        else if ("阿里云盘".equals(targetDrive) && cookie.contains("alipan.com")) isLogged = true;
        else if ("天翼云盘".equals(targetDrive) && cookie.contains("COOKIE_LOGIN_USER=")) isLogged = true;
        else if ("迅雷云盘".equals(targetDrive) && cookie.contains("userid=")) isLogged = true;
        else if ("123云盘".equals(targetDrive) && cookie.contains("token")) isLogged = true;
        else if ("115网盘".equals(targetDrive) && cookie.contains("UID=")) isLogged = true;

        if (isLogged) {
            getSharedPreferences("DriveAuth", MODE_PRIVATE)
                    .edit()
                    .putString(targetDrive + "_COOKIE", cookie)
                    .apply();
            Toast.makeText(this, "✅ " + targetDrive + " 授权成功！已开启极速与大文件特权", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
