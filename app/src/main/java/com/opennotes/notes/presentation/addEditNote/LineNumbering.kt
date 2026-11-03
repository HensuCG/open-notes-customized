package com.opennotes.notes.presentation.addEditNote

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Numbers physical lines, replacing existing list numbers without changing body text. */
object LineNumbering {
    private val lineBreak = Regex("\\r\\n|\\r|\\n")
    // Require whitespace (or the end of the line) after the dot to preserve decimals.
    private val numberPrefix = Regex("^[ \\t]*[0-9]+\\.(?:[ \\t]+|$)")

    fun renumber(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val result = StringBuilder()
        val offsets = IntArray(text.length + 1)
        var start = 0
        var number = 1

        while (start <= text.length) {
            val separator = lineBreak.find(text, start)
            val end = separator?.range?.first ?: text.length
            val line = text.substring(start, end)
            val oldPrefixLength = numberPrefix.find(line)?.value?.length ?: 0
            val prefix = "${number++}. "
            val contentStart = result.length + prefix.length
            result.append(prefix).append(line.substring(oldPrefixLength))
            for (offset in start..end) {
                offsets[offset] = contentStart + (offset - start - oldPrefixLength).coerceAtLeast(0)
            }
            if (end == text.length) break
            val newline = separator!!.value
            for (offset in 1 until newline.length) {
                offsets[end + offset] = result.length + offset
            }
            result.append(newline)
            start = end + newline.length
        }

        if (result.toString() == text) return value

        fun mapRange(range: TextRange) = TextRange(offsets[range.start], offsets[range.end])

        return value.copy(
            text = result.toString(),
            selection = mapRange(value.selection),
            composition = value.composition?.let(::mapRange),
        )
    }
}
