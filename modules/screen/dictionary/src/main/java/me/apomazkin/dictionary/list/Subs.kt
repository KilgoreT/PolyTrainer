package me.apomazkin.dictionary.list

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок экрана списка словарей — длящиеся источники
 * данных, управляемые дифф-механикой mate: после каждого reduce
 * раннер сравнивает набор [subscriptions] с активным, включает
 * новые подписки и гасит исчезнувшие.
 */
sealed interface DictionaryListSub : Subscription {

    /**
     * Живой список словарей → [DictionaryListMsg.DictionariesLoaded].
     * Без параметров: экран показывает все словари, подписка жива
     * всё время жизни экрана.
     */
    data object Dictionaries : DictionaryListSub
}

/**
 * Декларация подписок экрана: список нужен безусловно, поэтому набор
 * константный — [DictionaryListSub.Dictionaries] живёт от создания до
 * уничтожения раннера.
 */
fun DictionaryListScreenState.subscriptions(): Set<Subscription> = setOf(DictionaryListSub.Dictionaries)
