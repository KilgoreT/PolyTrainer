package me.apomazkin.feature_add_word_impl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

class AddWordViewModel @Inject constructor(
    private val dbApi: CoreDbApi
) : ViewModel() {
    val count = MutableLiveData<String>()
    val test = MutableLiveData<String>("piska")

    init {
        val qqq = dbApi.getList()
        count.postValue(qqq.size.toString())
    }


}