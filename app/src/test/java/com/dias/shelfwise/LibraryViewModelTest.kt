package com.dias.shelfwise

import com.dias.shelfwise.data.LibraryStore
import com.dias.shelfwise.domain.UserLibrary
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    private class MemoryStore : LibraryStore {
        var saved = UserLibrary()
        var fail = false
        override fun load() = saved
        override suspend fun save(library: UserLibrary) {
            delay(10) // expose lost-update races
            if (fail) throw IOException("disk full")
            saved = library
        }
    }
    @Test fun `queued increments are serialized and survive a new viewmodel`() = runTest(dispatcher) {
        val store = MemoryStore()
        val vm = LibraryViewModel(store)
        vm.borrow("kotlin")
        repeat(8) { vm.addPages("kotlin", 10) }
        advanceUntilIdle()
        assertEquals(80, vm.state.value.library.record("kotlin").pagesRead)
        assertFalse(vm.state.value.saving)
        assertEquals(vm.state.value.library, LibraryViewModel(store).state.value.library)
    }
    @Test fun `failed persistence does not publish an unsaved change and permits retry`() = runTest(dispatcher) {
        val store = MemoryStore().apply { fail = true }
        val vm = LibraryViewModel(store)
        vm.borrow("kotlin")
        advanceUntilIdle()
        assertEquals(0, vm.state.value.library.activeLoanCount)
        assertTrue(vm.state.value.notice!!.text.contains("Could not save"))
        assertFalse(vm.state.value.saving)
        store.fail = false
        vm.borrow("kotlin")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.library.activeLoanCount)
    }
    @Test fun `invalid ids are ignored and an old notice cannot clear a newer one`() = runTest(dispatcher) {
        val vm = LibraryViewModel(MemoryStore())
        vm.borrow("missing")
        advanceUntilIdle()
        assertEquals(UserLibrary(), vm.state.value.library)
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
