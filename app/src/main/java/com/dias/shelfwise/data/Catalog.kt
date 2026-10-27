package com.dias.shelfwise.data

import com.dias.shelfwise.domain.Book
import com.dias.shelfwise.domain.BookFormat

// Fictional teaching catalog, available offline. No real lending service is contacted.
object Catalog {
    val books = listOf(
        Book("kotlin", "Kotlin for Curious Minds", "Alex Reed", "Programming", 240, BookFormat.PRINT,
            "Build confidence with Kotlin one small idea at a time. Explore expressive functions, collections, null safety, and practical object-oriented design through approachable exercises.", 0xFF31594C, "K"),
        Book("patterns", "Patterns That Make Sense", "Morgan Hill", "Programming", 280, BookFormat.PRINT,
            "Recognize recurring design problems and choose a clear solution. Learn Observer, State, Command, and other patterns through game and application examples.", 0xFFB66543, "P"),
        Book("compose", "A Little Book of Compose", "Jamie Park", "Design", 180, BookFormat.DIGITAL,
            "Create thoughtful Android interfaces with reusable composables. Understand state, navigation, accessibility, and the small choices that make an app comfortable to use.", 0xFF796B91, "C"),
        Book("ai", "Reasoning in a Noisy World", "Sam Lee", "AI", 220, BookFormat.DIGITAL,
            "Think clearly when observations are uncertain. Work through probability, diagnostic reasoning, Bayesian models, and the trade-offs behind intelligent decisions.", 0xFF3D7286, "AI"),
        Book("data", "The Shape of Data", "Taylor Brook", "Data", 260, BookFormat.PRINT,
            "Turn a collection of facts into a reliable data model. Explore relational tables, SQL, transactions, and careful reasoning about information.", 0xFFAC843D, "D"),
        Book("focus", "Make Time to Learn", "Casey Wells", "Learning", 160, BookFormat.DIGITAL,
            "Design a reading routine that fits a busy student life. Break material into useful sessions, keep meaningful notes, and notice progress without rushing.", 0xFF587358, "L"),
        Book("systems", "Small Systems, Big Ideas", "Robin Blake", "Programming", 320, BookFormat.PRINT,
            "Follow a simple application from a single process to cooperating services. Learn about containers, communication, persistence, and keeping systems understandable.", 0xFF5C6582, "S"),
        Book("visual", "Everyday Visual Thinking", "Drew Lane", "Design", 190, BookFormat.DIGITAL,
            "Use hierarchy, spacing, typography, and diagrams to make ideas easier to understand. Practice communicating with structure rather than decoration.", 0xFF9C6976, "V")
    )
    fun book(id: String): Book? = books.find { it.id == id }
    val categories: List<String> = listOf("All") + books.map { it.category }.distinct()
}
