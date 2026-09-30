package me.apomazkin.quiz.chat.widget.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Стыковка соседей с въезжающим элементом: [dockedFraction]. */
class DockedFractionTest {

    private val extra = 16f
    private val distance = 48f
    private val delta = 0.0001f

    @Test
    fun `neighbor stays until the new element has passed the extra`() {
        // Новый прошёл ровно добавку: (extra + d) * e == extra.
        val eDock = extra / (extra + distance)
        assertEquals(0f, dockedFraction(extra, distance, eased = 0f), delta)
        assertEquals(0f, dockedFraction(extra, distance, eased = eDock / 2), delta)
        assertEquals(0f, dockedFraction(extra, distance, eased = eDock), delta)
    }

    @Test
    fun `after docking neighbor shares the remaining path with the new element`() {
        val e = 0.75f
        val newRemaining = (extra + distance) * (1f - e)
        val neighborRemaining = distance * (1f - dockedFraction(extra, distance, e))
        assertEquals(newRemaining, neighborRemaining, delta)
    }

    @Test
    fun `ends at the target and is monotonic`() {
        assertEquals(1f, dockedFraction(extra, distance, eased = 1f), delta)
        var prev = 0f
        var e = 0f
        while (e <= 1f) {
            val f = dockedFraction(extra, distance, e)
            assertTrue("монотонно при e=$e", f >= prev)
            prev = f
            e += 0.05f
        }
    }

    @Test
    fun `zero shift means already in place`() {
        assertEquals(1f, dockedFraction(extra, 0f, eased = 0f), delta)
    }

    @Test
    fun `negative shift docks by absolute distance`() {
        assertEquals(dockedFraction(extra, distance, 0.5f), dockedFraction(extra, -distance, 0.5f), delta)
    }
}
