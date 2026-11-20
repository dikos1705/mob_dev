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
fun BookEditorScreen(book: Book?, saving: Boolean, onSave: (BookDraft) -> Unit) {
    var title by rememberSaveable(book?.id) { mutableStateOf(book?.title.orEmpty()) }
    var author by rememberSaveable(book?.id) { mutableStateOf(book?.author.orEmpty()) }
    var category by rememberSaveable(book?.id) { mutableStateOf(book?.category.orEmpty()) }
    var pages by rememberSaveable(book?.id) { mutableStateOf(book?.pages?.toString().orEmpty()) }
    var format by rememberSaveable(book?.id) { mutableStateOf(book?.format?.name ?: BookFormat.PRINT.name) }
    var description by rememberSaveable(book?.id) { mutableStateOf(book?.description.orEmpty()) }
    var submitted by rememberSaveable(book?.id) { mutableStateOf(false) }
    val draft = BookDraft(title, author, category, pages, BookFormat.valueOf(format), description,
        book?.coverColor ?: 0xFF31594C)
    val errors = draft.errors()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("book-editor"),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Eyebrow(if (book == null) "A new place on your shelf" else "Keep your collection up to date")
        Text(if (book == null) "Add something\nworth reading." else "Make it your own.", style = MaterialTheme.typography.headlineMedium)
        EditorField(title, { title = it.take(80) }, "Title", "title", submitted, errors["title"])
        EditorField(author, { author = it.take(80) }, "Author", "author", submitted, errors["author"])
        EditorField(category, { category = it.take(40) }, "Subject", "category", submitted, errors["category"])
        OutlinedTextField(pages, { pages = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth().testTag("book-pages"),
            label = { Text("Total pages") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = submitted && errors["pages"] != null,
            supportingText = { Text(if (submitted && errors["pages"] != null) errors.getValue("pages") else "1 to 10,000 pages; existing history must still fit.") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BookFormat.entries.forEach { option ->
                FilterChip(selected = format == option.name, onClick = { format = option.name },
                    label = { Text("${option.label} · ${option.loanDays}-day loan") })
            }
        }
        OutlinedTextField(description, { description = it.take(2000) }, Modifier.fillMaxWidth().testTag("book-description"),
            label = { Text("Description") }, minLines = 3, maxLines = 6,
            isError = submitted && errors["description"] != null,
            supportingText = { Text(if (submitted && errors["description"] != null) errors.getValue("description") else "${description.length}/2000") })
        Button(onClick = { submitted = true; if (errors.isEmpty()) onSave(draft) }, enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save-book")) {
            Text(if (saving) "Saving…" else "Save book")
        }
        Text("Changes are stored on this device and appear across your collection, shelf and journal.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EditorField(value: String, onChange: (String) -> Unit, label: String, key: String,
    submitted: Boolean, error: String?) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth().testTag("book-$key"), label = { Text(label) },
        singleLine = true, isError = submitted && error != null,
        supportingText = { if (submitted && error != null) Text(error) })
}
