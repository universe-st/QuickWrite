package com.universe_st.quickwriter.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.local.dao.UserSettingDao
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.data.local.entity.UserSettingEntity

@Database(
    entities = [
        ProjectEntity::class,
        AiModelConfigEntity::class,
        UserSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun aiModelConfigDao(): AiModelConfigDao
    abstract fun userSettingDao(): UserSettingDao

    companion object {
        const val DATABASE_NAME = "quickwrite_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}