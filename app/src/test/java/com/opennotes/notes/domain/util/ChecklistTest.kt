package com.opennotes.notes.domain.util

import com.opennotes.notes.presentation.addEditNote.components.markdown.*
import org.junit.Assert.*
import org.junit.Test

class ChecklistTest {
    @Test
    fun changesOnlyTheSelectedMarkerAndPreservesCrLf() {
        val text = "Title\r\n- [ ] Same\r\n- [ ] Same\r\n"
        assertEquals("Title\r\n- [ ] Same\r\n- [x] Same\r\n", Checklist.setChecked(text, 2, "- [ ] Same", true))
        assertNull(Checklist.setChecked(text, 1, "- [ ] Old text", true))
        assertNull(Checklist.setChecked(text, 8, "- [ ] Same", true))
    }

    @Test
    fun supportsBareIndentedAndBulletCheckboxes() {
        for (prefix in listOf("", "- ", "* ", "+ ", "  - ")) {
            val line = "${prefix}[X] Task"
            assertEquals("${prefix}[ ] Task", Checklist.setChecked(line, 0, line, false))
            assertTrue(parseMarkdownElements(line).single() is CheckboxItem)
        }
    }

    @Test
    fun parserKeepsSourceIndicesAndIgnoresTasksInsideCode() {
        val elements = parseMarkdownElements("# Heading\n```\n- [ ] literal\n```\n- [ ] Real\nNew line")
        assertEquals(1, elements.filterIsInstance<CheckboxItem>().size)
        assertEquals(4, elements.filterIsInstance<CheckboxItem>().single().index)
        assertEquals(12, parseMarkdownElements((1..12).joinToString("\n") { "Line $it" }).size)
    }

    @Test
    fun mergesRemoteChecksWithoutDroppingLocalTypingOrLocalChecks() {
        val base = "[ ] Task\nOld text"
        assertEquals("[x] Task\nNew text", Checklist.merge(base, "[ ] Task\nNew text", "[x] Task\nOld text"))
        assertEquals("[x] Task\nOld text", Checklist.merge(base, base, "[x] Task\nOld text"))
        assertEquals("[x] Task\nNew text", Checklist.merge(base, "[x] Task\nNew text", base))
        assertEquals("Inserted\n[x] Task\nOld text", Checklist.merge(base, "Inserted\n[ ] Task\nOld text", "[x] Task\nOld text"))
    }
}
