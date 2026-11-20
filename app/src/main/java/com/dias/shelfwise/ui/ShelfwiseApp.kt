package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.dias.shelfwise.LibraryViewModel

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfwiseApp(viewModel: LibraryViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "discover"
    val editor = route == "new-book" || route.startsWith("edit/")
    val detail = route.startsWith("book/") || editor || route.startsWith("journal/")
    val snackbar = remember { SnackbarHostState() }
    val tabs = remember { listOf(Tab("discover", "Discover", Icons.Outlined.Explore),
        Tab("shelf", "My shelf", Icons.AutoMirrored.Outlined.MenuBook), Tab("journal", "Journal", Icons.Outlined.History),
        Tab("progress", "Progress", Icons.Outlined.Insights)) }
    LaunchedEffect(state.notice?.id) {
        state.notice?.let { notice -> snackbar.showSnackbar(notice.text, withDismissAction = true); viewModel.clearNotice(notice.id) }
    }
    fun navigateTab(destination: String) {
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(state.savedBookId, state.deletedBookId, state.books) {
        state.savedBookId?.let { id ->
            if (state.books.any { it.id == id }) {
                if (editor) nav.navigate("book/$id") { popUpTo(route) { inclusive = true }; launchSingleTop = true }
                viewModel.consumeNavigation()
            }
        }
        state.deletedBookId?.let { id ->
            if (state.books.none { it.id == id }) {
                if (entry?.arguments?.getString("bookId") == id)
                    nav.navigate("discover") { popUpTo(nav.graph.findStartDestination().id); launchSingleTop = true }
                viewModel.consumeNavigation()
            }
        }
    }
    Scaffold(modifier = Modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(when {
            editor -> if (route == "new-book") "New book" else "Edit book"
            route.startsWith("journal/") -> "Book journal"
            detail -> "Book details"
            else -> "shelfwise."
        }, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            navigationIcon = { if (detail) IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.testTag("back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                if (state.saving) CircularProgressIndicator(Modifier.padding(16.dp).size(18.dp), strokeWidth = 2.dp)
                if (route == "discover" && !state.loading && state.loadError == null)
                    IconButton(onClick = { nav.navigate("new-book") }, modifier = Modifier.testTag("add-book")) { Icon(Icons.Filled.Add, "Add a book") }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = { if (!state.loading && state.loadError == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEach { tab -> NavigationBarItem(selected = route == tab.route || (tab.route == "journal" && route.startsWith("journal/")),
                onClick = { navigateTab(tab.route) }, icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label) },
                modifier = Modifier.testTag("tab-${tab.route}")) }
        } }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (state.loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (state.loadError != null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            EmptyState("Your library could not open", state.loadError!!, "Retry", viewModel::retryLoading)
        } else NavHost(navController = nav, startDestination = "discover", modifier = Modifier.padding(padding)) {
            composable("discover") { DiscoverScreen(state.books, state.library, { nav.navigate("book/$it") }, viewModel::toggleSaved, { nav.navigate("new-book") }) }
            composable("shelf") { ShelfScreen(state.books, state.library, { nav.navigate("book/$it") }, viewModel::toggleSaved, { navigateTab("discover") }) }
            composable("progress") { ProgressScreen(state.books, state.library, state.sessions, viewModel::setGoal,
                { nav.navigate("book/$it") }, viewModel::toggleSaved, { navigateTab("journal") }) }
            composable("journal") { JournalScreen(state.books, state.library, state.sessions, null, state.saving,
                viewModel::undoSession, { nav.navigate("book/$it") }, { navigateTab("discover") }) }
            composable("journal/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { stack ->
                JournalScreen(state.books, state.library, state.sessions, stack.arguments?.getString("bookId"), state.saving,
                    viewModel::undoSession, { nav.navigate("book/$it") }, { navigateTab("discover") })
            }
            composable("new-book") { BookEditorScreen(null, state.saving) { viewModel.saveBook(null, it) } }
            composable("edit/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { stack ->
                val id = stack.arguments?.getString("bookId")
                val book = state.books.find { it.id == id }
                if (book != null) BookEditorScreen(book, state.saving) { viewModel.saveBook(book.id, it) }
                else EmptyState("Book not found", "It may have been deleted.", "Discover", { navigateTab("discover") })
            }
            composable("book/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { stack ->
                val id = stack.arguments?.getString("bookId").orEmpty()
                val book = state.books.find { it.id == id }
                if (book == null) EmptyState("Book not found", "Return to the collection to choose another book.", "Discover", { navigateTab("discover") })
                else BookDetailsScreen(book, state.library.record(id), { viewModel.borrow(id) }, { viewModel.returnBook(id) },
                    { viewModel.toggleSaved(id) }, { viewModel.setPages(id, it) }, { viewModel.addPages(id, 10) },
                    { viewModel.saveNote(id, it) }, { nav.navigate("edit/$id") }, { viewModel.deleteBook(id) },
                    { pages, minutes, note -> viewModel.logSession(id, pages, minutes, note) },
                    { nav.navigate("journal/$id") }, state.saving)
            }
        }
    }
}
