package com.opennotes

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.opennotes.notes.data.datasource.NoteDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteNumberingMigrationTest {
    @Test
    fun preservesExistingNotesAndPersistsNewMode() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "numbering-migration-test.db"
            context.deleteDatabase(name)
            val helper =
                FrameworkSQLiteOpenHelperFactory().create(
                    SupportSQLiteOpenHelper.Configuration
                        .builder(context)
                        .name(name)
                        .callback(
                            object : SupportSQLiteOpenHelper.Callback(6) {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    db.execSQL("CREATE TABLE Note (title TEXT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, color INTEGER NOT NULL, isPinned INTEGER NOT NULL, reminderTime INTEGER, repeatInterval INTEGER, repeatUnit TEXT, id INTEGER, PRIMARY KEY(id))")
                                    db.execSQL("INSERT INTO Note VALUES ('Existing', 'Keep this text', 1, 2, 0, 1, 100, 2, 'DAYS', 7)")
                                }

                                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                            },
                        ).build(),
                )
            helper.writableDatabase
            helper.close()

            val database = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_6_7).build()
            try {
                val note = requireNotNull(database.noteDao.getNoteById(7))
                assertEquals("Keep this text", note.content)
                assertEquals(1L, note.createdAt)
                assertEquals(2L, note.updatedAt)
                assertEquals(100L, note.reminderTime)
                assertTrue(note.isPinned)
                assertFalse(note.autoNumberLines)
                database.noteDao.insertNote(note.copy(autoNumberLines = true))
                assertTrue(requireNotNull(database.noteDao.getNoteById(7)).autoNumberLines)
            } finally {
                database.close()
                context.deleteDatabase(name)
            }
        }
}
