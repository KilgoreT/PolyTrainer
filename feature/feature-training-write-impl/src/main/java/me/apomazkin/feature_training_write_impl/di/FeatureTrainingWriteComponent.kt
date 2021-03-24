package me.apomazkin.feature_training_write_impl.di

import android.content.Context
import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.feature_training_write_api.FeatureTrainingWriteApi
import me.apomazkin.feature_training_write_impl.di.module.FeatureModuleTrain
import me.apomazkin.feature_training_write_impl.ui.TrainingWriteFragment
import javax.inject.Singleton

@Singleton
@Component(modules = [FeatureModuleTrain::class])
abstract class FeatureTrainingWriteComponent : FeatureTrainingWriteApi {
    abstract fun inject(trainingWriteFragment: TrainingWriteFragment)


    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance navController: NavController,
            @BindsInstance context: Context,
        ): FeatureTrainingWriteComponent
    }


    companion object {

        private var instance: FeatureTrainingWriteComponent? = null

        fun initAndGet(
            navController: NavController,
            context: Context
        ): FeatureTrainingWriteComponent {
            if (instance == null) {
                instance = DaggerFeatureTrainingWriteComponent.factory()
                    .create(navController, context)
            }
            return instance ?: throw RuntimeException("No FeatureTrainingListComponent!!!")
        }

        fun get() = instance ?: throw RuntimeException("No FeatureTrainingListComponent")

        fun destroyFeature() {
            instance = null
        }

    }

}