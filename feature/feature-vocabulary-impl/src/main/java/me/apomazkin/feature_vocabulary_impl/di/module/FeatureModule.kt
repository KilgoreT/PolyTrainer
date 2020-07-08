package me.apomazkin.feature_vocabulary_impl.di.module

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyNavigation
import me.apomazkin.feature_vocabulary_impl.navigator.FeatureVocabularyNavigationImpl
import me.apomazkin.feature_vocabulary_impl.viewModelFactory.VocabularyViewModelFactory
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureVocabularyNavigationImpl): FeatureVocabularyNavigation

    @Module
    companion object {

        @JvmStatic
        @Singleton
        @Provides
        fun provideViewModelFactory(
            dbApi: CoreDbApi,
            navigation: FeatureVocabularyNavigation
        ): ViewModelProvider.Factory {
            return VocabularyViewModelFactory(dbApi, navigation)
        }
    }
}