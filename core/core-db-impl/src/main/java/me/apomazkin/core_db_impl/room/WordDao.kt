package me.apomazkin.core_db_impl.room

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.LexemeDbEntity
import me.apomazkin.core_db_impl.entity.QuizConfigDb
import me.apomazkin.core_db_impl.entity.SampleDb
import me.apomazkin.core_db_impl.entity.TermDbEntity
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.entity.WriteQuizDbEntity

// TODO: 20.03.2021 переименгвать Dao
@Dao
interface WordDao {

    /**
     * Dictionaries
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun _addDictionaryRow(dictionaryDb: DictionaryDb): Long

    @Insert
    suspend fun _addQuizConfigRow(config: QuizConfigDb): Long

    /**
     * IS481 (AGG-4 реверс): atomic INSERT dictionary + default quiz_config row(s)
     * в одной транзакции. F1 invariant — каждый dictionary имеет default config
     * `[BuiltIn(TRANSLATION)]` для `quiz_mode='write'` сразу после создания.
     */
    @Transaction
    suspend fun addDictionary(dictionaryDb: DictionaryDb): Long {
        val newDictionaryId = _addDictionaryRow(dictionaryDb)
        _addQuizConfigRow(
            QuizConfigDb(
                dictionaryId = newDictionaryId,
                quizMode = "write",
                componentRefs = """[{"type":"builtin","key":"translation"}]""",
            )
        )
        return newDictionaryId
    }

    @Query("SELECT * FROM dictionaries WHERE numericCode = :numericCode")
    suspend fun getDictionaryByNumeric(numericCode: Int): DictionaryDb?

    @Query("SELECT * FROM dictionaries")
    suspend fun getDictionaries(): List<DictionaryDb>

    @Query("SELECT * FROM dictionaries WHERE id = :id")
    suspend fun getDictionaryById(id: Long): DictionaryDb?

    @Query("UPDATE dictionaries SET name = :name, numericCode = :numericCode, changeDate = :changeDate WHERE id = :id")
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, changeDate: Long)

    @Query("DELETE FROM dictionaries WHERE id = :id")
    suspend fun deleteDictionary(id: Long)

    @Query("SELECT * FROM dictionaries")
    fun flowDictionaries(): Flow<List<DictionaryDb>>

    /**
     * WORD
     */
    @Insert
    fun addWordSuspend(wordDb: WordDb): Long

    @Update
    fun updateWorldSuspend(wordDb: WordDb): Int

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun removeWordSuspend(id: Long): Int

    /**
     * TERM
     */

    @Transaction
    @Query("SELECT * FROM words WHERE dictionary_id = :langId ORDER BY id DESC")
    suspend fun getTermList(langId: Int): List<TermDbEntity>


    @Transaction
    @Query("SELECT * FROM words WHERE value LIKE :pattern AND dictionary_id = :langId ORDER BY id DESC")
    suspend fun searchTerms(pattern: String, langId: Long): List<TermDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM words
             WHERE (:pattern = '' OR value LIKE :pattern || '%')
             AND dictionary_id = :langId
             ORDER BY id DESC
    """
    )
    fun searchTermsPaging(pattern: String, langId: Int): PagingSource<Int, TermDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM words
            WHERE (:pattern = '' OR value LIKE :pattern || '%')
            AND dictionary_id = :langId
            ORDER BY id
            DESC
            LIMIT :limit
            OFFSET :offset
    """
    )
    suspend fun searchTermsManual(
        pattern: String,
        langId: Int,
        limit: Int,
        offset: Int
    ): List<TermDbEntity>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id ORDER BY id DESC")
    suspend fun getTermById(id: Long): TermDbEntity?

    /**
     * LEXEME
     */
    @Insert
    suspend fun addLexeme(lexemeDb: LexemeDb): Long

    @Update
    suspend fun updateLexeme(lexemeDb: LexemeDb): Int

    @Transaction
    @Query("SELECT * FROM lexemes WHERE id = :id")
    suspend fun getLexemeById(id: Long): LexemeDbEntity?

    @Query("UPDATE lexemes SET word_class = :value WHERE id = :id")
    suspend fun updateLexemeCategory(id: Long, value: String): Int

    @Query("DELETE FROM lexemes WHERE id = :id")
    suspend fun deleteLexemeById(id: Long): Int


    /**
     * Other
     */
    @Query("DELETE FROM lexemes WHERE id = :id")
    suspend fun deleteDefinitionSuspend(vararg id: Long): Int

    @Delete
    suspend fun deleteDefinitionsSuspend(vararg definition: LexemeDb)

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordSuspend(id: Long): TermDbEntity

    /**
     * SAMPLE
     */

    @Delete
    suspend fun removeSampleSuspend(vararg sampleDb: SampleDb)

    /**
     * QUIZ
     */
    @Insert
    suspend fun addWriteQuiz(writeQuizDb: WriteQuizDb): Long

    @Insert
    suspend fun _insertComponentValue(value: ComponentValueDb): Long

    /**
     * Atomic INSERT лексемы + write-quiz записи в одной транзакции.
     * Гарантирует domain-инвариант «у каждой лексемы есть write-quiz».
     */
    @Transaction
    suspend fun addLexemeWithQuiz(lexemeDb: LexemeDb, dictionaryId: Long): Long {
        val newLexemeId = addLexeme(lexemeDb)
        addWriteQuiz(WriteQuizDb.create(dictionaryId = dictionaryId, lexemeId = newLexemeId))
        return newLexemeId
    }

    /**
     * IS481 (MIN-9 + M13): atomic compound INSERT — lexeme + write_quiz +
     * N component_values в одной транзакции. FK violation на любом шаге → rollback
     * всего (regression test IS479 F1 + MIN-9 atomicity).
     *
     * **F171 (M13):** cardinality pre-check (F169 / F170) выполняется в
     * `LexemeApiImpl.addLexemeWithComponents` (Room `@Dao interface` не имеет
     * cross-DAO access — отсюда нельзя вызвать `componentTypeDao.getById(...)`).
     * WordDao выполняет ТОЛЬКО cascading INSERTs.
     *
     * @param components список full `ComponentValueDb` entities (с createdAt/updatedAt).
     *   `lexemeId` будет перезаписан на новый id после INSERT lexeme.
     */
    @Transaction
    suspend fun addLexemeWithComponents(
        lexemeDb: LexemeDb,
        dictionaryId: Long,
        components: List<ComponentValueDb>,
    ): Long {
        val newLexemeId = addLexeme(lexemeDb)
        addWriteQuiz(WriteQuizDb.create(dictionaryId = dictionaryId, lexemeId = newLexemeId))
        components.forEach { cv ->
            _insertComponentValue(cv.copy(lexemeId = newLexemeId))
        }
        return newLexemeId
    }

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateWriteQuiz(writeQuizDb: List<WriteQuizDb>): Int

    @Query("SELECT id from write_quiz WHERE grade = :grade AND dictionary_id = :langId")
    suspend fun getWriteQuizIds(
        grade: Int,
        langId: Long
    ): List<Long>

    @Transaction
    @Query("SELECT * from write_quiz WHERE id IN (:ids)")
    suspend fun getWriteQuizByIds(
        ids: List<Long>
    ): List<WriteQuizDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM write_quiz
        WHERE dictionary_id = :langId
        ORDER BY last_select_date ASC
        LIMIT :limit
    """
    )
    suspend fun getEarliest(
        limit: Int,
        langId: Long
    ): List<WriteQuizDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM write_quiz
        WHERE dictionary_id = :langId
        ORDER BY error_count DESC
        LIMIT :limit
    """
    )
    suspend fun getFrequentMistakes(
        limit: Int,
        langId: Long
    ): List<WriteQuizDbEntity>

    @Query("DELETE FROM write_quiz WHERE lexeme_id = :lexemeId")
    fun removeWriteQuiz(lexemeId: Long): Int

    /**
     * STATISTIC
     */

    @Transaction
    @Query("SELECT COUNT(*) FROM words WHERE dictionary_id = :langId")
    fun flowWordCount(langId: Int): Flow<Int>

    @Transaction
    @Query(
        """
            SELECT COUNT(*)
            FROM lexemes
            INNER JOIN words ON lexemes.word_id = words.id
            WHERE words.dictionary_id = :langId
        """
    )
    fun flowLexemeCount(langId: Int): Flow<Int>


    @Query("SELECT COUNT(*) FROM write_quiz WHERE dictionary_id = :langId AND grade = :grade")
    fun flowQuizCount(langId: Int, grade: Int): Flow<Int>

}
