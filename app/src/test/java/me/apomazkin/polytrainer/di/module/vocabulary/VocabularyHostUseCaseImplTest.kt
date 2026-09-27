package me.apomazkin.polytrainer.di.module.vocabulary

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Test cases for VocabularyHostUseCaseImpl (IS493 Э2, D9.1; IS500 —
 * делегация в CurrentDictionaryProvider, провайдер здесь живой):
 * 1. Boundary: null когда prefs пуст И список пуст (IS476 «словарей нет»)
 * 2. Standard: id из prefs, когда словарь существует
 * 3. Fallback: первый словарь списка, когда prefs пуст
 * 4. Fallback: первый словарь списка, когда prefs-id протух (словарь удалён)
 */
class VocabularyHostUseCaseImplTest {

    private lateinit var dictionaryApi: CoreDbApi.DictionaryApi
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var useCase: VocabularyHostUseCaseImpl

    private val now = Date()
    private val dictEn =
        DictionaryApiEntity(id = 1L, numericCode = 826, name = "English", addDate = now)
    private val dictEs =
        DictionaryApiEntity(id = 2L, numericCode = 724, name = "Spanish", addDate = now)

    private lateinit var prefsFlow: MutableStateFlow<Long?>

    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)

        prefsFlow = MutableStateFlow(null)
        every { prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns prefsFlow

        useCase = VocabularyHostUseCaseImpl(
            currentDictionaryProvider = CurrentDictionaryProvider(
                dictionaryApi = dictionaryApi,
                prefsProvider = prefsProvider,
            ),
        )
    }

    @Test
    fun `emits null when prefs empty and no dictionaries`() = runTest {
        prefsFlow.value = null
        every { dictionaryApi.flowDictionaryList() } returns flowOf(emptyList())

        val result: Long? = useCase.flowCurrentDictId().first()

        assertNull("Expected null when no dictionaries exist", result)
    }

    @Test
    fun `emits id from prefs when dictionary exists`() = runTest {
        prefsFlow.value = 1L
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEs, dictEn))

        val result = useCase.flowCurrentDictId().first()

        assertEquals(1L, result)
    }

    @Test
    fun `emits first dictionary id when prefs empty but list non-empty`() = runTest {
        prefsFlow.value = null
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEs, dictEn))

        val result = useCase.flowCurrentDictId().first()

        assertEquals(2L, result)
    }

    @Test
    fun `emits first dictionary id when prefs id is stale`() = runTest {
        prefsFlow.value = 99L
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEn))

        val result = useCase.flowCurrentDictId().first()

        assertEquals(1L, result)
    }
}
