package me.apomazkin.core_db_impl.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_impl.entity.*

// TODO: 20.03.2021 переименгвать Dao
@Dao
interface WordDao {

    @Insert
    fun addWord(wordDb: WordDb): Completable

    @Query("SELECT * FROM words WHERE id = :id")
    fun getWordById(id: Long): Single<WordDb>

    @Update
    fun updateWorld(wordDb: WordDb): Completable

    @Query("DELETE FROM words WHERE id = :id")
    fun removeWord(id: Long): Completable

    @Transaction
    @Query("SELECT * FROM words ORDER BY id DESC")
    fun getTermList(): Observable<List<TermDb>>

    @Transaction
    @Query("SELECT * FROM words WHERE word LIKE :pattern ORDER BY id DESC")
    fun searchTerms(pattern: String): Observable<List<TermDb>>

    @Insert
    fun addDefinition(definitionDb: DefinitionDb): Single<Long>

    @Query("SELECT * FROM definitions")
    fun getAllDefinition(): Single<List<DefinitionDb>>

    @Query("SELECT * FROM definitions WHERE id = :id")
    fun getDefinitionById(id: Long): Single<DefinitionDb>

    @Query("SELECT * FROM definitions WHERE wordId = :wordId")
    fun getDefinitionListByWordId(wordId: Long): Single<List<DefinitionDb>>

    @Update
    fun updateDefinition(definitionDb: DefinitionDb): Completable

    @Query("DELETE FROM definitions WHERE id = :id")
    fun deleteDefinition(id: Long): Completable

    @Delete
    fun deleteDefinitions(vararg definition: DefinitionDb): Completable

    // TODO: 18.02.2021 used to manual remove definition before removing word
    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    fun getWord(id: Long): Single<TermDb>

    @Query("SELECT COUNT(*) FROM words")
    fun getWordCount(): Single<Int>

    @Query("SELECT COUNT(*) FROM definitions")
    fun getDefinitionCount(): Single<Int>

    @Query("SELECT COUNT(*) FROM definitions WHERE wordClass = :wordClass")
    fun getDefinitionTypeCount(wordClass: String): Single<Int>

    @Insert
    fun addWriteQuiz(writeQuizDb: WriteQuizDb): Completable

    @Query("SELECT * from writeQuiz")
    fun getWriteQuizList(): Single<List<WriteQuizDb>>

    @Query("SELECT * from writeQuiz  WHERE grade = :grade ORDER BY lastSelectDate LIMIT :limit")
    fun getWriteQuizListByAccessTime(grade: Int, limit: Int): Single<List<WriteQuizDb>>

    @Query("SELECT * from writeQuiz  WHERE grade = :grade ORDER BY RANDOM() LIMIT :limit")
    fun getRandomWriteQuizList(grade: Int, limit: Int): Single<List<WriteQuizDb>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateWriteQuiz(writeQuizDb: WriteQuizDb): Completable

    @Query("DELETE FROM writeQuiz WHERE definitionId = :definitionId")
    fun removeWriteQuiz(definitionId: Long): Completable

    @Query("SELECT COUNT(*) FROM writeQuiz WHERE grade = :tier")
    fun getWriteQuizCountByGrade(tier: Int): Single<Int>

    @Insert
    fun addHint(hintDb: HintDb): Completable

    @Update
    fun updateHint(hintDb: HintDb): Completable

    @Query("DELETE FROM hint WHERE id = :id")
    fun removeHint(id: Long): Completable

    @Delete
    fun removeHint(hintDb: HintDb): Completable

}