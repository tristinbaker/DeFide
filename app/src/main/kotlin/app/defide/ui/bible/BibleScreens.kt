package app.defide.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.defide.data.model.Verse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleHomeScreen(
    onBookSelected: (String, Int) -> Unit,
    onBookmarksSelected: () -> Unit,
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
            actions = {
                OutlinedButton(
                    onClick = onBookmarksSelected,
                    modifier = Modifier.padding(end = 8.dp),
                ) { Text("Bookmarks") }
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
                items(otBooks, key = { it.bookNumber }) { book ->
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
                items(ntBooks, key = { it.bookNumber }) { book ->
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
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
    val highlights by viewModel.highlights.collectAsState()
    val book = books.firstOrNull { it.bookNumber == bookNumber }
    val listState = rememberLazyListState()

    var selectedVerse by remember { mutableStateOf<Verse?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(book?.id, chapter) {
        book?.let { viewModel.loadVerses(it.id, chapter) }
    }
    LaunchedEffect(verses.firstOrNull()?.id, scrollToVerse) {
        if (verses.isNotEmpty() && scrollToVerse > 1) {
            val idx = verses.indexOfFirst { it.verse >= scrollToVerse }.coerceAtLeast(0)
            listState.scrollToItem(idx)
        }
    }

    if (selectedVerse != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedVerse = null },
            sheetState = sheetState,
        ) {
            VerseActionsSheet(
                verse = selectedVerse!!,
                bookName = book?.fullName ?: "",
                chapter = chapter,
                currentHighlightColor = highlights[selectedVerse!!.id],
                onBookmark = {
                    viewModel.addBookmark(bookNumber, chapter, selectedVerse!!.verse)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedVerse = null }
                },
                onHighlight = { color ->
                    if (color == null) viewModel.removeHighlight(selectedVerse!!.id)
                    else viewModel.addHighlight(selectedVerse!!.id, color)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedVerse = null }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { selectedVerse = null }
                },
            )
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
                    val highlightColor by remember(verse.id) {
                        derivedStateOf { highlightNameToColor(highlights[verse.id]) }
                    }
                    val annotatedText = remember(verse.id, verseNumberStyle) {
                        buildAnnotatedString {
                            withStyle(verseNumberStyle) { append("${verse.verse}  ") }
                            append(verse.text)
                        }
                    }
                    Text(
                        text = annotatedText,
                        style = bodyStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (highlightColor != null)
                                    Modifier.background(highlightColor!!)
                                else
                                    Modifier
                            )
                            .pointerInput(verse.id) {
                                detectTapGestures(onLongPress = { selectedVerse = verse })
                            }
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

@Composable
private fun VerseActionsSheet(
    verse: Verse,
    bookName: String,
    chapter: Int,
    currentHighlightColor: String?,
    onBookmark: () -> Unit,
    onHighlight: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val highlightColors = listOf(
        "yellow" to Color(0xFFFFF176),
        "green"  to Color(0xFFA5D6A7),
        "blue"   to Color(0xFF90CAF9),
        "pink"   to Color(0xFFF48FB1),
    )

    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
        Text(
            "$bookName $chapter:${verse.verse}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(onClick = onBookmark, modifier = Modifier.fillMaxWidth()) {
            Text("Bookmark")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                clipboardManager.setText(
                    AnnotatedString("$bookName $chapter:${verse.verse}  ${verse.text}")
                )
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Copy verse")
        }
        Spacer(Modifier.height(16.dp))
        Text("Highlight", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            highlightColors.forEach { (name, color) ->
                val isActive = name == currentHighlightColor
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onHighlight(if (isActive) null else name) },
                    ) {}
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active highlight",
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookmarksScreen(
    onBookmarkSelected: (translationId: String, bookNumber: Int, chapter: Int, verse: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: BibleViewModel = hiltViewModel(),
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val books by viewModel.books.collectAsState()
    val bookNameMap = remember(books) { books.associate { it.bookNumber to it.fullName } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
                    )
                    Text(
                        "No bookmarks yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Long-press any verse in the reader to add one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onBookmarkSelected(
                                    bookmark.translationId,
                                    bookmark.bookNumber,
                                    bookmark.chapter,
                                    bookmark.verse,
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${bookNameMap[bookmark.bookNumber] ?: "Book ${bookmark.bookNumber}"} ${bookmark.chapter}:${bookmark.verse}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (!bookmark.note.isNullOrBlank()) {
                                Text(
                                    text = bookmark.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.deleteBookmark(bookmark.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete bookmark",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun highlightNameToColor(name: String?): Color? = when (name) {
    "yellow" -> Color(0x99FFF176)
    "green"  -> Color(0x99A5D6A7)
    "blue"   -> Color(0x9990CAF9)
    "pink"   -> Color(0x99F48FB1)
    else     -> null
}
