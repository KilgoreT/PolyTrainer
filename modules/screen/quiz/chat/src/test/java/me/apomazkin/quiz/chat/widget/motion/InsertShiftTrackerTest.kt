package me.apomazkin.quiz.chat.widget.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Правила трекера въезда: когда элемент «новый», кому и какая дистанция
 * выдаётся, что чистится. Раскладка — списком проекций, как её отдаёт
 * лента после измерения (в реверсе индекс 0 — нижний элемент).
 */
class InsertShiftTrackerTest {

    private val actions = "user_actions"

    private fun msg(order: Int, offset: Int) =
        LaidOutItem(key = order.toString(), offset = offset, isAction = false, order = order)

    private fun chips(offset: Int) =
        LaidOutItem(key = actions, offset = offset, isAction = true, order = null)

    @Test
    fun `first layout issues nothing and marks nothing new`() {
        val tracker = InsertShiftTracker()
        assertFalse(tracker.isNewOrder(3))

        tracker.onLaidOut(listOf(msg(2, 0), msg(1, 50)), lastMessageIsNew = false)

        assertNull(tracker.distanceOf("2"))
        assertNull(tracker.distanceOf("1"))
        assertFalse("история не новая", tracker.isNewOrder(2))
        assertTrue("следующий порядок — новый", tracker.isNewOrder(3))
    }

    @Test
    fun `armed insert issues anchor shift to the new message and chips only`() {
        val tracker = InsertShiftTracker()
        tracker.onLaidOut(listOf(msg(2, 0), msg(1, 50)), lastMessageIsNew = false)

        // Вставка сообщения 3 и чипов: прежний нижний (2) уехал на 60.
        tracker.armed = true
        tracker.onLaidOut(
            listOf(chips(0), msg(3, 30), msg(2, 60), msg(1, 110)),
            lastMessageIsNew = true,
        )

        assertEquals(60f, tracker.distanceOf("3"))
        assertEquals(60f, tracker.distanceOf(actions))
        assertNull("старое сообщение не въезжает", tracker.distanceOf("2"))
        assertFalse("после раскладки 3 уже не новое", tracker.isNewOrder(3))
        assertFalse(tracker.armed)
    }

    @Test
    fun `distance is issued once and survives intermediate layouts`() {
        val tracker = InsertShiftTracker()
        tracker.onLaidOut(listOf(msg(1, 0)), lastMessageIsNew = false)
        tracker.armed = true
        tracker.onLaidOut(listOf(msg(2, 0), msg(1, 40)), lastMessageIsNew = true)

        // Промежуточный проход без взвода (например, клавиатура) — дистанция не обнуляется.
        tracker.onLaidOut(listOf(msg(2, 0), msg(1, 40)), lastMessageIsNew = false)

        assertEquals(40f, tracker.distanceOf("2"))
    }

    @Test
    fun `unarmed insert issues zero distance - no slide when scrolled in history`() {
        val tracker = InsertShiftTracker()
        tracker.onLaidOut(listOf(msg(1, 0)), lastMessageIsNew = false)

        tracker.onLaidOut(listOf(msg(2, 0), msg(1, 40)), lastMessageIsNew = true)

        assertEquals(0f, tracker.distanceOf("2"))
        assertEquals(0f, tracker.slideExtra("2", extraPx = 16f))
    }

    @Test
    fun `chips distance is cleared when chips disappear, message distance is kept`() {
        val tracker = InsertShiftTracker()
        tracker.onLaidOut(listOf(msg(1, 0)), lastMessageIsNew = false)
        tracker.armed = true
        tracker.onLaidOut(listOf(chips(0), msg(2, 30), msg(1, 70)), lastMessageIsNew = true)
        assertEquals(70f, tracker.distanceOf(actions))

        // Чипы исчезли (ответ отправлен), сообщение 2 на проход выпало из видимых.
        tracker.onLaidOut(listOf(msg(1, 0)), lastMessageIsNew = false)

        assertNull(tracker.distanceOf(actions))
        assertEquals("дистанция сообщения переживает выпадение из окна", 70f, tracker.distanceOf("2"))
    }

    @Test
    fun `slide extra applies only to a real insert`() {
        val tracker = InsertShiftTracker()
        tracker.onLaidOut(listOf(msg(1, 0)), lastMessageIsNew = false)
        tracker.armed = true
        tracker.onLaidOut(listOf(msg(2, 0), msg(1, 40)), lastMessageIsNew = true)
        assertEquals(16f, tracker.slideExtra("2", extraPx = 16f))

        // Оседание: якорь уехал вниз.
        tracker.armed = true
        tracker.onLaidOut(listOf(msg(3, 0), msg(2, -10), msg(1, 30)), lastMessageIsNew = true)
        assertEquals(-10f, tracker.distanceOf("3"))
        assertEquals(0f, tracker.slideExtra("3", extraPx = 16f))
    }

    @Test
    fun `history scrolled into view after recreation is never new`() {
        val tracker = InsertShiftTracker()
        // После поворота трекер новый, видны сообщения 8 и 9.
        tracker.onLaidOut(listOf(msg(9, 0), msg(8, 40)), lastMessageIsNew = false)

        // Скролл вверх: в раскладку вошло сообщение 7 из истории.
        tracker.onLaidOut(listOf(msg(9, 0), msg(8, 40), msg(7, 80)), lastMessageIsNew = false)

        assertNull(tracker.distanceOf("7"))
        assertFalse(tracker.isNewOrder(7))
        assertTrue(tracker.isNewOrder(10))
    }
}
