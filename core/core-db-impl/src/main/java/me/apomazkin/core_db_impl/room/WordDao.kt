package me.apomazkin.core_db_impl.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.LanguageDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.SampleDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WordDefinitionRel
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.entity.WriteQuizDefinitionRel

// TODO: 20.03.2021 переименгвать Dao
@Dao
interface WordDao {

    /**
     * Languages
     */
    @Insert
    fun addLanguage(languageDb: LanguageDb): Completable

    @Query("SELECT * FROM languages")
    fun getLanguages(): Single<List<LanguageDb>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addLanguageSuspend(languageDb: LanguageDb): Long

    @Query("SELECT * FROM languages")
    suspend fun getLanguagesSuspend(): List<LanguageDb>

    @Query("SELECT * FROM languages")
    fun flowLanguages(): Flow<List<LanguageDb>>

    /**
     * WORD
     */
    @Insert
    fun addWord(wordDb: WordDb): Completable

    @Insert
    fun addWordSuspend(wordDb: WordDb): Long

    @Query("SELECT * FROM words WHERE id = :id")
    fun getWordById(id: Long): Single<WordDb>

    @Query("SELECT * FROM words")
    fun getWord(): Single<List<WordDb>>

    @Update
    fun updateWorld(wordDb: WordDb): Completable

    @Update
    fun updateWorldSuspend(wordDb: WordDb): Int

    @Query("DELETE FROM words WHERE id = :id")
    fun removeWord(id: Long): Completable

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun removeWordSuspend(id: Long): Int

    /**
     * TERM
     */
    @Transaction
    @Query("SELECT * FROM words ORDER BY id DESC")
    fun getTermList(): Observable<List<WordDefinitionRel>>

    @Transaction
    @Query("SELECT * FROM words WHERE langId = :langId ORDER BY id DESC")
    suspend fun getTermList(langId: Long): List<WordDefinitionRel>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id ORDER BY id DESC")
    suspend fun getTermById(id: Long): WordDefinitionRel

    @Transaction
    @Query("SELECT * FROM words WHERE value LIKE :pattern AND langId = :langId ORDER BY id DESC")
    fun searchTerms(pattern: String, langId: Long): Observable<List<WordDefinitionRel>>

    /**
     * DEFINITION
     */
    @Insert
    fun addDefinition(lexemeDb: LexemeDb): Single<Long>

    @Insert
    suspend fun addDefinitionSuspend(lexemeDb: LexemeDb): Long

    @Update
    suspend fun updateDefinitionSuspend(lexemeDb: LexemeDb): Int

    @Query("UPDATE lexemes SET definition = :value WHERE id = :id")
    suspend fun updateLexemeDefinition(id: Long, value: String): Int

    @Query("UPDATE lexemes SET wordClass = :value WHERE id = :id")
    suspend fun updateLexemeCategory(id: Long, value: String): Int

    @Query("SELECT * FROM lexemes")
    fun getAllDefinition(): Single<List<LexemeDb>>

    @Query("SELECT * FROM lexemes WHERE id = :id")
    fun getDefinitionById(id: Long): Single<LexemeDb>

    @Query("SELECT * FROM lexemes WHERE wordId = :wordId")
    fun getDefinitionListByWordId(wordId: Long): Single<List<LexemeDb>>

    @Update
    fun updateDefinition(lexemeDb: LexemeDb): Completable

    @Query("DELETE FROM lexemes WHERE id = :id")
    fun deleteDefinition(vararg id: Long): Completable

    @Query("DELETE FROM lexemes WHERE id = :id")
    suspend fun deleteDefinitionSuspend(vararg id: Long): Int

    @Delete
    fun deleteDefinitions(vararg definition: LexemeDb): Completable

    @Delete
    suspend fun deleteDefinitionsSuspend(vararg definition: LexemeDb)

    // TODO: 18.02.2021 used to manual remove definition before removing word
    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    fun getWord(id: Long): Single<WordDefinitionRel>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordSuspend(id: Long): WordDefinitionRel

    /**
     * HINT
     */
    @Insert
    fun addHint(hintDb: HintDb): Completable

    @Query("SELECT * FROM hints")
    fun getAllHint(): Single<List<HintDb>>

    @Query("SELECT * FROM hints WHERE lexemeId = :lexemeId")
    fun getHintListByDefinitionId(lexemeId: Long): Single<HintDb>

    @Update
    fun updateHint(hintDb: HintDb): Completable

    @Query("DELETE FROM hints WHERE id = :id")
    fun removeHint(id: Long): Completable

    @Delete
    fun removeHint(hintDb: HintDb): Completable

    /**
     * SAMPLE
     */
    @Insert
    fun addSample(sampleDb: SampleDb): Completable

    @Query("SELECT * FROM samples WHERE lexemeId = :lexemeId")
    fun getSampleListByDefinitionId(lexemeId: Long): Single<List<SampleDb>>

    @Query("SELECT * FROM samples")
    fun getAllSample(): Single<List<SampleDb>>

    @Query("SELECT * FROM samples")
    fun getSampleList(): Observable<List<SampleDb>>

    @Delete
    fun removeSample(vararg sampleDb: SampleDb): Completable

    @Delete
    suspend fun removeSampleSuspend(vararg sampleDb: SampleDb)

    /**
     * QUIZ
     */
    @Insert
    fun addWriteQuiz(writeQuizDb: WriteQuizDb): Completable

    @Query("SELECT * from writeQuiz")
    fun getAllWriteQuiz(): Single<List<WriteQuizDb>>

    @Query("SELECT * from writeQuiz WHERE langId = :langId")
    fun getWriteQuizList(langId: Long): Single<List<WriteQuizDefinitionRel>>

    @Query("SELECT * from writeQuiz  WHERE langId = :langId LIMIT :limit")
    fun getWriteQuizList(limit: Int, langId: Long): Single<List<WriteQuizDefinitionRel>>

    @Query("SELECT * from writeQuiz  WHERE grade = :grade AND langId = :langId ORDER BY lastSelectDate LIMIT :limit")
    fun getWriteQuizListByAccessTime(
        grade: Int,
        limit: Int,
        langId: Long
    ): Single<List<WriteQuizDefinitionRel>>

    @Query("SELECT * from writeQuiz  WHERE grade = :grade AND langId = :langId ORDER BY RANDOM() LIMIT :limit")
    fun getRandomWriteQuizList(
        grade: Int,
        limit: Int,
        langId: Long
    ): Single<List<WriteQuizDefinitionRel>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateWriteQuiz(writeQuizDb: WriteQuizDb): Completable

    @Query("DELETE FROM writeQuiz WHERE definitionId = :definitionId")
    fun removeWriteQuiz(definitionId: Long): Completable

    /**
     * ANALYTICS
     */
    @Query("SELECT COUNT(*) FROM words WHERE langId = :langId")
    fun getWordCount(langId: Long): Single<Int>

    @Query("SELECT COUNT(*) FROM lexemes")
    fun getDefinitionCount(): Single<Int>

    @Query("SELECT COUNT(*) FROM lexemes WHERE wordClass = :wordClass")
    fun getDefinitionTypeCount(wordClass: String): Single<Int>

    @Query("SELECT COUNT(*) FROM writeQuiz WHERE grade = :tier AND langId = :langId")
    fun getWriteQuizCountByGrade(tier: Int, langId: Long): Single<Int>

}