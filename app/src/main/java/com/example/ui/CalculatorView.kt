package com.example.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ButtonType
import com.example.ui.components.CalculatorButton
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.CalculatorMode
import kotlinx.coroutines.launch
import kotlin.math.abs

// Model class to hold sketch line info
data class SketchLine(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float = 6f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorView(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? ComponentActivity }
    val scope = rememberCoroutineScope()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var isModeMenuExpanded by remember { mutableStateOf(false) }

    // Handle physical back press
    BackHandler {
        if (viewModel.showHistory) {
            viewModel.showHistory = false
        } else if (viewModel.displayValue != "0") {
            viewModel.onDeleteLastDigit()
        } else {
            activity?.finish()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Top Navigation & Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: History
                IconButton(
                    onClick = { viewModel.showHistory = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF222222), shape = CircleShape)
                        .testTag("history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Show Calculation History",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title
                Text(
                    text = when (viewModel.currentMode) {
                        CalculatorMode.BASIC -> "Basic"
                        CalculatorMode.SCIENTIFIC -> "Scientific"
                        CalculatorMode.MATHS_NOTES -> "Maths Notes"
                        CalculatorMode.CONVERT -> "Convert"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )

                // Right Button: Mode Selector
                Box {
                    IconButton(
                        onClick = { isModeMenuExpanded = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF222222), shape = CircleShape)
                            .testTag("mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Change Calculator Mode",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // iOS/Bento styled dropdown menu
                    DropdownMenu(
                        expanded = isModeMenuExpanded,
                        onDismissRequest = { isModeMenuExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF1C1B1F))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.currentMode == CalculatorMode.BASIC) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(26.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Basic",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Basic", color = Color.White)
                                }
                            },
                            onClick = {
                                viewModel.currentMode = CalculatorMode.BASIC
                                isModeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.currentMode == CalculatorMode.SCIENTIFIC) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(26.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Functions,
                                        contentDescription = "Scientific",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scientific", color = Color.White)
                                }
                            },
                            onClick = {
                                viewModel.currentMode = CalculatorMode.SCIENTIFIC
                                isModeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.currentMode == CalculatorMode.MATHS_NOTES) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(26.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Maths Notes",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Maths Notes", color = Color.White)
                                }
                            },
                            onClick = {
                                viewModel.currentMode = CalculatorMode.MATHS_NOTES
                                isModeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.currentMode == CalculatorMode.CONVERT) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(26.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Convert",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Convert", color = Color.White)
                                }
                            },
                            onClick = {
                                viewModel.currentMode = CalculatorMode.CONVERT
                                isModeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Render view depending on selected mode
            AnimatedContent(
                targetState = viewModel.currentMode,
                label = "mode_transition",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }
            ) { targetMode ->
                when (targetMode) {
                    CalculatorMode.BASIC -> {
                        BasicCalculatorLayout(viewModel)
                    }
                    CalculatorMode.SCIENTIFIC -> {
                        ScientificCalculatorLayout(viewModel)
                    }
                    CalculatorMode.MATHS_NOTES -> {
                        MathsNotesLayout()
                    }
                    CalculatorMode.CONVERT -> {
                        ConvertLayout()
                    }
                }
            }
        }

        // Sliding Bottom Sheet for History (iOS Pull up style)
        if (viewModel.showHistory) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showHistory = false },
                containerColor = Color(0xFF1C1C1E),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF444446)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Title and Clear Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (historyList.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearHistory() }
                            ) {
                                Text("Clear", color = Color(0xFFFF9F0A), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No History Yet",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Calculations you make will appear here.",
                                    color = Color(0xFF555559),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(historyList, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF2C2C2E), shape = RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.onClearPressed()
                                            viewModel.onDigitPressed("0")
                                            for (ch in item.result) {
                                                if (ch.isDigit()) {
                                                    viewModel.onDigitPressed(ch.toString())
                                                } else if (ch == '.') {
                                                    viewModel.onDecimalPressed()
                                                } else if (ch == '-') {
                                                    viewModel.onSignTogglePressed()
                                                }
                                            }
                                            viewModel.showHistory = false
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.expression,
                                            color = Color(0xFF8E8E93),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = viewModel.formatDisplayString(item.result),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteHistoryItem(item.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = Color(0xFF8E8E93),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun BasicCalculatorLayout(viewModel: CalculatorViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))

        // Large Output Display Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            val formattedDisplay = viewModel.formatDisplayString(viewModel.displayValue)
            val displayText = if (viewModel.formulaText.isNotEmpty()) viewModel.formulaText else formattedDisplay
            val fontSize = when {
                displayText.length <= 5 -> 80.sp
                displayText.length == 6 -> 70.sp
                displayText.length == 7 -> 62.sp
                displayText.length == 8 -> 54.sp
                displayText.length == 9 -> 48.sp
                else -> 40.sp
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Single Unified Display
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("output_display")
                        .pointerInput(Unit) {
                            var accumulatedDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDrag = 0f },
                                onDragEnd = {
                                    if (abs(accumulatedDrag) > 50f) {
                                        viewModel.onDeleteLastDigit()
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    accumulatedDrag += dragAmount
                                }
                            )
                        }
                )
            }
        }

        // Keyboard / Buttons Panel
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            val gap = 12.dp
            val density = LocalContext.current.resources.displayMetrics.density
            val totalWidthPx = constraints.maxWidth
            val totalWidthDp = totalWidthPx / density

            val buttonWidth = (totalWidthDp.dp - (gap * 3)) / 4

            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                // Row 1: Backspace, AC, %, ÷
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Backspace button (instead of +/-)
                    CalculatorButton(
                        text = "⌫",
                        icon = Icons.Default.ArrowBack,
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onDeleteLastDigit() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    val isAllClear = viewModel.displayValue == "0" || viewModel.displayValue == "-0"
                    CalculatorButton(
                        text = if (isAllClear) "AC" else "C",
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onClearPressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "%",
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onPercentPressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "÷",
                        type = ButtonType.OPERATION,
                        isActiveOperation = viewModel.pendingOperation == "÷" && viewModel.isEnteringNewNumber,
                        onClick = { viewModel.onOperationPressed("÷") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(
                        text = "7",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("7") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "8",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("8") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "9",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("9") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "×",
                        type = ButtonType.OPERATION,
                        isActiveOperation = viewModel.pendingOperation == "×" && viewModel.isEnteringNewNumber,
                        onClick = { viewModel.onOperationPressed("×") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                // Row 3: 4, 5, 6, -
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(
                        text = "4",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("4") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "5",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("5") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "6",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("6") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "-",
                        type = ButtonType.OPERATION,
                        isActiveOperation = viewModel.pendingOperation == "-" && viewModel.isEnteringNewNumber,
                        onClick = { viewModel.onOperationPressed("-") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                // Row 4: 1, 2, 3, +
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(
                        text = "1",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("1") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "2",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("2") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "3",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("3") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "+",
                        type = ButtonType.OPERATION,
                        isActiveOperation = viewModel.pendingOperation == "+" && viewModel.isEnteringNewNumber,
                        onClick = { viewModel.onOperationPressed("+") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                // Row 5: 0, ., +/-, =
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(
                        text = "0",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDigitPressed("0") },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = ".",
                        type = ButtonType.NUMBER,
                        onClick = { viewModel.onDecimalPressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "+/-",
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onSignTogglePressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "=",
                        type = ButtonType.OPERATION,
                        onClick = { viewModel.onEqualPressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                }
            }
        }
    }
}

@Composable
fun ScientificCalculatorLayout(viewModel: CalculatorViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))

        // Large Output Display Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            val formattedDisplay = viewModel.formatDisplayString(viewModel.displayValue)
            val displayText = if (viewModel.formulaText.isNotEmpty()) viewModel.formulaText else formattedDisplay
            val fontSize = when {
                displayText.length <= 5 -> 70.sp
                displayText.length == 6 -> 60.sp
                displayText.length == 7 -> 52.sp
                displayText.length == 8 -> 44.sp
                displayText.length == 9 -> 38.sp
                else -> 32.sp
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Single Unified Display
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var accumulatedDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDrag = 0f },
                                onDragEnd = {
                                    if (abs(accumulatedDrag) > 50f) {
                                        viewModel.onDeleteLastDigit()
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    accumulatedDrag += dragAmount
                                }
                            )
                        }
                )
            }
        }

        // Keyboard / Buttons Panel
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            val gap = 12.dp
            val density = LocalContext.current.resources.displayMetrics.density
            val totalWidthPx = constraints.maxWidth
            val totalWidthDp = totalWidthPx / density

            val buttonWidth = (totalWidthDp.dp - (gap * 3)) / 4
            val sciButtonWidth = (totalWidthDp.dp - (gap * 4)) / 4.5f // 4.5 columns visible

            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                
                // Horizontally Scrollable Scientific Functions (2 Rows)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    // Column 1
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "2nd", type = ButtonType.SCIENTIFIC, isActiveOperation = viewModel.isSecondActive, onClick = { viewModel.isSecondActive = !viewModel.isSecondActive }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "x!", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x!") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 2
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "x²", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x²") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "sin⁻¹" else "sin", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "sin⁻¹" else "sin") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 3
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "x³", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x³") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "cos⁻¹" else "cos", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "cos⁻¹" else "cos") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 4
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "x^y", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onOperationPressed("x^y") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "tan⁻¹" else "tan", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "tan⁻¹" else "tan") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 5
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "e^x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("e^x") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "e", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("e") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 6
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "10^x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("10^x") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "EE", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onEEPressed() }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 7
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "1/x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("1/x") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "Rand", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("Rand") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 8
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "2√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("2√x") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "sinh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("sinh") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 9
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "3√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("3√x") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "cosh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("cosh") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 10
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = "y√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onOperationPressed("y√x") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "tanh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("tanh") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 11
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = if (viewModel.isSecondActive) "e^x" else "ln", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "e^x" else "ln") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = "π", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("π") }, modifier = Modifier.width(sciButtonWidth))
                    }
                    // Column 12
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CalculatorButton(text = if (viewModel.isSecondActive) "10^x" else "log10", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "10^x" else "log10") }, modifier = Modifier.width(sciButtonWidth))
                        CalculatorButton(text = if (viewModel.isDegreeMode) "Deg" else "Rad", type = ButtonType.SCIENTIFIC, onClick = { viewModel.isDegreeMode = !viewModel.isDegreeMode }, modifier = Modifier.width(sciButtonWidth))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Standard Calculator 5x4 layout (Same as Basic layout)
                // Row 1: Backspace, AC, %, ÷
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(
                        text = "⌫",
                        icon = Icons.Default.ArrowBack,
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onDeleteLastDigit() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    val isAllClear = viewModel.displayValue == "0" || viewModel.displayValue == "-0"
                    CalculatorButton(
                        text = if (isAllClear) "AC" else "C",
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onClearPressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "%",
                        type = ButtonType.UTILITY,
                        onClick = { viewModel.onPercentPressed() },
                        modifier = Modifier.width(buttonWidth)
                    )
                    CalculatorButton(
                        text = "÷",
                        type = ButtonType.OPERATION,
                        isActiveOperation = viewModel.pendingOperation == "÷" && viewModel.isEnteringNewNumber,
                        onClick = { viewModel.onOperationPressed("÷") },
                        modifier = Modifier.width(buttonWidth)
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(text = "7", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("7") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "8", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("8") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "9", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("9") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "×", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "×" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("×") }, modifier = Modifier.width(buttonWidth))
                }

                // Row 3: 4, 5, 6, -
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(text = "4", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("4") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "5", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("5") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "6", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("6") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "-", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "-" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("-") }, modifier = Modifier.width(buttonWidth))
                }

                // Row 4: 1, 2, 3, +
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(text = "1", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("1") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "2", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("2") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "3", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("3") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "+", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "+" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("+") }, modifier = Modifier.width(buttonWidth))
                }

                // Row 5: 0, ., +/-, =
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorButton(text = "0", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("0") }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = ".", type = ButtonType.NUMBER, onClick = { viewModel.onDecimalPressed() }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "+/-", type = ButtonType.UTILITY, onClick = { viewModel.onSignTogglePressed() }, modifier = Modifier.width(buttonWidth))
                    CalculatorButton(text = "=", type = ButtonType.OPERATION, onClick = { viewModel.onEqualPressed() }, modifier = Modifier.width(buttonWidth))
                }
            }
        }
    }
}

@Composable
fun MathsNotesLayout() {
    val lines = remember { mutableStateListOf<SketchLine>() }
    var currentLinePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFFF1C40F)) } // default bright math yellow

    val penColors = listOf(
        Color(0xFFF1C40F), // Yellow
        Color(0xFF2ECC71), // Green
        Color(0xFF3498DB), // Blue
        Color(0xFFE74C3C), // Red
        Color.White
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151417))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // School notebook grid line background effect
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineSpacing = 32.dp.toPx()
                    val color = Color(0xFF222225)
                    // Horizontal lines
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = color,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f
                        )
                        y += lineSpacing
                    }
                    // Vertical margin line
                    drawLine(
                        color = Color(0xFF4A2525),
                        start = Offset(lineSpacing * 1.5f, 0f),
                        end = Offset(lineSpacing * 1.5f, size.height),
                        strokeWidth = 3f
                    )
                }

                // Title overlay
                Text(
                    text = "Scribble notes and sketch math here...",
                    color = Color(0xFF555559),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(16.dp)
                )

                // Hand-drawing canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentLinePoints = listOf(offset)
                                },
                                onDragEnd = {
                                    if (currentLinePoints.isNotEmpty()) {
                                        lines.add(SketchLine(currentLinePoints, selectedColor))
                                        currentLinePoints = emptyList()
                                    }
                                },
                                onDragCancel = {
                                    currentLinePoints = emptyList()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newPoint = change.position
                                    currentLinePoints = currentLinePoints + newPoint
                                }
                            )
                        }
                ) {
                    // Draw historical lines
                    lines.forEach { line ->
                        if (line.points.size > 1) {
                            val path = Path().apply {
                                val first = line.points.first()
                                moveTo(first.x, first.y)
                                line.points.forEach { pt ->
                                    lineTo(pt.x, pt.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = line.color,
                                style = Stroke(width = line.strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Draw current drawing line
                    if (currentLinePoints.size > 1) {
                        val path = Path().apply {
                            val first = currentLinePoints.first()
                            moveTo(first.x, first.y)
                            currentLinePoints.forEach { pt ->
                                lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = selectedColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colors list
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                penColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, shape = CircleShape)
                            .border(
                                width = if (selectedColor == color) 3.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { lines.clear() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ConvertLayout() {
    var selectedCategory by remember { mutableStateOf("Length") }
    var inputValue by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf("Meters") }
    var toUnit by remember { mutableStateOf("Feet") }

    val categories = listOf("Length", "Weight", "Temperature")

    val unitsMap = mapOf(
        "Length" to listOf("Meters", "Feet", "Inches", "Kilometers", "Miles"),
        "Weight" to listOf("Kilograms", "Pounds", "Ounces", "Grams"),
        "Temperature" to listOf("Celsius", "Fahrenheit", "Kelvin")
    )

    // Trigger update of units if category changes
    LaunchedEffect(selectedCategory) {
        val list = unitsMap[selectedCategory] ?: emptyList()
        if (list.size >= 2) {
            fromUnit = list[0]
            toUnit = list[1]
        }
    }

    // Live calculation
    val resultString = remember(selectedCategory, inputValue, fromUnit, toUnit) {
        val value = inputValue.toDoubleOrNull() ?: 0.0
        val res = when (selectedCategory) {
            "Length" -> {
                // Convert all to meters first
                val meters = when (fromUnit) {
                    "Meters" -> value
                    "Feet" -> value * 0.3048
                    "Inches" -> value * 0.0254
                    "Kilometers" -> value * 1000.0
                    "Miles" -> value * 1609.34
                    else -> value
                }
                // Convert from meters to target
                when (toUnit) {
                    "Meters" -> meters
                    "Feet" -> meters / 0.3048
                    "Inches" -> meters / 0.0254
                    "Kilometers" -> meters / 1000.0
                    "Miles" -> meters / 1609.34
                    else -> meters
                }
            }
            "Weight" -> {
                // Convert all to kg first
                val kg = when (fromUnit) {
                    "Kilograms" -> value
                    "Pounds" -> value * 0.453592
                    "Ounces" -> value * 0.0283495
                    "Grams" -> value * 0.001
                    else -> value
                }
                // Convert from kg to target
                when (toUnit) {
                    "Kilograms" -> kg
                    "Pounds" -> kg / 0.453592
                    "Ounces" -> kg / 0.0283495
                    "Grams" -> kg / 0.001
                    else -> kg
                }
            }
            "Temperature" -> {
                // Convert to Celsius first
                val celsius = when (fromUnit) {
                    "Celsius" -> value
                    "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0
                    "Kelvin" -> value - 273.15
                    else -> value
                }
                // Convert from Celsius to target
                when (toUnit) {
                    "Celsius" -> celsius
                    "Fahrenheit" -> (celsius * 9.0 / 5.0) + 32.0
                    "Kelvin" -> celsius + 273.15
                    else -> celsius
                }
            }
            else -> 0.0
        }
        String.format("%.4f", res).trimEnd('0').trimEnd('.')
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Categories Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color(0xFFFF9F0A) else Color(0xFF252525),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card containing conversion details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // From Value Input
                Text("Convert From:", color = Color(0xFF8E8E93), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF9F0A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Units selection Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // From Unit Select
                    Column(modifier = Modifier.weight(1f)) {
                        Text("From Unit:", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        var fromMenuExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C2E), shape = RoundedCornerShape(8.dp))
                                .clickable { fromMenuExpanded = true }
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(fromUnit, color = Color.White, fontSize = 14.sp)
                            DropdownMenu(
                                expanded = fromMenuExpanded,
                                onDismissRequest = { fromMenuExpanded = false },
                                modifier = Modifier.background(Color(0xFF2C2C2E))
                            ) {
                                (unitsMap[selectedCategory] ?: emptyList()).forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u, color = Color.White) },
                                        onClick = {
                                            fromUnit = u
                                            fromMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // To Unit Select
                    Column(modifier = Modifier.weight(1f)) {
                        Text("To Unit:", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        var toMenuExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C2E), shape = RoundedCornerShape(8.dp))
                                .clickable { toMenuExpanded = true }
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(toUnit, color = Color.White, fontSize = 14.sp)
                            DropdownMenu(
                                expanded = toMenuExpanded,
                                onDismissRequest = { toMenuExpanded = false },
                                modifier = Modifier.background(Color(0xFF2C2C2E))
                            ) {
                                (unitsMap[selectedCategory] ?: emptyList()).forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u, color = Color.White) },
                                        onClick = {
                                            toUnit = u
                                            toMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Result display
                Divider(color = Color(0xFF2C2C2E))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Result:", color = Color(0xFF8E8E93), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$resultString $toUnit",
                    color = Color(0xFFFF9F0A),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
