package me.apomazkin.feature_training_write_impl.di

import me.apomazkin.core_db_api.CoreDbApi

interface FeatureTrainingWriteDependency {
    fun getDb(): CoreDbApi
}
