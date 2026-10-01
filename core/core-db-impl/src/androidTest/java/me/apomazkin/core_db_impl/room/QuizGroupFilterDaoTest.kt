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
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.lexeme.ComponentTemplate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Фильтры квиз-выборок: групповой (IS500) и по ядрам (IS511), счётчики пикера.
 *
 * Контракты:
 * - `getWriteQuizIds`/`getEarliest`/`getFrequentMistakes` с
 *   `groupId != null` возвращают ТОЛЬКО квиз-записи слов живой группы;
 *   null — весь словарь;
 * - те же запросы отдают только лексемы с живым значением хотя бы в
 *   одном из `coreTypeIds`; удалённое значение не считается;
 * - оба фильтра применяются в WHERE до ORDER BY/LIMIT: топ считается
 *   ИЗ подходящих, а не пересечением глобального топа с фильтром (пины);
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

    /** Два ядра словаря: встроенный перевод и пользовательское «Определение». */
    private var translationType: Long = 0
    private var definitionType: Long = 0

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
            translationType = db.componentTypeDao().insert(
                coreTypeDb(systemKey = "translation", name = null, position = 0),
            )
            definitionType = db.componentTypeDao().insert(
                coreTypeDb(systemKey = null, name = "Определение", position = 1),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun coreTypeDb(systemKey: String?, name: String?, position: Int) = ComponentTypeDb(
        systemKey = systemKey,
        dictionaryId = dictId,
        name = name,
        templateKey = ComponentTemplate.TEXT.key,
        position = position,
        core = true,
        enabled = true,
        createdAt = now,
        updatedAt = now,
    )

    private suspend fun createGroup(name: String): Long =
        (groupApi.createGroup(dictId, name) as CreateGroupOutcome.Success).groupId

    private suspend fun addWord(value: String, vararg groups: Long): Long {
        val wordId = db.wordDao().addWordSuspend(
            WordDb(value = value, dictionaryId = dictId, addDate = now),
        )
        groups.forEach { groupApi.addWordToGroup(wordId, it) }
        return wordId
    }

    /** Живое (или удалённое) значение ядра у лексемы. */
    private suspend fun addValue(lexemeId: Long, typeId: Long, removedAt: Date? = null) {
        db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId,
                componentTypeId = typeId,
                value = """{"fields":{"value":{"type":"text","value":"x"}}}""",
                createdAt = now,
                updatedAt = now,
                removedAt = removedAt,
            ),
        )
    }

    /**
     * Слово + лексема + квиз-строка (инвариант «лексема ⇔ write_quiz») +
     * живой перевод, чтобы фильтр по ядрам слово пропускал.
     */
    private suspend fun addQuizWord(value: String, vararg groups: Long): Long {
        val wordId = addWord(value, *groups)
        val lexemeId = db.wordDao().addLexemeWithQuiz(
            lexemeDb = LexemeDb(wordId = wordId, addDate = now),
            dictionaryId = dictId,
        )
        addValue(lexemeId, translationType)
        return wordId
    }

    /** Слово с квиз-строкой и значениями ровно в указанных ядрах. */
    private suspend fun addQuizWordWithCores(value: String, vararg typeIds: Long): Long {
        val wordId = addWord(value)
        val lexemeId = db.wordDao().addLexemeWithQuiz(
            lexemeDb = LexemeDb(wordId = wordId, addDate = now),
            dictionaryId = dictId,
        )
        typeIds.forEach { addValue(lexemeId, it) }
        return wordId
    }

    /** Квиз-строка с ручными полями (для пинов earliest/mistakes) и живым переводом. */
    private suspend fun addQuizWordCustom(
        value: String,
        lastSelect: Date?,
        errorCount: Int,
        vararg groups: Long,
        coreType: Long = translationType,
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
        addValue(lexemeId, coreType)
        return wordId
    }

    /**
     * Хелперы повторяют прод-путь `CoreDbApiImpl`: пустой набор групп —
     * `allGroups = true` и пустой `IN ()` (не читается); иначе — набор.
     */
    private suspend fun quizIds(
        groupId: Long?,
        cores: List<Long> = listOf(translationType),
        groups: List<Long> = listOfNotNull(groupId),
    ): List<Long> = db.wordDao().getWriteQuizIds(
        grade = 0,
        langId = dictId,
        allGroups = groups.isEmpty(),
        groupIds = groups,
        coreTypeIds = cores,
    )

    private suspend fun earliest(
        limit: Int,
        groupId: Long?,
        cores: List<Long> = listOf(translationType),
        groups: List<Long> = listOfNotNull(groupId),
    ) = db.wordDao().getEarliest(
        limit = limit,
        langId = dictId,
        allGroups = groups.isEmpty(),
        groupIds = groups,
        coreTypeIds = cores,
    )

    private suspend fun frequentMistakes(
        limit: Int,
        groupId: Long?,
        cores: List<Long> = listOf(translationType),
        groups: List<Long> = listOfNotNull(groupId),
    ) = db.wordDao().getFrequentMistakes(
        limit = limit,
        langId = dictId,
        allGroups = groups.isEmpty(),
        groupIds = groups,
        coreTypeIds = cores,
    )

    // === getWriteQuizIds: групповой фильтр ===

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

    // === getWriteQuizIds: набор групп ===

    @Test
    fun groupSet_union_ofSelectedGroups() = runBlocking {
        addQuizWord("cat", groupA)
        addQuizWord("dog", groupB)
        addQuizWord("fox") // вне групп — в объединение не входит

        assertEquals(2, quizIds(groupId = null, groups = listOf(groupA, groupB)).size)
    }

    @Test
    fun groupSet_wordInBothGroups_singleRow() = runBlocking {
        addQuizWord("cat", groupA, groupB)

        assertEquals(1, quizIds(groupId = null, groups = listOf(groupA, groupB)).size)
    }

    @Test
    fun groupSet_emptyMeansWholeDictionary() = runBlocking {
        addQuizWord("cat", groupA)
        addQuizWord("fox") // вне групп

        assertEquals(2, quizIds(groupId = null, groups = emptyList()).size)
    }

    @Test
    fun groupSet_deadGroupInSet_ignored() = runBlocking {
        addQuizWord("cat", groupA)
        addQuizWord("dog", groupB)
        groupApi.deleteGroup(groupB)

        assertEquals(1, quizIds(groupId = null, groups = listOf(groupA, groupB)).size)
    }

    @Test
    fun groupSet_earliestTopFromUnion_notGlobalTop() = runBlocking {
        // Глобально самая ранняя — вне набора.
        addQuizWordCustom("global-oldest", lastSelect = Date(1_000L), errorCount = 0)
        val inB = addQuizWordCustom("b-word", lastSelect = Date(2_000L), errorCount = 0, groupB)
        addQuizWordCustom("a-word", lastSelect = Date(3_000L), errorCount = 0, groupA)

        val result = earliest(limit = 1, groupId = null, groups = listOf(groupA, groupB))

        assertEquals(listOf(inB), result.map { it.lexemeDbWithWordDbRelation.wordDb.id })
    }

    @Test
    fun groupSet_mistakesTopFromUnion_notGlobalTop() = runBlocking {
        // Глобальный чемпион ошибок — вне набора.
        addQuizWordCustom("global-mistakes", lastSelect = null, errorCount = 9)
        val inA = addQuizWordCustom("a-word", lastSelect = null, errorCount = 3, groupA)
        addQuizWordCustom("b-word", lastSelect = null, errorCount = 1, groupB)

        val result = frequentMistakes(limit = 1, groupId = null, groups = listOf(groupA, groupB))

        assertEquals(listOf(inA), result.map { it.lexemeDbWithWordDbRelation.wordDb.id })
    }

    // === getWriteQuizIds: фильтр по ядрам ===

    @Test
    fun coreFilter_valueInListedType_included() = runBlocking {
        addQuizWordWithCores("cat", translationType)

        assertEquals(1, quizIds(groupId = null, cores = listOf(translationType)).size)
    }

    @Test
    fun coreFilter_noValueInListedTypes_excluded() = runBlocking {
        addQuizWordWithCores("cat", translationType) // только перевод

        assertTrue(quizIds(groupId = null, cores = listOf(definitionType)).isEmpty())
    }

    @Test
    fun coreFilter_anyOfSeveralCores_included() = runBlocking {
        addQuizWordWithCores("cat", translationType)
        addQuizWordWithCores("dog", definitionType)
        addQuizWordWithCores("bare") // без значений — нет ни в одном

        val both = listOf(translationType, definitionType)
        assertEquals(2, quizIds(groupId = null, cores = both).size)
        assertEquals(1, quizIds(groupId = null, cores = listOf(definitionType)).size)
    }

    @Test
    fun coreFilter_removedValue_notCounted() = runBlocking {
        val wordId = addWord("cat")
        val lexemeId = db.wordDao().addLexemeWithQuiz(
            lexemeDb = LexemeDb(wordId = wordId, addDate = now),
            dictionaryId = dictId,
        )
        addValue(lexemeId, translationType, removedAt = Date(1_000L))

        assertTrue(quizIds(groupId = null, cores = listOf(translationType)).isEmpty())
    }

    @Test
    fun coreFilter_combinesWithGroupFilter() = runBlocking {
        addQuizWord("cat", groupA) // перевод, в группе
        val wordId = addWord("dog", groupA) // определение, в группе
        val lexemeId = db.wordDao().addLexemeWithQuiz(
            lexemeDb = LexemeDb(wordId = wordId, addDate = now),
            dictionaryId = dictId,
        )
        addValue(lexemeId, definitionType)
        addQuizWordWithCores("fox", definitionType) // определение, вне группы

        assertEquals(1, quizIds(groupA, cores = listOf(definitionType)).size)
        assertEquals(2, quizIds(groupId = null, cores = listOf(definitionType)).size)
    }

    // === Пины: фильтры в WHERE, до ORDER BY/LIMIT ===

    @Test
    fun earliest_topIsFromGroup_notGlobalTopFiltered() = runBlocking {
        // Глобально самая ранняя — ВНЕ группы.
        addQuizWordCustom("global-oldest", lastSelect = Date(1_000L), errorCount = 0)
        val inGroup = addQuizWordCustom(
            "group-word", lastSelect = Date(2_000L), errorCount = 0, groupA,
        )

        val result = earliest(limit = 1, groupId = groupA)

        assertEquals(1, result.size)
        assertEquals(inGroup, result.single().lexemeDbWithWordDbRelation.wordDb.id)
    }

    @Test
    fun earliest_topIsFromCores_notGlobalTopFiltered() = runBlocking {
        // Глобально самая ранняя — без определения.
        addQuizWordCustom("global-oldest", lastSelect = Date(1_000L), errorCount = 0)
        val withDefinition = addQuizWordCustom(
            "defined", lastSelect = Date(2_000L), errorCount = 0, coreType = definitionType,
        )

        val result = earliest(limit = 1, groupId = null, cores = listOf(definitionType))

        assertEquals(1, result.size)
        assertEquals(withDefinition, result.single().lexemeDbWithWordDbRelation.wordDb.id)
    }

    @Test
    fun frequentMistakes_topIsFromGroup_notGlobalTopFiltered() = runBlocking {
        // Глобальный чемпион ошибок — ВНЕ группы.
        addQuizWordCustom("global-mistakes", lastSelect = null, errorCount = 9)
        val inGroup = addQuizWordCustom(
            "group-word", lastSelect = null, errorCount = 1, groupA,
        )

        val result = frequentMistakes(limit = 1, groupId = groupA)

        assertEquals(1, result.size)
        assertEquals(inGroup, result.single().lexemeDbWithWordDbRelation.wordDb.id)
    }

    @Test
    fun frequentMistakes_topIsFromCores_notGlobalTopFiltered() = runBlocking {
        // Глобальный чемпион ошибок — без определения.
        addQuizWordCustom("global-mistakes", lastSelect = null, errorCount = 9)
        val withDefinition = addQuizWordCustom(
            "defined", lastSelect = null, errorCount = 1, coreType = definitionType,
        )

        val result = frequentMistakes(limit = 1, groupId = null, cores = listOf(definitionType))

        assertEquals(1, result.size)
        assertEquals(withDefinition, result.single().lexemeDbWithWordDbRelation.wordDb.id)
    }

    @Test
    fun frequentMistakes_zeroErrors_excluded() = runBlocking {
        // IS508 (Д6): «частые ошибки» — только строки с ошибками.
        addQuizWordCustom("no-mistakes", lastSelect = null, errorCount = 0)
        val withMistakes = addQuizWordCustom("mistaken", lastSelect = null, errorCount = 1)

        val result = frequentMistakes(limit = 10, groupId = null)

        assertEquals(listOf(withMistakes), result.map { it.lexemeDbWithWordDbRelation.wordDb.id })
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
