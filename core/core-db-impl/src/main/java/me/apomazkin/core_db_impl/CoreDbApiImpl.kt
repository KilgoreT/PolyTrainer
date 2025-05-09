package me.apomazkin.core_db_impl

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.LanguageApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.core_db_impl.entity.LanguageDb
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
    private val wordDao: WordDao
) : CoreDbApi {
    
    class DbInstanceImpl @Inject constructor(
        private val db: Database
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
    
    class LangApiImpl @Inject constructor(
        private val wordDao: WordDao
    ) : CoreDbApi.LangApi {
        
        override suspend fun addLang(numericCode: Int, name: String): Long {
            val currentDate = Date(System.currentTimeMillis())
            return wordDao.addLanguage(
                LanguageDb(
                    numericCode = numericCode,
                    code = "",
                    name = name,
                    addDate = currentDate,
                )
            )
        }
        
        override suspend fun getLang(numericCode: Int): LanguageApiEntity? {
            return wordDao.getLanguageByNumeric(numericCode)
                ?.let { return it.toApiEntity() }
        }
        
        override suspend fun getLangList(): List<LanguageApiEntity> {
            return wordDao.getLanguages().map { it.toApiEntity() }
        }
        
        override fun flowLangList(): Flow<List<LanguageApiEntity>> {
            return wordDao.flowLanguages().map { it.toApiEntity() }
        }
        
    }
    
    class TermApiImpl @Inject constructor(
        private val wordDao: WordDao
    ) : CoreDbApi.TermApi {
        
        override suspend fun getTermList(langId: Int): List<TermApiEntity> {
            return wordDao.getTermList(langId).map { it.toApiEntity() }
        }
        
        override suspend fun searchTerms(
            pattern: String,
            langId: Long
        ): List<TermApiEntity> {
            return wordDao.searchTerms(pattern, langId).map { it.toApiEntity() }
        }
        
        override suspend fun getTermById(id: Long): TermApiEntity? {
            return wordDao.getTermById(id = id)?.toApiEntity()
        }
    }
    
    class LexemeApiImpl @Inject constructor(
        private val wordDao: WordDao
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
            ).also {
                Log.d("###", "<CoreDbApiImpl.kt>::addLexeme => id: $it")
            }
        }
        
        override suspend fun addLexeme(
            wordId: Long,
            translation: TranslationApiEntity
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
            definition: DefinitionApiEntity
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
            translation: TranslationApiEntity?
        ): Long? {
            val updatedRows =
                wordDao.updateLexemeTranslation(id, translation?.value)
            return if (updatedRows > 0) id else null
        }
        
        override suspend fun updateLexemeDefinition(
            id: Long,
            definition: DefinitionApiEntity?
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
        private val wordDao: WordDao
    ) : CoreDbApi.QuizApi {
        
        override suspend fun addWriteQuiz(
            langId: Long,
            lexemeId: Long
        ): Long {
            return wordDao.addWriteQuiz(
                WriteQuizDb.create(
                    langId = langId,
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
            langId: Long
        ): List<WriteQuizComplexEntity> {
            return wordDao.getRandomWriteQuizList(
                langId = langId,
                grade = grade,
                limit = limit,
            ).map { it.toApiEntity() }
        }
    }
    
    
    override fun addWordSuspend(value: String, langId: Int): Long {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addWordSuspend(
            WordDb(
                value = value,
                langId = langId.toLong(),
                addDate = currentDate,
            )
        )
    }
    
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
    
    override suspend fun addLexemeSuspend(
        wordId: Long,
        category: String,
        definition: String,
    ): Long {
        // TODO: Не забыть добавить WriteQuizDb
        val lexemeDb = LexemeDb(
            wordId = wordId,
            definition = definition,
            wordClass = category,
            options = 0L,
            addDate = Date(System.currentTimeMillis())
        )
        return wordDao.addLexeme(lexemeDb)
    }
    
    override suspend fun editLexemeSuspend(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ): Int {
        val lexemeDb = LexemeDb(
            id = lexemeId,
            wordId = wordId,
            definition = definition,
            wordClass = category,
            options = 0L,
            addDate = Date(System.currentTimeMillis())
        )
        return wordDao.updateLexeme(lexemeDb)
    }
}