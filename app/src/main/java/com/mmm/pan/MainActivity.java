package com.mmm.pan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {
    EditText input;

    @Override public void onCreate(Bundle b){ super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        TextView title=new TextView(this); title.setText("mmm"); title.setTextSize(30); title.setTextColor(Color.rgb(40,35,50)); title.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView sub=new TextView(this); sub.setText("网盘分享链接解析与下载 V4"); sub.setTextSize(14); sub.setGravity(Gravity.CENTER_HORIZONTAL);
        input=new EditText(this); input.setHint("粘贴网盘分享链接，例如 pan.baidu.com/s/..."); input.setSingleLine(false); input.setMinLines(3);
        Button open=new Button(this); open.setText("打开并解析");
        Button downloads=new Button(this); downloads.setText("下载任务");
        root.addView(title); root.addView(sub); root.addView(input, new LinearLayout.LayoutParams(-1,-2)); root.addView(open); root.addView(downloads);
        setContentView(root);

        String shared = readSharedText(); if(shared!=null) input.setText(shared);
        open.setOnClickListener(v -> {
            ShareLinkParser.Result result = ShareLinkParser.parse(input.getText().toString());
            if (!result.ok) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
                return;
            }
            input.setText(result.url);
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            BrowserActivity.open(this, result.url);
        });
        downloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));
    }

    private String readSharedText(){ Intent i=getIntent(); if(Intent.ACTION_SEND.equals(i.getAction())) return i.getStringExtra(Intent.EXTRA_TEXT); return null; }
}
