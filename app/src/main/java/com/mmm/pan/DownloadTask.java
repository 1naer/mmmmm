package com.mmm.pan;

import org.json.JSONObject;

public class DownloadTask {
    public String id, name, url, headersJson;
    public long size, done;
    public int status;

    public static DownloadTask fromJson(JSONObject o) {
        DownloadTask t = new DownloadTask();
        t.url = firstNonEmpty(o.optString("url"), o.optString("dlink"), o.optString("link"));
        t.name = firstNonEmpty(o.optString("name"), o.optString("filename"), o.optString("server_filename"), filename(t.url));
        t.id = firstNonEmpty(o.optString("id"), o.optString("fs_id"), "t" + System.currentTimeMillis() + "_" + Math.abs((t.url + t.name).hashCode()));
        JSONObject headers = o.optJSONObject("headers");
        t.headersJson = headers != null ? headers.toString() : "{}";
        t.size = o.optLong("size", o.optLong("length", -1));
        t.done = o.optLong("done", 0);
        t.status = o.optInt("status", 0);
        return t;
    }

    static String filename(String u) {
        try {
            if (u == null || u.length() == 0) return "download.bin";
            String p = new java.net.URL(u).getPath();
            int i = p.lastIndexOf('/');
            String n = i >= 0 ? p.substring(i + 1) : p;
            return n.length() > 0 ? java.net.URLDecoder.decode(n, "UTF-8") : "download.bin";
        } catch (Exception e) {
            return "download.bin";
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values != null) {
            for (String v : values) {
                if (v != null && v.trim().length() > 0) return v;
            }
        }
        return "";
    }
}
