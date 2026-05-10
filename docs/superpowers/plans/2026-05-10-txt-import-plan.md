# TXT 小说导入 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement TXT novel import with automatic chapter detection, encoding auto-detection, and streaming file parsing.

**Architecture:** New `TxtChapterParser` utility handles encoding detection + chapter splitting via regex patterns. `ProjectManagementUseCase.importProjectFromTxt()` orchestrates the flow (like existing `importProjectFromZip`). UI adds a menu item in `ProjectListScreen` and a `TxtImportDialog` in `MainScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, juniversalchardet 1.0.3, existing Room/FileManager/ChapterFileHelper

**Design Spec:** `docs/superpowers/specs/2026-05-10-txt-import-design.md`

---

### Task 1: Add juniversalchardet Dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version and library entry to libs.versions.toml**

In `gradle/libs.versions.toml`, add to `[versions]` block:
```toml
juniversalchardet = "1.0.3"
```

In `gradle/libs.versions.toml`, add to `[libraries]` block:
```toml
juniversalchardet = { group = "com.googlecode.juniversalchardet", name = "juniversalchardet", version.ref = "juniversalchardet" }
```

- [ ] **Step 2: Add implementation dependency to app/build.gradle.kts**

In `app/build.gradle.kts`, find the `dependencies { }` block and add after the last `implementation(...)` line:
```kotlin
    implementation(libs.juniversalchardet)
```

- [ ] **Step 3: Sync and verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: add juniversalchardet dependency for TXT encoding detection"
```

---

### Task 2: Create TxtChapterParser (Data Types + ChapterPattern Enum)

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/util/TxtChapterParser.kt`

This task creates the data types only (enum + data class). Implementation follows in Task 4.

- [ ] **Step 1: Create TxtChapterParser.kt with ChapterPattern enum and ChapterSlice**

```kotlin
package com.universe_st.quickwriter.util

enum class ChapterPattern(val displayNameKey: Int, val regex: String?) {
    CN_CHAPTER(com.universe_st.quickwriter.R.string.txt_import_pattern_cn, """^第[\d零一二三四五六七八九十百千万]+[章回卷节].*$"""),
    EN_CHAPTER(com.universe_st.quickwriter.R.string.txt_import_pattern_en, """^[Cc]hapter\s+\d+.*$"""),
    NUM_HEADING(com.universe_st.quickwriter.R.string.txt_import_pattern_num, """^\d+[\.、．\s].*$"""),
    VOLUME(com.universe_st.quickwriter.R.string.txt_import_pattern_vol, """^第[\d零一二三四五六七八九十百千万]+卷.*$"""),
    CUSTOM(com.universe_st.quickwriter.R.string.txt_import_pattern_custom, null);

    fun buildRegex(customRegex: String?): Regex? {
        return when (this) {
            CUSTOM -> if (customRegex.isNullOrBlank()) null else try { Regex(customRegex) } catch (_: Exception) { null }
            else -> regex?.let { Regex(it) }
        }
    }
}

data class ChapterSlice(
    val index: Int,
    val title: String,
    val body: String,
    val isVolumeHeader: Boolean = false
)
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/TxtChapterParser.kt
git commit -m "feat: add ChapterPattern enum and ChapterSlice data class for TXT parsing"
```

---

### Task 3: Create TxtEncodingDetector

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/util/TxtEncodingDetector.kt`

- [ ] **Step 1: Create TxtEncodingDetector.kt**

```kotlin
package com.universe_st.quickwriter.util

import org.mozilla.universalchardet.UniversalDetector
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

object TxtEncodingDetector {

    private val CHARSET_MAPPING = mapOf(
        "GB2312" to "GBK",
        "GB18030" to "GBK",
        "Big5-HKSCS" to "Big5"
    )

    fun detectEncoding(file: File): Charset {
        return try {
            val detector = UniversalDetector(null)
            FileInputStream(file).use { input ->
                val buf = ByteArray(4096)
                var nread: Int
                while (input.read(buf).also { nread = it } > 0 && !detector.isDone) {
                    detector.handleData(buf, 0, nread)
                }
                detector.dataEnd()
            }
            val detected = detector.detectedCharset
            val mapped = if (detected != null) CHARSET_MAPPING[detected] ?: detected else null
            val result = if (mapped != null) {
                try {
                    Charset.forName(mapped)
                } catch (_: Exception) {
                    Charsets.UTF_8
                }
            } else {
                Charsets.UTF_8
            }
            Timber.tag("TxtEncoding").i("Detected: %s -> mapped: %s -> %s", detected, mapped, result.name())
            result
        } catch (e: Exception) {
            Timber.tag("TxtEncoding").e(e, "Encoding detection failed, fallback to UTF-8")
            Charsets.UTF_8
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/TxtEncodingDetector.kt
git commit -m "feat: add TxtEncodingDetector for auto-detecting TXT file encoding"
```

---

### Task 4: Implement TxtChapterParser (Streaming Parse Logic)

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/util/TxtChapterParser.kt`

This extends the file created in Task 2 with the streaming parse method.

- [ ] **Step 1: Add parseChapters method to TxtChapterParser.kt**

Replace the content of `TxtChapterParser.kt` with the full implementation:

```kotlin
package com.universe_st.quickwriter.util

import android.content.Context
import android.net.Uri
import com.universe_st.quickwriter.R
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class ChapterPattern(val displayNameKey: Int, val regex: String?) {
    CN_CHAPTER(R.string.txt_import_pattern_cn, """^第[\d零一二三四五六七八九十百千万]+[章回卷节].*$"""),
    EN_CHAPTER(R.string.txt_import_pattern_en, """^[Cc]hapter\s+\d+.*$"""),
    NUM_HEADING(R.string.txt_import_pattern_num, """^\d+[\.、．\s].*$"""),
    VOLUME(R.string.txt_import_pattern_vol, """^第[\d零一二三四五六七八九十百千万]+卷.*$"""),
    CUSTOM(R.string.txt_import_pattern_custom, null);

    fun buildRegex(customRegex: String?): Regex? {
        return when (this) {
            CUSTOM -> if (customRegex.isNullOrBlank()) null else try { Regex(customRegex) } catch (_: Exception) { null }
            else -> regex?.let { Regex(it) }
        }
    }
}

data class ChapterSlice(
    val index: Int,
    val title: String,
    val body: String,
    val isVolumeHeader: Boolean = false
)

data class TxtParseResult(
    val preludeBody: String?,
    val chapters: List<ChapterSlice>
)

object TxtChapterParser {

    fun parseChapters(
        context: Context,
        txtUri: Uri,
        charset: java.nio.charset.Charset,
        selectedPatterns: Set<ChapterPattern>,
        customRegex: String?
    ): TxtParseResult {
        val patterns = selectedPatterns.mapNotNull { it.buildRegex(customRegex) }
        val preludeLines = mutableListOf<String>()
        val chapterSlices = mutableListOf<ChapterSlice>()
        val currentBody = StringBuilder()

        var currentTitle: String? = null
        var chapterIndex = 0

        context.contentResolver.openInputStream(txtUri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, charset))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) {
                    currentBody.appendLine()
                    continue
                }

                val matchedPattern = patterns.find { it.matches(trimmed) }
                if (matchedPattern != null) {
                    if (currentTitle != null) {
                        chapterSlices.add(
                            ChapterSlice(
                                index = chapterIndex,
                                title = currentTitle,
                                body = currentBody.toString().trimEnd()
                            )
                        )
                        chapterIndex++
                    } else {
                        val preludeText = currentBody.toString().trimEnd()
                        if (preludeText.isNotEmpty()) {
                            preludeLines.add(preludeText)
                        }
                    }
                    currentTitle = trimmed.take(100).trim()
                    currentBody.clear()
                } else {
                    if (currentTitle != null) {
                        currentBody.appendLine(line!!)
                    } else {
                        currentBody.appendLine(line!!)
                    }
                }
            }

            if (currentTitle != null) {
                chapterSlices.add(
                    ChapterSlice(
                        index = chapterIndex,
                        title = currentTitle,
                        body = currentBody.toString().trimEnd()
                    )
                )
            }
        } ?: throw IllegalStateException("Cannot open input stream for TXT file")

        val prelude = if (preludeLines.isNotEmpty()) preludeLines.joinToString("\n").trimEnd() else null

        val dedupedChapters = deduplicateTitles(chapterSlices)

        Timber.tag("TxtChapterParser").i(
            "Parsed %d chapters, prelude=%b",
            dedupedChapters.size,
            prelude != null
        )
        return TxtParseResult(prelude, dedupedChapters)
    }

    private fun deduplicateTitles(chapters: List<ChapterSlice>): List<ChapterSlice> {
        val titleCounts = mutableMapOf<String, Int>()
        return chapters.map { chapter ->
            val baseTitle = chapter.title
            val count = titleCounts.getOrDefault(baseTitle, 0)
            titleCounts[baseTitle] = count + 1
            if (count == 0) {
                chapter
            } else {
                chapter.copy(title = "$baseTitle (${count + 1})")
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/TxtChapterParser.kt
git commit -m "feat: implement streaming TXT chapter parser with regex detection"
```

---

### Task 5: Add String Resources for TXT Import

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Read existing strings.xml files to find insert locations**

Find the locations of existing import strings (`project_import_success`, etc.) in each file. The new TXT import strings should be added immediately after those.

- [ ] **Step 2: Add English strings to values/strings.xml**

After the existing `project_importing` string (around line 402), append:

```xml
    <!-- ===== TXT Import ===== -->
    <string name="menu_import_txt">Import TXT</string>
    <string name="txt_import_title">Import TXT Novel</string>
    <string name="txt_import_label_title">Title</string>
    <string name="txt_import_label_author">Author</string>
    <string name="txt_import_label_genre">Genre</string>
    <string name="txt_import_label_patterns">Chapter Detection</string>
    <string name="txt_import_pattern_cn">Ch. X / Vol. X</string>
    <string name="txt_import_pattern_en">Chapter X</string>
    <string name="txt_import_pattern_num">Numbered Headings</string>
    <string name="txt_import_pattern_vol">Volume Mark</string>
    <string name="txt_import_pattern_custom">Custom Regex</string>
    <string name="txt_import_custom_hint">Enter regex pattern</string>
    <string name="txt_import_btn_start">Start Import</string>
    <string name="txt_import_importing">Importing TXT, please wait...</string>
    <string name="txt_import_success">Imported successfully, %1$d chapters</string>
    <string name="txt_import_no_chapter">No chapters detected. Please check the detection patterns.</string>
    <string name="txt_import_file_too_large">File is too large (max 500MB)</string>
```

- [ ] **Step 3: Add Simplified Chinese strings to values-zh-rCN/strings.xml**

After the existing `project_importing` string, append:

```xml
    <!-- ===== TXT导入 ===== -->
    <string name="menu_import_txt">导入TXT</string>
    <string name="txt_import_title">导入TXT小说</string>
    <string name="txt_import_label_title">书名</string>
    <string name="txt_import_label_author">作者</string>
    <string name="txt_import_label_genre">类型</string>
    <string name="txt_import_label_patterns">章节识别模式</string>
    <string name="txt_import_pattern_cn">第X章/第X回</string>
    <string name="txt_import_pattern_en">Chapter X</string>
    <string name="txt_import_pattern_num">纯数字标题</string>
    <string name="txt_import_pattern_vol">分卷标记</string>
    <string name="txt_import_pattern_custom">自定义正则</string>
    <string name="txt_import_custom_hint">输入正则表达式</string>
    <string name="txt_import_btn_start">开始导入</string>
    <string name="txt_import_importing">正在导入TXT，请稍候...</string>
    <string name="txt_import_success">导入成功，共 %1$d 个章节</string>
    <string name="txt_import_no_chapter">未检测到章节，请检查章节识别模式</string>
    <string name="txt_import_file_too_large">文件过大（超过500MB）</string>
```

- [ ] **Step 4: Add Traditional Chinese strings to values-zh-rTW/strings.xml**

After the existing `project_importing` string, append:

```xml
    <!-- ===== TXT匯入 ===== -->
    <string name="menu_import_txt">匯入TXT</string>
    <string name="txt_import_title">匯入TXT小說</string>
    <string name="txt_import_label_title">書名</string>
    <string name="txt_import_label_author">作者</string>
    <string name="txt_import_label_genre">類型</string>
    <string name="txt_import_label_patterns">章節識別模式</string>
    <string name="txt_import_pattern_cn">第X章/第X回</string>
    <string name="txt_import_pattern_en">Chapter X</string>
    <string name="txt_import_pattern_num">純數字標題</string>
    <string name="txt_import_pattern_vol">分卷標記</string>
    <string name="txt_import_pattern_custom">自訂義正則</string>
    <string name="txt_import_custom_hint">輸入正則表達式</string>
    <string name="txt_import_btn_start">開始匯入</string>
    <string name="txt_import_importing">正在匯入TXT，請稍候...</string>
    <string name="txt_import_success">匯入成功，共 %1$d 個章節</string>
    <string name="txt_import_no_chapter">未檢測到章節，請檢查章節識別模式</string>
    <string name="txt_import_file_too_large">文件過大（超過500MB）</string>
```

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "feat: add TXT import string resources (en/zh-rCN/zh-rTW)"
```

---

### Task 6: Add TXT Import States to ProjectListUiState + Import Method in ViewModel

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/ProjectListViewModel.kt`

- [ ] **Step 1: Add TXT import states to ProjectListUiState sealed class**

In `ProjectListViewModel.kt`, add to the `ProjectListUiState` sealed class (after `ImportError`):

```kotlin
    object TxtImporting : ProjectListUiState()
    data class TxtImportSuccess(val chapterCount: Int) : ProjectListUiState()
    data class TxtImportError(val message: UiText) : ProjectListUiState()
```

- [ ] **Step 2: Add `importFromTxt` method to ProjectListViewModel**

Add after the existing `importProject` method (line 110):

```kotlin
    fun importFromTxt(
        context: Context,
        txtUri: Uri,
        title: String,
        author: String,
        genre: String,
        selectedPatterns: Set<com.universe_st.quickwriter.util.ChapterPattern>,
        customRegex: String?
    ) {
        viewModelScope.launch {
            _uiState.value = ProjectListUiState.TxtImporting
            try {
                val result = projectManagementUseCase.importProjectFromTxt(
                    context = context,
                    txtUri = txtUri,
                    title = title,
                    author = author,
                    genre = genre,
                    selectedPatterns = selectedPatterns,
                    customRegex = customRegex
                )
                if (result.isSuccess) {
                    val chapterCount = result.getOrThrow()
                    _uiState.value = ProjectListUiState.TxtImportSuccess(chapterCount)
                    loadProjects()
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ProjectListUiState.TxtImportError(
                        UiText.DynamicString(error?.message ?: "Import failed")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectListUiState.TxtImportError(
                    UiText.DynamicString(e.message ?: "Import failed")
                )
            }
        }
    }
```

- [ ] **Step 3: Update `resetImportState` to handle TXT states**

Modify the existing `resetImportState` method (line 112-117) to also clear TXT states:

```kotlin
    fun resetImportState() {
        val current = _uiState.value
        if (current is ProjectListUiState.ImportSuccess ||
            current is ProjectListUiState.ImportError ||
            current is ProjectListUiState.TxtImportSuccess ||
            current is ProjectListUiState.TxtImportError) {
            _uiState.value = ProjectListUiState.Empty
        }
    }
```

- [ ] **Step 4: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/ProjectListViewModel.kt
git commit -m "feat: add TXT import states and importFromTxt method to ProjectListViewModel"
```

---

### Task 7: Add importProjectFromTxt to ProjectManagementUseCase

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt`

- [ ] **Step 1: Add importProjectFromTxt method**

Add after the existing `importProjectFromZip` method (line 367). The method mirrors `importProjectFromZip` but uses `TxtChapterParser` and `TxtEncodingDetector` instead of ZIP extraction:

```kotlin
    suspend fun importProjectFromTxt(
        context: Context,
        txtUri: Uri,
        title: String,
        author: String,
        genre: String,
        selectedPatterns: Set<com.universe_st.quickwriter.util.ChapterPattern>,
        customRegex: String?
    ): Result<Int> {
        var storagePath: String? = null
        try {
            val fileSize = context.contentResolver.openFileDescriptor(txtUri, "r")?.statSize ?: 0
            if (fileSize > 500L * 1024 * 1024) {
                return Result.failure(IllegalArgumentException("File is too large (max 500MB)"))
            }

            val tmpFile = java.io.File(context.cacheDir, "txt_import_${AppUtils.generateProjectId()}")
            try {
                context.contentResolver.openInputStream(txtUri)?.use { input ->
                    tmpFile.outputStream().use { output ->
                        input.copyTo(output, 8192)
                    }
                } ?: return Result.failure(java.io.IOException("Cannot open TXT file"))

                val charset = com.universe_st.quickwriter.util.TxtEncodingDetector.detectEncoding(tmpFile)
                val parseResult = com.universe_st.quickwriter.util.TxtChapterParser.parseChapters(
                    context = context,
                    txtUri = txtUri,
                    charset = charset,
                    selectedPatterns = selectedPatterns,
                    customRegex = customRegex
                )

                if (parseResult.chapters.isEmpty()) {
                    return Result.failure(IllegalStateException("No chapters detected"))
                }

                var finalTitle = title.trim().ifBlank { "Imported Novel" }
                if (!projectRepository.isProjectTitleUnique(finalTitle)) {
                    var suffix = 1
                    while (!projectRepository.isProjectTitleUnique("$finalTitle ($suffix)")) {
                        suffix++
                    }
                    finalTitle = "$finalTitle ($suffix)"
                }

                val projectId = AppUtils.generateProjectId()
                storagePath = fileManager.getProjectDirectory(projectId).absolutePath
                val projectDir = java.io.File(storagePath)

                fileManager.createProjectDirectoryStructure(projectId)

                var chapterCount = 0
                var totalWords = 0

                val safeGenre = if (FileManager.NOVEL_GENRES.contains(genre)) genre else "其他"

                if (parseResult.preludeBody != null && parseResult.preludeBody.isNotBlank()) {
                    val preludeMeta = com.universe_st.quickwriter.util.ChapterMeta(
                        title = "序章",
                        order = 0
                    )
                    val preludeContent = com.universe_st.quickwriter.util.ChapterFileHelper.buildChapterContent(
                        preludeMeta,
                        parseResult.preludeBody
                    )
                    val preludeFile = java.io.File(
                        java.io.File(projectDir, "正文"),
                        "${AppUtils.sanitizeFileName("序章")}.md"
                    )
                    preludeFile.writeText(preludeContent, java.nio.charset.Charsets.UTF_8)
                    totalWords += FileManager.countWords(parseResult.preludeBody)
                    chapterCount++
                }

                for (slice in parseResult.chapters) {
                    val meta = com.universe_st.quickwriter.util.ChapterMeta(
                        title = slice.title,
                        order = chapterCount
                    )
                    val content = com.universe_st.quickwriter.util.ChapterFileHelper.buildChapterContent(
                        meta,
                        slice.body
                    )
                    var fileName = AppUtils.sanitizeFileName(slice.title).ifBlank { "Chapter_${slice.index}" }
                    if (!fileName.endsWith(".md")) {
                        fileName = "$fileName.md"
                    }
                    var chapterFile = java.io.File(java.io.File(projectDir, "正文"), fileName)
                    var dedupSuffix = 1
                    while (chapterFile.exists()) {
                        dedupSuffix++
                        val baseName = fileName.removeSuffix(".md")
                        chapterFile = java.io.File(java.io.File(projectDir, "正文"), "${baseName}_$dedupSuffix.md")
                    }
                    chapterFile.writeText(content, java.nio.charset.Charsets.UTF_8)
                    totalWords += FileManager.countWords(slice.body)
                    chapterCount++
                }

                fileManager.createInfoJson(
                    projectDir,
                    finalTitle,
                    author.trim(),
                    safeGenre,
                    "",
                    AppUtils.getCurrentTimestamp()
                )

                val project = com.universe_st.quickwriter.data.local.entity.ProjectEntity(
                    id = projectId,
                    title = finalTitle,
                    author = author.trim(),
                    genre = safeGenre,
                    description = null,
                    coverImagePath = null,
                    storagePath = storagePath,
                    createdTime = AppUtils.getCurrentTimestamp(),
                    modifiedTime = AppUtils.getCurrentTimestamp(),
                    status = "active",
                    wordCount = totalWords,
                    chapterCount = chapterCount
                )

                projectRepository.insertProjectDirect(project)

                Timber.tag("TxtImport").i(
                    "Import successful: id=%s title=%s chapters=%d words=%d",
                    projectId, finalTitle, chapterCount, totalWords
                )
                return Result.success(chapterCount)
            } finally {
                tmpFile.delete()
            }
        } catch (e: Exception) {
            Timber.tag("TxtImport").e(e, "TXT import failed")
            storagePath?.let { try { java.io.File(it).deleteRecursively() } catch (_: Exception) {} }
            return Result.failure(e)
        }
    }
```



- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt
git commit -m "feat: add importProjectFromTxt method to ProjectManagementUseCase"
```

---

### Task 8: Create TxtImportDialog Composable

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/TxtImportDialog.kt`

- [ ] **Step 1: Create TxtImportDialog.kt**

```kotlin
package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.util.ChapterPattern
import com.universe_st.quickwriter.util.FileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxtImportDialog(
    defaultTitle: String,
    onConfirm: (title: String, author: String, genre: String, patterns: Set<ChapterPattern>, customRegex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle) }
    var author by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("其他") }
    var selectedPatterns by remember { mutableStateOf(setOf(ChapterPattern.CN_CHAPTER)) }
    var customRegex by remember { mutableStateOf("") }
    var genreExpanded by remember { mutableStateOf(false) }

    val isValid = title.isNotBlank() && author.isNotBlank() && selectedPatterns.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.txt_import_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.txt_import_label_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.txt_import_label_author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = genreExpanded,
                    onExpandedChange = { genreExpanded = it }
                ) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.txt_import_label_genre)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = genreExpanded,
                        onDismissRequest = { genreExpanded = false }
                    ) {
                        FileManager.NOVEL_GENRES.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    genre = g
                                    genreExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.txt_import_label_patterns),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ChapterPattern.values().forEach { pattern ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPatterns = if (pattern in selectedPatterns) {
                                    selectedPatterns - pattern
                                } else {
                                    selectedPatterns + pattern
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = pattern in selectedPatterns,
                            onCheckedChange = {
                                selectedPatterns = if (it) {
                                    selectedPatterns + pattern
                                } else {
                                    selectedPatterns - pattern
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(pattern.displayNameKey),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (ChapterPattern.CUSTOM in selectedPatterns) {
                    OutlinedTextField(
                        value = customRegex,
                        onValueChange = { customRegex = it },
                        label = { Text("Regex") },
                        placeholder = { Text(stringResource(R.string.txt_import_custom_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, author, genre, selectedPatterns, customRegex) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.txt_import_btn_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/TxtImportDialog.kt
git commit -m "feat: add TxtImportDialog composable for TXT import metadata input"
```

---

### Task 9: Wire TXT Import UI in ProjectListScreen + MainScreen

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectListScreen.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/MainScreen.kt`

- [ ] **Step 1: Add onImportTxt parameter to ProjectListScreen**

In `ProjectListScreen.kt`, modify the function signature (line 35-41) to add `onImportTxt`:

```kotlin
fun ProjectListScreen(
    onCreateProject: () -> Unit,
    onProjectLongClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onImportProject: () -> Unit,
    onImportTxt: () -> Unit,
    viewModel: ProjectListViewModel
) {
```

- [ ] **Step 2: Add TXT import menu item to ProjectListScreen**

In `ProjectListScreen.kt`, inside the `DropdownMenu` block, add a new `DropdownMenuItem` after the existing import item (line 94-100):

```kotlin
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_import_txt)) },
                                onClick = {
                                    showMenu = false
                                    onImportTxt()
                                }
                            )
```

- [ ] **Step 3: Add TxtImportSuccess/TxtImportError handling in LaunchedEffect**

In `ProjectListScreen.kt`, in the `LaunchedEffect(uiState)` block, add handling alongside the existing `ImportSuccess`/`ImportError`:

```kotlin
            is ProjectListUiState.TxtImportSuccess -> {
                val count = state.chapterCount
                snackbarHostState.showSnackbar(
                    context.getString(R.string.txt_import_success, count)
                )
                viewModel.resetImportState()
            }
            is ProjectListUiState.TxtImportError -> {
                val msg = state.message.asString(context)
                snackbarHostState.showSnackbar(msg)
                viewModel.resetImportState()
            }
```

- [ ] **Step 4: Also handle TxtImporting in loading UI and TxtImportSuccess/TxtImportError in the when block**

In `ProjectListScreen.kt`, update the loading condition (line 147) to include `TxtImporting`:

```kotlin
                is ProjectListUiState.Loading, is ProjectListUiState.Importing, is ProjectListUiState.TxtImporting -> {
```

And update the when branch that previously only handled `ImportSuccess`/`ImportError` (line 217-219) to also handle TXT states:

```kotlin
                is ProjectListUiState.ImportSuccess,
                is ProjectListUiState.ImportError,
                is ProjectListUiState.TxtImportSuccess,
                is ProjectListUiState.TxtImportError -> {
                    // Handled via LaunchedEffect + Snackbar
                }
```

- [ ] **Step 5: Add txtFilePickerLauncher and TxtImportDialog state in MainScreen.kt**

In `MainScreen.kt`, after the existing `zipFilePickerLauncher` definition (line 93-99), add:

```kotlin
    var showTxtImportDialog by remember { mutableStateOf<Uri?>(null) }
    val txtFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { showTxtImportDialog = it }
    }
```

- [ ] **Step 6: Add TxtImportDialog display in MainScreen.kt**

In `MainScreen.kt`, add the TxtImportDialog at the bottom of the `MainScreen` composable (before the closing brace of the outermost function), outside `Scaffold`:

```kotlin
    showTxtImportDialog?.let { uri ->
        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".txt") ?: ""
        TxtImportDialog(
            defaultTitle = fileName,
            onConfirm = { title, author, genre, patterns, customRegex ->
                showTxtImportDialog = null
                projectListViewModel.importFromTxt(context, uri, title, author, genre, patterns, customRegex.ifBlank { null })
            },
            onDismiss = {
                showTxtImportDialog = null
            }
        )
    }
```

Add the import for `TxtImportDialog` at the top:

```kotlin
import com.universe_st.quickwriter.presentation.ui.components.TxtImportDialog
```

- [ ] **Step 7: Wire onImportTxt in the ProjectListScreen composable call**

In `MainScreen.kt`, in the `ProjectListScreen` composable call (around line 168-182), add the `onImportTxt` parameter:

```kotlin
                ProjectListScreen(
                    onProjectLongClick = { projectId ->
                        showActionDialog = projectId
                    },
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    },
                    onCreateProject = {
                        navController.navigate(Screen.ProjectCreate.route)
                    },
                    onImportProject = {
                        zipFilePickerLauncher.launch(arrayOf("application/zip"))
                    },
                    onImportTxt = {
                        txtFilePickerLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                    },
                    viewModel = projectListViewModel
                )
```

- [ ] **Step 8: Build to verify compilation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectListScreen.kt app/src/main/java/com/universe_st/quickwriter/presentation/MainScreen.kt
git commit -m "feat: wire TXT import UI triggers, file picker, dialog, and success/error handling"
```

---

### Task 10: Final Verification Build + Test

**Files:** None (verification only)

- [ ] **Step 1: Full clean build**

Run: `./gradlew clean :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all new/modified files are tracked**

Run: `git status`
Expected: Only expected files listed, no untracked surprises.

- [ ] **Step 3: Run unit tests**

Run: `./gradlew test`
Expected: All tests pass.

