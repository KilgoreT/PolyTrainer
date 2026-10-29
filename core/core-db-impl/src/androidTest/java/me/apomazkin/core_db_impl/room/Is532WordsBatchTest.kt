package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_api.entity.ReservedGroupNames
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.BatchWord
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS532, Э4 и IS538, Э10: пакетная запись слов со снимка одной
 * транзакцией (AWP-20) — пачкой первой версии и по одному слову (AWP-20.9,
 * 20.10), типизированный отказ до первой вставки (AWP-21) и повтор после
 * устранения причины (AWP-21.2), тексты словаря для пометки «в словаре»,
 * option_id у лексемы из addLexemeWithComponents (AWP-32.9). In-memory Room
 * + bundled driver, Api-impl собираются руками, как в Is486DataLayerTest.
 */
@RunWith(AndroidJUnit4::class)
class Is532WordsBatchTest {

    private lateinit var db: Database
    private lateinit var dictionaryApi: CoreDbApiImpl.DictionaryApiImpl
    private lateinit var lexemeApi: CoreDbApiImpl.LexemeApiImpl
    private lateinit var termApi: CoreDbApiImpl.TermApiImpl
    private lateinit var groupApi: CoreDbApiImpl.GroupApiImpl
    private lateinit var batchApi: CoreDbApiImpl.WordsBatchApiImpl
    private lateinit var logger: RecordingLogger

    private var dictId = 0L
    private var translation = 0L
    private var partOfSpeech = 0L
    private var example = 0L
    private var verb = 0L
    private var noun = 0L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        logger = RecordingLogger()
        dictionaryApi = CoreDbApiImpl.DictionaryApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentOptionDao = db.componentOptionDao(),
            logger = logger,
        )
        lexemeApi = CoreDbApiImpl.LexemeApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentValueDao = db.componentValueDao(),
            componentOptionDao = db.componentOptionDao(),
            quizConfigDao = db.quizConfigDao(),
            logger = logger,
        )
        termApi = CoreDbApiImpl.TermApiImpl(wordDao = db.wordDao(), logger = logger)
        groupApi = CoreDbApiImpl.GroupApiImpl(
            database = db,
            groupDao = db.groupDao(),
            wordDao = db.wordDao(),
            reservedGroupNames = ReservedGroupNames(values = setOf("Все", "All")),
            logger = logger,
        )
        batchApi = CoreDbApiImpl.WordsBatchApiImpl(
            database = db,
            wordDao = db.wordDao(),
            groupDao = db.groupDao(),
            componentTypeDao = db.componentTypeDao(),
            componentOptionDao = db.componentOptionDao(),
        )
        runBlocking {
            dictId = dictionaryApi.addDictionary("EN", null, "en", "ru")
            translation = typeId(BuiltInComponent.TRANSLATION)
            partOfSpeech = typeId(BuiltInComponent.PART_OF_SPEECH)
            example = typeId(BuiltInComponent.EXAMPLE)
            val options = db.componentOptionDao().getForType(partOfSpeech)
            verb = options.single { it.systemKey == "verb" }.id
            noun = options.single { it.systemKey == "noun" }.id
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun typeId(key: BuiltInComponent): Long =
        db.componentTypeDao().getBySystemKeyForDictionary(key.key, dictId)!!.id

    private suspend fun newWord(value: String, dictionaryId: Long = dictId): Long =
        db.wordDao().addWordSuspend(WordDb(dictionaryId = dictionaryId, value = value, addDate = Date(0L)))

    private suspend fun newGroup(name: String): Long =
        (groupApi.createGroup(dictId, name) as CreateGroupOutcome.Success).groupId

    private suspend fun groupIdsOf(wordId: Long): List<Long> =
        groupApi.wordGroups(wordId).first().map { it.id }

    private suspend fun count(table: String): Long =
        db.useWriterConnection { transactor ->
            transactor.usePrepared("SELECT COUNT(*) FROM $table") { stmt ->
                stmt.step()
                stmt.getLong(0)
            }
        }

    private suspend fun counts(): Map<String, Long> =
        listOf("words", "lexemes", "component_values", "write_quiz", "word_groups").associateWith { count(it) }

    private fun text(value: String): TemplateValues = TextValues(Primitive.Text(value))

    private fun caption(text: String, source: String): TemplateValues =
        CaptionedTextValues(text = Primitive.Text(text), caption = Primitive.Text(source))

    private fun lexeme(vararg components: Pair<Long, TemplateValues>): BatchLexeme =
        BatchLexeme(components.map { (typeId, data) -> BatchComponent(typeId = typeId, data = data) })

    private fun word(
        text: String,
        groupId: Long? = null,
        wordId: Long? = null,
        vararg lexemes: BatchLexeme,
    ): BatchWord = BatchWord(wordId = wordId, text = text, groupId = groupId, lexemes = lexemes.toList())

    // === AWP-20: один пакет — новое слово, существующее, без группы ===

    @Test
    fun awp20_1_awp20_2_awp20_5_awp20_7_batchWritesWordsLexemesAndMembershipsAtOnce() = runBlocking {
        val bank = newWord("bank")
        val oldLexeme = lexemeApi.addLexemeWithComponents(
            wordId = bank, dictionaryId = dictId,
            components = listOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION) to text("берег")),
        )!!
        val potter = newGroup("Harry Potter 1")
        val finance = newGroup("Finance")
        val before = counts()

        val outcome = batchApi.addWordsBatch(
            WordsBatch(
                dictionaryId = dictId,
                words = listOf(
                    word(
                        "run", potter, null,
                        lexeme(translation to text("бежать"), partOfSpeech to ChoiceValues(verb)),
                        lexeme(
                            translation to text("бег"),
                            partOfSpeech to ChoiceValues(noun),
                            example to caption("I went for a run.", "enwiktionary"),
                        ),
                    ),
                    word("bank", finance, bank, lexeme(translation to text("банк"), partOfSpeech to ChoiceValues(noun))),
                    word("perro", null, null, lexeme(translation to text("собака"))),
                ),
            )
        )

        val added = outcome as AddWordsBatchOutcome.Added
        assertEquals(3, added.wordIds.size)
        assertEquals(bank, added.wordIds[1])
        assertEquals(4, added.lexemeIds.size)
        val after = counts()
        assertEquals(before.getValue("words") + 2, after.getValue("words"))
        assertEquals(before.getValue("lexemes") + 4, after.getValue("lexemes"))
        assertEquals(before.getValue("component_values") + 8, after.getValue("component_values"))
        assertEquals(before.getValue("write_quiz") + 4, after.getValue("write_quiz"))
        assertEquals(before.getValue("word_groups") + 2, after.getValue("word_groups"))
        // AWP-20.5: старая лексема «bank» на месте, новая рядом.
        val bankTerm = termApi.getTermById(bank)!!
        assertEquals(2, bankTerm.lexemes.size)
        assertTrue(bankTerm.lexemes.any { it.id == oldLexeme })
        // AWP-20.7: «run» в Harry Potter 1, «bank» в Finance; AWP-20.2: «perro» нигде.
        assertEquals(listOf(potter), groupIdsOf(added.wordIds[0]))
        assertEquals(listOf(finance), groupIdsOf(bank))
        assertEquals(emptyList<Long>(), groupIdsOf(added.wordIds[2]))
    }

    // === AWP-20.3: у одной леммы лексемы разных частей речи; option_id на месте ===

    @Test
    fun awp20_3_oneWordTwoLexemesWithDifferentPartsOfSpeech() = runBlocking {
        val outcome = batchApi.addWordsBatch(
            WordsBatch(
                dictionaryId = dictId,
                words = listOf(
                    word(
                        "run", null, null,
                        lexeme(translation to text("бежать"), partOfSpeech to ChoiceValues(verb)),
                        lexeme(translation to text("бег"), partOfSpeech to ChoiceValues(noun)),
                    ),
                ),
            )
        )

        val added = outcome as AddWordsBatchOutcome.Added
        assertEquals(1, added.wordIds.size)
        val term = termApi.getTermById(added.wordIds.single())!!
        val parts = term.lexemes.map { lexeme ->
            lexeme.components.single { it.type.systemKey == BuiltInComponent.PART_OF_SPEECH }.data
        }
        assertEquals(setOf<TemplateValues>(ChoiceValues(verb), ChoiceValues(noun)), parts.toSet())
    }

    // === AWP-20.9, AWP-20.10: запись по одному слову — новое и в выбранную лемму ===

    @Test
    fun awp20_9_newWordTwoLexemesCommonGroup() = runBlocking {
        val potter = newGroup("Harry Potter 1")
        val before = counts()

        val outcome = batchApi.addWordsBatch(
            WordsBatch(
                dictionaryId = dictId,
                words = listOf(
                    word(
                        "run", potter, null,
                        lexeme(translation to text("бежать"), partOfSpeech to ChoiceValues(verb)),
                        lexeme(translation to text("управлять"), partOfSpeech to ChoiceValues(verb)),
                    ),
                ),
            )
        )

        val added = outcome as AddWordsBatchOutcome.Added
        val run = added.wordIds.single()
        assertEquals(2, added.lexemeIds.size)
        assertEquals(before.getValue("words") + 1, counts().getValue("words"))
        assertEquals(2, termApi.getTermById(run)!!.lexemes.size)
        assertEquals(listOf(potter), groupIdsOf(run))
    }

    @Test
    fun awp20_10_lexemeIntoExistingWordKeepsGroups() = runBlocking {
        val bank = newWord("bank")
        val finance = newGroup("Finance")
        val potter = newGroup("Harry Potter 1")
        val first = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("bank", finance, bank, lexeme(translation to text("берег")))))
        ) as AddWordsBatchOutcome.Added

        val outcome = batchApi.addWordsBatch(
            WordsBatch(
                dictId,
                listOf(word("bank", potter, bank, lexeme(translation to text("банк"), partOfSpeech to ChoiceValues(noun)))),
            )
        )

        val added = outcome as AddWordsBatchOutcome.Added
        assertEquals(listOf(bank), added.wordIds)
        val term = termApi.getTermById(bank)!!
        assertEquals(2, term.lexemes.size)
        assertTrue(term.lexemes.any { it.id == first.lexemeIds.single() })
        assertEquals(setOf(finance, potter), groupIdsOf(bank).toSet())
    }

    // === AWP-32.9: addLexemeWithComponents хранит option_id ===

    @Test
    fun awp32_9_addLexemeWithComponentsKeepsPartOfSpeechOption() = runBlocking {
        val wordId = newWord("run")

        val lexemeId = lexemeApi.addLexemeWithComponents(
            wordId = wordId, dictionaryId = dictId,
            components = listOf(
                ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION) to text("бежать"),
                ComponentTypeRef.BuiltIn(BuiltInComponent.PART_OF_SPEECH) to ChoiceValues(verb),
            ),
        )!!

        val components = lexemeApi.getLexemeById(lexemeId)!!.components
        assertEquals(2, components.size)
        assertEquals(ChoiceValues(verb), components.single { it.type.systemKey == BuiltInComponent.PART_OF_SPEECH }.data)
    }

    // === AWP-21: отказ до первой вставки, ничего не записано ===

    @Test
    fun awp21_1_rejectionOnLastLexemeOfBatchWritesNothing() = runBlocking {
        val group = newGroup("Harry Potter 1")
        val before = counts()

        val outcome = batchApi.addWordsBatch(
            WordsBatch(
                dictionaryId = dictId,
                words = listOf(
                    word("run", group, null, lexeme(translation to text("бежать"), partOfSpeech to ChoiceValues(verb))),
                    word("perro", group, null, lexeme(translation to text("собака"))),
                    word("gato", group, null, lexeme(partOfSpeech to ChoiceValues(noun))),
                ),
            )
        )

        assertEquals(AddWordsBatchOutcome.Rejected.NoCoreValue(wordIndex = 2, lexemeIndex = 0), outcome)
        assertEquals(before, counts())
    }

    @Test
    fun awp21_2_retryAfterCauseRemovedWritesOnce() = runBlocking {
        val group = newGroup("Harry Potter 1")
        assertEquals(DeleteGroupOutcome.Success, groupApi.deleteGroup(group))
        val before = counts()
        val rejected = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("run", group, null, lexeme(translation to text("бежать")))))
        )

        val retried = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("run", null, null, lexeme(translation to text("бежать")))))
        )

        assertEquals(AddWordsBatchOutcome.Rejected.GroupMissing(group), rejected)
        assertTrue(retried is AddWordsBatchOutcome.Added)
        val after = counts()
        assertEquals(before.getValue("words") + 1, after.getValue("words"))
        assertEquals(before.getValue("lexemes") + 1, after.getValue("lexemes"))
        assertEquals(before.getValue("word_groups"), after.getValue("word_groups"))
    }

    @Test
    fun awp21_3_groupDeletedBeforeWriteIsRejectedBeforeFirstInsert() = runBlocking {
        val group = newGroup("Harry Potter 1")
        assertEquals(DeleteGroupOutcome.Success, groupApi.deleteGroup(group))
        val before = counts()

        val outcome = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("run", group, null, lexeme(translation to text("бежать")))))
        )

        assertEquals(AddWordsBatchOutcome.Rejected.GroupMissing(group), outcome)
        assertEquals(before, counts())
    }

    @Test
    fun awp21_4_wordDeletedBeforeWriteIsRejected() = runBlocking {
        val bank = newWord("bank")
        db.wordDao().removeWordSuspend(bank)
        val before = counts()

        val outcome = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("bank", null, bank, lexeme(translation to text("банк")))))
        )

        assertEquals(AddWordsBatchOutcome.Rejected.WordMissing(bank), outcome)
        assertEquals(before, counts())
    }

    @Test
    fun awp21_4_wordOfAnotherDictionaryIsRejected() = runBlocking {
        val other = dictionaryApi.addDictionary("ES", null, "es", "ru")
        val correr = newWord("correr", dictionaryId = other)

        val outcome = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("correr", null, correr, lexeme(translation to text("бежать")))))
        )

        assertEquals(AddWordsBatchOutcome.Rejected.WordMissing(correr), outcome)
    }

    @Test
    fun awp21_6_typeDisabledBeforeWriteIsRejected() = runBlocking {
        lexemeApi.setComponentEnabled(typeId = example, enabled = false)
        val before = counts()

        val outcome = batchApi.addWordsBatch(
            WordsBatch(
                dictId,
                listOf(word("run", null, null, lexeme(translation to text("бежать"), example to caption("I run.", "enwiktionary")))),
            )
        )

        assertEquals(AddWordsBatchOutcome.Rejected.TypeUnavailable(example), outcome)
        assertEquals(before, counts())
    }

    @Test
    fun awp21_6_optionOfAnotherTypeOrTemplateMismatchIsRejected() = runBlocking {
        val foreignOption = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("run", null, null, lexeme(translation to text("бежать"), partOfSpeech to ChoiceValues(999_999L)))))
        )
        val wrongTemplate = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("run", null, null, lexeme(translation to text("бежать"), partOfSpeech to text("verb")))))
        )
        val twoValuesInSingle = batchApi.addWordsBatch(
            WordsBatch(dictId, listOf(word("run", null, null, lexeme(translation to text("бежать"), translation to text("бегать")))))
        )

        assertEquals(AddWordsBatchOutcome.Rejected.TypeUnavailable(partOfSpeech), foreignOption)
        assertEquals(AddWordsBatchOutcome.Rejected.TypeUnavailable(partOfSpeech), wrongTemplate)
        assertEquals(AddWordsBatchOutcome.Rejected.TypeUnavailable(translation), twoValuesInSingle)
        assertEquals(0L, count("lexemes"))
    }

    // === Тексты словаря для пометки «в словаре»: только свой словарь ===

    @Test
    fun getWordTextsReturnsOnlyThisDictionary() = runBlocking {
        val other = dictionaryApi.addDictionary("ES", null, "es", "ru")
        val run = newWord("Run")
        newWord("correr", dictionaryId = other)

        val texts = batchApi.getWordTexts(dictId)

        assertEquals(listOf(run to "Run"), texts.map { it.id to it.value })
    }
}
