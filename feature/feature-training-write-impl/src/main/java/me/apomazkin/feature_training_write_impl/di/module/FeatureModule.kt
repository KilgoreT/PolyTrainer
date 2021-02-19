package me.apomazkin.feature_training_write_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteNavigator
import me.apomazkin.feature_training_write_impl.navigator.FeatureTrainingWriteNavigatorImpl
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureTrainingWriteNavigatorImpl): FeatureTrainingWriteNavigator

//    @Binds
//    abstract fun provideStateDelegate(impl: LoadStateImpl): LoadState

//    @Module
//    companion object {
//
//        // TODO: 18.12.2020 Нужно ли оставить одну фабрику??? может лучше под каждую модуль свою фабрику???
//        @JvmStatic
//        @Singleton
//        @Provides
//        fun provideViewModelFactory(
//            dbApi: CoreDbApi,
//            navigation: FeatureVocabularyNavigation,
//            delegate: LoadState
//        ): ViewModelProvider.Factory {
//            return VocabularyViewModelFactory(dbApi, navigation, delegate)
//        }
//    }
}