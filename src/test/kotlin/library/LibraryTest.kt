package library

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LibraryTest {
    private fun fixture(limit: Int = 3): Library = runBlocking {
        Library().apply {
            addItems(DemoCatalog.load())
            register(Member(1, "Test Member", limit))
            register(Member(2, "Another Member"))
        }
    }

    @Test fun `catalog loads using suspend function`() = runBlocking {
        val items = DemoCatalog.load()
        assertEquals(5, items.size)
        assertEquals(5, items.map { it.id }.toSet().size)
    }

    @Test fun `printed and digital books have different loan policies`() {
        val library = fixture()
        val printed = assertIs<BorrowResult.Success>(library.borrow(1, 1))
        val digital = assertIs<BorrowResult.Success>(library.borrow(2, 1))
        assertEquals(14, printed.loan.durationDays)
        assertEquals(7, digital.loan.durationDays)
        assertTrue(printed.instructions.contains("shelf A1"))
        assertTrue(digital.instructions.contains("PDF"))
        assertEquals(2, library.activeLoans().size)
    }

    @Test fun `item cannot be borrowed twice and can be borrowed after return`() {
        val library = fixture()
        assertIs<BorrowResult.Success>(library.borrow(1, 1))
        assertIs<BorrowResult.Rejected>(library.borrow(1, 2))
        assertFalse(library.isAvailable(1))
        assertFalse(library.returnItem(1, 2))
        assertTrue(library.returnItem(1, 1))
        assertFalse(library.returnItem(1, 1))
        assertIs<BorrowResult.Success>(library.borrow(1, 2))
    }

    @Test fun `invalid IDs do not change loans`() {
        val library = fixture()
        assertIs<BorrowResult.Rejected>(library.borrow(999, 1))
        assertIs<BorrowResult.Rejected>(library.borrow(1, 999))
        assertFalse(library.isAvailable(999))
        assertFalse(library.returnItem(999, 1))
        assertTrue(library.activeLoans().isEmpty())
    }

    @Test fun `member loan limit enforced and return frees a slot`() {
        val library = fixture(limit = 1)
        assertIs<BorrowResult.Success>(library.borrow(1, 1))
        assertIs<BorrowResult.Rejected>(library.borrow(2, 1))
        assertTrue(library.isAvailable(2))
        library.returnItem(1, 1)
        assertIs<BorrowResult.Success>(library.borrow(2, 1))
    }

    @Test fun `collections provide filtered catalog unique genres and page total`() {
        val library = fixture()
        assertEquals(listOf(1, 2), library.findItems { it.details.genre == "Programming" }.map { it.id })
        assertEquals(setOf("Programming", "Fiction", "Design"), library.genres())
        assertEquals(1100, library.totalPages())
    }

    @Test fun `empty catalog handles reduce safely`() {
        val library = Library()
        assertEquals(0, library.totalPages())
        assertTrue(library.genres().isEmpty())
        assertTrue(library.findItems { true }.isEmpty())
    }

    @Test fun `duplicate IDs rejected without partial catalog changes`() {
        val library = fixture()
        assertFailsWith<IllegalArgumentException> { library.addItems(library.allItems()) }
        assertEquals(5, library.allItems().size)
        assertFailsWith<IllegalArgumentException> { library.register(Member(1, "Duplicate")) }
    }

    @Test fun `domain values are validated`() {
        assertFailsWith<IllegalArgumentException> { Member(0, "Test") }
        assertFailsWith<IllegalArgumentException> { Member(1, "Test", 0) }
        assertFailsWith<IllegalArgumentException> { BookDetails("Title", "Author", "Genre", 0) }
    }
}
