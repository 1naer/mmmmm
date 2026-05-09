package com.mmm.pan;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class DownloadActivity extends Activity {
    LinearLayout list;
    Handler handler = new Handler(Looper.getMainLooper());
    final Runnable ticker = new Runnable() { @Override public void run() { render(); handler.postDelayed(this, 1200); } };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(24, 36, 24, 24);
        scroll.addView(list);
        setContentView(scroll);
        render();
    }

    @Override protected void onResume() { super.onResume(); handler.postDelayed(ticker, 1200); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }

    void render() {
        list.removeAllViews();
        TextView title = new TextView(this);
        title.setText("下载任务");
        title.setTextSize(24);
        list.addView(title);

        Button refresh = new Button(this);
        refresh.setText("刷新");
        refresh.setAllCaps(false);
        refresh.setOnClickListener(v -> render());
        list.addView(refresh);

        List<DownloadTask> tasks = DownloadRepository.load(this);
        if (tasks.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("暂无任务。先到首页打开网盘链接解析。");
            list.addView(e);
            return;
        }
        for (DownloadTask t : tasks) addTaskView(t);
    }

    void addTaskView(DownloadTask t) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 18, 0, 18);

        TextView name = new TextView(this);
        name.setText(t.name + sizeText(t.size));
        name.setTextSize(17);
        box.addView(name);

        TextView state = new TextView(this);
        state.setText(t.statusText() + "  " + progressText(t.done, t.size) + (t.error == null || t.error.length() == 0 ? "" : "\n错误: " + t.error));
        box.addView(state);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(1000);
        pb.setProgress(t.size > 0 ? (int)Math.min(1000, t.done * 1000 / t.size) : 0);
        box.addView(pb);

        TextView path = new TextView(this);
        path.setText((t.path == null || t.path.length() == 0 ? t.url : t.path));
        path.setTextSize(12);
        box.addView(path);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button start = new Button(this);
        start.setAllCaps(false);
        start.setText(t.status == DownloadTask.STATUS_FAILED ? "重试" : (t.status == DownloadTask.STATUS_PAUSED ? "继续" : "开始"));
        start.setEnabled(t.status != DownloadTask.STATUS_RUNNING && t.status != DownloadTask.STATUS_DONE);
        start.setOnClickListener(v -> { DownloadService.startTask(this, t.id); render(); });
        row.addView(start, new LinearLayout.LayoutParams(0, -2, 1));

        Button pause = new Button(this);
        pause.setAllCaps(false);
        pause.setText("暂停");
        pause.setEnabled(t.status == DownloadTask.STATUS_RUNNING);
        pause.setOnClickListener(v -> { DownloadService.pauseTask(this, t.id); render(); });
        row.addView(pause, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(row);

        View line = new View(this);
        line.setBackgroundColor(0xffdddddd);
        box.addView(line, new LinearLayout.LayoutParams(-1, 1));
        list.addView(box);
    }

    String progressText(long done, long size) {
        if (size <= 0) return readable(done) + " / 未知";
        return readable(done) + " / " + readable(size) + "  " + String.format(Locale.US, "%d%%", Math.min(100, done * 100 / size));
    }

    String sizeText(long size) { return size <= 0 ? "" : "  (" + readable(size) + ")"; }

    String readable(long size) {
        if (size < 0) return "未知";
        double v = size;
        String[] u = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(Locale.US, "%.1f%s", v, u[i]);
    }
}