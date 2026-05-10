package com.mmm.pan;

import android.util.Log;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RangeDownloader {
    public interface Controller {
        boolean isCancelled();
        void onProgress(long done, long total);
    }

    public static class CancelledException extends Exception {}

    private static final int THREAD_COUNT = 8; // 极速 8 线程并发

    public static void download(DownloadTask task, File out, Controller ctrl) throws Exception {
        URL url = new URL(task.url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setConnectTimeout(10000);
        
        // 注入包含 Cookie 的授权头
        injectHeaders(conn, task.headersJson);

        long totalSize = conn.getContentLength();
        conn.disconnect();

        if (totalSize <= 0) {
            // 不支持获取大小或分片，退回单线程下载
            singleThreadDownload(task, out, ctrl);
            return;
        }

        // 预分配磁盘空间
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.setLength(totalSize);
        raf.close();

        AtomicLong downloadedBytes = new AtomicLong(0);
        long blockSize = totalSize / THREAD_COUNT;

        ExecutorService executor = Executors.newFixedFixedThreadPool(THREAD_COUNT);
        DownloadExceptionHolder errorHolder = new DownloadExceptionHolder();

        for (int i = 0; i < THREAD_COUNT; i++) {
            long start = i * blockSize;
            long end = (i == THREAD_COUNT - 1) ? totalSize - 1 : start + blockSize - 1;
            executor.submit(() -> {
                try {
                    downloadBlock(task, out, start, end, downloadedBytes, totalSize, ctrl, errorHolder);
                } catch (Exception e) {
                    errorHolder.exception = e;
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            if (ctrl.isCancelled()) {
                executor.shutdownNow();
                throw new CancelledException();
            }
            if (errorHolder.exception != null) {
                executor.shutdownNow();
                throw errorHolder.exception;
            }
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
        
        if (errorHolder.exception != null) {
            throw errorHolder.exception;
        }
    }

    private static void downloadBlock(DownloadTask task, File out, long start, long end, 
                                    AtomicLong downloadedBytes, long totalSize, 
                                    Controller ctrl, DownloadExceptionHolder errorHolder) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(task.url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
        injectHeaders(conn, task.headersJson);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Server does not support range requests. Code: " + responseCode);
        }

        InputStream is = conn.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        raf.seek(start);

        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            if (ctrl.isCancelled() || errorHolder.exception != null) {
                break;
            }
            raf.write(buffer, 0, read);
            long currentDone = downloadedBytes.addAndGet(read);
            ctrl.onProgress(currentDone, totalSize);
        }

        raf.close();
        is.close();
        conn.disconnect();
    }

    private static void singleThreadDownload(DownloadTask task, File out, Controller ctrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(task.url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        injectHeaders(conn, task.headersJson);

        long total = conn.getContentLength();
        InputStream is = conn.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(out, "rw");
        
        byte[] buffer = new byte[8192];
        int read;
        long done = 0;
        
        while ((read = is.read(buffer)) != -1) {
            if (ctrl.isCancelled()) {
                raf.close();
                is.close();
                conn.disconnect();
                throw new CancelledException();
            }
            raf.write(buffer, 0, read);
            done += read;
            ctrl.onProgress(done, total);
        }

        raf.close();
        is.close();
        conn.disconnect();
    }

    private static void injectHeaders(HttpURLConnection conn, String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(headersJson);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                conn.setRequestProperty(key, obj.getString(key));
            }
        } catch (Exception e) {
            Log.e("RangeDownloader", "Failed to parse headers: " + e.getMessage());
        }
    }

    private static class DownloadExceptionHolder {
        volatile Exception exception = null;
    }
}
