package me.apomazkin.view_progress_quiz

import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData

// TODO: 14.03.2021 нехрен имх хранить здесь биндер адаптер, потому что зависимость от капта(
@BindingAdapter("bindCurrentQuiz")
fun bindCurrentQuiz(view: ProgressQuizView, value: MutableLiveData<Int?>) {
    value.observeForever { currentQuiz ->
        currentQuiz?.let { view.currentQuiz = it }
    }
}
