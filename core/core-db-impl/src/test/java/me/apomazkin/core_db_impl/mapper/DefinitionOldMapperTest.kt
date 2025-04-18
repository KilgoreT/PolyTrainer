//package me.apomazkin.core_db_impl.mapper
//
//import me.apomazkin.core_db_api.entity.Adjective
//import me.apomazkin.core_db_api.entity.Adjective.Gradability.COMPARATIVE
//import me.apomazkin.core_db_api.entity.Adjective.Gradability.NOT_GRADABLE
//import me.apomazkin.core_db_api.entity.Adjective.Gradability.SUPERLATIVE
//import me.apomazkin.core_db_api.entity.Adjective.Order.AFTER_NOUN
//import me.apomazkin.core_db_api.entity.Adjective.Order.AFTER_VERB
//import me.apomazkin.core_db_api.entity.Adjective.Order.BEFORE_NOUN
//import me.apomazkin.core_db_api.entity.Adverb
//import me.apomazkin.core_db_api.entity.DefinitionOld
//import me.apomazkin.core_db_api.entity.Grade
//import me.apomazkin.core_db_api.entity.Noun
//import me.apomazkin.core_db_api.entity.Noun.Countability.COUNTABLE
//import me.apomazkin.core_db_api.entity.Noun.Countability.PLURAL
//import me.apomazkin.core_db_api.entity.Noun.Countability.UNCOUNTABLE
//import me.apomazkin.core_db_api.entity.Noun.Countability.USUALLY_PLURAL
//import me.apomazkin.core_db_api.entity.Noun.Countability.USUALLY_SINGULAR
//import me.apomazkin.core_db_api.entity.Verb
//import me.apomazkin.core_db_api.entity.Verb.Transitivity.INTRANSITIVE
//import me.apomazkin.core_db_api.entity.Verb.Transitivity.TRANSITIVE
//import org.junit.After
//import org.junit.Assert.assertTrue
//import org.junit.Before
//import org.junit.Test
//
//class DefinitionOldMapperTest {
//
//    private val mapper = DefinitionMapper()
//
//    @Before
//    fun setUp() {
//    }
//
//    @After
//    fun tearDown() {
//    }
//
//    @Test
//    fun `Grade Convert`() {
//        val gradeA1 = DefinitionOld(wordClass = Noun(null, Grade.A1))
//        val gradeA2 = DefinitionOld(wordClass = Verb(null, Grade.A2))
//        val gradeB1 = DefinitionOld(wordClass = Adjective(null, null, Grade.B1))
//        val gradeB2 = DefinitionOld(wordClass = Noun(null, Grade.B2))
//        val gradeC1 = DefinitionOld(wordClass = Adverb(Grade.C1))
//        val gradeC2 = DefinitionOld(wordClass = Noun(null, Grade.C2))
//
//        assertTrue("Grade A1", mapper.reverseMap(gradeA1).options == 1L)
//        assertTrue("Grade A2", mapper.reverseMap(gradeA2).options == 2L)
//        assertTrue("Grade B1", mapper.reverseMap(gradeB1).options == 4L)
//        assertTrue("Grade B2", mapper.reverseMap(gradeB2).options == 8L)
//        assertTrue("Grade C1", mapper.reverseMap(gradeC1).options == 16L)
//        assertTrue("Grade C2", mapper.reverseMap(gradeC2).options == 32L)
//    }
//
//    @Test
//    fun `Noun Options Convert`() {
//        val nounCountable = DefinitionOld(wordClass = Noun(COUNTABLE, null))
//        val nounUncountable = DefinitionOld(wordClass = Noun(UNCOUNTABLE, null))
//        val nounPlural = DefinitionOld(wordClass = Noun(PLURAL, null))
//        val nounUsuallyPlural = DefinitionOld(wordClass = Noun(USUALLY_PLURAL, null))
//        val nounUsuallySingular = DefinitionOld(wordClass = Noun(USUALLY_SINGULAR, null))
//
//        assertTrue("COUNTABLE", mapper.reverseMap(nounCountable).options == 64L)
//        assertTrue("UNCOUNTABLE", mapper.reverseMap(nounUncountable).options == 128L)
//        assertTrue("PLURAL", mapper.reverseMap(nounPlural).options == 256L)
//        assertTrue("USUALLY_PLURAL", mapper.reverseMap(nounUsuallyPlural).options == 512L)
//        assertTrue("USUALLY_SINGULAR", mapper.reverseMap(nounUsuallySingular).options == 1024L)
//    }
//
//    @Test
//    fun `Verb Options Convert`() {
//        val verbTransitive = DefinitionOld(wordClass = Verb(TRANSITIVE, null))
//        val verbIntransitive = DefinitionOld(wordClass = Verb(INTRANSITIVE, null))
//
//        assertTrue("TRANSITIVE", mapper.reverseMap(verbTransitive).options == 64L)
//        assertTrue("INTRANSITIVE", mapper.reverseMap(verbIntransitive).options == 128L)
//    }
//
//    @Test
//    fun `Adjective Options Convert`() {
//        val adjAfterNoun = DefinitionOld(wordClass = Adjective(order = AFTER_NOUN))
//        val adjAfterVerb = DefinitionOld(wordClass = Adjective(order = AFTER_VERB))
//        val adjBeforeNoun = DefinitionOld(wordClass = Adjective(order = BEFORE_NOUN))
//
//        assertTrue("AFTER_NOUN", mapper.reverseMap(adjAfterNoun).options == 64L)
//        assertTrue("AFTER_VERB", mapper.reverseMap(adjAfterVerb).options == 128L)
//        assertTrue("BEFORE_NOUN", mapper.reverseMap(adjBeforeNoun).options == 256L)
//
//        val adjComparative = DefinitionOld(wordClass = Adjective(gradability = COMPARATIVE))
//        val adjSuperlative = DefinitionOld(wordClass = Adjective(gradability = SUPERLATIVE))
//        val adjNotGradable = DefinitionOld(wordClass = Adjective(gradability = NOT_GRADABLE))
//
//        assertTrue("COMPARATIVE", mapper.reverseMap(adjComparative).options == 512L)
//        assertTrue("SUPERLATIVE", mapper.reverseMap(adjSuperlative).options == 1024L)
//        assertTrue("NOT_GRADABLE", mapper.reverseMap(adjNotGradable).options == 2048L)
//
//        val adjMixed = DefinitionOld(
//            wordClass = Adjective(
//                order = BEFORE_NOUN,
//                gradability = SUPERLATIVE,
//                grade = Grade.B2
//            )
//        )
//        assertTrue("ADJ_MIXED", mapper.reverseMap(adjMixed).options == (256L + 1024L + 8L))
//    }
//}