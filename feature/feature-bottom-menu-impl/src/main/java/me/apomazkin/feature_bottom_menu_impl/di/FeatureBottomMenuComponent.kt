package me.apomazkin.feature_bottom_menu_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuApi
import me.apomazkin.feature_bottom_menu_impl.di.module.GeneralModule

@Component(
    modules = [GeneralModule::class]
)
abstract class FeatureBottomMenuComponent : FeatureBottomMenuApi {

    @Component.Factory
    interface FeatureBottomMenuComponentFactory {
        fun create(
            @BindsInstance navController: NavController
        ): FeatureBottomMenuComponent
    }

    companion object {
        fun getOrInit(navController: NavController): FeatureBottomMenuApi {
            return DaggerFeatureBottomMenuComponent
                .factory()
                .create(navController)
        }
    }
}