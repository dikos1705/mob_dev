package com.dias.shelfwise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.domain.UserLibrary

@Composable
fun DiscoverScreen(library: UserLibrary, onOpen: (String) -> Unit, onSave: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val filtered = remember(query, category) {
        Catalog.books.filter { book ->
            (category == "All" || book.category == category) &&
                listOf(book.title, book.author, book.category).any { it.contains(query.trim(), ignoreCase = true) }
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("discover-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "intro") {
            Eyebrow("Your campus reading companion")
            Text("Make room for\na good book.", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 10.dp))
        }
        if (query.isBlank() && category == "All") item(key = "featured") {
            val book = Catalog.books.first()
            Row(Modifier.fillMaxWidth().testTag("featured-${book.id}").clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer).clickable { onOpen(book.id) }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("This week's starting point")
                    Spacer(Modifier.height(10.dp))
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Clear ideas. Small steps.\nSomething worth learning.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Text("Explore the book →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(14.dp)); BookCover(book, 88.dp)
            }
        }
        item(key = "search") {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("catalog-search"),
                placeholder = { Text("Find a title, author, or subject") }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Clear search") } },
                shape = RoundedCornerShape(14.dp))
        }
        item(key = "categories") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Catalog.categories) { name ->
                    FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
                }
            }
        }
        item(key = "collection-heading") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Browse the collection", style = MaterialTheme.typography.titleLarge)
                Text("${filtered.size} books", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (filtered.isEmpty()) item(key = "empty-results") {
            EmptyState("No books found", "Try another title or a different subject.", "Clear filters", { query = ""; category = "All" })
        }
        items(filtered, key = { it.id }) { book ->
            BookCard(book, library.record(book.id), { onOpen(book.id) }, { onSave(book.id) })
        }
    }
}
