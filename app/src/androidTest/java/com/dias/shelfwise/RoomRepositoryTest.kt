package com.dias.shelfwise

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dias.shelfwise.data.*
import com.dias.shelfwise.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var database: LibraryDatabase
    private lateinit var repo: RoomLibraryRepository

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        repo = RoomLibraryRepository(database)
        repo.initialize()
    }
    @After fun close() { database.close() }
    private suspend fun snapshot() = repo.snapshots.first()
    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Expected a validation failure") } catch (_: IllegalArgumentException) { }
    }

    @Test fun a_catalogCrudAndCascadeAreRealDatabaseOperations() = runBlocking {
        val id = repo.saveBook(null, BookDraft("A new book", "Author", "Learning", "100", description = "Local book."))
        assertEquals(9, snapshot().books.size)
        repo.borrow(id)
        repo.toggleSaved(id)
        repo.logSession(id, 20, 15, "A useful thought")
        repo.saveNote(id, "A note")
        repo.saveBook(id, BookDraft("Edited book", "Author", "Learning", "120", description = "Updated."))
        assertEquals("Edited book", snapshot().books.first { it.id == id }.title)
        assertEquals(20, snapshot().library.record(id).pagesRead)
        rejected { repo.saveBook(id, BookDraft("Short", "Author", "Learning", "10", description = "Too short.")) }
        rejected { repo.saveBook(id, BookDraft("Digital", "Author", "Learning", "120", BookFormat.DIGITAL, "Cannot change on loan.")) }
        repo.deleteBook(id)
        val after = snapshot()
        assertEquals(8, after.books.size)
        assertFalse(after.library.records.containsKey(id))
        assertTrue(after.sessions.none { it.bookId == id })
        rejected { repo.borrow(id) }
    }

    @Test fun b_journalUpdatesAtomicallyAndUndoHonorsOrder() = runBlocking {
        rejected { repo.logSession("kotlin", 10, 20, "No loan") }
        repo.borrow("kotlin")
        repo.logSession("kotlin", 30, 20, "First")
        val first = snapshot().sessions.first().id
        repo.logSession("kotlin", 10, 15, "Second")
        val second = snapshot().sessions.first().id
        assertEquals(40, snapshot().library.record("kotlin").pagesRead)
        rejected { repo.undoSession(first) }
        repo.undoSession(second)
        assertEquals(30, snapshot().library.record("kotlin").pagesRead)
        assertNotNull(snapshot().sessions.first { it.id == second }.undoneAt)
        rejected { repo.undoSession(second) }
        repo.setPages("kotlin", 50)
        rejected { repo.undoSession(first) }
        val adjustment = snapshot().sessions.first().id
        repo.undoSession(adjustment)
        repo.undoSession(first)
        assertEquals(0, snapshot().library.record("kotlin").pagesRead)
        assertTrue(snapshot().sessions.all { it.undoneAt != null })
    }

    @Test fun c_parallelWritesRespectLoanLimitAndProgress() = runBlocking {
        coroutineScope { listOf("kotlin", "compose", "ai", "focus").map { id -> async { repo.borrow(id) } }.awaitAll() }
        assertEquals(3, snapshot().library.activeLoanCount)
        val borrowed = snapshot().library.records.entries.first { it.value.borrowedAt != null }.key
        coroutineScope { (1..8).map { async { repo.setPages(borrowed, 10, relative = true) } }.awaitAll() }
        assertEquals(80, snapshot().library.record(borrowed).pagesRead)
        assertEquals(8, snapshot().sessions.size)
    }

    @Test fun d_reopeningDatabasePreservesCrudGoalsAndHistoryWithoutReseeding() = runBlocking {
        val name = "persistence-task3-test.db"
        context.deleteDatabase(name)
        var fileDb = Room.databaseBuilder(context, LibraryDatabase::class.java, name).build()
        try {
            var fileRepo = RoomLibraryRepository(fileDb)
            fileRepo.initialize()
            fileRepo.borrow("kotlin")
            fileRepo.logSession("kotlin", 45, 25, "Persistent session")
            fileRepo.saveNote("kotlin", "Persistent note")
            fileRepo.setGoal(700)
            fileRepo.deleteBook("ai")
            fileDb.close()
            fileDb = Room.databaseBuilder(context, LibraryDatabase::class.java, name).build()
            fileRepo = RoomLibraryRepository(fileDb)
            fileRepo.initialize()
            val restored = fileRepo.snapshots.first()
            assertEquals(7, restored.books.size)
            assertEquals(45, restored.library.record("kotlin").pagesRead)
            assertEquals("Persistent note", restored.library.record("kotlin").note)
            assertEquals(700, restored.library.goalPages)
            assertEquals("Persistent session", restored.sessions.single().note)
        } finally { fileDb.close(); context.deleteDatabase(name) }
    }

    @Test fun e_legacyImportIsOnceOnlyAndEmptyCatalogStaysEmpty() = runBlocking {
        database.close()
        database = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        var imports = 0
        repo = RoomLibraryRepository(database, readLegacy = {
            imports++
            UserLibrary(mapOf("kotlin" to ReadingRecord(true, 123, 40, "Old note")), 600)
        })
        repo.initialize()
        assertEquals(40, snapshot().library.record("kotlin").pagesRead)
        assertEquals(600, snapshot().library.goalPages)
        snapshot().books.forEach { repo.deleteBook(it.id) }
        repo.initialize()
        assertEquals(1, imports)
        assertTrue(snapshot().books.isEmpty())
    }
}
