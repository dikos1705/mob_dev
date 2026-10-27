package com.dias.shelfwise.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dias.shelfwise.domain.Book
import com.dias.shelfwise.domain.ReadingRecord

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.6.sp)
}

@Composable
fun BookCover(book: Book, width: Dp = 72.dp) {
    Box(Modifier.width(width).height(width * 1.42f).clip(RoundedCornerShape(7.dp)).background(Color(book.coverColor))) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(Color.White.copy(alpha = .15f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("SHELF / WISE", color = Color.White.copy(alpha = .75f), fontSize = (width.value * .075f).sp, letterSpacing = 1.sp)
            Text(book.coverMark, color = Color(0xFFFFF3D9), fontSize = (width.value * .45f).sp, fontFamily = FontFamily.Serif)
            Text(book.category.uppercase(), color = Color.White.copy(alpha = .8f), fontSize = (width.value * .08f).sp, maxLines = 1)
        }
    }
}

@Composable
fun SaveBookButton(book: Book, saved: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = (if (saved) "Unsave " else "Save ") + book.title,
            tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun BookCard(book: Book, record: ReadingRecord, onOpen: () -> Unit, onSave: () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("book-${book.id}").clip(RoundedCornerShape(16.dp)).clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookCover(book)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow(book.category)
                    Spacer(Modifier.height(5.dp))
                    Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(7.dp))
                    Text("${book.format.label} · ${book.pages} pages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SaveBookButton(book, record.saved, onSave)
            }
            if (record.pagesRead > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { record.pagesRead.toFloat() / book.pages }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                Text("${record.pagesRead} / ${book.pages} pages", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onAction) { Text(action) }
    }
}
