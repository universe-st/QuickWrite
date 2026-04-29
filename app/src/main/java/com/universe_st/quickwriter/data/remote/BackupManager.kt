package com.universe_st.quickwriter.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupManager(
    private val appFilesDir: File
) {
    private val backupDir = File(appFilesDir, "ai_backups")
    private val operationsDir = File(backupDir, "operations")
    private val projectsDir = File(backupDir, "projects")
    private val metadataFile = File(backupDir, "metadata.json")
    private val gson = Gson()

    private val maxTotalSize: Long = 5L * 1024 * 1024 * 1024

    init {
        operationsDir.mkdirs()
        projectsDir.mkdirs()
    }

    fun getBackupDir(): File = backupDir

    suspend fun ensureCapacity(requiredSize: Long) = withContext(Dispatchers.IO) {
        var totalSize = calculateTotalSize()
        while (totalSize + requiredSize > maxTotalSize) {
            val oldest = findOldestExpendableOperation()
            if (oldest == null) throw BackupCapacityExceededException()
            deleteBackup(oldest)
            totalSize = calculateTotalSize()
        }
    }

    fun getOperationBackupFile(operationId: String): File {
        return File(operationsDir, "$operationId.bak")
    }

    fun getProjectBackupDir(projectId: String): File {
        return File(projectsDir, projectId).also { it.mkdirs() }
    }

    suspend fun createOperationBackup(operationId: String, sourceFile: File) = withContext(Dispatchers.IO) {
        val backupFile = getOperationBackupFile(operationId)
        val fileSize = if (sourceFile.isFile) sourceFile.length() else 0
        ensureCapacity(fileSize)
        sourceFile.copyTo(backupFile, overwrite = true)
        backupFile
    }

    suspend fun createProjectBackup(projectId: String, projectDir: File) = withContext(Dispatchers.IO) {
        val projectBackupDir = getProjectBackupDir(projectId)
        val projectZip = File(projectBackupDir, "project.zip")

        var zipSize = 0L
        projectDir.walkTopDown().forEach { file ->
            if (file.isFile) zipSize += file.length()
        }
        ensureCapacity(zipSize)
    }

    suspend fun deleteOperationBackup(operationId: String) = withContext(Dispatchers.IO) {
        getOperationBackupFile(operationId).delete()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        backupDir.deleteRecursively()
        backupDir.mkdirs()
        operationsDir.mkdirs()
        projectsDir.mkdirs()
    }

    fun getTotalBackupSize(): Long = calculateTotalSize()

    fun getBackupCount(): Int {
        return operationsDir.listFiles()?.size ?: 0
    }

    private fun calculateTotalSize(): Long {
        if (!backupDir.exists()) return 0L
        return backupDir.walkTopDown().sumOf { file ->
            if (file.isFile) file.length() else 0L
        }
    }

    private fun deleteBackup(operationId: String) {
        val backupFile = getOperationBackupFile(operationId)
        backupFile.delete()
    }

    private fun findOldestExpendableOperation(): String? {
        val files = operationsDir.listFiles() ?: return null
        return files.minByOrNull { it.lastModified() }?.nameWithoutExtension
    }
}

class BackupCapacityExceededException : Exception("Backup capacity exceeded (5GB limit)")
