package me.apomazkin.wordcard.mate

import me.apomazkin.candidates.ComponentMatchUi
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.FormOptionUi
import me.apomazkin.candidates.LookupLemmaUi
import me.apomazkin.candidates.LookupLexemeUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTemplate.CAPTIONED_TEXT
import me.apomazkin.lexeme.ComponentTemplate.CHOICE
import me.apomazkin.lexeme.ComponentTemplate.TEXT
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.lexemeTextKey

/**
 * Фикстуры диалога лексем-кандидатов (WCL) на UI-типах. Слово «run» (id 7)
 * словаря 3 с одной лексемой «управлять»; ответы прода по форме — «run» →
 * «run» и «rin» (past participle), «running» → «run» (форма) и «running»,
 * «ran» → «run», «rin», «ran». Сопоставление — на компоненты словаря:
 * «Перевод» 17 (ядро), «Часть речи» 18, «Пример» 19, «Definition» 20 (ядро)
 * и «Заметка» 21 — зависит от «Примера», не от ядра (AWP-17.6). Ответы
 * контракта (`backend/docs/handbook/specs/lexemes-from-lemma/contract.md`, 5.2 и
 * 5.4) — «perro» и «zarpó» с деревом контракта `contractForm`.
 */
internal object CFx {
    const val DICT = 3L
    const val WORD = 7L
    const val TRANSLATION = 17L
    const val PART_OF_SPEECH = 18L
    const val EXAMPLE = 19L
    const val DEFINITION = 20L
    const val NOTE = 21L
    const val NOUN = 32L
    const val VERB = 31L

    // Дерево контракта: «Перевод» (1), «Часть речи» (2) с опциями 21–23,
    // «Род» (5) под опцией «существительное», «Пример» (3), «Толкование» (4).
    const val C_TRANSLATION = 1L
    const val C_PART_OF_SPEECH = 2L
    const val C_EXAMPLE = 3L
    const val C_GLOSS = 4L
    const val C_GENDER = 5L
    const val C_NOUN = 21L
    const val C_VERB = 22L
    const val C_ADJECTIVE = 23L
    const val C_MASCULINE = 51L

    // Форма словаря 3

    private fun component(
        id: Long,
        position: Int,
        template: ComponentTemplate,
        systemKey: BuiltInComponent? = null,
        name: String? = null,
        isCore: Boolean = false,
        isMultiple: Boolean = false,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
    ): FormComponentUi = FormComponentUi(
        id = id,
        systemKey = systemKey,
        name = name,
        template = template,
        isCore = isCore,
        isMultiple = isMultiple,
        position = position,
        dependsOn = dependsOn,
    )

    private fun existing(
        wordId: Long,
        text: String,
        values: List<String> = emptyList(),
    ): ExistingLemmaUi = ExistingLemmaUi(
        wordId = wordId,
        text = text,
        lexemeCount = values.size,
        coreValues = values,
        lexemeTextKeys = values.mapTo(HashSet()) { lexemeTextKey(it) },
        groupNames = emptyList(),
        addedAt = 0L,
        lexemes = emptyList(),
    )

    val form = DictionaryFormUi(
        dictionaryId = DICT,
        translationLanguage = "ru",
        components = listOf(
            component(TRANSLATION, 0, TEXT, BuiltInComponent.TRANSLATION, isCore = true),
            component(PART_OF_SPEECH, 1, CHOICE, BuiltInComponent.PART_OF_SPEECH),
            component(EXAMPLE, 2, CAPTIONED_TEXT, BuiltInComponent.EXAMPLE, isMultiple = true),
            component(
                id = NOTE,
                position = 0,
                template = TEXT,
                name = "Заметка",
                dependsOn = DependencyTarget.Component(ComponentTypeId(EXAMPLE)),
            ),
            component(DEFINITION, 3, TEXT, name = "Definition", isCore = true),
        ),
        options = mapOf(
            PART_OF_SPEECH to listOf(
                FormOptionUi(id = NOUN, key = "noun", label = null, position = 0),
                FormOptionUi(id = VERB, key = "verb", label = null, position = 1),
            ),
        ),
        groups = emptyList(),
        existingLemmas = listOf(existing(WORD, "run", listOf("управлять"))),
    )

    /** Форма без лексем у слова — пометок «есть» нет (WCL-06.3). */
    val emptyWordForm = form.copy(existingLemmas = listOf(existing(WORD, "run")))

    /** Слово «run» (id 8) с «бежать, бегать» и «управлять» — получатель переноса «running». */
    val runWordLemma = existing(8L, "run", listOf("бежать, бегать", "управлять"))

    /** Второе слово «run» (id 9) без лексем: одноимённые получатели различаются значениями. */
    val runWordTwin = existing(9L, "run")

    /** Форма слова «running» (id 7, «управлять») без одноимённых слов у лемм-форм. */
    val runningForm = form.copy(
        existingLemmas = listOf(existing(WORD, "running", listOf("управлять"))),
    )

    /** Форма слова «running» (id 7, «управлять») и слова словаря «run» (id 8). */
    val formWithRunWord = runningForm.copy(
        existingLemmas = runningForm.existingLemmas + runWordLemma,
    )

    /** Та же форма с двумя словами «run» (id 8 и 9). */
    val formWithTwoRunWords = formWithRunWord.copy(
        existingLemmas = formWithRunWord.existingLemmas + runWordTwin,
    )

    // Построители ответа

    fun byField(
        componentId: Long,
        field: String,
    ): ComponentMatchUi = ComponentMatchUi(componentId, optionId = null, field = field)

    fun byOption(
        componentId: Long,
        optionId: Long,
    ): ComponentMatchUi = ComponentMatchUi(componentId, optionId = optionId, field = null)

    fun lexeme(
        key: String,
        partOfSpeech: String? = "verb",
        translations: List<String> = emptyList(),
        gloss: String? = null,
        examples: List<String> = emptyList(),
        notes: List<String> = emptyList(),
        matches: List<ComponentMatchUi> = emptyList(),
    ): LookupLexemeUi = LookupLexemeUi(
        key = key,
        partOfSpeechKey = partOfSpeech,
        source = key.substringBefore('/'),
        translations = translations,
        gloss = gloss,
        caption = null,
        notes = notes,
        examples = examples,
        grammar = emptyMap(),
        level = null,
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = matches,
    )

    fun lemma(
        text: String,
        lexemes: List<LookupLexemeUi>,
        formNote: String? = null,
    ): LookupLemmaUi = LookupLemmaUi(
        text = text,
        formNote = formNote,
        pronunciations = emptyList(),
        lexemes = lexemes,
    )

    fun word(
        text: String,
        vararg lemmas: LookupLemmaUi,
        remarks: List<WordRemarkUi> = emptyList(),
    ): LookupWordUi = LookupWordUi(
        text = text,
        status = LookupWordStatus.FOUND,
        lemmas = lemmas.toList(),
        remarks = remarks,
    )

    private fun bareWord(
        text: String,
        status: LookupWordStatus,
    ): LookupWordUi = LookupWordUi(text, status, lemmas = emptyList(), remarks = emptyList())

    /** Адрес значения: лемма карточки, ключ лексемы, компонент, номер значения. */
    fun ref(
        lemma: String,
        key: String,
        componentId: Long,
        index: Int = 0,
    ): ValueRefUi = ValueRefUi(lemma = lemma, key = key, componentId = componentId, index = index)

    fun componentRef(
        lemma: String,
        key: String,
        componentId: Long,
    ): ComponentRefUi = ComponentRefUi(lemma = lemma, key = key, componentId = componentId)

    // Ответы по форме словаря 3: «Перевод» из переводов, «Часть речи» опцией, «Пример» из
    // примеров, «Definition» из толкования

    private val translationAndVerb = listOf(
        byField(TRANSLATION, "translations"),
        byOption(PART_OF_SPEECH, VERB),
    )

    /** Шесть лексем «run»: все приходят в раздел, «ещё N» нет (AWP-05 отменён). */
    val runLexemes = listOf(
        lexeme("ru/v1", translations = listOf("бежать, бегать"), matches = translationAndVerb),
        lexeme(
            key = "ru/v2",
            translations = listOf("управлять"),
            examples = listOf("to run a business"),
            matches = translationAndVerb + byField(EXAMPLE, "examples"),
        ),
        lexeme("ru/v3", translations = listOf("проехать"), matches = translationAndVerb),
        lexeme(
            key = "en/g1",
            gloss = "To move swiftly.",
            matches = listOf(byField(DEFINITION, "gloss"), byOption(PART_OF_SPEECH, VERB)),
        ),
        lexeme(
            key = "en/v1",
            translations = listOf("бегать, побегать"),
            gloss = "To move forward quickly.",
            examples = listOf("Run!", "Run, Sarah!"),
            matches = translationAndVerb +
                byField(EXAMPLE, "examples") +
                byField(DEFINITION, "gloss"),
        ),
        lexeme(
            key = "ru/n1",
            partOfSpeech = "noun",
            translations = listOf("бег"),
            matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, NOUN)),
        ),
    )

    val rinLemma = lemma(
        text = "rin",
        formNote = "past participle",
        lexemes = listOf(
            lexeme(
                key = "en/rin1",
                gloss = "To run.",
                matches = listOf(byField(DEFINITION, "gloss"), byOption(PART_OF_SPEECH, VERB)),
            ),
        ),
    )

    val runWord = word("run", lemma("run", runLexemes), rinLemma)

    val runningWord = word(
        "running",
        lemma("run", runLexemes.take(3), formNote = "present participle and gerund"),
        lemma(
            text = "running",
            lexemes = listOf(
                lexeme(
                    key = "ru/a1",
                    partOfSpeech = "adjective",
                    translations = listOf("бегущий"),
                    matches = listOf(byField(TRANSLATION, "translations")),
                ),
            ),
        ),
    )

    val ranWord = word(
        "ran",
        lemma("run", runLexemes.take(2), formNote = "simple past"),
        rinLemma,
        lemma(
            text = "ran",
            lexemes = listOf(
                lexeme(
                    key = "en/ran1",
                    partOfSpeech = "noun",
                    gloss = "Yarns coiled on a spun-yarn winch.",
                    matches = listOf(byField(DEFINITION, "gloss"), byOption(PART_OF_SPEECH, NOUN)),
                ),
            ),
        ),
    )

    val formsOnlyWord = word(
        "ran",
        lemma("run", runLexemes.take(2), formNote = "simple past"),
        rinLemma,
    )

    /** «run» с одной лексемой: перевод, один пример и зависимая от примера «Заметка» (AWP-17.6). */
    val noteWord = word(
        "run",
        lemma(
            text = "run",
            lexemes = listOf(
                lexeme(
                    key = "ru/n2",
                    translations = listOf("бег"),
                    examples = listOf("утренний бег"),
                    notes = listOf("спорт"),
                    matches = listOf(
                        byField(TRANSLATION, "translations"),
                        byField(EXAMPLE, "examples"),
                        byField(NOTE, "notes"),
                    ),
                ),
            ),
        ),
    )

    /** «qwrtzk» `not_found` (WCL-11.1): лексем-кандидатов нет. */
    val notFoundWord = bareWord("qwrtzk", LookupWordStatus.NOT_FOUND)

    /** «bank» `found` без лемм (LFL-100.2): лексем-кандидатов нет. */
    val emptyLemmasWord = bareWord("bank", LookupWordStatus.FOUND)

    // Дерево и ответы контракта

    /** Дерево контракта в порядке обхода [1, 2, 5, 3, 4]; слово «perro» (id 7) без лексем. */
    val contractForm = DictionaryFormUi(
        dictionaryId = DICT,
        translationLanguage = "ru",
        components = listOf(
            component(C_TRANSLATION, 0, TEXT, BuiltInComponent.TRANSLATION, isCore = true),
            component(C_PART_OF_SPEECH, 1, CHOICE, BuiltInComponent.PART_OF_SPEECH),
            component(
                id = C_GENDER,
                position = 2,
                template = CHOICE,
                name = "Род",
                dependsOn = DependencyTarget.Option(C_NOUN),
            ),
            component(C_EXAMPLE, 3, CAPTIONED_TEXT, BuiltInComponent.EXAMPLE, isMultiple = true),
            component(C_GLOSS, 4, TEXT, name = "Толкование"),
        ),
        options = mapOf(
            C_PART_OF_SPEECH to listOf(
                FormOptionUi(id = C_NOUN, key = "noun", label = null, position = 0),
                FormOptionUi(id = C_VERB, key = "verb", label = null, position = 1),
                FormOptionUi(id = C_ADJECTIVE, key = "adjective", label = null, position = 2),
            ),
            C_GENDER to listOf(
                FormOptionUi(id = C_MASCULINE, key = null, label = "м.р.", position = 0),
                FormOptionUi(id = 52L, key = null, label = "ж.р.", position = 1),
                FormOptionUi(id = 53L, key = null, label = "общ.", position = 2),
            ),
        ),
        groups = emptyList(),
        existingLemmas = listOf(existing(WORD, "perro")),
    )

    /** «perro», контракт 5.2: существительное с родом, прилагательное, второе существительное. */
    val perro = word(
        "perro",
        lemma(
            text = "perro",
            lexemes = listOf(
                lexeme(
                    key = "ruwiktionary/1.noun.1",
                    partOfSpeech = "noun",
                    translations = listOf("собака; пёс (также ругательство)"),
                    gloss = "dog (the species Canis familiaris …)",
                    examples = listOf("perro callejero"),
                    matches = listOf(
                        byField(C_TRANSLATION, "translations"),
                        byOption(C_PART_OF_SPEECH, C_NOUN),
                        byOption(C_GENDER, C_MASCULINE),
                        byField(C_EXAMPLE, "examples"),
                        byField(C_GLOSS, "gloss"),
                    ),
                ),
                lexeme(
                    key = "ruwiktionary/2.adjective.1",
                    partOfSpeech = "adjective",
                    translations = listOf("дурной, скверный, паршивый; несладкий, собачий"),
                    gloss = "wicked; mean",
                    matches = listOf(
                        byField(C_TRANSLATION, "translations"),
                        byOption(C_PART_OF_SPEECH, C_ADJECTIVE),
                        byField(C_GLOSS, "gloss"),
                    ),
                ),
                lexeme(
                    key = "ruwiktionary/1.noun.2",
                    partOfSpeech = "noun",
                    translations = listOf("надувательство, обман"),
                    matches = listOf(
                        byField(C_TRANSLATION, "translations"),
                        byOption(C_PART_OF_SPEECH, C_NOUN),
                        byOption(C_GENDER, C_MASCULINE),
                    ),
                ),
            ),
        ),
    )

    private val zarpoMatches = listOf(
        byField(C_TRANSLATION, "gloss"),
        byOption(C_PART_OF_SPEECH, C_VERB),
        byField(C_GLOSS, "gloss"),
    )

    /** «zarpó», контракт 5.4: русского значения нет, английское толкование, обе с «англ.». */
    val zarpo = word(
        "zarpó",
        lemma(
            text = "zarpar",
            formNote = "third-person singular preterite indicative",
            lexemes = listOf(
                lexeme(
                    key = "enwiktionary/1.verb.1",
                    gloss = "to embark; to set sail",
                    matches = zarpoMatches,
                ),
                lexeme(
                    key = "enwiktionary/2.verb.1",
                    gloss = "to behave in a daring and out of place way",
                    matches = zarpoMatches,
                ),
            ),
        ),
        remarks = listOf(
            WordRemarkUi.NoTranslation(lemma = "zarpar", lexemeKey = "enwiktionary/1.verb.1"),
            WordRemarkUi.NoTranslation(lemma = "zarpar", lexemeKey = "enwiktionary/2.verb.1"),
        ),
    )

    /** Рекомендация ответа, контракт 5.6: завести «Род» с опциями. */
    val genderRecommendation = ComponentRecommendationUi(
        name = "Род",
        template = CHOICE,
        options = listOf("м.р.", "ж.р."),
    )
}
