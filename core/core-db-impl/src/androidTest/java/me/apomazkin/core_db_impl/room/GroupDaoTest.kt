package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.apomazkin.core_db_impl.entity.GroupDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WordGroupDb
import me.apomazkin.core_db_impl.room.dao.MembershipSliceDb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS493: `GroupDao.membershipSlice` + `WordDao.getTermsByIds` (stage2_plan 1.4).
 *
 * Контракт slice (D6.3): LEFT JOIN на живые membership; слово вне живых
 * групп — `(wordId, null)`; порядок id DESC; наблюдение трёх таблиц (А13).
 *
 * Тест переэмита — ПЕРВЫЙ Flow-DAO-тест проекта: без turbine (решение
 * триажа ревью T-3) — фоновый collector + ожидание с таймаутом.
 */
@RunWith(AndroidJUnit4::class)
class GroupDaoTest {

    private lateinit var db: Database

    private var dictId: Long = 0
    private var otherDictId: Long = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        runBlocking {
            dictId = db.wordDao().addDictionary(dictionaryDb("EN"))
            otherDictId = db.wordDao().addDictionary(dictionaryDb("ES"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // === Порядок и null-группы: пустой word_groups ===
    @Test
    fun emptyWordGroups_allWordsWithNullGroup_orderIdDesc() = runBlocking {
        val w1 = addWord("дом")
        val w2 = addWord("кот")
        val w3 = addWord("мир")

        val slice = firstEmission()

        assertEquals(
            listOf(
                MembershipSliceDb(wordId = w3, groupId = null),
                MembershipSliceDb(wordId = w2, groupId = null),
                MembershipSliceDb(wordId = w1, groupId = null),
            ),
            slice,
        )
    }

    // === Слово только в мёртвой группе — не теряется, groupId = null ===
    @Test
    fun deadGroupOnly_wordKeptWithNullGroup() = runBlocking {
        val w1 = addWord("дом")
        val deadGroup = addGroup("Старая", removed = true)
        db.groupDao().insertWordGroup(WordGroupDb(wordId = w1, groupId = deadGroup, createdAt = Date(0)))

        val slice = firstEmission()

        assertEquals(listOf(MembershipSliceDb(wordId = w1, groupId = null)), slice)
    }

    // === Мёртвая + живая: только живая строка, без фантомного null ===
    @Test
    fun deadPlusAliveGroup_onlyAliveRow() = runBlocking {
        val w1 = addWord("дом")
        val deadGroup = addGroup("Старая", removed = true)
        val aliveGroup = addGroup("Дом")
        db.groupDao().insertWordGroup(WordGroupDb(wordId = w1, groupId = deadGroup, createdAt = Date(0)))
        db.groupDao().insertWordGroup(WordGroupDb(wordId = w1, groupId = aliveGroup, createdAt = Date(0)))

        val slice = firstEmission()

        assertEquals(listOf(MembershipSliceDb(wordId = w1, groupId = aliveGroup)), slice)
    }

    // === Мультичленство в живых группах: строка на membership (форма Э3) ===
    @Test
    fun twoAliveGroups_rowPerMembership() = runBlocking {
        val w1 = addWord("дом")
        val g1 = addGroup("Дом")
        val g2 = addGroup("Быт")
        db.groupDao().insertWordGroup(WordGroupDb(wordId = w1, groupId = g1, createdAt = Date(0)))
        db.groupDao().insertWordGroup(WordGroupDb(wordId = w1, groupId = g2, createdAt = Date(0)))

        val slice = firstEmission()

        assertEquals(2, slice.size)
        assertEquals(setOf(g1, g2), slice.map { it.groupId }.toSet())
        assertEquals(setOf(w1), slice.map { it.wordId }.toSet())
    }

    // === Чужой словарь не попадает в срез ===
    @Test
    fun otherDictionary_excluded() = runBlocking {
        val w1 = addWord("дом")
        addWord("gato", dictionary = otherDictId)

        val slice = firstEmission()

        assertEquals(listOf(MembershipSliceDb(wordId = w1, groupId = null)), slice)
    }

    // === Переэмит: добавление слова триггерит новую эмиссию (А13/Р5) ===
    @Test
    fun wordInsert_triggersReemission() {
        runBlocking {
            addWord("дом")

            val emissions = mutableListOf<List<MembershipSliceDb>>()
            val collector = launch(Dispatchers.IO) {
                db.groupDao().membershipSlice(dictId).take(2).collect { emissions.add(it) }
            }
            withTimeout(5_000) {
                while (emissions.isEmpty()) delay(10)
            }
            assertEquals(1, emissions.first().size)

            val w2 = addWord("кот")

            withTimeout(5_000) { collector.join() }
            assertEquals(2, emissions.size)
            // Новое слово первым (id DESC).
            assertEquals(w2, emissions[1].first().wordId)
            assertEquals(2, emissions[1].size)
        }
    }

    // === flowTermsWindow: живое окно — limit + переэмит на вставку ===
    @Test
    fun flowTermsWindow_respectsLimit_reemitsWithNewHeadWord() {
        runBlocking {
            val w1 = addWord("дом")
            val w2 = addWord("кот")
            val w3 = addWord("мир")

            val emissions = mutableListOf<List<Long>>()
            val collector = launch(Dispatchers.IO) {
                db.wordDao().flowTermsWindow(dictionaryId = dictId, limit = 2)
                    .take(2)
                    .collect { terms -> emissions.add(terms.map { it.wordDb.id }) }
            }
            withTimeout(5_000) {
                while (emissions.isEmpty()) delay(10)
            }
            // Окно = первые 2 по id DESC.
            assertEquals(listOf(w3, w2), emissions.first())

            val w4 = addWord("сад")

            withTimeout(5_000) { collector.join() }
            // Вставка в голову вошла в окно сама; w1 так и не в окне (limit).
            assertEquals(listOf(w4, w3), emissions[1])
            check(w1 != w4)
        }
    }

    // === getTermsByIds: порядок id DESC, только запрошенные ===
    @Test
    fun getTermsByIds_orderDesc_onlyRequested() = runBlocking {
        val w1 = addWord("дом")
        addWord("кот")
        val w3 = addWord("мир")

        val terms = db.wordDao().getTermsByIds(listOf(w1, w3))

        assertEquals(listOf(w3, w1), terms.map { it.wordDb.id })
    }

    // === getTermsByIds: пустой список — пустой результат ===
    @Test
    fun getTermsByIds_emptyIds_emptyResult() = runBlocking {
        addWord("дом")

        val terms = db.wordDao().getTermsByIds(emptyList())

        assertEquals(0, terms.size)
    }

    // === Helpers ===

    private fun dictionaryDb(name: String) =
        me.apomazkin.core_db_impl.entity.DictionaryDb(name = name, addDate = Date(0))

    private suspend fun addWord(value: String, dictionary: Long = dictId): Long =
        db.wordDao().addWordSuspend(WordDb(dictionaryId = dictionary, value = value, addDate = Date(0)))

    private suspend fun addGroup(name: String, removed: Boolean = false): Long =
        db.groupDao().insertGroup(
            GroupDb(
                dictionaryId = dictId,
                name = name,
                createdAt = Date(0),
                updatedAt = Date(0),
                removedAt = if (removed) Date(1) else null,
            )
        )

    private suspend fun firstEmission(): List<MembershipSliceDb> {
        var result: List<MembershipSliceDb>? = null
        withTimeout(5_000) {
            db.groupDao().membershipSlice(dictId).take(1).collect { result = it }
        }
        return requireNotNull(result)
    }
}
