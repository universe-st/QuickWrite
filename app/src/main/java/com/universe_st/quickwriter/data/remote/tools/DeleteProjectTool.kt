package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject

class DeleteProjectTool : ChatTool {

    override val definition = ToolDefinition(
        name = "delete_project",
        description = "Delete a specified project including all files and database records. Requires project title confirmation to prevent accidental deletion.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project to delete (must be explicitly specified)"
                ),
                "confirmTitle" to mapOf(
                    "type" to "string",
                    "description" to "Must provide the exact project title to confirm deletion"
                )
            ),
            "required" to listOf("projectId", "confirmTitle")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val confirmTitle = arguments.optString("confirmTitle", "")

        if (confirmTitle.isEmpty()) return """{"error": "confirmTitle is required for safety"}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        if (project.title != confirmTitle) {
            return """{"error": "Title confirmation failed. Expected '${project.title}' but got '$confirmTitle'"}"""
        }

        val result = context.projectManagementUseCase.deleteProject(projectId)

        return result.fold(
            onSuccess = {
                JSONObject().apply {
                    put("deleted", true)
                    put("projectId", projectId)
                    put("title", project.title)
                }.toString(2)
            },
            onFailure = { error ->
                """{"error": "Failed to delete project: ${error.message}"}"""
            }
        )
    }
}
