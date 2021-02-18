package me.apomazkin.feature_training_list_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.feature_training_list_api.FeatureTrainingListNavigator
import me.apomazkin.feature_training_list_impl.navigator.FeatureTrainingListNavigatorImpl
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureTrainingListNavigatorImpl): FeatureTrainingListNavigator

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