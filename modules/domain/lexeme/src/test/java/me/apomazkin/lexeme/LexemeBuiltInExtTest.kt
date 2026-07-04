package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

/**
 * Unit tests для extension `Lexeme.builtIn(BuiltInComponent)` (Gap-7).
 */
@Suppress("DEPRECATION")
class LexemeBuiltInExtTest {

    private val addDate = Date(1_700_000_000_000L)

    private fun translationType(id: Long = 1L) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = addDate,
        updatedAt = addDate,
    )

    private fun userDefinedType(id: Long, name: String) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = null,
        dictionaryId = 1L,
        name = name,
        template = ComponentTemplate.TEXT,
        position = 1,
        createdAt = addDate,
        updatedAt = addDate,
    )

    private fun translationCv(text: String = "hola") = ComponentValue(
        id = ComponentValueId(10L),
        lexemeId = LexemeId(42L),
        type = translationType(),
        data = TextValues(value = Primitive.Text(text)),
    )

    private fun userDefinedCv(name: String = "Definition", text: String = "greeting") = ComponentValue(
        id = ComponentValueId(11L),
        lexemeId = LexemeId(42L),
        type = userDefinedType(2L, name),
        data = TextValues(value = Primitive.Text(text)),
    )

    private fun lexemeOf(components: List<ComponentValue>) = Lexeme(
        lexemeId = LexemeId(42L),
        components = components,
        addDate = addDate,
    )

    @Test
    fun `builtIn TRANSLATION returns component when translation present`() {
        val cv = translationCv()
        val lex = lexemeOf(listOf(cv))

        val result = lex.builtIn(BuiltInComponent.TRANSLATION)

        assertEquals(cv, result)
    }

    @Test
    fun `builtIn TRANSLATION returns null when no translation`() {
        val lex = lexemeOf(listOf(userDefinedCv()))

        val result = lex.builtIn(BuiltInComponent.TRANSLATION)

        assertNull(result)
    }

    @Test
    fun `builtIn ignores user-defined types with name='translation'`() {
        // Защита от ложных positive: lookup только по systemKey, не по name.
        val fakeUser = ComponentValue(
            id = ComponentValueId(100L),
            lexemeId = LexemeId(42L),
            type = userDefinedType(50L, "translation"),  // user-defined с именем translation
            data = TextValues(value = Primitive.Text("imposter")),
        )
        val lex = lexemeOf(listOf(fakeUser))

        val result = lex.builtIn(BuiltInComponent.TRANSLATION)

        assertNull("user-defined name='translation' не должен матчиться", result)
    }

    @Test
    fun `builtIn returns built-in even when user-defined components present`() {
        val translation = translationCv("hi")
        val definition = userDefinedCv("Definition", "greet")
        val other = userDefinedCv("Other", "x")
        val lex = lexemeOf(listOf(definition, other, translation))

        val result = lex.builtIn(BuiltInComponent.TRANSLATION)

        assertEquals(translation, result)
    }

    @Test
    fun `builtIn on empty components returns null`() {
        val lex = lexemeOf(emptyList())

        val result = lex.builtIn(BuiltInComponent.TRANSLATION)

        assertNull(result)
    }
}
