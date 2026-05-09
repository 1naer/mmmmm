package com.mmm.pan;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
    final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 1200);
        }
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#F5F7FA"));
        scroll.setFillViewport(true);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40, 60, 40, 60);
        scroll.addView(list);
        setContentView(scroll);
        
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(ticker, 1200);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    void render() {
        list.removeAllViews();
        
        // Header Layout
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 40);

        TextView title = new TextView(this);
        title.setText("下载任务");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#2C3E50"));
        title.getPaint().setFakeBoldText(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        header.addView(title, titleParams);

        Button refresh = createSmallButton("刷新");
        refresh.setOnClickListener(v -> render());
        header.addView(refresh);

        list.addView(header);

        List<DownloadTask> tasks = DownloadRepository.load(this);
        if (tasks.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("暂无任务。请先到首页解析网盘链接并开始下载。");
            e.setTextColor(Color.parseColor("#7F8C8D"));
            e.setTextSize(16);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, 100, 0, 0);
            list.addView(e);
            return;
        }
        
        for (DownloadTask t : tasks) {
            addTaskView(t);
        }
    }

    void addTaskView(DownloadTask t) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createCardBackground());
        card.setElevation(8f);
        card.setPadding(40, 40, 40, 40);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 30);

        // Name
        TextView name = new TextView(this);
        name.setText(t.name + sizeText(t.size));
        name.setTextSize(16);
        name.setTextColor(Color.parseColor("#34495E"));
        name.getPaint().setFakeBoldText(true);
        card.addView(name);

        // Status & Progress Text
        TextView state = new TextView(this);
        String errStr = (!TextUtils.isEmpty(t.error)) ? "\n错误: " + t.error : "";
        state.setText(t.statusText() + "  " + progressText(t.done, t.size) + errStr);
        state.setTextColor(Color.parseColor("#7F8C8D"));
        state.setTextSize(13);
        state.setPadding(0, 10, 0, 20);
        card.addView(state);

        // ProgressBar
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(1000);
        pb.setProgress(t.size > 0 ? (int) Math.min(1000, t.done * 1000 / t.size) : 0);
        pb.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#3498DB")));
        card.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));

        // Path / URL
        TextView path = new TextView(this);
        path.setText((TextUtils.isEmpty(t.path)) ? t.url : t.path);
        path.setTextSize(11);
        path.setTextColor(Color.parseColor("#BDC3C7"));
        path.setSingleLine(true);
        path.setEllipsize(TextUtils.TruncateAt.END);
        path.setPadding(0, 20, 0, 30);
        card.addView(path);

        // Actions Row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        
        Button start = createActionBtn(t.status == DownloadTask.STATUS_FAILED ? "重试" : (t.status == DownloadTask.STATUS_PAUSED ? "继续" : "开始"), "#3498DB");
        start.setEnabled(t.status != DownloadTask.STATUS_RUNNING && t.status != DownloadTask.STATUS_DONE);
        start.setOnClickListener(v -> { DownloadService.startTask(this, t.id); render(); });
        
        Button pause = createActionBtn("暂停", "#E74C3C");
        pause.setEnabled(t.status == DownloadTask.STATUS_RUNNING);
        pause.setOnClickListener(v -> { DownloadService.pauseTask(this, t.id); render(); });
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, 100, 1);
        btnParams.setMargins(0, 0, 10, 0);
        row.addView(start, btnParams);
        
        LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, 100, 1);
        btnParams2.setMargins(10, 0, 0, 0);
        row.addView(pause, btnParams2);

        card.addView(row);
        list.addView(card, cardParams);
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(24f);
        return drawable;
    }

    private Button createSmallButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#3498DB"));
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setBackgroundColor(Color.TRANSPARENT);
        return btn;
    }

    private Button createActionBtn(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.parseColor(colorHex));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setStateListAnimator(null);

        GradientDrawable contentDrawable = new GradientDrawable();
        contentDrawable.setColor(Color.WHITE);
        contentDrawable.setCornerRadius(16f);
        contentDrawable.setStroke(2, Color.parseColor(colorHex));

        RippleDrawable rippleDrawable = new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#EAEDED")),
                contentDrawable,
                null
        );
        button.setBackground(rippleDrawable);
        return button;
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
        return String.format(Locale.US, "%.1f %s", v, u[i]);
    }
}
