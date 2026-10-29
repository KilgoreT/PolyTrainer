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
import me.apomazkin.core_network_api.entity.CandidateLexemeApiEntity
import me.apomazkin.core_network_api.entity.LemmaApiEntity
import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_api.entity.LexemesRequestApiEntity
import me.apomazkin.core_network_api.entity.LexemesResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LemmaContext
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.SavedLexeme
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
 * Порт экрана снимка (Д51): распознавание и файл снимка (TFP-08, TFP-09, TFP-15,
 * TFP-25), доступ к картинке галереи (Д52, TFP-22), запрос лемм текста с языками
 * текущего словаря (TFP-28, TFP-01, LFT-02, AWP-01), значения слова по лемме с
 * деревом словаря снимка (LFL-103, AWP-01, AWP-43.7), форма словаря и леммы
 * с датой и сохранёнными лексемами (AWP-07, AWP-09, AWP-18, AWP-24, AWP-46),
 * запись пакета (AWP-20).
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
            currentDictionaryProvider = CurrentDictionaryProvider(dictionaryApi, prefsProvider),
            textRecognizer = textRecognizer,
            wordsApi = wordsApi,
            wordsBatchApi = wordsBatchApi,
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
    fun `TFP-25_1 tokens go as recognized - same order, repeats, case, confidence and language`() = runTest {
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
    fun `TFP-25_2 a blank token is dropped, the rest keep their order`() = runTest {
        recognized(listOf(word("Las"), word("  "), word("nubes"), word("")))

        assertEquals(listOf("Las", "nubes"), tokensOf(useCase.recognize(GALLERY)).map { it.text })
    }

    @Test
    fun `TFP-25_3 the same text twice goes twice, each with its own confidence`() = runTest {
        recognized(listOf(word("the", confidence = 0.4f), word("the", confidence = 0.9f)))

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(listOf("the", "the"), tokens.map { it.text })
        assertEquals(listOf(0.4f, 0.9f), tokens.map { it.confidence })
    }

    @Test
    fun `TFP-25_5 the order of recognition is not corrected`() = runTest {
        val texts = listOf("observarlo.", "Nemec", "emocionado", "se", "puso", "a")
        recognized(texts.map { word(it) })

        assertEquals(texts, tokensOf(useCase.recognize(GALLERY)).map { it.text })
    }

    @Test
    fun `TFP-25_6 tokens of spaces only give no words and nothing for the server`() = runTest {
        recognized(listOf(word(" "), word("   ")))

        assertEquals(RecognitionOutcome.Empty, useCase.recognize(GALLERY))
    }

    @Test
    fun `TFP-19_1 a photo without text gives no words`() = runTest {
        recognized(emptyList())

        assertEquals(RecognitionOutcome.Empty, useCase.recognize(GALLERY))
    }

    @Test
    fun `TFP-25_7 blank tokens leave before the cut - 2001 tokens with a blank one give 2000`() = runTest {
        recognized((1..2001).map { word(if (it == 1000) "   " else "w$it") })

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(2000, tokens.size)
        assertEquals("w2001", tokens.last().text)
    }

    @Test
    fun `TFP-15_1 2001 tokens are cut to the first 2000`() = runTest {
        recognized((1..2001).map { word("w$it") })

        val tokens = tokensOf(useCase.recognize(GALLERY))

        assertEquals(2000, tokens.size)
        assertEquals("w1", tokens.first().text)
        assertEquals("w2000", tokens.last().text)
    }

    @Test
    fun `TFP-15_2 exactly 2000 tokens all go`() = runTest {
        recognized((1..2000).map { word("w$it") })

        assertEquals(2000, tokensOf(useCase.recognize(GALLERY)).size)
    }

    @Test
    fun `TFP-08_1 the camera file is deleted after recognition`() = runTest {
        recognized(listOf(word("hello")))

        useCase.recognize(CAMERA)

        verify(exactly = 1) { resolver.delete(cameraUri, null, null) }
    }

    @Test
    fun `TFP-08_2 the camera file is deleted when recognition failed`() = runTest {
        coEvery { textRecognizer.recognize(any()) } throws IOException("Cannot decode image")

        val failure = runCatching { useCase.recognize(CAMERA) }.exceptionOrNull()

        assertTrue(failure is IOException)
        verify(exactly = 1) { resolver.delete(cameraUri, null, null) }
    }

    @Test
    fun `TFP-08_3 the camera file is deleted when the screen is closed during recognition`() = runTest {
        coEvery { textRecognizer.recognize(any()) } coAnswers { awaitCancellation() }

        val job = launch { useCase.recognize(CAMERA) }
        advanceUntilIdle()
        verify(exactly = 0) { resolver.delete(any(), any(), any()) }
        job.cancel()
        advanceUntilIdle()

        verify(exactly = 1) { resolver.delete(cameraUri, null, null) }
    }

    @Test
    fun `TFP-08 a camera file that cannot be deleted does not spoil the recognized text`() = runTest {
        recognized(listOf(word("hello")))
        every { resolver.delete(any(), null, null) } throws SecurityException("no")

        assertEquals(listOf("hello"), tokensOf(useCase.recognize(CAMERA)).map { it.text })
    }

    @Test
    fun `TFP-09_1 a gallery image is not deleted or changed`() = runTest {
        recognized(listOf(word("hello")))

        useCase.recognize(GALLERY)

        verify(exactly = 0) { resolver.delete(any(), any(), any()) }
    }

    // ===== Доступ к картинке галереи =====

    @Test
    fun `TFP-22_4 access to a gallery image is released`() = runTest {
        useCase.releaseImageAccess(GALLERY)

        verify(exactly = 1) {
            resolver.releasePersistableUriPermission(
                galleryUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    @Test
    fun `TFP-22_4 a camera file has no access to release`() = runTest {
        useCase.releaseImageAccess(CAMERA)

        verify(exactly = 0) { resolver.releasePersistableUriPermission(any(), any()) }
    }

    @Test
    fun `TFP-22_4 release can be repeated and never throws, even when the system has no grant`() = runTest {
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

    // ===== Компоненты словаря для формы =====

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

    // ===== Запрос лемм текста (IS537) =====

    private val lemmasOk = LemmasResult.Success(
        text = "Las nubes corrían por el cielo.",
        translation = "Облака бежали по небу.",
        lexical = listOf("nube", "correr", "cielo"),
        proper = emptyList(),
        function = listOf("el", "por"),
        unparsed = emptyList(),
    )

    @Test
    fun `TFP-28_1 LFT-02_1 lemmas asks exactly once with the languages of the current dictionary and the tokens as they came`() = runTest {
        val request = slot<LemmasRequestApiEntity>()
        coEvery { wordsApi.lemmasFromTokens(capture(request), any()) } returns lemmasOk
        val texts = listOf("Las", "nubes", "corrían", "por", "el", "cielo.", "El", "perro", "duerme", "en", "el", "jardín.")
        val tokens = texts.mapIndexed { index, text ->
            LookupToken(text, confidence = 1f - index / 20f, language = if (index == 5) null else "es")
        }

        useCase.lemmasFromTokens(tokens)

        coVerify(exactly = 1) { wordsApi.lemmasFromTokens(any(), any()) }
        assertEquals("es", request.captured.learningLanguage)
        assertEquals("ru", request.captured.translationLanguage)
        assertEquals(texts, request.captured.tokens.map { it.text })
        assertEquals(tokens.map { it.confidence }, request.captured.tokens.map { it.confidence })
        assertEquals(tokens.map { it.language }, request.captured.tokens.map { it.language })
    }

    @Test
    fun `AWP-01_1 the lemmas answer carries the id of the dictionary of the photo and the lists`() = runTest {
        coEvery { wordsApi.lemmasFromTokens(any(), any()) } returns lemmasOk

        val outcome = useCase.lemmasFromTokens(listOf(LookupToken("Las", 0.9f, "es")))

        outcome as LemmasOutcome.Found
        assertEquals(1L, outcome.dictionaryId)
        assertEquals(listOf("nube", "correr", "cielo"), outcome.lemmas.lexical)
        assertEquals("Облака бежали по небу.", outcome.lemmas.translation)
    }

    @Test
    fun `TFP-01_1 every request has a run number greater than any before`() = runTest {
        val runIds = mutableListOf<Long>()
        coEvery { wordsApi.lemmasFromTokens(any(), capture(runIds)) } returns LemmasResult.Unavailable("offline")
        val tokens = listOf(LookupToken("perro", 0.9f, "es"))

        useCase.lemmasFromTokens(tokens)
        useCase.lemmasFromTokens(tokens)
        useCase.lemmasFromTokens(tokens)

        assertEquals(3, runIds.size)
        assertTrue(runIds[0] < runIds[1] && runIds[1] < runIds[2])
    }

    @Test
    fun `lemmas without a current dictionary is a program error`() = runTest {
        every { dictionaryApi.flowDictionaryList() } returns flowOf(emptyList())

        val failure = runCatching {
            useCase.lemmasFromTokens(listOf(LookupToken("perro", 0.9f, "es")))
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        coVerify(exactly = 0) { wordsApi.lemmasFromTokens(any(), any()) }
    }

    // ===== Значения слова по лемме (IS538) =====

    private val photoContext = LemmaContext(
        text = "Las nubes corrían por el cielo.",
        translation = "Облака бежали по небу.",
    )

    private fun candidate(key: String) = CandidateLexemeApiEntity(
        key = key,
        partOfSpeechKey = "verb",
        source = "ruwiktionary",
        translations = listOf("бегать; бежать"),
        gloss = null,
        caption = null,
        notes = emptyList(),
        examples = emptyList(),
        grammar = emptyMap(),
        level = null,
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = emptyList(),
        inContext = true,
    )

    /** Ответ одним словом [text] с леммой [lemma] и одной лексемой. */
    private fun answer(text: String, lemma: String, formNote: String? = null) = LexemesResult.Success(
        attribution = "Wiktionary, CC BY-SA 4.0",
        word = NetWordApiEntity(
            text = text,
            status = WordStatus.FOUND,
            lemmas = listOf(LemmaApiEntity(lemma, formNote, emptyList(), listOf(candidate("ruwiktionary/1.verb.1")))),
            remarks = emptyList(),
        ),
        recommendations = emptyList(),
    )

    @Test
    fun `LFL-103_1 AWP-01_1 lexemes asks once with the lemma the text and its translation the languages and the component tree of the dictionary of the photo`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation, partOfSpeech, example, gloss, gender)
        val request = slot<LexemesRequestApiEntity>()
        coEvery { wordsApi.lexemesFromLemma(capture(request), any()) } returns answer("correr", "correr")

        val outcome = useCase.lexemesFromLemma(1L, "correr", photoContext)

        coVerify(exactly = 1) { wordsApi.lexemesFromLemma(any(), any()) }
        assertEquals("correr", request.captured.lemma)
        assertEquals("Las nubes corrían por el cielo.", request.captured.context?.text)
        assertEquals("Облака бежали по небу.", request.captured.context?.translation)
        assertEquals("es", request.captured.learningLanguage)
        assertEquals("ru", request.captured.translationLanguage)
        assertEquals(listOf(1L, 2L, 3L, 4L), request.captured.components.map { it.id })
        assertEquals(listOf(5L), request.captured.components[1].options[0].children.map { it.id })
        assertTrue(outcome is LexemesOutcome.Found)
    }

    @Test
    fun `LFL-103_2 the lemma goes as given - an unparsed token too`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation)
        val request = slot<LexemesRequestApiEntity>()
        coEvery { wordsApi.lexemesFromLemma(capture(request), any()) } returns LexemesResult.Unavailable("down")

        useCase.lexemesFromLemma(1L, "destibrg", photoContext)

        assertEquals("destibrg", request.captured.lemma)
        assertEquals("Las nubes corrían por el cielo.", request.captured.context?.text)
    }

    @Test
    fun `TFP-01_1 every lexemes request has a run number greater than any before`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation)
        val runIds = mutableListOf<Long>()
        coEvery { wordsApi.lexemesFromLemma(any(), capture(runIds)) } returns LexemesResult.Unavailable("down")

        useCase.lexemesFromLemma(1L, "perro", photoContext)
        useCase.lexemesFromLemma(1L, "perro", photoContext)

        assertEquals(2, runIds.size)
        assertTrue(runIds[0] < runIds[1])
    }

    @Test
    fun `AWP-43_7 an answer with another lemma brings the dictionary lemmas of that text`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation)
        coEvery { wordsApi.lexemesFromLemma(any(), any()) } returns answer("children", "child", formNote = "plural")
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(
            WordTextApiEntity(10L, "child"),
            WordTextApiEntity(11L, "children"),
        )
        coEvery { termApi.getTermsByIds(listOf(10L)) } returns listOf(
            term(10L, "child", lexeme(1L, value(1L, translation, text("ребёнок")))),
        )
        every { groupApi.wordGroups(10L) } returns flowOf(
            listOf(GroupApiEntity(id = 8L, dictionaryId = 1L, parentGroupId = null, name = "Family")),
        )

        val outcome = useCase.lexemesFromLemma(1L, "children", photoContext) as LexemesOutcome.Found

        assertEquals("child", outcome.word.lemmas.single().text)
        val existing = outcome.existing.single()
        assertEquals(10L, existing.wordId)
        assertEquals("child", existing.text)
        assertEquals(listOf("ребёнок"), existing.coreValues)
        assertEquals(listOf("Family"), existing.groupNames)
    }

    @Test
    fun `AWP-43_7 the lemma that was asked for brings no dictionary lemmas`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation)
        coEvery { wordsApi.lexemesFromLemma(any(), any()) } returns answer("correr", "Correr")

        val outcome = useCase.lexemesFromLemma(1L, "correr", photoContext) as LexemesOutcome.Found

        assertTrue(outcome.existing.isEmpty())
        coVerify(exactly = 0) { wordsBatchApi.getWordTexts(any()) }
    }

    @Test
    fun `lexemes without the dictionary of the photo is unavailable and asks nothing`() = runTest {
        coEvery { dictionaryApi.getDictionaryById(1L) } returns null

        assertEquals(LexemesOutcome.Unavailable, useCase.lexemesFromLemma(1L, "perro", photoContext))
        coVerify(exactly = 0) { wordsApi.lexemesFromLemma(any(), any()) }
    }

    @Test
    fun `addWord writes the batch as it is through the batch api`() = runTest {
        val batch = WordsBatch(dictionaryId = 1L, words = emptyList())
        val added = AddWordsBatchOutcome.Added(wordIds = listOf(7L), lexemeIds = listOf(1L))
        coEvery { wordsBatchApi.addWordsBatch(batch) } returns added

        assertEquals(added, useCase.addWord(batch))
    }

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
    fun `AWP-18_1 AWP-18_7 AWP-50_1 existing lemmas - same text ignoring case, newest first, core values, text keys of cores, groups`() = runTest {
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
    fun `AWP-18_10 AWP-46_1 AWP-46_4 existing lemma carries the add date of the word and its saved lexemes with live values of enabled components, a word without lexemes has none`() = runTest {
        val disabled = type(6L, name = "Выключенный", enabled = false)
        coEvery { dictionaryApi.getDictionaryById(1L) } returns dictEs
        stubDictionaryComponents(translation, gloss, disabled)
        every { groupApi.groupTree(1L) } returns flowOf(emptyList())
        coEvery { wordsBatchApi.getWordTexts(1L) } returns listOf(WordTextApiEntity(10L, "bank"), WordTextApiEntity(11L, "run"))
        val added = Date(1_700_000_000_000L)
        coEvery { termApi.getTermsByIds(listOf(10L, 11L)) } returns listOf(
            TermApiEntity(
                word = WordApiEntity(id = 10L, dictionaryId = 1L, value = "bank", addDate = added, changeDate = Date(1_800_000_000_000L)),
                lexemes = listOf(
                    lexeme(
                        2L,
                        value(1L, translation, text("банк")),
                        value(2L, gloss, text("bank (an institution)")),
                        value(3L, disabled, text("мимо")),
                        value(4L, translation, text("удалённое"), removed = true),
                    ),
                ),
            ),
            term(11L, "run"),
        )
        every { groupApi.wordGroups(any()) } returns flowOf(emptyList())

        val form = (useCase.loadForm(1L, listOf("bank", "run")) as FormOutcome.Loaded).form

        val bank = form.existingLemmas.first { it.wordId == 10L }
        assertEquals(1_700_000_000_000L, bank.addedAt)
        assertEquals(
            listOf(
                SavedLexeme(
                    id = 2L,
                    components = listOf(
                        BatchComponent(typeId = 1L, data = text("банк")),
                        BatchComponent(typeId = 4L, data = text("bank (an institution)")),
                    ),
                ),
            ),
            bank.lexemes,
        )
        assertTrue(form.existingLemmas.first { it.wordId == 11L }.lexemes.isEmpty())
    }

    @Test
    fun `AWP-50_6 Д42 text keys come from cores only - a gloss that is not a core is not compared`() = runTest {
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
    fun `AWP-50_1 the value of a disabled core gives no mark, the value of an enabled core does`() = runTest {
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

    private companion object {
        const val PACKAGE = "co.lexeme.app.dev"
        const val CAMERA = "content://co.lexeme.app.dev.fileprovider/photos/photo_1.jpg"
        const val GALLERY = "content://media/picker/0/com.android.providers.media.photopicker/media/1000012"
    }
}
