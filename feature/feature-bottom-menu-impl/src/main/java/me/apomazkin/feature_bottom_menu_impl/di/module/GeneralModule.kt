package me.apomazkin.feature_bottom_menu_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuNavigator
import me.apomazkin.feature_bottom_menu_impl.navigator.FeatureBottomMenuNavigatorImpl


@Module
interface GeneralModule {

    @Binds
    fun provideNavigator(impl: FeatureBottomMenuNavigatorImpl): FeatureBottomMenuNavigator

}