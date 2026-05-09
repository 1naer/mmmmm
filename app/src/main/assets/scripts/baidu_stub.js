(function(){
  if (window.__mmm_baidu_adapter__) return;
  window.__mmm_baidu_adapter__ = true;

  const TAG = '[mmm][baidu]';
  const PAN_HOST_RE = /(^|\.)(pan|yun)\.baidu\.com$/i;
  const DL_RE = /https?:\/\/[^\s"'<>]+/ig;

  function isBaiduPan(){
    return PAN_HOST_RE.test(location.hostname);
  }

  function nativePost(obj){
    try {
      if (window.MMM_NATIVE && MMM_NATIVE.postMessage) {
        MMM_NATIVE.postMessage(JSON.stringify(obj));
        return true;
      }
    } catch(e) { console.warn(TAG, 'nativePost failed', e); }
    return false;
  }

  function cookies(){
    try { return MMM_NATIVE && MMM_NATIVE.getCookies ? (MMM_NATIVE.getCookies(location.href) || '') : document.cookie; }
    catch(e){ return document.cookie || ''; }
  }

  function authSnapshot(){
    const ls = {};
    try {
      ['BDUSS','STOKEN','PANWEB','csrfToken','token','uk','share_uk','shareid','sign','timestamp','fid_list'].forEach(k => {
        const v = localStorage.getItem(k) || sessionStorage.getItem(k);
        if (v) ls[k] = String(v).slice(0, 500);
      });
    } catch(e){}
    return {
      adapter: 'baidu',
      url: location.href,
      title: document.title || '',
      cookie: cookies(),
      storage: ls
    };
  }

  function filenameFromUrl(url, fallback){
    try {
      const u = new URL(url, location.href);
      const n = u.searchParams.get('filename') || u.searchParams.get('fin') || u.searchParams.get('name') || decodeURIComponent((u.pathname.split('/').pop() || ''));
      return (n && n.length < 180) ? n : fallback;
    } catch(e){ return fallback; }
  }

  function looksLikeDownloadUrl(url){
    if (!url || typeof url !== 'string') return false;
    return /\b(d.pcs|pcsdata|baidupcs|download|dlink|file)\b/i.test(url) || /[?&](app_id|fid|path|filename|fin)=/i.test(url);
  }

  function normalizeFile(item, idx){
    if (!item) return null;
    if (typeof item === 'string') item = {url:item};
    const url = item.url || item.dlink || item.downloadUrl || item.download_url || item.link || item.href;
    if (!url || !/^https?:/i.test(url)) return null;
    return {
      url: url,
      name: item.name || item.server_filename || item.filename || filenameFromUrl(url, 'baidu_' + (idx + 1) + '.bin'),
      size: Number(item.size || item.length || 0),
      headers: {
        Cookie: cookies(),
        Referer: location.href,
        'User-Agent': navigator.userAgent
      },
      source: 'baidu'
    };
  }

  function emitFiles(files, reason){
    files = (files || []).map(normalizeFile).filter(Boolean);
    if (!files.length) return false;
    console.log(TAG, 'emit files', reason, files);
    return nativePost({type:'download_links', source:'baidu', reason:reason || 'capture', payload:{files:files, auth:authSnapshot()}});
  }

  function scanJson(obj, out, depth){
    if (!obj || depth > 6) return;
    if (Array.isArray(obj)) { obj.forEach(x => scanJson(x, out, depth + 1)); return; }
    if (typeof obj === 'object') {
      const direct = normalizeFile(obj, out.length);
      if (direct && looksLikeDownloadUrl(direct.url)) out.push(direct);
      ['dlink','downloadUrl','download_url','url','link','href'].forEach(k => {
        if (typeof obj[k] === 'string' && /^https?:/i.test(obj[k]) && looksLikeDownloadUrl(obj[k])) out.push(normalizeFile({url:obj[k], name:obj.server_filename || obj.filename || obj.name}, out.length));
      });
      Object.keys(obj).slice(0,120).forEach(k => scanJson(obj[k], out, depth + 1));
    } else if (typeof obj === 'string' && obj.indexOf('http') >= 0) {
      const m = obj.match(DL_RE) || [];
      m.forEach(u => { if (looksLikeDownloadUrl(u)) out.push(normalizeFile(u, out.length)); });
    }
  }

  function handleResponseText(text, url){
    if (!text || text.length < 20) return;
    const out = [];
    try { scanJson(JSON.parse(text), out, 0); } catch(e) {
      const m = String(text).match(DL_RE) || [];
      m.forEach(u => { if (looksLikeDownloadUrl(u)) out.push(normalizeFile(u, out.length)); });
    }
    if (out.length) emitFiles(out, 'network:' + (url || 'unknown'));
  }

  const oldFetch = window.fetch;
  if (oldFetch) {
    window.fetch = function(){
      const reqUrl = arguments[0] && (arguments[0].url || arguments[0]);
      return oldFetch.apply(this, arguments).then(resp => {
        try {
          const ct = resp.headers && resp.headers.get && (resp.headers.get('content-type') || '');
          if (/json|text|javascript/i.test(ct) || /api|download|dlink|pcs/i.test(String(reqUrl))) {
            resp.clone().text().then(t => handleResponseText(t, String(reqUrl))).catch(()=>{});
          }
        } catch(e){}
        return resp;
      });
    };
  }

  const oldOpen = XMLHttpRequest.prototype.open;
  const oldSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(method, url){ this.__mmm_url = url; return oldOpen.apply(this, arguments); };
  XMLHttpRequest.prototype.send = function(){
    this.addEventListener('load', () => {
      try { handleResponseText(this.responseText, this.__mmm_url); } catch(e){}
    });
    return oldSend.apply(this, arguments);
  };

  function addPanel(){
    if (!isBaiduPan() || document.getElementById('mmm-baidu-panel')) return;
    const btn = document.createElement('button');
    btn.id = 'mmm-baidu-panel';
    btn.textContent = 'MMM捕获';
    btn.style.cssText = 'position:fixed;right:12px;bottom:88px;z-index:2147483647;background:#1677ff;color:#fff;border:0;border-radius:18px;padding:10px 14px;font-size:14px;box-shadow:0 4px 12px #0005;';
    btn.onclick = function(){
      const files = [];
      document.querySelectorAll('a[href]').forEach((a,i) => {
        const href = a.href;
        if (looksLikeDownloadUrl(href)) files.push({url:href, name:(a.textContent||'').trim() || filenameFromUrl(href, 'baidu_'+(i+1)+'.bin')});
      });
      if (!emitFiles(files, 'manual-dom-scan')) nativePost({type:'baidu_auth_snapshot', payload:authSnapshot()});
    };
    document.documentElement.appendChild(btn);
  }

  window.mmmBaiduAuthSnapshot = authSnapshot;
  window.mmmBaiduEmitFiles = emitFiles;
  addPanel();
  new MutationObserver(addPanel).observe(document.documentElement, {childList:true, subtree:true});
  console.log(TAG, 'adapter injected', location.href);
})();