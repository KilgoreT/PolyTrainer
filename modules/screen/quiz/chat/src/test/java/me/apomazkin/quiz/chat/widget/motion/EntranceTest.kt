package me.apomazkin.quiz.chat.widget.motion

import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.UserMessageOrigin
import org.junit.Assert.assertEquals
import org.junit.Test

/** Таблица правил [entranceFor] — единственного места выбора анимации входа. */
class EntranceTest {

    private fun system(order: Int) = ChatMessage.addSystemMessage(order = order, message = "bot")

    private fun user(order: Int, origin: UserMessageOrigin) = ChatMessage.addUserMessage(
        order = order,
        message = MessageContent.create(text = "me"),
        origin = origin,
    )

    private fun message(message: ChatMessage, isInChain: Boolean = false) =
        ChatItem.Message(message = message, isInChain = isInChain)

    @Test
    fun `known message enters without animation`() {
        assertEquals(Entrance.None, entranceFor(message(system(5)), isNew = false, flightOrder = null))
        assertEquals(
            Entrance.None,
            entranceFor(message(user(5, UserMessageOrigin.SKIP_CHIP)), isNew = false, flightOrder = null),
        )
    }

    @Test
    fun `new system message slides from below, avatar descends only in chain`() {
        assertEquals(
            Entrance.SlideFromBelow(avatarDescends = false),
            entranceFor(message(system(5), isInChain = false), isNew = true, flightOrder = null),
        )
        assertEquals(
            Entrance.SlideFromBelow(avatarDescends = true),
            entranceFor(message(system(5), isInChain = true), isNew = true, flightOrder = null),
        )
    }

    @Test
    fun `new typed answer flies when flight targets it, otherwise slides`() {
        val typed = message(user(7, UserMessageOrigin.INPUT))
        assertEquals(Entrance.Flight, entranceFor(typed, isNew = true, flightOrder = 7))
        assertEquals(
            Entrance.SlideFromBelow(avatarDescends = false),
            entranceFor(typed, isNew = true, flightOrder = null),
        )
        assertEquals(
            Entrance.SlideFromBelow(avatarDescends = false),
            entranceFor(typed, isNew = true, flightOrder = 6),
        )
    }

    @Test
    fun `new message born from a button morphs from it`() {
        listOf(
            UserMessageOrigin.START_BUTTON,
            UserMessageOrigin.SKIP_CHIP,
            UserMessageOrigin.SHOW_ANSWER_CHIP,
        ).forEach { origin ->
            assertEquals(
                "$origin",
                Entrance.MorphFromButton(origin),
                entranceFor(message(user(7, origin)), isNew = true, flightOrder = 7),
            )
        }
    }

    @Test
    fun `action chips slide only when new, start button never animates`() {
        assertEquals(
            Entrance.SlideFromBelow(avatarDescends = false),
            entranceFor(ChatItem.Actions, isNew = true, flightOrder = null),
        )
        assertEquals(Entrance.None, entranceFor(ChatItem.Actions, isNew = false, flightOrder = null))
        assertEquals(Entrance.None, entranceFor(ChatItem.Start, isNew = true, flightOrder = null))
    }
}
