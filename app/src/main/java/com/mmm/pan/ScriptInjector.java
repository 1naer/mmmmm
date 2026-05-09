package com.mmm.pan;

import android.content.*;
import android.webkit.WebView;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ScriptInjector {
    private final Context ctx; public ScriptInjector(Context c){ctx=c;}
    private int currentNavigationId = -1;
    private final Set<String> injectedKeys = new HashSet<>();

    public void beginNavigation(int navigationId){
        if(currentNavigationId == navigationId) return;
        currentNavigationId = navigationId;
        injectedKeys.clear();
    }

    public void injectAll(WebView web,String url){
        injectAll(web, url, currentNavigationId);
    }

    public void injectAll(WebView web,String url,int navigationId){
        if (web == null) return;
        beginNavigation(navigationId);
        String safeUrl = url == null ? "" : url;
        injectAssetOnce(web,"scripts/gm_shim.js");
        injectAssetOnce(web,"scripts/generic_link_capture.js");
        // TODO: 根据域名注入专用脚本：baidu、aliyun、quark、xunlei、tianyi...
        if(safeUrl.contains("pan.baidu.com")||safeUrl.contains("yun.baidu.com")) injectAssetOnce(web,"scripts/baidu_stub.js");
        if(safeUrl.contains("quark.cn")||safeUrl.contains("uc.cn")) injectAssetOnce(web,"scripts/quark_stub.js");
    }

    private void injectAssetOnce(WebView web,String path){
        String key = currentNavigationId + ":" + path;
        if(injectedKeys.contains(key)) return;
        injectedKeys.add(key);
        evalAsset(web,path);
    }

    private void evalAsset(WebView web,String path){ try{ String js=read(path); web.evaluateJavascript(js,null);}catch(Exception ignored){} }
    private String read(String path) throws IOException { InputStream in=ctx.getAssets().open(path); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); return out.toString("UTF-8"); }
}
