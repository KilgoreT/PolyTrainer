package me.apomazkin.core_db_impl.room

import androidx.room.Dao
import androidx.room.Insert
import me.apomazkin.core_db_impl.entity.DefinitionDb

@Dao
interface DefinitionDao {

    @Insert
    fun addDefinition(definitionDb: DefinitionDb)

}
