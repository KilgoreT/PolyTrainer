package me.apomazkin.quiz.chat.logic

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок экрана чат-квиза — длящиеся источники данных,
 * управляемые дифф-механикой mate: после каждого reduce раннер
 * сравнивает набор [subscriptions] с активным, включает новые
 * подписки и гасит исчезнувшие.
 *
 * Обе подписки без параметров и живут всё время жизни экрана.
 */
sealed interface ChatSub : Subscription {

    /**
     * Живые тумблеры меню app bar'а (earliest / frequent mistakes /
     * debug) из prefs → [Msg.UpdateMenu].
     */
    data object AppBarMenu : ChatSub

    /**
     * Выбор компонентов квиза текущего словаря: каждая запись
     * pref-ключа `quiz_picker_dict_<id>` (включая начальное значение)
     * → пере-выборка доступных типов и восстановленного выбора →
     * [Msg.QuizComponentTypesLoaded]. Если текущего словаря нет —
     * поток завершается, не эмитя ничего.
     */
    data object QuizPicker : ChatSub
}

/**
 * Декларация подписок экрана: обе нужны безусловно, поэтому набор
 * константный — живут от создания до уничтожения раннера.
 */
fun ChatScreenState.subscriptions(): Set<Subscription> = setOf(
    ChatSub.AppBarMenu,
    ChatSub.QuizPicker,
)
