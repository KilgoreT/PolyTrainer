package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_db_api.entity.Adjective.Gradability.*
import me.apomazkin.core_db_api.entity.Adjective.Order.*
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

// NOUN
private val COUNTABLE_INDEX = 1L shl (GRADE_OFFSET + COUNTABLE.ordinal)
private val UNCOUNTABLE_INDEX = 1L shl (GRADE_OFFSET + UNCOUNTABLE.ordinal)
private val PLURAL_INDEX = 1L shl (GRADE_OFFSET + PLURAL.ordinal)
private val USUALLY_PLURAL_INDEX = 1L shl (GRADE_OFFSET + USUALLY_PLURAL.ordinal)
private val USUALLY_SINGULAR_INDEX = 1L shl (GRADE_OFFSET + USUALLY_SINGULAR.ordinal)

// VERB
private val TRANSITIVE_INDEX = 1L shl (GRADE_OFFSET + TRANSITIVE.ordinal)
private val INTRANSITIVE_INDEX = 1L shl (GRADE_OFFSET + INTRANSITIVE.ordinal)

// ADJECTIVE
private val AFTER_NOUN_INDEX = 1L shl (GRADE_OFFSET + AFTER_NOUN.ordinal)
private val AFTER_VERB_INDEX = 1L shl (GRADE_OFFSET + AFTER_VERB.ordinal)
private val BEFORE_NOUN_INDEX = 1L shl (GRADE_OFFSET + BEFORE_NOUN.ordinal)
private val ADJ_ORDER_OFFSET = GRADE_OFFSET + Adjective.Order.values().size
private val COMPARATIVE_INDEX = 1L shl (ADJ_ORDER_OFFSET + COMPARATIVE.ordinal)
private val SUPERLATIVE_INDEX = 1L shl (ADJ_ORDER_OFFSET + SUPERLATIVE.ordinal)
private val NOT_GRADABLE_INDEX = 1L shl (ADJ_ORDER_OFFSET + NOT_GRADABLE.ordinal)

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
        val countability = when {
            options and COUNTABLE_INDEX > 0 -> COUNTABLE
            options and UNCOUNTABLE_INDEX > 0 -> UNCOUNTABLE
            options and PLURAL_INDEX > 0 -> PLURAL
            options and USUALLY_PLURAL_INDEX > 0 -> USUALLY_PLURAL
            options and USUALLY_SINGULAR_INDEX > 0 -> USUALLY_SINGULAR
            else -> null
        }
        return Noun(
            grade = getGrade(options),
            countability = countability
        )
    }

    private fun getVerb(options: Long): Verb {
        val transitivity = when {
            options and TRANSITIVE_INDEX > 0 -> TRANSITIVE
            options and INTRANSITIVE_INDEX > 0 -> INTRANSITIVE
            else -> null
        }
        return Verb(
            grade = getGrade(options),
            transitivity = transitivity
        )
    }

    private fun getAdjective(options: Long): Adjective {
        val order = when {
            options and AFTER_NOUN_INDEX > 0 -> AFTER_NOUN
            options and AFTER_VERB_INDEX > 0 -> AFTER_VERB
            options and BEFORE_NOUN_INDEX > 0 -> BEFORE_NOUN
            else -> null
        }
        val gradability = when {
            options and COMPARATIVE_INDEX > 0 -> COMPARATIVE
            options and SUPERLATIVE_INDEX > 0 -> SUPERLATIVE
            options and NOT_GRADABLE_INDEX > 0 -> NOT_GRADABLE
            else -> null
        }
        return Adjective(
            order = order,
            gradability = gradability,
            grade = getGrade(options),
        )
    }

    private fun getAdverb(options: Long): Adverb {
        return Adverb(
            grade = getGrade(options),
        )
    }

    private fun getGrade(options: Long): Grade? {
        return when {
            options and GRADE_A1 > 0 -> A1
            options and GRADE_A2 > 0 -> A2
            options and GRADE_B1 > 0 -> B1
            options and GRADE_B2 > 0 -> B2
            options and GRADE_C1 > 0 -> C1
            options and GRADE_C2 > 0 -> C2
            else -> null
        }
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
            null -> 0L
        }
        options = options or when (wordClass) {
            is Noun -> convertNounOptions(wordClass)
            is Verb -> convertVerbOptions(wordClass)
            is Adjective -> convertAdjOptions(wordClass)
            is Adverb -> 0L
            null -> 0L
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
            null -> 0L
        }
    }

    private fun convertVerbOptions(wordClass: Verb): Long {
        return 0L or when (wordClass.transitivity) {
            TRANSITIVE -> TRANSITIVE_INDEX
            INTRANSITIVE -> INTRANSITIVE_INDEX
            null -> 0L
        }
    }

    private fun convertAdjOptions(wordClass: Adjective): Long {
        var options = 0L or when (wordClass.order) {
            AFTER_NOUN -> AFTER_NOUN_INDEX
            AFTER_VERB -> AFTER_VERB_INDEX
            BEFORE_NOUN -> BEFORE_NOUN_INDEX
            null -> 0L
        }
        options = options or when (wordClass.gradability) {
            COMPARATIVE -> COMPARATIVE_INDEX
            SUPERLATIVE -> SUPERLATIVE_INDEX
            NOT_GRADABLE -> NOT_GRADABLE_INDEX
            null -> 0L
        }
        return options
    }
}