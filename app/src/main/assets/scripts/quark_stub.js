/**
 * Quark Deep Interceptor for mmmmm
 * 深度拦截并提取夸克网盘的真实下载链接，彻底解决“点击后无事发生”的问题。
 */
(function() {
    console.log("[MMM-Quark] 深度拦截脚本已注入，等待捕获下载直链...");

    function triggerNativeDownload(url, filename) {
        console.log("[MMM-Quark] 成功捕获直链:", url, "文件名:", filename);
        if (window.MMM_NATIVE && window.MMM_NATIVE.startDownload) {
            window.MMM_NATIVE.startDownload(url, filename || "quark_file");
            // Toast 提示让用户知道拦截成功
            var toast = document.createElement("div");
            toast.innerText = "成功拦截下载: " + (filename || "");
            toast.style.cssText = "position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.8);color:#fff;padding:10px 20px;border-radius:20px;z-index:999999;";
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        } else {
            console.error("[MMM-Quark] MMM_NATIVE 未定义!");
        }
    }

    // 1. 拦截 XMLHttpRequest (网盘常用的异步请求方式)
    const originalXhrOpen = XMLHttpRequest.prototype.open;
    const originalXhrSend = XMLHttpRequest.prototype.send;

    XMLHttpRequest.prototype.open = function(method, url) {
        this._requestUrl = url;
        return originalXhrOpen.apply(this, arguments);
    };

    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() {
            try {
                // 夸克网盘获取下载链接的典型 API 路径
                if (this._requestUrl && this._requestUrl.includes('/clouddrive/file/download')) {
                    const response = JSON.parse(this.responseText);
                    console.log("[MMM-Quark] XHR 拦截到 download API 响应:", response);
                    
                    if (response && response.data && response.data.length > 0) {
                        const fileData = response.data[0];
                        const downloadUrl = fileData.download_url;
                        const filename = fileData.file_name;
                        
                        if (downloadUrl) {
                            triggerNativeDownload(downloadUrl, filename);
                        }
                    }
                }
            } catch (e) {
                // Ignore parse errors for non-JSON responses
            }
        });
        return originalXhrSend.apply(this, arguments);
    };

    // 2. 拦截 Fetch API (现代前端常用的请求方式)
    const originalFetch = window.fetch;
    window.fetch = async function(...args) {
        const response = await originalFetch.apply(this, args);
        const url = args[0];
        
        try {
            if (typeof url === 'string' && url.includes('/clouddrive/file/download')) {
                const clonedResponse = response.clone();
                const data = await clonedResponse.json();
                console.log("[MMM-Quark] Fetch 拦截到 download API 响应:", data);
                
                if (data && data.data && data.data.length > 0) {
                    const fileData = data.data[0];
                    const downloadUrl = fileData.download_url;
                    const filename = fileData.file_name;
                    
                    if (downloadUrl) {
                        triggerNativeDownload(downloadUrl, filename);
                    }
                }
            }
        } catch (e) {
            console.error("[MMM-Quark] Fetch 拦截解析失败:", e);
        }
        return response;
    };

    // 3. 兜底方案：拦截 A 标签的直接点击
    document.addEventListener('click', function(e) {
        let target = e.target;
        while (target && target.tagName !== 'A') {
            target = target.parentElement;
        }
        if (target && target.href && target.href.includes('drive-pc.quark.cn/1/clouddrive/file/download')) {
            e.preventDefault();
            e.stopPropagation();
            triggerNativeDownload(target.href, target.download || target.innerText);
        }
    }, true);

})();
