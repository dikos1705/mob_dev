package com.dias.shelfwise.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dias.shelfwise.LibraryViewModel
import com.dias.shelfwise.data.Catalog

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfwiseApp(viewModel: LibraryViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "discover"
    val detail = route.startsWith("book/")
    val snackbar = remember { SnackbarHostState() }
    val tabs = remember { listOf(Tab("discover", "Discover", Icons.Outlined.Explore), Tab("shelf", "My shelf", Icons.Outlined.MenuBook), Tab("progress", "Progress", Icons.Outlined.Insights)) }
    LaunchedEffect(state.notice?.id) {
        state.notice?.let { notice -> snackbar.showSnackbar(notice.text); viewModel.clearNotice(notice.id) }
    }
    fun navigateTab(destination: String) {
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(modifier = Modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(if (detail) "Book details" else "shelfwise.", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            navigationIcon = { if (detail) IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { if (state.saving) CircularProgressIndicator(Modifier.padding(16.dp).size(18.dp), strokeWidth = 2.dp) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEach { tab -> NavigationBarItem(selected = route == tab.route, onClick = { navigateTab(tab.route) },
                icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label) }, modifier = Modifier.testTag("tab-${tab.route}")) }
        } }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        NavHost(navController = nav, startDestination = "discover", modifier = Modifier.padding(padding)) {
            composable("discover") { DiscoverScreen(state.library, { nav.navigate("book/$it") }, viewModel::toggleSaved) }
            composable("shelf") { ShelfScreen(state.library, { nav.navigate("book/$it") }, viewModel::toggleSaved, { navigateTab("discover") }) }
            composable("progress") { ProgressScreen(state.library, viewModel::setGoal, { nav.navigate("book/$it") }, viewModel::toggleSaved) }
            composable("book/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType })) { backStack ->
                val id = backStack.arguments?.getString("bookId").orEmpty()
                val book = Catalog.book(id)
                if (book == null) EmptyState("Book not found", "Return to the collection to choose another book.", "Discover", { navigateTab("discover") })
                else BookDetailsScreen(book, state.library.record(id), { viewModel.borrow(id) }, { viewModel.returnBook(id) },
                    { viewModel.toggleSaved(id) }, { viewModel.setPages(id, it) }, { viewModel.addPages(id, 10) }, { viewModel.saveNote(id, it) })
            }
        }
    }
}
