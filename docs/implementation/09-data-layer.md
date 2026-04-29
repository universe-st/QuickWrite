# 数据层 (Data Layer)

## 功能概述

使用 Room 数据库进行本地持久化，包含 3 张数据表和对应的 DAO、Repository 层。通过 Flow 提供响应式数据流，支持协程异步操作。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| AppDatabase | `data/local/database/AppDatabase.kt` | Room 数据库配置 (50行) |
| Converters | `data/local/database/Converters.kt` | Room 类型转换器 |
| ProjectEntity | `data/local/entity/ProjectEntity.kt` | 项目表实体 |
| AiModelConfigEntity | `data/local/entity/AiModelConfigEntity.kt` | AI 配置表实体 |
| UserSettingEntity | `data/local/entity/UserSettingEntity.kt` | 用户设置表实体 |
| ProjectDao | `data/local/dao/ProjectDao.kt` | 项目 DAO |
| AiModelConfigDao | `data/local/dao/AiModelConfigDao.kt` | AI 配置 DAO |
| UserSettingDao | `data/local/dao/UserSettingDao.kt` | 设置 DAO |
| ProjectRepository | `data/repository/ProjectRepository.kt` | 项目数据仓库 |
| AiModelConfigRepository | `data/repository/AiModelConfigRepository.kt` | AI 配置数据仓库 |
| UserSettingsRepository | `data/repository/UserSettingsRepository.kt` | 设置数据仓库 |

## 数据库配置

### AppDatabase
```kotlin
@Database(
    entities = [
        ProjectEntity::class,
        AiModelConfigEntity::class,
        UserSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun aiModelConfigDao(): AiModelConfigDao
    abstract fun userSettingDao(): UserSettingDao

    companion object {
        const val DATABASE_NAME = "quickwrite_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- 数据库名称：`quickwrite_database`
- 单例模式：双重检查锁定（DCL）
- 迁移策略：`fallbackToDestructiveMigration(false)`（不启用破坏性迁移）
- 编译时处理：KSP（替代 kapt）

## 实体定义

### ProjectEntity (projects 表)
```kotlin
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val genre: String,
    val description: String?,
    val createdTime: Long,
    val modifiedTime: Long,
    val status: String,
    val coverImagePath: String?,
    val wordCount: Int = 0,
    val chapterCount: Int = 0,
    val storagePath: String
)
```

### AiModelConfigEntity (ai_model_configs 表)
```kotlin
@Entity(tableName = "ai_model_configs")
data class AiModelConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val configName: String,
    val provider: String,
    val apiKey: String,
    val baseUrl: String? = null,
    val modelName: String,
    val temperature: Float = 0.8f,
    val maxTokens: Int = 2000,
    val topP: Float = 1.0f,
    val topK: Int = 1,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val isDefault: Boolean = false
)
```

### UserSettingEntity (user_settings 表)
```kotlin
@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: String
)
```

### TypeConverter (Converters)
```kotlin
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }
}
```

## DAO 层

### ProjectDao
```kotlin
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY modified_time DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY created_time DESC")
    fun getAllProjectsByCreatedTime(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY title ASC")
    fun getAllProjectsByTitle(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE title = :title LIMIT 1")
    suspend fun getProjectByTitle(title: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("UPDATE projects SET word_count = :wordCount, modified_time = :modifiedTime WHERE id = :id")
    suspend fun updateWordCount(id: String, wordCount: Int, modifiedTime: Long)

    @Query("UPDATE projects SET chapter_count = chapter_count + 1, modified_time = :modifiedTime WHERE id = :id")
    suspend fun incrementChapterCount(id: String, modifiedTime: Long)
}
```

### AiModelConfigDao
```kotlin
@Dao
interface AiModelConfigDao {
    @Query("SELECT * FROM ai_model_configs ORDER BY is_default DESC, id ASC")
    fun getAllConfigs(): Flow<List<AiModelConfigEntity>>

    @Query("SELECT * FROM ai_model_configs WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultConfig(): AiModelConfigEntity?

    @Query("SELECT * FROM ai_model_configs WHERE id = :id")
    suspend fun getConfigById(id: Int): AiModelConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AiModelConfigEntity)

    @Update
    suspend fun updateConfig(config: AiModelConfigEntity)

    @Delete
    suspend fun deleteConfig(config: AiModelConfigEntity)

    @Query("UPDATE ai_model_configs SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultConfig(id: Int)

    @Query("UPDATE ai_model_configs SET is_default = 0")
    suspend fun clearDefaultConfig()
}
```

### UserSettingDao
```kotlin
@Dao
interface UserSettingDao {
    @Query("SELECT * FROM user_settings")
    fun getAllSettings(): Flow<List<UserSettingEntity>>

    @Query("SELECT * FROM user_settings WHERE category = :category")
    fun getSettingsByCategory(category: String): Flow<List<UserSettingEntity>>

    @Query("SELECT * FROM user_settings WHERE `key` = :key")
    suspend fun getSettingByKey(key: String): UserSettingEntity?

    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    fun getSettingValueByKeyAsFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: UserSettingEntity)

    @Query("DELETE FROM user_settings WHERE `key` = :key")
    suspend fun deleteSettingByKey(key: String)

    @Query("DELETE FROM user_settings WHERE category = :category")
    suspend fun deleteSettingsByCategory(category: String)
}
```

## Repository 层

### ProjectRepository
```kotlin
class ProjectRepository(private val projectDao: ProjectDao) {
    fun getAllProjects(): Flow<List<ProjectEntity>>
    suspend fun getProjectById(id: String): ProjectEntity?
    suspend fun createProject(project: ProjectEntity)
    suspend fun updateProject(project: ProjectEntity)
    suspend fun deleteProject(project: ProjectEntity)
    suspend fun deleteProjectById(id: String)
    suspend fun insertProjectDirect(project: ProjectEntity)   // 直接插入已构建的实体（导入场景）
    suspend fun isProjectTitleUnique(title: String, excludeId: String? = null): Boolean
    suspend fun updateWordCount(projectId: String, wordCount: Int)
    suspend fun incrementChapterCount(projectId: String)
    suspend fun updateModifiedTime(projectId: String)
}
```

### AiModelConfigRepository
```kotlin
class AiModelConfigRepository(private val dao: AiModelConfigDao) {
    fun getAllConfigs(): Flow<List<AiModelConfigEntity>>
    suspend fun getDefaultConfig(): AiModelConfigEntity?
    suspend fun createConfig(config: AiModelConfigEntity)  // 含唯一性检查
    suspend fun updateConfig(config: AiModelConfigEntity)
    suspend fun setDefaultConfig(id: Int)
    suspend fun deleteConfig(id: Int)                       // 自动转移默认
    suspend fun isConfigNameUnique(name: String, excludeId: Int? = null): Boolean
    suspend fun hasAnyConfig(): Boolean

    companion object {
        const val PROVIDER_OPENAI = "OpenAI"
        const val PROVIDER_ANTHROPIC = "Anthropic"
        const val PROVIDER_CUSTOM = "Custom"
        const val MODEL_GPT_35_TURBO = "gpt-3.5-turbo"
        const val MODEL_GPT_4 = "gpt-4"
        const val MODEL_CLAUDE_3 = "claude-3"
    }
}
```

### UserSettingsRepository
```kotlin
class UserSettingsRepository(private val userSettingDao: UserSettingDao) {
    // 通用存取
    suspend fun getSetting(key: String): String?
    suspend fun getSetting(key: String, defaultValue: String): String
    suspend fun setSetting(key: String, value: String, category: String): Result<Unit>
    
    // 类型化存取（含默认值）
    suspend fun getThemeMode(): String          // 默认 "system"
    suspend fun getFontSize(): Int              // 默认 14
    suspend fun getAutoSaveInterval(): Int      // 默认 5
    suspend fun getAutoSaveImmediately(): Boolean  // 默认 false
    suspend fun getLanguage(): String           // 默认 "system"
    suspend fun getCurrentProjectId(): String?
    fun getCurrentProjectIdAsFlow(): Flow<String?>
    suspend fun getUseModelConfig(): Boolean    // 默认 true
    suspend fun getDefaultTemperature(): Float  // 默认 0.8
    suspend fun getDefaultMaxTokens(): Int      // 默认 2000
    suspend fun getDefaultTopP(): Float         // 默认 1.0
}
```

## 数据流

### 响应式数据流
```
DAO (Flow) → Repository → UseCase → ViewModel (collectAsState → Compose UI)
```

1. DAO 返回 `Flow<List<Entity>>`（Room 自动通知数据变更）
2. Repository 可对 Flow 进行 `map`/`combine` 转换
3. UseCase 将 Flow 暴露给 ViewModel
4. ViewModel 通过 `stateIn()` 或直接 `collectAsState()` 绑定到 UI

### 数据库写入
```
UI Action → ViewModel → UseCase → Repository → DAO (.insert/.update/.delete)
                                                    │
                                                    ▼
                                            Room 自动通知 Flow 订阅者
                                                    │
                                                    ▼
                                            UI 自动 recompose
```

## 关键实现细节

### 数据库单例
使用 `@Volatile` + `synchronized` 实现线程安全的双重检查锁定单例模式。

### 冲突策略
- `ProjectDao.insertProject()` — `OnConflictStrategy.REPLACE`
- `UserSettingDao.insertSetting()` — `OnConflictStrategy.REPLACE`
- 所有设置项使用 REPLACE 确保幂等性

### 默认配置管理
`AiModelConfigRepository` 在以下操作中维护 `isDefault` 唯一性：
- **创建配置**: 若 `isDefault=true`，先清除所有默认再插入
- **设置默认**: 先清除所有默认再设置指定配置
- **删除默认配置**: 自动将列表第一个配置提升为默认

### KSP 代码生成
项目使用 KSP (Kotlin Symbol Processing) 替代 kapt 生成 Room 编译时代码：
```gradle
ksp("androidx.room:room-compiler:${version}")
```

## 已知问题/技术债务

1. **数据库迁移**: 使用 `fallbackToDestructiveMigration(false)` 拒绝破坏性迁移，版本升级时必须编写 Migration 对象
2. **API Key 明文存储**: `AiModelConfigEntity.apiKey` 以明文存储在 Room 数据库中，需要加密
3. **TypeConverter 未被使用**: `Converters` 类已定义但当前实体中没有 `List<String>` 字段需要转换
4. **无 DAO 测试**: 缺少 Room DAO 的插桩测试
5. `ProjectDao.deleteProject()` 方法已存在但未被调用（实际使用 `deleteProjectById()`）
