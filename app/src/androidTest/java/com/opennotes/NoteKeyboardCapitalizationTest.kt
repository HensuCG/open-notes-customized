package com.opennotes

import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.opennotes.notes.presentation.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoteKeyboardCapitalizationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun titleAndBodyRequestSentenceCapitalizationWithoutRewritingText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("opennotes://note/-1"))

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
            for (field in 0..1) {
                // Exercise both the String title field and TextFieldValue body field.
                val editor = compose.onAllNodes(hasSetTextAction())[field]
                editor.performClick()
                for (text in listOf("hello. ", "hello? ", "hello! ", "hello, ")) {
                    editor.performTextReplacement(text)
                    compose.waitForIdle()
                    scenario.onActivity { activity ->
                        val (info, connection) = requireNotNull(findInputConnection(activity.window.decorView))
                        val sentenceFlag = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        assertTrue("Field $field must request sentence capitals", info.inputType and sentenceFlag != 0)
                        assertEquals("Preserve typed casing and trailing space", text, connection.getTextBeforeCursor(text.length, 0).toString())
                        val caps = connection.getCursorCapsMode(sentenceFlag) and sentenceFlag
                        assertEquals("Field $field after '$text'", if (text.endsWith(", ")) 0 else sentenceFlag, caps)
                    }
                }
            }
        }
    }

    private fun findInputConnection(view: View): Pair<EditorInfo, InputConnection>? {
        val info = EditorInfo()
        view.onCreateInputConnection(info)?.let { return info to it }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findInputConnection(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
