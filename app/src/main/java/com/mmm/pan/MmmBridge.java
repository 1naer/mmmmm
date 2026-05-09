package com.mmm.pan;

import android.app.*;
import android.content.*;
import android.webkit.*;
import android.widget.Toast;
import org.json.*;
import java.util.*;

public class MmmBridge {
    private final Activity activity; private final WebView web; private final android.content.SharedPreferences sp;
    public MmmBridge(Activity a, WebView w){ activity=a; web=w; sp=a.getSharedPreferences("mmm_gm",0); }
    @JavascriptInterface public void postMessage(String json){
        try{ JSONObject o=new JSONObject(json); String type=o.optString("type", o.optString("handler"));
            if(type.contains("download") || json.contains("download_links")){ handleDownload(o); }
            else toast("JS: "+type);
        }catch(Exception e){ toast("JS消息: "+json); }
    }
    @JavascriptInterface public String getCookies(String url){ return CookieManager.getInstance().getCookie(url); }
    @JavascriptInterface public void saveValue(String k,String v){ sp.edit().putString(k,v).apply(); }
    @JavascriptInterface public String getValue(String k){ return sp.getString(k,""); }
    @JavascriptInterface public String gmXmlHttpRequest(String json){ return NativeHttp.syncRequest(json); }
    @JavascriptInterface public void gmXmlHttpRequestAsync(final String callbackId, final String json){
        NativeHttp.asyncRequest(json, new NativeHttp.Callback(){
            @Override public void onComplete(final String result){
                activity.runOnUiThread(new Runnable(){
                    @Override public void run(){
                        String js = "window.__mmmGMXmlHttpRequestCallback&&window.__mmmGMXmlHttpRequestCallback("
                                + JSONObject.quote(callbackId == null ? "" : callbackId) + ","
                                + JSONObject.quote(result == null ? "{}" : result) + " );";
                        web.evaluateJavascript(js, null);
                    }
                });
            }
        });
    }
    private void handleDownload(JSONObject o) throws Exception{
        JSONArray arr = null; JSONObject p=o.optJSONObject("payload"); if(p!=null) arr=p.optJSONArray("files");
        if(arr==null) arr=o.optJSONArray("data");
        if(arr==null){ String u=o.optString("url"); if(u.length()>0){ arr=new JSONArray(); JSONObject f=new JSONObject(); f.put("url",u); f.put("name",o.optString("name","download.bin")); arr.put(f);} }
        if(arr==null || arr.length()==0){ toast("未收到下载链接"); return; }
        DownloadRepository.addFromJson(activity, arr.toString());
        toast("已加入下载任务: "+arr.length());
        activity.startActivity(new Intent(activity, DownloadActivity.class));
    }
    private void toast(String s){ activity.runOnUiThread(()-> Toast.makeText(activity,s,Toast.LENGTH_SHORT).show()); }
}
