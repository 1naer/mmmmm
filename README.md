# mmm V4

![Android Build](https://github.com/1naer/mmmmm/actions/workflows/android-build.yml/badge.svg)

网盘分享链接解析与高速下载 Android 原生工程骨架。项目目标是在用户授权访问文件的前提下，提供分享链接识别、WebView 脚本注入、下载链接捕获和多线程 Range 下载能力。

> 合规声明：本项目仅用于下载用户有权访问的文件，不用于绕过版权、权限控制或平台规则。

## 当前已实现

- 首页输入/接收分享链接
- WebView 打开网盘页面
- 域名识别和脚本注入入口
- `window.MMM` JSBridge
- `flutter_inappwebview.callHandler` 兼容层
- 最小 GM API Shim：`GM_info`、`unsafeWindow`、`GM_setValue`、`GM_getValue`、`GM_xmlhttpRequest` 桥接入口
- JS 侧通用下载链接捕获器
- 下载任务模型
- `HttpURLConnection` Range 多线程下载骨架
- GitHub Actions 自动构建 Debug APK

## 技术栈

- Android Gradle Plugin 8.7.3
- Gradle Wrapper 8.10.2
- Java 17 源码兼容级别
- compileSdk / targetSdk 35
- 原生 Android Java 工程

## 目录结构

```text
.
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/scripts/
│       ├── java/com/mmm/pan/
│       └── res/
├── gradle/wrapper/
├── .github/workflows/android-build.yml
├── build.gradle
├── settings.gradle
└── README.md
```

## 本地构建

环境要求：

- JDK 17 或更高版本，推荐 21
- Android SDK，包含 compileSdk 35

构建 Debug APK：

```bash
chmod +x ./gradlew
./gradlew assembleDebug --stacktrace
```

构建产物路径：

```text
app/build/outputs/apk/debug/*.apk
```

## CI/CD

仓库已配置 GitHub Actions：

- push 到 `main` 自动构建
- 支持手动触发 `workflow_dispatch`
- 自动上传 Debug APK artifact

Actions 页面：

```text
https://github.com/1naer/mmmmm/actions
```

## 后续路线

详见 [`docs/ROADMAP.md`](docs/ROADMAP.md)。

优先级较高的方向：

1. 接入完整 LinkSwift/定制网盘脚本。
2. 完善 `GM_xmlhttpRequest` 的异步回调。
3. 优先适配百度网盘。
4. 下载器加入数据库、前台服务和通知。
5. 再接 aria2c / Gopeed core。

## 许可证

当前仓库尚未声明开源许可证。正式分发或协作前，建议根据预期使用方式补充 LICENSE 文件。
