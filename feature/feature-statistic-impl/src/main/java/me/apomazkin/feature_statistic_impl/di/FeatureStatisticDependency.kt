package me.apomazkin.feature_statistic_impl.di

import me.apomazkin.core_db_api.CoreDbApi

interface FeatureStatisticDependency {
    fun getDb(): CoreDbApi
}
