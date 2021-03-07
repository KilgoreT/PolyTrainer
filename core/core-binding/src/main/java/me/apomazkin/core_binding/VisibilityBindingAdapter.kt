package me.apomazkin.core_binding

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData


@BindingAdapter("bindVisibility")
fun bindVisibility(view: View, value: MutableLiveData<Boolean>) {
    value.observeForever {
        view.visibility = setupVisibility(it, View.INVISIBLE)
    }
}

@BindingAdapter("bindVisibilityOrGone")
fun bindVisibilityOrGone(view: View, value: MutableLiveData<Boolean>) {
    value.observeForever {
        view.visibility = setupVisibility(it, View.GONE)
    }
}

private fun setupVisibility(isVisible: Boolean, negativeValue: Int): Int {
    return if (isVisible) {
        View.VISIBLE
    } else {
        negativeValue
    }
}