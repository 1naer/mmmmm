package com.mmm.pan;

import android.content.*;
import android.webkit.WebView;
import java.io.*;

public class ScriptInjector {
    private final Context ctx; public ScriptInjector(Context c){ctx=c;}

    public void injectAll(WebView web,String url){
        if (web == null) return;
        String safeUrl = url == null ? "" : url;
        evalAsset(web,"scripts/gm_shim.js");
        evalAsset(web,"scripts/generic_link_capture.js");
        // TODO: 根据域名注入专用脚本：baidu、aliyun、quark、xunlei、tianyi...
        if(safeUrl.contains("pan.baidu.com")||safeUrl.contains("yun.baidu.com")) evalAsset(web,"scripts/baidu_stub.js");
    }

    private void evalAsset(WebView web,String path){ try{ String js=read(path); web.evaluateJavascript(js,null);}catch(Exception ignored){} }
    private String read(String path) throws IOException { InputStream in=ctx.getAssets().open(path); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); return out.toString("UTF-8"); }
}
