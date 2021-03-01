package me.apomazkin.feature_training_write_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteApi
import me.apomazkin.feature_training_write_impl.di.module.FeatureModuleTrain
import me.apomazkin.feature_training_write_impl.ui.TrainingWriteFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FeatureModuleTrain::class],
    dependencies = [FeatureTrainingWriteDependency::class]
)
abstract class FeatureTrainingWriteComponent : FeatureTrainingWriteApi {
    abstract fun inject(trainingWriteFragment: TrainingWriteFragment)


    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance navController: NavController,
            dependency: FeatureTrainingWriteDependency
        ): FeatureTrainingWriteComponent
    }


    companion object {

        private var instance: FeatureTrainingWriteComponent? = null

        fun initAndGet(
            navController: NavController,
            dependency: FeatureTrainingWriteDependency
        ): FeatureTrainingWriteComponent {
            if (instance == null) {
                instance = DaggerFeatureTrainingWriteComponent.factory()
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
    interface FeatureTrainingListDependencyComponent : FeatureTrainingWriteDependency

}