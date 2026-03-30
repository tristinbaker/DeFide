package com.tristinbaker.defide.ui.novena

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovenaListScreen(
    onNovenaSelected: (String) -> Unit,
    onProgressSelected: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: NovenaViewModel = hiltViewModel(),
) {
    val novenas by viewModel.novenas.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.novena_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onProgressSelected,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(R.string.novena_my_novenas)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(novenas) { novena ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNovenaSelected(novena.id) }
                        .padding(16.dp),
                ) {
                    Text(novena.title, style = MaterialTheme.typography.titleSmall)
                    novena.feastDay?.let {
                        Text(stringResource(R.string.novena_feast_label, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovenaDetailScreen(
    novenaId: String,
    onStartSession: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: NovenaViewModel = hiltViewModel(),
) {
    val novena by viewModel.detail.collectAsState()
    val progress by viewModel.progress.collectAsState()

    LaunchedEffect(novenaId) {
        viewModel.loadDetail(novenaId)
        viewModel.loadProgress(novenaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(novena?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            novena?.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            novena?.let {
                Text(pluralStringResource(R.plurals.days, it.totalDays, it.totalDays), style = MaterialTheme.typography.bodySmall)
                it.feastDay?.let { day -> Text(stringResource(R.string.novena_feast_traditional, day), style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(24.dp))

            if (progress == null) {
                Button(onClick = { viewModel.startNovena(novenaId) { progressId -> onStartSession(novenaId, progressId) } }) { Text(stringResource(R.string.novena_begin)) }
            } else {
                Button(onClick = { onStartSession(novenaId, progress!!.id) }) {
                    Text(if (progress!!.completed) stringResource(R.string.novena_view) else stringResource(R.string.novena_continue, progress!!.lastCompletedDay + 1))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovenaSessionScreen(
    novenaId: String,
    progressId: String,
    onBack: () -> Unit,
    viewModel: NovenaViewModel = hiltViewModel(),
) {
    val currentDay by viewModel.currentDay.collectAsState()
    val progress by viewModel.progress.collectAsState()

    LaunchedEffect(novenaId, progressId) { viewModel.loadCurrentDay(novenaId, progressId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.novena_day, (progress?.lastCompletedDay ?: 0) + 1)) },
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
                currentDay?.title?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                }
                currentDay?.body?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(24.dp))
                }
                if (progress?.completed == false) {
                    Button(
                        onClick = { viewModel.completeDay(novenaId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.novena_mark_complete)) }
                } else if (progress?.completed == true) {
                    Text(stringResource(R.string.novena_complete_msg), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovenaProgressScreen(
    onNovenaSelected: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: NovenaViewModel = hiltViewModel(),
) {
    val active by viewModel.activeNovenas.collectAsState()
    val completed by viewModel.completedNovenas.collectAsState()
    val titles by viewModel.novenaTitles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.novena_my_novenas)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            if (active.isNotEmpty()) {
                item { Text(stringResource(R.string.novena_in_progress), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp)) }
                items(active) { prog ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNovenaSelected(prog.novenaId, prog.id) }
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(titles[prog.novenaId] ?: prog.novenaId, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.novena_day, prog.lastCompletedDay + 1), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.abandonNovena(prog.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Abandon", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
            }
            if (completed.isNotEmpty()) {
                item { Text(stringResource(R.string.novena_section_completed), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp)) }
                items(completed) { prog ->
                    Text(
                        titles[prog.novenaId] ?: prog.novenaId,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                    HorizontalDivider()
                }
            }
            if (active.isEmpty() && completed.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.novena_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        }
    }
}
