(function(){
 if(window.__mmm_gm_shim__) return; window.__mmm_gm_shim__=true;
 window.unsafeWindow = window.unsafeWindow || window;
 window.GM_info = window.GM_info || {script:{name:'mmm-shim',version:'0.1.0'},scriptHandler:'mmm'};
 window.GM_setValue=function(k,v){ MMM_NATIVE.saveValue(String(k), JSON.stringify(v)); };
 window.GM_getValue=function(k,d){ var r=MMM_NATIVE.getValue(String(k)); if(!r) return d; try{return JSON.parse(r)}catch(e){return r} };
 window.GM_deleteValue=function(k){ MMM_NATIVE.saveValue(String(k),''); };
 window.GM_setClipboard=function(t){ MMM_NATIVE.postMessage(JSON.stringify({type:'clipboard',text:String(t)})); };
 window.GM_cookie={ list:function(o,cb){ cb && cb([{domain:location.hostname,cookie:MMM_NATIVE.getCookies(location.href)}]); } };
 window.GM_xmlhttpRequest=function(opt){
   try{ var res=JSON.parse(MMM_NATIVE.gmXmlHttpRequest(JSON.stringify({url:opt.url,method:opt.method||'GET',headers:opt.headers||{},data:opt.data||null}))); if(opt.onload) opt.onload(res); }catch(e){ if(opt.onerror) opt.onerror(e); }
 };
 window.GM={xmlhttpRequest:window.GM_xmlhttpRequest};
 window.flutter_inappwebview={ callHandler:function(name,data){ MMM_NATIVE.postMessage(JSON.stringify({handler:name,data:data})); } };
 window.MMM={ postMessage:function(o){ MMM_NATIVE.postMessage(typeof o==='string'?o:JSON.stringify(o)); }, addDownload:function(file){ MMM_NATIVE.postMessage(JSON.stringify({type:'download_links',payload:{files:[file]}})); } };
 console.log('[mmm] GM shim injected');
})();