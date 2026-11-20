package com.dias.shelfwise

import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.data.LibraryRepository
import com.dias.shelfwise.domain.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    private class MemoryRepository : LibraryRepository {
        override val snapshots = MutableStateFlow(LibrarySnapshot(Catalog.books))
        var fail = false
        override suspend fun initialize() { }
        private suspend fun change(operation: (UserLibrary) -> LibraryChange): String {
            delay(10)
            if (fail) throw IOException("Disk full")
            val change = operation(snapshots.value.library)
            snapshots.value = snapshots.value.copy(library = change.library)
            return change.message
        }
        override suspend fun borrow(id: String) = change { LibraryActions.borrow(it, Catalog.book(id)!!, 100) }
        override suspend fun returnBook(id: String) = change { LibraryActions.returnBook(it, Catalog.book(id)!!) }
        override suspend fun toggleSaved(id: String) = change { LibraryActions.toggleSaved(it, Catalog.book(id)!!) }
        override suspend fun setPages(id: String, pages: Int, relative: Boolean) = change {
            LibraryActions.setPages(it, Catalog.book(id)!!, if (relative) it.record(id).pagesRead + pages else pages)
        }
        override suspend fun saveNote(id: String, note: String) = change { LibraryActions.saveNote(it, Catalog.book(id)!!, note) }
        override suspend fun setGoal(pages: Int) = change { LibraryActions.setGoal(it, pages) }
        override suspend fun saveBook(id: String?, draft: BookDraft): String = error("Not used in this fake")
        override suspend fun deleteBook(id: String): String = error("Not used in this fake")
        override suspend fun logSession(id: String, pages: Int, minutes: Int, note: String): String = error("Not used in this fake")
        override suspend fun undoSession(id: Long): String = error("Not used in this fake")
    }

    @Test fun `state comes from repository flow and queued increments do not lose updates`() = runTest(dispatcher) {
        val repo = MemoryRepository()
        val vm = LibraryViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        vm.borrow("kotlin")
        repeat(8) { vm.addPages("kotlin", 10) }
        advanceUntilIdle()
        assertEquals(80, vm.state.value.library.record("kotlin").pagesRead)
        assertFalse(vm.state.value.saving)
        val restored = LibraryViewModel(repo)
        advanceUntilIdle()
        assertEquals(vm.state.value.library, restored.state.value.library)
    }

    @Test fun `failed repository write does not publish unsaved progress and retry succeeds`() = runTest(dispatcher) {
        val repo = MemoryRepository().apply { fail = true }
        val vm = LibraryViewModel(repo)
        advanceUntilIdle()
        vm.borrow("kotlin")
        advanceUntilIdle()
        assertEquals(0, vm.state.value.library.activeLoanCount)
        assertTrue(vm.state.value.notice!!.text.contains("Could not save"))
        assertFalse(vm.state.value.saving)
        repo.fail = false
        vm.borrow("kotlin")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.library.activeLoanCount)
    }

    @Test fun `external flow update refreshes the UI and old notice cannot clear a new one`() = runTest(dispatcher) {
        val repo = MemoryRepository()
        val vm = LibraryViewModel(repo)
        advanceUntilIdle()
        repo.snapshots.value = repo.snapshots.value.copy(books = Catalog.books.take(2))
        advanceUntilIdle()
        assertEquals(2, vm.state.value.books.size)
        vm.setGoal(500)
        advanceUntilIdle()
        val oldId = vm.state.value.notice!!.id
        vm.toggleSaved("kotlin")
        advanceUntilIdle()
        vm.clearNotice(oldId)
        assertNotNull(vm.state.value.notice)
        vm.clearNotice(vm.state.value.notice!!.id)
        assertNull(vm.state.value.notice)
    }
}
