package library

import kotlinx.coroutines.delay

// A singleton provides fictional sample records. No external service is contacted.
object DemoCatalog {
    suspend fun load(): List<LibraryItem> {
        delay(200) // Simulated I/O latency: suspends the coroutine instead of sleeping.
        return listOf(
            PrintedBook(1, BookDetails("Kotlin on Campus", "Alex Reed", "Programming", 240), "A1"),
            EBook(2, BookDetails("Coroutines in Practice", "Sam Lee", "Programming", 180), "PDF"),
            PrintedBook(3, BookDetails("The Quiet Planet", "Morgan Hill", "Fiction", 320), "B2"),
            EBook(4, BookDetails("Design for Everyone", "Jamie Park", "Design", 150), "EPUB"),
            PrintedBook(5, BookDetails("Small Worlds", "Taylor Brook", "Fiction", 210), "B3")
        )
    }
}
