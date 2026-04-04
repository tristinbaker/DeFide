package com.tristinbaker.defide.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R
import com.tristinbaker.defide.data.preferences.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onHowToUse: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    var showNotificationDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showNotificationDialog = true
    }

    fun requestNotificationPermissionThenShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                showNotificationDialog = true
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            showNotificationDialog = true
        }
    }

    if (showNotificationDialog) {
        NotificationTimeDialog(
            current = prefs.novenaNotificationTime,
            onConfirm = { time ->
                viewModel.setNovenaNotificationTime(time)
                showNotificationDialog = false
            },
            onDismiss = { showNotificationDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SectionHeader(stringResource(R.string.section_appearance))
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AppTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = prefs.theme == theme,
                                onClick = { viewModel.setTheme(theme) },
                            )
                            Text(
                                text = stringResource(when (theme) {
                                    AppTheme.SYSTEM -> R.string.theme_system
                                    AppTheme.LIGHT  -> R.string.theme_light
                                    AppTheme.DARK   -> R.string.theme_dark
                                }),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_language))
            }
            item {
                val languages = listOf(
                    "en" to "English",
                    "pt-BR" to "Português (Brasil)",
                    "pt-PT" to "Português (Portugal)",
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    languages.forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = prefs.appLanguage == code,
                                onClick = { viewModel.setAppLanguage(code) },
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                val uriHandler = LocalUriHandler.current
                Text(
                    text = stringResource(R.string.translate_cta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { uriHandler.openUri("https://hosted.weblate.org/projects/de-fide/app-strings/") },
                )
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_bible_translation))
            }
            item {
                // Content-level decision: which translations are relevant per language
                val translations = when (prefs.appLanguage) {
                    "pt-BR" -> listOf(
                        Triple("ave-maria", "Bíblia Ave-Maria",             "Tradução católica brasileira"),
                        Triple("vulgate",   "Vulgata Latina",               "Texto latino original de São Jerônimo"),
                    )
                    "pt-PT" -> listOf(
                        Triple("porcap",   "Bíblia dos Capuchinhos",        "Tradução da Difusora Bíblica (Capuchinhos)"),
                        Triple("vulgate",  "Vulgata Latina",                "Texto latino original de São Jerónimo"),
                    )
                    else -> listOf(
                        Triple("dra",        "Douay-Rheims (1899)",            "Traditional Catholic translation"),
                        Triple("web-c",      "World English Bible (Catholic)", "Modern English, public domain"),
                        Triple("vulgate",    "Latin Vulgate",                  "Original Latin text of St. Jerome"),
                        Triple("vulgate-et", "Latin Vulgate (English)",        "English translation of the Latin Vulgate"),
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    translations.forEach { (id, label, subtitle) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = prefs.bibleTranslationId == id,
                                onClick = { viewModel.setBibleTranslation(id) },
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_notifications))
            }
            item {
                val label = if (prefs.novenaNotificationTime.isNotEmpty())
                    stringResource(R.string.daily_at, prefs.novenaNotificationTime)
                else
                    stringResource(R.string.status_off)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { requestNotificationPermissionThenShow() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.novena_reminder_label), style = MaterialTheme.typography.bodyMedium)
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_bible_streak))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.chapters_per_day_label), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.chapters_per_day_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.FilledIconButton(
                            onClick = { if (prefs.bibleStreakGoal > 1) viewModel.setBibleStreakGoal(prefs.bibleStreakGoal - 1) },
                            enabled = prefs.bibleStreakGoal > 1,
                        ) { Text("−") }
                        Text(
                            text = "${prefs.bibleStreakGoal}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        androidx.compose.material3.FilledIconButton(
                            onClick = { if (prefs.bibleStreakGoal < 10) viewModel.setBibleStreakGoal(prefs.bibleStreakGoal + 1) },
                            enabled = prefs.bibleStreakGoal < 10,
                        ) { Text("+") }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_help))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHowToUse() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.how_to_use_label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
            item {
                SectionHeader(stringResource(R.string.section_about))
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("De Fide", style = MaterialTheme.typography.bodyMedium)
                    Text("Version 1.3.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        stringResource(R.string.about_bible_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimeDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHour = current.split(":").firstOrNull()?.toIntOrNull() ?: 8
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novena_reminder_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.novena_reminder_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = formatHour(selectedHour),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.hour_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        (0..23).forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(formatHour(hour)) },
                                onClick = { selectedHour = hour; expanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:00".format(selectedHour)) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm("") }) { Text(stringResource(R.string.action_turn_off)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun formatHour(hour: Int): String = when {
    hour == 0  -> "12:00 AM (midnight)"
    hour < 12  -> "$hour:00 AM"
    hour == 12 -> "12:00 PM (noon)"
    else       -> "${hour - 12}:00 PM"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}
