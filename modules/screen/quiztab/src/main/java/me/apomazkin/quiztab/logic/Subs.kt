package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок таба «Тренировки»: длящиеся источники данных,
 * которые экран слушает, пока они нужны его состоянию. Раннер mate
 * диффит набор [subscriptions] после каждого reduce.
 */
sealed interface QuizTabSub : Subscription {
    /**
     * IS500: пункты пикера группы chat-карточки. Без параметров —
     * словарь резолвится внутри потока (`flatMapLatest` по текущему
     * словарю), подписка живёт всё время жизни экрана: смена словаря,
     * мутации групп/слов и правки pref'а выбора переэмичивают сами.
     */
    data object GroupOptions : QuizTabSub
}

/**
 * Декларация «что таб слушает»: подписка опций — безусловная,
 * всё время жизни экрана.
 */
fun QuizTabState.subscriptions(): Set<Subscription> = setOf(QuizTabSub.GroupOptions)
