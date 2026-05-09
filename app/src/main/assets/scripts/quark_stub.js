(function() {
    if (window._quarkStubInjected) return;
    window._quarkStubInjected = true;
    console.log('[Quark Stub] Injected (LinkSwift adapted)');

    const downloadApi = "https://drive-pc.quark.cn/1/clouddrive/file/download?entry=ft&fr=pc&pr=ucpro";
    const userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/3.20.0 Chrome/112.0.5615.165 Electron/24.1.3.8 Safari/537.36 Channel/pckk_other_ch";

    // 拦截全局 fetch 和 XHR 看看有没有被夸克官方页面调用的接口
    // 当点击页面原有的“下载”按钮时，它自己也会去请求下载接口
    const originalOpen = XMLHttpRequest.prototype.open;
    const originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function(method, url) {
        this._url = url;
        return originalOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() {
            try {
                // 如果拦截到夸克自身的下载请求
                if (this._url.indexOf('clouddrive/file/download') !== -1) {
                    let res = JSON.parse(this.responseText);
                    if (res && res.code === 0 && res.data && res.data.length > 0) {
                        console.log('[Quark Stub] API Intercepted', res.data);
                        if (window.MMM_NATIVE) {
                            res.data.forEach(file => {
                                if (file.download_url) {
                                    window.MMM_NATIVE.startDownload(file.download_url, file.file_name || 'quark_file', file.size || 0, document.cookie);
                                }
                            });
                        }
                    }
                }
            } catch(e) {}
        });
        return originalSend.apply(this, arguments);
    };

    // 拦截按钮点击，触发下载抓取
    document.addEventListener('click', async function(e) {
        let target = e.target;
        while (target && target !== document) {
            let text = (target.innerText || '').trim();
            let className = (target.className || '').toLowerCase();
            // 匹配下载或保存按钮
            if (text === '下载' || text.includes('保存到网盘') || text.includes('打开APP') || className.includes('download') || className.includes('pl-button-save')) {
                
                // 检查全局状态是否有勾选的文件（借用React Fiber节点通常难以提取，所以上面的XHR拦截是第一道防线）
                // 这里我们可以进一步尝试主动提取当前选中的 fid。
                // 因为不同网盘分享页结构复杂，所以上面的 XHR 拦截往往能直接捕捉到原生按钮触发的下载请求
                console.log('[Quark Stub] Triggered download button click');
            }
            target = target.parentNode;
        }
    }, true);
})();
