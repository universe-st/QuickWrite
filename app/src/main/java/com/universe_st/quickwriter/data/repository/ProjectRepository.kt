package com.universe_st.quickwriter.data.repository

import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.util.AppUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(
    private val projectDao: ProjectDao
) {

    fun getAllProjects(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjects()
    }

    fun getAllProjectsByCreatedTime(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjectsByCreatedTime()
    }

    fun getAllProjectsByTitle(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjectsByTitle()
    }

    suspend fun getProjectById(id: String): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun getProjectByTitle(title: String): ProjectEntity? {
        return projectDao.getProjectByTitle(title)
    }

    suspend fun isProjectTitleUnique(title: String, excludeId: String? = null): Boolean {
        val existingProject = getProjectByTitle(title)
        return existingProject == null || existingProject.id == excludeId
    }

    suspend fun createProject(
        title: String,
        author: String,
        genre: String,
        description: String?,
        coverImagePath: String?,
        storagePath: String
    ): Result<ProjectEntity> {
        return try {
            val isUnique = isProjectTitleUnique(title)
            if (!isUnique) {
                return Result.failure(IllegalArgumentException("项目标题已存在"))
            }

            val project = ProjectEntity(
                id = AppUtils.generateProjectId(),
                title = title,
                author = author,
                genre = genre,
                description = description,
                coverImagePath = coverImagePath,
                storagePath = storagePath,
                createdTime = AppUtils.getCurrentTimestamp(),
                modifiedTime = AppUtils.getCurrentTimestamp(),
                status = "active",
                wordCount = 0,
                chapterCount = 0
            )

            projectDao.insertProject(project)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(
        id: String,
        title: String,
        author: String,
        genre: String,
        description: String?,
        coverImagePath: String?,
        currentProject: ProjectEntity
    ): Result<ProjectEntity> {
        return try {
            val isUnique = isProjectTitleUnique(title, excludeId = id)
            if (!isUnique) {
                return Result.failure(IllegalArgumentException("项目标题已存在"))
            }

            val updatedProject = currentProject.copy(
                title = title,
                author = author,
                genre = genre,
                description = description,
                coverImagePath = coverImagePath,
                modifiedTime = AppUtils.getCurrentTimestamp()
            )

            projectDao.updateProject(updatedProject)
            Result.success(updatedProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(project: ProjectEntity): Result<Unit> {
        return try {
            projectDao.deleteProject(project)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectById(id: String): Result<Unit> {
        return try {
            projectDao.deleteProjectById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertProjectDirect(project: ProjectEntity): Result<Unit> {
        return try {
            projectDao.insertProject(project)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWordCount(projectId: String, wordCount: Int): Result<Unit> {
        return try {
            projectDao.updateWordCount(projectId, wordCount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementChapterCount(projectId: String): Result<Unit> {
        return try {
            projectDao.incrementChapterCount(projectId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateModifiedTime(projectId: String, project: ProjectEntity): Result<Unit> {
        return try {
            val updatedProject = project.copy(
                modifiedTime = AppUtils.getCurrentTimestamp()
            )
            projectDao.updateProject(updatedProject)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}