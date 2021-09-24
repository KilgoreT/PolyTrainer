package me.apomazkin.core_db_api.entity

private const val NOUN_OFFSET = 0
private const val VERB_OFFSET = 5

private const val UNCOUNTABLE_INDEX = 0 + NOUN_OFFSET
private const val TRANSITIVE_INDEX = 0 + VERB_OFFSET

const val OPT_UNCOUNTABLE = 1L shl UNCOUNTABLE_INDEX
const val OPT_TRANSITIVE = 1L shl TRANSITIVE_INDEX

data class Definition(
    val id: Long? = null,
    val wordId: Long? = null,
    val value: String? = null,
    val wordClass: WordClass? = null,
    val sampleList: List<Sample>? = null
)

enum class Grade {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2
}

sealed class WordClass(
    val grade: Grade? = null
)

class Noun(
    val countability: Countability? = null,
    grade: Grade? = null
) : WordClass(grade) {
    enum class Countability {
        COUNTABLE,
        UNCOUNTABLE,
        PLURAL,
        USUALLY_PLURAL,
        USUALLY_SINGULAR
    }
}

class Verb(
    val transitivity: Transitivity? = null,
    grade: Grade? = null
) : WordClass(grade) {
    enum class Transitivity {
        TRANSITIVE,
        INTRANSITIVE
    }
}

class Adjective(
    val order: Order? = null,
    val gradability: Gradability? = null,
    grade: Grade? = null
) : WordClass(grade) {
    enum class Order {
        AFTER_NOUN,
        AFTER_VERB,
        BEFORE_NOUN
    }
    enum class Gradability {
        COMPARATIVE,
        SUPERLATIVE,
        NOT_GRADABLE
    }
}

class Adverb(
    grade: Grade? = null
) : WordClass(grade)

