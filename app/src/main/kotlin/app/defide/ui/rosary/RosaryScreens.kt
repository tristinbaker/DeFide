package app.defide.ui.rosary

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosaryHomeScreen(
    onStartSession: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: RosaryViewModel = hiltViewModel(),
) {
    val mysteries by viewModel.mysteries.collectAsState()
    val todaysMysteryId = viewModel.todaysMysteryId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rosary") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Select a Mystery",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(mysteries) { mystery ->
                val isToday = mystery.id == todaysMysteryId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onStartSession(mystery.id) },
                    colors = if (isToday)
                        androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    else androidx.compose.material3.CardDefaults.cardColors(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(mystery.name, style = MaterialTheme.typography.titleSmall)
                        mystery.traditionalDays?.let { days ->
                            Text(
                                text = if (isToday) "✦ Today — $days" else days,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isToday)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosarySessionScreen(
    mysteryId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onScriptureClicked: (String) -> Unit,
    viewModel: RosaryViewModel = hiltViewModel(),
) {
    val beads by viewModel.beads.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val prayerTexts by viewModel.prayerTexts.collectAsState()

    LaunchedEffect(mysteryId) { viewModel.startSession(mysteryId) }

    val currentBead = beads.getOrNull(position)
    val isLast = position == beads.lastIndex && beads.isNotEmpty()
    val isAnnouncementBead = currentBead?.prayerId == null && currentBead?.mysteryTitle != null

    val prayerName = currentBead?.prayerId
        ?.replace('-', ' ')
        ?.split(' ')
        ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        ?: ""
    val prayerBody = currentBead?.prayerId?.let { prayerTexts[it] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${position + 1} / ${beads.size}") },
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
                .padding(padding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // --- Scrollable prayer/mystery content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isAnnouncementBead) {
                    // Dedicated mystery page
                    currentBead.mysteryNumber?.let { num ->
                        Text(
                            text = "The ${ordinal(num)} Mystery",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = currentBead!!.mysteryTitle!!,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    currentBead.mysteryScripture?.let { scripture ->
                        val firstRef = scripture.substringBefore(";").trim()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = firstRef,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.Underline,
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { onScriptureClicked(firstRef) },
                        )
                    }
                    currentBead.mysteryMeditation?.let { meditation ->
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = meditation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    // Regular prayer bead
                    currentBead?.mysteryTitle?.let { title ->
                        // On intro Hail Marys this is the intention; on mystery Hail Marys this is null
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    currentBead?.mysteryScripture?.let { scripture ->
                        val firstRef = scripture.substringBefore(";").trim()
                        Text(
                            text = firstRef,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.Underline,
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { onScriptureClicked(firstRef) },
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(
                        text = prayerName,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    prayerBody?.let { body ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // --- Bead indicator ---
            if (beads.isNotEmpty()) {
                RosaryBeadIndicator(
                    beadCount = beads.size,
                    currentIndex = position,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                )
            }

            // --- Navigation buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(onClick = { viewModel.back() }) { Text("Back") }
                if (isLast) {
                    Button(onClick = { viewModel.completeSession(onFinished) }) {
                        Text("Complete")
                    }
                } else {
                    Button(onClick = { viewModel.advance() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Text(" Next")
                    }
                }
            }
        }
    }
}

/**
 * Draws a rosary shape:
 *   - Oval loop for the mystery decades (loop beads: indices [tailCount]..[total-2])
 *   - Junction bead (index total-1, closing) at the bottom of the oval
 *   - Short tail hanging below for the intro beads (indices 0..[tailCount-1])
 *
 * Current bead = filled primary, past = dim filled, future = outline.
 */
@Composable
private fun RosaryBeadIndicator(
    beadCount: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (beadCount == 0) return

    val tailCount   = 6                   // intro beads  0-5
    val closingIdx  = beadCount - 1       // Hail Holy Queen (76)
    val loopCount   = beadCount - tailCount - 1  // 70 mystery beads (6-75)
    // Total oval slots = loopCount + 1 (closing bead at junction)
    val ovalSlots   = loopCount + 1

    val primary  = MaterialTheme.colorScheme.primary
    val outline  = MaterialTheme.colorScheme.outlineVariant
    val past     = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val cord     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val cx  = size.width / 2f
        val a   = size.width / 2f - 14.dp.toPx()          // horizontal semi-axis
        val b   = size.height * 0.375f                     // vertical semi-axis
        val oy  = b + 6.dp.toPx()                          // oval centre y (near top)
        val junctionY = oy + b                             // bottom of oval

        // Tail spacing: fill the remaining canvas height below the junction
        val tailAvail = size.height - junctionY - 4.dp.toPx()
        val tailStep  = tailAvail / (tailCount + 0.5f)

        // Oval position for slot k (k=0 → junction at bottom, k=1..loopCount → loop beads going clockwise)
        fun ovalPos(k: Int): Offset {
            val angle = (PI / 2.0 + 2.0 * PI * k / ovalSlots).toFloat()
            return Offset(cx + a * cos(angle), oy + b * sin(angle))
        }

        // Tail position: intro bead introIdx=0 is furthest from oval, introIdx=5 is closest
        fun tailPos(introIdx: Int): Offset {
            val stepsDown = tailCount - introIdx   // 5→1, 0→6
            return Offset(cx, junctionY + stepsDown * tailStep)
        }

        // ── Cords ──────────────────────────────────────────────────────────
        drawOval(
            color     = cord,
            topLeft   = Offset(cx - a, oy - b),
            size      = Size(a * 2f, b * 2f),
            style     = Stroke(width = 1.dp.toPx()),
        )
        drawLine(
            color       = cord,
            start       = Offset(cx, junctionY),
            end         = tailPos(0),
            strokeWidth = 1.dp.toPx(),
        )

        // ── Beads ──────────────────────────────────────────────────────────
        val rNorm    = 3.dp.toPx()
        val rCurrent = 4.8.dp.toPx()
        val rOurFather = 4.dp.toPx()  // slightly larger for Our Father / announcement beads
        val strokeW  = 1.dp.toPx()

        for (idx in 0 until beadCount) {
            val pos: Offset = when {
                idx < tailCount  -> tailPos(idx)
                idx == closingIdx -> ovalPos(0)
                else -> {
                    val loopIdx = idx - tailCount   // 0-based within loop
                    ovalPos(loopIdx + 1)
                }
            }

            // Every 14th loop bead starting at 0 is an announcement bead (slightly larger)
            val isDecadeBoundary = idx >= tailCount && idx < closingIdx &&
                                   (idx - tailCount) % 14 == 0
            val r = when {
                idx == currentIndex -> rCurrent
                isDecadeBoundary    -> rOurFather
                else                -> rNorm
            }

            when {
                idx == currentIndex -> drawCircle(primary, r, pos)
                idx < currentIndex  -> drawCircle(past, r, pos)
                else                -> drawCircle(outline, r, pos, style = Stroke(strokeW))
            }
        }
    }
}

private fun ordinal(n: Int): String = when (n) {
    1 -> "First"; 2 -> "Second"; 3 -> "Third"; 4 -> "Fourth"; 5 -> "Fifth"
    else -> "$n."
}
