package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.ComponentValueWithType
import me.apomazkin.core_db_impl.entity.toApiEntity
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * IS486 фаза 1, зона D1/D2: маппинг CHOICE-значений.
 * phase1_plan.md § Зона D.
 *
 * Контракт (spec §10): строка values для CHOICE несёт payload в `option_id`,
 * JSON `value` — пустой envelope (колонка NOT NULL). Сама опция и есть значение.
 */
class ChoiceValueMappingTest {

    private lateinit var logger: RecordingLexemeLogger

    private val now = Date(0L)

    @Before
    fun setUp() {
        logger = RecordingLexemeLogger()
    }

    private fun typeDb(templateKey: String) = ComponentTypeDb(
        id = 5L,
        systemKey = null,
        dictionaryId = 1L,
        name = "Часть речи",
        templateKey = templateKey,
        position = 0,
        createdAt = now,
        updatedAt = now,
    )

    private fun valueDb(value: String, optionId: Long?) = ComponentValueDb(
        id = 100L,
        lexemeId = 10L,
        componentTypeId = 5L,
        value = value,
        optionId = optionId,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `ChoiceValues toJson is empty envelope`() {
        val json = ChoiceValues(optionId = 7L).toJson()

        val root = JSONObject(json)
        assertTrue("envelope must contain fields object", root.has("fields"))
        assertEquals(0, root.getJSONObject("fields").length())
    }

    @Test
    fun `CHOICE row with optionId maps to ChoiceValues`() {
        val row = ComponentValueWithType(
            value = valueDb(value = ChoiceValues(7L).toJson(), optionId = 7L),
            type = typeDb(templateKey = "choice"),
        )

        val api = row.toApiEntity(logger)

        assertNotNull(api)
        assertEquals(ChoiceValues(optionId = 7L), api?.data)
        assertTrue("no errors expected", logger.errors.isEmpty())
    }

    @Test
    fun `CHOICE row with null optionId is fail-soft skip with log`() {
        val row = ComponentValueWithType(
            value = valueDb(value = "{\"fields\":{}}", optionId = null),
            type = typeDb(templateKey = "choice"),
        )

        val api = row.toApiEntity(logger)

        assertNull(api)
        assertTrue("broken CHOICE row must be logged", logger.errors.isNotEmpty())
    }

    @Test
    fun `TEXT row with optionId column still maps as text - regression`() {
        val original = TextValues(Primitive.Text("собака"))
        val row = ComponentValueWithType(
            value = valueDb(value = original.toJson(), optionId = null),
            type = typeDb(templateKey = "text"),
        )

        val api = row.toApiEntity(logger)

        assertEquals(original, api?.data)
        assertTrue(logger.errors.isEmpty())
    }

    @Test
    fun `parseTemplateValues for CHOICE is fail-soft null with log - wrong call-site contract`() {
        val parsed = parseTemplateValues("{\"fields\":{}}", ComponentTemplate.CHOICE, logger)

        assertNull(parsed)
        assertTrue(logger.errors.isNotEmpty())
    }
}

private class RecordingLexemeLogger : me.apomazkin.logger.LexemeLogger {
    val errors: MutableList<String> = mutableListOf()
    override fun log(
        level: me.apomazkin.logger.LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (level == me.apomazkin.logger.LogLevel.ERROR) errors += message
    }
}
