package app.defide.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.defide.ui.bible.BibleBookmarksScreen
import app.defide.ui.bible.BibleChapterScreen
import app.defide.ui.bible.BibleHomeScreen
import app.defide.ui.bible.BibleReaderScreen
import app.defide.ui.home.HomeScreen
import app.defide.ui.novena.NovenaDetailScreen
import app.defide.ui.novena.NovenaListScreen
import app.defide.ui.novena.NovenaProgressScreen
import app.defide.ui.novena.NovenaSessionScreen
import app.defide.ui.prayers.PrayerDetailScreen
import app.defide.ui.prayers.PrayerSearchScreen
import app.defide.ui.bible.BibleViewModel
import app.defide.ui.rosary.RosaryHomeScreen
import app.defide.ui.rosary.RosarySessionScreen
import app.defide.ui.settings.HowToUseScreen
import app.defide.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class DrawerItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val drawerItems = listOf(
    DrawerItem("home",      "Home",      Icons.Default.Home),
    DrawerItem("rosary",    "Rosary",    Icons.Default.Circle),
    DrawerItem("bible",     "Bible",     Icons.Default.MenuBook),
    DrawerItem("catechism", "Catechism", Icons.AutoMirrored.Filled.LibraryBooks),
    DrawerItem("prayers",   "Prayers",   Icons.Default.Star),
    DrawerItem("novena",    "Novenas",   Icons.Default.Book),
)

private const val CCC_URL = "https://usccb.cld.bz/Catechism-of-the-Catholic-Church2/7"

@Composable
fun DeFideApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCatechismDialog by remember { mutableStateOf(false) }

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    if (showCatechismDialog) {
        AlertDialog(
            onDismissRequest = { showCatechismDialog = false },
            title = { Text("Open Catechism") },
            text = { Text("This will open the Catechism of the Catholic Church on the USCCB website in your browser.") },
            confirmButton = {
                TextButton(onClick = {
                    showCatechismDialog = false
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CCC_URL)))
                }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showCatechismDialog = false }) { Text("Cancel") }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "De Fide",
                    modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 16.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            closeDrawer()
                            if (item.route == "catechism") {
                                showCatechismDialog = true
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        closeDrawer()
                        navController.navigate("settings")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold { padding ->
            DeFideNavHost(
                navController = navController,
                openDrawer = openDrawer,
                padding = padding,
            )
        }
    }
}

/** Parses "Luke 1:26–38" → (bookName="Luke", chapter=1, verse=26), or null. */
private fun parseScripture(ref: String): Triple<String, Int, Int>? {
    val match = Regex("""^(.+?)\s+(\d+):(\d+)""").find(ref.trim()) ?: return null
    return Triple(
        match.groupValues[1].trim(),
        match.groupValues[2].toIntOrNull() ?: return null,
        match.groupValues[3].toIntOrNull() ?: return null,
    )
}

@Composable
private fun DeFideNavHost(
    navController: NavHostController,
    openDrawer: () -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    // Shared BibleViewModel for scripture → Bible navigation (books read lazily, not collected as state)
    val bibleViewModel: BibleViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val navScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.padding(padding),
    ) {
        // Home
        composable("home") {
            HomeScreen(
                onOpenDrawer = openDrawer,
                onPrayRosary = { mysteryId -> navController.navigate("rosary/session/$mysteryId") },
                onVerseClicked = { translationId, bookNumber, chapter, verse ->
                    navController.navigate("bible/$translationId/book/$bookNumber/chapter/$chapter?verse=$verse")
                },
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onHowToUse = { navController.navigate("how_to_use") },
            )
        }
        composable("how_to_use") {
            HowToUseScreen(onBack = { navController.popBackStack() })
        }

        // Rosary
        composable("rosary") {
            RosaryHomeScreen(
                onStartSession = { mysteryId -> navController.navigate("rosary/session/$mysteryId") },
                onOpenDrawer = openDrawer,
            )
        }
        composable(
            "rosary/session/{mysteryId}",
            arguments = listOf(navArgument("mysteryId") { type = NavType.StringType }),
        ) { backStack ->
            RosarySessionScreen(
                mysteryId = backStack.arguments?.getString("mysteryId") ?: "",
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() },
                onScriptureClicked = { ref ->
                    val parsed = parseScripture(ref) ?: return@RosarySessionScreen
                    val (bookName, chapter, verse) = parsed
                    navScope.launch {
                        val book = bibleViewModel.books.first { it.isNotEmpty() }.find {
                            it.fullName.equals(bookName, ignoreCase = true) ||
                            it.shortName.equals(bookName, ignoreCase = true) ||
                            it.drName.equals(bookName, ignoreCase = true)
                        } ?: return@launch
                        navController.navigate("bible/dra/book/${book.bookNumber}/chapter/$chapter?verse=$verse")
                    }
                },
            )
        }

        // Bible
        composable("bible") {
            BibleHomeScreen(
                onBookSelected = { translationId, bookNumber ->
                    navController.navigate("bible/$translationId/book/$bookNumber")
                },
                onBookmarksSelected = { navController.navigate("bible/bookmarks") },
                onOpenDrawer = openDrawer,
            )
        }
        composable("bible/bookmarks") {
            BibleBookmarksScreen(
                onBookmarkSelected = { translationId, bookNumber, chapter, verse ->
                    navController.navigate("bible/$translationId/book/$bookNumber/chapter/$chapter?verse=$verse")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "bible/{translationId}/book/{bookNumber}",
            arguments = listOf(
                navArgument("translationId") { type = NavType.StringType },
                navArgument("bookNumber") { type = NavType.IntType },
            ),
        ) { backStack ->
            BibleChapterScreen(
                translationId = backStack.arguments?.getString("translationId") ?: "",
                bookNumber = backStack.arguments?.getInt("bookNumber") ?: 1,
                onChapterSelected = { translationId, bookNumber, chapter ->
                    navController.navigate("bible/$translationId/book/$bookNumber/chapter/$chapter")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "bible/{translationId}/book/{bookNumber}/chapter/{chapter}?verse={verse}",
            arguments = listOf(
                navArgument("translationId") { type = NavType.StringType },
                navArgument("bookNumber") { type = NavType.IntType },
                navArgument("chapter") { type = NavType.IntType },
                navArgument("verse") { type = NavType.IntType; defaultValue = 1 },
            ),
        ) { backStack ->
            val translationId = backStack.arguments?.getString("translationId") ?: ""
            val bookNumber = backStack.arguments?.getInt("bookNumber") ?: 1
            val chapter = backStack.arguments?.getInt("chapter") ?: 1
            val verse = backStack.arguments?.getInt("verse") ?: 1
            BibleReaderScreen(
                translationId = translationId,
                bookNumber = bookNumber,
                chapter = chapter,
                scrollToVerse = verse,
                onBack = { navController.popBackStack() },
                onPrevChapter = {
                    navController.navigate("bible/$translationId/book/$bookNumber/chapter/${chapter - 1}") {
                        popUpTo("bible/$translationId/book/$bookNumber/chapter/$chapter") { inclusive = true }
                    }
                },
                onNextChapter = {
                    navController.navigate("bible/$translationId/book/$bookNumber/chapter/${chapter + 1}") {
                        popUpTo("bible/$translationId/book/$bookNumber/chapter/$chapter") { inclusive = true }
                    }
                },
            )
        }

        // Prayers
        composable("prayers") {
            PrayerSearchScreen(
                onPrayerSelected = { id -> navController.navigate("prayers/$id") },
                onOpenDrawer = openDrawer,
            )
        }
        composable(
            "prayers/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStack ->
            PrayerDetailScreen(
                prayerId = backStack.arguments?.getString("id") ?: "",
                onBack = { navController.popBackStack() },
            )
        }

        // Novenas — "novena/progress" before "novena/{id}" to avoid collision
        composable("novena") {
            NovenaListScreen(
                onNovenaSelected = { id -> navController.navigate("novena/$id") },
                onProgressSelected = { navController.navigate("novena/progress") },
                onOpenDrawer = openDrawer,
            )
        }
        composable("novena/progress") {
            NovenaProgressScreen(
                onNovenaSelected = { novenaId, progressId ->
                    navController.navigate("novena/$novenaId/session/$progressId")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "novena/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStack ->
            NovenaDetailScreen(
                novenaId = backStack.arguments?.getString("id") ?: "",
                onStartSession = { novenaId, progressId ->
                    navController.navigate("novena/$novenaId/session/$progressId")
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "novena/{novenaId}/session/{progressId}",
            arguments = listOf(
                navArgument("novenaId") { type = NavType.StringType },
                navArgument("progressId") { type = NavType.StringType },
            ),
        ) { backStack ->
            NovenaSessionScreen(
                novenaId = backStack.arguments?.getString("novenaId") ?: "",
                progressId = backStack.arguments?.getString("progressId") ?: "",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
