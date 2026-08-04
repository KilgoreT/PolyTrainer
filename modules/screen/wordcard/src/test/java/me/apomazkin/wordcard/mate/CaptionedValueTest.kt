package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Primitive
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS491 фаза 2.1: captioned_text в edit-пайплайне карточки (UC2, UC5–UC9, UC25–UC27).
 *
 * Контракты:
 * - commitDecision: «пусто» = пустой text (caption не считается, Д7/UC6);
 *   caption-изменение — тоже Update (UC7); trim caption → null (UC26);
 *   оба поля — один Update (UC27); NoOp при полном совпадении.
 * - Origin-lossy fix: сохранённое значение шаблона без текстового представления
 *   не может «тихо» исчезнуть через LocalRemove — только PessimisticRemove.
 * - CreateComponentValue переносит template типа в pristine (рендер-ветвление).
 * - EnterEdit сеет editedCaption из originCaption; отмена (UC25) — не эмитит эффектов.
 * - Commit captioned несёт CaptionedTextValues (upsert), anchor черновика — по text.
 * - Подсказки: LoadCaptionSuggestions → эффект; CaptionSuggestionsLoaded → state.
 */
class CaptionedValueTest {

    private val reducer = WordCardReducer()
    private fun savedKey(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))

    private fun capCv(
        id: Long = 5L,
        origin: String = "T1",
        originCaption: String? = null,
        isEdit: Boolean = true,
        edited: String = origin,
        editedCaption: String? = originCaption,
    ) = savedCv(
        id = id,
        origin = origin,
        isEdit = isEdit,
        edited = edited,
        template = ComponentTemplate.CAPTIONED_TEXT,
        originCaption = originCaption,
        editedCaption = editedCaption,
    )

    // ---------- commitDecision ----------

    @Test
    fun `captioned empty text is removal even with caption — caption-only impossible`() {
        val cv = capCv(origin = "T1", edited = "", editedCaption = "IELTS")
        assertEquals(CommitOutcome.PessimisticRemove, cv.commitDecision())
    }

    @Test
    fun `captioned pristine empty text with caption is LocalRemove`() {
        val cv = pristineCv(
            key = 1L,
            template = ComponentTemplate.CAPTIONED_TEXT,
            edited = "",
            editedCaption = "IELTS",
        )
        assertEquals(CommitOutcome.LocalRemove, cv.commitDecision())
    }

    @Test
    fun `captioned text change is Update carrying caption`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", edited = "T2")
        assertEquals(CommitOutcome.Update("T2", "IELTS"), cv.commitDecision())
    }

    @Test
    fun `captioned caption-only change is Update`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", editedCaption = "Grammar")
        assertEquals(CommitOutcome.Update("T1", "Grammar"), cv.commitDecision())
    }

    @Test
    fun `captioned caption cleared is Update with null caption`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", editedCaption = "")
        assertEquals(CommitOutcome.Update("T1", null), cv.commitDecision())
    }

    @Test
    fun `captioned whitespace caption trims to null UC26`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", editedCaption = "   ")
        assertEquals(CommitOutcome.Update("T1", null), cv.commitDecision())
    }

    @Test
    fun `captioned both fields changed is single Update UC27`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", edited = "T2", editedCaption = "Grammar")
        assertEquals(CommitOutcome.Update("T2", "Grammar"), cv.commitDecision())
    }

    @Test
    fun `captioned unchanged text and caption is NoOp`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS")
        assertEquals(CommitOutcome.NoOp, cv.commitDecision())
    }

    @Test
    fun `origin lossy fix — saved non-text template emptied in edit is PessimisticRemove`() {
        // Гипотетический сохранённый IMAGE (origin текстом не выражается → "").
        val cv = savedCv(
            id = 9L,
            origin = "",
            isEdit = true,
            edited = "",
            template = ComponentTemplate.IMAGE,
        )
        assertEquals(CommitOutcome.PessimisticRemove, cv.commitDecision())
    }

    // ---------- reducer: create / edit / input ----------

    @Test
    fun `CreateComponentValue carries template from type`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR, isMultiple = true, template = ComponentTemplate.CAPTIONED_TEXT)),
            lexemes = listOf(lexeme(1L, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(50L)))
        assertEquals(
            ComponentTemplate.CAPTIONED_TEXT,
            result.state().lexemeList.single().components.single().template,
        )
    }

    @Test
    fun `EnterComponentValueEditMode seeds editedCaption from originCaption`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", isEdit = false, edited = "", editedCaption = null)
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val result = reducer.testReduce(initial, Msg.EnterComponentValueEditMode(1L, savedKey(5L)))
        val edited = result.state().lexemeList.single().components.single()
        assertTrue(edited.isEdit)
        assertEquals("T1", edited.edited)
        assertEquals("IELTS", edited.editedCaption)
    }

    @Test
    fun `UpdateComponentCaptionInput updates editedCaption in edit mode`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS")
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val result = reducer.testReduce(
            initial,
            Msg.UpdateComponentCaptionInput(1L, savedKey(5L), "Grammar"),
        )
        assertEquals("Grammar", result.state().lexemeList.single().components.single().editedCaption)
        result.assertNoEffects()
    }

    @Test
    fun `UpdateComponentCaptionInput ignored when not in edit`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", isEdit = false, edited = "", editedCaption = null)
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val result = reducer.testReduce(
            initial,
            Msg.UpdateComponentCaptionInput(1L, savedKey(5L), "Grammar"),
        )
        assertNull(result.state().lexemeList.single().components.single().editedCaption)
    }

    // ---------- reducer: commit ----------

    @Test
    fun `Commit captioned edit emits UpdateValue with CaptionedTextValues`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS", edited = "T2", editedCaption = "Grammar")
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(1L, savedKey(5L)))
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 1L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = CaptionedTextValues(Primitive.Text("T2"), Primitive.Text("Grammar")),
                ),
            ),
        )
    }

    @Test
    fun `Commit captioned without caption carries null caption`() {
        val cv = capCv(origin = "T1", originCaption = null, edited = "T2", editedCaption = null)
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(1L, savedKey(5L)))
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 1L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = CaptionedTextValues(Primitive.Text("T2"), caption = null),
                ),
            ),
        )
    }

    @Test
    fun `draft anchor captioned pristine creates lexeme with caption data`() {
        val pristine = pristineCv(
            key = 1L,
            template = ComponentTemplate.CAPTIONED_TEXT,
            edited = "quote",
            editedCaption = "IELTS",
        )
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristine))))
        val result = reducer.testReduce(initial, Msg.NavigateBack)
        val create = result.effects()
            .filterIsInstance<DatasourceEffect.UpsertComponentValue.CreateLexeme>()
            .single()
        assertEquals(
            CaptionedTextValues(Primitive.Text("quote"), Primitive.Text("IELTS")),
            create.data,
        )
    }

    // ---------- reducer: refresh merge ----------

    @Test
    fun `RefreshLexemeComponents merges originCaption from domain data`() {
        val cv = capCv(origin = "old", originCaption = "Old", isEdit = false, edited = "", editedCaption = null)
            .copy(isCommitting = true)
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val domain = domainCv(
            id = 5L,
            lexemeId = 1L,
            text = "new",
            data = CaptionedTextValues(Primitive.Text("new"), Primitive.Text("Fresh")),
            template = ComponentTemplate.CAPTIONED_TEXT,
        )
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(1L, listOf(domain)))
        val merged = result.state().lexemeList.single().components.single()
        assertEquals("new", merged.origin)
        assertEquals("Fresh", merged.originCaption)
    }

    // ---------- отмена редактирования (UC25) ----------

    @Test
    fun `unchanged captioned commit on flush produces no effects`() {
        val cv = capCv(origin = "T1", originCaption = "IELTS")
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(cv))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(1L, savedKey(5L)))
        val closed = result.state().lexemeList.single().components.single()
        assertTrue(!closed.isEdit)
        result.assertNoEffects()
    }

    // ---------- подсказки ----------

    @Test
    fun `LoadCaptionSuggestions emits datasource effect`() {
        val initial = loaded()
        val result = reducer.testReduce(initial, Msg.LoadCaptionSuggestions(ComponentTypeId(50L)))
        result.assertEffects(setOf(DatasourceEffect.LoadCaptionSuggestions(ComponentTypeId(50L))))
    }

    @Test
    fun `CaptionSuggestionsLoaded stores list per type`() {
        val initial = loaded()
        val result = reducer.testReduce(
            initial,
            Msg.CaptionSuggestionsLoaded(ComponentTypeId(50L), listOf("Grammar", "IELTS")),
        )
        assertEquals(
            listOf("Grammar", "IELTS"),
            result.state().captionSuggestions[ComponentTypeId(50L)],
        )
        result.assertNoEffects()
    }
}
