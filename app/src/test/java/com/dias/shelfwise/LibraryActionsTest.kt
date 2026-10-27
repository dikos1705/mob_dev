package com.dias.shelfwise

import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.domain.*
import org.junit.Assert.*
import org.junit.Test

class LibraryActionsTest {
    private val book = Catalog.books.first()

    @Test fun `fourth loan is refused without changing the library`() {
        val full = Catalog.books.take(3).fold(UserLibrary()) { library, item -> LibraryActions.borrow(library, item, 100L).library }
        val result = LibraryActions.borrow(full, Catalog.books[3], 200L)
        assertEquals(3, result.library.activeLoanCount)
        assertEquals(full, result.library)
        assertTrue(result.message.contains("Return a book"))
    }
    @Test fun `borrowing twice preserves the original due date`() {
        val library = LibraryActions.borrow(UserLibrary(), book, 100L).library
        assertEquals(library, LibraryActions.borrow(library, book, 200L).library)
        assertEquals(100L, library.record(book.id).borrowedAt)
    }
    @Test fun `return frees a slot and preserves saved flag progress and notes`() {
        val record = ReadingRecord(true, 100L, 42, "Useful chapter")
        val library = UserLibrary(mapOf(book.id to record))
        val returned = LibraryActions.returnBook(library, book).library
        assertEquals(0, returned.activeLoanCount)
        assertEquals(record.copy(borrowedAt = null), returned.record(book.id))
        assertEquals(record, library.record(book.id)) // immutable input
    }
    @Test fun `progress requires an active loan and stays within page bounds`() {
        val empty = UserLibrary()
        assertEquals(empty, LibraryActions.setPages(empty, book, 20).library)
        val loan = LibraryActions.borrow(empty, book, 100L).library
        assertEquals(0, LibraryActions.setPages(loan, book, -5).library.record(book.id).pagesRead)
        assertEquals(book.pages, LibraryActions.setPages(loan, book, 10000).library.record(book.id).pagesRead)
    }
    @Test fun `notes are trimmed and bounded`() {
        val result = LibraryActions.saveNote(UserLibrary(), book, "  hello  ").library
        assertEquals("hello", result.record(book.id).note)
        assertEquals(1000, LibraryActions.saveNote(result, book, "a".repeat(1100)).library.record(book.id).note.length)
    }
    @Test fun `saved status toggles without creating a loan`() {
        val saved = LibraryActions.toggleSaved(UserLibrary(), book).library
        assertTrue(saved.record(book.id).saved)
        assertEquals(0, saved.activeLoanCount)
        assertFalse(LibraryActions.toggleSaved(saved, book).library.record(book.id).saved)
    }
    @Test fun `goals enforce both boundaries`() {
        val library = UserLibrary()
        listOf(-1, 0, 49, 5001).forEach { assertEquals(library, LibraryActions.setGoal(library, it).library) }
        listOf(50, 5000).forEach { assertEquals(it, LibraryActions.setGoal(library, it).library.goalPages) }
    }
}
