package app.defide.ui.bible

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleHomeScreen(
    onBookSelected: (String, Int) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: BibleViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsState()
    val translationId by viewModel.selectedTranslationId.collectAsState()

    val otBooks = books.filter { it.testament == "OT" || it.testament == "DC" }
    val ntBooks = books.filter { it.testament == "NT" }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Bible") },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
        )
    }) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (otBooks.isNotEmpty()) {
                item {
                    Text(
                        text = "Old Testament",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    HorizontalDivider()
                }
                items(otBooks) { book ->
                    Text(
                        text = book.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookSelected(translationId, book.bookNumber) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
            }
            if (ntBooks.isNotEmpty()) {
                item {
                    Text(
                        text = "New Testament",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    HorizontalDivider()
                }
                items(ntBooks) { book ->
                    Text(
                        text = book.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookSelected(translationId, book.bookNumber) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BibleChapterScreen(
    translationId: String,
    bookNumber: Int,
    onChapterSelected: (String, Int, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: BibleViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsState()
    val chapterCount by viewModel.chapterCount.collectAsState()
    val book = books.firstOrNull { it.bookNumber == bookNumber }

    LaunchedEffect(books, bookNumber) {
        books.firstOrNull { it.bookNumber == bookNumber }?.let { viewModel.loadChapterCount(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.fullName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(chapterCount) { i ->
                OutlinedCard(
                    modifier = Modifier.clickable {
                        onChapterSelected(translationId, bookNumber, i + 1)
                    },
                ) {
                    Text(
                        text = "${i + 1}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    translationId: String,
    bookNumber: Int,
    chapter: Int,
    scrollToVerse: Int = 1,
    onBack: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    viewModel: BibleViewModel = hiltViewModel(),
) {
    val verses by viewModel.verses.collectAsState()
    val books by viewModel.books.collectAsState()
    val chapterCount by viewModel.chapterCount.collectAsState()
    val book = books.firstOrNull { it.bookNumber == bookNumber }
    val listState = rememberLazyListState()

    LaunchedEffect(book?.id, chapter) {
        book?.let { viewModel.loadVerses(it.id, chapter) }
    }
    LaunchedEffect(verses.firstOrNull()?.id, scrollToVerse) {
        if (verses.isNotEmpty() && scrollToVerse > 1) {
            val idx = verses.indexOfFirst { it.verse >= scrollToVerse }.coerceAtLeast(0)
            listState.scrollToItem(idx)
        }
    }

    val verseNumberColor = MaterialTheme.colorScheme.primary
    val verseNumberStyle = remember(verseNumberColor) {
        androidx.compose.ui.text.SpanStyle(color = verseNumberColor, fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp))
    }
    val bodyStyle = MaterialTheme.typography.bodyLarge

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${book?.fullName ?: ""} $chapter") },
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(verses, key = { it.id }, contentType = { "verse" }) { verse ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(verseNumberStyle) { append("${verse.verse}  ") }
                            append(verse.text)
                        },
                        style = bodyStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    HorizontalDivider()
                }
            }

            if (chapter > 1) {
                SmallFloatingActionButton(
                    onClick = onPrevChapter,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous chapter")
                }
            }
            if (chapterCount > 0 && chapter < chapterCount) {
                SmallFloatingActionButton(
                    onClick = onNextChapter,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next chapter")
                }
            }
        }
    }
}
