package me.apomazkin.feature_statistic_impl.di.module

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.core_interactor.di.CoreInteractorComponent
import me.apomazkin.feature_statistic_api.FeatureStatisticNavigation
import me.apomazkin.feature_statistic_impl.navigation.FeatureStatisticNavigationImpl
import me.apomazkin.feature_statistic_impl.ui.FeatureViewModelFactory
import javax.inject.Singleton

@Module
abstract class FeatureModule {

    @Singleton
    @Binds
    abstract fun provideNavigator(impl: FeatureStatisticNavigationImpl): FeatureStatisticNavigation



//    @Binds
//    abstract fun provideStateDelegate(impl: LoadStateImpl): LoadState

    @Module
    companion object {

        @JvmStatic
        @Singleton
        @Provides
        fun provideViewModelFactory(
            coreInteractorApi: CoreInteractorApi,
            navigation: FeatureStatisticNavigation,
//            delegate: LoadState
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