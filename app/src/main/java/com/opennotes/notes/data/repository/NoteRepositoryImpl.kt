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

package com.opennotes.notes.data.repository

import com.opennotes.notes.data.datasource.NoteDao
import com.opennotes.notes.data.datasource.toNoteEntity
import com.opennotes.notes.domain.model.Note
import com.opennotes.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val dao: NoteDao,
    private val onNotesChanged: () -> Unit,
) : NoteRepository {
    override fun getAllNotes(): Flow<List<Note>> = dao.getNotes().map { entities -> entities.map { it.toNote() } }

    override suspend fun getNoteById(id: Int): Note? = dao.getNoteById(id)?.toNote()

    override fun observeNoteById(id: Int): Flow<Note?> = dao.observeNoteById(id).map { it?.toNote() }

    override suspend fun setCheckbox(id: Int, lineIndex: Int, expectedLine: String, checked: Boolean): Boolean =
        dao.setCheckbox(id, lineIndex, expectedLine, checked).also { if (it) onNotesChanged() }

    override suspend fun insertNote(note: Note): Int = dao.insertNote(note.toNoteEntity()).toInt().also { onNotesChanged() }

    override suspend fun deleteNote(note: Note) {
        dao.deleteNote(note.toNoteEntity())
        onNotesChanged()
    }

    override fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query).map { entities -> entities.map { it.toNote() } }

    override suspend fun insertNotes(notes: List<Note>) {
        dao.insertAll(notes.map { it.toNoteEntity() })
        onNotesChanged()
    }
}
