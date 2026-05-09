package com.mmm.pan;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShareLinkParser {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)((?:https?://)?(?:[a-z0-9-]+\\.)+[a-z]{2,}(?::\\d{1,5})?(?:/[^\\s\\u3000，。！？；、]*)?)"
    );

    private ShareLinkParser() {}

    public static Result parse(String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return Result.error("请先粘贴网盘分享链接");
        }

        String candidate = extractUrl(raw.trim());
        if (candidate.length() == 0) {
            return Result.error("未识别到有效链接，请粘贴完整分享文本或 URL");
        }

        if (!candidate.matches("(?i)^https?://.*")) {
            candidate = "https://" + candidate;
        }

        try {
            URI uri = new URI(candidate);
            String scheme = lower(uri.getScheme());
            String host = lower(uri.getHost());
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();

            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return Result.error("仅支持 http/https 分享链接");
            }
            if (host.length() == 0) {
                return Result.error("链接缺少有效域名");
            }

            String provider = providerOf(host);
            if (provider.length() == 0) {
                return Result.error("暂不支持该网盘域名：" + host);
            }
            if (!looksLikeShare(provider, host, path)) {
                return Result.error("该链接不像可解析的分享链接，请确认是否复制了分享页地址");
            }

            return Result.ok(uri.toString(), provider, "已识别：" + provider);
        } catch (Exception e) {
            return Result.error("链接格式不正确，请检查后重试");
        }
    }

    private static String extractUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = trimTrailing(matcher.group(1));
            if (url.length() > 0) return url;
        }
        return "";
    }

    private static String trimTrailing(String url) {
        while (url.endsWith(".") || url.endsWith(",") || url.endsWith(";") || url.endsWith(")")
                || url.endsWith("]") || url.endsWith("}") || url.endsWith("。") || url.endsWith("，")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String providerOf(String host) {
        if (endsWithHost(host, "pan.baidu.com") || endsWithHost(host, "yun.baidu.com")) return "百度网盘";
        if (endsWithHost(host, "aliyundrive.com") || endsWithHost(host, "alipan.com")) return "阿里云盘";
        if (endsWithHost(host, "pan.quark.cn") || endsWithHost(host, "drive.uc.cn")) return "夸克网盘";
        if (endsWithHost(host, "cloud.189.cn")) return "天翼云盘";
        if (endsWithHost(host, "pan.xunlei.com")) return "迅雷云盘";
        if (endsWithHost(host, "123pan.com") || endsWithHost(host, "123684.com")) return "123云盘";
        if (endsWithHost(host, "115.com") || endsWithHost(host, "anxia.com")) return "115网盘";
        return "";
    }

    private static boolean looksLikeShare(String provider, String host, String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if ("百度网盘".equals(provider)) return p.contains("/s/") || p.contains("/share/") || p.contains("/wap/init");
        if ("阿里云盘".equals(provider)) return p.contains("/s/") || p.contains("/drive/file");
        if ("夸克网盘".equals(provider)) return p.contains("/s/") || p.contains("/clouddrive/share");
        if ("天翼云盘".equals(provider)) return p.contains("/t/") || p.contains("/web/share");
        if ("迅雷云盘".equals(provider)) return p.contains("/s/");
        if ("123云盘".equals(provider)) return p.contains("/s/") || p.contains("/share/");
        if ("115网盘".equals(provider)) return p.contains("/s/") || p.contains("/share/");
        return false;
    }

    private static boolean endsWithHost(String host, String suffix) {
        return host.equals(suffix) || host.endsWith("." + suffix);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public static final class Result {
        public final boolean ok;
        public final String url;
        public final String provider;
        public final String message;

        private Result(boolean ok, String url, String provider, String message) {
            this.ok = ok;
            this.url = url;
            this.provider = provider;
            this.message = message;
        }

        public static Result ok(String url, String provider, String message) {
            return new Result(true, url, provider, message);
        }

        public static Result error(String message) {
            return new Result(false, "", "", message);
        }
    }
}
