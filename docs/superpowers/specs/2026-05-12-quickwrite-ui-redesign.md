# QuickWrite UI 全面重构设计规范

**日期**: 2026-05-12  
**状态**: 已确认  
**设计方向**: 纸墨写作风 (Warm Literary / Ink & Paper)  

---

## 1. 设计理念

将 QuickWrite 从"深蓝色工具应用"转变为"暖色调写作伴侣"。核心理念：**像在稿纸上写作**。

关键词：温暖、专注、文学感、纸质触感、克制优雅。

---

## 2. 色彩系统

### 2.1 语义色板（浅色模式）

| 角色 | Hex | Tailwind | 用途 |
|------|-----|----------|------|
| `primary` | `#78716C` | warm-stone-500 | 导航栏、主按钮、关键强调、选中态 |
| `onPrimary` | `#FFFFFF` | white | 主色上的文字/图标 |
| `primaryContainer` | `#E7E5E4` | warm-stone-200 | 主色的容器/背景（如选中卡片） |
| `onPrimaryContainer` | `#44403C` | warm-stone-700 | 主色容器上的文字 |
| `secondary` | `#A8A29E` | warm-stone-400 | 辅助按钮、选项卡指示器 |
| `onSecondary` | `#FFFFFF` | white | 辅助色上的文字 |
| `secondaryContainer` | `#F5F5F4` | warm-stone-100 | 辅助色容器 |
| `onSecondaryContainer` | `#57534E` | warm-stone-600 | 辅助色容器上的文字 |
| `tertiary` | `#D97706` | amber-600 | FAB、CTA 按钮、重要提示 |
| `onTertiary` | `#FFFFFF` | white | 第三色上的文字 |
| `tertiaryContainer` | `#FEF3C7` | amber-100 | 第三色容器（如警告卡片） |
| `onTertiaryContainer` | `#92400E` | amber-800 | 第三色容器上的文字 |
| `background` | `#FFFBEB` | cream-50 | 页面背景（奶油纸色） |
| `onBackground` | `#0F172A` | slate-900 | 页面背景上的主要文字 |
| `surface` | `#FFFFFF` | white | 卡片、对话框、BottomSheet |
| `onSurface` | `#0F172A` | slate-900 | surface 上的主要文字 |
| `surfaceVariant` | `#F5F0E8` | warm-100 | 输入框背景、工具栏背景 |
| `onSurfaceVariant` | `#44403C` | warm-stone-700 | surfaceVariant 上的文字 |
| `outline` | `#D6D3D1` | warm-stone-300 | 边框/分割线 |
| `outlineVariant` | `#E7E5E4` | warm-stone-200 | 弱分割线 |
| `error` | `#DC2626` | red-600 | 错误/删除/破坏性操作 |
| `onError` | `#FFFFFF` | white | 错误色上的文字 |
| `errorContainer` | `#FEE2E2` | red-100 | 错误提示背景 |
| `onErrorContainer` | `#991B1B` | red-800 | 错误容器上的文字 |
| `inverseSurface` | `#1C1917` | warm-stone-900 | 深色表面（暗色模式基础） |
| `inverseOnSurface` | `#F5F0E8` | warm-100 | 深色表面上文字 |
| `inversePrimary` | `#A8A29E` | warm-stone-400 | 深色背景上主色 |

### 2.2 暗色模式

| 角色 | Hex | 说明 |
|------|-----|------|
| `background` | `#1C1917` | 深棕底色 |
| `surface` | `#282421` | 卡片表面 |
| `primary` | `#A8A29E` | 主色提亮 |
| `onBackground` | `#F5F0E8` | 正文提亮 |
| `tertiary` | `#F59E0B` | 琥珀提亮 |

暗色模式保持纸墨质感：低饱和度、暖灰调、不过度锐利。

### 2.3 渐变色

移除所有硬编码渐变色（`Gradient.kt` 删除）。项目列表 TopAppBar 改用纯色 `primary` 背景，FAB 使用 `tertiary`（琥珀）标准 M3 FAB。

---

## 3. 字体系统

### 3.1 字体家族

| 角色 | 字体 | 类型 | 用途 |
|------|------|------|------|
| Display | Cormorant Garamond | Serif | 大标题（28sp+） |
| Headline | Cormorant Garamond | Serif | 页面标题（24sp） |
| Title | Libre Baskerville | Serif | 卡片标题、对话框标题（20sp） |
| Body | Libre Baskerville | Serif | 正文文字（14-16sp） |
| Label | Inter | Sans | 标签、徽章、辅助文字（11-14sp） |
| Code | JetBrains Mono | Mono | 代码块、Markdown 代码（14sp） |

### 3.2 中文回退

- 衬线标题回退：Noto Serif SC（简体）/ Noto Serif TC（繁体）
- 正文 UI 回退：Noto Sans SC / Noto Sans TC

### 3.3 M3 Typography 重映射

```
displayLarge   -> Cormorant Garamond 600 28sp
headlineLarge  -> Cormorant Garamond 600 24sp
headlineMedium -> Cormorant Garamond 500 22sp
titleLarge     -> Libre Baskerville 700 20sp
titleMedium    -> Libre Baskerville 600 16sp
titleSmall     -> Libre Baskerville 500 14sp
bodyLarge      -> Libre Baskerville 400 16sp
bodyMedium     -> Libre Baskerville 400 14sp
bodySmall      -> Libre Baskerville 400 12sp
labelLarge     -> Inter 500 14sp
labelMedium    -> Inter 500 12sp
labelSmall     -> Inter 500 11sp
```

---

## 4. 动效系统

### 4.1 动画参数

| 场景 | 时长 | 缓动 | 说明 |
|------|------|------|------|
| 微交互（按钮按压） | 100ms | FastOutSlowInEasing | Ripple + scale(0.97) |
| 过渡（页面切换） | 250ms | FastOutLinearInEasing (enter) / LinearOutSlowInEasing (exit) | Fade + 轻微位移 |
| 组件展开/收起 | 300ms | FastOutSlowInEasing | Slide + Fade |
| 列表项入场 | 30-50ms stagger | FastOutSlowInEasing | 每个 item 延迟递增 |
| 通知/Toast | 200ms in / 150ms out | FastOutSlowInEasing | Fade + Slide from top |

### 4.2 动效原则

- 所有动画必须可中断（用户操作立即响应）
- 尊重 `prefers-reduced-motion`：动画替换为即时切换
- 不阻塞用户输入
- 每个动效传达因果关系（进=更深层级, 出=返回上级）
- 退出动画为进入动画的 60-70% 时长
- 仅在 transform/opacity 上做动画（避免 layout 重排）

### 4.3 页面过渡

- 前进导航：fadeIn + 轻微 slideInHorizontally(+50dp)
- 返回导航：fadeOut + slideOutHorizontally(+50dp)
- Modal/BottomSheet：从触发源展开（scale + fade）

---

## 5. 页面级改动清单

### 5.1 启动页 (SplashScreen)
- 背景改为 `background` 色 + 微纹理（纸张噪点感）
- Logo 图标改用 `primary` 色
- 标题使用 Cormorant Garamond Display，添加淡入动画
- 保持无动效启动的简洁原则

### 5.2 项目列表 (ProjectListScreen)
- TopAppBar：`primary` 纯色背景（移除渐变）
- FAB：标准 M3 `FloatingActionButton`，使用 `tertiary`（琥珀）色
- ProjectCard：增大封面纵向占比，改为 3:4 竖版
- 空状态：加入纸质纹理背景插图
- 排序指示器：当前排序方式有视觉标识

### 5.3 项目卡片 (ProjectCard)
- 封面比例改为 2:3 竖版（原 72x96dp → 80x120dp）
- 圆角改为 8dp（原 12dp）
- 按下效果：M3 标准 ripple + scale(0.97)
- 移除 detectTapGestures 自定义处理，改用 Card onClick
- 类型徽章：`secondaryContainer` 背景
- 元数据使用 `onSurfaceVariant` 替代硬编码 `TextSecondary`

### 5.4 写作编辑器 (WritingScreen)
- TopAppBar：`surfaceVariant` 背景（保持与内容区区分）
- TabRow：迁移到非弃用 API，指示器用 `primary` 色
- 编辑区背景：`surface`（白色卡片感）
- 文件侧栏：`surfaceVariant` 背景，圆角 `medium` 右边缘
- 暗色模式检测：使用 `isSystemInDarkTheme()` 替代颜色相等比较
- Markdown 高亮适配新字体

### 5.5 AI 对话 (ChatTab)
- 用户气泡：`primaryContainer` 背景，圆角不对称（右下角小）
- AI 气泡：`surfaceVariant` 背景，圆角不对称（左上角小）
- 输入框：`surfaceVariant` 背景，20dp 圆角，琥珀色发送按钮
- 回到底部按钮：`surfaceContainerHigh` 背景
- 加载态：骨架屏替代纯文字 loading
- 参考块：`tertiaryContainer` 背景

### 5.6 设置页面 (SettingsScreen)
- TopAppBar：`primary` 纯色背景
- 设置分组卡片：`surface` 背景，`medium` 圆角
- 标题色：`primary`
- 底部内容加 padding 避免被系统导航栏遮挡

### 5.7 AI 配置 (AiConfigScreen)
- TopAppBar：`surfaceVariant` 背景
- 默认模型卡片：`primaryContainer` 背景
- 提供商标识：简化 when 分支，提取为工具函数
- 表单输入框：标准圆角 8dp

### 5.8 关于页 (AboutScreen)
- 背景：`background`
- 版本信息：Label 字体 + `onSurfaceVariant` 色

---

## 6. 技术债务修复清单

| 问题 | 修复方案 |
|------|----------|
| `TextSecondary` 硬编码 `#757575` | 全部替换为 `MaterialTheme.colorScheme.onSurfaceVariant` |
| 硬编码字号 (sp) | 全部替换为 `MaterialTheme.typography.*.fontSize` |
| `WindowInsets(0.dp)` | 各页面按需设置 `contentWindowInsets`，尊重系统安全区 |
| 暗色模式颜色相等比较 | 替换为 `isSystemInDarkTheme()` |
| 弃用 `TabRow` API | 迁移到新版 `TabRow` |
| 弃用 `LocalClipboardManager` | 迁移到 `ClipboardManager` |
| `Gradient.kt` 硬编码渐变色 | 文件删除，用纯色替代 |
| 多处 when 分支重复 | 提取为工具函数或映射表 |
| 加载动画重复 | 提取 `LoadingDots` 为公共组件 |
| 无自定义字体加载 | 添加 Google Fonts 下载 + 字体资源 |
| `PlaceholderScreen` 未使用代码 | 清理 |
| `detectTapGestures` 代替 Card onClick | 改用标准 Card 交互 |

---

## 7. 文件变更范围

### 修改文件
- `ui/theme/Color.kt` — 全新色板 + 暗色模式色板
- `ui/theme/Type.kt` — 新字体映射
- `ui/theme/Theme.kt` — 字体注入 + 暗色检测逻辑
- `ui/theme/Gradient.kt` — **删除**
- `ui/theme/Shape.kt` — 微调（卡片 8dp, 按钮 8dp）

### 重做页面
- `presentation/ui/screens/SplashScreen.kt`
- `presentation/ui/screens/ProjectListScreen.kt`
- `presentation/ui/screens/WritingScreen.kt`
- `presentation/ui/screens/ChatTab.kt`
- `presentation/ui/screens/SettingsScreen.kt`
- `presentation/ui/screens/AiConfigScreen.kt`
- `presentation/ui/screens/AboutScreen.kt`
- `presentation/ui/screens/ProjectCreateScreen.kt`
- `presentation/ui/screens/ProjectEditScreen.kt`
- `presentation/ui/screens/ProjectDetailScreen.kt`

### 重做组件
- `presentation/ui/components/ProjectCard.kt`
- `presentation/ui/components/ChatBubble.kt`
- `presentation/ui/components/ToolExecutionCard.kt`
- `presentation/ui/components/SettingsComponents.kt`
- `presentation/ui/components/ProjectCoverImage.kt`

### 新增文件
- `ui/theme/Font.kt` — 自定义字体加载
- `app/src/main/res/font/` — 字体文件（Cormorant Garamond, Libre Baskerville, Inter, JetBrains Mono）

### 可能新增
- `ui/components/LoadingDots.kt` — 提取公共加载动画
- `util/ToolDisplayHelper.kt` — 提取工具显示名映射
- `util/ProviderDisplayHelper.kt` — 提取 AI 提供商显示名映射

---

## 8. 不变更范围

- 数据库结构（Entity、DAO、Room）
- 网络层（Retrofit、OkHttp）
- 业务逻辑（UseCase、Repository）
- ViewModel 核心逻辑
- 文件系统（FileManager、ChapterFileHelper）
- markor-editor 模块内部逻辑
- 导航路由结构
- 国际化字符串资源（strings.xml 无需改动，因为颜色/字体变更不影响文本）

---

## 9. 实施顺序建议

1. **主题层** — Color.kt → Type.kt → Font.kt → Theme.kt → Shape.kt → 删除 Gradient.kt
2. **基础组件** — ProjectCard → ChatBubble → ToolExecutionCard → SettingsComponents → ProjectCoverImage
3. **核心页面** — ProjectListScreen → WritingScreen → ChatTab
4. **次级页面** — SettingsScreen → AiConfigScreen → 其余页面
5. **启动页与收尾** — SplashScreen → 全局 Review

---

## 10. 验证清单

- [ ] 浅色/暗色模式切换无闪烁
- [ ] 所有文字对比度 ≥4.5:1（AA）
- [ ] 支持系统字体缩放（Dynamic Type / Accessibility）
- [ ] 支持 reduced-motion（动画降级为即时切换）
- [ ] 安全区适配（状态栏、导航栏、刘海屏）
- [ ] 中文/繁体中文/英文三语言无布局异常
- [ ] `./gradlew :app:assembleDebug` 编译通过
