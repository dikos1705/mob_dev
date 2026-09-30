package library

class Library {
    private val catalog: MutableList<LibraryItem> = mutableListOf()
    private val members: MutableMap<Int, Member> = mutableMapOf()
    private val loans: MutableMap<Int, Loan> = mutableMapOf()

    fun addItems(items: List<LibraryItem>) {
        val ids = (catalog + items).map { it.id }
        require(ids.toSet().size == ids.size) { "Catalog item IDs must be unique." }
        catalog.addAll(items)
    }

    fun register(member: Member) {
        require(member.id !in members) { "Member ID is already registered." }
        members[member.id] = member
    }

    // Higher-order function: callers supply their own filtering rule as a lambda.
    fun findItems(predicate: (LibraryItem) -> Boolean): List<LibraryItem> = catalog.filter(predicate)

    fun allItems(): List<LibraryItem> = catalog.toList()
    fun isAvailable(itemId: Int): Boolean = catalog.any { it.id == itemId } && itemId !in loans
    fun genres(): Set<String> = catalog.map { it.details.genre }.toSet()
    fun activeLoans(): List<Loan> = loans.values.toList()

    fun totalPages(): Int {
        val pageCounts: List<Int> = catalog.map { it.details.pages }
        // reduce needs a nonempty collection; the empty catalog is handled first.
        return if (pageCounts.isEmpty()) 0 else pageCounts.reduce { total, pages -> total + pages }
    }

    fun borrow(itemId: Int, memberId: Int): BorrowResult {
        val member = members[memberId] ?: return BorrowResult.Rejected("Member not found.")
        val item = catalog.find { it.id == itemId } ?: return BorrowResult.Rejected("Item not found.")
        if (itemId in loans) return BorrowResult.Rejected("This item is already on loan.")
        if (loans.values.count { it.memberId == memberId } >= member.loanLimit) {
            return BorrowResult.Rejected("Loan limit reached (${member.loanLimit}).")
        }
        // Interface polymorphism: printed and digital items supply different policies.
        val policy: Borrowable = item
        val loan = Loan(item.id, memberId, policy.loanDays())
        loans[itemId] = loan
        return BorrowResult.Success(loan, policy.borrowingInstructions())
    }

    fun returnItem(itemId: Int, memberId: Int): Boolean {
        val loan = loans[itemId] ?: return false
        if (loan.memberId != memberId) return false
        loans.remove(itemId)
        return true
    }
}
