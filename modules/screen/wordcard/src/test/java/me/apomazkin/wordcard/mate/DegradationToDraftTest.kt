package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS486 фаза 3 (В4): деградация лексемы в черновик (spec §12).
 *
 * Каскадное удаление лексемы упразднено: потеря последнего значения (каскад
 * иерархии либо ручное удаление) оставляет лексему живой с пустым списком
 * компонентов — UI показывает её как черновик (Draft-бейдж, чипы добавления).
 * Undo-снек для этого пути не эмитится (значения восстанавливаются каскадным
 * restore на data-слое, не через снек).
 */
class DegradationToDraftTest {

    private val reducer = WordCardReducer()

    @Test
    fun `refresh with empty components keeps lexeme as draft`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(8L, listOf(savedCv(60L, origin = "x", isCommitting = true))),
                lexeme(9L, listOf(savedCv(61L, origin = "y"))),
            ),
        )
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(8L, emptyList()))
        val s = result.state()
        assertEquals("обе лексемы живы", listOf(8L, 9L), s.lexemeList.map { it.id })
        assertTrue("деградировавшая лексема пуста", s.lexemeList.first { it.id == 8L }.components.isEmpty())
        assertEquals("соседняя лексема не тронута", 1, s.lexemeList.first { it.id == 9L }.components.size)
        result.assertNoEffects()
    }

    @Test
    fun `degraded draft can re-add components`() {
        val type = ctype(50L)
        val initial = loaded(
            availableTypes = listOf(type),
            lexemes = listOf(lexeme(8L, emptyList())),
        )
        val addable = initial.addableTypeIdsFor(initial.lexemeList.single())
        assertTrue("пустой черновик снова может добавить тип", type.id in addable)
    }

    // ===== Решение 2026-07-21: черновик живёт только пока карточка открыта =====

    /**
     * Вход: WordLoaded не показывает сохранённые пустые лексемы — они тихо
     * удаляются (PurgeEmptyLexeme, без undo-снека). «Вышел-зашёл — черновика нет».
     */
    @Test
    fun `WordLoaded purges saved empty lexemes silently`() {
        val initial = WordCardState(isLoading = true)
        val term = stubTerm(
            wordId = 7L,
            lexemes = listOf(
                domainLexeme(8L, listOf(domainCv(60L, 8L, "x", ref = TR))),
                domainLexeme(9L, emptyList()),
            ),
        )
        val result = reducer.testReduce(initial, Msg.WordLoaded(term))
        assertEquals("пустая лексема скрыта", listOf(8L), result.state().lexemeList.map { it.id })
        assertTrue(
            "пустая лексема тихо удаляется",
            DatasourceEffect.PurgeEmptyLexeme(7L, 9L) in result.second,
        )
    }

    /** Выход: flush-on-back эмитит тихое удаление пустой сохранённой лексемы. */
    @Test
    fun `exit emits purge for saved empty draft`() {
        val initial = loaded(
            wordId = 7L,
            lexemes = listOf(
                lexeme(8L, listOf(savedCv(60L, origin = "x"))),
                lexeme(9L, emptyList()),
            ),
        )
        val result = reducer.testReduce(initial, Msg.NavigateBack)
        assertTrue(
            "выход удаляет пустой черновик",
            DatasourceEffect.PurgeEmptyLexeme(7L, 9L) in result.second,
        )
    }

    @Test
    fun `degradation does not clear pending or emit undo`() {
        val initial = loaded(
            isPendingDbOp = false,
            lexemes = listOf(lexeme(8L, listOf(savedCv(60L, origin = "x", isCommitting = true)))),
        )
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(8L, emptyList()))
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }
}
