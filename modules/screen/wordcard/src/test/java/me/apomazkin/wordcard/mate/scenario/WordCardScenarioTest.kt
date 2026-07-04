package me.apomazkin.wordcard.mate.scenario

import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.wordcard.mate.ComponentValueKey
import me.apomazkin.wordcard.mate.DatasourceEffect
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.NOT_IN_DB
import me.apomazkin.wordcard.mate.UiEffect
import me.apomazkin.wordcard.mate.WordCardReducer
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.ctype
import me.apomazkin.wordcard.mate.domainCv
import me.apomazkin.wordcard.mate.domainLexeme
import me.apomazkin.wordcard.mate.lexeme
import me.apomazkin.wordcard.mate.loaded
import me.apomazkin.wordcard.mate.pristineCv
import me.apomazkin.wordcard.mate.savedCv
import me.apomazkin.wordcard.mate.textValuesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §3 Scenario-тесты (end-to-end через reducer, chained testReduce). Ключевые юзкейсы + traps.
 */
class WordCardScenarioTest {

    private val reducer = WordCardReducer()
    private val TR = ComponentTypeRef.BuiltIn(me.apomazkin.lexeme.BuiltInComponent.TRANSLATION)
    private val SYN = ComponentTypeRef.UserDefined("Synonym")
    private val EX = ComponentTypeRef.UserDefined("Example")
    private fun pK(k: Long) = ComponentValueKey.Pristine(k)
    private fun savedK(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))

    @Test
    fun `S1 new dictionary loads single translation chip`() {
        val s0 = loaded(availableTypes = emptyList(), lexemes = listOf(lexeme(1L, emptyList())))
        val s1 = reducer.testReduce(s0, Msg.ComponentTypesLoaded(listOf(ctype(50L, TR)))).state()
        assertEquals(listOf(ctype(50L, TR)), s1.availableComponentTypes)
    }

    @Test
    fun `S3 non-multi add full cycle hides chip`() {
        var s = loaded(
            availableTypes = listOf(ctype(51L, SYN, isMultiple = false)),
            lexemes = listOf(lexeme(7L, emptyList())),
        )
        s = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L))).state()
        s = reducer.testReduce(s, Msg.UpdateComponentValueInput(7L, pK(1L), "syn")).state()
        val committed = reducer.testReduce(s, Msg.CommitComponentValueEdit(7L, pK(1L)))
        committed.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 7L, pristineKey = 1L,
                    componentTypeId = ComponentTypeId(51L), componentTypeRef = SYN,
                    data = textValuesOf("syn"),
                ),
            ),
        )
        s = committed.state()
        s = reducer.testReduce(s, Msg.RefreshLexemeComponents(7L, listOf(domainCv(50L, 7L, "syn", typeId = 51L, ref = SYN)))).state()
        s = reducer.testReduce(s, Msg.ComponentValueInserted(7L, 1L, ComponentValueId(50L))).state()
        val lex = s.lexemeList.single()
        assertEquals(savedK(50L), lex.components.single().key)
        assertFalse(s.isPendingDbOp)
        assertTrue(lex.addedNonMultipleTypeIds.contains(ComponentTypeId(51L)))
    }

    @Test
    fun `S4 multi three pristine distinct keys`() {
        var s = loaded(
            availableTypes = listOf(ctype(51L, EX, isMultiple = true)),
            lexemes = listOf(lexeme(7L, emptyList())),
        )
        s = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L))).state()
        s = reducer.testReduce(s, Msg.UpdateComponentValueInput(7L, pK(1L), "a")).state()
        s = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L))).state()
        s = reducer.testReduce(s, Msg.UpdateComponentValueInput(7L, pK(2L), "b")).state()
        s = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L))).state()
        assertEquals(4L, s.nextPristineKey)
        assertFalse(s.lexemeList.single().addedNonMultipleTypeIds.contains(ComponentTypeId(51L)))
    }

    @Test
    fun `S5 NOT_IN_DB first non-translation promotes`() {
        var s = loaded(
            availableTypes = listOf(ctype(51L, EX, isMultiple = false)),
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, typeId = 51L, ref = EX, edited = "good")))),
        )
        val committed = reducer.testReduce(s, Msg.CommitComponentValueEdit(NOT_IN_DB, pK(1L)))
        committed.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.CreateLexeme(
                    wordId = 7L, dictionaryId = 3L, pristineKey = 1L,
                    componentTypeId = ComponentTypeId(51L), componentTypeRef = EX,
                    data = textValuesOf("good"),
                ),
            ),
        )
        s = committed.state()
        val promoted = domainLexeme(900L, listOf(domainCv(60L, 900L, "good", typeId = 51L, ref = EX)))
        s = reducer.testReduce(s, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L)).state()
        assertEquals(900L, s.lexemeList.single().id)
        assertFalse(s.isPendingDbOp)
    }

    @Test
    fun `S6 NOT_IN_DB multi-component draft promotes with survivor reemit`() {
        var s = loaded(
            lexemes = listOf(
                lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t"), pristineCv(2L, typeId = 51L, ref = EX, edited = "ex"))),
            ),
        )
        // commitAndCloseAllEdits через CreateLexeme-путь не нужен — промоут на одном CommitComponentValueEdit якоря:
        val committed = reducer.testReduce(s, Msg.CommitComponentValueEdit(NOT_IN_DB, pK(1L)))
        s = committed.state()
        val promoted = domainLexeme(900L, listOf(domainCv(60L, 900L, "t", ref = TR)))
        val res = reducer.testReduce(s, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        res.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 900L, pristineKey = 2L,
                    componentTypeId = ComponentTypeId(51L), componentTypeRef = EX,
                    data = textValuesOf("ex"),
                ),
            ),
        )
    }

    @Test
    fun `S9 clear blur deletes via PessimisticRemove`() {
        var s = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "x", isEdit = true, edited = "")))))
        val committed = reducer.testReduce(s, Msg.CommitComponentValueEdit(7L, savedK(5L)))
        committed.assertEffects(setOf(DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L)))
    }

    @Test
    fun `S11 delete last cascade then undo restores`() {
        var s = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(60L, origin = "x")))))
        val removed = domainLexeme(8L, listOf(domainCv(60L, 8L, "x", ref = TR)))
        val cascade = reducer.testReduce(s, Msg.LexemeCascadeRemoved(removed))
        cascade.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithUndo(
                    messageRes = R.string.word_card_snackbar_lexeme_deleted,
                    actionLabelRes = R.string.word_card_snackbar_undo,
                    undoMsg = Msg.UndoRestoreLexeme(removed),
                ),
            ),
        )
        s = cascade.state()
        assertTrue(s.lexemeList.isEmpty())
        val undo = reducer.testReduce(s, Msg.UndoRestoreLexeme(removed))
        undo.assertEffects(setOf(DatasourceEffect.RestoreLexemeWithComponents(7L, 3L, removed)))
    }

    @Test
    fun `S17 load fail then retry then loaded`() {
        var s = loaded()
        val failed = reducer.testReduce(s, Msg.ComponentTypesLoadFailed(RuntimeException()))
        failed.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithRetry(
                    messageRes = R.string.word_card_error_load_component_types,
                    actionLabelRes = R.string.word_card_action_retry,
                    retryMsg = Msg.RetryLoadComponentTypes,
                ),
            ),
        )
        s = failed.state()
        val retry = reducer.testReduce(s, Msg.RetryLoadComponentTypes)
        retry.assertEffects(setOf(DatasourceEffect.LoadAvailableComponentTypes(3L)))
        s = reducer.testReduce(retry.state(), Msg.ComponentTypesLoaded(listOf(ctype(50L, TR)))).state()
        assertEquals(1, s.availableComponentTypes.size)
    }

    @Test
    fun `S_trap_2 multi commit under pending swallowed then create not guarded`() {
        var s = loaded(
            availableTypes = listOf(ctype(51L, EX, isMultiple = true)),
            lexemes = listOf(lexeme(7L, listOf(pristineCv(1L, typeId = 51L, ref = EX, edited = "a")))),
            nextPristineKey = 2L, // существующий pristine уже занял key=1 → следующий = 2
        )
        s = reducer.testReduce(s, Msg.CommitComponentValueEdit(7L, pK(1L))).state()
        assertTrue("pending after commit#1", s.isPendingDbOp)
        // commit#2 guarded (no-op); CreateComponentValue НЕ guarded:
        val create = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L)))
        assertTrue("pristine добавлен при pending (B5)", create.state().lexemeList.single().components.any { it.pristineKey == 2L })
    }

    @Test
    fun `S_trap_3 refresh keeps typing pristine then inserted flips`() {
        var s = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L), pristineCv(10L, edited = "typing")))))
        s = reducer.testReduce(s, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "x"), domainCv(7L, 7L, "new")))).state()
        assertTrue("pristine P10 сохранён", s.lexemeList.single().components.any { it.pristineKey == 10L })
        s = reducer.testReduce(s, Msg.ComponentValueInserted(7L, 10L, ComponentValueId(11L))).state()
        assertTrue("P10 флипнут в Saved(11)", s.lexemeList.single().components.any { it.key == savedK(11L) })
    }

    @Test
    fun `S21 create lexeme then second create swallowed`() {
        var s = loaded(lexemes = emptyList())
        s = reducer.testReduce(s, Msg.CreateLexeme).state()
        assertTrue(s.isCreatingLexeme)
        val second = reducer.testReduce(s, Msg.CreateLexeme)
        assertEquals("single-draft guard", s, second.state())
    }

    @Test
    fun `S22 empty pristine not stacked`() {
        var s = loaded(
            availableTypes = listOf(ctype(51L, EX, isMultiple = true)),
            lexemes = listOf(lexeme(7L, emptyList())),
        )
        s = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L))).state()
        s = reducer.testReduce(s, Msg.CreateComponentValue(7L, ComponentTypeId(51L))).state()
        assertEquals("пустой первый дропнут, остаётся 1 pristine", 1, s.lexemeList.single().components.count { it.isPristine })
    }

    @Test
    fun `S23 remove translation keeps lexeme with other component`() {
        var s = loaded(
            lexemes = listOf(lexeme(7L, listOf(savedCv(5L, ref = TR, origin = "arg"), savedCv(6L, typeId = 51L, ref = EX, origin = "good arg")))),
        )
        val removed = reducer.testReduce(s, Msg.RemoveComponentValueRequested(7L, savedK(5L)))
        removed.assertEffects(setOf(DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L)))
        s = removed.state()
        s = reducer.testReduce(s, Msg.RefreshLexemeComponents(7L, listOf(domainCv(6L, 7L, "good arg", typeId = 51L, ref = EX)))).state()
        val lex = s.lexemeList.single()
        assertEquals("лексема осталась с примером", 1, lex.components.size)
        assertFalse("перевода нет", lex.components.any { it.componentTypeRef == TR })
    }
}
