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

### 项目结构
```
com.universe_st.quickwriter/
├── presentation/
│   ├── ui/
│   │   ├── components/      # 可复用 UI 组件
│   │   └── screens/         # 页面级 Composable (10 个页面)
│   └── viewmodel/           # ViewModels (5 个)
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
└── util/                    # 工具类
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
- 关于界面（AboutScreen）
- 数据验证和错误处理基本框架
- 项目删除功能（长按交互 + 确认对话框）
- 启动界面和闪屏（SplashScreen, QuickWriterApp）
- 全部 5 个 ViewModel 实现（含工厂类）
- Room TypeConverters 支持（List\<String\> 转换）
- 项目排序功能（按创建时间、修改时间、名称）

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

#### 文件操作
- 使用 `FileManager` 进行所有文件操作
- 统一使用 UTF-8 编码
- 文件路径必须使用路径验证，防止目录遍历攻击
- 所有文件操作必须是线程安全的

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
- `presentation/ui/screens/SettingsScreen.kt`: 系统设置界面（含内部导航）
- `presentation/ui/screens/AppSettingsScreen.kt`: 外观与字体设置
- `presentation/ui/screens/WritingSettingsScreen.kt`: 写作设置界面
- `presentation/ui/screens/AiConfigScreen.kt`: AI模型配置界面
- `presentation/ui/screens/AboutScreen.kt`: 关于界面
- `presentation/ui/components/ProjectCard.kt`: 项目卡片组件
- `presentation/ui/components/SettingsComponents.kt`: 设置页面组件
- `presentation/viewmodel/ProjectListViewModel.kt`: 项目列表 ViewModel
- `presentation/viewmodel/ProjectCreateViewModel.kt`: 项目创建 ViewModel
- `presentation/viewmodel/ProjectEditViewModel.kt`: 项目编辑 ViewModel
- `presentation/viewmodel/ProjectDetailViewModel.kt`: 项目详情 ViewModel
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

## 项目文档

- **需求文档**: `docs/requirement.md` - 完整的产品需求和技术规范
- **第一期需求**: `docs/第一期需求.md` - 第一期开发计划和进度
- **依赖清单**: `docs/依赖清单.md` - 使用的依赖库列表

## 联系方式

如有问题或建议，请更新此文档或直接在项目中提出。

---

**文档版本**: 2.1  
**最后更新**: 2026-04-23  
**适用范围**: AI Agents 和开发团队
