package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject

class GetProjectListTool : ChatTool {

    override val definition = ToolDefinition(
        name = "get_project_list",
        description = "Get the list of all projects in the application, including basic info (ID, title, author, genre, status)",
        parameters = mapOf(
            "type" to "object",
            "properties" to emptyMap<String, Any>(),
            "required" to emptyList<String>()
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projects = context.projectRepository.getAllProjects().firstOrNull() ?: emptyList()

        val result = JSONObject()
        val items = JSONArray()
        projects.forEach { p ->
            val item = JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("author", p.author)
                put("genre", p.genre)
                put("status", p.status)
                put("createdTime", p.createdTime)
                put("modifiedTime", p.modifiedTime)
            }
            items.put(item)
        }
        result.put("projects", items)
        result.put("count", items.length())
        return result.toString(2)
    }
}
