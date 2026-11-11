package com.opennotes.widget

import android.content.Context
import androidx.annotation.Keep
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.EntryPoints

@Keep
class ToggleNoteCheckboxAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val noteId = parameters[NoteId] ?: return
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        if (preferences[intPreferencesKey("note_$widgetId")] != noteId) return
        val line = parameters[LineIndex] ?: return
        val expected = parameters[ExpectedLine] ?: return
        val checked = parameters[Checked] ?: return
        val repository = EntryPoints.get(context.applicationContext, WidgetModelRepositoryEntryPoint::class.java).noteRepository()
        repository.setCheckbox(noteId, line, expected, checked)
        // Refresh stale actions too, and every instance displaying this note.
        NotesWidget().updateAll(context)
    }

    companion object {
        val NoteId = ActionParameters.Key<Int>("note_id")
        val LineIndex = ActionParameters.Key<Int>("line_index")
        val ExpectedLine = ActionParameters.Key<String>("expected_line")
        val Checked = ActionParameters.Key<Boolean>("checked")
    }
}
