package com.universe_st.quickwriter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY modified_time DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY created_time DESC")
    fun getAllProjectsByCreatedTime(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY title ASC")
    fun getAllProjectsByTitle(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE title = :title LIMIT 1")
    suspend fun getProjectByTitle(title: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("UPDATE projects SET word_count = word_count + :wordCount WHERE id = :projectId")
    suspend fun updateWordCount(projectId: String, wordCount: Int)

    @Query("UPDATE projects SET chapter_count = chapter_count + 1 WHERE id = :projectId")
    suspend fun incrementChapterCount(projectId: String)
}