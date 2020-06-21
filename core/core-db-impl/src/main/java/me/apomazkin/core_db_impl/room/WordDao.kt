package me.apomazkin.core_db_impl.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.reactivex.Observable
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WordWithDefinitionsDb

@Dao
interface WordDao {

    @Insert
    fun addWord(wordDb: WordDb)

    @Query("DELETE FROM words WHERE id = :id")
    fun removeWord(id: Long)

    @Query("SELECT * from words")
    fun getWordList(): Observable<List<WordDb>>

    @Transaction
    @Query("SELECT * FROM words")
    fun getWordListWithDefinition(): Observable<List<WordWithDefinitionsDb>>

}