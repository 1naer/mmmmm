package com.mmm.pan;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadService extends Service {
    public static final String ACTION_START = "com.mmm.pan.download.START";
    public static final String ACTION_PAUSE = "com.mmm.pan.download.PAUSE";
    public static final String EXTRA_ID = "id";
    private static final String CHANNEL_ID = "mmm_downloads";
    private static final int NOTIFY_BASE = 4100;
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();

    public static void startTask(Context c, String id) {
        Intent i = new Intent(c, DownloadService.class).setAction(ACTION_START).putExtra(EXTRA_ID, id);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    public static void pauseTask(Context c, String id) {
        Intent i = new Intent(c, DownloadService.class).setAction(ACTION_PAUSE).putExtra(EXTRA_ID, id);
        c.startService(i);
    }

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        String id = intent == null ? null : intent.getStringExtra(EXTRA_ID);
        if (ACTION_PAUSE.equals(action)) {
            pause(id);
        } else if (id != null) {
            start(id);
        }
        return START_STICKY;
    }

    private void start(String id) {
        if (workers.containsKey(id)) return;
        DownloadTask task = DownloadRepository.find(this, id);
        if (task == null) return;
        Worker w = new Worker(task);
        workers.put(id, w);
        startForeground(NOTIFY_BASE, buildNotification(task, "引擎启动中...", 0, 0, true));
        new Thread(w, "mmm-download-" + id).start();
    }

    private void pause(String id) {
        Worker w = workers.get(id);
        if (w != null) w.cancelled = true;
        DownloadRepository.updateStatus(this, id, DownloadTask.STATUS_PAUSED, "");
        DownloadTask t = DownloadRepository.find(this, id);
        if (t != null) notifyTask(t, "已暂停", t.done, t.size, false);
    }

    private class Worker implements Runnable, RangeDownloader.Controller {
        final DownloadTask task;
        volatile boolean cancelled;
        long lastNotify;
        Worker(DownloadTask task) { this.task = task; }

        @Override public void run() {
            try {
                task.status = DownloadTask.STATUS_RUNNING;
                task.error = "";
                File out = resolveOutputFile(task);
                task.path = out.getAbsolutePath();
                DownloadRepository.update(DownloadService.this, task);
                notifyTask(task, "多线程极速下载中", task.done, task.size, true);
                
                // 调用我们全新编写的高性能原生并发下载引擎
                RangeDownloader.download(task, out, this);
                
                if (cancelled) throw new RangeDownloader.CancelledException();
                task.done = out.length();
                task.status = DownloadTask.STATUS_DONE;
                task.error = "";
                DownloadRepository.update(DownloadService.this, task);
                notifyTask(task, "下载完成", task.done, task.size, false);
            } catch (RangeDownloader.CancelledException e) {
                task.status = DownloadTask.STATUS_PAUSED;
                task.error = "";
                DownloadRepository.update(DownloadService.this, task);
                notifyTask(task, "已暂停", task.done, task.size, false);
            } catch (Exception e) {
                task.status = DownloadTask.STATUS_FAILED;
                task.error = e.getMessage() == null ? e.toString() : e.getMessage();
                task.retryCount++;
                DownloadRepository.update(DownloadService.this, task);
                notifyTask(task, "下载失败: " + task.error, task.done, task.size, false);
            } finally {
                workers.remove(task.id);
                if (workers.isEmpty()) stopForeground(false);
            }
        }

        @Override public boolean isCancelled() { return cancelled; }

        @Override public void onProgress(long done, long total) {
            task.done = done;
            if (total > 0) task.size = total;
            long now = System.currentTimeMillis();
            if (now - lastNotify > 800 || done == total) {
                lastNotify = now;
                DownloadRepository.update(DownloadService.this, task);
                notifyTask(task, "多线程极速下载中", done, task.size, true);
            }
        }
    }

    private File resolveOutputFile(DownloadTask task) {
        File dir;
        if (Build.VERSION.SDK_INT >= 29) {
            dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mmm");
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mmm");
        }
        if (!dir.exists()) dir.mkdirs();
        if (task.path != null && task.path.length() > 0) return new File(task.path);
        return DownloadTask.uniqueFile(new File(dir, DownloadTask.sanitizeFileName(task.name)));
    }

    private void notifyTask(DownloadTask task, String text, long done, long total, boolean running) {
        Notification n = buildNotification(task, text, done, total, running);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            nm.notify(NOTIFY_BASE + Math.abs(task.id.hashCode() % 1000), n);
        }
    }

    private Notification buildNotification(DownloadTask task, String text, long done, long total, boolean running) {
        Intent open = new Intent(this, DownloadActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open, pendingFlags());
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setContentTitle(task.name == null ? "下载任务" : task.name)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(openPi)
                .setOngoing(running);
        if (total > 0) b.setProgress(100, (int)Math.min(100, done * 100 / total), false); else b.setProgress(0, 0, running);
        if (running) {
            Intent pause = new Intent(this, DownloadService.class).setAction(ACTION_PAUSE).putExtra(EXTRA_ID, task.id);
            PendingIntent pausePi = PendingIntent.getService(this, task.id.hashCode(), pause, pendingFlags());
            b.addAction(android.R.drawable.ic_media_pause, "暂停", pausePi);
        }
        return b.build();
    }

    private int pendingFlags() {
        return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "下载任务", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
