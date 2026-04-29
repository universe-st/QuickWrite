package com.universe_st.quickwriter.util

import android.content.Context
import com.universe_st.quickwriter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileManager(private val context: Context) {

    companion object {
        private const val PROJECTS_DIR = "projects"
        
        val NOVEL_GENRES = listOf(
            "玄幻", "奇幻", "历史", "都市", "科幻",
            "武侠", "仙侠", "军事", "悬疑", "恐怖",
            "游戏", "竞技", "同人", "轻小说", "其他"
        )
    }

    fun getProjectsRootDirectory(): File {
        val rootDir = File(context.filesDir, PROJECTS_DIR)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        return rootDir
    }

    fun getProjectDirectory(projectId: String): File {
        val projectDir = File(getProjectsRootDirectory(), projectId)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        return projectDir
    }

    suspend fun createProjectDirectoryStructure(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDirectory(projectId)
            
            val directories = listOf(
                File(projectDir, "正文"),
                File(projectDir, "设定${File.separator}人物"),
                File(projectDir, "设定${File.separator}地点"),
                File(projectDir, "设定${File.separator}组织"),
                File(projectDir, "设定${File.separator}物品"),
                File(projectDir, "时间线"),
                File(projectDir, "记录"),
                File(projectDir, "配置")
            )

            directories.forEach { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }

            createIntroFile(projectDir)
            createAiInstructionFile(projectDir)
            createWritingRulesFile(projectDir)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createIntroFile(projectDir: File) {
        val introFile = File(projectDir, "简介.md")
        if (!introFile.exists()) {
            introFile.createNewFile()
        }
    }

    private fun createAiInstructionFile(projectDir: File) {
        val configDir = File(projectDir, "配置")
        val aiInstructionFile = File(configDir, "AI指令.md")
        if (!aiInstructionFile.exists()) {
            aiInstructionFile.createNewFile()
            aiInstructionFile.writeText("# AI写作指令\n\n", Charsets.UTF_8)
        }
    }

    fun createInfoJson(projectDir: File, title: String, author: String, genre: String, createdTime: Long) {
        val infoFile = File(projectDir, "info.json")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val json = JSONObject().apply {
            put("title", title)
            put("author", author)
            put("genre", genre)
            put("createdTime", dateFormat.format(Date(createdTime)))
            put("version", "1.0")
        }
        infoFile.writeText(json.toString(2), Charsets.UTF_8)
    }

    private fun createWritingRulesFile(projectDir: File) {
        val configDir = File(projectDir, "配置")
        val writingRulesFile = File(configDir, "写作规范.md")
        if (!writingRulesFile.exists()) {
            writingRulesFile.createNewFile()
            writingRulesFile.writeText("# 写作规范\n\n", Charsets.UTF_8)
        }
    }

    suspend fun readFileContent(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Result.failure(IOException(context.getString(R.string.file_error_not_found, filePath)))
            } else {
            val content = file.readText(Charsets.UTF_8)
            Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFileContent(filePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(dirPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFileOrDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.deleteRecursively()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFileOrDirectory(oldPath: String, newPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            val newFile = File(newPath)
            
            if (oldFile.exists()) {
                newFile.parentFile?.mkdirs()
                if (oldFile.renameTo(newFile)) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException(context.getString(R.string.file_error_rename_failed)))
                }
            } else {
                Result.failure(IOException(context.getString(R.string.file_error_source_not_found)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    suspend fun directoryExists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists() && File(path).isDirectory
    }

    suspend fun getDirectoryContents(path: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                Result.failure(IOException(context.getString(R.string.file_error_dir_not_found)))
            } else {
                val contents = dir.listFiles()?.map { it.absolutePath } ?: emptyList()
                Result.success(contents)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDirectory(projectId)
            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isPathSafe(path: String): Boolean {
        val projectsRoot = getProjectsRootDirectory().canonicalPath
        val targetPath = File(path).canonicalPath
        return targetPath.startsWith(projectsRoot)
    }

    suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists() && file.isFile) {
            file.length()
        } else {
            0L
        }
    }

    fun getCoverImagePath(projectId: String): String {
        return CoverImageProcessor.getCoverFilePath(getProjectDirectory(projectId).absolutePath)
    }

    fun hasCoverImage(projectId: String): Boolean {
        return CoverImageProcessor.hasCoverImage(getProjectDirectory(projectId).absolutePath)
    }

    suspend fun zipProjectToFile(projectDirPath: String, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        val tag = "ZipExport"
        try {
            val projectDir = File(projectDirPath)
            Timber.tag(tag).i("dir=%s, exists=%s, isDir=%s",
                projectDir.absolutePath, projectDir.exists(), projectDir.isDirectory)

            if (!projectDir.exists() || !projectDir.isDirectory) {
                Timber.tag(tag).w("Project directory not found: %s", projectDir.absolutePath)
                return@withContext Result.failure(IOException("Project directory not found: ${projectDir.absolutePath}"))
            }

            val basePath = projectDir.absolutePath.trimEnd(File.separatorChar) + File.separator
            Timber.tag(tag).i("basePath=%s", basePath)

            var allFiles = projectDir.walkTopDown().toList()
            Timber.tag(tag).i("walkTopDown found %d total entries", allFiles.size)

            var fileCount = 0
            var dirCount = 0
            allFiles.forEach { f ->
                if (f.isDirectory) {
                    dirCount++
                    Timber.tag(tag).i("  DIR  [%d] %s", dirCount, f.absolutePath)
                } else {
                    fileCount++
                    Timber.tag(tag).i("  FILE [%d] %s (%d bytes)", fileCount, f.absolutePath, f.length())
                }
            }
            Timber.tag(tag).i("Summary: %d dirs, %d files", dirCount, fileCount)

            if (fileCount == 0 && dirCount <= 1) {
                Timber.tag(tag).w("Project directory is empty, creating structure")
                createDirectoryStructureAt(projectDir)
                allFiles = projectDir.walkTopDown().toList()
                Timber.tag(tag).i("After structure creation: %d total entries", allFiles.size)
                allFiles.forEach { f ->
                    if (f.isDirectory) {
                        Timber.tag(tag).i("  DIR  %s", f.absolutePath)
                    } else {
                        Timber.tag(tag).i("  FILE %s (%d bytes)", f.absolutePath, f.length())
                    }
                }
            }

            var entryCount = 0
            FileOutputStream(outputFile).use { fileOut ->
                ZipOutputStream(fileOut).use { zipOut ->
                    allFiles.forEach { file ->
                        val absPath = file.absolutePath
                        if (!absPath.startsWith(basePath)) return@forEach
                        val relativePath = absPath.substring(basePath.length).replace(File.separator, "/")
                        if (relativePath.isEmpty() && file.isDirectory) return@forEach

                        if (file.isDirectory) {
                            zipOut.putNextEntry(ZipEntry("$relativePath/"))
                            zipOut.closeEntry()
                            entryCount++
                        } else {
                            zipOut.putNextEntry(ZipEntry(relativePath))
                            BufferedInputStream(FileInputStream(file)).use { input ->
                                input.copyTo(zipOut, 8192)
                            }
                            zipOut.closeEntry()
                            entryCount++
                        }
                    }
                }
            }

            val outputSize = outputFile.length()
            Timber.tag(tag).i("ZIP complete: %d entries, %d bytes", entryCount, outputSize)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "ZIP creation failed for path=%s", projectDirPath)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            Result.failure(e)
        }
    }

    private fun createDirectoryStructureAt(projectDir: File) {
        val dirs = listOf(
            File(projectDir, "正文"),
            File(projectDir, "设定${File.separator}人物"),
            File(projectDir, "设定${File.separator}地点"),
            File(projectDir, "设定${File.separator}组织"),
            File(projectDir, "设定${File.separator}物品"),
            File(projectDir, "时间线"),
            File(projectDir, "记录"),
            File(projectDir, "配置")
        )
        dirs.forEach { it.mkdirs() }

        File(projectDir, "简介.md").createNewFile()

        val aiFile = File(projectDir, "配置${File.separator}AI指令.md")
        aiFile.parentFile?.mkdirs()
        if (aiFile.createNewFile()) {
            aiFile.writeText("# AI写作指令\n\n", Charsets.UTF_8)
        }

        val rulesFile = File(projectDir, "配置${File.separator}写作规范.md")
        rulesFile.parentFile?.mkdirs()
        if (rulesFile.createNewFile()) {
            rulesFile.writeText("# 写作规范\n\n", Charsets.UTF_8)
        }
    }

    data class InfoJsonData(
        val title: String,
        val author: String,
        val genre: String,
        val createdTime: Long
    )

    fun readInfoJson(projectDir: File): InfoJsonData? {
        val infoFile = File(projectDir, "info.json")
        if (!infoFile.exists()) return null
        return try {
            val json = JSONObject(infoFile.readText(Charsets.UTF_8))
            val title = json.getString("title")
            val author = json.getString("author")
            val genre = json.getString("genre")
            val createdTimeStr = json.getString("createdTime")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val createdTime = dateFormat.parse(createdTimeStr)?.time ?: System.currentTimeMillis()
            InfoJsonData(title, author, genre, createdTime)
        } catch (e: Exception) {
            Timber.tag("ImportProject").w(e, "Failed to parse info.json")
            null
        }
    }

    suspend fun extractZipTo(zipFile: File, outputDir: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryFile = File(outputDir, entry.name)
                    if (entry.isDirectory || entry.name.endsWith("/")) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(entryFile)).use { output ->
                            val buffer = ByteArray(8192)
                            var count: Int
                            while (zis.read(buffer).also { count = it } != -1) {
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("ImportProject").e(e, "Failed to extract ZIP")
            outputDir.deleteRecursively()
            Result.failure(e)
        }
    }

}