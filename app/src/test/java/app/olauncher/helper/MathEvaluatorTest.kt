package app.olauncher.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MathEvaluatorTest {

    private fun assertEval(expression: String, expected: Double) {
        val actual = MathEvaluator.evaluate(expression)
        assertNotNull("Expected '$expression' to evaluate", actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun `basic arithmetic`() {
        assertEval("12*8", 96.0)
        assertEval("45 / 3", 15.0)
        assertEval("3.5*2", 7.0)
    }

    @Test
    fun `operator precedence`() {
        assertEval("2+3*4", 14.0)
        assertEval("(2+3)*4", 20.0)
        assertEval("2*(3+4)", 14.0)
    }

    @Test
    fun `calculator style percent`() {
        assertEval("150 + 20%", 180.0)
        assertEval("100 + 50%", 150.0)
        assertEval("50%", 0.5)
        assertEval("100 + 100 + 20%", 240.0)
    }

    @Test
    fun `unary minus and whitespace`() {
        assertEval("-5 + 3", -2.0)
        assertEval("-(5)", -5.0)
        assertEval(" 12 * 8 ", 96.0)
    }

    @Test
    fun `invalid input returns null`() {
        assertNull(MathEvaluator.evaluate("12*"))
        assertNull(MathEvaluator.evaluate("abc"))
        assertNull(MathEvaluator.evaluate("1/0"))
        assertNull(MathEvaluator.evaluate("12++"))
        assertNull(MathEvaluator.evaluate("(2+3"))
        assertNull(MathEvaluator.evaluate(""))
        assertNull(MathEvaluator.evaluate("%"))
    }

    @Test
    fun `result formatting strips trailing zeros`() {
        assertEquals("96", MathEvaluator.formatResult(96.0))
        assertEquals("0.5", MathEvaluator.formatResult(0.5))
        assertEquals("0.3", MathEvaluator.formatResult(0.1 + 0.2))
        assertEquals("0", MathEvaluator.formatResult(-0.0))
    }
}