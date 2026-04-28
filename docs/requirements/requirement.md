# QuickWrite - 长篇创作Agent应用需求文档

## 1. 项目概述

### 1.1 项目背景
QuickWrite是一款专为长篇小说创作者设计的Android原生应用程序，旨在通过AI助理功能帮助作家系统化地管理复杂的小说创作过程。应用采用项目化管理理念，为每部小说创建独立的项目空间，整合内容管理、设定维护、时间线跟踪和写作辅助功能。

### 1.2 产品定位
- **目标用户**: 网络小说作者、传统文学创作者、小说爱好者
- **核心价值**: 提供结构化的创作管理工具，降低长篇创作复杂度
- **差异化优势**: 基于项目结构的系统化管理，AI辅助写作，跨平台支持

### 1.3 技术栈要求
- **平台**: Android (API 24+)
- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM + Repository模式
- **本地存储**: Room数据库 + 文件系统
- **网络**: Retrofit + OkHttp
- **协程**: Kotlin协程与Flow
- **依赖注入**: Hilt
- **AI集成**: OpenAI兼容API接口

## 2. 功能需求

### 2.1 项目管理系统

#### 2.1.1 项目创建与配置
```kotlin
// 项目基础属性
data class NovelProject(
    val id: String,           // 项目唯一标识
    val title: String,        // 小说标题
    val author: String,       // 作者名
    val genre: String,        // 小说类型（历史、奇幻、科幻等）
    val description: String?, // 项目描述
    val createdTime: Long,    // 创建时间
    val modifiedTime: Long,   // 修改时间
    val status: ProjectStatus // 项目状态（进行中、已完结、暂停）
)

// 项目状态枚举
enum class ProjectStatus {
    ACTIVE,      // 进行中
    COMPLETED,   // 已完结
    PAUSED,      // 暂停
    ARCHIVED     // 归档
}
```

#### 2.1.2 项目目录结构（纯文件系统管理）
每个项目创建独立的目录，所有内容以纯文本/Markdown文件存储。数据库只存储项目元数据，文档结构完全由文件系统管理。

```
/{项目ID}/                     # 项目根目录
├── 简介.md                    # 小说简介、核心设定
├── 目录结构说明.md            # 项目文档结构说明
│
├── 正文/                      # 小说正文章节
│   ├── 第一章.md
│   ├── 第二章.md
│   ├── 第三章.md
│   └── ...
│
├── 设定/                      # 所有设定文档
│   ├── 人物/
│   │   ├── 张无忌.md
│   │   ├── 赵敏.md
│   │   ├── 杨逍.md
│   │   └── ...
│   │
│   ├── 地点/
│   │   ├── 光明顶.md
│   │   ├── 冰火岛.md
│   │   ├── 武当山.md
│   │   └── ...
│   │
│   ├── 组织/
│   │   ├── 明教.md
│   │   ├── 六大派.md
│   │   ├── 朝廷.md
│   │   └── ...
│   │
│   ├── 物品/
│   │   ├── 倚天剑.md
│   │   ├── 屠龙刀.md
│   │   └── ...
│   │
│   └── 世界观/
│       ├── 武功体系.md
│       ├── 社会结构.md
│       ├── 文化风俗.md
│       └── ...
│
├── 时间线/                    # 时间管理文档
│   ├── 总览.md                # 时间线总览
│   ├── 元朝末年.md
│   ├── 六大派围攻光明顶.md
│   ├── 张无忌成长时间线.md
│   └── ...
│
├── 记录/                      # 创作过程记录
│   ├── 事件记录/
│   │   ├── 光明顶之战.md
│   │   ├── 冰火岛事件.md
│   │   └── ...
│   │
│   ├── 人物成长/
│   │   ├── 张无忌成长.md
│   │   ├── 赵敏转变.md
│   │   └── ...
│   │
│   └── 灵感笔记/
│       ├── 伏笔.md
│       ├── 待解决.md
│       └── 创意.md
│
└── 配置/                      # 项目配置
    ├── AI指令.md              # 给AI agent的指令
    ├── 写作规范.md            # 文风、禁词等
    ├── 项目目标.md            # 写作目标、进度
    └── AI模型设置.json        # AI模型配置
```

**文件命名规范**:
- 使用中文命名，便于AI理解
- 避免特殊字符和空格
- 保持名称简洁明确
- 使用`.md`扩展名表示Markdown文件

**设计原则**:
1. **纯文件系统**: 不依赖数据库存储文档结构
2. **扁平化**: 目录深度不超过3级，便于导航
3. **自描述**: 每个目录都有清晰的名称和用途
4. **可探索**: AI agent可以自由探索目录结构
5. **可移植**: 项目目录可单独复制、备份、共享

### 2.2 内容管理模块

#### 2.2.1 章节管理
- **章节创建**: 支持添加、删除、重命名、排序章节
- **章节模板**: 提供多种章节模板（开端、发展、高潮、结局）
- **字数统计**: 实时统计章节和总字数
- **版本管理**: 支持章节内容的版本历史记录

#### 2.2.2 设定管理（文档化设计）
设定管理采用纯文档文件方式，为AI agent提供结构化的知识库：

**人物文档格式模板** (`人物/核心人物/张无忌.md`):
```
# 张无忌

## 基本信息
- **角色类型**: 主角
- **年龄**: 约20岁（小说开始时）
- **性别**: 男
- **别名**: 曾阿牛、张教主
- **出身**: 武当派张翠山与天鹰教殷素素之子

## 外貌特征
身高约178cm，面容清秀但略显忧郁，眼神中带有慈悲与智慧。常年穿着简朴的布衣，但气质出众。

## 性格特点
1. **仁慈悲悯**: 天性善良，不忍伤害他人
2. **优柔寡断**: 在感情选择上犹豫不决
3. **重情重义**: 对朋友和亲人极为忠诚
4. **大智若愚**: 看似单纯，实则洞察力强

## 背景故事
出生于冰火岛，父母为避六大派追杀隐居海外。十岁时返回中原，父母双双身亡。身中玄冥神掌寒毒，后被张三丰救治。因缘际会学会九阳神功、乾坤大挪移等绝世武功。

## 武功技能
- **九阳神功**: 至阳至刚的内功，百毒不侵
- **乾坤大挪移**: 明教镇教神功，可转移敌人攻击
- **太极拳剑**: 张三丰所传，以柔克刚
- **圣火令武功**: 波斯武功，诡异多变

## 人物关系
- **父亲**: 张翠山（已故）
- **母亲**: 殷素素（已故）
- **义父**: 谢逊（金毛狮王）
- **爱人**: 赵敏、周芷若、小昭、殷离
- **师公**: 张三丰
- **朋友**: 杨逍、范遥、韦一笑等明教众人

## 成长轨迹
1. **童年时期**: 冰火岛生活，跟随父母和义父
2. **少年时期**: 返回中原，父母双亡，身中寒毒
3. **青年时期**: 学会九阳神功，接任明教教主
4. **成熟时期**: 领导明教抗元，最终归隐

## 关键语录
> "我张无忌行事但求问心无愧，不求人人理解。"

## 备注
- 此人物有多次重大转变节点
- 感情线复杂，需注意处理
- 武功成长有明确递进关系
```

**组织文档格式** (`组织/明教.md`):
```
# 明教

## 组织性质
武林第一大教，亦正亦邪，以抗元为宗旨

## 组织结构
总坛: 光明顶
分坛: 五行旗、天地风雷四门

## 核心成员
教主: 张无忌
光明左使: 杨逍
光明右使: 范遥
四大法王: 紫衫龙王、白眉鹰王、金毛狮王、青翼蝠王
五散人: 冷谦、说不得、彭莹玉、张中、周颠

## 组织历史
起源于波斯，唐代传入中原。元朝时成为抗元主力，但因行事诡异被名门正派误解。

## 与其他组织关系
- 敌对: 元朝朝廷、六大派（前期）
- 合作: 天鹰教（后并入明教）
- 中立: 少林、武当（后期）
```

#### 2.2.3 时间线管理（文档化设计）
时间线通过纯文档文件管理，便于AI agent按时间顺序理解事件：

**时间线文档结构**:
1. **主时间线文档** (`时间线/时间线总览.md`):
   ```
   # 《倚天屠龙记》时间线总览
   
   ## 时间线结构
   元朝末年 → 六大派围攻光明顶 → 张无忌接任教主 → 抗元斗争 → 最终归隐
   
   ## 主要时间节点
   - 1336年: 张翠山殷素素结婚，隐居冰火岛
   - 1346年: 张无忌出生
   - 1356年: 张无忌返回中原，父母双亡
   - 1360年: 六大派围攻光明顶
   - 1362年: 张无忌接任明教教主
   - 1368年: 明军建立明朝，张无忌归隐
   
   ## 时间线文件索引
   详见各子目录的时间文档
   ```

2. **详细时间文档** (`时间线/历史大事记/元朝末年-1350年.md`):
   ```
   # 元朝末年 (1330-1350年)
   
   ## 政治背景
   元朝统治日益腐败，各地农民起义频发。
   
   ## 武林格局
   六大派与明教矛盾加剧，武林暗流涌动。
   
   ## 关键事件
   ### 1336年 - 冰火岛隐居
   张翠山与殷素素为避追杀，携义兄谢逊隐居冰火岛。
   
   ### 1346年 - 张无忌出生
   张无忌在冰火岛诞生，体质特殊。
   
   ## 涉及人物
   - 张翠山、殷素素、谢逊、张三丰
   
   ## 关联章节
   第1-3章涉及此时期背景
   ```

3. **人物时间线文档** (`时间线/人物时间线/张无忌时间线.md`):
   ```
   # 张无忌个人时间线
   
   ## 出生与童年 (1346-1356)
   - 1346年: 出生于冰火岛
   - 1350年: 开始学习武功基础
   - 1356年: 随父母返回中原
   
   ## 少年磨难 (1356-1360)
   - 1356年: 父母双亡，身中玄冥神掌
   - 1357年: 武当山养伤
   - 1358年: 蝴蝶谷学医
   - 1359年: 昆仑山遇险
   
   ## 青年崛起 (1360-1365)
   - 1360年: 学会九阳神功，光明顶扬名
   - 1362年: 接任明教教主
   - 1364年: 领导抗元斗争
   
   ## 成熟归隐 (1365-1368)
   - 1368年: 明朝建立，携赵敏归隐
   ```

**时间线查询机制**:
1. **时间搜索**: AI agent可根据时间关键词查找相关文档
2. **事件关联**: 通过文档间的引用建立事件关系
3. **时间索引**: 统一的年表文档提供快速查找
4. **交叉验证**: 多文档对比确保时间一致性

### 2.3 AI写作辅助模块（文档驱动设计）

#### 2.3.1 AI模型与文档集成
应用的核心是将文件系统中的文档内容作为AI agent的知识库。AI agent通过读取和分析文档文件来获取上下文信息。

**文档读取系统架构**:
```kotlin
data class DocumentContext(
    val projectId: String,
    val currentFile: String,           // 当前编辑的文件路径
    val contextFiles: List<String>,    // 相关上下文文件列表
    val queryType: QueryType          // 查询类型（续写、设定查询等）
)

interface DocumentProcessor {
    /**
     * 根据查询类型智能选择相关文档
     */
    fun selectRelevantDocuments(
        projectId: String,
        currentFile: String,
        queryType: QueryType
    ): List<File>
    
    /**
     * 提取文档关键信息，准备AI提示词
     */
    fun extractDocumentContext(files: List<File>): String
    
    /**
     * 生成AI提示词，包含文档上下文
     */
    fun generatePromptWithContext(
        userPrompt: String,
        documentContext: String,
        writingRules: String
    ): String
}

enum class QueryType {
    CONTINUE_WRITING,     // 内容续写
    CHARACTER_DIALOGUE,   // 人物对话
    SCENE_DESCRIPTION,    // 场景描写
    PLOT_SUGGESTION,      // 情节建议
    SETTING_QUERY,        // 设定查询
    CONFLICT_CHECK        // 一致性检查
}
```

#### 2.3.2 文档驱动的AI写作功能

**1. 上下文感知的内容续写**:
当用户在章节中请求续写时，系统自动：
1. 读取当前章节的前3-5段作为直接上下文
2. 查找本章涉及的人物，读取相关人物文档
3. 根据时间线文档确定当前时间点
4. 检索此时间点的事件和背景设定
5. 整合所有信息生成AI提示词

**2. 基于文档的人物对话生成**:
- **角色一致性**: 读取人物文档的性格特点、说话习惯
- **关系准确性**: 参照人物关系文档确定对话语气
- **背景适配**: 结合当前情节场景调整对话内容

**3. 设定驱动的场景描写**:
```
输入: "生成光明顶大厅的场景描写"
处理流程:
1. 读取 `设定/世界观/地理环境/光明顶.md`
2. 读取 `设定/组织/明教.md` 获取组织信息
3. 读取相关时间线文档了解当前时期
4. 整合生成详细的场景描写
```

**4. 文档查询与设定补充**:
```
用户提问: "张无忌现在会什么武功？"
系统响应:
1. 读取 `设定/人物/核心人物/张无忌.md`
2. 提取"武功技能"部分
3. 根据当前时间线确定已掌握的武功
4. 生成准确回答
```

#### 2.3.3 智能文档分析系统

**文档结构分析器**:
```kotlin
interface DocumentAnalyzer {
    /**
     * 分析文档结构，提取关键信息
     */
    fun analyzeDocument(file: File): DocumentAnalysis
    
    /**
     * 建立文档间的引用关系
     */
    fun buildDocumentRelationships(projectId: String): RelationshipGraph
    
    /**
     * 检查设定一致性
     */
    fun checkConsistency(newContent: String, existingDocs: List<File>): List<ConsistencyIssue>
}
```

**一致性检查机制**:
1. **人物一致性**: 新内容中的人物行为是否与人物文档一致
2. **时间一致性**: 事件时间点是否符合时间线文档
3. **设定一致性**: 新设定是否与现有世界观冲突
4. **关系一致性**: 人物间关系变化是否合理

**文档检索优化**:
1. **语义索引**: 建立文档内容的语义索引，快速定位
2. **关联度计算**: 根据查询内容计算文档相关度
3. **上下文缓存**: 缓存常用文档内容，减少文件读取
4. **增量更新**: 只读取变化的部分文档

#### 2.3.4 AI agent文档探索器
为AI agent提供文件系统探索功能，让agent能够主动查找和阅读项目中的文档。

**文档探索器接口设计**:
```kotlin
/**
 * AI agent文档探索器
 * 提供文件系统导航和文档读取功能
 */
interface DocumentExplorer {
    /**
     * 获取项目根目录信息
     */
    fun getProjectRootInfo(projectId: String): ProjectRootInfo
    
    /**
     * 列出目录内容
     */
    fun listDirectory(projectId: String, directoryPath: String): List<FileInfo>
    
    /**
     * 读取文档内容
     */
    fun readDocument(projectId: String, filePath: String): DocumentContent?
    
    /**
     * 搜索文档
     */
    fun searchDocuments(projectId: String, keyword: String): List<SearchResult>
    
    /**
     * 获取项目文档结构概览
     */
    fun getProjectStructureOverview(projectId: String): ProjectStructure
    
    /**
     * 查找相关文档（基于当前上下文）
     */
    fun findRelatedDocuments(
        projectId: String,
        currentDocument: String,
        context: String
    ): List<RelatedDocument>
}

/**
 * 项目根目录信息
 */
data class ProjectRootInfo(
    val projectId: String,
    val projectPath: String,
    val rootFiles: List<String>,           // 根目录下的文件
    val directories: List<String>,         // 根目录下的子目录
    val totalFiles: Int,
    val totalSize: Long
)

/**
 * 文件信息
 */
data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val typeHint: String = ""              // 类型提示（人物、章节等）
)

/**
 * 搜索结果
 */
data class SearchResult(
    val filePath: String,
    val fileName: String,
    val relevance: Double,
    val snippet: String,                   // 匹配片段
    val lineNumber: Int
)

/**
 * 相关文档
 */
data class RelatedDocument(
    val filePath: String,
    val fileName: String,
    val relationType: RelationType,
    val relevanceScore: Double,
    val summary: String                    // 内容摘要
)

enum class RelationType {
    CHARACTER,      // 人物相关
    LOCATION,       // 地点相关
    ORGANIZATION,   // 组织相关
    TIMELINE,       // 时间线相关
    EVENT,          // 事件相关
    SETTING         // 设定相关
}

/**
 * 项目文档结构
 */
data class ProjectStructure(
    val projectId: String,
    val directories: Map<String, List<String>>,  // 目录结构
    val fileCounts: Map<String, Int>,            // 各类型文件数量
    val importantFiles: List<String>,            // 重要文件列表
    val lastModifiedFiles: List<String>          // 最近修改的文件
)
```

**AI agent探索流程**:
```
用户请求 → AI识别需求类型 → 文档探索器查找相关文档 → 
读取文档内容 → 构建上下文 → 生成回答
```

**探索策略示例**:
1. **人物相关问题**:
   ```
   用户: "张无忌的性格特点是什么？"
   探索器: 
   1. 搜索"张无忌"关键词
   2. 找到 "设定/人物/张无忌.md"
   3. 读取文档，提取性格部分
   4. 返回给AI agent
   ```

2. **场景描写需求**:
   ```
   用户: "描写光明顶的大厅"
   探索器:
   1. 搜索"光明顶"关键词
   2. 找到 "设定/地点/光明顶.md"
   3. 搜索"明教"相关文档
   4. 查找时间线文档确定时期
   5. 整合所有相关文档
   ```

3. **设定查询**:
   ```
   用户: "现在有哪些主要势力？"
   探索器:
   1. 列出"设定/组织/"目录
   2. 读取各组织文档
   3. 提取核心信息
   4. 生成势力列表
   ```

#### 2.3.5 AI agent系统提示词配置
每个项目的AI agent会收到包含项目信息的系统提示词：

**系统提示词模板**:
```
# 小说创作助手 - 项目专属配置

## 角色设定
你是一位专业的小说创作助手，正在帮助作者创作《{小说标题}》。

## 项目基本情况
- **小说简介**: 
{小说简介内容（来自简介.md）}

- **作者**: {作者姓名}
- **类型**: {小说类型}
- **当前进度**: {写作进度}

## 项目文档结构
{项目文档结构说明（自动生成）}

### 主要目录：
1. **正文/** - 小说正文章节
2. **设定/** - 人物、地点、组织、物品等设定
3. **时间线/** - 时间管理和事件记录
4. **记录/** - 创作过程和灵感记录
5. **配置/** - 项目配置和AI指令

### 重要文件：
- {重要文件列表}

## 你的能力
你可以：
1. 读取项目中的文档文件获取设定信息
2. 根据文档内容提供准确的设定查询
3. 基于现有设定进行内容创作
4. 检查新内容与现有设定的一致性
5. 建议完善设定文档

## 工作原则
1. **准确性优先**: 所有回答必须基于项目文档
2. **主动探索**: 遇到不了解的信息时，主动查找相关文档
3. **一致性检查**: 确保建议内容与现有设定不冲突
4. **文档优先**: 优先使用文档中已有信息，避免凭空创造

## 探索指南
当需要了解项目设定时，请：
1. 首先查阅相关目录的文件列表
2. 读取最相关的文档
3. 提取关键信息
4. 基于文档信息进行回答

## 错误处理
如果发现信息不足或冲突：
1. 明确告知用户需要补充什么信息
2. 建议查看或创建相关文档
3. 提供具体的文档路径建议

---

**当前项目路径**: {项目存储路径}
**项目ID**: {项目ID}
**最后更新时间**: {项目最后更新时间}
```

**系统提示词生成器**:
```kotlin
interface SystemPromptGenerator {
    /**
     * 为项目生成AI系统提示词
     */
    fun generateSystemPrompt(projectId: String): String
    
    /**
     * 更新系统提示词（当项目变更时）
     */
    fun updateSystemPrompt(projectId: String)
    
    /**
     * 获取项目简介
     */
    fun getProjectSummary(projectId: String): String
    
    /**
     * 获取项目文档结构描述
     */
    fun getProjectStructureDescription(projectId: String): String
}
```

**系统提示词更新机制**:
1. **项目创建时**: 生成初始系统提示词
2. **文档变更时**: 检测重要文档变更，更新提示词
3. **用户手动更新**: 提供更新选项
4. **定期刷新**: 每周自动刷新一次提示词

### 2.4 用户界面需求

#### 2.4.1 主界面设计
- **项目仪表盘**: 展示所有项目的概览信息
- **快速导航**: 侧边栏导航到各个功能模块
- **写作统计**: 显示字数、进度等统计数据
- **最近活动**: 显示最近编辑的章节和设定

#### 2.4.2 编辑器界面
- **Markdown支持**: 支持Markdown语法和预览
- **语法高亮**: 代码块、标题、列表等的语法高亮
- **分屏编辑**: 支持编辑和预览分屏显示
- **快速插入**: 快捷插入角色、地点、时间等元素

#### 2.4.3 设定管理界面
- **卡片式布局**: 人物、地点等设定以卡片形式展示
- **关系图**: 可视化展示角色关系网络
- **时间轴**: 交互式时间轴视图
- **搜索筛选**: 强大的搜索和筛选功能

### 2.5 数据管理需求

#### 2.5.1 本地存储
- **文件存储**: 使用文件系统存储文档内容
- **数据库存储**: SQLite存储元数据和索引
- **缓存机制**: 实现内容缓存加速访问
- **数据加密**: 敏感数据的本地加密存储

#### 2.5.2 数据同步
- **自动保存**: 实时自动保存编辑内容
- **版本备份**: 定期创建备份版本
- **导入导出**: 
  - 导出为Markdown文件包
  - 导出为Word文档
  - 导出为PDF格式
  - 导入已有文档

#### 2.5.3 云同步（可选）
- **跨设备同步**: 通过云服务同步项目数据
- **协作功能**: 多人协同编辑（未来扩展）
- **云端备份**: 自动备份到云端

## 3. 非功能需求

### 3.1 性能要求
- **启动时间**: 冷启动时间 < 3秒
- **响应时间**: 界面操作响应时间 < 100ms
- **文件加载**: 大文件（>1MB）加载时间 < 2秒
- **AI响应**: AI生成响应时间 < 30秒（依赖网络）

### 3.2 稳定性要求
- **崩溃率**: 应用崩溃率 < 0.1%
- **数据安全**: 数据丢失率 < 0.01%
- **网络容错**: 网络异常时的优雅处理
- **内存管理**: OOM发生率 < 0.05%

### 3.3 兼容性要求
- **Android版本**: 支持Android 7.0+（API 24+）
- **屏幕适配**: 适配手机和平板设备
- **权限管理**: 合理处理存储权限
- **深色模式**: 支持系统深色模式

### 3.4 安全性要求
- **API密钥保护**: 本地加密存储API密钥
- **数据隐私**: 用户数据不上传服务器（除非配置云同步）
- **权限控制**: 最小权限原则
- **输入验证**: 防止注入攻击

## 4. 技术实现方案

### 4.1 架构设计（Compose + Room）
```
┌─────────────────────────────────────────────┐
│             UI Layer (Compose)              │
│   @Composable Screens / ViewModels         │
│   StateFlow / MutableState / SnackbarState │
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│             Domain Layer                    │
│   UseCases / Repositories / Models         │
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│              Data Layer                     │
│   Repositories / Room DAOs / FileSystem    │
│   Retrofit Services / Preferences          │
└─────────────────────────────────────────────┘
```

### 4.2 核心包结构（Compose + Room优化）
```
com.universe_st.quickwrite/
├── app/
│   ├── MainActivity.kt          # Entry point with Compose主题
│   ├── QuickWriteApp.kt         # 顶层Composable应用
│   └── navigation/              # Compose Navigation
├── di/                          # Hilt依赖注入
├── domain/                      # 领域层
│   ├── model/                   # 数据模型（Entity、DTO）
│   ├── repository/              # Repository接口
│   └── usecase/                 # UseCase实现
├── data/                        # 数据层
│   ├── local/                   # 本地数据源
│   │   ├── database/            # Room数据库
│   │   │   ├── dao/             # Data Access Objects
│   │   │   ├── entity/          # Room Entity类
│   │   │   └── AppDatabase.kt   # Room数据库配置
│   │   ├── filesystem/          # 文件系统管理
│   │   └── preferences/         # 首选项存储
│   ├── remote/                  # 远程数据源
│   │   └── ai/                  # AI接口服务
│   └── repository/              # Repository实现
├── presentation/                # 表现层（Compose）
│   ├── ui/                      # UI组件
│   │   ├── component/           # 通用Composable组件
│   │   │   ├── card/            # 卡片组件
│   │   │   ├── dialog/          # 对话框组件
│   │   │   ├── topbar/          # 顶部栏组件
│   │   │   └── textfield/       # 输入框组件
│   │   ├── screen/              # 屏幕级组件
│   │   │   ├── project/         # 项目相关屏幕
│   │   │   ├── editor/          # 编辑器屏幕
│   │   │   ├── settings/        # 设置屏幕
│   │   │   └── timeline/        # 时间线屏幕
│   │   └── theme/               # Compose主题配置
│   │       ├── Color.kt         # 颜色方案
│   │       ├── Typography.kt    # 字体排版
│   │       └── Theme.kt         # App主题
│   └── viewmodel/               # ViewModels
│       ├── ProjectViewModel.kt
│       ├── EditorViewModel.kt
│       └── SettingsViewModel.kt
└── utils/                       # 工具类
    ├── extensions/              # Kotlin扩展函数
    ├── helpers/                 # 辅助工具
    └── constants/               # 常量定义
```

### 4.3 关键技术实现（文件系统驱动设计）

#### 4.3.1 文件系统管理器（核心组件）
负责项目目录结构创建、文档管理，并提供给AI agent探索功能。

```kotlin
/**
 * 文件系统管理器
 * 核心：所有文档结构都在文件系统中，不依赖数据库
 */
interface FileSystemManager {
    // 项目目录管理
    fun createProjectDirectory(projectId: String, config: ProjectConfig): ProjectDirectory
    fun deleteProjectDirectory(projectId: String): Boolean
    fun getProjectDirectory(projectId: String): File?
    fun listProjects(): List<ProjectInfo>
    
    // 文件操作
    fun createFile(projectId: String, filePath: String, content: String = ""): Boolean
    fun readFile(projectId: String, filePath: String): String?
    fun updateFile(projectId: String, filePath: String, content: String): Boolean
    fun deleteFile(projectId: String, filePath: String): Boolean
    fun moveFile(projectId: String, sourcePath: String, targetPath: String): Boolean
    
    // 目录操作（提供给AI agent探索）
    fun listDirectory(projectId: String, directoryPath: String = ""): DirectoryListing
    fun getFileInfo(projectId: String, filePath: String): FileInfo?
    fun searchFiles(projectId: String, keyword: String): List<SearchResult>
    
    // 项目统计
    fun getProjectStatistics(projectId: String): ProjectStatistics
    fun updateProjectStatistics(projectId: String)
}

/**
 * 项目目录信息
 */
data class ProjectDirectory(
    val projectId: String,
    val rootPath: String,
    val directories: List<ProjectSubdirectory>,
    val totalFiles: Int,
    val totalSize: Long,
    val createdTime: Long,
    val lastModifiedTime: Long
)

/**
 * 项目子目录
 */
data class ProjectSubdirectory(
    val name: String,
    val path: String,
    val description: String,     // 目录用途描述
    val fileCount: Int,
    val importantFiles: List<String>  // 重要文件列表
)

/**
 * 目录列表
 */
data class DirectoryListing(
    val path: String,
    val parentPath: String?,
    val directories: List<DirectoryItem>,
    val files: List<FileItem>,
    val totalItems: Int
)

/**
 * 目录项
 */
data class DirectoryItem(
    val name: String,
    val path: String,
    val itemCount: Int,
    val lastModified: Long
)

/**
 * 文件项
 */
data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val type: FileType,
    val preview: String = ""      // 文件内容预览（前几行）
)

enum class FileType {
    MARKDOWN,      // .md文件
    TEXT,          // .txt文件
    JSON,          // .json文件
    IMAGE,         // 图片文件
    OTHER          // 其他文件
}

/**
 * 文件信息
 */
data class FileInfo(
    val name: String,
    val path: String,
    val fullPath: String,
    val size: Long,
    val createdTime: Long,
    val modifiedTime: Long,
    val content: String? = null,      // 可选：文件内容
    val metadata: FileMetadata = FileMetadata()
)

/**
 * 文件元数据
 */
data class FileMetadata(
    val type: String = "",
    val author: String = "",
    val tags: List<String> = emptyList(),
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val lineCount: Int = 0
)

/**
 * 搜索结果
 */
data class SearchResult(
    val filePath: String,
    val fileName: String,
    val relevance: Double,
    val matches: List<TextMatch>,
    val preview: String
)

/**
 * 文本匹配
 */
data class TextMatch(
    val lineNumber: Int,
    val lineContent: String,
    val matchedText: String
)

/**
 * 项目统计
 */
data class ProjectStatistics(
    val projectId: String,
    val totalFiles: Int,
    val totalSize: Long,
    val wordCount: Int,
    val chapterCount: Int,
    val characterCount: Int,
    val timelineCount: Int,
    val lastActivityTime: Long,
    val directoryBreakdown: Map<String, Int>  // 各目录文件数
)
```

#### 4.3.2 AI agent文档探索器实现
实现给AI agent使用的文档探索功能。

```kotlin
/**
 * AI agent文档探索器（给AI agent使用的接口）
 */
class AIAgentDocumentExplorer(
    private val fileSystemManager: FileSystemManager,
    private val projectContext: ProjectContext
) {
    /**
     * 获取项目根信息（提供给AI agent）
     */
    fun exploreProjectRoot(projectId: String): ExplorationResult {
        val projectDir = fileSystemManager.getProjectDirectory(projectId)
        val rootListing = fileSystemManager.listDirectory(projectId)
        
        return ExplorationResult(
            projectId = projectId,
            rootPath = projectDir?.absolutePath ?: "",
            structure = describeProjectStructure(projectId),
            importantFiles = findImportantFiles(projectId),
            statistics = fileSystemManager.getProjectStatistics(projectId)
        )
    }
    
    /**
     * 浏览目录（提供给AI agent）
     */
    fun browseDirectory(projectId: String, directoryPath: String): DirectoryExploration {
        val listing = fileSystemManager.listDirectory(projectId, directoryPath)
        val description = generateDirectoryDescription(listing)
        
        return DirectoryExploration(
            path = directoryPath,
            description = description,
            subdirectories = listing.directories.map { it.name },
            files = listing.files.map { it.name },
            suggestion = generateNavigationSuggestion(listing)
        )
    }
    
    /**
     * 读取文档（提供给AI agent）
     */
    fun readDocument(projectId: String, filePath: String): DocumentReading {
        val content = fileSystemManager.readFile(projectId, filePath)
        val info = fileSystemManager.getFileInfo(projectId, filePath)
        
        return DocumentReading(
            filePath = filePath,
            fileName = info?.name ?: "",
            content = content ?: "",
            summary = generateDocumentSummary(content),
            metadata = info?.metadata ?: FileMetadata(),
            relatedFiles = findRelatedFiles(projectId, filePath, content)
        )
    }
    
    /**
     * 搜索文档（提供给AI agent）
     */
    fun searchInProject(projectId: String, query: String): SearchExploration {
        val results = fileSystemManager.searchFiles(projectId, query)
        
        return SearchExploration(
            query = query,
            results = results.map {
                SearchItem(
                    filePath = it.filePath,
                    fileName = it.fileName,
                    relevance = it.relevance,
                    preview = it.preview,
                    suggestedAction = generateActionForSearchResult(it)
                )
            },
            categories = categorizeSearchResults(results),
            nextSteps = suggestNextSearchSteps(query, results)
        )
    }
    
    /**
     * 生成项目结构描述（用于AI系统提示词）
     */
    fun generateProjectStructureDescription(projectId: String): String {
        val stats = fileSystemManager.getProjectStatistics(projectId)
        val rootListing = fileSystemManager.listDirectory(projectId)
        
        val sb = StringBuilder()
        sb.appendLine("## 项目文档结构")
        sb.appendLine()
        sb.appendLine("### 主要目录：")
        
        for (dir in rootListing.directories) {
            val subListing = fileSystemManager.listDirectory(projectId, dir.path)
            sb.appendLine("1. **${dir.name}/** - ${getDirectoryDescription(dir.name)}")
            sb.appendLine("   包含 ${subListing.files.size} 个文件")
            if (subListing.files.isNotEmpty()) {
                sb.appendLine("   重要文件：${subListing.files.take(3).joinToString("、") { it.name }}")
            }
            sb.appendLine()
        }
        
        sb.appendLine("### 项目统计：")
        sb.appendLine("- 总文件数：${stats.totalFiles}")
        sb.appendLine("- 总字数：${stats.wordCount}")
        sb.appendLine("- 章节数：${stats.chapterCount}")
        sb.appendLine("- 人物设定数：${stats.characterCount}")
        sb.appendLine("- 最后活动时间：${formatTime(stats.lastActivityTime)}")
        
        return sb.toString()
    }
    
    private fun getDirectoryDescription(dirName: String): String {
        return when (dirName) {
            "正文" -> "小说正文章节"
            "设定" -> "人物、地点、组织、物品等设定文档"
            "时间线" -> "时间管理和事件记录"
            "记录" -> "创作过程和灵感记录"
            "配置" -> "项目配置和AI指令"
            else -> "项目文档"
        }
    }
    
    // 其他辅助方法...
}

/**
 * 探索结果
 */
data class ExplorationResult(
    val projectId: String,
    val rootPath: String,
    val structure: String,           // 结构描述
    val importantFiles: List<String>,
    val statistics: ProjectStatistics
)

/**
 * 目录探索
 */
data class DirectoryExploration(
    val path: String,
    val description: String,
    val subdirectories: List<String>,
    val files: List<String>,
    val suggestion: String          // 导航建议
)

/**
 * 文档阅读
 */
data class DocumentReading(
    val filePath: String,
    val fileName: String,
    val content: String,
    val summary: String,
    val metadata: FileMetadata,
    val relatedFiles: List<String>
)

/**
 * 搜索结果探索
 */
data class SearchExploration(
    val query: String,
    val results: List<SearchItem>,
    val categories: Map<String, List<SearchItem>>,
    val nextSteps: List<String>
)

data class SearchItem(
    val filePath: String,
    val fileName: String,
    val relevance: Double,
    val preview: String,
    val suggestedAction: String
)
```

#### 4.3.2 文档驱动的AI集成
AI服务直接读取文档文件内容构建上下文。

```kotlin
/**
 * 文档上下文构建器
 * 负责从文档文件中提取和构建AI提示词
 */
interface DocumentContextBuilder {
    /**
     * 根据写作任务构建上下文
     */
    suspend fun buildContextForWritingTask(
        projectId: String,
        currentDocument: DocumentContent,
        taskType: WritingTaskType
    ): WritingContext
    
    /**
     * 生成完整的AI提示词
     */
    fun generateAIPrompt(
        userRequest: String,
        context: WritingContext,
        writingRules: String
    ): AIPrompt
    
    /**
     * 提取关键信息摘要（减少token消耗）
     */
    fun extractKeyInformation(
        documents: List<DocumentContent>,
        maxTokens: Int
    ): String
}

/**
 * 写作任务上下文
 */
data class WritingContext(
    val projectId: String,
    val currentDocument: DocumentContent,
    val relevantDocuments: List<DocumentContent>,
    val timelineContext: TimelineContext?,
    val characterContext: List<CharacterContext>,
    val worldContext: WorldContext?,
    val writingRules: WritingRules
)

/**
 * 时间线上下文
 */
data class TimelineContext(
    val currentTimePoint: String,
    val recentEvents: List<String>,
    val upcomingEvents: List<String>,
    val timelineDocuments: List<DocumentContent>
)

/**
 * 人物上下文
 */
data class CharacterContext(
    val characterName: String,
    val documentContent: DocumentContent,
    val currentStatus: String?,
    val relationships: List<Relationship>
)

enum class WritingTaskType {
    CONTINUE_WRITING,    // 内容续写
    DIALOGUE_GENERATION, // 对话生成
    SCENE_DESCRIPTION,   // 场景描写
    PLOT_DEVELOPMENT,    // 情节发展
    CHARACTER_ACTIONS,   // 人物行动
    SETTING_EXPANSION    // 设定扩展
}
```

#### 4.3.3 文档编辑器与AI助手集成
```kotlin
/**
 * 智能文档编辑器
 * 集成AI助手的Markdown编辑器
 */
class SmartDocumentEditor : AppCompatEditText {
    // 基础Markdown编辑功能
    fun setupMarkdownSupport()
    fun enableLivePreview()
    fun addSyntaxHighlighting()
    
    // AI集成功能
    fun initAIAssistant(aiService: AIService)
    fun showAISuggestions(suggestions: List<AISuggestion>)
    fun insertAIContent(content: String)
    fun showConsistencyWarnings(warnings: List<ConsistencyWarning>)
    
    // 上下文感知功能
    fun detectContextElements(): List<ContextElement>
    fun highlightRelatedContent()
    fun showDocumentReferences()
}

/**
 * AI助手管理器
 */
interface AIAssistantManager {
    /**
     * 分析当前编辑的内容
     */
    suspend fun analyzeCurrentContent(
        projectId: String,
        documentPath: String,
        currentText: String
    ): ContentAnalysis
    
    /**
     * 获取AI建议
     */
    suspend fun getAISuggestions(
        projectId: String,
        context: WritingContext,
        suggestionType: SuggestionType
    ): List<AISuggestion>
    
    /**
     * 检查内容一致性
     */
    suspend fun checkContentConsistency(
        projectId: String,
        newContent: String,
        existingContext: WritingContext
    ): ConsistencyCheckResult
    
    /**
     * 查询设定信息
     */
    suspend fun querySetting(
        projectId: String,
        question: String,
        context: WritingContext
    ): SettingAnswer
}

/**
 * 内容分析结果
 */
data class ContentAnalysis(
    val characterMentions: List<CharacterMention>,
    val timelineReferences: List<String>,
    val settingReferences: List<SettingReference>,
    val suggestionPoints: List<SuggestionPoint>,
    val consistencyIssues: List<String>
)

/**
 * AI建议
 */
data class AISuggestion(
    val id: String,
    val type: SuggestionType,
    val content: String,
    val confidence: Double,
    val reasoning: String,
    val sourceDocuments: List<String>
)

enum class SuggestionType {
    CONTINUATION,       // 内容续写
    DIALOGUE,           // 对话建议
    DESCRIPTION,        // 描写建议
    PLOT_TWIST,         // 情节转折
    CHARACTER_ACTION,   // 人物行动
    SETTING_DETAIL,     // 设定细节
    CONFLICT_RESOLUTION // 冲突解决
}
```

#### 4.3.4 文档索引与检索系统
```kotlin
/**
 * 文档索引管理器
 * 建立文档的快速检索索引
 */
interface DocumentIndexManager {
    /**
     * 建立项目文档索引
     */
    fun buildProjectIndex(projectId: String): IndexResult
    
    /**
     * 增量更新索引
     */
    fun updateIndexForFile(projectId: String, filePath: String)
    
    /**
     * 语义搜索文档
     */
    fun semanticSearch(
        projectId: String,
        query: String,
        limit: Int = 10
    ): List<SemanticSearchResult>
    
    /**
     * 查找相关文档
     */
    fun findRelatedDocuments(
        projectId: String,
        filePath: String,
        relationType: RelationType
    ): List<String>
}

/**
 * 语义搜索结果
 */
data class SemanticSearchResult(
    val filePath: String,
    val fileName: String,
    val relevanceScore: Double,
    val matchingSections: List<TextSection>,
    val documentType: DocumentType
)

data class TextSection(
    val content: String,
    val startLine: Int,
    val endLine: Int,
    val relevanceScore: Double
)

enum class RelationType {
    CHARACTER_RELATED,      // 人物相关
    TIMELINE_RELATED,       // 时间线相关
    LOCATION_RELATED,       // 地点相关
    EVENT_RELATED,          // 事件相关
    THEMATIC_RELATED,       // 主题相关
    DIRECT_REFERENCE        // 直接引用
}
```

### 4.4 数据库设计（Room数据库方案）

使用Room数据库存储必要的元数据和对话记录，文档结构完全由文件系统管理。AI agent通过文件系统直接访问文档。

#### 4.4.1 核心Room实体类
```kotlin
// 项目表实体类
@Entity(
    tableName = "projects",
    indices = [Index(value = ["modified_time"], name = "idx_projects_modified")]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "author")
    val author: String,
    @ColumnInfo(name = "genre")
    val genre: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "created_time")
    val createdTime: Long,
    @ColumnInfo(name = "modified_time")
    val modifiedTime: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "storage_path")
    val storagePath: String,
    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String? = null,
    @ColumnInfo(name = "word_count")
    val wordCount: Int = 0,
    @ColumnInfo(name = "chapter_count")
    val chapterCount: Int = 0,
    @ColumnInfo(name = "last_active_time")
    val lastActiveTime: Long = System.currentTimeMillis()
)

// AI对话记录表实体类
@Entity(
    tableName = "ai_conversations",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("project_id"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id", "created_time"], name = "idx_conversations_project"),
        Index(value = ["conversation_id"], name = "idx_conversations_session")
    ]
)
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "metadata")
    val metadata: String? = null,
    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int = 0,
    @ColumnInfo(name = "model_used")
    val modelUsed: String? = null,
    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis()
)

// 用户设置表实体类
@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "description")
    val description: String? = null
)

// AI模型配置表实体类
@Entity(
    tableName = "ai_model_configs",
    indices = [Index(value = ["config_name"], unique = true)]
)
data class AiModelConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "config_name")
    val configName: String,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "api_key_hash")
    val apiKeyHash: String,
    @ColumnInfo(name = "base_url")
    val baseUrl: String? = null,
    @ColumnInfo(name = "model_name")
    val modelName: String,
    @ColumnInfo(name = "temperature")
    val temperature: Float = 0.7f,
    @ColumnInfo(name = "max_tokens")
    val maxTokens: Int = 2000,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis()
)

// 项目-AI配置关联表实体类
@Entity(
    tableName = "project_ai_configs",
    primaryKeys = ["project_id", "config_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("project_id"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AiModelConfigEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("config_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProjectAiConfigEntity(
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "config_id")
    val configId: Int,
    @ColumnInfo(name = "priority")
    val priority: Int = 0,
    @ColumnInfo(name = "created_time")
    val createdTime: Long = System.currentTimeMillis()
)

// 最近文件表实体类
@Entity(
    tableName = "recent_files",
    indices = [Index(value = ["project_id", "last_opened_time"], name = "idx_recent_files_project")],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("project_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "last_opened_time")
    val lastOpenedTime: Long,
    @ColumnInfo(name = "open_count")
    val openCount: Int = 1
)
```
#### 4.4.2 Room Data Access Objects (DAO) 示例
```kotlin
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY modified_time DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE status = :status ORDER BY modified_time DESC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)
    
    @Update
    suspend fun updateProject(project: ProjectEntity)
    
    @Delete
    suspend fun deleteProject(project: ProjectEntity)
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?
    
    @Query("SELECT COUNT(*) FROM projects WHERE status = :status")
    suspend fun getProjectCountByStatus(status: String): Int
    
    @Query("UPDATE projects SET last_active_time = :timestamp WHERE id = :projectId")
    suspend fun updateLastActiveTime(projectId: String, timestamp: Long)
}

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations WHERE project_id = :projectId ORDER BY created_time DESC")
    fun getConversationsByProject(projectId: String): Flow<List<AiConversationEntity>>
    
    @Query("SELECT * FROM ai_conversations WHERE conversation_id = :conversationId ORDER BY created_time ASC")
    fun getConversationHistory(conversationId: String): Flow<List<AiConversationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity)
    
    @Query("DELETE FROM ai_conversations WHERE project_id = :projectId")
    suspend fun deleteConversationsByProject(projectId: String)
    
    @Query("SELECT DISTINCT conversation_id FROM ai_conversations WHERE project_id = :projectId ORDER BY created_time DESC")
    fun getConversationIdsByProject(projectId: String): Flow<List<String>>
}

@Dao
interface AiModelConfigDao {
    @Query("SELECT * FROM ai_model_configs")
    fun getAllConfigs(): Flow<List<AiModelConfigEntity>>
    
    @Query("SELECT * FROM ai_model_configs WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultConfig(): AiModelConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AiModelConfigEntity)
    
    @Update
    suspend fun updateConfig(config: AiModelConfigEntity)
    
    @Delete
    suspend fun deleteConfig(config: AiModelConfigEntity)
    
    @Query("SELECT * FROM ai_model_configs WHERE id = :configId")
    suspend fun getConfigById(configId: Int): AiModelConfigEntity?
}
```
#### 4.4.3 Room数据库配置类
```kotlin
@Database(
    entities = [
        ProjectEntity::class,
        AiConversationEntity::class,
        UserSettingEntity::class,
        AiModelConfigEntity::class,
        ProjectAiConfigEntity::class,
        RecentFileEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun userSettingDao(): UserSettingDao
    abstract fun aiModelConfigDao(): AiModelConfigDao
    abstract fun projectAiConfigDao(): ProjectAiConfigDao
    abstract fun recentFileDao(): RecentFileDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickwrite_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 数据库创建时执行初始化操作
                db.execSQL("INSERT INTO user_settings (key, value, description) VALUES ('theme', 'system', '应用主题设置')")
                db.execSQL("INSERT INTO user_settings (key, value, description) VALUES ('auto_save', 'true', '自动保存设置')")
            }
        }
    }
}
```
#### 4.4.4 使用Repository模式封装数据库操作
```kotlin
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val fileSystemManager: FileSystemManager,
    private val ioDispatcher: CoroutineDispatcher
) {
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(ioDispatcher)
    
    suspend fun createProject(project: Project): Result<Project> = withContext(ioDispatcher) {
        return@withContext try {
            // 1. 创建项目目录结构
            val storagePath = fileSystemManager.createProjectDirectory(project.id, project.toConfig())
            if (storagePath == null) {
                return@withContext Result.failure(Exception("创建项目目录失败"))
            }
            
            // 2. 保存到数据库
            val entity = project.copy(storagePath = storagePath).toEntity()
            projectDao.insertProject(entity)
            
            // 3. 更新项目文件统计
            updateProjectStatistics(project.id)
            
            Result.success(project.copy(storagePath = storagePath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun updateProjectStatistics(projectId: String) {
        val stats = fileSystemManager.getProjectStatistics(projectId)
        projectDao.getProjectById(projectId)?.let { project ->
            val updated = project.copy(
                wordCount = stats.wordCount,
                chapterCount = stats.chapterCount,
                modifiedTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis()
            )
            projectDao.updateProject(updated)
        }
    }
    
    // 扩展函数：Entity转Domain
    private fun ProjectEntity.toDomain(): Project = Project(
        id = id,
        title = title,
        author = author,
        genre = genre,
        description = description,
        createdTime = createdTime,
        modifiedTime = modifiedTime,
        status = status,
        storagePath = storagePath,
        coverImagePath = coverImagePath,
        wordCount = wordCount,
        chapterCount = chapterCount
    )
    
    // 扩展函数：Domain转Entity
    private fun Project.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        title = title,
        author = author,
        genre = genre,
        description = description,
        createdTime = createdTime,
        modifiedTime = modifiedTime,
        status = status,
        storagePath = storagePath,
        coverImagePath = coverImagePath,
        wordCount = wordCount,
        chapterCount = chapterCount,
        lastActiveTime = lastActiveTime
    )
}
```

#### 4.4.2 关键索引
```sql
-- 性能优化索引
CREATE INDEX idx_projects_modified ON projects(modified_time DESC);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_conversations_project ON ai_conversations(project_id, created_time DESC);
CREATE INDEX idx_conversations_session ON ai_conversations(conversation_id);
CREATE INDEX idx_recent_files_project ON recent_files(project_id, last_opened_time DESC);
CREATE INDEX idx_search_history_project ON search_history(project_id, created_time DESC);
```

#### 4.4.3 视图（简化查询）
```sql
-- 项目概览视图
CREATE VIEW v_project_overview AS
SELECT 
    p.*,
    COUNT(DISTINCT c.conversation_id) as conversation_count,
    MAX(c.created_time) as last_conversation_time
FROM projects p
LEFT JOIN ai_conversations c ON p.id = c.project_id
GROUP BY p.id;

-- 项目AI配置视图
CREATE VIEW v_project_ai_configs AS
SELECT 
    p.id as project_id,
    p.title as project_title,
    amc.*
FROM projects p
JOIN project_ai_configs pac ON p.id = pac.project_id
JOIN ai_model_configs amc ON pac.config_id = amc.id
ORDER BY pac.priority DESC;

-- 最近活动项目视图
CREATE VIEW v_recent_projects AS
SELECT 
    p.*,
    MAX(rf.last_opened_time) as last_file_opened
FROM projects p
LEFT JOIN recent_files rf ON p.id = rf.project_id
GROUP BY p.id
ORDER BY COALESCE(MAX(rf.last_opened_time), p.last_active_time) DESC;
```

**数据库设计原则**:
1. **最小化存储**: 只存储必要的元数据，文档内容在文件系统中
2. **对话记录**: 完整记录AI对话历史，便于追溯和继续
3. **快速访问**: 通过索引和视图优化常用查询
4. **配置分离**: AI配置与项目分离，支持多模型切换
5. **性能优先**: 优化常用操作的性能和响应速度

**文件系统与数据库的分工**:
- **文件系统**: 存储所有文档内容、设定、时间线等
- **数据库**: 存储项目列表、对话记录、用户设置、搜索历史等元数据
- **集成**: 通过文件路径关联文件系统中的文档和数据库中的项目

## 10. 系统架构总结

### 10.1 核心设计理念
**文档驱动 + AI探索 + 文件系统优先**

1. **文档驱动**: 所有小说设定、时间线、记录都以纯文本/Markdown文件形式存储
2. **文件系统存储**: 项目结构完全由文件系统管理，数据库只存元数据
3. **AI自主探索**: AI agent能够主动探索项目目录，读取相关文档
4. **系统提示词集成**: 每个项目的AI agent都包含项目简介和文档结构信息

### 10.2 AI agent工作流程
```
用户请求 → 系统识别请求类型 → AI agent根据系统提示词理解项目 → 
agent探索相关文档 → 读取文档内容 → 整合信息 → 生成回答/创作内容
```

### 10.3 项目结构示例
```
/QuickWrite_Projects/
├── project_001_倚天屠龙记/
│   ├── 简介.md                # 包含在AI系统提示词中
│   ├── 目录结构说明.md        # 包含在AI系统提示词中
│   ├── 正文/
│   ├── 设定/
│   ├── 时间线/
│   ├── 记录/
│   └── 配置/                  # 包含AI指令.md
│
├── project_002_西游记/
│   ├── 简介.md
│   ├── 正文/
│   └── ...
│
└── project_003_科幻小说/
    ├── 简介.md
    └── ...
```

### 4.5 Compose UI与ViewModel集成

```kotlin
// ViewModel示例：项目管理
@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val fileSystemManager: FileSystemManager
) : ViewModel() {
    
    // UI状态管理
    private val _uiState = MutableStateFlow<ProjectListUiState>(ProjectListUiState.Loading)
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()
    
    // 项目列表状态（使用Room的Flow）
    val projects = projectRepository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 项目详情状态
    private val _projectDetail = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val projectDetail: StateFlow<ProjectDetailUiState> = _projectDetail.asStateFlow()
    
    // 项目操作
    fun createProject(project: Project) = viewModelScope.launch {
        _uiState.value = ProjectListUiState.Loading
        val result = projectRepository.createProject(project)
        _uiState.value = when (result) {
            is Result.Success -> ProjectListUiState.Success("项目创建成功")
            is Result.Failure -> ProjectListUiState.Error(result.exception.message ?: "创建项目失败")
        }
    }
    
    fun loadProjectDetails(projectId: String) = viewModelScope.launch {
        _projectDetail.value = ProjectDetailUiState.Loading
        val project = projectRepository.getProjectById(projectId)
        val stats = fileSystemManager.getProjectStatistics(projectId)
        project?.let {
            _projectDetail.value = ProjectDetailUiState.Success(
                project = project,
                statistics = stats
            )
        } ?: run {
            _projectDetail.value = ProjectDetailUiState.Error("项目不存在")
        }
    }
    
    // UI状态密封类
    sealed class ProjectListUiState {
        data object Loading : ProjectListUiState()
        data class Success(val message: String) : ProjectListUiState()
        data class Error(val errorMessage: String) : ProjectListUiState()
    }
    
    sealed class ProjectDetailUiState {
        data object Loading : ProjectDetailUiState()
        data class Success(
            val project: Project,
            val statistics: ProjectStatistics
        ) : ProjectDetailUiState()
        data class Error(val errorMessage: String) : ProjectDetailUiState()
    }
}

// Compose屏幕示例：项目列表
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部标题
        Text(
            text = "我的项目",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 项目列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onClick = { /* 导航到详情 */ },
                    onLongClick = { /* 显示选项菜单 */ }
                )
            }
        }
        
        // 处理UI状态
        when (val state = uiState) {
            is ProjectViewModel.ProjectListUiState.Error -> {
                LaunchedEffect(state) {
                    // 显示错误Snackbar
                }
            }
            else -> {}
        }
    }
}

// Compose组件：项目卡片
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 项目封面（如果有）
            project.coverImagePath?.let { imagePath ->
                AsyncImage(
                    model = imagePath,
                    contentDescription = "${project.title}封面",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
            
            // 项目信息
            Text(
                text = project.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = project.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 项目统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 字数统计
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${project.wordCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "字数",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // 章节统计
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${project.chapterCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "章节",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // 状态
                Chip(
                    label = {
                        Text(
                            text = when (project.status) {
                                "ACTIVE" -> "进行中"
                                "COMPLETED" -> "已完成"
                                "PAUSED" -> "暂停"
                                else -> project.status
                            }
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        containerColor = when (project.status) {
                            "ACTIVE" -> MaterialTheme.colorScheme.primaryContainer
                            "COMPLETED" -> MaterialTheme.colorScheme.secondaryContainer
                            "PAUSED" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        labelColor = when (project.status) {
                            "ACTIVE" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "COMPLETED" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "PAUSED" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }
            
            // 最后修改时间
            Text(
                text = "最后修改：${formatTime(project.modifiedTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Compose屏幕示例：项目创建
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    onProjectCreated: (Project) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val viewModel: ProjectViewModel = hiltViewModel()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 表单标题
        Text(
            text = "创建新项目",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 表单字段
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("项目标题 *") },
            isError = title.isBlank(),
            supportingText = {
                if (title.isBlank()) {
                    Text("请输入项目标题")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("作者名称 *") },
            isError = author.isBlank(),
            supportingText = {
                if (author.isBlank()) {
                    Text("请输入作者名称")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 小说类型选择
        var expanded by remember { mutableStateOf(false) }
        val genres = listOf("历史", "奇幻", "科幻", "都市", "玄幻", "武侠", "言情", "悬疑", "其他")
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("小说类型 *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                genres.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            genre = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("项目描述（可选）") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 取消按钮
            OutlinedButton(
                onClick = onCancel,
                enabled = !isLoading,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text("取消")
            }
            
            // 创建按钮
            Button(
                onClick = {
                    if (title.isNotBlank() && author.isNotBlank() && genre.isNotBlank()) {
                        isLoading = true
                        viewModelScope.launch {
                            val project = Project(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                author = author,
                                genre = genre,
                                description = if (description.isNotBlank()) description else null,
                                createdTime = System.currentTimeMillis(),
                                modifiedTime = System.currentTimeMillis(),
                                status = "ACTIVE",
                                storagePath = "",
                                wordCount = 0,
                                chapterCount = 0
                            )
                            
                            try {
                                viewModel.createProject(project)
                                onProjectCreated(project)
                            } catch (e: Exception) {
                                // 显示错误
                                Toast.makeText(
                                    context,
                                    "创建失败：${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "请填写所有必填字段",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = title.isNotBlank() && author.isNotBlank() && genre.isNotBlank() && !isLoading,
                modifier = Modifier
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("创建项目")
            }
        }
    }
}
```

### 10.4 Compose主题配置
```kotlin
// theme/Color.kt
val QuickWriteLightColorScheme = lightColorScheme(
    primary = Color(0xFF1a237e),
    secondary = Color(0xFF2196f3),
    tertiary = Color(0xFFff9800),
    background = Color(0xFFfafafa),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121)
)

val QuickWriteDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7986cb),
    secondary = Color(0xFF64b5f6),
    tertiary = Color(0xFFffb74d),
    background = Color(0xFF121212),
    surface = Color(0xFF1e1e1e),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFe0e0e0),
    onSurface = Color(0xFFe0e0e0)
)

// theme/Typography.kt
val QuickWriteTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )
)

// theme/Theme.kt
@Composable
fun QuickWriteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        QuickWriteDarkColorScheme
    } else {
        QuickWriteLightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuickWriteTypography,
        content = content
    )
}
```

### 10.5 系统提示词内容示例
```
# 小说创作助手 - 《倚天屠龙记》项目

## 项目简介
[从 简介.md 读取的内容]

## 项目文档结构
- 正文/: 正文章节
- 设定/: 人物、地点、组织、物品设定
- 时间线/: 时间管理和事件记录  
- 记录/: 创作过程记录
- 配置/: 项目配置

## 重要文件
- 设定/人物/张无忌.md
- 设定/组织/明教.md
- 时间线/总览.md

## 你的能力
你可以探索项目目录，读取文档，基于现有设定进行创作...
```

### 10.5 技术优势
1. **可移植性**: 项目目录可单独复制、备份、共享
2. **透明性**: 所有设定都是纯文本文件，易于查看和编辑
3. **灵活性**: AI agent能适应不同的项目结构
4. **可扩展性**: 新类型文档只需创建对应目录和文件
5. **兼容性**: 支持多种AI模型，只需OpenAI兼容API

### 10.6 使用Jetpack Compose和Room的优势

#### Compose优势：
1. **声明式UI**: 更直观的UI构建方式，状态驱动UI更新
2. **状态管理**: 内置的State和Flow支持，更好的数据流管理
3. **组件化**: 高度可重用的Composable组件，提高开发效率
4. **预览功能**: 实时预览UI组件，快速迭代设计
5. **Material 3**: 原生支持最新的Material Design规范
6. **动画支持**: 声明式动画系统，创建流畅的交互体验
7. **测试友好**: 更易于进行UI测试

#### Room优势：
1. **编译时检查**: SQL查询在编译时验证，减少运行时错误
2. **类型安全**: Kotlin原生类型支持，避免类型转换问题
3. **Flow集成**: 支持在数据变化时自动更新UI
4. **迁移支持**: 内置数据库迁移支持，简化版本升级
5. **性能优化**: 自动处理线程切换，避免主线程阻塞
6. **简化代码**: 自动生成大量样板代码，减少手写SQL需求

#### 结合优势：
1. **响应式架构**: Flow + Compose实现完全响应式UI更新
2. **单向数据流**: ViewModel处理业务逻辑，Composable只负责显示
3. **实时更新**: 数据库变化自动反映到UI
4. **更好的错误处理**: 结构化异常处理和状态管理
5. **模块化开发**: 清晰的层级分离，便于团队协作

### 10.7 开发重点
1. **文件系统管理**: 稳定可靠的文件操作
2. **AI探索器**: 高效的文档检索和读取
3. **Compose UI**: 构建现代化、响应式的用户界面
4. **Room数据库**: 类型安全的数据持久化
5. **系统提示词生成**: 自动整合项目信息
6. **对话管理**: 完整的AI对话历史记录
7. **性能优化**: 大文件处理和快速检索
8. **用户体验**: 流畅的动画和交互反馈

### 10.8 开发工具推荐
1. **IDE**: Android Studio最新版（支持Compose预览）
2. **性能分析**: Android Profiler, Layout Inspector
3. **调试工具**: Compose Debugger, Database Inspector
4. **构建工具**: Gradle with Kotlin DSL
5. **版本控制**: Git with conventional commits
6. **CI/CD**: GitHub Actions / Jenkins

---

*文档版本: 3.0*
*最后更新: 2025-04-21*
*更新内容: 更新为Jetpack Compose + Room架构*
*文档负责人: 产品经理/技术负责人*