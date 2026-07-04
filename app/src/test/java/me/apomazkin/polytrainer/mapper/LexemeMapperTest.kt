package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.ComponentValueApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.ImageValues
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests для `LexemeApiEntity.toDomain()` маппера IS481.
 *
 * Покрывает (test_gap categories 1):
 * - B7 shim consistency invariant — translation/definition shim ↔ components built-in lookup.
 * - B8 / C11 orphan lexeme (empty components).
 * - C12 malformed data (M13: variant не TextValues → null shim, no crash).
 * - C13 multi-component lookup correctness.
 * - Translation built-in by systemKey (не name=="translation").
 */
@Suppress("DEPRECATION")
class LexemeMapperTest {

    private val addDate = Date(1_700_000_000_000L)
    private val changeDate = Date(1_700_000_100_000L)

    private fun translationType(id: Long = 1L) = ComponentTypeApiEntity(
        id = id,
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = addDate,
        updatedAt = addDate,
    )

    private fun definitionType(id: Long = 2L, dictId: Long = 1L) = ComponentTypeApiEntity(
        id = id,
        systemKey = null,
        dictionaryId = dictId,
        name = "Definition",
        template = ComponentTemplate.TEXT,
        position = 1,
        createdAt = addDate,
        updatedAt = addDate,
    )

    private fun userDefinedType(
        id: Long,
        name: String,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        dictId: Long = 1L,
    ) = ComponentTypeApiEntity(
        id = id,
        systemKey = null,
        dictionaryId = dictId,
        name = name,
        template = template,
        position = 2,
        createdAt = addDate,
        updatedAt = addDate,
    )

    private fun cv(
        id: Long,
        lexemeId: Long,
        type: ComponentTypeApiEntity,
        data: TemplateValues,
    ) = ComponentValueApiEntity(
        id = id,
        lexemeId = lexemeId,
        type = type,
        data = data,
        createdAt = addDate,
        updatedAt = addDate,
    )

    private fun lexeme(
        id: Long = 42L,
        components: List<ComponentValueApiEntity> = emptyList(),
    ) = LexemeApiEntity(
        id = id,
        components = components,
        wordClass = null,
        options = 0,
        addDate = addDate,
        changeDate = changeDate,
    )

    @Test
    fun `given empty components then translation and definition shim null and components empty`() {
        // B8 / C11 — orphan lexeme.
        val api = lexeme(components = emptyList())

        val domain = api.toDomain()

        assertNull(domain.translation)
        assertNull(domain.definition)
        assertTrue(domain.components.isEmpty())
        assertEquals(LexemeId(42L), domain.lexemeId)
    }

    @Test
    fun `given translation-only built-in component then translation shim is populated`() {
        val api = lexeme(
            components = listOf(
                cv(10L, 42L, translationType(), TextValues(value = Primitive.Text("hola"))),
            ),
        )

        val domain = api.toDomain()

        assertEquals(Translation("hola"), domain.translation)
        assertNull(domain.definition)
        assertEquals(1, domain.components.size)
    }

    @Test
    fun `given user-defined Definition only then definition shim is populated and translation null`() {
        val api = lexeme(
            components = listOf(
                cv(11L, 42L, definitionType(), TextValues(value = Primitive.Text("a salutation"))),
            ),
        )

        val domain = api.toDomain()

        assertNull(domain.translation)
        assertEquals(Definition("a salutation"), domain.definition)
    }

    @Test
    fun `given both translation builtin and Definition user-defined then both shim populated`() {
        val api = lexeme(
            components = listOf(
                cv(10L, 42L, translationType(), TextValues(value = Primitive.Text("hi"))),
                cv(11L, 42L, definitionType(), TextValues(value = Primitive.Text("greeting"))),
            ),
        )

        val domain = api.toDomain()

        assertEquals(Translation("hi"), domain.translation)
        assertEquals(Definition("greeting"), domain.definition)
        assertEquals(2, domain.components.size)
    }

    @Test
    fun `given multiple user-defined types but no Definition then definition shim is null`() {
        // C13 — multi-component, definition shim только для name="Definition".
        val api = lexeme(
            components = listOf(
                cv(20L, 42L, userDefinedType(id = 100L, name = "Other"), TextValues(value = Primitive.Text("v1"))),
                cv(21L, 42L, userDefinedType(id = 101L, name = "Example"), TextValues(value = Primitive.Text("v2"))),
            ),
        )

        val domain = api.toDomain()

        assertNull(domain.translation)
        assertNull(domain.definition)
        assertEquals(2, domain.components.size)
    }

    @Test
    fun `given translation built-in with non-text data then translation shim null no crash`() {
        // C12 — defensive cast on data.
        val api = lexeme(
            components = listOf(
                cv(10L, 42L, translationType(), ImageValues(value = Primitive.Image("file://img"))),
            ),
        )

        val domain = api.toDomain()

        assertNull("non-text data → shim null, not crash", domain.translation)
        assertEquals(1, domain.components.size)
    }

    @Test
    fun `translation lookup is by systemKey not by name`() {
        // Built-in lookup НЕ должен срабатывать на user-defined `name="translation"`.
        val fakeUserTranslation = ComponentTypeApiEntity(
            id = 50L,
            systemKey = null,
            dictionaryId = 1L,
            name = "translation",
            template = ComponentTemplate.TEXT,
            position = 0,
            createdAt = addDate,
            updatedAt = addDate,
        )
        val api = lexeme(
            components = listOf(
                cv(10L, 42L, fakeUserTranslation, TextValues(value = Primitive.Text("imposter"))),
            ),
        )

        val domain = api.toDomain()

        assertNull("translation shim only matches systemKey=TRANSLATION", domain.translation)
    }

    @Test
    fun `given id when toDomain then wraps into LexemeId`() {
        val api = lexeme(id = 123L)

        val domain = api.toDomain()

        assertEquals(LexemeId(123L), domain.lexemeId)
    }

    @Test
    fun `given addDate and changeDate when toDomain then they pass through`() {
        val api = lexeme(id = 1L)

        val domain = api.toDomain()

        assertEquals(addDate, domain.addDate)
        assertEquals(changeDate, domain.changeDate)
    }

    @Test
    fun `components in api are mapped to domain in same order`() {
        val api = lexeme(
            components = listOf(
                cv(10L, 42L, translationType(), TextValues(value = Primitive.Text("t"))),
                cv(20L, 42L, userDefinedType(100L, "Other"), TextValues(value = Primitive.Text("o"))),
                cv(11L, 42L, definitionType(), TextValues(value = Primitive.Text("d"))),
            ),
        )

        val domain = api.toDomain()

        assertEquals(3, domain.components.size)
        assertEquals(BuiltInComponent.TRANSLATION, domain.components[0].type.systemKey)
        assertEquals("Other", domain.components[1].type.name)
        assertEquals("Definition", domain.components[2].type.name)
    }
}
