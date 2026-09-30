package me.apomazkin.quiz.chat.widget.motion

import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.UserMessageOrigin

/** Элемент ленты с точки зрения анимации входа. */
internal sealed interface ChatItem {
    /** Сообщение; [isInChain] — продолжает цепочку системных (аватар едет по ней). */
    data class Message(val message: ChatMessage, val isInChain: Boolean) : ChatItem

    /** Ряд чипов «Показать ответ» / «Пропустить» под вопросом. */
    data object Actions : ChatItem

    /** Кнопка «Начать» под приветствием. */
    data object Start : ChatItem
}

/** Как элемент входит в ленту на апдейте, в котором родился. */
internal sealed interface Entrance {
    /** Не новый элемент (история, первая композиция) или кнопка «Начать» — без анимации входа. */
    data object None : Entrance

    /** Выходит из-под поля ввода и стыкуется с колонкой; [avatarDescends] — аватар едет по цепочке. */
    data class SlideFromBelow(val avatarDescends: Boolean) : Entrance

    /** Пузырь юзера превращается из системной кнопки на её месте. */
    data class MorphFromButton(val origin: UserMessageOrigin) : Entrance

    /** Набранный ответ: к пузырю летит копия текста из поля, сам пузырь скрыт до прилёта. */
    data object Flight : Entrance
}

/**
 * Единственное место выбора анимации входа. [isNew] — элемент появился в
 * этом апдейте (для сообщений — порядок выше всех размещённых, для чипов
 * — новое последнее сообщение); [flightOrder] — сообщение, к которому
 * сейчас летит копия текста из поля.
 */
internal fun entranceFor(
    item: ChatItem,
    isNew: Boolean,
    flightOrder: Int?,
): Entrance = when (item) {
    is ChatItem.Start -> Entrance.None
    is ChatItem.Actions -> if (isNew) Entrance.SlideFromBelow(avatarDescends = false) else Entrance.None
    is ChatItem.Message -> {
        val message = item.message
        when {
            !isNew -> Entrance.None
            message.isSystemMessage -> Entrance.SlideFromBelow(avatarDescends = item.isInChain)
            message.origin == UserMessageOrigin.INPUT && flightOrder == message.order -> Entrance.Flight
            message.origin == UserMessageOrigin.INPUT -> Entrance.SlideFromBelow(avatarDescends = false)
            else -> Entrance.MorphFromButton(message.origin)
        }
    }
}
