package com.mmm.pan;
import android.content.*;
import org.json.*;
import java.util.*;

public class DownloadRepository {
    static final String KEY="tasks";
    public static void addFromJson(Context c,String arrJson)throws Exception{ JSONArray in=new JSONArray(arrJson); JSONArray all=loadArray(c); for(int i=0;i<in.length();i++){ all.put(toJson(DownloadTask.fromJson(in.getJSONObject(i)))); } save(c,all); }
    public static JSONArray loadArray(Context c){ try{return new JSONArray(c.getSharedPreferences("downloads",0).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();} }
    public static void save(Context c,JSONArray a){ c.getSharedPreferences("downloads",0).edit().putString(KEY,a.toString()).apply(); }
    static JSONObject toJson(DownloadTask t)throws Exception{ JSONObject o=new JSONObject(); o.put("id",t.id); o.put("name",t.name); o.put("url",t.url); o.put("headers",new JSONObject(t.headersJson)); o.put("size",t.size); o.put("status",t.status); o.put("done",t.done); return o; }
}
