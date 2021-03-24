package me.apomazkin.feature_training_write_impl.di.module

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.core_interactor.di.CoreInteractorComponent
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
            coreInteractorApi: CoreInteractorApi,
            navigation: FeatureTrainingWriteNavigator,
        ): ViewModelProvider.Factory {
            return FeatureViewModelFactory(coreInteractorApi, navigation)
        }

        @JvmStatic
        @Singleton
        @Provides
        fun provideCoreInteractorApi(context: Context): CoreInteractorApi {
            return CoreInteractorComponent.getOrInit(context)
        }
    }
}