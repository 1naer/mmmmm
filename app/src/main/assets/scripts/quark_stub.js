(function() {
    if (window._quarkStubInjected) return;
    window._quarkStubInjected = true;
    console.log('[Quark Stub] Injected - Real extraction logic');

    const downloadApi = "https://drive-pc.quark.cn/1/clouddrive/file/download?entry=ft&fr=pc&pr=ucpro";
    const userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) quark-cloud-drive/3.20.0 Chrome/112.0.5615.165 Electron/24.1.3.8 Safari/537.36 Channel/pckk_other_ch";

    // 辅助函数：拦截原生API响应
    const originalOpen = XMLHttpRequest.prototype.open;
    const originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function(method, url) {
        this._url = url;
        return originalOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() {
            try {
                // 尝试抓取文件列表
                if (this._url.indexOf('clouddrive/share/sharePageInfo') !== -1 || this._url.indexOf('clouddrive/file/sort') !== -1) {
                    let res = JSON.parse(this.responseText);
                    if (res && res.data && res.data.list) {
                        window._quarkCurrentFiles = res.data.list;
                    }
                }
            } catch(e) {}
        });
        return originalSend.apply(this, arguments);
    };

    // 拦截点击事件
    document.addEventListener('click', function(e) {
        let target = e.target;
        while (target && target !== document) {
            let text = (target.innerText || '').trim();
            let className = (target.className || '').toLowerCase();
            
            if (text === '下载' || text.includes('保存到网盘') || className.includes('download') || className.includes('pl-button-save')) {
                console.log('[Quark Stub] Intercepted button click: ', text);
                
                // 尝试提取 fid 和 pwd_id 并请求真实链接
                let pwd_id = location.pathname.match(/^\/(?:s|share)\/([a-zA-Z0-9]+)/);
                pwd_id = pwd_id ? pwd_id[1] : '';

                // 如果已经有缓存的文件列表
                if (window._quarkCurrentFiles && window._quarkCurrentFiles.length > 0) {
                    let fids = window._quarkCurrentFiles.map(f => f.fid);
                    let fids_token = window._quarkCurrentFiles.map(f => f.share_fid_token);
                    let stoken = window._quarkCurrentFiles[0].stoken || '';
                    
                    // 发起获取直链的请求
                    fetch(downloadApi, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'User-Agent': userAgent
                        },
                        body: JSON.stringify({
                            fids: fids,
                            fids_token: fids_token,
                            pwd_id: pwd_id,
                            stoken: stoken
                        })
                    }).then(r => r.json()).then(res => {
                        if (res.code === 0 && res.data) {
                            res.data.forEach(file => {
                                if (file.download_url && window.MMM_NATIVE) {
                                    console.log('[Quark Stub] Extracted real link!', file);
                                    window.MMM_NATIVE.startDownload(file.download_url, file.file_name, file.size || 0, document.cookie);
                                }
                            });
                            alert('已提取并开始下载 ' + res.data.length + ' 个文件！');
                        } else {
                            alert('提取直链失败，可能需要登录：' + (res.message || res.code));
                        }
                    }).catch(err => console.error('[Quark Stub]', err));

                    e.preventDefault();
                    e.stopPropagation();
                }
                break;
            }
            target = target.parentNode;
        }
    }, true);
})();
