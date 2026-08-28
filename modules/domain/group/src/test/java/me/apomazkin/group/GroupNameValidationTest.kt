package me.apomazkin.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * IS493 Э3 (D12.1/D12.2, stage3_plan Фаза 1):
 *
 * === normalize ===
 * 1. trim обрезает пробелы
 * 2. NFC: разложенное «й» (U+0438+U+0306) → композитное U+0439;
 *    входы различны посимвольно, после normalize равны (T-4)
 *
 * === validateGroupName ===
 * 3. пустое/пробельное имя → Empty
 * 4. валидное имя → Valid(normalizedName) — нормализация ИЗ валидатора
 * 5. дубликат живого сиблинга case-insensitive («Дом» vs «дом») → Duplicate
 * 6. сиблинги БЕЗ self (rename-контракт): своё имя валидно (self-exclusion
 *    обеспечивает вызывающий — D-1/T-1)
 * 7. резерв из set («Все»/«All», любой регистр) → Reserved
 * 8. дубликат через нормализацию: " Дом " при живой «Дом» → Duplicate
 */
class GroupNameValidationTest {

    private val locale = Locale.forLanguageTag("ru-RU")
    private val reserved = setOf("Все", "All")

    // === normalize ===

    @Test
    fun `normalize trims whitespace`() {
        assertEquals("Дом", normalizeGroupName("  Дом  "))
    }

    @Test
    fun `normalize composes decomposed cyrillic short i via NFC`() {
        // «зайка» с разложенным й: и (U+0438) + combining breve (U+0306).
        val decomposed = "зайка"
        val composed = "зайка"
        assertNotEquals("inputs must differ char-wise", composed, decomposed)

        assertEquals("decomposed must be 6 code units", 6, decomposed.length)
        assertEquals("composed must be 5 code units", 5, composed.length)

        val normalized = normalizeGroupName(decomposed)

        assertEquals(composed, normalized)
    }

    // === validateGroupName ===

    @Test
    fun `blank name - Empty`() {
        assertEquals(
            NameCheck.Empty,
            validateGroupName("   ", emptyList(), reserved, locale),
        )
    }

    @Test
    fun `valid name - Valid with normalized form`() {
        val result = validateGroupName("  Дом  ", emptyList(), reserved, locale)

        assertEquals(NameCheck.Valid(normalizedName = "Дом"), result)
    }

    @Test
    fun `duplicate against living sibling case-insensitive - Duplicate`() {
        val result = validateGroupName("дом", listOf("Дом", "Быт"), reserved, locale)

        assertEquals(NameCheck.Duplicate, result)
    }

    @Test
    fun `siblings exclude self - own name is valid (rename contract)`() {
        // Вызывающий (rename-транзакция) передаёт сиблингов БЕЗ переименуемой
        // группы: смена регистра собственного имени легальна.
        val result = validateGroupName("ДОМ", listOf("Быт"), reserved, locale)

        assertTrue(result is NameCheck.Valid)
    }

    @Test
    fun `reserved name any case - Reserved`() {
        assertEquals(
            NameCheck.Reserved,
            validateGroupName("все", emptyList(), reserved, locale),
        )
        assertEquals(
            NameCheck.Reserved,
            validateGroupName(" ALL ", emptyList(), reserved, locale),
        )
    }

    @Test
    fun `duplicate via normalization - trimmed input collides`() {
        val result = validateGroupName("  Дом ", listOf("Дом"), reserved, locale)

        assertEquals(NameCheck.Duplicate, result)
    }
}
