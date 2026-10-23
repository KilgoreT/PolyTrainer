package me.apomazkin.scancandidates.mate

import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.BatchWord
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
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
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordRemark
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.lexeme.lexemeTextKey

/**
 * Фикстуры на доменных типах — то, что отдаёт юзкейс хендлеру. Мало данных:
 * слово «perro» с одной лексемой, форма из «Перевода» и «Части речи», пакет с
 * одним словом. Тесты редьюсера работают на UI-типах (`Fx`), эти — для хендлера
 * и сквозного цикла.
 */
object DomainFx {
    const val TRANSLATION = 1L
    const val PART_OF_SPEECH = 2L
    const val NOUN = 21L
    const val GROUP = 701L
    const val PERRO_WORD = 100L

    val form = DictionaryForm(
        dictionaryId = Fx.DICT,
        translationLanguage = "ru",
        components = listOf(
            FormComponent(
                id = TRANSLATION,
                systemKey = BuiltInComponent.TRANSLATION,
                name = null,
                template = ComponentTemplate.TEXT,
                isCore = true,
                isMultiple = false,
                position = 0,
                dependsOn = DependencyTarget.Lexeme,
            ),
            FormComponent(
                id = PART_OF_SPEECH,
                systemKey = BuiltInComponent.PART_OF_SPEECH,
                name = null,
                template = ComponentTemplate.CHOICE,
                isCore = false,
                isMultiple = false,
                position = 1,
                dependsOn = DependencyTarget.Lexeme,
            ),
        ),
        options = mapOf(
            PART_OF_SPEECH to listOf(
                FormOption(id = NOUN, key = "noun", label = null, position = 0),
            ),
        ),
        groups = listOf(FormGroup(id = GROUP, name = "Harry Potter 1")),
        existingLemmas = listOf(
            ExistingLemma(
                wordId = PERRO_WORD,
                text = "perro",
                lexemeCount = 1,
                coreValues = listOf("пёс"),
                lexemeTextKeys = setOf(lexemeTextKey("пёс")),
                groupNames = emptyList(),
            ),
        ),
    )

    val perroLexeme = LookupLexeme(
        key = "ruwiktionary/1.noun.1",
        partOfSpeechKey = "noun",
        source = "ruwiktionary",
        translations = listOf("собака"),
        gloss = null,
        caption = null,
        notes = emptyList(),
        examples = emptyList(),
        grammar = emptyMap(),
        level = null,
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = listOf(
            ComponentMatch(componentId = TRANSLATION, optionId = null, field = "translations"),
            ComponentMatch(componentId = PART_OF_SPEECH, optionId = NOUN, field = null),
        ),
    )

    val perro = LookupWord(
        text = "perro",
        status = LookupWordStatus.FOUND,
        lemmas = listOf(
            LookupLemma(
                text = "perro",
                formNote = null,
                pronunciations = emptyList(),
                lexemes = listOf(perroLexeme),
            ),
        ),
        remarks = listOf(WordRemark.NoTranslation(lemma = "perro", lexemeKey = perroLexeme.key)),
    )

    val genderRecommendation = ComponentRecommendation(
        name = "Род",
        template = ComponentTemplate.CHOICE,
        options = listOf("м.р.", "ж.р."),
    )

    val batch = WordsBatch(
        dictionaryId = Fx.DICT,
        words = listOf(
            BatchWord(
                wordId = null,
                text = "perro",
                groupId = null,
                lexemes = listOf(
                    BatchLexeme(
                        components = listOf(
                            BatchComponent(TRANSLATION, TextValues(Primitive.Text("собака"))),
                        ),
                    ),
                ),
            ),
        ),
    )
}
