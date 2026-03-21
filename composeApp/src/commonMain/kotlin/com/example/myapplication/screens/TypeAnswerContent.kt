package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.models.Question

@Composable
internal fun TypeAnswerContent(
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
internal fun TriangleCanvas(
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
