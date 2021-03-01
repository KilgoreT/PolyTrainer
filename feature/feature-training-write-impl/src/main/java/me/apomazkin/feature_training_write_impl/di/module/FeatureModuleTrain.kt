package me.apomazkin.feature_training_write_impl.di.module

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import me.apomazkin.feature_training_write_impl.FeatureViewModelFactory
import me.apomazkin.feature_training_write_impl.navigator.FeatureTrainingWriteNavigatorImpl
import javax.inject.Singleton

@Module
abstract class FeatureModuleTrain {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureTrainingWriteNavigatorImpl): FeatureTrainingWriteNavigator

    @Module
    companion object {

        @JvmStatic
        @Singleton
        @Provides
        fun provideViewModelFactory(
            dbApi: CoreDbApi,
            navigation: FeatureTrainingWriteNavigator,
        ): ViewModelProvider.Factory {
            return FeatureViewModelFactory(dbApi, navigation)
        }
    }
}