# QuickWrite

一款专为长篇小说创作者设计的 Android 原生应用，通过 AI 助理功能帮助作家系统化地管理复杂的小说创作过程。

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-green.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-purple.svg)]()

## 核心功能

- **项目管理** — 创建、编辑、删除小说项目，支持封面图片和排序
- **写作编辑器** — 基于 Markor HighlightingEditor 移植，支持语法高亮、行号、自动格式化
- **章节管理** — YAML Front Matter 元数据格式，支持分卷和排序
- **AI 模型配置** — 管理多个 AI 服务商和模型配置
- **文件系统** — 每项目独立文件目录，正文/设定/时间线分类管理
- **多语言** — 简体中文、繁体中文、英文三语切换
- **深蓝色主题** — 完整的 Material Design 3 主题系统

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + 手动依赖注入 |
| 数据库 | Room + DataStore |
| 网络 | Retrofit + OkHttp |
| 异步 | Kotlin 协程 + Flow |
| 代码生成 | KSP |
| 编辑器 | markor-editor（移植自 Markor） |

## 构建

```bash
# Debug 构建并安装
./gradlew installDebug

# 编译检查（不安装）
./gradlew :app:assembleDebug

# Release 构建
./gradlew assembleRelease

# 清理
./gradlew clean
```

### 环境要求

- Android Studio 最新版
- JDK 11+
- Android SDK API 36
- Gradle 9.2+

## 项目结构

```
com.universe_st.quickwriter/
├── presentation/
│   ├── ui/components/    # 可复用 UI 组件
│   ├── ui/screens/       # 页面级 Composable（10 个页面）
│   └── viewmodel/        # ViewModels（7 个）
├── domain/
│   ├── usecase/          # 业务逻辑层
│   └── model/            # 领域模型
├── data/
│   ├── local/database/   # Room 数据库 + TypeConverters
│   ├── local/dao/        # 数据访问对象
│   ├── local/entity/     # 数据库实体
│   └── repository/       # 数据仓库
├── di/                   # 依赖注入（AppContainer）
├── util/                 # 工具类
└── markor-editor/        # 编辑器独立模块（Android Library）
```

## 文件系统

每个项目使用独立目录结构：

```
/{项目ID}/
├── info.json             # 项目元数据
├── 正文/                 # 小说章节
├── 设定/
│   ├── 人物/
│   ├── 地点/
│   ├── 组织/
│   └── 物品/
├── 时间线/
├── 记录/
└── 配置/
    ├── AI指令.md
    └── 写作规范.md
```

章节文件使用 YAML Front Matter 格式：

```markdown
---
title: "第一章：初入异世"
order: 1
volume: "第一卷：风云初起"
summary: "主角醒来发现自己穿越到了异世界"
---

正文内容...
```

## 开发状态

**完成度：80%** | 第二期开发中

已完成：项目管理、写作编辑器、章节管理、AI 模型配置、设置系统、主题系统、文件系统、导航、国际化、启动页

进行中：AI 对话 UI、单元测试

待开发：Markdown 预览、设定管理界面、时间线管理、导出分享

## 许可证

[Mozilla Public License Version 2.0](LICENSE)
