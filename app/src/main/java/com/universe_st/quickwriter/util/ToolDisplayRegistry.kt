package com.universe_st.quickwriter.util

object ToolDisplayRegistry {

    enum class Category { READ, WRITE, MANAGE }

    data class Info(
        val displayNameKey: String,
        val loadingTextKey: String
    )

    private val registry = mapOf(
        "create_file" to Info("tool_name_create_file", "tool_loading_create_file"),
        "edit_file" to Info("tool_name_edit_file", "tool_loading_edit_file"),
        "delete_file" to Info("tool_name_delete_file", "tool_loading_delete_file"),
        "move_file" to Info("tool_name_move_file", "tool_loading_move_file"),
        "copy_file" to Info("tool_name_copy_file", "tool_loading_copy_file"),
        "create_project" to Info("tool_name_create_project", "tool_loading_create_project"),
        "delete_project" to Info("tool_name_delete_project", "tool_loading_delete_project"),
        "update_project_info" to Info("tool_name_update_project_info", "tool_loading_update_project_info"),
        "view_file" to Info("tool_name_view_file", "tool_loading_view_file"),
        "search_in_project" to Info("tool_name_search_in_project", "tool_loading_search_in_project"),
        "get_project_list" to Info("tool_name_get_project_list", "tool_loading_get_project_list"),
        "get_project_info" to Info("tool_name_get_project_info", "tool_loading_get_project_info"),
        "get_folder_structure" to Info("tool_name_get_folder_structure", "tool_loading_get_folder_structure"),
        "get_chapter_meta" to Info("tool_name_get_chapter_meta", "tool_loading_get_chapter_meta"),
        "update_chapter_meta" to Info("tool_name_update_chapter_meta", "tool_loading_update_chapter_meta")
    )

    fun getInfo(toolName: String): Info? = registry[toolName]

    fun getCategory(toolName: String): Category = when (toolName) {
        "create_file", "edit_file", "delete_file", "move_file", "copy_file", "update_chapter_meta" -> Category.WRITE
        "create_project", "delete_project", "update_project_info" -> Category.MANAGE
        else -> Category.READ
    }
}
