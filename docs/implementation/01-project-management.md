# 项目管理 (Project Management)

## 功能概述

提供小说项目的完整生命周期管理：创建、查看列表、查看详情、编辑、删除和排序。支持当前项目标记、封面图片管理和 ZIP 导出。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| ProjectListScreen | `presentation/ui/screens/ProjectListScreen.kt` | 项目列表 UI |
| ProjectCreateScreen | `presentation/ui/screens/ProjectCreateScreen.kt` | 项目创建表单 UI |
| ProjectEditScreen | `presentation/ui/screens/ProjectEditScreen.kt` | 项目编辑表单 UI |
| ProjectDetailScreen | `presentation/ui/screens/ProjectDetailScreen.kt` | 项目详情 UI (681行) |
| ProjectCard | `presentation/ui/components/ProjectCard.kt` | 项目卡片组件 |
| ProjectCoverImage | `presentation/ui/components/ProjectCoverImage.kt` | 封面图片加载组件 |
| ProjectListViewModel | `presentation/viewmodel/ProjectListViewModel.kt` | 项目列表状态管理 |
| ProjectCreateViewModel | `presentation/viewmodel/ProjectCreateViewModel.kt` | 创建表单状态管理 |
| ProjectEditViewModel | `presentation/viewmodel/ProjectEditViewModel.kt` | 编辑表单状态管理 |
| ProjectDetailViewModel | `presentation/viewmodel/ProjectDetailViewModel.kt` | 详情页状态管理 |
| ProjectManagementUseCase | `domain/usecase/ProjectManagementUseCase.kt` | 项目管理业务逻辑 (261行) |
| ProjectRepository | `data/repository/ProjectRepository.kt` | 项目数据仓库 |
| ProjectDao | `data/local/dao/ProjectDao.kt` | Room DAO |
| ProjectEntity | `data/local/entity/ProjectEntity.kt` | 数据库实体 |

## 核心类/函数

### 数据实体
```kotlin
// ProjectEntity.kt
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,           // UUID
    val title: String,
    val author: String,
    val genre: String,                     // 15 种小说类型之一
    val description: String?,
    val createdTime: Long,                // 时间戳
    val modifiedTime: Long,               // 最后修改时间
    val status: String,                   // 项目状态
    val coverImagePath: String?,          // 封面图片路径
    val wordCount: Int = 0,              // 总字数
    val chapterCount: Int = 0,           // 章节数
    val storagePath: String              // 文件系统存储路径
)
```

### DAO 排序查询
- `getAllProjects()` → `ORDER BY modified_time DESC`
- `getAllProjectsByCreatedTime()` → `ORDER BY created_time DESC`
- `getAllProjectsByTitle()` → `ORDER BY title ASC`

### UiState 密封类
```kotlin
// ProjectListUiState
sealed class ProjectListUiState {
    object Loading : ProjectListUiState()
    object Empty : ProjectListUiState()
    data class Success(val projects: List<ProjectEntity>) : ProjectListUiState()
    data class Error(val message: UiText) : ProjectListUiState()
}
```

### 排序枚举
```kotlin
// ProjectManagementUseCase.kt
enum class SortOption {
    MODIFIED_TIME,  // 按修改时间
    CREATED_TIME,   // 按创建时间
    TITLE           // 按名称
}
```

## 设计架构

```
┌──────────────────────────────────────────┐
│           Presentation Layer             │
│  ProjectListScreen / Create / Edit / Detail │
│  ViewModels (List/Create/Edit/Detail)    │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────┴───────────────────────┐
│           Domain Layer                    │
│  ProjectManagementUseCase                │
│  (验证、编排、文件+DB操作协调)              │
└──────────────────┬───────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     │             │             │
┌────┴────┐ ┌──────┴──────┐ ┌───┴──────────┐
│Project  │ │ FileManager │ │SettingsUseCase│
│Repository│ │             │ │(currentProject│
│(Room DB)│ │(文件系统)    │ │  管理)       │
└─────────┘ └─────────────┘ └──────────────┘
```

## 数据流

### 项目列表加载
1. `ProjectListViewModel.init` 订阅 `sortOption` 和 `currentProjectId` 两个 Flow
2. 通过 `combine` 触发 `useCase.getSortedProjects(sortOption, currentProjectId)`
3. UseCase 内部通过 `baseFlow.map { ... }` 将当前项目置顶
4. DAO 根据排序选项执行不同 SQL 查询，返回 `Flow<List<ProjectEntity>>`

### 项目创建
1. 用户填写表单 → `updateTitle()`/`updateAuthor()` 触发实时验证
2. `validateFormData()` 调用 `useCase.validateProjectTitle()` 和 `validateProjectAuthor()`
3. 点击"创建项目" → `createProject()` 设置 Loading 状态
4. UseCase: `ProjectRepository.createProject()` 插入数据库 → 成功则 `FileManager.createProjectDirectoryStructure()` 创建目录 → `FileManager.createInfoJson()` 写入 info.json
5. 如果目录创建失败，回滚数据库记录（删除刚插入的项目）
6. Success 状态 → `LaunchedEffect` 触发 `onNavigateBack()`

### 项目删除
1. 长按项目卡片 → 显示 `ProjectActionDialog`（编辑/删除两个按钮）
2. 点击删除 → 显示 `DeleteConfirmDialog`（确认对话框，显示项目名称）
3. 确认 → `viewModel.deleteProject(projectId)`
4. UseCase 先调用 `fileManager.deleteProject(projectId)` 删除文件目录
5. 再调用 `projectRepository.deleteProject(projectId)` 删除数据库记录
6. 如果被删除项目是当前项目，`settingsUseCase.setCurrentProjectId(null)` 清除

### 项目排序
- 三种排序选项存储在 `ProjectListViewModel._sortOption: MutableStateFlow<SortOption>`
- 切换排序调用 `updateSortOption(sortOption)` 更新 StateFlow
- `combine(sortOption, currentProjectId)` 自动触发重新查询
- 当前项目始终置顶（通过 `baseFlow.map { projects -> ... }` 实现）

## 关键实现细节

### 存储路径规范
**必须使用数据库中的 `ProjectEntity.storagePath`，禁止在运行时重新计算项目目录路径。**

原因：`FileManager.getProjectDirectory(projectId)` 基于 `context.filesDir` 实时计算路径，若 `filesDir` 因应用重装、系统升级等原因变化，会与数据库创建时记录的 `storagePath` 不一致。

| 场景 | ✅ 正确做法 | ❌ 禁止做法 |
|------|------------|-----------|
| ZIP 导出 | `project.storagePath` → `File(project.storagePath)` | `getProjectDirectory(projectId)` (会创建空目录) |
| 文件浏览器 | `project.storagePath` (当前已正确) | — |
| 章节文件操作 | `File(project.storagePath, "正文/...")` | `getProjectDirectory(projectId)` |

### 封面图片处理
- 通过系统文件选择器（`ActivityResultContracts.OpenDocument`）选择图片
- MIME 类型限制：`image/jpeg`, `image/png`, `image/bmp`, `image/x-bmp`
- URI 通过 `takePersistableUriPermission` 持久化
- `CoverImageProcessor.saveCoverImage()` 处理：解码 → 缩放至 600x800 白色画布居中 → 保存为 JPEG 质量 90
- 封面文件固定为 `{projectDir}/cover.jpg`

### ZIP 导出
- 使用 `project.storagePath` 定位项目目录
- 走查目录树 (`walkTopDown`) 收集所有文件
- 若目录为空，先调用 `createDirectoryStructureAt()` 就地补建目录结构
- 若缺少 `info.json`，自动补建
- 创建临时 ZIP 文件 → 通过 SAF (Storage Access Framework) 复制到用户选择的位置
- `finally` 块中清理临时文件

### 项目详情页
- 支持设置/取消当前项目（"设为当前项目"→"取消当前项目"切换）
- 封面管理：添加/替换/删除封面，使用 `CoverMenuDialog` 根据是否已有封面条件显示
- ZIP 导出带进度遮罩覆盖层（`Exporting` 状态 → `ExportSuccess`/`ExportError`）
- 项目信息卡：书名、作者、类型、描述、相对时间、字数、章节数

## 已知问题/技术债务

1. 项目删除使用 `AlertDialog`（Compose 框架内），在低性能设备上可能出现交互延迟
2. 编辑页面检测表单变更的逻辑 (`isFormDataChanged()`) 比较所有字段，未使用 `data class copy()` 直观对比
3. 封面图片缺少缓存机制，每次进入详情页都重新检查文件是否存在
