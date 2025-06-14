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
import me.apomazkin.core_db_impl.entity.LanguageDb
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
     * Languages
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addLanguage(languageDb: LanguageDb): Long
    
    @Query("SELECT * FROM languages WHERE numericCode = :numericCode")
    suspend fun getLanguageByNumeric(numericCode: Int): LanguageDb?

    @Query("SELECT * FROM languages")
    suspend fun getLanguages(): List<LanguageDb>

    @Query("SELECT * FROM languages")
    fun flowLanguages(): Flow<List<LanguageDb>>

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
    @Query("SELECT * FROM words WHERE lang_id = :langId ORDER BY id DESC")
    suspend fun getTermList(langId: Int): List<TermDbEntity>


    @Transaction
    @Query("SELECT * FROM words WHERE value LIKE :pattern AND lang_id = :langId ORDER BY id DESC")
    suspend fun searchTerms(pattern: String, langId: Long): List<TermDbEntity>

    @Transaction
    @Query("""
        SELECT * FROM words
             WHERE (:pattern = '' OR value LIKE :pattern || '%') 
             AND lang_id = :langId 
             ORDER BY id DESC
    """)
    fun searchTermsPaging(pattern: String, langId: Int): PagingSource<Int, TermDbEntity>

    @Query("""
        SELECT * FROM words
            WHERE (:pattern = '' OR value LIKE :pattern || '%')
            AND lang_id = :langId
            ORDER BY id    
            DESC
            LIMIT :limit 
            OFFSET :offset
    """)
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
    
    @Query("SELECT * from write_quiz  WHERE grade = :grade AND lang_id = :langId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWriteQuizList(
        grade: Int,
        limit: Int,
        langId: Long
    ): List<WriteQuizDbEntity>
    
    @Query("DELETE FROM write_quiz WHERE lexeme_id = :lexemeId")
    fun removeWriteQuiz(lexemeId: Long): Int

}