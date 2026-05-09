(function() {
    if (window._quarkStubInjected) return;
    window._quarkStubInjected = true;
    console.log('[Quark Stub] Injected');
    // 基本的点击拦截：寻找包含“下载”、“保存到”等文本的按钮，尝试上报或阻止默认行为。
    document.addEventListener('click', function(e) {
        let target = e.target;
        while (target && target !== document) {
            let text = (target.innerText || '').trim();
            if (text === '下载' || text.includes('保存到网盘') || text.includes('打开APP')) {
                console.log('[Quark Stub] Intercepted button click: ', text);
                if (window.MMM_NATIVE) {
                    // 这里之后需要接入真实的直链解析逻辑
                    // window.MMM_NATIVE.startDownload(url, name, size, document.cookie);
                }
                break;
            }
            target = target.parentNode;
        }
    }, true);
})();
