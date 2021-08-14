package me.apomazkin.feature_training_write_impl.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.core_interactor.entity.WriteQuizStep
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// TODO: 28.02.2021 QUIZ
class TrainingWriteViewModel @Inject constructor(
    private val coreInteractorApi: CoreInteractorApi,
    private val navigation: FeatureTrainingWriteNavigator,
//    private val loadStateDelegate: LoadState
) : ViewModel()/*, LoadState by loadStateDelegate*/ {


    //    val currentQuizNumber = MutableLiveData<Int>()
    private var currentQuiz = 0
    private var data: List<WriteQuizStep> = emptyList()
    val currentQuizInt = MutableLiveData<Int>()
    val currentQuizTitle = MutableLiveData<String>()
    val currentQuizDate = MutableLiveData<String>()
    val currentQuizValue = MutableLiveData<String>()
    val currentQuizAnswer = MutableLiveData<String>(null)
    val quizAttemptValue = MutableLiveData<String>()
    val quizAttempt = MutableLiveData(false)
    val btnCheckVisibility = MutableLiveData(false)
    val btnNextVisibility = MutableLiveData(false)
    val btnReloadVisibility = MutableLiveData(false)

    val errorMessage = MutableLiveData<String>("")


    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    fun loadData() {
        currentQuizTitle.postValue("")
        coreInteractorApi
            .writeQuizScenario()
            .getWriteQuizStepList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                data = list
                setupQuiz()
            }, { ttt ->
                errorMessage.postValue(ttt.message ?: "")
                Log.d("###", ">>>> definition error: ${ttt.message}")
                btnReloadVisibility.postValue(true)
            })
    }

    private fun setupQuiz() {
        currentQuiz = 0
        currentQuizInt.postValue(currentQuiz)
        currentQuizTitle.postValue("${currentQuiz + 1} quiz: grade - ${data[currentQuiz].grade} score - ${data[currentQuiz].score}")
        val date = data[currentQuiz].lastSelectDate?.let { date ->
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } ?: "unknown"
        currentQuizDate.postValue(date)
        currentQuizValue.postValue(data[currentQuiz].definition)
        quizAttempt.postValue(true)
        setupButtons(check = true)
    }

    private fun setupButtons(
        check: Boolean = false,
        next: Boolean = false,
        reload: Boolean = false
    ) {
        btnCheckVisibility.postValue(check)
        btnNextVisibility.postValue(next)
        btnReloadVisibility.postValue(reload)
    }

    fun onPressCheck() {
        val answer = data[currentQuiz]
        val lastSelectDate = Date(System.currentTimeMillis())
        if (quizAttemptValue.value == data[currentQuiz].answer) {
            currentQuizTitle.postValue("Right!")
            val copy = if (answer.score < 5) {
                answer.copy(
                    score = answer.score + 1,
                    lastSelectDate = lastSelectDate,
                )
            } else {
                answer.copy(
                    score = 0,
                    grade = if (answer.grade < 2) answer.grade + 1 else answer.grade,
                    lastSelectDate = lastSelectDate,
                )
            }
            coreInteractorApi
                .writeQuizScenario()
                .updateWriteQuizStep(copy)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        } else {
            currentQuizTitle.postValue("Wrong!")

            val copy = when {
                answer.score > 0 -> {
                    answer.copy(
                        score = answer.score - 1,
                    )
                }
                else -> answer.copy()
            }
            coreInteractorApi
                .writeQuizScenario()
                .updateWriteQuizStep(copy)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        }
        currentQuizAnswer.postValue(data[currentQuiz].answer)
        quizAttemptValue.postValue("")
        quizAttempt.postValue(false)
        setupButtons(next = true)
    }

    fun onPressNext() {
        currentQuizAnswer.postValue(null)
        if (currentQuiz < 9) {
            currentQuiz++
            currentQuizInt.postValue(currentQuiz)
            currentQuizTitle.postValue("${currentQuiz + 1} quiz: grade - ${data[currentQuiz].grade} score - ${data[currentQuiz].score}")
            val date = data[currentQuiz].lastSelectDate?.let { date ->
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
            } ?: "unknown"
            currentQuizDate.postValue(date)
            currentQuizValue.postValue(data[currentQuiz].definition)
            quizAttempt.postValue(true)
            setupButtons(check = true)
        } else {
            currentQuizTitle.postValue("Summary")
            currentQuizValue.postValue(null)
            quizAttempt.postValue(false)
            setupButtons(reload = true)
        }

    }

    fun onPressReload() {
        loadData()
    }

}

sealed class QuizState

// TODO: 25.07.2021 нужен ли этот стейт?
object StartQuizState : QuizState() {
    fun onStart(): LoadingQuizList = LoadingQuizList
}

object LoadingQuizList : QuizState() {
    fun onErrorLoading(cause: String): ErrorQuizState = ErrorQuizState(cause)
    fun onSuccessLoading(list: List<WriteQuizStep>): ResultQuizState = ResultQuizState(list)
}

class ErrorQuizState(
    val cause: String
) : QuizState() {
    fun onReload(): LoadingQuizList = LoadingQuizList
    fun onFinish(): FinishQuizState = FinishQuizState
}

class ResultQuizState(
    val list: List<WriteQuizStep>
) : QuizState() {
    fun onError(cause: String): ErrorQuizState = ErrorQuizState(cause)
    fun onFinish(): FinishQuizState = FinishQuizState

    sealed class QuizProgress {

        // TODO: 15.08.2021 replace this chain outside. And add increment of list
        class StartQuiz(
            private val list: List<WriteQuizStep>
        ) : QuizProgress() {
            fun onStart(): StepQuiz = StepQuiz(list.first())
        }

        class StepQuiz(
            val quiz: WriteQuizStep
        ) : QuizProgress()

    }

}

object FinishQuizState : QuizState() {
    fun onClose() {}
    fun onRestart(): LoadingQuizList = LoadingQuizList
}


sealed class QuizStateOld(
    val answer: String? = null,
    val quizTitle: String? = null,
    val quizValue: String? = null,
    val btnCheckVisibility: Int = View.GONE,
    val btnNextVisibility: Int = View.GONE,
    val btnReloadVisibility: Int = View.GONE,
)

class CheckStateOld(
    quizTitle: String?,
    quizValue: String?,
) : QuizStateOld(
    quizTitle = quizTitle,
    quizValue = quizValue,
    btnCheckVisibility = View.VISIBLE,
)

class AnswerStateOld(
    answer: String,
    quizTitle: String,
    quizValue: String,
) : QuizStateOld(
    answer = answer,
    quizTitle = quizTitle,
    quizValue = quizValue,
    btnNextVisibility = View.VISIBLE,
)

class ReloadStateOld(
    quizTitle: String,
) : QuizStateOld(
    quizTitle = quizTitle,
    btnReloadVisibility = View.VISIBLE,
)