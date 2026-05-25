package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for Translation chip per contract_spec § "Тестовые сценарии".
 *
 * Test cases (CommitTranslationEdit 4 branches + Refresh + cascade):
 * - CreateTranslation: создаёт TextValueState(origin="", edited="", isEdit=true).
 * - UpdateTranslationInput: локальный update edited.
 * - EnterTranslationEditMode: closeAllEditModes + isEdit=true.
 * - CommitTranslationEdit ветвь 1a (empty-empty): локальный nullify, нет Effect.
 * - CommitTranslationEdit ветвь 1 (pessimistic Remove): real id, edited="", origin!="" → Effect RemoveTranslation, isEdit=false, edited="".
 * - CommitTranslationEdit ветвь 2 (no-op): edited == origin → reset isEdit/edited, нет Effect.
 * - CommitTranslationEdit ветвь 3 (first-Commit, NOT_IN_DB): Effect UpdateLexemeTranslation(wordId, lexemeId=null, value), pending=true.
 * - CommitTranslationEdit ветвь 3 (Update existing real id): Effect UpdateLexemeTranslation(wordId, lexemeId=realId, value), pending=true.
 * - RefreshTranslation для existing lexemeId: обновляет origin, сохраняет активный edit (F073).
 * - RefreshTranslation для NOT_IN_DB → real id replacement (origin=value, isEdit=false, edited="").
 * - RefreshTranslation(translation=null) для existing lexemeId: nullify translation.
 * - Локальная cascade: RemoveTranslation на NOT_IN_DB → если обе суб-сущности null → лексема удалена.
 * - RemoveTranslation для real id: Effect + pending=true.
 * - Глобальный guard isPendingDbOp на CommitTranslationEdit.
 */
class TranslationManagementTest {

    private fun loaded(
        wordId: Long = 7L,
        lexemes: List<LexemeState> = emptyList(),
        isPendingDbOp: Boolean = false,
    ): WordCardState = WordCardState(
        isLoading = false,
        isPendingDbOp = isPendingDbOp,
        wordState = WordState.Loaded(id = wordId, added = Date(0L), value = "w"),
        lexemeList = lexemes,
    )

    @Test
    fun `given lexeme without translation when CreateTranslation then translation becomes editable empty`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 1L, translation = null, definition = TextValueState(origin = "def", isEdit = false)),
        ))

        val result = reducer.testReduce(initial, Msg.CreateTranslation(lexemeId = 1L))

        val lex = result.state().lexemeList.first()
        assertNotNull("translation created", lex.translation)
        assertEquals("origin empty", "", lex.translation?.origin)
        assertEquals("edited empty initially", "", lex.translation?.edited)
        assertTrue("isEdit true", lex.translation?.isEdit ?: false)
        result.assertNoEffects()
    }

    @Test
    fun `given existing translation when CreateTranslation then state unchanged (guard)`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "x", isEdit = false)),
        ))

        val result = reducer.testReduce(initial, Msg.CreateTranslation(lexemeId = 1L))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `when UpdateTranslationInput then edited text updated locally`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "a", isEdit = true, edited = "")),
        ))

        val result = reducer.testReduce(initial, Msg.UpdateTranslationInput(lexemeId = 1L, value = "hi"))

        assertEquals("hi", result.state().lexemeList.first().translation?.edited)
        result.assertNoEffects()
    }

    @Test
    fun `when EnterTranslationEditMode then commits pending edits and enables this one (Bug 2 IS479)`() {
        // Bug 2 (IS479): открытие нового edit commit'ит pending dirty edits
        // вместо cancel-сброса.
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 9L,
            lexemes = listOf(
                LexemeState(id = 1L, translation = TextValueState(origin = "a", isEdit = false)),
                LexemeState(id = 2L, definition = TextValueState(origin = "d", isEdit = true, edited = "edit-d")),
            ),
        ).copy(wordState = WordState.Loaded(id = 9L, added = Date(0L), value = "w", isEditMode = true, edited = "ed"))

        val result = reducer.testReduce(initial, Msg.EnterTranslationEditMode(lexemeId = 1L))

        val state = result.state()
        val loaded = state.wordState as WordState.Loaded
        assertFalse("word edit closed", loaded.isEditMode)
        assertEquals("word edited reset", "", loaded.edited)
        assertEquals("word value advanced to commit value", "ed", loaded.value)
        assertTrue("translation isEdit set", state.lexemeList.first { it.id == 1L }.translation?.isEdit ?: false)
        val def = state.lexemeList.first { it.id == 2L }.definition
        assertFalse("other definition edit closed", def?.isEdit ?: true)
        assertEquals("other definition edited reset", "", def?.edited)
        assertEquals("other definition origin advanced to commit value", "edit-d", def?.origin)
        assertTrue("pending DB op set due to commit effects", state.isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateWord(wordId = 9L, value = "ed"),
                DatasourceEffect.UpdateLexemeDefinition(
                    wordId = 9L,
                    lexemeId = 2L,
                    definition = "edit-d",
                ),
            ),
        )
    }

    @Test
    fun `branch 1a empty-empty CommitTranslationEdit nullifies translation locally without Effect`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = NOT_IN_DB,
                translation = TextValueState(origin = "", edited = "  ", isEdit = true),
                definition = TextValueState(origin = "def", isEdit = false),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.CommitTranslationEdit(lexemeId = NOT_IN_DB))

        // Lexeme NOT_IN_DB остаётся (definition non-null), translation = null.
        val lex = result.state().lexemeList.first { it.id == NOT_IN_DB }
        assertNull("translation nullified", lex.translation)
        assertNotNull("definition preserved", lex.definition)
        assertFalse("no pending op", result.state().isPendingDbOp)
        result.assertNoEffects("branch 1a is purely local")
    }

    @Test
    fun `branch 1 pessimistic remove CommitTranslationEdit emits RemoveTranslation and resets edit flag`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 42L,
                translation = TextValueState(origin = "x", edited = "", isEdit = true),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.CommitTranslationEdit(lexemeId = 42L))

        val lex = result.state().lexemeList.first()
        assertFalse("isEdit reset to false", lex.translation?.isEdit ?: true)
        assertEquals("edited reset to empty", "", lex.translation?.edited)
        assertEquals("origin preserved (not nullified yet)", "x", lex.translation?.origin)
        assertTrue("pending set", result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveTranslation(lexemeId = 42L, currentValue = "x")))
    }

    @Test
    fun `branch 2 no-op CommitTranslationEdit resets edit state without Effect`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 5L,
                translation = TextValueState(origin = "same", edited = "same", isEdit = true),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.CommitTranslationEdit(lexemeId = 5L))

        val lex = result.state().lexemeList.first()
        assertFalse("isEdit reset", lex.translation?.isEdit ?: true)
        assertEquals("edited reset", "", lex.translation?.edited)
        assertEquals("origin preserved", "same", lex.translation?.origin)
        assertFalse("no pending op", result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `branch 3 first-Commit on NOT_IN_DB emits UpdateLexemeTranslation with null lexemeId and sets pending`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 100L,
            lexemes = listOf(
                LexemeState(
                    id = NOT_IN_DB,
                    translation = TextValueState(origin = "", edited = "hello", isEdit = true),
                ),
            ),
        )

        val result = reducer.testReduce(initial, Msg.CommitTranslationEdit(lexemeId = NOT_IN_DB))

        assertTrue("pending set", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeTranslation(
                    wordId = 100L,
                    lexemeId = null,
                    translation = "hello",
                ),
            ),
        )
    }

    @Test
    fun `branch 3 update existing real id emits UpdateLexemeTranslation with real lexemeId`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 100L,
            lexemes = listOf(
                LexemeState(
                    id = 7L,
                    translation = TextValueState(origin = "old", edited = "new", isEdit = true),
                ),
            ),
        )

        val result = reducer.testReduce(initial, Msg.CommitTranslationEdit(lexemeId = 7L))

        assertTrue("pending set", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeTranslation(
                    wordId = 100L,
                    lexemeId = 7L,
                    translation = "new",
                ),
            ),
        )
    }

    @Test
    fun `RefreshTranslation for NOT_IN_DB replaces id with real and constructs TextValueState`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(
                    id = NOT_IN_DB,
                    translation = TextValueState(origin = "", edited = "", isEdit = false),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshTranslation(lexemeId = 555L, translation = "hello"),
        )

        val state = result.state()
        assertFalse("pending cleared", state.isPendingDbOp)
        val lex = state.lexemeList.first()
        assertEquals("id replaced with real", 555L, lex.id)
        assertEquals("origin set", "hello", lex.translation?.origin)
        assertEquals("edited empty", "", lex.translation?.edited)
        assertFalse("isEdit false", lex.translation?.isEdit ?: true)
    }

    @Test
    fun `RefreshTranslation preserves active edit of existing lexeme F073`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(
                    id = 11L,
                    translation = TextValueState(origin = "old", edited = "user-typed", isEdit = true),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshTranslation(lexemeId = 11L, translation = "new"),
        )

        val lex = result.state().lexemeList.first()
        assertEquals("origin updated", "new", lex.translation?.origin)
        assertTrue("isEdit preserved (user still editing)", lex.translation?.isEdit ?: false)
        assertEquals("edited preserved", "user-typed", lex.translation?.edited)
        assertFalse("pending cleared", result.state().isPendingDbOp)
    }

    @Test
    fun `RefreshTranslation with null translation nullifies for existing lexeme`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(
                    id = 11L,
                    translation = TextValueState(origin = "old", edited = "", isEdit = false),
                    definition = TextValueState(origin = "d", isEdit = false),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshTranslation(lexemeId = 11L, translation = null),
        )

        val lex = result.state().lexemeList.first()
        assertNull("translation nullified", lex.translation)
        assertNotNull("definition preserved", lex.definition)
        assertFalse("pending cleared", result.state().isPendingDbOp)
    }

    @Test
    fun `RemoveTranslation for NOT_IN_DB with no definition cascade removes lexeme`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = NOT_IN_DB,
                translation = TextValueState(origin = "", isEdit = false),
                definition = null,
            ),
        ))

        val result = reducer.testReduce(initial, Msg.RemoveTranslation(lexemeId = NOT_IN_DB))

        assertTrue("lexeme cascade-removed locally", result.state().lexemeList.isEmpty())
        assertFalse("no pending op for local cascade", result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `RemoveTranslation for real id emits RemoveTranslation effect and sets pending`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 42L, translation = TextValueState(origin = "x", isEdit = false)),
        ))

        val result = reducer.testReduce(initial, Msg.RemoveTranslation(lexemeId = 42L))

        assertTrue("pending set", result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveTranslation(lexemeId = 42L, currentValue = "x")))
    }

    @Test
    fun `given isPendingDbOp true CommitTranslationEdit is no-op (global guard)`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(id = 1L, translation = TextValueState(origin = "a", edited = "b", isEdit = true)),
            ),
        )

        val result = reducer.testReduce(initial, Msg.CommitTranslationEdit(lexemeId = 1L))

        assertEquals("state unchanged under pending guard", initial, result.state())
        result.assertNoEffects()
    }

}
