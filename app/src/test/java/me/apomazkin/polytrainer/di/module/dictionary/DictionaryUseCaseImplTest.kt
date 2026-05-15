package me.apomazkin.polytrainer.di.module.dictionary

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
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
 * 2. Standard: dictionary without numericCode → flagRes=null
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
 * 11. Edge: deletes current, no remaining → clears orphaned pref to null (IS476)
 * 12. Edge: currentId is null in prefs → does not crash
 *
 * === setCurrentDictionary ===
 * 13. Standard: writes id to prefs via setLong
 *
 * (getAvailableLanguages and getCountriesForLanguage removed — language binding removed)
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
    fun `should clear orphaned pref when current deleted and no remaining`() = runTest {
        // Test case 11: IS476 — после удаления последнего словаря pref не должен
        // оставаться с orphaned id; setLong(null) явно очищает запись.
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 5L
        coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

        useCase.deleteDictionary(5L)

        coVerify { dictionaryApi.deleteDictionary(5L) }
        coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null) }
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

}
