package com.universe_st.quickwriter.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R

object ToolDisplayHelper {

    data class DisplayStrings(
        val displayName: String,
        val loadingText: String
    )

    private data class ToolDef(
        val nameRes: Int,
        val loadingRes: Int,
        val icon: ImageVector
    )

    private val toolMap = mapOf(
        "create_file" to ToolDef(R.string.tool_name_create_file, R.string.tool_loading_create_file, Icons.Outlined.NoteAdd),
        "edit_file" to ToolDef(R.string.tool_name_edit_file, R.string.tool_loading_edit_file, Icons.Outlined.Edit),
        "delete_file" to ToolDef(R.string.tool_name_delete_file, R.string.tool_loading_delete_file, Icons.Outlined.DeleteOutline),
        "move_file" to ToolDef(R.string.tool_name_move_file, R.string.tool_loading_move_file, Icons.Outlined.DriveFileMove),
        "copy_file" to ToolDef(R.string.tool_name_copy_file, R.string.tool_loading_copy_file, Icons.Outlined.ContentCopy),
        "create_project" to ToolDef(R.string.tool_name_create_project, R.string.tool_loading_create_project, Icons.Outlined.CreateNewFolder),
        "delete_project" to ToolDef(R.string.tool_name_delete_project, R.string.tool_loading_delete_project, Icons.Outlined.DeleteForever),
        "update_project_info" to ToolDef(R.string.tool_name_update_project_info, R.string.tool_loading_update_project_info, Icons.Outlined.Settings),
        "view_file" to ToolDef(R.string.tool_name_view_file, R.string.tool_loading_view_file, Icons.Outlined.Visibility),
        "search_in_project" to ToolDef(R.string.tool_name_search_in_project, R.string.tool_loading_search_in_project, Icons.Outlined.Search),
        "get_project_list" to ToolDef(R.string.tool_name_get_project_list, R.string.tool_loading_get_project_list, Icons.Outlined.FolderOpen),
        "get_project_info" to ToolDef(R.string.tool_name_get_project_info, R.string.tool_loading_get_project_info, Icons.Outlined.Info),
        "get_folder_structure" to ToolDef(R.string.tool_name_get_folder_structure, R.string.tool_loading_get_folder_structure, Icons.Outlined.FolderOpen)
    )

    @Composable
    fun getDisplayStrings(toolName: String): DisplayStrings {
        val def = toolMap[toolName]
        return if (def != null) {
            DisplayStrings(
                displayName = stringResource(def.nameRes),
                loadingText = stringResource(def.loadingRes)
            )
        } else {
            DisplayStrings(displayName = toolName, loadingText = "Executing...")
        }
    }

    fun getIcon(toolName: String): ImageVector {
        return toolMap[toolName]?.icon ?: Icons.Outlined.Info
    }
}
