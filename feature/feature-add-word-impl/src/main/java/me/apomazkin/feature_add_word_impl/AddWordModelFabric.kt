package me.apomazkin.feature_add_word_impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.apomazkin.core_db_api.CoreDbApi
import javax.inject.Inject

class AddWordModelFabric @Inject constructor(
    private val dbApi: CoreDbApi
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(AddWordViewModel::class.java)) {
            AddWordViewModel(dbApi) as T
        } else {
            throw IllegalStateException("Cannot find ViewModel class")
        }
    }
}