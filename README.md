# AnvilCraft-DingDongJi
铁砧工艺-叮咚叽
致力于铁砧工艺护甲，工具，锻造相关的模组

## 构建

每次推送到 `main` 时，GitHub Actions 会自动用 JDK 21 编译。完成后：

- 打开仓库 **Actions** 页面，进入最近一次 **Build**，下载产物 `anvilcraft-dingdongji`
- 若 `build.gradle` 里的版本（当前 `0.0.7`）还没有对应标签，会自动打 `v0.0.7` 并发布到 **Releases**
- 发新版时改 `build.gradle` 和 `neoforge.mods.toml` 的 `version` 再推送即可

本地：

```bat
gradlew.bat build
```

产物在 `build/libs/`。需要对照铁砧工艺源码编译时，把对应 1.6.x 的 jar 放到 `libs/`。

