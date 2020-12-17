package me.apomazkin.feature_vocabulary_impl.loadState

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData

const val LOAD_ROLE = "load"
const val DATA_ROLE = "data"
const val EMPTY_ROLE = "empty"
const val ERROR_ROLE = "error"

@BindingAdapter("bindLoadState", "bindLoadStatusRole")
fun bindT(view: View, state: MutableLiveData<LoadStateStatus>, role: String) {
    state.observeForever { status ->
        when (status) {
            is Load -> setLoad(view, role)
            is Data -> setData(view, role)
            is Empty -> {
            }
            is Error -> {
            }
        }
    }
}

private fun setLoad(view: View, role: String) {
    if (role == LOAD_ROLE) {
        view.visibility = View.VISIBLE
    }
}

fun setData(view: View, status: String) {
    when (status) {
        LOAD_ROLE -> {
            view.visibility = View.GONE
        }
        else -> {
            view.visibility = View.VISIBLE
        }
    }
}

