package me.apomazkin.feature_add_word_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_add_word_api.FeatureAddWordApi
import me.apomazkin.feature_add_word_impl.NewWordListFragment
import me.apomazkin.feature_add_word_impl.di.module.FeatureModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FeatureModule::class],
    dependencies = [FeatureAddWordDependency::class]
)
abstract class FeatureAddWordComponent : FeatureAddWordApi {

    abstract fun inject(newWordListFragment: NewWordListFragment)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance navController: NavController,
            dependency: FeatureAddWordDependency
        ): FeatureAddWordComponent
    }


    companion object {

        private var instance: FeatureAddWordComponent? = null

        fun initAndGet(
            navController: NavController,
            dependency: FeatureAddWordDependency
        ): FeatureAddWordComponent {
            if (instance == null) {
                instance = DaggerFeatureAddWordComponent
                    .factory()
                    .create(navController, dependency)
            }
            return instance ?: throw RuntimeException("njkjhkj")
        }

        fun get() = instance ?: throw RuntimeException("No FeatureAddWordComponent")

//        fun createComponent(
//            navController: NavController
//        ) = DaggerFeatureAddWordComponent
//            .factory()
//            .create(navController)
    }

    @Singleton
    @Component(dependencies = [CoreDbApi::class])
    interface FeatureAddWordDependencyComponent : FeatureAddWordDependency


}