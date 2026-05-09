package com.mmm.pan;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DownloadRepository {
    static final String PREF = "downloads";
    static final String KEY = "tasks";

    public static synchronized void addFromJson(Context c, String arrJson) throws Exception {
        JSONArray in = new JSONArray(arrJson);
        List<DownloadTask> all = load(c);
        for (int i = 0; i < in.length(); i++) {
            DownloadTask t = DownloadTask.fromJson(in.getJSONObject(i));
            boolean exists = false;
            for (DownloadTask old : all) {
                if (old.id.equals(t.id) || (old.url.equals(t.url) && old.name.equals(t.name) && old.status != DownloadTask.STATUS_DONE)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) all.add(t);
        }
        save(c, all);
    }

    public static synchronized List<DownloadTask> load(Context c) {
        List<DownloadTask> out = new ArrayList<>();
        JSONArray a = loadArray(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) out.add(DownloadTask.fromJson(o));
        }
        return out;
    }

    public static synchronized JSONArray loadArray(Context c) {
        try {
            return new JSONArray(c.getSharedPreferences(PREF, 0).getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static synchronized DownloadTask find(Context c, String id) {
        for (DownloadTask t : load(c)) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    public static synchronized void update(Context c, DownloadTask task) {
        List<DownloadTask> all = load(c);
        boolean found = false;
        task.updatedAt = System.currentTimeMillis();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(task.id)) {
                all.set(i, task);
                found = true;
                break;
            }
        }
        if (!found) all.add(task);
        save(c, all);
    }

    public static synchronized void updateStatus(Context c, String id, int status, String error) {
        DownloadTask t = find(c, id);
        if (t == null) return;
        t.status = status;
        t.error = error == null ? "" : error;
        update(c, t);
    }

    public static synchronized void save(Context c, List<DownloadTask> tasks) {
        JSONArray a = new JSONArray();
        try {
            for (DownloadTask t : tasks) a.put(t.toJson());
        } catch (Exception ignored) {}
        save(c, a);
    }

    public static synchronized void save(Context c, JSONArray a) {
        c.getSharedPreferences(PREF, 0).edit().putString(KEY, a.toString()).apply();
    }
}