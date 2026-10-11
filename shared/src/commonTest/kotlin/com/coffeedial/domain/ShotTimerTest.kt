package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShotTimerTest {
    @Test fun pauseResumeFirstDropAndReset() {
        var clock = 1000L
        val timer = ShotTimer(now = { clock })
        timer.markFirstDrop()
        assertNull(timer.firstDropMillis)
        timer.start()
        clock += 7500
        timer.markFirstDrop()
        clock += 12500
        timer.markFirstDrop()
        assertEquals(7500L, timer.firstDropMillis)
        timer.pause()
        clock += 5000
        assertEquals(20000L, timer.elapsedMillis)
        timer.start()
        clock += 8300
        assertEquals(28300L, timer.elapsedMillis)
        timer.pause()
        val restored = ShotTimer(timer.elapsedMillis, timer.firstDropMillis)
        assertFalse(restored.running)
        assertEquals(7500L, restored.firstDropMillis)
        timer.reset()
        assertEquals(0L, timer.elapsedMillis)
        assertNull(timer.firstDropMillis)
    }

    @Test fun delayedFramesDoNotLoseTimeAndLimitMatchesShot() {
        var clock = 0L
        val timer = ShotTimer(now = { clock })
        timer.start()
        clock = 30000
        assertEquals(30000L, timer.elapsedMillis)
        clock = 700000
        assertEquals(600000L, timer.elapsedMillis)
        timer.pause()
        timer.start()
        assertFalse(timer.running)
        assertTrue(ShotDraft(seconds = "28.3", firstDrop = "7.5").errors().isEmpty())
        for (value in listOf("-1", "NaN", "30")) {
            assertTrue("firstDrop" in ShotDraft(seconds = "28.3", firstDrop = value).errors())
        }
    }
}
