package me.apomazkin.polytrainer.di.module.widget

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
 * Test cases for DictionaryAppBarUseCaseImpl.flowCurrentDict():
 *
 * === flowCurrentDict — reactivity ===
 * 1. Standard: emits current dict matching prefs ID
 * 2. Standard: emits updated dict when dictionary data changes (flag change) without ID change
 * 3. Standard: emits new dict when prefs ID changes
 * 4. Fallback: emits first dict from list when prefs ID not found in list
 * 5. Boundary: emits null when list is empty (IS476 — no throw)
 * 6. Standard: maps numericCode to flagRes via CountryProvider
 * 7. Standard: flagRes=0 when numericCode is null
 *
 * === flowAvailableDict ===
 * 8. Standard: emits mapped list from Room Flow
 *
 * === changeDict ===
 * 9. Standard: writes id to prefs
 */
class DictionaryAppBarUseCaseImplTest {

    private lateinit var dictionaryApi: CoreDbApi.DictionaryApi
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var countryProvider: CountryProvider
    private lateinit var useCase: DictionaryAppBarUseCaseImpl

    private val now = Date()
    private val dictEn =
        DictionaryApiEntity(id = 1L, numericCode = 826, name = "English", addDate = now)
    private val dictEs =
        DictionaryApiEntity(id = 2L, numericCode = 724, name = "Spanish", addDate = now)
    private val dictNoFlag =
        DictionaryApiEntity(id = 3L, numericCode = null, name = "Biology", addDate = now)

    private lateinit var prefsFlow: MutableStateFlow<Long?>
    private lateinit var dictListFlow: MutableStateFlow<List<DictionaryApiEntity>>

    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)
        countryProvider = mockk(relaxed = true)

        every { countryProvider.getFlagRes(826) } returns 100
        every { countryProvider.getFlagRes(724) } returns 200

        prefsFlow = MutableStateFlow(1L)
        dictListFlow = MutableStateFlow(listOf(dictEn, dictEs))

        every { prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns prefsFlow
        every { dictionaryApi.flowDictionaryList() } returns dictListFlow

        useCase = DictionaryAppBarUseCaseImpl(dictionaryApi, prefsProvider, countryProvider)
    }

    // === flowCurrentDict — reactivity ===

    @Test
    fun `flowCurrentDict emits current dict matching prefs ID`() = runTest {
        val result = useCase.flowCurrentDict().first()

        assertEquals(1L, result?.id)
        assertEquals("English", result?.title)
        assertEquals(100, result?.flagRes)
        assertEquals(826, result?.numericCode)
    }

    @Test
    fun `flowCurrentDict emits updated dict when dictionary data changes without ID change`() =
        runTest {
            val results = mutableListOf<DictUiEntity?>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                useCase.flowCurrentDict().collect { results.add(it) }
            }

            // Initial emission
            assertEquals(1, results.size)
            assertEquals(100, results[0]?.flagRes)

            // Simulate flag change: English dict gets new numericCode (flag changed)
            val updatedEn = dictEn.copy(numericCode = 276) // Germany
            every { countryProvider.getFlagRes(276) } returns 300
            dictListFlow.value = listOf(updatedEn, dictEs)

            // Should emit again with updated flag
            assertEquals(2, results.size)
            assertEquals(300, results[1]?.flagRes)
            assertEquals(276, results[1]?.numericCode)

            job.cancel()
        }

    @Test
    fun `flowCurrentDict emits new dict when prefs ID changes`() = runTest {
        val results = mutableListOf<DictUiEntity?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase.flowCurrentDict().collect { results.add(it) }
        }

        // Initial
        assertEquals(1, results.size)
        assertEquals(1L, results[0]?.id)

        // Switch to Spanish
        prefsFlow.value = 2L

        assertEquals(2, results.size)
        assertEquals(2L, results[1]?.id)
        assertEquals("Spanish", results[1]?.title)
        assertEquals(200, results[1]?.flagRes)

        job.cancel()
    }

    @Test
    fun `flowCurrentDict falls back to first dict when prefs ID not found`() = runTest {
        prefsFlow.value = 999L

        val result = useCase.flowCurrentDict().first()

        assertEquals(dictEn.id, result?.id)
        assertEquals("English", result?.title)
    }

    @Test
    fun `flowCurrentDict emits null when list is empty`() = runTest {
        // IS476: пустой список — валидное доменное состояние, эмит null а не throw.
        dictListFlow.value = emptyList()
        prefsFlow.value = 1L

        val result: DictUiEntity? = useCase.flowCurrentDict().first()

        assertNull("Expected null emission for empty dictionary list", result)
    }

    @Test
    fun `flowCurrentDict maps numericCode to flagRes via CountryProvider`() = runTest {
        prefsFlow.value = 2L

        val result = useCase.flowCurrentDict().first()

        assertEquals(200, result?.flagRes)
        assertEquals(724, result?.numericCode)
    }

    @Test
    fun `flowCurrentDict returns flagRes 0 when numericCode is null`() = runTest {
        dictListFlow.value = listOf(dictNoFlag)
        prefsFlow.value = 3L

        val result = useCase.flowCurrentDict().first()

        assertEquals(0, result?.flagRes)
        assertEquals(0, result?.numericCode)
    }

    // === flowAvailableDict ===

    @Test
    fun `flowAvailableDict emits mapped list from Room Flow`() = runTest {
        val result = useCase.flowAvailableDict().first()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(100, result[0].flagRes)
        assertEquals(2L, result[1].id)
        assertEquals(200, result[1].flagRes)
    }

    // === changeDict ===

    @Test
    fun `changeDict writes id to prefs`() = runTest {
        coEvery { prefsProvider.setLong(any(), any()) } returns Unit

        useCase.changeDict(42L)

        io.mockk.coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, 42L) }
    }

    // === null prefs ID ===

    @Test
    fun `flowCurrentDict falls back to first dict when prefs ID is null`() = runTest {
        prefsFlow.value = null

        val result = useCase.flowCurrentDict().first()

        assertEquals(dictEn.id, result?.id)
        assertEquals("English", result?.title)
    }
}
