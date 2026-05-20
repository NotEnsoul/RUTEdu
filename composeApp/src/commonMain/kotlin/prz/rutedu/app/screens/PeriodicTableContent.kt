package prz.rutedu.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prz.rutedu.app.models.Element
import prz.rutedu.app.models.ElementCategory
import prz.rutedu.app.models.ELEMENTS
import prz.rutedu.app.models.Hint
import prz.rutedu.app.models.Question
import prz.rutedu.app.models.elementByNumber
import kotlinx.coroutines.delay

private fun Float.to3dp(): String {
    val rounded = kotlin.math.round(this * 1000).toLong()
    val intPart = rounded / 1000
    val decPart = kotlin.math.abs(rounded % 1000)
    return "$intPart.${decPart.toString().padStart(3, '0')}"
}

enum class FindCheckState { IDLE, CORRECT, WRONG }

// ── Layout constants ─────────────────────────────────────────────────────────
private val CELL_SIZE = 44.dp
private val CELL_PADDING = 1.dp
private const val TABLE_COLS = 18
// Visual rows: 1-7 normal, gap at 8, lanthanides at 9, actinides at 10
private const val TABLE_ROWS = 10

// Category colors
private fun elementColor(cat: ElementCategory): Color = Color(cat.colorHex)
private val COLOR_EMPTY_SLOT = Color(0xFFE0E0E0)
private val COLOR_EMPTY_BORDER = Color(0xFFBDBDBD)

// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PeriodicTableContent(
    question: Question.PeriodicTableQuiz,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {}
) {
    // slotContents: slot's correct atomicNumber → which element was placed there
    val slotContents = remember(question.id) { mutableStateMapOf<Int, Int>() }
    // checkResult: slot atomicNumber → correct(true) / wrong(false), null = not checked yet
    var checkResult by remember(question.id) { mutableStateOf<Map<Int, Boolean>?>(null) }
    var selectedFromTray by remember(question.id) { mutableStateOf<Int?>(null) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    val allSlotsFilled = slotContents.size == question.missingAtomicNumbers.size
    // Elements still in the tray = not yet placed in any slot
    val placedElements = slotContents.values.toSet()
    val trayElements = question.missingAtomicNumbers.filter { it !in placedElements }

    // After Sprawdź: show result, then either advance or reset wrong slots
    LaunchedEffect(checkResult) {
        val result = checkResult ?: return@LaunchedEffect
        delay(1000)
        if (result.all { it.value }) {
            onCorrect()
        } else {
            // Return wrong-placed elements to tray
            result.filter { !it.value }.keys.forEach { slot -> slotContents.remove(slot) }
            checkResult = null
        }
    }

    if (showHint) {
        HintBottomSheet(
            hint = question.hint,
            accentColor = accentColor,
            onDismiss = { showHint = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 5.dp)
        ) {
            Text("CHEMIA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = question.questionText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Text(
            text = "Wybierz pierwiastek z tacy, wstaw w puste miejsce",
            fontSize = 13.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        // ── Zoomable periodic table ───────────────────────────────────────────
        ZoomablePeriodicTable(
            modifier = Modifier.fillMaxWidth().weight(1f),
            missingAtomicNumbers = question.missingAtomicNumbers,
            slotContents = slotContents,
            checkResult = checkResult,
            selectedFromTray = selectedFromTray,
            accentColor = accentColor,
            onSlotTapped = { slotAtomicNumber ->
                if (checkResult != null) return@ZoomablePeriodicTable  // locked during reveal
                val sel = selectedFromTray
                val currentOccupant = slotContents[slotAtomicNumber]
                if (sel != null) {
                    // Place selected element; if slot was occupied, its element returns to tray
                    slotContents[slotAtomicNumber] = sel
                    selectedFromTray = null
                } else if (currentOccupant != null) {
                    // Tap occupied slot with nothing selected → pick up that element
                    slotContents.remove(slotAtomicNumber)
                    selectedFromTray = currentOccupant
                }
            }
        )

        // ── Element tray ─────────────────────────────────────────────────────
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.missingAtomicNumbers.forEach { atomicNum ->
                        val el = elementByNumber[atomicNum] ?: return@forEach
                        val inTray = atomicNum in trayElements
                        val isSelected = selectedFromTray == atomicNum
                        TrayElementCard(
                            element = el,
                            isPlaced = !inTray,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            onClick = {
                                if (checkResult != null) return@TrayElementCard
                                if (inTray) selectedFromTray = if (isSelected) null else atomicNum
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (question.hint.mainText.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showHint = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Podpowiedź")
                        }
                    }
                    Button(
                        onClick = {
                            if (allSlotsFilled && checkResult == null) {
                                val cr = slotContents.mapValues { (slot, placed) -> slot == placed }
                                checkResult = cr
                                if (!cr.all { it.value }) onWrong()

                            }
                        },
                        enabled = allSlotsFilled && checkResult == null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Sprawdź ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Zoomable table using graphicsLayer + pointerInput transform detection
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ZoomablePeriodicTable(
    modifier: Modifier,
    missingAtomicNumbers: List<Int>,
    slotContents: Map<Int, Int> = emptyMap(),
    checkResult: Map<Int, Boolean>? = null,
    selectedFromTray: Int?,
    accentColor: Color,
    findSelected: Int? = null,
    findCheckState: FindCheckState = FindCheckState.IDLE,
    onSlotTapped: (Int) -> Unit,
    onElementTapped: (Int) -> Unit = {}
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val elementGrid: Map<Pair<Int,Int>, Element> = remember {
        ELEMENTS.associateBy { el -> Pair(el.tableRow, el.tableCol) }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.3f, 4f)
                        offset += pan
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                TableContent(
                    elementGrid = elementGrid,
                    missingAtomicNumbers = missingAtomicNumbers,
                    slotContents = slotContents,
                    checkResult = checkResult,
                    selectedFromTray = selectedFromTray,
                    accentColor = accentColor,
                    scale = scale,
                    offset = offset,
                    findSelected = findSelected,
                    findCheckState = findCheckState,
                    onSlotTapped = onSlotTapped,
                    onElementTapped = onElementTapped
                )
            }
        }
    }
}

@Composable
private fun TableContent(
    elementGrid: Map<Pair<Int,Int>, Element>,
    missingAtomicNumbers: List<Int>,
    slotContents: Map<Int, Int> = emptyMap(),
    checkResult: Map<Int, Boolean>? = null,
    selectedFromTray: Int?,
    accentColor: Color,
    scale: Float,
    offset: Offset,
    findSelected: Int? = null,
    findCheckState: FindCheckState = FindCheckState.IDLE,
    onSlotTapped: (Int) -> Unit,
    onElementTapped: (Int) -> Unit = {}
) {
    val missingSet = missingAtomicNumbers.toSet()

    fun visualRow(tableRow: Int) = if (tableRow <= 7) tableRow else tableRow + 1

    val totalVisualRows = 11
    val totalWidth = (CELL_SIZE + CELL_PADDING * 2) * TABLE_COLS
    val totalHeight = (CELL_SIZE + CELL_PADDING * 2) * totalVisualRows

    Box(modifier = Modifier.size(width = totalWidth, height = totalHeight)) {
        ELEMENTS.forEach { el ->
            val vRow = visualRow(el.tableRow)
            val col = el.tableCol
            val isMissing = el.atomicNumber in missingSet
            val occupant = slotContents[el.atomicNumber]   // element placed in this slot
            val slotChecked = checkResult?.get(el.atomicNumber)  // null/true/false
            val isHighlightedSlot = selectedFromTray != null && isMissing && occupant == null
            val isFindSelected = findSelected == el.atomicNumber

            val x = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * (col - 1))
            val y = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * (vRow - 1))

            when {
                isMissing && occupant == null -> {
                    // Empty slot
                    EmptySlotCell(
                        x = x, y = y,
                        isHighlighted = isHighlightedSlot,
                        accentColor = accentColor,
                        onClick = { onSlotTapped(el.atomicNumber) }
                    )
                }
                isMissing && occupant != null -> {
                    // Slot with element placed — show Wordle color after check
                    val placedEl = elementByNumber[occupant]
                    if (placedEl != null) {
                        val slotColor = when (slotChecked) {
                            true  -> Color(0xFF4CAF50)   // correct — green
                            false -> Color(0xFFF44336)   // wrong — red
                            null  -> accentColor.copy(alpha = 0.25f)  // pending — light accent
                        }
                        FilledSlotCell(
                            x = x, y = y,
                            element = placedEl,
                            slotColor = slotColor,
                            checkState = slotChecked,
                            onClick = { onSlotTapped(el.atomicNumber) }
                        )
                    }
                }
                else -> {
                    // Normal element cell — tappable in find-mode
                    val findCellColor: Color? = when {
                        isFindSelected && findCheckState == FindCheckState.CORRECT -> Color(0xFF66BB6A)
                        isFindSelected && findCheckState == FindCheckState.WRONG   -> Color(0xFFE53935)
                        isFindSelected -> accentColor
                        else -> null
                    }
                    ElementCell(
                        x = x, y = y,
                        element = el,
                        isJustPlaced = false,
                        findOverrideColor = findCellColor,
                        onClick = { onElementTapped(el.atomicNumber) }
                    )
                }
            }
        }

        // Period labels (left side, col 0)
        for (period in 1..7) {
            val vRow = visualRow(period)
            val y = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * (vRow - 1))
            Text(
                text = "$period",
                fontSize = 8.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier
                    .offset(x = 2.dp, y = y + CELL_SIZE / 2 - 5.dp)
            )
        }

        // "57-71" and "89-103" placeholders in period 6,7 col 3 area for f-block ref
        // Already have La (57) at 6,3 and Ac (89) at 7,3

        // Gap row label
        Text(
            text = "Lantanowce →",
            fontSize = 7.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.offset(
                x = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * 2),
                y = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * 8)
            )
        )
        Text(
            text = "Aktynowce →",
            fontSize = 7.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.offset(
                x = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * 2),
                y = (CELL_PADDING + (CELL_SIZE + CELL_PADDING * 2) * 9)
            )
        )
    }
}

@Composable
private fun ElementCell(
    x: Dp,
    y: Dp,
    element: Element,
    isJustPlaced: Boolean,
    findOverrideColor: Color? = null,
    onClick: () -> Unit = {}
) {
    val bgColor = when {
        isJustPlaced -> Color(0xFF66BB6A)
        findOverrideColor != null -> findOverrideColor.copy(alpha = 0.35f)
        else -> elementColor(element.category)
    }

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(CELL_SIZE)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(if (findOverrideColor != null) Modifier.border(2.dp, findOverrideColor, RoundedCornerShape(4.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "${element.atomicNumber}",
                fontSize = 7.sp,
                color = Color(0xFF424242),
                lineHeight = 7.sp
            )
            Text(
                text = element.symbol,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                lineHeight = 14.sp
            )
            Text(
                text = element.atomicMass.to3dp(),
                fontSize = 6.sp,
                color = Color(0xFF616161),
                lineHeight = 6.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun EmptySlotCell(
    x: Dp,
    y: Dp,
    isHighlighted: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isHighlighted) accentColor else COLOR_EMPTY_BORDER,
        animationSpec = tween(200)
    )
    val bgColor by animateColorAsState(
        if (isHighlighted) accentColor.copy(alpha = 0.15f) else COLOR_EMPTY_SLOT,
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(CELL_SIZE)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = borderColor
        )
    }
}

@Composable
private fun FilledSlotCell(
    x: Dp,
    y: Dp,
    element: Element,
    slotColor: Color,
    checkState: Boolean?,   // null=pending, true=correct, false=wrong
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(CELL_SIZE)
            .clip(RoundedCornerShape(4.dp))
            .background(slotColor.copy(alpha = if (checkState != null) 0.8f else 0.25f))
            .border(2.dp, slotColor, RoundedCornerShape(4.dp))
            .clickable(enabled = checkState == null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = element.symbol,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (checkState != null) Color.White else Color(0xFF1A1A1A),
                lineHeight = 14.sp
            )
            if (checkState != null) {
                Text(
                    text = if (checkState) "✓" else "✗",
                    fontSize = 10.sp,
                    color = Color.White,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tray element card (bottom palette)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TrayElementCard(
    element: Element,
    isPlaced: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor = when {
        isPlaced -> Color(0xFFE8F5E9)
        isSelected -> accentColor.copy(alpha = 0.12f)
        else -> elementColor(element.category)
    }
    val borderColor = when {
        isPlaced -> Color(0xFF66BB6A)
        isSelected -> accentColor
        else -> Color.Transparent
    }
    val textAlpha = if (isPlaced) 0.4f else 1f

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = !isPlaced, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaced) {
            Text("✓", fontSize = 24.sp, color = Color(0xFF66BB6A))
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = element.symbol,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A).copy(alpha = textAlpha)
                )
                Text(
                    text = element.namePL,
                    fontSize = 9.sp,
                    color = Color(0xFF616161).copy(alpha = textAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = element.atomicMass.to3dp(),
                    fontSize = 8.sp,
                    color = Color(0xFF757575).copy(alpha = textAlpha)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PeriodicTableByShellContent  —  show shell config, tap the right element
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PeriodicTableByShellContent(
    question: Question.PeriodicTableByShell,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {}
) {
    PeriodicTableFindContent(
        topLabel = "CHEMIA",
        questionText = "Wskaż pierwiastek o konfiguracji powłokowej",
        clueContent = {
            Text(
                text = question.shellConfig,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            Text(
                text = "elektrony w powłokach K, L, M, N…",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
        },
        targetAtomicNumber = question.targetAtomicNumber,
        hint = question.hint,
        accentColor = accentColor,
        bottomPadding = bottomPadding,
        onCorrect = onCorrect,
        onWrong = onWrong
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PeriodicTableByNameContent  —  show element name, tap the right element
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PeriodicTableByNameContent(
    question: Question.PeriodicTableByName,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {}
) {
    PeriodicTableFindContent(
        topLabel = "CHEMIA",
        questionText = "Znajdź w układzie pierwiastek o nazwie",
        clueContent = {
            Text(
                text = question.elementNamePL,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )
        },
        targetAtomicNumber = question.targetAtomicNumber,
        hint = question.hint,
        accentColor = accentColor,
        bottomPadding = bottomPadding,
        onCorrect = onCorrect,
        onWrong = onWrong
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared "find element" table screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PeriodicTableFindContent(
    topLabel: String,
    questionText: String,
    clueContent: @Composable ColumnScope.() -> Unit,
    targetAtomicNumber: Int,
    hint: Hint,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {}
) {
    // Keyed by targetAtomicNumber so state resets with each new question
    var selectedElement by remember(targetAtomicNumber) { mutableStateOf<Int?>(null) }
    var checkState by remember(targetAtomicNumber) { mutableStateOf(FindCheckState.IDLE) }
    var showHint by remember(targetAtomicNumber) { mutableStateOf(false) }

    LaunchedEffect(checkState) {
        when (checkState) {
            FindCheckState.CORRECT -> { delay(600); onCorrect() }
            FindCheckState.WRONG   -> { delay(500); checkState = FindCheckState.IDLE }
            FindCheckState.IDLE    -> {}
        }
    }

    if (showHint) {
        HintBottomSheet(hint = hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 5.dp)
        ) {
            Text(topLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = questionText,
            fontSize = 15.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Clue card — flashes green/red based on check result
        val cardBg = when (checkState) {
            FindCheckState.CORRECT -> Color(0xFFE8F5E9)
            FindCheckState.WRONG   -> Color(0xFFFFCDD2)
            FindCheckState.IDLE    -> Color.White
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                clueContent()
            }
        }

        Spacer(Modifier.height(6.dp))

        // Zoomable table
        ZoomablePeriodicTable(
            modifier = Modifier.fillMaxWidth().weight(1f),
            missingAtomicNumbers = emptyList(),
            selectedFromTray = null,
            accentColor = accentColor,
            findSelected = selectedElement,
            findCheckState = checkState,
            onSlotTapped = {},
            onElementTapped = { atomicNumber ->
                if (checkState == FindCheckState.IDLE) {
                    selectedElement = atomicNumber
                }
            }
        )

        // Bottom bar — solid background so table doesn't show through
        Surface(
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .padding(bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hint.mainText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showHint = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Podpowiedź")
                    }
                }
                Button(
                    onClick = {
                        val sel = selectedElement
                        if (sel != null) {
                            checkState = if (sel == targetAtomicNumber) FindCheckState.CORRECT
                                         else FindCheckState.WRONG
                            if (sel != targetAtomicNumber) onWrong()

                        }
                    },
                    enabled = selectedElement != null && checkState == FindCheckState.IDLE,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Sprawdź ✓", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
