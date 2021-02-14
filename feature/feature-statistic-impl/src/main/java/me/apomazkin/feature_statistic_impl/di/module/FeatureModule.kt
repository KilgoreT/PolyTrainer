package me.apomazkin.feature_statistic_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import me.apomazkin.feature_statistic_impl.navigation.FeatureStatisticNavigationImpl
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureStatisticNavigationImpl): FeatureStatisticNavigation

//    @Binds
//    abstract fun provideStateDelegate(impl: LoadStateImpl): LoadState

//    @Module
//    companion object {
//
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