package com.mmm.pan;

import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class NativeHttp {
    public static String syncRequest(String json){
        HttpURLConnection c = null;
        try{
            JSONObject req = new JSONObject(json == null ? "{}" : json);
            String url = req.optString("url", "").trim();
            if(url.length() == 0){
                return errorResponse("Missing request url");
            }

            String method = req.optString("method", "GET").trim().toUpperCase(Locale.US);
            if(method.length() == 0) method = "GET";

            c = (HttpURLConnection)new URL(url).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setInstanceFollowRedirects(true);

            JSONObject hs = req.optJSONObject("headers");
            if(hs != null){
                Iterator<String> it = hs.keys();
                while(it.hasNext()){
                    String k = it.next();
                    String v = hs.optString(k, null);
                    if(k != null && k.length() > 0 && v != null){
                        c.setRequestProperty(k, v);
                    }
                }
            }

            String data = req.optString("data", null);
            if(data != null && data.length() > 0 && !"GET".equals(method) && !"HEAD".equals(method)){
                c.setDoOutput(true);
                byte[] bytes = data.getBytes("UTF-8");
                if(c.getRequestProperty("Content-Type") == null){
                    c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                }
                c.setFixedLengthStreamingMode(bytes.length);
                OutputStream out = null;
                try{
                    out = c.getOutputStream();
                    out.write(bytes);
                    out.flush();
                }finally{
                    closeQuietly(out);
                }
            }

            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            String body = read(in);

            JSONObject res = new JSONObject();
            res.put("status", code);
            res.put("statusText", c.getResponseMessage() == null ? "" : c.getResponseMessage());
            res.put("readyState", 4);
            res.put("finalUrl", c.getURL() == null ? url : c.getURL().toString());
            res.put("responseText", body);
            res.put("responseHeaders", flattenHeaders(c.getHeaderFields()));
            return res.toString();
        }catch(Exception e){
            return errorResponse(e.toString());
        }finally{
            if(c != null) c.disconnect();
        }
    }

    private static String errorResponse(String error){
        try{
            JSONObject res = new JSONObject();
            res.put("status", 0);
            res.put("statusText", "");
            res.put("readyState", 4);
            res.put("responseText", "");
            res.put("responseHeaders", "");
            res.put("error", error == null ? "Unknown error" : error);
            return res.toString();
        }catch(Exception x){
            return "{\"status\":0,\"error\":\"Unknown error\"}";
        }
    }

    private static String flattenHeaders(Map<String, List<String>> fields){
        if(fields == null) return "";
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, List<String>> e : fields.entrySet()){
            String key = e.getKey();
            if(key == null) continue;
            List<String> values = e.getValue();
            if(values == null || values.isEmpty()) continue;
            sb.append(key).append(": " ).append(join(values, ", " )).append("\r\n");
        }
        return sb.toString();
    }

    private static String join(List<String> values, String sep){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < values.size(); i++){
            if(i > 0) sb.append(sep);
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    static String read(InputStream in)throws IOException{
        if(in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        try{
            while((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toString("UTF-8");
        }finally{
            closeQuietly(in);
        }
    }

    private static void closeQuietly(Closeable c){
        if(c == null) return;
        try{ c.close(); }catch(Exception ignored){}
    }
}
