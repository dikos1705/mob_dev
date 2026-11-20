package com.dias.shelfwise.data

import androidx.room.withTransaction
import com.dias.shelfwise.domain.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface LibraryRepository {
    val snapshots: Flow<LibrarySnapshot>
    suspend fun initialize()
    suspend fun borrow(id: String): String
    suspend fun returnBook(id: String): String
    suspend fun toggleSaved(id: String): String
    suspend fun setPages(id: String, pages: Int, relative: Boolean = false): String
    suspend fun saveNote(id: String, note: String): String
    suspend fun setGoal(pages: Int): String
    suspend fun saveBook(id: String?, draft: BookDraft): String
    suspend fun deleteBook(id: String): String
    suspend fun logSession(id: String, pages: Int, minutes: Int, note: String): String
    suspend fun undoSession(id: Long): String
}

class RoomLibraryRepository(
    private val database: LibraryDatabase,
    private val readLegacy: () -> UserLibrary = { UserLibrary() },
    private val now: () -> Long = System::currentTimeMillis
) : LibraryRepository {
    private val dao = database.libraryDao()
    override val snapshots: Flow<LibrarySnapshot> = dao.observeLibrary().filterNotNull().map { it.model() }

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (dao.settings() == null) {
                // One-time import from the previous homework. The transaction also records completion.
                val legacy = readLegacy()
                Catalog.books.forEachIndexed { index, book ->
                    dao.saveBook(BookEntity.from(book, index))
                    dao.saveRecord(ReadingRecordEntity.from(book.id, legacy.record(book.id)))
                }
                dao.saveSettings(SettingsEntity(goalPages = legacy.goalPages))
            }
        }
    }

    private suspend fun snapshot() = requireNotNull(dao.readLibrary()) { "Library is still loading. Try again." }.model()
    private fun LibrarySnapshot.book(id: String) = books.find { it.id == id }
        ?: throw IllegalArgumentException("This book no longer exists.")

    private suspend fun mutate(operation: (LibrarySnapshot) -> LibraryChange): String = database.withTransaction {
        val state = snapshot()
        val change = operation(state)
        change.library.records.forEach { (id, record) ->
            if (record != state.library.record(id)) dao.saveRecord(ReadingRecordEntity.from(id, record))
        }
        if (change.library.goalPages != state.library.goalPages)
            dao.saveSettings(SettingsEntity(goalPages = change.library.goalPages))
        change.message
    }

    override suspend fun borrow(id: String) = mutate { LibraryActions.borrow(it.library, it.book(id), now()) }
    override suspend fun returnBook(id: String) = mutate { LibraryActions.returnBook(it.library, it.book(id)) }
    override suspend fun toggleSaved(id: String) = mutate { LibraryActions.toggleSaved(it.library, it.book(id)) }
    override suspend fun saveNote(id: String, note: String) = mutate { LibraryActions.saveNote(it.library, it.book(id), note) }
    override suspend fun setGoal(pages: Int) = mutate { LibraryActions.setGoal(it.library, pages) }

    override suspend fun setPages(id: String, pages: Int, relative: Boolean): String = database.withTransaction {
        val state = snapshot()
        val book = state.book(id)
        val before = state.library.record(id)
        val change = LibraryActions.setPages(state.library, book, if (relative) before.pagesRead + pages else pages)
        val after = change.library.record(id)
        if (before.pagesRead != after.pagesRead) {
            dao.saveRecord(ReadingRecordEntity.from(id, after))
            dao.addSession(ReadingSessionEntity(bookId = id, fromPage = before.pagesRead,
                toPage = after.pagesRead, minutes = 0, note = "Manual progress adjustment", createdAt = now()))
        }
        change.message
    }

    override suspend fun saveBook(id: String?, draft: BookDraft): String = database.withTransaction {
        require(draft.errors().isEmpty()) { draft.errors().values.first() }
        val state = snapshot()
        val existing = id?.let { state.book(it) }
        val pages = draft.pages.toInt()
        val highestRecordedPage = state.sessions.filter { it.bookId == id }
            .maxOfOrNull { maxOf(it.fromPage, it.toPage) } ?: 0
        require(pages >= maxOf(state.library.record(id.orEmpty()).pagesRead, highestRecordedPage)) {
            "Page count cannot be smaller than existing progress or journal history."
        }
        require(existing == null || existing.format == draft.format || state.library.record(existing.id).borrowedAt == null) {
            "Return this book before changing its format."
        }
        val book = Book(existing?.id ?: UUID.randomUUID().toString(), draft.title.trim(), draft.author.trim(),
            draft.category.trim(), pages, draft.format, draft.description.trim(), draft.coverColor,
            draft.title.trim().take(1).uppercase())
        // Use stable, unique ordering even after deleting an earlier catalog entry.
        val stored = dao.readLibrary()!!.books
        val actualPosition = existing?.let { old -> stored.first { it.book.id == old.id }.book.position }
            ?: ((stored.maxOfOrNull { it.book.position } ?: -1) + 1)
        dao.saveBook(BookEntity.from(book, actualPosition))
        book.id
    }

    override suspend fun deleteBook(id: String): String = database.withTransaction {
        require(dao.deleteBook(id) > 0) { "This book was already deleted." }
        "Book deleted with its local notes, progress and journal."
    }

    override suspend fun logSession(id: String, pages: Int, minutes: Int, note: String): String = database.withTransaction {
        val state = snapshot()
        val book = state.book(id)
        val record = state.library.record(id)
        val error = SessionRules.error(book, record, pages, minutes, note)
        require(error == null) { error!! }
        val after = record.copy(pagesRead = record.pagesRead + pages)
        dao.saveRecord(ReadingRecordEntity.from(id, after))
        dao.addSession(ReadingSessionEntity(bookId = id, fromPage = record.pagesRead,
            toPage = after.pagesRead, minutes = minutes, note = note.trim(), createdAt = now()))
        if (after.pagesRead == book.pages) "Book finished! Your session is in the journal."
        else "$pages pages recorded. Journal and progress updated."
    }

    override suspend fun undoSession(id: Long): String = database.withTransaction {
        val entry = dao.session(id) ?: throw IllegalArgumentException("This journal entry no longer exists.")
        val state = snapshot()
        val record = state.library.record(entry.bookId)
        val error = SessionRules.undoError(entry.model(), dao.latestActiveSessionId(entry.bookId), record.pagesRead)
        require(error == null) { error!! }
        check(dao.markUndone(id, now()) == 1)
        dao.saveRecord(ReadingRecordEntity.from(entry.bookId, record.copy(pagesRead = entry.fromPage)))
        "Entry undone. Pages restored; history kept."
    }
}
