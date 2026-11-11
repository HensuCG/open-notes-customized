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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

class MarkdownBuilder(
    internal val lines: List<String>,
    private var lineProcessors: List<MarkdownLineProcessor>,
) {
    var lineIndex = -1

    internal val content = mutableListOf<MarkdownElement>()

    fun add(element: MarkdownElement) {
        content.add(element)
    }

    fun parse() {
        while (hasNextLine()) {
            val line = nextLine()
            val processor = lineProcessors.find { it.canProcessLine(line) }
            if (processor != null) {
                processor.processLine(line, this)
            } else {
                add(NormalText(line))
            }
        }
    }

    private fun hasNextLine(): Boolean = lineIndex + 1 < lines.size

    private fun nextLine(): String {
        lineIndex++
        return lines[lineIndex]
    }
}

/** Shared by the app and widgets so task syntax and source line indices stay identical. */
fun parseMarkdownElements(markdown: String): List<MarkdownElement> {
    val builder = MarkdownBuilder(
        markdown.lines(),
        listOf(CodeBlockProcessor(), CheckboxProcessor(), HeadingProcessor(), ListItemProcessor(), QuoteProcessor(), ImageInsertionProcessor(), LinkProcessor(), HorizontalRuleProcessor()),
    )
    builder.parse()
    return builder.content.toList()
}

/**
 * Splits the input string by the specified delimiter and returns a list of index pairs.
 * Each pair represents the start and end indices of segments between delimiters.
 */
fun splitByDelimiter(
    input: String,
    delimiter: String,
): List<Pair<Int, Int>> {
    val segments = mutableListOf<Pair<Int, Int>>()
    var startIndex = 0
    var delimiterIndex = input.indexOf(delimiter, startIndex)

    while (delimiterIndex != -1) {
        if (startIndex != delimiterIndex) {
            segments.add(Pair(startIndex, delimiterIndex))
        } else {
            segments.add(Pair(startIndex, startIndex))
        }
        startIndex = delimiterIndex + delimiter.length
        delimiterIndex = input.indexOf(delimiter, startIndex)
    }

    if (startIndex < input.length) {
        segments.add(Pair(startIndex, input.length))
    } else if (startIndex == input.length) {
        segments.add(Pair(startIndex, startIndex))
    }

    // Only keep segments that are odd-indexed (i.e., inside delimiters)
    return segments.filterIndexed { index, _ -> index % 2 == 1 }
}

/**
 * Checks if a given index is within any of the provided segments.
 */
fun isInSegments(
    index: Int,
    segments: List<Pair<Int, Int>>,
): Boolean = segments.any { segment -> index in segment.first until segment.second }

fun buildAnnotatedMarkdownString(
    input: String,
    defaultFontWeight: FontWeight = FontWeight.Normal,
): AnnotatedString {
    if (input.isBlank()) return AnnotatedString(input)
    if (input.length > 5000) { // Reduced limit for better performance
        return AnnotatedString(input)
    }

    val textStyleSegments: List<TextStyleSegment> =
        listOf(
            BoldSegment(),
            CodeSegment(),
            ItalicSegment(),
            HighlightSegment(),
            Strikethrough(),
            Underline(),
        )

    return buildAnnotatedString {
        val currentText = input
        val styleRanges = mutableListOf<Triple<Int, Int, SpanStyle>>()

        // Process each formatting type in order with safety limits
        for (segment in textStyleSegments) {
            val delimiter = segment.delimiter
            if (!currentText.contains(delimiter)) continue // Skip if delimiter not present

            var offset = 0
            var iterations = 0
            val maxIterations = 20 // Reduced iterations to prevent freezing

            while (iterations < maxIterations) {
                val startIndex = currentText.indexOf(delimiter, offset)
                if (startIndex == -1) break

                val endDelimiterStart =
                    currentText.indexOf(delimiter, startIndex + delimiter.length)
                if (endDelimiterStart == -1) break

                // Extract the content between delimiters
                val contentStart = startIndex + delimiter.length
                val contentEnd = endDelimiterStart

                if (contentStart < contentEnd && (contentEnd - contentStart) < 500) { // Reduced segment length limit
                    // Add style range for this content
                    styleRanges.add(
                        Triple(
                            startIndex,
                            endDelimiterStart + delimiter.length,
                            segment.getSpanStyle(),
                        ),
                    )
                }

                offset = endDelimiterStart + delimiter.length
                iterations++
            }
        }

        // Sort ranges by start position with size limit
        val sortedRanges = styleRanges.sortedBy { it.first }.take(50) // Reduced limit

        // Remove overlapping ranges (keep the first one)
        val nonOverlappingRanges = mutableListOf<Triple<Int, Int, SpanStyle>>()
        for (range in sortedRanges) {
            val overlaps =
                nonOverlappingRanges.any { existing ->
                    range.first < existing.second && range.second > existing.first
                }
            if (!overlaps) {
                nonOverlappingRanges.add(range)
            }
        }

        // Build the final string by removing delimiters and applying styles
        var processedText = currentText

        // Process ranges in reverse order to maintain correct indices
        for ((start, end, style) in nonOverlappingRanges.sortedByDescending { it.first }) {
            try {
                if (start >= processedText.length || end > processedText.length) continue

                val originalSegment = processedText.substring(start, end)

                // Find delimiter length by checking the segment
                val delimiter =
                    textStyleSegments
                        .find { segment ->
                            originalSegment.startsWith(segment.delimiter) &&
                                originalSegment.endsWith(
                                    segment.delimiter,
                                )
                        }?.delimiter ?: continue

                // Extract content without delimiters
                val content =
                    originalSegment.substring(
                        delimiter.length,
                        originalSegment.length - delimiter.length,
                    )

                // Replace the segment with just the content
                processedText =
                    processedText.substring(0, start) + content + processedText.substring(end)
            } catch (e: Exception) {
                // Skip problematic ranges
                continue
            }
        }

        // Apply default style to entire text
        withStyle(SpanStyle(fontWeight = defaultFontWeight)) {
            append(processedText)
        }

        // Calculate and apply styles to the processed text
        var adjustmentOffset = 0
        for ((originalStart, originalEnd, style) in nonOverlappingRanges.sortedBy { it.first }) {
            try {
                val originalSegment = input.substring(originalStart, originalEnd)

                // Find delimiter
                val delimiter =
                    textStyleSegments
                        .find { segment ->
                            originalSegment.startsWith(segment.delimiter) &&
                                originalSegment.endsWith(
                                    segment.delimiter,
                                )
                        }?.delimiter ?: continue

                val contentLength = originalSegment.length - (2 * delimiter.length)
                if (contentLength > 0) {
                    val adjustedStart = originalStart - adjustmentOffset
                    val adjustedEnd = adjustedStart + contentLength

                    if (adjustedStart >= 0 && adjustedEnd <= processedText.length && adjustedStart < adjustedEnd) {
                        addStyle(style, adjustedStart, adjustedEnd)
                    }

                    // Update adjustment for removed delimiters
                    adjustmentOffset += (2 * delimiter.length)
                }
            } catch (e: Exception) {
                // Skip problematic styles
                continue
            }
        }
    }
}
