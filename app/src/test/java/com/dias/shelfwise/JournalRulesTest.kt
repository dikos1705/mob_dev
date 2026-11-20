package com.dias.shelfwise

import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.domain.*
import org.junit.Assert.*
import org.junit.Test

class JournalRulesTest {
    private val book = Catalog.books.first()
    private val record = ReadingRecord(borrowedAt = 10, pagesRead = 20)
    private val entry = ReadingSession(7, book.id, 10, 20, 15, "A thought", 100)

    @Test fun `session requires a loan remaining pages and bounded minutes`() {
        assertNull(SessionRules.error(book, record, 220, 240, ""))
        assertNotNull(SessionRules.error(book, record.copy(borrowedAt = null), 10, 20, ""))
        assertNotNull(SessionRules.error(book, record, 221, 20, ""))
        assertNotNull(SessionRules.error(book, record, 0, 20, ""))
        assertNotNull(SessionRules.error(book, record, null, 20, ""))
        assertNotNull(SessionRules.error(book, record, 10, 0, ""))
        assertNotNull(SessionRules.error(book, record, 10, 241, ""))
        assertNotNull(SessionRules.error(book, record, 10, 20, "x".repeat(301)))
        assertNotNull(SessionRules.error(book, record.copy(pagesRead = 240), 1, 20, ""))
    }

    @Test fun `undo is restricted to latest active entry with matching progress`() {
        assertNull(SessionRules.undoError(entry, 7, 20))
        assertNotNull(SessionRules.undoError(entry.copy(undoneAt = 200), 7, 20))
        assertNotNull(SessionRules.undoError(entry, 8, 20))
        assertNotNull(SessionRules.undoError(entry, 7, 21))
        assertNotNull(SessionRules.undoError(entry, null, 20))
    }

    @Test fun `book forms validate required fields and page boundaries`() {
        val valid = BookDraft("New book", "Author", "Learning", "240", BookFormat.PRINT, "Description")
        assertTrue(valid.errors().isEmpty())
        assertEquals(5, BookDraft().errors().size)
        assertTrue(valid.copy(pages = "0").errors().containsKey("pages"))
        assertTrue(valid.copy(pages = "10001").errors().containsKey("pages"))
        assertTrue(valid.copy(title = " ").errors().containsKey("title"))
        assertTrue(valid.copy(author = "x".repeat(81)).errors().containsKey("author"))
        assertTrue(valid.copy(category = "x".repeat(41)).errors().containsKey("category"))
        assertTrue(valid.copy(description = "x".repeat(2001)).errors().containsKey("description"))
        assertTrue(valid.copy(pages = "1").errors().isEmpty())
        assertTrue(valid.copy(pages = "10000").errors().isEmpty())
        assertEquals(book.title, BookDraft.from(book).title)
    }
}
