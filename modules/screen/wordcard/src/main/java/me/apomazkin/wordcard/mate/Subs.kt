package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок карточки слова — длящиеся источники данных,
 * управляемые дифф-механикой mate: после каждого reduce раннер
 * сравнивает набор [subscriptions] с активным, включает новые
 * подписки и гасит исчезнувшие.
 *
 * Все три подписки параметризуются словом/словарём, которые
 * становятся известны только после загрузки слова (wordState →
 * Loaded) — до этого набор пуст, подписки стартуют сами, как только
 * state их декларирует.
 */
sealed interface WordCardSub : Subscription {
    /**
     * Живой список доступных типов компонентов словаря (driver для
     * ChipsRow) → [Msg.ComponentTypesLoaded]; ошибка потока →
     * [Msg.ComponentTypesLoadFailed] (снек с Retry).
     *
     * @param dictionaryId словарь загруженного слова.
     * @param generation различающее поле рестарта: Retry из снека
     *   инкрементит его в state — упавшая подписка гаснет, новая
     *   стартует.
     */
    data class ComponentTypes(
        val dictionaryId: Long,
        val generation: Int,
    ) : WordCardSub

    /**
     * Живой id-set групп, в которых состоит слово (чипы блока групп)
     * → [Msg.WordGroupsLoaded].
     *
     * @param wordId загруженное слово.
     */
    data class WordGroups(
        val wordId: Long,
    ) : WordCardSub

    /**
     * Живой список всех групп словаря для пикера (Collator-сортировка)
     * → [Msg.DictGroupsLoaded].
     *
     * @param dictionaryId словарь загруженного слова.
     */
    data class DictGroups(
        val dictionaryId: Long,
    ) : WordCardSub
}

/**
 * Декларация подписок карточки: все три требуют загруженного слова
 * (wordId/dictionaryId берутся из [WordState.Loaded]); до загрузки —
 * пустой набор. У типов компонентов меняется только generation при
 * retry.
 */
fun WordCardState.subscriptions(): Set<Subscription> =
    buildSet {
        val loaded = wordState as? WordState.Loaded ?: return@buildSet
        add(
            WordCardSub.ComponentTypes(
                dictionaryId = loaded.dictionaryId,
                generation = typesGeneration,
            ),
        )
        add(WordCardSub.WordGroups(wordId = loaded.id))
        add(WordCardSub.DictGroups(dictionaryId = loaded.dictionaryId))
    }
