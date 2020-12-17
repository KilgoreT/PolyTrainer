package me.apomazkin.feature_vocabulary_impl.di.module

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.loadState.LoadState
import me.apomazkin.feature_vocabulary_impl.loadState.LoadStateImpl
import me.apomazkin.feature_vocabulary_impl.navigator.FeatureVocabularyNavigationImpl
import me.apomazkin.feature_vocabulary_impl.viewModelFactory.VocabularyViewModelFactory
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureVocabularyNavigationImpl): FeatureVocabularyNavigation

    @Binds
    abstract fun provideStateDelegate(impl: LoadStateImpl): LoadState

    @Module
    companion object {

        // TODO: 18.12.2020 Нужно ли оставить одну фабрику??? может лучше под каждую модуль свою фабрику???
        @JvmStatic
        @Singleton
        @Provides
        fun provideViewModelFactory(
            dbApi: CoreDbApi,
            navigation: FeatureVocabularyNavigation,
            delegate: LoadState
        ): ViewModelProvider.Factory {
            return VocabularyViewModelFactory(dbApi, navigation, delegate)
        }
    }
}