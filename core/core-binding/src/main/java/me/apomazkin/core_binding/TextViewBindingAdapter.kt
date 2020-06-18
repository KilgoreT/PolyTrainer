package me.apomazkin.core_binding

import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData


@BindingAdapter("bindText")
fun bindText(view: TextView, value: MutableLiveData<String>) {
    value.observeForever {
        view.text = it
    }
}