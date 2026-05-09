package com.mmm.pan;

import org.json.JSONObject;

import java.io.File;

public class DownloadTask {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_DONE = 3;
    public static final int STATUS_FAILED = 4;

    public String id, name, url, headersJson, path, error;
    public long size, done, createdAt, updatedAt;
    public int status, retryCount;

    public static DownloadTask fromJson(JSONObject o) {
        DownloadTask t = new DownloadTask();
        t.url = firstNonEmpty(o.optString("url"), o.optString("dlink"), o.optString("link"));
        t.name = sanitizeFileName(firstNonEmpty(o.optString("name"), o.optString("filename"), o.optString("server_filename"), filename(t.url)));
        t.id = firstNonEmpty(o.optString("id"), o.optString("fs_id"), stableId(t.url, t.name));
        JSONObject headers = o.optJSONObject("headers");
        t.headersJson = headers != null ? headers.toString() : o.optString("headers", "{}");
        if (t.headersJson == null || t.headersJson.trim().length() == 0) t.headersJson = "{}";
        t.size = o.optLong("size", o.optLong("length", -1));
        t.done = o.optLong("done", 0);
        t.status = o.optInt("status", STATUS_PENDING);
        t.path = o.optString("path", "");
        t.error = o.optString("error", "");
        t.retryCount = o.optInt("retryCount", 0);
        long now = System.currentTimeMillis();
        t.createdAt = o.optLong("createdAt", now);
        t.updatedAt = o.optLong("updatedAt", now);
        return t;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("url", url);
        o.put("headers", new JSONObject(headersJson == null || headersJson.length() == 0 ? "{}" : headersJson));
        o.put("size", size);
        o.put("done", done);
        o.put("status", status);
        o.put("path", path == null ? "" : path);
        o.put("error", error == null ? "" : error);
        o.put("retryCount", retryCount);
        o.put("createdAt", createdAt);
        o.put("updatedAt", updatedAt);
        return o;
    }

    public String statusText() {
        switch (status) {
            case STATUS_RUNNING: return "下载中";
            case STATUS_PAUSED: return "已暂停";
            case STATUS_DONE: return "已完成";
            case STATUS_FAILED: return "失败";
            default: return "等待中";
        }
    }

    public static String filename(String u) {
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

    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().length() == 0) return "download.bin";
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return clean.length() == 0 ? "download.bin" : clean;
    }

    public static File uniqueFile(File f) {
        if (!f.exists()) return f;
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        String base = dot > 0 ? n.substring(0, dot) : n;
        String ext = dot > 0 ? n.substring(dot) : "";
        File dir = f.getParentFile();
        for (int i = 1; i < 10000; i++) {
            File x = new File(dir, base + " (" + i + ")" + ext);
            if (!x.exists()) return x;
        }
        return f;
    }

    private static String stableId(String url, String name) {
        return "t" + System.currentTimeMillis() + "_" + Math.abs(((url == null ? "" : url) + "|" + (name == null ? "" : name)).hashCode());
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
