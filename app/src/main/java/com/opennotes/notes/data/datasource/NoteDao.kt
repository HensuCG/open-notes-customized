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

package com.opennotes.notes.data.datasource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.opennotes.notes.domain.util.Checklist
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(value = "SELECT * FROM Note")
    fun getNotes(): Flow<List<NoteEntity>>

    @Query(value = "SELECT * FROM Note WHERE id=:id")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM Note WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM Note WHERE id=:id")
    fun observeNoteById(id: Int): Flow<NoteEntity?>

    @Transaction
    suspend fun setCheckbox(id: Int, lineIndex: Int, expectedLine: String, checked: Boolean): Boolean {
        val note = getNoteById(id) ?: return false
        val content = Checklist.setChecked(note.content, lineIndex, expectedLine, checked) ?: return false
        insertNote(note.copy(content = content, updatedAt = System.currentTimeMillis()))
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)
}
