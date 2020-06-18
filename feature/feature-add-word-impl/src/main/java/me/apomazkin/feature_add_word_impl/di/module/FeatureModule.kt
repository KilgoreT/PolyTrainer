package me.apomazkin.feature_add_word_impl.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_add_word_api.FeatureAddWordNavigation
import me.apomazkin.feature_add_word_impl.AddWordModelFabric
import me.apomazkin.feature_add_word_impl.navigator.FeatureAddWordNavigationImpl
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureAddWordNavigationImpl): FeatureAddWordNavigation

    @Module
    companion object {

        @JvmStatic
        @Singleton
        @Provides
        fun provideViewModelFactory(dbApi: CoreDbApi): AddWordModelFabric {
            return AddWordModelFabric(dbApi)
        }
    }
}