package me.apomazkin.quiz.chat.logic

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Инвариант темпа чата: пауза бота не короче движения ленты с запасом на
 * кадры старта анимации — иначе сообщения бота встают друг на друга
 * посреди анимации. Страхует ручную правку констант.
 */
class ChatTimingTest {

    @Test
    fun `bot pause covers motion and its start latency`() {
        assertTrue(
            "пауза бота должна покрывать движение и кадры старта",
            ChatTiming.BOT_PAUSE_MIN_MS >= ChatTiming.MOTION_DURATION_MS + ChatTiming.MOTION_LATENCY_MS,
        )
    }

    @Test
    fun `bot pause range is valid`() {
        assertTrue(ChatTiming.BOT_PAUSE_MIN_MS <= ChatTiming.BOT_PAUSE_MAX_MS)
    }
}
