package me.apomazkin.quiz.chat.quiz

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.LogTags
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * IS461: QuizGameImpl crash на пустом quizList.
 *
 * Тесты воспроизводят баг: при пустом результате fetchData()
 * метод hasNextQuestion() должен возвращать false,
 * а не crashить с IndexOutOfBoundsException.
 *
 * Вариант C: guard в nextStep() + bounds check в getQuiz() + лог в loadData().
 */
class QuizGameImplEmptyListTest {

    private lateinit var quizChatUseCase: QuizChatUseCase
    private lateinit var resourceManager: ResourceManager
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var logger: LexemeLogger
    private lateinit var quizGame: QuizGameImpl

    @Before
    fun setUp() {
        quizChatUseCase = mockk()
        resourceManager = mockk()
        prefsProvider = mockk()
        logger = mockk(relaxed = true)

        coEvery { quizChatUseCase.getCurrentDictionaryId() } returns 1L
        coEvery { quizChatUseCase.getQuizConfig(any(), any()) } returns QuizConfig(
            dictionaryId = 1L,
            quizMode = "write",
            componentRefs = listOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)),
        )
        coEvery { quizChatUseCase.getQuizPickerSelection(any()) } returns null
        coEvery { prefsProvider.getBoolean(any()) } returns false

        quizGame = QuizGameImpl(
            quizChatUseCase = quizChatUseCase,
            resourceManager = resourceManager,
            prefsProvider = prefsProvider,
            logger = logger,
        )
    }

    /**
     * Корневой баг: hasNextQuestion() возвращает true при пустом quizList,
     * затем nextQuestion() крашит с IndexOutOfBoundsException.
     *
     * После фикса: hasNextQuestion() должен вернуть false.
     */
    @Test
    fun `hasNextQuestion returns false when quizList is empty after loadData`() = runTest {
        // Given: fetchData возвращает пустой список
        coEvery {
            quizChatUseCase.getRandomWriteQuizList(
                dictionaryId = any(),
                limit = any(),
                maxGrade = any()
            )
        } returns emptyList()

        // When: загружаем данные
        quizGame.loadData()

        // Then: hasNextQuestion должен быть false (не крашить)
        assertFalse(
            "hasNextQuestion() должен вернуть false при пустом quizList",
            quizGame.hasNextQuestion()
        )
    }

    /**
     * Вариант C: при пустом списке loadData() логирует warning.
     */
    @Test
    fun `loadData logs warning when fetchData returns empty list`() = runTest {
        // Given
        coEvery {
            quizChatUseCase.getRandomWriteQuizList(
                dictionaryId = any(),
                limit = any(),
                maxGrade = any()
            )
        } returns emptyList()

        // When
        quizGame.loadData()

        // Then
        verify {
            logger.w(
                tag = LogTags.CHAT,
                message = "loadData: fetchData returned empty list"
            )
        }
    }

    /**
     * Повторная сессия (reload): после clearData + пустой fetchData
     * hasNextQuestion() должен быть false.
     */
    @Test
    fun `hasNextQuestion returns false on reload with empty list`() = runTest {
        // Given: первая загрузка — пустая
        coEvery {
            quizChatUseCase.getRandomWriteQuizList(
                dictionaryId = any(),
                limit = any(),
                maxGrade = any()
            )
        } returns emptyList()

        quizGame.loadData()

        // When: повторная загрузка — тоже пустая
        quizGame.loadData()

        // Then
        assertFalse(
            "hasNextQuestion() должен вернуть false при повторной загрузке с пустым списком",
            quizGame.hasNextQuestion()
        )
    }
}
