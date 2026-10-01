package me.apomazkin.quiz.chat.quiz

import io.mockk.coEvery
import io.mockk.coVerify
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
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/**
 * Проводка `QuizGameImpl.fetchData`:
 * - ядра раунда = выбор ∩ кандидаты (пусто → все), их id уходят в порцию;
 * - нет кандидатов → пусто, порция не запрашивается;
 * - `QuizConfig` не читается;
 * - лексема без значения в ядрах раунда пропускается (страховка после SQL).
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
        core = true,
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
        core = true,
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
        coEvery { quizChatUseCase.getQuizCoreTypes(1L) } returns listOf(translationType, definitionType)
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns emptySet()
        coEvery { quizChatUseCase.getPartOfSpeechOptions(1L) } returns emptyList()
        coEvery { prefsProvider.getBoolean(any()) } returns false

        quizGame = QuizGameImpl(
            quizChatUseCase = quizChatUseCase,
            resourceManager = resourceManager,
            prefsProvider = prefsProvider,
            logger = logger,
            random = Random(42),
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

    private fun stubPortion(vararg quizzes: WriteQuiz) {
        coEvery {
            quizChatUseCase.getRandomWriteQuizList(any(), any(), any(), any())
        } returns quizzes.toList()
    }

    @Test
    fun `selected cores intersected with candidates go to the portion`() = runTest {
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns
            setOf(ComponentTypeRef.UserDefined("Definition"))
        stubPortion(quiz(listOf(translationCv(), definitionCv("def-text"))))

        quizGame.loadData()

        coVerify {
            quizChatUseCase.getRandomWriteQuizList(
                limit = any(),
                maxGrade = any(),
                dictionaryId = 1L,
                coreTypeIds = listOf(2L),
            )
        }
        assertTrue(quizGame.hasNextQuestion())
        assertEquals("def-text", quizGame.nextQuestion().value)
    }

    @Test
    fun `empty selection - all candidates go to the portion`() = runTest {
        stubPortion(quiz(listOf(translationCv("trans-text"))))

        quizGame.loadData()

        coVerify {
            quizChatUseCase.getRandomWriteQuizList(
                limit = any(),
                maxGrade = any(),
                dictionaryId = 1L,
                coreTypeIds = listOf(1L, 2L),
            )
        }
        assertTrue(quizGame.hasNextQuestion())
        assertEquals("trans-text", quizGame.nextQuestion().value)
    }

    @Test
    fun `stale selection - all candidates go to the portion`() = runTest {
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns
            setOf(ComponentTypeRef.UserDefined("Removed"))
        stubPortion(quiz(listOf(translationCv("trans-text"))))

        quizGame.loadData()

        coVerify {
            quizChatUseCase.getRandomWriteQuizList(
                limit = any(),
                maxGrade = any(),
                dictionaryId = 1L,
                coreTypeIds = listOf(1L, 2L),
            )
        }
    }

    @Test
    fun `no candidates - empty round without portion request`() = runTest {
        coEvery { quizChatUseCase.getQuizCoreTypes(1L) } returns emptyList()

        quizGame.loadData()

        assertFalse(quizGame.hasNextQuestion())
        coVerify(exactly = 0) { quizChatUseCase.getRandomWriteQuizList(any(), any(), any(), any()) }
    }

    @Test
    fun `quiz config is never read`() = runTest {
        stubPortion(quiz(listOf(translationCv())))

        quizGame.loadData()

        coVerify(exactly = 0) { quizChatUseCase.getQuizConfig(any(), any()) }
    }

    @Test
    fun `lexeme without value in round cores - graceful skip (no crash)`() = runTest {
        coEvery { quizChatUseCase.getQuizPickerSelection(1L) } returns
            setOf(ComponentTypeRef.UserDefined("Definition"))
        // Только перевод, а раунд спрашивает определением — слово пропускается.
        stubPortion(quiz(listOf(translationCv())))

        quizGame.loadData()

        assertFalse("graceful skip — no questions", quizGame.hasNextQuestion())
    }

    @Test
    fun `no current dictionary - empty round`() = runTest {
        coEvery { quizChatUseCase.getCurrentDictionaryId() } returns null

        quizGame.loadData()

        assertFalse(quizGame.hasNextQuestion())
        coVerify(exactly = 0) { quizChatUseCase.getQuizCoreTypes(any()) }
    }
}
