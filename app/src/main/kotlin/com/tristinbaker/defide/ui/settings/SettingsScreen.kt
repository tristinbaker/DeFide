package com.tristinbaker.defide.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tristinbaker.defide.data.preferences.AppFont
import com.tristinbaker.defide.data.preferences.AppLockTimeout
import com.tristinbaker.defide.data.preferences.AppRite
import com.tristinbaker.defide.data.preferences.AppTheme
import com.tristinbaker.defide.data.model.Translation
import com.tristinbaker.defide.data.preferences.BackupFrequency
import com.tristinbaker.defide.data.preferences.RosaryOrder
import com.tristinbaker.defide.ui.applock.APP_LOCK_AUTHENTICATORS

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
    var showRosaryNotificationDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showNotificationDialog = true
    }
    val rosaryNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showRosaryNotificationDialog = true
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.backup(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restore(uri)
    }
    val autoBackupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.setAutoBackupFolder(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.backupMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
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

    fun requestRosaryNotificationPermissionThenShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                showRosaryNotificationDialog = true
            } else {
                rosaryNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            showRosaryNotificationDialog = true
        }
    }

    if (showNotificationDialog) {
        NotificationTimeDialog(
            title = stringResource(R.string.novena_reminder_dialog_title),
            description = stringResource(R.string.novena_reminder_dialog_desc),
            current = prefs.novenaNotificationTime,
            onConfirm = { time ->
                viewModel.setNovenaNotificationTime(time)
                showNotificationDialog = false
            },
            onDismiss = { showNotificationDialog = false },
        )
    }

    if (showRosaryNotificationDialog) {
        NotificationTimeDialog(
            title = stringResource(R.string.rosary_reminder_dialog_title),
            description = stringResource(R.string.rosary_reminder_dialog_desc),
            current = prefs.rosaryNotificationTime,
            onConfirm = { time ->
                viewModel.setRosaryNotificationTime(time)
                showRosaryNotificationDialog = false
            },
            onDismiss = { showRosaryNotificationDialog = false },
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
                    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    AppTheme.entries
                        .filter { it != AppTheme.DYNAMIC || supportsDynamic }
                        .forEach { theme ->
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
                                        AppTheme.SYSTEM   -> R.string.theme_system
                                        AppTheme.LIGHT    -> R.string.theme_light
                                        AppTheme.DARK     -> R.string.theme_dark
                                        AppTheme.AMOLED   -> R.string.theme_amoled
                                        AppTheme.DYNAMIC  -> R.string.theme_dynamic
                                    }),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.keep_screen_on_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.keepScreenOn,
                        onCheckedChange = { viewModel.setKeepScreenOn(it) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.full_screen_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.fullScreenMode,
                        onCheckedChange = { viewModel.setFullScreenMode(it) },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_security))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_lock_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.appLockEnabled,
                        onCheckedChange = { enabled ->
                            val canAuthenticate = BiometricManager.from(context)
                                .canAuthenticate(APP_LOCK_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
                            if (enabled && !canAuthenticate) {
                                Toast.makeText(context, R.string.app_lock_unavailable, Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.setAppLockEnabled(enabled)
                            }
                        },
                    )
                }
                if (prefs.appLockEnabled) {
                    Text(
                        stringResource(R.string.app_lock_timeout_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    val timeoutLabels = mapOf(
                        AppLockTimeout.IMMEDIATELY to stringResource(R.string.app_lock_immediately),
                        AppLockTimeout.ONE_MINUTE to stringResource(R.string.app_lock_one_minute),
                        AppLockTimeout.FIVE_MINUTES to stringResource(R.string.app_lock_five_minutes),
                        AppLockTimeout.FIFTEEN_MINUTES to stringResource(R.string.app_lock_fifteen_minutes),
                    )
                    var timeoutDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = timeoutDropdownExpanded,
                        onExpandedChange = { timeoutDropdownExpanded = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        OutlinedTextField(
                            value = timeoutLabels.getValue(prefs.appLockTimeout),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = timeoutDropdownExpanded,
                            onDismissRequest = { timeoutDropdownExpanded = false },
                        ) {
                            AppLockTimeout.entries.forEach { timeout ->
                                DropdownMenuItem(
                                    text = { Text(timeoutLabels.getValue(timeout)) },
                                    onClick = {
                                        viewModel.setAppLockTimeout(timeout)
                                        timeoutDropdownExpanded = false
                                    },
                                )
                            }
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
                    "en"    to stringResource(R.string.lang_en),
                    "es"    to stringResource(R.string.lang_es),
                    "fr"    to stringResource(R.string.lang_fr),
                    "lt"    to stringResource(R.string.lang_lt),
                    "pt-BR" to stringResource(R.string.lang_pt_BR),
                    "pt-PT" to stringResource(R.string.lang_pt_PT),
                    "zh-CN" to stringResource(R.string.lang_zh_CN),
                    "it"    to stringResource(R.string.lang_it),
                    "la"    to stringResource(R.string.lang_la),
                )
                var languageDropdownExpanded by remember { mutableStateOf(false) }
                val selectedLanguageLabel = languages.find { it.first == prefs.appLanguage }?.second
                    ?: prefs.appLanguage
                ExposedDropdownMenuBox(
                    expanded = languageDropdownExpanded,
                    onExpandedChange = { languageDropdownExpanded = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    OutlinedTextField(
                        value = selectedLanguageLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false },
                    ) {
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setAppLanguage(code)
                                    languageDropdownExpanded = false
                                },
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
                SectionHeader(stringResource(R.string.section_rite))
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AppRite.entries.forEach { rite ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = prefs.appRite == rite,
                                onClick = { viewModel.setAppRite(rite) },
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = stringResource(when (rite) {
                                        AppRite.MODERN      -> R.string.rite_modern
                                        AppRite.TRADITIONAL -> R.string.rite_traditional
                                        AppRite.LATIN       -> R.string.rite_latin
                                    }),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = stringResource(when (rite) {
                                        AppRite.MODERN      -> R.string.rite_modern_desc
                                        AppRite.TRADITIONAL -> R.string.rite_traditional_desc
                                        AppRite.LATIN       -> R.string.rite_latin_desc
                                    }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.rite_explanatory),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionHeader(stringResource(R.string.section_bible_translation))
            }
            item {
                // Content-level decision: which translations are relevant per language
                val translations = when (prefs.appLanguage) {
                    "es" -> listOf(
                        Triple("platense", "Biblia Platense (Straubinger)", "Traducción católica de Juan Straubinger"),
                        Triple("vulgate",  "Vulgata Latina",               "Texto latino original de San Jerónimo"),
                    )
                    "pt-BR" -> listOf(
                        Triple("ave-maria", "Bíblia Ave-Maria",             "Tradução católica brasileira"),
                        Triple("vulgate",   "Vulgata Latina",               "Texto latino original de São Jerônimo"),
                    )
                    "pt-PT" -> listOf(
                        Triple("porcap",   "Bíblia dos Capuchinhos",        "Tradução da Difusora Bíblica (Capuchinhos)"),
                        Triple("vulgate",  "Vulgata Latina",                "Texto latino original de São Jerónimo"),
                    )
                    "fr" -> listOf(
                        Triple("crampon",  "Bible Crampon 1923",            "Traduction catholique française"),
                        Triple("vulgate",  "Vulgate Latine",                "Texte latin original de saint Jérôme"),
                    )
                    "lt" -> listOf(
                        Triple("rk1998",  "Biblija (RK, K1998)",            "Katalikiška lietuviška Biblija"),
                        Triple("vulgate", "Vulgata Latina",                  "Originarinis lotyniškas šv. Jeronimo tekstas"),
                    )
                    "it" -> listOf(
                        Triple("mar", "Bibbia Martini (1782)", "Traduzione cattolica italiana di Antonio Martini"),
                        Triple("vulgate", "Vulgata Latina", "Testo latino originale di San Girolamo"),
                    )
                    "zh-CN" -> listOf(
                        Triple("sg", "思高圣经", "天主教中文圣经译本"),
                    )
                    else -> listOf(
                        Triple("dra",        "Douay-Rheims (1899)",            "Traditional Catholic translation"),
                        Triple("web-c",      "World English Bible (Catholic)", "Modern English, public domain"),
                        Triple("vulgate",    "Latin Vulgate",                  "Original Latin text of St. Jerome"),
                        Triple("vulgate-et", "Latin Vulgate (English)",        "English translation of the Latin Vulgate"),
                    )
                }
                val importedBibles by viewModel.importedBibles.collectAsState()
                val bibleImporting by viewModel.bibleImporting.collectAsState()
                var pendingBibleUri by remember { mutableStateOf<android.net.Uri?>(null) }
                var bibleDeleteCandidate by remember { mutableStateOf<Translation?>(null) }
                val importBibleLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) pendingBibleUri = uri
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
                    importedBibles.forEach { translation ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = prefs.bibleTranslationId == translation.id,
                                onClick = { viewModel.setBibleTranslation(translation.id) },
                            )
                            Column(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f),
                            ) {
                                Text(translation.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.bible_imported_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { bibleDeleteCandidate = translation }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            importBibleLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream")
                            )
                        },
                        enabled = !bibleImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) { Text(stringResource(R.string.bible_import_button)) }
                    if (bibleImporting) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.bible_import_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                pendingBibleUri?.let { uri ->
                    var importName by remember(uri) { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { pendingBibleUri = null },
                        title = { Text(stringResource(R.string.bible_import_name_title)) },
                        text = {
                            OutlinedTextField(
                                value = importName,
                                onValueChange = { importName = it },
                                singleLine = true,
                                label = { Text(stringResource(R.string.bible_import_name_hint)) },
                            )
                        },
                        confirmButton = {
                            TextButton(
                                enabled = importName.isNotBlank(),
                                onClick = {
                                    viewModel.importBible(uri, importName)
                                    pendingBibleUri = null
                                },
                            ) { Text(stringResource(R.string.bible_import_confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingBibleUri = null }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        },
                    )
                }
                bibleDeleteCandidate?.let { translation ->
                    AlertDialog(
                        onDismissRequest = { bibleDeleteCandidate = null },
                        title = { Text(stringResource(R.string.bible_import_delete_title)) },
                        text = { Text(stringResource(R.string.bible_import_delete_message, translation.name)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteImportedBible(translation.id)
                                    bibleDeleteCandidate = null
                                },
                            ) { Text(stringResource(R.string.action_delete)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { bibleDeleteCandidate = null }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionHeader(stringResource(R.string.font_style_label))
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AppFont.entries.forEach { font ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = prefs.appFont == font,
                                onClick = { viewModel.setAppFont(font) },
                            )
                            Text(
                                text = stringResource(when (font) {
                                    AppFont.SERIF      -> R.string.font_serif
                                    AppFont.SYSTEM     -> R.string.font_system
                                    AppFont.SANS_SERIF -> R.string.font_sans_serif
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
                SectionHeader(stringResource(R.string.section_rosary))
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (prefs.appLanguage == "pt-PT") {
                        Text(
                            stringResource(R.string.rosary_order_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        RosaryOrder.entries.forEach { order ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = prefs.rosaryOrder == order,
                                    onClick = { viewModel.setRosaryOrder(order) },
                                )
                                Text(
                                    text = stringResource(when (order) {
                                        RosaryOrder.DOMINICAN -> R.string.rosary_order_dominican
                                        RosaryOrder.FATIMA    -> R.string.rosary_order_fatima
                                    }),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.rosary_haptic_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.rosaryHapticFeedback,
                        onCheckedChange = { viewModel.setRosaryHapticFeedback(it) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.rosary_narration_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.rosaryNarrationEnabled,
                        onCheckedChange = { viewModel.setRosaryNarrationEnabled(it) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.rosary_intention_in_design_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.rosaryIntentionInDesign,
                        onCheckedChange = { viewModel.setRosaryIntentionInDesign(it) },
                    )
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
                val label = if (prefs.rosaryNotificationTime.isNotEmpty())
                    stringResource(R.string.daily_at, prefs.rosaryNotificationTime)
                else
                    stringResource(R.string.status_off)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { requestRosaryNotificationPermissionThenShow() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.rosary_reminder_label), style = MaterialTheme.typography.bodyMedium)
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
                SectionHeader(stringResource(R.string.section_backup))
            }
            item {
                val today = java.time.LocalDate.now().toString().replace("-", "")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { backupLauncher.launch("DeFide_$today.json") },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.backup_label)) }
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.restore_label)) }
                }
            }
            item {
                var frequencyDropdownExpanded by remember { mutableStateOf(false) }
                val frequencyOptions = listOf(
                    BackupFrequency.OFF     to stringResource(R.string.auto_backup_freq_off),
                    BackupFrequency.DAILY   to stringResource(R.string.auto_backup_freq_daily),
                    BackupFrequency.WEEKLY  to stringResource(R.string.auto_backup_freq_weekly),
                    BackupFrequency.MONTHLY to stringResource(R.string.auto_backup_freq_monthly),
                )
                val selectedFrequencyLabel = frequencyOptions.firstOrNull { it.first == prefs.autoBackupFrequency }?.second
                    ?: stringResource(R.string.auto_backup_freq_off)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.auto_backup_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ExposedDropdownMenuBox(
                        expanded = frequencyDropdownExpanded,
                        onExpandedChange = { frequencyDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedFrequencyLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.auto_backup_frequency_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = frequencyDropdownExpanded,
                            onDismissRequest = { frequencyDropdownExpanded = false },
                        ) {
                            frequencyOptions.forEach { (freq, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setAutoBackupFrequency(freq)
                                        frequencyDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (prefs.autoBackupFrequency != BackupFrequency.OFF) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val folderLabel = if (prefs.autoBackupFolderUri.isNotEmpty())
                            stringResource(R.string.auto_backup_folder_set)
                        else
                            stringResource(R.string.auto_backup_folder_none)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.auto_backup_folder_label), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    folderLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { autoBackupFolderLauncher.launch(null) }) {
                                Text(stringResource(R.string.auto_backup_choose_folder))
                            }
                        }
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
                    val context = LocalContext.current
                    val versionName = remember {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }
                    val uriHandler = LocalUriHandler.current
                    Text(
                        "Version $versionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/tristinbaker/DeFide")
                        },
                    )
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
    title: String,
    description: String,
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHour = current.split(":").firstOrNull()?.toIntOrNull() ?: 8
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    description,
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
