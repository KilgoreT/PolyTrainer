package me.apomazkin.polytrainer.di.module.dictionarytab

import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.ComponentValueApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.GroupApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WordTextApiEntity
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.flags.CountryProvider
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.di.module.wordstab.PhotoMappingStore
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.textrecognition.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * WordsTabUseCaseImpl: nullable-контракт словаря (IS476), словарь снимка в
 * исходе запроса (AWP-01), форма словаря и леммы «есть» (AWP-06, AWP-07,
 * AWP-09, AWP-18, AWP-24).
 */
class WordsTabUseCaseImplTest {

    private lateinit var dictionaryApi: CoreDbApi.DictionaryApi
    private lateinit var wordApi: CoreDbApi.WordApi
    private lateinit var termApi: CoreDbApi.TermApi
    private lateinit var lexemeApi: CoreDbApi.LexemeApi
    private lateinit var groupApi: CoreDbApi.GroupApi
    private lateinit var wordsBatchApi: CoreDbApi.WordsBatchApi
    private lateinit var wordsApi: CoreNetworkApi.WordsApi
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var countryProvider: CountryProvider
    private lateinit var useCase: WordsTabUseCaseImpl

    private val now = Date()
    private val dictEn =
        DictionaryApiEntity(id = 1L, numericCode = 826, name = "English", addDate = now, learningLanguage = "en", translationLanguage = "ru")

    private lateinit var prefsFlow: MutableStateFlow<Long?>

    @Before
    fun setUp() {
        dictionaryApi = mockk(relaxed = true)
        wordApi = mockk(relaxed = true)
        termApi = mockk(relaxed = true)
        lexemeApi = mockk(relaxed = true)
        groupApi = mockk(relaxed = true)
        wordsBatchApi = mockk(relaxed = true)
        wordsApi = mockk()
        prefsProvider = mockk(relaxed = true)
        countryProvider = mockk(relaxed = true)

        every { countryProvider.getFlagRes(826) } returns 100

        prefsFlow = MutableStateFlow(null)
        every { prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns prefsFlow

        useCase = WordsTabUseCaseImpl(
            dictionaryApi = dictionaryApi,
            wordApi = wordApi,
            termApi = termApi,
            lexemeApi = lexemeApi,
            groupApi = groupApi,
            wordsBatchApi = wordsBatchApi,
            photoMappingStore = PhotoMappingStore(prefsProvider = prefsProvider, logger = mockk(relaxed = true)),
            prefsProvider = prefsProvider,
            countryProvider = countryProvider,
            // IS500: живой провайдер на тех же моках — семантика
            // flowCurrentDict проверяется сквозь него.
            currentDictionaryProvider = CurrentDictionaryProvider(
                dictionaryApi = dictionaryApi,
                prefsProvider = prefsProvider,
            ),
            // IS527: распознавание в этих тестах не трогается.
            textRecognizer = mockk(),
            wordsApi = wordsApi,
            appContext = mockk(),
        )
    }

    // === flowCurrentDict — nullable contract ===

    @Test
    fun `flowCurrentDict emits null when list is empty`() = runTest {
        prefsFlow.value = null
        every { dictionaryApi.flowDictionaryList() } returns flowOf(emptyList())

        val result: DictUiEntity? = useCase.flowCurrentDict().first()

        assertNull("Expected null emission when no dictionaries exist", result)
    }

    @Test
    fun `flowCurrentDict emits current dict matching prefs ID`() = runTest {
        prefsFlow.value = 1L
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEn))

        val result = useCase.flowCurrentDict().first()

        assertEquals(1L, result?.id)
        assertEquals("English", result?.title)
        assertEquals(100, result?.flagRes)
        assertEquals(826, result?.numericCode)
    }

    @Test
    fun `flowCurrentDict emits first dict when prefs is null but list non-empty`() = runTest {
        prefsFlow.value = null
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEn))

        val result = useCase.flowCurrentDict().first()

        assertEquals(dictEn.id, result?.id)
        assertEquals("English", result?.title)
    }

    // === getCurrentDict — nullable contract ===

    @Test
    fun `getCurrentDict returns null when no dictionaries exist`() = runTest {
        coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns null
        coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

        val result: DictUiEntity? = useCase.getCurrentDict()

        assertNull("Expected null when no dictionaries exist", result)
    }

    @Test
    fun `getCurrentDict returns dict from prefs ID when present`() = runTest {
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
            coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns null
            coEvery { dictionaryApi.getDictionaryList() } returns listOf(dictEn)

            val result = useCase.getCurrentDict()

            assertEquals(dictEn.id, result?.id)
            assertEquals("English", result?.title)
            coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, dictEn.id) }
        }

    // === IS532: словарь снимка и форма ===

    private fun type(
        id: Long,
        systemKey: BuiltInComponent? = null,
        name: String? = null,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        core: Boolean = false,
        enabled: Boolean = true,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
        position: Int = 0,
        isMultiple: Boolean = false,
    ) = ComponentTypeApiEntity(
        id = id,
        systemKey = systemKey,
        dictionaryId = 1L,
        name = name,
        template = template,
        position = position,
        isMultiple = isMultiple,
        core = core,
        enabled = enabled,
        dependsOn = dependsOn,
        createdAt = now,
        updatedAt = now,
    )

    private val translation = type(1L, BuiltInComponent.TRANSLATION, core = true, position = 0)
    private val definition = type(2L, name = "Определение", core = true, position = 1)
    private val partOfSpeech = type(3L, BuiltInComponent.PART_OF_SPEECH, template = ComponentTemplate.CHOICE, position = 2)
    private val example = type(4L, BuiltInComponent.EXAMPLE, template = ComponentTemplate.CAPTIONED_TEXT, isMultiple = true, position = 3)
    private val disabled = type(5L, name = "Выключенный", enabled = false)
    private val dependent = type(6L, name = "Разбор", dependsOn = DependencyTarget.Option(31L))

    private fun value(id: Long, type: ComponentTypeApiEntity, data: me.apomazkin.lexeme.TemplateValues, removed: Boolean = false) =
        ComponentValueApiEntity(id = id, lexemeId = 0L, type = type, data = data, createdAt = now, updatedAt = now, removedAt = if (removed) now else null)

    private fun text(value: String) = TextValues(Primitive.Text(value))

    private fun term(wordId: Long, text: String, vararg lexemes: LexemeApiEntity) =
        TermApiEntity(word = WordApiEntity(id = wordId, dictionaryId = 1L, value = text, addDate = now), lexemes = lexemes.toList())

    private fun lexeme(id: Long, vararg values: ComponentValueApiEntity) = LexemeApiEntity(id = id, components = values.toList(), addDate = now)

    @Test
    fun `AWP-01_1 lookup outcome carries the id of the dictionary of the photo`() = runTest {
        prefsFlow.value = 1L
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEn))
        coEvery { wordsApi.lookup(any(), any()) } returns LookupResult.Success("Wiktionary, CC BY-SA 4.0", emptyList())

        val outcome = useCase.lookupWords(listOf(RecognizedWord("run", 0.9f, "en")), runId = 1L)

        assertEquals(PhotoLookupOutcome.Words(dictionaryId = 1L, words = emptyList()), outcome)
    }

    @Test
    fun `AWP-01_2 AWP-24_4 form of a deleted dictionary is dictionary missing`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns null

        assertEquals(PhotoFormOutcome.DictionaryMissing, useCase.loadPhotoForm(1L, listOf("run")))
    }

    @Test
    fun `AWP-24_1 reading failure gives a failed outcome instead of a crash`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEn
        coEvery { lexemeApi.getComponentTypes(1L) } throws IllegalStateException("db closed")

        val outcome = useCase.loadPhotoForm(1L, listOf("run"))

        assertTrue(outcome is PhotoFormOutcome.Failed)
    }

    @Test
    fun `AWP-09_4 AWP-09_7 AWP-06_2 form carries enabled lexeme components only, part of speech options, groups and the remembered mapping`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEn
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(translation, definition, partOfSpeech, example, disabled, dependent)
        coEvery { lexemeApi.getComponentOptions(3L) } returns listOf(
            ComponentOptionApiEntity(id = 31L, componentTypeId = 3L, systemKey = "verb", label = null, position = 1),
            ComponentOptionApiEntity(id = 32L, componentTypeId = 3L, systemKey = "noun", label = null, position = 0, removedAt = now),
        )
        every { groupApi.groupTree(1L) } returns flowOf(listOf(GroupApiEntity(id = 7L, dictionaryId = 1L, parentGroupId = null, name = "Harry Potter 1")))
        coEvery { wordsBatchApi.getWordTexts(1L) } returns emptyList()
        coEvery { prefsProvider.getStringByRawKey("photo_words_mapping_dict_1") } returns "translation=1,gloss=2,examples=4,partOfSpeech=3"

        val outcome = useCase.loadPhotoForm(1L, listOf("run")) as PhotoFormOutcome.Loaded

        assertEquals(listOf(1L, 2L, 3L, 4L), outcome.form.components.map { it.id })
        assertEquals(PhotoTemplate.CAPTIONED_TEXT, outcome.form.components[3].template)
        assertTrue(outcome.form.components[3].isMultiple)
        assertEquals(listOf(31L), outcome.form.partOfSpeechOptions.map { it.id })
        assertEquals("verb", outcome.form.partOfSpeechOptions.single().key)
        assertEquals(listOf("Harry Potter 1"), outcome.form.groups.map { it.name })
        assertEquals("ru", outcome.form.translationLanguage)
        assertEquals(PhotoTarget.Component(2L), outcome.remembered?.gloss)
    }

    @Test
    fun `AWP-18_1 AWP-18_7 AWP-07_2 AWP-07_3 existing lemmas - same text ignoring case, newest first, core values, text keys, groups`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEn
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(translation, definition, partOfSpeech)
        coEvery { lexemeApi.getComponentOptions(3L) } returns emptyList()
        every { groupApi.groupTree(1L) } returns flowOf(emptyList())
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(
            WordTextApiEntity(101L, "bank"),
            WordTextApiEntity(102L, "Bank"),
            WordTextApiEntity(5L, "run"),
        )
        coEvery { termApi.getTermsByIds(listOf(101L, 102L)) } returns listOf(
            term(
                102L, "Bank",
                lexeme(1L, value(1L, translation, text("берег")), value(2L, definition, text("edge of a river"))),
            ),
            term(
                101L, "bank",
                lexeme(2L, value(3L, translation, text("банк")), value(4L, partOfSpeech, ChoiceValues(32L))),
                lexeme(3L, value(5L, translation, text("банкомёт")), value(6L, translation, text("удалённое"), removed = true)),
            ),
        )
        every { groupApi.wordGroups(101L) } returns flowOf(listOf(GroupApiEntity(id = 8L, dictionaryId = 1L, parentGroupId = null, name = "Finance")))
        every { groupApi.wordGroups(102L) } returns flowOf(emptyList())
        coEvery { prefsProvider.getStringByRawKey(any()) } returns null

        val form = (useCase.loadPhotoForm(1L, listOf("bank")) as PhotoFormOutcome.Loaded).form

        assertEquals(listOf(102L, 101L), form.existingLemmas.map { it.wordId })
        val newest = form.existingLemmas[0]
        val older = form.existingLemmas[1]
        assertEquals(listOf("берег"), newest.coreValues)
        assertEquals(setOf(lexemeTextKey("берег"), lexemeTextKey("edge of a river")), newest.lexemeTextKeys)
        assertEquals(2, older.lexemeCount)
        assertEquals(listOf("банк", "банкомёт"), older.coreValues)
        assertEquals(setOf(lexemeTextKey("банк"), lexemeTextKey("банкомёт")), older.lexemeTextKeys)
        assertEquals(listOf("Finance"), older.groupNames)
    }

    @Test
    fun `AWP-06 saved mapping goes to prefs under the dictionary key`() = runTest {
        useCase.savePhotoMapping(1L, me.apomazkin.candidates.PhotoMapping.NONE)

        coVerify { prefsProvider.setStringByRawKey("photo_words_mapping_dict_1", "translation=none,gloss=none,examples=none,partOfSpeech=none") }
    }
}
