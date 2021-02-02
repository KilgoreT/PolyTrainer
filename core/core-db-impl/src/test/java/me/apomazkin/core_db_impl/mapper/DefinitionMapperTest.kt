package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Grade
import me.apomazkin.core_db_api.entity.Noun
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
    fun test_grade() {
        val gradeA1 = Definition(wordClass = Noun(null, Grade.A1))
        val gradeA2 = Definition(wordClass = Noun(null, Grade.A2))
        val gradeB1 = Definition(wordClass = Noun(null, Grade.B1))
        val gradeB2 = Definition(wordClass = Noun(null, Grade.B2))
        val gradeC1 = Definition(wordClass = Noun(null, Grade.C1))
        val gradeC2 = Definition(wordClass = Noun(null, Grade.C2))

        assertTrue("Grade A1", mapper.reverseMap(gradeA1).options == 1L)
        assertTrue("Grade A2", mapper.reverseMap(gradeA2).options == 2L)
        assertTrue("Grade B1", mapper.reverseMap(gradeB1).options == 4L)
        assertTrue("Grade B2", mapper.reverseMap(gradeB2).options == 8L)
        assertTrue("Grade C1", mapper.reverseMap(gradeC1).options == 16L)
        assertTrue("Grade C2", mapper.reverseMap(gradeC2).options == 32L)
    }
}