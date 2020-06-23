package me.apomazkin.feature_vocabulary_impl.di

import me.apomazkin.core_db_api.CoreDbApi

interface FeatureVocabularyDependency {
    fun getDb(): CoreDbApi
}
