package me.apomazkin.vocabulary.logic

import io.github.kilgoret.mate.Sub

/**
 * Семейство подписок host'а вкладок словаря — длящиеся источники
 * данных, управляемые дифф-механикой mate: после каждого reduce
 * раннер сравнивает набор [subscriptions] с активным, включает
 * новые подписки и гасит исчезнувшие.
 */
sealed interface VocabularyHostSub : Sub {

    /**
     * Текущий выбранный словарь приложения. Без параметров: host
     * слушает глобальный выбор всё время жизни экрана; каждая
     * эмиссия (включая null — «словарей нет») проводится вкладкам
     * через [Msg.DictionaryChanged].
     */
    data object CurrentDict : VocabularyHostSub
}

/**
 * Декларация подписок host'а: текущий словарь нужен безусловно,
 * поэтому набор константный — [VocabularyHostSub.CurrentDict] живёт
 * от создания до уничтожения раннера.
 */
fun VocabularyHostState.subscriptions(): Set<Sub> = setOf(VocabularyHostSub.CurrentDict)
