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

package com.opennotes.notes.presentation.addEditNote.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue

// Ask the IME to capitalize sentences; leave the user's actual text and casing intact.
private val noteKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
fun TransParentHintTextField(
    text: String,
    hint: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle = TextStyle(),
    singleLine: Boolean = false,
    onFocusChange: (FocusState) -> Unit,
    focusRequester: FocusRequester,
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        singleLine = singleLine,
        keyboardOptions = noteKeyboardOptions,
        textStyle = textStyle,
        cursorBrush = SolidColor(textStyle.color),
        interactionSource = interactionSource,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged(onFocusChange),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = hint,
                        style = textStyle,
                        color = Color.DarkGray,
                    )
                }

                innerTextField()
            }
        },
    )
}

@Composable
fun TransParentHintTextField(
    textFieldValue: TextFieldValue,
    hint: String,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle = TextStyle(),
    singleLine: Boolean = false,
    onFocusChange: (FocusState) -> Unit,
    focusRequester: FocusRequester,
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = textFieldValue,
        onValueChange = onValueChange,
        singleLine = singleLine,
        keyboardOptions = noteKeyboardOptions,
        textStyle = textStyle,
        cursorBrush = SolidColor(textStyle.color),
        interactionSource = interactionSource,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged(onFocusChange),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier,
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = hint,
                        style = textStyle,
                        color = Color.DarkGray,
                    )
                }

                innerTextField()
            }
        },
    )
}
