package com.universe_st.quickwriter.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

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
                Result.failure(IOException("文件不存在: $filePath"))
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
                    Result.failure(IOException("重命名失败"))
                }
            } else {
                Result.failure(IOException("源文件/目录不存在"))
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
                Result.failure(IOException("目录不存在"))
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
}