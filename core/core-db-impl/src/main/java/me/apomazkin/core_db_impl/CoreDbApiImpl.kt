package me.apomazkin.core_db_impl

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_impl.room.Definition
import me.apomazkin.core_db_impl.room.DefinitionDao
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    val definitionDao: DefinitionDao
) : CoreDbApi {
    override fun insert(word: String) {
        definitionDao.insertDefinition(Definition(word = word))
    }

    override fun getList(): List<String> {
        return definitionDao.getDefinitionList()
            .map { ttt -> ttt.word ?: "default" }
    }
}