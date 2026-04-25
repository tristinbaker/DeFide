package com.tristinbaker.defide.ui.prayers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R

@Composable
private fun localizedCategory(category: String): String {
    val resId = when (category) {
        "rosary"           -> R.string.category_rosary
        "morning"          -> R.string.category_morning
        "evening"          -> R.string.category_evening
        "marian"           -> R.string.category_marian
        "meals"            -> R.string.category_meals
        "penitential"      -> R.string.category_penitential
        "devotional"       -> R.string.category_devotional
        "saints"           -> R.string.category_saints
        "after-communion"  -> R.string.tag_after_communion
        "anxiety"          -> R.string.tag_anxiety
        "charity"          -> R.string.tag_charity
        "children"         -> R.string.tag_children
        "confession"       -> R.string.tag_confession
        "contrition"       -> R.string.tag_contrition
        "creed"            -> R.string.tag_creed
        "daily"            -> R.string.tag_daily
        "darkness"         -> R.string.tag_darkness
        "dead"             -> R.string.tag_dead
        "death"            -> R.string.tag_death
        "depression"       -> R.string.tag_depression
        "discernment"      -> R.string.tag_discernment
        "eucharist"        -> R.string.tag_eucharist
        "faith"            -> R.string.tag_faith
        "family"           -> R.string.tag_family
        "fathers"          -> R.string.tag_fathers
        "forgiveness"      -> R.string.tag_forgiveness
        "gratitude"        -> R.string.tag_gratitude
        "grief"            -> R.string.tag_grief
        "guardian-angel"   -> R.string.tag_guardian_angel
        "healing"          -> R.string.tag_healing
        "home"             -> R.string.tag_home
        "hope"             -> R.string.tag_hope
        "hospital"         -> R.string.tag_hospital
        "intercession"     -> R.string.tag_intercession
        "loneliness"       -> R.string.tag_loneliness
        "loss"             -> R.string.tag_loss
        "love"             -> R.string.tag_love
        "marriage"         -> R.string.tag_marriage
        "mental-health"    -> R.string.tag_mental_health
        "mercy"            -> R.string.tag_mercy
        "mourning"         -> R.string.tag_mourning
        "night"            -> R.string.tag_night
        "offering"         -> R.string.tag_offering
        "opening"          -> R.string.tag_opening
        "passion"          -> R.string.tag_passion
        "peace"            -> R.string.tag_peace
        "priesthood"       -> R.string.tag_priesthood
        "protection"       -> R.string.tag_protection
        "purgatory"        -> R.string.tag_purgatory
        "religious-life"   -> R.string.tag_religious_life
        "sick"             -> R.string.tag_sick
        "spiritual-warfare"-> R.string.tag_spiritual_warfare
        "st-joseph"        -> R.string.tag_st_joseph
        "suffering"        -> R.string.tag_suffering
        "trust"            -> R.string.tag_trust
        "vocations"        -> R.string.tag_vocations
        "workers"          -> R.string.tag_workers
        else               -> return category.replace('-', ' ')
            .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }
    return stringResource(resId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSearchScreen(
    onPrayerSelected: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: PrayerViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsState()
    val results by viewModel.results.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var tagDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.prayers_title)) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedTag = null
                        viewModel.search(it)
                    },
                    placeholder = { Text(stringResource(R.string.search_prayers_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    OutlinedButton(onClick = { tagDropdownExpanded = true }) {
                        Text(
                            if (selectedTag != null) localizedCategory(selectedTag!!) else stringResource(R.string.tag_label),
                            maxLines = 1,
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = tagDropdownExpanded,
                        onDismissRequest = { tagDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_all)) },
                            onClick = {
                                selectedTag = null
                                searchQuery = ""
                                viewModel.filterByTag(null)
                                tagDropdownExpanded = false
                            },
                        )
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(localizedCategory(tag)) },
                                onClick = {
                                    selectedTag = tag
                                    searchQuery = ""
                                    viewModel.filterByTag(tag)
                                    tagDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            LazyColumn {
                items(results) { prayer ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPrayerSelected(prayer.id) }
                            .padding(16.dp),
                    ) {
                        Text(prayer.title, style = MaterialTheme.typography.titleSmall)
                        Text(localizedCategory(prayer.category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerDetailScreen(
    prayerId: String,
    onBack: () -> Unit,
    viewModel: PrayerViewModel = hiltViewModel(),
) {
    val prayer by viewModel.detail.collectAsState()
    val prayedToday by viewModel.prayedToday.collectAsState()
    LaunchedEffect(prayerId) { viewModel.loadDetail(prayerId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(prayer?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                prayer?.body?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                prayer?.let {
                    Button(
                        onClick = { viewModel.logPrayer(it.id) },
                        modifier = Modifier.padding(top = 24.dp),
                        enabled = !prayedToday,
                    ) {
                        Text(
                            if (prayedToday) stringResource(R.string.prayed_today)
                            else stringResource(R.string.mark_as_prayed)
                        )
                    }
                }
            }
        }
    }
}
