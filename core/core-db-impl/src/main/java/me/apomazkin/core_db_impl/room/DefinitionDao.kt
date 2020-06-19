package me.apomazkin.core_db_impl.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Observable

@Dao
interface DefinitionDao {

    @Query("SELECT * from DEFINITION")
//    fun getDefinitionList(): List<Definition>
    fun getDefinitionList(): Observable<List<Definition>>

    @Insert
    fun insertDefinition(definition: Definition)
}