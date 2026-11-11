package com.opennotes

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.opennotes.documents.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class ExternalTextFileTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun uri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    @Test
    fun opensEditsOriginalAndRetainsUnsavedDraftAcrossRecreation() {
        val file = File(context.getExternalFilesDir(null), "editor-test.md")
        val original = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "First\r\nÕun\r\n".toByteArray(Charset.forName("UTF-16LE"))
        file.writeBytes(original)
        val target = uri(file)
        val intent = Intent(context, TextFileActivity::class.java).setAction(Intent.ACTION_EDIT).setDataAndType(target, "text/markdown")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        ActivityScenario.launch<TextFileActivity>(intent).use { scenario ->
            compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 1 }
            compose.onNode(hasSetTextAction()).performTextReplacement("Edited\nÕun\n")
            compose.waitForIdle()
            assertArrayEquals(original, file.readBytes())
            scenario.recreate()
            compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction() and hasText("Edited\nÕun\n")).fetchSemanticsNodes().size == 1 }
            compose.onNodeWithContentDescription(context.getString(R.string.save_text_file)).assertIsEnabled().performClick()
            compose.waitUntil(10_000) { file.readBytes().contentEquals(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "Edited\r\nÕun\r\n".toByteArray(Charset.forName("UTF-16LE"))) }
        }
        file.delete()
    }

    @Test
    fun protectsConflictingOriginalAndCanWriteACopy() {
        val file = File(context.getExternalFilesDir(null), "conflict-test.json").apply { writeText("original") }
        val copy = File(context.getExternalFilesDir(null), "copy-test.json").apply { writeText("") }
        val store = TextDocumentStore(context)
        val target = uri(file)
        val baseline = store.read(target)
        file.writeText("changed elsewhere")
        assertTrue(runCatching { store.write(target, "edited".toByteArray(), baseline) }.exceptionOrNull() is DocumentChangedException)
        assertEquals("changed elsewhere", file.readText())
        store.write(uri(copy), "edited".toByteArray())
        assertEquals("edited", copy.readText())
        file.setWritable(false, false)
        assertFalse(store.canWrite(Uri.fromFile(file)))
        file.setWritable(true, false)
        file.delete()
        copy.delete()
    }

    @Test
    fun advertisesOpenWithForTextAndStructuredDocuments() {
        for ((name, mime) in listOf("test.txt" to "text/plain", "test.md" to "text/markdown", "test.json" to "application/json", "test.xml" to "application/xml", "test.yaml" to "application/octet-stream")) {
            val target = Uri.parse("content://${context.packageName}.fileprovider/notes_backups/$name")
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(target, mime).setPackage(context.packageName)
            val matches = context.packageManager.queryIntentActivities(intent, 0)
            assertTrue("No Open With handler for $name ($mime)", matches.any { it.activityInfo.name == TextFileActivity::class.java.name })
        }
    }
}
