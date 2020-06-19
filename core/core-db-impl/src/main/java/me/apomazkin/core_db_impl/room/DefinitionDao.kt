package me.apomazkin.core_db_impl.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Observable
import me.apomazkin.core_db_impl.entity.WordDb

@Dao
interface DefinitionDao {

    @Insert
    fun addWord(wordDb: WordDb)

    @Query("DELETE FROM WORDDB WHERE id = :id")
    fun removeWord(id: Long)

    @Query("SELECT * from WORDDB")
    fun getWordList(): Observable<List<WordDb>>

}