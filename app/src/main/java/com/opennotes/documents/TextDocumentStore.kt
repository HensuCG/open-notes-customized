package com.opennotes.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File

class DocumentChangedException : Exception()

class TextDocumentStore(private val context: Context) {
    fun name(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document.txt"

    fun read(uri: Uri): ByteArray {
        require(uri.scheme in setOf("content", "file"))
        return requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
            stream.readBytesBounded()
        }
    }

    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= TextFileCodec.MAX_BYTES) { "File too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    fun canWrite(uri: Uri): Boolean {
        if (uri.scheme == "file") return File(requireNotNull(uri.path)).canWrite()
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val supported = runCatching {
                context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)?.use {
                    it.moveToFirst() && it.getInt(0) and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0
                } ?: false
            }.getOrDefault(false)
            if (!supported) return false
        }
        // A caller can also own the provider, without an explicit URI grant. Probe without truncating.
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { true } ?: false
        }.getOrDefault(false)
    }

    fun write(uri: Uri, bytes: ByteArray, expected: ByteArray? = null) {
        // Detect changes made by another editor before truncating the original document.
        if (expected != null && !read(uri).contentEquals(expected)) throw DocumentChangedException()
        requireNotNull(context.contentResolver.openOutputStream(uri, "rwt")).use { it.write(bytes) }
    }
}
