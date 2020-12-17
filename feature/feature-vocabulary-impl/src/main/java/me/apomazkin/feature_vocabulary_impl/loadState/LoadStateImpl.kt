package me.apomazkin.feature_vocabulary_impl.loadState

import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

class LoadStateImpl @Inject constructor() : LoadState {

    private val state = MutableLiveData<LoadStateStatus>(Empty)
    override fun getState() = state

    override fun onLoad() {
        state.postValue(Load)
    }

    override fun onData() {
        state.postValue(Data)
    }

    override fun onEmpty() {
        state.postValue(Empty)
    }

    override fun onError(error: Throwable) {
        state.postValue(Error(error))
    }
}