package me.apomazkin.quiz.chat.widget

/** Теги семантики для тестов движения ленты (androidTest): корни элементов и аватар. */
internal object ChatTestTags {
    const val ACTIONS = "chat_actions"

    fun message(order: Int): String = "chat_msg_$order"

    fun avatar(order: Int): String = "chat_avatar_$order"
}
