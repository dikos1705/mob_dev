package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.domain.UserLibrary

@Composable
fun ProgressScreen(library: UserLibrary, onGoal: (Int) -> Unit, onOpen: (String) -> Unit, onSave: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var goalDraft by rememberSaveable { mutableStateOf(library.goalPages.toString()) }
    val read = library.records.values.sumOf { it.pagesRead }
    val completed = Catalog.books.count { library.record(it.id).pagesRead >= it.pages }
    val notes = library.records.values.count { it.note.isNotBlank() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column { Eyebrow("Small steps add up"); Text("Look how far\nyou have read.", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 10.dp)) }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Your reading journey")
                Text("$read / ${library.goalPages}", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("goal-progress"))
                Text("pages toward your personal goal", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { (read.toFloat()/library.goalPages).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { goalDraft = library.goalPages.toString(); editing = true }, modifier = Modifier.testTag("edit-goal")) { Text("Change my goal") }
                if (read >= library.goalPages) Text("Goal reached. Make a little room for the next one!", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Finished books", completed.toString(), Modifier.weight(1f))
            StatTile("Reading notes", notes.toString(), Modifier.weight(1f))
        }
        Text("Your pages, book by book", style = MaterialTheme.typography.titleLarge)
        val started = Catalog.books.filter { library.record(it.id).pagesRead > 0 }
        if (started.isEmpty()) Text("Borrow a book and record a few pages. Your progress will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        started.forEach { book -> BookCard(book, library.record(book.id), { onOpen(book.id) }, { onSave(book.id) }) }
        Text("Progress includes returned books. It stays with your reading journey.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (editing) GoalEditor(goalDraft, { goalDraft = it.filter(Char::isDigit).take(4) },
        { goalDraft.toIntOrNull()?.let(onGoal); editing = false }, { editing = false })
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    OutlinedCard(modifier) { Column(Modifier.padding(18.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    } }
}

// Stateless input component: the screen owns the draft and hoists events.
@Composable
fun GoalEditor(value: String, onValueChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    val number = value.toIntOrNull()
    val valid = number != null && number in 50..5000
    AlertDialog(onDismissRequest = onDismiss, title = { Text("A goal that feels right") },
        text = { OutlinedTextField(value, onValueChange, Modifier.testTag("goal-input"), label = { Text("Pages") },
            supportingText = { Text("Choose 50 to 5000 pages.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = !valid) },
        confirmButton = { TextButton(onSave, enabled = valid) { Text("Save goal") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
