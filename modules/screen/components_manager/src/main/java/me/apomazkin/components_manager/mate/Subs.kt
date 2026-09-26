package me.apomazkin.components_manager.mate

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок экрана менеджера компонентов — длящиеся
 * источники данных, управляемые дифф-механикой mate: после каждого
 * reduce раннер сравнивает набор [subscriptions] с активным,
 * включает новые подписки и гасит исчезнувшие.
 */
sealed interface ComponentsManagerSub : Subscription {
    /**
     * Живой список всех пользовательских типов компонентов →
     * [Msg.TypesLoaded]; ошибка потока → [Msg.TypesLoadFailed]
     * (поток завершается, экран уходит в error state).
     *
     * @param generation различающее поле рестарта: Retry в error
     *   state инкрементит его в state — упавшая подписка гаснет,
     *   новая стартует.
     */
    data class AllTypes(
        val generation: Int,
    ) : ComponentsManagerSub

    /**
     * Живой список словарей для multi-dict scope picker'а в
     * Create-диалоге → [Msg.DictionariesLoaded]. Ошибка потока
     * деградирует до пустого списка (chip-list скрывается, остаётся
     * Global) — retry не предусмотрен.
     */
    data object Dictionaries : ComponentsManagerSub
}

/**
 * Декларация подписок экрана: обе нужны безусловно и живут от
 * создания до уничтожения раннера; у списка типов меняется только
 * generation при retry.
 */
fun ComponentsManagerScreenState.subscriptions(): Set<Subscription> =
    setOf(
        ComponentsManagerSub.AllTypes(generation = loadGeneration),
        ComponentsManagerSub.Dictionaries,
    )
