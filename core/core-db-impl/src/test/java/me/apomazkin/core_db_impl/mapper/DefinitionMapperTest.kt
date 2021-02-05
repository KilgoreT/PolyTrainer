package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_db_api.entity.Adjective.Gradability.*
import me.apomazkin.core_db_api.entity.Adjective.Order.*
import me.apomazkin.core_db_api.entity.Noun.Countability.*
import me.apomazkin.core_db_api.entity.Verb.Transitivity.INTRANSITIVE
import me.apomazkin.core_db_api.entity.Verb.Transitivity.TRANSITIVE
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefinitionMapperTest {

    private val mapper = DefinitionMapper()

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `Grade Convert`() {
        val gradeA1 = Definition(wordClass = Noun(null, Grade.A1))
        val gradeA2 = Definition(wordClass = Verb(null, Grade.A2))
        val gradeB1 = Definition(wordClass = Adjective(null, null, Grade.B1))
        val gradeB2 = Definition(wordClass = Noun(null, Grade.B2))
        val gradeC1 = Definition(wordClass = Adverb(Grade.C1))
        val gradeC2 = Definition(wordClass = Noun(null, Grade.C2))

        assertTrue("Grade A1", mapper.reverseMap(gradeA1).options == 1L)
        assertTrue("Grade A2", mapper.reverseMap(gradeA2).options == 2L)
        assertTrue("Grade B1", mapper.reverseMap(gradeB1).options == 4L)
        assertTrue("Grade B2", mapper.reverseMap(gradeB2).options == 8L)
        assertTrue("Grade C1", mapper.reverseMap(gradeC1).options == 16L)
        assertTrue("Grade C2", mapper.reverseMap(gradeC2).options == 32L)
    }

    @Test
    fun `Noun Options Convert`() {
        val nounCountable = Definition(wordClass = Noun(COUNTABLE, null))
        val nounUncountable = Definition(wordClass = Noun(UNCOUNTABLE, null))
        val nounPlural = Definition(wordClass = Noun(PLURAL, null))
        val nounUsuallyPlural = Definition(wordClass = Noun(USUALLY_PLURAL, null))
        val nounUsuallySingular = Definition(wordClass = Noun(USUALLY_SINGULAR, null))

        assertTrue("COUNTABLE", mapper.reverseMap(nounCountable).options == 64L)
        assertTrue("UNCOUNTABLE", mapper.reverseMap(nounUncountable).options == 128L)
        assertTrue("PLURAL", mapper.reverseMap(nounPlural).options == 256L)
        assertTrue("USUALLY_PLURAL", mapper.reverseMap(nounUsuallyPlural).options == 512L)
        assertTrue("USUALLY_SINGULAR", mapper.reverseMap(nounUsuallySingular).options == 1024L)
    }

    @Test
    fun `Verb Options Convert`() {
        val verbTransitive = Definition(wordClass = Verb(TRANSITIVE, null))
        val verbIntransitive = Definition(wordClass = Verb(INTRANSITIVE, null))

        assertTrue("TRANSITIVE", mapper.reverseMap(verbTransitive).options == 64L)
        assertTrue("INTRANSITIVE", mapper.reverseMap(verbIntransitive).options == 128L)
    }

    @Test
    fun `Adjective Options Convert`() {
        val adjAfterNoun = Definition(wordClass = Adjective(order = AFTER_NOUN))
        val adjAfterVerb = Definition(wordClass = Adjective(order = AFTER_VERB))
        val adjBeforeNoun = Definition(wordClass = Adjective(order = BEFORE_NOUN))

        assertTrue("AFTER_NOUN", mapper.reverseMap(adjAfterNoun).options == 64L)
        assertTrue("AFTER_VERB", mapper.reverseMap(adjAfterVerb).options == 128L)
        assertTrue("BEFORE_NOUN", mapper.reverseMap(adjBeforeNoun).options == 256L)

        val adjComparative = Definition(wordClass = Adjective(gradability = COMPARATIVE))
        val adjSuperlative = Definition(wordClass = Adjective(gradability = SUPERLATIVE))
        val adjNotGradable = Definition(wordClass = Adjective(gradability = NOT_GRADABLE))

        assertTrue("COMPARATIVE", mapper.reverseMap(adjComparative).options == 512L)
        assertTrue("SUPERLATIVE", mapper.reverseMap(adjSuperlative).options == 1024L)
        assertTrue("NOT_GRADABLE", mapper.reverseMap(adjNotGradable).options == 2048L)

        val adjMixed = Definition(
            wordClass = Adjective(
                order = BEFORE_NOUN,
                gradability = SUPERLATIVE,
                grade = Grade.B2
            )
        )
        assertTrue("ADJ_MIXED", mapper.reverseMap(adjMixed).options == (256L + 1024L + 8L))
    }
}