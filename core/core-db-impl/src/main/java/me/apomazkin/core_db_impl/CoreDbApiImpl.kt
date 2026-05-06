package me.apomazkin.core_db_impl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.entity.toApiEntity
import me.apomazkin.core_db_impl.entity.toDb
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.WordDao
import java.util.Date
import javax.inject.Inject


class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao,
) : CoreDbApi {

    class DbInstanceImpl @Inject constructor(
        private val db: Database,
    ) : CoreDbApi.DbInstance {

        override suspend fun instance(): String {
            return System.identityHashCode(db).toString()
        }

        override suspend fun closeDatabase() {
            withContext(Dispatchers.IO) {
                db.close()
            }
        }

        override suspend fun openDatabase() {
            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.isOpen
            }
        }

        override suspend fun isDatabaseOpen(): Boolean {
            return db.isOpen
        }

        override fun getDbInfo(): CoreDbApi.DbInfo {
            val dbName: String? = db.openHelper.databaseName
            val dbVersion: Int = db.openHelper.readableDatabase.version
            val dbPath: String? = db.openHelper.readableDatabase.path
            val isOpen: Boolean = db.isOpen
            return CoreDbApi.DbInfo(
                mem = System.identityHashCode(db).toString(),
                name = dbName ?: "",
                version = dbVersion,
                path = dbPath ?: "",
                isOpen = isOpen,
            )
        }
    }

    class DictionaryApiImpl @Inject constructor(
        private val wordDao: WordDao,
    ) : CoreDbApi.DictionaryApi {

        override suspend fun addDictionary(name: String, numericCode: Int?): Long {
            val currentDate = Date(System.currentTimeMillis())
            return wordDao.addDictionary(
                DictionaryDb(
                    numericCode = numericCode,
                    name = name,
                    addDate = currentDate,
                )
            )
        }

        override suspend fun getDictionary(numericCode: Int): DictionaryApiEntity? {
            return wordDao.getDictionaryByNumeric(numericCode)
                ?.let { return it.toApiEntity() }
        }

        override suspend fun getDictionaryById(id: Long): DictionaryApiEntity? {
            return wordDao.getDictionaryById(id)?.toApiEntity()
        }

        override suspend fun getDictionaryList(): List<DictionaryApiEntity> {
            return wordDao.getDictionaries().map { it.toApiEntity() }
        }

        override suspend fun updateDictionary(id: Long, name: String, numericCode: Int?) {
            wordDao.updateDictionary(id, name, numericCode, System.currentTimeMillis())
        }

        override suspend fun deleteDictionary(id: Long) {
            wordDao.deleteDictionary(id)
        }

        override fun flowDictionaryList(): Flow<List<DictionaryApiEntity>> {
            return wordDao.flowDictionaries().map { it.toApiEntity() }
        }

    }

    class TermApiImpl @Inject constructor(
        private val wordDao: WordDao,
    ) : CoreDbApi.TermApi {

        override suspend fun getTermList(dictionaryId: Int): List<TermApiEntity> {
            return wordDao.getTermList(dictionaryId).map { it.toApiEntity() }
        }

        override suspend fun searchTerms(
            pattern: String,
            dictionaryId: Long,
        ): List<TermApiEntity> {
            return wordDao.searchTerms(pattern, dictionaryId).map { it.toApiEntity() }
        }

        override fun searchTermsPaging(
            pattern: String,
            dictionaryId: Int,
        ): Flow<PagingData<TermApiEntity>> {
            return Pager(
                config = PagingConfig(
                    pageSize = 50,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    wordDao.searchTermsPaging(pattern, dictionaryId)
                }
            ).flow.map { pagingData ->
                pagingData.map {
                    it.toApiEntity()
                }
            }
        }

        override suspend fun getTermById(id: Long): TermApiEntity? {
            return wordDao.getTermById(id = id)?.toApiEntity()
        }
    }

    class WordApiImpl @Inject constructor(
        private val wordDao: WordDao,
    ) : CoreDbApi.WordApi {
        override fun addWordSuspend(value: String, dictionaryId: Int): Long {
            val currentDate = Date(System.currentTimeMillis())
            return wordDao.addWordSuspend(
                WordDb(
                    value = value,
                    dictionaryId = dictionaryId.toLong(),
                    addDate = currentDate,
                )
            )
        }

        //TODO kilg 30.06.2025 03:42 проверить рекурсив.
        // может можно упроситить
        override suspend fun deleteWordSuspend(id: Long): Int {
            wordDao.getWordSuspend(id).also { word ->
                wordDao.removeSampleSuspend(
                    *word.lexemeListDb
                        .map { it.sampleDbList }
                        .flatten()
                        .toTypedArray()
                )
                wordDao.deleteDefinitionsSuspend(
                    *word.lexemeListDb.map { it.lexemeDb }.toTypedArray()
                )
                return wordDao.removeWordSuspend(id)
            }
        }

        override suspend fun updateWordSuspend(id: Long, value: String): Boolean {
            val wordRel = wordDao.getWordSuspend(id)
            val wordDb = wordRel.wordDb.copy(value = value)
            return wordDao.updateWorldSuspend(wordDb) == 1
        }

    }

    class LexemeApiImpl @Inject constructor(
        private val wordDao: WordDao,
    ) : CoreDbApi.LexemeApi {

        override suspend fun getLexemeById(id: Long): LexemeApiEntity? {
            return wordDao.getLexemeById(id)?.toApiEntity()
        }

        override suspend fun addLexeme(wordId: Long): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    addDate = date,
                )
            )
        }

        override suspend fun addLexeme(
            wordId: Long,
            translation: TranslationApiEntity,
        ): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    translation = translation.value,
                    addDate = date,
                )
            )
        }

        override suspend fun addLexeme(
            wordId: Long,
            definition: DefinitionApiEntity,
        ): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    definition = definition.value,
                    addDate = date,
                )
            )
        }

        override suspend fun updateLexemeTranslation(
            id: Long,
            translation: TranslationApiEntity?,
        ): Long? {
            val updatedRows =
                wordDao.updateLexemeTranslation(id, translation?.value)
            return if (updatedRows > 0) id else null
        }

        override suspend fun updateLexemeDefinition(
            id: Long,
            definition: DefinitionApiEntity?,
        ): Long? {
            val updatedRows =
                wordDao.updateLexemeDefinition(id, definition?.value)
            return if (updatedRows > 0) id else null
        }

        override suspend fun deleteLexeme(id: Long): Int {
            return wordDao.deleteLexemeById(id)
        }
    }

    class QuizApiImpl @Inject constructor(
        private val wordDao: WordDao,
    ) : CoreDbApi.QuizApi {

        override suspend fun addWriteQuiz(
            dictionaryId: Long,
            lexemeId: Long,
        ): Long {
            return wordDao.addWriteQuiz(
                WriteQuizDb.create(
                    dictionaryId = dictionaryId,
                    lexemeId = lexemeId
                )
            )
        }

        override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertApiEntity>): Int {
            return wordDao.updateWriteQuiz(entity.toDb())
        }

        override suspend fun getRandomWriteQuizList(
            grade: Int,
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity> {
            return wordDao.getRandomWriteQuizList(
                langId = dictionaryId,
                grade = grade,
                limit = limit,
            ).map { it.toApiEntity() }
        }

        override suspend fun getEarliestWriteQuizList(
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity> {
            return wordDao.getEarliest(
                langId = dictionaryId,
                limit = limit,
            ).map { it.toApiEntity() }
        }

        override suspend fun getFrequentMistakesWriteQuizList(
            limit: Int,
            dictionaryId: Long
        ): List<WriteQuizComplexEntity> {
            return wordDao.getFrequentMistakes(
                langId = dictionaryId,
                limit = limit,
            ).map { it.toApiEntity() }
        }
    }

    class StatisticApiImpl @Inject constructor(
        private val wordDao: WordDao
    ): CoreDbApi.StatisticApi {

        override fun flowWordCount(dictionaryId: Int): Flow<Int> =
            wordDao.flowWordCount(langId = dictionaryId)

        override fun flowLexemeCount(dictionaryId: Int): Flow<Int> =
            wordDao.flowLexemeCount(langId = dictionaryId)

        override fun flowQuizCount(dictionaryId: Int, maxGrade: Int): Flow<Map<Int, Int>> {
            val gradeRange = 0..maxGrade
            val flows: List<Flow<Pair<Int, Int>>> = gradeRange.map { grade ->
                wordDao.flowQuizCount(dictionaryId, grade).map { count -> grade to count }
            }

            return combine(flows) { pairs: Array<Pair<Int, Int>> ->
                pairs.toMap()
            }
        }

    }
}