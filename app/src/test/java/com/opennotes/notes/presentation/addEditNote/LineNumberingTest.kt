package com.opennotes.notes.presentation.addEditNote

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class LineNumberingTest {
    private fun number(text: String) = LineNumbering.renumber(TextFieldValue(text)).text

    @Test
    fun replacesOldNumbersAndNumbersUnnumberedLines() {
        assertEquals("1. First\n2. Second\n3. Third", number("9. First\nSecond\n42. Third"))
    }

    @Test
    fun includesBlankAndTrailingLines() {
        assertEquals("1. First\n2. \n3. Last\n4. ", number("First\n\nLast\n"))
        assertEquals("1. ", number(""))
    }

    @Test
    fun preservesDecimalsDatesAndMarkdownBody() {
        assertEquals("1. 3.14\n2. 2026-11-03\n3. **Bold**\n4.   indented", number("3.14\n2026-11-03\n**Bold**\n  indented"))
    }

    @Test
    fun handlesLargeNumbersAndWhitespaceWithoutParsingIntegers() {
        assertEquals("1. First\n2. Second\n3. ", number("  999999999999999999999.\tFirst\n2. Second\n9."))
    }

    @Test
    fun isIdempotentAndRetainsSelectionAndComposition() {
        val value = TextFieldValue("1. First\n2. Second", TextRange(5), TextRange(4, 6))
        assertEquals(value, LineNumbering.renumber(value))
    }

    @Test
    fun keepsCaretAtTheSameBodyPositionAfterRenumbering() {
        val value = TextFieldValue("99. First\n8. Second", TextRange(15))
        val result = LineNumbering.renumber(value)
        assertEquals("1. First\n2. Second", result.text)
        assertEquals(TextRange(14), result.selection)
    }

    @Test
    fun mapsReversedSelectionsAcrossLines() {
        val result = LineNumbering.renumber(TextFieldValue("First\nSecond", TextRange(12, 0)))
        assertEquals(TextRange(18, 3), result.selection)
    }

    @Test
    fun mapsImeCompositionWhenPrefixIsAdded() {
        val result = LineNumbering.renumber(TextFieldValue("hello", TextRange(5), TextRange(0, 5)))
        assertEquals(TextRange(8), result.selection)
        assertEquals(TextRange(3, 8), result.composition)
    }

    @Test
    fun renumbersAfterInsertionDeletionAndPaste() {
        assertEquals("1. First\n2. Inserted\n3. Last", number("1. First\nInserted\n2. Last"))
        assertEquals("1. First\n2. Last", number("1. First\n3. Last"))
        assertEquals("1. First\n2. Pasted\n3. More", number("1. First\n20. Pasted\n40. More"))
    }

    @Test
    fun keepsCaretAfterNewlineAndHandlesNumberWidthChanges() {
        val result = LineNumbering.renumber(TextFieldValue("1. First\n", TextRange(9)))
        assertEquals(TextRange(12), result.selection)
        val lines = (1..11).joinToString("\n") { "99. Item $it" }
        val numbered = number(lines)
        assertEquals("10. Item 10", numbered.lines()[9])
        assertEquals(numbered, number(numbered))
    }

    @Test
    fun preservesWindowsAndLegacyLineEndings() {
        assertEquals("1. First\r\n2. \r3. Last", number("9. First\r\n2.\rLast"))
    }
}
