package me.apomazkin.feature_training_list_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_training_list_api.FeatureTrainingListApi
import me.apomazkin.feature_training_list_impl.di.module.FeatureModule
import me.apomazkin.feature_training_list_impl.ui.TrainingListFragment
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FeatureModule::class],
    dependencies = [FeatureTrainingListDependency::class]
)
abstract class FeatureTrainingListComponent : FeatureTrainingListApi {

    abstract fun inject(trainingListFragment: TrainingListFragment)

//    abstract fun inject(target: WordListFragment)
//    abstract fun inject(target: AddWordDialog)
//    abstract fun inject(target: AddDefinitionDialog)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance @Named("parent") parentController: NavController,
            @BindsInstance @Named("current") currentController: NavController,
            dependency: FeatureTrainingListDependency
        ): FeatureTrainingListComponent
    }


    companion object {

        private var instance: FeatureTrainingListComponent? = null

        fun initAndGet(
            parentController: NavController,
            currentController: NavController,
            dependency: FeatureTrainingListDependency
        ): FeatureTrainingListComponent {
            if (instance == null) {
                instance = DaggerFeatureTrainingListComponent.factory()
                    .create(parentController, currentController, dependency)
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