package com.opennotes

import android.appwidget.*
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.opennotes.notes.domain.model.Note
import com.opennotes.notes.presentation.MainActivity
import com.opennotes.widget.*
import dagger.hilt.EntryPoints
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WidgetSynchronizationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun checkboxClicksUpdateOpenEditorAndAppEditsRefreshExpiredWidgets() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val repository = EntryPoints.get(context.applicationContext, WidgetModelRepositoryEntryPoint::class.java).noteRepository()
        val id = repository.insertNote(Note("Widget synchronization", "- [ ] Task\nOriginal", 1L, 1L, 0))
        val host = AppWidgetHost(context, 4172)
        val widgetId = host.allocateAppWidgetId()
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, NotesWidgetReceiver::class.java)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 300)
        }
        assertTrue("Grant widget bind permission for this emulator test", manager.bindAppWidgetIdIfAllowed(widgetId, provider, options))
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(widgetId)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply { this[intPreferencesKey("note_$widgetId")] = id }
        }
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse("opennotes://note/$id"))
        var widget: AppWidgetHostView? = null
        try {
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
                scenario.onActivity { activity ->
                    host.startListening()
                    widget = host.createView(context, widgetId, manager.getAppWidgetInfo(widgetId))
                    activity.addContentView(widget, ViewGroup.LayoutParams(600, 600))
                }
                NotesWidget().update(context, glanceId)
                var lastTree = ""
                waitFor(15_000) {
                    var ready = false
                    instrumentation.runOnMainSync {
                        val tree = widget?.let { descendants(it).joinToString(" | ") { view -> view.javaClass.simpleName + if (view is TextView) ":" + view.text else "" } } ?: "missing host"
                        if (tree != lastTree) { android.util.Log.i("WidgetSyncTest", tree); lastTree = tree }
                        ready = widget?.let { descendants(it).any { v -> v is CompoundButton } } == true
                    }
                    ready
                }
                instrumentation.runOnMainSync {
                    val check = descendants(requireNotNull(widget)).filterIsInstance<CompoundButton>().first()
                    assertFalse(check.isChecked)
                    check.performClick()
                }
                waitFor(10_000) { repository.getNoteById(id)?.content?.startsWith("- [x]") == true }
                compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction() and hasText("- [x] Task\nOriginal")).fetchSemanticsNodes().size == 1 }

                // Glance compositions expire after inactivity. A Room Flow alone stops working then.
                delay(50_000)
                compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("- [x] Task\nWritten in app")
                waitFor(15_000) {
                    var updated = false
                    instrumentation.runOnMainSync {
                        updated = widget?.let { root -> descendants(root).filterIsInstance<TextView>().any { it.text.toString().contains("Written in app") } } == true
                    }
                    updated
                }
                assertEquals("- [x] Task\nWritten in app", repository.getNoteById(id)?.content)
                assertFalse(repository.setCheckbox(id, 0, "- [ ] Stale task", false))
                assertEquals("- [x] Task\nWritten in app", repository.getNoteById(id)?.content)
            }
        } finally {
            host.stopListening()
            host.deleteAppWidgetId(widgetId)
            repository.getNoteById(id)?.let { repository.deleteNote(it) }
        }
    }

    private fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private suspend fun waitFor(timeout: Long, condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeout
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for widget synchronization" }
            delay(200)
        }
    }
}
