package me.apomazkin.scancandidates.mate

import me.apomazkin.candidates.CandidateValueUiItem
import me.apomazkin.candidates.CardLexemeUiItem
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.ComponentMatchUi
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.EditableTextUiItem
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.FormGroupUi
import me.apomazkin.candidates.FormOptionUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.LookupLemmaUi
import me.apomazkin.candidates.LookupLexemeUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.SavedLexemeUi
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.candidates.componentOf
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.lexemeTextKey

/**
 * Фикстуры экрана на UI-типах. Ответ на токены — абзац учебника из контрольного
 * набора `backend/docs/features/IS536_llm_values/control_set.md` (24 смысловых,
 * 4 имени, 13 служебных, LFT-14.1) и «не разобрано» плотной страницы
 * `es_dense_screenshot` (LFT-13.1). Дерево компонентов и ответы по лемме — из
 * контракта `backend/docs/handbook/specs/lexemes-from-lemma/contract.md`:
 * «Перевод» (1), «Часть речи» (2) с опциями noun/verb/adjective (21–23), «Род»
 * (5) под опцией «существительное» с опциями 51–53, «Пример» (3, текст с
 * подписью, несколько), «Толкование» (4); слова «corrían», «perro», «zarpó»;
 * слова английского словаря («ran», «run», «children», «bank», «tiempo»,
 * «llegar») — из примеров спеки AWP. Группы «Harry Potter 1» и «Finance».
 */
object Fx {
    const val DICT = 1L
    const val URI = "content://photo/1"

    const val TRANSLATION = 1L
    const val PART_OF_SPEECH = 2L
    const val EXAMPLE = 3L
    const val GLOSS = 4L
    const val GENDER = 5L

    /** Компоненты отдельных примеров: непонятный, «Стиль» (несколько), ядро «Определение». */
    const val UNCLEAR = 7L
    const val STYLE = 8L
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
    const val STUDY_WORD = 103L
    const val CHILD_WORD = 104L

    // Токены снимка

    val tokens = listOf(
        LookupToken(text = "perro", confidence = 0.9f, language = "es"),
        LookupToken(text = "corrían", confidence = 0.8f, language = "es"),
    )

    // Ответ на токены: абзац учебника

    const val TEXTBOOK_TEXT =
        "Study links early friendships with high-quality sibling relationships " +
        "Children who experience a rewarding friendship before the birth of a sibling are likely " +
        "to have a better relationship with that brother or sister that endures throughout their " +
        "childhood, said Laurie Kramer in a University of Illinois study published in December's " +
        "Journal of Family Psychology."

    const val TEXTBOOK_TRANSLATION = "Исследование связывает раннюю дружбу с качественными " +
        "отношениями между братьями и сёстрами."

    val textbookLexical = listOf(
        "study", "link", "early", "friendship", "high-quality", "sibling", "relationship", "child",
        "experience", "rewarding", "birth", "likely", "have", "good", "brother", "sister", "endure",
        "childhood", "say", "university", "publish", "journal", "family", "psychology",
    )

    val textbookProper = listOf("Laurie", "Kramer", "Illinois", "December")

    val textbookFunction = listOf(
        "with", "who", "a", "before", "the", "of", "be", "to", "that", "or", "throughout",
        "their", "in",
    )

    /** «Не разобрано» плотной страницы, в порядке текста (LFT-13.1). */
    val denseUnparsed = listOf("sh", "e", "ier", "destibrg")

    fun lists(
        lexical: List<String> = emptyList(),
        proper: List<String> = emptyList(),
        function: List<String> = emptyList(),
        unparsed: List<String> = emptyList(),
    ) = ScanWordListsUi(
        lexical = lexical.map { ScanWordUiItem(it) },
        proper = proper.map { ScanWordUiItem(it) },
        function = function.map { ScanWordUiItem(it) },
        unparsed = unparsed.map { ScanWordUiItem(it) },
    )

    fun textbookLists() = lists(
        lexical = textbookLexical,
        proper = textbookProper,
        function = textbookFunction,
    )

    /** Ответ на токены сообщением хендлера; по умолчанию — абзац учебника. */
    fun found(
        lists: ScanWordListsUi = textbookLists(),
        text: String = TEXTBOOK_TEXT,
        translation: String = TEXTBOOK_TRANSLATION,
        dictionaryId: Long = DICT,
    ) = Msg.LemmasFound(
        dictionaryId = dictionaryId,
        text = text,
        translation = translation,
        lists = lists,
    )

    /** Ответ по лемме сообщением хендлера: слово списка [text] и слово ответа [word]. */
    fun lexemes(
        text: String,
        word: LookupWordUi,
        recommendations: List<ComponentRecommendationUi> = emptyList(),
        existing: List<ExistingLemmaUi> = emptyList(),
    ) = Msg.LexemesFound(
        text = text,
        word = word,
        recommendations = recommendations,
        existing = existing,
    )

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

    /** Второе ядро рядом с «Переводом»: без одного из двух ядер лексема остаётся (AWP-11.9). */
    val definition = component(
        id = DEFINITION,
        systemKey = null,
        name = "Определение",
        template = ComponentTemplate.TEXT,
        isCore = true,
        position = 1,
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

    val potter = FormGroupUi(id = POTTER, name = "Harry Potter 1")
    val finance = FormGroupUi(id = FINANCE, name = "Finance")

    // Леммы словаря

    /**
     * Лемма словаря с текстом [text] и первыми значениями ядра [values]; для
     * пометки «в словаре» важен только текст, для карточки «в словаре» —
     * сохранённые лексемы [lexemes] и день добавления [addedAt].
     */
    fun existing(
        wordId: Long,
        text: String,
        values: List<String> = emptyList(),
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

    /** Сохранённая лексема словаря с одним текстом «Перевода». */
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

    val run1 = existing(RUN_WORD, "run")
    val study1 = existing(STUDY_WORD, "study")

    val runExisting = existing(
        RUN_WORD,
        "run",
        listOf("управлять"),
        groups = listOf("Finance"),
        addedAt = 1_758_000_000_000L,
        lexemes = listOf(saved(1L, "управлять")),
    )
    val bank1 = existing(
        BANK_1,
        "bank",
        listOf("банк", "банкомёт"),
        groups = listOf("Finance"),
        addedAt = 1_760_000_000_000L,
        lexemes = listOf(saved(2L, "банк"), saved(3L, "банкомёт")),
    )
    val bank2 = existing(
        BANK_2,
        "bank",
        listOf("берег"),
        addedAt = 1_756_000_000_000L,
        lexemes = listOf(saved(4L, "берег")),
    )
    val childExisting = existing(
        CHILD_WORD,
        "child",
        listOf("ребёнок"),
        addedAt = 1_759_000_000_000L,
        lexemes = listOf(saved(9L, "ребёнок")),
    )

    fun form(
        components: List<FormComponentUi> = tree,
        options: Map<Long, List<FormOptionUi>> = this.options,
        groups: List<FormGroupUi> = listOf(potter, finance),
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

    /** Форма со «Стилем» (выбор, несколько) после «Перевода». */
    fun formWithStyle(): DictionaryFormUi = form(components = listOf(translation, style))

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

    // Построители ответа по лемме

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
        partOfSpeechKey: String? = null,
        matches: List<ComponentMatchUi> = emptyList(),
        inContext: Boolean = false,
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
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = matches,
        inContext = inContext,
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

    /** Найденное слово без лемм (LFL-100.2). */
    fun foundWithoutLemmas(text: String): LookupWordUi =
        LookupWordUi(text, LookupWordStatus.FOUND, lemmas = emptyList(), remarks = emptyList())

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

    // «corrían», контракт 2.1: лемма «correr», три значения, первое «в тексте»

    val corrianVerb3 = lexeme(
        key = "ruwiktionary/1.verb.3",
        translations = listOf("течь, литься, струиться (о жидкости)"),
        gloss = "to flow",
        partOfSpeechKey = "verb",
        matches = verbMatches(),
        inContext = true,
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

    // «perro», контракт 2.2: существительное с родом, прилагательное, второе существительное

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
        inContext = true,
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

    // «zarpó», контракт 2.4: русского значения нет, английское с пометкой

    val zarpoVerb1 = lexeme(
        key = "enwiktionary/1.verb.1",
        gloss = "to embark; to set sail",
        partOfSpeechKey = "verb",
        matches = listOf(
            byField(TRANSLATION, "gloss"),
            byOption(PART_OF_SPEECH, VERB),
            byField(GLOSS, "gloss"),
        ),
        inContext = true,
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

    /** Рекомендация ответа, контракт 2.6: завести «Род» с опциями. */
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
        inContext = true,
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
    val runMachine = lexeme(
        key = "enwiktionary/1.verb.37",
        translations = listOf("работать", "функционировать", "идти"),
        gloss = "Of a machine, including computer programs, to be operating or working normally.",
        partOfSpeechKey = "verb",
        matches = verbMatches(),
    )

    /** Существительное «run» рядом с глаголом: одна лемма, разная часть речи (AWP-20.3). */
    val runNoun = lexeme(
        key = "ruwiktionary/1.noun.1",
        translations = listOf("бег"),
        partOfSpeechKey = "noun",
        matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, NOUN)),
    )

    /** «ran» — форма «run» с двумя лексемами. */
    val ran = word("ran", lemma("run", listOf(runVerb1, runVerb2), formNote = "simple past"))

    /** «run» само: прямая лемма без пометки формы, как у проверки по тексту (AWP-49). */
    val run = word("run", lemma("run", listOf(runVerb1, runVerb2)))

    /** Три лексемы-кандидата в одной карточке «run» (AWP-15.1, AWP-03.1). */
    val runThree = word("run", lemma("run", listOf(runVerb1, runVerb2, runMachine)))

    /** Глагол и существительное в одной карточке «run» (AWP-20.3). */
    val runVerbAndNoun = word("run", lemma("run", listOf(runVerb1, runNoun)))

    /** «children» → лемма «child» с пометкой формы (AWP-43.7): одно значение словаря, одно новое. */
    val childNoun1 = lexeme(
        key = "ruwiktionary/1.noun.1",
        translations = listOf("ребёнок"),
        partOfSpeechKey = "noun",
        matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, NOUN)),
        inContext = true,
    )
    val childNoun2 = lexeme(
        key = "ruwiktionary/1.noun.2",
        translations = listOf("дитя"),
        partOfSpeechKey = "noun",
        matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, NOUN)),
    )
    val children = word("children", lemma("child", listOf(childNoun1, childNoun2), formNote = "plural"))

    val bankNoun = lexeme(
        key = "ruwiktionary/1.noun.1",
        translations = listOf("банк (финансовое учреждение)"),
        partOfSpeechKey = "noun",
        matches = listOf(byField(TRANSLATION, "translations"), byOption(PART_OF_SPEECH, NOUN)),
        inContext = true,
    )
    val bank = word("bank", lemma("bank", listOf(bankNoun)))

    /** «tiempo», `ruwiktionary/1.noun.3` «погода» с тремя примерами (AWP-15.2, AWP-03.2). */
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
                    inContext = true,
                ),
            ),
        ),
    )

    /** «llegar»: «Стиль» с несколькими значениями «перен.» и «разг.» (AWP-15.7). */
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
                    inContext = true,
                ),
            ),
        ),
    )

    // Адреса

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

    // Карточки для тестов списков

    /** Карточка леммы без значений: по лексеме на флаг «уже в словаре» из [exists]. */
    fun lemmaCard(
        lemma: String,
        vararg exists: Boolean,
    ): LemmaCardUiItem = LemmaCardUiItem(
        lemma = lemma,
        sourceWords = listOf(lemma),
        lexemes = exists.mapIndexed { index, flag ->
            CardLexemeUiItem(
                key = "ruwiktionary/1.verb.${index + 1}",
                source = "ruwiktionary",
                values = emptyList(),
                isEnglishFallback = false,
                exists = flag,
                lacksCore = false,
            )
        },
        existingCount = 0,
        target = CardTargetUi.NewWord,
        ownGroup = CardOwnGroupUi.AsCommon,
    )
}

// Чтение результата в тестах

internal fun List<ScanWordUiItem>.texts(): List<String> = map { it.text }

internal fun List<ScanWordUiItem>.marked(): List<String> =
    filter { it.isInDictionary }.map { it.text }

/** Слово списков по тексту; нет — ошибка теста. */
internal fun ScanCandidatesState.word(text: String): ScanWordUiItem =
    lists.wordOf(text) ?: error("no word $text")

/** Карточка под словом; нет — ошибка теста. */
internal fun ScanCandidatesState.card(text: String): WordCardUi = word(text).card ?: error("no card $text")

internal fun ScanCandidatesState.lemmaCard(text: String): LemmaCardUiItem = card(text).lemma

/** Лексема-кандидат карточки слова по ключу. */
internal fun ScanCandidatesState.lexeme(
    text: String,
    key: String,
): CardLexemeUiItem = lemmaCard(text).lexemes.firstOrNull { it.key == key } ?: error("no lexeme $text / $key")

/** Что видит пользователь у лексемы-кандидата карточки слова. */
internal fun ScanCandidatesState.shown(
    text: String,
    key: String,
): List<String> = lexeme(text, key).shown(requireNotNull(form))

/** Тексты значения-текста компонента у лексемы-кандидата карточки слова. */
internal fun ScanCandidatesState.texts(
    text: String,
    key: String,
    componentId: Long = Fx.TRANSLATION,
): List<String> = lexeme(text, key).texts(componentId)

/** Опции значения-выбора компонента у лексемы-кандидата карточки слова. */
internal fun ScanCandidatesState.chosen(
    text: String,
    key: String,
    componentId: Long,
): List<Long> = lexeme(text, key).chosen(componentId)

/** Значение компонента у лексемы-кандидата активно. */
internal fun ScanCandidatesState.isActive(
    text: String,
    key: String,
    componentId: Long,
): Boolean = lexeme(text, key).values.first { it.componentId == componentId }.isActive

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

/**
 * Что видит пользователь у лексемы: активные значения строками «Имя: значение»
 * в порядке карточки (AWP-29, AWP-44); у «Выбора» без значения — «без значения».
 */
fun CardLexemeUiItem.shown(form: DictionaryFormUi): List<String> = values
    .filter { it.isActive }
    .flatMap { it.shown(form) }

/** Сохранённая лексема словаря строками «Имя: значение» (AWP-46). */
fun SavedLexemeUi.shown(form: DictionaryFormUi): List<String> = values.flatMap { it.shown(form) }

private fun CandidateValueUiItem.shown(form: DictionaryFormUi): List<String> {
    val component = form.componentOf(componentId) ?: error("no component $componentId")
    val name = nameOf(component)
    return when (this) {
        is CandidateValueUiItem.Text -> texts.map { "$name: ${it.text}" }
        is CandidateValueUiItem.Choice -> if (options.isEmpty()) {
            listOf("$name: без значения")
        } else {
            options.map { "$name: ${optionName(form, it)}" }
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
