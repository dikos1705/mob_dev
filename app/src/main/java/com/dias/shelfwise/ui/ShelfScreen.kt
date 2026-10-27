package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.domain.UserLibrary

@Composable
fun ShelfScreen(library: UserLibrary, onOpen: (String) -> Unit, onSave: (String) -> Unit, onDiscover: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Reading") }
    val books = Catalog.books.filter { book ->
        val record = library.record(book.id)
        when (filter) {
            "Saved" -> record.saved
            "Finished" -> record.pagesRead >= book.pages
            else -> record.borrowedAt != null && record.pagesRead < book.pages
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Eyebrow("A place for your next chapter"); Text("Your reading shelf", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp)) }
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("A little space to focus", style = MaterialTheme.typography.titleMedium)
                        Text("Keep up to 3 books on loan.\nYour notes stay when you return them.", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${library.activeLoanCount}/3", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Reading", "Saved", "Finished")) { value ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
                }
            }
        }
        if (books.isEmpty()) item {
            EmptyState(if (filter == "Reading") "Your next book is waiting" else if (filter == "Saved") "Keep a few possibilities" else "Every page adds up",
                if (filter == "Reading") "Borrow a book from Discover to begin." else if (filter == "Saved") "Tap a bookmark to save a book for later." else "Finished books will appear here.",
                "Explore books", onDiscover)
        }
        items(books, key = { it.id }) { book ->
            BookCard(book, library.record(book.id), { onOpen(book.id) }, { onSave(book.id) })
        }
    }
}
