package com.universe_st.quickwriter.domain.usecase

import android.content.Context
import android.net.Uri
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.util.CoverImageProcessor
import com.universe_st.quickwriter.util.FileManager
import com.universe_st.quickwriter.util.AppUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProjectManagementUseCase(
    private val projectRepository: ProjectRepository,
    private val fileManager: FileManager
) {

    enum class SortOption {
        MODIFIED_TIME, CREATED_TIME, TITLE
    }

    fun getAllProjects(sortOption: SortOption = SortOption.MODIFIED_TIME): Flow<List<ProjectEntity>> {
        return when (sortOption) {
            SortOption.MODIFIED_TIME -> projectRepository.getAllProjects()
            SortOption.CREATED_TIME -> projectRepository.getAllProjectsByCreatedTime()
            SortOption.TITLE -> projectRepository.getAllProjectsByTitle()
        }
    }

    suspend fun getProjectById(id: String): ProjectEntity? {
        return projectRepository.getProjectById(id)
    }

    suspend fun createProject(
        title: String,
        author: String,
        genre: String,
        description: String? = null,
        coverImagePath: String? = null
    ): Result<ProjectEntity> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("项目标题不能为空"))
        }
        if (author.isBlank()) {
            return Result.failure(IllegalArgumentException("作者名称不能为空"))
        }

        if (!FileManager.NOVEL_GENRES.contains(genre)) {
            return Result.failure(IllegalArgumentException("无效的小说类型"))
        }

        val projectId = AppUtils.generateProjectId()
        val storagePath = fileManager.getProjectDirectory(projectId).absolutePath

        val result = projectRepository.createProject(
            title = title.trim(),
            author = author.trim(),
            genre = genre,
            description = description?.trim(),
            coverImagePath = coverImagePath,
            storagePath = storagePath
        )

        if (result.isSuccess) {
            val createDirResult = fileManager.createProjectDirectoryStructure(projectId)
            if (createDirResult.isFailure) {
                deleteProject(projectId)
                return Result.failure(createDirResult.exceptionOrNull() ?: Exception("创建项目目录失败"))
            }
            val project = result.getOrThrow()
            val projectDir = File(project.storagePath)
            fileManager.createInfoJson(projectDir, project.title, project.author, project.genre, project.description ?: "", project.createdTime)
        }

        return result
    }

    suspend fun updateProject(
        id: String,
        title: String,
        author: String,
        genre: String,
        description: String?,
        coverImagePath: String?
    ): Result<ProjectEntity> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("项目标题不能为空"))
        }
        if (author.isBlank()) {
            return Result.failure(IllegalArgumentException("作者名称不能为空"))
        }

        if (!FileManager.NOVEL_GENRES.contains(genre)) {
            return Result.failure(IllegalArgumentException("无效的小说类型"))
        }

        val currentProject = projectRepository.getProjectById(id)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))

        return projectRepository.updateProject(
            id = id,
            title = title.trim(),
            author = author.trim(),
            genre = genre,
            description = description?.trim(),
            coverImagePath = coverImagePath,
            currentProject = currentProject
        )
    }

    suspend fun deleteProject(id: String): Result<Unit> {
        val project = projectRepository.getProjectById(id)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))

        val fileDeleteResult = fileManager.deleteProject(id)
        if (fileDeleteResult.isFailure) {
            return Result.failure(fileDeleteResult.exceptionOrNull() ?: Exception("删除项目文件失败"))
        }

        return projectRepository.deleteProject(project)
    }

    suspend fun validateProjectTitle(title: String, excludeId: String? = null): Result<Unit> {
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("项目标题不能为空"))
        }
        if (!projectRepository.isProjectTitleUnique(title, excludeId)) {
            return Result.failure(IllegalArgumentException("项目标题已存在"))
        }
        return Result.success(Unit)
    }

    suspend fun validateProjectAuthor(author: String): Result<Unit> {
        if (author.isBlank()) {
            return Result.failure(IllegalArgumentException("作者名称不能为空"))
        }
        return Result.success(Unit)
    }

    suspend fun updateProjectStatistics(projectId: String): Result<Unit> {
        val project = projectRepository.getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))

        val modifiedTimeResult = projectRepository.updateModifiedTime(projectId, project)
        return modifiedTimeResult
    }

    fun getSortedProjects(sortOption: SortOption, currentProjectId: String? = null): Flow<List<ProjectEntity>> {
        val baseFlow = getAllProjects(sortOption)
        if (currentProjectId == null) return baseFlow
        return baseFlow.map { projects ->
            val currentProject = projects.find { it.id == currentProjectId }
            if (currentProject != null) {
                listOf(currentProject) + projects.filter { it.id != currentProjectId }
            } else {
                projects
            }
        }
    }

    fun hasCoverImage(projectId: String): Boolean {
        return fileManager.hasCoverImage(projectId)
    }

    suspend fun getChapterFiles(projectId: String): Result<List<String>> {
        val project = getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))
        val chapterDir = File(project.storagePath, "正文")
        return fileManager.getDirectoryContents(chapterDir.absolutePath).map { files ->
            files.filter { it.endsWith(".md") }.sorted()
        }
    }

    suspend fun readFileContent(filePath: String): Result<String> {
        return fileManager.readFileContent(filePath)
    }

    suspend fun writeFileContent(filePath: String, content: String): Result<Unit> {
        return fileManager.writeFileContent(filePath, content)
    }

    suspend fun createChapterFile(projectId: String, fileName: String): Result<String> {
        val project = getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))
        val filePath = File(File(project.storagePath, "正文"), fileName).absolutePath
        return fileManager.createFile(filePath).map { filePath }
    }

    suspend fun deleteChapterFile(projectId: String, fileName: String): Result<Unit> {
        val project = getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))
        val filePath = File(File(project.storagePath, "正文"), fileName).absolutePath
        return fileManager.deleteFileOrDirectory(filePath)
    }

    fun getProjectDirectory(projectId: String): String {
        return fileManager.getProjectDirectory(projectId).absolutePath
    }

    fun getCoverImagePath(projectId: String): String {
        return fileManager.getCoverImagePath(projectId)
    }

    suspend fun saveCoverImage(context: Context, sourceUri: Uri, projectId: String): Result<String> {
        val project = projectRepository.getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))

        val projectDir = fileManager.getProjectDirectory(projectId).absolutePath
        val saveResult = CoverImageProcessor.saveCoverImage(context, sourceUri, projectDir)
        if (saveResult.isSuccess) {
            val coverPath = saveResult.getOrThrow()
            projectRepository.updateProject(
                id = projectId,
                title = project.title,
                author = project.author,
                genre = project.genre,
                description = project.description,
                coverImagePath = coverPath,
                currentProject = project
            )
        }
        return saveResult
    }

    suspend fun importProjectFromZip(context: Context, zipUri: Uri): Result<ProjectEntity> {
        val tempDir = File(context.cacheDir, "import_${AppUtils.generateProjectId()}")
        val tempZipFile = File(tempDir, "project.zip")
        var storagePath: String? = null
        try {
            if (!tempDir.exists()) tempDir.mkdirs()

            context.contentResolver.openInputStream(zipUri)?.use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    input.copyTo(output, 8192)
                }
            } ?: return Result.failure(IOException("Cannot open ZIP file"))

            val extractDir = File(tempDir, "extracted")
            val extractResult = fileManager.extractZipTo(tempZipFile, extractDir)
            if (extractResult.isFailure) {
                return Result.failure(extractResult.exceptionOrNull() ?: Exception("Failed to extract ZIP"))
            }

            val infoData = fileManager.readInfoJson(extractDir)
                ?: return Result.failure(IllegalArgumentException("info.json not found in the ZIP, not a valid project export"))

            var title = infoData.title.ifBlank { "Imported Project" }
            if (!projectRepository.isProjectTitleUnique(title)) {
                var suffix = 1
                while (!projectRepository.isProjectTitleUnique("$title ($suffix)")) {
                    suffix++
                }
                title = "$title ($suffix)"
            }

            val projectId = AppUtils.generateProjectId()
            storagePath = fileManager.getProjectDirectory(projectId).absolutePath
            val targetDir = File(storagePath)
            extractDir.copyRecursively(targetDir, overwrite = true)

            val coverImagePath = if (CoverImageProcessor.hasCoverImage(storagePath)) {
                CoverImageProcessor.getCoverFilePath(storagePath)
            } else null

            val project = ProjectEntity(
                id = projectId,
                title = title,
                author = infoData.author.ifBlank { "" },
                genre = if (FileManager.NOVEL_GENRES.contains(infoData.genre)) infoData.genre else "其他",
                description = null,
                coverImagePath = coverImagePath,
                storagePath = storagePath,
                createdTime = infoData.createdTime,
                modifiedTime = AppUtils.getCurrentTimestamp(),
                status = "active",
                wordCount = 0,
                chapterCount = countChapterFiles(storagePath)
            )

            projectRepository.insertProjectDirect(project)

            val infoFile = File(targetDir, "info.json")
            if (!infoFile.exists()) {
                fileManager.createInfoJson(targetDir, project.title, project.author, project.genre, project.description ?: "", project.createdTime)
            }

            Timber.tag("ImportProject").i("Import successful: id=%s title=%s", projectId, title)
            return Result.success(project)
        } catch (e: Exception) {
            Timber.tag("ImportProject").e(e, "Import failed")
            storagePath?.let { try { File(it).deleteRecursively() } catch (_: Exception) {} }
            return Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun countChapterFiles(storagePath: String): Int {
        val chaptersDir = File(storagePath, "正文")
        return if (chaptersDir.exists() && chaptersDir.isDirectory) {
            chaptersDir.listFiles()?.count { it.isFile && it.name.endsWith(".md") } ?: 0
        } else 0
    }

    suspend fun exportProjectAsZip(projectId: String, outputFile: File): Result<Unit> {
        val project = projectRepository.getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("Project not found"))
        val computedPath = fileManager.getProjectDirectory(projectId).absolutePath
        Timber.tag("ZipExport").i("DB storagePath=%s, computed getProjectDirectory=%s",
            project.storagePath, computedPath)

        val projectDir = File(project.storagePath)
        val infoFile = File(projectDir, "info.json")
        if (!infoFile.exists()) {
            fileManager.createInfoJson(projectDir, project.title, project.author, project.genre, project.description ?: "", project.createdTime)
        }

        return fileManager.zipProjectToFile(project.storagePath, outputFile)
    }

    suspend fun deleteCoverImage(projectId: String): Result<Unit> {
        val project = projectRepository.getProjectById(projectId)
            ?: return Result.failure(IllegalArgumentException("项目不存在"))
        val projectDir = fileManager.getProjectDirectory(projectId).absolutePath
        val deleteResult = CoverImageProcessor.deleteCoverImage(projectDir)
        if (deleteResult.isSuccess) {
            projectRepository.updateProject(
                id = projectId,
                title = project.title,
                author = project.author,
                genre = project.genre,
                description = project.description,
                coverImagePath = null,
                currentProject = project
            )
        }
        return deleteResult
    }
}