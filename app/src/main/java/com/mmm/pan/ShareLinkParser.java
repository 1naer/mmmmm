package com.mmm.pan;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.CookieManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        } else if (text.contains("pan.baidu.com") || text.contains("yun.baidu.com")) {
            r.ok = true;
            r.provider = "百度网盘";
        } else if (text.contains("aliyundrive.com") || text.contains("alipan.com")) {
            r.ok = true;
            r.provider = "阿里云盘";
        } else {
            r.ok = false;
            r.message = "暂不支持该网盘";
        }
        return r;
    }

    public interface Callback {
        void onSuccess(String directUrl, String filename);
        void onFail(String reason);
    }

    public static void fetchDirectLinkByFidsAsync(Context context, String provider, String currentUrl, List<String> fids, Callback callback) {
        new Thread(() -> {
            try {
                String cookie = CookieManager.getInstance().getCookie(currentUrl);
                if (TextUtils.isEmpty(cookie)) {
                    callback.onFail("请先在网页中登录账号！");
                    return;
                }

                if ("夸克网盘".equals(provider)) {
                    doQuarkApiParse(cookie, fids, callback);
                } else if ("阿里云盘".equals(provider)) {
                    doAliyunApiParse(cookie, fids, callback);
                } else if ("百度网盘".equals(provider)) {
                    doBaiduApiParse(cookie, fids, callback);
                } else {
                    callback.onFail("当前网盘的底层隧道正在建设中...");
                }
            } catch (Exception e) {
                callback.onFail("API解析异常：" + e.getMessage());
            }
        }).start();
    }

    private static void doQuarkApiParse(String cookie, List<String> fids, Callback callback) throws Exception {
        if (fids == null || fids.isEmpty()) {
            callback.onFail("请在页面上勾选需要下载的文件！");
            return;
        }

        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/2.5.20 Chrome/100.0.4896.160 Electron/18.3.5.4-b478491100 Safari/537.36 Channel/pckk_other_ch";
        String apiUrl = "https://drive.quark.cn/1/clouddrive/file/download?pr=ucpro&fr=pc";
        
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Cookie", cookie);
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        conn.setDoOutput(true);

        JSONArray fidsArray = new JSONArray(fids);
        JSONObject body = new JSONObject();
        body.put("fids", fidsArray);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line.trim());
            }
            in.close();
            
            JSONObject respObj = new JSONObject(response.toString());
            if (respObj.optInt("code", -1) == 0) {
                JSONArray data = respObj.optJSONArray("data");
                if (data != null && data.length() > 0) {
                    JSONObject fileObj = data.getJSONObject(0);
                    String downloadUrl = fileObj.optString("download_url");
                    String filename = fileObj.optString("file_name");
                    if (!TextUtils.isEmpty(downloadUrl)) {
                        callback.onSuccess(downloadUrl, filename);
                        return;
                    }
                }
                callback.onFail("成功访问 API，但未找到下载链接。");
            } else {
                callback.onFail("夸克风控拦截，代码：" + respObj.optInt("code", -1));
            }
        } else {
            callback.onFail("夸克接口请求失败：" + responseCode);
        }
    }

    private static void doBaiduApiParse(String cookie, List<String> fids, Callback callback) throws Exception {
        if (fids == null || fids.isEmpty()) {
            callback.onFail("请在页面上勾选需要下载的文件！");
            return;
        }

        // 获取BDUSS用于签名验证
        String bduss = extractCookie(cookie, "BDUSS");
        if (TextUtils.isEmpty(bduss)) {
            callback.onFail("未找到百度的核心凭证BDUSS，请重新登录！");
            return;
        }

        // 模拟PC客户端请求
        String userAgent = "netdisk;7.0.3.2;PC;PC-Windows;10.0.19042;WindowsBaiduYunGuanJia";
        JSONArray fidList = new JSONArray(fids);
        
        // 百度获取直链API
        String apiUrl = "https://pan.baidu.com/api/download?clienttype=1&app_id=250528&web=1";
        String postData = "fidlist=" + java.net.URLEncoder.encode(fidList.toString(), "utf-8") + "&type=dlink";

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Cookie", cookie);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line.trim());
            }
            in.close();

            JSONObject respObj = new JSONObject(response.toString());
            if (respObj.optInt("errno", -1) == 0) {
                JSONArray dlinkList = respObj.optJSONArray("dlink");
                if (dlinkList != null && dlinkList.length() > 0) {
                    JSONObject fileObj = dlinkList.getJSONObject(0);
                    String directUrl = fileObj.optString("dlink");
                    // 真实的Gopeed等扩展需要配合特定User-Agent和Cookie去下载dlink
                    if (!TextUtils.isEmpty(directUrl)) {
                        callback.onSuccess(directUrl, "baidu_download_file");
                        return;
                    }
                }
                callback.onFail("百度API返回成功，但缺少dlink信息。");
            } else {
                callback.onFail("百度风控拦截，错误码：" + respObj.optInt("errno", -1) + " (提示: 百度对黑号限速极严，可能需要SVIP账号)");
            }
        } else {
            callback.onFail("百度接口请求失败：" + responseCode);
        }
    }

    private static void doAliyunApiParse(String cookie, List<String> fids, Callback callback) throws Exception {
        callback.onFail("阿里云盘的底层API需先从 localStorage 获取 Token，已排入下发计划。");
    }

    private static String extractCookie(String cookieStr, String key) {
        if (TextUtils.isEmpty(cookieStr)) return "";
        String[] pairs = cookieStr.split(";");
        for (String pair : pairs) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return "";
    }
}
