# 导航系统 (Navigation System)

## 功能概述

使用 Jetpack Navigation Compose 实现页面间的导航，包含底部导航栏（三标签页）和内部子页面导航。支持页面切换动画和条件性隐藏底部导航栏。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| MainScreen | `presentation/MainScreen.kt` | 主导航器 + 底部导航 + 对话框 (388行) |
| QuickWriterApp | `presentation/QuickWriterApp.kt` | 应用入口 Composable (32行) |
| SettingsScreen | `presentation/ui/screens/SettingsScreen.kt` | 设置页内部子导航 |

## 核心类/函数

### Screen 路由定义
```kotlin
sealed class Screen(val route: String) {
    object ProjectList : Screen("project_list")
    object ProjectCreate : Screen("project_create")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }
    object ProjectEdit : Screen("project_edit/{projectId}") {
        fun createRoute(projectId: String) = "project_edit/$projectId"
    }
    object Writing : Screen("writing")
    object Settings : Screen("settings")
}
```

### NavHost 结构
```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.ProjectList.route
) {
    composable("project_list") { ... }                    // 项目列表
    composable("project_create") { ... }                  // 项目创建
    composable("project_detail/{projectId}") { ... }      // 项目详情
    composable("project_edit/{projectId}") { ... }        // 项目编辑
    composable("writing") { ... }                         // 写作页面
    composable("settings") { ... }                        // 设置页面
}
```

### 底部导航项
| 标签 | 图标 | 路由 | selectedTab |
|------|------|------|-------------|
| 项目 (Projects) | `Icons.Default.Home` | `project_list` | 0 |
| 写作 (Writing) | `Icons.Default.Edit` | `writing` | 1 |
| 设置 (Settings) | `Icons.Default.Settings` | `settings` | 2 |

## 设计架构

```
┌────────────────────────────────────────────────┐
│                  QuickWriterApp                 │
│  LaunchedEffect → 1秒延迟 → Crossfade            │
│  ┌──────────┐     ┌──────────┐                 │
│  │ SplashScr│ →→→ │MainScreen│                 │
│  └──────────┘     └──────────┘                 │
└───────────────────────┬────────────────────────┘
                        │
┌───────────────────────┴────────────────────────┐
│                  MainScreen                      │
│                                                  │
│  Scaffold(                                       │
│    bottomBar = NavigationBar (条件显示)            │
│  ) {                                             │
│    NavHost(startDestination = "project_list") {  │
│      ├─ project_list     → ProjectListScreen     │
│      ├─ project_create   → ProjectCreateScreen   │
│      ├─ project_detail   → ProjectDetailScreen   │
│      ├─ project_edit     → ProjectEditScreen     │
│      ├─ writing          → WritingScreen         │
│      └─ settings         → SettingsScreen        │
│    }                                             │
│  }                                               │
│                                                  │
│  ProjectActionDialog (长按触发)                    │
│  DeleteConfirmDialog (删除确认)                    │
└──────────────────────────────────────────────────┘
```

## 页面路由与底部导航栏关系

| 路由 | 页面 | 底部导航栏 | 动画 |
|------|------|-----------|------|
| `project_list` | ProjectListScreen | ✅ 显示 | 滑出/滑入 (水平) |
| `project_create` | ProjectCreateScreen | ❌ 隐藏 | 无 |
| `project_detail/{projectId}` | ProjectDetailScreen | ❌ 隐藏 | 滑入 (水平, 从右) |
| `project_edit/{projectId}` | ProjectEditScreen | ❌ 隐藏 | 无 |
| `writing` | WritingScreen | ✅ 显示 | 无 |
| `settings` | SettingsScreen | ✅ 显示 | 无 |

底部导航栏在 `project_create` 和 `project_edit` 页面时隐藏：
```kotlin
bottomBar = {
    if (currentRoute != Screen.ProjectCreate.route && 
        currentRoute != Screen.ProjectEdit.route) {
        NavigationBar { ... }
    }
}
```

## 数据流

### 导航跳转
```
用户点击项目卡片
    │
    ▼
onProjectClick = { projectId ->
    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
}
    │
    ▼
NavHost composable 匹配 "project_detail/{projectId}"
    │
    ▼
backStackEntry.arguments.getString("projectId")
    │
    ▼
创建 ProjectDetailViewModel，渲染 ProjectDetailScreen
```

### 底部标签切换
```
用户点击"写作"标签
    │
    ▼
selectedTab = 1
navController.navigate(Screen.Writing.route) {
    popUpTo(Screen.ProjectList.route)
}
    │
    ▼
NavHost composable 匹配 "writing"
    │
    ▼
创建 WritingViewModel，渲染 WritingScreen
```

### 设置页面子导航
与主页面导航不同，设置页面使用**内部状态**而非 NavController 管理子页面：

```
SettingsScreen
    │
    ▼
var currentSubScreen: SettingsSubScreen = Main
    │
    ├─ Main → SettingsMainScreen
    ├─ AiConfigList → AiConfigListScreen
    ├─ AiConfigEdit → AiConfigEditScreen
    ├─ WritingSettings → WritingSettingsScreen
    ├─ AppSettings → AppSettingsScreen
    └─ About → AboutScreen
```

## 关键实现细节

### 页面切换动画
```kotlin
// 项目列表退出/进入
composable(
    route = "project_list",
    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) }
)

// 项目详情进入/退出
composable(
    route = "project_detail/{projectId}",
    enterTransition = { slideInHorizontally(initialOffsetX = { it }) },   // 从右滑入
    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }  // 向右滑出
)
```

### ViewModel 创建
每个页面通过 factory 创建 ViewModel：
```kotlin
val projectListViewModel: ProjectListViewModel = viewModel(
    factory = ProjectListViewModelFactory(
        appContainer.projectManagementUseCase,
        appContainer.settingsUseCase
    )
)
```

### 删除对话框
项目长按删除的对话框在 `MainScreen` 层级管理（不在 `ProjectListScreen` 内部），通过 Compose 状态变量 `showActionDialog` 和 `showDeleteConfirmDialog` 控制。

### 返回栈管理
- 底部标签切换使用 `popUpTo(ProjectList.route)` 避免无限制入栈
- `project_list` 导航使用 `popUpTo(...) { inclusive = true }` 确保单实例
- `project_edit` 导航会 `popUpTo(ProjectList.route)` 清理中间栈

## 已知问题/技术债务

1. 底部导航栏的 `selectedTab` 状态与 NavController 的 `currentRoute` 可能不同步
2. 设置页面的子导航使用内部状态管理，无法与系统返回键深度集成
3. 缺少深度链接支持
4. `PlaceholderScreen` 为遗留组件，未被实际使用
