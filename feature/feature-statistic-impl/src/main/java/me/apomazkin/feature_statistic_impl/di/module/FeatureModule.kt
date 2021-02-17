package me.apomazkin.feature_statistic_impl.di.module

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import me.apomazkin.feature_statistic_impl.domain.*
import me.apomazkin.feature_statistic_impl.navigation.FeatureStatisticNavigationImpl
import me.apomazkin.feature_statistic_impl.ui.FeatureViewModelFactory
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureStatisticNavigationImpl): FeatureStatisticNavigation

    @Singleton
    @Binds
    abstract fun provideWordCountUseCase(impl: WordCountUseCase.WordCountUseCaseImpl): WordCountUseCase

    @Singleton
    @Binds
    abstract fun provideDefinitionCountUseCase(impl: DefinitionCountUseCase.DefinitionCountUseCaseImpl): DefinitionCountUseCase

    @Singleton
    @Binds
    abstract fun provideNounCountUseCase(impl: NounCountUseCase.NounCountUseCaseImpl): NounCountUseCase

    @Singleton
    @Binds
    abstract fun provideVerbCountUseCase(impl: VerbCountUseCase.VerbCountUseCaseImpl): VerbCountUseCase

    @Singleton
    @Binds
    abstract fun provideAdverbCountUseCase(impl: AdverbCountUseCase.AdverbCountUseCaseImpl): AdverbCountUseCase

    @Singleton
    @Binds
    abstract fun provideAdjectiveCountUseCase(impl: AdjectiveCountUseCase.AdjectiveCountUseCaseImpl): AdjectiveCountUseCase

    @Singleton
    @Binds
    abstract fun provideStatisticScenario(impl: StatisticScenario.StatisticScenarioImpl): StatisticScenario

//    @Binds
//    abstract fun provideStateDelegate(impl: LoadStateImpl): LoadState

    @Module
    companion object {

        @JvmStatic
        @Singleton
        @Provides
        fun provideViewModelFactory(
            dbApi: CoreDbApi,
            navigation: FeatureStatisticNavigation,
            scenario: StatisticScenario
//            delegate: LoadState
        ): ViewModelProvider.Factory {
            return FeatureViewModelFactory(dbApi, navigation, scenario)
        }
    }
}