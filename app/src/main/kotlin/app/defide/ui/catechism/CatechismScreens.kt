package app.defide.ui.catechism

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
fun CatechismHomeScreen(
    onSectionSelected: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: CatechismViewModel = hiltViewModel(),
) {
    val parts by viewModel.parts.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var selectedPart by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Catechism") },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.search(it) },
                placeholder = { Text("Search CCC…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )

            if (searchQuery.isNotBlank()) {
                LazyColumn {
                    items(searchResults) { section ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSectionSelected(section.id) }
                                .padding(16.dp),
                        ) {
                            Text("CCC ${section.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            section.heading?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                            Text(section.body.take(120) + "…", style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider()
                    }
                }
            } else if (parts.isNotEmpty() && selectedPart == null) {
                LazyColumn {
                    items(parts) { part ->
                        Text(
                            text = "Part $part",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPart = part
                                    viewModel.loadSections(part)
                                }
                                .padding(16.dp),
                        )
                        HorizontalDivider()
                    }
                }
            } else if (parts.isNotEmpty() && selectedPart != null) {
                LazyColumn {
                    item {
                        Text(
                            text = "← Parts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { selectedPart = null }
                                .padding(16.dp),
                        )
                    }
                    items(sections) { section ->
                        SectionRow(section, onSectionSelected)
                    }
                }
            } else {
                // No hierarchy data — flat browse
                LazyColumn {
                    items(sections) { section ->
                        SectionRow(section, onSectionSelected)
                    }
                    item {
                        if (sections.isNotEmpty()) {
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.loadMore(sections.size) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Load more…") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionRow(section: app.defide.data.model.CccSection, onClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(section.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text("CCC ${section.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        section.heading?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            ?: Text(section.body.take(80) + "…", style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatechismDetailScreen(
    sectionId: Int,
    onNavigateTo: (Int) -> Unit,
    onPrevSection: (Int) -> Unit,
    onNextSection: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: CatechismViewModel = hiltViewModel(),
) {
    val section by viewModel.detail.collectAsState()
    val prevId by viewModel.prevSectionId.collectAsState()
    val nextId by viewModel.nextSectionId.collectAsState()
    LaunchedEffect(sectionId) { viewModel.loadDetail(sectionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CCC $sectionId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            ) {
                item {
                    section?.heading?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                    section?.body?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }

            prevId?.let { id ->
                SmallFloatingActionButton(
                    onClick = { onPrevSection(id) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous paragraph")
                }
            }
            nextId?.let { id ->
                SmallFloatingActionButton(
                    onClick = { onNextSection(id) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next paragraph")
                }
            }
        }
    }
}
