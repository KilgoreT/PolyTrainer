package me.apomazkin.wordcard.mate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты чистой функции [subscriptions]: декларативный вывод набора
 * подписок карточки слова из state. Все три подписки требуют
 * загруженного слова (wordId/dictionaryId берутся из
 * [WordState.Loaded]) — до загрузки набор пуст; generation типов
 * компонентов пробрасывается из state (retry ломает equality и
 * перезапускает упавшую подписку).
 */
class SubsTest {

    @Test
    fun `word not loaded - empty set`() {
        // До WordState.Loaded слушать нечего: параметры подписок неизвестны.
        assertTrue(WordCardState().subscriptions().isEmpty())
    }

    @Test
    fun `word loaded - all three subscriptions from loaded ids`() {
        val state = loaded(wordId = 7L, dictionaryId = 3L)

        assertEquals(
            setOf(
                WordCardSub.ComponentTypes(dictionaryId = 3L, generation = 0),
                WordCardSub.WordGroups(wordId = 7L),
                WordCardSub.DictGroups(dictionaryId = 3L),
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `types generation propagates into ComponentTypes subscription only`() {
        val state = loaded(wordId = 7L, dictionaryId = 3L).copy(typesGeneration = 2)

        assertEquals(
            setOf(
                WordCardSub.ComponentTypes(dictionaryId = 3L, generation = 2),
                WordCardSub.WordGroups(wordId = 7L),
                WordCardSub.DictGroups(dictionaryId = 3L),
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `retry increments generation - only ComponentTypes identity changes`() {
        // Дифф-механика: рестарт возможен только через новую identity подписки;
        // группы при retry типов не перезапускаются.
        val before = loaded().copy(typesGeneration = 1).subscriptions()
        val after = loaded().copy(typesGeneration = 2).subscriptions()

        assertEquals(setOf<Any>(WordCardSub.ComponentTypes(dictionaryId = 3L, generation = 1)), before - after)
        assertEquals(setOf<Any>(WordCardSub.ComponentTypes(dictionaryId = 3L, generation = 2)), after - before)
    }
}
