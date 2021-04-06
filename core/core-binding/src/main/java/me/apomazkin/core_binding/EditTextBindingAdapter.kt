package me.apomazkin.core_binding

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputLayout

// TODO: 29.03.2021 выносить ли TextInputLayout в новый файл?
/**
 * TextInputLayout
 */
@BindingAdapter("bindEndIconClick")
fun bindEndIconClick(view: TextInputLayout, method: () -> Unit) {
    view.setEndIconOnClickListener { method.invoke() }
}

/**
 * Пример кастомного two-way data binding
 */
@BindingAdapter("bindTwoWayText")
fun setValue(view: EditText, value: MutableLiveData<String>) {
    value.observeForever {
        if (view.text.toString() != it) view.setText(it)
    }
}

@InverseBindingAdapter(attribute = "bindTwoWayText")
fun getValue(view: EditText): String {
    return view.text.toString()
}

@BindingAdapter("app:bindTwoWayTextAttrChanged")
fun setListener(view: EditText, attrChange: InverseBindingListener) {
    view.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            attrChange.onChange()
        }

        override fun afterTextChanged(s: Editable?) {}
    })
}