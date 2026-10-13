package me.apomazkin.wordstab.logic.photo

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.wordstab.deps.PhotoComponent
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoExistingLemma
import me.apomazkin.wordstab.deps.PhotoGroup
import me.apomazkin.wordstab.deps.PhotoLemma
import me.apomazkin.wordstab.deps.PhotoLexeme
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoPartOfSpeechOption
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.deps.PhotoTemplate
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus

/**
 * Фикстуры окна добавления: словарь «English» с «Перевод», «Определение»,
 * «Часть речи», «Пример», «Подсказка»; две группы; леммы «run» и «bank»
 * в словаре; ответ сервера по строкам спеки AWP («run» 7 лексем трёх
 * частей речи, «running», «bank», «fuera»).
 */
internal object Fx {
    const val DICT = 1L

    const val TRANSLATION = 1L
    const val DEFINITION = 2L
    const val PART_OF_SPEECH = 3L
    const val EXAMPLE = 4L
    const val HINT = 5L

    const val VERB = 31L
    const val NOUN = 32L
    const val ADJECTIVE = 33L

    const val POTTER = 7L
    const val FINANCE = 8L

    const val RUN_WORD = 100L
    const val BANK_1 = 101L
    const val BANK_2 = 102L

    val translation = component(TRANSLATION, BuiltInComponent.TRANSLATION, null, PhotoTemplate.TEXT, isCore = true, position = 0)
    val definition = component(DEFINITION, null, "Определение", PhotoTemplate.TEXT, isCore = true, position = 1)
    val partOfSpeech = component(PART_OF_SPEECH, BuiltInComponent.PART_OF_SPEECH, null, PhotoTemplate.CHOICE, isCore = false, position = 2)
    val example = component(EXAMPLE, BuiltInComponent.EXAMPLE, null, PhotoTemplate.CAPTIONED_TEXT, isCore = false, isMultiple = true, position = 3)
    val hint = component(HINT, null, "Подсказка", PhotoTemplate.TEXT, isCore = false, position = 4)

    val options = listOf(
        PhotoPartOfSpeechOption(id = NOUN, key = "noun", label = null, position = 0),
        PhotoPartOfSpeechOption(id = VERB, key = "verb", label = null, position = 1),
        PhotoPartOfSpeechOption(id = ADJECTIVE, key = "adjective", label = null, position = 2),
    )

    val groups = listOf(PhotoGroup(POTTER, "Harry Potter 1"), PhotoGroup(FINANCE, "Finance"))

    val runExisting = existing(RUN_WORD, "run", listOf("управлять"), groups = listOf("Finance"))
    val bank1 = existing(BANK_1, "bank", listOf("банк", "банкомёт"), groups = listOf("Finance"))
    val bank2 = existing(BANK_2, "bank", listOf("берег"))

    fun form(
        components: List<PhotoComponent> = listOf(translation, definition, partOfSpeech, example, hint),
        options: List<PhotoPartOfSpeechOption> = this.options,
        groups: List<PhotoGroup> = this.groups,
        existing: List<PhotoExistingLemma> = listOf(runExisting, bank1, bank2),
        translationLanguage: String = "ru",
    ): PhotoDictionaryForm = PhotoDictionaryForm(
        dictionaryId = DICT,
        translationLanguage = translationLanguage,
        components = components,
        partOfSpeechOptions = options,
        groups = groups,
        existingLemmas = existing,
    )

    /** Умолчания формы: перевод → «Перевод», толкование → нет, примеры → «Пример», часть речи → «Часть речи». */
    val defaultMapping = PhotoMapping(
        translation = PhotoTarget.Component(TRANSLATION),
        gloss = PhotoTarget.None,
        examples = PhotoTarget.Component(EXAMPLE),
        partOfSpeech = PhotoTarget.Component(PART_OF_SPEECH),
    )

    fun component(
        id: Long,
        systemKey: BuiltInComponent?,
        name: String?,
        template: PhotoTemplate,
        isCore: Boolean,
        isMultiple: Boolean = false,
        position: Int,
    ): PhotoComponent = PhotoComponent(
        id = id,
        systemKey = systemKey,
        name = name,
        template = template,
        isCore = isCore,
        isMultiple = isMultiple,
        position = position,
    )

    fun existing(
        wordId: Long,
        text: String,
        values: List<String>,
        groups: List<String> = emptyList(),
    ): PhotoExistingLemma = PhotoExistingLemma(
        wordId = wordId,
        text = text,
        lexemeCount = values.size,
        coreValues = values,
        lexemeTextKeys = values.mapTo(HashSet()) { lexemeTextKey(it) },
        groupNames = groups,
    )

    fun lexeme(
        key: String,
        partOfSpeechKey: String?,
        translations: List<String> = emptyList(),
        gloss: String? = null,
        examples: List<String> = emptyList(),
        caption: String? = null,
        notes: List<String> = emptyList(),
        parent: Boolean = false,
        parentKey: String? = null,
    ): PhotoLexeme = PhotoLexeme(
        key = key,
        partOfSpeechKey = partOfSpeechKey,
        source = if (gloss == null && caption == null) "ruwiktionary" else "enwiktionary",
        translations = translations,
        gloss = gloss,
        caption = caption,
        notes = notes,
        examples = examples,
        parent = parent,
        parentKey = parentKey,
    )

    /** «run»: глагол ru ×3, родитель en, уточнение en с переводами и примером, сущ. ru, прил. en — 7 лексем. */
    val runLexemes = listOf(
        lexeme("ru/v1", "verb", listOf("бежать, бегать")),
        lexeme("ru/v2", "verb", listOf("управлять")),
        lexeme("ru/v3", "verb", listOf("проехать (практически всегда имеется в виду красный сигнал светофора)")),
        lexeme("en/g1", "verb", gloss = "To move swiftly.", parent = true),
        lexeme("en/v1", "verb", listOf("бегать", "побегать", "бежать", "побежать"), gloss = "To move forward quickly upon two feet…", examples = listOf("Run, Sarah, run!", "She ran home."), parentKey = "en/g1"),
        lexeme("ru/n1", "noun", listOf("бег")),
        lexeme("en/a1", "adjective", gloss = "In a liquid state; melted or molten."),
    )

    val runFromRan = PhotoLemma(text = "run", formNote = "simple past", lexemes = runLexemes)
    val runFromRunning = PhotoLemma(text = "run", formNote = "present participle and gerund", lexemes = runLexemes)
    val runningLemma = PhotoLemma(text = "running", formNote = null, lexemes = listOf(lexeme("en/running1", "noun", listOf("бег"), gloss = "The action of the verb to run.")))
    val bankLemma = PhotoLemma(
        text = "bank",
        formNote = null,
        lexemes = listOf(
            lexeme("ru/b1", "noun", listOf("банк (финансовое учреждение)")),
            lexeme("en/b1", "noun", listOf("банк"), gloss = "A branch office of such an institution."),
            lexeme("en/bu1", "noun", listOf("ставка"), caption = "gambling: banker's funds"),
        ),
    )
    val fueraLemmas = listOf(
        PhotoLemma("fuera", null, listOf(lexeme("ru/f1", "adverb", listOf("снаружи")), lexeme("en/f1", "interjection", gloss = "out!, get out!"))),
        PhotoLemma("ser", "first/third-person singular imperfect subjunctive", listOf(lexeme("en/s1", "verb", gloss = "to be"))),
        PhotoLemma("ir", "first/third-person singular imperfect subjunctive", listOf(lexeme("en/i1", "verb", gloss = "to go"))),
    )

    fun word(text: String, vararg lemmas: PhotoLemma): PhotoWord = PhotoWord(text = text, status = PhotoWordStatus.FOUND, lemmas = lemmas.toList())

    val ran = word("ran", runFromRan)
    val running = word("running", runFromRunning, runningLemma)
    val bank = word("bank", bankLemma)
    val fuera = PhotoWord("fuera", PhotoWordStatus.FOUND, fueraLemmas)
    val notFound = PhotoWord("qwrt", PhotoWordStatus.NOT_FOUND, emptyList())
    val errorWord = PhotoWord("Grimmauld", PhotoWordStatus.ERROR, emptyList())
    val emptyLemmas = PhotoWord("void", PhotoWordStatus.FOUND, listOf(PhotoLemma("void", null, emptyList())))
}
