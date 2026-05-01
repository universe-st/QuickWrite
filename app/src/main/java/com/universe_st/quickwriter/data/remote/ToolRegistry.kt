package com.universe_st.quickwriter.data.remote

import com.universe_st.quickwriter.data.remote.tools.CopyFileTool
import com.universe_st.quickwriter.data.remote.tools.CreateFileTool
import com.universe_st.quickwriter.data.remote.tools.CreateProjectTool
import com.universe_st.quickwriter.data.remote.tools.DeleteFileTool
import com.universe_st.quickwriter.data.remote.tools.DeleteProjectTool
import com.universe_st.quickwriter.data.remote.tools.EditFileTool
import com.universe_st.quickwriter.data.remote.tools.GetChapterMetaTool
import com.universe_st.quickwriter.data.remote.tools.GetFolderStructureTool
import com.universe_st.quickwriter.data.remote.tools.GetProjectInfoTool
import com.universe_st.quickwriter.data.remote.tools.GetProjectListTool
import com.universe_st.quickwriter.data.remote.tools.MoveFileTool
import com.universe_st.quickwriter.data.remote.tools.SearchInProjectTool
import com.universe_st.quickwriter.data.remote.tools.UpdateChapterMetaTool
import com.universe_st.quickwriter.data.remote.tools.UpdateProjectInfoTool
import com.universe_st.quickwriter.data.remote.tools.ViewFileTool
import com.universe_st.quickwriter.domain.model.ChatTool

object ToolRegistry {
    val allTools: List<ChatTool> = listOf(
        GetProjectListTool(),
        GetProjectInfoTool(),
        GetFolderStructureTool(),
        ViewFileTool(),
        EditFileTool(),
        DeleteFileTool(),
        CreateFileTool(),
        MoveFileTool(),
        CopyFileTool(),
        SearchInProjectTool(),
        UpdateProjectInfoTool(),
        CreateProjectTool(),
        DeleteProjectTool(),
        GetChapterMetaTool(),
        UpdateChapterMetaTool()
    )
}
