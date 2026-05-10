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

        // 1. 模拟竞品请求参数 (移植自 ceshi.js)
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 Safari/537.36 Channel/pckk_other_ch";
        String apiUrl = "https://drive.quark.cn/1/clouddrive/file/download?pr=ucpro&fr=pc";
        
        // 实际场景需要先请求 share 接口获取 fid，为了演示底层通道的打通，这里先尝试构造基础请求框架
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Cookie", cookie);
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        conn.setDoOutput(true);

        // 占位 JSON，实际需要先提取分享链接里的 fid
        String jsonBody = "{\"fids\":[]}";
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            // 竞品逻辑：读取响应 JSON 中的 download_url
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line.trim());
            }
            in.close();
            
            String resStr = response.toString();
            if (resStr.contains("\"code\":31001")) {
                callback.onFail("Cookie已过期或无效，请重新登录夸克网盘！");
            } else {
                callback.onFail("原生API直调成功！响应内容已捕获，接下来需补齐fid提取逻辑：" + resStr.substring(0, Math.min(resStr.length(), 100)));
            }
        } else {
            callback.onFail("网络请求失败，状态码：" + responseCode);
        }
    }

    public interface Callback {
        void onSuccess(String directUrl, String filename);
        void onFail(String reason);
    }
}
