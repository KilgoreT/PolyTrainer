package me.apomazkin.polytrainer.di.module.scancandidates

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
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
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LookupRequestApiEntity
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.polytrainer.di.module.candidates.CandidatesFormLoader
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.textrecognition.RecognizedText
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.textrecognition.TextRecognizer
import me.apomazkin.ui.resource.ResourceManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date
import me.apomazkin.core_network_api.entity.WordApiEntity as NetWordApiEntity

/**
 * Порт экрана лексем-кандидатов (Д51): распознавание и файл снимка (WFP-08,
 * WFP-09, WFP-15, WFP-25), доступ к картинке галереи (Д52, WFP-22), запрос слов
 * с деревом компонентов текущего словаря (WFT-20, WFT-78), форма словаря и леммы
 * «есть» (AWP-07, AWP-09, AWP-18, AWP-24), запись пакета (AWP-20).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanCandidatesUseCaseImplTest {

    private val now = Date(0L)
    private val dictEs = DictionaryApiEntity(
        id = 1L,
        numericCode = 724,
        name = "Español",
        addDate = now,
        learningLanguage = "es",
        translationLanguage = "ru",
    )

    private lateinit var dictionaryApi: CoreDbApi.DictionaryApi
    private lateinit var termApi: CoreDbApi.TermApi
    private lateinit var lexemeApi: CoreDbApi.LexemeApi
    private lateinit var groupApi: CoreDbApi.GroupApi
    private lateinit var wordsBatchApi: CoreDbApi.WordsBatchApi
    private lateinit var prefsProvider: PrefsProvider
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var wordsApi: CoreNetworkApi.WordsApi
    private lateinit var appContext: Context
    private lateinit var resolver: ContentResolver
    private lateinit var prefsFlow: MutableStateFlow<Long?>
    private lateinit var useCase: ScanCandidatesUseCaseImpl

    private val cameraUri = mockk<Uri>()
    private val galleryUri = mockk<Uri>()

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(CAMERA) } returns cameraUri
        every { Uri.parse(GALLERY) } returns galleryUri
        every { cameraUri.authority } returns "$PACKAGE.fileprovider"
        every { galleryUri.authority } returns "media"

        dictionaryApi = mockk(relaxed = true)
        termApi = mockk(relaxed = true)
        lexemeApi = mockk(relaxed = true)
        groupApi = mockk(relaxed = true)
        wordsBatchApi = mockk(relaxed = true)
        prefsProvider = mockk(relaxed = true)
        textRecognizer = mockk()
        wordsApi = mockk()
        resolver = mockk()
        appContext = mockk {
            every { packageName } returns PACKAGE
            every { contentResolver } returns resolver
        }
        every { resolver.delete(any(), null, null) } returns 1
        every { resolver.releasePersistableUriPermission(any(), any()) } just Runs

        prefsFlow = MutableStateFlow(null)
        every { prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns prefsFlow
        prefsFlow.value = 1L
        every { dictionaryApi.flowDictionaryList() } returns flowOf(listOf(dictEs))

        val resources = object : ResourceManager {
            override fun stringByResId(id: Int): String = "res$id"

            override fun stringByResId(id: Int, value: String): String = "res$id"

            override fun stringByArrayId(id: Int): String = "res$id"
        }
        val tree = ComponentTreeLoader(lexemeApi, resources)
        useCase = ScanCandidatesUseCaseImpl(
            dictionaryApi = dictionaryApi,
            wordsBatchApi = wordsBatchApi,
            currentDictionaryProvider = CurrentDictionaryProvider(dictionaryApi, prefsProvider),
            textRecognizer = textRecognizer,
            wordsApi = wordsApi,
            componentTree = tree,
            formLoader = CandidatesFormLoader(groupApi, termApi, wordsBatchApi, tree),
            appContext = appContext,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    private fun word(text: String, confidence: Float = 0.9f, language: String? = "es") =
        RecognizedWord(text = text, confidence = confidence, language = language)

    private fun recognized(words: List<RecognizedWord>) {
        coEvery { textRecognizer.recognize(any()) } returns
            RecognizedText(lines = emptyList(), words = words)
    }

    private fun tokensOf(outcome: RecognitionOutcome): List<LookupToken> =
        (outcome as RecognitionOutcome.Text).tokens

    // ===== Распознавание =====

    @Test
    fun `WFP-25_1 tokens go as recognized - same order, repeats, case, confidence and language`() = runTest {
        val texts = listOf(
            "Las", "nubes", "corrían", "por", "el", "cielo.", "El", "perro", "duerme", "en", "el", "jardín.",
        )
        val words = texts.mapIndexed { index, text -> word(text, confidence = 1f - index / 20f) }
        recognized(words)

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(texts, tokens.map { it.text })
        assertEquals(listOf(4, 10), tokens.indices.filter { tokens[it].text == "el" })
        assertEquals(words.map { it.confidence }, tokens.map { it.confidence })
        assertEquals(words.map { it.language }, tokens.map { it.language })
    }

    @Test
    fun `WFP-25_2 a blank token is dropped, the rest keep their order`() = runTest {
        recognized(listOf(word("Las"), word("  "), word("nubes"), word("")))

        assertEquals(listOf("Las", "nubes"), tokensOf(useCase.recognize(GALLERY)).map { it.text })
    }

    @Test
    fun `WFP-25_3 the same text twice goes twice, each with its own confidence`() = runTest {
        recognized(listOf(word("the", confidence = 0.4f), word("the", confidence = 0.9f)))

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(listOf("the", "the"), tokens.map { it.text })
        assertEquals(listOf(0.4f, 0.9f), tokens.map { it.confidence })
    }

    @Test
    fun `WFP-25_5 the order of recognition is not corrected`() = runTest {
        val texts = listOf("observarlo.", "Nemec", "emocionado", "se", "puso", "a")
        recognized(texts.map { word(it) })

        assertEquals(texts, tokensOf(useCase.recognize(GALLERY)).map { it.text })
    }

    @Test
    fun `WFP-25_6 tokens of spaces only give no words and nothing for the server`() = runTest {
        recognized(listOf(word(" "), word("   ")))

        assertEquals(RecognitionOutcome.Empty, useCase.recognize(GALLERY))
    }

    @Test
    fun `WFP-19_1 a photo without text gives no words`() = runTest {
        recognized(emptyList())

        assertEquals(RecognitionOutcome.Empty, useCase.recognize(GALLERY))
    }

    @Test
    fun `WFP-25_7 blank tokens leave before the cut - 2001 tokens with a blank one give 2000`() = runTest {
        recognized((1..2001).map { word(if (it == 1000) "   " else "w$it") })

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(2000, tokens.size)
        assertEquals("w2001", tokens.last().text)
    }

    @Test
    fun `WFP-15_1 2001 tokens are cut to the first 2000`() = runTest {
        recognized((1..2001).map { word("w$it") })

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(2000, tokens.size)
        assertEquals("w1", tokens.first().text)
        assertEquals("w2000", tokens.last().text)
    }

    @Test
    fun `WFP-15_2 exactly 2000 tokens all go`() = runTest {
        recognized((1..2000).map { word("w$it") })

        assertEquals(2000, tokensOf(useCase.recognize(GALLERY)).size)
    }

    @Test
    fun `WFP-08_1 the camera file is deleted after recognition`() = runTest {
        recognized(listOf(word("hello")))

        useCase.recognize(CAMERA)

        verify(exactly = 1) { resolver.delete(cameraUri, null, null) }
    }

    @Test
    fun `WFP-08_2 the camera file is deleted when recognition failed`() = runTest {
        coEvery { textRecognizer.recognize(any()) } throws IOException("Cannot decode image")

        val failure = runCatching { useCase.recognize(CAMERA) }.exceptionOrNull()

        assertTrue(failure is IOException)
        verify(exactly = 1) { resolver.delete(cameraUri, null, null) }
    }

    @Test
    fun `WFP-08_3 the camera file is deleted when the screen is closed during recognition`() = runTest {
        coEvery { textRecognizer.recognize(any()) } coAnswers { awaitCancellation() }

        val job = launch { useCase.recognize(CAMERA) }
        advanceUntilIdle()
        verify(exactly = 0) { resolver.delete(any(), any(), any()) }
        job.cancel()
        advanceUntilIdle()

        verify(exactly = 1) { resolver.delete(cameraUri, null, null) }
    }

    @Test
    fun `WFP-08 a camera file that cannot be deleted does not spoil the recognized text`() = runTest {
        recognized(listOf(word("hello")))
        every { resolver.delete(any(), null, null) } throws SecurityException("no")

        assertEquals(listOf("hello"), tokensOf(useCase.recognize(CAMERA)).map { it.text })
    }

    @Test
    fun `WFP-09_1 a gallery image is not deleted or changed`() = runTest {
        recognized(listOf(word("hello")))

        useCase.recognize(GALLERY)

        verify(exactly = 0) { resolver.delete(any(), any(), any()) }
    }

    // ===== Доступ к картинке галереи =====

    @Test
    fun `WFP-22_4 access to a gallery image is released`() = runTest {
        useCase.releaseImageAccess(GALLERY)

        verify(exactly = 1) {
            resolver.releasePersistableUriPermission(
                galleryUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    @Test
    fun `WFP-22_4 a camera file has no access to release`() = runTest {
        useCase.releaseImageAccess(CAMERA)

        verify(exactly = 0) { resolver.releasePersistableUriPermission(any(), any()) }
    }

    @Test
    fun `WFP-22_4 release can be repeated and never throws, even when the system has no grant`() = runTest {
        var calls = 0
        every { resolver.releasePersistableUriPermission(galleryUri, any()) } answers {
            calls++
            if (calls > 1) throw SecurityException("No permission grants found")
        }

        useCase.releaseImageAccess(GALLERY)
        useCase.releaseImageAccess(GALLERY)
        useCase.releaseImageAccess(GALLERY)

        assertEquals(3, calls)
    }

    // ===== Запрос слов =====

    private val translation = type(1L, BuiltInComponent.TRANSLATION, core = true)
    private val partOfSpeech = type(
        2L,
        BuiltInComponent.PART_OF_SPEECH,
        template = ComponentTemplate.CHOICE,
        position = 1,
    )
    private val example = type(
        3L,
        BuiltInComponent.EXAMPLE,
        template = ComponentTemplate.CAPTIONED_TEXT,
        position = 2,
        isMultiple = true,
    )
    private val gloss = type(4L, name = "Толкование")
    private val gender = type(
        5L,
        name = "Род",
        template = ComponentTemplate.CHOICE,
        dependsOn = DependencyTarget.Option(21L),
    )

    private fun type(
        id: Long,
        systemKey: BuiltInComponent? = null,
        name: String? = null,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        position: Int = 0,
        isMultiple: Boolean = false,
        core: Boolean = false,
        enabled: Boolean = true,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
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

    private fun option(id: Long, typeId: Long, key: String?, label: String? = null, position: Int = 0) =
        ComponentOptionApiEntity(
            id = id,
            componentTypeId = typeId,
            systemKey = key,
            label = label,
            position = position,
        )

    private fun stubDictionaryComponents(vararg types: ComponentTypeApiEntity) {
        coEvery { lexemeApi.getComponentTypes(1L) } returns types.toList()
        coEvery { lexemeApi.getComponentOptions(2L) } returns listOf(
            option(21L, 2L, "noun", position = 0),
            option(22L, 2L, "verb", position = 1),
        )
        coEvery { lexemeApi.getComponentOptions(5L) } returns listOf(
            option(51L, 5L, null, label = "м.р.", position = 0),
            option(52L, 5L, null, label = "ж.р.", position = 1),
        )
    }

    @Test
    fun `WFT-20_1 WFT-78_1 AWP-01_1 lookup asks with the languages, tokens and component tree of the current dictionary`() = runTest {
        stubDictionaryComponents(translation, partOfSpeech, example, gloss, gender)
        val request = slot<LookupRequestApiEntity>()
        coEvery { wordsApi.lookup(capture(request), any()) } returns
            LookupResult.Success("Wiktionary, CC BY-SA 4.0", emptyList(), emptyList())
        val tokens = listOf(
            LookupToken("corrían", 0.93f, "es"),
            LookupToken("corrían", 0.5f, null),
        )

        useCase.lookupWords(tokens)

        assertEquals("es", request.captured.learningLanguage)
        assertEquals("ru", request.captured.translationLanguage)
        assertEquals(listOf("corrían", "corrían"), request.captured.tokens.map { it.text })
        assertEquals(listOf(0.93f, 0.5f), request.captured.tokens.map { it.confidence })
        assertEquals(listOf("es", null), request.captured.tokens.map { it.language })
        assertEquals(listOf(1L, 2L, 3L, 4L), request.captured.components.map { it.id })
        assertEquals(
            5L,
            request.captured.components[1].options.first { it.id == 21L }.children.single().id,
        )
    }

    @Test
    fun `WFT-78_7 dictionary without enabled components asks without a tree`() = runTest {
        stubDictionaryComponents(translation.copy(enabled = false))
        val request = slot<LookupRequestApiEntity>()
        coEvery { wordsApi.lookup(capture(request), any()) } returns
            LookupResult.Unavailable("offline")

        useCase.lookupWords(listOf(LookupToken("perro", 0.9f, "es")))

        assertTrue(request.captured.components.isEmpty())
    }

    @Test
    fun `AWP-01_1 the answer carries the id of the dictionary of the photo`() = runTest {
        stubDictionaryComponents(translation)
        coEvery { wordsApi.lookup(any(), any()) } returns
            LookupResult.Success("Wiktionary", listOf(wordOf("perro")), emptyList())

        val outcome = useCase.lookupWords(listOf(LookupToken("perro", 0.9f, "es")))

        outcome as LookupOutcome.Words
        assertEquals(1L, outcome.dictionaryId)
        assertEquals(listOf("perro"), outcome.words.map { it.text })
    }

    @Test
    fun `WFP-19_2 an answer without words is no words`() = runTest {
        stubDictionaryComponents(translation)
        coEvery { wordsApi.lookup(any(), any()) } returns
            LookupResult.Success("Wiktionary", emptyList(), emptyList())

        assertEquals(
            LookupOutcome.NoWords,
            useCase.lookupWords(listOf(LookupToken("accorc", 0.4f, null))),
        )
    }

    @Test
    fun `WFT-63_5 WFT-23_1 a denial gives its reason, no answer gives unavailable`() = runTest {
        stubDictionaryComponents(translation)
        val tokens = listOf(LookupToken("perro", 0.9f, "es"))

        coEvery { wordsApi.lookup(any(), any()) } returns LookupResult.Denied(DenyReason.LLM_UNAVAILABLE)
        val denied = useCase.lookupWords(tokens)
        coEvery { wordsApi.lookup(any(), any()) } returns LookupResult.Unavailable("offline")
        val unavailable = useCase.lookupWords(tokens)

        assertEquals(LookupOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE), denied)
        assertEquals(LookupOutcome.Unavailable, unavailable)
    }

    @Test
    fun `WFP-01_1 every request has a run number greater than any before`() = runTest {
        stubDictionaryComponents(translation)
        val runIds = mutableListOf<Long>()
        coEvery { wordsApi.lookup(any(), capture(runIds)) } returns
            LookupResult.Unavailable("offline")
        val tokens = listOf(LookupToken("perro", 0.9f, "es"))

        useCase.lookupWords(tokens)
        useCase.lookupWords(tokens)
        useCase.lookupWords(tokens)

        assertEquals(3, runIds.size)
        assertTrue(runIds[0] < runIds[1] && runIds[1] < runIds[2])
    }

    @Test
    fun `lookup without a current dictionary is a program error`() = runTest {
        every { dictionaryApi.flowDictionaryList() } returns flowOf(emptyList())

        val failure = runCatching {
            useCase.lookupWords(listOf(LookupToken("perro", 0.9f, "es")))
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        coVerify(exactly = 0) { wordsApi.lookup(any(), any()) }
    }

    private fun wordOf(text: String) = NetWordApiEntity(
        text = text,
        status = WordStatus.NOT_FOUND,
        lemmas = emptyList(),
        remarks = emptyList(),
    )

    // ===== Форма словаря =====

    private fun value(
        id: Long,
        type: ComponentTypeApiEntity,
        data: TemplateValues,
        removed: Boolean = false,
    ) = ComponentValueApiEntity(
        id = id,
        lexemeId = 0L,
        type = type,
        data = data,
        createdAt = now,
        updatedAt = now,
        removedAt = if (removed) now else null,
    )

    private fun text(value: String) = TextValues(Primitive.Text(value))

    private fun term(wordId: Long, text: String, vararg lexemes: LexemeApiEntity) = TermApiEntity(
        word = WordApiEntity(id = wordId, dictionaryId = 1L, value = text, addDate = now),
        lexemes = lexemes.toList(),
    )

    private fun lexeme(id: Long, vararg values: ComponentValueApiEntity) =
        LexemeApiEntity(id = id, components = values.toList(), addDate = now)

    @Test
    fun `AWP-24_4 form of a deleted dictionary is dictionary missing`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns null

        assertEquals(FormOutcome.DictionaryMissing, useCase.loadForm(1L, listOf("run")))
    }

    @Test
    fun `AWP-24_1 reading failure gives a failed outcome instead of a crash`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        coEvery { lexemeApi.getComponentTypes(1L) } throws IllegalStateException("db closed")

        val outcome = useCase.loadForm(1L, listOf("run"))

        assertTrue(outcome is FormOutcome.Failed)
    }

    @Test
    fun `AWP-09_4 AWP-17 form carries enabled components of all dependencies in tree order, options of every choice and groups`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(
            translation,
            partOfSpeech,
            example,
            gloss,
            gender,
            type(6L, name = "Выключенный", enabled = false),
        )
        every { groupApi.groupTree(1L) } returns flowOf(
            listOf(GroupApiEntity(id = 7L, dictionaryId = 1L, parentGroupId = null, name = "Harry Potter 1")),
        )
        coEvery { wordsBatchApi.getWordTexts(1L) } returns emptyList()

        val form = (useCase.loadForm(1L, listOf("perro")) as FormOutcome.Loaded).form

        assertEquals(listOf(1L, 2L, 5L, 3L, 4L), form.components.map { it.id })
        val genderComponent = form.components.first { it.id == 5L }
        assertEquals(DependencyTarget.Option(21L), genderComponent.dependsOn)
        assertEquals(ComponentTemplate.CHOICE, genderComponent.template)
        assertEquals("Род", genderComponent.name)
        assertNull(form.components.first { it.id == 1L }.name)
        assertEquals(BuiltInComponent.TRANSLATION, form.components.first { it.id == 1L }.systemKey)
        assertTrue(form.components.first { it.id == 3L }.isMultiple)
        assertEquals(listOf(2L, 5L), form.options.keys.sorted())
        assertEquals(listOf(21L, 22L), form.options.getValue(2L).map { it.id })
        assertEquals("noun", form.options.getValue(2L).first().key)
        assertEquals(listOf("м.р.", "ж.р."), form.options.getValue(5L).map { it.label })
        assertEquals(listOf("Harry Potter 1"), form.groups.map { it.name })
        assertEquals(1L, form.dictionaryId)
        assertEquals("ru", form.translationLanguage)
    }

    @Test
    fun `AWP-18_1 AWP-18_7 AWP-07_2 AWP-07_3 existing lemmas - same text ignoring case, newest first, core values, text keys of cores, groups`() = runTest {
        val definition = type(8L, name = "Определение", core = true, position = 1)
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(translation, definition, partOfSpeech)
        coEvery { lexemeApi.getComponentOptions(any()) } returns emptyList()
        every { groupApi.groupTree(1L) } returns flowOf(emptyList())
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(
            WordTextApiEntity(101L, "bank"),
            WordTextApiEntity(102L, "Bank"),
            WordTextApiEntity(5L, "run"),
        )
        coEvery { termApi.getTermsByIds(listOf(101L, 102L)) } returns listOf(
            term(
                102L,
                "Bank",
                lexeme(1L, value(1L, translation, text("берег")), value(2L, definition, text("edge of a river"))),
            ),
            term(
                101L,
                "bank",
                lexeme(2L, value(3L, translation, text("банк")), value(4L, partOfSpeech, ChoiceValues(32L))),
                lexeme(3L, value(5L, translation, text("банкомёт")), value(6L, translation, text("удалённое"), removed = true)),
            ),
        )
        every { groupApi.wordGroups(101L) } returns flowOf(
            listOf(GroupApiEntity(id = 8L, dictionaryId = 1L, parentGroupId = null, name = "Finance")),
        )
        every { groupApi.wordGroups(102L) } returns flowOf(emptyList())

        val form = (useCase.loadForm(1L, listOf("bank")) as FormOutcome.Loaded).form

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
    fun `AWP-07_11 Д42 text keys come from cores only - a gloss that is not a core is not compared`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation, gloss)
        every { groupApi.groupTree(1L) } returns flowOf(emptyList())
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(WordTextApiEntity(10L, "estudiante"))
        coEvery { termApi.getTermsByIds(listOf(10L)) } returns listOf(
            term(
                10L,
                "estudiante",
                lexeme(
                    1L,
                    value(1L, translation, text("студент, учащийся")),
                    value(2L, gloss, text("student (a person who studies…)")),
                ),
            ),
        )
        every { groupApi.wordGroups(10L) } returns flowOf(emptyList())

        val existing = (useCase.loadForm(1L, listOf("estudiante")) as FormOutcome.Loaded)
            .form
            .existingLemmas
            .single()

        assertEquals(setOf(lexemeTextKey("студент, учащийся")), existing.lexemeTextKeys)
        assertEquals(listOf("студент, учащийся"), existing.coreValues)
    }

    @Test
    fun `AWP-07_5 the value of a disabled core gives no mark, the value of an enabled core does`() = runTest {
        // Словарь «только определения»: «Перевод» выключен, но старое значение в нём осталось.
        val disabledTranslation = translation.copy(enabled = false)
        val definition = type(8L, name = "Определение", core = true, position = 1)
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(disabledTranslation, definition)
        every { groupApi.groupTree(1L) } returns flowOf(emptyList())
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(WordTextApiEntity(10L, "run"))
        coEvery { termApi.getTermsByIds(listOf(10L)) } returns listOf(
            term(
                10L,
                "run",
                lexeme(
                    1L,
                    value(1L, disabledTranslation, text("бежать")),
                    value(2L, definition, text("To move swiftly.")),
                ),
            ),
        )
        every { groupApi.wordGroups(10L) } returns flowOf(emptyList())

        val existing = (useCase.loadForm(1L, listOf("run")) as FormOutcome.Loaded)
            .form
            .existingLemmas
            .single()

        assertEquals(setOf(lexemeTextKey("To move swiftly.")), existing.lexemeTextKeys)
        assertEquals(listOf("To move swiftly."), existing.coreValues)
    }

    @Test
    fun `AWP-18_7 core values come from the first enabled text core - built-in before own`() = runTest {
        val ownCore = type(9L, name = "Определение", core = true, position = 0)
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(ownCore, translation)
        coEvery { lexemeApi.getComponentOptions(any()) } returns emptyList()
        every { groupApi.groupTree(1L) } returns flowOf(emptyList())
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(WordTextApiEntity(10L, "perro"))
        coEvery { termApi.getTermsByIds(listOf(10L)) } returns listOf(
            term(
                10L,
                "perro",
                lexeme(1L, value(1L, translation, text("собака")), value(2L, ownCore, text("domestic animal"))),
            ),
        )
        every { groupApi.wordGroups(10L) } returns flowOf(emptyList())

        val existing = (useCase.loadForm(1L, listOf("perro")) as FormOutcome.Loaded)
            .form
            .existingLemmas
            .single()

        assertEquals(listOf("собака"), existing.coreValues)
    }

    // ===== Запись =====

    @Test
    fun `AWP-20_1 the batch is written as it is by one call`() = runTest {
        val batch = WordsBatch(dictionaryId = 1L, words = emptyList())
        val added = AddWordsBatchOutcome.Added(wordIds = listOf(7L), lexemeIds = listOf(1L, 2L))
        coEvery { wordsBatchApi.addWordsBatch(batch) } returns added

        assertEquals(added, useCase.addWords(batch))
    }

    @Test
    fun `AWP-21_1 a rejection of the check comes as an outcome`() = runTest {
        val batch = WordsBatch(dictionaryId = 1L, words = emptyList())
        coEvery { wordsBatchApi.addWordsBatch(batch) } returns AddWordsBatchOutcome.Rejected.DictionaryMissing

        assertEquals(AddWordsBatchOutcome.Rejected.DictionaryMissing, useCase.addWords(batch))
    }

    @Test
    fun `AWP-21 a failure of the transaction flies to the effect`() = runTest {
        val batch = WordsBatch(dictionaryId = 1L, words = emptyList())
        coEvery { wordsBatchApi.addWordsBatch(batch) } throws IllegalStateException("rolled back")

        val failure = runCatching { useCase.addWords(batch) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }

    private companion object {
        const val PACKAGE = "co.lexeme.app.dev"
        const val CAMERA = "content://co.lexeme.app.dev.fileprovider/photos/photo_1.jpg"
        const val GALLERY = "content://media/picker/0/com.android.providers.media.photopicker/media/1000012"
    }
}
