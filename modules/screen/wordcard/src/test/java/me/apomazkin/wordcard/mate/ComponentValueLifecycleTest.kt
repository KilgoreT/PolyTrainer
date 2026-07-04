package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.3 CreateComponentValue / UpdateComponentValueInput / EnterComponentValueEditMode
 * + §1.5 RemoveComponentValueRequested.
 */
class ComponentValueLifecycleTest {

    private val reducer = WordCardReducer()
    private fun savedKey(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))
    private fun pKey(k: Long) = ComponentValueKey.Pristine(k)

    // ---------- CreateComponentValue ----------

    @Test
    fun `CreateComponentValue appends pristine and increments nextPristineKey`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR, isMultiple = false)),
            lexemes = listOf(lexeme(1L, emptyList())),
            nextPristineKey = 1L,
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(50L)))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals(pKey(1L), cv.key)
        assertTrue(cv.isEdit)
        assertEquals(2L, result.state().nextPristineKey)
        result.assertNoEffects()
    }

    @Test
    fun `CreateComponentValue commits open edits first`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR, isMultiple = false)),
            lexemes = listOf(lexeme(1L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "new")))),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(50L)))
        val saved = result.state().lexemeList.single().components.first { it.key == savedKey(5L) }
        assertTrue("A10: edit держится", saved.isEdit)
        assertTrue("isCommitting", saved.isCommitting)
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 1L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
            ),
        )
    }

    @Test
    fun `CreateComponentValue_B5_not_guarded_under_pending`() {
        val initial = loaded(
            isPendingDbOp = true,
            availableTypes = listOf(ctype(50L, TR, isMultiple = true)),
            lexemes = listOf(lexeme(1L, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(50L)))
        assertNotEquals(initial.lexemeList, result.state().lexemeList)
    }

    @Test
    fun `CreateComponentValue sets isMultiple from availableTypes`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR, isMultiple = true)),
            lexemes = listOf(lexeme(1L, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(50L)))
        assertTrue(result.state().lexemeList.single().components.single().isMultiple)
    }

    @Test
    fun `CreateComponentValue_G9_unknown_type_is_noop`() {
        val initial = loaded(availableTypes = emptyList(), lexemes = listOf(lexeme(1L, emptyList())))
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(999L)))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    /**
     * Regression (ревью IS481, CRITICAL): тап первого chip'а на ПУСТОМ NOT_IN_DB черновике.
     * Баг: `commitAndCloseAllEdits` выкидывал пустой черновик (`commitDraftLexeme` → `survived.isEmpty()`
     * → null) ДО `appendPristine` → черновик исчезал, первый компонент терялся, создать новую лексему
     * через FAB+chip было нельзя. Прошёл 218 зелёных — ни один тест не подавал CreateComponentValue(NOT_IN_DB).
     */
    @Test
    fun `CreateComponentValue on empty NOT_IN_DB draft keeps draft and adds editable pristine`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR, isMultiple = false)),
            lexemes = listOf(lexeme(NOT_IN_DB, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(NOT_IN_DB, ComponentTypeId(50L)))
        val draft = result.state().lexemeList.firstOrNull { it.id == NOT_IN_DB }
        assertNotNull("черновик не должен исчезнуть после тапа первого chip'а", draft)
        assertEquals("в черновик добавлен ровно один pristine", 1, draft!!.components.size)
        val cv = draft.components.single()
        assertTrue("компонент — pristine", cv.isPristine)
        assertTrue("компонент в режиме редактирования", cv.isEdit)
        assertTrue("создание лексемы продолжается", result.state().isCreatingLexeme)
    }

    /**
     * Regression (ревью IS481): `CreateComponentValue` на real `lexemeId`, которого уже нет в списке
     * (гонка с удалением лексемы — `CreateComponentValue` не guarded). Восстановление черновика должно
     * срабатывать ТОЛЬКО для `NOT_IN_DB`; иначе else-ветка фабриковала фантомную `LexemeState(realId)`
     * → `AddValue` в несуществующую лексему → DB-fail. Для real-id должно быть no-op.
     */
    @Test
    fun `CreateComponentValue on missing real lexeme is no-op without phantom`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR)),
            lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(999L, ComponentTypeId(50L)))
        assertTrue("фантомная лексема не создана", result.state().lexemeList.none { it.id == 999L })
        assertEquals("список лексем не изменился", initial.lexemeList, result.state().lexemeList)
        result.assertNoEffects()
    }

    // ---------- UpdateComponentValueInput ----------

    @Test
    fun `UpdateComponentValueInput updates only edited on pristine`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(pristineCv(1L, edited = "")))))
        val result = reducer.testReduce(initial, Msg.UpdateComponentValueInput(1L, pKey(1L), "hi"))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals("hi", cv.edited)
        assertEquals("", cv.origin)
        assertTrue(cv.isEdit)
        result.assertNoEffects()
    }

    @Test
    fun `UpdateComponentValueInput updates edited on saved in edit mode`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "old")))))
        val result = reducer.testReduce(initial, Msg.UpdateComponentValueInput(1L, savedKey(5L), "new"))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals("new", cv.edited)
        assertEquals("old", cv.origin)
    }

    @Test
    fun `UpdateComponentValueInput on saved not in edit is no-op`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(savedCv(5L, origin = "x", isEdit = false)))))
        val result = reducer.testReduce(initial, Msg.UpdateComponentValueInput(1L, savedKey(5L), "hack"))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `UpdateComponentValueInput not guarded by pending`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(1L, listOf(pristineCv(1L)))))
        val result = reducer.testReduce(initial, Msg.UpdateComponentValueInput(1L, pKey(1L), "hi"))
        assertEquals("hi", result.state().lexemeList.single().components.single().edited)
    }

    @Test
    fun `UpdateComponentValueInput unknown key is no-op`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(pristineCv(1L)))))
        val result = reducer.testReduce(initial, Msg.UpdateComponentValueInput(1L, savedKey(999L), "x"))
        assertEquals(initial, result.state())
    }

    // ---------- EnterComponentValueEditMode ----------

    @Test
    fun `EnterComponentValueEditMode sets edited origin and isEdit`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(savedCv(5L, origin = "word", isEdit = false)))))
        val result = reducer.testReduce(initial, Msg.EnterComponentValueEditMode(1L, savedKey(5L)))
        val cv = result.state().lexemeList.single().components.single()
        assertTrue(cv.isEdit)
        assertEquals("word", cv.edited)
    }

    @Test
    fun `EnterComponentValueEditMode commits other open edits first`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(
                    1L,
                    listOf(
                        savedCv(5L, origin = "a", isEdit = false),
                        savedCv(6L, origin = "old", isEdit = true, edited = "new"),
                    ),
                ),
            ),
        )
        val result = reducer.testReduce(initial, Msg.EnterComponentValueEditMode(1L, savedKey(5L)))
        val six = result.state().lexemeList.single().components.first { it.key == savedKey(6L) }
        assertTrue("A10: #6 edit держится", six.isEdit)
        assertTrue(six.isCommitting)
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 1L,
                    componentValueId = ComponentValueId(6L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
            ),
        )
    }

    @Test
    fun `EnterComponentValueEditMode also commits word edit (H-11)`() {
        val base = loaded(lexemes = listOf(lexeme(1L, listOf(savedCv(5L, origin = "a", isEdit = false)))))
        val initial = base.copy(wordState = (base.wordState as WordState.Loaded).copy(isEditMode = true, edited = "ed", value = "w"))
        val result = reducer.testReduce(initial, Msg.EnterComponentValueEditMode(1L, savedKey(5L)))
        val loaded = result.state().wordState as WordState.Loaded
        assertEquals("ed", loaded.value)
        assertTrue(result.effects().any { it is DatasourceEffect.UpdateWord && it.wordId == 7L && it.value == "ed" })
    }

    @Test
    fun `EnterComponentValueEditMode is guarded by pending`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(1L, listOf(savedCv(5L, isEdit = false)))))
        val result = reducer.testReduce(initial, Msg.EnterComponentValueEditMode(1L, savedKey(5L)))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ---------- §1.5 RemoveComponentValueRequested ----------

    @Test
    fun `pristine removes locally no effect`() {
        val initial = loaded(
            lexemes = listOf(lexeme(7L, listOf(pristineCv(1L, edited = "x"), savedCv(5L)))),
        )
        val result = reducer.testReduce(initial, Msg.RemoveComponentValueRequested(7L, pKey(1L)))
        val comps = result.state().lexemeList.single().components
        assertTrue("pristine удалён", comps.none { it.isPristine })
        assertEquals(1, comps.size)
        result.assertNoEffects()
    }

    @Test
    fun `pristine last comp on NOT_IN_DB cascades removal`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L)))))
        val result = reducer.testReduce(initial, Msg.RemoveComponentValueRequested(NOT_IN_DB, pKey(1L)))
        assertTrue(result.state().lexemeList.isEmpty())
        result.assertNoEffects()
    }

    @Test
    fun `saved emits RemoveComponentValue and sets pending`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "x")))))
        val result = reducer.testReduce(initial, Msg.RemoveComponentValueRequested(7L, savedKey(5L)))
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L)))
    }

    @Test
    fun `saved with empty origin local nullify`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "")))))
        val result = reducer.testReduce(initial, Msg.RemoveComponentValueRequested(7L, savedKey(5L)))
        assertTrue(result.state().lexemeList.single().components.isEmpty())
        assertEquals(false, result.state().isPendingDbOp)
        result.assertNoEffects()
    }
}
