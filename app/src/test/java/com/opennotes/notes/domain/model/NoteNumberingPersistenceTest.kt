package com.opennotes.notes.domain.model

import com.opennotes.notes.data.datasource.toNoteEntity
import com.opennotes.notes.data.repository.KotlinxJsonHandler
import com.opennotes.notes.domain.usecase.noteFromBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NoteNumberingPersistenceTest {
    private val note = Note("Title", "1. First", 1L, 2L, 0, autoNumberLines = true)

    @Test
    fun preservesModeThroughDatabaseMappingAndBackup() {
        assertEquals(note, note.toNoteEntity().toNote())
        assertEquals(note, Json.decodeFromString<Note>(Json.encodeToString(note)))
        val handler = KotlinxJsonHandler()
        val backup = handler.serializeNotes(listOf(note))
        assertEquals(note, noteFromBackup(handler.parseToJsonArray(backup).single().jsonObject))
    }

    @Test
    fun importsOlderBackupsWithNumberingDisabled() {
        val oldBackup = """{"title":"Old","content":"Text","createdAt":1,"updatedAt":2,"color":0}"""
        assertFalse(Json.decodeFromString<Note>(oldBackup).autoNumberLines)
        assertFalse(requireNotNull(noteFromBackup(Json.parseToJsonElement(oldBackup).jsonObject)).autoNumberLines)
    }

    @Test
    fun backupImportPreservesNumberedBlankLines() {
        val numberedNote = note.copy(content = "1. First\n2. \n3. ")
        val handler = KotlinxJsonHandler()
        val backup = handler.serializeNotes(listOf(numberedNote))
        assertEquals(numberedNote, noteFromBackup(handler.parseToJsonArray(backup).single().jsonObject))
    }
}
