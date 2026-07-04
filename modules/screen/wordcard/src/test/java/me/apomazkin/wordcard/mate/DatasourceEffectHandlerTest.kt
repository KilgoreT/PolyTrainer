package me.apomazkin.wordcard.mate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordcard.deps.AddComponentValueResult
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §9.5 DatasourceEffectHandler (REWRITE, generic) — two-Msg burst + error-ветки.
 * TDD red — против НОВОГО WordCardUseCase (generic) и новых Effect/Msg/result-типов.
 */
class DatasourceEffectHandlerTest {

    /** Fake нового generic WordCardUseCase (лямбда-поля). */
    private class FakeUseCase(
        var getTermByIdImpl: suspend (Long) -> Term? = { null },
        var deleteWordImpl: suspend (Long) -> Int = { 0 },
        var updateWordImpl: suspend (Long, String) -> Boolean = { _, _ -> false },
        var deleteLexemeImpl: suspend (Long, Long) -> RemoveLexemeResult? = { _, _ -> null },
        var addLexemeWithComponentImpl: suspend (Long, Long, ComponentTypeRef, TemplateValues) -> Lexeme? =
            { _, _, _, _ -> null },
        var addComponentValueImpl: suspend (Long, ComponentTypeId, TemplateValues) -> AddComponentValueResult? =
            { _, _, _ -> null },
        var updateComponentValueImpl: suspend (ComponentValueId, Long, TemplateValues) -> Lexeme? =
            { _, _, _ -> null },
        var deleteComponentValueImpl: suspend (ComponentValueId, Long) -> RemoveComponentResult? =
            { _, _ -> null },
        var restoreImpl: suspend (Long, Long, Lexeme) -> Lexeme? = { _, _, _ -> null },
        var flowTypesImpl: (Long) -> Flow<List<ComponentType>> = { flowOf(emptyList()) },
    ) : WordCardUseCase {
        override suspend fun getTermById(wordId: Long): Term? = getTermByIdImpl(wordId)
        override suspend fun deleteWord(wordId: Long): Int = deleteWordImpl(wordId)
        override suspend fun updateWord(wordId: Long, value: String): Boolean = updateWordImpl(wordId, value)
        override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): RemoveLexemeResult? =
            deleteLexemeImpl(wordId, lexemeId)
        override suspend fun addLexemeWithComponent(
            wordId: Long, dictionaryId: Long, ref: ComponentTypeRef, data: TemplateValues,
        ): Lexeme? = addLexemeWithComponentImpl(wordId, dictionaryId, ref, data)
        override suspend fun addComponentValue(
            lexemeId: Long, componentTypeId: ComponentTypeId, data: TemplateValues,
        ): AddComponentValueResult? = addComponentValueImpl(lexemeId, componentTypeId, data)
        override suspend fun updateComponentValue(
            componentValueId: ComponentValueId, lexemeId: Long, data: TemplateValues,
        ): Lexeme? = updateComponentValueImpl(componentValueId, lexemeId, data)
        override suspend fun deleteComponentValue(
            componentValueId: ComponentValueId, lexemeId: Long,
        ): RemoveComponentResult? = deleteComponentValueImpl(componentValueId, lexemeId)
        override suspend fun restoreLexemeWithComponents(
            wordId: Long, dictionaryId: Long, snapshot: Lexeme,
        ): Lexeme? = restoreImpl(wordId, dictionaryId, snapshot)
        override fun flowAvailableComponentTypes(dictionaryId: Long): Flow<List<ComponentType>> =
            flowTypesImpl(dictionaryId)
    }

    private object NoopLogger : LexemeLogger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    private fun run(useCase: WordCardUseCase, effect: DatasourceEffect): List<Msg> {
        val msgs = mutableListOf<Msg>()
        val handler = DatasourceEffectHandler(useCase, NoopLogger)
        runBlocking { handler.runEffect(effect) { msgs += it } }
        return msgs
    }

    @Test
    fun `LoadWord success yields WordLoaded`() {
        val term = stubTerm()
        val msgs = run(FakeUseCase(getTermByIdImpl = { term }), DatasourceEffect.LoadWord(7L))
        assertEquals(1, msgs.size)
        assertEquals(Msg.WordLoaded(term), msgs.single())
    }

    @Test
    fun `LoadWord null yields WordNotFound`() {
        val msgs = run(FakeUseCase(getTermByIdImpl = { null }), DatasourceEffect.LoadWord(7L))
        assertTrue(msgs.single() is Msg.WordNotFound)
    }

    @Test
    fun `LoadWord exception yields WordNotFound`() {
        val msgs = run(FakeUseCase(getTermByIdImpl = { throw IllegalStateException() }), DatasourceEffect.LoadWord(7L))
        assertTrue(msgs.single() is Msg.WordNotFound)
    }

    @Test
    fun `AddValue success yields Refresh then Inserted burst`() {
        val lex = domainLexeme(7L, listOf(domainCv(60L, 7L, "v")))
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> AddComponentValueResult(lex, ComponentValueId(60L)) })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L, dictionaryId = 3L, lexemeId = 7L, pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("v"),
            ),
        )
        assertEquals(2, msgs.size)
        assertTrue("первый Refresh", msgs[0] is Msg.RefreshLexemeComponents)
        val inserted = msgs[1] as Msg.ComponentValueInserted
        assertEquals(1L, inserted.pristineKey)
        assertEquals(ComponentValueId(60L), inserted.newCvId)
    }

    @Test
    fun `CreateLexeme success yields LexemeDraftPromoted with anchor from effect`() {
        val lex = domainLexeme(900L, listOf(domainCv(60L, 900L, "v")))
        val uc = FakeUseCase(addLexemeWithComponentImpl = { _, _, _, _ -> lex })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.CreateLexeme(
                wordId = 7L, dictionaryId = 3L, pristineKey = 5L,
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("v"),
            ),
        )
        assertEquals(listOf(Msg.LexemeDraftPromoted(lex, anchorPristineKey = 5L)), msgs)
    }

    @Test
    fun `UpdateValue success yields single Refresh`() {
        val lex = domainLexeme(7L, listOf(domainCv(5L, 7L, "new")))
        val uc = FakeUseCase(updateComponentValueImpl = { _, _, _ -> lex })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.UpdateValue(
                wordId = 7L, dictionaryId = 3L, lexemeId = 7L,
                componentValueId = ComponentValueId(5L),
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("new"),
            ),
        )
        assertEquals(1, msgs.size)
        assertTrue(msgs.single() is Msg.RefreshLexemeComponents)
    }

    @Test
    fun `AddValue null yields OperationFailed`() {
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> null })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L, dictionaryId = 3L, lexemeId = 7L, pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("v"),
            ),
        )
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `RemoveComponentValue ComponentRemoved yields Refresh`() {
        val lex = domainLexeme(7L, listOf(domainCv(6L, 7L, "rest")))
        val uc = FakeUseCase(deleteComponentValueImpl = { _, _ -> RemoveComponentResult.ComponentRemoved(lex) })
        val msgs = run(uc, DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L))
        assertTrue(msgs.single() is Msg.RefreshLexemeComponents)
    }

    @Test
    fun `RemoveComponentValue Cascade yields LexemeCascadeRemoved`() {
        val snapshot = domainLexeme(7L, listOf(domainCv(5L, 7L, "x")))
        val uc = FakeUseCase(deleteComponentValueImpl = { _, _ -> RemoveComponentResult.LexemeCascadeRemoved(snapshot) })
        val msgs = run(uc, DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L))
        assertEquals(Msg.LexemeCascadeRemoved(snapshot), msgs.single())
    }

    @Test
    fun `RemoveComponentValue null yields OperationFailed`() {
        val uc = FakeUseCase(deleteComponentValueImpl = { _, _ -> null })
        val msgs = run(uc, DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L))
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `RestoreLexemeWithComponents success yields WordLoaded`() {
        val snapshot = domainLexeme(7L, listOf(domainCv(5L, 7L, "x")))
        val term = stubTerm()
        val uc = FakeUseCase(restoreImpl = { _, _, _ -> snapshot }, getTermByIdImpl = { term })
        val msgs = run(uc, DatasourceEffect.RestoreLexemeWithComponents(7L, 3L, snapshot))
        assertTrue(msgs.any { it is Msg.WordLoaded })
    }

    @Test
    fun `RestoreLexemeWithComponents null yields RestoreLexemeFailed with snapshot`() {
        val snapshot = domainLexeme(7L, listOf(domainCv(5L, 7L, "x")))
        val uc = FakeUseCase(restoreImpl = { _, _, _ -> null })
        val msgs = run(uc, DatasourceEffect.RestoreLexemeWithComponents(7L, 3L, snapshot))
        assertEquals(Msg.RestoreLexemeFailed(snapshot), msgs.single())
    }

    @Test
    fun `UpdateValue null yields OperationFailed`() {
        val uc = FakeUseCase(updateComponentValueImpl = { _, _, _ -> null })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.UpdateValue(
                wordId = 7L, dictionaryId = 3L, lexemeId = 7L,
                componentValueId = ComponentValueId(5L),
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("x"),
            ),
        )
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `CreateLexeme null yields OperationFailed`() {
        val uc = FakeUseCase(addLexemeWithComponentImpl = { _, _, _, _ -> null })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.CreateLexeme(
                wordId = 7L, dictionaryId = 3L, pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("x"),
            ),
        )
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `useCase exception in upsert yields OperationFailed single Msg`() {
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> throw IllegalStateException("boom") })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L, dictionaryId = 3L, lexemeId = 7L, pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("v"),
            ),
        )
        assertEquals(1, msgs.size)
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `CancellationException propagates not mapped to OperationFailed`() {
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> throw kotlinx.coroutines.CancellationException() })
        run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L, dictionaryId = 3L, lexemeId = 7L, pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L), componentTypeRef = TR, data = textValuesOf("v"),
            ),
        )
    }

    @Test
    fun `RemoveWord success yields NavigateBack`() {
        val msgs = run(FakeUseCase(deleteWordImpl = { 1 }), DatasourceEffect.RemoveWord(7L))
        assertTrue(msgs.single() is Msg.NavigateBack)
    }
}
