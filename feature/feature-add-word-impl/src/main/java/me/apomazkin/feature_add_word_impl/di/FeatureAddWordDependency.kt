package me.apomazkin.feature_add_word_impl.di

import me.apomazkin.core_db_api.CoreDbApi

interface FeatureAddWordDependency {
    fun getDb(): CoreDbApi
}
