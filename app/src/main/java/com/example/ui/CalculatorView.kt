package com.example.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.offset
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
                        CalculatorMode.ABOUT -> "About"
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

                    // Custom Animated Popup Menu
                    if (isModeMenuExpanded) {
                        ModeMenuPopup(
                            isExpanded = isModeMenuExpanded,
                            currentMode = viewModel.currentMode,
                            onDismiss = { isModeMenuExpanded = false },
                            onModeSelected = { mode -> 
                                viewModel.currentMode = mode
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
                    val slideDir = if (initialState.ordinal < targetState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { width: Int -> slideDir * width } + fadeIn(animationSpec = tween(250))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { width: Int -> -slideDir * width } + fadeOut(animationSpec = tween(200)))
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
                    CalculatorMode.ABOUT -> {
                        AboutLayout()
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
                    fontWeight = FontWeight.Bold,
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))

        // Large Output Display Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isLandscape) 4.dp else 12.dp)
        ) {
            val formattedDisplay = viewModel.formatDisplayString(viewModel.displayValue)
            val displayText = if (viewModel.formulaText.isNotEmpty()) viewModel.formulaText else formattedDisplay
            val fontSize = if (isLandscape) {
                when {
                    displayText.length <= 8 -> 44.sp
                    displayText.length == 9 -> 38.sp
                    displayText.length == 10 -> 34.sp
                    else -> 28.sp
                }
            } else {
                when {
                    displayText.length <= 5 -> 64.sp
                    displayText.length == 6 -> 56.sp
                    displayText.length == 7 -> 48.sp
                    displayText.length == 8 -> 42.sp
                    displayText.length == 9 -> 36.sp
                    else -> 30.sp
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Angle Mode Indicator (Rad/Deg)
                Text(
                    text = if (viewModel.isDegreeMode) "DEG" else "RAD",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp, end = 8.dp)
                )

                // Single Unified Display
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
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
                .padding(bottom = if (isLandscape) 4.dp else 16.dp)
        ) {
            val gap = if (isLandscape) 4.dp else 6.dp
            val density = LocalContext.current.resources.displayMetrics.density
            val totalWidthPx = constraints.maxWidth
            val totalWidthDp = totalWidthPx / density

            if (isLandscape) {
                // 10 Columns, 5 Rows (Exact iPhone layout)
                val buttonWidth = (totalWidthDp.dp - (gap * 9)) / 10

                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    // Row 1: (, ), mc, m+, m-, mr, C, +/-, %, ÷
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "(", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("(") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = ")", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(")") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "mc", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("mc") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "m+", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("m+") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "m-", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("m-") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "mr", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("mr") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        val isAllClear = viewModel.displayValue == "0" || viewModel.displayValue == "-0"
                        CalculatorButton(text = if (isAllClear) "AC" else "C", type = ButtonType.UTILITY, onClick = { viewModel.onClearPressed() }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "+/-", type = ButtonType.UTILITY, onClick = { viewModel.onSignTogglePressed() }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "%", type = ButtonType.UTILITY, onClick = { viewModel.onPercentPressed() }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "÷", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "÷" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("÷") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                    }

                    // Row 2: 2nd, x², x³, x^y, e^x, 10^x, 7, 8, 9, ×
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "2nd", type = ButtonType.SCIENTIFIC, isActiveOperation = viewModel.isSecondActive, onClick = { viewModel.isSecondActive = !viewModel.isSecondActive }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "x²", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x²") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "x³", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x³") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "x^y", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onOperationPressed("x^y") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "y^x" else "e^x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "y^x" else "e^x") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "2^x" else "10^x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "2^x" else "10^x") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "7", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("7") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "8", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("8") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "9", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("9") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "×", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "×" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("×") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                    }

                    // Row 3: 1/x, 2√x, 3√x, y√x, ln, log10, 4, 5, 6, -
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "1/x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("1/x") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "2√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("2√x") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "3√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("3√x") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "y√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onOperationPressed("y√x") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "logy" else "ln", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "logy" else "ln") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "log2" else "log10", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "log2" else "log10") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "4", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("4") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "5", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("5") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "6", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("6") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "-", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "-" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("-") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                    }

                    // Row 4: x!, sin, cos, tan, e, EE, 1, 2, 3, +
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "x!", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x!") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "sin⁻¹" else "sin", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "sin⁻¹" else "sin") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "cos⁻¹" else "cos", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "cos⁻¹" else "cos") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "tan⁻¹" else "tan", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "tan⁻¹" else "tan") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "e", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("e") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "EE", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onEEPressed() }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "1", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("1") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "2", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("2") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "3", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("3") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "+", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "+" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("+") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                    }

                    // Row 5: Rad/Deg, sinh, cosh, tanh, π, Rand, 0 (capsule), ., =
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = if (viewModel.isDegreeMode) "Deg" else "Rad", type = ButtonType.SCIENTIFIC, onClick = { viewModel.isDegreeMode = !viewModel.isDegreeMode }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "sinh⁻¹" else "sinh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "sinh⁻¹" else "sinh") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "cosh⁻¹" else "cosh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "cosh⁻¹" else "cosh") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = if (viewModel.isSecondActive) "tanh⁻¹" else "tanh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "tanh⁻¹" else "tanh") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "π", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("π") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "Rand", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("Rand") }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "0", type = ButtonType.NUMBER, isZeroButton = true, onClick = { viewModel.onDigitPressed("0") }, forceCircle = false, modifier = Modifier.width((buttonWidth * 2) + gap))
                        CalculatorButton(text = ".", type = ButtonType.NUMBER, onClick = { viewModel.onDecimalPressed() }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                        CalculatorButton(text = "=", type = ButtonType.OPERATION, onClick = { viewModel.onEqualPressed() }, forceCircle = false, modifier = Modifier.width(buttonWidth))
                    }
                }
            } else {
                // 10-Row iOS Scientific Layout (Matching screenshot exactly)
                val btnWidth6 = (totalWidthDp.dp - (gap * 5)) / 6
                val btnWidth4 = (totalWidthDp.dp - (gap * 3)) / 4
                val btnHeight = btnWidth6 // All buttons have the same height

                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    // --- 6-COLUMN SCIENTIFIC SECTION (TOP 5 ROWS) ---
                    // Row 1: (, ), mc, m+, m-, mr
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "(", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("(") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = ")", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(")") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "mc", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("mc") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "m+", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("m+") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "m-", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("m-") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "mr", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onMemoryPressed("mr") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                    }

                    // Row 2: 2nd, x², x³, xʸ, eˣ, 10ˣ
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "2nd", type = ButtonType.SCIENTIFIC, isActiveOperation = viewModel.isSecondActive, onClick = { viewModel.isSecondActive = !viewModel.isSecondActive }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "x²", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x²") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "x³", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x³") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "xʸ", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onOperationPressed("x^y") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "yˣ" else "eˣ", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "yˣ" else "e^x") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "2ˣ" else "10ˣ", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "2ˣ" else "10^x") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                    }

                    // Row 3: 1/x, ²√x, ³√x, ʸ√x, ln, log₁₀
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "1/x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("1/x") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "²√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("2√x") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "³√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("3√x") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "ʸ√x", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onOperationPressed("y√x") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "log₂" else "ln", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "log₂" else "ln") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "logy" else "log₁₀", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "logy" else "log10") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                    }

                    // Row 4: x!, sin, cos, tan, e, EE
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "x!", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("x!") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "sin⁻¹" else "sin", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "sin⁻¹" else "sin") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "cos⁻¹" else "cos", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "cos⁻¹" else "cos") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "tan⁻¹" else "tan", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "tan⁻¹" else "tan") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "e", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("e") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "EE", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onEEPressed() }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                    }

                    // Row 5: Rand, sinh, cosh, tanh, π, Rad/Deg
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "Rand", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("Rand") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "sinh⁻¹" else "sinh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "sinh⁻¹" else "sinh") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "cosh⁻¹" else "cosh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "cosh⁻¹" else "cosh") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isSecondActive) "tanh⁻¹" else "tanh", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed(if (viewModel.isSecondActive) "tanh⁻¹" else "tanh") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = "π", type = ButtonType.SCIENTIFIC, onClick = { viewModel.onScientificFunctionPressed("π") }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                        CalculatorButton(text = if (viewModel.isDegreeMode) "Deg" else "Rad", type = ButtonType.SCIENTIFIC, onClick = { viewModel.isDegreeMode = !viewModel.isDegreeMode }, forceCircle = true, modifier = Modifier.width(btnWidth6))
                    }

                    // --- 4-COLUMN STANDARD KEYPAD SECTION (BOTTOM 5 ROWS) ---
                    // Row 6: ⌫, AC/C, %, ÷
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "⌫", type = ButtonType.NUMBER, onClick = { viewModel.onDeleteLastDigit() }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        val isAllClear = viewModel.displayValue == "0" || viewModel.displayValue == "-0"
                        CalculatorButton(text = if (isAllClear) "AC" else "C", type = ButtonType.NUMBER, onClick = { viewModel.onClearPressed() }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "%", type = ButtonType.NUMBER, onClick = { viewModel.onPercentPressed() }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "÷", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "÷" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("÷") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                    }

                    // Row 7: 7, 8, 9, ×
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "7", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("7") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "8", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("8") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "9", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("9") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "×", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "×" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("×") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                    }

                    // Row 8: 4, 5, 6, -
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "4", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("4") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "5", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("5") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "6", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("6") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "-", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "-" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("-") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                    }

                    // Row 9: 1, 2, 3, +
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "1", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("1") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "2", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("2") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "3", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("3") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "+", type = ButtonType.OPERATION, isActiveOperation = viewModel.pendingOperation == "+" && viewModel.isEnteringNewNumber, onClick = { viewModel.onOperationPressed("+") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                    }

                    // Row 10: +/- , 0, ., =
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(text = "+/-", type = ButtonType.NUMBER, onClick = { viewModel.onSignTogglePressed() }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "0", type = ButtonType.NUMBER, onClick = { viewModel.onDigitPressed("0") }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = ".", type = ButtonType.NUMBER, onClick = { viewModel.onDecimalPressed() }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                        CalculatorButton(text = "=", type = ButtonType.OPERATION, onClick = { viewModel.onEqualPressed() }, isCapsule = true, forceCircle = false, modifier = Modifier.width(btnWidth4).height(btnHeight))
                    }
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

    val categories = listOf("Length", "Weight", "Temperature", "Currency")

    val unitsMap = mapOf(
        "Length" to listOf("Meters", "Feet", "Inches", "Kilometers", "Miles"),
        "Weight" to listOf("Kilograms", "Pounds", "Ounces", "Grams"),
        "Temperature" to listOf("Celsius", "Fahrenheit", "Kelvin"),
        "Currency" to listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "INR", "BRL", "ZAR", "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "SEK", "NZD", "THB", "UGX")
    )

    val initialCurrencyRates = mapOf("USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 150.0, "AUD" to 1.53, "CAD" to 1.35, "CHF" to 0.88, "CNY" to 7.19, "INR" to 82.9, "BRL" to 5.0, "ZAR" to 19.0, "MXN" to 17.0, "SGD" to 1.34, "HKD" to 7.8, "NOK" to 10.5, "KRW" to 1300.0, "TRY" to 30.0, "RUB" to 90.0, "SEK" to 10.5, "NZD" to 1.6, "THB" to 35.0, "UGX" to 3800.0)

    // Currency API State
    val currencyRates by produceState(initialValue = initialCurrencyRates, selectedCategory) {
        if (selectedCategory == "Currency") {
            try {
                val jsonStr = withContext(Dispatchers.IO) {
                    URL("https://api.frankfurter.app/latest?from=USD").readText()
                }
                val rates = initialCurrencyRates.toMutableMap()
                val ratesObj = JSONObject(jsonStr).getJSONObject("rates")
                ratesObj.keys().forEach { key ->
                    rates[key] = ratesObj.getDouble(key)
                }
                value = rates
            } catch (e: Exception) {
                // Fallback to initial value
            }
        }
    }

    // Trigger update of units if category changes
    LaunchedEffect(selectedCategory) {
        val list = unitsMap[selectedCategory] ?: emptyList()
        if (list.size >= 2) {
            fromUnit = list[0]
            toUnit = list[1]
        }
    }

    // Live calculation
    val resultString = remember(selectedCategory, inputValue, fromUnit, toUnit, currencyRates) {
        val value = inputValue.toDoubleOrNull() ?: 0.0
        val res = when (selectedCategory) {
            "Currency" -> {
                val fromRate = currencyRates[fromUnit] ?: 1.0
                val toRate = currencyRates[toUnit] ?: 1.0
                val usdValue = value / fromRate
                usdValue * toRate
            }
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
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .border(1.dp, Color(0xFF444446), RoundedCornerShape(16.dp))
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
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .border(1.dp, Color(0xFF444446), RoundedCornerShape(16.dp))
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

@Composable
fun ModeMenuPopup(
    isExpanded: Boolean,
    currentMode: CalculatorMode,
    onDismiss: () -> Unit,
    onModeSelected: (CalculatorMode) -> Unit
) {
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(-16, 120),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = false
        )
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2C2C2E))
                    .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(20.dp))
                    .padding(vertical = 8.dp)
            ) {
                val modes = listOf(
                    CalculatorMode.BASIC to Pair("Basic", Icons.Default.Calculate),
                    CalculatorMode.SCIENTIFIC to Pair("Scientific", Icons.Default.Functions),
                    CalculatorMode.MATHS_NOTES to Pair("Maths Notes", Icons.Default.Edit),
                    CalculatorMode.CONVERT to Pair("Convert", Icons.Default.SwapHoriz),
                    CalculatorMode.ABOUT to Pair("About", Icons.Default.Info)
                )
                
                modes.forEach { (mode, pair) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (currentMode == mode) {
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
                            imageVector = pair.second,
                            contentDescription = pair.first,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(pair.first, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutLayout() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var expandedSection by remember { mutableStateOf<String?>(null) }
    
    val sections = listOf(
        Pair("Basic", "Standard calculator for everyday use. Use the history button to view previous calculations."),
        Pair("Scientific", "Advanced functions like trigonometry, logarithms, and roots. Swipe or tap mode menu to access."),
        Pair("Maths Notes", "Jot down your mathematical expressions and let the app evaluate them securely."),
        Pair("Convert", "Instantly convert currencies, units of measure like length, weight, and temperature.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = "App Logo",
                tint = Color(0xFFFF9F0A),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Calculator", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        Text("How to use", color = Color(0xFF8E8E93), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))

        sections.forEach { section ->
            val isExpanded = expandedSection == section.first
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable { expandedSection = if (isExpanded) null else section.first }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.first, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(visible = isExpanded) {
                    Text(
                        text = section.second,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Developer", color = Color(0xFF8E8E93), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
        ) {
            // Email
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("mailto:jessesrekdev@gmail.com") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Jesse Srek", color = Color.White, fontSize = 16.sp)
                    Text("jessesrekdev@gmail.com", color = Color.Gray, fontSize = 14.sp)
                }
            }
            Divider(color = Color(0xFF2C2C2E), modifier = Modifier.padding(start = 56.dp))
            // Telegram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://t.me/jesse_pro") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Telegram", tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Our Community", color = Color.White, fontSize = 16.sp)
                    Text("t.me/jesse_pro", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
