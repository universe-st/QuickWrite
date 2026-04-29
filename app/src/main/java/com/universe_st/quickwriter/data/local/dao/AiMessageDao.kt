package com.universe_st.quickwriter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.universe_st.quickwriter.data.local.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {

    @Query("SELECT * FROM ai_messages WHERE session_id = :sessionId ORDER BY message_order ASC")
    suspend fun getMessagesBySession(sessionId: String): List<AiMessageEntity>

    @Query("SELECT * FROM ai_messages WHERE session_id = :sessionId AND is_silent = 0 ORDER BY message_order ASC")
    fun getVisibleMessagesBySession(sessionId: String): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AiMessageEntity>)

    @Query("DELETE FROM ai_messages WHERE session_id = :sessionId AND message_order >= :fromOrder")
    suspend fun deleteMessagesFrom(sessionId: String, fromOrder: Int)

    @Query("DELETE FROM ai_messages WHERE session_id = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: String)

    @Query("""
        SELECT COUNT(*) FROM ai_messages 
        WHERE session_id = :sessionId AND is_silent = 0 AND role = 'user'
    """)
    suspend fun getUserMessageCount(sessionId: String): Int
}
