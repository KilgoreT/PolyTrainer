package me.apomazkin.core_db_impl.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WordWithDefinitionsDb

@Dao
interface WordDao {

    @Insert
    fun addWord(wordDb: WordDb)

    @Query("DELETE FROM words WHERE id = :id")
    fun removeWord(id: Long): Completable

    @Deprecated("not used")
    @Query("SELECT * from words")
    fun getWordList(): Observable<List<WordDb>>

    @Transaction
    @Query("SELECT * FROM words ORDER BY id DESC")
    fun getWordListWithDefinition(): Observable<List<WordWithDefinitionsDb>>

    @Insert
    fun addDefinition(definitionDb: DefinitionDb)

    @Query("DELETE FROM definitions WHERE id = :id")
    fun deleteDefinition(id: Long): Completable

    @Delete
    fun deleteWordWithDefinition(vararg definition: DefinitionDb): Completable

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    fun getWord(id: Long): Single<WordWithDefinitionsDb>

}