/**
 * 百度网盘Token提取器 AccessToken & Cookie
 * 作者: Token Extractor
 * 版本: 2.0.0 (Flutter WebView版本)
 * 更新时间: 2025-09-05
 */

(function() {
    'use strict';

    // 防止重复执行
    if (window.baiduTokenExtractor) {
        console.log('🔄 百度网盘Token提取器已存在，跳过重复初始化');
        return;
    }
    window.baiduTokenExtractor = true;

    console.log('🔑 百度网盘Token提取器已启动 (Flutter WebView版本 v2.0.0)');
    console.log('📅 启动时间:', new Date().toLocaleString());
    console.log('🌐 当前页面URL:', window.location.href);

    // 百度网盘API配置
    const config = {
        CLIENT_ID: 'IlLqBbU3GjQ0t46TRwFateTprHWl39zF',
        AUTH_URL: 'https://openapi.baidu.com/oauth/2.0/authorize',
        SCOPE: 'basic,netdisk',
        maxRetries: 3,
        retryDelay: 2000,
        requestTimeout: 10000
    };

    function extractToken() {
        const hash = window.location.hash;
        if (hash.includes('access_token')) {
            const params = new URLSearchParams(hash.replace('#', '?'));
            const token = params.get('access_token');
            if (token && window.MMM_NATIVE) {
                window.MMM_NATIVE.onTokenExtracted(token);
            }
        } else {
            // 如果没有 token，触发重定向授权
            const redirectUrl = `${config.AUTH_URL}?response_type=token&client_id=${config.CLIENT_ID}&redirect_uri=oob&scope=${config.SCOPE}`;
            window.location.href = redirectUrl;
        }
    }
    
    extractToken();
})();