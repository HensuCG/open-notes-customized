package com.opennotes.documents

import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.Charset

class TextFileCodecTest {
    @Test
    fun preservesUnicodeBomAndWindowsNewlines() {
        val text = "First\r\nÕun 😀\r\n"
        for ((charset, bom) in listOf(
            "UTF-8" to byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()),
            "UTF-16LE" to byteArrayOf(0xFF.toByte(), 0xFE.toByte()),
            "UTF-16BE" to byteArrayOf(0xFE.toByte(), 0xFF.toByte()),
            "UTF-32LE" to byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0, 0),
        )) {
            val original = bom + text.toByteArray(Charset.forName(charset))
            val (decoded, format) = TextFileCodec.decode(original)
            assertEquals("First\nÕun 😀\n", decoded)
            assertArrayEquals(original, format.encode(decoded))
            assertArrayEquals(bom + "Edited\r\nÕun 😀\r\n".toByteArray(Charset.forName(charset)), format.encode(decoded.replace("First", "Edited")))
        }
    }

    @Test
    fun preservesEmptyUtf8AndLegacyCarriageReturns() {
        for (text in listOf("", "One\nTwo\n", "One\rTwo\r", "no final newline")) {
            val (decoded, format) = TextFileCodec.decode(text.toByteArray())
            assertArrayEquals(text.toByteArray(), format.encode(decoded))
        }
    }

    @Test
    fun recognizesPlainTextMarkdownAndStructuredFormats() {
        for (name in listOf("notes.TXT", "README.md", "data.csv", "data.json", "settings.yaml", "settings.ini", "script.py", "page.xml")) {
            assertTrue(name, TextFileCodec.supports(name, "application/octet-stream"))
        }
        assertTrue(TextFileCodec.supports("opaque", "text/plain"))
        assertFalse(TextFileCodec.supports("photo.png", "image/png"))
    }

    @Test
    fun rejectsOversizedEditsBeforeTheyCanTruncateTheOriginal() {
        val format = TextFileCodec.decode(byteArrayOf()).second
        assertTrue(runCatching { format.encode("a".repeat(TextFileCodec.MAX_BYTES + 1)) }.exceptionOrNull() is TextFileTooLargeException)
        val utf16 = TextFileFormat("UTF-16LE", byteArrayOf(0xFF.toByte(), 0xFE.toByte()), "\n")
        assertTrue(runCatching { utf16.encode("a".repeat(TextFileCodec.MAX_BYTES / 2)) }.exceptionOrNull() is TextFileTooLargeException)
    }

    @Test
    fun rejectsBinaryMalformedUnicodeAndOversizedFiles() {
        for (bytes in listOf(byteArrayOf(0, 1, 2), byteArrayOf(0xC3.toByte()), ByteArray(TextFileCodec.MAX_BYTES + 1))) {
            assertTrue(runCatching { TextFileCodec.decode(bytes) }.isFailure)
        }
    }
}
