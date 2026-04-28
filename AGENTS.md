# QuickWrite AGENTS.md

本文档为 AI agents 提供与 QuickWrite 项目协作的指南和规范。

## 项目概述

**QuickWrite** 是一款专为长篇小说创作者设计的 Android 原生应用程序，旨在通过 AI 助理功能帮助作家系统化地管理复杂的小说创作过程。

### 技术栈
- **平台**: Android (API 24+)
- **开发语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material Design 3
- **架构模式**: MVVM (Model-View-ViewModel)
- **本地存储**: Room 数据库 + DataStore + 文件系统
- **网络请求**: Retrofit + OkHttp
- **异步处理**: Kotlin 协程 + Flow
- **依赖管理**: 手动依赖注入 (AppContainer)
- **主题**: 深蓝色主题 (#1a237e) + 完整主题系统
- **代码处理**: KSP (Kotlin Symbol Processing) 替代 kapt
- **编辑器引擎**: markor-editor 模块 (基于 Markor HighlightingEditor 移植)

### 项目结构
```
com.universe_st.quickwriter/
├── presentation/
│   ├── ui/
│   │   ├── components/      # 可复用 UI 组件
│   │   └── screens/         # 页面级 Composable (10 个页面)
│   └── viewmodel/           # ViewModels (7 个)
├── domain/
│   ├── usecase/             # 业务逻辑层
│   └── model/               # 领域模型（当前直接使用 entity）
├── data/
│   ├── local/
│   │   ├── database/        # Room 数据库 + TypeConverters
│   │   ├── dao/             # 数据访问对象
│   │   └── entity/          # 数据库实体
│   └── repository/          # 数据仓库
├── di/                      # 依赖注入
├── util/                    # 工具类
└── markor-editor/           # Markor 移植编辑器模块（独立 Android Library）
```

## AI Agent 协作指南

### 当前开发状态

**完成度**: 70% | **状态**: 第二期开发中

#### 已完成功能 ✅
- 项目基础架构搭建（AppContainer, AppDatabase）
- MVVM 架构实现（手动依赖注入）
- Room 数据库和表结构设计（ProjectEntity, AiModelConfigEntity, UserSettingEntity）
- 项目列表页面 UI 和功能（ProjectListScreen, ProjectCard）
- 项目创建页面 UI 和功能（ProjectCreateScreen）
- 项目编辑页面 UI 和功能（ProjectEditScreen）
- 项目详情查看页面 UI 和功能（ProjectDetailScreen, ProjectDetailViewModel）
- 项目数据管理（增删改查核心逻辑）
- 完整的深蓝色主题系统配置（Theme.kt, Color.kt, Type.kt）
- 文件系统管理工具（FileManager）
- 封面图片处理工具（CoverImageProcessor）
- 底部导航和页面路由（MainScreen, Navigation, 10 个路由页面）
- Material Design 3 UI 规范完整实现
- 系统设置界面（SettingsScreen, AppSettingsScreen）
- AI 模型配置管理系统（AiConfigScreen, AiModelConfigRepository）
- 用户设置数据管理（UserSettingsRepository, SettingsUseCase）
- 写作设置界面（WritingSettingsScreen）
- 写作编辑器页面（WritingScreen，基于 markor-editor）
- 文件浏览器页面（FileBrowserScreen，支持章节文件管理）
- 关于界面（AboutScreen）
- 数据验证和错误处理基本框架
- 项目删除功能（长按交互 + 确认对话框）
- 启动界面和闪屏（SplashScreen, QuickWriterApp）
- 全部 7 个 ViewModel 实现（含工厂类）
- Room TypeConverters 支持（List\<String\> 转换）
- 项目排序功能（按创建时间、修改时间、名称）
- markor-editor 模块导入（基于 Markor HighlightingEditor 移植，支持语法高亮、行号、自动格式化）

#### 进行中功能 🚧
- AI 写作辅助功能集成
- 网络请求层实现（AI模型配置与 API 集成）
- 单元测试和集成测试编写

#### 待开发功能 ⏳
- Markdown 编辑器
- 文档管理系统（文件管理界面）
- 正文章节编辑器
- 小说设定管理界面（人物/地点/组织/物品）
- 时间线管理功能
- AI对话和写作助手界面
- 导出和分享功能

## 工作规范

### 1. 代码风格

#### Kotlin 代码规范
- 使用 Kotlin 官方编码规范
- 遵循 Android Kotlin 最佳实践
- 使用有意义的命名
- 避免使用 `!!` 非空断言，优先使用安全调用 `?.`
- 使用数据类 (`data class`) 定义数据模型
- 使用密封类 (`sealed class`) 定义状态和类型

#### Jetpack Compose 规范
- 遵循 Compose 编码最佳实践
- 使用 `@Composable` 注解标记 UI 组件
- 遵循单一职责原则，每个 Composable 只负责一个功能
- 使用命名参数提高代码可读性
- 状态提升：将状态提升到合适的层级
- 使用 `remember` 和 `mutableStateOf` 管理状态

#### 命名规范
- UI 组件: `PascalCase` (例: `ProjectCard`, `ProjectListScreen`)
- 函数和方法: `camelCase` (例: `createProject`, `updateFile`)
- 变量和常量: `camelCase`
- 常量: `UPPER_SNAKE_CASE` (例: `MAX_RETRY_COUNT`)
- 数据库表: 小写下划线 (例: `projects`, `ai_conversations`)

### 2. 架构规范

#### MVVM 分层
- **Presentation Layer**: 包含 Composable UI 组件和 ViewModels
- **Domain Layer**: 包含 UseCases 和领域模型
- **Data Layer**: 包含 Repositories、数据源和数据库访问

#### 依赖注入
- 使用手动依赖注入（AppContainer）
- 所有依赖通过 AppContainer 管理
- 避免直接在 ViewModels 中创建 Repository 实例

#### 数据流
- 使用 Flow 进行响应式数据流
- 使用 StateFlow 管理单一来源的状态
- 使用协程处理异步操作
- 所有 IO 操作必须在 `suspend` 函数或协程作用域中执行

### 3. 文件系统规范

#### 项目目录结构
每个项目都有独立的文件系统目录，遵循以下结构：

```
/{项目ID}/
├── info.json                # 项目元数据（书名、作者、类型、创建时间等）
├── 简介.md                  # 项目基本信息
├── 正文/                    # 小说正文章节
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

#### info.json 格式
项目根目录下的 `info.json` 存储项目元数据，新建项目时自动生成，导出时若缺失则自动补建。

```json
{
  "title": "书名",
  "author": "作者",
  "genre": "类型",
  "createdTime": "2026-04-28T12:00:00.000Z",
  "version": "1.0"
}
```

- `createdTime` 使用 ISO 8601 UTC 格式
- `version` 为格式版本号，当前固定 `"1.0"`
- 生成位置：`FileManager.createInfoJson()` 方法，分别由项目创建（`ProjectManagementUseCase.createProject`）和 ZIP 导出（`ProjectManagementUseCase.exportProjectAsZip`）调用

#### 文件操作
- 使用 `FileManager` 进行所有文件操作
- 统一使用 UTF-8 编码
- 文件路径必须使用路径验证，防止目录遍历攻击
- 所有文件操作必须是线程安全的

#### 项目路径获取规范（重要）
**必须使用数据库中的 `ProjectEntity.storagePath`，禁止在运行时重新计算项目目录路径。**

原因：`FileManager.getProjectDirectory(projectId)` 基于 `context.filesDir` 实时计算路径，若 `filesDir` 因应用重装、系统升级等原因变化，会与数据库创建时记录的 `storagePath` 不一致，导致文件读取/写入指向错误（或不存在的空）目录。

| 场景 | ✅ 正确做法 | ❌ 禁止做法 |
|---|---|---|
| ZIP 导出 | `project.storagePath` → `File(project.storagePath)` | `getProjectDirectory(projectId)` (会创建空目录) |
| 文件浏览器 | `project.storagePath` (当前已正确) | — |
| 章节文件操作 | `File(project.storagePath, "正文/...")` | `getProjectDirectory(projectId)` |
| 目录创建 | 仅项目首次创建时调用 `createProjectDirectoryStructure(projectId)`；导出时如目录为空，调用 `createDirectoryStructureAt(projectDir)` 就地补建 | 重复调用 `getProjectDirectory(projectId)` |

#### 章节文件格式（YAML Front Matter）
每个章节 `.md` 文件使用标准 YAML 前置元数据格式：

```markdown
---
title: "第一章：初入异世"    # 章节标题
order: 1                     # 排序序号
volume: "第一卷：风云初起"   # 分卷信息（可选）
summary: "主角醒来发现自己穿越到了异世界"  # 内容摘要（可选）
---

# 第一章：初入异世

（正文内容...）
```

- 元数据使用 `ChapterFileHelper`（`util/ChapterFileHelper.kt`）解析和构建
- 编辑器中不可直接编辑元数据，加载时自动剥离
- 保存时自动重组元数据与正文
- 排序通过交换 `order` 值实现

### 4. 数据库规范

#### Room 数据库
- 使用 KSP (Kotlin Symbol Processing) 替代 kapt
- 实体类必须使用 `@Entity` 注解
- 主键使用 `@PrimaryKey` 注解
- 复杂类型需要使用 `@TypeConverters`
- DAO 接口使用 `@Dao` 注解
- 数据库类使用 `@Database` 注解
- 使用 Flow 返回数据变化

#### DataStore
- 用户设置使用 DataStore 存储
- 支持键值对存储结构
- 提供类型安全的访问
- 使用协程进行异步操作

#### 数据库迁移
- 当前使用 `fallbackToDestructiveMigration()`（需要修复）
- 未来应该实现正确的数据库迁移策略

### 5. UI 主题规范

#### 颜色方案
- **主色调**: 深蓝色 (#1a237e)
- **辅助色**: 浅蓝色 (#2196f3)
- **强调色**: 橙色 (#ff9800)
- **文本色**: 主要文字 #212121, 次要文字 #757575
- **背景色**: 浅色背景 #ffffff, 深色背景 #121212

#### Material Design 3
- 使用 Material 3 组件和设计规范
- 圆角: 按钮 8dp, 卡片 12dp
- 阴影: 卡片 2dp elevation
- 字体: 英文 Roboto, 中文思源黑体

### 6. 错误处理规范

#### 异常处理
- 使用 `try-catch` 捕获可能抛出的异常
- 不要使用 `printStackTrace()`，使用 Timber 记录日志
- 对用户显示友好的错误消息
- 网络请求必须有超时和重试机制

#### UI 错误状态
- 使用 `Snackbar` 显示简单的错误消息
- 使用对话框显示重要错误
- 使用状态字段处理加载、成功、错误状态

### 7. 国际化 (i18n) 与字符串规范

本项目支持三种语言：**简体中文 (zh-rCN)**、**繁体中文 (zh-rTW)** 和 **英文 (en)**。

#### 字符串资源文件
- `res/values/strings.xml` — 默认语言（英文）
- `res/values-zh-rCN/strings.xml` — 简体中文
- `res/values-zh-rTW/strings.xml` — 繁体中文

#### 硬编码字符串禁令
**绝对禁止**在任何 `.kt` 文件中硬编码用户可见的字符串。所有用户界面文本必须通过 `stringResource()` 从资源文件获取。

#### UiText 类
ViewModel 中的错误消息和成功消息必须使用 `UiText`（位于 `util/UiText.kt`），而非直接使用 `String`。`UiText` 支持两种类型：
- `UiText.DynamicString(value)` — 动态字符串（如异常消息）
- `UiText.StringResource(resId, args...)` — 字符串资源引用

```kotlin
// ✅ 正确 — 使用 UiText.StringResource
_uiState.value = SettingsUiState.Error(
    UiText.StringResource(R.string.error_load_settings_failed)
)

// ✅ 正确 — 使用 UiText.DynamicString 处理异常消息
_uiState.value = ProjectDetailUiState.Error(
    UiText.DynamicString(e.message ?: "Unknown error")
)

// ❌ 错误 — 硬编码字符串
_uiState.value = SettingsUiState.Error(UiText.DynamicString("加载失败"))
```

在 Compose UI 中显示 UiText：
```kotlin
val context = LocalContext.current
Text(text = errorMessage.asString(context))
```

#### UiState 规范
所有 `UiState` 密封类中的 `Error` 和 `Success` 状态必须使用 `UiText` 类型作为消息字段：
```kotlin
sealed class SomeUiState {
    data class Success(val message: UiText) : SomeUiState()
    data class Error(val message: UiText) : SomeUiState()
}
```

#### 新增字符串流程
1. 在 `res/values/strings.xml` 中添加英文字符串
2. 在 `res/values-zh-rCN/strings.xml` 中添加对应简体中文翻译
3. 在 `res/values-zh-rTW/strings.xml` 中添加对应繁体中文翻译
4. 在代码中使用 `stringResource(R.string.xxx)` 或 `UiText.StringResource(R.string.xxx)`

#### 字符串命名规范
- 使用 `snake_case` 命名
- 按功能分组使用前缀：`common_`、`nav_`、`project_`、`writing_`、`file_`、`settings_`、`ai_config_`、`about_`、`error_`、`success_`、`validation_`、`genre_`、`format_`、`splash_`、`time_` 等
- 参数化字符串使用 `%1$s`（字符串）、`%1$d`（整数）、`%.1f`（浮点）等占位符

#### 不可翻译的内容
以下内容**不需要**翻译（保留原始值）：
- 文件系统目录名（如 `正文/`、`设定/`）— 这些是实际文件路径
- 技术符号（如 `?`、`/`、` · `）
- 代码中用作键值的字符串（如 genre 内部存储值、provider 标识符）
- 文件模板内容（如 AI 指令模板）

#### 工具类 Context 依赖
若工具类（如 `AppUtils`、`FileManager`、`CoverImageProcessor`）需要使用字符串资源，须通过参数接收 `Context` 对象，并添加 `import com.universe_st.quickwriter.R`。

## 常用命令

### 构建
```bash
# Debug 构建运行
./gradlew installDebug

# 检查编译是否通过（不安装到设备）
./gradlew :app:assembleDebug

# Release 构建
./gradlew assembleRelease

# 清理构建
./gradlew clean
```

### 测试
```bash
# 运行单元测试
./gradlew test

# 运行 Android 测试
./gradlew connectedAndroidTest
```

### 代码质量
目前项目尚未配置 lint 和 typecheck 命令, 建议配置:

```bash
# Lint 检查 (待配置)
./gradlew lint

# 类型检查 (待配置)
./gradlew kspKotlin
```

## 关键文件说明

### 核心文件
- `di/AppContainer.kt`: 依赖注入容器
- `data/local/database/AppDatabase.kt`: Room 数据库配置
- `util/FileManager.kt`: 文件系统管理工具
- `util/ChapterFileHelper.kt`: 章节文件 YAML Front Matter 解析与构建
- `util/CoverImageProcessor.kt`: 封面图片处理工具
- `util/UiText.kt`: 国际化文本包装类（支持字符串资源与动态字符串）
- `util/AppEditorConfig.kt`: 编辑器配置（常用文件类型、扩展名等）
- `util/LocaleHelper.kt`: 多语言环境切换辅助工具
- `util/AppUtils.kt`: 通用应用工具（类型简称、日期格式化等）
- `data/local/database/Converters.kt`: Room TypeConverters（List&lt;String&gt; 转换）
- `domain/usecase/ProjectManagementUseCase.kt`: 项目管理的业务逻辑
- `domain/usecase/SettingsUseCase.kt`: 设置管理的业务逻辑

### UI 文件
- `presentation/MainScreen.kt`: 主界面和导航
- `presentation/QuickWriterApp.kt`: 应用入口 Composable
- `presentation/ui/screens/SplashScreen.kt`: 启动闪屏
- `presentation/ui/screens/ProjectListScreen.kt`: 项目列表页面
- `presentation/ui/screens/ProjectCreateScreen.kt`: 项目创建页面
- `presentation/ui/screens/ProjectEditScreen.kt`: 项目编辑页面
- `presentation/ui/screens/ProjectDetailScreen.kt`: 项目详情查看页面
- `presentation/ui/screens/WritingScreen.kt`: 写作编辑器页面（基于 markor-editor）
- `presentation/ui/screens/FileBrowserScreen.kt`: 文件浏览器页面
- `presentation/ui/screens/SettingsScreen.kt`: 系统设置界面（含内部导航）
- `presentation/ui/screens/AppSettingsScreen.kt`: 外观与字体设置
- `presentation/ui/screens/WritingSettingsScreen.kt`: 写作设置界面
- `presentation/ui/screens/AiConfigScreen.kt`: AI模型配置界面
- `presentation/ui/screens/AboutScreen.kt`: 关于界面
- `presentation/ui/components/ProjectCard.kt`: 项目卡片组件
- `presentation/ui/components/ProjectCoverImage.kt`: 封面图片加载组件
- `presentation/ui/components/SettingsComponents.kt`: 设置页面组件
- `presentation/viewmodel/ProjectListViewModel.kt`: 项目列表 ViewModel
- `presentation/viewmodel/ProjectCreateViewModel.kt`: 项目创建 ViewModel
- `presentation/viewmodel/ProjectEditViewModel.kt`: 项目编辑 ViewModel
- `presentation/viewmodel/ProjectDetailViewModel.kt`: 项目详情 ViewModel
- `presentation/viewmodel/WritingViewModel.kt`: 写作编辑器 ViewModel
- `presentation/viewmodel/FileBrowserViewModel.kt`: 文件浏览器 ViewModel
- `presentation/viewmodel/SettingsViewModel.kt`: 设置页面 ViewModel

### 数据层文件
- `data/repository/ProjectRepository.kt`: 项目数据仓库
- `data/repository/AiModelConfigRepository.kt`: AI模型配置仓库
- `data/repository/UserSettingsRepository.kt`: 用户设置仓库
- `data/local/dao/ProjectDao.kt`: 项目数据访问对象
- `data/local/dao/AiModelConfigDao.kt`: AI模型配置数据访问对象
- `data/local/dao/UserSettingDao.kt`: 用户设置数据访问对象
- `data/local/entity/ProjectEntity.kt`: 项目数据库实体
- `data/local/entity/AiModelConfigEntity.kt`: AI模型配置数据库实体
- `data/local/entity/UserSettingEntity.kt`: 用户设置数据库实体

## 构建系统配置

### 版本信息
| 组件 | 版本 |
|---|---|
| AGP (Android Gradle Plugin) | 9.2.0 |
| Kotlin | 2.3.10 |
| KSP | 2.3.7 |
| Compose BOM | 2026.02.01 |
| Room | 2.7.2 |
| Retrofit | 2.11.0 |
| OkHttp | 5.0.0-alpha.14 |
| Navigation Compose | 2.8.7 |
| Coil | 2.6.0 |
| DataStore | 1.1.1 |

### 版本目录
项目使用 `gradle/libs.versions.toml` 统一管理依赖版本，所有依赖声明集中在版本目录文件中。

### Maven 仓库
使用阿里云 Maven 镜像加速（Google、Maven Central、Gradle Plugin），同时保留官方仓库作为回退。

### 特殊配置
- **compileSdk**: 36, **minSdk**: 24, **targetSdk**: 35
- **Java**: VERSION_11
- **BuildConfig**: 已启用 (`buildConfig = true`)
- **KSP** 替代 kapt 进行 Room 编译时代码生成
- **Minification**: 当前关闭 (`isMinifyEnabled = false`)
- Hilt 依赖已在版本目录中声明但尚未启用（当前使用手动 DI）

### markor-editor 模块
独立的 Android Library 模块 (`:markor-editor`)，基于 Markor 应用的 HighlightingEditor 移植。

**模块文件结构**:
```
markor-editor/src/main/
├── kotlin/com/universe_st/markor_editor/
│   ├── MarkorEditor.kt              # Compose 包装器
│   └── EditorConfig.kt              # 编辑器配置
├── java/net/gsantner/markor/
│   ├── format/markdown/MarkdownSyntaxHighlighter.java
│   ├── format/plaintext/PlaintextSyntaxHighlighter.java
│   ├── format/general/ColorUnderlineSpan.java
│   └── frontend/textview/
│       ├── HighlightingEditor.java   # 核心编辑器控件
│       ├── AutoTextFormatter.java    # 自动格式化
│       ├── LineNumbersView.java      # 行号显示
│       ├── SyntaxHighlighterBase.java # 语法高亮基类
│       ├── TextViewUtils.java
│       └── TextViewUndoRedo.java     # 撤销/重做
└── java/other/writeily/format/      # 标题 span 样式
```

**依赖**: appcompat 1.7.1, Material 1.13.0, Compose BOM/ui/material3

**参考文档**: `docs/integration/markor-editor集成说明.md` — 详细移植记录

## 开发注意事项

### 1. 当前已知问题
- Room 数据库使用了已弃用的 `fallbackToDestructiveMigration()`
- API 密钥使用 DataStore 存储但未加密
- 项目删除功能的 UI 交互需要优化（长按触发删除）
- 缺少错误处理的详细反馈机制
- AI模型配置与网络请求集成尚未完成

### 2. 技术债务
- 需要实现正确的数据库迁移策略
- 需要添加单元测试和集成测试
- 需要配置代码质量检查工具（lint, detekt）
- 需要实现 API 密钥的安全存储

### 3. 开发优先级
1. AI 写作辅助功能（网络请求层、对话界面）
2. 正文章节编辑器与 Markdown 编辑器
3. 小说设定管理界面（人物/地点/组织/物品）
4. 时间线管理功能
5. 导出和分享功能
6. 单元测试和集成测试
7. 技术债务清理（数据库迁移策略、API 密钥加密、代码质量工具）

## 与 AI 协作的最佳实践

### 1. 提供清晰上下文
在请求时提供:
- 当前功能的目标和需求
- 相关的文件路径和代码片段
- 已有的实现和技术规范
- 预期的结果和行为

### 2. 遵循现有模式
- 参考现有的代码结构实现新功能
- 使用已有的工具类和组件
- 遵循项目的命名和架构规范

### 3. 代码验证
- 在修改代码后运行 `./gradlew :app:assembleDebug` 命令检查编译是否通过
- 参考现有 UI 组件实现新的 UI
- 测试新功能是否符合预期

### 4. 文档更新
在使用 agents 时，如果发现文档不准确，建议更新此文件。

### 5. 功能点实现文档约束

`docs/implementation/` 目录包含各功能点的详细实现文档，**修改代码时必须同步更新相关文档**。

**必须遵守的规则**：
- **修改功能前**：必须先查看 `docs/implementation/README.md` 目录索引，找到对应的功能点实现文档，理解现有实现
- **修改/添加功能后**：必须在同一 PR/commit 中同步更新或新增功能点实现文档
- **保持一致性**：文档中的文件路径、类名、函数名、设计描述必须与实际代码匹配
- **文档更新范围**：
  - 修改了某个功能 → 更新对应的 `docs/implementation/XX-功能名.md`
  - 新增了某个功能 → 在 `docs/implementation/` 下新建文档，并更新 `README.md` 索引
  - 修改了跨功能的架构 → 更新所有受影响的功能文档 + `README.md`

## 项目文档

```
docs/
├── requirements/                  # 需求文档
│   ├── requirement.md             # 完整的产品需求和技术规范
│   └── 编辑器需求文档.md           # 编辑器功能详细需求
├── planning/                      # 开发计划
│   ├── 第一期需求.md               # 第一期开发计划和进度
│   └── 第二期需求.md               # 第二期开发计划和需求
├── technical/                     # 技术文档
│   └── 依赖清单.md                 # 使用的依赖库列表
├── integration/                   # 集成说明
│   └── markor-editor集成说明.md    # Markor 编辑器移植详细记录
└── implementation/                # 功能点实现文档
    ├── README.md                  # 目录索引
    ├── 01-project-management.md   # 项目管理（CRUD、排序）
    ├── 02-writing-editor.md       # 写作编辑器（markor-editor）
    ├── 03-chapter-management.md   # 章节管理（YAML Front Matter）
    ├── 04-ai-model-config.md      # AI 模型配置
    ├── 05-settings-system.md      # 设置系统
    ├── 06-theme-system.md         # 主题系统
    ├── 07-file-system.md          # 文件系统管理
    ├── 08-navigation.md           # 导航系统
    ├── 09-data-layer.md           # 数据层（Room/DAO/Repository）
    ├── 10-dependency-injection.md # 依赖注入（AppContainer）
    ├── 11-internationalization.md # 国际化（UiText/LocaleHelper）
    ├── 12-splash-and-entry.md     # 启动与入口
    └── 13-utilities.md            # 工具类（AppUtils/CoverImageProcessor）
```

## 联系方式

如有问题或建议，请更新此文档或直接在项目中提出。

---

**文档版本**: 2.6  
**最后更新**: 2026-04-28  
**适用范围**: AI Agents 和开发团队
