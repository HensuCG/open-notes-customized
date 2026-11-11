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

package com.opennotes.notes.presentation.addEditNote.components.markdown

interface MarkdownLineProcessor {
    fun canProcessLine(line: String): Boolean

    fun processLine(
        line: String,
        builder: MarkdownBuilder,
    )
}

class CodeBlockProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean = line.startsWith("```")

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        val language = line.removePrefix("```").trim().takeIf { it.isNotEmpty() }
        val codeBlock = StringBuilder()
        var index = builder.lineIndex + 1
        var isEnded = false

        while (index < builder.lines.size) {
            val nextLine = builder.lines[index]
            if (nextLine == "```") {
                builder.lineIndex = index
                isEnded = true
                break
            }
            codeBlock.appendLine(nextLine)
            index++
        }

        builder.add(CodeBlock(codeBlock.toString(), isEnded, line, language))
    }
}

class CheckboxProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean = line.matches(Regex("^\\s*(?:[-*+]\\s+)?\\[[ xX]](?:\\s.*)?$"))

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        val checked = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])]").find(line)?.groupValues?.get(1) != " "
        val text = line.replace(Regex("^\\s*(?:[-*+]\\s+)?\\[[ xX]] ?"), "").trim()
        builder.add(CheckboxItem(text, checked, builder.lineIndex))
    }
}

class HeadingProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean = line.startsWith("#")

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        val level = line.takeWhile { it == '#' }.length
        val text = line.drop(level).trim()
        builder.add(Heading(level, text))
    }
}

class QuoteProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean = line.trim().startsWith(">")

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        val level = line.takeWhile { it == '>' }.length
        val text = line.drop(level).trim()
        builder.add(Quote(level, text))
    }
}

class ListItemProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("- ") ||
            trimmed.startsWith("+ ") ||
            trimmed.startsWith("* ") ||
            trimmed == "-" ||
            trimmed == "+" ||
            trimmed == "*" ||
            trimmed.matches(Regex("^\\d+\\. .*")) ||
            trimmed.matches(Regex("^\\d+\\.$"))
    }

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        val trimmed = line.trim()
        val text =
            when {
                trimmed.startsWith("- ") -> trimmed.removePrefix("- ").trim()
                trimmed.startsWith("+ ") -> trimmed.removePrefix("+ ").trim()
                trimmed.startsWith("* ") -> trimmed.removePrefix("* ").trim()
                trimmed == "-" || trimmed == "+" || trimmed == "*" -> ""
                trimmed.matches(Regex("^\\d+\\. .*")) -> {
                    trimmed.substringAfter(". ").trim()
                }
                trimmed.matches(Regex("^\\d+\\.$")) -> ""

                else -> trimmed
            }

        val isNumbered = trimmed.matches(Regex("^\\d+\\. .*")) || trimmed.matches(Regex("^\\d+\\.$"))
        val number =
            if (isNumbered) {
                trimmed.substringBefore(".").toIntOrNull() ?: 1
            } else {
                null
            }

        builder.add(ListItem(text, isNumbered, number))
    }
}

class ImageInsertionProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean = line.trim().startsWith("!(") && line.trim().endsWith(")")

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        val photoUri = line.substringAfter("!(", "").substringBefore(")")
        builder.add(ImageInsertion(photoUri))
    }
}

fun String.stripMarkdown(): String {
    val lines = this.lines()
    val result = StringBuilder()
    var inCodeBlock = false

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            inCodeBlock = !inCodeBlock
            continue
        }
        if (inCodeBlock) {
            result.appendLine(line)
            continue
        }
        if (line.trim() == "---" || line.trim().startsWith("!(")) {
            continue
        }

        var processedLine = line

        val trimmedLine = processedLine.trim()
        when {
            trimmedLine.startsWith("#") -> {
                processedLine = trimmedLine.dropWhile { it == '#' }.trim()
            }
            trimmedLine.startsWith(">") -> {
                processedLine = processedLine.dropWhile { it == '>' }.trim()
            }
            trimmedLine.matches(Regex("^\\s*(?:[-*+]\\s+)?\\[[ xX]](?:\\s.*)?$")) -> {
                val isChecked = processedLine.contains(Regex("\\[[Xx]]"))
                val text = trimmedLine.replace(Regex("^\\s*(?:[-*+]\\s+)?\\[[ xX]] ?"), "").trim()
                processedLine = (if (isChecked) "[✓] " else "[ ] ") + text
            }
            trimmedLine.startsWith("- ") ||
                trimmedLine.startsWith("+ ") ||
                trimmedLine.startsWith("* ") ||
                trimmedLine == "-" ||
                trimmedLine == "+" ||
                trimmedLine == "*" -> {
                processedLine = "• " + if (trimmedLine.length > 2) trimmedLine.drop(2).trim() else ""
            }
            processedLine.matches(Regex("^\\d+\\. .*")) || processedLine.matches(Regex("^\\d+\\.$")) -> {
                processedLine = if (processedLine.contains(". ")) processedLine.substringAfter(". ") else ""
            }
        }

        // Apply inline markdown stripping to the processed line
        processedLine =
            processedLine
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                .replace(Regex("\\*(.+?)\\*"), "$1")
                .replace(Regex("~~(.+?)~~"), "$1")
                .replace(Regex("`(.+?)`"), "$1")
                .replace(Regex("\\[(.+?)\\]\\(.*?\\)"), "$1")

        result.appendLine(processedLine)
    }

    return result.toString().trim()
}

class LinkProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean {
        // Simple, fast check for URLs without regex
        return line.contains("http://") || line.contains("https://")
    }

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        try {
            // Simple URL detection without regex to prevent freezing
            val urlRanges = mutableListOf<Pair<String, IntRange>>()
            var searchIndex = 0
            var urlCount = 0
            val maxUrls = 3 // Limit URLs per line to prevent performance issues

            while (urlCount < maxUrls && searchIndex < line.length) {
                val httpIndex = line.indexOf("http://", searchIndex)
                val httpsIndex = line.indexOf("https://", searchIndex)

                val startIndex =
                    when {
                        httpIndex != -1 && httpsIndex != -1 -> minOf(httpIndex, httpsIndex)
                        httpIndex != -1 -> httpIndex
                        httpsIndex != -1 -> httpsIndex
                        else -> -1
                    }

                if (startIndex == -1) break

                // Find end of URL - stop at first space, newline, or end of string
                var endIndex = startIndex
                while (endIndex < line.length &&
                    line[endIndex] != ' ' &&
                    line[endIndex] != '\n' &&
                    line[endIndex] != '\t' &&
                    line[endIndex] != ')' &&
                    line[endIndex] != ']'
                ) {
                    endIndex++
                }

                // Extract URL - limit length to prevent issues
                val url = line.substring(startIndex, minOf(endIndex, startIndex + 200))
                if (url.length > 10) { // Only include reasonable URLs
                    urlRanges.add(Pair(url, startIndex until endIndex))
                    urlCount++
                }

                searchIndex = endIndex
            }

            if (urlRanges.isNotEmpty()) {
                builder.add(Link(line, urlRanges))
            } else {
                builder.add(NormalText(line))
            }
        } catch (e: Exception) {
            // Always fallback to normal text if there's any issue
            builder.add(NormalText(line))
        }
    }
}

class HorizontalRuleProcessor : MarkdownLineProcessor {
    override fun canProcessLine(line: String): Boolean = line.trim() == "---"

    override fun processLine(
        line: String,
        builder: MarkdownBuilder,
    ) {
        builder.add(HorizontalRule(line))
    }
}
