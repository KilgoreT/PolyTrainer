package me.apomazkin.feature_vocabulary_impl.loadState

import androidx.lifecycle.MutableLiveData

interface LoadState {
    fun getState(): MutableLiveData<LoadStateStatus>
    fun onLoad()
    fun onData()
    fun onEmpty()
    fun onError(error: Throwable)
}