package com.universe_st.quickwriter.domain.usecase

import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.util.FileManager
import com.universe_st.quickwriter.util.AppUtils
import kotlinx.coroutines.flow.Flow

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

    fun getSortedProjects(sortOption: SortOption): Flow<List<ProjectEntity>> {
        return getAllProjects(sortOption)
    }
}