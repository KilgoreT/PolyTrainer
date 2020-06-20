package me.apomazkin.core_db_impl

import io.reactivex.Observable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.mapper.DefinitionMapper
import me.apomazkin.core_db_impl.mapper.WordMapper
import me.apomazkin.core_db_impl.mapper.WordWithDefinitionsMapper
import me.apomazkin.core_db_impl.room.DefinitionDao
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val definitionDao: DefinitionDao
) : CoreDbApi {

    override fun addWord(value: String) {
        definitionDao.addWord(WordDb(word = value))
    }

    override fun removeWord(id: Long) {
        definitionDao
            .removeWord(id)
    }

    override fun getWordList(): Observable<List<Word>> {
        val mapper = WordMapper()
        return definitionDao
            .getWordList()
            .map { list -> list.map { item -> mapper.map(item) } }
    }

    override fun addDefinition(definition: Definition) {
        val mapper = DefinitionMapper()
        definitionDao
            .addDefinition(mapper.reverseMap(definition))
    }

    override fun getWordWithDefinition(): Observable<List<WordWithDefinition>> {
        val mapper = WordWithDefinitionsMapper()
        return definitionDao
            .getWordListWithDefinition()
            .map { list -> list.map { item -> mapper.map(item) } }
    }

}