package me.apomazkin.core_db_impl.mapper

import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ImageValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты для `TemplateValuesJson.kt` (M13 envelope).
 *
 * Coverage:
 * - Round-trip `TextValues / ImageValues → toJson() → parseTemplateValues()`.
 * - Fail-soft: malformed JSON / schema mismatch / unknown primitive → null + logger.e().
 * - Golden fixtures (см. `src/test/resources/fixtures/component_values/`).
 */
class TemplateValuesJsonTest {

    private lateinit var logger: FakeLexemeLogger

    @Before
    fun setUp() {
        logger = FakeLexemeLogger()
    }

    @Test
    fun textValues_roundTrip() {
        val original = TextValues(Primitive.Text("hello"))
        val json = original.toJson()
        val parsed = parseTemplateValues(json, ComponentTemplate.TEXT, logger)
        assertEquals(original, parsed)
        assertTrue("no errors expected", logger.errors.isEmpty())
    }

    @Test
    fun imageValues_roundTrip() {
        val original = ImageValues(Primitive.Image("file:///tmp/photo.jpg"))
        val json = original.toJson()
        val parsed = parseTemplateValues(json, ComponentTemplate.IMAGE, logger)
        assertEquals(original, parsed)
        assertTrue("no errors expected", logger.errors.isEmpty())
    }

    @Test
    fun textValues_unicodeRoundTrip() {
        val original = TextValues(Primitive.Text("кошка 🦊 a 'b' \"c\""))
        val json = original.toJson()
        val parsed = parseTemplateValues(json, ComponentTemplate.TEXT, logger)
        assertEquals(original, parsed)
    }

    @Test
    fun schemaMismatch_textTemplate_imageJson_returnsNull() {
        val imageJson = ImageValues(Primitive.Image("uri")).toJson()
        val parsed = parseTemplateValues(imageJson, ComponentTemplate.TEXT, logger)
        assertNull(parsed)
        assertTrue("schema mismatch must be logged", logger.errors.isNotEmpty())
        assertTrue(logger.errors.any { it.contains("schema mismatch") })
    }

    @Test
    fun schemaMismatch_imageTemplate_textJson_returnsNull() {
        val textJson = TextValues(Primitive.Text("v")).toJson()
        val parsed = parseTemplateValues(textJson, ComponentTemplate.IMAGE, logger)
        assertNull(parsed)
        assertTrue(logger.errors.any { it.contains("schema mismatch") })
    }

    @Test
    fun malformedJson_returnsNull_loggerCalled() {
        val parsed = parseTemplateValues("not-json-at-all", ComponentTemplate.TEXT, logger)
        assertNull(parsed)
        assertTrue(logger.errors.any { it.contains("malformed JSON") })
    }

    @Test
    fun missingFieldsKey_returnsNull() {
        val parsed = parseTemplateValues(
            """{"some":"other"}""",
            ComponentTemplate.TEXT,
            logger,
        )
        assertNull(parsed)
        assertTrue(logger.errors.isNotEmpty())
    }

    @Test
    fun unknownPrimitiveType_returnsNull() {
        val unknown =
            """{"fields":{"value":{"type":"video","src":"x"}}}"""
        val parsed = parseTemplateValues(unknown, ComponentTemplate.TEXT, logger)
        assertNull(parsed)
        assertTrue(logger.errors.any { it.contains("schema mismatch") })
    }

    // IS491: CAPTIONED_TEXT — пара text + опциональный caption (UC2, UC5; С4).

    @Test
    fun captioned_roundTrip_withCaption() {
        val original = CaptionedTextValues(
            text = Primitive.Text("multi\nline quote"),
            caption = Primitive.Text("IELTS"),
        )
        val json = original.toJson()
        val parsed = parseTemplateValues(json, ComponentTemplate.CAPTIONED_TEXT, logger)
        assertEquals(original, parsed)
        assertTrue("no errors expected", logger.errors.isEmpty())
    }

    @Test
    fun captioned_roundTrip_withoutCaption_keyOmitted() {
        val original = CaptionedTextValues(
            text = Primitive.Text("quote only"),
            caption = null,
        )
        val json = original.toJson()
        assertTrue("null caption must omit the key", !json.contains("caption"))
        val parsed = parseTemplateValues(json, ComponentTemplate.CAPTIONED_TEXT, logger)
        assertEquals(original, parsed)
        assertTrue(logger.errors.isEmpty())
    }

    @Test
    fun captioned_brokenCaption_textSurvives_captionNull() {
        // Битый caption (unknown primitive type) — значение живёт с caption=null + лог.
        val json =
            """{"fields":{"text":{"type":"text","value":"quote"},"caption":{"type":"video","src":"x"}}}"""
        val parsed = parseTemplateValues(json, ComponentTemplate.CAPTIONED_TEXT, logger)
        assertEquals(
            CaptionedTextValues(text = Primitive.Text("quote"), caption = null),
            parsed,
        )
        assertTrue("broken caption must be logged", logger.errors.isNotEmpty())
    }

    @Test
    fun captioned_missingText_returnsNull() {
        val json = """{"fields":{"caption":{"type":"text","value":"IELTS"}}}"""
        val parsed = parseTemplateValues(json, ComponentTemplate.CAPTIONED_TEXT, logger)
        assertNull(parsed)
        assertTrue(logger.errors.isNotEmpty())
    }

    @Test
    fun captioned_schemaMismatch_textEnvelope_returnsNull() {
        // TEXT-envelope (единственный ключ "value") не парсится как captioned.
        val textJson = TextValues(Primitive.Text("v")).toJson()
        val parsed = parseTemplateValues(textJson, ComponentTemplate.CAPTIONED_TEXT, logger)
        assertNull(parsed)
        assertTrue(logger.errors.isNotEmpty())
    }

    @Test
    fun captioned_unicodeRoundTrip() {
        val original = CaptionedTextValues(
            text = Primitive.Text("правило: ¿cómo? 🦊"),
            caption = Primitive.Text("Разговорный it's"),
        )
        val json = original.toJson()
        val parsed = parseTemplateValues(json, ComponentTemplate.CAPTIONED_TEXT, logger)
        assertEquals(original, parsed)
    }

    @Test
    fun choiceTemplate_parse_returnsNull_wrongCallSite() {
        val parsed = parseTemplateValues(
            """{"fields":{}}""",
            ComponentTemplate.CHOICE,
            logger,
        )
        assertNull(parsed)
        assertTrue(logger.errors.isNotEmpty())
    }

    @Test
    fun goldenFixture_text() {
        val json = readFixture("fixtures/component_values/text_value.json")
        val parsed = parseTemplateValues(json, ComponentTemplate.TEXT, logger)
        assertNotNull(parsed)
        assertEquals(TextValues(Primitive.Text("Sample")), parsed)
    }

    @Test
    fun goldenFixture_image() {
        val json = readFixture("fixtures/component_values/image_value.json")
        val parsed = parseTemplateValues(json, ComponentTemplate.IMAGE, logger)
        assertNotNull(parsed)
        assertEquals(ImageValues(Primitive.Image("file:///tmp/img.jpg")), parsed)
    }

    private fun readFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: error("Fixture not found: $path")
        return stream.bufferedReader().use { it.readText() }.trim()
    }
}

/** Простой test double для `LexemeLogger` — фиксирует errors. */
private class FakeLexemeLogger : LexemeLogger {
    val errors: MutableList<String> = mutableListOf()
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level == LogLevel.ERROR) errors += message
    }
}
