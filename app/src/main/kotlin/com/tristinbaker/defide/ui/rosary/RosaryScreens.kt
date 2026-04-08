package com.tristinbaker.defide.ui.rosary

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R
import com.tristinbaker.defide.data.preferences.RosaryDiagramStyle
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
    val diagramStyle by viewModel.diagramStyle.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rosary_title)) },
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
                    text = stringResource(R.string.select_mystery),
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
                                text = if (isToday) stringResource(R.string.today_mystery_prefix, days) else days,
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
    val prayerTitles by viewModel.prayerTitles.collectAsState()
    val diagramStyle by viewModel.diagramStyle.collectAsState()

    LaunchedEffect(mysteryId) { viewModel.startSession(mysteryId) }

    val currentBead = beads.getOrNull(position)
    val isLast = position == beads.lastIndex && beads.isNotEmpty()
    val isAnnouncementBead = currentBead?.prayerId == null && currentBead?.mysteryTitle != null

    val prayerName = currentBead?.prayerId?.let { prayerTitles[it] } ?: ""
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
                actions = {
                    FilterChip(
                        selected = diagramStyle == RosaryDiagramStyle.CLASSIC,
                        onClick = { viewModel.setDiagramStyle(RosaryDiagramStyle.CLASSIC) },
                        label = { Text(stringResource(R.string.rosary_diagram_classic)) },
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = diagramStyle == RosaryDiagramStyle.COMPACT,
                        onClick = { viewModel.setDiagramStyle(RosaryDiagramStyle.COMPACT) },
                        label = { Text(stringResource(R.string.rosary_diagram_compact)) },
                    )
                    Spacer(Modifier.width(8.dp))
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
                        val ordinalStr = when (num) {
                            1 -> stringResource(R.string.ordinal_first)
                            2 -> stringResource(R.string.ordinal_second)
                            3 -> stringResource(R.string.ordinal_third)
                            4 -> stringResource(R.string.ordinal_fourth)
                            5 -> stringResource(R.string.ordinal_fifth)
                            else -> "$num."
                        }
                        Text(
                            text = stringResource(R.string.the_nth_mystery, ordinalStr),
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
                    style = diagramStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (diagramStyle == RosaryDiagramStyle.COMPACT) 100.dp else 190.dp),
                )
            }

            // --- Navigation buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(onClick = { viewModel.back() }) { Text(stringResource(R.string.action_back)) }
                if (isLast) {
                    Button(onClick = { viewModel.completeSession(onFinished) }) {
                        Text(stringResource(R.string.action_complete))
                    }
                } else {
                    Button(onClick = { viewModel.advance() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Text(" ${stringResource(R.string.action_next)}")
                    }
                }
            }
        }
    }
}

/**
 * Draws a rosary shape:
 *   - Rounded rectangle loop for the mystery decades (indices [tailCount]..[total-2])
 *   - Junction bead (closing, index total-1) at the right-center of the rectangle
 *   - Horizontal tail extending right for intro beads (indices 0..[tailCount-1])
 *   - Cross drawn at the far end of the tail
 *
 * Current bead = filled primary, past = dim filled, future = outline.
 */
@Composable
private fun RosaryBeadIndicator(
    beadCount: Int,
    currentIndex: Int,
    style: RosaryDiagramStyle,
    modifier: Modifier = Modifier,
) {
    if (style == RosaryDiagramStyle.CLASSIC) {
        RosaryBeadIndicatorClassic(beadCount, currentIndex, modifier)
    } else {
        RosaryBeadIndicatorCompact(beadCount, currentIndex, modifier)
    }
}

@Composable
private fun RosaryBeadIndicatorClassic(
    beadCount: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (beadCount == 0) return

    val tailCount   = 6
    val closingIdx  = beadCount - 1
    val loopCount   = beadCount - tailCount - 1
    val ovalSlots   = loopCount + 1

    val primary  = MaterialTheme.colorScheme.primary
    val outline  = MaterialTheme.colorScheme.outlineVariant
    val past     = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val cord     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val cx  = size.width / 2f
        val a   = size.width / 2f - 14.dp.toPx()
        val b   = size.height * 0.375f
        val oy  = b + 6.dp.toPx()
        val junctionY = oy + b

        val tailAvail = size.height - junctionY - 4.dp.toPx()
        val tailStep  = tailAvail / (tailCount + 0.5f)

        fun ovalPos(k: Int): Offset {
            val angle = (PI / 2.0 + 2.0 * PI * k / ovalSlots).toFloat()
            return Offset(cx + a * cos(angle), oy + b * sin(angle))
        }

        fun tailPos(introIdx: Int): Offset {
            val stepsDown = tailCount - introIdx
            return Offset(cx, junctionY + stepsDown * tailStep)
        }

        drawOval(
            color     = cord,
            topLeft   = Offset(cx - a, oy - b),
            size      = Size(a * 2f, b * 2f),
            style     = Stroke(width = 1.dp.toPx()),
        )
        drawLine(cord, Offset(cx, junctionY), tailPos(0), strokeWidth = 1.dp.toPx())

        // Cross below the last tail bead
        val crossY  = tailPos(0).y + tailStep * 0.85f
        val cH      = 18.dp.toPx()
        val cW      = 12.dp.toPx()
        val barW    = 3.5.dp.toPx()
        drawLine(cord, tailPos(0), Offset(cx, crossY - cH * 0.65f), strokeWidth = 1.dp.toPx())
        drawRect(primary, topLeft = Offset(cx - barW / 2f, crossY - cH * 0.65f), size = Size(barW, cH))
        drawRect(primary, topLeft = Offset(cx - cW / 2f, crossY - cH * 0.40f), size = Size(cW, barW))

        val rNorm      = 3.dp.toPx()
        val rCurrent   = 4.8.dp.toPx()
        val rOurFather = 4.dp.toPx()
        val strokeW    = 1.dp.toPx()

        for (idx in 0 until beadCount) {
            val pos: Offset = when {
                idx < tailCount   -> tailPos(idx)
                idx == closingIdx -> ovalPos(0)
                else              -> ovalPos(idx - tailCount + 1)
            }
            val isDecadeBoundary = idx >= tailCount && idx < closingIdx && (idx - tailCount) % 14 == 0
            val beadR = when {
                idx == currentIndex -> rCurrent
                isDecadeBoundary    -> rOurFather
                else                -> rNorm
            }
            when {
                idx == currentIndex -> drawCircle(primary, beadR, pos)
                idx < currentIndex  -> drawCircle(past, beadR, pos)
                else                -> drawCircle(outline, beadR, pos, style = Stroke(strokeW))
            }
        }
    }
}

@Composable
private fun RosaryBeadIndicatorCompact(
    beadCount: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (beadCount == 0) return

    val tailCount  = 6
    val closingIdx = beadCount - 1
    val loopCount  = beadCount - tailCount - 1
    val ovalSlots  = loopCount + 1   // closing bead + loop beads

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val past    = primary.copy(alpha = 0.32f)
    val cord    = outline.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height

        // Rounded rectangle
        val cornerR = minOf(H * 0.30f, W * 0.12f)
        val rW      = W * 0.62f
        val rH      = H * 0.76f
        val rLeft   = 8.dp.toPx()
        val rTop    = (H - rH) / 2f
        val rRight  = rLeft + rW
        val rBottom = rTop + rH

        // Junction: right side, vertically centered — where tail meets loop
        val jX = rRight
        val jY = rTop + rH / 2f

        // Tail extends horizontally to the right
        val crossEndX = W - 10.dp.toPx()
        val tailStep  = (crossEndX - jX) / (tailCount + 2f)

        // introIdx=5 is closest to junction, introIdx=0 is furthest (near cross)
        fun tailPos(introIdx: Int): Offset {
            val steps = tailCount - introIdx   // 5→1, 0→6
            return Offset(jX + steps * tailStep, jY)
        }

        // Perimeter traversal clockwise from junction:
        //  1. Right side down to bottom-right corner
        //  2. Bottom-right arc
        //  3. Bottom edge right→left
        //  4. Bottom-left arc
        //  5. Left side bottom→top
        //  6. Top-left arc
        //  7. Top edge left→right
        //  8. Top-right arc
        //  9. Right side down to junction
        val seg1      = rH / 2f - cornerR
        val seg9      = rH / 2f - cornerR
        val straightW = rW - 2f * cornerR
        val straightH = rH - 2f * cornerR
        val arcLen    = (PI / 2.0 * cornerR).toFloat()
        val totalPerimeter = seg1 + arcLen + straightW + arcLen + straightH + arcLen + straightW + arcLen + seg9
        val spacing   = totalPerimeter / ovalSlots

        fun perimeterPos(dist: Float): Offset {
            var d = dist
            if (d <= seg1) return Offset(rRight, jY + d)
            d -= seg1
            if (d <= arcLen) {
                val a = (PI / 2.0 * d / arcLen).toFloat()
                return Offset((rRight - cornerR) + cornerR * cos(a), (rBottom - cornerR) + cornerR * sin(a))
            }
            d -= arcLen
            if (d <= straightW) return Offset(rRight - cornerR - d, rBottom)
            d -= straightW
            if (d <= arcLen) {
                val a = (PI / 2.0 + PI / 2.0 * d / arcLen).toFloat()
                return Offset((rLeft + cornerR) + cornerR * cos(a), (rBottom - cornerR) + cornerR * sin(a))
            }
            d -= arcLen
            if (d <= straightH) return Offset(rLeft, rBottom - cornerR - d)
            d -= straightH
            if (d <= arcLen) {
                val a = (PI + PI / 2.0 * d / arcLen).toFloat()
                return Offset((rLeft + cornerR) + cornerR * cos(a), (rTop + cornerR) + cornerR * sin(a))
            }
            d -= arcLen
            if (d <= straightW) return Offset(rLeft + cornerR + d, rTop)
            d -= straightW
            if (d <= arcLen) {
                val a = (-PI / 2.0 + PI / 2.0 * d / arcLen).toFloat()
                return Offset((rRight - cornerR) + cornerR * cos(a), (rTop + cornerR) + cornerR * sin(a))
            }
            d -= arcLen
            return Offset(rRight, rTop + cornerR + d)
        }

        // ── Cords ──────────────────────────────────────────────────────────
        drawRoundRect(
            color        = cord,
            topLeft      = Offset(rLeft, rTop),
            size         = Size(rW, rH),
            cornerRadius = CornerRadius(cornerR),
            style        = Stroke(width = 1.dp.toPx()),
        )
        drawLine(cord, Offset(jX, jY), tailPos(0), strokeWidth = 1.dp.toPx())

        // Cross at the end of the tail — bigger, filled primary/gold
        val crossX  = tailPos(0).x + tailStep * 0.85f
        val cH      = 18.dp.toPx()
        val cW      = 12.dp.toPx()
        val barW    = 3.5.dp.toPx()
        drawRect(primary, topLeft = Offset(crossX - barW / 2f, jY - cH * 0.65f), size = Size(barW, cH))
        drawRect(primary, topLeft = Offset(crossX - cW / 2f, jY - cH * 0.40f), size = Size(cW, barW))

        // ── Beads ──────────────────────────────────────────────────────────
        val rNorm      = 3.dp.toPx()
        val rCurrent   = 5.dp.toPx()
        val rOurFather = 4.5.dp.toPx()
        val strokeW    = 1.dp.toPx()

        for (idx in 0 until beadCount) {
            val pos = when {
                idx < tailCount   -> tailPos(idx)
                idx == closingIdx -> perimeterPos(0f)
                else              -> perimeterPos((idx - tailCount + 1) * spacing)
            }

            val isDecadeBoundary = idx >= tailCount && idx < closingIdx &&
                                   (idx - tailCount) % 14 == 0
            val beadR = when {
                idx == currentIndex -> rCurrent
                isDecadeBoundary    -> rOurFather
                else                -> rNorm
            }

            when {
                idx == currentIndex -> drawCircle(primary, beadR, pos)
                idx < currentIndex  -> drawCircle(past, beadR, pos)
                else                -> drawCircle(outline, beadR, pos, style = Stroke(strokeW))
            }
        }
    }
}

