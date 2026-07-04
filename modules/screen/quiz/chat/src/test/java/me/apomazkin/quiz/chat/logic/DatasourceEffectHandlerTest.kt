package me.apomazkin.quiz.chat.logic

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import me.apomazkin.quiz.chat.quiz.QuizGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS481 unit tests для новых веток `DatasourceEffectHandler`:
 * - `LoadQuizComponentTypes` → emit `Msg.QuizComponentTypesLoaded` (либо Empty при null dict).
 * - `SaveQuizPickerSelection(ref)` → useCase.setQuizPickerSelection, emit `Msg.Empty`.
 *
 * Используем `FakeUseCase` вместо mockk, чтобы избежать проблем mockk с
 * `@JvmInline value class ComponentTypeRef` (mockk не умеет распаковывать
 * value-class при `any()` / capture — fails с "null packRef").
 */
class DatasourceEffectHandlerTest {

    private class FakeUseCase(
        var currentDictId: Long? = 1L,
        var availableTypes: List<ComponentType> = emptyList(),
        var pickerSelection: ComponentTypeRef? = null,
    ) : QuizChatUseCase {
        var setCallCount: Int = 0
            private set
        var setCallDictId: Long? = null
            private set
        var setCallRef: ComponentTypeRef? = null
            private set

        override suspend fun getCurrentDictionaryId(): Long? = currentDictId
        override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int = 0
        override suspend fun getRandomWriteQuizList(
            limit: Int, maxGrade: Int, dictionaryId: Long
        ): List<WriteQuiz> = emptyList()
        override suspend fun getQuizConfig(dictionaryId: Long, quizMode: String): QuizConfig? = null
        override suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType> =
            availableTypes
        override suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef? =
            pickerSelection
        override suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef) {
            setCallCount++
            setCallDictId = dictionaryId
            setCallRef = ref
        }
    }

    private val quizGame = mockk<QuizGame>(relaxed = true)
    private val prefsProvider = mockk<PrefsProvider>(relaxed = true)
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val builtInTranslation = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)

    private val translationType = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private fun makeHandler(useCase: QuizChatUseCase) = DatasourceEffectHandler(
        quizGame = quizGame,
        prefsProvider = prefsProvider,
        useCase = useCase,
        logger = logger,
    )

    private suspend fun runEffect(
        handler: DatasourceEffectHandler,
        effect: DatasourceEffect,
    ): Msg {
        var captured: Msg = Msg.Empty
        handler.runEffect(effect) { captured = it }
        return captured
    }

    // ===== LoadQuizComponentTypes =====

    @Test
    fun `LoadQuizComponentTypes resolves dictId and emits Loaded`() = runTest {
        val fake = FakeUseCase(
            currentDictId = 1L,
            availableTypes = listOf(translationType),
            pickerSelection = builtInTranslation,
        )
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizComponentTypes)

        assertTrue("emit QuizComponentTypesLoaded", msg is Msg.QuizComponentTypesLoaded)
        val loaded = msg as Msg.QuizComponentTypesLoaded
        assertEquals(listOf(translationType), loaded.types)
        assertEquals(builtInTranslation, loaded.restoredSelectedRef)
    }

    @Test
    fun `LoadQuizComponentTypes with null dict emits Empty`() = runTest {
        val fake = FakeUseCase(currentDictId = null)
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizComponentTypes)

        assertEquals(Msg.Empty, msg)
    }

    // ===== SaveQuizPickerSelection =====

    @Test
    fun `SaveQuizPickerSelection calls UseCase set and emits Empty`() = runTest {
        val fake = FakeUseCase(currentDictId = 1L)
        val handler = makeHandler(fake)

        val msg = runEffect(
            handler,
            DatasourceEffect.SaveQuizPickerSelection(builtInTranslation),
        )

        assertEquals(Msg.Empty, msg)
        assertEquals(1, fake.setCallCount)
        assertEquals(1L, fake.setCallDictId)
        assertEquals(builtInTranslation, fake.setCallRef)
    }

    @Test
    fun `SaveQuizPickerSelection with null dict skips set and emits Empty`() = runTest {
        val fake = FakeUseCase(currentDictId = null)
        val handler = makeHandler(fake)

        val msg = runEffect(
            handler,
            DatasourceEffect.SaveQuizPickerSelection(builtInTranslation),
        )

        assertEquals(Msg.Empty, msg)
        assertEquals(0, fake.setCallCount)
        assertNull(fake.setCallRef)
    }
}
