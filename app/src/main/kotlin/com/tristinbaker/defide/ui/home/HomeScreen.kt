package com.tristinbaker.defide.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onPrayRosary: (String) -> Unit,
    onVerseClicked: (translationId: String, bookNumber: Int, chapter: Int, verse: Int) -> Unit,
    onSaintClicked: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val verseOfDay by viewModel.verseOfDay.collectAsState()
    val todaysMystery by viewModel.todaysMystery.collectAsState()
    val saintOfDay by viewModel.saintOfDay.collectAsState()
    val bibleStreak by viewModel.bibleStreak.collectAsState()
    val rosaryStreak by viewModel.rosaryStreak.collectAsState()
    val sinHabits by viewModel.sinHabits.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var habitDialogTarget by remember { mutableStateOf<SinHabitUi?>(null) }

    val today = LocalDate.now()
    val dateLocale = Locale.forLanguageTag(appLanguage)
    val datePattern = if (appLanguage.startsWith("pt")) "EEEE, d 'de' MMMM" else "EEEE, MMMM d"
    val dateText = today.format(DateTimeFormatter.ofPattern(datePattern, dateLocale))
        .split(", ")
        .joinToString(", ") { part ->
            val words = part.split(" ")
            words.mapIndexed { i, w ->
                if (i == 0 || i == words.lastIndex) w.replaceFirstChar { it.uppercase() }
                else w
            }.joinToString(" ")
        }
    val yearText = today.format(DateTimeFormatter.ofPattern("yyyy", dateLocale))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            )
                        )
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
            ) {
                Column {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = yearText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(16.dp))

                // Verse of the day
                SectionCard(
                    title = stringResource(R.string.verse_of_the_day),
                    onClick = verseOfDay?.let { v ->
                        { onVerseClicked(v.translationId, v.bookNumber, v.chapter, v.verse) }
                    },
                ) {
                    if (verseOfDay != null) {
                        Text(
                            text = "\u201C${verseOfDay!!.text}\u201D",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "— ${verseOfDay!!.reference}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    } else {
                        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Today's mystery
                SectionCard(title = stringResource(R.string.todays_rosary)) {
                    if (todaysMystery != null) {
                        Text(
                            text = todaysMystery!!.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        todaysMystery!!.traditionalDays?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onPrayRosary(todaysMystery!!.id) }) {
                            Text(stringResource(R.string.pray_the_rosary))
                        }
                    } else {
                        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                saintOfDay?.let { saint ->
                    Spacer(Modifier.height(12.dp))
                    SectionCard(
                        title = stringResource(R.string.saint_of_the_day),
                        onClick = { onSaintClicked(saint.id) },
                    ) {
                        Text(
                            text = saint.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        saint.feastDate?.let { date ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Streaks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StreakCard(label = stringResource(R.string.bible_streak), days = bibleStreak, modifier = Modifier.weight(1f))
                    StreakCard(label = stringResource(R.string.rosary_streak), days = rosaryStreak, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                // Overcoming Sin
                Text(
                    text = stringResource(R.string.sin_streak_section).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
                )
                Spacer(Modifier.height(8.dp))
                sinHabits.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { habit ->
                            StreakCard(
                                label = habit.name,
                                days = habit.streak,
                                modifier = Modifier.weight(1f),
                                onClick = { habitDialogTarget = habit },
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedButton(
                    onClick = { showAddHabitDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.sin_streak_add))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddHabitDialog) {
        var habitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text(stringResource(R.string.sin_streak_add)) },
            text = {
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    placeholder = { Text(stringResource(R.string.sin_streak_add_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addSinHabit(habitName)
                    showAddHabitDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    habitDialogTarget?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitDialogTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(habit.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        viewModel.removeSinHabit(habit.id)
                        habitDialogTarget = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            },
            text = { Text(stringResource(R.string.sin_streak_relapse_prompt)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.logSinRelapse(habit.id)
                    habitDialogTarget = null
                }) { Text(stringResource(R.string.sin_streak_relapse_button)) }
            },
            dismissButton = {
                TextButton(onClick = { habitDialogTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StreakCard(label: String, days: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (days > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = pluralStringResource(R.plurals.days, days, days),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (days > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

