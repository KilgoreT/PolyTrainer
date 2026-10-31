package me.apomazkin.candidates

import me.apomazkin.candidates.mapper.toUi
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.DictionaryForm
import me.apomazkin.lexeme.ExistingLemma
import me.apomazkin.lexeme.FormComponent
import me.apomazkin.lexeme.FormGroup
import me.apomazkin.lexeme.FormOption
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupLexeme
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.WordRemark
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Домен → UI поле в поле (Д57): ответ сервера, форма словаря и отказ записи.
 * У каждого поля своё значение, чтобы перепутанные поля не прошли.
 */
class MappersTest {

    private val lexeme = LookupLexeme(
        key = "ruwiktionary/1.noun.1",
        partOfSpeechKey = "noun",
        source = "ruwiktionary",
        translations = listOf("собака", "пёс"),
        gloss = "dog",
        caption = "gambling: banker's funds",
        notes = listOf("разг."),
        examples = listOf("perro callejero"),
        grammar = mapOf("gender" to "masculine"),
        level = "A1",
        synonyms = listOf("can"),
        antonyms = listOf("gato"),
        componentMatches = listOf(
            ComponentMatch(componentId = 1, optionId = null, field = "translations"),
            ComponentMatch(componentId = 2, optionId = 21, field = null),
        ),
    )

    private val lexemeUi = LookupLexemeUi(
        key = "ruwiktionary/1.noun.1",
        partOfSpeechKey = "noun",
        source = "ruwiktionary",
        translations = listOf("собака", "пёс"),
        gloss = "dog",
        caption = "gambling: banker's funds",
        notes = listOf("разг."),
        examples = listOf("perro callejero"),
        grammar = mapOf("gender" to "masculine"),
        level = "A1",
        synonyms = listOf("can"),
        antonyms = listOf("gato"),
        componentMatches = listOf(
            ComponentMatchUi(componentId = 1, optionId = null, field = "translations"),
            ComponentMatchUi(componentId = 2, optionId = 21, field = null),
        ),
    )

    @Test
    fun `a lookup lexeme maps field by field with its component matches`() {
        assertEquals(lexemeUi, lexeme.toUi())
    }

    @Test
    fun `a lookup word maps with lemmas, lexemes and both kinds of remarks`() {
        val word = LookupWord(
            text = "perro",
            status = LookupWordStatus.FOUND,
            lemmas = listOf(
                LookupLemma(
                    text = "perro",
                    formNote = "note",
                    pronunciations = listOf("/ˈpe.ro/"),
                    lexemes = listOf(lexeme),
                ),
            ),
            remarks = listOf(
                WordRemark.ComponentUnclear(componentId = 7),
                WordRemark.NoTranslation(lemma = "zarpar", lexemeKey = "enwiktionary/1.verb.1"),
            ),
        )

        assertEquals(
            LookupWordUi(
                text = "perro",
                status = LookupWordStatus.FOUND,
                lemmas = listOf(
                    LookupLemmaUi(
                        text = "perro",
                        formNote = "note",
                        pronunciations = listOf("/ˈpe.ro/"),
                        lexemes = listOf(lexemeUi),
                    ),
                ),
                remarks = listOf(
                    WordRemarkUi.ComponentUnclear(componentId = 7),
                    WordRemarkUi.NoTranslation(
                        lemma = "zarpar",
                        lexemeKey = "enwiktionary/1.verb.1",
                    ),
                ),
            ),
            word.toUi(),
        )
    }

    @Test
    fun `a word that was not found maps with its status and no lemmas`() {
        val word = LookupWord("accorc", LookupWordStatus.NOT_FOUND, emptyList(), emptyList())

        val ui = LookupWordUi("accorc", LookupWordStatus.NOT_FOUND, emptyList(), emptyList())
        assertEquals(ui, word.toUi())
    }

    @Test
    fun `a component recommendation maps field by field`() {
        val recommendation = ComponentRecommendation(
            name = "Род",
            template = ComponentTemplate.CHOICE,
            options = listOf("м.р.", "ж.р."),
        )

        assertEquals(
            ComponentRecommendationUi(
                name = "Род",
                template = ComponentTemplate.CHOICE,
                options = listOf("м.р.", "ж.р."),
            ),
            recommendation.toUi(),
        )
    }

    private fun formComponent(
        id: Long,
        systemKey: BuiltInComponent?,
        name: String?,
        template: ComponentTemplate,
        isCore: Boolean,
        isMultiple: Boolean,
        position: Int,
        dependsOn: DependencyTarget,
    ) = FormComponent(id, systemKey, name, template, isCore, isMultiple, position, dependsOn)

    private fun formComponentUi(
        id: Long,
        systemKey: BuiltInComponent?,
        name: String?,
        template: ComponentTemplate,
        isCore: Boolean,
        isMultiple: Boolean,
        position: Int,
        dependsOn: DependencyTarget,
    ) = FormComponentUi(id, systemKey, name, template, isCore, isMultiple, position, dependsOn)

    @Test
    fun `a dictionary form maps with components of all dependencies, options, groups, lemmas`() {
        val form = DictionaryForm(
            dictionaryId = 3,
            translationLanguage = "ru",
            components = listOf(
                formComponent(
                    1, BuiltInComponent.TRANSLATION, null, ComponentTemplate.TEXT,
                    isCore = true, isMultiple = false, position = 0,
                    dependsOn = DependencyTarget.Lexeme,
                ),
                formComponent(
                    4, null, "Толкование", ComponentTemplate.TEXT,
                    isCore = false, isMultiple = true, position = 4,
                    dependsOn = DependencyTarget.Component(ComponentTypeId(1)),
                ),
                formComponent(
                    5, null, "Род", ComponentTemplate.CHOICE,
                    isCore = false, isMultiple = false, position = 2,
                    dependsOn = DependencyTarget.Option(21),
                ),
            ),
            options = mapOf(
                2L to listOf(FormOption(id = 21, key = "noun", label = null, position = 0)),
                5L to listOf(FormOption(id = 51, key = null, label = "м.р.", position = 1)),
            ),
            groups = listOf(FormGroup(id = 7, name = "Harry Potter 1")),
            existingLemmas = listOf(
                ExistingLemma(
                    wordId = 100,
                    text = "run",
                    lexemeCount = 2,
                    coreValues = listOf("управлять"),
                    lexemeTextKeys = setOf("управлять", "бежать"),
                    groupNames = listOf("Finance"),
                    addedAt = 0L,
                    lexemes = emptyList(),
                ),
            ),
        )

        assertEquals(
            DictionaryFormUi(
                dictionaryId = 3,
                translationLanguage = "ru",
                components = listOf(
                    formComponentUi(
                        1, BuiltInComponent.TRANSLATION, null, ComponentTemplate.TEXT,
                        isCore = true, isMultiple = false, position = 0,
                        dependsOn = DependencyTarget.Lexeme,
                    ),
                    formComponentUi(
                        4, null, "Толкование", ComponentTemplate.TEXT,
                        isCore = false, isMultiple = true, position = 4,
                        dependsOn = DependencyTarget.Component(ComponentTypeId(1)),
                    ),
                    formComponentUi(
                        5, null, "Род", ComponentTemplate.CHOICE,
                        isCore = false, isMultiple = false, position = 2,
                        dependsOn = DependencyTarget.Option(21),
                    ),
                ),
                options = mapOf(
                    2L to listOf(FormOptionUi(id = 21, key = "noun", label = null, position = 0)),
                    5L to listOf(FormOptionUi(id = 51, key = null, label = "м.р.", position = 1)),
                ),
                groups = listOf(FormGroupUi(id = 7, name = "Harry Potter 1")),
                existingLemmas = listOf(
                    ExistingLemmaUi(
                        wordId = 100,
                        text = "run",
                        lexemeCount = 2,
                        coreValues = listOf("управлять"),
                        lexemeTextKeys = setOf("управлять", "бежать"),
                        groupNames = listOf("Finance"),
                        addedAt = 0L,
                        lexemes = emptyList(),
                    ),
                ),
            ),
            form.toUi(),
        )
    }

    @Test
    fun `AWP-21_3 a rejected group gives the failure with the id of the group`() {
        val rejected = AddWordsBatchOutcome.Rejected.GroupMissing(5)

        assertEquals(AddFailureUi.GroupMissing(groupId = 5), rejected.toUi())
    }

    @Test
    fun `AWP-21_4 a rejected word gives the failure with the id of the word`() {
        val rejected = AddWordsBatchOutcome.Rejected.WordMissing(6)

        assertEquals(AddFailureUi.WordMissing(wordId = 6), rejected.toUi())
    }

    @Test
    fun `AWP-21_6 a rejected component gives the failure with the id of the component`() {
        val rejected = AddWordsBatchOutcome.Rejected.TypeUnavailable(7)

        assertEquals(AddFailureUi.TypeUnavailable(typeId = 7), rejected.toUi())
    }

    @Test
    fun `AWP-21_1 a lexeme without a core value and a missing dictionary give no-data failures`() {
        val noCore = AddWordsBatchOutcome.Rejected.NoCoreValue(wordIndex = 1, lexemeIndex = 2)

        assertEquals(AddFailureUi.NoCoreValue, noCore.toUi())
        assertEquals(
            AddFailureUi.DictionaryMissing,
            AddWordsBatchOutcome.Rejected.DictionaryMissing.toUi(),
        )
    }
}
