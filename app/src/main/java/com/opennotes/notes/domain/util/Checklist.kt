package com.opennotes.notes.domain.util

/** Operates on physical lines without changing line endings, indentation, or other text. */
object Checklist {
    private val marker = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])] *(?:.*)$")
    private val lines = Regex("[^\\r\\n]*(?:\\r\\n|\\r|\\n|$)")

    fun setChecked(text: String, lineIndex: Int, expectedLine: String, checked: Boolean): String? {
        val line = lines.findAll(text).elementAtOrNull(lineIndex) ?: return null
        val body = line.value.trimEnd('\r', '\n')
        if (body != expectedLine) return null
        val state = marker.matchEntire(body)?.groups?.get(1) ?: return null
        val offset = line.range.first + state.range.first
        return text.replaceRange(offset, offset + 1, if (checked) "x" else " ")
    }

    /** Merge remote checkbox-only edits into a dirty editor, retaining local text edits. */
    fun merge(base: String, local: String, remote: String): String {
        if (local == base) return remote
        if (remote == base || remote == local) return local
        val before = base.lines()
        val after = remote.lines()
        if (before.size != after.size) return local
        var result = local
        before.forEachIndexed { index, old ->
            val oldState = marker.matchEntire(old)?.groups?.get(1) ?: return@forEachIndexed
            val new = after[index]
            val newState = marker.matchEntire(new)?.groups?.get(1) ?: return@forEachIndexed
            if (old.replaceRange(oldState.range, " ") != new.replaceRange(newState.range, " ")) return@forEachIndexed
            if (oldState.value.equals(newState.value, ignoreCase = true)) return@forEachIndexed
            val candidates = result.lines().withIndex().filter { it.value == old }
            // Identical task labels are resolved by their original position, otherwise only by a unique match.
            val target = candidates.firstOrNull { it.index == index } ?: candidates.singleOrNull() ?: return@forEachIndexed
            result = setChecked(result, target.index, old, newState.value != " ") ?: result
        }
        return result
    }
}
