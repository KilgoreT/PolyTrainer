package me.apomazkin.core_db_impl.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Observable
import me.apomazkin.core_db_impl.entity.WordDb

@Dao
interface DefinitionDao {

    @Query("SELECT * from WORDDB")
    fun getWordList(): Observable<List<WordDb>>

    @Insert
    fun insertWord(wordDb: WordDb)
}