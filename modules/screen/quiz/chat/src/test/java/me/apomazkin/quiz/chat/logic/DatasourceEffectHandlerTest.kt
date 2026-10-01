package me.apomazkin.quiz.chat.logic

import androidx.compose.ui.text.AnnotatedString
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import me.apomazkin.quiz.chat.quiz.QuizGame
import me.apomazkin.quiz.chat.quiz.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Ветки `DatasourceEffectHandler`:
 * - `LoadQuizComponentTypes` → `Msg.QuizComponentTypesLoaded` (либо Empty при null dict);
 * - `SaveQuizPickerSelection(refs)` → useCase.setQuizPickerSelection, emit `Msg.Empty`;
 * - `LoadQuiz(reload)` → `QuizLoaded` / `QuizReLoaded`;
 * - капельная выдача: пауза бота, `then` пробрасывается.
 *
 * `FakeUseCase` вместо mockk: mockk не умеет распаковывать
 * `@JvmInline value class ComponentTypeRef` при `any()` / capture.
 */
class DatasourceEffectHandlerTest {

    private class FakeUseCase(
        var currentDictId: Long? = 1L,
        var coreTypes: List<ComponentType> = emptyList(),
        var pickerSelection: Set<ComponentTypeRef> = emptySet(),
    ) : QuizChatUseCase {
        var setCallCount: Int = 0
            private set
        var setCallDictId: Long? = null
            private set
        var setCallRefs: Set<ComponentTypeRef>? = null
            private set

        override suspend fun getCurrentDictionaryId(): Long? = currentDictId
        override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int = 0
        override suspend fun getRandomWriteQuizList(
            limit: Int, maxGrade: Int, dictionaryId: Long, coreTypeIds: List<Long>,
        ): List<WriteQuiz> = emptyList()
        override suspend fun getQuizConfig(dictionaryId: Long, quizMode: String): QuizConfig? = null
        override suspend fun getQuizCoreTypes(dictionaryId: Long): List<ComponentType> = coreTypes
        override suspend fun getQuizPickerSelection(dictionaryId: Long): Set<ComponentTypeRef> =
            pickerSelection
        override suspend fun setQuizPickerSelection(dictionaryId: Long, refs: Set<ComponentTypeRef>) {
            setCallCount++
            setCallDictId = dictionaryId
            setCallRefs = refs
        }
        override suspend fun getPartOfSpeechOptions(dictionaryId: Long): List<ComponentOption> =
            emptyList()
        override suspend fun getSelectedQuizGroupLabel(dictionaryId: Long): QuizGroupLabel? =
            quizGroupLabel

        var quizGroupLabel: QuizGroupLabel? = null
    }

    private val quizGame = mockk<QuizGame>(relaxed = true)
    private val prefsProvider = mockk<PrefsProvider>(relaxed = true)
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val builtInTranslation = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)
    private val userDefinition = ComponentTypeRef.UserDefined("Definition")

    private val translationType = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        core = true,
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
            coreTypes = listOf(translationType),
            pickerSelection = setOf(builtInTranslation),
        )
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizComponentTypes)

        assertTrue("emit QuizComponentTypesLoaded", msg is Msg.QuizComponentTypesLoaded)
        val loaded = msg as Msg.QuizComponentTypesLoaded
        assertEquals(listOf(translationType), loaded.types)
        assertEquals(setOf(builtInTranslation), loaded.restoredSelectedRefs)
    }

    @Test
    fun `LoadQuizComponentTypes with null dict emits Empty`() = runTest {
        val fake = FakeUseCase(currentDictId = null)
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizComponentTypes)

        assertEquals(Msg.Empty, msg)
    }

    // ===== LoadQuiz: первый раунд и продолжение =====

    @Test
    fun `LoadQuiz first round emits QuizLoaded`() = runTest {
        every { quizGame.getStat() } returns null
        val handler = makeHandler(FakeUseCase(currentDictId = 1L))

        val msg = runEffect(handler, DatasourceEffect.LoadQuiz(reload = false))

        assertEquals(Msg.QuizLoaded(content = null), msg)
    }

    @Test
    fun `LoadQuiz reload emits QuizReLoaded`() = runTest {
        every { quizGame.getStat() } returns null
        val handler = makeHandler(FakeUseCase(currentDictId = 1L))

        val msg = runEffect(handler, DatasourceEffect.LoadQuiz(reload = true))

        assertEquals(Msg.QuizReLoaded(content = null), msg)
    }

    // ===== LoadQuizGroupLabel =====

    @Test
    fun `LoadQuizGroupLabel emits validated group label`() = runTest {
        val label = QuizGroupLabel(first = "Быт", more = 2)
        val fake = FakeUseCase(currentDictId = 1L).apply { quizGroupLabel = label }
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizGroupLabel)

        assertEquals(Msg.QuizGroupLabelLoaded(label = label), msg)
    }

    @Test
    fun `LoadQuizGroupLabel with All selection emits null label`() = runTest {
        val fake = FakeUseCase(currentDictId = 1L)
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizGroupLabel)

        assertEquals(Msg.QuizGroupLabelLoaded(label = null), msg)
    }

    @Test
    fun `LoadQuizGroupLabel with null dict emits null label`() = runTest {
        val fake = FakeUseCase(currentDictId = null).apply {
            quizGroupLabel = QuizGroupLabel(first = "Быт", more = 0)
        }
        val handler = makeHandler(fake)

        val msg = runEffect(handler, DatasourceEffect.LoadQuizGroupLabel)

        assertEquals(Msg.QuizGroupLabelLoaded(label = null), msg)
    }

    // ===== SaveQuizPickerSelection =====

    @Test
    fun `SaveQuizPickerSelection calls UseCase set and emits Empty`() = runTest {
        val fake = FakeUseCase(currentDictId = 1L)
        val handler = makeHandler(fake)

        val msg = runEffect(
            handler,
            DatasourceEffect.SaveQuizPickerSelection(setOf(builtInTranslation, userDefinition)),
        )

        assertEquals(Msg.Empty, msg)
        assertEquals(1, fake.setCallCount)
        assertEquals(1L, fake.setCallDictId)
        assertEquals(setOf(builtInTranslation, userDefinition), fake.setCallRefs)
    }

    @Test
    fun `SaveQuizPickerSelection with null dict skips set and emits Empty`() = runTest {
        val fake = FakeUseCase(currentDictId = null)
        val handler = makeHandler(fake)

        val msg = runEffect(
            handler,
            DatasourceEffect.SaveQuizPickerSelection(setOf(builtInTranslation)),
        )

        assertEquals(Msg.Empty, msg)
        assertEquals(0, fake.setCallCount)
        assertNull(fake.setCallRefs)
    }

    // ===== IS508 чат-фикс 8: капельная выдача пачки сообщений бота =====

    /** Хендлер на тестовом диспатчере: паузы бота — виртуальное время runTest. */
    private fun TestScope.makeVirtualTimeHandler() = DatasourceEffectHandler(
        quizGame = quizGame,
        prefsProvider = prefsProvider,
        useCase = FakeUseCase(),
        logger = logger,
        io = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `DeliverSystemMessages waits bot pause and emits first with the rest`() = runTest {
        val handler = makeVirtualTimeHandler()
        val a = MessageContent.create(text = "a")
        val b = MessageContent.create(text = "b")

        val msg = runEffect(handler, DatasourceEffect.DeliverSystemMessages(listOf(a, b)))

        assertEquals(Msg.SystemMessageDelivered(message = a, rest = listOf(b)), msg)
        assertTrue(
            "пауза бота не короче минимальной",
            testScheduler.currentTime >= ChatTiming.BOT_PAUSE_MIN_MS,
        )
    }

    @Test
    fun `DeliverSystemMessages carries then into the delivered message`() = runTest {
        val handler = makeVirtualTimeHandler()
        val rule = MessageContent.create(text = "rule")

        val msg = runEffect(
            handler,
            DatasourceEffect.DeliverSystemMessages(listOf(rule), then = DatasourceEffect.NextQuestion),
        )

        assertEquals(
            Msg.SystemMessageDelivered(message = rule, rest = emptyList(), then = DatasourceEffect.NextQuestion),
            msg,
        )
    }

    @Test
    fun `bot messages wait at least the minimal pause`() = runTest {
        every { quizGame.hasNextQuestion() } returns true
        every { quizGame.nextQuestion() } returns QuizQuestion(
            header = "h",
            badge = null,
            value = "q",
            debugHeader = null,
        )
        every { quizGame.makeAssessment(any()) } returns AnnotatedString("ok")
        every { quizGame.skipAndGetAnswer() } returns AnnotatedString("answer")
        val handler = makeVirtualTimeHandler()

        listOf(
            DatasourceEffect.NextQuestion,
            DatasourceEffect.CheckAnswer("x"),
            DatasourceEffect.GetAnswer,
        ).forEach { effect ->
            val before = testScheduler.currentTime
            runEffect(handler, effect)
            assertTrue(
                "$effect: пауза не короче минимальной",
                testScheduler.currentTime - before >= ChatTiming.BOT_PAUSE_MIN_MS,
            )
        }
    }

    @Test
    fun `NextQuestion emits structured question`() = runTest {
        val question = QuizQuestion(header = "Перевод", badge = "сущ.", value = "яблоко", debugHeader = null)
        every { quizGame.hasNextQuestion() } returns true
        every { quizGame.nextQuestion() } returns question
        val handler = makeVirtualTimeHandler()

        val msg = runEffect(handler, DatasourceEffect.NextQuestion)

        assertEquals(Msg.NextQuestion(MessageContent.question(question)), msg)
    }

    @Test
    fun `DeliverSystemMessages with empty queue emits Empty without pause`() = runTest {
        val handler = makeVirtualTimeHandler()

        val msg = runEffect(handler, DatasourceEffect.DeliverSystemMessages(emptyList()))

        assertEquals(Msg.Empty, msg)
        assertEquals(0L, testScheduler.currentTime)
    }
}
