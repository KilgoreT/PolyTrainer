package me.apomazkin.quiz.chat.quiz

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * IS481 (AGG-12) integration tests `QuizGameImpl.fetchData`:
 * - selectedRef non-null → `effectiveRefs = [selectedRef]` (override).
 * - selectedRef null → fallback на `quizConfig.componentRefs`.
 * - selectedRef override semantics — quizConfig игнорируется (F4).
 * - lexeme без выбранного компонента → graceful skip (no crash).
 */
class QuizGameImplFetchDataTest {

    private lateinit var quizChatUseCase: QuizChatUseCase
    private lateinit var resourceManager: ResourceManager
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var logger: LexemeLogger
    private lateinit var quizGame: QuizGameImpl

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

    private val definitionType = ComponentType(
        id = ComponentTypeId(2L),
        systemKey = null,
        dictionaryId = 1L,
        name = "Definition",
        template = ComponentTemplate.TEXT,
        position = 1,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    @Before
    fun setUp() {
        quizChatUseCase = mockk()
        resourceManager = mockk(relaxed = true)
        prefsProvider = mockk()
        logger = mockk(relaxed = true)

        coEvery { quizChatUseCase.getCurrentDictionaryId() } returns 1L
        coEvery { prefsProvider.getBoolean(any()) } returns false

        quizGame = QuizGameImpl(
            quizChatUseCase = quizChatUseCase,
            resourceManager = resourceManager,
            prefsProvider = prefsProvider,
            logger = logger,
        )
    }

    private fun translationCv(text: String = "hola") = ComponentValue(
        id = ComponentValueId(10L),
        lexemeId = LexemeId(42L),
        type = translationType,
        data = TextValues(value = Primitive.Text(text)),
    )

    private fun definitionCv(text: String = "definition text") = ComponentValue(
        id = ComponentValueId(11L),
        lexemeId = LexemeId(42L),
        type = definitionType,
        data = TextValues(value = Primitive.Text(text)),
    )

    private fun lexemeWith(components: List<ComponentValue>) = Lexeme(
        lexemeId = LexemeId(42L),
        components = components,
        addDate = Date(0L),
    )

    private fun quiz(components: List<ComponentValue>) = WriteQuiz(
        id = 1L,
        dictionaryId = 1L,
        grade = 0,
        score = 0,
        errorCount = 0,
        addDate = Date(0L),
        lexeme = lexemeWith(components),
        word = Word(id = 1L, value = "answer"),
    )

    @Test
    fun `selectedRef non-null filters - lexeme matching selectedRef yields next question`() = runTest {
        coEvery { quizChatUseCase.getQuizConfig(any(), any()) } returns QuizConfig(
            dictionaryId = 1L,
            quizMode = "write",
            componentRefs = listOf(
                ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
                ComponentTypeRef.UserDefined("Definition"),
            ),
        )
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns
                ComponentTypeRef.UserDefined("Definition")
        // Lexeme has both translation & definition — selectedRef=Definition picks only definition.
        coEvery { quizChatUseCase.getRandomWriteQuizList(any(), any(), any()) } returns listOf(
            quiz(listOf(translationCv(), definitionCv("def-text"))),
        )

        quizGame.loadData()

        assertTrue("should have at least one question", quizGame.hasNextQuestion())
        val q = quizGame.nextQuestion()
        // Question text — text от matched component. selectedRef=Definition → "def-text".
        assertTrue(
            "question should contain definition text, got: ${q.text}",
            q.text.contains("def-text"),
        )
    }

    @Test
    fun `selectedRef null - fallback to quizConfig componentRefs`() = runTest {
        coEvery { quizChatUseCase.getQuizConfig(any(), any()) } returns QuizConfig(
            dictionaryId = 1L,
            quizMode = "write",
            componentRefs = listOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)),
        )
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns null
        coEvery { quizChatUseCase.getRandomWriteQuizList(any(), any(), any()) } returns listOf(
            quiz(listOf(translationCv("trans-text"))),
        )

        quizGame.loadData()

        assertTrue("should have a question", quizGame.hasNextQuestion())
        val q = quizGame.nextQuestion()
        assertTrue(
            "question should contain translation text, got: ${q.text}",
            q.text.contains("trans-text"),
        )
    }

    @Test
    fun `selectedRef override - quizConfig refs ignored when selectedRef set (F4)`() = runTest {
        coEvery { quizChatUseCase.getQuizConfig(any(), any()) } returns QuizConfig(
            dictionaryId = 1L,
            quizMode = "write",
            componentRefs = listOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)),
        )
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns
                ComponentTypeRef.UserDefined("Definition")
        // quizConfig has only translation; selectedRef=Definition; lexeme has both.
        // selectedRef overrides — only Definition is shown.
        coEvery { quizChatUseCase.getRandomWriteQuizList(any(), any(), any()) } returns listOf(
            quiz(listOf(translationCv("trans-text"), definitionCv("def-text"))),
        )

        quizGame.loadData()

        assertTrue("should have a question", quizGame.hasNextQuestion())
        val q = quizGame.nextQuestion()
        assertTrue(
            "question should be def-text (override), got: ${q.text}",
            q.text.contains("def-text"),
        )
    }

    @Test
    fun `lexeme without selected component - graceful skip (no crash)`() = runTest {
        coEvery { quizChatUseCase.getQuizConfig(any(), any()) } returns QuizConfig(
            dictionaryId = 1L,
            quizMode = "write",
            componentRefs = listOf(
                ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
                ComponentTypeRef.UserDefined("Definition"),
            ),
        )
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns
                ComponentTypeRef.UserDefined("Definition")
        // Lexeme has only translation, no Definition. With selectedRef=Definition →
        // toQuizItem returns null → quizList empty → hasNextQuestion false.
        coEvery { quizChatUseCase.getRandomWriteQuizList(any(), any(), any()) } returns listOf(
            quiz(listOf(translationCv())),
        )

        quizGame.loadData()

        assertFalse("graceful skip — no questions", quizGame.hasNextQuestion())
    }
}
