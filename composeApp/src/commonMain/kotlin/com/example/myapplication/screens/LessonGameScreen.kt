package com.example.myapplication.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.cash.sqldelight.db.SqlDriver
import com.example.myapplication.data.LessonProgressStore
import com.example.myapplication.data.QuestionBank
import com.example.myapplication.data.SubjectConfigStore
import com.example.myapplication.data.SubjectRepository
import com.example.myapplication.models.Hint
import com.example.myapplication.models.MathOperator
import com.example.myapplication.models.Question
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LessonGameScreen(
    subjectId: String,
    topicId: String,
    lessonId: String,
    navController: NavController,
    driver: SqlDriver,
    bottomPadding: Dp = 0.dp
) {
    val subject = SubjectRepository.getById(subjectId)
    val topic = SubjectRepository.getTopicById(subjectId, topicId)
    val lesson = topic?.lessons?.find { it.id == lessonId }
    val allQuestions = remember { QuestionBank.questionsFor(lessonId) }
    val configuredCount = remember {
        SubjectConfigStore.load(driver, lessonId)?.questionCount?.coerceIn(1, allQuestions.size)
            ?: allQuestions.size
    }
    val questions = remember(configuredCount) { allQuestions.take(configuredCount) }
    val totalCount = questions.size

    val savedProgress = remember { LessonProgressStore.load(driver, lessonId) }

    var currentIndex by remember {
        mutableStateOf(
            when {
                savedProgress == null -> 0
                savedProgress.currentIndex >= totalCount -> totalCount  // complete – keep at end, not 0
                savedProgress.currentIndex >= 0 -> savedProgress.currentIndex
                else -> 0
            }
        )
    }
    var correctCount by remember { mutableStateOf(savedProgress?.correctCount ?: 0) }

    var isComplete by remember {
        mutableStateOf(savedProgress != null && savedProgress.currentIndex >= totalCount && totalCount > 0)
    }

    val latestIndex = rememberUpdatedState(currentIndex)
    val latestCorrect = rememberUpdatedState(correctCount)
    DisposableEffect(Unit) {
        onDispose {
            if (questions.isNotEmpty()) {
                LessonProgressStore.save(
                    driver, lessonId,
                    LessonProgressStore.Progress(latestIndex.value, latestCorrect.value, totalCount)
                )
            }
        }
    }

    val accentColor = subject?.color ?: Color(0xFF4A80F0)

    if (isComplete) {
        LessonCompleteContent(
            subjectName = subject?.name ?: "",
            lessonName = lesson?.name ?: "",
            accentColor = accentColor,
            bottomPadding = bottomPadding,
            onReset = {
                currentIndex = 0
                correctCount = 0
                isComplete = false
                LessonProgressStore.save(driver, lessonId, LessonProgressStore.Progress(0, 0, totalCount))
            },
            onBack = { navController.popBackStack() }
        )
        return
    }

    if (questions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Brak pytań dla tej lekcji", color = Color(0xFF9E9E9E), fontSize = 16.sp)
        }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, totalCount - 1)
    val question = questions[safeIndex]
    val displayQuestion = (currentIndex + 1).coerceAtMost(totalCount)
    val headerProgress = if (totalCount > 0) displayQuestion.toFloat() / totalCount else 0f

    val onAnsweredCorrectly: () -> Unit = {
        val newCorrect = correctCount + 1
        correctCount = newCorrect
        if (currentIndex < totalCount - 1) {
            val newIndex = currentIndex + 1
            currentIndex = newIndex
            LessonProgressStore.save(driver, lessonId, LessonProgressStore.Progress(newIndex, newCorrect, totalCount))
        } else {
            currentIndex = totalCount
            isComplete = true
            LessonProgressStore.save(driver, lessonId, LessonProgressStore.Progress(totalCount, newCorrect, totalCount))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Wróć", tint = Color(0xFF1A1A1A))
                }
                Text(
                    text = subject?.name ?: "Lekcja",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(headerProgress * 100).roundToInt()}% ukończone",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$displayQuestion / $totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(accentColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(headerProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (question) {
                is Question.FindAnswer -> FindAnswerContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly,
                    onSkip = { if (currentIndex < totalCount - 1) currentIndex++ }
                )
                is Question.FindOperator -> FindOperatorContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly,
                    onSkip = { if (currentIndex < totalCount - 1) currentIndex++ }
                )
                is Question.SelectFromList -> SelectFromListContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly
                )
                is Question.TypeAnswer -> TypeAnswerContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly
                )
                is Question.MapQuiz -> MapQuizContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly
                )
                is Question.PeriodicTableQuiz -> PeriodicTableContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly
                )
                is Question.PeriodicTableByShell -> PeriodicTableByShellContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly
                )
                is Question.PeriodicTableByName -> PeriodicTableByNameContent(
                    question = question,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onCorrect = onAnsweredCorrectly
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Completion screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LessonCompleteContent(
    subjectName: String,
    lessonName: String,
    accentColor: Color,
    bottomPadding: Dp,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
            .statusBarsPadding()
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Wróć", tint = Color(0xFF1A1A1A))
            }
            Text(
                text = subjectName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Świetna robota!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = lessonName,
            fontSize = 16.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text("100% ukończone", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Resetuj i zagraj ponownie", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Wróć do lekcji", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 1 – Find the answer (numpad)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FindAnswerContent(
    question: Question.FindAnswer,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onSkip: () -> Unit
) {
    var input by remember(question.id) { mutableStateOf("") }
    var isWrong by remember(question.id) { mutableStateOf(false) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    val inputColor by animateColorAsState(
        when {
            isWrong -> Color(0xFFE53935)
            input.isNotEmpty() -> accentColor
            else -> Color(0xFFBCC1CA)
        }
    )

    if (showHint) {
        HintBottomSheet(hint = question.hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        val eqFontSize = if (maxOf("${question.operand1}".length, "${question.operand2}".length) >= 3) 34.sp else 48.sp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            EquationText("${question.operand1}", Color(0xFF1A1A1A), eqFontSize)
            Spacer(Modifier.width(12.dp))
            EquationText(question.operator.symbol, accentColor, eqFontSize)
            Spacer(Modifier.width(12.dp))
            EquationText("${question.operand2}", Color(0xFF1A1A1A), eqFontSize)
            Spacer(Modifier.width(12.dp))
            EquationText("=", Color(0xFF1A1A1A), eqFontSize)
            Spacer(Modifier.width(12.dp))
            EquationText("?", accentColor, eqFontSize)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.width(180.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (input.isEmpty()) "?" else input,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = inputColor
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(inputColor)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Numpad(
            onDigit = { d -> isWrong = false; if (input.length < 6) input += d },
            onClear = { input = ""; isWrong = false },
            onBackspace = { if (input.isNotEmpty()) input = input.dropLast(1); isWrong = false }
        )

        Spacer(modifier = Modifier.height(16.dp))

        BottomButtons(
            accentColor = accentColor,
            onHint = { showHint = true },
            onCheck = {
                if (input.toIntOrNull() == question.correctAnswer) onCorrect()
                else isWrong = true
            },
            checkEnabled = input.isNotEmpty()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 2 – Find the operator (drag & drop)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FindOperatorContent(
    question: Question.FindOperator,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onSkip: () -> Unit
) {
    var selectedOperator by remember(question.id) { mutableStateOf<MathOperator?>(null) }
    var isWrong by remember(question.id) { mutableStateOf(false) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    // 4 chips: correct + 3 random others, shuffled once per question
    val displayOperators = remember(question.id) {
        val others = MathOperator.entries
            .filter { it != question.correctOperator }
            .shuffled()
            .take(3)
        (others + question.correctOperator).shuffled()
    }

    var draggingOperator by remember { mutableStateOf<MathOperator?>(null) }
    var fingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var containerRootPos by remember { mutableStateOf(Offset.Zero) }
    val chipRootPositions = remember { mutableMapOf<MathOperator, Offset>() }
    var targetRootPos by remember { mutableStateOf(Offset.Zero) }
    var targetSizePx by remember { mutableStateOf(Offset.Zero) }

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
                                                val overTarget =
                                                    fingerRootPos.x in targetRootPos.x..(targetRootPos.x + targetSizePx.x) &&
                                                        fingerRootPos.y in targetRootPos.y..(targetRootPos.y + targetSizePx.y)
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
                    else isWrong = true
                },
                checkEnabled = selectedOperator != null
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (draggingOperator != null) {
            Box(
                modifier = Modifier
                    .offset {
                        val localX = fingerRootPos.x - containerRootPos.x - 32.dp.toPx()
                        val localY = fingerRootPos.y - containerRootPos.y - 32.dp.toPx()
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
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// Screen 3 – Select from list (single / multi-select)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectFromListContent(
    question: Question.SelectFromList,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit
) {
    var selected by remember(question.id) { mutableStateOf(emptySet<Int>()) }
    var isWrong by remember(question.id) { mutableStateOf(false) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    if (showHint) {
        HintBottomSheet(hint = question.hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding)
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = question.prompt,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (question.multiSelect) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Zaznacz wszystkie poprawne odpowiedzi",
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))

        question.options.forEachIndexed { index, option ->
            val isSelected = index in selected
            val borderColor = when {
                isWrong && isSelected -> Color(0xFFE53935)
                isSelected -> accentColor
                else -> Color(0xFFE8EAF0)
            }
            val bgColor = when {
                isWrong && isSelected -> Color(0xFFFFEBEA)
                isSelected -> accentColor.copy(alpha = 0.08f)
                else -> Color.White
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable {
                        isWrong = false
                        selected = if (question.multiSelect) {
                            if (isSelected) selected - index else selected + index
                        } else {
                            setOf(index)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(if (question.multiSelect) RoundedCornerShape(6.dp) else CircleShape)
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (isSelected) accentColor else Color(0xFFBCC1CA),
                            if (question.multiSelect) RoundedCornerShape(6.dp) else CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = option,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        BottomButtons(
            accentColor = accentColor,
            onHint = { showHint = true },
            onCheck = {
                if (selected == question.correctIndices) onCorrect()
                else isWrong = true
            },
            checkEnabled = selected.isNotEmpty()
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen 4 – Type numeric answer (with optional triangle visual)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TypeAnswerContent(
    question: Question.TypeAnswer,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit
) {
    var input by remember(question.id) { mutableStateOf("") }
    var isWrong by remember(question.id) { mutableStateOf(false) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    if (showHint) {
        HintBottomSheet(hint = question.hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            text = question.prompt,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Triangle visual
        if (question.triangleAngles != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                TriangleCanvas(
                    angle1 = question.triangleAngles.first,
                    angle2 = question.triangleAngles.second,
                    color = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(20.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // "Twoja odpowiedź" label
        Text(
            text = "Twoja odpowiedź",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Input field
        val borderColor = if (isWrong) Color(0xFFE53935) else if (input.isNotEmpty()) accentColor else Color(0xFFE8EAF0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextField(
                value = input,
                onValueChange = { v ->
                    if (v.all { it.isDigit() } && v.length <= 4) {
                        input = v
                        isWrong = false
                    }
                },
                placeholder = { Text("Wpisz wynik...", color = Color(0xFFBCC1CA)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (input.toIntOrNull() == question.correctAnswer) onCorrect()
                    else isWrong = true
                }),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            )
            if (question.unit.isNotEmpty()) {
                Text(
                    text = question.unit,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9E9E9E)
                )
            }
        }

        // Inline always-visible hint
        if (question.inlineHint != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.07f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = question.inlineHint,
                    fontSize = 13.sp,
                    color = Color(0xFF4A4A4A),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        BottomButtons(
            accentColor = accentColor,
            onHint = { showHint = true },
            onCheck = {
                if (input.toIntOrNull() == question.correctAnswer) onCorrect()
                else isWrong = true
            },
            checkEnabled = input.isNotEmpty()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TriangleCanvas(
    angle1: Int,
    angle2: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val bLeft  = Offset(w * 0.08f, h * 0.88f)
        val bRight = Offset(w * 0.92f, h * 0.88f)
        val apex   = Offset(w * 0.50f, h * 0.05f)

        val stroke = Stroke(width = 2.5.dp.toPx())

        // Triangle sides
        val triPath = Path().apply {
            moveTo(bLeft.x, bLeft.y)
            lineTo(apex.x, apex.y)
            lineTo(bRight.x, bRight.y)
            lineTo(bLeft.x, bLeft.y)
        }
        drawPath(triPath, color = color, style = stroke)

        // Dashed arc at apex (unknown angle)
        val apexArcR = 28.dp.toPx()
        val apexArcPath = Path().apply {
            addArc(
                oval = Rect(
                    center = apex,
                    radius = apexArcR
                ),
                startAngleDegrees = 100f,
                sweepAngleDegrees = -80f  // approximate visual arc between the two sides
            )
        }
        drawPath(
            apexArcPath,
            color = color.copy(alpha = 0.6f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
        )

        // Small angle arcs at base corners
        val baseArcR = 22.dp.toPx()
        val arc1Path = Path().apply {
            addArc(
                oval = Rect(center = bLeft, radius = baseArcR),
                startAngleDegrees = -60f,
                sweepAngleDegrees = 45f
            )
        }
        drawPath(arc1Path, color = color, style = Stroke(width = 1.5.dp.toPx()))

        val arc2Path = Path().apply {
            addArc(
                oval = Rect(center = bRight, radius = baseArcR),
                startAngleDegrees = 180f + 15f,
                sweepAngleDegrees = 45f
            )
        }
        drawPath(arc2Path, color = color, style = Stroke(width = 1.5.dp.toPx()))

        // Text labels
        val labelStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        val questionStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        val a1Text = textMeasurer.measure("${angle1}°", labelStyle)
        val a2Text = textMeasurer.measure("${angle2}°", labelStyle)
        val qText  = textMeasurer.measure("?", questionStyle)

        // angle1 bottom-left
        drawText(a1Text, topLeft = Offset(bLeft.x - 10.dp.toPx(), bLeft.y - 36.dp.toPx()))
        // angle2 bottom-right
        drawText(a2Text, topLeft = Offset(bRight.x - a2Text.size.width - 6.dp.toPx(), bRight.y - 36.dp.toPx()))
        // ? at apex
        drawText(qText, topLeft = Offset(apex.x - qText.size.width / 2f, apex.y + 32.dp.toPx()))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hint bottom sheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HintBottomSheet(
    hint: Hint,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Podpowiedź",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color(0xFF9E9E9E))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main text with left border and optional bold
            if (hint.mainText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.07f))
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                    val annotated = buildAnnotatedString {
                        val text = hint.mainText
                        val bold = hint.boldPart
                        if (bold != null) {
                            val idx = text.indexOf(bold)
                            if (idx >= 0) {
                                append(text.substring(0, idx))
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                                append(text.substring(idx + bold.length))
                            } else append(text)
                        } else append(text)
                    }
                    Text(
                        text = annotated,
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Section title + items
            if (hint.sectionTitle != null) {
                Text(
                    text = hint.sectionTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            if (hint.items.isNotEmpty()) {
                hint.items.forEachIndexed { index, item ->
                    val dotColor = when (index % 3) {
                        0 -> Color(0xFFBCC1CA)
                        1 -> accentColor
                        else -> accentColor.copy(alpha = 0.5f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (index == 1) accentColor.copy(alpha = 0.08f) else Color(0xFFF5F6FA))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(text = item, fontSize = 14.sp, color = Color(0xFF1A1A1A))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Steps
            if (hint.steps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F6FA))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Krok po kroku:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        hint.steps.forEach { step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9E9E9E),
                                    modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                                )
                                Text(text = step, fontSize = 14.sp, color = Color(0xFF4A4A4A), lineHeight = 20.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Rozumiem, wracam do zadania", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EquationText(text: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit = 48.sp) {
    Text(text = text, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = color)
}

@Composable
private fun BottomButtons(
    accentColor: Color,
    onHint: () -> Unit,
    onCheck: () -> Unit,
    checkEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onHint,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Podpowiedź", color = accentColor, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onCheck,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            enabled = checkEnabled
        ) {
            Text("Sprawdź", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Numpad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = key, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    }
                }
            }
        }
    }
}
