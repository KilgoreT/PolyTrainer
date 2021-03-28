package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_db_impl.entity.TermDb

class TermMapper : Mapper<TermDb, Term>() {

    override fun map(value: TermDb): Term {
        val wordMapper = WordMapper()
        val definitionMapper = DefinitionMapper()
        return Term(
            wordMapper.map(value.wordDb),
            definitionMapper.map(value.definitionDbList)
        )
    }

    override fun reverseMap(value: Term): TermDb {
        val wordMapper = WordMapper()
        val definitionMapper = DefinitionMapper()
        return TermDb(
            wordMapper.reverseMap(value.word),
            definitionMapper.reverseMap(value.definitionList)
        )
    }

}