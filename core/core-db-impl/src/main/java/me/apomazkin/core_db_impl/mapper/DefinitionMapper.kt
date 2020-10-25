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
        mapPartOfSpeech(value)
    )

    private fun mapPartOfSpeech(value: DefinitionDb): PartOfSpeech? {
        return when (value.partOfSpeech) {
            NOUN -> Noun(value.isCountable)
            VERB -> Verb(value.isTransitive)
            ADJECTIVE -> Adjective
            ADVERB -> Adverb
            else -> null
        }
    }

    override fun reverseMap(value: Definition) = DefinitionDb(
        value.id,
        value.wordId,
        value.definition,
        // TODO: 26.10.2020 Все таки подумать о том, чтобы отправлять туда объект, а не поля
        reverseMapPartOfSpeech(value),
        reverseMapIsTransitive(value),
        reverseMapIsCountable(value)
    )

    private fun reverseMapPartOfSpeech(value: Definition): String? {
        return when (value.partOfSpeech) {
            is Verb -> VERB
            is Noun -> NOUN
            is Adverb -> ADVERB
            is Adjective -> ADJECTIVE
            else -> null
        }
    }

    private fun reverseMapIsTransitive(value: Definition): Boolean? {
        return when (val partOfSpeech = value.partOfSpeech) {
            is Verb -> {
                partOfSpeech.isTransitive
            }
            else -> null
        }
    }

    private fun reverseMapIsCountable(value: Definition): Boolean? {
        return when (val partOfSpeech = value.partOfSpeech) {
            is Noun -> {
                partOfSpeech.isCountable
            }
            else -> null
        }
    }
}