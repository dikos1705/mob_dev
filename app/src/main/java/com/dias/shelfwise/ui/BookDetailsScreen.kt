package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.dias.shelfwise.domain.Book
import com.dias.shelfwise.domain.ReadingRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun BookDetailsScreen(book: Book, record: ReadingRecord, onBorrow: () -> Unit, onReturn: () -> Unit,
    onSave: () -> Unit, onPages: (Int) -> Unit, onAddPages: () -> Unit, onSaveNote: (String) -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit, onLogSession: (Int, Int, String) -> Unit,
    onJournal: () -> Unit, saving: Boolean) {
    var confirmingDelete by rememberSaveable(book.id) { mutableStateOf(false) }
    var loggingSession by rememberSaveable(book.id) { mutableStateOf(false) }
    var note by rememberSaveable(book.id) { mutableStateOf(record.note) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var draftPages by rememberSaveable(book.id, record.pagesRead) { mutableFloatStateOf(record.pagesRead.toFloat()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row {
            BookCover(book, 120.dp)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(book.category)
                Text(book.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp))
                Text(book.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("${book.pages} pages · ${book.format.label}", style = MaterialTheme.typography.labelMedium)
                Text("${book.format.loanDays}-day loan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = if (record.borrowedAt == null) onBorrow else onReturn,
                modifier = Modifier.weight(1f).height(50.dp).testTag("borrow-button"), shape = RoundedCornerShape(12.dp)) {
                Text(if (record.borrowedAt == null) "Borrow book" else "Return book")
            }
            SaveBookButton(book, record.saved, onSave)
        }
        record.borrowedAt?.let { borrowed ->
            val due = Instant.ofEpochMilli(borrowed).atZone(ZoneId.systemDefault()).plusDays(book.format.loanDays.toLong())
            Text("On your shelf · return by ${due.format(DateTimeFormatter.ofPattern("MMM d"))}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("One session at a time")
                Text("Make your pages count.", style = MaterialTheme.typography.titleLarge)
                Text("Record pages, time and a thought. Review your history or undo the latest entry.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { loggingSession = true }, enabled = !saving && record.borrowedAt != null && record.pagesRead < book.pages,
                    modifier = Modifier.fillMaxWidth().testTag("log-session")) { Text("Record a reading session") }
                if (record.borrowedAt == null) Text("Borrow this book to record a session.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onJournal, modifier = Modifier.testTag("book-journal")) { Text("View this book's journal") }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Inside this book", style = MaterialTheme.typography.titleLarge)
            Text(book.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (record.borrowedAt != null || record.pagesRead > 0) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your reading progress", style = MaterialTheme.typography.titleMedium)
                    Text("${record.pagesRead} of ${book.pages} pages", Modifier.testTag("reading-count"), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = draftPages, onValueChange = { draftPages = it }, valueRange = 0f..book.pages.toFloat(),
                        onValueChangeFinished = { onPages(draftPages.roundToInt()) }, enabled = record.borrowedAt != null,
                        modifier = Modifier.testTag("reading-slider"))
                    OutlinedButton(onClick = onAddPages, enabled = record.borrowedAt != null && record.pagesRead < book.pages,
                        modifier = Modifier.testTag("progress-add")) { Text(if (record.pagesRead >= book.pages) "Finished" else "Read 10 pages") }
                    if (record.borrowedAt == null) Text("Borrow again to continue reading.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("A note to remember", style = MaterialTheme.typography.titleLarge)
            Text("Capture an idea or a question for later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(note, { note = it.take(1000) }, Modifier.fillMaxWidth().testTag("reading-note"),
                placeholder = { Text("What stayed with you?") }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(12.dp),
                supportingText = { Text("${note.length}/1000") })
            OutlinedButton(onClick = { onSaveNote(note); focus.clearFocus(); keyboard?.hide() }, modifier = Modifier.testTag("save-note")) { Text("Save note") }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onEdit, enabled = !saving, modifier = Modifier.testTag("edit-book")) { Text("Edit book") }
            TextButton(onClick = { confirmingDelete = true }, enabled = !saving,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("delete-book")) { Text("Delete book") }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (loggingSession) SessionEditor(book, record, saving,
        onConfirm = { pages, minutes, sessionNote -> onLogSession(pages, minutes, sessionNote); loggingSession = false },
        onDismiss = { loggingSession = false })
    if (confirmingDelete) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text("Delete ${book.title}?") },
        text = { Text("The book, its saved status, loan, notes, progress and journal entries will be removed from this device. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirmingDelete = false; onDelete() }, modifier = Modifier.testTag("confirm-delete")) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } })
}
