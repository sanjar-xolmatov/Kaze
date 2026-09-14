package app.olauncher.helper.timeutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private var now = 0L
    private val engine = TimerEngine { now }

    @Test
    fun `configure sets duration when idle`() {
        engine.configure(300_000L)
        assertEquals(300_000L, engine.durationMillis)
        assertEquals(300_000L, engine.remaining())
        assertFalse(engine.isRunning)
        assertFalse(engine.hasRun)
    }

    @Test
    fun `start sets end time relative to clock`() {
        engine.configure(30_000L)
        now = 1_000L
        engine.start()
        assertTrue(engine.isRunning)
        assertTrue(engine.hasRun)
        assertEquals(31_000L, engine.endElapsedRealtime)
    }

    @Test
    fun `remaining counts down while running`() {
        engine.configure(30_000L)
        now = 0L
        engine.start()
        now = 5_000L
        assertEquals(25_000L, engine.remaining())
        now = 31_000L
        assertEquals(0L, engine.remaining())
    }

    @Test
    fun `pause freezes remaining and resume continues`() {
        engine.configure(60_000L)
        now = 0L
        engine.start()
        now = 10_000L
        engine.pause()
        assertEquals(50_000L, engine.remaining())
        assertFalse(engine.isRunning)

        now = 70_000L
        assertEquals(50_000L, engine.remaining())

        engine.start()
        assertTrue(engine.isRunning)
        now = 80_000L
        assertEquals(40_000L, engine.remaining())
    }

    @Test
    fun `reset clears all state`() {
        engine.configure(60_000L)
        engine.start()
        engine.reset()
        assertEquals(0L, engine.remaining())
        assertFalse(engine.isRunning)
        assertFalse(engine.hasRun)
        assertEquals(0L, engine.durationMillis)
    }

    @Test
    fun `markFinished stops running and clears remaining`() {
        engine.configure(60_000L)
        engine.start()
        engine.markFinished()
        assertFalse(engine.isRunning)
        assertEquals(0L, engine.remaining())
        assertTrue(engine.hasRun)
    }

    @Test
    fun `configure is a no-op while running`() {
        engine.configure(60_000L)
        engine.start()
        engine.configure(10_000L)
        assertEquals(60_000L, engine.durationMillis)
    }

    @Test
    fun `configure is a no-op after first run`() {
        engine.configure(60_000L)
        engine.start()
        engine.reset()
        engine.configure(10_000L)
        assertEquals(10_000L, engine.durationMillis)
    }

    @Test
    fun `configure caps at max timer`() {
        engine.configure(TimeUtility.MAX_TIMER_MILLIS + 1)
        assertEquals(TimeUtility.MAX_TIMER_MILLIS, engine.durationMillis)
    }

    @Test
    fun `restore reconstructs running state`() {
        now = 5_000L
        engine.restore(
            endElapsedRealtime = 35_000L,
            pausedRemainingMillis = 0L,
            isRunning = true,
            hasRun = true,
            durationMillis = 30_000L,
        )
        assertTrue(engine.isRunning)
        assertEquals(30_000L, engine.remaining())
    }

    @Test
    fun `restore reconstructs paused state`() {
        engine.restore(
            endElapsedRealtime = 0L,
            pausedRemainingMillis = 12_000L,
            isRunning = false,
            hasRun = true,
            durationMillis = 30_000L,
        )
        assertFalse(engine.isRunning)
        assertEquals(12_000L, engine.remaining())
    }
}