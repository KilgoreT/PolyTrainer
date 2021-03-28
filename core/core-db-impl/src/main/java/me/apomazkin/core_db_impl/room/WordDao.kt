package me.apomazkin.core_db_impl.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.TermDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb

// TODO: 20.03.2021 переименгвать Dao
@Dao
interface WordDao {

    @Insert
    fun addWord(wordDb: WordDb): Completable

    @Query("SELECT * FROM words WHERE id = :id")
    fun getWordById(id: Long): Single<WordDb>

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

    @Query("SELECT * FROM definitions WHERE id = :id")
    fun getDefinitionById(id: Long): Single<DefinitionDb>

    @Query("SELECT COUNT(*) FROM definitions")
    fun getDefinitionCount1(): Int

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

    @Query("SELECT * from writeQuiz  WHERE grade = :grade ORDER BY RANDOM() LIMIT :limit")
    fun getWriteQuizList(grade: Int, limit: Int): Single<List<WriteQuizDb>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateWriteQuiz(writeQuizDb: WriteQuizDb): Completable
}