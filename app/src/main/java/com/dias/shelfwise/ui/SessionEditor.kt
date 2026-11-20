package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dias.shelfwise.domain.*

@Composable
fun SessionEditor(book: Book, record: ReadingRecord, saving: Boolean,
    onConfirm: (Int, Int, String) -> Unit, onDismiss: () -> Unit) {
    var pages by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val error = SessionRules.error(book, record, pages.toIntOrNull(), minutes.toIntOrNull(), note)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("A little reading, recorded.") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                Text("At page ${record.pagesRead} of ${book.pages}. ${book.pages - record.pagesRead} pages left.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(pages, { pages = it.filter(Char::isDigit).take(5) },
                    Modifier.fillMaxWidth().testTag("session-pages"), label = { Text("Pages read") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) },
                    Modifier.fillMaxWidth().testTag("session-minutes"), label = { Text("Minutes spent") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth().testTag("session-note"),
                    label = { Text("A thought (optional)") }, minLines = 2, maxLines = 3)
                Text(error ?: "Progress and journal will be saved together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pages.toInt(), minutes.toInt(), note) },
            enabled = error == null && !saving, modifier = Modifier.testTag("confirm-session")) { Text("Save session") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
