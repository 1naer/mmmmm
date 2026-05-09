package com.mmm.pan;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RangeDownloader {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long MULTI_PART_THRESHOLD = 8L * 1024L * 1024L;
    private static final int MAX_PARTS = 4;

    public interface Controller {
        boolean isCancelled();
        void onProgress(long done, long total);
    }

    public static void download(DownloadTask task, File out, Controller controller) throws Exception {
        if (task == null || task.url == null || task.url.trim().length() == 0) {
            throw new IllegalArgumentException("missing download url");
        }
        JSONObject headers = new JSONObject(task.headersJson == null || task.headersJson.length() == 0 ? "{}" : task.headersJson);
        Probe probe = probe(task.url, headers);
        long len = probe.length > 0 ? probe.length : task.size;
        boolean rangeSupported = probe.rangeSupported && len > 0;
        task.size = len;

        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("cannot create output dir: " + parent);
        }

        if (!rangeSupported) {
            singlePartResume(task.url, out, headers, len, controller);
        } else {
            int parts = len > MULTI_PART_THRESHOLD ? MAX_PARTS : 1;
            multiPartResume(task.url, out, headers, len, parts, controller);
        }

        if (len > 0 && out.length() != len) {
            throw new java.io.IOException("file size mismatch: " + out.length() + "/" + len);
        }
        cleanupMeta(out);
        controller.onProgress(out.length(), len);
    }

    private static void multiPartResume(String url, File out, JSONObject headers, long len, int parts, Controller controller) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.setLength(len);
        raf.close();

        AtomicLong done = new AtomicLong(sumDone(out, len, parts));
        controller.onProgress(done.get(), len);
        AtomicReference<Exception> firstError = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            long start = len * i / parts;
            long end = i == parts - 1 ? len - 1 : (len * (i + 1) / parts - 1);
            int index = i;
            Thread t = new Thread(() -> {
                try {
                    part(url, out, index, start, end, headers, done, len, controller);
                } catch (Exception e) {
                    firstError.compareAndSet(null, e);
                }
            }, "mmm-range-" + i);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();
        if (firstError.get() != null) throw firstError.get();
        if (controller.isCancelled()) throw new CancelledException();
    }

    private static void singlePartResume(String url, File out, JSONObject headers, long len, Controller controller) throws Exception {
        long existing = out.exists() ? out.length() : 0;
        boolean tryResume = existing > 0;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        apply(c, headers);
        if (tryResume) c.setRequestProperty("Range", "bytes=" + existing + "-");
        int code = c.getResponseCode();
        if (code >= 400) throw new java.io.IOException("HTTP " + code);
        boolean append = tryResume && code == HttpURLConnection.HTTP_PARTIAL;
        if (tryResume && !append) existing = 0;
        InputStream in = c.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        if (append) raf.seek(existing); else raf.setLength(0);
        byte[] b = new byte[BUFFER_SIZE];
        int n;
        long done = existing;
        controller.onProgress(done, len);
        while ((n = in.read(b)) > 0) {
            if (controller.isCancelled()) throw new CancelledException();
            raf.write(b, 0, n);
            done += n;
            controller.onProgress(done, len);
        }
        raf.close();
        in.close();
        c.disconnect();
    }

    private static void part(String url, File out, int index, long s, long e, JSONObject headers, AtomicLong totalDone, long total, Controller controller) throws Exception {
        File meta = metaFile(out, index);
        long doneInPart = readLong(meta);
        long from = s + doneInPart;
        if (from > e) return;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        apply(c, headers);
        c.setRequestProperty("Range", "bytes=" + from + "-" + e);
        int code = c.getResponseCode();
        if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
            throw new java.io.IOException("HTTP " + code + " for range " + from + "-" + e);
        }
        if (code == HttpURLConnection.HTTP_OK && from > 0) {
            throw new java.io.IOException("server ignored range request " + from + "-" + e);
        }
        InputStream in = c.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.seek(code == HttpURLConnection.HTTP_PARTIAL ? from : 0);
        byte[] b = new byte[BUFFER_SIZE];
        int n;
        long localDone = doneInPart;
        while ((n = in.read(b)) > 0) {
            if (controller.isCancelled()) throw new CancelledException();
            raf.write(b, 0, n);
            localDone += n;
            writeLong(meta, localDone);
            controller.onProgress(totalDone.addAndGet(n), total);
        }
        raf.close();
        in.close();
        c.disconnect();
    }

    public static Probe probe(String url, JSONObject headers) throws Exception {
        long len = -1;
        boolean rangeSupported = false;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("HEAD");
            apply(c, headers);
            int code = c.getResponseCode();
            len = c.getContentLengthLong();
            String ranges = c.getHeaderField("Accept-Ranges");
            rangeSupported = ranges != null && ranges.toLowerCase().contains("bytes");
            c.disconnect();
            if (code >= 400) len = -1;
        } catch (Exception ignored) {}

        if (len <= 0 || !rangeSupported) {
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
                if (k != null && k.trim().length() > 0) c.setRequestProperty(k, h.optString(k));
            }
        }
    }

    private static long sumDone(File out, long len, int parts) {
        long sum = 0;
        for (int i = 0; i < parts; i++) {
            long start = len * i / parts;
            long end = i == parts - 1 ? len - 1 : (len * (i + 1) / parts - 1);
            long max = end - start + 1;
            sum += Math.min(max, Math.max(0, readLong(metaFile(out, i))));
        }
        return sum;
    }

    private static File metaFile(File out, int index) { return new File(out.getAbsolutePath() + ".part" + index); }
    private static long readLong(File f) {
        try {
            if (!f.exists()) return 0;
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            long v = raf.length() >= 8 ? raf.readLong() : 0;
            raf.close();
            return v;
        } catch (Exception e) { return 0; }
    }
    private static void writeLong(File f, long v) {
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            raf.setLength(0);
            raf.writeLong(v);
            raf.close();
        } catch (Exception ignored) {}
    }
    private static void cleanupMeta(File out) {
        File dir = out.getParentFile();
        if (dir == null) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        String prefix = out.getName() + ".part";
        for (File f : files) if (f.getName().startsWith(prefix)) f.delete();
    }

    public static class Probe {
        public final long length;
        public final boolean rangeSupported;
        Probe(long length, boolean rangeSupported) {
            this.length = length;
            this.rangeSupported = rangeSupported;
        }
    }

    public static class CancelledException extends java.io.IOException {
        public CancelledException() { super("cancelled"); }
    }
}