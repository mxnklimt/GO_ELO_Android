package dev.goelo.android.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Small boundary around Android's document providers; callers own user-facing status. */
class AndroidDocumentGateway(private val context: Context) {
    suspend fun read(uri: Uri, maxBytes: Int = 32 * 1024 * 1024): ByteArray = withContext(Dispatchers.IO) {
        require(maxBytes > 0)
        val resolver = context.contentResolver
        val stream = resolver.openInputStream(uri) ?: throw IOException("文件不可读")
        stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) throw IOException("备份文件过大")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    suspend fun write(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("无法写入文件")
        stream.use { output ->
            output.write(bytes)
            output.flush()
        }
    }

    suspend fun createShareUri(fileName: String, bytes: ByteArray): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val now = System.currentTimeMillis()
        dir.listFiles()?.filter { now - it.lastModified() > 24 * 60 * 60 * 1000L }?.forEach { it.delete() }
        val file = File(dir, fileName).canonicalFile
        require(file.parentFile == dir.canonicalFile)
        file.outputStream().use { it.write(bytes); it.flush() }
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    private companion object { const val DEFAULT_BUFFER = 8192 }
}
