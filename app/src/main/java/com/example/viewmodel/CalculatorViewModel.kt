package com.example.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryItem
import com.example.data.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

enum class CalculatorMode {
    BASIC,
    SCIENTIFIC,
    MATHS_NOTES,
    CONVERT,
    ABOUT
}

class CalculatorViewModel(private val repository: HistoryRepository) : ViewModel() {

    var currentMode by mutableStateOf(CalculatorMode.BASIC)

    var formulaText by mutableStateOf("")
        private set

    var pendingOperation by mutableStateOf<String?>(null)
        private set
    var isEnteringNewNumber by mutableStateOf(false)
        private set

    var isSecondActive by mutableStateOf(false)
    var isDegreeMode by mutableStateOf(false)
    var memoryValue by mutableStateOf(BigDecimal.ZERO)

    var displayValue by mutableStateOf("0")
        private set

    var showHistory by mutableStateOf(false)

    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var justEvaluated = false

    fun onDigitPressed(digit: String) {
        if (displayValue == "Error") {
            formulaText = ""
            displayValue = "0"
            justEvaluated = false
        }
        if (justEvaluated) {
            formulaText = ""
            justEvaluated = false
        }
        formulaText += digit
    }

    fun onDecimalPressed() {
        if (justEvaluated) {
            formulaText = "0"
            justEvaluated = false
        }
        formulaText += "."
    }

    fun onOperationPressed(operation: String) {
        if (displayValue == "Error") {
            formulaText = ""
            displayValue = "0"
        }
        if (justEvaluated) {
            formulaText = displayValue
            justEvaluated = false
        }
        
        val opStr = when (operation) {
            "x^y" -> "^"
            "y√x" -> "y√"
            else -> operation
        }
        formulaText += opStr
    }

    fun onScientificFunctionPressed(func: String) {
        if (displayValue == "Error") {
            formulaText = ""
            displayValue = "0"
            justEvaluated = false
        }
        
        // Handle parenthesis inputs
        if (func == "(" || func == ")") {
            if (justEvaluated) {
                formulaText = ""
                justEvaluated = false
            }
            formulaText += func
            return
        }

        // Handle Constants
        val constVal = when (func) {
            "π" -> Math.PI
            "e" -> Math.E
            "Rand" -> Math.random()
            else -> null
        }
        if (constVal != null) {
            if (justEvaluated) {
                formulaText = func
                justEvaluated = false
            } else {
                val lastOpIdx = getLastOperandRange(formulaText)
                if (lastOpIdx < formulaText.length && (formulaText[lastOpIdx].isDigit() || formulaText[lastOpIdx] == '.')) {
                    formulaText = formulaText.substring(0, lastOpIdx) + func
                } else {
                    formulaText += func
                }
            }
            isEnteringNewNumber = false
            return
        }

        // Functions that act as postfix or wrap the entire expression so far
        val postfixOps = setOf("x²", "x³", "1/x", "x!")
        if (func in postfixOps) {
            if (justEvaluated) {
                formulaText = displayValue
                justEvaluated = false
            } else if (formulaText.isEmpty()) {
                formulaText = displayValue
            }
            
            val formulaFunc = when (func) {
                "x²" -> "^2"
                "x³" -> "^3"
                "1/x" -> "^(-1)"
                "x!" -> "!"
                else -> ""
            }
            
            val lastOpIdx = getLastOperandRange(formulaText)
            if (lastOpIdx == 0 && !formulaText.startsWith("(")) {
                // Wrap the whole thing
                formulaText = "($formulaText)$formulaFunc"
            } else {
                val lastOperand = formulaText.substring(lastOpIdx)
                formulaText = formulaText.substring(0, lastOpIdx) + "($lastOperand)$formulaFunc"
            }
            return
        }
        
        // Functions that act as prefix with an open parenthesis (e.g. sin(, cos(, 10^(, etc.)
        if (justEvaluated) {
            formulaText = ""
            justEvaluated = false
        }

        val prefixFunc = when (func) {
            "10^x" -> "10^("
            "e^x" -> "e^("
            "log10" -> "log10("
            "log" -> "log("
            "ln" -> "ln("
            "2√x", "√" -> "√("
            "3√x" -> "³√("
            "sin⁻¹" -> "sin⁻¹("
            "cos⁻¹" -> "cos⁻¹("
            "tan⁻¹" -> "tan⁻¹("
            else -> "$func("
        }
        formulaText += prefixFunc
    }

    fun onPercentPressed() {
        if (justEvaluated) {
            formulaText = displayValue
            justEvaluated = false
        }
        formulaText += "%"
    }

    fun onSignTogglePressed() {
        if (justEvaluated) {
            formulaText = displayValue
            justEvaluated = false
        }
        // Basic sign toggle implementation
        if (formulaText.startsWith("-")) {
            formulaText = formulaText.substring(1)
        } else if (formulaText.isNotEmpty()) {
            formulaText = "-$formulaText"
        } else {
            formulaText = "-"
        }
    }

    fun onDeleteLastDigit() {
        if (justEvaluated) {
            formulaText = ""
            displayValue = "0"
            justEvaluated = false
            return
        }
        if (formulaText.isNotEmpty()) {
            formulaText = formulaText.dropLast(1)
        }
        if (formulaText.isEmpty()) {
            displayValue = "0"
        }
    }

    fun onClearPressed() {
        formulaText = ""
        displayValue = "0"
        justEvaluated = false
    }

    fun onMemoryPressed(action: String) {
        try {
            val currentNum = if (formulaText.isNotEmpty()) {
                BigDecimal(Evaluator(isDegreeMode).evaluate(formulaText))
            } else {
                BigDecimal(displayValue.replace(",", ""))
            }
            when (action) {
                "mc" -> memoryValue = BigDecimal.ZERO
                "mr" -> {
                    displayValue = formatResult(memoryValue)
                    formulaText = displayValue
                    justEvaluated = true
                }
                "m+" -> memoryValue = memoryValue.add(currentNum)
                "m-" -> memoryValue = memoryValue.subtract(currentNum)
            }
        } catch (e: Exception) {
            if (action == "mc") {
                memoryValue = BigDecimal.ZERO
            } else if (action == "mr") {
                displayValue = formatResult(memoryValue)
                formulaText = displayValue
                justEvaluated = true
            }
        }
    }

    fun onEEPressed() {
        if (justEvaluated) {
            formulaText = displayValue
            justEvaluated = false
        }
        formulaText += "E"
    }

    fun onEqualPressed() {
        if (formulaText.isEmpty()) return
        try {
            val resultDouble = Evaluator(isDegreeMode).evaluate(formulaText)
            
            // Clean up to BigDecimal
            fun cleanDouble(d: Double): BigDecimal {
                if (d.isNaN()) throw ArithmeticException("Invalid input")
                if (d.isInfinite()) throw ArithmeticException("Overflow")
                val bd = BigDecimal(d).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros()
                return if (bd.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else bd
            }
            
            val result = cleanDouble(resultDouble)
            displayValue = formatResult(result)
            
            viewModelScope.launch {
                repository.insert(HistoryItem(expression = formulaText, result = displayValue))
            }
            
            formulaText = ""
            justEvaluated = true
        } catch (e: Exception) {
            displayValue = "Error"
            justEvaluated = true
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private fun formatResult(result: BigDecimal): String {
        val absVal = result.abs()
        if (absVal == BigDecimal.ZERO) return "0"

        return if (absVal >= BigDecimal("1000000000") || (absVal > BigDecimal.ZERO && absVal < BigDecimal("0.000001"))) {
            val df = DecimalFormat("0.#####E0")
            df.format(result).lowercase().replace("e+", "e")
        } else {
            val scaled = result.stripTrailingZeros()
            var str = scaled.toPlainString()

            if (str.replace("-", "").replace(".", "").length > 9) {
                val integerPartLength = scaled.precision() - scaled.scale()
                val maxDecimals = if (integerPartLength >= 9) 0 else 9 - integerPartLength
                if (maxDecimals > 0) {
                    val rounded = scaled.setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
                    str = rounded.toPlainString()
                } else {
                    val df = DecimalFormat("0.#####E0")
                    return df.format(result).lowercase().replace("e+", "e")
                }
            }
            str
        }
    }

    fun formatDisplayString(valueStr: String): String {
        if (valueStr == "Error" || valueStr == "Infinity" || valueStr == "NaN") return valueStr
        if (valueStr.contains("e", ignoreCase = true)) return valueStr.lowercase()

        val parts = valueStr.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null

        val isNegative = integerPart.startsWith("-")
        val absInteger = if (isNegative) integerPart.substring(1) else integerPart

        val formattedInteger = if (absInteger.isEmpty()) {
            if (isNegative) "-" else ""
        } else {
            val reversed = absInteger.reversed()
            val chunked = reversed.chunked(3)
            val grouped = chunked.joinToString(",").reversed()
            if (isNegative) "-$grouped" else grouped
        }

        return if (decimalPart != null) {
            "$formattedInteger.$decimalPart"
        } else if (valueStr.endsWith(".")) {
            "$formattedInteger."
        } else {
            formattedInteger
        }
    }

    private fun getLastOperandRange(formula: String): Int {
        if (formula.isEmpty()) return 0
        
        // If it ends with ')', find the start of the matching parenthesis, then also include any leading letters/symbols (like 'sin', 'cos', '√')
        if (formula.endsWith(")")) {
            var parenCount = 0
            var openParenIdx = -1
            for (i in formula.length - 1 downTo 0) {
                if (formula[i] == ')') parenCount++
                else if (formula[i] == '(') parenCount--
                if (parenCount == 0) {
                    openParenIdx = i
                    break
                }
            }
            if (openParenIdx != -1) {
                var start = openParenIdx
                while (start > 0 && (formula[start - 1].isLetter() || formula[start - 1] == '√' || formula[start - 1] == '⁻' || formula[start - 1] == '¹' || formula[start - 1] == '²' || formula[start - 1] == '³')) {
                    start--
                }
                return start
            }
            return 0
        }
        
        // Otherwise, scan backwards for the last binary operator
        val operators = listOf('+', '-', '×', '÷', '*', '/', '^', '(')
        for (i in formula.length - 1 downTo 0) {
            if (formula[i] in operators) {
                // Check if this '-' is a unary minus
                if (formula[i] == '-' && (i == 0 || formula[i - 1] in operators)) {
                    return i
                }
                return i + 1
            }
        }
        return 0
    }
}

class Evaluator(private val isDegreeMode: Boolean) {
    private fun preprocess(expr: String): String {
        var str = expr.replace("×", "*").replace("÷", "/").replace(" ", "")
        val sb = StringBuilder()
        for (i in 0 until str.length) {
            val curr = str[i]
            sb.append(curr)
            if (i < str.length - 1) {
                val next = str[i + 1]
                val isCurrEnding = curr.isDigit() || curr == ')' || curr == '%' || curr == '!' || curr == 'π' || curr == 'e'
                val isNextStarting = next == '(' || next == 'π' || next == 'e' || next == '√' || 
                                     next == 's' || next == 'c' || next == 't' || next == 'l' || next == 'R' ||
                                     ((curr == ')' || curr == '%' || curr == '!' || curr == 'π' || curr == 'e') && (next.isDigit() || next == '.'))
                
                if (isCurrEnding && isNextStarting) {
                    if (!(curr == 'e' && next == '^') && !(curr == '3' && next == '√')) {
                        sb.append('*')
                    }
                }
            }
        }
        return sb.toString()
    }

    fun evaluate(expr: String): Double {
        val str = preprocess(expr)
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm(null)
                while (true) {
                    if (eat('+'.code)) x += parseTerm(x) // addition
                    else if (eat('-'.code)) x -= parseTerm(x) // subtraction
                    else return x
                }
            }

            fun parseTerm(addContext: Double?): Double {
                var x = parseFactor(addContext)
                while (true) {
                    if (eat('*'.code)) x *= parseFactor(null) // multiplication
                    else if (eat('/'.code)) x /= parseFactor(null) // division
                    else return x
                }
            }

            fun parseFactor(addContext: Double?): Double {
                if (eat('+'.code)) return parseFactor(addContext) // unary plus
                if (eat('-'.code)) return -parseFactor(addContext) // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, this.pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code || ch == '√'.code || ch == '⁻'.code || ch == '¹'.code || ch == 'π'.code || ch == '²'.code || ch == '³'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code || ch == '√'.code || ch == '⁻'.code || ch == '¹'.code || ch == 'π'.code || ch == '²'.code || ch == '³'.code || (ch >= '0'.code && ch <= '9'.code)) nextChar()
                    val func = str.substring(startPos, this.pos)
                    x = when (func) {
                        "e" -> Math.E
                        "π" -> Math.PI
                        "Rand" -> Math.random()
                        else -> {
                            val arg = parseFactor(null)
                            when (func) {
                                "sin" -> Math.sin(if (isDegreeMode) Math.toRadians(arg) else arg)
                                "cos" -> Math.cos(if (isDegreeMode) Math.toRadians(arg) else arg)
                                "tan" -> {
                                    if (isDegreeMode && arg % 180 == 90.0) throw ArithmeticException("Undefined")
                                    Math.tan(if (isDegreeMode) Math.toRadians(arg) else arg)
                                }
                                "sin⁻¹" -> if (isDegreeMode) Math.toDegrees(Math.asin(arg)) else Math.asin(arg)
                                "cos⁻¹" -> if (isDegreeMode) Math.toDegrees(Math.acos(arg)) else Math.acos(arg)
                                "tan⁻¹" -> if (isDegreeMode) Math.toDegrees(Math.atan(arg)) else Math.atan(arg)
                                "sinh" -> Math.sinh(arg)
                                "cosh" -> Math.cosh(arg)
                                "tanh" -> Math.tanh(arg)
                                "ln" -> Math.log(arg)
                                "log" -> Math.log10(arg)
                                "log10" -> Math.log10(arg)
                                "√" -> Math.sqrt(arg)
                                "cbrt" -> Math.cbrt(arg)
                                "³√" -> Math.cbrt(arg)
                                else -> throw RuntimeException("Unknown function: $func")
                            }
                        }
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                while (true) {
                    if (eat('%'.code)) {
                        if (addContext != null) {
                            x = addContext * (x / 100.0)
                        } else {
                            x /= 100.0
                        }
                    }
                    else if (eat('!'.code)) {
                        var fact = 1.0
                        for (i in 1..x.toLong()) { fact *= i }
                        x = fact
                    }
                    else if (eat('^'.code)) x = Math.pow(x, parseFactor(null))
                    else if (eat('y'.code) && eat('√'.code)) {
                         x = Math.pow(x, 1.0 / parseFactor(null))
                    }
                    else if (eat('E'.code) || eat('e'.code)) {
                        x = x * Math.pow(10.0, parseFactor(null))
                    }
                    else break
                }
                return x
            }
        }.parse()
    }
}
