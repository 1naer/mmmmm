package com.mmm.pan;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.webkit.*;
import android.widget.*;

public class BrowserActivity extends Activity {
    WebView web; TextView bar; ScriptInjector injector;
    private int navigationSeq = 0;
    private boolean injectedCurrentNavigation = false;

    public static void open(Context c,String url){ Intent i=new Intent(c,BrowserActivity.class); i.putExtra("url",url); c.startActivity(i); }
    @Override public void onCreate(Bundle b){ super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        bar=new TextView(this); bar.setPadding(20,12,20,12); bar.setText("加载中...");
        web=new WebView(this); root.addView(bar,new LinearLayout.LayoutParams(-1,-2)); root.addView(web,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        injector=new ScriptInjector(this);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setAllowFileAccess(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.addJavascriptInterface(new MmmBridge(this, web), "MMM_NATIVE");
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView v,String url,Bitmap icon){
                bar.setText(url);
                navigationSeq++;
                injectedCurrentNavigation = false;
                injector.beginNavigation(navigationSeq);
            }
            @Override public void onPageCommitVisible(WebView v,String url){
                bar.setText(url);
                injectForCurrentNavigation(v, url);
            }
            @Override public void onPageFinished(WebView v,String url){
                bar.setText(url);
                injectForCurrentNavigation(v, url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r){ return false; }
        });
        web.setWebChromeClient(new WebChromeClient());
        String url=getIntent().getStringExtra("url"); if(url==null) url="https://pan.baidu.com"; web.loadUrl(url);
    }

    private void injectForCurrentNavigation(WebView v, String url){
        if(injectedCurrentNavigation) return;
        injectedCurrentNavigation = true;
        injector.injectAll(v, url, navigationSeq);
    }

    @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }

    @Override protected void onDestroy(){
        if(web!=null){
            web.stopLoading();
            web.removeJavascriptInterface("MMM_NATIVE");
            web.destroy();
            web=null;
        }
        super.onDestroy();
    }
}
