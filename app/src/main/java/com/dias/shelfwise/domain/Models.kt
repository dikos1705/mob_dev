package com.dias.shelfwise.domain

enum class BookFormat(val label: String, val loanDays: Int) { PRINT("Print", 14), DIGITAL("Digital", 7) }

data class Book(
    val id: String, val title: String, val author: String, val category: String,
    val pages: Int, val format: BookFormat, val description: String,
    val coverColor: Long, val coverMark: String
)

data class ReadingRecord(
    val saved: Boolean = false,
    val borrowedAt: Long? = null,
    val pagesRead: Int = 0,
    val note: String = ""
)

data class UserLibrary(val records: Map<String, ReadingRecord> = emptyMap(), val goalPages: Int = 300) {
    fun record(id: String) = records[id] ?: ReadingRecord()
    val activeLoanCount: Int get() = records.values.count { it.borrowedAt != null }
}

data class LibraryChange(val library: UserLibrary, val message: String)

object LibraryActions {
    const val MAX_LOANS = 3
    fun borrow(library: UserLibrary, book: Book, now: Long): LibraryChange {
        val record = library.record(book.id)
        if (record.borrowedAt != null) return LibraryChange(library, "This book is already on your shelf.")
        if (library.activeLoanCount >= MAX_LOANS) return LibraryChange(library, "Your shelf holds 3 loans. Return a book first.")
        return update(library, book.id, record.copy(borrowedAt = now), "Book added to your reading shelf.")
    }
    fun returnBook(library: UserLibrary, book: Book): LibraryChange {
        val record = library.record(book.id)
        if (record.borrowedAt == null) return LibraryChange(library, "This book is not on loan.")
        return update(library, book.id, record.copy(borrowedAt = null), "Book returned. Your notes and progress are kept.")
    }
    fun toggleSaved(library: UserLibrary, book: Book): LibraryChange {
        val record = library.record(book.id)
        return update(library, book.id, record.copy(saved = !record.saved), if (record.saved) "Removed from saved books." else "Saved for later.")
    }
    fun setPages(library: UserLibrary, book: Book, pages: Int): LibraryChange {
        val record = library.record(book.id)
        if (record.borrowedAt == null) return LibraryChange(library, "Borrow this book to track your reading.")
        return update(library, book.id, record.copy(pagesRead = pages.coerceIn(0, book.pages)), "Reading progress updated.")
    }
    fun saveNote(library: UserLibrary, book: Book, note: String): LibraryChange =
        update(library, book.id, library.record(book.id).copy(note = note.trim().take(1000)), "Reading note saved.")
    fun setGoal(library: UserLibrary, pages: Int): LibraryChange =
        if (pages in 50..5000) LibraryChange(library.copy(goalPages = pages), "Reading goal updated.")
        else LibraryChange(library, "Choose a goal from 50 to 5000 pages.")
    private fun update(library: UserLibrary, id: String, record: ReadingRecord, message: String) =
        LibraryChange(library.copy(records = library.records + (id to record)), message)
}
