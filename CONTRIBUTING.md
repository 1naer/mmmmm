# Contributing

感谢你关注 mmm。当前项目仍处于早期阶段，欢迎通过 Issue、Pull Request 或测试反馈参与完善。

## 开发环境

- Android Studio Ladybug 或更高版本
- JDK 17
- Gradle Wrapper：请优先使用仓库内的 `./gradlew`

## 本地构建

```bash
./gradlew assembleDebug
```

## 代码约定

- Java 源码保持简单、可读，优先避免引入重量级依赖。
- 下载、脚本注入、网盘适配逻辑应尽量模块化，避免把大量业务逻辑塞进 Activity。
- 修改 WebView/JS Bridge 相关逻辑时，请特别注意安全边界和异常处理。
- 提交前建议至少执行一次 `./gradlew assembleDebug`。

## 适配新网盘脚本

1. 在 `app/src/main/assets/scripts/` 下新增对应脚本。
2. 在 `ScriptInjector` 中按域名注入。
3. 统一通过 `MMM_NATIVE.postMessage(JSON.stringify({ type: 'download_links', payload: { files } }))` 返回候选下载任务。
4. `files` 中建议包含：`url`、`name`、`size`、`headers`、`source`。

## Pull Request 建议

- 一个 PR 聚焦一个主题。
- 说明变更目的、测试方式和潜在风险。
- UI 或下载行为变化建议附上截图/日志。
