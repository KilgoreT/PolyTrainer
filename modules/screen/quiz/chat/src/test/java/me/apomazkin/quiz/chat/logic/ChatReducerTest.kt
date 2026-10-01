package me.apomazkin.quiz.chat.logic

import androidx.compose.ui.text.AnnotatedString
import io.mockk.every
import io.mockk.mockk
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.quiz.QuizQuestion
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Ветки `ChatReducer`:
 * - пикер ядер: загрузка (`QuizComponentTypesLoaded`), галки
 *   (`ToggleQuizComponent`, последнюю снять нельзя), видимость подменю;
 * - раунды: `Start`/`CONTINUE` → `LoadQuiz(reload)`, правило игры один
 *   раз после «Начать» капельно, «Продолжить квиз» без правила;
 * - капельная выдача с продолжением (`then`), структурный вопрос.
 */
class ChatReducerTest {

    private val logger = mockk<LexemeLogger>(relaxed = true)
    private val resourceManager = mockk<ResourceManager>().apply {
        every { stringByResId(any()) } returns "Welcome"
        every { stringByResId(any(), any()) } returns "Welcome"
        every { stringByResId(R.string.chat_quiz_msg_system_rule) } returns "Rule"
        every { stringByResId(R.string.chat_quiz_msg_user_start) } returns "Start"
        every { stringByResId(R.string.chat_quiz_msg_user_continue) } returns "Continue"
    }
    private val reducer = ChatReducer(logger = logger, resourceManager = resourceManager)

    private fun translationType() = ComponentType(
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

    private fun definitionType() = ComponentType(
        id = ComponentTypeId(2L),
        systemKey = null,
        dictionaryId = 1L,
        name = "Definition",
        template = ComponentTemplate.TEXT,
        position = 1,
        core = true,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private val builtInTranslation = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)
    private val userDefinition = ComponentTypeRef.UserDefined("Definition")

    private val initialState = ChatScreenState()

    private fun ChatScreenState.picker() = appBarState.itemsState.quizComponent
    private fun ChatScreenState.messages() = chat.messagesState.list
    private fun ChatScreenState.messageCount(): Int = messages().size

    // ===== PrepareToStart =====

    @Test
    fun `PrepareToStart emits LoadQuizComponentTypes effect`() {
        val result = reducer.testReduce(initialState, Msg.PrepareToStart)

        assertTrue(
            "should emit LoadQuizComponentTypes",
            result.effects().contains(DatasourceEffect.LoadQuizComponentTypes),
        )
    }

    // ===== QuizComponentTypesLoaded =====

    @Test
    fun `QuizComponentTypesLoaded with empty types sets empty state`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(types = emptyList(), restoredSelectedRefs = setOf(builtInTranslation)),
        )

        val qc = result.state().picker()
        assertTrue(qc.availableTypes.isEmpty())
        assertTrue(qc.selectedRefs.isEmpty())
        assertFalse(qc.isPickerVisible)
        result.assertNoEffects()
    }

    @Test
    fun `QuizComponentTypesLoaded restored subset preserved`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRefs = setOf(userDefinition),
            ),
        )

        assertEquals(setOf(userDefinition), result.state().picker().selectedRefs)
    }

    @Test
    fun `QuizComponentTypesLoaded restored with stale ref keeps only available`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRefs = setOf(userDefinition, ComponentTypeRef.UserDefined("Removed")),
            ),
        )

        assertEquals(setOf(userDefinition), result.state().picker().selectedRefs)
    }

    @Test
    fun `QuizComponentTypesLoaded restored all stale - all available`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRefs = setOf(ComponentTypeRef.UserDefined("Removed")),
            ),
        )

        assertEquals(setOf(builtInTranslation, userDefinition), result.state().picker().selectedRefs)
    }

    @Test
    fun `QuizComponentTypesLoaded restored empty - all available`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRefs = emptySet(),
            ),
        )

        assertEquals(setOf(builtInTranslation, userDefinition), result.state().picker().selectedRefs)
        assertTrue(result.state().picker().isPickerVisible)
    }

    @Test
    fun `QuizComponentTypesLoaded single type - selected, picker hidden`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType()),
                restoredSelectedRefs = setOf(ComponentTypeRef.UserDefined("Removed")),
            ),
        )

        val qc = result.state().picker()
        assertEquals(setOf(builtInTranslation), qc.selectedRefs)
        assertFalse(qc.isPickerVisible)
    }

    @Test
    fun `QuizComponentTypesLoaded double-emit idempotent`() {
        // Начальная загрузка и первая эмиссия подписки могут прийти подряд.
        val msg = Msg.QuizComponentTypesLoaded(
            types = listOf(translationType(), definitionType()),
            restoredSelectedRefs = setOf(builtInTranslation),
        )

        val r1 = reducer.testReduce(initialState, msg)
        val r2 = reducer.testReduce(r1.state(), msg)

        assertEquals(r1.state(), r2.state())
    }

    // ===== ToggleQuizComponent =====

    private fun twoCoresState(selected: Set<ComponentTypeRef>) = initialState.updateQuizComponent(
        types = listOf(translationType(), definitionType()),
        selectedRefs = selected,
    )

    @Test
    fun `ToggleQuizComponent check adds ref and saves set`() {
        val state = twoCoresState(selected = setOf(builtInTranslation))

        val result = reducer.testReduce(state, Msg.ToggleQuizComponent(userDefinition, checked = true))

        assertEquals(setOf(builtInTranslation, userDefinition), result.state().picker().selectedRefs)
        assertEquals(
            setOf(DatasourceEffect.SaveQuizPickerSelection(setOf(builtInTranslation, userDefinition))),
            result.effects(),
        )
    }

    @Test
    fun `ToggleQuizComponent uncheck removes ref and saves set`() {
        val state = twoCoresState(selected = setOf(builtInTranslation, userDefinition))

        val result = reducer.testReduce(state, Msg.ToggleQuizComponent(builtInTranslation, checked = false))

        assertEquals(setOf(userDefinition), result.state().picker().selectedRefs)
        assertEquals(
            setOf(DatasourceEffect.SaveQuizPickerSelection(setOf(userDefinition))),
            result.effects(),
        )
    }

    @Test
    fun `ToggleQuizComponent uncheck last is no-op`() {
        val state = twoCoresState(selected = setOf(builtInTranslation))

        val result = reducer.testReduce(state, Msg.ToggleQuizComponent(builtInTranslation, checked = false))

        assertEquals(state, result.state())
        result.assertNoEffects()
    }

    // ===== раунды и правило игры =====

    @Test
    fun `Start loads first round`() {
        val result = reducer.testReduce(initialState, Msg.Start)

        assertEquals(setOf(DatasourceEffect.LoadQuiz(reload = false)), result.effects())
    }

    @Test
    fun `Continue reloads round`() {
        val result = reducer.testReduce(initialState, Msg.UserAction(ChatMessage.Companion.UserAction.CONTINUE))

        assertEquals(setOf(DatasourceEffect.LoadQuiz(reload = true)), result.effects())
    }

    @Test
    fun `QuizLoaded puts Start bubble and drips rule before first question`() {
        val result = reducer.testReduce(initialState, Msg.QuizLoaded(content = null))
        val state = result.state()

        assertTrue(state.chat.readyToStart)
        assertEquals(initialState.messageCount() + 1, state.messageCount())
        val last = state.messages().last()
        assertFalse(last.isSystemMessage)
        assertEquals("Start", last.message.asString())
        assertEquals(UserMessageOrigin.START_BUTTON, last.origin)

        val effect = result.effects().single() as DatasourceEffect.DeliverSystemMessages
        assertEquals(listOf("Rule"), effect.messages.map { it.value.asString() })
        assertEquals(DatasourceEffect.NextQuestion, effect.then)
    }

    @Test
    fun `QuizLoaded with debug stat drips rule then stat`() {
        val result = reducer.testReduce(initialState, Msg.QuizLoaded(content = AnnotatedString("stat")))

        val effect = result.effects().single() as DatasourceEffect.DeliverSystemMessages
        assertEquals(listOf("Rule", "stat"), effect.messages.map { it.value.asString() })
        assertEquals(DatasourceEffect.NextQuestion, effect.then)
    }

    @Test
    fun `QuizReLoaded puts Continue bubble and asks next question without rule`() {
        val result = reducer.testReduce(initialState, Msg.QuizReLoaded(content = null))
        val state = result.state()

        val last = state.messages().last()
        assertFalse(last.isSystemMessage)
        assertEquals("Continue", last.message.asString())
        assertEquals(setOf(DatasourceEffect.NextQuestion), result.effects())
    }

    @Test
    fun `QuizReLoaded with debug stat drips stat only`() {
        val result = reducer.testReduce(initialState, Msg.QuizReLoaded(content = AnnotatedString("stat")))

        val effect = result.effects().single() as DatasourceEffect.DeliverSystemMessages
        assertEquals(listOf("stat"), effect.messages.map { it.value.asString() })
        assertEquals(DatasourceEffect.NextQuestion, effect.then)
    }

    @Test
    fun `SystemMessageDelivered last in queue runs then`() {
        val rule = MessageContent.create(text = "rule")

        val result = reducer.testReduce(
            initialState,
            Msg.SystemMessageDelivered(message = rule, rest = emptyList(), then = DatasourceEffect.NextQuestion),
        )

        assertEquals(initialState.messageCount() + 1, result.state().messageCount())
        assertEquals(setOf(DatasourceEffect.NextQuestion), result.effects())
    }

    @Test
    fun `SystemMessageDelivered with rest carries then forward`() {
        val a = MessageContent.create(text = "a")
        val b = MessageContent.create(text = "b")

        val result = reducer.testReduce(
            initialState,
            Msg.SystemMessageDelivered(message = a, rest = listOf(b), then = DatasourceEffect.NextQuestion),
        )

        assertEquals(
            setOf(DatasourceEffect.DeliverSystemMessages(listOf(b), then = DatasourceEffect.NextQuestion)),
            result.effects(),
        )
    }

    // ===== сабтайтл: подпись набора групп =====

    @Test
    fun `QuizGroupLabelLoaded stores label for app bar subtitle`() {
        val label = QuizGroupLabel(first = "Быт", more = 2)

        val result = reducer.testReduce(initialState, Msg.QuizGroupLabelLoaded(label))

        assertEquals(label, result.state().appBarState.quizGroupLabel)
        result.assertNoEffects()
    }

    @Test
    fun `QuizGroupLabelLoaded null means All`() {
        val withLabel = reducer
            .testReduce(initialState, Msg.QuizGroupLabelLoaded(QuizGroupLabel(first = "Быт", more = 0)))
            .state()

        val result = reducer.testReduce(withLabel, Msg.QuizGroupLabelLoaded(null))

        assertEquals(null, result.state().appBarState.quizGroupLabel)
    }

    // ===== структурный вопрос =====

    @Test
    fun `NextQuestion with structured question keeps it structured in the feed`() {
        val question = QuizQuestion(header = "Перевод", badge = "сущ.", value = "яблоко", debugHeader = null)

        val result = reducer.testReduce(initialState, Msg.NextQuestion(MessageContent.question(question)))

        val last = result.state().messages().last()
        assertTrue(last.isSystemMessage)
        assertEquals(ChatMessage.MessageValue.Question(question), last.message)
        assertEquals("Перевод:\nяблоко", last.message.asString())
        assertEquals("Перевод:\nяблоко", last.message.asText().text)
    }

    // ===== IS508 чат-фикс 2: кнопки действий — элемент ленты =====

    /** Сессия идёт: вопрос — последнее сообщение, флаг действий поднят. */
    private fun questionState(): ChatScreenState = reducer
        .testReduce(initialState, Msg.NextQuestion(MessageContent.create(text = "question")))
        .state()

    @Test
    fun `NextQuestion raises showUserActions, question is last system message without buttons`() {
        val state = questionState()
        val last = state.messages().last()

        assertTrue(state.chat.showUserActions)
        assertTrue(last.isSystemMessage)
        assertTrue(last.buttons.isEmpty())
    }

    @Test
    fun `UserAttempt, GetAnswer, Skip drop showUserActions`() {
        val state = questionState()

        listOf(Msg.UserAttempt("x"), Msg.GetAnswer, Msg.Skip).forEach { msg ->
            val result = reducer.testReduce(state, msg)
            assertEquals("$msg", false, result.state().chat.showUserActions)
        }
    }

    // ===== IS508 чат-фикс 8: пачки сообщений бота — по одному =====

    @Test
    fun `SessionOver adds summary now and queues options via DeliverSystemMessages`() {
        val state = questionState()
        val result = reducer.testReduce(state, Msg.SessionOver(MessageContent.create(text = "summary")))
        val newState = result.state()

        assertEquals(state.messageCount() + 1, newState.messageCount())
        assertTrue(newState.messages().last().isSystemMessage)
        assertTrue(newState.messages().last().buttons.isEmpty())
        val effect = result.effects().single() as DatasourceEffect.DeliverSystemMessages
        assertEquals(1, effect.messages.size)
        assertEquals(3, effect.messages.single().buttons.size)
        assertEquals(null, effect.then)
    }

    @Test
    fun `SystemMessageDelivered adds message and requeues the rest until empty`() {
        val state = questionState()
        val a = MessageContent.create(text = "a")
        val b = MessageContent.create(text = "b")

        val r1 = reducer.testReduce(state, Msg.SystemMessageDelivered(message = a, rest = listOf(b)))
        assertEquals(state.messageCount() + 1, r1.state().messageCount())
        assertEquals(1, r1.effects().size)
        assertTrue(r1.effects().contains(DatasourceEffect.DeliverSystemMessages(listOf(b))))

        val r2 = reducer.testReduce(r1.state(), Msg.SystemMessageDelivered(message = b, rest = emptyList()))
        assertEquals(state.messageCount() + 2, r2.state().messageCount())
        r2.assertNoEffects()
    }

    @Test
    fun `Summary adds first now, queues options, drops user actions`() {
        val state = questionState()
        val result = reducer.testReduce(state, Msg.Summary(listOf(MessageContent.create(text = "detail"))))

        assertEquals(state.messageCount() + 1, result.state().messageCount())
        assertEquals(false, result.state().chat.showUserActions)
        val effect = result.effects().single() as DatasourceEffect.DeliverSystemMessages
        assertEquals(1, effect.messages.size)
        assertEquals(2, effect.messages.single().buttons.size)
    }
}
