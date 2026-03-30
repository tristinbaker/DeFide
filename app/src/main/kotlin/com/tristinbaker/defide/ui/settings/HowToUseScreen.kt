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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tristinbaker.defide.R

private data class HowToSection(val title: String, val steps: List<String>)

@Composable
private fun rememberHowToSections() = listOf(
    HowToSection(
        title = stringResource(R.string.nav_bible),
        steps = listOf(
            stringResource(R.string.htu_bible_1),
            stringResource(R.string.htu_bible_2),
            stringResource(R.string.htu_bible_3),
            stringResource(R.string.htu_bible_4),
            stringResource(R.string.htu_bible_5),
            stringResource(R.string.htu_bible_6),
            stringResource(R.string.htu_bible_7),
        ),
    ),
    HowToSection(
        title = stringResource(R.string.nav_rosary),
        steps = listOf(
            stringResource(R.string.htu_rosary_1),
            stringResource(R.string.htu_rosary_2),
            stringResource(R.string.htu_rosary_3),
            stringResource(R.string.htu_rosary_4),
            stringResource(R.string.htu_rosary_5),
        ),
    ),
    HowToSection(
        title = stringResource(R.string.nav_prayers),
        steps = listOf(
            stringResource(R.string.htu_prayers_1),
            stringResource(R.string.htu_prayers_2),
            stringResource(R.string.htu_prayers_3),
        ),
    ),
    HowToSection(
        title = stringResource(R.string.nav_novenas),
        steps = listOf(
            stringResource(R.string.htu_novenas_1),
            stringResource(R.string.htu_novenas_2),
            stringResource(R.string.htu_novenas_3),
            stringResource(R.string.htu_novenas_4),
        ),
    ),
    HowToSection(
        title = stringResource(R.string.section_bible_streak),
        steps = listOf(
            stringResource(R.string.htu_streaks_1),
            stringResource(R.string.htu_streaks_2),
            stringResource(R.string.htu_streaks_3),
            stringResource(R.string.htu_streaks_4),
        ),
    ),
    HowToSection(
        title = stringResource(R.string.nav_settings),
        steps = listOf(
            stringResource(R.string.htu_settings_1),
            stringResource(R.string.htu_settings_2),
            stringResource(R.string.htu_settings_3),
            stringResource(R.string.htu_settings_4),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onBack: () -> Unit) {
    val sections = rememberHowToSections()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.how_to_use_label)) },
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
