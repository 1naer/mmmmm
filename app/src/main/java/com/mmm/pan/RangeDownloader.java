package com.mmm.pan;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RangeDownloader {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long MULTI_PART_THRESHOLD = 8L * 1024L * 1024L;

    public static void start(Context ctx, JSONObject task) {
        Toast.makeText(ctx, "开始下载: " + task.optString("name"), Toast.LENGTH_SHORT).show();
        new Thread(() -> download(ctx.getApplicationContext(), task)).start();
    }

    static void download(Context ctx, JSONObject task) {
        try {
            String url = task.optString("url", task.optString("dlink"));
            if (url == null || url.trim().length() == 0) {
                throw new IllegalArgumentException("missing download url");
            }

            String name = sanitizeFileName(task.optString("name", DownloadTask.filename(url)));
            JSONObject headers = task.optJSONObject("headers");
            File base = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (base == null) base = ctx.getFilesDir();
            File dir = new File(base, "mmm");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("cannot create output dir: " + dir);
            }
            File out = uniqueFile(new File(dir, name));

            Probe probe = probe(url, headers);
            long len = probe.length;
            boolean rangeSupported = probe.rangeSupported;

            if (len <= 0 || !rangeSupported) {
                singlePart(url, out, headers);
            } else {
                int parts = len > MULTI_PART_THRESHOLD ? 4 : 1;
                multiPart(url, out, headers, len, parts);
            }

            mainToast(ctx, "下载完成: " + out.getAbsolutePath());
        } catch (Exception e) {
            mainToast(ctx, "下载失败: " + e.getMessage());
        }
    }

    private static void multiPart(String url, File out, JSONObject headers, long len, int parts) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.setLength(len);
        raf.close();

        AtomicReference<Exception> firstError = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            long start = len * i / parts;
            long end = i == parts - 1 ? len - 1 : (len * (i + 1) / parts - 1);
            Thread t = new Thread(() -> {
                try {
                    part(url, out, start, end, headers);
                } catch (Exception e) {
                    firstError.compareAndSet(null, e);
                }
            }, "mmm-range-" + i);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        if (firstError.get() != null) {
            throw firstError.get();
        }
    }

    private static void singlePart(String url, File out, JSONObject headers) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        apply(c, headers);
        int code = c.getResponseCode();
        if (code >= 400) {
            throw new java.io.IOException("HTTP " + code);
        }
        InputStream in = c.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.setLength(0);
        byte[] b = new byte[BUFFER_SIZE];
        int n;
        while ((n = in.read(b)) > 0) {
            raf.write(b, 0, n);
        }
        raf.close();
        in.close();
        c.disconnect();
    }

    static void part(String url, File out, long s, long e, JSONObject headers) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        apply(c, headers);
        c.setRequestProperty("Range", "bytes=" + s + "-" + e);
        int code = c.getResponseCode();
        if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
            throw new java.io.IOException("HTTP " + code + " for range " + s + "-" + e);
        }
        if (code == HttpURLConnection.HTTP_OK && s > 0) {
            throw new java.io.IOException("server ignored range request " + s + "-" + e);
        }
        InputStream in = c.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.seek(code == HttpURLConnection.HTTP_PARTIAL ? s : 0);
        byte[] b = new byte[BUFFER_SIZE];
        int n;
        while ((n = in.read(b)) > 0) {
            raf.write(b, 0, n);
        }
        raf.close();
        in.close();
        c.disconnect();
    }

    private static Probe probe(String url, JSONObject headers) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("HEAD");
        apply(c, headers);
        int code = c.getResponseCode();
        long len = c.getContentLengthLong();
        String ranges = c.getHeaderField("Accept-Ranges");
        boolean rangeSupported = ranges != null && ranges.toLowerCase().contains("bytes");
        c.disconnect();

        if (code >= 400 || len <= 0 || !rangeSupported) {
            HttpURLConnection r = (HttpURLConnection) new URL(url).openConnection();
            apply(r, headers);
            r.setRequestProperty("Range", "bytes=0-0");
            int rc = r.getResponseCode();
            String cr = r.getHeaderField("Content-Range");
            if (rc == HttpURLConnection.HTTP_PARTIAL && cr != null) {
                int slash = cr.lastIndexOf('/');
                if (slash >= 0) {
                    try { len = Long.parseLong(cr.substring(slash + 1).trim()); } catch (Exception ignored) {}
                }
                rangeSupported = true;
            }
            r.disconnect();
        }
        return new Probe(len, rangeSupported);
    }

    static void apply(HttpURLConnection c, JSONObject h) throws Exception {
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setInstanceFollowRedirects(true);
        if (h != null) {
            Iterator<String> it = h.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (k != null && k.trim().length() > 0) {
                    c.setRequestProperty(k, h.optString(k));
                }
            }
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.trim().length() == 0) return "download.bin";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static File uniqueFile(File f) {
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

    private static void mainToast(Context ctx, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }

    private static class Probe {
        final long length;
        final boolean rangeSupported;
        Probe(long length, boolean rangeSupported) {
            this.length = length;
            this.rangeSupported = rangeSupported;
        }
    }
}
