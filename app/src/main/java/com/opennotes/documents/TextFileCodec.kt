package com.opennotes.documents

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Keep a document's encoding, BOM, and newline convention when writing it back. */
data class TextFileFormat(val charset: String, val bom: ByteArray, val newline: String) {
    fun encode(text: String): ByteArray {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n').replace("\n", newline)
        return bom + normalized.toByteArray(Charset.forName(charset))
    }
}

object TextFileCodec {
    const val MAX_BYTES = 4 * 1024 * 1024
    val extensions = setOf("txt", "text", "md", "markdown", "log", "csv", "tsv", "json", "jsonl", "ndjson", "xml", "yaml", "yml", "toml", "ini", "cfg", "conf", "properties", "sql", "html", "htm", "css", "svg", "js", "ts", "py", "sh", "kt", "java", "c", "cpp", "h", "rs", "rst", "tex")
    val mimeTypes = setOf("application/json", "application/ld+json", "application/xml", "application/javascript", "application/x-javascript", "application/yaml", "application/x-yaml", "application/toml", "application/sql", "application/x-ndjson")

    fun supports(name: String, mime: String?): Boolean =
        mime?.startsWith("text/") == true || mime in mimeTypes || name.substringAfterLast('.', "").lowercase() in extensions

    fun decode(bytes: ByteArray): Pair<String, TextFileFormat> {
        require(bytes.size <= MAX_BYTES) { "File too large" }
        val signatures = listOf(
            byteArrayOf(0, 0, 0xFE.toByte(), 0xFF.toByte()) to "UTF-32BE",
            byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0, 0) to "UTF-32LE",
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) to "UTF-8",
            byteArrayOf(0xFF.toByte(), 0xFE.toByte()) to "UTF-16LE",
            byteArrayOf(0xFE.toByte(), 0xFF.toByte()) to "UTF-16BE",
        )
        val signature = signatures.firstOrNull { (bom, _) -> bytes.size >= bom.size && bom.indices.all { bytes[it] == bom[it] } }
        val bom = signature?.first ?: byteArrayOf()
        val charset = signature?.second ?: "UTF-8"
        val decoder = Charset.forName(charset).newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = decoder.decode(ByteBuffer.wrap(bytes, bom.size, bytes.size - bom.size)).toString()
        require(text.none { it == '\u0000' || (it.code < 0x20 && it !in "\t\r\n\u000C") }) { "Not a text file" }
        val newline = Regex("\r\n|\r|\n").find(text)?.value ?: "\n"
        return text.replace("\r\n", "\n").replace('\r', '\n') to TextFileFormat(charset, bom, newline)
    }
}
