package me.apomazkin.feature_vocabulary_impl.loadState

sealed class LoadStateStatus
object Load : LoadStateStatus()
object Empty : LoadStateStatus()
object Data : LoadStateStatus()
class Error(error: Throwable) : LoadStateStatus()