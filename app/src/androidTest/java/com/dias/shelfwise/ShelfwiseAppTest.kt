package com.dias.shelfwise

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.dias.shelfwise.data.PreferencesLibraryStore
import com.dias.shelfwise.domain.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ShelfwiseAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun clearData() {
        assertTrue(context.getSharedPreferences("shelfwise_library", Context.MODE_PRIVATE).edit().clear().commit())
    }
    @After fun closeActivity() { scenario?.close() }
    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java) }
    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun openKotlin() { compose.onNodeWithTag("featured-kotlin").performClick() }
    private fun recordPages() {
        compose.onNodeWithTag("progress-add").performScrollTo().performClick()
        awaitText("10 of 240 pages")
    }

    @Test fun a_borrowReadNoteGoalAndReturn() {
        launch()
        openKotlin()
        compose.onNodeWithTag("borrow-button").performClick()
        awaitText("Return book")
        recordPages()
        compose.onNodeWithTag("reading-note").performScrollTo().performTextInput("Use immutable state for UI.")
        compose.onNodeWithTag("reading-note").assertTextContains("Use immutable state for UI.")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("save-note").performScrollTo().performClick()
        compose.waitUntil(10_000) { PreferencesLibraryStore(context).load().record("kotlin").note.isNotEmpty() }
        compose.onNodeWithTag("tab-progress").performClick()
        compose.onNodeWithTag("goal-progress").assertTextEquals("10 / 300")
        compose.onNodeWithTag("edit-goal").performClick()
        compose.onNodeWithTag("goal-input").performTextReplacement("25")
        compose.onNodeWithText("Save goal").assertIsNotEnabled()
        compose.onNodeWithTag("goal-input").performTextReplacement("500")
        compose.onNodeWithText("Save goal").performClick()
        awaitText("10 / 500")
        compose.onNodeWithTag("tab-shelf").performClick()
        compose.onNodeWithTag("book-kotlin").performClick()
        compose.onNodeWithTag("borrow-button").performClick()
        awaitText("Borrow book")
        val returned = PreferencesLibraryStore(context).load()
        assertEquals(0, returned.activeLoanCount)
        assertEquals(10, returned.record("kotlin").pagesRead)
        assertEquals("Use immutable state for UI.", returned.record("kotlin").note)
    }

    @Test fun b_recreationRestoresSearchRouteAndUnsavedDraft() {
        launch()
        compose.onNodeWithTag("catalog-search").performScrollTo().performTextInput("No matching book")
        compose.onNodeWithTag("catalog-search").assertTextContains("No matching book")
        compose.waitForIdle()
        scenario!!.recreate()
        compose.onNodeWithTag("catalog-search").assertTextContains("No matching book")
        compose.onNodeWithTag("catalog-search").performTextClearance()
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        // Clear the keyboard focus before scrolling back to the featured book.
        compose.onNodeWithTag("discover-list").performScrollToIndex(0)
        openKotlin()
        compose.onNodeWithTag("borrow-button").performClick()
        awaitText("Return book")
        compose.onNodeWithTag("reading-note").performScrollTo().performTextInput("Draft survives recreation")
        compose.onNodeWithTag("reading-note").assertTextContains("Draft survives recreation")
        compose.waitForIdle()
        scenario!!.recreate()
        compose.onNodeWithTag("reading-note").performScrollTo().assertTextContains("Draft survives recreation")
        scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) { context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        compose.onNodeWithTag("reading-note").performScrollTo().assertTextContains("Draft survives recreation")
        scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(10_000) { context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        compose.onNodeWithTag("borrow-button").performScrollTo().assertTextEquals("Return book")
        assertEquals("", PreferencesLibraryStore(context).load().record("kotlin").note)
    }

    @Test fun c_savedReadingStateSurvivesActivityRelaunch() {
        launch()
        openKotlin()
        compose.onNodeWithTag("borrow-button").performClick()
        awaitText("Return book")
        recordPages()
        scenario!!.close()
        launch()
        compose.onNodeWithTag("tab-shelf").performClick()
        compose.onNodeWithTag("book-kotlin").performClick()
        compose.onNodeWithTag("reading-count").performScrollTo().assertTextEquals("10 of 240 pages")
        compose.onNodeWithTag("borrow-button").performScrollTo().assertTextEquals("Return book")
    }

    @Test fun d_corruptPreferencesFallBackToAnEmptyLibrary() {
        context.getSharedPreferences("shelfwise_library", Context.MODE_PRIVATE).edit().putString("library_v1", "{broken").commit()
        assertEquals(UserLibrary(), PreferencesLibraryStore(context).load())
        launch()
        compose.onNodeWithTag("tab-shelf").performClick()
        compose.onNodeWithText("Your next book is waiting").assertExists()
    }

    /** Real emulator screenshots; seeded fictional reading history makes every screen useful. */
    @Test fun z_captureReadmeScreenshots() {
        val demo = UserLibrary(mapOf(
            "kotlin" to ReadingRecord(true, System.currentTimeMillis(), 70, "State flows down; events flow up. Try this in my next project."),
            "compose" to ReadingRecord(true, System.currentTimeMillis(), 40, "Keep screen composables focused and reusable."),
            "focus" to ReadingRecord(false, null, 160, "A small daily reading habit is easier to keep."),
            "ai" to ReadingRecord(saved = true)
        ), goalPages = 500)
        runBlocking { PreferencesLibraryStore(context).save(demo) }
        launch()
        capture("01-discover.png")
        openKotlin()
        capture("02-book-details.png")
        compose.onNodeWithTag("reading-note").performScrollTo()
        capture("03-reading-notes.png")
        compose.onNodeWithTag("tab-shelf").performClick()
        capture("04-my-shelf.png")
        compose.onNodeWithTag("tab-progress").performClick()
        capture("05-progress.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Allow navigation and platform drawing to finish before taking the actual display bitmap.
        android.os.SystemClock.sleep(700)
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
