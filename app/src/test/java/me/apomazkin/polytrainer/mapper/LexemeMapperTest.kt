package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

/**
 * Unit tests for [LexemeApiEntity.toDomain] mapper.
 *
 * Per business_contract.md § "Mapper signature":
 * - LexemeId wrap (Long → LexemeId).
 * - Translation/Definition value-classes wrap (nullable).
 * - addDate / changeDate passthrough.
 * - wordClass / options не пробрасываются (вынесены за пределы domain shape).
 *
 * TDD: mapper создаётся в business_implement шаге — тесты могут не компилироваться
 * до того момента (это ожидаемо).
 */
class LexemeMapperTest {

    private val addDate = Date(1_700_000_000_000L)
    private val changeDate = Date(1_700_000_100_000L)

    private fun makeApi(
        id: Long = 42L,
        translation: TranslationApiEntity? = null,
        definition: DefinitionApiEntity? = null,
        wordClass: String? = null,
        options: Long = 0,
        addDate: Date = this.addDate,
        changeDate: Date? = this.changeDate,
    ) = LexemeApiEntity(
        id = id,
        translation = translation,
        definition = definition,
        wordClass = wordClass,
        options = options,
        addDate = addDate,
        changeDate = changeDate,
    )

    @Test
    fun `given null translation when toDomain then domain translation is null`() {
        val api = makeApi(translation = null)

        val domain = api.toDomain()

        assertNull("translation must be null", domain.translation)
    }

    @Test
    fun `given non-null translation when toDomain then wraps into Translation value class`() {
        val api = makeApi(translation = TranslationApiEntity("hola"))

        val domain = api.toDomain()

        assertEquals(Translation("hola"), domain.translation)
    }

    @Test
    fun `given null definition when toDomain then domain definition is null`() {
        val api = makeApi(definition = null)

        val domain = api.toDomain()

        assertNull("definition must be null", domain.definition)
    }

    @Test
    fun `given non-null definition when toDomain then wraps into Definition value class`() {
        val api = makeApi(definition = DefinitionApiEntity("greeting"))

        val domain = api.toDomain()

        assertEquals(Definition("greeting"), domain.definition)
    }

    @Test
    fun `given id when toDomain then wraps into LexemeId`() {
        val api = makeApi(id = 123L)

        val domain = api.toDomain()

        assertEquals(LexemeId(123L), domain.lexemeId)
    }

    @Test
    fun `given addDate when toDomain then addDate is passed through unchanged`() {
        val expected = Date(1_234_567_890_000L)
        val api = makeApi(addDate = expected)

        val domain = api.toDomain()

        assertEquals(expected, domain.addDate)
    }

    @Test
    fun `given non-null changeDate when toDomain then changeDate is passed through unchanged`() {
        val expected = Date(1_234_999_999_000L)
        val api = makeApi(changeDate = expected)

        val domain = api.toDomain()

        assertEquals(expected, domain.changeDate)
    }

    @Test
    fun `given null changeDate when toDomain then changeDate is null`() {
        val api = makeApi(changeDate = null)

        val domain = api.toDomain()

        assertNull("changeDate must be null", domain.changeDate)
    }

    /**
     * Поля wordClass / options в API присутствуют, но НЕ пробрасываются в domain.
     * Проверяем что наличие этих полей в API не влияет на domain-результат
     * (domain shape стабилен — wordClass/options не доступны на Lexeme на этапе компиляции).
     */
    @Test
    fun `given wordClass and options in api when toDomain then domain ignores them`() {
        val withExtras = makeApi(wordClass = "noun", options = 42L)
        val withoutExtras = makeApi(wordClass = null, options = 0L)

        assertEquals(withoutExtras.toDomain(), withExtras.toDomain())
    }

    @Test
    fun `given full api entity when toDomain then all mapped fields match`() {
        val api = LexemeApiEntity(
            id = 1L,
            translation = TranslationApiEntity("hi"),
            definition = DefinitionApiEntity("a salutation"),
            wordClass = "noun",
            options = 7L,
            addDate = addDate,
            changeDate = changeDate,
        )

        val domain = api.toDomain()

        assertEquals(LexemeId(1L), domain.lexemeId)
        assertEquals(Translation("hi"), domain.translation)
        assertEquals(Definition("a salutation"), domain.definition)
        assertEquals(addDate, domain.addDate)
        assertEquals(changeDate, domain.changeDate)
    }
}
