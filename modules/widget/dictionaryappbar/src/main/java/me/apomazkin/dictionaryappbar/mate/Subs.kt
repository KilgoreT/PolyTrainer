package me.apomazkin.dictionaryappbar.mate

import io.github.kilgoret.mate.Sub

/**
 * Семейство подписок app bar'а словарей — длящиеся источники данных,
 * управляемые дифф-механикой mate: после каждого reduce раннер
 * сравнивает набор [subscriptions] с активным, включает новые
 * подписки и гасит исчезнувшие.
 *
 * Обе подписки без параметров и живут всё время жизни виджета:
 * app bar всегда показывает выбранный словарь и держит свежий
 * список для переключателя.
 */
sealed interface DictionaryAppBarSub : Sub {

    /** Живой список доступных словарей → [Msg.AvailableDict]. */
    data object AvailableDicts : DictionaryAppBarSub

    /** Текущий выбранный словарь (null — нет) → [Msg.CurrentDict]. */
    data object CurrentDict : DictionaryAppBarSub
}

/**
 * Декларация подписок app bar'а: обе нужны безусловно, поэтому набор
 * константный — живут от создания до уничтожения раннера.
 */
fun DictionaryAppBarState.subscriptions(): Set<Sub> = setOf(
    DictionaryAppBarSub.AvailableDicts,
    DictionaryAppBarSub.CurrentDict,
)
