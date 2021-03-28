package me.apomazkin.core_binding

import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData

@BindingAdapter("bindTransition")
fun bindTransition(view: MotionLayout, value: MutableLiveData<String>) {
    value.observeForever {
        when (it) {
            "START" -> {
                view.transitionToStart()
            }
            "END" -> {
                view.transitionToEnd()
            }
            else -> {
            }
        }
    }
}