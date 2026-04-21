package com.universe_st.quickwriter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "author")
    val author: String,
    @ColumnInfo(name = "genre")
    val genre: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "created_time")
    val createdTime: Long,
    @ColumnInfo(name = "modified_time")
    val modifiedTime: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String?,
    @ColumnInfo(name = "word_count")
    val wordCount: Int = 0,
    @ColumnInfo(name = "chapter_count")
    val chapterCount: Int = 0,
    @ColumnInfo(name = "storage_path")
    val storagePath: String
)