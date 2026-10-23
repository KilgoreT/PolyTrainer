package me.apomazkin.wordcard.mate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.ComponentMatchUi
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.candidates.mapper.toUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.DictionaryForm
import me.apomazkin.lexeme.ExistingLemma
import me.apomazkin.lexeme.FormComponent
import me.apomazkin.lexeme.FormGroup
import me.apomazkin.lexeme.FormOption
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupLexeme
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.WordLookupOutcome
import me.apomazkin.lexeme.WordRemark
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordcard.deps.AddComponentValueResult
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §9.5 DatasourceEffectHandler (REWRITE, generic) — two-Msg burst + error-ветки.
 * TDD red — против НОВОГО WordCardUseCase (generic) и новых Effect/Msg/result-типов.
 */
class DatasourceEffectHandlerTest {
    /** Fake нового generic WordCardUseCase (лямбда-поля). */
    private class FakeUseCase(
        var getTermByIdImpl: suspend (Long) -> Term? = { null },
        var deleteWordImpl: suspend (Long) -> Int = { 0 },
        var updateWordImpl: suspend (Long, String) -> Boolean = { _, _ -> false },
        var deleteLexemeImpl: suspend (Long, Long) -> RemoveLexemeResult? = { _, _ -> null },
        var addLexemeWithComponentImpl: suspend (Long, Long, ComponentTypeRef, TemplateValues) -> Lexeme? =
            { _, _, _, _ -> null },
        var addComponentValueImpl: suspend (Long, ComponentTypeId, TemplateValues) -> AddComponentValueResult? =
            { _, _, _ -> null },
        var updateComponentValueImpl: suspend (ComponentValueId, Long, TemplateValues) -> Lexeme? =
            { _, _, _ -> null },
        var deleteComponentValueImpl: suspend (ComponentValueId, Long) -> RemoveComponentResult? =
            { _, _ -> null },
        var restoreImpl: suspend (Long, Long, Lexeme) -> Lexeme? = { _, _, _ -> null },
        var flowTypesImpl: (Long) -> Flow<me.apomazkin.wordcard.deps.AvailableComponents> =
            {
                flowOf(
                    me
                        .apomazkin
                        .wordcard
                        .deps
                        .AvailableComponents(emptyList()),
                )
            },
        var captionSuggestionsImpl: suspend (ComponentTypeId) -> List<String> = { emptyList() },
        // IS533/IS536: лексемы-кандидаты
        var hasServerAccessImpl: suspend () -> Boolean = { false },
        var lookupWordImpl: suspend (Long, Long) -> WordLookupOutcome =
            { _, _ -> WordLookupOutcome.WordMissing },
        var loadCandidatesFormImpl: suspend (Long, List<String>) -> FormOutcome =
            { _, _ -> FormOutcome.DictionaryMissing },
        var addCandidatesImpl: suspend (WordsBatch) -> AddWordsBatchOutcome =
            { AddWordsBatchOutcome.Rejected.DictionaryMissing },
    ) : WordCardUseCase {
        override suspend fun hasServerAccess(): Boolean = hasServerAccessImpl()

        override suspend fun lookupWord(
            wordId: Long,
            runId: Long,
        ): WordLookupOutcome = lookupWordImpl(wordId, runId)

        override suspend fun loadCandidatesForm(
            wordId: Long,
            lemmaTexts: List<String>,
        ): FormOutcome = loadCandidatesFormImpl(wordId, lemmaTexts)

        override suspend fun addCandidates(batch: WordsBatch): AddWordsBatchOutcome =
            addCandidatesImpl(batch)

        override suspend fun getTermById(wordId: Long): Term? = getTermByIdImpl(wordId)

        override suspend fun deleteWord(wordId: Long): Int = deleteWordImpl(wordId)

        override suspend fun updateWord(
            wordId: Long,
            value: String,
        ): Boolean = updateWordImpl(wordId, value)

        override suspend fun deleteLexeme(
            wordId: Long,
            lexemeId: Long,
        ): RemoveLexemeResult? = deleteLexemeImpl(wordId, lexemeId)

        override suspend fun addLexemeWithComponent(
            wordId: Long,
            dictionaryId: Long,
            ref: ComponentTypeRef,
            data: TemplateValues,
        ): Lexeme? = addLexemeWithComponentImpl(wordId, dictionaryId, ref, data)

        override suspend fun addComponentValue(
            lexemeId: Long,
            componentTypeId: ComponentTypeId,
            data: TemplateValues,
        ): AddComponentValueResult? = addComponentValueImpl(lexemeId, componentTypeId, data)

        override suspend fun updateComponentValue(
            componentValueId: ComponentValueId,
            lexemeId: Long,
            data: TemplateValues,
        ): Lexeme? = updateComponentValueImpl(componentValueId, lexemeId, data)

        override suspend fun deleteComponentValue(
            componentValueId: ComponentValueId,
            lexemeId: Long,
        ): RemoveComponentResult? = deleteComponentValueImpl(componentValueId, lexemeId)

        override suspend fun restoreLexemeWithComponents(
            wordId: Long,
            dictionaryId: Long,
            snapshot: Lexeme,
        ): Lexeme? = restoreImpl(wordId, dictionaryId, snapshot)

        override fun flowAvailableComponentTypes(dictionaryId: Long): Flow<me.apomazkin.wordcard.deps.AvailableComponents> =
            flowTypesImpl(dictionaryId)

        override suspend fun getCaptionSuggestions(componentTypeId: ComponentTypeId): List<String> = captionSuggestionsImpl(componentTypeId)

        override fun wordGroups(wordId: Long): Flow<List<me.apomazkin.group.GroupNode>> = TODO()

        override fun dictGroups(dictionaryId: Long): Flow<List<me.apomazkin.group.GroupNode>> = TODO()

        override suspend fun addWordToGroup(
            wordId: Long,
            groupId: Long,
        ): me.apomazkin.group.AddMembershipOutcome = TODO()

        override suspend fun removeWordFromGroup(
            wordId: Long,
            groupId: Long,
        ): me.apomazkin.group.RemoveMembershipOutcome = TODO()
    }

    private object NoopLogger : LexemeLogger {
        override fun log(
            level: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit
    }

    private fun run(
        useCase: WordCardUseCase,
        effect: DatasourceEffect,
    ): List<Msg> {
        val msgs = mutableListOf<Msg>()
        val handler = DatasourceEffectHandler(useCase, NoopLogger)
        runBlocking { handler.runEffect(effect) { msgs += it } }
        return msgs
    }

    @Test
    fun `LoadWord success yields WordLoaded`() {
        val term = stubTerm()
        val msgs = run(FakeUseCase(getTermByIdImpl = { term }), DatasourceEffect.LoadWord(7L))
        assertEquals(1, msgs.size)
        assertEquals(Msg.WordLoaded(term), msgs.single())
    }

    @Test
    fun `LoadWord null yields WordNotFound`() {
        val msgs = run(FakeUseCase(getTermByIdImpl = { null }), DatasourceEffect.LoadWord(7L))
        assertTrue(msgs.single() is Msg.WordNotFound)
    }

    @Test
    fun `LoadWord exception propagates, onFail yields WordNotFound`() {
        val effect = DatasourceEffect.LoadWord(7L)

        // Handler ошибок не ловит — исключение уходит раннеру.
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            run(FakeUseCase(getTermByIdImpl = { throw IllegalStateException() }), effect)
        }

        // Маппинг провала объявлен в эффекте (RecoverableEffect).
        assertTrue(effect.onFail(IllegalStateException()) is Msg.WordNotFound)
    }

    @Test
    fun `AddValue success yields Refresh then Inserted burst`() {
        val lex = domainLexeme(7L, listOf(domainCv(60L, 7L, "v")))
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> AddComponentValueResult(lex, ComponentValueId(60L)) })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L,
                dictionaryId = 3L,
                lexemeId = 7L,
                pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("v"),
            ),
        )
        assertEquals(2, msgs.size)
        assertTrue("первый Refresh", msgs[0] is Msg.RefreshLexemeComponents)
        val inserted = msgs[1] as Msg.ComponentValueInserted
        assertEquals(1L, inserted.pristineKey)
        assertEquals(ComponentValueId(60L), inserted.newCvId)
    }

    @Test
    fun `CreateLexeme success yields LexemeDraftPromoted with anchor from effect`() {
        val lex = domainLexeme(900L, listOf(domainCv(60L, 900L, "v")))
        val uc = FakeUseCase(addLexemeWithComponentImpl = { _, _, _, _ -> lex })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.CreateLexeme(
                wordId = 7L,
                dictionaryId = 3L,
                pristineKey = 5L,
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("v"),
            ),
        )
        assertEquals(listOf(Msg.LexemeDraftPromoted(lex, anchorPristineKey = 5L)), msgs)
    }

    @Test
    fun `UpdateValue success yields single Refresh`() {
        val lex = domainLexeme(7L, listOf(domainCv(5L, 7L, "new")))
        val uc = FakeUseCase(updateComponentValueImpl = { _, _, _ -> lex })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.UpdateValue(
                wordId = 7L,
                dictionaryId = 3L,
                lexemeId = 7L,
                componentValueId = ComponentValueId(5L),
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("new"),
            ),
        )
        assertEquals(1, msgs.size)
        assertTrue(msgs.single() is Msg.RefreshLexemeComponents)
    }

    @Test
    fun `AddValue null yields OperationFailed`() {
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> null })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L,
                dictionaryId = 3L,
                lexemeId = 7L,
                pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("v"),
            ),
        )
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `RemoveComponentValue ComponentRemoved yields Refresh`() {
        val lex = domainLexeme(7L, listOf(domainCv(6L, 7L, "rest")))
        val uc = FakeUseCase(deleteComponentValueImpl = { _, _ -> RemoveComponentResult.ComponentRemoved(lex) })
        val msgs = run(uc, DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L))
        assertTrue(msgs.single() is Msg.RefreshLexemeComponents)
    }

    // IS486 фаза 3 (В4): удаление последнего значения деградирует лексему в черновик —
    // тот же ComponentRemoved-путь с пустым списком компонентов.
    @Test
    fun `RemoveComponentValue last value yields Refresh with empty components`() {
        val emptyLexeme = domainLexeme(7L, emptyList())
        val uc = FakeUseCase(deleteComponentValueImpl = { _, _ -> RemoveComponentResult.ComponentRemoved(emptyLexeme) })
        val msgs = run(uc, DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L))
        assertEquals(Msg.RefreshLexemeComponents(7L, emptyList()), msgs.single())
    }

    @Test
    fun `RemoveComponentValue null yields OperationFailed`() {
        val uc = FakeUseCase(deleteComponentValueImpl = { _, _ -> null })
        val msgs = run(uc, DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L))
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `RestoreLexemeWithComponents success yields WordLoaded`() {
        val snapshot = domainLexeme(7L, listOf(domainCv(5L, 7L, "x")))
        val term = stubTerm()
        val uc = FakeUseCase(restoreImpl = { _, _, _ -> snapshot }, getTermByIdImpl = { term })
        val msgs = run(uc, DatasourceEffect.RestoreLexemeWithComponents(7L, 3L, snapshot))
        assertTrue(msgs.any { it is Msg.WordLoaded })
    }

    @Test
    fun `RestoreLexemeWithComponents null yields RestoreLexemeFailed with snapshot`() {
        val snapshot = domainLexeme(7L, listOf(domainCv(5L, 7L, "x")))
        val uc = FakeUseCase(restoreImpl = { _, _, _ -> null })
        val msgs = run(uc, DatasourceEffect.RestoreLexemeWithComponents(7L, 3L, snapshot))
        assertEquals(Msg.RestoreLexemeFailed(snapshot), msgs.single())
    }

    @Test
    fun `UpdateValue null yields OperationFailed`() {
        val uc = FakeUseCase(updateComponentValueImpl = { _, _, _ -> null })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.UpdateValue(
                wordId = 7L,
                dictionaryId = 3L,
                lexemeId = 7L,
                componentValueId = ComponentValueId(5L),
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("x"),
            ),
        )
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `CreateLexeme null yields OperationFailed`() {
        val uc = FakeUseCase(addLexemeWithComponentImpl = { _, _, _, _ -> null })
        val msgs = run(
            uc,
            DatasourceEffect.UpsertComponentValue.CreateLexeme(
                wordId = 7L,
                dictionaryId = 3L,
                pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("x"),
            ),
        )
        assertTrue(msgs.single() is Msg.OperationFailed)
    }

    @Test
    fun `useCase exception in upsert propagates, onFail yields OperationFailed`() {
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> throw IllegalStateException("boom") })
        val effect = DatasourceEffect.UpsertComponentValue.AddValue(
            wordId = 7L,
            dictionaryId = 3L,
            lexemeId = 7L,
            pristineKey = 1L,
            componentTypeId = ComponentTypeId(50L),
            componentTypeRef = TR,
            data = textValuesOf("v"),
        )

        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            run(uc, effect)
        }

        assertTrue(effect.onFail(IllegalStateException("boom")) is Msg.OperationFailed)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `CancellationException propagates not mapped to OperationFailed`() {
        val uc = FakeUseCase(addComponentValueImpl = { _, _, _ -> throw kotlinx.coroutines.CancellationException() })
        run(
            uc,
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = 7L,
                dictionaryId = 3L,
                lexemeId = 7L,
                pristineKey = 1L,
                componentTypeId = ComponentTypeId(50L),
                componentTypeRef = TR,
                data = textValuesOf("v"),
            ),
        )
    }

    @Test
    fun `RemoveWord success yields NavigateBack`() {
        val msgs = run(FakeUseCase(deleteWordImpl = { 1 }), DatasourceEffect.RemoveWord(7L))
        assertTrue(msgs.single() is Msg.NavigateBack)
    }

    // ===== IS533/IS536: лексемы-кандидаты (WCL) =====

    private val emptyBatch = WordsBatch(dictionaryId = 3L, words = emptyList())

    private val lookupEffect = DatasourceEffect.LookupCandidates(wordId = 7L, requestId = 4L)

    private val formEffect = DatasourceEffect.LoadCandidatesForm(
        wordId = 7L,
        lemmaTexts = emptyList(),
        requestId = 2L,
    )

    private val addEffect = DatasourceEffect.AddCandidates(
        batch = emptyBatch,
        wordId = 7L,
        requestId = 1L,
    )

    /** Слово ответа в домене: лемма с лексемой и строкой сопоставления на «Перевод» (17). */
    private val domainWord = LookupWord(
        text = "run",
        status = LookupWordStatus.FOUND,
        lemmas = listOf(
            LookupLemma(
                text = "run",
                formNote = null,
                pronunciations = listOf("/rʌn/"),
                lexemes = listOf(
                    LookupLexeme(
                        key = "ru/v1",
                        partOfSpeechKey = "verb",
                        source = "ruwiktionary",
                        translations = listOf("бежать, бегать"),
                        gloss = null,
                        caption = null,
                        notes = emptyList(),
                        examples = emptyList(),
                        grammar = emptyMap(),
                        level = null,
                        synonyms = emptyList(),
                        antonyms = emptyList(),
                        componentMatches = listOf(
                            ComponentMatch(17L, optionId = null, field = "translations"),
                        ),
                    ),
                ),
            ),
        ),
        remarks = listOf(WordRemark.NoTranslation(lemma = "run", lexemeKey = "ru/v1")),
    )

    private val domainRecommendation = ComponentRecommendation(
        name = "Род",
        template = ComponentTemplate.CHOICE,
        options = listOf("м.р.", "ж.р."),
    )

    private fun lookup(outcome: WordLookupOutcome): List<Msg> =
        run(FakeUseCase(lookupWordImpl = { _, _ -> outcome }), lookupEffect)

    private fun loadForm(outcome: FormOutcome): List<Msg> =
        run(FakeUseCase(loadCandidatesFormImpl = { _, _ -> outcome }), formEffect)

    private fun add(outcome: AddWordsBatchOutcome): List<Msg> =
        run(FakeUseCase(addCandidatesImpl = { outcome }), addEffect)

    @Test
    fun `WCL-08_1 LoadServerAccess reports whether the build has a key`() {
        val useCase = FakeUseCase(hasServerAccessImpl = { true })
        val msgs = run(useCase, DatasourceEffect.LoadServerAccess)
        assertEquals(listOf<Msg>(Msg.ServerAccessLoaded(true)), msgs)
    }

    @Test
    fun `WCL-03_1 LookupCandidates passes the word and the run number to the use case`() {
        var seen: Pair<Long, Long>? = null
        val useCase = FakeUseCase(
            lookupWordImpl = { wordId, runId ->
                seen = wordId to runId
                WordLookupOutcome.NotFound
            },
        )
        run(useCase, lookupEffect)
        assertEquals(7L to 4L, seen)
    }

    @Test
    fun `WCL-04_1 Found maps the word and the recommendations to UI with the run number`() {
        val msgs = lookup(WordLookupOutcome.Found(domainWord, listOf(domainRecommendation)))

        val found = msgs.single() as Msg.CandidatesFound
        assertEquals(4L, found.requestId)
        assertEquals(domainWord.toUi(), found.word)
        assertEquals(listOf(domainRecommendation.toUi()), found.recommendations)
        // Сопоставление и замечание доезжают до стейта как есть (AWP-29, AWP-34).
        val lexeme = found.word.lemmas.single().lexemes.single()
        val match = ComponentMatchUi(componentId = 17L, optionId = null, field = "translations")
        assertEquals(listOf(match), lexeme.componentMatches)
        val remark = WordRemarkUi.NoTranslation(lemma = "run", lexemeKey = "ru/v1")
        assertEquals(listOf<WordRemarkUi>(remark), found.word.remarks)
    }

    @Test
    fun `WCL-11_1 NotFound reports no candidates`() {
        val msgs = lookup(WordLookupOutcome.NotFound)
        assertEquals(listOf<Msg>(Msg.CandidatesNotFound(requestId = 4L)), msgs)
    }

    @Test
    fun `WCL-01_3 WordMissing reports the deleted word as a failure without retry`() {
        val msgs = lookup(WordLookupOutcome.WordMissing)
        val failed = Msg.CandidatesLookupFailed(R.string.candidates_word_missing, requestId = 4L)
        assertEquals(listOf<Msg>(failed), msgs)
    }

    @Test
    fun `WCL-11_2 a refusal by key or limit reports its text as a failure without retry`() {
        val expected = mapOf(
            LookupDenyReason.NO_ACCESS_KEY to R.string.photo_words_error_no_access,
            LookupDenyReason.UNAUTHORIZED to R.string.photo_words_error_no_access,
            LookupDenyReason.RATE_LIMITED to R.string.photo_words_error_rate_limited,
            LookupDenyReason.REQUEST_TOO_LARGE to R.string.photo_words_error_too_many,
            LookupDenyReason.UNSUPPORTED_LANGUAGE to R.string.photo_words_error_language,
        )
        for ((reason, errorRes) in expected) {
            assertEquals(
                reason.name,
                listOf<Msg>(Msg.CandidatesLookupFailed(errorRes = errorRes, requestId = 4L)),
                lookup(WordLookupOutcome.Denied(reason)),
            )
        }
    }

    @Test
    fun `WCL-11_6 WCL-11_9 llm_unavailable reports the unavailable text with retry`() {
        val msgs = lookup(WordLookupOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE))
        val unavailable = Msg.CandidatesLookupUnavailable(R.string.scan_server_unavailable, 4L)
        assertEquals(listOf<Msg>(unavailable), msgs)
    }

    @Test
    fun `WCL-11_3 WCL-11_9 Unavailable reports the silent server text with retry`() {
        val msgs = lookup(WordLookupOutcome.Unavailable)
        val unavailable = Msg.CandidatesLookupUnavailable(R.string.photo_words_error_server, 4L)
        assertEquals(listOf<Msg>(unavailable), msgs)
    }

    @Test
    fun `WCL-11_7 WCL-11_9 WordFailed reports the unavailable text with retry`() {
        val msgs = lookup(WordLookupOutcome.WordFailed)
        val unavailable = Msg.CandidatesLookupUnavailable(R.string.scan_server_unavailable, 4L)
        assertEquals(listOf<Msg>(unavailable), msgs)
    }

    @Test
    fun `WCL-15_1 LoadCandidatesForm passes the form lemma texts to the use case`() {
        var seen: Pair<Long, List<String>>? = null
        val useCase = FakeUseCase(
            loadCandidatesFormImpl = { wordId, lemmas ->
                seen = wordId to lemmas
                FormOutcome.DictionaryMissing
            },
        )
        run(useCase, formEffect.copy(lemmaTexts = listOf("correr")))
        assertEquals(7L to listOf("correr"), seen)
    }

    @Test
    fun `WCL-07_1 LoadCandidatesForm Loaded maps the form to UI with the run number`() {
        val form = DictionaryForm(
            dictionaryId = 3L,
            translationLanguage = "ru",
            components = listOf(
                FormComponent(
                    id = 17L,
                    systemKey = BuiltInComponent.TRANSLATION,
                    name = null,
                    template = ComponentTemplate.TEXT,
                    isCore = true,
                    isMultiple = false,
                    position = 0,
                    dependsOn = DependencyTarget.Lexeme,
                ),
                FormComponent(
                    id = 5L,
                    systemKey = null,
                    name = "Род",
                    template = ComponentTemplate.CHOICE,
                    isCore = false,
                    isMultiple = false,
                    position = 1,
                    dependsOn = DependencyTarget.Option(21L),
                ),
            ),
            options = mapOf(
                5L to listOf(FormOption(id = 51L, key = null, label = "м.р.", position = 0)),
            ),
            groups = listOf(FormGroup(id = 1L, name = "Finance")),
            existingLemmas = listOf(
                ExistingLemma(
                    wordId = 7L,
                    text = "run",
                    lexemeCount = 1,
                    coreValues = listOf("управлять"),
                    lexemeTextKeys = setOf("управлять"),
                    groupNames = listOf("Finance"),
                ),
            ),
        )
        val msgs = loadForm(FormOutcome.Loaded(form))

        val loaded = msgs.single() as Msg.CandidatesFormLoaded
        assertEquals(2L, loaded.requestId)
        assertEquals(form.toUi(), loaded.form)
        // Зависимость и опции «Выбора» доезжают как есть (AWP-17.6).
        assertEquals(DependencyTarget.Option(21L), loaded.form.components.last().dependsOn)
        assertEquals(listOf(51L), loaded.form.options.getValue(5L).map { it.id })
    }

    @Test
    fun `WCL-11_5 LoadCandidatesForm reports a missing dictionary and a failed read`() {
        val missing = Msg.CandidatesFormFailed(R.string.photo_add_dictionary_missing, 2L)
        assertEquals(listOf<Msg>(missing), loadForm(FormOutcome.DictionaryMissing))

        val failed = Msg.CandidatesFormFailed(R.string.photo_add_form_failed, 2L)
        assertEquals(listOf<Msg>(failed), loadForm(FormOutcome.Failed("boom")))
    }

    @Test
    fun `WCL-10_1 AddCandidates success reloads the word and then reports the count`() {
        val term = stubTerm()
        val useCase = FakeUseCase(
            getTermByIdImpl = { term },
            addCandidatesImpl = {
                AddWordsBatchOutcome.Added(wordIds = listOf(7L), lexemeIds = listOf(1L, 2L, 3L))
            },
        )
        val msgs = run(useCase, addEffect)
        assertEquals(2, msgs.size)
        assertEquals(true, msgs[0] is Msg.WordLoaded)
        assertEquals(Msg.CandidatesAdded(lexemeCount = 3, requestId = 1L), msgs[1])
    }

    @Test
    fun `WCL-15_3 AddCandidates with a transfer reports it instead of reloading the word`() {
        val useCase = FakeUseCase(
            getTermByIdImpl = { error("the dialog word is gone — nothing to reload") },
            addCandidatesImpl = {
                AddWordsBatchOutcome.Added(wordIds = listOf(8L), lexemeIds = listOf(1L, 2L))
            },
        )
        val msgs = run(useCase, addEffect.copy(isTransfer = true))
        assertEquals(listOf<Msg>(Msg.CandidatesTransferred(requestId = 1L)), msgs)
    }

    @Test
    fun `WCL-13_2 WCL-15_6 a vanished word is rejected with its own message and typed failure`() {
        val msgs = add(AddWordsBatchOutcome.Rejected.WordMissing(7L))
        val gone = Msg.CandidatesWordGone(AddFailureUi.WordMissing(wordId = 7L), requestId = 1L)
        assertEquals(listOf<Msg>(gone), msgs)
    }

    @Test
    fun `WCL-13_3 a disabled component is rejected with its own message and typed failure`() {
        val msgs = add(AddWordsBatchOutcome.Rejected.TypeUnavailable(typeId = 20L))
        val failure = AddFailureUi.TypeUnavailable(typeId = 20L)
        val gone = Msg.CandidatesComponentGone(failure, requestId = 1L)
        assertEquals(listOf<Msg>(gone), msgs)
    }

    @Test
    fun `WCL-13_1 the other rejections go to the panel as they are`() {
        val cases = mapOf(
            AddWordsBatchOutcome.Rejected.GroupMissing(groupId = 5L) to
                AddFailureUi.GroupMissing(groupId = 5L),
            AddWordsBatchOutcome.Rejected.NoCoreValue(wordIndex = 0, lexemeIndex = 1) to
                AddFailureUi.NoCoreValue,
            AddWordsBatchOutcome.Rejected.DictionaryMissing to AddFailureUi.DictionaryMissing,
        )
        for ((rejected, failure) in cases) {
            val failed = Msg.CandidatesAddFailed(failure, requestId = 1L)
            assertEquals(listOf<Msg>(failed), add(rejected))
        }
    }

    @Test
    fun `WCL-11_3 WCL-11_5 WCL-13_1 failed effects become the unavailable, form, add failures`() {
        val error = RuntimeException("x")
        assertEquals(
            Msg.CandidatesLookupUnavailable(R.string.photo_words_error_server, requestId = 4L),
            lookupEffect.onFail(error),
        )
        assertEquals(
            Msg.CandidatesFormFailed(R.string.photo_add_form_failed, requestId = 2L),
            formEffect.onFail(error),
        )
        assertEquals(
            Msg.CandidatesAddFailed(AddFailureUi.Failed, requestId = 1L),
            addEffect.onFail(error),
        )
    }
}
