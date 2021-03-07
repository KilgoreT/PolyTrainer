package me.apomazkin.feature_training_write_impl.ui

import android.annotation.SuppressLint
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Quiz
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import javax.inject.Inject

// TODO: 28.02.2021 QUIZ
class TrainingWriteViewModel @Inject constructor(
    private val dbApi: CoreDbApi,
    private val navigation: FeatureTrainingWriteNavigator,
//    private val loadStateDelegate: LoadState
) : ViewModel()/*, LoadState by loadStateDelegate*/ {


    //    val currentQuizNumber = MutableLiveData<Int>()
    private var currentQuiz = 0
    private var data: List<Quiz> = emptyList()
    val currentQuizTitle = MutableLiveData<String>()
    val currentQuizValue = MutableLiveData<String>()
    val currentQuizAnswer = MutableLiveData<String>(null)
    val quizAttemptValue = MutableLiveData<String>()
    val quizAttempt = MutableLiveData(false)
    val btnCheckVisibility = MutableLiveData(false)
    val btnNextVisibility = MutableLiveData(false)
    val btnReloadVisibility = MutableLiveData(false)


    init {
        loadData()
    }

    @SuppressLint("CheckResult")
    fun loadData() {
        dbApi
            .getRandomQuizList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list ->
                    data = list
                    setupQuiz()
                },
                {
                    throw RuntimeException("Trololo")
                }
            )
    }

    private fun setupQuiz() {
        currentQuiz = 0
        currentQuizTitle.postValue("${currentQuiz + 1} quiz")
        currentQuizValue.postValue(data[currentQuiz].definition.definition)
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
        if (quizAttemptValue.value == data[currentQuiz].answer) {
            currentQuizTitle.postValue("Right!")

        } else {
            currentQuizTitle.postValue("Wrong!")
        }
        currentQuizAnswer.postValue(data[currentQuiz].answer)
        quizAttempt.postValue(false)
        quizAttemptValue.postValue("")
        setupButtons(next = true)
    }

    fun onPressNext() {
        currentQuizAnswer.postValue(null)
        if (currentQuiz < 9) {
            currentQuiz++
            currentQuizTitle.postValue("${currentQuiz + 1} quiz")
            currentQuizValue.postValue(data[currentQuiz].definition.definition)
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

sealed class QuizState(
    val answer: String? = null,
    val quizTitle: String? = null,
    val quizValue: String? = null,
    val btnCheckVisibility: Int = View.GONE,
    val btnNextVisibility: Int = View.GONE,
    val btnReloadVisibility: Int = View.GONE,
)

class CheckState(
    quizTitle: String?,
    quizValue: String?,
) : QuizState(
    quizTitle = quizTitle,
    quizValue = quizValue,
    btnCheckVisibility = View.VISIBLE,
)

class AnswerState(
    answer: String,
    quizTitle: String,
    quizValue: String,
) : QuizState(
    answer = answer,
    quizTitle = quizTitle,
    quizValue = quizValue,
    btnNextVisibility = View.VISIBLE,
)

class ReloadState(
    quizTitle: String,
) : QuizState(
    quizTitle = quizTitle,
    btnReloadVisibility = View.VISIBLE,
)