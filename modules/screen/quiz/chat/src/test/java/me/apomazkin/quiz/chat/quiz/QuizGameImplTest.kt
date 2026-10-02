package me.apomazkin.quiz.chat.quiz

import io.mockk.every
import io.mockk.mockk
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentOption
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
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/**
 * `WriteQuiz.toQuizItem(coreRefs, posOptions, random, …)` — чистая сборка
 * вопроса по лексеме:
 * - ядро показа — случайное из заполненных у лексемы (стаб `Random` с
 *   заданным индексом), ядра без значения в розыгрыше не участвуют;
 * - ни одного заполненного / пустые ядра / пустая лексема → null (пропуск);
 * - заголовок — имя ядра (ресурс для встроенного, имя для пользовательского);
 * - метка части речи — сокращение для встроенной опции, подпись для
 *   пользовательской, нет значения или опции — без метки;
 * - debug-шапка только в debug.
 */
class QuizGameImplTest {

    private lateinit var resourceManager: ResourceManager

    @Before
    fun setUp() {
        resourceManager = mockk()
        every { resourceManager.stringByResId(any()) } returns "Header"
        every { resourceManager.stringByResId(any(), any()) } returns "Header"
        every { resourceManager.stringByResId(R.string.chat_menu_item_component_translation) } returns "Перевод"
        every { resourceManager.stringByResId(R.string.part_of_speech_short_noun) } returns "сущ."
        every { resourceManager.stringByResId(R.string.part_of_speech_short_collocation) } returns "колл."
    }

    /** Детерминированный «случайный» выбор: всегда заданный индекс. */
    private fun fixedRandom(index: Int): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = index.coerceIn(0, until - 1)
    }

    private val translationRef = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)
    private val definitionRef = ComponentTypeRef.UserDefined("Definition")

    private val posType = ComponentType(
        id = ComponentTypeId(3L),
        systemKey = BuiltInComponent.PART_OF_SPEECH,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.CHOICE,
        position = 2,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private val nounOption = ComponentOption(
        id = 100L,
        componentTypeId = ComponentTypeId(3L),
        systemKey = "noun",
        position = 0,
    )

    private val customOption = ComponentOption(
        id = 101L,
        componentTypeId = ComponentTypeId(3L),
        systemKey = null,
        label = "междометие",
        position = 1,
    )

    private val collocationOption = ComponentOption(
        id = 102L,
        componentTypeId = ComponentTypeId(3L),
        systemKey = "collocation",
        position = 9,
    )

    private val posOptions = listOf(nounOption, customOption, collocationOption).associateBy { it.id }

    private fun translationCv(text: String = "hola") = ComponentValue(
        id = ComponentValueId(10L),
        lexemeId = LexemeId(42L),
        type = ComponentType(
            id = ComponentTypeId(1L),
            systemKey = BuiltInComponent.TRANSLATION,
            dictionaryId = null,
            name = null,
            template = ComponentTemplate.TEXT,
            position = 0,
            core = true,
            createdAt = Date(0L),
            updatedAt = Date(0L),
        ),
        data = TextValues(value = Primitive.Text(text)),
    )

    private fun definitionCv(text: String = "greeting") = ComponentValue(
        id = ComponentValueId(11L),
        lexemeId = LexemeId(42L),
        type = ComponentType(
            id = ComponentTypeId(2L),
            systemKey = null,
            dictionaryId = 1L,
            name = "Definition",
            template = ComponentTemplate.TEXT,
            position = 1,
            core = true,
            createdAt = Date(0L),
            updatedAt = Date(0L),
        ),
        data = TextValues(value = Primitive.Text(text)),
    )

    private fun posCv(optionId: Long) = ComponentValue(
        id = ComponentValueId(12L),
        lexemeId = LexemeId(42L),
        type = posType,
        data = ChoiceValues(optionId = optionId),
    )

    private fun lexemeWith(components: List<ComponentValue>) = Lexeme(
        lexemeId = LexemeId(42L),
        components = components,
        addDate = Date(0L),
    )

    private fun quizWith(lexeme: Lexeme) = WriteQuiz(
        id = 1L,
        dictionaryId = 1L,
        grade = 0,
        score = 0,
        errorCount = 0,
        addDate = Date(0L),
        lexeme = lexeme,
        word = Word(id = 1L, value = "answer"),
    )

    private fun WriteQuiz.item(
        refs: List<ComponentTypeRef>,
        index: Int = 0,
        isDebugOn: Boolean = false,
    ): QuizItem? = toQuizItem(
        coreRefs = refs,
        posOptions = posOptions,
        random = fixedRandom(index),
        resourceManager = resourceManager,
        isDebugOn = isDebugOn,
    )

    // ===== выбор ядра =====

    @Test
    fun `translation core matched yields QuizItem with resource header`() {
        val q = quizWith(lexemeWith(listOf(translationCv("hello"))))

        val item = q.item(listOf(translationRef))

        assertNotNull(item)
        assertEquals("answer", item!!.answer)
        assertEquals("Перевод", item.question.header)
        assertEquals("hello", item.question.value)
    }

    @Test
    fun `user defined core matched yields QuizItem with name header`() {
        val q = quizWith(lexemeWith(listOf(definitionCv("a salutation"))))

        val item = q.item(listOf(definitionRef))

        assertNotNull(item)
        assertEquals("Definition", item!!.question.header)
        assertEquals("a salutation", item.question.value)
    }

    @Test
    fun `core not present in lexeme yields null (graceful skip)`() {
        val q = quizWith(lexemeWith(listOf(translationCv())))

        assertNull(q.item(listOf(definitionRef)))
    }

    @Test
    fun `empty core refs yields null`() {
        val q = quizWith(lexemeWith(listOf(translationCv())))

        assertNull(q.item(emptyList()))
    }

    @Test
    fun `empty lexeme components with non-empty refs yields null`() {
        val q = quizWith(lexemeWith(emptyList()))

        assertNull(q.item(listOf(translationRef)))
    }

    @Test
    fun `two cores present - random index picks either`() {
        val q = quizWith(lexemeWith(listOf(definitionCv("def"), translationCv("trn"))))
        val refs = listOf(translationRef, definitionRef)

        assertEquals("trn", q.item(refs, index = 0)!!.question.value)
        assertEquals("def", q.item(refs, index = 1)!!.question.value)
    }

    @Test
    fun `core without value is excluded from the draw`() {
        // Из двух ядер заполнено одно — розыгрыш идёт только по нему.
        val q = quizWith(lexemeWith(listOf(definitionCv("only-def"))))
        val refs = listOf(translationRef, definitionRef)

        assertEquals("only-def", q.item(refs, index = 0)!!.question.value)
        assertEquals("only-def", q.item(refs, index = 1)!!.question.value)
    }

    // ===== метка части речи =====

    @Test
    fun `builtin part of speech option gives abbreviation badge`() {
        val q = quizWith(lexemeWith(listOf(translationCv(), posCv(optionId = 100L))))

        assertEquals("сущ.", q.item(listOf(translationRef))!!.question.badge)
    }

    @Test
    fun `multiword unit option gives its abbreviation badge`() {
        val q = quizWith(lexemeWith(listOf(translationCv(), posCv(optionId = 102L))))

        assertEquals("колл.", q.item(listOf(translationRef))!!.question.badge)
    }

    @Test
    fun `custom part of speech option gives its label as badge`() {
        val q = quizWith(lexemeWith(listOf(translationCv(), posCv(optionId = 101L))))

        assertEquals("междометие", q.item(listOf(translationRef))!!.question.badge)
    }

    @Test
    fun `no part of speech value - no badge`() {
        val q = quizWith(lexemeWith(listOf(translationCv())))

        assertNull(q.item(listOf(translationRef))!!.question.badge)
    }

    @Test
    fun `part of speech option unknown (removed) - no badge`() {
        val q = quizWith(lexemeWith(listOf(translationCv(), posCv(optionId = 999L))))

        assertNull(q.item(listOf(translationRef))!!.question.badge)
    }

    // ===== debug-шапка =====

    @Test
    fun `debug header only when debug is on`() {
        val q = quizWith(lexemeWith(listOf(translationCv())))

        assertNull(q.item(listOf(translationRef), isDebugOn = false)!!.question.debugHeader)
        val debug = q.item(listOf(translationRef), isDebugOn = true)!!.question.debugHeader
        assertNotNull(debug)
        assertEquals(true, debug!!.text.contains("grade: 0"))
    }
}
