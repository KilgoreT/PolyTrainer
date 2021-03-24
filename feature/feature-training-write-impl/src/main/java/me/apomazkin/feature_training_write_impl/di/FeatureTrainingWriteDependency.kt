package me.apomazkin.feature_training_write_impl.di

import me.apomazkin.core_interactor.CoreInteractorApi

interface FeatureTrainingWriteDependency {
    fun getCoreInteractor(): CoreInteractorApi
}
