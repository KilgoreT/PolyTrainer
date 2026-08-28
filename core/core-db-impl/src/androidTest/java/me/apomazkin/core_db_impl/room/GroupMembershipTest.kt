package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.apomazkin.core_db_api.entity.ReservedGroupNames
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.group.AddMembershipOutcome
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.RemoveMembershipOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS493 Э5 (stage5_plan Фаза 1): membership-мутации + живые потоки
 * wordGroups / flowGroupWordsWindow (`GroupApiImpl`, D20).
 * Инстанцирование — ручная сборка inner-класса (образец
 * GroupMutationsTest), БЕЗ Dagger.
 */
@RunWith(AndroidJUnit4::class)
class GroupMembershipTest {

    private lateinit var db: Database
    private lateinit var groupApi: CoreDbApiImpl.GroupApiImpl

    private var dictId: Long = 0
    private var otherDictId: Long = 0
    private var wordId: Long = 0
    private var groupId: Long = 0

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
            otherDictId = db.wordDao().addDictionary(DictionaryDb(name = "DE", addDate = Date(0)))
            wordId = addWord("cat")
            groupId = createGroup("Дом")
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun addWord(value: String, dict: Long = dictId): Long =
        db.wordDao().addWordSuspend(
            WordDb(value = value, dictionaryId = dict, addDate = Date(0))
        )

    private suspend fun createGroup(name: String, dict: Long = dictId): Long =
        (groupApi.createGroup(dict, name) as CreateGroupOutcome.Success).groupId

    private suspend fun <T> awaitFirst(block: suspend () -> T): T =
        withTimeout(5_000) { block() }

    // === add ===

    @Test
    fun add_success_visibleInWordGroupsAndSlice() = runBlocking {
        val outcome = groupApi.addWordToGroup(wordId, groupId)

        assertEquals(AddMembershipOutcome.Added, outcome)
        assertEquals(1, db.groupDao().countWordGroups(groupId))
        // wordGroups эмитит живую группу.
        val groups = awaitFirst { groupApi.wordGroups(wordId).first() }
        assertEquals(listOf(groupId), groups.map { it.id })
        // slice отражает членство (счётчик группы на вкладке).
        val slice = awaitFirst { groupApi.membershipSlice(dictId).first() }
        assertTrue(slice.any { it.wordId == wordId && it.groupId == groupId })
    }

    @Test
    fun add_duplicate_alreadyIn_noExtraRow() = runBlocking {
        groupApi.addWordToGroup(wordId, groupId)

        val second = groupApi.addWordToGroup(wordId, groupId)

        assertEquals(AddMembershipOutcome.AlreadyIn, second)
        assertEquals(1, db.groupDao().countWordGroups(groupId))
    }

    @Test
    fun add_softDeletedGroup_groupNotFound() = runBlocking {
        groupApi.deleteGroup(groupId)

        val outcome = groupApi.addWordToGroup(wordId, groupId)

        assertEquals(AddMembershipOutcome.GroupNotFound, outcome)
        assertEquals(0, db.groupDao().countWordGroups(groupId))
    }

    @Test
    fun add_groupOfForeignDictionary_groupNotFound() = runBlocking {
        val foreignGroup = createGroup("Chaos", dict = otherDictId)

        val outcome = groupApi.addWordToGroup(wordId, foreignGroup)

        assertEquals(AddMembershipOutcome.GroupNotFound, outcome)
        assertEquals(0, db.groupDao().countWordGroups(foreignGroup))
    }

    @Test
    fun add_missingWord_wordNotFound() = runBlocking {
        val outcome = groupApi.addWordToGroup(wordId = 999L, groupId = groupId)

        assertEquals(AddMembershipOutcome.WordNotFound, outcome)
        assertEquals(0, db.groupDao().countWordGroups(groupId))
    }

    // === remove ===

    @Test
    fun remove_success_thenIdempotentNotFound() = runBlocking {
        groupApi.addWordToGroup(wordId, groupId)

        val removed = groupApi.removeWordFromGroup(wordId, groupId)
        val repeat = groupApi.removeWordFromGroup(wordId, groupId)

        assertEquals(RemoveMembershipOutcome.Removed, removed)
        assertEquals(RemoveMembershipOutcome.NotFound, repeat)
        assertEquals(0, db.groupDao().countWordGroups(groupId))
        val groups = awaitFirst { groupApi.wordGroups(wordId).first() }
        assertTrue(groups.isEmpty())
    }

    // === wordGroups: только живые ===

    @Test
    fun wordGroups_excludesDeadGroup_afterDeleteGroup() = runBlocking {
        val secondGroup = createGroup("Быт")
        groupApi.addWordToGroup(wordId, groupId)
        groupApi.addWordToGroup(wordId, secondGroup)

        groupApi.deleteGroup(groupId)

        val groups = awaitFirst { groupApi.wordGroups(wordId).first() }
        assertEquals(listOf(secondGroup), groups.map { it.id })
    }

    // === окно слов группы ===

    @Test
    fun groupWindow_respectsLimit_descOrder_excludesForeignMembership() = runBlocking {
        val w1 = wordId
        val w2 = addWord("dog")
        val w3 = addWord("fox")
        val foreignWord = addWord("owl")
        val otherGroup = createGroup("Быт")
        groupApi.addWordToGroup(w1, groupId)
        groupApi.addWordToGroup(w2, groupId)
        groupApi.addWordToGroup(w3, groupId)
        // Чужой membership: слово ДРУГОЙ группы в окно попадать не должно
        // (мутант без WHERE group_id — ревью Test-4).
        groupApi.addWordToGroup(foreignWord, otherGroup)

        val window = awaitFirst { groupApi.flowGroupWordsWindow(groupId, limit = 2).first() }

        assertEquals(listOf(w3, w2), window.map { it.word.id })
    }

    @Test
    fun groupWindow_reemitsOnAddAndRemove() = runBlocking {
        groupApi.addWordToGroup(wordId, groupId)
        val before = awaitFirst { groupApi.flowGroupWordsWindow(groupId, limit = 10).first() }
        assertEquals(listOf(wordId), before.map { it.word.id })

        val w2 = addWord("dog")
        groupApi.addWordToGroup(w2, groupId)
        val afterAdd = awaitFirst {
            groupApi.flowGroupWordsWindow(groupId, limit = 10)
                .first { rows -> rows.size == 2 }
        }
        assertEquals(listOf(w2, wordId), afterAdd.map { it.word.id })

        groupApi.removeWordFromGroup(wordId, groupId)
        val afterRemove = awaitFirst {
            groupApi.flowGroupWordsWindow(groupId, limit = 10)
                .first { rows -> rows.size == 1 }
        }
        assertEquals(listOf(w2), afterRemove.map { it.word.id })
    }

    @Test
    fun groupWindow_deleteGroupUnderWindow_emitsEmpty() = runBlocking {
        // deleteGroup = soft-delete группы + hard-delete membership
        // атомарно — окно обязано переэмитить пустым (ревью Data-5).
        groupApi.addWordToGroup(wordId, groupId)

        groupApi.deleteGroup(groupId)

        val window = awaitFirst {
            groupApi.flowGroupWordsWindow(groupId, limit = 10)
                .first { rows -> rows.isEmpty() }
        }
        assertTrue(window.isEmpty())
    }

    // === hard-delete слова: каскад FK (ревью Data-1) ===

    @Test
    fun hardDeleteWord_cascadesMembership_flowsReemit() = runBlocking {
        val w2 = addWord("dog")
        groupApi.addWordToGroup(wordId, groupId)
        groupApi.addWordToGroup(w2, groupId)
        assertEquals(2, db.groupDao().countWordGroups(groupId))

        db.wordDao().removeWordSuspend(wordId)

        // FK CASCADE вычистил membership удалённого слова.
        assertEquals(1, db.groupDao().countWordGroups(groupId))
        val window = awaitFirst {
            groupApi.flowGroupWordsWindow(groupId, limit = 10)
                .first { rows -> rows.size == 1 }
        }
        assertEquals(listOf(w2), window.map { it.word.id })
        val groups = awaitFirst { groupApi.wordGroups(wordId).first() }
        assertTrue(groups.isEmpty())
        // Slice больше не содержит удалённого слова.
        val slice = awaitFirst { groupApi.membershipSlice(dictId).first() }
        assertTrue(slice.none { it.wordId == wordId })
    }
}
