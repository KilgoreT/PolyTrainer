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
import me.apomazkin.lexeme.SavedLexeme
import me.apomazkin.lexeme.TextLemmas
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.lexeme.lexemeTextKey

/**
 * Фикстуры на доменных типах — то, что отдаёт юзкейс хендлеру: леммы текста
 * абзаца учебника и первого предложения снимка, форма из «Перевода» и «Части
 * речи» с леммой «study», слово ответа по лемме, лемма словаря «child» с
 * сохранённой лексемой, пакет записи. Тесты редьюсера работают на UI-типах
 * (`Fx`), эти — для хендлера и сквозного цикла.
 */
object DomainFx {
    const val TRANSLATION = 1L
    const val PART_OF_SPEECH = 2L
    const val NOUN = 21L
    const val GROUP = 701L
    const val STUDY_WORD = 103L
    const val CHILD_WORD = 104L

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
                wordId = STUDY_WORD,
                text = "study",
                lexemeCount = 1,
                coreValues = listOf("исследование"),
                lexemeTextKeys = setOf(lexemeTextKey("исследование")),
                groupNames = emptyList(),
                addedAt = 0L,
                lexemes = emptyList(),
            ),
        ),
    )

    /** Абзац учебника как прислал сервер (LFT-14.1). */
    val textbook = TextLemmas(
        text = Fx.TEXTBOOK_TEXT,
        translation = Fx.TEXTBOOK_TRANSLATION,
        lexical = Fx.textbookLexical,
        proper = Fx.textbookProper,
        function = Fx.textbookFunction,
        unparsed = emptyList(),
    )

    /** Первое предложение снимка из контракта LFT. */
    val sentence = TextLemmas(
        text = "Las nubes corrían por el cielo.",
        translation = "Облака бежали по небу.",
        lexical = listOf("nube", "correr", "cielo"),
        proper = emptyList(),
        function = listOf("el", "por"),
        unparsed = emptyList(),
    )

    /**
     * Слово ответа по лемме (LFL-103): одна лемма [lemma] с одной
     * лексемой-кандидатом «в тексте» — перевод [translation] в «Перевод» и опция
     * `noun` в «Часть речи».
     */
    fun lookupWord(
        text: String,
        lemma: String = text,
        translation: String = "перевод",
    ): LookupWord = LookupWord(
        text = text,
        status = LookupWordStatus.FOUND,
        lemmas = listOf(
            LookupLemma(
                text = lemma,
                formNote = if (lemma == text) null else "plural",
                pronunciations = emptyList(),
                lexemes = listOf(
                    LookupLexeme(
                        key = "ruwiktionary/1.noun.1",
                        partOfSpeechKey = "noun",
                        source = "ruwiktionary",
                        translations = listOf(translation),
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
                        inContext = true,
                    ),
                ),
            ),
        ),
        remarks = emptyList(),
    )

    /** Рекомендация ответа: завести «Род» с опциями (LFL-89). */
    val recommendation = ComponentRecommendation(
        name = "Род",
        template = ComponentTemplate.CHOICE,
        options = listOf("м.р.", "ж.р."),
    )

    /** Лемма словаря «child» с сохранённой лексемой «ребёнок» — ответ «children» → «child» (AWP-43.7). */
    val existingChild = ExistingLemma(
        wordId = CHILD_WORD,
        text = "child",
        lexemeCount = 1,
        coreValues = listOf("ребёнок"),
        lexemeTextKeys = setOf(lexemeTextKey("ребёнок")),
        groupNames = emptyList(),
        addedAt = 1_759_000_000_000L,
        lexemes = listOf(
            SavedLexeme(
                id = 9L,
                components = listOf(BatchComponent(TRANSLATION, TextValues(Primitive.Text("ребёнок")))),
            ),
        ),
    )

    /** Пакет записи нового слова [text] с одной лексемой «Перевод». */
    fun batch(text: String): WordsBatch = WordsBatch(
        dictionaryId = Fx.DICT,
        words = listOf(
            BatchWord(
                wordId = null,
                text = text,
                groupId = null,
                lexemes = listOf(
                    BatchLexeme(components = listOf(BatchComponent(TRANSLATION, TextValues(Primitive.Text("перевод"))))),
                ),
            ),
        ),
    )
}
