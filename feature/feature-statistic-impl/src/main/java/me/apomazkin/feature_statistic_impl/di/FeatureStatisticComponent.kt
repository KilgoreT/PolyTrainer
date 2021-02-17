package me.apomazkin.feature_statistic_impl.di

import android.view.ViewGroup
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_statistic_api.FeatureStatisticApi
import me.apomazkin.feature_statistic_impl.di.module.FeatureModule
import me.apomazkin.feature_statistic_impl.ui.StatisticFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FeatureModule::class],
    dependencies = [FeatureStatisticDependency::class]
)
abstract class FeatureStatisticComponent : FeatureStatisticApi {

    abstract fun inject(statisticFragment: StatisticFragment)

    @Component.Factory
    interface Factory {
        fun create(
//            @BindsInstance navController: NavController,
            @BindsInstance featureContainer: ViewGroup,
            dependency: FeatureStatisticDependency
        ): FeatureStatisticComponent
    }


    companion object {

        private var instance: FeatureStatisticComponent? = null

        fun initAndGet(
            featureContainer: ViewGroup,
            dependency: FeatureStatisticDependency
        ): FeatureStatisticComponent {
            if (instance == null) {
                instance = DaggerFeatureStatisticComponent.factory()
                    .create(featureContainer, dependency)
            }
            return instance ?: throw RuntimeException("njkjhkj")
        }

        fun get() = instance ?: throw RuntimeException("No Component")

        fun destroyFeature() {
            instance = null
        }

    }

    @Singleton
    @Component(dependencies = [CoreDbApi::class])
    interface FeatureStatisticDependencyComponent : FeatureStatisticDependency


}