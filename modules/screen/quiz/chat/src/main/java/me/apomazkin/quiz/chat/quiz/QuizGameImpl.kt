package me.apomazkin.quiz.chat.quiz

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.chatCorrectColor
import me.apomazkin.ui.resource.ResourceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class QuizGameImpl(
        private val quizChatUseCase: QuizChatUseCase,
        private val resourceManager: ResourceManager,
        private val prefsProvider: PrefsProvider,
        private val maxStepInSession: Int = 10,
        private val maxGrade: Int = 3,
        private val maxScoreInGrade: Int = 5,
) : QuizGame {

    private var currentStep: Step = Step.Pending
    private val userAnswers = mutableMapOf<Step, Answer>()
    private val quizList: MutableList<QuizItem> = mutableListOf()

    private var allStat: AnnotatedString? = null

    override fun getStat(): AnnotatedString? = allStat

    override suspend fun loadData() {
        clearData()
        val quizData = fetchData()
        addQuizData(quizData)
    }

    override fun hasNextQuestion(): Boolean {
        return hasNextStep()
    }

    override fun nextQuestion(): AnnotatedString {
        return getNextQuestion()
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
                maxScoreInGrade = maxScoreInGrade
        )
    }

    private suspend fun fetchData(): List<QuizItem> {
        val langId = quizChatUseCase.getCurrentLangId()
        return quizChatUseCase.getRandomWriteQuizList(
                langId = langId,
                limit = maxStepInSession,
                maxGrade = maxGrade
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
            allStat = if (prefsProvider.getBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN) == true) stat
            else null
        }.map {
            it.toQuizItem(
                    resourceManager = resourceManager,
                    isDebugOn = prefsProvider.getBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN) ?: false,
            )
        }
    }

    private fun getNextQuestion(): AnnotatedString {
        return getQuiz(currentStep).fullQuestion
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
                                "$icon ${getQuiz(entry.key).question} - ${
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
        Log.d("###", "<QuizGameImpl.kt>::addQuizData => quiz size = ${quizList.size}")
    }

    private fun getQuiz(step: Step): QuizItem {
        return when (step) {
            is Step.Pending -> throw QuizNotLoadedException()
            is Step.Started -> quizList[step.value]
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
            is Step.Pending -> Step.Started(0)
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

data class QuizItem(
        val answer: String,
        val fullQuestion: AnnotatedString,
        val question: AnnotatedString,
        val info: QuizInfo,
) {
    data class QuizInfo(
            val id: Long,
            val langId: Long,
            val lexemeId: Long,
            val grade: Int,
            val score: Int,
            val errorCount: Int,
            val addDate: Date,
            val lastSelectDate: Date? = null,
    )
}

fun WriteQuiz.toQuizItem(
        resourceManager: ResourceManager,
        isDebugOn: Boolean,
): QuizItem {
    val last = if (lastSelectDate != null) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        formatter.format(lastSelectDate)
    } else {
        "none"
    }
    return QuizItem(
            answer = word.value,
            fullQuestion = when {
                lexeme.translation != null -> buildAnnotatedString {
                    if (isDebugOn) {
                        withStyle(
                                style = LexemeStyle.BodySBold.copy(
                                        color = Color.Gray
                                ).toSpanStyle()
                        ) {
                            append("### grade: $grade | score: $score | errorCount: $errorCount")
                            append("\n")
                            append("### last: $last")
                            append("\n")
                            append("#############################")
                            append("\n")
                        }
                    }
                    append(
                            resourceManager.stringByResId(R.string.chat_quiz_ask_translation_header),
                    )
                    append("\n")
                    withStyle(style = LexemeStyle.BodyMBold.toSpanStyle()) {
                        append(
                                lexeme.translation.value
                        )
                    }
                }

                lexeme.definition != null -> buildAnnotatedString {
                    if (isDebugOn) {
                        withStyle(
                                style = LexemeStyle.BodySBold
                                        .copy(
                                                color = Color.Gray
                                        ).toSpanStyle()
                        ) {
                            append("### grade: $grade | score: $score | errorCount: $errorCount")
                            append("\n")
                            append("### last: $last")
                            append("\n")
                            append("### type: $type")
                            append("\n")
                            append("#############################")
                            append("\n")
                        }
                    }
                    append(
                            resourceManager.stringByResId(R.string.chat_quiz_ask_definition_header),
                    )
                    append("\n")
                    withStyle(style = LexemeStyle.BodyMBold.toSpanStyle()) {
                        append(
                                lexeme.definition.value
                        )
                    }
                }

                else -> throw IllegalArgumentException("No translation or definition")
            },
            question = buildAnnotatedString {
                if (lexeme.translation != null)
                    append(lexeme.translation.value)
                else if (lexeme.definition != null) {
                    append(lexeme.definition.value)
                } else {
                    append("No translation or definition")
                }
            },
            info = QuizItem.QuizInfo(
                    id = id,
                    langId = langId,
                    lexemeId = lexeme.id,
                    grade = grade,
                    score = score,
                    errorCount = errorCount,
                    addDate = addDate,
                    lastSelectDate = lastSelectDate,
            )
    )
}


fun QuizItem.QuizInfo.correct(
        maxGrade: Int,
        maxScoreInGrade: Int,
): WriteQuizUpsertEntity {

    var errorCount = this.errorCount

    val (scoreNew, gradeNew) = when {

        score == maxScoreInGrade && grade == maxGrade -> {
            errorCount = max(0, this.errorCount - 1)
            score to grade
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
            langId = langId,
            lexemeId = lexemeId,
            grade = gradeNew,
            score = scoreNew,
            errorCount = errorCount,
            addDate = addDate,
            lastSelectDate = Date(System.currentTimeMillis()),
    )
}

fun QuizItem.QuizInfo.incorrect(
        maxScoreInGrade: Int,
): WriteQuizUpsertEntity {
    val (scoreNew, gradeNew) = when {
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
            langId = langId,
            lexemeId = lexemeId,
            grade = gradeNew,
            score = scoreNew,
            errorCount = errorCount + 1,
            addDate = addDate,
            lastSelectDate = Date(System.currentTimeMillis()),
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