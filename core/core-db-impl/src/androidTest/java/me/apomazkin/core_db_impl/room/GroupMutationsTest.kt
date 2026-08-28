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
import me.apomazkin.core_db_api.entity.ReservedGroupNames
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WordGroupDb
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.RenameGroupOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS493 Э3: транзакционные мутации групп (`GroupApiImpl`, §2.4) —
 * stage3_plan Фаза 2. Инстанцирование — ручная сборка inner-класса
 * (образец Is486DataLayerTest), БЕЗ Dagger.
 *
 * NFC-кейс — на разложенном «й» (U+0438+U+0306); «е́» непригодно
 * (нет композитной формы, T-4).
 */
@RunWith(AndroidJUnit4::class)
class GroupMutationsTest {

    private lateinit var db: Database
    private lateinit var groupApi: CoreDbApiImpl.GroupApiImpl

    private var dictId: Long = 0

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
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // === create ===

    @Test
    fun create_success_storesNormalizedName() = runBlocking {
        val outcome = groupApi.createGroup(dictId, "  Дом  ")

        assertTrue(outcome is CreateGroupOutcome.Success)
        val stored = db.groupDao().livingGroups(dictId)
        assertEquals(listOf("Дом"), stored.map { it.name })
    }

    @Test
    fun create_duplicateCaseInsensitive_rejected() = runBlocking {
        groupApi.createGroup(dictId, "Дом")

        val outcome = groupApi.createGroup(dictId, "дом")

        assertEquals(CreateGroupOutcome.DuplicateSibling, outcome)
        assertEquals(1, db.groupDao().livingGroups(dictId).size)
    }

    @Test
    fun create_trimmedInputCollides() = runBlocking {
        groupApi.createGroup(dictId, "Дом")

        val outcome = groupApi.createGroup(dictId, " Дом ")

        assertEquals(CreateGroupOutcome.DuplicateSibling, outcome)
    }

    @Test
    fun create_nfc_decomposedCollidesWithComposed_storedComposed() = runBlocking {
        // «Зайки» с разложенным й (и U+0438 + combining breve U+0306) —
        // только эскейпами: литерал в редакторе нормализуется в композитный.
        val decomposed = "Зайки"
        val composed = "Зайки"
        check(decomposed != composed)

        val first = groupApi.createGroup(dictId, decomposed)
        val second = groupApi.createGroup(dictId, composed)

        assertTrue(first is CreateGroupOutcome.Success)
        assertEquals(CreateGroupOutcome.DuplicateSibling, second)
        // Хранимая форма — композитная (NFC при записи).
        assertEquals(composed, db.groupDao().livingGroups(dictId).single().name)
    }

    @Test
    fun create_overSoftDeletedName_success() = runBlocking {
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
        groupApi.deleteGroup(id)

        val outcome = groupApi.createGroup(dictId, "Дом")

        assertTrue("soft-deleted имя не блокирует", outcome is CreateGroupOutcome.Success)
    }

    @Test
    fun create_reserved_rejected() = runBlocking {
        assertEquals(CreateGroupOutcome.ReservedName, groupApi.createGroup(dictId, "все"))
        assertEquals(CreateGroupOutcome.ReservedName, groupApi.createGroup(dictId, " ALL "))
    }

    @Test
    fun create_blank_rejected() = runBlocking {
        assertEquals(CreateGroupOutcome.EmptyName, groupApi.createGroup(dictId, "   "))
    }

    @Test
    fun create_freedByRename_success() = runBlocking {
        // A→B, затем create A — ловит устаревший SELECT сиблингов (T-5).
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
        groupApi.renameGroup(id, "Быт")

        val outcome = groupApi.createGroup(dictId, "Дом")

        assertTrue(outcome is CreateGroupOutcome.Success)
    }

    // === rename ===

    @Test
    fun rename_caseOnlyOfOwnName_success() = runBlocking {
        // Self-exclusion (D-1/T-1): смена регистра собственного имени легальна.
        val id = (groupApi.createGroup(dictId, "дом") as CreateGroupOutcome.Success).groupId

        val outcome = groupApi.renameGroup(id, "ДОМ")

        assertEquals(RenameGroupOutcome.Success, outcome)
        assertEquals("ДОМ", db.groupDao().livingGroups(dictId).single().name)
    }

    @Test
    fun rename_toOccupiedName_rejected() = runBlocking {
        groupApi.createGroup(dictId, "Дом")
        val id = (groupApi.createGroup(dictId, "Быт") as CreateGroupOutcome.Success).groupId

        assertEquals(RenameGroupOutcome.DuplicateSibling, groupApi.renameGroup(id, "дом"))
    }

    @Test
    fun rename_toReserved_rejected() = runBlocking {
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId

        assertEquals(RenameGroupOutcome.ReservedName, groupApi.renameGroup(id, "Все"))
    }

    @Test
    fun rename_missing_notFound() = runBlocking {
        assertEquals(RenameGroupOutcome.NotFound, groupApi.renameGroup(999L, "Дом"))
    }

    @Test
    fun rename_softDeleted_notFound() = runBlocking {
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
        groupApi.deleteGroup(id)

        assertEquals(RenameGroupOutcome.NotFound, groupApi.renameGroup(id, "Быт"))
    }

    // === delete ===

    @Test
    fun delete_success_softDeletesAndCleansOwnMembership() = runBlocking {
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
        val otherId = (groupApi.createGroup(dictId, "Быт") as CreateGroupOutcome.Success).groupId
        val wordId = db.wordDao().addWordSuspend(
            WordDb(dictionaryId = dictId, value = "кот", addDate = Date(0)),
        )
        db.groupDao().insertWordGroup(WordGroupDb(wordId = wordId, groupId = id, createdAt = Date(0)))
        db.groupDao().insertWordGroup(WordGroupDb(wordId = wordId, groupId = otherId, createdAt = Date(0)))

        val outcome = groupApi.deleteGroup(id)

        assertEquals(DeleteGroupOutcome.Success, outcome)
        // Soft-delete: живых с этим id нет, физически строка есть.
        assertEquals(listOf("Быт"), db.groupDao().livingGroups(dictId).map { it.name })
        // Membership удалённой — hard-deleted ФИЗИЧЕСКИ (прямой счётчик
        // таблицы: slice мёртвые группы фильтрует сам и осиротевшую строку
        // не отличает — слепое место, найденное мутационной проверкой М6).
        assertEquals(0, db.groupDao().countWordGroups(id))
        // Membership ЧУЖОЙ группы — пережил (D-6б).
        assertEquals(1, db.groupDao().countWordGroups(otherId))
        val slice = firstSliceEmission()
        assertEquals(listOf(otherId), slice.mapNotNull { it.groupId })
    }

    @Test
    fun delete_repeated_notFound() = runBlocking {
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
        groupApi.deleteGroup(id)

        assertEquals(DeleteGroupOutcome.NotFound, groupApi.deleteGroup(id))
    }

    // === groupTree ===

    @Test
    fun groupTree_reemitsAfterCreateAndDelete() {
        runBlocking {
            val emissions = mutableListOf<List<String>>()
            val collector = launch(Dispatchers.IO) {
                groupApi.groupTree(dictId).take(3).collect { groups ->
                    emissions.add(groups.map { it.name })
                }
            }
            withTimeout(5_000) { while (emissions.isEmpty()) delay(10) }
            assertEquals(emptyList<String>(), emissions[0])

            val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
            withTimeout(5_000) { while (emissions.size < 2) delay(10) }
            assertEquals(listOf("Дом"), emissions[1])

            groupApi.deleteGroup(id)
            withTimeout(5_000) { collector.join() }
            assertEquals(emptyList<String>(), emissions[2])
        }
    }

    // === membershipSlice после deleteGroup ===

    @Test
    fun membershipSlice_wordOfDeletedGroup_fallsBackToNull() = runBlocking {
        val id = (groupApi.createGroup(dictId, "Дом") as CreateGroupOutcome.Success).groupId
        val wordId = db.wordDao().addWordSuspend(
            WordDb(dictionaryId = dictId, value = "кот", addDate = Date(0)),
        )
        db.groupDao().insertWordGroup(WordGroupDb(wordId = wordId, groupId = id, createdAt = Date(0)))

        groupApi.deleteGroup(id)

        val slice = firstSliceEmission()
        assertEquals(1, slice.size)
        assertEquals(wordId, slice.single().wordId)
        assertEquals(null, slice.single().groupId)
    }

    // === Helpers ===

    private suspend fun firstSliceEmission() =
        run {
            var result: List<me.apomazkin.core_db_api.entity.MembershipSliceApiEntity>? = null
            withTimeout(5_000) {
                groupApi.membershipSlice(dictId).take(1).collect { result = it }
            }
            requireNotNull(result)
        }
}
