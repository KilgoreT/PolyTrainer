package me.apomazkin.polytrainer.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.feature_vocabulary.FeatureVocabularyDependencies
import me.apomazkin.feature_vocabulary.FeatureVocabularyDependenciesImpl

@Module
interface TestModule {
    @Binds
    fun provide(impl: FeatureVocabularyDependenciesImpl): FeatureVocabularyDependencies
}