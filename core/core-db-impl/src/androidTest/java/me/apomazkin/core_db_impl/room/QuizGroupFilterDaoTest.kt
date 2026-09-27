package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_api.entity.ReservedGroupNames
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.group.CreateGroupOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS500: групповой фильтр квиз-выборок и счётчики пикера.
 *
 * Контракты:
 * - `getWriteQuizIds`/`getEarliest`/`getFrequentMistakes` с
 *   `groupId != null` возвращают ТОЛЬКО квиз-записи слов живой группы;
 *   null — весь словарь (поведение до фичи);
 * - фильтр применяется в WHERE до ORDER BY/LIMIT: топ считается
 *   ИЗ ГРУППЫ, а не пересечением глобального топа с группой (пины);
 * - счётчики уровня 1: слово считается при наличии хотя бы одной
 *   лексемы; пустая группа возвращается со счётчиком 0.
 */
@RunWith(AndroidJUnit4::class)
class QuizGroupFilterDaoTest {

    private lateinit var db: Database
    private lateinit var groupApi: CoreDbApiImpl.GroupApiImpl

    private var dictId: Long = 0
    private var groupA: Long = 0
    private var groupB: Long = 0

    private val now = Date(0L)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        groupApi = CoreDbApiImpl.GroupApiImpl(
            database = db,
            groupDao = db.groupDao(),
            wordDao = db.wordDao(),
            reservedGroupNames = ReservedGroupNames(values = setOf("Все", "All")),
            logger = RecordingLogger(),
        )
        runBlocking {
            dictId = db.wordDao().addDictionary(DictionaryDb(name = "EN", addDate = now))
            groupA = createGroup("dom")
            groupB = createGroup("byt")
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun createGroup(name: String): Long =
        (groupApi.createGroup(dictId, name) as CreateGroupOutcome.Success).groupId

    private suspend fun addWord(value: String, vararg groups: Long): Long {
        val wordId = db.wordDao().addWordSuspend(
            WordDb(value = value, dictionaryId = dictId, addDate = now),
        )
        groups.forEach { groupApi.addWordToGroup(wordId, it) }
        return wordId
    }

    /** Слово + лексема + квиз-строка (инвариант «лексема ⇔ write_quiz»). */
    private suspend fun addQuizWord(value: String, vararg groups: Long): Long {
        val wordId = addWord(value, *groups)
        db.wordDao().addLexemeWithQuiz(
            lexemeDb = LexemeDb(wordId = wordId, addDate = now),
            dictionaryId = dictId,
        )
        return wordId
    }

    /** Квиз-строка с ручными полями (для пинов earliest/mistakes). */
    private suspend fun addQuizWordCustom(
        value: String,
        lastSelect: Date?,
        errorCount: Int,
        vararg groups: Long,
    ): Long {
        val wordId = addWord(value, *groups)
        val lexemeId = db.wordDao().addLexeme(LexemeDb(wordId = wordId, addDate = now))
        db.wordDao().addWriteQuiz(
            WriteQuizDb(
                dictionaryId = dictId,
                lexemeId = lexemeId,
                addDate = now,
                lastCorrectAnswerDate = lastSelect,
                errorCount = errorCount,
            ),
        )
        return wordId
    }

    private suspend fun quizIds(groupId: Long?): List<Long> =
        db.wordDao().getWriteQuizIds(grade = 0, langId = dictId, groupId = groupId)

    // === getWriteQuizIds: фильтр ===

    @Test
    fun quizIds_nullGroup_wholeDictionary() = runBlocking {
        addQuizWord("cat", groupA)
        addQuizWord("dog", groupB)
        addQuizWord("fox") // вне групп

        assertEquals(3, quizIds(groupId = null).size)
    }

    @Test
    fun quizIds_group_onlyItsWords() = runBlocking {
        addQuizWord("cat", groupA)
        addQuizWord("dog", groupB)
        addQuizWord("fox")

        assertEquals(1, quizIds(groupA).size)
    }

    @Test
    fun quizIds_wordInTwoGroups_singleRow() = runBlocking {
        addQuizWord("cat", groupA, groupB)

        assertEquals(1, quizIds(groupA).size)
        assertEquals(1, quizIds(groupB).size)
    }

    @Test
    fun quizIds_wordWithoutLexeme_absent() = runBlocking {
        addWord("bare", groupA) // слово без лексемы — квиз-строки нет

        assertTrue(quizIds(groupA).isEmpty())
    }

    @Test
    fun quizIds_deadGroup_empty() = runBlocking {
        addQuizWord("cat", groupA)
        groupApi.deleteGroup(groupA)

        assertTrue(quizIds(groupA).isEmpty())
        // Словарь целиком группу не потерял.
        assertEquals(1, quizIds(groupId = null).size)
    }

    // === Пины: фильтр в WHERE, до ORDER BY/LIMIT ===

    @Test
    fun earliest_topIsFromGroup_notGlobalTopFiltered() = runBlocking {
        // Глобально самая ранняя — ВНЕ группы.
        addQuizWordCustom("global-oldest", lastSelect = Date(1_000L), errorCount = 0)
        val inGroup = addQuizWordCustom(
            "group-word", lastSelect = Date(2_000L), errorCount = 0, groupA,
        )

        val result = db.wordDao().getEarliest(limit = 1, langId = dictId, groupId = groupA)

        assertEquals(1, result.size)
        assertEquals(inGroup, result.single().lexemeDbWithWordDbRelation.wordDb.id)
    }

    @Test
    fun frequentMistakes_topIsFromGroup_notGlobalTopFiltered() = runBlocking {
        // Глобальный чемпион ошибок — ВНЕ группы.
        addQuizWordCustom("global-mistakes", lastSelect = null, errorCount = 9)
        val inGroup = addQuizWordCustom(
            "group-word", lastSelect = null, errorCount = 1, groupA,
        )

        val result = db.wordDao().getFrequentMistakes(limit = 1, langId = dictId, groupId = groupA)

        assertEquals(1, result.size)
        assertEquals(inGroup, result.single().lexemeDbWithWordDbRelation.wordDb.id)
    }

    // === Счётчики ===

    @Test
    fun groupCounts_level1_semantics() = runBlocking {
        addQuizWord("cat", groupA)
        addQuizWord("dog", groupA)
        addWord("bare", groupA) // без лексемы — не считается
        // groupB — пустая: должна вернуться с нулём.

        val counts = db.wordDao().flowQuizGroupCounts(dictId).first()
            .associate { it.groupId to it.wordCount }

        assertEquals(2, counts[groupA])
        assertEquals(0, counts[groupB])
    }

    @Test
    fun groupCounts_wordWithTwoLexemes_countedOnce() = runBlocking {
        val wordId = addQuizWord("cat", groupA)
        db.wordDao().addLexemeWithQuiz(
            lexemeDb = LexemeDb(wordId = wordId, addDate = now),
            dictionaryId = dictId,
        )

        val counts = db.wordDao().flowQuizGroupCounts(dictId).first()
            .associate { it.groupId to it.wordCount }

        assertEquals(1, counts[groupA])
    }

    @Test
    fun groupCounts_deadGroup_absent() = runBlocking {
        addQuizWord("cat", groupA)
        groupApi.deleteGroup(groupA)

        val counts = db.wordDao().flowQuizGroupCounts(dictId).first()

        assertEquals(listOf(groupB), counts.map { it.groupId })
    }

    @Test
    fun dictionaryQuizWordCount_ignoresWordsWithoutLexemes() = runBlocking {
        addQuizWord("cat")
        addQuizWord("dog", groupA)
        addWord("bare")

        assertEquals(2, db.wordDao().flowDictionaryQuizWordCount(dictId).first())
    }
}
