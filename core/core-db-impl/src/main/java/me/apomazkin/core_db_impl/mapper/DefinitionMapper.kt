package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_impl.entity.DefinitionDb

class DefinitionMapper : Mapper<DefinitionDb, Definition>() {

    override fun map(value: DefinitionDb) = Definition(
        value.id,
        value.wordId,
        value.definition,
        value.type
    )

    override fun reverseMap(value: Definition) = DefinitionDb(
        value.id,
        value.wordId,
        value.definition,
        value.type
    )

}