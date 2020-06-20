package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.core_db_impl.entity.WordWithDefinitionsDb

class WordWithDefinitionsMapper : Mapper<WordWithDefinitionsDb, WordWithDefinition>() {

    override fun map(value: WordWithDefinitionsDb): WordWithDefinition {
        val wordMapper = WordMapper()
        val definitionMapper = DefinitionMapper()
        return WordWithDefinition(
            wordMapper.map(value.wordDb),
            definitionMapper.map(value.definitionDbList)
        )
    }

    override fun reverseMap(value: WordWithDefinition): WordWithDefinitionsDb {
        val wordMapper = WordMapper()
        val definitionMapper = DefinitionMapper()
        return WordWithDefinitionsDb(
            wordMapper.reverseMap(value.word),
            definitionMapper.reverseMap(value.definitionList)
        )
    }

}