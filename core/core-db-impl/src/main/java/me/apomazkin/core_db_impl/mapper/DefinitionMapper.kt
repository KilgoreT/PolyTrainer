package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_db_api.entity.Grade.*
import me.apomazkin.core_db_api.entity.Noun.Countability.*
import me.apomazkin.core_db_api.entity.Verb.Transitivity.INTRANSITIVE
import me.apomazkin.core_db_api.entity.Verb.Transitivity.TRANSITIVE
import me.apomazkin.core_db_impl.entity.DefinitionDb

private const val NOUN = "noun"
private const val VERB = "verb"
private const val ADJECTIVE = "adjective"
private const val ADVERB = "adverb"

private val GRADE_A1 = 1L shl A1.ordinal
private val GRADE_A2 = 1L shl A2.ordinal
private val GRADE_B1 = 1L shl B1.ordinal
private val GRADE_B2 = 1L shl B2.ordinal
private val GRADE_C1 = 1L shl C1.ordinal
private val GRADE_C2 = 1L shl C2.ordinal

private val GRADE_OFFSET = Grade.values().size

private val COUNTABLE_INDEX = 1L shl (GRADE_OFFSET + COUNTABLE.ordinal)
private val UNCOUNTABLE_INDEX = 1L shl (GRADE_OFFSET + UNCOUNTABLE.ordinal)
private val PLURAL_INDEX = 1L shl (GRADE_OFFSET + PLURAL.ordinal)
private val USUALLY_PLURAL_INDEX = 1L shl (GRADE_OFFSET + USUALLY_PLURAL.ordinal)
private val USUALLY_SINGULAR_INDEX = 1L shl (GRADE_OFFSET + USUALLY_SINGULAR.ordinal)


private val TRANSITIVE_INDEX = 1L shl (GRADE_OFFSET + TRANSITIVE.ordinal)
private val INTRANSITIVE_INDEX = 1L shl (GRADE_OFFSET + INTRANSITIVE.ordinal)

class DefinitionMapper : Mapper<DefinitionDb, Definition>() {

    override fun map(value: DefinitionDb) = Definition(
        value.id,
        value.wordId,
        value.definition,
        mapWordClass(value)
    )

    private fun mapWordClass(value: DefinitionDb): WordClass? {
        return when (value.wordClass) {
            NOUN -> getNoun(value.options)
            VERB -> getVerb(value.options)
            ADJECTIVE -> getAdjective(value.options)
            ADVERB -> getAdverb(value.options)
            else -> null
        }
    }

    private fun getNoun(options: Long): Noun {
        val countability = COUNTABLE
        return Noun(countability)
    }

    private fun getVerb(options: Long): Verb {
        val transitivity = TRANSITIVE
        return Verb(transitivity)
    }

    private fun getAdjective(options: Long): Adjective {
        return Adjective()
    }

    private fun getAdverb(options: Long): Adverb {
        return Adverb()
    }


    override fun reverseMap(value: Definition) = DefinitionDb(
        value.id,
        value.wordId,
        value.definition,
        reverseMapWordClass(value),
        convertOptions(value.wordClass)
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

    private fun convertOptions(wordClass: WordClass?): Long {
        var options = 0L
        options = options or when (wordClass?.grade) {
            A1 -> GRADE_A1
            A2 -> GRADE_A2
            B1 -> GRADE_B1
            B2 -> GRADE_B2
            C1 -> GRADE_C1
            C2 -> GRADE_C2
            null -> 0
        }
        options = options or when (wordClass) {
            is Noun -> convertNounOptions(wordClass)
            is Verb -> convertVerbOptions(wordClass)
            is Adjective -> TODO()
            is Adverb -> TODO()
            null -> TODO()
        }

        return options
    }

    private fun convertNounOptions(wordClass: Noun): Long {
        return 0L or when (wordClass.countability) {
            COUNTABLE -> COUNTABLE_INDEX
            UNCOUNTABLE -> UNCOUNTABLE_INDEX
            PLURAL -> PLURAL_INDEX
            USUALLY_PLURAL -> USUALLY_PLURAL_INDEX
            USUALLY_SINGULAR -> USUALLY_SINGULAR_INDEX
            null -> 0
        }
    }

    private fun convertVerbOptions(wordClass: Verb): Long {
        return 0L or when (wordClass.transitivity) {
            TRANSITIVE -> TRANSITIVE_INDEX
            INTRANSITIVE -> INTRANSITIVE_INDEX
            null -> 0
        }
    }
}