package library

// Data classes hold values and provide equals, toString and copy automatically.
data class BookDetails(val title: String, val author: String, val genre: String, val pages: Int) {
    init {
        require(title.isNotBlank() && author.isNotBlank() && genre.isNotBlank())
        require(pages > 0) { "Page count must be positive." }
    }
}

data class Member(val id: Int, val name: String, val loanLimit: Int = 3) {
    init {
        require(id > 0 && name.isNotBlank() && loanLimit > 0)
    }
}

interface Borrowable {
    fun loanDays(): Int
    fun borrowingInstructions(): String
}

abstract class LibraryItem(val id: Int, val details: BookDetails) : Borrowable {
    init { require(id > 0) }
    abstract fun description(): String
}

class PrintedBook(id: Int, details: BookDetails, val shelf: String) : LibraryItem(id, details) {
    override fun loanDays(): Int = 14
    override fun borrowingInstructions(): String = "Collect from shelf $shelf."
    override fun description(): String = "Printed book | ${details.title} | shelf $shelf"
}

class EBook(id: Int, details: BookDetails, val format: String) : LibraryItem(id, details) {
    override fun loanDays(): Int = 7
    override fun borrowingInstructions(): String = "Use the library reader ($format)."
    override fun description(): String = "E-book | ${details.title} | $format"
}

data class Loan(val itemId: Int, val memberId: Int, val durationDays: Int)

// Every possible borrowing outcome is explicitly represented.
sealed class BorrowResult {
    data class Success(val loan: Loan, val instructions: String) : BorrowResult()
    data class Rejected(val reason: String) : BorrowResult()
}
