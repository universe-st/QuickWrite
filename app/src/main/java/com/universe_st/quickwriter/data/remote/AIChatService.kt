package com.universe_st.quickwriter.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.universe_st.quickwriter.MainActivity
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.data.local.dao.AiMessageDao
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.dao.AiOperationDao
import com.universe_st.quickwriter.data.local.dao.AiSessionDao
import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.repository.AiServiceRepository
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.data.repository.UserSettingsRepository
import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.SessionSummary
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager

class AIChatService : Service(), IChatService {

    private val binder = ChatServiceBinder(this)

    private val backupManager: BackupManager by lazy {
        BackupManager(filesDir)
    }

    private val toolExecutor: ToolExecutor by lazy {
        val executor = ToolExecutor(
            fileManager = AppServiceContainer.fileManager,
            projectRepository = AppServiceContainer.projectRepository,
            projectManagementUseCase = AppServiceContainer.projectManagementUseCase,
            aiOperationDao = AppServiceContainer.aiOperationDao,
            backupManager = backupManager
        )
        executor.registerTools(ToolRegistry.allTools)
        executor
    }

    private val sessionManager: SessionManager by lazy {
        SessionManager(
            aiSessionDao = AppServiceContainer.aiSessionDao,
            aiMessageDao = AppServiceContainer.aiMessageDao,
            aiOperationDao = AppServiceContainer.aiOperationDao,
            projectDao = AppServiceContainer.projectDao,
            aiModelConfigDao = AppServiceContainer.aiModelConfigDao
        )
    }

    private val apiDispatcher: ApiDispatcher by lazy {
        ApiDispatcher(
            aiServiceRepository = AppServiceContainer.aiServiceRepository,
            aiMessageDao = AppServiceContainer.aiMessageDao,
            aiModelConfigDao = AppServiceContainer.aiModelConfigDao,
            sessionManager = sessionManager,
            toolExecutor = toolExecutor,
            userSettingsRepository = AppServiceContainer.userSettingsRepository
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_GENERATION -> {
                intent.getStringExtra(EXTRA_SESSION_ID)?.let { stopGeneration(it) }
            }
        }

        val notification = buildNotification(
            getString(R.string.ai_service_waiting)
        )
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        sessionManager.clear()
        super.onDestroy()
    }

    override fun createSession(projectId: String, systemPrompt: String?, modelConfigId: Int?): String {
        return sessionManager.createSession(projectId, systemPrompt, modelConfigId)
    }

    override fun deleteSession(sessionId: String) {
        sessionManager.deleteSession(sessionId)
    }

    override fun switchToSession(sessionId: String) {
        sessionManager.switchToSession(sessionId)
    }

    override fun getSessionList(): List<SessionSummary> {
        return sessionManager.getSessionList()
    }

    override fun getSessionDetail(sessionId: String): SessionDetail? {
        return sessionManager.getSessionDetail(sessionId)
    }

    override fun renameSession(sessionId: String, title: String) {
        sessionManager.renameSession(sessionId, title)
    }

    override fun sendMessage(sessionId: String, content: String, attachedFiles: List<String>) {
        apiDispatcher.sendMessage(sessionId, content, attachedFiles)
    }

    override fun stopGeneration(sessionId: String) {
        apiDispatcher.stopGeneration(sessionId)
    }

    override fun retryLastMessage(sessionId: String) {
        apiDispatcher.retryLastMessage(sessionId)
    }

    override fun deleteMessage(sessionId: String, messageIndex: Int) {
        sessionManager.deleteMessage(sessionId, messageIndex)
    }

    override fun observeSessionState(sessionId: String): StateFlowWrapper<SessionState> {
        return sessionManager.observeSessionState(sessionId)
    }

    override fun observeSessionList(): StateFlowWrapper<List<SessionSummary>> {
        return sessionManager.observeSessionList()
    }

    fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(getString(R.string.ai_service_title))
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            android.R.drawable.ic_media_pause,
            getString(R.string.ai_service_stop),
            PendingIntent.getService(
                this,
                1,
                Intent(this, AIChatService::class.java).apply {
                    action = ACTION_STOP_GENERATION
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ai_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.ai_service_channel_desc)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    internal class ChatServiceBinder(private val service: AIChatService) : Binder() {
        fun getService(): IChatService = service
    }

    companion object {
        const val CHANNEL_ID = "ai_chat_service_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_GENERATION = "com.universe_st.quickwriter.action.STOP_GENERATION"
        const val EXTRA_SESSION_ID = "session_id"
    }
}

object AppServiceContainer {
    lateinit var aiSessionDao: AiSessionDao
    lateinit var aiMessageDao: AiMessageDao
    lateinit var aiOperationDao: AiOperationDao
    lateinit var aiServiceRepository: AiServiceRepository
    lateinit var projectDao: ProjectDao
    lateinit var aiModelConfigDao: AiModelConfigDao
    lateinit var fileManager: FileManager
    lateinit var projectRepository: ProjectRepository
    lateinit var projectManagementUseCase: ProjectManagementUseCase
    lateinit var userSettingsRepository: UserSettingsRepository
}
