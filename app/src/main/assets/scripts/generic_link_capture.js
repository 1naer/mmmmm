(function(){
 if(window.__mmm_generic_capture__) return; window.__mmm_generic_capture__=true;
 function guessName(u){try{var p=new URL(u).pathname.split('/').pop();return decodeURIComponent(p||'download.bin')}catch(e){return 'download.bin'}}
 window.mmmScanDownloadLinks=function(){
   var exts=/\.(zip|rar|7z|apk|exe|dmg|mp4|mkv|mp3|flac|pdf|docx?|xlsx?|pptx?)(\?|$)/i;
   var files=[]; document.querySelectorAll('a[href]').forEach(function(a){var u=a.href;if(/^https?:/.test(u)&&exts.test(u)) files.push({url:u,name:(a.textContent||guessName(u)).trim(),headers:{Referer:location.href,'User-Agent':navigator.userAgent,Cookie:MMM_NATIVE.getCookies(location.href)||''}})});
   if(files.length) MMM_NATIVE.postMessage(JSON.stringify({type:'download_links',payload:{files:files}}));
   return files;
 };
 console.log('[mmm] generic link capture injected');
})();