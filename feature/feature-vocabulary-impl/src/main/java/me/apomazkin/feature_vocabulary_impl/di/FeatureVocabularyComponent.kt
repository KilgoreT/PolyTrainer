package me.apomazkin.feature_vocabulary_impl.di

import androidx.navigation.NavController
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.feature_vocabulary_api.FeatureVocabularyApi
import me.apomazkin.feature_vocabulary_impl.di.module.FeatureModule
import me.apomazkin.feature_vocabulary_impl.ui.addDefinition.AddDefinitionDialog
import me.apomazkin.feature_vocabulary_impl.ui.addWord.AddWordDialog
import me.apomazkin.feature_vocabulary_impl.ui.wordList.WordListFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [FeatureModule::class],
    dependencies = [FeatureVocabularyDependency::class]
)
abstract class FeatureVocabularyComponent : FeatureVocabularyApi {

    abstract fun inject(target: WordListFragment)
    abstract fun inject(target: AddWordDialog)
    abstract fun inject(target: AddDefinitionDialog)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance navController: NavController,
            dependency: FeatureVocabularyDependency
        ): FeatureVocabularyComponent
    }


    companion object {

        private var instance: FeatureVocabularyComponent? = null

        fun initAndGet(
            navController: NavController,
            dependency: FeatureVocabularyDependency
        ): FeatureVocabularyComponent {
            if (instance == null) {
                instance = DaggerFeatureVocabularyComponent.factory()
                    .create(navController, dependency)
            }
            return instance ?: throw RuntimeException("njkjhkj")
        }

        fun get() = instance ?: throw RuntimeException("No FeatureAddWordComponent")

    }

    @Singleton
    @Component(dependencies = [CoreDbApi::class])
    interface FeatureVocabularyDependencyComponent : FeatureVocabularyDependency


}