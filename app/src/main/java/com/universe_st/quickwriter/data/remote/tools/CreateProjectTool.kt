package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject

class CreateProjectTool : ChatTool {

    override val definition = ToolDefinition(
        name = "create_project",
        description = "Create a new novel project with directory structure and metadata. Generates a UUID, creates directories, writes info.json, and inserts Room record.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "title" to mapOf(
                    "type" to "string",
                    "description" to "Novel title"
                ),
                "author" to mapOf(
                    "type" to "string",
                    "description" to "Author name"
                ),
                "genre" to mapOf(
                    "type" to "string",
                    "description" to "Novel genre"
                ),
                "description" to mapOf(
                    "type" to "string",
                    "description" to "Project description (optional)"
                )
            ),
            "required" to listOf("title", "author", "genre")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val title = arguments.optString("title", "")
        val author = arguments.optString("author", "")
        val genre = arguments.optString("genre", "")
        val description = if (arguments.has("description") && !arguments.isNull("description"))
            arguments.optString("description", "") else null

        if (title.isEmpty()) return """{"error": "title is required"}"""
        if (author.isEmpty()) return """{"error": "author is required"}"""
        if (genre.isEmpty()) return """{"error": "genre is required"}"""

        val result = context.projectManagementUseCase.createProject(
            title = title,
            author = author,
            genre = genre,
            description = description
        )

        return result.fold(
            onSuccess = { project ->
                JSONObject().apply {
                    put("created", true)
                    put("projectId", project.id)
                    put("title", project.title)
                    put("storagePath", project.storagePath)
                }.toString(2)
            },
            onFailure = { error ->
                """{"error": "Failed to create project: ${error.message}"}"""
            }
        )
    }
}
