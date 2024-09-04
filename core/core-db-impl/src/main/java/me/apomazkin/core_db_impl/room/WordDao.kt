package me.apomazkin.core_db_impl.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.*

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
    @Query("SELECT * FROM words WHERE word LIKE :pattern AND langId = :langId ORDER BY id DESC")
    fun searchTerms(pattern: String, langId: Long): Observable<List<WordDefinitionRel>>

    /**
     * DEFINITION
     */
    @Insert
    fun addDefinition(definitionDb: DefinitionDb): Single<Long>

    @Insert
    suspend fun addDefinitionSuspend(definitionDb: DefinitionDb): Long

    @Update
    suspend fun updateDefinitionSuspend(definitionDb: DefinitionDb): Int

    @Query("UPDATE definitions SET definition = :value WHERE id = :id")
    suspend fun updateLexemeDefinition(id: Long, value: String): Int

    @Query("UPDATE definitions SET wordClass = :value WHERE id = :id")
    suspend fun updateLexemeCategory(id: Long, value: String): Int

    @Query("SELECT * FROM definitions")
    fun getAllDefinition(): Single<List<DefinitionDb>>

    @Query("SELECT * FROM definitions WHERE id = :id")
    fun getDefinitionById(id: Long): Single<DefinitionDb>

    @Query("SELECT * FROM definitions WHERE wordId = :wordId")
    fun getDefinitionListByWordId(wordId: Long): Single<List<DefinitionDb>>

    @Update
    fun updateDefinition(definitionDb: DefinitionDb): Completable

    @Query("DELETE FROM definitions WHERE id = :id")
    fun deleteDefinition(vararg id: Long): Completable

    @Query("DELETE FROM definitions WHERE id = :id")
    suspend fun deleteDefinitionSuspend(vararg id: Long): Int

    @Delete
    fun deleteDefinitions(vararg definition: DefinitionDb): Completable

    @Delete
    suspend fun deleteDefinitionsSuspend(vararg definition: DefinitionDb)

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

    @Query("SELECT * FROM hint")
    fun getAllHint(): Single<List<HintDb>>

    @Query("SELECT * FROM hint WHERE definitionId = :definitionId")
    fun getHintListByDefinitionId(definitionId: Long): Single<HintDb>

    @Update
    fun updateHint(hintDb: HintDb): Completable

    @Query("DELETE FROM hint WHERE id = :id")
    fun removeHint(id: Long): Completable

    @Delete
    fun removeHint(hintDb: HintDb): Completable

    /**
     * SAMPLE
     */
    @Insert
    fun addSample(sampleDb: SampleDb): Completable

    @Query("SELECT * FROM sample WHERE definitionId = :definitionId")
    fun getSampleListByDefinitionId(definitionId: Long): Single<List<SampleDb>>

    @Query("SELECT * FROM sample")
    fun getAllSample(): Single<List<SampleDb>>

    @Query("SELECT * FROM sample")
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

    @Query("SELECT COUNT(*) FROM definitions")
    fun getDefinitionCount(): Single<Int>

    @Query("SELECT COUNT(*) FROM definitions WHERE wordClass = :wordClass")
    fun getDefinitionTypeCount(wordClass: String): Single<Int>

    @Query("SELECT COUNT(*) FROM writeQuiz WHERE grade = :tier AND langId = :langId")
    fun getWriteQuizCountByGrade(tier: Int, langId: Long): Single<Int>

}