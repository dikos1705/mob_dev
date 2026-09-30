package library

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    if (args.any { it !in setOf("--demo", "--interactive") } || args.distinct().size > 1) {
        println("Usage: campus-library [--demo | --interactive]")
        return@runBlocking
    }
    val library = Library()
    val member = Member(1, "Dias")
    library.register(member)
    println("=== Campus Library ===")
    val loading = launch { library.addItems(DemoCatalog.load()) }
    println("Loading the sample catalog in a coroutine...")
    loading.join() // Do not allow borrowing until the catalog is ready.
    println("Loaded ${library.allItems().size} items. Welcome, ${member.name}!")
    if ("--interactive" in args) runMenu(library, member) else runDemo(library, member)
}

fun showItems(library: Library, items: List<LibraryItem>) {
    if (items.isEmpty()) println("No matching items.")
    for (item in items) {
        val status: String = if (library.isAvailable(item.id)) "available" else "on loan"
        println("${item.id}. ${item.description()} | ${item.details.author} | $status")
    }
}

fun showResult(result: BorrowResult) {
    // Exhaustive when over a sealed class: no catch-all else is needed.
    when (result) {
        is BorrowResult.Success -> println("Borrowed #${result.loan.itemId} for ${result.loan.durationDays} days. ${result.instructions}")
        is BorrowResult.Rejected -> println("Cannot borrow: ${result.reason}")
    }
}

fun showStatistics(library: Library) {
    println("Genres (unique Set): ${library.genres().sorted().joinToString()}")
    println("Total catalog pages (map + reduce): ${library.totalPages()}")
    println("Active loans: ${library.activeLoans().size}")
    val available = library.findItems { library.isAvailable(it.id) }
    println("Available titles (filter + map): ${available.map { it.details.title }.joinToString()}")
}

fun runDemo(library: Library, member: Member) {
    showItems(library, library.allItems())
    println("\nProgramming items:")
    showItems(library, library.findItems { it.details.genre == "Programming" })
    println("\nBorrow a printed book and an e-book:")
    showResult(library.borrow(1, member.id))
    showResult(library.borrow(2, member.id))
    showResult(library.borrow(1, member.id))
    println("Return #1: ${library.returnItem(1, member.id)}")
    println("\nLibrary statistics:")
    showStatistics(library)
    println("\nDemo complete. Start with --interactive to use the menu.")
}

fun runMenu(library: Library, member: Member) {
    var running: Boolean = true
    while (running) {
        println("\n1 Catalog | 2 Search | 3 Borrow | 4 Return | 5 Statistics | 0 Exit")
        print("> ")
        val choice = readlnOrNull()?.trim()
        when (choice) {
            null, "0" -> running = false
            "1" -> showItems(library, library.allItems())
            "2" -> {
                print("Title, author or genre: ")
                val query = readlnOrNull()?.trim() ?: return
                showItems(library, library.findItems {
                    it.details.title.contains(query, ignoreCase = true) ||
                        it.details.author.contains(query, ignoreCase = true) ||
                        it.details.genre.contains(query, ignoreCase = true)
                })
            }
            "3", "4" -> {
                print("Item ID: ")
                val input = readlnOrNull() ?: return
                val id = input.trim().toIntOrNull()
                if (id == null || id <= 0) {
                    println("Enter a positive numeric ID.")
                } else if (choice == "3") {
                    showResult(library.borrow(id, member.id))
                } else {
                    println(if (library.returnItem(id, member.id)) "Returned #$id." else "You do not have this item on loan.")
                }
            }
            "5" -> showStatistics(library)
            else -> println("Choose a number from 0 to 5.")
        }
    }
    println("Goodbye!")
}
