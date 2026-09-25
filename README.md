# AnvilCraft-DingDongJi
铁砧工艺-叮咚叽
致力于铁砧工艺护甲，工具，锻造相关的模组

## 构建

每次推送到 `main` 时，GitHub Actions 会自动用 JDK 21 编译。完成后：

- 打开仓库 **Actions** 页面，进入最近一次 **Build**，下载产物 `anvilcraft-dingdongji`
- 若 `build.gradle` 里的版本（当前 `0.0.7`）还没有对应标签，会自动打 `v0.0.7` 并发布到 **Releases**
- 发新版时只改 `build.gradle` 的 `version` 再推送即可（`neoforge.mods.toml` 由 `src/main/templates/` 生成，版本会自动展开）

本地：

```bat
gradlew.bat build
```

产物在 `build/libs/`。需要对照铁砧工艺源码编译时，把对应 1.6.x 的 jar 放到 `libs/`。

## 构建脚本结构

- `build.gradle`：只做插件声明、`version` / `group` 和脚本装配
- `dependencies.gradle`：依赖集中声明
- `gradle/libs.versions.toml`：版本目录（NeoForge、ModDevGradle）
- `gradle.properties`：环境与模组元信息
- `gradle/scripts/`：拆分后的构建逻辑
  - `moddevgradle.gradle`：`neoForge {}` 配置与 `run` 任务
  - `repositories.gradle`：仓库声明
  - `jars.gradle`：`jar` 与 `sourcesJar`
  - `publishing.gradle`：Maven 发布
  - `resources.gradle`：从 `src/main/templates` 展开 mod 元数据

`neoforge.mods.toml` 的源文件位于 `src/main/templates/META-INF/`，其中的 `${...}` 占位符在构建时由 `gradle.properties` 和 `build.gradle` 的 `version` 填充。

