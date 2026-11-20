package com.dias.shelfwise.data

import androidx.room.*
import com.dias.shelfwise.domain.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "settings")
data class SettingsEntity(@PrimaryKey val id: Int = 1, val goalPages: Int = 300)

@Entity(tableName = "books", indices = [Index("catalogId")])
data class BookEntity(
    @PrimaryKey val id: String, val catalogId: Int = 1, val position: Int,
    val title: String, val author: String, val category: String, val pages: Int,
    val format: String, val description: String, val coverColor: Long, val coverMark: String
) {
    fun model() = Book(id, title, author, category, pages, BookFormat.valueOf(format), description,
        coverColor, coverMark)
    companion object {
        fun from(book: Book, position: Int) = BookEntity(book.id, position = position, title = book.title,
            author = book.author, category = book.category, pages = book.pages, format = book.format.name,
            description = book.description, coverColor = book.coverColor, coverMark = book.coverMark)
    }
}

@Entity(tableName = "reading_records", foreignKeys = [ForeignKey(entity = BookEntity::class,
    parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)])
data class ReadingRecordEntity(@PrimaryKey val bookId: String, val saved: Boolean = false,
    val borrowedAt: Long? = null, val pagesRead: Int = 0, val note: String = "") {
    fun model() = ReadingRecord(saved, borrowedAt, pagesRead, note)
    companion object {
        fun from(id: String, record: ReadingRecord) = ReadingRecordEntity(id, record.saved,
            record.borrowedAt, record.pagesRead, record.note)
    }
}

@Entity(tableName = "reading_sessions", foreignKeys = [ForeignKey(entity = BookEntity::class,
    parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId"), Index("catalogId")])
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val catalogId: Int = 1,
    val bookId: String, val fromPage: Int, val toPage: Int, val minutes: Int,
    val note: String, val createdAt: Long, val undoneAt: Long? = null
) {
    fun model() = ReadingSession(id, bookId, fromPage, toPage, minutes, note, createdAt, undoneAt)
}

data class BookWithRecord(@Embedded val book: BookEntity,
    @Relation(parentColumn = "id", entityColumn = "bookId") val record: ReadingRecordEntity?)

data class StoredLibrary(
    @Embedded val settings: SettingsEntity,
    @Relation(parentColumn = "id", entityColumn = "catalogId", entity = BookEntity::class)
    val books: List<BookWithRecord>,
    @Relation(parentColumn = "id", entityColumn = "catalogId") val sessions: List<ReadingSessionEntity>
) {
    fun model() = LibrarySnapshot(books.sortedBy { it.book.position }.map { it.book.model() },
        UserLibrary(books.associate { it.book.id to (it.record?.model() ?: ReadingRecord()) }, settings.goalPages),
        sessions.sortedByDescending { it.id }.map { it.model() })
}

@Dao
interface LibraryDao {
    @Transaction @Query("SELECT * FROM settings WHERE id = 1")
    fun observeLibrary(): Flow<StoredLibrary?>

    @Transaction @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun readLibrary(): StoredLibrary?

    @Query("SELECT * FROM settings WHERE id = 1") suspend fun settings(): SettingsEntity?
    @Upsert suspend fun saveSettings(settings: SettingsEntity)
    @Upsert suspend fun saveBook(book: BookEntity)
    @Query("DELETE FROM books WHERE id = :id") suspend fun deleteBook(id: String): Int
    @Upsert suspend fun saveRecord(record: ReadingRecordEntity)
    @Insert suspend fun addSession(session: ReadingSessionEntity): Long
    @Query("SELECT * FROM reading_sessions WHERE id = :id") suspend fun session(id: Long): ReadingSessionEntity?
    @Query("SELECT id FROM reading_sessions WHERE bookId = :bookId AND undoneAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun latestActiveSessionId(bookId: String): Long?
    @Query("UPDATE reading_sessions SET undoneAt = :now WHERE id = :id AND undoneAt IS NULL")
    suspend fun markUndone(id: Long, now: Long): Int
}

@Database(entities = [BookEntity::class, ReadingRecordEntity::class, SettingsEntity::class,
    ReadingSessionEntity::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
