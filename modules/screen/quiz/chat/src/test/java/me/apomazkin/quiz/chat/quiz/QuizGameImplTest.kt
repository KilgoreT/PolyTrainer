package me.apomazkin.quiz.chat.quiz

import io.mockk.every
import io.mockk.mockk
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
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests для `WriteQuiz.toQuizItem(componentRefs, ...)` (IS481, AGG-5 / F2 / F4).
 *
 * Покрывает:
 * - Resolve translation built-in.
 * - Resolve user-defined Definition.
 * - Graceful skip (null) когда componentRefs не резолвится — заменяет
 *   удалённый `throw IllegalArgumentException`.
 * - Order priority — порядок componentRefs определяет приоритет (F4).
 * - Empty componentRefs → null (skip).
 * - Empty lexeme.components → null (skip).
 */
class QuizGameImplTest {

    private lateinit var resourceManager: ResourceManager

    @Before
    fun setUp() {
        resourceManager = mockk()
        every { resourceManager.stringByResId(any()) } returns "Header"
        every { resourceManager.stringByResId(any(), any()) } returns "Header"
    }

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
            createdAt = Date(0L),
            updatedAt = Date(0L),
        ),
        data = TextValues(value = Primitive.Text(text)),
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

    @Test
    fun `componentRefs translation matched in lexeme yields QuizItem`() {
        val q = quizWith(lexemeWith(listOf(translationCv("hello"))))
        val refs = listOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNotNull(item)
        assertEquals("answer", item!!.answer)
    }

    @Test
    fun `componentRefs userdef Definition matched yields QuizItem`() {
        val q = quizWith(lexemeWith(listOf(definitionCv("a salutation"))))
        val refs = listOf(ComponentTypeRef.UserDefined("Definition"))

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNotNull(item)
    }

    @Test
    fun `componentRefs not matched by lexeme yields null (graceful skip)`() {
        // F2 — graceful skip заменяет удалённый throw.
        val q = quizWith(lexemeWith(listOf(translationCv())))
        val refs = listOf(ComponentTypeRef.UserDefined("Definition"))

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNull(item)
    }

    @Test
    fun `empty componentRefs yields null`() {
        val q = quizWith(lexemeWith(listOf(translationCv())))

        val item = q.toQuizItem(emptyList(), resourceManager, isDebugOn = false)

        assertNull(item)
    }

    @Test
    fun `empty lexeme components with non-empty refs yields null`() {
        val q = quizWith(lexemeWith(emptyList()))
        val refs = listOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNull(item)
    }

    @Test
    fun `componentRefs order priority - translation first picks translation when both available (F4)`() {
        // F4: первый match по порядку config — translation выигрывает.
        val q = quizWith(lexemeWith(listOf(definitionCv("def"), translationCv("trn"))))
        val refs = listOf(
            ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
            ComponentTypeRef.UserDefined("Definition"),
        )

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNotNull(item)
        // Question = matched source text (translation за приоритет).
        assertEquals("trn", item!!.question.text)
    }

    @Test
    fun `componentRefs order priority - definition first picks definition (F4)`() {
        val q = quizWith(lexemeWith(listOf(translationCv("trn"), definitionCv("def"))))
        val refs = listOf(
            ComponentTypeRef.UserDefined("Definition"),
            ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
        )

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNotNull(item)
        assertEquals("def", item!!.question.text)
    }

    @Test
    fun `partial mismatch - first ref missing falls through to second ref`() {
        // Lexeme имеет definition но не translation. Config просит сначала translation
        // (нет) → потом definition (есть). Должен вернуть QuizItem с definition.
        val q = quizWith(lexemeWith(listOf(definitionCv("only-def"))))
        val refs = listOf(
            ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
            ComponentTypeRef.UserDefined("Definition"),
        )

        val item = q.toQuizItem(refs, resourceManager, isDebugOn = false)

        assertNotNull(item)
        assertEquals("only-def", item!!.question.text)
    }
}
