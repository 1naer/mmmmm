# mmm V4

网盘分享链接解析与高速下载 Android 原生工程骨架。

## 当前已实现

- 首页输入/接收分享链接
- WebView 打开网盘页面
- 域名识别和脚本注入入口
- `window.MMM` JSBridge
- `flutter_inappwebview.callHandler` 兼容层
- 最小 GM API Shim：`GM_info`、`unsafeWindow`、`GM_setValue`、`GM_getValue`、`GM_xmlhttpRequest` 桥接入口
- JS 侧通用下载链接捕获器
- 下载任务模型
- HttpURLConnection Range 多线程下载骨架

## 后续

1. 接入完整 LinkSwift/定制网盘脚本。
2. 完善 GM_xmlhttpRequest 的异步回调。
3. 优先适配百度网盘。
4. 下载器加入数据库、前台服务和通知。
5. 再接 aria2c / Gopeed core。

> 注意：本项目只应用于下载用户有权访问的文件。