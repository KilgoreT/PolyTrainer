package me.apomazkin.core_binding

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData


@BindingAdapter("bindText")
fun bindText(view: TextView, value: MutableLiveData<String>) {
    value.observeForever {
        if (view.text != it) view.text = it
    }
}

@BindingAdapter("bindTextOrInvisible")
fun bindTextOrInvisible(view: TextView, value: MutableLiveData<String?>) {
    value.observeForever {
        view.visibility = it?.let {
            if (view.text != it) view.text = it
            View.VISIBLE
        } ?: View.INVISIBLE
    }
}

@BindingAdapter("bindTextOrGone")
fun bindTextOrGone(view: TextView, value: MutableLiveData<String?>) {
    value.observeForever {
        view.visibility = it?.let {
            if (view.text != it) view.text = it
            View.VISIBLE
        } ?: View.GONE
    }
}