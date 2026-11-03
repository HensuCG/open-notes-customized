/*
 *
 *  *  Copyright (c) 2026 Dhanush Sugganahalli <dhanush41230@gmail.com>
 *  *
 *  *  This program is free software; you can redistribute it and/or modify it under
 *  *  the terms of the GNU General Public License as published by the Free Software
 *  *  Foundation; either version 3 of the License, or (at your option) any later
 *  *  version.
 *  *
 *  *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *  *
 *  *  You should have received a copy of the GNU General Public License along with
 *  *  this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.opennotes.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.opennotes.notes.domain.usecase.NoteUseCases
import com.opennotes.settings.data.repository.DataStoreRepository
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetModelRepositoryEntryPoint {
    fun noteUseCase(): NoteUseCases

    fun settingsRepository(): DataStoreRepository
}

fun getWidgetSettingsRepository(context: Context): DataStoreRepository =
    EntryPoints.get(context.applicationContext, WidgetModelRepositoryEntryPoint::class.java).settingsRepository()

fun getNoteUseCase(context: Context): NoteUseCases {
    val entryPoint =
        EntryPoints.get(
            context.applicationContext,
            WidgetModelRepositoryEntryPoint::class.java,
        )
    return entryPoint.noteUseCase()
}

class NotesWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val noteUseCase = getNoteUseCase(context)
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val settingsRepository = getWidgetSettingsRepository(context)
        val initialSettings = settingsRepository.getSettings()

        provideContent {
            val settings by settingsRepository.getSettingsFlow().collectAsState(initialSettings)
            WidgetTheme(pureBlack = settings.pureBlackWidgets) {
                val prefs = currentState<Preferences>()
                val noteId = prefs[intPreferencesKey("note_$widgetId")]
                val notes by noteUseCase.getNotes().collectAsState(initial = emptyList())
                val selectedNote = notes.firstOrNull { it.id == noteId }

                when {
                    selectedNote == null -> ZeroState(widgetId = widgetId, pureBlack = settings.pureBlackWidgets)
                    else -> SelectedNote(selectedNote, widgetId = widgetId, pureBlack = settings.pureBlackWidgets)
                }
            }
        }
    }
}
