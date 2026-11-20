package com.dias.shelfwise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dias.shelfwise.data.LibraryRepository
import com.dias.shelfwise.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class Notice(val id: Long, val text: String)
data class LibraryUiState(
    val books: List<Book> = emptyList(), val library: UserLibrary = UserLibrary(),
    val sessions: List<ReadingSession> = emptyList(), val loading: Boolean = true,
    val loadError: String? = null, val saving: Boolean = false, val notice: Notice? = null,
    val savedBookId: String? = null, val deletedBookId: String? = null
)

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var noticeId = 0L
    private var observation: Job? = null

    init { retryLoading() }

    fun retryLoading() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, loadError = null) }
            try {
                repository.initialize()
                repository.snapshots.collect { snapshot ->
                    mutableState.update { it.copy(books = snapshot.books, library = snapshot.library,
                        sessions = snapshot.sessions, loading = false, loadError = null) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadError = "Could not open your library. Please retry.") }
            }
        }
    }

    fun borrow(id: String) { mutate { repository.borrow(id) } }
    fun returnBook(id: String) { mutate { repository.returnBook(id) } }
    fun toggleSaved(id: String) { mutate { repository.toggleSaved(id) } }
    fun setPages(id: String, pages: Int) { mutate { repository.setPages(id, pages) } }
    fun addPages(id: String, pages: Int) { mutate { repository.setPages(id, pages, relative = true) } }
    fun saveNote(id: String, note: String) { mutate { repository.saveNote(id, note) } }
    fun setGoal(pages: Int) { mutate { repository.setGoal(pages) } }
    fun logSession(id: String, pages: Int, minutes: Int, note: String) {
        mutate { repository.logSession(id, pages, minutes, note) }
    }
    fun undoSession(id: Long) { mutate { repository.undoSession(id) } }
    fun saveBook(id: String?, draft: BookDraft) {
        mutate {
            val saved = repository.saveBook(id, draft)
            mutableState.update { it.copy(savedBookId = saved) }
            if (id == null) "Book added to your collection." else "Book updated."
        }
    }
    fun deleteBook(id: String) {
        mutate {
            val message = repository.deleteBook(id)
            mutableState.update { it.copy(deletedBookId = id) }
            message
        }
    }
    fun consumeNavigation() { mutableState.update { it.copy(savedBookId = null, deletedBookId = null) } }
    fun clearNotice(id: Long) { mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it } }

    private fun mutate(operation: suspend () -> String) {
        viewModelScope.launch {
            mutex.withLock {
                if (mutableState.value.loading || mutableState.value.loadError != null) return@withLock
                mutableState.update { it.copy(saving = true) }
                try {
                    val message = operation()
                    mutableState.update { it.copy(notice = Notice(++noticeId, message)) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (invalid: IllegalArgumentException) {
                    mutableState.update { it.copy(notice = Notice(++noticeId, invalid.message ?: "Check your input.")) }
                } catch (_: Exception) {
                    mutableState.update { it.copy(notice = Notice(++noticeId, "Could not save. Please try again.")) }
                } finally { mutableState.update { it.copy(saving = false) } }
            }
        }
    }

    class Factory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LibraryViewModel::class.java))
            return LibraryViewModel(repository) as T
        }
    }
}
