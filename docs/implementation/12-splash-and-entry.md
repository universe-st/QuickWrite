# 启动与入口 (Splash Screen & Entry Points)

## 功能概述

管理应用的启动流程：Application 初始化、闪屏展示（1秒延迟）、主界面过渡动画。通过 `Crossfade` 实现平滑的闪屏到主界面切换。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| QuickWriteApplication | `QuickWriteApplication.kt` | Application 初始化，创建 AppContainer |
| MainActivity | `MainActivity.kt` | 唯一 Activity，设置 Compose 内容 (57行) |
| QuickWriterApp | `presentation/QuickWriterApp.kt` | 顶层 Composable，闪屏过渡 (32行) |
| SplashScreen | `presentation/ui/screens/SplashScreen.kt` | 闪屏 UI |

## 核心类/函数

### QuickWriteApplication
```kotlin
class QuickWriteApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        appContainer = AppContainer(this)
    }
}
```

### MainActivity
```kotlin
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        // 获取保存的语言设置
        val languageCode = try {
            val appContainer = (newBase?.applicationContext as? QuickWriteApplication)?.appContainer
            appContainer?.let { runBlocking { it.settingsUseCase.getLanguage() } }
                ?: LocaleHelper.CODE_SYSTEM
        } catch (_: Exception) { LocaleHelper.CODE_SYSTEM }
        val wrappedContext = newBase?.let { LocaleHelper.wrapContextForLocale(it, languageCode) }
        super.attachBaseContext(wrappedContext ?: newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedLanguage()
        enableEdgeToEdge()
        setContent {
            QuickWriterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuickWriterApp()
                }
            }
        }
    }

    private fun applySavedLanguage() {
        val appContainer = (application as QuickWriteApplication).appContainer
        val languageCode = runBlocking { appContainer.settingsUseCase.getLanguage() }
        LocaleHelper.applyLocale(this, languageCode)
    }
}
```

### QuickWriterApp
```kotlin
@Composable
fun QuickWriterApp() {
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)           // 1 秒闪屏延迟
        isReady = true
    }

    Crossfade(targetState = isReady, label = "splash") { ready ->
        if (!ready) {
            SplashScreen()
        } else {
            MainScreen()
        }
    }
}
```

### SplashScreen
```kotlin
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    statusText: String = ""
)
```
- SplashGradient 渐变背景
- `Icons.Default.Create` 图标 (80dp)
- 应用名 "QuickWrite" (32sp Bold)
- 标语文字
- 状态文字（底部）

## 启动流程

```
应用启动
    │
    ▼
QuickWriteApplication.onCreate()
    ├─ Timber.plant(DebugTree())             // Debug 日志
    └─ AppContainer(this)                    // 创建 DI 容器
    │
    ▼
MainActivity.attachBaseContext()
    ├─ 读取语言设置
    └─ LocalHelper.wrapContextForLocale()    // 包装 Context
    │
    ▼
MainActivity.onCreate()
    ├─ applySavedLanguage()                  // 应用语言环境
    ├─ enableEdgeToEdge()                    // 全屏显示
    └─ setContent { QuickWriterTheme { QuickWriterApp() } }
    │
    ▼
QuickWriterApp()
    ├─ LaunchedEffect: delay(1000)
    │    └─ 显示 SplashScreen (1秒)
    ├─ isReady = true
    └─ Crossfade → MainScreen()
         ├─ NavigationBar
         └─ NavHost (startDestination = "project_list")
```

## 关键实现细节

### 闪屏策略
- 纯静态 Composable：`SplashScreen` 不包含任何状态变化或副作用
- 1 秒固定延迟（`delay(1000)`），不依赖数据加载
- 使用 `Crossfade` 动画实现平滑过渡

### 语言初始化
采用两阶段语言设置：
1. `attachBaseContext()` — 在 Activity 附加 Base Context 时提前设置语言 Context（确保资源加载时语言已正确）
2. `onCreate()` → `applySavedLanguage()` — 通过 `AppCompatDelegate` 应用语言设置

### Edge-to-Edge
`enableEdgeToEdge()` 使应用内容延伸到系统栏区域，配合 `WindowInsets(0.dp)` 实现沉浸式显示。

### 主题初始化
`QuickWriterTheme` 不传 `darkTheme` 参数，默认使用 `isSystemInDarkTheme()`。后续可通过设置中的主题模式选择覆盖。

## 已知问题/技术债务

1. `attachBaseContext()` 中使用 `runBlocking` 同步读取数据库语言设置，可能阻塞 UI 线程
2. 闪屏 1 秒延迟是固定值，与设备性能无关
3. 对于启动时的异常（如数据库初始化失败），缺少错误状态处理和降级展示
4. Timber 日志仅在 Debug 模式下初始化，Release 模式无日志记录
