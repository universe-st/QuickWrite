package com.universe_st.quickwriter.util

import android.content.Context
import com.universe_st.quickwriter.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

object CoverImageProcessor {

    private const val COVER_WIDTH = 600
    private const val COVER_HEIGHT = 800
    private const val COVER_QUALITY = 90
    private const val COVER_FILE_NAME = "cover.jpg"

    fun getCoverFilePath(projectDir: String): String {
        return File(projectDir, COVER_FILE_NAME).absolutePath
    }

    fun hasCoverImage(projectDir: String): Boolean {
        return File(projectDir, COVER_FILE_NAME).exists()
    }

    fun getCoverFile(projectDir: String): File {
        return File(projectDir, COVER_FILE_NAME)
    }

    suspend fun saveCoverImage(
        context: Context,
        sourceUri: Uri,
        projectDir: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IOException(context.getString(R.string.cover_cannot_open)))

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return@withContext Result.failure(IOException(context.getString(R.string.cover_cannot_decode)))
            }

            val processedBitmap = processBitmap(originalBitmap)

            val coverFile = File(projectDir, COVER_FILE_NAME)
            FileOutputStream(coverFile).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, out)
                out.flush()
            }

            if (originalBitmap != processedBitmap) {
                originalBitmap.recycle()
            }
            processedBitmap.recycle()

            Result.success(coverFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "保存书封图片失败")
            Result.failure(e)
        }
    }

    private fun processBitmap(original: Bitmap): Bitmap {
        val srcWidth = original.width
        val srcHeight = original.height

        val scaleX = COVER_WIDTH.toFloat() / srcWidth
        val scaleY = COVER_HEIGHT.toFloat() / srcHeight
        val scale = minOf(scaleX, scaleY)

        val scaledWidth = (srcWidth * scale).toInt()
        val scaledHeight = (srcHeight * scale).toInt()

        val scaledBitmap = original.scale(scaledWidth, scaledHeight)

        val result = createBitmap(COVER_WIDTH, COVER_HEIGHT)
        result.eraseColor(Color.WHITE)

        val canvas = Canvas(result)
        val left = (COVER_WIDTH - scaledWidth) / 2f
        val top = (COVER_HEIGHT - scaledHeight) / 2f

        canvas.drawBitmap(scaledBitmap, left, top, null)

        if (scaledBitmap != original && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        return result
    }

    fun deleteCoverImage(projectDir: String): Result<Unit> {
        return try {
            val coverFile = File(projectDir, COVER_FILE_NAME)
            if (coverFile.exists()) {
                coverFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "删除书封图片失败")
            Result.failure(e)
        }
    }
}
