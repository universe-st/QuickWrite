package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class UpdateProjectInfoTool : ChatTool {

    override val definition = ToolDefinition(
        name = "update_project_info",
        description = "Modify basic project information (title, author, genre, description, status). Updates both Room database and info.json.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "fields" to mapOf(
                    "type" to "object",
                    "description" to "Key-value pairs of fields to update. Supported: title, author, genre, description, status"
                )
            ),
            "required" to listOf("fields")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
        val fields = arguments.optJSONObject("fields")

        if (fields == null || fields.length() == 0) {
            return """{"error": "fields object is required"}"""
        }

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val allowedFields = setOf("title", "author", "genre", "description", "status")
        val changedFields = mutableListOf<String>()

        val newTitle = fields.optString("title", project.title)
        if (fields.has("title") && newTitle != project.title) changedFields.add("title")

        val newAuthor = fields.optString("author", project.author)
        if (fields.has("author") && newAuthor != project.author) changedFields.add("author")

        val newGenre = fields.optString("genre", project.genre)
        if (fields.has("genre") && newGenre != project.genre) changedFields.add("genre")

        val newDescription = if (fields.has("description")) {
            if (fields.isNull("description")) null else fields.optString("description", "")
        } else {
            project.description
        }
        if (fields.has("description")) changedFields.add("description")

        val newStatus = fields.optString("status", project.status)
        if (fields.has("status") && newStatus != project.status) changedFields.add("status")

        if (changedFields.isEmpty()) {
            return """{"message": "No fields changed"}"""
        }

        val updatedProject = project.copy(
            title = newTitle,
            author = newAuthor,
            genre = newGenre,
            description = newDescription,
            status = newStatus
        )
        context.projectRepository.updateProject(
            id = projectId,
            title = updatedProject.title,
            author = updatedProject.author,
            genre = updatedProject.genre,
            description = updatedProject.description,
            coverImagePath = updatedProject.coverImagePath,
            currentProject = project
        )

        val infoJsonFile = File(project.storagePath, "info.json")
        if (infoJsonFile.exists()) {
            val infoJson = JSONObject(infoJsonFile.readText())
            changedFields.forEach { field ->
                when (field) {
                    "title" -> infoJson.put("title", newTitle)
                    "author" -> infoJson.put("author", newAuthor)
                    "genre" -> infoJson.put("genre", newGenre)
                }
            }
            infoJsonFile.writeText(infoJson.toString(2))
        }

        val result = JSONObject().apply {
            put("updated", true)
            put("projectId", projectId)
            put("changedFields", JSONObject(fields.toString()))
        }
        return result.toString(2)
    }
}
