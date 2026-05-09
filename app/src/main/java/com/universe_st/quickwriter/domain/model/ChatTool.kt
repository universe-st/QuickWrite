package com.universe_st.quickwriter.domain.model

import com.universe_st.quickwriter.data.remote.ViewTracker
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject

interface ChatTool {
    val definition: ToolDefinition

    suspend fun execute(arguments: JSONObject, context: ToolContext): String
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class ToolContext(
    val projectId: String,
    val sessionId: String = "",
    val fileManager: FileManager,
    val projectRepository: ProjectRepository,
    val projectManagementUseCase: ProjectManagementUseCase,
    val renameSession: (suspend (String, String) -> Unit)? = null,
    val viewTracker: ViewTracker? = null
)
