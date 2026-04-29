package com.universe_st.quickwriter.domain.model

enum class OperationType {
    EDIT_FILE,
    DELETE_FILE,
    CREATE_FILE,
    MOVE_FILE,
    COPY_FILE,
    CREATE_PROJECT,
    DELETE_PROJECT,
    UPDATE_PROJECT
}

sealed class AiOperation {
    abstract val id: String
    abstract val type: OperationType
    abstract val projectId: String
    abstract val sessionId: String
    abstract val toolCallId: String
    abstract val executedAt: Long

    abstract suspend fun canRollback(context: ToolContext): String?
    abstract suspend fun rollback(context: ToolContext): Result<Unit>
}

data class EditFileOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val filePath: String,
    val hashAfterEdit: String,
    val hashBeforeEdit: String,
    val lastModifiedAfterEdit: Long
) : AiOperation() {
    override val type = OperationType.EDIT_FILE

    override suspend fun canRollback(context: ToolContext): String? {
        val fullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), filePath).absolutePath)
        if (!context.fileManager.isPathSafe(fullPath.absolutePath)) return "Path unsafe"
        if (!fullPath.exists()) return "File no longer exists"
        val currentHash = HashUtil.computeSha256(fullPath)
        if (currentHash != hashAfterEdit) return "File has been modified (hash mismatch)"
        val backupFile = java.io.File(context.fileManager.getProjectDirectory(projectId).parentFile, "ai_backups/operations/${id}.bak")
        if (!backupFile.exists()) return "Backup file not found"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        val fullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), filePath).absolutePath)
        val backupFile = java.io.File(context.fileManager.getProjectDirectory(projectId).parentFile, "ai_backups/operations/${id}.bak")
        return try {
            backupFile.copyTo(fullPath, overwrite = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class DeleteFileOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val filePath: String,
    val deletedHash: String,
    val isDirectory: Boolean
) : AiOperation() {
    override val type = OperationType.DELETE_FILE

    override suspend fun canRollback(context: ToolContext): String? {
        val backupFile = java.io.File(context.fileManager.getProjectDirectory(projectId).parentFile, "ai_backups/operations/${id}.bak")
        if (!backupFile.exists()) return "Backup file not found"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        val fullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), filePath).absolutePath)
        val backupFile = java.io.File(context.fileManager.getProjectDirectory(projectId).parentFile, "ai_backups/operations/${id}.bak")
        return try {
            fullPath.parentFile?.mkdirs()
            backupFile.copyTo(fullPath, overwrite = false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CreateFileOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val filePath: String,
    val createdHash: String
) : AiOperation() {
    override val type = OperationType.CREATE_FILE

    override suspend fun canRollback(context: ToolContext): String? {
        val fullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), filePath).absolutePath)
        if (!fullPath.exists()) return "File already deleted"
        val currentHash = HashUtil.computeSha256(fullPath)
        if (currentHash != createdHash) return "File has been modified"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        val fullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), filePath).absolutePath)
        return context.fileManager.deleteFileOrDirectory(fullPath.absolutePath)
    }
}

data class MoveFileOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val sourcePath: String,
    val targetPath: String,
    val hashAfterMove: String,
    val isDirectory: Boolean
) : AiOperation() {
    override val type = OperationType.MOVE_FILE

    override suspend fun canRollback(context: ToolContext): String? {
        val targetFullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), targetPath).absolutePath)
        if (!targetFullPath.exists()) return "Target file no longer exists"
        val currentHash = HashUtil.computeSha256(targetFullPath)
        if (currentHash != hashAfterMove) return "Target file has been modified"
        val sourceFullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), sourcePath).absolutePath)
        if (sourceFullPath.exists()) return "Source path already occupied by new file"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        val targetFullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), targetPath).absolutePath)
        val sourceFullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), sourcePath).absolutePath)
        return context.fileManager.renameFileOrDirectory(targetFullPath.absolutePath, sourceFullPath.absolutePath)
    }
}

data class CopyFileOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val targetPath: String,
    val hashAfterCopy: String
) : AiOperation() {
    override val type = OperationType.COPY_FILE

    override suspend fun canRollback(context: ToolContext): String? {
        val targetFullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), targetPath).absolutePath)
        if (!targetFullPath.exists()) return "Target file no longer exists"
        val currentHash = HashUtil.computeSha256(targetFullPath)
        if (currentHash != hashAfterCopy) return "Target file has been modified"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        val targetFullPath = java.io.File(java.io.File(context.fileManager.getProjectDirectory(projectId), targetPath).absolutePath)
        return context.fileManager.deleteFileOrDirectory(targetFullPath.absolutePath)
    }
}

data class CreateProjectOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val createdProjectId: String
) : AiOperation() {
    override val type = OperationType.CREATE_PROJECT

    override suspend fun canRollback(context: ToolContext): String? {
        val project = context.projectRepository.getProjectById(createdProjectId)
        if (project == null) return "Project already deleted"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        return context.projectManagementUseCase.deleteProject(createdProjectId)
    }
}

data class DeleteProjectOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val deletedProjectId: String,
    val deletedProjectTitle: String
) : AiOperation() {
    override val type = OperationType.DELETE_PROJECT

    override suspend fun canRollback(context: ToolContext): String? {
        val backupDir = java.io.File(context.fileManager.getProjectDirectory(projectId).parentFile, "ai_backups/projects/$deletedProjectId")
        if (!backupDir.exists()) return "Project backup not found"
        val existing = context.projectRepository.getProjectById(deletedProjectId)
        if (existing != null) return "Project with same ID already exists"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        val backupDir = java.io.File(context.fileManager.getProjectDirectory(projectId).parentFile, "ai_backups/projects/$deletedProjectId")
        val projectZip = java.io.File(backupDir, "project.zip")
        val restoreDir = context.fileManager.getProjectDirectory(deletedProjectId)
        return context.fileManager.extractZipTo(projectZip, restoreDir)
    }
}

data class UpdateProjectOperation(
    override val id: String,
    override val projectId: String,
    override val sessionId: String,
    override val toolCallId: String,
    override val executedAt: Long,
    val changedFields: Map<String, String>
) : AiOperation() {
    override val type = OperationType.UPDATE_PROJECT

    override suspend fun canRollback(context: ToolContext): String? {
        val project = context.projectRepository.getProjectById(projectId)
        if (project == null) return "Project no longer exists"
        return null
    }

    override suspend fun rollback(context: ToolContext): Result<Unit> {
        return try {
            val project = context.projectRepository.getProjectById(projectId)
                ?: return Result.failure(Exception("Project not found"))
            context.projectRepository.updateProject(
                id = projectId,
                title = project.title,
                author = project.author,
                genre = project.genre,
                description = project.description,
                coverImagePath = project.coverImagePath,
                currentProject = project
            ).map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
