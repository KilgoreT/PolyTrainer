package me.apomazkin.feature_bottom_menu_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.feature_bottom_menu_api.FeatureBottomMenuApi
import me.apomazkin.feature_bottom_menu_impl.di.module.GeneralModule
import me.apomazkin.feature_bottom_menu_impl.ui.tab.TrainingTabFragment

@Component(
    modules = [GeneralModule::class]
)
abstract class FeatureBottomMenuComponent : FeatureBottomMenuApi {

    abstract fun inject(trainingTabFragment: TrainingTabFragment)

    @Component.Factory
    interface FeatureBottomMenuComponentFactory {
        fun create(
            @BindsInstance navController: NavController
        ): FeatureBottomMenuComponent
    }

    companion object {

        private var instance: FeatureBottomMenuComponent? = null

        fun getOrInit(navController: NavController): FeatureBottomMenuApi {
            if (instance == null) {
                instance = DaggerFeatureBottomMenuComponent
                    .factory()
                    .create(navController)
            }
            return instance ?: throw RuntimeException("njkjhkj")
        }

        fun get() = instance ?: throw RuntimeException("No FeatureAddWordComponent")
        fun destroyFeature() {
            instance = null
        }
    }
}