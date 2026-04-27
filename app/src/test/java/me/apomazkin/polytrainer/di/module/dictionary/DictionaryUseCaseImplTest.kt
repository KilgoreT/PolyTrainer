package me.apomazkin.polytrainer.di.module.dictionary

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.flags.CountryInfo
import me.apomazkin.flags.CountryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Test cases for DictionaryUseCaseImpl:
 *
 * === getDictionaryList ===
 * 1. Standard: returns list mapped with flags and language names
 * 2. Standard: dictionary without numericCode → flagRes=null, languageName=null
 * 3. Boundary: empty list → returns empty
 * 4. Standard: multiple dictionaries mapped correctly
 *
 * === addDictionary ===
 * 5. Standard: delegates to API, returns id
 * 6. Standard: does NOT set current dictionary (separate operation)
 * 7. Standard: passes numericCode=null when no flag
 *
 * === updateDictionary ===
 * 8. Standard: delegates to API with correct params
 *
 * === deleteDictionary ===
 * 9. Standard: deletes and does not switch current if different dict
 * 10. Standard: deletes current → switches to first remaining
 * 11. Edge: deletes current, no remaining → does not crash
 * 12. Edge: currentId is null in prefs → does not crash
 *
 * === setCurrentDictionary ===
 * 13. Standard: writes id to prefs via setLong
 *
 * === getAvailableLanguages ===
 * 14. Standard: returns non-empty sorted list
 * 15. Standard: displayName capitalized
 * 16. Standard: filtered — no blank displayNames
 *
 * === getCountriesForLanguage ===
 * 17. Standard: filters countries by language name
 * 18. Standard: returns flags for matching countries
 * 19. Edge: no countries speak this language → empty list
 * 20. Standard: case-insensitive matching
 */
class DictionaryUseCaseImplTest {

    private lateinit var dictionaryApi: CoreDbApi.DictionaryApi
    private lateinit var countryProvider: CountryProvider
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var useCase: DictionaryUseCaseImpl

    private val now = Date()

    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        countryProvider = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)
        useCase = DictionaryUseCaseImpl(dictionaryApi, countryProvider, prefsProvider)
    }

    // === getDictionaryList ===

    @Test
    fun `should return list with flags and language names`() = runTest {
        // Test case 1
        coEvery { dictionaryApi.getDictionaryList() } returns listOf(
            DictionaryApiEntity(id = 1, numericCode = 826, name = "English", addDate = now)
        )
        coEvery { countryProvider.getFlagRes(826) } returns 100
        every { countryProvider.getLanguagesForCountry(826) } returns listOf("English")

        val result = useCase.getDictionaryList()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("English", result[0].name)
        assertEquals(100, result[0].flagRes)
        assertEquals("English", result[0].languageName)
    }

    @Test
    fun `should return null flag and language when no numericCode`() = runTest {
        // Test case 2
        coEvery { dictionaryApi.getDictionaryList() } returns listOf(
            DictionaryApiEntity(id = 2, numericCode = null, name = "Biology", addDate = now)
        )

        val result = useCase.getDictionaryList()

        assertEquals(1, result.size)
        assertNull("flagRes should be null", result[0].flagRes)
        assertNull("languageName should be null", result[0].languageName)
    }

    @Test
    fun `should return empty list when no dictionaries`() = runTest {
        // Test case 3
        coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

        val result = useCase.getDictionaryList()

        assertTrue("should be empty", result.isEmpty())
    }

    @Test
    fun `should map multiple dictionaries correctly`() = runTest {
        // Test case 4
        coEvery { dictionaryApi.getDictionaryList() } returns listOf(
            DictionaryApiEntity(id = 1, numericCode = 826, name = "English", addDate = now),
            DictionaryApiEntity(id = 2, numericCode = null, name = "Bio", addDate = now),
            DictionaryApiEntity(id = 3, numericCode = 724, name = "Spanish", addDate = now),
        )
        coEvery { countryProvider.getFlagRes(826) } returns 100
        coEvery { countryProvider.getFlagRes(724) } returns 200
        every { countryProvider.getLanguagesForCountry(826) } returns listOf("English")
        every { countryProvider.getLanguagesForCountry(724) } returns listOf("Spanish")

        val result = useCase.getDictionaryList()

        assertEquals(3, result.size)
        assertEquals(100, result[0].flagRes)
        assertNull(result[1].flagRes)
        assertEquals(200, result[2].flagRes)
    }

    // === addDictionary ===

    @Test
    fun `should delegate to API and return id`() = runTest {
        // Test case 5
        coEvery { dictionaryApi.addDictionary("English", 826) } returns 42L

        val result = useCase.addDictionary("English", 826)

        assertEquals(42L, result)
    }

    @Test
    fun `should set current dictionary on add`() = runTest {
        // Test case 6
        coEvery { dictionaryApi.addDictionary("English", 826) } returns 42L

        useCase.addDictionary("English", 826)

        coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, 42L) }
    }

    @Test
    fun `should pass null numericCode when no flag`() = runTest {
        // Test case 7
        coEvery { dictionaryApi.addDictionary("Bio", null) } returns 10L

        val result = useCase.addDictionary("Bio", null)

        assertEquals(10L, result)
        coVerify { dictionaryApi.addDictionary("Bio", null) }
    }

    // === updateDictionary ===

    @Test
    fun `should delegate update to API`() = runTest {
        // Test case 8
        useCase.updateDictionary(5L, "Updated", 724)

        coVerify { dictionaryApi.updateDictionary(5L, "Updated", 724) }
    }

    // === deleteDictionary ===

    @Test
    fun `should delete and not switch when different dict is current`() = runTest {
        // Test case 9
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 99L

        useCase.deleteDictionary(5L)

        coVerify { dictionaryApi.deleteDictionary(5L) }
        coVerify(exactly = 0) { prefsProvider.setLong(any(), any()) }
    }

    @Test
    fun `should switch to first remaining when current deleted`() = runTest {
        // Test case 10
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 5L
        coEvery { dictionaryApi.getDictionaryList() } returns listOf(
            DictionaryApiEntity(id = 3, numericCode = null, name = "Remaining", addDate = now)
        )

        useCase.deleteDictionary(5L)

        coVerify { dictionaryApi.deleteDictionary(5L) }
        coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, 3L) }
    }

    @Test
    fun `should not crash when current deleted and no remaining`() = runTest {
        // Test case 11
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 5L
        coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

        useCase.deleteDictionary(5L)

        coVerify { dictionaryApi.deleteDictionary(5L) }
        coVerify(exactly = 0) { prefsProvider.setLong(any(), any()) }
    }

    @Test
    fun `should not crash when currentId is null`() = runTest {
        // Test case 12
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns null

        useCase.deleteDictionary(5L)

        coVerify { dictionaryApi.deleteDictionary(5L) }
        coVerify(exactly = 0) { prefsProvider.setLong(any(), any()) }
    }

    // === setCurrentDictionary ===

    @Test
    fun `should write id to prefs`() = runTest {
        // Test case 13
        useCase.setCurrentDictionary(42L)

        coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, 42L) }
    }

    // === getAvailableLanguages ===

    @Test
    fun `should return non-empty sorted list of languages`() {
        // Test case 14
        val result = useCase.getAvailableLanguages()

        assertTrue("should not be empty", result.isNotEmpty())
        val sorted = result.sortedBy { it.displayName }
        assertEquals("should be sorted", sorted, result)
    }

    @Test
    fun `should have capitalized displayNames`() {
        // Test case 15
        val result = useCase.getAvailableLanguages()

        result.forEach { item ->
            assertTrue(
                "${item.displayName} should start with uppercase",
                item.displayName.first().isUpperCase()
            )
        }
    }

    @Test
    fun `should not have blank displayNames`() {
        // Test case 16
        val result = useCase.getAvailableLanguages()

        result.forEach { item ->
            assertTrue(
                "displayName should not be blank for ${item.code}",
                item.displayName.isNotBlank()
            )
        }
    }

    // === getCountriesForLanguage ===

    @Test
    fun `should filter countries by language`() = runTest {
        // Test case 17
        every { countryProvider.getAllCountries() } returns listOf(
            CountryInfo(724, "Spain"),
            CountryInfo(276, "Germany"),
            CountryInfo(484, "Mexico"),
        )
        every { countryProvider.getLanguagesForCountry(724) } returns listOf("Spanish", "Catalan")
        every { countryProvider.getLanguagesForCountry(276) } returns listOf("German")
        every { countryProvider.getLanguagesForCountry(484) } returns listOf("Spanish")
        coEvery { countryProvider.getFlagRes(724) } returns 100
        coEvery { countryProvider.getFlagRes(484) } returns 101

        val result = useCase.getCountriesForLanguage("es")

        assertEquals("should have 2 Spanish countries", 2, result.size)
        assertEquals("Spain", result[0].countryName)
        assertEquals("Mexico", result[1].countryName)
    }

    @Test
    fun `should return flags for matching countries`() = runTest {
        // Test case 18
        every { countryProvider.getAllCountries() } returns listOf(
            CountryInfo(392, "Japan"),
        )
        every { countryProvider.getLanguagesForCountry(392) } returns listOf("Japanese")
        coEvery { countryProvider.getFlagRes(392) } returns 200

        val result = useCase.getCountriesForLanguage("ja")

        assertEquals(1, result.size)
        assertEquals(200, result[0].flagRes)
        assertEquals(392, result[0].numericCode)
    }

    @Test
    fun `should return empty when no countries speak language`() = runTest {
        // Test case 19
        every { countryProvider.getAllCountries() } returns listOf(
            CountryInfo(276, "Germany"),
        )
        every { countryProvider.getLanguagesForCountry(276) } returns listOf("German")

        val result = useCase.getCountriesForLanguage("xx")

        assertTrue("should be empty", result.isEmpty())
    }

    @Test
    fun `should match case-insensitively`() = runTest {
        // Test case 20
        every { countryProvider.getAllCountries() } returns listOf(
            CountryInfo(724, "Spain"),
        )
        every { countryProvider.getLanguagesForCountry(724) } returns listOf("SPANISH")
        coEvery { countryProvider.getFlagRes(724) } returns 100

        val result = useCase.getCountriesForLanguage("es")

        assertEquals("should find Spain despite case", 1, result.size)
    }
}
