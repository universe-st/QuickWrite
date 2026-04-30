package com.universe_st.quickwriter.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.universe_st.quickwriter.data.local.dao.AiMessageDao
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.dao.AiOperationDao
import com.universe_st.quickwriter.data.local.dao.AiSessionDao
import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.local.dao.UserSettingDao
import com.universe_st.quickwriter.data.local.entity.AiMessageEntity
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.data.local.entity.AiOperationEntity
import com.universe_st.quickwriter.data.local.entity.AiSessionEntity
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.data.local.entity.UserSettingEntity

@Database(
    entities = [
        ProjectEntity::class,
        AiModelConfigEntity::class,
        UserSettingEntity::class,
        AiSessionEntity::class,
        AiMessageEntity::class,
        AiOperationEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun aiModelConfigDao(): AiModelConfigDao
    abstract fun userSettingDao(): UserSettingDao
    abstract fun aiSessionDao(): AiSessionDao
    abstract fun aiMessageDao(): AiMessageDao
    abstract fun aiOperationDao(): AiOperationDao

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
                    .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
