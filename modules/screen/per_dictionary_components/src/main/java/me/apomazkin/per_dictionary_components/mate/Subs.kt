package me.apomazkin.per_dictionary_components.mate

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок экрана компонентов словаря — длящиеся источники
 * данных, управляемые дифф-механикой mate: после каждого reduce
 * раннер сравнивает набор [subscriptions] с активным, включает новые
 * подписки и гасит исчезнувшие.
 */
sealed interface PerDictionaryComponentsSub : Subscription {

    /**
     * Живой список компонентов словаря → [Msg.ItemsLoaded];
     * ошибка потока → [Msg.ItemsLoadFailed] (поток завершается,
     * экран уходит в error state).
     *
     * @param dictionaryId словарь, чьи компоненты слушаем.
     * @param generation различающее поле рестарта: Retry в error
     *   state инкрементит его в state — упавшая подписка гаснет,
     *   новая стартует с тем же dictionaryId.
     */
    data class Components(
        val dictionaryId: Long,
        val generation: Int,
    ) : PerDictionaryComponentsSub
}

/**
 * Декларация подписок экрана: список компонентов нужен безусловно
 * (dictionaryId фиксирован при создании экрана), подписка живёт от
 * создания до уничтожения раннера; меняется только generation при
 * retry.
 */
fun PerDictionaryComponentsScreenState.subscriptions(): Set<Subscription> = setOf(
    PerDictionaryComponentsSub.Components(
        dictionaryId = dictionaryId,
        generation = loadGeneration,
    ),
)
