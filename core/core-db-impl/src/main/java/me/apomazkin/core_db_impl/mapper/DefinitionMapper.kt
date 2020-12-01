package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_db_impl.entity.DefinitionDb

private const val NOUN = "noun"
private const val VERB = "verb"
private const val ADJECTIVE = "adjective"
private const val ADVERB = "adverb"

class DefinitionMapper : Mapper<DefinitionDb, Definition>() {

    override fun map(value: DefinitionDb) = Definition(
        value.id,
        value.wordId,
        value.definition,
        mapWordClass(value)
    )

    private fun mapWordClass(value: DefinitionDb): WordClass? {
        return when (value.wordClass) {
            NOUN -> Noun(value.options)
            VERB -> Verb(value.options)
            ADJECTIVE -> Adjective(value.options)
            ADVERB -> Adverb(value.options)
            else -> null
        }
    }

    override fun reverseMap(value: Definition) = DefinitionDb(
        value.id,
        value.wordId,
        value.definition,
        reverseMapWordClass(value),
        value.wordClass?.options ?: 0
    )

    private fun reverseMapWordClass(value: Definition): String? {
        return when (value.wordClass) {
            is Verb -> VERB
            is Noun -> NOUN
            is Adverb -> ADVERB
            is Adjective -> ADJECTIVE
            else -> null
        }
    }
}