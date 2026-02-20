package com.opencode.sshterminal.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TerminalInputBar(
    onSendBytes: (ByteArray) -> Unit,
    onMenuClick: (() -> Unit)? = null,
    onPageScroll: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var lastCommittedText by remember { mutableStateOf("") }
    var ctrlArmed by remember { mutableStateOf(false) }
    var altArmed by remember { mutableStateOf(false) }

    fun sendTypedText(text: String) {
        val payload = buildPayload(text, ctrlArmed, altArmed)
        if (payload.isNotEmpty()) {
            onSendBytes(payload)
            ctrlArmed = false
            altArmed = false
        }
    }

    val flushComposing = {
        val comp = textFieldValue.composition
        if (comp != null) {
            val composingText = textFieldValue.text.substring(comp.start, comp.end)
            if (composingText.isNotEmpty()) sendTypedText(composingText)
        }
    }

    val clearInput = {
        textFieldValue = TextFieldValue()
        lastCommittedText = ""
    }

    val sendEnter = {
        onSendBytes(byteArrayOf('\r'.code.toByte()))
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onMenuClick != null) {
                    KeyChip("\u2630") { onMenuClick() }
                }
                KeyChip("ESC") { onSendBytes(byteArrayOf(0x1B)); ctrlArmed = false; altArmed = false }
                KeyChip("TAB") { onSendBytes(byteArrayOf('\t'.code.toByte())); ctrlArmed = false; altArmed = false }
                ToggleKeyChip("Ctrl", ctrlArmed) { ctrlArmed = !ctrlArmed }
                ToggleKeyChip("Alt", altArmed) { altArmed = !altArmed }
                KeyChip("\u2191") { onSendBytes("\u001B[A".toByteArray(Charsets.UTF_8)) }
                KeyChip("\u2193") { onSendBytes("\u001B[B".toByteArray(Charsets.UTF_8)) }
                KeyChip("\u2190") { onSendBytes("\u001B[D".toByteArray(Charsets.UTF_8)) }
                KeyChip("\u2192") { onSendBytes("\u001B[C".toByteArray(Charsets.UTF_8)) }
                KeyChip("PgUp") { onPageScroll?.invoke(1) }
                KeyChip("PgDn") { onPageScroll?.invoke(-1) }
                KeyChip("\u232B") { onSendBytes(byteArrayOf(0x7F)) }
                KeyChip("^C") { onSendBytes(byteArrayOf(0x03)) }
                KeyChip("^D") { onSendBytes(byteArrayOf(0x04)) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val newCommitted = newValue.committedText()
                        if (newCommitted != lastCommittedText) {
                            when {
                                newCommitted.startsWith(lastCommittedText) -> {
                                    val inserted = newCommitted.substring(lastCommittedText.length)
                                    if (inserted.isNotEmpty()) sendTypedText(inserted)
                                }
                                lastCommittedText.startsWith(newCommitted) -> {
                                    val deleted = lastCommittedText.length - newCommitted.length
                                    repeat(deleted) { onSendBytes(byteArrayOf(0x7F)) }
                                }
                                else -> {
                                    repeat(lastCommittedText.length) { onSendBytes(byteArrayOf(0x7F)) }
                                    if (newCommitted.isNotEmpty()) sendTypedText(newCommitted)
                                }
                            }
                            lastCommittedText = newCommitted
                        }
                        textFieldValue = newValue
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { flushComposing(); sendEnter(); clearInput() },
                        onDone = { flushComposing(); sendEnter(); clearInput() }
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    "...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                KeyChip("\u23CE") { flushComposing(); sendEnter(); clearInput() }
            }
        }
    }
}

@Composable
private fun KeyChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(34.dp)
            .widthIn(min = 36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun ToggleKeyChip(
    label: String,
    armed: Boolean,
    onClick: () -> Unit
) {
    val bg = if (armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (armed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .height(34.dp)
            .widthIn(min = 40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = bg,
        tonalElevation = 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = fg
                )
            )
        }
    }
}

private fun TextFieldValue.committedText(): String {
    val comp = composition ?: return text
    return text.removeRange(comp.start, comp.end)
}

private fun buildPayload(text: String, ctrlArmed: Boolean, altArmed: Boolean): ByteArray {
    if (text.isEmpty() && !ctrlArmed && !altArmed) return ByteArray(0)
    val core = when {
        ctrlArmed && text.length == 1 -> {
            val upper = text[0].uppercaseChar().code
            byteArrayOf(if (upper in 64..95) (upper - 64).toByte() else text[0].code.toByte())
        }
        else -> text.toByteArray(Charsets.UTF_8)
    }
    return if (altArmed) byteArrayOf(0x1B) + core else core
}
