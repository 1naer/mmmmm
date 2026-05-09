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
 var gmXhrSeq = 1;
 var gmXhrCallbacks = {};
 function normalizeXhrResponse(res){
   res = res || {};
   res.readyState = typeof res.readyState === 'number' ? res.readyState : 4;
   res.status = typeof res.status === 'number' ? res.status : 0;
   res.statusText = res.statusText || '';
   res.responseText = res.responseText || '';
   res.response = res.responseText;
   res.responseHeaders = res.responseHeaders || '';
   return res;
 }
 function dispatchXhrCallback(opt, res){
   res = normalizeXhrResponse(res);
   gmCall(opt.onreadystatechange, res);
   if(res.status >= 200 && res.status < 400 && !res.error) gmCall(opt.onload, res);
   else gmCall(opt.onerror, res);
   gmCall(opt.onloadend, res);
 }
 window.__mmmGMXmlHttpRequestCallback=function(id, payload){
   var entry = gmXhrCallbacks[id];
   if(!entry || entry.aborted) return;
   delete gmXhrCallbacks[id];
   try{
     var res = typeof payload === 'string' ? JSON.parse(payload || '{}') : (payload || {});
     dispatchXhrCallback(entry.opt, res);
   }catch(e){
     dispatchXhrCallback(entry.opt, {readyState:4,status:0,statusText:'',responseText:'',response:'',responseHeaders:'',error:String(e)});
   }
 };
 window.GM_xmlhttpRequest=function(opt){
   opt = opt || {};
   var req = {url:opt.url, method:opt.method||'GET', headers:opt.headers||{}, data:opt.data||null};
   var id = 'gmxhr_' + (gmXhrSeq++);
   gmXhrCallbacks[id] = {opt:opt, aborted:false};
   try{
     if(typeof MMM_NATIVE.gmXmlHttpRequestAsync === 'function'){
       MMM_NATIVE.gmXmlHttpRequestAsync(id, JSON.stringify(req));
       return {abort:function(){ var e=gmXhrCallbacks[id]; if(e){ e.aborted=true; delete gmXhrCallbacks[id]; } }};
     }
     var res = JSON.parse(MMM_NATIVE.gmXmlHttpRequest(JSON.stringify(req)) || '{}');
     delete gmXhrCallbacks[id];
     setTimeout(function(){ dispatchXhrCallback(opt, res); }, 0);
     return {abort:function(){}};
   }catch(e){
     delete gmXhrCallbacks[id];
     setTimeout(function(){ dispatchXhrCallback(opt, {readyState:4,status:0,statusText:'',responseText:'',response:'',responseHeaders:'',error:String(e)}); }, 0);
     return {abort:function(){}};
   }
 };
 window.GM={xmlhttpRequest:window.GM_xmlhttpRequest};
 window.flutter_inappwebview={ callHandler:function(name,data){ MMM_NATIVE.postMessage(JSON.stringify({handler:name,data:data})); } };
 window.MMM={ postMessage:function(o){ MMM_NATIVE.postMessage(typeof o==='string'?o:JSON.stringify(o)); }, addDownload:function(file){ MMM_NATIVE.postMessage(JSON.stringify({type:'download_links',payload:{files:[file]}})); } };
 console.log('[mmm] GM shim injected');
})();
