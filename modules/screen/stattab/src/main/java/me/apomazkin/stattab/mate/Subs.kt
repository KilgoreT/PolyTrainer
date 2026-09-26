package me.apomazkin.stattab.mate

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок экрана статистики — длящиеся источники данных,
 * которые экран слушает через дифф-механику mate: раннер после
 * каждого reduce сравнивает набор [subscriptions] с активным,
 * включает новые подписки и гасит исчезнувшие.
 */
sealed interface StatSub : Subscription {

    /**
     * Живые счётчики (слова / лексемы / статистика квизов) одним
     * потоком. Без параметров: экран показывает статистику всей базы,
     * подписка жива всё время жизни экрана.
     */
    data object Counters : StatSub
}

/**
 * Декларация подписок экрана: счётчики нужны безусловно, поэтому
 * набор константный — [StatSub.Counters] живёт от создания до
 * уничтожения раннера.
 */
fun StatisticState.subscriptions(): Set<Subscription> = setOf(StatSub.Counters)
