package me.apomazkin.wordcard.mate

import me.apomazkin.candidates.PhotoComponent
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoExistingLemma
import me.apomazkin.candidates.PhotoLemma
import me.apomazkin.candidates.PhotoLexeme
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.PhotoPartOfSpeechOption
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoTemplate
import me.apomazkin.candidates.PhotoWord
import me.apomazkin.candidates.PhotoWordStatus
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.lexemeTextKey

/**
 * Фикстуры диалога «значения из словаря» (WCL): слово «run» (id 7) словаря 3
 * с одной лексемой «управлять»; ответы прода по форме — «run» → «run» и
 * «rin» (past participle), «running» → «run» (форма) и «running», «ran» →
 * «run», «rin», «ran».
 */
internal object CFx {
    const val DICT = 3L
    const val WORD = 7L
    const val TRANSLATION = 17L
    const val PART_OF_SPEECH = 18L
    const val EXAMPLE = 19L
    const val DEFINITION = 20L
    const val NOUN = 32L
    const val VERB = 31L

    val form = PhotoDictionaryForm(
        dictionaryId = DICT,
        translationLanguage = "ru",
        components = listOf(
            PhotoComponent(id = TRANSLATION, systemKey = BuiltInComponent.TRANSLATION, name = null, template = PhotoTemplate.TEXT, isCore = true, isMultiple = false, position = 0),
            PhotoComponent(id = PART_OF_SPEECH, systemKey = BuiltInComponent.PART_OF_SPEECH, name = null, template = PhotoTemplate.CHOICE, isCore = false, isMultiple = false, position = 1),
            PhotoComponent(id = EXAMPLE, systemKey = BuiltInComponent.EXAMPLE, name = null, template = PhotoTemplate.CAPTIONED_TEXT, isCore = false, isMultiple = true, position = 2),
            PhotoComponent(id = DEFINITION, systemKey = null, name = "Definition", template = PhotoTemplate.TEXT, isCore = true, isMultiple = false, position = 3),
        ),
        partOfSpeechOptions = listOf(
            PhotoPartOfSpeechOption(id = NOUN, key = "noun", label = null, position = 0),
            PhotoPartOfSpeechOption(id = VERB, key = "verb", label = null, position = 1),
        ),
        groups = emptyList(),
        existingLemmas = listOf(
            PhotoExistingLemma(
                wordId = WORD,
                text = "run",
                lexemeCount = 1,
                coreValues = listOf("управлять"),
                lexemeTextKeys = setOf(lexemeTextKey("управлять")),
                groupNames = emptyList(),
            ),
        ),
    )

    /** Форма без лексем у слова — пометок «есть» нет (WCL-06.3). */
    val emptyWordForm = form.copy(existingLemmas = listOf(form.existingLemmas.first().copy(lexemeCount = 0, coreValues = emptyList(), lexemeTextKeys = emptySet())))

    /** Слово словаря «run» (id 8) с «бежать, бегать» и «управлять» — получатель переноса для слова «running» (WCL-15). */
    val runWordLemma = PhotoExistingLemma(
        wordId = 8L,
        text = "run",
        lexemeCount = 2,
        coreValues = listOf("бежать, бегать", "управлять"),
        lexemeTextKeys = setOf(lexemeTextKey("бежать, бегать"), lexemeTextKey("управлять")),
        groupNames = emptyList(),
    )

    /** Второе слово «run» (id 9) без лексем — одноимённые получатели различаются значениями (WCL-15.1). */
    val runWordTwin = PhotoExistingLemma(wordId = 9L, text = "run", lexemeCount = 0, coreValues = emptyList(), lexemeTextKeys = emptySet(), groupNames = emptyList())

    /** Форма слова «running» (id 7, «управлять») без одноимённых слов у лемм-форм. */
    val runningForm = form.copy(existingLemmas = listOf(form.existingLemmas.first().copy(text = "running")))

    /** Форма слова «running» (id 7, «управлять») и слова словаря «run» (id 8). */
    val formWithRunWord = runningForm.copy(existingLemmas = runningForm.existingLemmas + runWordLemma)

    /** Та же форма с двумя словами «run» (id 8 и 9). */
    val formWithTwoRunWords = formWithRunWord.copy(existingLemmas = formWithRunWord.existingLemmas + runWordTwin)

    val remembered = PhotoMapping(
        translation = PhotoTarget.Component(DEFINITION),
        gloss = PhotoTarget.None,
        examples = PhotoTarget.None,
        partOfSpeech = PhotoTarget.Component(PART_OF_SPEECH),
    )

    fun lexeme(
        key: String,
        partOfSpeech: String? = "verb",
        translations: List<String> = emptyList(),
        gloss: String? = null,
        examples: List<String> = emptyList(),
        parent: Boolean = false,
        parentKey: String? = null,
    ): PhotoLexeme = PhotoLexeme(
        key = key,
        partOfSpeechKey = partOfSpeech,
        source = if (key.startsWith("ru")) "ruwiktionary" else "enwiktionary",
        translations = translations,
        gloss = gloss,
        caption = null,
        notes = emptyList(),
        examples = examples,
        parent = parent,
        parentKey = parentKey,
    )

    /** Шесть лексем «run»: пять видимых и «ещё 1». */
    val runLexemes = listOf(
        lexeme("ru/v1", translations = listOf("бежать, бегать")),
        lexeme("ru/v2", translations = listOf("управлять"), examples = listOf("to run a business")),
        lexeme("ru/v3", translations = listOf("проехать")),
        lexeme("en/g1", gloss = "To move swiftly.", parent = true),
        lexeme("en/v1", translations = listOf("бегать, побегать"), gloss = "To move forward quickly.", examples = listOf("Run!", "Run, Sarah!"), parentKey = "en/g1"),
        lexeme("ru/n1", partOfSpeech = "noun", translations = listOf("бег")),
    )

    val rinLemma = PhotoLemma("rin", "past participle", listOf(lexeme("en/rin1", gloss = "To run.")))

    val runWord = PhotoWord("run", PhotoWordStatus.FOUND, listOf(PhotoLemma("run", null, runLexemes), rinLemma))

    val runningWord = PhotoWord(
        "running",
        PhotoWordStatus.FOUND,
        listOf(
            PhotoLemma("run", "present participle and gerund", runLexemes.take(3)),
            PhotoLemma("running", null, listOf(lexeme("ru/a1", partOfSpeech = "adjective", translations = listOf("бегущий")))),
        ),
    )

    val ranWord = PhotoWord(
        "ran",
        PhotoWordStatus.FOUND,
        listOf(
            PhotoLemma("run", "simple past", runLexemes.take(2)),
            rinLemma,
            PhotoLemma("ran", null, listOf(lexeme("en/ran1", partOfSpeech = "noun", gloss = "Yarns coiled on a spun-yarn winch."))),
        ),
    )

    val formsOnlyWord = PhotoWord("ran", PhotoWordStatus.FOUND, listOf(PhotoLemma("run", "simple past", runLexemes.take(2)), rinLemma))

    val notFoundWord = PhotoWord("qwrtzk", PhotoWordStatus.NOT_FOUND, emptyList())

    val emptyLemmasWord = PhotoWord("bank", PhotoWordStatus.FOUND, listOf(PhotoLemma("bank", null, emptyList())))

    /** Лемма с [count] лексемами одной части речи — пятёрка и «ещё N». */
    fun bigWord(count: Int): PhotoWord = PhotoWord(
        "give up",
        PhotoWordStatus.FOUND,
        listOf(PhotoLemma("give up", null, (1..count).map { lexeme("en/$it", partOfSpeech = "phrasal_verb", translations = listOf("сдаться $it")) })),
    )
}
