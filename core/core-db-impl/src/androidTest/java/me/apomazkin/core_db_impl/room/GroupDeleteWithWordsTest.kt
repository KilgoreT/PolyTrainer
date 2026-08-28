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
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.DeleteGroupWithWordsOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS493 Э6 (stage6_plan Фаза 1): деструктивная транзакция
 * deleteGroupWithWords (D30.2) — hard-delete слов группы + каскады +
 * чанкование + идемпотентность.
 */
@RunWith(AndroidJUnit4::class)
class GroupDeleteWithWordsTest {

    private lateinit var db: Database
    private lateinit var groupApi: CoreDbApiImpl.GroupApiImpl

    private var dictId: Long = 0
    private var groupA: Long = 0
    private var groupB: Long = 0

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
            dictId = db.wordDao().addDictionary(DictionaryDb(name = "EN", addDate = Date(0)))
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

    private suspend fun addWordInGroup(value: String, vararg groups: Long): Long {
        val wordId = db.wordDao().addWordSuspend(
            WordDb(value = value, dictionaryId = dictId, addDate = Date(0))
        )
        groups.forEach { groupApi.addWordToGroup(wordId, it) }
        return wordId
    }

    private suspend fun countWords(): Int =
        db.wordDao().getTermList(dictId.toInt()).size

    private suspend fun countLexemesOf(wordId: Long): Int =
        db.useWriterConnection { transactor ->
            transactor.usePrepared("SELECT COUNT(*) FROM lexemes WHERE word_id = $wordId") { stmt ->
                stmt.step()
                stmt.getLong(0).toInt()
            }
        }

    // === Основной путь ===

    @Test
    fun deleteWithWords_wordsGone_cascadesEverywhere_groupDead() = runBlocking {
        val w1 = addWordInGroup("cat", groupA)
        val w2 = addWordInGroup("dog", groupA, groupB) // и в другой группе
        addWordInGroup("fox", groupB) // чужое слово — выживает
        // Лексема у удаляемого слова — глубина каскада (ревью Data-3).
        db.wordDao().addLexeme(LexemeDb(wordId = w1, addDate = Date(0)))

        val outcome = groupApi.deleteGroupWithWords(groupA)

        assertEquals(DeleteGroupWithWordsOutcome.Success(deletedWords = 2), outcome)
        // Слова группы A удалены из words; чужое живо.
        assertEquals(1, countWords())
        // Каскад глубины: лексем удалённого слова нет.
        assertEquals(0, countLexemesOf(w1))
        // Membership удалённых слов вычищен и в ДРУГОЙ группе:
        // в B осталось только fox.
        assertEquals(1, db.groupDao().countWordGroups(groupB))
        assertEquals(0, db.groupDao().countWordGroups(groupA))
        // Группа мертва.
        assertEquals(null, db.groupDao().getLivingGroupById(groupA))
        // Живые потоки согласованы: окно B без dog.
        val windowB = groupApi.flowGroupWordsWindow(groupB, limit = 10).first()
        assertEquals(listOf("fox"), windowB.map { it.word.value })
        // w2 больше нигде.
        assertTrue(groupApi.wordGroups(w2).first().isEmpty())
    }

    // === NotFound: слова не тронуты ===

    @Test
    fun deleteWithWords_deadGroup_notFound_wordsUntouched() = runBlocking {
        addWordInGroup("cat", groupA)
        groupApi.deleteGroup(groupA)

        val outcome = groupApi.deleteGroupWithWords(groupA)

        assertEquals(DeleteGroupWithWordsOutcome.NotFound, outcome)
        assertEquals(1, countWords())
    }

    // === Пустая группа ===

    @Test
    fun deleteWithWords_emptyGroup_successZero() = runBlocking {
        val outcome = groupApi.deleteGroupWithWords(groupA)

        assertEquals(DeleteGroupWithWordsOutcome.Success(deletedWords = 0), outcome)
        assertEquals(null, db.groupDao().getLivingGroupById(groupA))
    }

    // === Чанкование (ревью Data-2): граница пересекается дважды ===

    @Test
    fun deleteWithWords_chunked_allDeletedAtomically() = runBlocking {
        repeat(8) { addWordInGroup("w$it", groupA) }

        val outcome = groupApi.deleteGroupWithWords(groupA, chunkSize = 3)

        assertEquals(DeleteGroupWithWordsOutcome.Success(deletedWords = 8), outcome)
        assertEquals(0, countWords())
        assertEquals(0, db.groupDao().countWordGroups(groupA))
    }

    // Кейс legacy-samples удалён 2026-08-29: samples мигрированы в builtin
    // «Пример» (Migration_011_to_012) и живут в component_values — их
    // уносит обычный FK CASCADE lexeme_id (покрыт кейсом каскада лексем).
}
