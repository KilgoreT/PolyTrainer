package me.apomazkin.polytrainer.di.module.dictionarytab

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.flags.CountryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Test cases for DictionaryTabUseCaseImpl (IS476 nullable contract).
 *
 * === flowCurrentDict — nullable contract ===
 * 1. Boundary: emits null when prefs is null AND list is empty (IS476)
 * 2. Standard: emits current dict matching prefs ID
 * 3. Fallback: emits first dict when prefs ID is null but list non-empty
 *
 * === getCurrentDict — nullable contract ===
 * 4. Boundary: returns null when prefs is null AND list is empty (IS476)
 * 5. Standard: returns dict from prefs ID when present
 * 6. Fallback: returns first dict from list when prefs is null, and writes id back
 */
class DictionaryTabUseCaseImplTest {

    private lateinit var dictionaryApi: CoreDbApi.DictionaryApi
    private lateinit var wordApi: CoreDbApi.WordApi
    private lateinit var termApi: CoreDbApi.TermApi
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var countryProvider: CountryProvider
    private lateinit var useCase: DictionaryTabUseCaseImpl

    private val now = Date()
    private val dictEn =
        DictionaryApiEntity(id = 1L, numericCode = 826, name = "English", addDate = now)

    private lateinit var prefsFlow: MutableStateFlow<Long?>

    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        wordApi = mockk(relaxed = true)
        termApi = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)
        countryProvider = mockk(relaxed = true)

        every { countryProvider.getFlagRes(826) } returns 100

        prefsFlow = MutableStateFlow(null)
        every { prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns prefsFlow

        useCase = DictionaryTabUseCaseImpl(
            dictionaryApi = dictionaryApi,
            wordApi = wordApi,
            termApi = termApi,
            prefsProvider = prefsProvider,
            countryProvider = countryProvider,
        )
    }

    // === flowCurrentDict — nullable contract ===

    @Test
    fun `flowCurrentDict emits null when list is empty`() = runTest {
        // Test case 1: IS476 — пустой список + пустой prefs → null,
        // вместо throw DictionaryNotFoundException.
        prefsFlow.value = null
        coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

        val result: DictUiEntity? = useCase.flowCurrentDict().first()

        assertNull("Expected null emission when no dictionaries exist", result)
    }

    @Test
    fun `flowCurrentDict emits current dict matching prefs ID`() = runTest {
        // Test case 2: prefs has id → возвращается соответствующий словарь.
        prefsFlow.value = 1L
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEn

        val result = useCase.flowCurrentDict().first()

        assertEquals(1L, result?.id)
        assertEquals("English", result?.title)
        assertEquals(100, result?.flagRes)
        assertEquals(826, result?.numericCode)
    }

    @Test
    fun `flowCurrentDict emits first dict when prefs is null but list non-empty`() = runTest {
        // Test case 3: prefs пуст, fallback на firstOrNull() из списка.
        prefsFlow.value = null
        coEvery { dictionaryApi.getDictionaryList() } returns listOf(dictEn)

        val result = useCase.flowCurrentDict().first()

        assertEquals(dictEn.id, result?.id)
        assertEquals("English", result?.title)
    }

    // === getCurrentDict — nullable contract ===

    @Test
    fun `getCurrentDict returns null when no dictionaries exist`() = runTest {
        // Test case 4: IS476 — нет ни prefs, ни словарей → null,
        // вместо throw DictionaryNotFoundException.
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns null
        coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

        val result: DictUiEntity? = useCase.getCurrentDict()

        assertNull("Expected null when no dictionaries exist", result)
    }

    @Test
    fun `getCurrentDict returns dict from prefs ID when present`() = runTest {
        // Test case 5: happy path — prefs указывает на существующий словарь.
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 1L
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEn

        val result = useCase.getCurrentDict()

        assertEquals(1L, result?.id)
        assertEquals("English", result?.title)
        assertEquals(100, result?.flagRes)
    }

    @Test
    fun `getCurrentDict returns first dict from list when prefs is null and writes id back`() =
        runTest {
            // Test case 6: prefs пуст → fallback на firstOrNull() + запись id обратно.
            coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns null
            coEvery { dictionaryApi.getDictionaryList() } returns listOf(dictEn)

            val result = useCase.getCurrentDict()

            assertEquals(dictEn.id, result?.id)
            assertEquals("English", result?.title)
            coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, dictEn.id) }
        }
}
