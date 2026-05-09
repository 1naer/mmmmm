package com.mmm.pan;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class NativeHttp {
    public static String syncRequest(String json){
        try{ JSONObject req=new JSONObject(json); String url=req.getString("url"); String method=req.optString("method","GET");
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(30000);
            JSONObject hs=req.optJSONObject("headers"); if(hs!=null){ Iterator<String> it=hs.keys(); while(it.hasNext()){String k=it.next(); c.setRequestProperty(k,hs.optString(k));}}
            int code=c.getResponseCode(); InputStream in=code>=400?c.getErrorStream():c.getInputStream(); String body=read(in);
            JSONObject res=new JSONObject(); res.put("status",code); res.put("responseText",body); return res.toString();
        }catch(Exception e){ try{ return new JSONObject().put("status",0).put("error",e.toString()).toString(); }catch(Exception x){return "{}";} }
    }
    static String read(InputStream in)throws IOException{ if(in==null)return""; ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n; while((n=in.read(b))>0) out.write(b,0,n); return out.toString("UTF-8"); }
}
