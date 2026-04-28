# 依赖注入 (Dependency Injection)

## 功能概述

使用手动依赖注入模式管理应用中的单例依赖。所有依赖通过 `AppContainer` 集中创建和管理，使用 Kotlin `by lazy` 延迟初始化。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| AppContainer | `di/AppContainer.kt` | 依赖注入容器 (56行) |
| QuickWriteApplication | `QuickWriteApplication.kt` | Application 类，持有 AppContainer |
| MainActivity | `MainActivity.kt` | 通过 Application 获取 AppContainer |
| MainScreen | `presentation/MainScreen.kt` | 通过 AppContainer 创建 ViewModel |

## 依赖图

```
AppContainer(context: Context)
│
├─ AppDatabase (singleton, DCL)
│   ├─ ProjectDao
│   ├─ AiModelConfigDao
│   └─ UserSettingDao
│
├─ FileManager(context)
│
├─ ProjectRepository(projectDao)
│
├─ AiModelConfigRepository(aiModelConfigDao)
│
├─ UserSettingsRepository(userSettingDao)
│
├─ ProjectManagementUseCase(projectRepository, fileManager)
│
└─ SettingsUseCase(userSettingsRepository, aiModelConfigRepository)
```

## AppContainer 实现

```kotlin
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val projectDao: ProjectDao by lazy { database.projectDao() }
    val aiModelConfigDao: AiModelConfigDao by lazy { database.aiModelConfigDao() }
    val userSettingDao: UserSettingDao by lazy { database.userSettingDao() }

    val fileManager: FileManager by lazy { FileManager(context) }

    val projectRepository: ProjectRepository by lazy { ProjectRepository(projectDao) }
    val aiModelConfigRepository: AiModelConfigRepository by lazy { AiModelConfigRepository(aiModelConfigDao) }
    val userSettingsRepository: UserSettingsRepository by lazy { UserSettingsRepository(userSettingDao) }

    val projectManagementUseCase: ProjectManagementUseCase by lazy {
        ProjectManagementUseCase(projectRepository, fileManager)
    }
    val settingsUseCase: SettingsUseCase by lazy {
        SettingsUseCase(userSettingsRepository, aiModelConfigRepository)
    }
}
```

所有属性使用 `by lazy` 延迟初始化，按依赖顺序自动解析。

## ViewModel 工厂

每个 ViewModel 都有对应的工厂类，从 AppContainer 获取 UseCase 后创建 ViewModel：

```kotlin
class ProjectListViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProjectListViewModel(projectManagementUseCase, settingsUseCase) as T
    }
}
```

### ViewModel 工厂依赖关系

| ViewModel | 所需 UseCase |
|-----------|------------|
| ProjectListViewModel | ProjectManagementUseCase + SettingsUseCase |
| ProjectCreateViewModel | ProjectManagementUseCase |
| ProjectEditViewModel | ProjectManagementUseCase |
| ProjectDetailViewModel | ProjectManagementUseCase + SettingsUseCase |
| WritingViewModel | ProjectManagementUseCase + SettingsUseCase |
| SettingsViewModel | SettingsUseCase |

## 数据流

### 初始化
```
QuickWriteApplication.onCreate()
    │
    ▼
appContainer = AppContainer(applicationContext)
    │
    ▼
所有依赖延迟初始化（首次访问时才创建）
```

### 使用
```
MainScreen Composable
    │
    ▼
val appContainer = (context.applicationContext as QuickWriteApplication).appContainer
    │
    ▼
viewModel(factory = XxxViewModelFactory(appContainer.xxxUseCase, appContainer.yyyUseCase))
```

## 关键实现细节

### 对比 Hilt
项目在 `libs.versions.toml` 中声明了 Hilt 依赖，但**当前未启用**。使用手动 DI 的原因：
- 避免引入额外的编译时注解处理
- 减少 Hilt 的学习曲线
- 依赖数量有限，手动管理足够清晰

### Context 获取
AppContainer 持有 `applicationContext` 引用，确保不会泄漏 Activity：
```kotlin
AppContainer(applicationContext)  // 在 Application.onCreate() 中创建
```

### 线程安全
- `AppDatabase` 使用 DCL 单例（`@Volatile` + `synchronized`）
- AppContainer 的 `by lazy` 在 Kotlin 中默认线程安全
- ViewModel 通过 Compose 的 `viewModel()` 自动管理

### 延迟初始化优势
- 只有实际使用的依赖才会被创建
- 数据库在首次访问 DAO 时才初始化（通过 lazy 链式触发）

## 已知问题/技术债务

1. Hilt 依赖已在 `libs.versions.toml` 中声明但未使用，存在冗余配置
2. 手动 DI 在依赖增多后管理成本上升，建议在未来迁移到 Hilt
3. `FileManager` 持有 `Context` 引用，但通过 `applicationContext` 间接避免了泄漏
4. ViewModel 工厂类代码重复度高，可以用泛型工厂简化
