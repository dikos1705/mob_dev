package com.dias.shelfwise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dias.shelfwise.data.Catalog
import com.dias.shelfwise.data.LibraryStore
import com.dias.shelfwise.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class Notice(val id: Long, val text: String)
data class LibraryUiState(val library: UserLibrary, val saving: Boolean = false, val notice: Notice? = null)

class LibraryViewModel(private val store: LibraryStore) : ViewModel() {
    private val mutableState = MutableStateFlow(LibraryUiState(store.load()))
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var noticeId = 0L

    fun borrow(id: String) = withBook(id) { LibraryActions.borrow(it, Catalog.book(id)!!, System.currentTimeMillis()) }
    fun returnBook(id: String) = withBook(id) { LibraryActions.returnBook(it, Catalog.book(id)!!) }
    fun toggleSaved(id: String) = withBook(id) { LibraryActions.toggleSaved(it, Catalog.book(id)!!) }
    fun setPages(id: String, pages: Int) = withBook(id) { LibraryActions.setPages(it, Catalog.book(id)!!, pages) }
    fun addPages(id: String, pages: Int) = withBook(id) { LibraryActions.setPages(it, Catalog.book(id)!!, it.record(id).pagesRead + pages) }
    fun saveNote(id: String, note: String) = withBook(id) { LibraryActions.saveNote(it, Catalog.book(id)!!, note) }
    fun setGoal(pages: Int) = mutate { LibraryActions.setGoal(it, pages) }
    fun clearNotice(id: Long) { mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it } }

    private fun withBook(id: String, operation: (UserLibrary) -> LibraryChange) {
        if (Catalog.book(id) != null) mutate(operation)
    }
    private fun mutate(operation: (UserLibrary) -> LibraryChange) {
        viewModelScope.launch {
            mutex.withLock {
                mutableState.update { it.copy(saving = true) }
                try {
                    val change = operation(mutableState.value.library)
                    if (change.library != mutableState.value.library) store.save(change.library)
                    mutableState.update { it.copy(library = change.library, notice = Notice(++noticeId, change.message)) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableState.update { it.copy(notice = Notice(++noticeId, "Could not save. Please try again.")) } }
                finally { mutableState.update { it.copy(saving = false) } }
            }
        }
    }
    class Factory(private val store: LibraryStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LibraryViewModel::class.java))
            return LibraryViewModel(store) as T
        }
    }
}
