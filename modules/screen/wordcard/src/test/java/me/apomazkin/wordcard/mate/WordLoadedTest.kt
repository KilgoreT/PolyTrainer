package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.assertSingleEffect
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * §1.1 WordLoaded / WordNotFound / RefreshWord (REWRITE, generic model).
 * TDD red — против будущего API (WordLoaded(word) одно-арг, dictionaryId на Loaded).
 */
class WordLoadedTest {
    private val reducer = WordCardReducer(NoopLogger)

    @Test
    fun `WordLoaded sets word lexemes dictionaryId and declares subscriptions`() {
        val term = stubTerm(
            wordId = 7L,
            dictionaryId = 3L,
            value = "w",
            lexemes = listOf(domainLexeme(1L, listOf(domainCv(5L, 1L, "hi")))),
        )

        val result = reducer.testReduce(WordCardState(), Msg.WordLoaded(term))

        val state = result.state()
        val loaded = state.wordState as WordState.Loaded
        assertEquals(3L, loaded.dictionaryId)
        assertFalse(state.isLoading)
        assertFalse(state.isPendingDbOp)
        val cv = state
            .lexemeList
            .single()
            .components
            .single()
        assertEquals("hi", cv.origin)
        assertEquals(ComponentValueKey.Saved(ComponentValueId(5L)), cv.key)
        // Подписочных эффектов больше нет: wordState стал Loaded, живые
        // источники (типы компонентов, группы) включит дифф subscriptions().
        result.assertNoEffects()
        assertEquals(
            setOf(
                WordCardSub.ComponentTypes(dictionaryId = 3L, generation = 0),
                WordCardSub.WordGroups(wordId = 7L),
                WordCardSub.DictGroups(dictionaryId = 3L),
            ),
            state.subscriptions(),
        )
    }

    /**
     * IS485 (редизайн карточки): `WordLoaded` прокидывает `dictionaryFlagRes` из Term
     * в `WordState.Loaded` — шапка показывает реальный флаг словаря вместо заглушки UK.
     * Full-state assert (R-TR-003): всё незаявленное обязано остаться дефолтным.
     */
    @Test
    fun `WordLoaded carries dictionaryFlagRes into loaded state`() {
        val term = stubTerm(wordId = 7L, dictionaryId = 3L, value = "w", dictionaryFlagRes = 42)

        val result = reducer.testReduce(WordCardState(), Msg.WordLoaded(term))

        val expected = WordCardState(
            isLoading = false,
            isPendingDbOp = false,
            wordState = WordState.Loaded(
                id = 7L,
                dictionaryId = 3L,
                dictionaryFlagRes = 42,
                added = term.addedDate,
                value = "w",
            ),
            lexemeList = emptyList(),
        )
        assertEquals(expected, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `WordLoaded clears pending`() {
        val result = reducer.testReduce(
            WordCardState(isPendingDbOp = true),
            Msg.WordLoaded(stubTerm()),
        )
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `WordLoaded maps multi component preserving order`() {
        val term = stubTerm(
            lexemes = listOf(
                domainLexeme(
                    1L,
                    listOf(
                        domainCv(5L, 1L, "tr", typeId = 50L, ref = TR),
                        domainCv(6L, 1L, "ex", typeId = 51L, ref = ComponentTypeRef.UserDefined("Example"), isMultiple = true),
                    ),
                ),
            ),
        )

        val comps = reducer
            .testReduce(WordCardState(), Msg.WordLoaded(term))
            .state()
            .lexemeList
            .single()
            .components

        assertEquals(2, comps.size)
        assertEquals(TR, comps[0].componentTypeRef)
        assertEquals(true, comps[1].isMultiple)
    }

    @Test
    fun `WordLoaded does not prepopulate availableComponentTypes`() {
        val state = reducer.testReduce(WordCardState(), Msg.WordLoaded(stubTerm())).state()
        assertEquals(emptyList<Any>(), state.availableComponentTypes)
    }

    @Test
    fun `WordLoaded keeps nextPristineKey unchanged and declares subscriptions`() {
        val result = reducer.testReduce(
            WordCardState(nextPristineKey = 5L),
            Msg.WordLoaded(stubTerm()),
        )
        assertEquals(5L, result.state().nextPristineKey)
        result.assertNoEffects()
        assertEquals(
            setOf(
                WordCardSub.ComponentTypes(dictionaryId = 3L, generation = 0),
                WordCardSub.WordGroups(wordId = 7L),
                WordCardSub.DictGroups(dictionaryId = 3L),
            ),
            result.state().subscriptions(),
        )
    }

    @Test
    fun `WordLoaded full rebuild wipes in-flight pristine on reload`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(pristineCv(10L, edited = "draft")))))
        val state = reducer.testReduce(initial, Msg.WordLoaded(stubTerm())).state()
        assertEquals(0, state.lexemeList.sumOf { l -> l.components.count { it.isPristine } })
    }

    @Test
    fun `RefreshWord updates value resets word-edit clears pending keeps lexemes`() {
        val base = loaded(
            isPendingDbOp = true,
            lexemes = listOf(lexeme(1L, listOf(savedCv(5L)))),
        )
        val initial = base.copy(
            wordState = (base.wordState as WordState.Loaded).copy(isEditMode = true, edited = "x"),
        )

        val result = reducer.testReduce(initial, Msg.RefreshWord(stubTerm(value = "w2")))

        val loaded = result.state().wordState as WordState.Loaded
        assertEquals("w2", loaded.value)
        assertFalse(loaded.isEditMode)
        assertEquals("", loaded.edited)
        assertFalse(result.state().isPendingDbOp)
        assertEquals(initial.lexemeList, result.state().lexemeList)
        result.assertNoEffects()
    }

    @Test
    fun `WordNotFound clears loading and navigates back`() {
        val result = reducer.testReduce(WordCardState(isLoading = true), Msg.WordNotFound)
        assertFalse(result.state().isLoading)
        result.assertSingleEffect<NavigationEffect.Back>()
    }
}
