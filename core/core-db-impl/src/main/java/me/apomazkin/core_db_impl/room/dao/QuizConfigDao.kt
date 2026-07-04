package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.QuizConfigDb

/**
 * DAO для `quiz_configs`. IS481.
 *
 * `insertDefaultQuizConfig` (MIN-8) — простой method, пишет hardcoded JSON
 * default config `[{"type":"builtin","key":"translation"}]`. Без передачи
 * доменного `QuizConfig` объекта (write-mapper не нужен в IS481).
 */
@Dao
interface QuizConfigDao {

    @Query("SELECT * FROM quiz_configs WHERE dictionary_id = :dictId AND quiz_mode = :mode")
    suspend fun getByDictionaryAndMode(dictId: Long, mode: String): QuizConfigDb?

    @Query("SELECT * FROM quiz_configs WHERE dictionary_id = :dictId")
    suspend fun getByDictionary(dictId: Long): List<QuizConfigDb>

    @Insert
    suspend fun insert(config: QuizConfigDb): Long

    @Update
    suspend fun update(config: QuizConfigDb)

    /**
     * Все configs — для `previewDeletionImpact.affectedQuizConfigs`
     * (scan + JSON match на UseCase-уровне).
     */
    @Query("SELECT * FROM quiz_configs")
    suspend fun getAllConfigs(): List<QuizConfigDb>

    @Query("SELECT * FROM quiz_configs")
    fun flowAllConfigs(): Flow<List<QuizConfigDb>>

    /**
     * Bulk update `component_refs` — caller собирает новый JSON (через
     * `ComponentTypeRefJson` mapper), DAO просто обновляет. Используется в
     * `LexemeApiImpl.softDeleteComponentType` + `renameComponentType`
     * для cascade очистки/переименования refs.
     */
    @Query("UPDATE quiz_configs SET component_refs = :newRefs WHERE id = :id")
    suspend fun updateComponentRefs(id: Long, newRefs: String)

    /**
     * INSERT default config `[BuiltIn(TRANSLATION)]` для конкретного словаря/режима.
     * Используется в `WordDao.addDictionary` (AGG-4 реверс) внутри транзакции.
     * Hardcoded JSON — без mapper'а и доменного объекта (MIN-8).
     */
    suspend fun insertDefaultQuizConfig(dictionaryId: Long, quizMode: String): Long =
        insert(
            QuizConfigDb(
                dictionaryId = dictionaryId,
                quizMode = quizMode,
                componentRefs = """[{"type":"builtin","key":"translation"}]""",
            )
        )
}
