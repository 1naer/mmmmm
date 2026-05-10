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
import android.util.Log;

import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.task.DownloadTask;

import java.io.File;

public class DownloadService extends Service {
    public static final String ACTION_START = "com.mmm.pan.download.START";
    public static final String ACTION_PAUSE = "com.mmm.pan.download.PAUSE";
    public static final String EXTRA_ID = "id";
    private static final String CHANNEL_ID = "mmm_downloads";
    private static final int NOTIFY_BASE = 4100;

    public static void startTask(Context c, String id) {
        Intent i = new Intent(c, DownloadService.class).setAction(ACTION_START).putExtra(EXTRA_ID, id);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    public static void pauseTask(Context c, String id) {
        Intent i = new Intent(c, DownloadService.class).setAction(ACTION_PAUSE).putExtra(EXTRA_ID, id);
        c.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        // 注册Aria
        Aria.download(this).register();
        // 配置Aria并发为最高（类似于萌娘助手的处理）
        Aria.get(this).getDownloadConfig().setMaxTaskNum(6);
        Aria.get(this).getDownloadConfig().setThreadNum(16); // 16 线程并发分块下载
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        com.mmm.pan.DownloadTask task = DownloadRepository.find(this, id);
        if (task == null) return;

        File out = resolveOutputFile(task);
        task.path = out.getAbsolutePath();
        task.status = com.mmm.pan.DownloadTask.STATUS_RUNNING;
        DownloadRepository.update(this, task);
        
        startForeground(NOTIFY_BASE, buildNotification(task, "准备极速下载", 0, 0, true));

        // 启动 Aria 极速下载引擎
        long taskId = Aria.download(this)
                .load(task.url)
                .setFilePath(task.path)
                .ignoreFilePathOccupy()
                .create();
                
        // 把 Aria 的 taskId 保存到我们的对象中
        task.error = String.valueOf(taskId); // 借用 error 字段存储 Aria Task ID
        DownloadRepository.update(this, task);
    }

    private void pause(String id) {
        com.mmm.pan.DownloadTask t = DownloadRepository.find(this, id);
        if (t != null) {
            t.status = com.mmm.pan.DownloadTask.STATUS_PAUSED;
            DownloadRepository.update(this, t);
            
            try {
                long ariaTaskId = Long.parseLong(t.error);
                Aria.download(this).load(ariaTaskId).stop();
            } catch (Exception ignored) {}
            
            notifyTask(t, "已暂停", t.done, t.size, false);
        }
    }

    // --- Aria Event Callbacks ---
    
    @com.arialyy.annotations.Download.onTaskRunning
    protected void running(DownloadTask task) {
        com.mmm.pan.DownloadTask localTask = findLocalTaskByAriaId(task.getTaskName(), task.getKey());
        if (localTask != null) {
            localTask.done = task.getCurrentProgress();
            localTask.size = task.getFileSize();
            localTask.status = com.mmm.pan.DownloadTask.STATUS_RUNNING;
            DownloadRepository.update(this, localTask);
            notifyTask(localTask, "极速下载中 " + task.getConvertSpeed(), localTask.done, localTask.size, true);
        }
    }

    @com.arialyy.annotations.Download.onTaskComplete
    protected void taskComplete(DownloadTask task) {
        com.mmm.pan.DownloadTask localTask = findLocalTaskByAriaId(task.getTaskName(), task.getKey());
        if (localTask != null) {
            localTask.done = task.getFileSize();
            localTask.size = task.getFileSize();
            localTask.status = com.mmm.pan.DownloadTask.STATUS_DONE;
            DownloadRepository.update(this, localTask);
            notifyTask(localTask, "下载完成", localTask.done, localTask.size, false);
            stopForeground(false);
        }
    }

    @com.arialyy.annotations.Download.onTaskFail
    protected void taskFail(DownloadTask task) {
        com.mmm.pan.DownloadTask localTask = findLocalTaskByAriaId(task.getTaskName(), task.getKey());
        if (localTask != null) {
            localTask.status = com.mmm.pan.DownloadTask.STATUS_FAILED;
            DownloadRepository.update(this, localTask);
            notifyTask(localTask, "下载失败", localTask.done, localTask.size, false);
        }
    }

    private com.mmm.pan.DownloadTask findLocalTaskByAriaId(String path, String url) {
        // 由于需要双向映射，这里简单扫描数据库。商业级应用可在此做更完善的哈希映射
        for (com.mmm.pan.DownloadTask t : DownloadRepository.load(this)) {
            if (t.url != null && t.url.equals(url)) return t;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        Aria.download(this).unRegister();
        super.onDestroy();
    }

    // --- IO Helpers ---

    private File resolveOutputFile(com.mmm.pan.DownloadTask task) {
        File dir;
        if (Build.VERSION.SDK_INT >= 29) {
            dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mmm");
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "mmm");
        }
        if (!dir.exists()) dir.mkdirs();
        if (task.path != null && task.path.length() > 0) return new File(task.path);
        return com.mmm.pan.DownloadTask.uniqueFile(new File(dir, com.mmm.pan.DownloadTask.sanitizeFileName(task.name)));
    }

    private void notifyTask(com.mmm.pan.DownloadTask task, String text, long done, long total, boolean running) {
        Notification n = buildNotification(task, text, done, total, running);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            nm.notify(NOTIFY_BASE + Math.abs(task.id.hashCode() % 1000), n);
        }
    }

    private Notification buildNotification(com.mmm.pan.DownloadTask task, String text, long done, long total, boolean running) {
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
