package app.olauncher.universal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorProviderTest {

    @Test
    fun `simple arithmetic is evaluated`() {
        assertEquals("1200", CalculatorProvider.evaluate("25*48"))
        assertEquals("4", CalculatorProvider.evaluate("2 + 2"))
        assertEquals("2", CalculatorProvider.evaluate("8/4"))
        assertEquals("10", CalculatorProvider.evaluate("10+0"))
        assertEquals("3", CalculatorProvider.evaluate("(1+2)"))
    }

    @Test
    fun `percent relates to the preceding value`() {
        assertEquals("0.1", CalculatorProvider.evaluate("10%"))
        // 120% of 25 -> 30
        assertEquals("30", CalculatorProvider.evaluate("120%*25"))
    }

    @Test
    fun `words without any math operator are not math`() {
        assertNull(CalculatorProvider.evaluate("firefox"))
        assertNull(CalculatorProvider.evaluate("hello world"))
        assertNull(CalculatorProvider.evaluate("timer 5m"))
    }

    @Test
    fun `blank and bang queries are not math`() {
        assertNull(CalculatorProvider.evaluate(""))
        assertNull(CalculatorProvider.evaluate("   "))
        assertNull(CalculatorProvider.evaluate("!bang"))
    }

    @Test
    fun `malformed expressions fall through`() {
        assertNull(CalculatorProvider.evaluate("100/"))
        assertNull(CalculatorProvider.evaluate("2++"))
        assertNull(CalculatorProvider.evaluate("(2+3"))
    }

    @Test
    fun `search emits a CALCULATOR result`() {
        val results = CalculatorProvider.search("25*48")
        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(SearchType.CALCULATOR, result.type)
        assertEquals("1200", result.title)
        assertEquals(CalculatorProvider.PRIORITY, result.weight)
        assertTrue(result.isApp.not())
    }
}