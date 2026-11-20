package com.dias.shelfwise.domain

data class ReadingSession(
    val id: Long, val bookId: String, val fromPage: Int, val toPage: Int,
    val minutes: Int, val note: String, val createdAt: Long, val undoneAt: Long? = null
) {
    val pages: Int get() = toPage - fromPage
    val isAdjustment: Boolean get() = minutes == 0
}

data class LibrarySnapshot(
    val books: List<Book> = emptyList(),
    val library: UserLibrary = UserLibrary(),
    val sessions: List<ReadingSession> = emptyList()
)

data class BookDraft(
    val title: String = "", val author: String = "", val category: String = "",
    val pages: String = "", val format: BookFormat = BookFormat.PRINT,
    val description: String = "", val coverColor: Long = 0xFF31594C
) {
    fun errors(): Map<String, String> = buildMap {
        if (title.trim().length !in 1..80) put("title", "Enter a title, up to 80 characters.")
        if (author.trim().length !in 1..80) put("author", "Enter an author, up to 80 characters.")
        if (category.trim().length !in 1..40) put("category", "Enter a subject, up to 40 characters.")
        if (pages.toIntOrNull() !in 1..10000) put("pages", "Choose 1 to 10,000 pages.")
        if (description.trim().length !in 1..2000) put("description", "Enter a description, up to 2,000 characters.")
    }
    companion object {
        fun from(book: Book) = BookDraft(book.title, book.author, book.category, book.pages.toString(),
            book.format, book.description, book.coverColor)
    }
}

object SessionRules {
    fun error(book: Book, record: ReadingRecord, pages: Int?, minutes: Int?, note: String): String? = when {
        record.borrowedAt == null -> "Borrow this book before recording a session."
        record.pagesRead >= book.pages -> "This book is finished. Undo a session or adjust its progress first."
        pages == null || pages !in 1..(book.pages - record.pagesRead) ->
            "Enter 1 to ${book.pages - record.pagesRead} pages."
        minutes == null || minutes !in 1..240 -> "Choose 1 to 240 minutes."
        note.length > 300 -> "Keep the session note within 300 characters."
        else -> null
    }

    fun undoError(session: ReadingSession, latestActiveId: Long?, currentPage: Int): String? = when {
        session.undoneAt != null -> "This session has already been undone."
        session.id != latestActiveId -> "Undo the latest entry for this book first."
        currentPage != session.toPage -> "Progress changed after this entry; undo the latest adjustment first."
        else -> null
    }
}
