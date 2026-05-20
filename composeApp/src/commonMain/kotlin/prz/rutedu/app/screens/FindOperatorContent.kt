package prz.rutedu.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prz.rutedu.app.models.MathOperator
import prz.rutedu.app.models.Question
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun FindOperatorContent(
    question: Question.FindOperator,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {},
    onSkip: () -> Unit
) {
    var selectedOperator by remember(question.id) { mutableStateOf<MathOperator?>(null) }
    var isWrong by remember(question.id) { mutableStateOf(false) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    val displayOperators = remember(question.id) {
        val others = MathOperator.entries
            .filter { it != question.correctOperator }
            .shuffled()
            .take(3)
        (others + question.correctOperator).shuffled()
    }

    // Chip is displayed (88dp - 32dp) = 56dp above the finger.
    // Hit detection must follow the chip center, not the raw finger position.
    val density = LocalDensity.current
    val dragLiftYPx = with(density) { (88.dp - 32.dp).toPx() }  // 56dp
    val hitMarginPx = with(density) { 20.dp.toPx() }

    var draggingOperator by remember { mutableStateOf<MathOperator?>(null) }
    var fingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var containerRootPos by remember { mutableStateOf(Offset.Zero) }
    val chipRootPositions = remember { mutableMapOf<MathOperator, Offset>() }
    var targetRootPos by remember { mutableStateOf(Offset.Zero) }
    var targetSizePx by remember { mutableStateOf(Offset.Zero) }

    // Ghost hint: flies from tapped chip toward the target, then fades out
    var ghostOp by remember { mutableStateOf<MathOperator?>(null) }
    val ghostProgress = remember { Animatable(0f) }
    val ghostAlpha = remember { Animatable(0f) }

    val animScope = rememberCoroutineScope()

    fun playTapHint(op: MathOperator) {
        animScope.launch {
            ghostOp = op
            ghostProgress.snapTo(0f)
            ghostAlpha.snapTo(1f)
            launch {
                ghostProgress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            }
            ghostAlpha.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
            ghostOp = null
        }
    }

    val targetBorderColor by animateColorAsState(
        when {
            isWrong -> Color(0xFFE53935)
            selectedOperator != null -> accentColor
            else -> Color(0xFFBCC1CA)
        }
    )

    if (showHint) {
        HintBottomSheet(hint = question.hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerRootPos = it.positionInRoot() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEBF1FF))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("MATEMATYKA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Uzupełnij równanie", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            Text(
                text = "Przeciągnij brakujący znak w puste pole",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                val eqFontSize = if (maxOf("${question.operand1}".length, "${question.operand2}".length, "${question.result}".length) >= 3) 34.sp else 48.sp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EquationText("${question.operand1}", Color(0xFF1A1A1A), eqFontSize)
                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(2.dp, targetBorderColor, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedOperator != null) accentColor.copy(alpha = 0.1f) else Color.Transparent)
                            .onGloballyPositioned { coords ->
                                targetRootPos = coords.positionInRoot()
                                targetSizePx = Offset(coords.size.width.toFloat(), coords.size.height.toFloat())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedOperator?.symbol ?: "",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    Spacer(Modifier.width(12.dp))
                    EquationText("${question.operand2}", Color(0xFF1A1A1A), eqFontSize)
                    Spacer(Modifier.width(8.dp))
                    EquationText("=", Color(0xFF1A1A1A), eqFontSize)
                    Spacer(Modifier.width(8.dp))
                    EquationText("${question.result}", accentColor, eqFontSize)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                displayOperators.forEach { op ->
                    val isSelected = selectedOperator == op
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) accentColor else Color.White)
                            .onGloballyPositioned { coords ->
                                chipRootPositions[op] = coords.positionInRoot()
                            }
                            .pointerInput(op) {
                                detectTapGestures {
                                    if (draggingOperator == null) playTapHint(op)
                                }
                            }
                            .pointerInput(op) {
                                coroutineScope {
                                    launch {
                                        detectDragGestures(
                                            onDragStart = { localTouchOffset ->
                                                draggingOperator = op
                                                fingerRootPos = (chipRootPositions[op] ?: Offset.Zero) + localTouchOffset
                                            },
                                            onDrag = { change, dragDelta ->
                                                change.consume()
                                                fingerRootPos += dragDelta
                                            },
                                            onDragEnd = {
                                                // Check chip visual position (centre is dragLiftYPx above finger)
                                                // with extra margin on all sides for comfort
                                                val chipCenterY = fingerRootPos.y - dragLiftYPx
                                                val overTarget =
                                                    fingerRootPos.x in (targetRootPos.x - hitMarginPx)..(targetRootPos.x + targetSizePx.x + hitMarginPx) &&
                                                        chipCenterY in (targetRootPos.y - hitMarginPx)..(targetRootPos.y + targetSizePx.y + hitMarginPx)
                                                if (overTarget) {
                                                    selectedOperator = draggingOperator
                                                    isWrong = false
                                                }
                                                draggingOperator = null
                                            },
                                            onDragCancel = { draggingOperator = null }
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = op.symbol,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF1A1A1A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            BottomButtons(
                accentColor = accentColor,
                onHint = { showHint = true },
                onCheck = {
                    if (selectedOperator == question.correctOperator) onCorrect()
                    else onWrong()
                },
                checkEnabled = selectedOperator != null
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Floating ghost during drag
        if (draggingOperator != null) {
            Box(
                modifier = Modifier
                    .offset {
                        val localX = fingerRootPos.x - containerRootPos.x - 32.dp.toPx()
                        val localY = fingerRootPos.y - containerRootPos.y - 88.dp.toPx()
                        IntOffset(localX.roundToInt(), localY.roundToInt())
                    }
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = draggingOperator!!.symbol,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Tap-hint ghost: flies from chip toward the target slot and fades out
        val currentGhostOp = ghostOp
        val chipSizePx = with(LocalDensity.current) { 64.dp.toPx() }
        if (currentGhostOp != null) {
            val chipPos = chipRootPositions[currentGhostOp] ?: Offset.Zero
            val startX = chipPos.x - containerRootPos.x
            val startY = chipPos.y - containerRootPos.y
            val endX = targetRootPos.x - containerRootPos.x + (targetSizePx.x - chipSizePx) / 2f
            val endY = targetRootPos.y - containerRootPos.y + (targetSizePx.y - chipSizePx) / 2f
            val t = ghostProgress.value
            val currentX = startX + (endX - startX) * t
            val currentY = startY + (endY - startY) * t

            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(64.dp)
                    .alpha(ghostAlpha.value)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentGhostOp.symbol,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
