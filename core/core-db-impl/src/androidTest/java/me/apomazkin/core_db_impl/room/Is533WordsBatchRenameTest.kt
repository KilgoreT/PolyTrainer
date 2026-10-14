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
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.BatchWord
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS533 (WCL-15): слово было формой — пачка переименовывает его в лемму
 * (WCL-15.2) или переносит всё в существующее слово леммы и удаляет его
 * (WCL-15.3) той же транзакцией, что пишет лексемы. In-memory Room +
 * bundled driver, как в Is532WordsBatchTest.
 */
@RunWith(AndroidJUnit4::class)
class Is533WordsBatchRenameTest {

    private lateinit var db: Database
    private lateinit var dictionaryApi: CoreDbApiImpl.DictionaryApiImpl
    private lateinit var groupApi: CoreDbApiImpl.GroupApiImpl
    private lateinit var batchApi: CoreDbApiImpl.WordsBatchApiImpl
    private lateinit var logger: RecordingLogger

    private var dictId = 0L
    private var translation = 0L

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
            dictId = dictionaryApi.addDictionary("ES", null, "es", "ru")
            translation = db.componentTypeDao().getBySystemKeyForDictionary(BuiltInComponent.TRANSLATION.key, dictId)!!.id
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newWord(value: String, dictionaryId: Long = dictId): Long =
        db.wordDao().addWordSuspend(WordDb(dictionaryId = dictionaryId, value = value, addDate = Date(0L)))

    private suspend fun newGroup(name: String): Long =
        (groupApi.createGroup(dictId, name) as CreateGroupOutcome.Success).groupId

    private fun lexeme(text: String): BatchLexeme =
        BatchLexeme(listOf(BatchComponent(typeId = translation, data = TextValues(Primitive.Text(text)))))

    private suspend fun write(vararg words: BatchWord): AddWordsBatchOutcome =
        batchApi.addWordsBatch(WordsBatch(dictionaryId = dictId, words = words.toList()))

    private fun existing(wordId: Long, text: String, vararg lexemes: BatchLexeme, groupId: Long? = null, renameTo: String? = null, absorbWordId: Long? = null): BatchWord =
        BatchWord(wordId = wordId, text = text, groupId = groupId, lexemes = lexemes.toList(), renameTo = renameTo, absorbWordId = absorbWordId)

    private suspend fun count(table: String): Long =
        db.useWriterConnection { transactor ->
            transactor.usePrepared("SELECT COUNT(*) FROM $table") { stmt ->
                stmt.step()
                stmt.getLong(0)
            }
        }

    private suspend fun lexemeIdsOf(wordId: Long): List<Long> =
        db.useWriterConnection { transactor ->
            transactor.usePrepared("SELECT id FROM lexemes WHERE word_id = $wordId ORDER BY id") { stmt ->
                buildList { while (stmt.step()) add(stmt.getLong(0)) }
            }
        }

    /** Сырые JSON значений всех лексем слова — хватает проверки на вхождение текста. */
    private suspend fun valuesOf(wordId: Long): List<String> =
        db.useWriterConnection { transactor ->
            transactor.usePrepared("SELECT value FROM component_values WHERE lexeme_id IN (SELECT id FROM lexemes WHERE word_id = $wordId)") { stmt ->
                buildList { while (stmt.step()) add(stmt.getText(0)) }
            }
        }

    private suspend fun groupIdsOf(wordId: Long): List<Long> = groupApi.wordGroups(wordId).first().map { it.id }

    // ===== WCL-15.2: переименование =====

    @Test
    fun wcl15_2_existingWordIsRenamedToTheLemmaInTheSameWriteAsItsLexemes() = runBlocking {
        val formWord = newWord("corrían")

        val outcome = write(existing(formWord, "corrían", lexeme("бегать"), renameTo = "correr"))

        assertTrue(outcome is AddWordsBatchOutcome.Added)
        val term = db.wordDao().getWordSuspend(formWord)
        assertEquals("correr", term.wordDb.value)
        assertNotNull(term.wordDb.changeDate)
        assertEquals(1, term.lexemeListDb.size)
    }

    @Test
    fun wcl15_2_withoutTheFlagTheTextStays() = runBlocking {
        val formWord = newWord("corrían")

        write(existing(formWord, "corrían", lexeme("бегать")))

        val term = db.wordDao().getWordSuspend(formWord)
        assertEquals("corrían", term.wordDb.value)
        assertNull(term.wordDb.changeDate)
    }

    @Test
    fun wcl15_2_blankRenameAndNewWordAreIgnored() = runBlocking {
        val formWord = newWord("corrían")

        val outcome = write(
            existing(formWord, "corrían", lexeme("бегать"), renameTo = "   "),
            BatchWord(wordId = null, text = "perro", groupId = null, lexemes = listOf(lexeme("собака")), renameTo = "gato"),
        ) as AddWordsBatchOutcome.Added

        assertEquals("corrían", db.wordDao().getWordSuspend(formWord).wordDb.value)
        assertEquals("perro", db.wordDao().getWordSuspend(outcome.wordIds[1]).wordDb.value)
    }

    // ===== WCL-15.3: перенос =====

    @Test
    fun wcl15_3_transferMovesTheLexemesAddsTheNewOnesAndDeletesTheSourceWord() = runBlocking {
        val formWord = newWord("corrían")
        val lemmaWord = newWord("correr")
        val forms = newGroup("Формы")
        val verbs = newGroup("Глаголы")
        write(existing(formWord, "corrían", lexeme("бежать"), lexeme("мчаться"), groupId = forms))
        write(existing(lemmaWord, "correr", lexeme("бегать"), groupId = verbs))
        val moved = lexemeIdsOf(formWord)
        val wordsBefore = count("words")
        val membershipsBefore = count("word_groups")

        val outcome = write(existing(lemmaWord, "correr", lexeme("течь"), absorbWordId = formWord))

        assertEquals(listOf(lemmaWord), (outcome as AddWordsBatchOutcome.Added).wordIds)
        assertEquals(1, outcome.lexemeIds.size)
        assertNull(db.wordDao().getWordDictionaryId(formWord))
        assertEquals(wordsBefore - 1, count("words"))
        val lexemes = lexemeIdsOf(lemmaWord)
        assertEquals(4, lexemes.size)
        assertTrue(lexemes.containsAll(moved))
        val values = valuesOf(lemmaWord)
        for (text in listOf("бегать", "бежать", "мчаться", "течь")) {
            assertTrue(text, values.any { text in it })
        }
        assertEquals(0, count("lexemes WHERE word_id = $formWord"))
        // Членства источника ушли каскадом, у получателя — как были.
        assertEquals(membershipsBefore - 1, count("word_groups"))
        assertEquals(listOf(verbs), groupIdsOf(lemmaWord))
    }

    @Test
    fun wcl15_3_transferWithoutNewLexemesStillMovesAndDeletes() = runBlocking {
        val formWord = newWord("corrían")
        val lemmaWord = newWord("correr")
        write(existing(formWord, "corrían", lexeme("бежать")))

        val outcome = write(existing(lemmaWord, "correr", absorbWordId = formWord))

        assertTrue(outcome is AddWordsBatchOutcome.Added)
        assertNull(db.wordDao().getWordDictionaryId(formWord))
        assertEquals(1, lexemeIdsOf(lemmaWord).size)
    }

    @Test
    fun wcl15_6_transferIsRejectedBeforeAnyWriteWhenTheSourceIsMissingForeignOrTheTargetItself() = runBlocking {
        val lemmaWord = newWord("correr")
        val foreignDict = dictionaryApi.addDictionary("EN", null, "en", "ru")
        val foreignWord = newWord("correr", dictionaryId = foreignDict)
        val lexemesBefore = count("lexemes")

        assertEquals(AddWordsBatchOutcome.Rejected.WordMissing(999L), write(existing(lemmaWord, "correr", lexeme("течь"), absorbWordId = 999L)))
        assertEquals(AddWordsBatchOutcome.Rejected.WordMissing(foreignWord), write(existing(lemmaWord, "correr", lexeme("течь"), absorbWordId = foreignWord)))
        assertEquals(AddWordsBatchOutcome.Rejected.WordMissing(lemmaWord), write(existing(lemmaWord, "correr", lexeme("течь"), absorbWordId = lemmaWord)))

        assertEquals(lexemesBefore, count("lexemes"))
        assertNotNull(db.wordDao().getWordDictionaryId(foreignWord))
    }

    @Test
    fun wcl15_3_newWordIgnoresTheSource() = runBlocking {
        val formWord = newWord("corrían")

        val outcome = write(BatchWord(wordId = null, text = "perro", groupId = null, lexemes = listOf(lexeme("собака")), absorbWordId = formWord))

        assertTrue(outcome is AddWordsBatchOutcome.Added)
        assertNotNull(db.wordDao().getWordDictionaryId(formWord))
    }
}
