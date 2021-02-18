package me.apomazkin.feature_training_list_impl.di

import me.apomazkin.core_db_api.CoreDbApi

interface FeatureTrainingListDependency {
    fun getDb(): CoreDbApi
}
