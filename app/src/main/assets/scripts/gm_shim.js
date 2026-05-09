(function(){
 if(window.__mmm_gm_shim__) return; window.__mmm_gm_shim__=true;
 window.unsafeWindow = window.unsafeWindow || window;
 window.GM_info = window.GM_info || {script:{name:'mmm-shim',version:'0.2.0'},scriptHandler:'mmm'};
 window.GM_setValue=function(k,v){ MMM_NATIVE.saveValue(String(k), JSON.stringify(v)); };
 window.GM_getValue=function(k,d){ var r=MMM_NATIVE.getValue(String(k)); if(!r) return d; try{return JSON.parse(r)}catch(e){return r} };
 window.GM_deleteValue=function(k){ MMM_NATIVE.saveValue(String(k),''); };
 window.GM_setClipboard=function(t){ MMM_NATIVE.postMessage(JSON.stringify({type:'clipboard',text:String(t)})); };
 window.GM_cookie={ list:function(o,cb){ cb && cb([{domain:location.hostname,cookie:MMM_NATIVE.getCookies(location.href)}]); } };
 function gmCall(fn, arg){ try{ if(typeof fn==='function') fn(arg); }catch(e){ console.warn('[mmm] GM callback failed', e); } }
 window.GM_xmlhttpRequest=function(opt){
   opt = opt || {};
   var req = {url:opt.url, method:opt.method||'GET', headers:opt.headers||{}, data:opt.data||null};
   try{
     var res = JSON.parse(MMM_NATIVE.gmXmlHttpRequest(JSON.stringify(req)) || '{}');
     res.readyState = typeof res.readyState === 'number' ? res.readyState : 4;
     res.status = typeof res.status === 'number' ? res.status : 0;
     res.statusText = res.statusText || '';
     res.responseText = res.responseText || '';
     res.response = res.responseText;
     gmCall(opt.onreadystatechange, res);
     if(res.status >= 200 && res.status < 400 && !res.error) gmCall(opt.onload, res);
     else gmCall(opt.onerror, res);
     gmCall(opt.onloadend, res);
     return {abort:function(){ console.warn('[mmm] GM_xmlhttpRequest abort is not supported yet'); }};
   }catch(e){
     var err = {readyState:4,status:0,statusText:'',responseText:'',response:'',responseHeaders:'',error:String(e)};
     gmCall(opt.onreadystatechange, err);
     gmCall(opt.onerror, err);
     gmCall(opt.onloadend, err);
     return {abort:function(){}};
   }
 };
 window.GM={xmlhttpRequest:window.GM_xmlhttpRequest};
 window.flutter_inappwebview={ callHandler:function(name,data){ MMM_NATIVE.postMessage(JSON.stringify({handler:name,data:data})); } };
 window.MMM={ postMessage:function(o){ MMM_NATIVE.postMessage(typeof o==='string'?o:JSON.stringify(o)); }, addDownload:function(file){ MMM_NATIVE.postMessage(JSON.stringify({type:'download_links',payload:{files:[file]}})); } };
 console.log('[mmm] GM shim injected');
})();