package com.tristinbaker.defide.ui.rosary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tristinbaker.defide.R
import com.tristinbaker.defide.data.preferences.AppRite
import com.tristinbaker.defide.data.preferences.RosaryOrder
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
    val currentRite by viewModel.currentRite.collectAsState()
    val englishMysteries by viewModel.englishMysteries.collectAsState()
    val todaysMysteryId = viewModel.todaysMysteryId
    val intentions by viewModel.intentions.collectAsState()
    var showIntentionsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rosary_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showIntentionsSheet = true },
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text(stringResource(R.string.rosary_intentions_title))
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
                        Text(
                            text = when (currentRite) {
                                AppRite.TRADITIONAL -> englishMysteries.find { it.id == mystery.id }?.name ?: mystery.name
                                else -> mystery.name
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
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

    if (showIntentionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        var draft by remember(intentions) {
            mutableStateOf(MutableList(5) { i -> intentions.getOrElse(i) { "" } })
        }
        ModalBottomSheet(
            onDismissRequest = { showIntentionsSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
                Text(
                    stringResource(R.string.rosary_intentions_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                (0..4).forEach { i ->
                    OutlinedTextField(
                        value = draft[i],
                        onValueChange = { v -> draft = draft.toMutableList().also { it[i] = v } },
                        label = { Text(stringResource(R.string.rosary_intention_hint, i + 1)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = {
                        draft = MutableList(5) { "" }
                        viewModel.clearIntentions()
                    }) { Text(stringResource(R.string.rosary_clear_intentions)) }
                    Button(onClick = {
                        viewModel.saveIntentions(draft)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showIntentionsSheet = false }
                    }) { Text(stringResource(R.string.rosary_save_intentions)) }
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
    val currentPhysicalBead by viewModel.currentPhysicalBead.collectAsState()
    val visitedPhysBeads by viewModel.visitedPhysBeads.collectAsState()
    val prayerTexts by viewModel.prayerTexts.collectAsState()
    val prayerTitles by viewModel.prayerTitles.collectAsState()
    val rosaryOrder by viewModel.rosaryOrder.collectAsState()
    // For Traditional mode: English mystery titles pulled directly from VM's StateFlows
    val englishMysteries by viewModel.englishMysteries.collectAsState()
    val englishBeads by viewModel.englishBeads.collectAsState()
    val currentRite by viewModel.currentRite.collectAsState()
    val isFatima = rosaryOrder == RosaryOrder.FATIMA
    val hapticEnabled by viewModel.hapticFeedback.collectAsState()
    val completing by viewModel.completing.collectAsState()
    val intentions by viewModel.intentions.collectAsState()
    val currentMysteryNumber by viewModel.currentMysteryNumber.collectAsState()
    val haptic = LocalHapticFeedback.current
    val narrationEnabled by viewModel.narrationEnabled.collectAsState()
    val intentionInDesign by viewModel.intentionInDesign.collectAsState()

    LaunchedEffect(mysteryId) { viewModel.startSession(mysteryId) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.stopNarration()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopNarration()
        }
    }

    val currentBead = beads.getOrNull(position)

    val isLast = position == beads.lastIndex && beads.isNotEmpty()
    val isAnnouncementBead = currentBead?.prayerId == null && currentBead?.mysteryTitle != null

    // The swipe handler lives in pointerInput(Unit), whose lambda is never re-created
    // on recomposition — read these through rememberUpdatedState or they'd stay stale.
    val latestPosition by rememberUpdatedState(position)
    val latestIsLast by rememberUpdatedState(isLast)
    val latestHapticEnabled by rememberUpdatedState(hapticEnabled)

    // In Traditional mode pull English titles/scripture from englishBeads (per-mystery, not group).
    val englishTitle: String? = if (currentRite == AppRite.TRADITIONAL) {
        englishBeads[mysteryId]?.find { it.mysteryNumber == currentBead?.mysteryNumber }?.mysteryTitle
    } else null

    /** English scripture reference for the current mystery (used in Traditional mode). */
    val englishScripture: String? = if (currentRite == AppRite.TRADITIONAL) {
        englishBeads[mysteryId]?.find { it.mysteryNumber == currentBead?.mysteryNumber }?.mysteryScripture
    } else null

    /** English meditation text for the current mystery (used in Traditional mode). */
    val englishMeditation: String? = if (currentRite == AppRite.TRADITIONAL) {
        englishBeads[mysteryId]?.find { it.mysteryNumber == currentBead?.mysteryNumber }?.mysteryMeditation
    } else null

    val prayerName = currentBead?.prayerId?.let { prayerTitles[it] } ?: ""
    val prayerBody = currentBead?.prayerId?.let { prayerTexts[it] }

    val counterText = when (currentPhysicalBead) {
        -1   -> "\u271d / 60"
        else -> "${visitedPhysBeads.size} / 60"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(counterText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setNarrationEnabled(!narrationEnabled) }) {
                        Icon(
                            imageVector = if (narrationEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = stringResource(
                                if (narrationEnabled) R.string.rosary_narration_mute_cd
                                else R.string.rosary_narration_unmute_cd
                            ),
                        )
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
                    .pointerInput(Unit) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onDragEnd = {
                                val threshold = 64.dp.toPx()
                                when {
                                    dragTotal <= -threshold && !latestIsLast -> {
                                        if (latestHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.advance()
                                    }
                                    dragTotal >= threshold && latestPosition > 0 -> {
                                        if (latestHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.back()
                                    }
                                }
                            },
                        ) { _, dragAmount -> dragTotal += dragAmount }
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isAnnouncementBead) {
                    // Dedicated mystery page — title/scripture shown in mystery's own language
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
                    // Mystery title: English in Traditional, mystery's own language otherwise
                    val mysteryTitle = when (currentRite) {
                        AppRite.TRADITIONAL -> englishTitle ?: currentBead!!.mysteryTitle!!
                        else -> currentBead!!.mysteryTitle!!
                    }
                    Text(
                        text = mysteryTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    currentBead.mysteryNumber?.let { num ->
                        if (num in 1..5 && !intentionInDesign) {
                            val intention = intentions.getOrElse(num - 1) { "" }
                            if (intention.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.rosary_your_intention, intention),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                )
                            }
                        }
                    }
                    // Scripture reference: English in Traditional, mystery's own language otherwise
                    val scriptureRef = when (currentRite) {
                        AppRite.TRADITIONAL -> englishScripture ?: currentBead.mysteryScripture
                        else -> currentBead.mysteryScripture
                    }
                    scriptureRef?.let { scripture ->
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
                    // Meditation: English in Traditional, mystery's own language otherwise
                    val meditationText = when (currentRite) {
                        AppRite.TRADITIONAL -> englishMeditation ?: currentBead.mysteryMeditation
                        else -> currentBead.mysteryMeditation
                    }
                    meditationText?.let { meditation ->
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
                    val mystNum = currentMysteryNumber
                    if (mystNum != null && mystNum in 1..5 && !intentionInDesign) {
                        val intention = intentions.getOrElse(mystNum - 1) { "" }
                        if (intention.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.rosary_your_intention, intention),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    // Regular prayer bead — in Traditional mode show English title
                    val displayTitle = if (currentRite == AppRite.TRADITIONAL && englishTitle != null) {
                        englishTitle
                    } else {
                        currentBead?.mysteryTitle
                    }
                    displayTitle?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    // Scripture in mystery's own language
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
                    } ?: run {
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
                val designIntention = if (intentionInDesign) {
                    currentMysteryNumber?.takeIf { it in 1..5 }
                        ?.let { intentions.getOrElse(it - 1) { "" } }
                        ?.takeIf { it.isNotBlank() }
                } else null
                RosaryBeadIndicatorCompact(
                    currentPhysicalBead = currentPhysicalBead,
                    visitedPhysBeads = visitedPhysBeads,
                    isFatima = isFatima,
                    centerText = designIntention,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                )
            }

            // --- Navigation buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.back()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Text(" ${stringResource(R.string.action_back)}")
                }
                if (isLast) {
                    Button(
                        onClick = {
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.completeSession(onFinished)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !completing,
                    ) {
                        Text(stringResource(R.string.action_complete))
                    }
                } else {
                    Button(
                        onClick = {
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.advance()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Text(" ${stringResource(R.string.action_next)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RosaryBeadIndicatorCompact(
    currentPhysicalBead: Int,
    visitedPhysBeads: Set<Int>,
    isFatima: Boolean,
    modifier: Modifier = Modifier,
    centerText: String? = null,
) {
    // Physical layout: 60 beads (0-59) + cross (-1)
    // Tail: beads 0-4 (0=Our Father large, 1-3=HM small, 4=Our Father decade 1 large)
    // Junction: no bead drawn -- cord only
    // Loop: beads 5-58 at perimeterPos(1..54)*spacing; bead 59 (Hail Holy Queen) at perimeterPos(0); ovalSlots=55
    val tailCount = 5
    val ovalSlots = 55   // bead 59 at slot 0, beads 5-58 at slots 1-54

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val past    = primary.copy(alpha = 0.32f)
    val cord    = outline.copy(alpha = 0.55f)

    fun isLargeBead(physBead: Int) =
        physBead == 0 || physBead == 4 ||
        (physBead in 15..48 && (physBead - 15) % 11 == 0) ||
        (isFatima && physBead == 59)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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

            // Junction: right side, vertically centered
            val jX = rRight
            val jY = rTop + rH / 2f

            // Tail extends horizontally to the right
            val crossEndX = W - 10.dp.toPx()
            val tailStep  = (crossEndX - jX) / (tailCount + 2f)

            fun tailPos(physBead: Int): Offset {
                val x = when (physBead) {
                    0    -> jX + 4.6f * tailStep
                    1    -> jX + 3.7f * tailStep
                    2    -> jX + 3.0f * tailStep
                    3    -> jX + 2.3f * tailStep
                    4    -> jX + 1.4f * tailStep
                    else -> jX + (tailCount - physBead) * tailStep
                }
                return Offset(x, jY)
            }

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

            val loopCount   = ovalSlots - 1           // 54 loop beads
            val junctionGap = 1.3f * spacing          // distance gap around junction oval
            val loopSpan    = totalPerimeter - 2f * junctionGap

            // Weighted spacing: slightly larger gap before/after each Our Father bead (15, 26, 37, 48)
            val largeLoopBeads = setOf(15, 26, 37, 48)
            val loopGapMult = 1.2f
            val loopGaps = FloatArray(53) { i ->
                val f = i + 5; val t = i + 6
                if (f in largeLoopBeads || t in largeLoopBeads) loopGapMult else 1.0f
            }
            val totalLoopWeight = loopGaps.sum()
            val loopT = FloatArray(54).also { arr ->
                arr[0] = 0.0f
                var cum = 0.0f
                for (i in 1 until 54) { cum += loopGaps[i - 1]; arr[i] = cum / totalLoopWeight }
            }

            fun beadPos(physBead: Int): Offset = when {
                physBead < tailCount -> tailPos(physBead)
                physBead == 59       -> perimeterPos(0f)  // Hail Holy Queen at junction
                else -> perimeterPos(junctionGap + loopT[physBead - 5] * loopSpan)
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

            // Cross at the end of the tail
            val crossX     = tailPos(0).x + tailStep * 0.85f
            val cH         = 18.dp.toPx()
            val cW         = 12.dp.toPx()
            val barW       = 3.5.dp.toPx()
            val crossColor = if (currentPhysicalBead == -1) primary else past
            drawRect(crossColor, topLeft = Offset(crossX - barW / 2f, jY - cH * 0.65f), size = Size(barW, cH))
            drawRect(crossColor, topLeft = Offset(crossX - cW / 2f,   jY - cH * 0.40f), size = Size(cW, barW))

            // ── Beads ──────────────────────────────────────────────────────────
            val rSmall        = 4.dp.toPx()
            val rLarge        = 7.dp.toPx()
            val rCurrentSmall = 5.5.dp.toPx()
            val rCurrentLarge = 8.5.dp.toPx()
            val strokeW       = 1.dp.toPx()

            for (physBead in 0 until 60) {
                val pos       = beadPos(physBead)
                val isCurrent = physBead == currentPhysicalBead
                val isPast    = physBead in visitedPhysBeads && !isCurrent
                val large     = isLargeBead(physBead)
                val beadR     = when {
                    isCurrent && large  -> rCurrentLarge
                    isCurrent           -> rCurrentSmall
                    large               -> rLarge
                    else                -> rSmall
                }
                val beadColor = when {
                    isCurrent -> primary
                    isPast    -> past
                    else      -> null
                }
                if (physBead == 59) {
                    val rx = rLarge * 1.0f
                    val ry = rLarge * 1.4f
                    val tl = Offset(pos.x - rx, pos.y - ry)
                    val sz = Size(rx * 2, ry * 2)
                    when {
                        isCurrent -> drawOval(primary, topLeft = tl, size = sz)
                        isPast    -> drawOval(past,    topLeft = tl, size = sz)
                        else      -> drawOval(outline, topLeft = tl, size = sz, style = Stroke(strokeW))
                    }
                } else {
                    when {
                        isCurrent -> drawCircle(primary, beadR, pos)
                        isPast    -> drawCircle(past,    beadR, pos)
                        else      -> drawCircle(outline, beadR, pos, style = Stroke(strokeW))
                    }
                }
            }
        }
        if (centerText != null) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.62f)
                    .padding(start = 8.dp)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}
