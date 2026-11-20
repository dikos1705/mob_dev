package com.dias.shelfwise

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dias.shelfwise.domain.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ShelfwiseAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val app get() = context as ShelfwiseApplication
    private val repo get() = app.repository
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun clearData() = runBlocking {
        app.getSharedPreferences("shelfwise_library", Context.MODE_PRIVATE).edit().clear().commit()
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
    }
    @After fun closeActivity() { scenario?.close() }
    private fun snapshot() = runBlocking { repo.snapshots.first() }
    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("featured-kotlin").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun awaitPages(pages: Int) {
        compose.waitUntil(10_000) { snapshot().library.record("kotlin").pagesRead == pages }
    }
    private fun openKotlin() { compose.onNodeWithTag("featured-kotlin").safeClick() }
    private fun hideKeyboard() { Espresso.closeSoftKeyboard(); compose.waitForIdle() }
    private fun fill(tag: String, value: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    }
    private fun SemanticsNodeInteraction.safeClick(): SemanticsNodeInteraction {
        compose.waitForIdle()
        if (compose.onAllNodesWithContentDescription("Dismiss").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithContentDescription("Dismiss").performClick()
        compose.waitUntil(10_000) { runCatching { assertIsEnabled() }.isSuccess }
        return performClick()
    }
    private fun session(pages: String, minutes: String, note: String) {
        compose.onNodeWithTag("log-session").performScrollTo().safeClick()
        fill("session-pages", pages)
        fill("session-minutes", minutes)
        fill("session-note", note)
        hideKeyboard()
    }

    @Test fun a_previousBorrowProgressNotesGoalAndReturnStillWork() {
        launch(); openKotlin()
        compose.onNodeWithTag("borrow-button").safeClick()
        awaitText("Return book")
        compose.onNodeWithTag("progress-add").performScrollTo().safeClick()
        awaitPages(10)
        fill("reading-note", "Use immutable state for UI.")
        hideKeyboard()
        compose.onNodeWithTag("save-note").performScrollTo().safeClick()
        compose.waitUntil(10_000) { snapshot().library.record("kotlin").note.isNotEmpty() }
        compose.onNodeWithTag("tab-progress").safeClick()
        compose.onNodeWithTag("goal-progress").assertTextEquals("10 / 300")
        compose.onNodeWithTag("edit-goal").safeClick()
        compose.onNodeWithTag("goal-input").performTextReplacement("25")
        compose.onNodeWithText("Save goal").assertIsNotEnabled()
        compose.onNodeWithTag("goal-input").performTextReplacement("500")
        compose.onNodeWithText("Save goal").safeClick()
        awaitText("10 / 500")
        compose.onNodeWithTag("tab-shelf").safeClick()
        compose.onNodeWithTag("book-kotlin").safeClick()
        compose.onNodeWithTag("borrow-button").safeClick()
        awaitText("Borrow book")
        assertEquals(0, snapshot().library.activeLoanCount)
        assertEquals(10, snapshot().library.record("kotlin").pagesRead)
        assertEquals("Use immutable state for UI.", snapshot().library.record("kotlin").note)
        assertTrue(snapshot().sessions.single().isAdjustment)
    }

    @Test fun b_bookCreateEditValidationAndConfirmedDeleteAreInTheUi() {
        launch()
        compose.onNodeWithTag("add-book").safeClick()
        compose.onNodeWithTag("save-book").performScrollTo().safeClick()
        compose.onNodeWithText("Enter a title, up to 80 characters.").assertExists()
        fill("book-title", "My local book")
        fill("book-author", "Dias")
        fill("book-category", "Learning")
        fill("book-pages", "0")
        fill("book-description", "A locally stored custom book.")
        hideKeyboard()
        compose.onNodeWithTag("save-book").performScrollTo().safeClick()
        compose.onNodeWithText("Choose 1 to 10,000 pages.").assertExists()
        fill("book-pages", "180")
        hideKeyboard()
        compose.onNodeWithTag("save-book").performScrollTo().safeClick()
        awaitText("My local book")
        val id = snapshot().books.first { it.title == "My local book" }.id
        compose.onNodeWithTag("edit-book").performScrollTo().safeClick()
        fill("book-title", "My edited book")
        hideKeyboard()
        compose.onNodeWithTag("save-book").performScrollTo().safeClick()
        awaitText("My edited book")
        compose.onNodeWithTag("borrow-button").performScrollTo().safeClick()
        awaitText("Return book")
        session("20", "15", "An actual journal entry.")
        compose.onNodeWithTag("confirm-session").safeClick()
        compose.waitUntil(10_000) { snapshot().library.record(id).pagesRead == 20 }
        compose.onNodeWithTag("delete-book").performScrollTo().safeClick()
        compose.onNodeWithText("Cancel").safeClick()
        assertTrue(snapshot().books.any { it.id == id })
        compose.onNodeWithTag("delete-book").performScrollTo().safeClick()
        compose.onNodeWithTag("confirm-delete").safeClick()
        compose.waitUntil(10_000) { snapshot().books.none { it.id == id } }
        assertTrue(snapshot().sessions.none { it.bookId == id })
        compose.onNodeWithTag("add-book").assertExists()
    }

    @Test fun c_sessionWorkflowUndoStatsAndRelaunchStayConsistent() {
        launch(); openKotlin()
        compose.onNodeWithTag("borrow-button").safeClick(); awaitText("Return book")
        session("999", "20", "First session")
        compose.onNodeWithTag("confirm-session").assertIsNotEnabled()
        fill("session-pages", "30"); hideKeyboard()
        compose.onNodeWithTag("confirm-session").safeClick(); awaitPages(30)
        val first = snapshot().sessions.first().id
        session("10", "15", "Second session")
        compose.onNodeWithTag("confirm-session").safeClick(); awaitPages(40)
        val second = snapshot().sessions.first().id
        compose.onNodeWithTag("book-journal").performScrollTo().safeClick()
        compose.onNodeWithTag("undo-$first").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("undo-$second").performScrollTo().safeClick()
        compose.onNodeWithTag("confirm-undo").safeClick(); awaitPages(30)
        compose.onNodeWithTag("undo-$second").assertIsNotEnabled()
        compose.onNodeWithTag("tab-progress").safeClick()
        compose.onNodeWithTag("goal-progress").assertTextEquals("30 / 300")
        compose.onNodeWithTag("session-statistics").performScrollTo().assertTextContains("20 minutes", substring = true)
        scenario!!.close(); launch()
        compose.onNodeWithTag("tab-journal").safeClick()
        compose.onNodeWithTag("undo-$second").performScrollTo().assertIsNotEnabled()
        assertEquals(30, snapshot().library.record("kotlin").pagesRead)
    }

    @Test fun d_formDraftAndNavigationSurviveRecreationAndRotation() {
        launch()
        compose.onNodeWithTag("add-book").safeClick()
        fill("book-title", "Draft survives rotation")
        hideKeyboard()
        scenario!!.recreate()
        compose.onNodeWithTag("book-title").performScrollTo().assertTextContains("Draft survives rotation")
        scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) { context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        compose.onNodeWithTag("book-title").performScrollTo().assertTextContains("Draft survives rotation")
        scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(10_000) { context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        assertEquals(8, snapshot().books.size)
    }

    @Test fun e_searchBookmarkAndSavedShelfStillWork() {
        launch()
        fill("catalog-search", "No matching book")
        hideKeyboard(); scenario!!.recreate()
        compose.onNodeWithTag("catalog-search").assertTextContains("No matching book")
        fill("catalog-search", ""); hideKeyboard()
        compose.onNodeWithTag("discover-list").performScrollToIndex(0)
        openKotlin()
        compose.onNodeWithContentDescription("Save Kotlin for Curious Minds").safeClick()
        compose.waitUntil(10_000) { snapshot().library.record("kotlin").saved }
        compose.onNodeWithTag("tab-shelf").safeClick()
        compose.onNodeWithText("Saved").safeClick()
        compose.onNodeWithTag("book-kotlin").assertExists()
    }

    @Test fun f_corruptLegacyPreferencesDoNotBreakRoomInitialization() {
        context.getSharedPreferences("shelfwise_library", Context.MODE_PRIVATE).edit()
            .putString("library_v1", "{broken").commit()
        launch()
        assertEquals(8, snapshot().books.size)
        assertEquals(0, snapshot().library.activeLoanCount)
    }

    /** Actual display screenshots from the new feature; production repository writes seed the demo. */
    @Test fun z_captureTask3Screenshots() {
        runBlocking {
            repo.initialize()
            repo.borrow("kotlin")
            repo.logSession("kotlin", 35, 25, "State flows down; events flow up.")
            repo.logSession("kotlin", 20, 15, "Try immutable state in my next project.")
            repo.saveNote("kotlin", "Keep screen composables focused and reusable.")
            repo.borrow("compose")
            repo.logSession("compose", 25, 20, "A small daily reading habit.")
            repo.setGoal(500)
        }
        launch(); openKotlin()
        session("30", "20", "Room keeps my progress and history in sync.")
        capture("01-record-session.png")
        compose.onNodeWithTag("confirm-session").safeClick(); awaitPages(85)
        val latest = snapshot().sessions.first().id
        compose.onNodeWithTag("book-journal").performScrollTo().safeClick()
        compose.onNodeWithTag("journal-list").performScrollToIndex(0)
        capture("02-session-history.png")
        compose.onNodeWithTag("undo-$latest").performScrollTo().safeClick()
        capture("03-undo-confirmation.png")
        compose.onNodeWithTag("confirm-undo").safeClick(); awaitPages(55)
        compose.onNodeWithTag("journal-list").performScrollToIndex(0)
        capture("04-undone-history.png")
        compose.onNodeWithTag("tab-progress").safeClick()
        compose.onNodeWithTag("session-statistics").performScrollTo()
        capture("05-reading-statistics.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(700)
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(context.getExternalFilesDir(null), "task3-screenshots").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
