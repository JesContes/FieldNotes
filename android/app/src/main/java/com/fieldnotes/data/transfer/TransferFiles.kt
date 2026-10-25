package com.fieldnotes.data.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransferFiles {
    const val MIME_JSON = "application/json"
    const val MIME_GZIP = "application/gzip"
    private const val MAX_BYTES = 32 * 1024 * 1024

    fun read(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val declaredSize = displaySize(context, uri)
        if (declaredSize != null && declaredSize > MAX_BYTES) {
            throw ImportException("文件过大（上限 32 MB）")
        }
        val input = resolver.openInputStream(uri) ?: throw IOException("无法读取文件")
        return input.use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BYTES) throw ImportException("文件过大（上限 32 MB）")
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }

    fun write(context: Context, uri: Uri, bytes: ByteArray) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("无法写入文件")
        output.use { it.write(bytes) }
    }

    fun displayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
    }

    fun displaySize(context: Context, uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
                }
        }.getOrNull()
    }

    fun suggestedName(gzip: Boolean): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(Date())
        return "寸笺-$stamp.json" + if (gzip) ".gz" else ""
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
