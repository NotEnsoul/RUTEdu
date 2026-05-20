package prz.rutedu.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import prz.rutedu.app.components.NumberKeypad
import prz.rutedu.app.models.Question

@Composable
internal fun FindAnswerContent(
    question: Question.FindAnswer,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {},
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

        NumberKeypad(
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
                else onWrong()
            },
            checkEnabled = input.isNotEmpty()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
