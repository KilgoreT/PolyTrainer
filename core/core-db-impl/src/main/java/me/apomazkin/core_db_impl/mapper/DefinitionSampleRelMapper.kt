package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Adjective
import me.apomazkin.core_db_api.entity.Adjective.Gradability.COMPARATIVE
import me.apomazkin.core_db_api.entity.Adjective.Gradability.NOT_GRADABLE
import me.apomazkin.core_db_api.entity.Adjective.Gradability.SUPERLATIVE
import me.apomazkin.core_db_api.entity.Adjective.Order.AFTER_NOUN
import me.apomazkin.core_db_api.entity.Adjective.Order.AFTER_VERB
import me.apomazkin.core_db_api.entity.Adjective.Order.BEFORE_NOUN
import me.apomazkin.core_db_api.entity.Adverb
import me.apomazkin.core_db_api.entity.DefinitionOld
import me.apomazkin.core_db_api.entity.Grade
import me.apomazkin.core_db_api.entity.Grade.A1
import me.apomazkin.core_db_api.entity.Grade.A2
import me.apomazkin.core_db_api.entity.Grade.B1
import me.apomazkin.core_db_api.entity.Grade.B2
import me.apomazkin.core_db_api.entity.Grade.C1
import me.apomazkin.core_db_api.entity.Grade.C2
import me.apomazkin.core_db_api.entity.Noun
import me.apomazkin.core_db_api.entity.Noun.Countability.COUNTABLE
import me.apomazkin.core_db_api.entity.Noun.Countability.PLURAL
import me.apomazkin.core_db_api.entity.Noun.Countability.UNCOUNTABLE
import me.apomazkin.core_db_api.entity.Noun.Countability.USUALLY_PLURAL
import me.apomazkin.core_db_api.entity.Noun.Countability.USUALLY_SINGULAR
import me.apomazkin.core_db_api.entity.Verb
import me.apomazkin.core_db_api.entity.Verb.Transitivity.INTRANSITIVE
import me.apomazkin.core_db_api.entity.Verb.Transitivity.TRANSITIVE
import me.apomazkin.core_db_api.entity.WordClass
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.LexemeDbEntity

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

//fun LexemeDbEntity.toApiEntity() = DefinitionOld(
//    id = lexemeDb.id ?: 0,
//    wordId = lexemeDb.wordId,
//    value = lexemeDb.definition,
//    wordClass = lexemeDb.toWordClass(),
//    sampleList = sampleDbList.map { item -> item.toApiEntity() }
//)

fun LexemeDbEntity.toMateApp() = DefinitionOld(
    id = lexemeDb.id ?: throw IllegalArgumentException("Id not found."),
    wordId = lexemeDb.wordId ?: throw IllegalArgumentException("WordId not found."),
    value = lexemeDb.definition ?: "Empty value",
//    category = lexemeDb.wordClass ?: throw IllegalArgumentException("WordClass not found."),
)

//fun List<LexemeDbEntity>.toApiEntity() = this.map { item -> item.toApiEntity() }
//fun List<LexemeDbEntity>.toMateApp() = this.map { item -> item.toMateApp() }

fun LexemeDb.toWordClass(): WordClass? {
    return when (wordClass) {
        NOUN -> Noun(
            grade = options.toGrade(),
            countability = options.toNounCountability()
        )
        VERB -> Verb(
            grade = options.toGrade(),
            transitivity = options.toVerbTransitivity()
        )
        ADJECTIVE -> Adjective(
            order = options.toAdjectiveOrder(),
            gradability = options.toAdjectiveGradability(),
            grade = options.toGrade(),
        )
        ADVERB -> Adverb(
            grade = options.toGrade(),
        )
        else -> null
    }
}

fun Long.toGrade(): Grade? {
    return when {
        this and GRADE_A1 > 0 -> A1
        this and GRADE_A2 > 0 -> A2
        this and GRADE_B1 > 0 -> B1
        this and GRADE_B2 > 0 -> B2
        this and GRADE_C1 > 0 -> C1
        this and GRADE_C2 > 0 -> C2
        else -> null
    }
}

fun Long.toNounCountability(): Noun.Countability? = when {
    this and COUNTABLE_INDEX > 0 -> COUNTABLE
    this and UNCOUNTABLE_INDEX > 0 -> UNCOUNTABLE
    this and PLURAL_INDEX > 0 -> PLURAL
    this and USUALLY_PLURAL_INDEX > 0 -> USUALLY_PLURAL
    this and USUALLY_SINGULAR_INDEX > 0 -> USUALLY_SINGULAR
    else -> null
}

fun Long.toVerbTransitivity(): Verb.Transitivity? = when {
    this and TRANSITIVE_INDEX > 0 -> TRANSITIVE
    this and INTRANSITIVE_INDEX > 0 -> INTRANSITIVE
    else -> null
}

fun Long.toAdjectiveOrder(): Adjective.Order? = when {
    this and AFTER_NOUN_INDEX > 0 -> AFTER_NOUN
    this and AFTER_VERB_INDEX > 0 -> AFTER_VERB
    this and BEFORE_NOUN_INDEX > 0 -> BEFORE_NOUN
    else -> null
}

fun Long.toAdjectiveGradability(): Adjective.Gradability? = when {
    this and COMPARATIVE_INDEX > 0 -> COMPARATIVE
    this and SUPERLATIVE_INDEX > 0 -> SUPERLATIVE
    this and NOT_GRADABLE_INDEX > 0 -> NOT_GRADABLE
    else -> null
}