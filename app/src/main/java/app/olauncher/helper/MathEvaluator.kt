package app.olauncher.helper

import java.util.Locale

object MathEvaluator {

    private const val DISPLAY_DECIMALS = 10

    fun evaluate(expression: String): Double? {
        if (expression.isBlank()) return null
        val parser = Parser(expression.replace(',', '.'))
        return try {
            val result = parser.parseAdditive(PRECEDING_VALUE_WHEN_NONE)
            if (parser.hasNext()) null
            else if (result.isFinite()) result
            else null
        } catch (_: Exception) {
            null
        }
    }

    fun formatResult(value: Double): String {
        var formatted = String.format(Locale.US, "%.${DISPLAY_DECIMALS}f", value)
            .trimEnd('0')
            .trimEnd('.')
        if (formatted == "-0") formatted = "0"
        if (formatted.isEmpty()) formatted = "0"
        return formatted
    }

    private class Parser(private val source: String) {

        private var position = 0

        private fun peek(): Char? = if (position < source.length) source[position] else null

        fun hasNext(): Boolean {
            skipWhitespace()
            return position < source.length
        }

        private fun skipWhitespace() {
            while (position < source.length && source[position].isWhitespace()) position++
        }

        private fun consume(): Char? {
            val char = peek() ?: throw IllegalArgumentException("Unexpected end of input")
            position++
            return char
        }

        private fun takeNumber(): Double {
            skipWhitespace()
            val start = position
            var hasDot = false
            while (position < source.length) {
                val char = source[position]
                when {
                    char.isDigit() -> position++
                    char == '.' && !hasDot -> {
                        hasDot = true
                        position++
                    }

                    else -> break
                }
            }
            if (position == start) throw IllegalArgumentException("Expected a number")
            return source.substring(start, position).toDouble()
        }

        fun parseAdditive(base: Double): Double {
            var left = parseMultiplicative(base)
            while (true) {
                skipWhitespace()
                val operator = peek()
                if (operator != '+' && operator != '-') break
                consume()
                val right = parseMultiplicative(left)
                left = if (operator == '+') left + right else left - right
            }
            return left
        }

        fun parseMultiplicative(base: Double): Double {
            var left = parseUnary(base)
            while (true) {
                skipWhitespace()
                val operator = peek()
                if (operator != '*' && operator != '/') break
                consume()
                val right = parseUnary(left)
                left = if (operator == '*') left * right else left / right
            }
            return left
        }

        private fun parseUnary(base: Double): Double {
            skipWhitespace()
            val sign = peek()
            if (sign == '-' || sign == '+') {
                consume()
                val value = parseUnary(base)
                return if (sign == '-') -value else value
            }
            var value = parsePrimary(base)
            while (true) {
                skipWhitespace()
                if (peek() != '%') break
                consume()
                value = value * base / 100.0
            }
            return value
        }

        private fun parsePrimary(base: Double): Double {
            skipWhitespace()
            if (peek() == '(') {
                consume()
                val inner = parseAdditive(base)
                skipWhitespace()
                if (peek() != ')') throw IllegalArgumentException("Expected ')'")
                consume()
                return inner
            }
            return takeNumber()
        }
    }

    private const val PRECEDING_VALUE_WHEN_NONE = 1.0
}