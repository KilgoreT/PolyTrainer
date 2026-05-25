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
 * Reducer tests for Definition chip — зеркальны TranslationManagementTest.
 * Покрытие веток CommitDefinitionEdit (1a / 1 / 2 / 3), RefreshDefinition, cascade.
 */
class DefinitionManagementTest {

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
    fun `given lexeme without definition when CreateDefinition then definition becomes editable empty`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "t", isEdit = false), definition = null),
        ))

        val result = reducer.testReduce(initial, Msg.CreateDefinition(lexemeId = 1L))

        val lex = result.state().lexemeList.first()
        assertNotNull(lex.definition)
        assertEquals("", lex.definition?.origin)
        assertTrue(lex.definition?.isEdit ?: false)
        result.assertNoEffects()
    }

    @Test
    fun `when UpdateDefinitionInput then edited text updated locally`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 1L, definition = TextValueState(origin = "a", isEdit = true, edited = "")),
        ))

        val result = reducer.testReduce(initial, Msg.UpdateDefinitionInput(lexemeId = 1L, value = "def"))

        assertEquals("def", result.state().lexemeList.first().definition?.edited)
        result.assertNoEffects()
    }

    @Test
    fun `branch 1a empty-empty CommitDefinitionEdit nullifies definition locally without Effect`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = NOT_IN_DB,
                translation = TextValueState(origin = "t", isEdit = false),
                definition = TextValueState(origin = "", edited = "   ", isEdit = true),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.CommitDefinitionEdit(lexemeId = NOT_IN_DB))

        val lex = result.state().lexemeList.first { it.id == NOT_IN_DB }
        assertNull(lex.definition)
        assertNotNull(lex.translation)
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `branch 1 pessimistic remove CommitDefinitionEdit emits RemoveDefinition and resets edit flag`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 42L,
                definition = TextValueState(origin = "x", edited = "", isEdit = true),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.CommitDefinitionEdit(lexemeId = 42L))

        val lex = result.state().lexemeList.first()
        assertFalse(lex.definition?.isEdit ?: true)
        assertEquals("", lex.definition?.edited)
        assertEquals("x", lex.definition?.origin)
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveDefinition(lexemeId = 42L, currentValue = "x")))
    }

    @Test
    fun `branch 2 no-op CommitDefinitionEdit resets edit state without Effect`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 5L,
                definition = TextValueState(origin = "same", edited = "same", isEdit = true),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.CommitDefinitionEdit(lexemeId = 5L))

        val lex = result.state().lexemeList.first()
        assertFalse(lex.definition?.isEdit ?: true)
        assertEquals("", lex.definition?.edited)
        assertEquals("same", lex.definition?.origin)
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `branch 3 first-Commit on NOT_IN_DB emits UpdateLexemeDefinition with null lexemeId`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 100L,
            lexemes = listOf(
                LexemeState(
                    id = NOT_IN_DB,
                    definition = TextValueState(origin = "", edited = "long def", isEdit = true),
                ),
            ),
        )

        val result = reducer.testReduce(initial, Msg.CommitDefinitionEdit(lexemeId = NOT_IN_DB))

        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeDefinition(
                    wordId = 100L,
                    lexemeId = null,
                    definition = "long def",
                ),
            ),
        )
    }

    @Test
    fun `branch 3 update existing real id emits UpdateLexemeDefinition with real lexemeId`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 100L,
            lexemes = listOf(
                LexemeState(
                    id = 7L,
                    definition = TextValueState(origin = "old", edited = "new", isEdit = true),
                ),
            ),
        )

        val result = reducer.testReduce(initial, Msg.CommitDefinitionEdit(lexemeId = 7L))

        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeDefinition(
                    wordId = 100L,
                    lexemeId = 7L,
                    definition = "new",
                ),
            ),
        )
    }

    @Test
    fun `RefreshDefinition for NOT_IN_DB replaces id with real and constructs TextValueState`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(
                    id = NOT_IN_DB,
                    definition = TextValueState(origin = "", edited = "", isEdit = false),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshDefinition(lexemeId = 555L, definition = "def"),
        )

        val state = result.state()
        assertFalse(state.isPendingDbOp)
        val lex = state.lexemeList.first()
        assertEquals(555L, lex.id)
        assertEquals("def", lex.definition?.origin)
        assertEquals("", lex.definition?.edited)
        assertFalse(lex.definition?.isEdit ?: true)
    }

    @Test
    fun `RefreshDefinition preserves active edit of existing lexeme F073`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(
                    id = 11L,
                    definition = TextValueState(origin = "old", edited = "user-typed", isEdit = true),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshDefinition(lexemeId = 11L, definition = "new"),
        )

        val lex = result.state().lexemeList.first()
        assertEquals("new", lex.definition?.origin)
        assertTrue(lex.definition?.isEdit ?: false)
        assertEquals("user-typed", lex.definition?.edited)
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `RefreshDefinition with null definition nullifies for existing lexeme`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                LexemeState(
                    id = 11L,
                    translation = TextValueState(origin = "t", isEdit = false),
                    definition = TextValueState(origin = "old", edited = "", isEdit = false),
                ),
            ),
        )

        val result = reducer.testReduce(
            initial,
            Msg.RefreshDefinition(lexemeId = 11L, definition = null),
        )

        val lex = result.state().lexemeList.first()
        assertNull(lex.definition)
        assertNotNull(lex.translation)
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `RemoveDefinition for NOT_IN_DB with no translation cascade removes lexeme`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = NOT_IN_DB,
                translation = null,
                definition = TextValueState(origin = "", isEdit = false),
            ),
        ))

        val result = reducer.testReduce(initial, Msg.RemoveDefinition(lexemeId = NOT_IN_DB))

        assertTrue(result.state().lexemeList.isEmpty())
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `RemoveDefinition for real id emits RemoveDefinition effect and sets pending`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(id = 42L, definition = TextValueState(origin = "x", isEdit = false)),
        ))

        val result = reducer.testReduce(initial, Msg.RemoveDefinition(lexemeId = 42L))

        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveDefinition(lexemeId = 42L, currentValue = "x")))
    }

}
