package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.models.Question

@Composable
internal fun EquationBalanceContent(
    question: Question.EquationBalance,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit
) {
    val blankPositions = remember(question.id) {
        buildList {
            question.reactants.forEachIndexed { i, t ->
                if (t.fixedCoefficient == null) add(i to (t.correctCoefficient ?: 1))
            }
            val offset = question.reactants.size
            question.products.forEachIndexed { i, t ->
                if (t.fixedCoefficient == null) add((offset + i) to (t.correctCoefficient ?: 1))
            }
        }
    }

    var inputs by remember(question.id) { mutableStateOf(mapOf<Int, String>()) }
    var activePos by remember(question.id) { mutableStateOf(blankPositions.firstOrNull()?.first ?: -1) }
    var isWrong by remember(question.id) { mutableStateOf(false) }
    var showHint by remember(question.id) { mutableStateOf(false) }

    val allFilled = blankPositions.all { (pos, _) -> inputs[pos]?.isNotEmpty() == true }

    fun advanceToNext(currentPos: Int) {
        val nextIdx = blankPositions.indexOfFirst { it.first == currentPos } + 1
        if (nextIdx < blankPositions.size) activePos = blankPositions[nextIdx].first
    }

    if (showHint) {
        HintBottomSheet(hint = question.hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = question.instruction,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = question.subInstruction,
            fontSize = 13.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE8EAF0), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                question.reactants.forEachIndexed { i, term ->
                    if (i > 0) Text("+", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    val pos = i
                    EquationTermCell(
                        formula = term.formula,
                        coefficient = if (term.fixedCoefficient == null) inputs[pos] ?: "" else term.fixedCoefficient.toString(),
                        isBlank = term.fixedCoefficient == null,
                        isActive = pos == activePos,
                        isWrong = isWrong && term.fixedCoefficient == null,
                        accentColor = accentColor,
                        onClick = if (term.fixedCoefficient == null) { { activePos = pos; isWrong = false } } else null
                    )
                }

                Text("→", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))

                question.products.forEachIndexed { i, term ->
                    if (i > 0) Text("+", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    val pos = question.reactants.size + i
                    EquationTermCell(
                        formula = term.formula,
                        coefficient = if (term.fixedCoefficient == null) inputs[pos] ?: "" else term.fixedCoefficient.toString(),
                        isBlank = term.fixedCoefficient == null,
                        isActive = pos == activePos,
                        isWrong = isWrong && term.fixedCoefficient == null,
                        accentColor = accentColor,
                        onClick = if (term.fixedCoefficient == null) { { activePos = pos; isWrong = false } } else null
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Numpad(
            onDigit = { digit ->
                if (activePos >= 0) {
                    val current = inputs[activePos] ?: ""
                    if (current.length < 2) {
                        val updated = current + digit
                        inputs = inputs + (activePos to updated)
                        if (updated.isNotEmpty()) advanceToNext(activePos)
                    }
                }
            },
            onClear = { if (activePos >= 0) inputs = inputs + (activePos to "") },
            onBackspace = {
                if (activePos >= 0) {
                    val current = inputs[activePos] ?: ""
                    inputs = inputs + (activePos to current.dropLast(1))
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        BottomButtons(
            accentColor = accentColor,
            onHint = { showHint = true },
            onCheck = {
                if (blankPositions.all { (pos, correct) -> inputs[pos]?.toIntOrNull() == correct }) onCorrect()
                else isWrong = true
            },
            checkEnabled = allFilled
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun EquationTermCell(
    formula: String,
    coefficient: String,
    isBlank: Boolean,
    isActive: Boolean,
    isWrong: Boolean,
    accentColor: Color,
    onClick: (() -> Unit)?
) {
    val borderColor = when {
        isWrong -> Color(0xFFE53935)
        isActive -> accentColor
        isBlank -> Color(0xFFBCC1CA)
        else -> Color(0xFFE8EAF0)
    }
    val bgColor = when {
        isWrong -> Color(0xFFFFEBEA)
        isActive -> accentColor.copy(alpha = 0.08f)
        else -> Color.White
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(1.5.dp, borderColor, CircleShape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isBlank && coefficient.isEmpty()) "?" else coefficient,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isBlank && coefficient.isEmpty() -> Color(0xFFBCC1CA)
                    isActive -> accentColor
                    else -> Color(0xFF1A1A1A)
                }
            )
        }
        Text(text = formula, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
    }
}
