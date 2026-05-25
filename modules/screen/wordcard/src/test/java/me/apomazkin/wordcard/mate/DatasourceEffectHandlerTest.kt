package me.apomazkin.wordcard.mate

import kotlinx.coroutines.runBlocking
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordcard.deps.RemoveDefinitionResult
import me.apomazkin.wordcard.deps.RemoveTranslationResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Definition
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Translation
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Handler interaction tests per contract_spec § "Handler".
 *
 * Test cases:
 * - LoadWord exception: handler ловит → consumer(Msg.WordNotFound) silent exit.
 * - UpdateLexemeTranslation first-Commit success: handler → RefreshTranslation(realId, value).
 * - UpdateLexemeTranslation first-Commit failure (exception): handler → ShowNotification("Не удалось сохранить перевод").
 * - RemoveTranslation cascade-path: handler → LexemeCascadeRemoved(lexemeId).
 * - RemoveTranslation non-cascade-path: handler → RefreshTranslation(lexemeId, null).
 * - RemoveTranslation null-result-path: handler → ShowNotification.
 * - RemoveWord success: handler → NavigateBack; failure → ShowNotification.
 */
class DatasourceEffectHandlerTest {

    /** Fake UseCase. Каждое поле — лямбда, переопределяемая в тесте. */
    private class FakeUseCase(
        var getTermByIdImpl: suspend (Long) -> Term? = { null },
        var deleteWordImpl: suspend (Long) -> Int = { 0 },
        var updateWordImpl: suspend (Long, String) -> Boolean = { _, _ -> false },
        var deleteLexemeImpl: suspend (Long, Long) -> List<Lexeme>? = { _, _ -> null },
        var addLexemeTranslationImpl: suspend (Long, Long?, String) -> Lexeme? = { _, _, _ -> null },
        var deleteLexemeTranslationImpl: suspend (Long) -> RemoveTranslationResult? = { null },
        var addLexemeDefinitionImpl: suspend (Long, Long?, String) -> Lexeme? = { _, _, _ -> null },
        var deleteLexemeDefinitionImpl: suspend (Long) -> RemoveDefinitionResult? = { null },
        var restoreLexemeImpl: suspend (Long, String?, String?) -> List<Lexeme>? = { _, _, _ -> null },
    ) : WordCardUseCase {
        override suspend fun getTermById(wordId: Long): Term? = getTermByIdImpl(wordId)
        override suspend fun deleteWord(wordId: Long): Int = deleteWordImpl(wordId)
        override suspend fun updateWord(wordId: Long, value: String): Boolean = updateWordImpl(wordId, value)
        override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>? =
            deleteLexemeImpl(wordId, lexemeId)
        override suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme? =
            addLexemeTranslationImpl(wordId, lexemeId, translation)
        override suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult? =
            deleteLexemeTranslationImpl(lexemeId)
        override suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme? =
            addLexemeDefinitionImpl(wordId, lexemeId, definition)
        override suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult? =
            deleteLexemeDefinitionImpl(lexemeId)
        override suspend fun restoreLexeme(
            wordId: Long,
            translation: String?,
            definition: String?,
        ): List<Lexeme>? = restoreLexemeImpl(wordId, translation, definition)
    }

    private object NoopLogger : LexemeLogger {
        override fun log(level: LogLevel, tag: String, message: String) = Unit
    }

    private fun runHandler(
        useCase: WordCardUseCase,
        effect: DatasourceEffect,
    ): List<Msg> {
        val collected = mutableListOf<Msg>()
        val handler = DatasourceEffectHandler(useCase, NoopLogger)
        runBlocking {
            handler.runEffect(effect) { msg -> collected.add(msg) }
        }
        return collected
    }

    @Test
    fun `LoadWord exception yields WordNotFound silent exit`() {
        val uc = FakeUseCase(getTermByIdImpl = { throw IllegalStateException("boom") })

        val msgs = runHandler(uc, DatasourceEffect.LoadWord(wordId = 1L))

        assertEquals(1, msgs.size)
        assertTrue("expected WordNotFound", msgs.first() is Msg.WordNotFound)
    }

    @Test
    fun `LoadWord null result yields WordNotFound`() {
        val uc = FakeUseCase(getTermByIdImpl = { null })

        val msgs = runHandler(uc, DatasourceEffect.LoadWord(wordId = 1L))

        assertTrue(msgs.first() is Msg.WordNotFound)
    }

    @Test
    fun `LoadWord success yields WordLoaded`() {
        val term = Term(
            wordId = WordId(1L), word = Word("w"),
            addedDate = Date(0L), changedDate = null, removedDate = null,
            lexemeList = emptyList(),
        )
        val uc = FakeUseCase(getTermByIdImpl = { term })

        val msgs = runHandler(uc, DatasourceEffect.LoadWord(wordId = 1L))

        val msg = msgs.first()
        assertTrue(msg is Msg.WordLoaded)
        assertEquals(term, (msg as Msg.WordLoaded).word)
    }

    @Test
    fun `UpdateLexemeTranslation first-Commit success yields RefreshTranslation with real id`() {
        val newLexeme = Lexeme(
            lexemeId = LexemeId(555L),
            translation = Translation("hello"),
            definition = null,
            category = null,
            addDate = Date(0L),
        )
        val uc = FakeUseCase(
            addLexemeTranslationImpl = { wordId, lexemeId, translation ->
                // first-Commit path: lexemeId == null
                assertEquals(null, lexemeId)
                assertEquals(7L, wordId)
                assertEquals("hello", translation)
                newLexeme
            },
        )

        val msgs = runHandler(
            uc,
            DatasourceEffect.UpdateLexemeTranslation(
                wordId = 7L, lexemeId = null, translation = "hello",
            ),
        )

        val msg = msgs.first()
        assertTrue(msg is Msg.RefreshTranslation)
        val refresh = msg as Msg.RefreshTranslation
        assertEquals(555L, refresh.lexemeId)
        assertEquals("hello", refresh.translation)
    }

    @Test
    fun `UpdateLexemeTranslation failure (exception) yields ShowError`() {
        val uc = FakeUseCase(
            addLexemeTranslationImpl = { _, _, _ -> throw IllegalStateException("dict missing") },
        )

        val msgs = runHandler(
            uc,
            DatasourceEffect.UpdateLexemeTranslation(
                wordId = 7L, lexemeId = null, translation = "hello",
            ),
        )

        val msg = msgs.first()
        assertTrue("expected ShowError", msg is Msg.ShowError)
    }

    @Test
    fun `UpdateLexemeTranslation null result yields ShowError`() {
        val uc = FakeUseCase(addLexemeTranslationImpl = { _, _, _ -> null })

        val msgs = runHandler(
            uc,
            DatasourceEffect.UpdateLexemeTranslation(wordId = 7L, lexemeId = null, translation = "hi"),
        )

        val msg = msgs.first()
        assertTrue(msg is Msg.ShowError)
    }

    @Test
    fun `RemoveTranslation cascade-path yields LexemeCascadeRemovedWithUndo with removedTranslation`() {
        val uc = FakeUseCase(
            deleteLexemeTranslationImpl = { lexemeId ->
                assertEquals(42L, lexemeId)
                RemoveTranslationResult.LexemeCascadeRemoved
            },
        )

        val msgs = runHandler(
            uc,
            DatasourceEffect.RemoveTranslation(lexemeId = 42L, currentValue = "removed-value"),
        )

        val msg = msgs.first()
        assertTrue(
            "expected LexemeCascadeRemovedWithUndo Msg",
            msg is Msg.LexemeCascadeRemovedWithUndo,
        )
        val cascade = msg as Msg.LexemeCascadeRemovedWithUndo
        assertEquals(42L, cascade.lexemeId)
        assertEquals("removed-value", cascade.removedTranslation)
        assertEquals(null, cascade.removedDefinition)
    }

    @Test
    fun `RemoveTranslation non-cascade-path yields TranslationDeleted with removedValue`() {
        val updatedLex = Lexeme(
            lexemeId = LexemeId(42L),
            translation = null,
            definition = Definition("d"),
            category = null,
            addDate = Date(0L),
        )
        val uc = FakeUseCase(
            deleteLexemeTranslationImpl = { RemoveTranslationResult.TranslationRemoved(updatedLex) },
        )

        val msgs = runHandler(
            uc,
            DatasourceEffect.RemoveTranslation(lexemeId = 42L, currentValue = "old-translation"),
        )

        val msg = msgs.first()
        assertTrue(msg is Msg.TranslationDeleted)
        val deleted = msg as Msg.TranslationDeleted
        assertEquals(42L, deleted.lexemeId)
        assertEquals("old-translation", deleted.removedValue)
    }

    @Test
    fun `RemoveTranslation null result yields ShowError`() {
        val uc = FakeUseCase(deleteLexemeTranslationImpl = { null })

        val msgs = runHandler(
            uc,
            DatasourceEffect.RemoveTranslation(lexemeId = 42L, currentValue = "x"),
        )

        assertTrue(msgs.first() is Msg.ShowError)
    }

    @Test
    fun `RemoveWord success yields NavigateBack`() {
        val uc = FakeUseCase(deleteWordImpl = { 1 })

        val msgs = runHandler(uc, DatasourceEffect.RemoveWord(wordId = 7L))

        assertTrue(msgs.first() is Msg.NavigateBack)
    }

    @Test
    fun `RemoveWord failure (returns 0) yields ShowError`() {
        val uc = FakeUseCase(deleteWordImpl = { 0 })

        val msgs = runHandler(uc, DatasourceEffect.RemoveWord(wordId = 7L))

        assertTrue(msgs.first() is Msg.ShowError)
    }

    @Test
    fun `RemoveWord exception yields ShowError`() {
        val uc = FakeUseCase(deleteWordImpl = { throw IllegalStateException("boom") })

        val msgs = runHandler(uc, DatasourceEffect.RemoveWord(wordId = 7L))

        assertTrue(msgs.first() is Msg.ShowError)
    }
}
