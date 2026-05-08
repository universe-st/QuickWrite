package com.universe_st.quickwriter.data.remote

import com.universe_st.quickwriter.data.local.dao.AiOperationDao
import com.universe_st.quickwriter.data.local.entity.AiOperationEntity
import com.universe_st.quickwriter.data.remote.dto.ToolDefinitionDto
import com.universe_st.quickwriter.data.remote.dto.ToolFunctionDto
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.AiOperation
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.CopyFileOperation
import com.universe_st.quickwriter.domain.model.CreateFileOperation
import com.universe_st.quickwriter.domain.model.CreateProjectOperation
import com.universe_st.quickwriter.domain.model.DeleteFileOperation
import com.universe_st.quickwriter.domain.model.DeleteProjectOperation
import com.universe_st.quickwriter.domain.model.EditFileOperation
import com.universe_st.quickwriter.domain.model.HashUtil
import com.universe_st.quickwriter.domain.model.MoveFileOperation
import com.universe_st.quickwriter.domain.model.OperationType
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.domain.model.UpdateProjectOperation
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ToolExecutor(
    private val fileManager: FileManager,
    private val projectRepository: ProjectRepository,
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val aiOperationDao: AiOperationDao,
    private val backupManager: BackupManager,
    private val renameSession: (suspend (String, String) -> Unit)? = null
) {
    private val tools: MutableMap<String, ChatTool> = mutableMapOf()

    fun registerTool(tool: ChatTool) {
        tools[tool.definition.name] = tool
    }

    fun registerTools(toolList: List<ChatTool>) {
        toolList.forEach { registerTool(it) }
    }

    fun getToolDefinitions(): List<ToolDefinitionDto> {
        return tools.values.map { tool ->
            val def = tool.definition
            ToolDefinitionDto(
                function = ToolFunctionDto(
                    name = def.name,
                    description = def.description,
                    parameters = def.parameters
                )
            )
        }
    }

    suspend fun executeToolCall(
        toolCallId: String,
        functionName: String,
        argumentsJson: String,
        projectId: String,
        sessionId: String
    ): String {
        val tool = tools[functionName]
        if (tool == null) {
            return """{"error": "Unknown tool: $functionName"}"""
        }

        return try {
            val arguments = if (argumentsJson.isNotBlank()) {
                try {
                    JSONObject(argumentsJson)
                } catch (je: org.json.JSONException) {
                    return """{"error": "Tool call arguments appear to be truncated (invalid JSON). This usually means max_tokens is too low — the AI output was cut off before the JSON completed. Please increase max_tokens in Settings > Writing Settings and retry. (Detail: ${je.message})"}"""
                }
            } else {
                JSONObject()
            }

            val context = ToolContext(
                projectId = projectId,
                sessionId = sessionId,
                fileManager = fileManager,
                projectRepository = projectRepository,
                projectManagementUseCase = projectManagementUseCase,
                renameSession = renameSession
            )

            val isModificationTool = isModificationTool(functionName)
            val operationId = if (isModificationTool) UUID.randomUUID().toString() else null

            if (isModificationTool && operationId != null) {
                prepareBackup(functionName, arguments, context, operationId)
            }

            val result = tool.execute(arguments, context)

            if (isModificationTool && operationId != null) {
                recordOperation(functionName, arguments, context, operationId, toolCallId, sessionId, projectId)
            }

            result
        } catch (e: Exception) {
            """{"error": "Tool execution failed: ${e.message}"}"""
        }
    }

    suspend fun rollbackOperation(operationId: String, projectId: String): Result<Unit> {
        return try {
            val entity = aiOperationDao.getOperationById(operationId)
                ?: return Result.failure(Exception("Operation not found"))

            val operation = entity.toAiOperation()
                ?: return Result.failure(Exception("Unknown operation type: ${entity.operationType}"))

            val context = ToolContext(
                projectId = projectId,
                fileManager = fileManager,
                projectRepository = projectRepository,
                projectManagementUseCase = projectManagementUseCase
            )

            val canRollback = operation.canRollback(context)
            if (canRollback != null) {
                return Result.failure(Exception(canRollback))
            }

            operation.rollback(context).onSuccess {
                aiOperationDao.deleteOperation(operationId)
                backupManager.deleteOperationBackup(operationId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun prepareBackup(
        functionName: String,
        arguments: JSONObject,
        context: ToolContext,
        operationId: String
    ) {
        val projectId = context.projectId
        val projectDir = context.fileManager.getProjectDirectory(projectId).let {
            if (it.exists()) it.absolutePath else null
        }

        when (functionName) {
            "edit_file", "delete_file", "move_file", "copy_file", "update_chapter_meta" -> {
                val relativePath = arguments.optString("relativePath", arguments.optString("sourcePath", ""))
                if (relativePath.isNotEmpty() && projectDir != null) {
                    val fullPath = File(projectDir, relativePath)
                    if (fullPath.exists()) {
                        backupManager.createOperationBackup(operationId, fullPath)
                    }
                }
            }
            "delete_project" -> {
                val targetProjectId = arguments.optString("projectId", projectId)
                val targetProjectDir = context.fileManager.getProjectDirectory(targetProjectId)
                if (targetProjectDir.exists()) {
                    backupManager.createProjectBackup(targetProjectId, targetProjectDir)
                    context.fileManager.zipProjectToFile(
                        targetProjectDir.absolutePath,
                        File(backupManager.getProjectBackupDir(targetProjectId), "project.zip")
                    )
                }
            }
        }
    }

    private suspend fun recordOperation(
        functionName: String,
        arguments: JSONObject,
        context: ToolContext,
        operationId: String,
        toolCallId: String,
        sessionId: String,
        projectId: String
    ) {
        val now = System.currentTimeMillis()
        val projectDir = context.fileManager.getProjectDirectory(projectId)

        val record = buildOperationRecord(functionName, arguments, projectDir, projectId)

        if (record.operationType.isEmpty()) return

        val entity = AiOperationEntity(
            id = operationId,
            operationType = record.operationType,
            projectId = projectId,
            sessionId = sessionId,
            toolCallId = toolCallId,
            filePath = record.filePath,
            hashBefore = record.hashBefore,
            hashAfter = record.hashAfter,
            backupFile = backupManager.getOperationBackupFile(operationId).absolutePath,
            extraData = record.extraData,
            executedAt = now
        )

        aiOperationDao.insertOperation(entity)
    }

    private data class OperationRecord(
        val operationType: String,
        val filePath: String?,
        val hashBefore: String?,
        val hashAfter: String?,
        val extraData: String?
    )

    private fun buildOperationRecord(
        functionName: String,
        arguments: JSONObject,
        projectDir: File,
        projectId: String
    ): OperationRecord {
        return when (functionName) {
            "edit_file", "update_chapter_meta" -> {
                val relativePath = arguments.optString("relativePath", "")
                val fullPath = File(projectDir, relativePath)
                val before = if (fullPath.exists()) HashUtil.computeSha256(fullPath) else ""
                OperationRecord(OperationType.EDIT_FILE.name, relativePath, before, before, null)
            }
            "delete_file" -> {
                val relativePath = arguments.optString("relativePath", "")
                val fullPath = File(projectDir, relativePath)
                val hash = if (fullPath.exists()) HashUtil.computeSha256(fullPath) else ""
                val isDir = fullPath.isDirectory
                val extra = JSONObject().apply { put("isDirectory", isDir) }.toString()
                OperationRecord(OperationType.DELETE_FILE.name, relativePath, hash, "", extra)
            }
            "create_file" -> {
                val relativePath = arguments.optString("relativePath", "")
                val fullPath = File(projectDir, relativePath)
                val hash = if (fullPath.exists()) HashUtil.computeSha256(fullPath) else ""
                OperationRecord(OperationType.CREATE_FILE.name, relativePath, "", hash, null)
            }
            "move_file" -> {
                val sourcePath = arguments.optString("sourcePath", "")
                val targetPath = arguments.optString("targetPath", "")
                val fullPath = File(projectDir, targetPath)
                val hash = if (fullPath.exists()) HashUtil.computeSha256(fullPath) else ""
                val extra = JSONObject().apply {
                    put("sourcePath", sourcePath)
                    put("isDirectory", fullPath.isDirectory)
                }.toString()
                OperationRecord(OperationType.MOVE_FILE.name, targetPath, "", hash, extra)
            }
            "copy_file" -> {
                val targetPath = arguments.optString("targetPath", "")
                val fullPath = File(projectDir, targetPath)
                val hash = if (fullPath.exists()) HashUtil.computeSha256(fullPath) else ""
                OperationRecord(OperationType.COPY_FILE.name, targetPath, "", hash, null)
            }
            "create_project" -> OperationRecord(OperationType.CREATE_PROJECT.name, null, null, null, null)
            "delete_project" -> {
                val extra = JSONObject().apply {
                    put("deletedProjectId", arguments.optString("projectId", projectId))
                    put("deletedProjectTitle", arguments.optString("confirmTitle", ""))
                }.toString()
                OperationRecord(OperationType.DELETE_PROJECT.name, null, null, null, extra)
            }
            "update_project_info" -> {
                val fields = arguments.optJSONObject("fields") ?: JSONObject()
                OperationRecord(OperationType.UPDATE_PROJECT.name, null, null, null, fields.toString())
            }
            else -> OperationRecord("", null, null, null, null)
        }
    }

    private fun isModificationTool(functionName: String): Boolean {
        return functionName in setOf(
            "edit_file", "delete_file", "create_file", "move_file", "copy_file",
            "create_project", "delete_project", "update_project_info", "update_chapter_meta"
        )
    }

    private fun AiOperationEntity.toAiOperation(): AiOperation? {
        return when (operationType) {
            OperationType.EDIT_FILE.name -> EditFileOperation(
                id = id, projectId = projectId, sessionId = sessionId,
                toolCallId = toolCallId, executedAt = executedAt,
                filePath = filePath ?: "", hashAfterEdit = hashAfter ?: "",
                hashBeforeEdit = hashBefore ?: "", lastModifiedAfterEdit = executedAt
            )
            OperationType.DELETE_FILE.name -> {
                val extra = try { JSONObject(extraData ?: "{}") } catch (e: Exception) { JSONObject() }
                DeleteFileOperation(
                    id = id, projectId = projectId, sessionId = sessionId,
                    toolCallId = toolCallId, executedAt = executedAt,
                    filePath = filePath ?: "", deletedHash = hashBefore ?: "",
                    isDirectory = extra.optBoolean("isDirectory")
                )
            }
            OperationType.CREATE_FILE.name -> CreateFileOperation(
                id = id, projectId = projectId, sessionId = sessionId,
                toolCallId = toolCallId, executedAt = executedAt,
                filePath = filePath ?: "", createdHash = hashAfter ?: ""
            )
            OperationType.MOVE_FILE.name -> {
                val extra = try { JSONObject(extraData ?: "{}") } catch (e: Exception) { JSONObject() }
                MoveFileOperation(
                    id = id, projectId = projectId, sessionId = sessionId,
                    toolCallId = toolCallId, executedAt = executedAt,
                    sourcePath = extra.optString("sourcePath", ""),
                    targetPath = filePath ?: "", hashAfterMove = hashAfter ?: "",
                    isDirectory = extra.optBoolean("isDirectory")
                )
            }
            OperationType.COPY_FILE.name -> CopyFileOperation(
                id = id, projectId = projectId, sessionId = sessionId,
                toolCallId = toolCallId, executedAt = executedAt,
                targetPath = filePath ?: "", hashAfterCopy = hashAfter ?: ""
            )
            OperationType.CREATE_PROJECT.name -> {
                val extra = try { JSONObject(extraData ?: "{}") } catch (e: Exception) { JSONObject() }
                CreateProjectOperation(
                    id = id, projectId = projectId, sessionId = sessionId,
                    toolCallId = toolCallId, executedAt = executedAt,
                    createdProjectId = extra.optString("createdProjectId", projectId)
                )
            }
            OperationType.DELETE_PROJECT.name -> {
                val extra = try { JSONObject(extraData ?: "{}") } catch (e: Exception) { JSONObject() }
                DeleteProjectOperation(
                    id = id, projectId = projectId, sessionId = sessionId,
                    toolCallId = toolCallId, executedAt = executedAt,
                    deletedProjectId = extra.optString("deletedProjectId", projectId),
                    deletedProjectTitle = extra.optString("deletedProjectTitle", "")
                )
            }
            OperationType.UPDATE_PROJECT.name -> UpdateProjectOperation(
                id = id, projectId = projectId, sessionId = sessionId,
                toolCallId = toolCallId, executedAt = executedAt,
                changedFields = try {
                    val fields = JSONObject(extraData ?: "{}")
                    fields.keys().asSequence().associateWith { fields.optString(it) }
                } catch (e: Exception) { emptyMap() }
            )
            else -> null
        }
    }
}
