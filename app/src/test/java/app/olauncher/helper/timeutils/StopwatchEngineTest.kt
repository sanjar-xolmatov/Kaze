package app.olauncher.helper.timeutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchEngineTest {

    private var now = 0L
    private val engine = StopwatchEngine { now }

    @Test
    fun `starts at zero`() {
        assertEquals(0L, engine.elapsed())
        assertFalse(engine.isRunning)
        engine.start()
        assertTrue(engine.isRunning)
    }

    @Test
    fun `elapsed grows while running`() {
        engine.start()
        now = 1_234L
        assertEquals(1_234L, engine.elapsed())
        now = 5_000L
        assertEquals(5_000L, engine.elapsed())
    }

    @Test
    fun `pause freezes elapsed and resume continues`() {
        engine.start()
        now = 1_000L
        engine.pause()
        assertEquals(1_000L, engine.elapsed())
        assertFalse(engine.isRunning)

        now = 10_000L
        assertEquals(1_000L, engine.elapsed())

        engine.start()
        now = 10_500L
        assertEquals(1_500L, engine.elapsed())
    }

    @Test
    fun `reset clears state`() {
        engine.start()
        now = 2_000L
        engine.pause()
        engine.reset()
        assertEquals(0L, engine.elapsed())
        assertFalse(engine.isRunning)
    }

    @Test
    fun `restore reconstructs running state`() {
        now = 7_000L
        engine.restore(
            accumulatedMillis = 3_000L,
            startElapsedRealtime = 7_000L,
            isRunning = true,
        )
        assertTrue(engine.isRunning)
        assertEquals(3_000L, engine.elapsed())

        now = 9_000L
        assertEquals(5_000L, engine.elapsed())
    }

    @Test
    fun `restore reconstructs paused state`() {
        engine.restore(
            accumulatedMillis = 4_000L,
            startElapsedRealtime = 0L,
            isRunning = false,
        )
        assertFalse(engine.isRunning)
        assertEquals(4_000L, engine.elapsed())
    }
}