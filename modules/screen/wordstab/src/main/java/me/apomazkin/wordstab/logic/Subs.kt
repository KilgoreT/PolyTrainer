package me.apomazkin.wordstab.logic

import io.github.kilgoret.mate.Sub

/**
 * Семейство подписок вкладки «Слова» — длящиеся источники данных,
 * управляемые дифф-механикой mate: после каждого reduce раннер
 * сравнивает набор [subscriptions] с активным, включает новые
 * подписки и гасит исчезнувшие.
 */
sealed interface WordsTabSub : Sub {

    /**
     * Текущий выбранный словарь приложения. Без параметров: вкладка
     * слушает глобальный выбор всё время жизни экрана; каждая
     * эмиссия (включая null — «словарей нет») → [Msg.SelectDictionary].
     */
    data object CurrentDict : WordsTabSub
}

/**
 * Декларация подписок вкладки: текущий словарь нужен безусловно,
 * поэтому набор константный — [WordsTabSub.CurrentDict] живёт от
 * создания до уничтожения раннера.
 */
fun WordsTabState.subscriptions(): Set<Sub> = setOf(WordsTabSub.CurrentDict)
