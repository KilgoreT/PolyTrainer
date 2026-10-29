package me.apomazkin.candidates

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.lexemeTextKey

/**
 * Фикстуры окна лексем-кандидатов. Дерево компонентов и ответы сервера — из
 * контракта `backend/docs/handbook/specs/lexemes-from-lemma/contract.md`:
 * «Перевод» (1), «Часть речи» (2) с опциями noun/verb/adjective (21–23),
 * «Род» (5) под опцией «существительное» с опциями 51–53, «Пример» (3, текст
 * с подписью, несколько), «Толкование» (4); слова «corrían», «perro»,
 * «zarpó», «accorc», «llave», «banco». Слова английского словаря («ran»,
 * «running», «bank», «tiempo», «llegar» …) — из примеров спеки AWP.
 */
object Fx {
    const val DICT = 1L

    const val TRANSLATION = 1L
    const val PART_OF_SPEECH = 2L
    const val EXAMPLE = 3L
    const val GLOSS = 4L
    const val GENDER = 5L

    /**
     * Компоненты отдельных примеров: непонятный, «Стиль» (несколько),
     * «Транскрипция», ядро «Определение».
     */
    const val UNCLEAR = 7L
    const val STYLE = 8L
    const val TRANSCRIPTION = 9L
    const val DEFINITION = 10L

    const val NOUN = 21L
    const val VERB = 22L
    const val ADJECTIVE = 23L

    const val MASCULINE = 51L
    const val FEMININE = 52L
    const val COMMON = 53L

    const val FIGURATIVE = 81L
    const val COLLOQUIAL = 82L

    const val POTTER = 701L
    const val FINANCE = 702L

    const val RUN_WORD = 100L
    const val BANK_1 = 101L
    const val BANK_2 = 102L
    const val CORRER_WORD = 103L
    const val CORRIAN_WORD = 104L

    // Компоненты

    val translation = component(
        id = TRANSLATION,
        systemKey = BuiltInComponent.TRANSLATION,
        name = null,
        template = ComponentTemplate.TEXT,
        isCore = true,
        position = 0,
    )
    val partOfSpeech = component(
        id = PART_OF_SPEECH,
        systemKey = BuiltInComponent.PART_OF_SPEECH,
        name = null,
        template = ComponentTemplate.CHOICE,
        isCore = false,
        position = 1,
    )
    val gender = component(
        id = GENDER,
        systemKey = null,
        name = "Род",
        template = ComponentTemplate.CHOICE,
        isCore = false,
        position = 2,
        dependsOn = DependencyTarget.Option(NOUN),
    )
    val example = component(
        id = EXAMPLE,
        systemKey = BuiltInComponent.EXAMPLE,
        name = null,
        template = ComponentTemplate.CAPTIONED_TEXT,
        isCore = false,
        position = 3,
        isMultiple = true,
    )
    val gloss = component(
        id = GLOSS,
        systemKey = null,
        name = "Толкование",
        template = ComponentTemplate.TEXT,
        isCore = false,
        position = 4,
    )

    /** Дерево контракта в порядке обхода: корни по позиции, зависимые сразу за родителем. */
    val tree = listOf(translation, partOfSpeech, gender, example, gloss)

    val unclear = component(
        id = UNCLEAR,
        systemKey = null,
        name = "Жопа",
        template = ComponentTemplate.TEXT,
        isCore = false,
        position = 5,
    )
    val style = component(
        id = STYLE,
        systemKey = null,
        name = "Стиль",
        template = ComponentTemplate.CHOICE,
        isCore = false,
        position = 6,
        isMultiple = true,
    )
    val transcription = component(
        id = TRANSCRIPTION,
        systemKey = null,
        name = "Транскрипция",
        template = ComponentTemplate.TEXT,
        isCore = false,
        position = 7,
    )
    val definition = component(
        id = DEFINITION,
        systemKey = null,
        name = "Определение",
        template = ComponentTemplate.TEXT,
        isCore = true,
        position = 0,
    )

    /** «Род» текстовым компонентом (AWP-29.14) вместо «Выбора». */
    val genderText = component(
        id = GENDER,
        systemKey = null,
        name = "Род",
        template = ComponentTemplate.TEXT,
        isCore = false,
        position = 2,
    )

    // Опции

    val options: Map<Long, List<FormOptionUi>> = mapOf(
        PART_OF_SPEECH to listOf(
            FormOptionUi(NOUN, key = "noun", label = null, position = 0),
            FormOptionUi(VERB, key = "verb", label = null, position = 1),
            FormOptionUi(ADJECTIVE, key = "adjective", label = null, position = 2),
        ),
        GENDER to listOf(
            FormOptionUi(MASCULINE, key = null, label = "м.р.", position = 0),
            FormOptionUi(FEMININE, key = null, label = "ж.р.", position = 1),
            FormOptionUi(COMMON, key = null, label = "общ.", position = 2),
        ),
        STYLE to listOf(
            FormOptionUi(FIGURATIVE, key = null, label = "перен.", position = 0),
            FormOptionUi(COLLOQUIAL, key = null, label = "разг.", position = 1),
        ),
    )

    val groups = listOf(FormGroupUi(POTTER, "Harry Potter 1"), FormGroupUi(FINANCE, "Finance"))

    // Слова словаря

    val runExisting = existing(RUN_WORD, "run", listOf("управлять"), groups = listOf("Finance"))
    val bank1 = existing(BANK_1, "bank", listOf("банк", "банкомёт"), groups = listOf("Finance"))
    val bank2 = existing(BANK_2, "bank", listOf("берег"))

    /** «correr» словаря: значение «бегать; бежать» совпадает с кандидатом `1.verb.1` «corrían». */
    val correrExisting = existing(CORRER_WORD, "correr", listOf("бегать; бежать"))

    fun form(
        components: List<FormComponentUi> = tree,
        options: Map<Long, List<FormOptionUi>> = this.options,
        groups: List<FormGroupUi> = this.groups,
        existing: List<ExistingLemmaUi> = emptyList(),
        translationLanguage: String = "ru",
    ): DictionaryFormUi = DictionaryFormUi(
        dictionaryId = DICT,
        translationLanguage = translationLanguage,
        components = components,
        options = options,
        groups = groups,
        existingLemmas = existing,
    )

    fun component(
        id: Long,
        systemKey: BuiltInComponent?,
        name: String?,
        template: ComponentTemplate,
        isCore: Boolean,
        position: Int,
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

    fun existing(
        wordId: Long,
        text: String,
        values: List<String>,
        groups: List<String> = emptyList(),
        addedAt: Long = 0L,
        lexemes: List<SavedLexemeUi> = emptyList(),
    ): ExistingLemmaUi = ExistingLemmaUi(
        wordId = wordId,
        text = text,
        lexemeCount = values.size,
        coreValues = values,
        lexemeTextKeys = values.mapTo(HashSet()) { lexemeTextKey(it) },
        groupNames = groups,
        addedAt = addedAt,
        lexemes = lexemes,
    )

    /** Сохранённая лексема словаря с одним текстом «Перевода» — для карточки «в словаре» (AWP-46). */
    fun saved(
        id: Long,
        translation: String,
    ): SavedLexemeUi = SavedLexemeUi(
        id = id,
        values = listOf(
            CandidateValueUiItem.Text(
                componentId = TRANSLATION,
                texts = listOf(EditableTextUiItem.of(translation)),
                field = null,
            ),
        ),
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
        translations: List<String> = emptyList(),
        gloss: String? = null,
        examples: List<String> = emptyList(),
        notes: List<String> = emptyList(),
        grammar: Map<String, String> = emptyMap(),
        synonyms: List<String> = emptyList(),
        partOfSpeechKey: String? = null,
        matches: List<ComponentMatchUi> = emptyList(),
    ): LookupLexemeUi = LookupLexemeUi(
        key = key,
        partOfSpeechKey = partOfSpeechKey,
        source = key.substringBefore('/'),
        translations = translations,
        gloss = gloss,
        caption = null,
        notes = notes,
        examples = examples,
        grammar = grammar,
        level = null,
        synonyms = synonyms,
        antonyms = emptyList(),
        componentMatches = matches,
    )

    fun lemma(
        text: String,
        lexemes: List<LookupLexemeUi>,
        formNote: String? = null,
        pronunciations: List<String> = emptyList(),
    ): LookupLemmaUi = LookupLemmaUi(
        text = text,
        formNote = formNote,
        pronunciations = pronunciations,
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

    fun notFound(text: String): LookupWordUi = bare(text, LookupWordStatus.NOT_FOUND)

    fun failed(text: String): LookupWordUi = bare(text, LookupWordStatus.ERROR)

    /** Найденное слово без лемм (LFL-100.2). */
    fun foundWithoutLemmas(text: String): LookupWordUi = bare(text, LookupWordStatus.FOUND)

    private fun bare(
        text: String,
        status: LookupWordStatus,
    ): LookupWordUi = LookupWordUi(text, status, lemmas = emptyList(), remarks = emptyList())

    fun noTranslation(
        lemma: String,
        lexemeKey: String,
    ): WordRemarkUi.NoTranslation = WordRemarkUi.NoTranslation(lemma = lemma, lexemeKey = lexemeKey)

    /** Сопоставление глагола: переводы, глагол, толкование — в свои компоненты. */
    private fun verbMatches(): List<ComponentMatchUi> = listOf(
        byField(TRANSLATION, "translations"),
        byOption(PART_OF_SPEECH, VERB),
        byField(GLOSS, "gloss"),
    )

    // «corrían», контракт 5.1: лемма «correr», три значения Русского Викисловаря

    val corrianVerb3 = lexeme(
        key = "ruwiktionary/1.verb.3",
        translations = listOf("течь, литься, струиться (о жидкости)"),
        gloss = "to flow",
        partOfSpeechKey = "verb",
        matches = verbMatches(),
    )
    val corrianVerb1 = lexeme(
        key = "ruwiktionary/1.verb.1",
        translations = listOf("бегать; бежать"),
        gloss = "to run, to jog",
        partOfSpeechKey = "verb",
        matches = verbMatches(),
    )
    val corrianVerb5 = lexeme(
        key = "ruwiktionary/1.verb.5",
        translations = listOf("бежать, проходить, лететь (о времени)"),
        gloss = "to elapse (time)",
        partOfSpeechKey = "verb",
        matches = verbMatches(),
    )
    val corrian = word(
        "corrían",
        lemma(
            text = "correr",
            lexemes = listOf(corrianVerb3, corrianVerb1, corrianVerb5),
            formNote = "third-person plural imperfect indicative",
        ),
    )

    // «perro», контракт 5.2: существительное с родом, прилагательное, второе существительное

    val perroNoun1 = lexeme(
        key = "ruwiktionary/1.noun.1",
        translations = listOf("собака; пёс (также ругательство)"),
        gloss = "dog (the species Canis familiaris …)",
        examples = listOf("perro callejero"),
        grammar = mapOf("gender" to "masculine"),
        partOfSpeechKey = "noun",
        matches = listOf(
            byField(TRANSLATION, "translations"),
            byOption(PART_OF_SPEECH, NOUN),
            byOption(GENDER, MASCULINE),
            byField(EXAMPLE, "examples"),
            byField(GLOSS, "gloss"),
        ),
    )
    val perroAdjective1 = lexeme(
        key = "ruwiktionary/2.adjective.1",
        translations = listOf("дурной, скверный, паршивый; несладкий, собачий"),
        gloss = "wicked; mean",
        notes = listOf("разг."),
        partOfSpeechKey = "adjective",
        matches = listOf(
            byField(TRANSLATION, "translations"),
            byOption(PART_OF_SPEECH, ADJECTIVE),
            byField(GLOSS, "gloss"),
        ),
    )
    val perroNoun2 = lexeme(
        key = "ruwiktionary/1.noun.2",
        translations = listOf("надувательство, обман"),
        partOfSpeechKey = "noun",
        matches = listOf(
            byField(TRANSLATION, "translations"),
            byOption(PART_OF_SPEECH, NOUN),
            byOption(GENDER, MASCULINE),
        ),
    )
    val perro = word("perro", lemma("perro", listOf(perroNoun1, perroAdjective1, perroNoun2)))

    // «zarpó», контракт 5.4: русского значения нет, английское с пометкой

    val zarpoVerb1 = lexeme(
        key = "enwiktionary/1.verb.1",
        gloss = "to embark; to set sail",
        partOfSpeechKey = "verb",
        matches = listOf(
            byField(TRANSLATION, "gloss"),
            byOption(PART_OF_SPEECH, VERB),
            byField(GLOSS, "gloss"),
        ),
    )
    val zarpoVerb2 = lexeme(
        key = "enwiktionary/2.verb.1",
        gloss = "to behave in a daring and out of place way",
        partOfSpeechKey = "verb",
        matches = listOf(
            byField(TRANSLATION, "gloss"),
            byOption(PART_OF_SPEECH, VERB),
            byField(GLOSS, "gloss"),
        ),
    )
    val zarpo = word(
        "zarpó",
        lemma(
            text = "zarpar",
            lexemes = listOf(zarpoVerb1, zarpoVerb2),
            formNote = "third-person singular preterite indicative",
        ),
        remarks = listOf(
            noTranslation("zarpar", "enwiktionary/1.verb.1"),
            noTranslation("zarpar", "enwiktionary/2.verb.1"),
        ),
    )

    // Контракт 5.5 и 5.3: не найдено, ошибка источника, нейронка упала на вызове

    val accorc = notFound("accorc")
    val llave = failed("llave")
    val banco = failed("banco")

    /** Рекомендация ответа, контракт 5.6: завести «Род» с опциями. */
    val genderRecommendation = ComponentRecommendationUi(
        name = "Род",
        template = ComponentTemplate.CHOICE,
        options = listOf("м.р.", "ж.р."),
    )

    // Английский словарь, спека AWP

    val runVerb1 = lexeme(
        key = "ruwiktionary/1.verb.1",
        translations = listOf("бежать, бегать"),
        partOfSpeechKey = "verb",
        matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, VERB)),
    )
    val runVerb2 = lexeme(
        key = "ruwiktionary/1.verb.2",
        translations = listOf("управлять"),
        examples = listOf("to run a business"),
        partOfSpeechKey = "verb",
        matches = listOf(
            byField(TRANSLATION, "translations"),
            byOption(PART_OF_SPEECH, VERB),
            byField(EXAMPLE, "examples"),
        ),
    )

    /** Тот же ключ `1.verb.1`, но другие данные: у «running» он не перебивает «ran» (AWP-04.8). */
    val runVerb1FromRunning = lexeme(
        key = "ruwiktionary/1.verb.1",
        translations = listOf("бегущий"),
        partOfSpeechKey = "adjective",
        matches = listOf(
            byField(TRANSLATION, "translations"),
            byOption(PART_OF_SPEECH, ADJECTIVE),
        ),
    )
    val runMachine = lexeme(
        key = "enwiktionary/1.verb.37",
        translations = listOf("работать", "функционировать", "идти"),
        gloss = "Of a machine, including computer programs, to be operating or working normally.",
        partOfSpeechKey = "verb",
        matches = verbMatches(),
    )
    val runningNoun = lexeme(
        key = "enwiktionary/1.noun.1",
        translations = listOf("бег"),
        gloss = "The action of the verb to run.",
        partOfSpeechKey = "noun",
        matches = listOf(
            byField(TRANSLATION, "translations"),
            byOption(PART_OF_SPEECH, NOUN),
            byField(GLOSS, "gloss"),
        ),
    )

    val ran = word("ran", lemma("run", listOf(runVerb1, runVerb2), formNote = "simple past"))
    val running = word(
        "running",
        lemma(
            text = "run",
            lexemes = listOf(runVerb1FromRunning, runMachine),
            formNote = "present participle and gerund",
        ),
        lemma("running", listOf(runningNoun)),
    )

    /** Тот же «run» из ещё одного слова с теми же лексемами (AWP-04.3). */
    val runs = word(
        "runs",
        lemma("run", listOf(runVerb1, runVerb2), formNote = "third-person singular"),
    )

    val bankNoun = lexeme(
        key = "ruwiktionary/1.noun.1",
        translations = listOf("банк (финансовое учреждение)"),
        partOfSpeechKey = "noun",
        matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, NOUN)),
    )
    val bank = word("bank", lemma("bank", listOf(bankNoun)))

    val fuera = word(
        "fuera",
        lemma(
            text = "fuera",
            lexemes = listOf(
                lexeme(
                    key = "ruwiktionary/1.adverb.1",
                    translations = listOf("снаружи"),
                    partOfSpeechKey = "adverb",
                    matches = listOf(byField(TRANSLATION, "translations")),
                ),
            ),
        ),
        lemma(
            text = "ser",
            lexemes = listOf(
                lexeme(
                    key = "enwiktionary/1.verb.1",
                    gloss = "to be",
                    partOfSpeechKey = "verb",
                    matches = listOf(byField(TRANSLATION, "gloss"), byOption(PART_OF_SPEECH, VERB)),
                ),
            ),
            formNote = "first/third-person singular imperfect subjunctive",
        ),
        lemma(
            text = "ir",
            lexemes = listOf(
                lexeme(
                    key = "enwiktionary/1.verb.1",
                    gloss = "to go",
                    partOfSpeechKey = "verb",
                    matches = listOf(byField(TRANSLATION, "gloss"), byOption(PART_OF_SPEECH, VERB)),
                ),
            ),
            formNote = "first/third-person singular imperfect subjunctive",
        ),
    )

    val london = word(
        "London",
        lemma(
            text = "London",
            lexemes = listOf(
                lexeme(
                    key = "enwiktionary/1.noun.1",
                    translations = listOf("Лондон"),
                    partOfSpeechKey = "noun",
                    matches = listOf(
                        byField(TRANSLATION, "translations"),
                        byOption(PART_OF_SPEECH, NOUN),
                    ),
                ),
            ),
        ),
    )

    /** «los» → «el»: артикль без части речи (AWP-17.2). */
    val los = word(
        "los",
        lemma(
            text = "el",
            lexemes = listOf(
                lexeme(
                    key = "enwiktionary/1.article.1",
                    gloss = "the",
                    partOfSpeechKey = null,
                    matches = listOf(byField(TRANSLATION, "gloss")),
                ),
            ),
        ),
    )

    /** «tiempo», `ruwiktionary/1.noun.3` «погода» с тремя примерами (AWP-15.2, AWP-29.6). */
    val tiempoExamples = listOf(
        "¡Hoy hace un tiempo de perro!",
        "¿Qué tiempo hace?",
        "Hace buen tiempo.",
    )
    val tiempo = word(
        "tiempo",
        lemma(
            text = "tiempo",
            lexemes = listOf(
                lexeme(
                    key = "ruwiktionary/1.noun.3",
                    translations = listOf("погода"),
                    examples = tiempoExamples,
                    partOfSpeechKey = "noun",
                    matches = listOf(
                        byField(TRANSLATION, "translations"),
                        byOption(PART_OF_SPEECH, NOUN),
                        byField(EXAMPLE, "examples"),
                    ),
                ),
            ),
        ),
    )

    /** «estudiante», `ruwiktionary/1.noun.2` «студентка» с «Толкованием» (AWP-07.11). */
    val estudiante = word(
        "estudiante",
        lemma(
            text = "estudiante",
            lexemes = listOf(
                lexeme(
                    key = "ruwiktionary/1.noun.2",
                    translations = listOf("студентка"),
                    gloss = "student (a person who studies…)",
                    partOfSpeechKey = "noun",
                    matches = listOf(byField(TRANSLATION, "translations"), byField(GLOSS, "gloss")),
                ),
            ),
        ),
    )

    /** «llegar»: «Стиль» с несколькими значениями «перен.» и «разг.» (AWP-15.7, AWP-32.8). */
    val llegar = word(
        "llegar",
        lemma(
            text = "llegar",
            lexemes = listOf(
                lexeme(
                    key = "ruwiktionary/1.verb.1",
                    translations = listOf("прибывать"),
                    partOfSpeechKey = "verb",
                    matches = listOf(
                        byField(TRANSLATION, "translations"),
                        byOption(STYLE, FIGURATIVE),
                        byOption(STYLE, COLLOQUIAL),
                    ),
                ),
            ),
        ),
    )

    /** Форма со «Стилем» (выбор, несколько) после «Перевода». */
    fun formWithStyle(): DictionaryFormUi = form(components = listOf(translation, style))

    /** «run», `enwiktionary/1.verb.1`: переводы списком в «Перевод» (AWP-29.9). */
    val runEnglishVerb = lexeme(
        key = "enwiktionary/1.verb.1",
        translations = listOf("бегать", "побегать", "бежать", "побежать"),
        gloss = "To move forward quickly upon two feet by alternately making a short jump.",
        partOfSpeechKey = "verb",
        matches = listOf(byField(TRANSLATION, "translations")),
    )
    val runEnglish = word("run", lemma("run", listOf(runEnglishVerb)))

    // Сборка

    /**
     * Карточки, как их собирает цепочка атомов экрана по слову: пустая карточка
     * леммы → карточка из ответа → активность, «уже в словаре» по выбранной
     * лемме словаря (AWP-50), ядро. Слово — одна карточка его первой леммы с
     * лексемами.
     */
    fun cards(
        vararg words: LookupWordUi,
        form: DictionaryFormUi = Fx.form(),
    ): List<LemmaCardUiItem> = words
        .map { word -> lemmaCardOf(word, emptyLemmaCard(word.text, form), form) }
        .withActiveValues(form)
        .map { card -> listOf(card).withExistsMarks(form.lexemeKeysOf(card.target), form).single() }
        .withCoreMarks(form)

    /** Адрес значения: лемма, ключ лексемы, компонент, номер значения. */
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

    /** Минимальный вид окна для тестов умолчаний. */
    fun view(
        form: DictionaryFormUi? = null,
        cards: List<LemmaCardUiItem> = emptyList(),
    ): CandidatesView = CandidatesView(
        form = form,
        cards = cards,
        summary = AddSummaryUi(),
        sheet = null,
        isAdding = false,
        addFailure = null,
        isConfirmingClose = false,
    )
}

// Чтение результата в тестах

/** Лексема-кандидат карточки по лемме и ключу. */
fun List<LemmaCardUiItem>.candidate(
    lemma: String,
    key: String,
): CardLexemeUiItem = lexemeOf(lemma, key) ?: error("no lexeme $lemma / $key")

/** Тексты значения-текста компонента у лексемы. */
fun CardLexemeUiItem.texts(componentId: Long): List<String> {
    val value = values.first { it.componentId == componentId }
    return (value as CandidateValueUiItem.Text).texts.map { it.text }
}

/** Опции значения-выбора компонента у лексемы. */
fun CardLexemeUiItem.chosen(componentId: Long): List<Long> {
    val value = values.first { it.componentId == componentId }
    return (value as CandidateValueUiItem.Choice).options
}

/** Компоненты, у которых лексема показывает значение (активные), в порядке показа. */
fun CardLexemeUiItem.shownComponents(): List<Long> =
    values.filter { it.isActive }.map { it.componentId }

/**
 * Что видит пользователь у лексемы: активные значения строками «Имя: значение»
 * в порядке компонентов, как в карточке слова (AWP-29); у «Выбора» без
 * значения — «без значения».
 */
fun CardLexemeUiItem.shown(form: DictionaryFormUi): List<String> = values
    .filter { it.isActive }
    .flatMap { value ->
        val component = form.componentOf(value.componentId)
            ?: error("no component ${value.componentId}")
        val name = nameOf(component)
        when (value) {
            is CandidateValueUiItem.Text -> value.texts.map { "$name: ${it.text}" }
            is CandidateValueUiItem.Choice -> if (value.options.isEmpty()) {
                listOf("$name: без значения")
            } else {
                value.options.map { "$name: ${optionName(form, it)}" }
            }
        }
    }

/**
 * Строка раздела «Сопоставление» так, как её видит пользователь (AWP-31):
 * «поле → компонент» или «компонент: подпись опции».
 */
fun ComponentMatchLineUiItem.shown(form: DictionaryFormUi): String = when (this) {
    is ComponentMatchLineUiItem.FieldToComponent ->
        "$field → ${nameOf(form.requireComponent(componentId))}"
    is ComponentMatchLineUiItem.ComponentOption ->
        "${nameOf(form.requireComponent(componentId))}: ${optionName(form, optionId)}"
}

private fun DictionaryFormUi.requireComponent(componentId: Long): FormComponentUi =
    componentOf(componentId) ?: error("no component $componentId")

/** Имя компонента, как его даёт интерфейс: встроенные — ресурсами, свои — из формы. */
private fun nameOf(component: FormComponentUi): String = when (component.systemKey) {
    BuiltInComponent.TRANSLATION -> "Перевод"
    BuiltInComponent.PART_OF_SPEECH -> "Часть речи"
    BuiltInComponent.EXAMPLE -> "Пример"
    null -> component.name.orEmpty()
}

/** Подпись опции, как её даёт интерфейс: встроенные «Части речи» — словами, свои — из формы. */
private fun optionName(
    form: DictionaryFormUi,
    optionId: Long,
): String {
    val option = form.options.values.flatten().first { it.id == optionId }
    return option.label ?: when (option.key) {
        "noun" -> "существительное"
        "verb" -> "глагол"
        "adjective" -> "прилагательное"
        else -> option.key.orEmpty()
    }
}
