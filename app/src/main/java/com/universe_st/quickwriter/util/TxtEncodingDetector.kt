package com.universe_st.quickwriter.util

import org.mozilla.universalchardet.UniversalDetector
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

object TxtEncodingDetector {

    private val CHARSET_MAPPING = mapOf(
        "GB2312" to "GBK",
        "GB18030" to "GBK",
        "Big5-HKSCS" to "Big5"
    )

    fun detectEncoding(file: File): Charset {
        return try {
            val detector = UniversalDetector(null)
            FileInputStream(file).use { input ->
                val buf = ByteArray(4096)
                var nread: Int
                while (input.read(buf).also { nread = it } > 0 && !detector.isDone) {
                    detector.handleData(buf, 0, nread)
                }
                detector.dataEnd()
            }
            val detected = detector.detectedCharset
            val mapped = if (detected != null) CHARSET_MAPPING[detected] ?: detected else null
            val result = if (mapped != null) {
                try {
                    Charset.forName(mapped)
                } catch (_: Exception) {
                    Charsets.UTF_8
                }
            } else {
                Charsets.UTF_8
            }
            Timber.tag("TxtEncoding").i("Detected: %s -> mapped: %s -> %s", detected, mapped, result.name())
            result
        } catch (e: Exception) {
            Timber.tag("TxtEncoding").e(e, "Encoding detection failed, fallback to UTF-8")
            Charsets.UTF_8
        }
    }
}
