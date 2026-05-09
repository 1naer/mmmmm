package com.mmm.pan;
import android.app.*;
import android.os.*;
import android.widget.*;
import org.json.*;

public class DownloadActivity extends Activity{ LinearLayout list;
    public void onCreate(Bundle b){super.onCreate(b); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(24,36,24,24); setContentView(list); render();}
    void render(){ list.removeAllViews(); TextView title=new TextView(this); title.setText("下载任务"); title.setTextSize(24); list.addView(title); JSONArray a=DownloadRepository.loadArray(this); if(a.length()==0){TextView e=new TextView(this); e.setText("暂无任务。先到首页打开网盘链接解析。"); list.addView(e); return;}
        for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o==null)continue; Button b=new Button(this); b.setText(o.optString("name") + sizeText(o.optLong("size", -1)) + "\n" + o.optString("url")); b.setAllCaps(false); b.setOnClickListener(v-> RangeDownloader.start(this,o)); list.addView(b); }
    }
    String sizeText(long size){ if(size<=0)return ""; double v=size; String[] u={" B"," KB"," MB"," GB"}; int i=0; while(v>=1024 && i<u.length-1){v/=1024;i++;} return String.format(java.util.Locale.US,"  %.1f%s",v,u[i]); }
}
