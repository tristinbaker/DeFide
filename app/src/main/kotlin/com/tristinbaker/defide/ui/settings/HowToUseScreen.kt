package com.tristinbaker.defide.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class HowToSection(val title: String, val steps: List<String>)

private val sections = listOf(
    HowToSection(
        title = "Bible",
        steps = listOf(
            "Open the Bible from the drawer and select a translation in Settings.",
            "Tap a book, then a chapter to start reading.",
            "Press the → button at the bottom right to go to the next chapter. This also marks the current chapter as read.",
            "Chapters you've read appear as filled chips in the chapter selector. Long-press a chip to unmark it, or use the ⋮ menu to reset all progress for a book.",
            "Long-press any verse to bookmark it or apply a highlight color.",
            "Use the search bar at the top of the Bible screen to search across the full text of the selected translation.",
            "View all bookmarks with the Bookmarks button in the top bar.",
        ),
    ),
    HowToSection(
        title = "Rosary",
        steps = listOf(
            "Open Rosary from the drawer or tap Pray the Rosary on the Home screen.",
            "Select a mystery set to begin a guided session.",
            "Swipe or tap the arrow buttons to move through each bead.",
            "Scripture references for each mystery are shown below the prayer text. Tap them to open the verse in the Bible.",
            "Completing a session extends your Rosary streak on the Home screen.",
        ),
    ),
    HowToSection(
        title = "Prayers",
        steps = listOf(
            "Open Prayers from the drawer to browse or search traditional Catholic prayers.",
            "Tap any prayer to read it in full.",
            "Use the search bar to find prayers by title, text, or tag.",
        ),
    ),
    HowToSection(
        title = "Novenas",
        steps = listOf(
            "Open Novenas from the drawer and select a novena to begin.",
            "Tap Begin Novena to start — you'll go straight to Day 1.",
            "Return each day and tap Continue to pray the next day's prayer.",
            "View active novenas under My Novenas. Tap the trash icon to abandon one.",
        ),
    ),
    HowToSection(
        title = "Streaks",
        steps = listOf(
            "Your Bible and Rosary streaks appear as widgets on the Home screen.",
            "The Bible streak counts consecutive days where you've read at least N chapters (set N in Settings → Bible Streak).",
            "The Rosary streak counts consecutive days where you completed at least one Rosary session.",
            "A streak resets if you miss a day.",
        ),
    ),
    HowToSection(
        title = "Settings",
        steps = listOf(
            "Change the app theme (Light, Dark, or System) under Appearance.",
            "Switch Bible translations under Bible Translation.",
            "Set your Bible streak goal (chapters per day) under Bible Streak.",
            "Enable daily novena reminders under Notifications.",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Use") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            sections.forEachIndexed { index, section ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        section.steps.forEachIndexed { i, step ->
                            Text(
                                text = "${i + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                    }
                    if (index < sections.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
