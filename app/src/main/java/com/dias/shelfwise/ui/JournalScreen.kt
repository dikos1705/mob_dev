package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dias.shelfwise.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalScreen(books: List<Book>, library: UserLibrary, sessions: List<ReadingSession>, bookId: String?,
    saving: Boolean, onUndo: (Long) -> Unit, onOpen: (String) -> Unit, onDiscover: () -> Unit) {
    val entries = sessions.filter { bookId == null || it.bookId == bookId }
    val active = entries.filter { it.undoneAt == null && !it.isAdjustment }
    val latest = sessions.filter { it.undoneAt == null }.groupBy { it.bookId }
        .mapValues { (_, values) -> values.maxOf { it.id } }
    var undoId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(Modifier.fillMaxSize().testTag("journal-list"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "heading") {
            Eyebrow("Your pages have a story")
            Text(if (bookId == null) "The reading journal." else "One book. Every step.",
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp))
            Text("Sessions and progress adjustments are saved locally. Undo keeps the history.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        item(key = "summary") {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Recorded, not rushed")
                    Text("${active.sumOf { it.pages }} pages · ${active.sumOf { it.minutes }} min",
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("journal-summary"))
                    Text("${active.size} active ${if (active.size == 1) "session" else "sessions"} · ${entries.count { it.undoneAt != null }} undone entries",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (entries.isEmpty()) item(key = "empty") {
            EmptyState("Your first session is waiting", "Borrow a book, open its details and record some reading.", "Explore books", onDiscover)
        }
        items(entries, key = { it.id }) { entry ->
            val book = books.find { it.id == entry.bookId }
            val error = SessionRules.undoError(entry, latest[entry.bookId], library.record(entry.bookId).pagesRead)
            OutlinedCard(Modifier.fillMaxWidth().testTag("session-${entry.id}")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow(if (entry.undoneAt != null) "Undone · history kept" else if (entry.isAdjustment) "Progress adjustment" else "Reading session · recorded")
                    Text(book?.title ?: "Book removed", style = MaterialTheme.typography.titleMedium)
                    Text("Page ${entry.fromPage} → ${entry.toPage}" + if (entry.minutes > 0) " · ${entry.minutes} minutes" else "",
                        style = MaterialTheme.typography.bodyMedium)
                    if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall)
                    Text(Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, HH:mm")), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { undoId = entry.id }, enabled = error == null && !saving,
                            modifier = Modifier.testTag("undo-${entry.id}")) { Text(if (entry.undoneAt != null) "Undone" else "Undo entry") }
                        if (book != null) TextButton(onClick = { onOpen(book.id) }) { Text("Open book") }
                    }
                    if (error != null && entry.undoneAt == null) Text("Undo the newest active entry for this book first.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    val chosen = entries.find { it.id == undoId }
    if (chosen != null) AlertDialog(onDismissRequest = { undoId = null }, title = { Text("Undo this entry?") },
        text = { Text("Progress returns to page ${chosen.fromPage}. The entry stays in history as undone, and its minutes no longer count toward statistics.") },
        confirmButton = { TextButton(onClick = { undoId = null; onUndo(chosen.id) },
            modifier = Modifier.testTag("confirm-undo")) { Text("Undo entry") } },
        dismissButton = { TextButton(onClick = { undoId = null }) { Text("Keep entry") } })
}
