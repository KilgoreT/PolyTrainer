package me.apomazkin.feature_training_list_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_training_list_api.FeatureTrainingListApi
import me.apomazkin.feature_training_list_impl.di.module.FeatureModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FeatureModule::class],
    dependencies = [FeatureTrainingListDependency::class]
)
abstract class FeatureTrainingListComponent : FeatureTrainingListApi {

//    abstract fun inject(target: WordListFragment)
//    abstract fun inject(target: AddWordDialog)
//    abstract fun inject(target: AddDefinitionDialog)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance navController: NavController,
            dependency: FeatureTrainingListDependency
        ): FeatureTrainingListComponent
    }


    companion object {

        private var instance: FeatureTrainingListComponent? = null

        fun initAndGet(
            navController: NavController,
            dependency: FeatureTrainingListDependency
        ): FeatureTrainingListComponent {
            if (instance == null) {
                instance = DaggerFeatureTrainingListComponent.factory()
                    .create(navController, dependency)
            }
            return instance ?: throw RuntimeException("No FeatureTrainingListComponent!!!")
        }

        fun get() = instance ?: throw RuntimeException("No FeatureTrainingListComponent")

        fun destroyFeature() {
            instance = null
        }

    }

    @Singleton
    @Component(dependencies = [CoreDbApi::class])
    interface FeatureTrainingListDependencyComponent : FeatureTrainingListDependency


}