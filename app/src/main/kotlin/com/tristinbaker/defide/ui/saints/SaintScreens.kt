package com.tristinbaker.defide.ui.saints

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaintsListScreen(
    onBack: () -> Unit,
    onSaintClick: (saintId: String) -> Unit,
    viewModel: SaintsViewModel = hiltViewModel(),
) {
    val saints by viewModel.saints.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val favorites = saints.filter { it.id in favoriteIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saints_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.saints_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = sortOrder == SaintSortOrder.NAME,
                        onClick = { viewModel.setSortOrder(SaintSortOrder.NAME) },
                        label = { Text(stringResource(R.string.saints_sort_name)) },
                    )
                    FilterChip(
                        selected = sortOrder == SaintSortOrder.FEAST_DATE,
                        onClick = { viewModel.setSortOrder(SaintSortOrder.FEAST_DATE) },
                        label = { Text(stringResource(R.string.saints_sort_feast)) },
                    )
                }
            }

            if (favorites.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.saints_favorites_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(favorites.sortedBy { it.name }, key = { "fav_${it.id}" }) { saint ->
                    SaintListItem(
                        saint = saint,
                        isFavorite = true,
                        onFavoriteToggle = { viewModel.toggleFavorite(saint.id) },
                        onClick = { onSaintClick(saint.id) },
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            items(saints, key = { it.id }) { saint ->
                SaintListItem(
                    saint = saint,
                    isFavorite = saint.id in favoriteIds,
                    onFavoriteToggle = { viewModel.toggleFavorite(saint.id) },
                    onClick = { onSaintClick(saint.id) },
                )
            }

            if (saints.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.saints_not_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaintListItem(
    saint: com.tristinbaker.defide.data.model.Saint,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaintAvatar(name = saint.name, category = saint.category)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = saint.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            saint.feastDate?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = saint.shortBio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        IconButton(onClick = onFavoriteToggle) {
            if (isFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}

@Composable
private fun SaintAvatar(name: String, category: String) {
    val bgColor = when (category) {
        "martyr"    -> MaterialTheme.colorScheme.errorContainer
        "apostle"   -> MaterialTheme.colorScheme.primaryContainer
        "doctor"    -> MaterialTheme.colorScheme.secondaryContainer
        "virgin"    -> MaterialTheme.colorScheme.surfaceVariant
        else        -> MaterialTheme.colorScheme.primaryContainer
    }
    val fgColor = when (category) {
        "martyr"    -> MaterialTheme.colorScheme.onErrorContainer
        "apostle"   -> MaterialTheme.colorScheme.onPrimaryContainer
        "doctor"    -> MaterialTheme.colorScheme.onSecondaryContainer
        "virgin"    -> MaterialTheme.colorScheme.onSurfaceVariant
        else        -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.first().uppercaseChar().toString(),
            style = MaterialTheme.typography.titleMedium,
            color = fgColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaintDetailScreen(
    saintId: String,
    language: String,
    onBack: () -> Unit,
    viewModel: SaintsViewModel = hiltViewModel(),
) {
    val saint by viewModel.selectedSaint.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    LaunchedEffect(saintId) { viewModel.loadSaint(saintId, language) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(saint?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    saint?.let { s ->
                        IconButton(onClick = { viewModel.toggleFavorite(s.id) }) {
                            if (s.id in favoriteIds) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        saint?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SaintAvatar(name = s.name, category = s.category)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = s.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        s.feastDate?.let { date ->
                            Text(
                                text = "${stringResource(R.string.saints_feast_date_label)}: $date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                s.patronage?.let { patronage ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.saints_patronage_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = patronage,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = s.fullBio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
