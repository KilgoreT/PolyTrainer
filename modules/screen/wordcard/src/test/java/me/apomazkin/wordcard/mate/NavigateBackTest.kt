package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.12 — flush-on-back (маркеры isCommitting + isExiting + edge-triggered пост-шаг).
 * Контракт: 03 §6.1 / §6.2.3 / §4.3.
 *
 * TDD red: ссылается на будущий API (isExiting, hasInFlightCommits, новые Msg/Effect).
 */
class NavigateBackTest {

    private val reducer = WordCardReducer()

    @Test
    fun `clean_back_navigates_immediately`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        assertTrue("isExiting остаётся true", result.state().isExiting)
        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `empty_draft_back_cleans_then_navigates`() {
        val initial = loaded(
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, edited = "")))),
        )

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        assertTrue("пустой черновик дропнут", result.state().lexemeList.none { it.id == NOT_IN_DB })
        assertTrue(result.state().isExiting)
        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `dirty_saved_back_sets_exiting_no_navigate_yet`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(7L, listOf(savedCv(5L, isEdit = true, edited = "new", origin = "old"))),
            ),
        )

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        val cv = result.state().lexemeList.first().components.first()
        assertTrue("компонент in-flight", cv.isCommitting)
        assertTrue("edit держится", cv.isEdit)
        assertTrue("isExiting", result.state().isExiting)
        assertTrue("pending", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L,
                    dictionaryId = 3L,
                    lexemeId = 7L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L),
                    componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
            ),
        )
        // Back НЕ эмитится — есть in-flight (assertEffects выше пинит точный set без Back).
    }

    @Test
    fun `dirty_NOT_IN_DB_draft_back_waits_for_create`() {
        val initial = loaded(
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t")))),
        )

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        val cv = result.state().lexemeList.first().components.first()
        assertTrue("anchor pristine in-flight", cv.isCommitting)
        assertTrue("isExiting", result.state().isExiting)
        assertTrue("pending", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.CreateLexeme(
                    wordId = 7L,
                    dictionaryId = 3L,
                    pristineKey = 1L,
                    componentTypeId = ComponentTypeId(50L),
                    componentTypeRef = TR,
                    data = textValuesOf("t"),
                ),
            ),
        )
    }

    @Test
    fun `flush_done_navigates`() {
        val initial = loaded(
            isExiting = true,
            lexemes = listOf(
                lexeme(
                    7L,
                    listOf(savedCv(5L, isEdit = true, isCommitting = true, origin = "old", edited = "new")),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshLexemeComponents(lexemeId = 7L, components = listOf(domainCv(5L, 7L, "new"))),
        )

        val cv = result.state().lexemeList.first().components.first()
        assertFalse("edit закрыт", cv.isEdit)
        assertFalse("isCommitting снят", cv.isCommitting)
        assertEquals("origin обновлён", "new", cv.origin)
        assertTrue("isExiting остаётся true (выход через переход)", result.state().isExiting)
        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `flush_still_has_inflight_no_navigate`() {
        val initial = loaded(
            isExiting = true,
            lexemes = listOf(
                lexeme(
                    55L,
                    listOf(
                        savedCv(99L),
                        pristineCv(2L, isCommitting = true, edited = "a"),
                        pristineCv(3L, isCommitting = true, edited = "b"),
                    ),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.ComponentValueInserted(lexemeId = 55L, pristineKey = 2L, newCvId = ComponentValueId(77L)),
        )

        val comps = result.state().lexemeList.first().components
        val survivor3 = comps.first { it.pristineKey == 3L }
        assertTrue("survivor #3 всё ещё in-flight", survivor3.isCommitting)
        result.assertNoEffects("есть незавершённый survivor → без Back")
    }

    @Test
    fun `survivor_AddValue_fail_no_hang`() {
        val initial = loaded(
            isExiting = true,
            lexemes = listOf(
                lexeme(
                    55L,
                    listOf(savedCv(99L), pristineCv(2L, isCommitting = true, edited = "ex")),
                ),
            ),
        )

        val result = reducer.testReduce(initial, Msg.OperationFailed(messageRes = R_STRING_X))

        val survivor = result.state().lexemeList.first().components.first { it.pristineKey == 2L }
        assertFalse("isCommitting снят даже у pristine — лоадер не виснет", survivor.isCommitting)
        assertEquals("ввод цел", "ex", survivor.edited)
        assertFalse("isExiting снят", result.state().isExiting)
        result.assertSingleEffect<UiEffect.ShowErrorSnackbar>()
    }

    @Test
    fun `flush_fail_cancels_exit_stays`() {
        val initial = loaded(
            isExiting = true,
            isPendingDbOp = true,
            lexemes = listOf(
                lexeme(
                    7L,
                    listOf(savedCv(5L, isEdit = true, isCommitting = true, edited = "new", origin = "old")),
                ),
            ),
        )

        val result = reducer.testReduce(initial, Msg.OperationFailed(messageRes = R_STRING_X))

        val cv = result.state().lexemeList.first().components.first()
        assertFalse("isExiting снят — остаёмся", result.state().isExiting)
        assertFalse("pending снят", result.state().isPendingDbOp)
        assertFalse("isCommitting снят", cv.isCommitting)
        assertTrue("edit цел", cv.isEdit)
        assertEquals("текст цел", "new", cv.edited)
        result.assertSingleEffect<UiEffect.ShowErrorSnackbar>()
    }

    @Test
    fun `late_msg_after_back_no_double_nav`() {
        // Back уже эмитнут ранее, isExiting ОСТАЁТСЯ true, in-flight нет.
        val initial = loaded(isExiting = true, lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))

        val result = reducer.testReduce(initial, Msg.RefreshWord(stubTerm()))

        result.assertNoEffects("readyBefore==readyNow → перехода нет → второго Back нет")
    }

    @Test
    fun `clean_double_back_no_second_nav`() {
        // Первый Back уже эмитнут (isExiting=true, in-flight нет) — второй тап «назад».
        val initial = loaded(isExiting = true, lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        result.assertNoEffects("второй NavigateBack — no-op, второго Back нет")
    }

    @Test
    fun `second_back_while_exiting_with_inflight_noop`() {
        val initial = loaded(
            isExiting = true,
            lexemes = listOf(lexeme(7L, listOf(savedCv(5L, isCommitting = true)))),
        )

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        assertEquals("state без изменений", initial, result.state())
        result.assertNoEffects()
    }
}
