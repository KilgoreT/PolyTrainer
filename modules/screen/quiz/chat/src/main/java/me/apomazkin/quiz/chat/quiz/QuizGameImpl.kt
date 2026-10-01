package me.apomazkin.quiz.chat.quiz

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.PartOfSpeechOption
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.toRef
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.chatCorrectColor
import me.apomazkin.quiz.chat.LogTags
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

/**
 * [random] — источник случайности для выбора ядра вопроса; в тестах
 * подменяется стабом с заданным индексом.
 */
class QuizGameImpl(
        private val quizChatUseCase: QuizChatUseCase,
        private val resourceManager: ResourceManager,
        private val prefsProvider: PrefsProvider,
        private val logger: LexemeLogger,
        private val random: Random,
) : QuizGame {

    /** Dagger не видит Kotlin-дефолтов: прод получает [Random.Default] отсюда. */
    @javax.inject.Inject
    constructor(
            quizChatUseCase: QuizChatUseCase,
            resourceManager: ResourceManager,
            prefsProvider: PrefsProvider,
            logger: LexemeLogger,
    ) : this(
            quizChatUseCase = quizChatUseCase,
            resourceManager = resourceManager,
            prefsProvider = prefsProvider,
            logger = logger,
            random = Random.Default,
    )

        private val maxStepInSession: Int = 10
        private val maxGrade: Int = 3
        private val maxScoreInGrade: Int = 5

    private var currentStep: Step = Step.Pending
    private val userAnswers = mutableMapOf<Step, Answer>()
    private val quizList: MutableList<QuizItem> = mutableListOf()

    private var allStat: AnnotatedString? = null

    override fun getStat(): AnnotatedString? = allStat

    override suspend fun loadData() {
        clearData()
        val quizData = fetchData()
        if (quizData.isEmpty()) {
            logger.w(tag = LogTags.CHAT, message = "loadData: fetchData returned empty list")
        }
        addQuizData(quizData)
    }

    override fun hasNextQuestion(): Boolean {
        return hasNextStep()
    }

    override fun nextQuestion(): QuizQuestion {
        return getQuiz(currentStep).question
    }

    override fun skip() {
        saveUserAnswer(answer = Answer.Skipped(getQuiz(currentStep).answer))
    }

    override fun skipAndGetAnswer(): AnnotatedString {
        saveUserAnswer(answer = Answer.Skipped(getQuiz(currentStep).answer))
        return buildAnnotatedString {
            append(resourceManager.stringByResId(R.string.chat_quiz_msg_system_answer))
            append("\n")
            withStyle(
                    style = LexemeStyle.BodyMBold.copy(
                            color = chatCorrectColor
                    ).toSpanStyle()
            ) {
                append(getQuiz(currentStep).answer)
            }
        }
    }

    override fun makeAssessment(userAttempt: String): AnnotatedString {
        val isCorrect = isAnswerCorrect(answer = userAttempt).also {
            val answer =
                    if (it) Answer.Correct(userAttempt) else Answer.Incorrect(
                            current = userAttempt,
                            correct = getQuiz(currentStep).answer
                    )
            saveUserAnswer(answer = answer)
        }
        return buildAnnotatedString {
            append(getAssessment(isCorrect))
            if (!isCorrect) {
                append("\n")
                append(resourceManager.stringByResId(R.string.chat_quiz_msg_system_answer))
                append("\n")
                withStyle(
                        style = LexemeStyle.BodyMBold.copy(
                                color = chatCorrectColor
                        ).toSpanStyle()
                ) {
                    append(getQuiz(currentStep).answer)
                }
            }
        }
    }

    override fun summaryGeneral(): AnnotatedString =
            getGeneralSummary()

    override fun summaryDetail(): AnnotatedString =
            getDetailSummary()

    override suspend fun saveSession() {
        withContext(Dispatchers.IO) {
            val evaluatedList = getStepList().map { step ->
                val evaluation = evaluateAnswer(step = step)
                getEvaluatedQuizItem(
                        step = step,
                        isCorrect = evaluation
                )
            }
            async {
                quizChatUseCase.updateWriteQuiz(
                        entity = evaluatedList
                )
            }.await()
        }
    }

    private fun evaluateAnswer(step: Step): Boolean {
        val answer = getUserAnswer(step = step)
        return when (answer) {
            is Answer.Correct -> true
            is Answer.Incorrect -> false
            is Answer.Skipped -> false
            null -> false
        }
    }

    private fun getEvaluatedQuizItem(
            step: Step,
            isCorrect: Boolean,
    ): WriteQuizUpsertEntity {
        return getQuiz(step = step).info
                .let { quizInfo ->
                    if (isCorrect) {
                        getCorrectUpdated(quizInfo)
                    } else {
                        getIncorrectUpdated(quizInfo)
                    }
                }
    }

    private fun getCorrectUpdated(
            quizInfo: QuizItem.QuizInfo,
    ): WriteQuizUpsertEntity {
        return quizInfo.correct(
                maxGrade = maxGrade,
                maxScoreInGrade = maxScoreInGrade
        )
    }

    private fun getIncorrectUpdated(
            quizInfo: QuizItem.QuizInfo,
    ): WriteQuizUpsertEntity {
        return quizInfo.incorrect(
                maxGrade = maxGrade,
                maxScoreInGrade = maxScoreInGrade
        )
    }

    private suspend fun fetchData(): List<QuizItem> {
        val dictionaryId = quizChatUseCase.getCurrentDictionaryId()
        if (dictionaryId == null) {
            // IS476: словарь отсутствует — возвращаем пустой список квизов.
            logger.w(tag = LogTags.CHAT, message = "fetchData: no current dictionary (null id)")
            return emptyList()
        }
        // Чем спрашивать: кандидаты словаря ∩ выбор в меню, пусто → все
        // кандидаты (одно правило на меню и порцию — resolveQuizCores).
        val candidates = quizChatUseCase.getQuizCoreTypes(dictionaryId)
        if (candidates.isEmpty()) {
            logger.w(tag = LogTags.CHAT, message = "fetchData: no quiz cores for dictionary $dictionaryId")
            return emptyList()
        }
        val selected = quizChatUseCase.getQuizPickerSelection(dictionaryId)
        val cores = resolveQuizCores(candidates, selected)
        val posOptions = quizChatUseCase.getPartOfSpeechOptions(dictionaryId).associateBy { it.id }
        val isDebugOn = prefsProvider.getBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN) ?: false
        return quizChatUseCase.getRandomWriteQuizList(
                dictionaryId = dictionaryId,
                limit = maxStepInSession,
                maxGrade = maxGrade,
                coreTypeIds = cores.map { core -> core.id.id },
        ).also {
            val stat = buildAnnotatedString {
                withStyle(
                        style = LexemeStyle.BodySBold.copy(
                                color = Color.Gray
                        ).toSpanStyle()
                ) {
                    append("### Quiz count by grade")
                    (0..maxGrade)
                            .forEach { grade ->
                                append("\n")
                                append("Grade: $grade | Count: ${
                                    it.count { quiz ->
                                        quiz.grade == grade
                                    }
                                }")
                            }
                    append("\n")
                    append("### Total: ${it.size}")
                    append("\n")
                    append("#######################")
                }
            }
            allStat = if (isDebugOn) stat else null
        }.mapNotNull {
            it.toQuizItem(
                    coreRefs = cores.map { core -> core.toRef() },
                    posOptions = posOptions,
                    random = random,
                    resourceManager = resourceManager,
                    isDebugOn = isDebugOn,
            )
        }
    }

    private fun isAnswerCorrect(answer: String): Boolean {
        return getQuiz(currentStep).answer == answer
    }

    private fun getAssessment(isCorrect: Boolean) =
            if (isCorrect) getSuccessMessage() else getFailureMessage()

    private fun saveUserAnswer(answer: Answer) {
        saveUserAnswer(getCurrentStep(), answer)
    }

    private fun getGeneralSummary() = buildAnnotatedString {
        withStyle(style = ParagraphStyle(lineHeight = 24.sp)) {
            append(resourceManager.stringByResId(R.string.chat_quiz_msg_system_session_end))
            append("\n")
            append(totalSummaryMessage(count = getTotalQuizCount()))
            append("\n")
            append(correctSummaryMessage(count = getCorrectAnswers()))
            append("\n")
            append(skippedSummaryMessage(count = getSkippedAnswers()))
            append("\n")
            append(incorrectSummaryMessage(count = getIncorrectAnswers()))
        }
    }

    private fun getDetailSummary() = buildAnnotatedString {
        withStyle(style = ParagraphStyle(lineHeight = 24.sp)) {
            append(summaryDetailMessage())
            userAnswers
                    .forEach { entry ->
                        append("\n")
                        val icon = if (entry.value is Answer.Correct) "✅" else "❌"
                        append(
                                "$icon ${getQuiz(entry.key).question.value} - ${
                                    getUserAnswer(entry.key)?.toSummaryString()
                                }"
                        )
                    }
        }
    }

    private fun getTotalQuizCount() =
            getTotalQuizCountInSession()

    private fun getCorrectAnswers() =
            getCorrectAnswersCount()

    private fun getIncorrectAnswers() =
            getIncorrectAnswersCount()

    private fun getSkippedAnswers() =
            getSkippedAnswersCount()

    private fun clearData() {
        clearQuizData()
        clearUserAnswers()
    }

    /**
     * ########################################
     * # Quiz data section
     * ########################################
     */
    private fun addQuizData(quizData: List<QuizItem>) {
        quizList.addAll(quizData)
        logger.d(tag = LogTags.CHAT, message = "addQuizData: size = ${quizList.size}")
    }

    private fun getQuiz(step: Step): QuizItem {
        return when (step) {
            is Step.Pending -> throw QuizNotLoadedException()
            is Step.Started -> quizList.getOrElse(step.value) {
                throw IndexOutOfBoundsException(
                    "Quiz index ${step.value} out of bounds, quizList.size=${quizList.size}"
                )
            }
        }
    }

    private fun getTotalQuizCountInSession(): Int = quizList.size

    private fun clearQuizData() {
        quizList.clear()
    }

    /**
     * ########################################
     * # User answers section
     * ########################################
     */
    private fun saveUserAnswer(step: Step, answer: Answer) {
        userAnswers[step] = answer
    }

    private fun getUserAnswer(step: Step): Answer? {
        return userAnswers[step]
    }

    private fun getStepList(): List<Step> {
        return userAnswers
                .keys
                .toList()
    }

    private fun getCorrectAnswersCount(): Int = userAnswers
            .count { it.value is Answer.Correct }

    private fun getIncorrectAnswersCount(): Int = userAnswers
            .count { it.value is Answer.Incorrect }

    private fun getSkippedAnswersCount(): Int = userAnswers
            .count { it.value is Answer.Skipped }

    private fun clearUserAnswers() {
        userAnswers.clear()
    }

    /**
     * ########################################
     * # Quiz step section
     * ########################################
     */

    private fun getCurrentStep(): Step {
        return currentStep
    }

    private fun hasNextStep(): Boolean {
        nextStep()
        return currentStep is Step.Started
    }

    private fun nextStep() {
        currentStep = when (val step = currentStep) {
            is Step.Pending -> if (quizList.isNotEmpty()) Step.Started(0) else Step.Pending
            is Step.Started -> if (step.value < maxStep() - 1) Step.Started(step.value + 1) else Step.Pending
        }
    }

    private fun maxStep() = quizList.size

    /**
     * ########################################
     * # String Provider Section
     * ########################################
     */
    private fun getSuccessMessage(): String {
        return resourceManager.stringByArrayId(R.array.chat_quiz_system_correct)
    }

    private fun getFailureMessage(): String {
        return resourceManager.stringByArrayId(R.array.chat_quiz_system_incorrect)
    }

    private fun totalSummaryMessage(count: Int): String =
            resourceManager.stringByResId(
                    R.string.chat_quiz_summary_total,
                    count.toString()
            )

    private fun correctSummaryMessage(count: Int): String =
            resourceManager.stringByResId(
                    R.string.chat_quiz_summary_correct,
                    count.toString()
            )

    private fun skippedSummaryMessage(count: Int): String =
            resourceManager.stringByResId(
                    R.string.chat_quiz_summary_skipped,
                    count.toString()
            )

    private fun incorrectSummaryMessage(count: Int): String =
            resourceManager.stringByResId(
                    R.string.chat_quiz_summary_incorrect,
                    count.toString()
            )

    private fun summaryDetailMessage(): String =
            resourceManager.stringByResId(R.string.chat_quiz_summary_detail_title)


    sealed interface Step {
        data object Pending : Step
        data class Started(val value: Int) : Step
    }
}

/**
 * [answer] — слово; [question] — чем и о чём спрашиваем; [info] — запись
 * квиза для апсерта после раунда (презентации там нет).
 */
data class QuizItem(
        val answer: String,
        val question: QuizQuestion,
        val info: QuizInfo,
) {
    data class QuizInfo(
            val id: Long,
            val dictionaryId: Long,
            val lexemeId: Long,
            val grade: Int,
            val score: Int,
            val errorCount: Int,
            val addDate: Date,
            val lastSelectDate: Date? = null,
    )
}

/**
 * Собирает вопрос по лексеме.
 *
 * Ядро показа — случайное (через [random]) из [coreRefs], у которых у
 * лексемы есть текстовое значение; порция уже отфильтрована в SQL, здесь
 * страховка: ни одного — null, слово пропускается. Имя ядра — заголовок
 * вопроса; метка части речи — по значению встроенного CHOICE через
 * [posOptions] (id опции → опция): нет значения или опции — без метки.
 */
fun WriteQuiz.toQuizItem(
        coreRefs: List<ComponentTypeRef>,
        posOptions: Map<Long, ComponentOption>,
        random: Random,
        resourceManager: ResourceManager,
        isDebugOn: Boolean,
): QuizItem? {
    val present: List<Pair<ComponentTypeRef, ComponentValue>> = coreRefs.mapNotNull { ref ->
        lexeme.components
            .firstOrNull { it.matchesRef(ref) && it.data is TextValues }
            ?.let { ref to it }
    }
    if (present.isEmpty()) return null
    val (ref, source) = present.random(random)
    val text = (source.data as TextValues).value.value

    val badge = lexeme.components
        .firstOrNull { it.type.systemKey == BuiltInComponent.PART_OF_SPEECH }
        ?.let { (it.data as? ChoiceValues)?.optionId }
        ?.let { optionId -> posOptions[optionId] }
        ?.let { option -> resourceManager.partOfSpeechBadge(option) }

    val debugHeader = if (isDebugOn) {
        val last = if (lastCorrectAnswerDate != null) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            formatter.format(lastCorrectAnswerDate)
        } else {
            "none"
        }
        buildAnnotatedString {
            withStyle(
                style = LexemeStyle.BodySBold.copy(
                    color = Color.Gray
                ).toSpanStyle()
            ) {
                append("### grade: $grade | score: $score | errorCount: $errorCount")
                append("\n")
                append("### last: $last")
                append("\n")
                if (ref is ComponentTypeRef.UserDefined) {
                    append("### type: $type")
                    append("\n")
                }
                append("#############################")
            }
        }
    } else {
        null
    }

    return QuizItem(
        answer = word.value,
        question = QuizQuestion(
            header = resourceManager.coreTitle(ref),
            badge = badge,
            value = text,
            debugHeader = debugHeader,
        ),
        info = QuizItem.QuizInfo(
            id = id,
            dictionaryId = dictionaryId,
            lexemeId = lexeme.lexemeId.id,
            grade = grade,
            score = score,
            errorCount = errorCount,
            addDate = addDate,
            lastSelectDate = lastCorrectAnswerDate,
        )
    )
}

private fun ComponentValue.matchesRef(ref: ComponentTypeRef): Boolean = when (ref) {
    is ComponentTypeRef.BuiltIn -> type.systemKey == ref.key
    is ComponentTypeRef.UserDefined -> type.systemKey == null && type.name == ref.name
}

/**
 * Имя ядра для заголовка вопроса: встроенное — из того же ресурса, что
 * подпись галки в меню (имена обязаны совпадать); пользовательское — как есть.
 */
private fun ResourceManager.coreTitle(ref: ComponentTypeRef): String = when (ref) {
    is ComponentTypeRef.UserDefined -> ref.name
    is ComponentTypeRef.BuiltIn -> when (ref.key) {
        BuiltInComponent.TRANSLATION -> stringByResId(R.string.chat_menu_item_component_translation)
        BuiltInComponent.PART_OF_SPEECH -> stringByResId(
            me.apomazkin.core_resources.R.string.builtin_component_part_of_speech,
        )
        BuiltInComponent.EXAMPLE -> stringByResId(
            me.apomazkin.core_resources.R.string.builtin_component_example,
        )
    }
}

/**
 * Текст метки части речи: встроенная опция — сокращение из ресурсов,
 * пользовательская — её подпись; ни того ни другого — метки нет.
 */
private fun ResourceManager.partOfSpeechBadge(option: ComponentOption): String? {
    val builtIn = option.systemKey?.let { key ->
        PartOfSpeechOption.entries.firstOrNull { it.key == key }
    }
    if (builtIn == null) return option.label
    val resId = when (builtIn) {
        PartOfSpeechOption.NOUN -> R.string.chat_quiz_pos_noun
        PartOfSpeechOption.VERB -> R.string.chat_quiz_pos_verb
        PartOfSpeechOption.ADJECTIVE -> R.string.chat_quiz_pos_adjective
        PartOfSpeechOption.ADVERB -> R.string.chat_quiz_pos_adverb
        PartOfSpeechOption.PREPOSITION -> R.string.chat_quiz_pos_preposition
        PartOfSpeechOption.PHRASE -> R.string.chat_quiz_pos_phrase
    }
    return stringByResId(resId)
}


fun QuizItem.QuizInfo.correct(
        maxGrade: Int,
        maxScoreInGrade: Int,
): WriteQuizUpsertEntity {

    val errorCount = max(0, this.errorCount - 1)

    val (scoreNew, gradeNew) = when {

        score == maxScoreInGrade && grade == maxGrade -> {
            0 to grade + 1
        }

        score == maxScoreInGrade -> {
            0 to grade + 1
        }

        else -> {
            score + 1 to grade
        }
    }
    return WriteQuizUpsertEntity(
            id = id,
            dictionaryId = dictionaryId,
            lexemeId = lexemeId,
            grade = gradeNew,
            score = scoreNew,
            errorCount = errorCount,
            addDate = addDate,
            lastCorrectAnswerDate = Date(System.currentTimeMillis()),
    )
}

fun QuizItem.QuizInfo.incorrect(
        maxGrade: Int,
        maxScoreInGrade: Int,
): WriteQuizUpsertEntity {
    val (scoreNew, gradeNew) = when {

        score == 0 && grade == maxGrade + 1 -> {
            score to grade
        }

        score == 0 && grade == 0 -> {
            0 to 0
        }

        score == 0 -> {
            maxScoreInGrade to grade - 1
        }

        else -> {
            score - 1 to grade
        }
    }
    return WriteQuizUpsertEntity(
            id = id,
            dictionaryId = dictionaryId,
            lexemeId = lexemeId,
            grade = gradeNew,
            score = scoreNew,
            errorCount = errorCount + 1,
            addDate = addDate,
            lastCorrectAnswerDate = lastSelectDate,
    )
}

sealed interface Answer {

    fun toSummaryString(): String {
        return when (this) {
            is Correct -> correct
            is Incorrect -> "$correct (ваш ответ: $current)"
            is Skipped -> "$correct (пропущено)"
        }
    }

    data class Correct(val correct: String) : Answer
    data class Incorrect(
            val current: String,
            val correct: String,
    ) : Answer

    data class Skipped(val correct: String) : Answer
}

class QuizNotLoadedException : IllegalStateException("Quiz is not loaded")