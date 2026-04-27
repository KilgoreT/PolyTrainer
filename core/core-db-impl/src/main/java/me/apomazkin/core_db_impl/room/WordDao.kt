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
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.LexemeDbEntity
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
    suspend fun addDictionary(dictionaryDb: DictionaryDb): Long

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

    @Query("UPDATE lexemes SET translation = :translation WHERE id = :id")
    suspend fun updateLexemeTranslation(id: Long, translation: String?): Int

    @Query("UPDATE lexemes SET definition = :definition WHERE id = :id")
    suspend fun updateLexemeDefinition(id: Long, definition: String?): Int

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

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateWriteQuiz(writeQuizDb: List<WriteQuizDb>): Int

    @Transaction
    @Query("SELECT * from write_quiz  WHERE grade = :grade AND dictionary_id = :langId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWriteQuizList(
        grade: Int,
        limit: Int,
        langId: Long
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
