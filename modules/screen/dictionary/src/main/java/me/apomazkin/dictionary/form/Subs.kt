package me.apomazkin.dictionary.form

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок формы словаря — длящиеся источники данных,
 * управляемые дифф-механикой mate: после каждого reduce раннер
 * сравнивает набор [subscriptions] с активным, включает новые
 * подписки и гасит исчезнувшие.
 */
sealed interface DictionaryFormSub : Subscription {

    /**
     * Живой отфильтрованный список флагов стран для пикера →
     * [DictionaryFormMsg.FlagsUpdated]. Без параметров: фильтр-строка
     * применяется на стороне use case (эффект
     * [FlagFilterEffect.FilterFlags] обновляет её, поток переэмитит).
     */
    data object Flags : DictionaryFormSub
}

/**
 * Декларация подписок формы: поток флагов нужен безусловно, поэтому
 * набор константный — [DictionaryFormSub.Flags] живёт от создания до
 * уничтожения раннера.
 */
fun DictionaryFormScreenState.subscriptions(): Set<Subscription> = setOf(DictionaryFormSub.Flags)
