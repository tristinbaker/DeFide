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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
            title = { Text("Prayers") },
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
                    placeholder = { Text("Search prayers…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    OutlinedButton(onClick = { tagDropdownExpanded = true }) {
                        Text(selectedTag ?: "Tag", maxLines = 1)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = tagDropdownExpanded,
                        onDismissRequest = { tagDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                selectedTag = null
                                searchQuery = ""
                                viewModel.filterByTag(null)
                                tagDropdownExpanded = false
                            },
                        )
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
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
                        Text(prayer.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    ) {
                        Text("Mark as Prayed")
                    }
                }
            }
        }
    }
}
