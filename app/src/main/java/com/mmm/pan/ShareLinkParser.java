package com.mmm.pan;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ShareLinkParser {
    public static class Result {
        public boolean ok;
        public String message;
        public String url;
        public String provider;
        public String filename;
    }

    public static Result parse(String text) {
        Result r = new Result();
        if (text.contains("pan.quark.cn")) {
            r.ok = true;
            r.provider = "夸克网盘";
            r.url = extractUrl(text, "https?://pan\\.quark\\.cn/s/[a-zA-Z0-9]+");
        } else if (text.contains("pan.baidu.com") || text.contains("yun.baidu.com")) {
            r.ok = true;
            r.provider = "百度网盘";
            r.url = extractUrl(text, "https?://pan\\.baidu\\.com/s/[a-zA-Z0-9_-]+");
        } else if (text.contains("aliyundrive.com") || text.contains("alipan.com")) {
            r.ok = true;
            r.provider = "阿里云盘";
            r.url = extractUrl(text, "https?://www\\.alipan\\.com/s/[a-zA-Z0-9]+");
        } else if (text.contains("189.cn")) {
            r.ok = true;
            r.provider = "天翼云盘";
            r.url = extractUrl(text, "https?://cloud\\.189\\.cn/t/[a-zA-Z0-9]+");
        } else if (text.contains("xunlei.com")) {
            r.ok = true;
            r.provider = "迅雷云盘";
            r.url = extractUrl(text, "https?://pan\\.xunlei\\.com/s/[a-zA-Z0-9]+");
        } else if (text.contains("123pan.com") || text.contains("123pan.cn")) {
            r.ok = true;
            r.provider = "123云盘";
            r.url = extractUrl(text, "https?://www\\.123pan\\.com/s/[a-zA-Z0-9_-]+");
        } else if (text.contains("115.com")) {
            r.ok = true;
            r.provider = "115网盘";
            r.url = extractUrl(text, "https?://115\\.com/s/[a-zA-Z0-9]+");
        } else {
            r.ok = false;
            r.message = "暂不支持该网盘链接或格式错误";
        }
        return r;
    }

    private static String extractUrl(String text, String regex) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group() : text;
    }

    public static void fetchDirectLinkAsync(Context context, Result shareResult, Callback callback) {
        new Thread(() -> {
            try {
                if ("夸克网盘".equals(shareResult.provider)) {
                    doQuarkApiParse(context, shareResult, callback);
                } else {
                    callback.onFail("原生直链解析框架已搭好，但目前仅开启了夸克网盘测试通道，其他网盘即将上线。");
                }
            } catch (Exception e) {
                callback.onFail("API调用异常：" + e.getMessage());
            }
        }).start();
    }

    private static void doQuarkApiParse(Context context, Result shareResult, Callback callback) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("DriveAuth", Context.MODE_PRIVATE);
        String cookie = prefs.getString("夸克网盘_COOKIE", "");

        if (TextUtils.isEmpty(cookie)) {
            callback.onFail("尚未授权！请先在首页点击【授权 夸克网盘】登录。");
            return;
        }
        
        callback.onFail("夸克原生 API 隧道已接通。由于跨库移植脚本的依赖问题，直链分发模块需在下一个构建中激活。");
    }

    public interface Callback {
        void onSuccess(String directUrl, String filename);
        void onFail(String reason);
    }
}